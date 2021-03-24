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

#include <nativehelper/ScopedLocalRef.h>
#include "ScopedBytes.h"

/**
 * ScopedByteBufferArray manages the dynamic buffer array of ScopedBytesRW/ScopedBytesRO.
 */
class ScopedByteBufferArray {
public:
    ScopedByteBufferArray(JNIEnv* env, int isRW)
    : mEnv(env), mIsRW(isRW)
    {
        mArrayPtr = NULL;
        mArraySize = 0;
    }

    ~ScopedByteBufferArray() {
        if(!mArrayPtr) {
            return;
        }

        // Loop over arrary and release memory.
        for (int i = 0; i < mArraySize; ++i) {
            if (!mArrayPtr[i])
                continue;

            if (mIsRW) {
                jobject tmp = ((ScopedBytesRW*)mArrayPtr[i])->getObject();
                delete (ScopedBytesRW*)mArrayPtr[i];
                mEnv->DeleteLocalRef(tmp);
            } else {
                jobject tmp = ((ScopedBytesRO*)mArrayPtr[i])->getObject();
                delete (ScopedBytesRO*)mArrayPtr[i];
                mEnv->DeleteLocalRef(tmp);
            }
        }
        delete[] mArrayPtr;
    }

    bool initArray(int size) {
        if (mArrayPtr) {
            return false;
        }

        mArraySize = size;

        if (mIsRW) {
            mArrayPtr = (void**)(new ScopedBytesRW*[size]);
        }
        else {
            mArrayPtr = (void**)(new ScopedBytesRO*[size]);
        }

        if (!mArrayPtr) {
            return false;
        }

        for (int i=0; i<size; ++i) {
            mArrayPtr[i] = 0;
        }

        return true;
    }

    bool isRW() const {
        return mIsRW;
    }

    bool setArrayItem(int itemNo, void* item) {
        if (itemNo >= mArraySize || itemNo < 0) {
            return false;
        }
        mArrayPtr[itemNo] = item;
        return true;
    }

private:
    JNIEnv* const mEnv;
    int mIsRW;
    int mArraySize;
    void** mArrayPtr;
};
