/*
 * Copyright (C) 2016 The Android Open Source Project
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

package libcore.java.text;

import junit.framework.TestCase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class DateFormatTest extends TestCase {

    // Regression test for http://b/31762542. If this test fails it implies that changes to
    // DateFormat.is24Hour will not be effective.
    public void testIs24Hour_notCached() throws Exception {
        Boolean originalIs24Hour = DateFormat.is24Hour;
        try {
            // These tests hardcode expectations for Locale.US.
            DateFormat.is24Hour = null; // null == locale default (12 hour for US)
            checkTimePattern(DateFormat.SHORT, "h:mm a");
            checkTimePattern(DateFormat.MEDIUM, "h:mm:ss a");

            DateFormat.is24Hour = true; // Explicit 24 hour.
            checkTimePattern(DateFormat.SHORT, "HH:mm");
            checkTimePattern(DateFormat.MEDIUM, "HH:mm:ss");

            DateFormat.is24Hour = false; // Explicit 12 hour.
            checkTimePattern(DateFormat.SHORT, "h:mm a");
            checkTimePattern(DateFormat.MEDIUM, "h:mm:ss a");
        } finally {
            DateFormat.is24Hour = originalIs24Hour;
        }
    }

    private static void checkTimePattern(int style, String expectedPattern) {
        final Locale locale = Locale.US;
        final Date current = new Date(1468250177000L); // 20160711 15:16:17 GMT
        DateFormat format = DateFormat.getTimeInstance(style, locale);
        String actualDateString = format.format(current);
        SimpleDateFormat sdf = new SimpleDateFormat(expectedPattern, locale);
        String expectedDateString = sdf.format(current);
        assertEquals(expectedDateString, actualDateString);
    }

    // 1 January 2022 00:00:00 GMT+00:00
    private static final Date TEST_DATE = new Date(1640995200000L);

    /**
     * Test {@link DateFormat#format(Date)} does not crash on available locales.
     */
    public void test_format_allLocales() {
        for (Locale locale : Locale.getAvailableLocales()) {
            for (int formatStyle = DateFormat.FULL; formatStyle <= DateFormat.SHORT;
                    formatStyle++) {
                try {
                    DateFormat.getDateInstance(formatStyle, locale)
                            .format(TEST_DATE);
                    DateFormat.getTimeInstance(formatStyle, locale)
                            .format(TEST_DATE);
                    DateFormat.getDateTimeInstance(formatStyle, formatStyle, locale)
                            .format(TEST_DATE);
                } catch (RuntimeException cause) {
                    throw new RuntimeException("locale:" + locale +
                            " formatStyle:" + formatStyle, cause);
                }
            }
        }
    }

    /**
     * Test {@link SimpleDateFormat#toPattern()} contains only supported symbols.
     */
    public void test_toPattern_allLocales() {
        for (Locale locale : Locale.getAvailableLocales()) {
            for (int formatStyle = DateFormat.FULL; formatStyle <= DateFormat.SHORT;
                    formatStyle++) {
                try {
                    assertSupportedSymbols(DateFormat.getDateInstance(formatStyle, locale), locale);
                    assertSupportedSymbols(DateFormat.getTimeInstance(formatStyle, locale), locale);
                    assertSupportedSymbols(DateFormat.getDateTimeInstance(
                            formatStyle, formatStyle, locale), locale);
                } catch (RuntimeException cause) {
                    throw new RuntimeException("locale:" + locale +
                            " formatStyle:" + formatStyle, cause);
                }
            }
        }
    }

    private static final Set<Character> SUPPORTED_SYMBOLS = "GyYMLwWDdFEuaHkKhmsSzZXLc".chars()
            .mapToObj(c -> (char)c)
            .collect(Collectors.toSet());

    private static void assertSupportedSymbols(DateFormat dateFormat, Locale locale) {
        SimpleDateFormat simpleDateFormat = (SimpleDateFormat) dateFormat;
        String pattern = simpleDateFormat.toPattern();
        // The string in the quotation is not interpreted.
        boolean inQuotation = false;
        for (int i = 0; i < pattern.length(); i++) {
            char curr = pattern.charAt(i);
            if (curr == '\'') {
                inQuotation = !inQuotation;
                continue;
            }
            if (inQuotation) {
                continue;
            }

            if ((curr >= 'a' && curr <= 'z') || (curr >= 'A' && curr <= 'Z')) { // ASCII alphabets
                assertTrue("Locale:" + locale + " Pattern:" + pattern + " has unsupported symbol "
                                + curr, SUPPORTED_SYMBOLS.contains(curr));
            }
        }
    }
}
