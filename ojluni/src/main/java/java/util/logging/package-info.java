/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Provides the classes and interfaces of
 * the Java 2 platform's core logging facilities.
 * The central goal of the logging APIs is to support maintaining and servicing
 * software at customer sites.
 *
 * <P>
 * There are four main target uses of the logs:
 * </P>
 *
 * <OL>
 *    <LI> <I>Problem diagnosis by end users and system administrators</I>.
 *           This consists of simple logging of common problems that can be fixed
 *           or tracked locally, such as running out of resources, security failures,
 *           and simple configuration errors.
 *
 *    <LI> <I>Problem diagnosis by field service engineers</I>. The logging information
 *            used by field service engineers may be considerably more complex and
 *            verbose than that required by system administrators.  Typically such information
 *            will require extra logging within particular subsystems.
 *
 *    <LI> <I>Problem diagnosis by the development organization</I>.
 *          When a problem occurs in the field, it may be necessary to return the captured logging
 *          information to the original development team for diagnosis. This logging
 *          information may be extremely detailed and fairly inscrutable. Such information might include
 *          detailed tracing on the internal execution of particular subsystems.
 *
 *    <LI> <I>Problem diagnosis by developers</I>. The Logging APIs may also be
 *            used to help debug an application under development. This may
 *            include logging information generated by the target application
 *            as well as logging information generated by lower-level libraries.
 *            Note however that while this use is perfectly reasonable,
 *            the logging APIs are not intended to replace the normal debugging
 *            and profiling tools that may already exist in the development environment.
 * </OL>
 *
 * <p>
 * The key elements of this package include:
 * <UL>
 *    <LI> <I>Logger</I>: The main entity on which applications make
 *                 logging calls. A Logger object is used to log messages
 *                 for a specific system or application
 *                 component.
 *    <LI> <I>LogRecord</I>: Used to pass logging requests between the logging
 *                    framework and individual log handlers.
 *    <LI> <I>Handler</I>: Exports LogRecord objects to a variety of destinations
 *                  including memory, output streams, consoles, files, and sockets.
 *                  A variety of Handler subclasses exist for this purpose. Additional Handlers
 *                  may be developed by third parties and delivered on top of the core platform.
 *    <LI> <I>Level</I>: Defines a set of standard logging levels that can be used
 *                       to control logging output. Programs can be configured to output logging
 *                       for some levels while ignoring output for others.
 *    <LI> <I>Filter</I>: Provides fine-grained control over what gets logged,
 *                        beyond the control provided by log levels. The logging APIs support a general-purpose
 *                        filter mechanism that allows application code to attach arbitrary filters to
 *                        control logging output.
 *
 *    <LI> <I>Formatter</I>: Provides support for formatting LogRecord objects. This
 *                           package includes two formatters, SimpleFormatter and
 *                           XMLFormatter, for formatting log records in plain text
 *                           or XML respectively. As with Handlers, additional Formatters
 *                           may be developed by third parties.
 * </UL>
 * <P>
 * The Logging APIs offer both static and dynamic configuration control.
 * Static control enables field service staff to set up a particular configuration and then re-launch the
 * application with the new logging settings. Dynamic control allows for updates to the
 * logging configuration within a currently running program. The APIs also allow for logging to be
 * enabled or disabled for different functional areas of the system. For example,
 * a field service engineer might be interested in tracing all AWT events, but might have no interest in
 * socket events or memory management.
 * </P>
 *
 * <h2>Null Pointers</h2>
 * <p>
 * In general, unless otherwise noted in the javadoc, methods and
 * constructors will throw NullPointerException if passed a null argument.
 * The one broad exception to this rule is that the logging convenience
 * methods in the Logger class (the config, entering, exiting, fine, finer, finest,
 * log, logp, logrb, severe, throwing, and warning methods)
 * will accept null values
 * for all arguments except for the initial Level argument (if any).
 *
 * <H2>Related Documentation</H2>
 * <P>
 * For an overview of control flow,
 * please refer to the
 * <a href="https://docs.oracle.com/pls/topic/lookup?ctx=javase17&id=logging_overview">Java Logging Overview</a>
 * </P>
 *
 * @since 1.4
 */
package java.util.logging;
