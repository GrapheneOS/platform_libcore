/*
 * Copyright (C) 2017 The Android Open Source Project
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
package libcore.java.time.zone;

import org.junit.Test;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Additional tests for {@link ZoneRules}.
 *
 * @see tck.java.time.zone.TCKZoneRules
 */
public class ZoneRulesTest {

    @Test
    public void test_of_ZoneOffset() {
        ZoneOffset offset = ZoneOffset.MIN;
        ZoneRules zoneRules = ZoneRules.of(offset);

        assertEquals(Collections.emptyList(), zoneRules.getTransitionRules());
        assertEquals(Collections.emptyList(), zoneRules.getTransitions());
        assertNull(zoneRules.nextTransition(Instant.MIN));

        // Check various offsets at a bunch of instants, as they should be constant.
        Instant[] instants = new Instant[] {
                LocalDateTime.MIN.toInstant(offset),
                Instant.EPOCH,
                LocalDateTime.of(2000, Month.JANUARY, 1, 1, 1).toInstant(ZoneOffset.UTC),
                Instant.now(),
                LocalDateTime.MAX.toInstant(offset),
        };

        for (Instant instant : instants) {
            assertEquals(Duration.ZERO, zoneRules.getDaylightSavings(instant));
            assertEquals(offset, zoneRules.getOffset(instant));
            assertEquals(offset, zoneRules.getStandardOffset(instant));
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, offset);
            assertNull(zoneRules.getTransition(localDateTime));
            assertEquals(Collections.singletonList(offset),
                    zoneRules.getValidOffsets(localDateTime));
        }
    }

    @Test(expected = NullPointerException.class)
    public void test_of_ZoneOffset_null() {
        ZoneRules.of(null);
    }

    @Test
    public void test_posixZones_haveIndependentHistoricalRules() {
        // TZDB 2026d restores these as separate zones instead of aliases for US cities.
        // Unlike those cities, their standard offsets extend back before the 1883 change
        // from local mean time to standard time.
        String[] zoneIds = { "PST8PDT", "MST7MDT", "CST6CDT", "EST5EDT" };
        int[] standardOffsetHours = { -8, -7, -6, -5 };
        Instant historicalInstant = Instant.parse("1880-01-01T00:00:00Z");
        Instant summerInstant = Instant.parse("2027-07-01T00:00:00Z");

        for (int i = 0; i < zoneIds.length; ++i) {
            String zoneId = zoneIds[i];
            ZoneRules rules = ZoneId.of(zoneId).getRules();
            ZoneOffset standardOffset = ZoneOffset.ofHours(standardOffsetHours[i]);
            assertEquals(zoneId, standardOffset, rules.getOffset(historicalInstant));
            assertEquals(zoneId, standardOffset, rules.getStandardOffset(historicalInstant));
            assertEquals(zoneId, Duration.ZERO, rules.getDaylightSavings(historicalInstant));

            assertEquals(zoneId, ZoneOffset.ofHours(standardOffsetHours[i] + 1),
                    rules.getOffset(summerInstant));
            assertEquals(zoneId, standardOffset, rules.getStandardOffset(summerInstant));
            assertEquals(zoneId, Duration.ofHours(1), rules.getDaylightSavings(summerInstant));
        }
    }

    @Test
    public void test_historicalTransitions_updatedInTzdb2026dAnd2026e() {
        // TZDB 2026d starts DST a day earlier in Bogota and Tehran.
        assertTransition("America/Bogota", "1992-05-02T05:00:00Z",
                ZoneOffset.ofHours(-5), ZoneOffset.ofHours(-4));
        assertTransition("Asia/Tehran", "1979-05-25T20:30:00Z",
                ZoneOffset.ofHoursMinutes(3, 30), ZoneOffset.ofHoursMinutes(4, 30));

        // TZDB 2026e ends Dublin's 1925 summer time two weeks before London's.
        assertTransition("Europe/Dublin", "1925-09-20T02:00:00Z",
                ZoneOffset.ofHours(1), ZoneOffset.UTC);
    }

    private static void assertTransition(String zoneId, String instantString,
            ZoneOffset beforeOffset, ZoneOffset afterOffset) {
        ZoneRules rules = ZoneId.of(zoneId).getRules();
        Instant instant = Instant.parse(instantString);
        assertEquals(zoneId, beforeOffset, rules.getOffset(instant.minusNanos(1)));
        assertEquals(zoneId, afterOffset, rules.getOffset(instant));
        assertEquals(zoneId, instant,
                rules.nextTransition(instant.minusNanos(1)).getInstant());
    }
}
