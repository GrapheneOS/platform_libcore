/*
 * Copyright (C) 2011 The Android Open Source Project
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

#define LOG_TAG "OsConstantsHolder"

#include <errno.h>
#include <fcntl.h>
#include <netdb.h>
#include <netinet/icmp6.h>
#include <netinet/if_ether.h>
#include <netinet/in.h>
#include <netinet/ip_icmp.h>
#include <netinet/tcp.h>
#include <netinet/udp.h>
#include <netpacket/packet.h>
#include <net/if.h>
#include <net/if_arp.h>
#include <poll.h>
#include <signal.h>
#include <stdlib.h>
#include <sys/capability.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/prctl.h>
#include <sys/resource.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/un.h>
#include <sys/wait.h>
#include <sys/xattr.h>
#include <unistd.h>

#include <linux/if_addr.h>
#include <linux/rtnetlink.h>

#include <nativehelper/JNIHelp.h>
#include <nativehelper/jni_macros.h>

#if defined(__GLIBC__)
// MADV_SOFT_OFFLINE is otherwise unavailable from glibc.
#include <asm-generic/mman-common.h>
#endif

#include "Portability.h"

static void initConstant(JNIEnv* env, jclass c, const char* fieldName, int value) {
    jfieldID field = env->GetStaticFieldID(c, fieldName, "I");
    env->SetStaticIntField(c, field, value);
}

static void OsConstantsHolder_initConstants(JNIEnv* env, jclass c) {
    initConstant(env, c, "AI_ADDRCONFIG", AI_ADDRCONFIG);
    initConstant(env, c, "AI_ALL", AI_ALL);
    initConstant(env, c, "AI_CANONNAME", AI_CANONNAME);
    initConstant(env, c, "AI_NUMERICHOST", AI_NUMERICHOST);
    initConstant(env, c, "AI_NUMERICSERV", AI_NUMERICSERV);
    initConstant(env, c, "AI_PASSIVE", AI_PASSIVE);
    initConstant(env, c, "AI_V4MAPPED", AI_V4MAPPED);

    initConstant(env, c, "EAI_AGAIN", EAI_AGAIN);
    initConstant(env, c, "EAI_BADFLAGS", EAI_BADFLAGS);
    initConstant(env, c, "EAI_FAIL", EAI_FAIL);
    initConstant(env, c, "EAI_FAMILY", EAI_FAMILY);
    initConstant(env, c, "EAI_MEMORY", EAI_MEMORY);
    initConstant(env, c, "EAI_NODATA", EAI_NODATA);
    initConstant(env, c, "EAI_NONAME", EAI_NONAME);
    initConstant(env, c, "EAI_OVERFLOW", EAI_OVERFLOW);
    initConstant(env, c, "EAI_SERVICE", EAI_SERVICE);
    initConstant(env, c, "EAI_SOCKTYPE", EAI_SOCKTYPE);
    initConstant(env, c, "EAI_SYSTEM", EAI_SYSTEM);

    initConstant(env, c, "F_GETLK", F_GETLK);
    initConstant(env, c, "F_GETLK64", F_GETLK64);
    initConstant(env, c, "F_SETLK", F_SETLK);
    initConstant(env, c, "F_SETLK64", F_SETLK64);
    initConstant(env, c, "F_SETLKW", F_SETLKW);
    initConstant(env, c, "F_SETLKW64", F_SETLKW64);

    initConstant(env, c, "NI_NAMEREQD", NI_NAMEREQD);
    initConstant(env, c, "NI_NOFQDN", NI_NOFQDN);
    initConstant(env, c, "NI_NUMERICHOST", NI_NUMERICHOST);
    initConstant(env, c, "NI_NUMERICSERV", NI_NUMERICSERV);

    initConstant(env, c, "O_DIRECT", O_DIRECT);
    initConstant(env, c, "O_NOFOLLOW", O_NOFOLLOW);

    initConstant(env, c, "SIGRTMIN", SIGRTMIN);

    initConstant(env, c, "_SC_2_CHAR_TERM", _SC_2_CHAR_TERM);
    initConstant(env, c, "_SC_2_C_BIND", _SC_2_C_BIND);
    initConstant(env, c, "_SC_2_C_DEV", _SC_2_C_DEV);
    initConstant(env, c, "_SC_2_C_VERSION", _SC_2_C_VERSION);
    initConstant(env, c, "_SC_2_FORT_DEV", _SC_2_FORT_DEV);
    initConstant(env, c, "_SC_2_FORT_RUN", _SC_2_FORT_RUN);
    initConstant(env, c, "_SC_2_LOCALEDEF", _SC_2_LOCALEDEF);
    initConstant(env, c, "_SC_2_SW_DEV", _SC_2_SW_DEV);
    initConstant(env, c, "_SC_2_UPE", _SC_2_UPE);
    initConstant(env, c, "_SC_2_VERSION", _SC_2_VERSION);
    initConstant(env, c, "_SC_AIO_LISTIO_MAX", _SC_AIO_LISTIO_MAX);
    initConstant(env, c, "_SC_AIO_MAX", _SC_AIO_MAX);
    initConstant(env, c, "_SC_AIO_PRIO_DELTA_MAX", _SC_AIO_PRIO_DELTA_MAX);
    initConstant(env, c, "_SC_ARG_MAX", _SC_ARG_MAX);
    initConstant(env, c, "_SC_ASYNCHRONOUS_IO", _SC_ASYNCHRONOUS_IO);
    initConstant(env, c, "_SC_ATEXIT_MAX", _SC_ATEXIT_MAX);
    initConstant(env, c, "_SC_AVPHYS_PAGES", _SC_AVPHYS_PAGES);
    initConstant(env, c, "_SC_BC_BASE_MAX", _SC_BC_BASE_MAX);
    initConstant(env, c, "_SC_BC_DIM_MAX", _SC_BC_DIM_MAX);
    initConstant(env, c, "_SC_BC_SCALE_MAX", _SC_BC_SCALE_MAX);
    initConstant(env, c, "_SC_BC_STRING_MAX", _SC_BC_STRING_MAX);
    initConstant(env, c, "_SC_CHILD_MAX", _SC_CHILD_MAX);
    initConstant(env, c, "_SC_CLK_TCK", _SC_CLK_TCK);
    initConstant(env, c, "_SC_COLL_WEIGHTS_MAX", _SC_COLL_WEIGHTS_MAX);
    initConstant(env, c, "_SC_DELAYTIMER_MAX", _SC_DELAYTIMER_MAX);
    initConstant(env, c, "_SC_EXPR_NEST_MAX", _SC_EXPR_NEST_MAX);
    initConstant(env, c, "_SC_FSYNC", _SC_FSYNC);
    initConstant(env, c, "_SC_GETGR_R_SIZE_MAX", _SC_GETGR_R_SIZE_MAX);
    initConstant(env, c, "_SC_GETPW_R_SIZE_MAX", _SC_GETPW_R_SIZE_MAX);
    initConstant(env, c, "_SC_IOV_MAX", _SC_IOV_MAX);
    initConstant(env, c, "_SC_JOB_CONTROL", _SC_JOB_CONTROL);
    initConstant(env, c, "_SC_LINE_MAX", _SC_LINE_MAX);
    initConstant(env, c, "_SC_LOGIN_NAME_MAX", _SC_LOGIN_NAME_MAX);
    initConstant(env, c, "_SC_MAPPED_FILES", _SC_MAPPED_FILES);
    initConstant(env, c, "_SC_MEMLOCK", _SC_MEMLOCK);
    initConstant(env, c, "_SC_MEMLOCK_RANGE", _SC_MEMLOCK_RANGE);
    initConstant(env, c, "_SC_MEMORY_PROTECTION", _SC_MEMORY_PROTECTION);
    initConstant(env, c, "_SC_MESSAGE_PASSING", _SC_MESSAGE_PASSING);
    initConstant(env, c, "_SC_MQ_OPEN_MAX", _SC_MQ_OPEN_MAX);
    initConstant(env, c, "_SC_MQ_PRIO_MAX", _SC_MQ_PRIO_MAX);
    initConstant(env, c, "_SC_NGROUPS_MAX", _SC_NGROUPS_MAX);
    initConstant(env, c, "_SC_NPROCESSORS_CONF", _SC_NPROCESSORS_CONF);
    initConstant(env, c, "_SC_NPROCESSORS_ONLN", _SC_NPROCESSORS_ONLN);
    initConstant(env, c, "_SC_OPEN_MAX", _SC_OPEN_MAX);
    initConstant(env, c, "_SC_PAGESIZE", _SC_PAGESIZE);
    initConstant(env, c, "_SC_PAGE_SIZE", _SC_PAGE_SIZE);
    initConstant(env, c, "_SC_PASS_MAX", _SC_PASS_MAX);
    initConstant(env, c, "_SC_PHYS_PAGES", _SC_PHYS_PAGES);
    initConstant(env, c, "_SC_PRIORITIZED_IO", _SC_PRIORITIZED_IO);
    initConstant(env, c, "_SC_PRIORITY_SCHEDULING", _SC_PRIORITY_SCHEDULING);
    initConstant(env, c, "_SC_REALTIME_SIGNALS", _SC_REALTIME_SIGNALS);
    initConstant(env, c, "_SC_RE_DUP_MAX", _SC_RE_DUP_MAX);
    initConstant(env, c, "_SC_RTSIG_MAX", _SC_RTSIG_MAX);
    initConstant(env, c, "_SC_SAVED_IDS", _SC_SAVED_IDS);
    initConstant(env, c, "_SC_SEMAPHORES", _SC_SEMAPHORES);
    initConstant(env, c, "_SC_SEM_NSEMS_MAX", _SC_SEM_NSEMS_MAX);
    initConstant(env, c, "_SC_SEM_VALUE_MAX", _SC_SEM_VALUE_MAX);
    initConstant(env, c, "_SC_SHARED_MEMORY_OBJECTS", _SC_SHARED_MEMORY_OBJECTS);
    initConstant(env, c, "_SC_SIGQUEUE_MAX", _SC_SIGQUEUE_MAX);
    initConstant(env, c, "_SC_STREAM_MAX", _SC_STREAM_MAX);
    initConstant(env, c, "_SC_SYNCHRONIZED_IO", _SC_SYNCHRONIZED_IO);
    initConstant(env, c, "_SC_THREADS", _SC_THREADS);
    initConstant(env, c, "_SC_THREAD_ATTR_STACKADDR", _SC_THREAD_ATTR_STACKADDR);
    initConstant(env, c, "_SC_THREAD_ATTR_STACKSIZE", _SC_THREAD_ATTR_STACKSIZE);
    initConstant(env, c, "_SC_THREAD_DESTRUCTOR_ITERATIONS", _SC_THREAD_DESTRUCTOR_ITERATIONS);
    initConstant(env, c, "_SC_THREAD_KEYS_MAX", _SC_THREAD_KEYS_MAX);
    initConstant(env, c, "_SC_THREAD_PRIORITY_SCHEDULING", _SC_THREAD_PRIORITY_SCHEDULING);
    initConstant(env, c, "_SC_THREAD_PRIO_INHERIT", _SC_THREAD_PRIO_INHERIT);
    initConstant(env, c, "_SC_THREAD_PRIO_PROTECT", _SC_THREAD_PRIO_PROTECT);
    initConstant(env, c, "_SC_THREAD_SAFE_FUNCTIONS", _SC_THREAD_SAFE_FUNCTIONS);
    initConstant(env, c, "_SC_THREAD_STACK_MIN", _SC_THREAD_STACK_MIN);
    initConstant(env, c, "_SC_THREAD_THREADS_MAX", _SC_THREAD_THREADS_MAX);
    initConstant(env, c, "_SC_TIMERS", _SC_TIMERS);
    initConstant(env, c, "_SC_TIMER_MAX", _SC_TIMER_MAX);
    initConstant(env, c, "_SC_TTY_NAME_MAX", _SC_TTY_NAME_MAX);
    initConstant(env, c, "_SC_TZNAME_MAX", _SC_TZNAME_MAX);
    initConstant(env, c, "_SC_VERSION", _SC_VERSION);
    initConstant(env, c, "_SC_XBS5_ILP32_OFF32", _SC_XBS5_ILP32_OFF32);
    initConstant(env, c, "_SC_XBS5_ILP32_OFFBIG", _SC_XBS5_ILP32_OFFBIG);
    initConstant(env, c, "_SC_XBS5_LP64_OFF64", _SC_XBS5_LP64_OFF64);
    initConstant(env, c, "_SC_XBS5_LPBIG_OFFBIG", _SC_XBS5_LPBIG_OFFBIG);
    initConstant(env, c, "_SC_XOPEN_CRYPT", _SC_XOPEN_CRYPT);
    initConstant(env, c, "_SC_XOPEN_ENH_I18N", _SC_XOPEN_ENH_I18N);
    initConstant(env, c, "_SC_XOPEN_LEGACY", _SC_XOPEN_LEGACY);
    initConstant(env, c, "_SC_XOPEN_REALTIME", _SC_XOPEN_REALTIME);
    initConstant(env, c, "_SC_XOPEN_REALTIME_THREADS", _SC_XOPEN_REALTIME_THREADS);
    initConstant(env, c, "_SC_XOPEN_SHM", _SC_XOPEN_SHM);
    initConstant(env, c, "_SC_XOPEN_UNIX", _SC_XOPEN_UNIX);
    initConstant(env, c, "_SC_XOPEN_VERSION", _SC_XOPEN_VERSION);
    initConstant(env, c, "_SC_XOPEN_XCU_VERSION", _SC_XOPEN_XCU_VERSION);
}

static const JNINativeMethod gMethods[] = {
    NATIVE_METHOD(OsConstantsHolder, initConstants, "()V"),
};
void register_android_system_OsConstantsHolder(JNIEnv* env) {
    jniRegisterNativeMethods(env, "android/system/OsConstantsHolder", gMethods, NELEM(gMethods));
}
