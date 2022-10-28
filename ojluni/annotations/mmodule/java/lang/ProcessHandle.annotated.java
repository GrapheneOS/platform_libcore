/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;

@libcore.api.Hide
@SuppressWarnings({"unchecked", "deprecation", "all"})
public interface ProcessHandle extends java.lang.Comparable<java.lang.ProcessHandle> {

public long pid();

public static java.util.Optional<java.lang.ProcessHandle> of(long pid) { throw new RuntimeException("Stub!"); }

public static java.lang.ProcessHandle current() { throw new RuntimeException("Stub!"); }

public java.util.Optional<java.lang.ProcessHandle> parent();

public java.util.stream.Stream<java.lang.ProcessHandle> children();

public java.util.stream.Stream<java.lang.ProcessHandle> descendants();

public static java.util.stream.Stream<java.lang.ProcessHandle> allProcesses() { throw new RuntimeException("Stub!"); }

public java.lang.ProcessHandle.Info info();

public java.util.concurrent.CompletableFuture<java.lang.ProcessHandle> onExit();

public boolean supportsNormalTermination();

public boolean destroy();

public boolean destroyForcibly();

public boolean isAlive();

public int hashCode();

public boolean equals(java.lang.Object other);

public int compareTo(java.lang.ProcessHandle other);
@SuppressWarnings({"unchecked", "deprecation", "all"})
public static interface Info {

public java.util.Optional<java.lang.String> command();

public java.util.Optional<java.lang.String> commandLine();

public java.util.Optional<java.lang.String[]> arguments();

public java.util.Optional<java.time.Instant> startInstant();

public java.util.Optional<java.time.Duration> totalCpuDuration();

public java.util.Optional<java.lang.String> user();
}

}

