/*
 * Copyright (C) 2015 The Android Open Source Project
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

#include <dlfcn.h>
#include <ios>
#include <sstream>

#include <nativehelper/JNIHelp.h>
#include <nativehelper/jni_macros.h>

typedef void (*FreeFunction)(void*);

static void NativeAllocationRegistry_applyFreeFunction(JNIEnv*,
                                                       jclass,
                                                       jlong freeFunction,
                                                       jlong ptr) {
    void* nativePtr = reinterpret_cast<void*>(static_cast<uintptr_t>(ptr));
    FreeFunction nativeFreeFunction
        = reinterpret_cast<FreeFunction>(static_cast<uintptr_t>(freeFunction));
    nativeFreeFunction(nativePtr);
}

static jstring NativeAllocationRegistry_dlAddr(JNIEnv* env, jclass, jlong freeFunction) {
    uintptr_t ff = static_cast<uintptr_t>(freeFunction);
    Dl_info info;
    int ret = dladdr(reinterpret_cast<void*>(ff), &info);
    std::stringstream result;
    if (ret == 0 /* failed */ || info.dli_fname == nullptr || strlen(info.dli_fname) == 0) {
      result << "0x" << std::hex << ff;
    } else if (info.dli_sname == nullptr) {
      result << info.dli_fname << "+" << (ff - reinterpret_cast<uintptr_t>(info.dli_fbase));
    } else {
      result << info.dli_sname << "+" << (ff - reinterpret_cast<uintptr_t>(info.dli_saddr));
    }
    return env->NewStringUTF(result.str().c_str());
}


static const JNINativeMethod gMethods[] = {
    NATIVE_METHOD(NativeAllocationRegistry, applyFreeFunction, "(JJ)V"),
    NATIVE_METHOD(NativeAllocationRegistry, dlAddr, "(J)Ljava/lang/String;"),
};

void register_libcore_util_NativeAllocationRegistry(JNIEnv* env) {
    jniRegisterNativeMethods(env, "libcore/util/NativeAllocationRegistry",
        gMethods, NELEM(gMethods));
}
