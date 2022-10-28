/*
 * Copyright (c) 1995, 2017, Oracle and/or its affiliates. All rights reserved.
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

@SuppressWarnings({"unchecked", "deprecation", "all"})
public abstract class Process {

public Process() { throw new RuntimeException("Stub!"); }

public abstract java.io.OutputStream getOutputStream();

public abstract java.io.InputStream getInputStream();

public abstract java.io.InputStream getErrorStream();

public abstract int waitFor() throws java.lang.InterruptedException;

public boolean waitFor(long timeout, java.util.concurrent.TimeUnit unit) throws java.lang.InterruptedException { throw new RuntimeException("Stub!"); }

public abstract int exitValue();

public abstract void destroy();

public java.lang.Process destroyForcibly() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public boolean supportsNormalTermination() { throw new RuntimeException("Stub!"); }

public boolean isAlive() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public long pid() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public java.util.concurrent.CompletableFuture<java.lang.Process> onExit() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public java.lang.ProcessHandle toHandle() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public java.lang.ProcessHandle.Info info() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public java.util.stream.Stream<java.lang.ProcessHandle> children() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public java.util.stream.Stream<java.lang.ProcessHandle> descendants() { throw new RuntimeException("Stub!"); }
}

