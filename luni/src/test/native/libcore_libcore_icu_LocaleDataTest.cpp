/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_NDEBUG 1
#define LOG_TAG "LocaleDataTest"

#include <string.h>
#include <memory>

#include <log/log.h>
#include <nativehelper/JNIHelp.h>
#include <nativehelper/ScopedLocalRef.h>
#include <nativehelper/ScopedStringChars.h>
#include <nativehelper/ScopedUtfChars.h>

#include "ScopedIcuLocale.h"
#include "unicode/brkiter.h"
#include "unicode/calendar.h"
#include "unicode/dcfmtsym.h"
#include "unicode/decimfmt.h"
#include "unicode/dtfmtsym.h"
#include "unicode/locid.h"
#include "unicode/numfmt.h"
#include "unicode/strenum.h"
#include "unicode/ucasemap.h"
#include "unicode/udat.h"
#include "unicode/uloc.h"
#include "unicode/ures.h"
#include "unicode/ustring.h"


static jclass icu4cLocaleDataClass = nullptr;

class ScopedResourceBundle {
 public:
  explicit ScopedResourceBundle(UResourceBundle* bundle) : bundle_(bundle) {
  }

  ~ScopedResourceBundle() {
    if (bundle_ != NULL) {
      ures_close(bundle_);
    }
  }

  UResourceBundle* get() {
    return bundle_;
  }

  bool hasKey(const char* key) {
    UErrorCode status = U_ZERO_ERROR;
    ures_getStringByKey(bundle_, key, NULL, &status);
    return U_SUCCESS(status);
  }

 private:
  UResourceBundle* bundle_;
  DISALLOW_COPY_AND_ASSIGN(ScopedResourceBundle);
};

template <typename T>
static jobject valueOf(JNIEnv* env, jclass c, const char* signature, const T& value) {
    static jmethodID valueOfMethod = env->GetStaticMethodID(c, "valueOf", signature);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    jobject result = env->CallStaticObjectMethod(c, valueOfMethod, value);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    return result;
}

static jobject integerValueOf(JNIEnv* env, jint value) {
    return valueOf(env, env->FindClass("java/lang/Integer"), "(I)Ljava/lang/Integer;", value);
}


static bool setIntegerField(JNIEnv* env, jobject obj, const char* fieldName, int value) {
  ScopedLocalRef<jobject> integerValue(env, integerValueOf(env, value));
  if (integerValue.get() == NULL) return false;
  jfieldID fid = env->GetFieldID(icu4cLocaleDataClass, fieldName, "Ljava/lang/Integer;");
  env->SetObjectField(obj, fid, integerValue.get());
  return true;
}

static void setStringField(JNIEnv* env, jobject obj, const char* fieldName, jstring value) {
    jfieldID fid = env->GetFieldID(icu4cLocaleDataClass, fieldName, "Ljava/lang/String;");
    env->SetObjectField(obj, fid, value);
    env->DeleteLocalRef(value);
}

static void setStringArrayField(JNIEnv* env, jobject obj, const char* fieldName, jobjectArray value) {
    jfieldID fid = env->GetFieldID(icu4cLocaleDataClass, fieldName, "[Ljava/lang/String;");
    env->SetObjectField(obj, fid, value);
}

static void setStringArrayField(JNIEnv* env, jobject obj, const char* fieldName, const icu::UnicodeString* valueArray, int32_t size) {
  ScopedLocalRef<jobjectArray> result(env, env->NewObjectArray(size, env->FindClass("java/lang/String"), NULL));
  for (int32_t i = 0; i < size ; i++) {
    ScopedLocalRef<jstring> s(env, jniCreateString(env, valueArray[i].getBuffer(),valueArray[i].length()));
    if (env->ExceptionCheck()) {
      return;
    }
    env->SetObjectArrayElement(result.get(), i, s.get());
    if (env->ExceptionCheck()) {
      return;
    }
  }
  setStringArrayField(env, obj, fieldName, result.get());
}

