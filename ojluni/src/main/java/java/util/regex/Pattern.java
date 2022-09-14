/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.util.regex;

import com.android.icu.util.regex.PatternNative;
import dalvik.system.VMRuntime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import libcore.util.EmptyArray;

// Android-changed: Document that named capturing is only available from API 26.
// Android-changed: Android always uses unicode character classes.
// Android-changed: Remove reference to Character.codePointOf(String) until it's implemented.
// Android-changed: UNICODE_CHARACTER_CLASS causes IllegalArgumentException on Android.
// Android-changed: POSIX character classes are Unicode-aware.
// Android-changed: Throw PatternSyntaxException for non-existent back references.
/**
 * A compiled representation of a regular expression.
 *
 * <p> A regular expression, specified as a string, must first be compiled into
 * an instance of this class.  The resulting pattern can then be used to create
 * a {@link Matcher} object that can match arbitrary {@linkplain
 * java.lang.CharSequence character sequences} against the regular
 * expression.  All of the state involved in performing a match resides in the
 * matcher, so many matchers can share the same pattern.
 *
 * <p> A typical invocation sequence is thus
 *
 * <blockquote><pre>
 * Pattern p = Pattern.{@link #compile compile}("a*b");
 * Matcher m = p.{@link #matcher matcher}("aaaaab");
 * boolean b = m.{@link Matcher#matches matches}();</pre></blockquote>
 *
 * <p> A {@link #matches matches} method is defined by this class as a
 * convenience for when a regular expression is used just once.  This method
 * compiles an expression and matches an input sequence against it in a single
 * invocation.  The statement
 *
 * <blockquote><pre>
 * boolean b = Pattern.matches("a*b", "aaaaab");</pre></blockquote>
 *
 * is equivalent to the three statements above, though for repeated matches it
 * is less efficient since it does not allow the compiled pattern to be reused.
 *
 * <p> Instances of this class are immutable and are safe for use by multiple
 * concurrent threads.  Instances of the {@link Matcher} class are not safe for
 * such use.
 *
 *
 * <h3><a id="sum">Summary of regular-expression constructs</a></h3>
 *
 * <table class="borderless">
 * <caption style="display:none">Regular expression constructs, and what they match</caption>
 * <thead style="text-align:left">
 * <tr>
 * <th id="construct">Construct</th>
 * <th id="matches">Matches</th>
 * </tr>
 * </thead>
 * <tbody style="text-align:left">
 *
 * <tr><th colspan="2" style="padding-top:20px" id="characters">Characters</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight: normal" id="x"><i>x</i></th>
 *     <td headers="matches characters x">The character <i>x</i></td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="backslash">{@code \\}</th>
 *     <td headers="matches characters backslash">The backslash character</td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="octal_n">{@code \0}<i>n</i></th>
 *     <td headers="matches characters octal_n">The character with octal value {@code 0}<i>n</i>
 *         (0&nbsp;{@code <=}&nbsp;<i>n</i>&nbsp;{@code <=}&nbsp;7)</td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="octal_nn">{@code \0}<i>nn</i></th>
 *     <td headers="matches characters octal_nn">The character with octal value {@code 0}<i>nn</i>
 *         (0&nbsp;{@code <=}&nbsp;<i>n</i>&nbsp;{@code <=}&nbsp;7)</td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="octal_nnn">{@code \0}<i>mnn</i></th>
 *     <td headers="matches characters octal_nnn">The character with octal value {@code 0}<i>mnn</i>
 *         (0&nbsp;{@code <=}&nbsp;<i>m</i>&nbsp;{@code <=}&nbsp;3,
 *         0&nbsp;{@code <=}&nbsp;<i>n</i>&nbsp;{@code <=}&nbsp;7)</td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="hex_hh">{@code \x}<i>hh</i></th>
 *     <td headers="matches characters hex_hh">The character with hexadecimal value {@code 0x}<i>hh</i></td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="hex_hhhh"><code>&#92;u</code><i>hhhh</i></th>
 *     <td headers="matches characters hex_hhhh">The character with hexadecimal&nbsp;value&nbsp;{@code 0x}<i>hhhh</i></td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="hex_h_h"><code>&#92;x</code><i>{h...h}</i></th>
 *     <td headers="matches characters hex_h_h">The character with hexadecimal value {@code 0x}<i>h...h</i>
 *         ({@link java.lang.Character#MIN_CODE_POINT Character.MIN_CODE_POINT}
 *         &nbsp;&lt;=&nbsp;{@code 0x}<i>h...h</i>&nbsp;&lt;=&nbsp;
 *          {@link java.lang.Character#MAX_CODE_POINT Character.MAX_CODE_POINT})</td></tr>
 * <tr><th style="vertical-align:top; font-weight: normal" id="unicode_name"><code>&#92;N{</code><i>name</i><code>}</code></th>
 *     <td headers="matches characters unicode_name">The character with Unicode character name <i>'name'</i></td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="tab">{@code \t}</th>
 *     <td headers="matches characters tab">The tab character (<code>'&#92;u0009'</code>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="newline">{@code \n}</th>
 *     <td headers="matches characters newline">The newline (line feed) character (<code>'&#92;u000A'</code>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="return">{@code \r}</th>
 *     <td headers="matches characters return">The carriage-return character (<code>'&#92;u000D'</code>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="form_feed">{@code \f}</th>
 *     <td headers="matches characters form_feed">The form-feed character (<code>'&#92;u000C'</code>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="bell">{@code \a}</th>
 *     <td headers="matches characters bell">The alert (bell) character (<code>'&#92;u0007'</code>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="escape">{@code \e}</th>
 *     <td headers="matches characters escape">The escape character (<code>'&#92;u001B'</code>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="ctrl_x">{@code \c}<i>x</i></th>
 *     <td headers="matches characters ctrl_x">The control character corresponding to <i>x</i></td></tr>
 *
 *  <tr><th colspan="2" style="padding-top:20px" id="classes">Character classes</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="simple">{@code [abc]}</th>
 *     <td headers="matches classes simple">{@code a}, {@code b}, or {@code c} (simple class)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="negation">{@code [^abc]}</th>
 *     <td headers="matches classes negation">Any character except {@code a}, {@code b}, or {@code c} (negation)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="range">{@code [a-zA-Z]}</th>
 *     <td headers="matches classes range">{@code a} through {@code z}
 *         or {@code A} through {@code Z}, inclusive (range)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="union">{@code [a-d[m-p]]}</th>
 *     <td headers="matches classes union">{@code a} through {@code d},
 *      or {@code m} through {@code p}: {@code [a-dm-p]} (union)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="intersection">{@code [a-z&&[def]]}</th>
 *     <td headers="matches classes intersection">{@code d}, {@code e}, or {@code f} (intersection)</tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="subtraction1">{@code [a-z&&[^bc]]}</th>
 *     <td headers="matches classes subtraction1">{@code a} through {@code z},
 *         except for {@code b} and {@code c}: {@code [ad-z]} (subtraction)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="subtraction2">{@code [a-z&&[^m-p]]}</th>
 *     <td headers="matches classes subtraction2">{@code a} through {@code z},
 *          and not {@code m} through {@code p}: {@code [a-lq-z]}(subtraction)</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="predef">Predefined character classes</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="any">{@code .}</th>
 *     <td headers="matches predef any">Any character (may or may not match <a href="#lt">line terminators</a>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="digit">{@code \d}</th>
 *     <td headers="matches predef digit">A digit: {@code [0-9]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_digit">{@code \D}</th>
 *     <td headers="matches predef non_digit">A non-digit: {@code [^0-9]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="horiz_white">{@code \h}</th>
 *     <td headers="matches predef horiz_white">A horizontal whitespace character:
 *     <code>[ \t\xA0&#92;u1680&#92;u180e&#92;u2000-&#92;u200a&#92;u202f&#92;u205f&#92;u3000]</code></td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_horiz_white">{@code \H}</th>
 *     <td headers="matches predef non_horiz_white">A non-horizontal whitespace character: {@code [^\h]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="white">{@code \s}</th>
 *     <td headers="matches predef white">A whitespace character: {@code [ \t\n\x0B\f\r]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_white">{@code \S}</th>
 *     <td headers="matches predef non_white">A non-whitespace character: {@code [^\s]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="vert_white">{@code \v}</th>
 *     <td headers="matches predef vert_white">A vertical whitespace character: <code>[\n\x0B\f\r\x85&#92;u2028&#92;u2029]</code>
 *     </td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_vert_white">{@code \V}</th>
 *     <td headers="matches predef non_vert_white">A non-vertical whitespace character: {@code [^\v]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="word">{@code \w}</th>
 *     <td headers="matches predef word">A word character: {@code [a-zA-Z_0-9]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_word">{@code \W}</th>
 *     <td headers="matches predef non_word">A non-word character: {@code [^\w]}</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="posix"><b>POSIX character classes (Unicode-aware)</b></th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="Lower">{@code \p{Lower}}</th>
 *     <td headers="matches posix Lower">A lower-case alphabetic character: {@code \p{Lowercase}}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Upper">{@code \p{Upper}}</th>
 *     <td headers="matches posix Upper">An upper-case alphabetic character:{@code \p{Uppercase}}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="ASCII">{@code \p{ASCII}}</th>
 *     <td headers="matches posix ASCII">All ASCII:{@code [\x00-\x7F]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Alpha">{@code \p{Alpha}}</th>
 *     <td headers="matches posix Alpha">An alphabetic character:{@code [\p{Lower}\p{Upper}]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Digit">{@code \p{Digit}}</th>
 *     <td headers="matches posix Digit">A decimal digit: {@code \p{gc=Decimal_Number}}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Alnum">{@code \p{Alnum}}</th>
 *     <td headers="matches posix Alnum">An alphanumeric character:{@code [\p{Alpha}\p{Digit}]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Punct">{@code \p{Punct}}</th>
 *     <td headers="matches posix Punct">Punctuation: {@code \p{gc=Punctuation}}</td></tr>
 *     <!-- {@code [\!"#\$%&'\(\)\*\+,\-\./:;\<=\>\?@\[\\\]\^_`\{\|\}~]}
 *          {@code [\X21-\X2F\X31-\X40\X5B-\X60\X7B-\X7E]} -->
 * <tr><th style="vertical-align:top; font-weight:normal" id="Graph">{@code \p{Graph}}</th>
 *     <td headers="matches posix Graph">A visible character:
 *     {@code [^p{space}\p{gc=Control}\p{gc=Surrogate}\p{gc=Unassigned}]}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Print">{@code \p{Print}}</th>
 *     <td headers="matches posix Print">A printable character: {@code \p{graph} \p{blank} -- \p{cntrl}}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Blank">{@code \p{Blank}}</th>
 *     <td headers="matches posix Blank">A space or a tab: {@code \p{gc=Space_Separator} \N{CHARACTER TABULATION}}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Cntrl">{@code \p{Cntrl}}</th>
 *     <td headers="matches posix Cntrl">A control character: {@code \p{gc=Control}}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="XDigit">{@code \p{XDigit}}</th>
 *     <td headers="matches posix XDigit">A hexadecimal digit: {@code \p{gc=Decimal_Number}\p{Hex_Digit}}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Space">{@code \p{Space}}</th>
 *     <td headers="matches posix Space">A whitespace character: {@code \p{Whitespace}}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="PosixCompatible">POSIX Compatible</th>
 *     <td headers="matches posix PosixCompatible">See <a href="http://www.unicode.org/reports/tr18/#Compatibility_Properties">Unicode documentation</a></td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="java">java.lang.Character classes (simple <a href="#jcc">java character type</a>)</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="javaLowerCase">{@code \p{javaLowerCase}}</th>
 *     <td headers="matches java javaLowerCase">Equivalent to java.lang.Character.isLowerCase()</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="javaUpperCase">{@code \p{javaUpperCase}}</th>
 *     <td headers="matches java javaUpperCase">Equivalent to java.lang.Character.isUpperCase()</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="javaWhitespace">{@code \p{javaWhitespace}}</th>
 *     <td headers="matches java javaWhitespace">Equivalent to java.lang.Character.isWhitespace()</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="javaMirrored">{@code \p{javaMirrored}}</th>
 *     <td headers="matches java javaMirrored">Equivalent to java.lang.Character.isMirrored()</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px"  id="unicode">Classes for Unicode scripts, blocks, categories and binary properties</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="IsLatin">{@code \p{IsLatin}}</th>
 *     <td headers="matches unicode IsLatin">A Latin&nbsp;script character (<a href="#usc">script</a>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="InGreek">{@code \p{InGreek}}</th>
 *     <td headers="matches unicode InGreek">A character in the Greek&nbsp;block (<a href="#ubc">block</a>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Lu">{@code \p{Lu}}</th>
 *     <td headers="matches unicode Lu">An uppercase letter (<a href="#ucc">category</a>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="IsAlphabetic">{@code \p{IsAlphabetic}}</th>
 *     <td headers="matches unicode IsAlphabetic">An alphabetic character (<a href="#ubpc">binary property</a>)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="Sc">{@code \p{Sc}}</th>
 *     <td headers="matches unicode Sc">A currency symbol</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="not_InGreek">{@code \P{InGreek}}</th>
 *     <td headers="matches unicode not_InGreek">Any character except one in the Greek block (negation)</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="not_uppercase">{@code [\p{L}&&[^\p{Lu}]]}</th>
 *     <td headers="matches unicode not_uppercase">Any letter except an uppercase letter (subtraction)</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="bounds">Boundary matchers</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="begin_line">{@code ^}</th>
 *     <td headers="matches bounds begin_line">The beginning of a line</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="end_line">{@code $}</th>
 *     <td headers="matches bounds end_line">The end of a line</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="word_boundary">{@code \b}</th>
 *     <td headers="matches bounds word_boundary">A word boundary</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="grapheme_cluster_boundary">{@code \b{g}}</th>
 *     <td headers="matches bounds grapheme_cluster_boundary">A Unicode extended grapheme cluster boundary</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_word_boundary">{@code \B}</th>
 *     <td headers="matches bounds non_word_boundary">A non-word boundary</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="begin_input">{@code \A}</th>
 *     <td headers="matches bounds begin_input">The beginning of the input</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="end_prev_match">{@code \G}</th>
 *     <td headers="matches bounds end_prev_match">The end of the previous match</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="end_input_except_term">{@code \Z}</th>
 *     <td headers="matches bounds end_input_except_term">The end of the input but for the final
 *         <a href="#lt">terminator</a>, if&nbsp;any</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="end_input">{@code \z}</th>
 *     <td headers="matches bounds end_input">The end of the input</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="linebreak">Linebreak matcher</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="any_unicode_linebreak">{@code \R}</th>
 *     <td headers="matches linebreak any_unicode_linebreak">Any Unicode linebreak sequence, is equivalent to
 *     <code>&#92;u000D&#92;u000A|[&#92;u000A&#92;u000B&#92;u000C&#92;u000D&#92;u0085&#92;u2028&#92;u2029]
 *     </code></td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="grapheme">Unicode Extended Grapheme matcher</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="grapheme_any">{@code \X}</th>
 *     <td headers="matches grapheme grapheme_any">Any Unicode extended grapheme cluster</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="greedy">Greedy quantifiers</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="greedy_once_or_not"><i>X</i>{@code ?}</th>
 *     <td headers="matches greedy greedy_once_or_not"><i>X</i>, once or not at all</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="greedy_zero_or_more"><i>X</i>{@code *}</th>
 *     <td headers="matches greedy greedy_zero_or_more"><i>X</i>, zero or more times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="greedy_one_or_more"><i>X</i>{@code +}</th>
 *     <td headers="matches greedy greedy_one_or_more"><i>X</i>, one or more times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="greedy_exactly"><i>X</i><code>{</code><i>n</i><code>}</code></th>
 *     <td headers="matches greedy greedy_exactly"><i>X</i>, exactly <i>n</i> times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="greedy_at_least"><i>X</i><code>{</code><i>n</i>{@code ,}}</th>
 *     <td headers="matches greedy greedy_at_least"><i>X</i>, at least <i>n</i> times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="greedy_at_least_up_to"><i>X</i><code>{</code><i>n</i>{@code ,}<i>m</i><code>}</code></th>
 *     <td headers="matches greedy greedy_at_least_up_to"><i>X</i>, at least <i>n</i> but not more than <i>m</i> times</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="reluc">Reluctant quantifiers</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="reluc_once_or_not"><i>X</i>{@code ??}</th>
 *     <td headers="matches reluc reluc_once_or_not"><i>X</i>, once or not at all</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="reluc_zero_or_more"><i>X</i>{@code *?}</th>
 *     <td headers="matches reluc reluc_zero_or_more"><i>X</i>, zero or more times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="reluc_one_or_more"><i>X</i>{@code +?}</th>
 *     <td headers="matches reluc reluc_one_or_more"><i>X</i>, one or more times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="reluc_exactly"><i>X</i><code>{</code><i>n</i><code>}?</code></th>
 *     <td headers="matches reluc reluc_exactly"><i>X</i>, exactly <i>n</i> times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="reluc_at_least"><i>X</i><code>{</code><i>n</i><code>,}?</code></th>
 *     <td headers="matches reluc reluc_at_least"><i>X</i>, at least <i>n</i> times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="reluc_at_least_up_to"><i>X</i><code>{</code><i>n</i>{@code ,}<i>m</i><code>}?</code></th>
 *     <td headers="matches reluc reluc_at_least_up_to"><i>X</i>, at least <i>n</i> but not more than <i>m</i> times</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="poss">Possessive quantifiers</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="poss_once_or_not"><i>X</i>{@code ?+}</th>
 *     <td headers="matches poss poss_once_or_not"><i>X</i>, once or not at all</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="poss_zero_or_more"><i>X</i>{@code *+}</th>
 *     <td headers="matches poss poss_zero_or_more"><i>X</i>, zero or more times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="poss_one_or_more"><i>X</i>{@code ++}</th>
 *     <td headers="matches poss poss_one_or_more"><i>X</i>, one or more times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="poss_exactly"><i>X</i><code>{</code><i>n</i><code>}+</code></th>
 *     <td headers="matches poss poss_exactly"><i>X</i>, exactly <i>n</i> times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="poss_at_least"><i>X</i><code>{</code><i>n</i><code>,}+</code></th>
 *     <td headers="matches poss poss_at_least"><i>X</i>, at least <i>n</i> times</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="poss_at_least_up_to"><i>X</i><code>{</code><i>n</i>{@code ,}<i>m</i><code>}+</code></th>
 *     <td headers="matches poss poss_at_least_up_to"><i>X</i>, at least <i>n</i> but not more than <i>m</i> times</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="logical">Logical operators</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="concat"><i>XY</i></th>
 *     <td headers="matches logical concat"><i>X</i> followed by <i>Y</i></td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="alternate"><i>X</i>{@code |}<i>Y</i></th>
 *     <td headers="matches logical alternate">Either <i>X</i> or <i>Y</i></td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="group">{@code (}<i>X</i>{@code )}</th>
 *     <td headers="matches logical group">X, as a <a href="#cg">capturing group</a></td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="backref">Back references</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="back_nth">{@code \}<i>n</i></th>
 *     <td headers="matches backref back_nth">Whatever the <i>n</i><sup>th</sup>
 *     <a href="#cg">capturing group</a> matched</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="back_named">{@code \}<i>k</i>&lt;<i>name</i>&gt;</th>
 *     <td headers="matches backref back_named">Whatever the
 *     <a href="#groupname">named-capturing group</a> "name" matched. Only available for API 26 or above</td></tr>
 *
 * <tr><th colspan="2" style="padding-top:20px" id="quote">Quotation</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="quote_follow">{@code \}</th>
 *     <td headers="matches quote quote_follow">Nothing, but quotes the following character</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="quote_begin">{@code \Q}</th>
 *     <td headers="matches quote quote_begin">Nothing, but quotes all characters until {@code \E}</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="quote_end">{@code \E}</th>
 *     <td headers="matches quote quote_end">Nothing, but ends quoting started by {@code \Q}</td></tr>
 *     <!-- Metachars: !$()*+.<>?[\]^{|} -->
 *
 * <tr><th colspan="2" style="padding-top:20px" id="special">Special constructs (named-capturing and non-capturing)</th></tr>
 *
 * <tr><th style="vertical-align:top; font-weight:normal" id="named_group"><code>(?&lt;<a href="#groupname">name</a>&gt;</code><i>X</i>{@code )}</th>
 *     <td headers="matches special named_group"><i>X</i>, as a named-capturing group. Only available for API 26 or above.</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_capture_group">{@code (?:}<i>X</i>{@code )}</th>
 *     <td headers="matches special non_capture_group"><i>X</i>, as a non-capturing group</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="flags"><code>(?idmsux-idmsux)&nbsp;</code></th>
 * <a href="#UNIX_LINES">d</a> <a href="#MULTILINE">m</a> <a href="#DOTALL">s</a>
 * <a href="#UNICODE_CASE">u</a> <a href="#COMMENTS">x</a> <a href="#UNICODE_CHARACTER_CLASS">U</a>
 * on - off</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="non_capture_group_flags"><code>(?idmsux-idmsux:</code><i>X</i>{@code )}&nbsp;&nbsp;</th>
 *     <td headers="matches special non_capture_group_flags"><i>X</i>, as a <a href="#cg">non-capturing group</a> with the
 *         given flags <a href="#CASE_INSENSITIVE">i</a> <a href="#UNIX_LINES">d</a>
 * <a href="#MULTILINE">m</a> <a href="#DOTALL">s</a> <a href="#UNICODE_CASE">u</a >
 * <a href="#COMMENTS">x</a> on - off</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="pos_lookahead">{@code (?=}<i>X</i>{@code )}</th>
 *     <td headers="matches special pos_lookahead"><i>X</i>, via zero-width positive lookahead</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="neg_lookahead">{@code (?!}<i>X</i>{@code )}</th>
 *     <td headers="matches special neg_lookahead"><i>X</i>, via zero-width negative lookahead</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="pos_lookbehind">{@code (?<=}<i>X</i>{@code )}</th>
 *     <td headers="matches special pos_lookbehind"><i>X</i>, via zero-width positive lookbehind</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="neg_lookbehind">{@code (?<!}<i>X</i>{@code )}</th>
 *     <td headers="matches special neg_lookbehind"><i>X</i>, via zero-width negative lookbehind</td></tr>
 * <tr><th style="vertical-align:top; font-weight:normal" id="indep_non_capture_group">{@code (?>}<i>X</i>{@code )}</th>
 *     <td headers="matches special indep_non_capture_group"><i>X</i>, as an independent, non-capturing group</td></tr>
 *
 * </tbody>
 * </table>
 *
 * <hr>
 *
 *
 * <h3><a id="bs">Backslashes, escapes, and quoting</a></h3>
 *
 * <p> The backslash character ({@code '\'}) serves to introduce escaped
 * constructs, as defined in the table above, as well as to quote characters
 * that otherwise would be interpreted as unescaped constructs.  Thus the
 * expression {@code \\} matches a single backslash and <code>\{</code> matches a
 * left brace.
 *
 * <p> It is an error to use a backslash prior to any alphabetic character that
 * does not denote an escaped construct; these are reserved for future
 * extensions to the regular-expression language.  A backslash may be used
 * prior to a non-alphabetic character regardless of whether that character is
 * part of an unescaped construct.
 *
 * <p> Backslashes within string literals in Java source code are interpreted
 * as required by
 * <cite>The Java&trade; Language Specification</cite>
 * as either Unicode escapes (section 3.3) or other character escapes (section 3.10.6)
 * It is therefore necessary to double backslashes in string
 * literals that represent regular expressions to protect them from
 * interpretation by the Java bytecode compiler.  The string literal
 * <code>"&#92;b"</code>, for example, matches a single backspace character when
 * interpreted as a regular expression, while {@code "\\b"} matches a
 * word boundary.  The string literal {@code "\(hello\)"} is illegal
 * and leads to a compile-time error; in order to match the string
 * {@code (hello)} the string literal {@code "\\(hello\\)"}
 * must be used.
 *
 * <h3><a id="cc">Character Classes</a></h3>
 *
 *    <p> Character classes may appear within other character classes, and
 *    may be composed by the union operator (implicit) and the intersection
 *    operator ({@code &&}).
 *    The union operator denotes a class that contains every character that is
 *    in at least one of its operand classes.  The intersection operator
 *    denotes a class that contains every character that is in both of its
 *    operand classes.
 *
 *    <p> The precedence of character-class operators is as follows, from
 *    highest to lowest:
 *
 *    <table class="striped" style="margin-left: 2em;">
 *      <caption style="display:none">Precedence of character class operators.</caption>
 *      <thead>
 *      <tr><th scope="col">Precedence<th scope="col">Name<th scope="col">Example
 *      </thead>
 *      <tbody>
 *      <tr><th scope="row">1</th>
 *        <td>Literal escape&nbsp;&nbsp;&nbsp;&nbsp;</td>
 *        <td>{@code \x}</td></tr>
 *     <tr><th scope="row">2</th>
 *        <td>Grouping</td>
 *        <td>{@code [...]}</td></tr>
 *     <tr><th scope="row">3</th>
 *        <td>Range</td>
 *        <td>{@code a-z}</td></tr>
 *      <tr><th scope="row">4</th>
 *        <td>Union</td>
 *        <td>{@code [a-e][i-u]}</td></tr>
 *      <tr><th scope="row">5</th>
 *        <td>Intersection</td>
 *        <td>{@code [a-z&&[aeiou]]}</td></tr>
 *      </tbody>
 *    </table>
 *
 *    <p> Note that a different set of metacharacters are in effect inside
 *    a character class than outside a character class. For instance, the
 *    regular expression {@code .} loses its special meaning inside a
 *    character class, while the expression {@code -} becomes a range
 *    forming metacharacter.
 *
 * <h3><a id="lt">Line terminators</a></h3>
 *
 * <p> A <i>line terminator</i> is a one- or two-character sequence that marks
 * the end of a line of the input character sequence.  The following are
 * recognized as line terminators:
 *
 * <ul>
 *
 *   <li> A newline (line feed) character ({@code '\n'}),
 *
 *   <li> A carriage-return character followed immediately by a newline
 *   character ({@code "\r\n"}),
 *
 *   <li> A standalone carriage-return character ({@code '\r'}),
 *
 *   <li> A next-line character (<code>'&#92;u0085'</code>),
 *
 *   <li> A line-separator character (<code>'&#92;u2028'</code>), or
 *
 *   <li> A paragraph-separator character (<code>'&#92;u2029'</code>).
 *
 * </ul>
 * <p>If {@link #UNIX_LINES} mode is activated, then the only line terminators
 * recognized are newline characters.
 *
 * <p> The regular expression {@code .} matches any character except a line
 * terminator unless the {@link #DOTALL} flag is specified.
 *
 * <p> By default, the regular expressions {@code ^} and {@code $} ignore
 * line terminators and only match at the beginning and the end, respectively,
 * of the entire input sequence. If {@link #MULTILINE} mode is activated then
 * {@code ^} matches at the beginning of input and after any line terminator
 * except at the end of input. When in {@link #MULTILINE} mode {@code $}
 * matches just before a line terminator or the end of the input sequence.
 *
 * <h3><a id="cg">Groups and capturing</a></h3>
 *
 * <h4><a id="gnumber">Group number</a></h4>
 * <p> Capturing groups are numbered by counting their opening parentheses from
 * left to right.  In the expression {@code ((A)(B(C)))}, for example, there
 * are four such groups: </p>
 *
 * <ol style="margin-left:2em;">
 *   <li> {@code ((A)(B(C)))}
 *   <li> {@code (A)}
 *   <li> {@code (B(C))}
 *   <li> {@code (C)}
 * </ol>
 *
 * <p> Group zero always stands for the entire expression.
 *
 * <p> Capturing groups are so named because, during a match, each subsequence
 * of the input sequence that matches such a group is saved.  The captured
 * subsequence may be used later in the expression, via a back reference, and
 * may also be retrieved from the matcher once the match operation is complete.
 *
 * <h4><a id="groupname">Group name</a></h4>
 * <p>The constructs and APIs are available since API level 26. A capturing group
 * can also be assigned a "name", a {@code named-capturing group},
 * and then be back-referenced later by the "name". Group names are composed of
 * the following characters. The first character must be a {@code letter}.
 *
 * <ul>
 *   <li> The uppercase letters {@code 'A'} through {@code 'Z'}
 *        (<code>'&#92;u0041'</code>&nbsp;through&nbsp;<code>'&#92;u005a'</code>),
 *   <li> The lowercase letters {@code 'a'} through {@code 'z'}
 *        (<code>'&#92;u0061'</code>&nbsp;through&nbsp;<code>'&#92;u007a'</code>),
 *   <li> The digits {@code '0'} through {@code '9'}
 *        (<code>'&#92;u0030'</code>&nbsp;through&nbsp;<code>'&#92;u0039'</code>),
 * </ul>
 *
 * <p> A {@code named-capturing group} is still numbered as described in
 * <a href="#gnumber">Group number</a>.
 *
 * <p> The captured input associated with a group is always the subsequence
 * that the group most recently matched.  If a group is evaluated a second time
 * because of quantification then its previously-captured value, if any, will
 * be retained if the second evaluation fails.  Matching the string
 * {@code "aba"} against the expression {@code (a(b)?)+}, for example, leaves
 * group two set to {@code "b"}.  All captured input is discarded at the
 * beginning of each match.
 *
 * <p> Groups beginning with {@code (?} are either pure, <i>non-capturing</i> groups
 * that do not capture text and do not count towards the group total, or
 * <i>named-capturing</i> group.
 *
 * <h3> Unicode support </h3>
 *
 * <p> This class is in conformance with Level 1 of <a
 * href="http://www.unicode.org/reports/tr18/"><i>Unicode Technical
 * Standard #18: Unicode Regular Expression</i></a>, plus RL2.1
 * Canonical Equivalents.
 * <p>
 * <b>Unicode escape sequences</b> such as <code>&#92;u2014</code> in Java source code
 * are processed as described in section 3.3 of
 * <cite>The Java&trade; Language Specification</cite>.
 * Such escape sequences are also implemented directly by the regular-expression
 * parser so that Unicode escapes can be used in expressions that are read from
 * files or from the keyboard.  Thus the strings <code>"&#92;u2014"</code> and
 * {@code "\\u2014"}, while not equal, compile into the same pattern, which
 * matches the character with hexadecimal value {@code 0x2014}.
 * <p>
 * A Unicode character can also be represented by using its <b>Hex notation</b>
 * (hexadecimal code point value) directly as described in construct
 * <code>&#92;x{...}</code>, for example a supplementary character U+2011F can be
 * specified as <code>&#92;x{2011F}</code>, instead of two consecutive Unicode escape
 * sequences of the surrogate pair <code>&#92;uD840</code><code>&#92;uDD1F</code>.
 * <p>
 * <b>Unicode character names</b> are supported by the named character construct
 * <code>\N{</code>...<code>}</code>, for example, <code>\N{WHITE SMILING FACE}</code>
 * specifies character <code>&#92;u263A</code>. The character names supported
 * by this class are the valid Unicode character names matched by
 * {@code java.lang.Character.codePointOf(String) Character.codePointOf(name)}.
 * <p>
 * <a href="http://www.unicode.org/reports/tr18/#Default_Grapheme_Clusters">
 * <b>Unicode extended grapheme clusters</b></a> are supported by the grapheme
 * cluster matcher {@code \X} and the corresponding boundary matcher {@code \b{g}}.
 * <p>
 * Unicode scripts, blocks, categories and binary properties are written with
 * the {@code \p} and {@code \P} constructs as in Perl.
 * <code>\p{</code><i>prop</i><code>}</code> matches if
 * the input has the property <i>prop</i>, while <code>\P{</code><i>prop</i><code>}</code>
 * does not match if the input has that property.
 * <p>
 * Scripts, blocks, categories and binary properties can be used both inside
 * and outside of a character class.
 *
 * <p>
 * <b><a id="usc">Scripts</a></b> are specified either with the prefix {@code Is}, as in
 * {@code IsHiragana}, or by using  the {@code script} keyword (or its short
 * form {@code sc}) as in {@code script=Hiragana} or {@code sc=Hiragana}.
 * <p>
 * The script names supported by {@code Pattern} are the valid script names
 * accepted and defined by
 * {@link java.lang.Character.UnicodeScript#forName(String) UnicodeScript.forName}.
 *
 * <p>
 * <b><a id="ubc">Blocks</a></b> are specified with the prefix {@code In}, as in
 * {@code InMongolian}, or by using the keyword {@code block} (or its short
 * form {@code blk}) as in {@code block=Mongolian} or {@code blk=Mongolian}.
 * <p>
 * The block names supported by {@code Pattern} are the valid block names
 * accepted and defined by
 * {@link java.lang.Character.UnicodeBlock#forName(String) UnicodeBlock.forName}.
 * <p>
 *
 * <b><a id="ucc">Categories</a></b> may be specified with the optional prefix {@code Is}:
 * Both {@code \p{L}} and {@code \p{IsL}} denote the category of Unicode
 * letters. Same as scripts and blocks, categories can also be specified
 * by using the keyword {@code general_category} (or its short form
 * {@code gc}) as in {@code general_category=Lu} or {@code gc=Lu}.
 * <p>
 * The supported categories are those of
 * <a href="http://www.unicode.org/unicode/standard/standard.html">
 * <i>The Unicode Standard</i></a> in the version specified by the
 * {@link java.lang.Character Character} class. The category names are those
 * defined in the Standard, both normative and informative.
 * <p>
 *
 * <b><a id="ubpc">Binary properties</a></b> are specified with the prefix {@code Is}, as in
 * {@code IsAlphabetic}. The supported binary properties by {@code Pattern}
 * are
 * <ul>
 *   <li> Alphabetic
 *   <li> Ideographic
 *   <li> Letter
 *   <li> Lowercase
 *   <li> Uppercase
 *   <li> Titlecase
 *   <li> Punctuation
 *   <Li> Control
 *   <li> White_Space
 *   <li> Digit
 *   <li> Hex_Digit
 *   <li> Join_Control
 *   <li> Noncharacter_Code_Point
 *   <li> Assigned
 * </ul>
 * <p>
 * The following <b>Predefined Character classes</b> and <b>POSIX character classes</b>
 * are in conformance with the recommendation of <i>Annex C: Compatibility Properties</i>
 * of <a href="http://www.unicode.org/reports/tr18/"><i>Unicode Regular Expression
 * </i></a>.
 *
 * <table class="striped">
 * <caption style="display:none">predefined and posix character classes in Unicode mode</caption>
 * <thead>
 * <tr>
 * <th scope="col" id="predef_classes">Classes</th>
 * <th scope="col" id="predef_matches">Matches</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr><th scope="row">{@code \p{Lower}}</th>
 *     <td>A lowercase character:{@code \p{IsLowercase}}</td></tr>
 * <tr><th scope="row">{@code \p{Upper}}</th>
 *     <td>An uppercase character:{@code \p{IsUppercase}}</td></tr>
 * <tr><th scope="row">{@code \p{ASCII}}</th>
 *     <td>All ASCII:{@code [\x00-\x7F]}</td></tr>
 * <tr><th scope="row">{@code \p{Alpha}}</th>
 *     <td>An alphabetic character:{@code \p{IsAlphabetic}}</td></tr>
 * <tr><th scope="row">{@code \p{Digit}}</th>
 *     <td>A decimal digit character:{@code \p{IsDigit}}</td></tr>
 * <tr><th scope="row">{@code \p{Alnum}}</th>
 *     <td>An alphanumeric character:{@code [\p{IsAlphabetic}\p{IsDigit}]}</td></tr>
 * <tr><th scope="row">{@code \p{Punct}}</th>
 *     <td>A punctuation character:{@code \p{IsPunctuation}}</td></tr>
 * <tr><th scope="row">{@code \p{Graph}}</th>
 *     <td>A visible character: {@code [^\p{IsWhite_Space}\p{gc=Cc}\p{gc=Cs}\p{gc=Cn}]}</td></tr>
 * <tr><th scope="row">{@code \p{Print}}</th>
 *     <td>A printable character: {@code [\p{Graph}\p{Blank}&&[^\p{Cntrl}]]}</td></tr>
 * <tr><th scope="row">{@code \p{Blank}}</th>
 *     <td>A space or a tab: {@code [\p{IsWhite_Space}&&[^\p{gc=Zl}\p{gc=Zp}\x0a\x0b\x0c\x0d\x85]]}</td></tr>
 * <tr><th scope="row">{@code \p{Cntrl}}</th>
 *     <td>A control character: {@code \p{gc=Cc}}</td></tr>
 * <tr><th scope="row">{@code \p{XDigit}}</th>
 *     <td>A hexadecimal digit: {@code [\p{gc=Nd}\p{IsHex_Digit}]}</td></tr>
 * <tr><th scope="row">{@code \p{Space}}</th>
 *     <td>A whitespace character:{@code \p{IsWhite_Space}}</td></tr>
 * <tr><th scope="row">{@code \d}</th>
 *     <td>A digit: {@code \p{IsDigit}}</td></tr>
 * <tr><th scope="row">{@code \D}</th>
 *     <td>A non-digit: {@code [^\d]}</td></tr>
 * <tr><th scope="row">{@code \s}</th>
 *     <td>A whitespace character: {@code \p{IsWhite_Space}}</td></tr>
 * <tr><th scope="row">{@code \S}</th>
 *     <td>A non-whitespace character: {@code [^\s]}</td></tr>
 * <tr><th scope="row">{@code \w}</th>
 *     <td>A word character: {@code [\p{Alpha}\p{gc=Mn}\p{gc=Me}\p{gc=Mc}\p{Digit}\p{gc=Pc}\p{IsJoin_Control}]}</td></tr>
 * <tr><th scope="row">{@code \W}</th>
 *     <td>A non-word character: {@code [^\w]}</td></tr>
 * </tbody>
 * </table>
 * <p>
 * <a id="jcc">
 * Categories that behave like the java.lang.Character
 * boolean is<i>methodname</i> methods (except for the deprecated ones) are
 * available through the same <code>\p{</code><i>prop</i><code>}</code> syntax where
 * the specified property has the name <code>java<i>methodname</i></code></a>.
 *
 * <h3> Behavior starting from API level 10 (Android 2.3) </h3>
 *
 * <p> Starting from Android 2.3 Gingerbread, ICU4C becomes the backend of the regular expression
 * implementation. Android could behave differently compared with other regex implementation, e.g.
 * literal right brace ('}') has to be escaped on Android.</p>
 *
 * <p> Some other behavior differences can be found in the
 * <a href="https://unicode-org.github.io/icu/userguide/strings/regexp.html#differences-with-java-regular-expressions">
 * ICU documentation</a>. </p>
 *
 * <h3> Comparison to Perl 5 </h3>
 *
 * <p>The {@code Pattern} engine performs traditional NFA-based matching
 * with ordered alternation as occurs in Perl 5.
 *
 * <p> Perl constructs not supported by this class: </p>
 *
 * <ul>
 *    <li><p> The backreference constructs, <code>\g{</code><i>n</i><code>}</code> for
 *    the <i>n</i><sup>th</sup><a href="#cg">capturing group</a> and
 *    <code>\g{</code><i>name</i><code>}</code> for
 *    <a href="#groupname">named-capturing group</a>.
 *    </p></li>
 *
 *    <li><p> The conditional constructs
 *    {@code (?(}<i>condition</i>{@code )}<i>X</i>{@code )} and
 *    {@code (?(}<i>condition</i>{@code )}<i>X</i>{@code |}<i>Y</i>{@code )},
 *    </p></li>
 *
 *    <li><p> The embedded code constructs <code>(?{</code><i>code</i><code>})</code>
 *    and <code>(??{</code><i>code</i><code>})</code>,</p></li>
 *
 *    <li><p> The embedded comment syntax {@code (?#comment)}, and </p></li>
 *
 *    <li><p> The preprocessing operations {@code \l} <code>&#92;u</code>,
 *    {@code \L}, and {@code \U}.  </p></li>
 *
 * </ul>
 *
 * <p> Constructs supported by this class but not by Perl: </p>
 *
 * <ul>
 *
 *    <li><p> Character-class union and intersection as described
 *    <a href="#cc">above</a>.</p></li>
 *
 * </ul>
 *
 * <p> Notable differences from Perl: </p>
 *
 * <ul>
 *
 *    <li><p> In Perl, {@code \1} through {@code \9} are always interpreted
 *    as back references; a backslash-escaped number greater than {@code 9} is
 *    treated as a back reference if at least that many subexpressions exist,
 *    otherwise it is interpreted, if possible, as an octal escape.  In this
 *    class octal escapes must always begin with a zero. In this class,
 *    {@link #compile(String)} throws {@link PatternSyntaxException} for any
 *    non-existent back references. Please use {@code \Q} and {@code \E} to
 *    quote any digit literals followed by back references.
 *    </p></li>
 *
 *    <li><p> Perl uses the {@code g} flag to request a match that resumes
 *    where the last match left off.  This functionality is provided implicitly
 *    by the {@link Matcher} class: Repeated invocations of the {@link
 *    Matcher#find find} method will resume where the last match left off,
 *    unless the matcher is reset.  </p></li>
 *
 *    <li><p> In Perl, embedded flags at the top level of an expression affect
 *    the whole expression.  In this class, embedded flags always take effect
 *    at the point at which they appear, whether they are at the top level or
 *    within a group; in the latter case, flags are restored at the end of the
 *    group just as in Perl.  </p></li>
 *
 * </ul>
 *
 *
 * <p> For a more precise description of the behavior of regular expression
 * constructs, please see <a href="http://www.oreilly.com/catalog/regex3/">
 * <i>Mastering Regular Expressions, 3nd Edition</i>, Jeffrey E. F. Friedl,
 * O'Reilly and Associates, 2006.</a>
 * </p>
 *
 * @see java.lang.String#split(String, int)
 * @see java.lang.String#split(String)
 *
 * @author      Mike McCloskey
 * @author      Mark Reinhold
 * @author      JSR-51 Expert Group
 * @since       1.4
 * @spec        JSR-51
 */

