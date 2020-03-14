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
#define LOG_TAG "ICU"

#include <memory>
#include <vector>

#include <androidicuinit/IcuRegistration.h>
#include <log/log.h>
#include <nativehelper/JNIHelp.h>
#include <nativehelper/ScopedStringChars.h>
#include <nativehelper/ScopedUtfChars.h>
#include <nativehelper/jni_macros.h>
#include <nativehelper/toStringArray.h>

#include "IcuUtilities.h"
#include "JniException.h"
#include "ScopedIcuULoc.h"
#include "unicode/udatpg.h"
#include "unicode/uloc.h"
#include "unicode/ures.h"
#include "unicode/ustring.h"
#include "ureslocs.h"

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

static jstring ICU_getScript(JNIEnv* env, jclass, jstring javaLocaleName) {
  ScopedIcuULoc icuLocale(env, javaLocaleName);
  if (!icuLocale.valid()) {
    return NULL;
  }
  // Normal script part is 4-char long. Being conservative for allocation size
  // because if the locale contains script part, it should not be longer than the locale itself.
  int32_t capacity = std::max(ULOC_SCRIPT_CAPACITY, icuLocale.locale_length() + 1);
  std::unique_ptr<char[]> buffer(new char(capacity));
  UErrorCode status = U_ZERO_ERROR;
  uloc_getScript(icuLocale.locale(), buffer.get(), capacity, &status);
  if (U_FAILURE(status)) {
    return NULL;
  }
  return env->NewStringUTF(buffer.get());
}

// TODO: rewrite this with int32_t ucurr_forLocale(const char* locale, UChar* buff, int32_t buffCapacity, UErrorCode* ec)...
static jstring ICU_getCurrencyCode(JNIEnv* env, jclass, jstring javaCountryCode) {
    UErrorCode status = U_ZERO_ERROR;
    ScopedResourceBundle supplData(ures_openDirect(U_ICUDATA_CURR, "supplementalData", &status));
    if (U_FAILURE(status)) {
        return NULL;
    }

    ScopedResourceBundle currencyMap(ures_getByKey(supplData.get(), "CurrencyMap", NULL, &status));
    if (U_FAILURE(status)) {
        return NULL;
    }

    ScopedUtfChars countryCode(env, javaCountryCode);
    ScopedResourceBundle currency(ures_getByKey(currencyMap.get(), countryCode.c_str(), NULL, &status));
    if (U_FAILURE(status)) {
        return NULL;
    }

    ScopedResourceBundle currencyElem(ures_getByIndex(currency.get(), 0, NULL, &status));
    if (U_FAILURE(status)) {
        return env->NewStringUTF("XXX");
    }

    // Check if there's a 'to' date. If there is, the currency isn't used anymore.
    ScopedResourceBundle currencyTo(ures_getByKey(currencyElem.get(), "to", NULL, &status));
    if (!U_FAILURE(status)) {
        return NULL;
    }
    // Ignore the failure to find a 'to' date.
    status = U_ZERO_ERROR;

    ScopedResourceBundle currencyId(ures_getByKey(currencyElem.get(), "id", NULL, &status));
    if (U_FAILURE(status)) {
        // No id defined for this country
        return env->NewStringUTF("XXX");
    }

    int32_t charCount;
    const UChar* chars = ures_getString(currencyId.get(), &charCount, &status);
    return (charCount == 0) ? env->NewStringUTF("XXX") : jniCreateString(env, chars, charCount);
}

static jstring ICU_getISO3Country(JNIEnv* env, jclass, jstring javaLanguageTag) {
  ScopedIcuULoc icuLocale(env, javaLanguageTag);
  if (!icuLocale.valid()) {
    return NULL;
  }
  return env->NewStringUTF(uloc_getISO3Country(icuLocale.locale()));
}

static jstring ICU_getISO3Language(JNIEnv* env, jclass, jstring javaLanguageTag) {
  ScopedIcuULoc icuLocale(env, javaLanguageTag);
  if (!icuLocale.valid()) {
    return NULL;
  }
  return env->NewStringUTF(uloc_getISO3Language(icuLocale.locale()));
}

static jobjectArray ICU_getISOCountriesNative(JNIEnv* env, jclass) {
    return toStringArray(env, uloc_getISOCountries());
}

static jobjectArray ICU_getISOLanguagesNative(JNIEnv* env, jclass) {
    return toStringArray(env, uloc_getISOLanguages());
}

