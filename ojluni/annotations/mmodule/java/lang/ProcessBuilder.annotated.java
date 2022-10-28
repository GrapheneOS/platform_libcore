/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
public final class ProcessBuilder {

public ProcessBuilder(java.util.List<java.lang.String> command) { throw new RuntimeException("Stub!"); }

public ProcessBuilder(java.lang.String... command) { throw new RuntimeException("Stub!"); }

public java.lang.ProcessBuilder command(java.util.List<java.lang.String> command) { throw new RuntimeException("Stub!"); }

public java.lang.ProcessBuilder command(java.lang.String... command) { throw new RuntimeException("Stub!"); }

public java.util.List<java.lang.String> command() { throw new RuntimeException("Stub!"); }

public java.util.Map<java.lang.String,java.lang.String> environment() { throw new RuntimeException("Stub!"); }

public java.io.File directory() { throw new RuntimeException("Stub!"); }

public java.lang.ProcessBuilder directory(java.io.File directory) { throw new RuntimeException("Stub!"); }

public java.lang.ProcessBuilder redirectInput(java.lang.ProcessBuilder.Redirect source) { throw new RuntimeException("Stub!"); }

public java.lang.ProcessBuilder redirectOutput(java.lang.ProcessBuilder.Redirect destination) { throw new RuntimeException("Stub!"); }

public java.lang.ProcessBuilder redirectError(java.lang.ProcessBuilder.Redirect destination) { throw new RuntimeException("Stub!"); }

public java.lang.ProcessBuilder redirectInput(java.io.File file) { throw new RuntimeException("Stub!"); }

public java.lang.ProcessBuilder redirectOutput(java.io.File file) { throw new RuntimeException("Stub!"); }

public java.lang.ProcessBuilder redirectError(java.io.File file) { throw new RuntimeException("Stub!"); }

public java.lang.ProcessBuilder.Redirect redirectInput() { throw new RuntimeException("Stub!"); }

public java.lang.ProcessBuilder.Redirect redirectOutput() { throw new RuntimeException("Stub!"); }

public java.lang.ProcessBuilder.Redirect redirectError() { throw new RuntimeException("Stub!"); }

public java.lang.ProcessBuilder inheritIO() { throw new RuntimeException("Stub!"); }

public boolean redirectErrorStream() { throw new RuntimeException("Stub!"); }

public java.lang.ProcessBuilder redirectErrorStream(boolean redirectErrorStream) { throw new RuntimeException("Stub!"); }

public java.lang.Process start() throws java.io.IOException { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public static java.util.List<java.lang.Process> startPipeline(java.util.List<java.lang.ProcessBuilder> builders) throws java.io.IOException { throw new RuntimeException("Stub!"); }
@SuppressWarnings({"unchecked", "deprecation", "all"})
public abstract static class Redirect {

private Redirect() { throw new RuntimeException("Stub!"); }

public abstract java.lang.ProcessBuilder.Redirect.Type type();

public java.io.File file() { throw new RuntimeException("Stub!"); }

public static java.lang.ProcessBuilder.Redirect from(java.io.File file) { throw new RuntimeException("Stub!"); }

public static java.lang.ProcessBuilder.Redirect to(java.io.File file) { throw new RuntimeException("Stub!"); }

public static java.lang.ProcessBuilder.Redirect appendTo(java.io.File file) { throw new RuntimeException("Stub!"); }

public boolean equals(java.lang.Object obj) { throw new RuntimeException("Stub!"); }

public int hashCode() { throw new RuntimeException("Stub!"); }

@libcore.api.Hide
public static final java.lang.ProcessBuilder.Redirect DISCARD;
static { DISCARD = null; }

public static final java.lang.ProcessBuilder.Redirect INHERIT;
static { INHERIT = null; }

public static final java.lang.ProcessBuilder.Redirect PIPE;
static { PIPE = null; }
@SuppressWarnings({"unchecked", "deprecation", "all"})
public enum Type {
PIPE,
INHERIT,
READ,
WRITE,
APPEND;
}

}

}

