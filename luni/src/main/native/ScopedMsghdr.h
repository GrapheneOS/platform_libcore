/*
 * Copyright (c) 2021, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *    *Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *    *Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *    *Neither the name of The Linux Foundation nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

#pragma once

#include <nativehelper/JNIHelp.h>

#include "JniConstants.h"
#include "ScopedBytes.h"

/**
 * Wrapper for managing the name and length of msghdr.
 */
class ScopedMsghdr {
public:
    ScopedMsghdr() {
    }

    ~ScopedMsghdr() {
        if (mMsghdrValue.msg_iov)
            free(mMsghdrValue.msg_iov);
        if (mMsghdrValue.msg_control)
            free(mMsghdrValue.msg_control);
    }

    struct msghdr& getObject() {
        return mMsghdrValue;
    }

    void setMsgNameAndLen(sockaddr* ss, socklen_t sa_len) {
        mMsghdrValue.msg_name = ss;
        mMsghdrValue.msg_namelen = sa_len;
    }

    bool isNameLenValid() const {
        if(mMsghdrValue.msg_name == NULL) {
            return false;
        }
        if ((mMsghdrValue.msg_namelen != sizeof(sockaddr_in6)) &&
            (mMsghdrValue.msg_namelen != sizeof(sockaddr_in))) {
            return false;
        }
        return true;
    }

private:
    struct msghdr mMsghdrValue = {};

};


