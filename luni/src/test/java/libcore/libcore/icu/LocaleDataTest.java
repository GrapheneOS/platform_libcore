/*
 * Copyright (C) 2012 The Android Open Source Project
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

package libcore.libcore.icu;

import static org.junit.Assert.assertArrayEquals;

import java.util.Locale;
import libcore.icu.LocaleData;

public class LocaleDataTest extends junit.framework.TestCase {
  static {
    System.loadLibrary("javacoretests");
  }

  public void testAll() throws Exception {
    // Test that we can get the locale data for all known locales.
    for (Locale l : Locale.getAvailableLocales()) {
      LocaleData d = LocaleData.get(l);
      System.err.format("%20s %10s %10s\n", l, d.timeFormat_hm, d.timeFormat_Hm);
    }
  }

  public void test_en_US() throws Exception {
    LocaleData l = LocaleData.get(Locale.US);
    assertEquals("AM", l.amPm[0]);
    assertEquals("a", l.narrowAm);

    assertEquals("BC", l.eras[0]);

    assertEquals("January", l.longMonthNames[0]);
    assertEquals("Jan", l.shortMonthNames[0]);
    assertEquals("J", l.tinyMonthNames[0]);

    assertEquals("January", l.longStandAloneMonthNames[0]);
    assertEquals("Jan", l.shortStandAloneMonthNames[0]);
    assertEquals("J", l.tinyStandAloneMonthNames[0]);

    assertEquals("Sunday", l.longWeekdayNames[1]);
    assertEquals("Sun", l.shortWeekdayNames[1]);
    assertEquals("S", l.tinyWeekdayNames[1]);

    assertEquals("Sunday", l.longStandAloneWeekdayNames[1]);
    assertEquals("Sun", l.shortStandAloneWeekdayNames[1]);
    assertEquals("S", l.tinyStandAloneWeekdayNames[1]);

  }

  public void test_cs_CZ() throws Exception {
    LocaleData l = LocaleData.get(new Locale("cs", "CZ"));

    assertEquals("ledna", l.longMonthNames[0]);
    assertEquals("led", l.shortMonthNames[0]);
    assertEquals("1", l.tinyMonthNames[0]);

    assertEquals("leden", l.longStandAloneMonthNames[0]);
    assertEquals("led", l.shortStandAloneMonthNames[0]);
    assertEquals("1", l.tinyStandAloneMonthNames[0]);
  }

  public void test_ru_RU() throws Exception {
    LocaleData l = LocaleData.get(new Locale("ru", "RU"));

    assertEquals("воскресенье", l.longWeekdayNames[1]);
    assertEquals("вс", l.shortWeekdayNames[1]);
    assertEquals("вс", l.tinyWeekdayNames[1]);

    // Russian stand-alone weekday names have no initial capital since CLDR 28/ICU 56.
    assertEquals("воскресенье", l.longStandAloneWeekdayNames[1]);
    assertEquals("вс", l.shortStandAloneWeekdayNames[1]);
    assertEquals("В", l.tinyStandAloneWeekdayNames[1]);
  }

  // http://code.google.com/p/android/issues/detail?id=38844
  public void testDecimalFormatSymbols_es() throws Exception {
    LocaleData es = LocaleData.get(new Locale("es"));
    assertEquals(',', es.decimalSeparator);
    assertEquals('.', es.groupingSeparator);

    LocaleData es_419 = LocaleData.get(new Locale("es", "419"));
    assertEquals('.', es_419.decimalSeparator);
    assertEquals(',', es_419.groupingSeparator);

    LocaleData es_US = LocaleData.get(new Locale("es", "US"));
    assertEquals('.', es_US.decimalSeparator);
    assertEquals(',', es_US.groupingSeparator);

    LocaleData es_MX = LocaleData.get(new Locale("es", "MX"));
    assertEquals('.', es_MX.decimalSeparator);
    assertEquals(',', es_MX.groupingSeparator);

    LocaleData es_AR = LocaleData.get(new Locale("es", "AR"));
    assertEquals(',', es_AR.decimalSeparator);
    assertEquals('.', es_AR.groupingSeparator);
  }

  // http://b/7924970
  public void testTimeFormat12And24() throws Exception {
    LocaleData en_US = LocaleData.get(Locale.US);
    assertEquals("h:mm a", en_US.timeFormat_hm);
    assertEquals("HH:mm", en_US.timeFormat_Hm);

    LocaleData ja_JP = LocaleData.get(Locale.JAPAN);
    assertEquals("aK:mm", ja_JP.timeFormat_hm);
    assertEquals("H:mm", ja_JP.timeFormat_Hm);
  }

  // http://b/26397197
  public void testPatternWithOverride() throws Exception {
    LocaleData haw = LocaleData.get(new Locale("haw"));
    assertFalse(haw.shortDateFormat.isEmpty());
  }

  /**
   * Check that LocaleData.get() does not throw when the input locale is invalid.
   * http://b/129070579
   */
  public void testInvalidLocale() {
    LocaleData.get(new Locale("invalidLocale"));
  }

  // BCP 47 language tags.
  private static final String[] TEST_LANGUAGE_TAGS = new String[] {
    "en-US",
    "en-GB",
    "ar-EG",
    "zh-CN",
    "zh-TW",
    "nl-NL",
    "fr-FR",
    "de-DE",
    "it-IT",
    "ja-JP",
    "ko-KR",
    "pl-PL",
    "pt-BR",
    "ru-RU",
    "es-ES",
    "th-TH",
    "tr-TR",
    "es-419",
  };

  public void testInitLocaleData_icu4cConsistency() throws Exception {
    // Don't test all available locales due to incorrect assumptions in the JNI test code
    // calling ICU4C. The JNI test code came from libcore, but now moved into this test to load
    // the locale data from ICU4C. The inappropriate assumptions are the below
    // 1. Use uloc_getParent as fallback locale in ICU resource bundle format.
    //    uloc_getParent only truncates the locale ID string tp get parent, but doesn't use the
    //    parent-child relationships in the locale data.
    // 2. Truncate single Unicode character, e.g. U+11136 CHAKMA DIGIT ZERO, into single high
    //    surrogate, e.g. 0xD804.
    // 3. Does not consider alias in Root Locale
    //    For AM/PM markers, AmPmMarkersNarrow is specified in the Root Locale as the alias of
    //    "/LOCALE/calendar/gregorian/AmPmMarkersAbbr".

    for (String languageTag : TEST_LANGUAGE_TAGS) {
      Locale locale = Locale.forLanguageTag(languageTag);
      LocaleData localeData = LocaleData.initLocaleData(locale);
      Icu4cLocaleData testData = new Icu4cLocaleData();
      assertTrue("Failed to load ICU4C data", initIcu4cLocaleData(locale.toLanguageTag(),
        testData, Icu4cLocaleData.class));
      assertLocaleDataEquals(locale, testData, localeData);
    }
  }

  /**
   * This function does not compare all fields in {@code Icu4cLocaleData} and {@code LocaleData}
   * because some fields, e.g. patternSeparator, in {@Code LocaleData} has always implemented
   * by ICU4J or always by ICU4C.
   */
  private static void assertLocaleDataEquals(Locale locale, Icu4cLocaleData testData,
      LocaleData localeData) {
    String baseMsg = "LocaleData provides different value in locale " + locale + " in the field:";
    assertEquals(baseMsg + "minimalDaysInFirstWeek", testData.firstDayOfWeek, localeData.firstDayOfWeek);
    assertEquals(baseMsg + "minimalDaysInFirstWeek", testData.minimalDaysInFirstWeek, localeData.minimalDaysInFirstWeek);
    assertArrayEquals(baseMsg + "amPm", testData.amPm, localeData.amPm);
    assertArrayEquals(baseMsg + "eras", testData.eras, localeData.eras);
    assertArrayEquals(baseMsg + "longMonthNames", testData.longMonthNames, localeData.longMonthNames);
    assertArrayEquals(baseMsg + "shortMonthNames", testData.shortMonthNames, localeData.shortMonthNames);
    assertArrayEquals(baseMsg + "tinyMonthNames", testData.tinyMonthNames, localeData.tinyMonthNames);
    assertArrayEquals(baseMsg + "longStandAloneMonthNames", testData.longStandAloneMonthNames, localeData.longStandAloneMonthNames);
    assertArrayEquals(baseMsg + "shortStandAloneMonthNames", testData.shortStandAloneMonthNames, localeData.shortStandAloneMonthNames);
    assertArrayEquals(baseMsg + "tinyStandAloneMonthNames", testData.tinyStandAloneMonthNames, localeData.tinyStandAloneMonthNames);
    assertArrayEquals(baseMsg + "longWeekdayNames", testData.longWeekdayNames, localeData.longWeekdayNames);
    assertArrayEquals(baseMsg + "shortWeekdayNames", testData.shortWeekdayNames, localeData.shortWeekdayNames);
    assertArrayEquals(baseMsg + "tinyWeekdayNames", testData.tinyWeekdayNames, localeData.tinyWeekdayNames);
    assertArrayEquals(baseMsg + "longStandAloneWeekdayNames", testData.longStandAloneWeekdayNames, localeData.longStandAloneWeekdayNames);
    assertArrayEquals(baseMsg + "shortStandAloneWeekdayNames", testData.shortStandAloneWeekdayNames, localeData.shortStandAloneWeekdayNames);
    assertArrayEquals(baseMsg + "tinyStandAloneWeekdayNames", testData.tinyStandAloneWeekdayNames, localeData.tinyStandAloneWeekdayNames);

    assertEquals(baseMsg + "fullTimeFormat", testData.fullTimeFormat, localeData.fullTimeFormat);
    assertEquals(baseMsg + "longTimeFormat", testData.longTimeFormat, localeData.longTimeFormat);
    assertEquals(baseMsg + "mediumTimeFormat", testData.mediumTimeFormat, localeData.mediumTimeFormat);
    assertEquals(baseMsg + "shortTimeFormat", testData.shortTimeFormat, localeData.shortTimeFormat);
    assertEquals(baseMsg + "fullDateFormat", testData.fullDateFormat, localeData.fullDateFormat);
    assertEquals(baseMsg + "longDateFormat", testData.longDateFormat, localeData.longDateFormat);
    assertEquals(baseMsg + "mediumDateFormat", testData.mediumDateFormat, localeData.mediumDateFormat);
    assertEquals(baseMsg + "shortDateFormat", testData.shortDateFormat, localeData.shortDateFormat);
    assertEquals(baseMsg + "narrowAm", testData.narrowAm, localeData.narrowAm);
    assertEquals(baseMsg + "narrowPm", testData.narrowPm, localeData.narrowPm);

    assertEquals(baseMsg + "zeroDigit", testData.zeroDigit, localeData.zeroDigit);
    assertEquals(baseMsg + "decimalSeparator", testData.decimalSeparator, localeData.decimalSeparator);
    assertEquals(baseMsg + "groupingSeparator", testData.groupingSeparator, localeData.groupingSeparator);
    assertEquals(baseMsg + "percent", testData.percent, localeData.percent);
    assertEquals(baseMsg + "perMill", testData.perMill, localeData.perMill);
    assertEquals(baseMsg + "monetarySeparator", testData.monetarySeparator, localeData.monetarySeparator);
    assertEquals(baseMsg + "minusSign", testData.minusSign, localeData.minusSign);
    assertEquals(baseMsg + "exponentSeparator", testData.exponentSeparator, localeData.exponentSeparator);
    assertEquals(baseMsg + "infinity", testData.infinity, localeData.infinity);
    assertEquals(baseMsg + "NaN", testData.NaN, localeData.NaN);
    assertEquals(baseMsg + "numberPattern", testData.numberPattern, localeData.numberPattern);
    assertEquals(baseMsg + "currencyPattern", testData.currencyPattern, localeData.currencyPattern);
    assertEquals(baseMsg + "percentPattern", testData.percentPattern, localeData.percentPattern);
  }

  private static native boolean initIcu4cLocaleData(String languageTag,
    Icu4cLocaleData icu4cLocaleData, Class<Icu4cLocaleData> Icu4cLocaleData);

  private static class Icu4cLocaleData {
      public Integer firstDayOfWeek;
      public Integer minimalDaysInFirstWeek;

      public String[] amPm; // "AM", "PM".
      public String[] eras; // "BC", "AD".
      public String[] longMonthNames; // "January", ...
      public String[] shortMonthNames; // "Jan", ...
      public String[] tinyMonthNames; // "J", ...
      public String[] longStandAloneMonthNames; // "January", ...
      public String[] shortStandAloneMonthNames; // "Jan", ...
      public String[] tinyStandAloneMonthNames; // "J", ...
      public String[] longWeekdayNames; // "Sunday", ...
      public String[] shortWeekdayNames; // "Sun", ...
      public String[] tinyWeekdayNames; // "S", ...
      public String[] longStandAloneWeekdayNames; // "Sunday", ...
      public String[] shortStandAloneWeekdayNames; // "Sun", ...
      public String[] tinyStandAloneWeekdayNames; // "S", ...

      public String fullTimeFormat;
      public String longTimeFormat;
      public String mediumTimeFormat;
      public String shortTimeFormat;

      public String fullDateFormat;
      public String longDateFormat;
      public String mediumDateFormat;
      public String shortDateFormat;

      // Used by TimePicker. Not currently used by UTS#35.
      public String narrowAm; // "a".
      public String narrowPm; // "p".

      // Used by DecimalFormatSymbols.
      public char zeroDigit;
      public char decimalSeparator;
      public char groupingSeparator;
      public char patternSeparator;
      public String percent;
      public String perMill;
      public char monetarySeparator;
      public String minusSign;
      public String exponentSeparator;
      public String infinity;
      public String NaN;

      // Used by DecimalFormat and NumberFormat.
      public String numberPattern;
      public String currencyPattern;
      public String percentPattern;

      private Icu4cLocaleData() {}
  }
}