static void setStringField(JNIEnv* env, jobject obj, const char* fieldName, UResourceBundle* bundle, int index) {
  UErrorCode status = U_ZERO_ERROR;
  int charCount;
  const UChar* chars;
  UResourceBundle* currentBundle = ures_getByIndex(bundle, index, NULL, &status);
  switch (ures_getType(currentBundle)) {
      case URES_STRING:
         chars = ures_getString(currentBundle, &charCount, &status);
         break;
      case URES_ARRAY:
         // In case there is an array, Android currently only cares about the
         // first string of that array, the rest of the array is used by ICU
         // for additional data ignored by Android.
         chars = ures_getStringByIndex(currentBundle, 0, &charCount, &status);
         break;
      default:
         status = U_INVALID_FORMAT_ERROR;
  }
  ures_close(currentBundle);

  if (U_SUCCESS(status)) {
    setStringField(env, obj, fieldName, jniCreateString(env, chars, charCount));
  } else {
    ALOGE("Error setting String field %s from ICU resource (index %d): %s", fieldName, index, u_errorName(status));
  }
}

static void setCharField(JNIEnv* env, jobject obj, const char* fieldName, const icu::UnicodeString& value) {
  if (value.length() == 0) {
    return;
  }
  jfieldID fid = env->GetFieldID(icu4cLocaleDataClass, fieldName, "C");
  env->SetCharField(obj, fid, value.charAt(0));
}

static void setStringField(JNIEnv* env, jobject obj, const char* fieldName, const icu::UnicodeString& value) {
  const UChar* chars = value.getBuffer();
  setStringField(env, obj, fieldName, jniCreateString(env, chars, value.length()));
}

static void setNumberPatterns(JNIEnv* env, jobject obj, icu::Locale& locale) {
  UErrorCode status = U_ZERO_ERROR;

  icu::UnicodeString pattern;
  std::unique_ptr<icu::DecimalFormat> fmt(static_cast<icu::DecimalFormat*>(icu::NumberFormat::createInstance(locale, UNUM_CURRENCY, status)));
  pattern = fmt->toPattern(pattern.remove());
  setStringField(env, obj, "currencyPattern", pattern);

  fmt.reset(static_cast<icu::DecimalFormat*>(icu::NumberFormat::createInstance(locale, UNUM_DECIMAL, status)));
  pattern = fmt->toPattern(pattern.remove());
  setStringField(env, obj, "numberPattern", pattern);

  fmt.reset(static_cast<icu::DecimalFormat*>(icu::NumberFormat::createInstance(locale, UNUM_PERCENT, status)));
  pattern = fmt->toPattern(pattern.remove());
  setStringField(env, obj, "percentPattern", pattern);
}

static void setDecimalFormatSymbolsData(JNIEnv* env, jobject obj, icu::Locale& locale) {
  UErrorCode status = U_ZERO_ERROR;
  icu::DecimalFormatSymbols dfs(locale, status);

  setCharField(env, obj, "decimalSeparator", dfs.getSymbol(icu::DecimalFormatSymbols::kDecimalSeparatorSymbol));
  setCharField(env, obj, "groupingSeparator", dfs.getSymbol(icu::DecimalFormatSymbols::kGroupingSeparatorSymbol));
  setCharField(env, obj, "patternSeparator", dfs.getSymbol(icu::DecimalFormatSymbols::kPatternSeparatorSymbol));
  setStringField(env, obj, "percent", dfs.getSymbol(icu::DecimalFormatSymbols::kPercentSymbol));
  setStringField(env, obj, "perMill", dfs.getSymbol(icu::DecimalFormatSymbols::kPerMillSymbol));
  setCharField(env, obj, "monetarySeparator", dfs.getSymbol(icu::DecimalFormatSymbols::kMonetarySeparatorSymbol));
  setStringField(env, obj, "minusSign", dfs.getSymbol(icu::DecimalFormatSymbols:: kMinusSignSymbol));
  setStringField(env, obj, "exponentSeparator", dfs.getSymbol(icu::DecimalFormatSymbols::kExponentialSymbol));
  setStringField(env, obj, "infinity", dfs.getSymbol(icu::DecimalFormatSymbols::kInfinitySymbol));
  setStringField(env, obj, "NaN", dfs.getSymbol(icu::DecimalFormatSymbols::kNaNSymbol));
  setCharField(env, obj, "zeroDigit", dfs.getSymbol(icu::DecimalFormatSymbols::kZeroDigitSymbol));
}