public final class Pattern
    implements java.io.Serializable
{

    /**
     * Regular expression modifier values.  Instead of being passed as
     * arguments, they can also be passed as inline modifiers.
     * For example, the following statements have the same effect.
     * <pre>
     * Pattern p1 = Pattern.compile("abc", Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
     * Pattern p2 = Pattern.compile("(?im)abc", 0);
     * </pre>
     */

    /**
     * Enables Unix lines mode.
     *
     * <p> In this mode, only the {@code '\n'} line terminator is recognized
     * in the behavior of {@code .}, {@code ^}, and {@code $}.
     *
     * <p> Unix lines mode can also be enabled via the embedded flag
     * expression&nbsp;{@code (?d)}.
     */
    public static final int UNIX_LINES = 0x01;

    // Android-changed: CASE_INSENSITIVE is Unicode-aware on Android.
    /**
     * Enables case-insensitive matching.
     *
     * <p> Case-insensitive matching is Unicode-aware on Android.
     *
     * <p> Case-insensitive matching can also be enabled via the embedded flag
     * expression&nbsp;{@code (?i)}.
     *
     * <p> Specifying this flag may impose a slight performance penalty.  </p>
     */
    public static final int CASE_INSENSITIVE = 0x02;

    /**
     * Permits whitespace and comments in pattern.
     *
     * <p> In this mode, whitespace is ignored, and embedded comments starting
     * with {@code #} are ignored until the end of a line.
     *
     * <p> Comments mode can also be enabled via the embedded flag
     * expression&nbsp;{@code (?x)}.
     */
    public static final int COMMENTS = 0x04;

    /**
     * Enables multiline mode.
     *
     * <p> In multiline mode the expressions {@code ^} and {@code $} match
     * just after or just before, respectively, a line terminator or the end of
     * the input sequence.  By default these expressions only match at the
     * beginning and the end of the entire input sequence.
     *
     * <p> Multiline mode can also be enabled via the embedded flag
     * expression&nbsp;{@code (?m)}.  </p>
     */
    public static final int MULTILINE = 0x08;

    /**
     * Enables literal parsing of the pattern.
     *
     * <p> When this flag is specified then the input string that specifies
     * the pattern is treated as a sequence of literal characters.
     * Metacharacters or escape sequences in the input sequence will be
     * given no special meaning.
     *
     * <p>The flags CASE_INSENSITIVE and UNICODE_CASE retain their impact on
     * matching when used in conjunction with this flag. The other flags
     * become superfluous.
     *
     * <p> There is no embedded flag character for enabling literal parsing.
     * @since 1.5
     */
    public static final int LITERAL = 0x10;

    /**
     * Enables dotall mode.
     *
     * <p> In dotall mode, the expression {@code .} matches any character,
     * including a line terminator.  By default this expression does not match
     * line terminators.
     *
     * <p> Dotall mode can also be enabled via the embedded flag
     * expression&nbsp;{@code (?s)}.  (The {@code s} is a mnemonic for
     * "single-line" mode, which is what this is called in Perl.)  </p>
     */
    public static final int DOTALL = 0x20;

    // Android-changed: UNICODE_CASE flag is ignored.
    /**
     * Enables Unicode-aware case folding. This flag is ignoredon Android.
     * When {@link #CASE_INSENSITIVE} flag is provided, case-insensitive
     * matching is always done in a manner consistent with the Unicode Standard.
     *
     * <p> The embedded flag &nbsp;{@code (?u)} is ignored.
     *
     * <p> Specifying this flag may impose a performance penalty.  </p>
     */
    public static final int UNICODE_CASE = 0x40;

    // Android-changed: Android does not support CANON_EQ flag.
    /**
     * This flag is not supported on Android.
     */
    public static final int CANON_EQ = 0x80;

    // Android-changed: Android always uses unicode character classes.
    /**
     * This flag is not supported on Android, and Unicode character classes are always
     * used.
     * <p>
     * See the Unicode version of <i>Predefined character classes</i> and
     * <i>POSIX character classes</i> as defined by <a href="http://www.unicode.org/reports/tr18/"><i>Unicode Technical
     * Standard #18: Unicode Regular Expression</i></a>
     * <i>Annex C: Compatibility Properties</i>.
     * <p>
     * @since 1.7
     */
    public static final int UNICODE_CHARACTER_CLASS = 0x100;

    /**
     * Contains all possible flags for compile(regex, flags).
     */
    private static final int ALL_FLAGS = CASE_INSENSITIVE | MULTILINE |
            // Android-changed: CANON_EQ and UNICODE_CHARACTER_CLASS flags aren't supported.
            // DOTALL | UNICODE_CASE | CANON_EQ | UNIX_LINES | LITERAL |
            // UNICODE_CHARACTER_CLASS | COMMENTS;
            DOTALL | UNICODE_CASE | UNIX_LINES | LITERAL | COMMENTS;

    /* Pattern has only two serialized components: The pattern string
     * and the flags, which are all that is needed to recompile the pattern
     * when it is deserialized.
     */

    /** use serialVersionUID from Merlin b59 for interoperability */
    private static final long serialVersionUID = 5073258162644648461L;

    /**
     * The original regular-expression pattern string.
     *
     * @serial
     */
    // Android-changed: reimplement matching logic natively via ICU.
    // private String pattern;
    private final String pattern;

    /**
     * The original pattern flags.
     *
     * @serial
     */
    // Android-changed: reimplement matching logic natively via ICU.
    // private int flags;
    private final int flags;

    // BEGIN Android-changed: reimplement matching logic natively via ICU.
    // We only need some tie-ins to native memory, instead of a large number
    // of fields on the .java side.
    /* package */ transient PatternNative nativePattern;
    // END Android-changed: reimplement matching logic natively via ICU.

    /**
     * Compiles the given regular expression into a pattern.
     *
     * @param  regex
     *         The expression to be compiled
     * @return the given regular expression compiled into a pattern
     * @throws  PatternSyntaxException
     *          If the expression's syntax is invalid
     */
    public static Pattern compile(String regex) {
        return new Pattern(regex, 0);
    }

    // Android-changed: Android doesn't support CANON_EQ and UNICODE_CHARACTER_CLASS flags.
    /**
     * Compiles the given regular expression into a pattern with the given
     * flags.
     *
     * @param  regex
     *         The expression to be compiled
     *
     * @param  flags
     *         Match flags, a bit mask that may include
     *         {@link #CASE_INSENSITIVE}, {@link #MULTILINE}, {@link #DOTALL},
     *         {@link #UNICODE_CASE}, {@link #UNIX_LINES}, {@link #LITERAL},
     *         and {@link #COMMENTS}
     *
     * @return the given regular expression compiled into a pattern with the given flags
     * @throws  IllegalArgumentException
     *          If bit values other than those corresponding to the defined
     *          match flags are set in {@code flags}
     *
     * @throws  PatternSyntaxException
     *          If the expression's syntax is invalid
     */
    public static Pattern compile(String regex, int flags) {
        return new Pattern(regex, flags);
    }

    /**
     * Returns the regular expression from which this pattern was compiled.
     *
     * @return  The source of this pattern
     */
    public String pattern() {
        return pattern;
    }

    /**
     * <p>Returns the string representation of this pattern. This
     * is the regular expression from which this pattern was
     * compiled.</p>
     *
     * @return  The string representation of this pattern
     * @since 1.5
     */
    public String toString() {
        return pattern;
    }

    /**
     * Creates a matcher that will match the given input against this pattern.
     *
     * @param  input
     *         The character sequence to be matched
     *
     * @return  A new matcher for this pattern
     */
    public Matcher matcher(CharSequence input) {
        // Android-removed: Pattern is eagerly compiled() upon construction.
        /*
        if (!compiled) {
            synchronized(this) {
                if (!compiled)
                    compile();
            }
        }
        */
        Matcher m = new Matcher(this, input);
        return m;
    }

    /**
     * Returns this pattern's match flags.
     *
     * @return  The match flags specified when this pattern was compiled
     */
    public int flags() {
        // Android-changed: We don't need the temporary pattern flags0.
        // return flags0;
        return flags;
    }

    /**
     * Compiles the given regular expression and attempts to match the given
     * input against it.
     *
     * <p> An invocation of this convenience method of the form
     *
     * <blockquote><pre>
     * Pattern.matches(regex, input);</pre></blockquote>
     *
     * behaves in exactly the same way as the expression
     *
     * <blockquote><pre>
     * Pattern.compile(regex).matcher(input).matches()</pre></blockquote>
     *
     * <p> If a pattern is to be used multiple times, compiling it once and reusing
     * it will be more efficient than invoking this method each time.  </p>
     *
     * @param  regex
     *         The expression to be compiled
     *
     * @param  input
     *         The character sequence to be matched
     * @return whether or not the regular expression matches on the input
     * @throws  PatternSyntaxException
     *          If the expression's syntax is invalid
     */
    public static boolean matches(String regex, CharSequence input) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(input);
        return m.matches();
    }

    // Android-changed: Adopt split() behavior change only for apps targeting API > 28.
    // http://b/109659282#comment7
    /**
     * Splits the given input sequence around matches of this pattern.
     *
     * <p> The array returned by this method contains each substring of the
     * input sequence that is terminated by another subsequence that matches
     * this pattern or is terminated by the end of the input sequence.  The
     * substrings in the array are in the order in which they occur in the
     * input. If this pattern does not match any subsequence of the input then
     * the resulting array has just one element, namely the input sequence in
     * string form.
     *
     * <p> When there is a positive-width match at the beginning of the input
     * sequence then an empty leading substring is included at the beginning
     * of the resulting array. A zero-width match at the beginning however
     * can only produce such an empty leading substring for apps running on or
     * targeting API versions <= 28.
     *
     * <p> The {@code limit} parameter controls the number of times the
     * pattern is applied and therefore affects the length of the resulting
     * array.
     * <ul>
     *    <li><p>
     *    If the <i>limit</i> is positive then the pattern will be applied
     *    at most <i>limit</i>&nbsp;-&nbsp;1 times, the array's length will be
     *    no greater than <i>limit</i>, and the array's last entry will contain
     *    all input beyond the last matched delimiter.</p></li>
     *
     *    <li><p>
     *    If the <i>limit</i> is zero then the pattern will be applied as
     *    many times as possible, the array can have any length, and trailing
     *    empty strings will be discarded.</p></li>
     *
     *    <li><p>
     *    If the <i>limit</i> is negative then the pattern will be applied
     *    as many times as possible and the array can have any length.</p></li>
     * </ul>
     *
     * <p> The input {@code "boo:and:foo"}, for example, yields the following
     * results with these parameters:
     *
     * <table class="plain" style="margin-left:2em;">
     * <caption style="display:none">Split example showing regex, limit, and result</caption>
     * <thead>
     * <tr>
     *     <th scope="col">Regex</th>
     *     <th scope="col">Limit</th>
     *     <th scope="col">Result</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr><th scope="row" rowspan="3" style="font-weight:normal">:</th>
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">2</th>
     *     <td>{@code { "boo", "and:foo" }}</td></tr>
     * <tr><!-- : -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">5</th>
     *     <td>{@code { "boo", "and", "foo" }}</td></tr>
     * <tr><!-- : -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">-2</th>
     *     <td>{@code { "boo", "and", "foo" }}</td></tr>
     * <tr><th scope="row" rowspan="3" style="font-weight:normal">o</th>
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">5</th>
     *     <td>{@code { "b", "", ":and:f", "", "" }}</td></tr>
     * <tr><!-- o -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">-2</th>
     *     <td>{@code { "b", "", ":and:f", "", "" }}</td></tr>
     * <tr><!-- o -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">0</th>
     *     <td>{@code { "b", "", ":and:f" }}</td></tr>
     * </tbody>
     * </table>
     *
     * @param  input
     *         The character sequence to be split
     *
     * @param  limit
     *         The result threshold, as described above
     *
     * @return  The array of strings computed by splitting the input
     *          around matches of this pattern
     */
    public String[] split(CharSequence input, int limit) {
        // BEGIN Android-added: fastSplit() to speed up simple cases.
        String[] fast = fastSplit(pattern, input.toString(), limit);
        if (fast != null) {
            return fast;
        }
        // END Android-added: fastSplit() to speed up simple cases.
        int index = 0;
        boolean matchLimited = limit > 0;
        ArrayList<String> matchList = new ArrayList<>();
        Matcher m = matcher(input);

        // Add segments before each match found
        while(m.find()) {
            if (!matchLimited || matchList.size() < limit - 1) {
                if (index == 0 && index == m.start() && m.start() == m.end()) {
                    // no empty leading substring included for zero-width match
                    // at the beginning of the input char sequence.
                    // BEGIN Android-changed: split() compat behavior for apps targeting <= 28.
                    // continue;
                    int targetSdkVersion = VMRuntime.getRuntime().getTargetSdkVersion();
                    if (targetSdkVersion > 28) {
                        continue;
                    }
                    // END Android-changed: split() compat behavior for apps targeting <= 28.
                }
                String match = input.subSequence(index, m.start()).toString();
                matchList.add(match);
                index = m.end();
            } else if (matchList.size() == limit - 1) { // last one
                String match = input.subSequence(index,
                                                 input.length()).toString();
                matchList.add(match);
                index = m.end();
            }
        }

        // If no match was found, return this
        if (index == 0)
            return new String[] {input.toString()};

        // Add remaining segment
        if (!matchLimited || matchList.size() < limit)
            matchList.add(input.subSequence(index, input.length()).toString());

        // Construct result
        int resultSize = matchList.size();
        if (limit == 0)
            while (resultSize > 0 && matchList.get(resultSize-1).equals(""))
                resultSize--;
        String[] result = new String[resultSize];
        return matchList.subList(0, resultSize).toArray(result);
    }

    // BEGIN Android-added: fastSplit() to speed up simple cases.
    private static final String FASTSPLIT_METACHARACTERS = "\\?*+[](){}^$.|";

    /**
     * Returns a result equivalent to {@code s.split(separator, limit)} if it's able
     * to compute it more cheaply than native impl, or null if the caller should fall back to
     * using native impl.
     *
     *  fastpath will work  if the regex is a
     *   (1)one-char String and this character is not one of the
     *      RegEx's meta characters ".$|()[{^?*+\\", or
     *   (2)two-char String and the first char is the backslash and
     *      the second is one of regEx's meta characters ".$|()[{^?*+\\".
     * @hide
     */
    public static String[] fastSplit(String re, String input, int limit) {
        // Can we do it cheaply?
        int len = re.length();
        if (len == 0) {
            return null;
        }
        char ch = re.charAt(0);
        if (len == 1) {
            if (Character.isSurrogate(ch)) {
                // Single surrogate is an invalid UTF-16 sequence.
                return null;
            } else if (FASTSPLIT_METACHARACTERS.indexOf(ch) != -1) {
                // We don't allow a single metacharacter.
                return null;
            }
            // pass through
        } else if (len == 2 && ch == '\\') {
            // We're looking for a quoted character.
            // Quoted metacharacters are effectively single non-metacharacters.
            ch = re.charAt(1);
            if (FASTSPLIT_METACHARACTERS.indexOf(ch) == -1) {
                return null;
            }
        } else {
            return null;
        }

        // We can do this cheaply...

        // Unlike Perl, which considers the result of splitting the empty string to be the empty
        // array, Java returns an array containing the empty string.
        if (input.isEmpty()) {
            return new String[] { "" };
        }

        // Count separators
        int separatorCount = 0;
        int begin = 0;
        int end;
        while (separatorCount + 1 != limit && (end = input.indexOf(ch, begin)) != -1) {
            ++separatorCount;
            begin = end + 1;
        }
        int lastPartEnd = input.length();
        if (limit == 0 && begin == lastPartEnd) {
            // Last part is empty for limit == 0, remove all trailing empty matches.
            if (separatorCount == lastPartEnd) {
                // Input contains only separators.
                return EmptyArray.STRING;
            }
            // Find the beginning of trailing separators.
            do {
                --begin;
            } while (input.charAt(begin - 1) == ch);
            // Reduce separatorCount and fix lastPartEnd.
            separatorCount -= input.length() - begin;
            lastPartEnd = begin;
        }

        // Collect the result parts.
        String[] result = new String[separatorCount + 1];
        begin = 0;
        for (int i = 0; i != separatorCount; ++i) {
            end = input.indexOf(ch, begin);
            result[i] = input.substring(begin, end);
            begin = end + 1;
        }
        // Add last part.
        result[separatorCount] = input.substring(begin, lastPartEnd);
        return result;
    }
    // END Android-added: fastSplit() to speed up simple cases.

    /**
     * Splits the given input sequence around matches of this pattern.
     *
     * <p> This method works as if by invoking the two-argument {@link
     * #split(java.lang.CharSequence, int) split} method with the given input
     * sequence and a limit argument of zero.  Trailing empty strings are
     * therefore not included in the resulting array. </p>
     *
     * <p> The input {@code "boo:and:foo"}, for example, yields the following
     * results with these expressions:
     *
     * <table class="plain" style="margin-left:2em">
     * <caption style="display:none">Split examples showing regex and result</caption>
     * <thead>
     * <tr>
     *  <th scope="col">Regex</th>
     *  <th scope="col">Result</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr><th scope="row" style="text-weight:normal">:</th>
     *     <td>{@code { "boo", "and", "foo" }}</td></tr>
     * <tr><th scope="row" style="text-weight:normal">o</th>
     *     <td>{@code { "b", "", ":and:f" }}</td></tr>
     * </tbody>
     * </table>
     *
     *
     * @param  input
     *         The character sequence to be split
     *
     * @return  The array of strings computed by splitting the input
     *          around matches of this pattern
     */
    public String[] split(CharSequence input) {
        return split(input, 0);
    }

    /**
     * Returns a literal pattern {@code String} for the specified
     * {@code String}.
     *
     * <p>This method produces a {@code String} that can be used to
     * create a {@code Pattern} that would match the string
     * {@code s} as if it were a literal pattern.</p> Metacharacters
     * or escape sequences in the input sequence will be given no special
     * meaning.
     *
     * @param  s The string to be literalized
     * @return  A literal string replacement
     * @since 1.5
     */
    public static String quote(String s) {
        int slashEIndex = s.indexOf("\\E");
        if (slashEIndex == -1)
            return "\\Q" + s + "\\E";

        int lenHint = s.length();
        lenHint = (lenHint < Integer.MAX_VALUE - 8 - lenHint) ?
                (lenHint << 1) : (Integer.MAX_VALUE - 8);

        StringBuilder sb = new StringBuilder(lenHint);
        sb.append("\\Q");
        int current = 0;
        do {
            sb.append(s, current, slashEIndex)
                    .append("\\E\\\\E\\Q");
            current = slashEIndex + 2;
        } while ((slashEIndex = s.indexOf("\\E", current)) != -1);

        return sb.append(s, current, s.length())
                .append("\\E")
                .toString();
    }

    /**
     * Recompile the Pattern instance from a stream.  The original pattern
     * string is read in and the object tree is recompiled from it.
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {

        // Read in all fields
        s.defaultReadObject();

        // Android-removed: reimplement matching logic natively via ICU.
        /*
        // reset the flags
        flags0 = flags;

        // Initialize counts
        capturingGroupCount = 1;
        localCount = 0;
        localTCNCount = 0;
        */

        // Android-changed: Pattern is eagerly compiled() upon construction.
        /*
        // if length > 0, the Pattern is lazily compiled
        if (pattern.isEmpty()) {
            root = new Start(lastAccept);
            matchRoot = lastAccept;
            compiled = true;
        }
        */
        compile();
    }

    // Android-changed: reimplement matching logic natively via ICU.
    // Dropped documentation reference to Start and LastNode implementation
    // details which do not apply on Android.
    /**
     * This private constructor is used to create all Patterns. The pattern
     * string and match flags are all that is needed to completely describe
     * a Pattern.
     */
    private Pattern(String p, int f) {
        // BEGIN Android-added: CANON_EQ and UNICODE_CHARACTER_CLASS flags are not supported.
        if ((f & CANON_EQ) != 0) {
            throw new IllegalArgumentException("CANON_EQ flag isn't supported");
        }
        if ((f & UNICODE_CHARACTER_CLASS) != 0) {
            throw new IllegalArgumentException("UNICODE_CHARACTER_CLASS flag not supported");
        }
        // END Android-added: CANON_EQ and UNICODE_CHARACTER_CLASS flags are not supported.
        if ((f & ~ALL_FLAGS) != 0) {
            throw new IllegalArgumentException("Unknown flag 0x"
                                               + Integer.toHexString(f));
        }
        pattern = p;
        flags = f;

        // Android-changed: Pattern is eagerly compiled() upon construction.
        // BEGIN Android-changed: Reimplement matching logic via ICU4C, and shouldn't overflow.
        /*
        // to use UNICODE_CASE if UNICODE_CHARACTER_CLASS present
        if ((flags & UNICODE_CHARACTER_CLASS) != 0)
            flags |= UNICODE_CASE;

        // 'flags' for compiling
        flags0 = flags;

        // Reset group index count
        capturingGroupCount = 1;
        localCount = 0;
        localTCNCount = 0;

        if (!pattern.isEmpty()) {
            try {
                compile();
            } catch (StackOverflowError soe) {
                throw error("Stack overflow during pattern compilation");
            }
        } else {
            root = new Start(lastAccept);
            matchRoot = lastAccept;
        }
        */
        compile();
        // END Android-changed: Reimplement matching logic via ICU4C, and shouldn't overflow.
    }

    // BEGIN Android-changed: reimplement matching logic natively via ICU.
    // Use native implementation instead of > 3000 lines of helper methods.
    private void compile() throws PatternSyntaxException {
        if (pattern == null) {
            throw new NullPointerException("pattern == null");
        }

        String icuPattern = pattern;
        if ((flags & LITERAL) != 0) {
            icuPattern = quote(pattern);
        }

        // These are the flags natively supported by ICU.
        // They even have the same value in native code.
        int icuFlags = flags & (CASE_INSENSITIVE | COMMENTS | MULTILINE | DOTALL | UNIX_LINES);
        nativePattern = PatternNative.create(icuPattern, icuFlags);
    }
    // END Android-changed: reimplement matching logic natively via ICU.

    /**
     * Creates a predicate that tests if this pattern is found in a given input
     * string.
     *
     * @apiNote
     * This method creates a predicate that behaves as if it creates a matcher
     * from the input sequence and then calls {@code find}, for example a
     * predicate of the form:
     * <pre>{@code
     *   s -> matcher(s).find();
     * }</pre>
     *
     * @return  The predicate which can be used for finding a match on a
     *          subsequence of a string
     * @since   1.8
     * @see     Matcher#find
     */
    public Predicate<String> asPredicate() {
        return s -> matcher(s).find();
    }

    /**
     * Creates a predicate that tests if this pattern matches a given input string.
     *
     * @apiNote
     * This method creates a predicate that behaves as if it creates a matcher
     * from the input sequence and then calls {@code matches}, for example a
     * predicate of the form:
     * <pre>{@code
     *   s -> matcher(s).matches();
     * }</pre>
     *
     * @return  The predicate which can be used for matching an input string
     *          against this pattern.
     * @since   11
     * @see     Matcher#matches
     */
    public Predicate<String> asMatchPredicate() {
        return s -> matcher(s).matches();
    }

    /**
     * Creates a stream from the given input sequence around matches of this
     * pattern.
     *
     * <p> The stream returned by this method contains each substring of the
     * input sequence that is terminated by another subsequence that matches
     * this pattern or is terminated by the end of the input sequence.  The
     * substrings in the stream are in the order in which they occur in the
     * input. Trailing empty strings will be discarded and not encountered in
     * the stream.
     *
     * <p> If this pattern does not match any subsequence of the input then
     * the resulting stream has just one element, namely the input sequence in
     * string form.
     *
     * <p> When there is a positive-width match at the beginning of the input
     * sequence then an empty leading substring is included at the beginning
     * of the stream. A zero-width match at the beginning however never produces
     * such empty leading substring.
     *
     * <p> If the input sequence is mutable, it must remain constant during the
     * execution of the terminal stream operation.  Otherwise, the result of the
     * terminal stream operation is undefined.
     *
     * @param   input
     *          The character sequence to be split
     *
     * @return  The stream of strings computed by splitting the input
     *          around matches of this pattern
     * @see     #split(CharSequence)
     * @since   1.8
     */
    public Stream<String> splitAsStream(final CharSequence input) {
        class MatcherIterator implements Iterator<String> {
            private Matcher matcher;
            // The start position of the next sub-sequence of input
            // when current == input.length there are no more elements
            private int current;
            // null if the next element, if any, needs to obtained
            private String nextElement;
            // > 0 if there are N next empty elements
            private int emptyElementCount;

            public String next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                if (emptyElementCount == 0) {
                    String n = nextElement;
                    nextElement = null;
                    return n;
                } else {
                    emptyElementCount--;
                    return "";
                }
            }

            public boolean hasNext() {
                if (matcher == null) {
                    matcher = matcher(input);
                    // If the input is an empty string then the result can only be a
                    // stream of the input.  Induce that by setting the empty
                    // element count to 1
                    emptyElementCount = input.length() == 0 ? 1 : 0;
                }
                if (nextElement != null || emptyElementCount > 0)
                    return true;

                if (current == input.length())
                    return false;

                // Consume the next matching element
                // Count sequence of matching empty elements
                while (matcher.find()) {
                    nextElement = input.subSequence(current, matcher.start()).toString();
                    current = matcher.end();
                    if (!nextElement.isEmpty()) {
                        return true;
                    } else if (current > 0) { // no empty leading substring for zero-width
                                              // match at the beginning of the input
                        emptyElementCount++;
                    }
                }

                // Consume last matching element
                nextElement = input.subSequence(current, input.length()).toString();
                current = input.length();
                if (!nextElement.isEmpty()) {
                    return true;
                } else {
                    // Ignore a terminal sequence of matching empty elements
                    emptyElementCount = 0;
                    nextElement = null;
                    return false;
                }
            }
        }
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                new MatcherIterator(), Spliterator.ORDERED | Spliterator.NONNULL), false);
    }
}
