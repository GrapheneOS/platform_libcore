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

#include <aicu/AIcu.h>

#include <log/log.h>
#include <nativehelper/JNIHelp.h>
#include <nativehelper/ScopedUtfChars.h>
#include <nativehelper/jni_macros.h>
#include <nativehelper/toStringArray.h>

#include "IcuUtilities.h"
#include "JniException.h"
#include "ScopedIcuULoc.h"
#include "unicode/char16ptr.h"
#include "unicode/uloc.h"
#include "unicode/ustring.h"

#define U_ICUDATA_CURR U_ICUDATA_NAME "-" "curr"

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
  AIcu_register();

  jniRegisterNativeMethods(env, "libcore/icu/ICU", gMethods, NELEM(gMethods));
}

// De-init ICU, unloading the data files. Do the opposite of the above function.
void unregister_libcore_icu_ICU() {
  // Skip unregistering JNI methods explicitly, class unloading takes care of
  // it.
  AIcu_deregister();
}