static jobjectArray ICU_getAvailableLocalesNative(JNIEnv* env, jclass) {
    return toStringArray(env, uloc_countAvailable, uloc_getAvailable);
}

static jstring ICU_getBestDateTimePatternNative(JNIEnv* env, jclass, jstring javaSkeleton, jstring javaLanguageTag) {
  ScopedIcuULoc icuLocale(env, javaLanguageTag);
  if (!icuLocale.valid()) {
    return NULL;
  }

  UErrorCode status = U_ZERO_ERROR;
  std::unique_ptr<UDateTimePatternGenerator, decltype(&udatpg_close)> generator(
    udatpg_open(icuLocale.locale(), &status), &udatpg_close);
  if (maybeThrowIcuException(env, "udatpg_open", status)) {
    return NULL;
  }

  const ScopedStringChars skeletonHolder(env, javaSkeleton);
  // Convert jchar* to UChar* with the inline-able utility provided by char16ptr.h
  // which prevents certain compiler optimization than reinterpret_cast.
  icu::ConstChar16Ptr skeletonPtr(skeletonHolder.get());
  const UChar* skeleton = icu::toUCharPtr(skeletonPtr.get());

  int32_t patternLength;
  // Try with fixed-size buffer. 128 chars should be enough for most patterns.
  // If the buffer is not sufficient, run the below case of U_BUFFER_OVERFLOW_ERROR.
  #define PATTERN_BUFFER_SIZE 128
  {
    UChar buffer[PATTERN_BUFFER_SIZE];
    status = U_ZERO_ERROR;
    patternLength = udatpg_getBestPattern(generator.get(), skeleton,
      skeletonHolder.size(), buffer, PATTERN_BUFFER_SIZE, &status);
    if (U_SUCCESS(status)) {
      return jniCreateString(env, buffer, patternLength);
    } else if (status != U_BUFFER_OVERFLOW_ERROR) {
      maybeThrowIcuException(env, "udatpg_getBestPattern", status);
      return NULL;
    }
  }
  #undef PATTERN_BUFFER_SIZE

  // Case U_BUFFER_OVERFLOW_ERROR
  std::unique_ptr<UChar[]> buffer(new UChar[patternLength+1]);
  status = U_ZERO_ERROR;
  patternLength = udatpg_getBestPattern(generator.get(), skeleton,
      skeletonHolder.size(), buffer.get(), patternLength+1, &status);
  if (maybeThrowIcuException(env, "udatpg_getBestPattern", status)) {
    return NULL;
  }

  return jniCreateString(env, buffer.get(), patternLength);
}

static void ICU_setDefaultLocale(JNIEnv* env, jclass, jstring javaLanguageTag) {
  ScopedIcuULoc icuLocale(env, javaLanguageTag);
  if (!icuLocale.valid()) {
    return;
  }

  UErrorCode status = U_ZERO_ERROR;
  uloc_setDefault(icuLocale.locale(), &status);
  maybeThrowIcuException(env, "uloc_setDefault", status);
}

static jstring ICU_getDefaultLocale(JNIEnv* env, jclass) {
  return env->NewStringUTF(uloc_getDefault());
}

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(ICU, getAvailableLocalesNative, "()[Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getBestDateTimePatternNative, "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getCurrencyCode, "(Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getDefaultLocale, "()Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getISO3Country, "(Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getISO3Language, "(Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getISOCountriesNative, "()[Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getISOLanguagesNative, "()[Ljava/lang/String;"),
    NATIVE_METHOD(ICU, getScript, "(Ljava/lang/String;)Ljava/lang/String;"),
    NATIVE_METHOD(ICU, setDefaultLocale, "(Ljava/lang/String;)V"),
};

//
// Global initialization & Teardown for ICU Setup
//   - Contains handlers for JNI_OnLoad and JNI_OnUnload
//

// Init ICU, configuring it and loading the data files.
void register_libcore_icu_ICU(JNIEnv* env) {
  androidicuinit::IcuRegistration::Register();

  jniRegisterNativeMethods(env, "libcore/icu/ICU", gMethods, NELEM(gMethods));
}

// De-init ICU, unloading the data files. Do the opposite of the above function.
void unregister_libcore_icu_ICU() {
  // Skip unregistering JNI methods explicitly, class unloading takes care of
  // it.

  androidicuinit::IcuRegistration::Deregister();
}
