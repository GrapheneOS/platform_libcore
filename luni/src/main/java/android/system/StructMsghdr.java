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

package android.system;

import libcore.util.NonNull;
import libcore.util.Nullable;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * Corresponds to C's {@code struct msghdr}
 *
 */
public final class StructMsghdr{
    /**
     * Optional address.
     * <p>Sendmsg: Caller must populate to specify the target address for a datagram, or pass
     * {@code null} to send to the destination of an already-connected socket.
     * Recvmsg: Populated by the system to specify the source address.
     */
    @Nullable public SocketAddress msg_name;

    /** Scatter/gather array */
    @NonNull public final ByteBuffer[] msg_iov;

    /** Ancillary data */
    @Nullable public StructCmsghdr[] msg_control;

    /** Flags on received message. */
    public int msg_flags;

    /**
     * Constructs an instance with the given field values
     */
    public StructMsghdr(@Nullable SocketAddress msg_name, @NonNull ByteBuffer[] msg_iov,
                        @Nullable StructCmsghdr[] msg_control, int msg_flags) {
        this.msg_name = msg_name;
        this.msg_iov = msg_iov;
        this.msg_control = msg_control;
        this.msg_flags = msg_flags;
    }
}