// Iterates up through the locale hierarchy. So "en_US" would return "en_US", "en", "".
class LocaleNameIterator {
 public:
  LocaleNameIterator(const char* locale_name, UErrorCode& status) : status_(status), has_next_(true) {
    strcpy(locale_name_, locale_name);
    locale_name_length_ = strlen(locale_name_);
  }

  const char* Get() {
      return locale_name_;
  }

  bool HasNext() {
    return has_next_;
  }

  void Up() {
    if (locale_name_length_ == 0) {
      has_next_ = false;
    } else {
      locale_name_length_ = uloc_getParent(locale_name_, locale_name_, sizeof(locale_name_), &status_);
    }
  }

 private:
  UErrorCode& status_;
  bool has_next_;
  char locale_name_[ULOC_FULLNAME_CAPACITY];
  int32_t locale_name_length_;

  DISALLOW_COPY_AND_ASSIGN(LocaleNameIterator);
};

static bool getAmPmMarkersNarrow(JNIEnv* env, jobject localeData, const char* locale_name) {
  UErrorCode status = U_ZERO_ERROR;
  ScopedResourceBundle root(ures_open(NULL, locale_name, &status));
  if (U_FAILURE(status)) {
    return false;
  }
  ScopedResourceBundle calendar(ures_getByKey(root.get(), "calendar", NULL, &status));
  if (U_FAILURE(status)) {
    return false;
  }
  ScopedResourceBundle gregorian(ures_getByKey(calendar.get(), "gregorian", NULL, &status));
  if (U_FAILURE(status)) {
    return false;
  }
  ScopedResourceBundle amPmMarkersNarrow(ures_getByKey(gregorian.get(), "AmPmMarkersNarrow", NULL, &status));
  if (U_FAILURE(status)) {
    return false;
  }
  setStringField(env, localeData, "narrowAm", amPmMarkersNarrow.get(), 0);
  setStringField(env, localeData, "narrowPm", amPmMarkersNarrow.get(), 1);
  return true;
}

static bool getDateTimePatterns(JNIEnv* env, jobject localeData, const char* locale_name) {
  UErrorCode status = U_ZERO_ERROR;
  ScopedResourceBundle root(ures_open(NULL, locale_name, &status));
  if (U_FAILURE(status)) {
    return false;
  }
  ScopedResourceBundle calendar(ures_getByKey(root.get(), "calendar", NULL, &status));
  if (U_FAILURE(status)) {
    return false;
  }
  ScopedResourceBundle gregorian(ures_getByKey(calendar.get(), "gregorian", NULL, &status));
  if (U_FAILURE(status)) {
    return false;
  }
  ScopedResourceBundle dateTimePatterns(ures_getByKey(gregorian.get(), "DateTimePatterns", NULL, &status));
  if (U_FAILURE(status)) {
    return false;
  }
  setStringField(env, localeData, "fullTimeFormat", dateTimePatterns.get(), 0);
  setStringField(env, localeData, "longTimeFormat", dateTimePatterns.get(), 1);
  setStringField(env, localeData, "mediumTimeFormat", dateTimePatterns.get(), 2);
  setStringField(env, localeData, "shortTimeFormat", dateTimePatterns.get(), 3);
  setStringField(env, localeData, "fullDateFormat", dateTimePatterns.get(), 4);
  setStringField(env, localeData, "longDateFormat", dateTimePatterns.get(), 5);
  setStringField(env, localeData, "mediumDateFormat", dateTimePatterns.get(), 6);
  setStringField(env, localeData, "shortDateFormat", dateTimePatterns.get(), 7);
  return true;
}

extern "C" jboolean Java_libcore_libcore_icu_LocaleDataTest_initIcu4cLocaleData(JNIEnv* env,
    jclass, jstring javaLanguageTag, jobject icu4cLocaleData, jobject dataClass) {
  if (icu4cLocaleDataClass == nullptr) {
    // It's okay to hold the class in the test.
    icu4cLocaleDataClass = reinterpret_cast<jclass>(env->NewGlobalRef(dataClass));
  }

  // alias pointer
  jobject localeData = icu4cLocaleData;

  ScopedUtfChars languageTag(env, javaLanguageTag);
  if (languageTag.c_str() == NULL) {
    return JNI_FALSE;
  }
  if (languageTag.size() >= ULOC_FULLNAME_CAPACITY) {
    return JNI_FALSE; // ICU has a fixed-length limit.
  }

  ScopedIcuLocale icuLocale(env, javaLanguageTag);
  if (!icuLocale.valid()) {
    return JNI_FALSE;
  }

  // Get the DateTimePatterns.
  UErrorCode status = U_ZERO_ERROR;
  bool foundDateTimePatterns = false;
  for (LocaleNameIterator it(icuLocale.locale().getBaseName(), status); it.HasNext(); it.Up()) {
    if (getDateTimePatterns(env, localeData, it.Get())) {
      foundDateTimePatterns = true;
      break;
    }
  }
  if (!foundDateTimePatterns) {
    ALOGE("Couldn't find ICU DateTimePatterns for %s", languageTag.c_str());
    return JNI_FALSE;
  }

  // Get the narrow "AM" and "PM" strings.
  bool foundAmPmMarkersNarrow = false;
  for (LocaleNameIterator it(icuLocale.locale().getBaseName(), status); it.HasNext(); it.Up()) {
    if (getAmPmMarkersNarrow(env, localeData, it.Get())) {
      foundAmPmMarkersNarrow = true;
      break;
    }
  }
  if (!foundAmPmMarkersNarrow) {
    ALOGE("Couldn't find ICU AmPmMarkersNarrow for %s", languageTag.c_str());
    return JNI_FALSE;
  }

  status = U_ZERO_ERROR;
  std::unique_ptr<icu::Calendar> cal(icu::Calendar::createInstance(icuLocale.locale(), status));
  if (U_FAILURE(status)) {
    return JNI_FALSE;
  }
  if (!setIntegerField(env, localeData, "firstDayOfWeek", cal->getFirstDayOfWeek())) {
    return JNI_FALSE;
  }
  if (!setIntegerField(env, localeData, "minimalDaysInFirstWeek", cal->getMinimalDaysInFirstWeek())) {
    return JNI_FALSE;
  }

  // Get DateFormatSymbols.
  status = U_ZERO_ERROR;
  icu::DateFormatSymbols dateFormatSym(icuLocale.locale(), status);
  if (U_FAILURE(status)) {
    return JNI_FALSE;
  }

  // Get AM/PM and BC/AD.
  int32_t count = 0;
  const icu::UnicodeString* amPmStrs = dateFormatSym.getAmPmStrings(count);
  setStringArrayField(env, localeData, "amPm", amPmStrs, count);
  const icu::UnicodeString* erasStrs = dateFormatSym.getEras(count);
  setStringArrayField(env, localeData, "eras", erasStrs, count);

  const icu::UnicodeString* longMonthNames =
      dateFormatSym.getMonths(count, icu::DateFormatSymbols::FORMAT, icu::DateFormatSymbols::WIDE);
  setStringArrayField(env, localeData, "longMonthNames", longMonthNames, count);
  const icu::UnicodeString* shortMonthNames =
      dateFormatSym.getMonths(count, icu::DateFormatSymbols::FORMAT, icu::DateFormatSymbols::ABBREVIATED);
  setStringArrayField(env, localeData, "shortMonthNames", shortMonthNames, count);
  const icu::UnicodeString* tinyMonthNames =
      dateFormatSym.getMonths(count, icu::DateFormatSymbols::FORMAT, icu::DateFormatSymbols::NARROW);
  setStringArrayField(env, localeData, "tinyMonthNames", tinyMonthNames, count);
  const icu::UnicodeString* longWeekdayNames =
      dateFormatSym.getWeekdays(count, icu::DateFormatSymbols::FORMAT, icu::DateFormatSymbols::WIDE);
  setStringArrayField(env, localeData, "longWeekdayNames", longWeekdayNames, count);
  const icu::UnicodeString* shortWeekdayNames =
      dateFormatSym.getWeekdays(count, icu::DateFormatSymbols::FORMAT, icu::DateFormatSymbols::ABBREVIATED);
  setStringArrayField(env, localeData, "shortWeekdayNames", shortWeekdayNames, count);
  const icu::UnicodeString* tinyWeekdayNames =
      dateFormatSym.getWeekdays(count, icu::DateFormatSymbols::FORMAT, icu::DateFormatSymbols::NARROW);
  setStringArrayField(env, localeData, "tinyWeekdayNames", tinyWeekdayNames, count);

  const icu::UnicodeString* longStandAloneMonthNames =
      dateFormatSym.getMonths(count, icu::DateFormatSymbols::STANDALONE, icu::DateFormatSymbols::WIDE);
  setStringArrayField(env, localeData, "longStandAloneMonthNames", longStandAloneMonthNames, count);
  const icu::UnicodeString* shortStandAloneMonthNames =
      dateFormatSym.getMonths(count, icu::DateFormatSymbols::STANDALONE, icu::DateFormatSymbols::ABBREVIATED);
  setStringArrayField(env, localeData, "shortStandAloneMonthNames", shortStandAloneMonthNames, count);
  const icu::UnicodeString* tinyStandAloneMonthNames =
      dateFormatSym.getMonths(count, icu::DateFormatSymbols::STANDALONE, icu::DateFormatSymbols::NARROW);
  setStringArrayField(env, localeData, "tinyStandAloneMonthNames", tinyStandAloneMonthNames, count);
  const icu::UnicodeString* longStandAloneWeekdayNames =
      dateFormatSym.getWeekdays(count, icu::DateFormatSymbols::STANDALONE, icu::DateFormatSymbols::WIDE);
  setStringArrayField(env, localeData, "longStandAloneWeekdayNames", longStandAloneWeekdayNames, count);
  const icu::UnicodeString* shortStandAloneWeekdayNames =
      dateFormatSym.getWeekdays(count, icu::DateFormatSymbols::STANDALONE, icu::DateFormatSymbols::ABBREVIATED);
  setStringArrayField(env, localeData, "shortStandAloneWeekdayNames", shortStandAloneWeekdayNames, count);
  const icu::UnicodeString* tinyStandAloneWeekdayNames =
      dateFormatSym.getWeekdays(count, icu::DateFormatSymbols::STANDALONE, icu::DateFormatSymbols::NARROW);
  setStringArrayField(env, localeData, "tinyStandAloneWeekdayNames", tinyStandAloneWeekdayNames, count);

  status = U_ZERO_ERROR;

  // For numberPatterns and symbols.
  setNumberPatterns(env, localeData, icuLocale.locale());
  setDecimalFormatSymbolsData(env, localeData, icuLocale.locale());

  return JNI_TRUE;
}
