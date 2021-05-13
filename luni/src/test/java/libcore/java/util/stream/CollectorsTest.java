/*
 * Copyright (C) 2021 The Android Open Source Project
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

package libcore.java.util.stream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import static java.util.stream.Collectors.counting;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.stream.Collector;
import java.util.stream.Stream;

@RunWith(JUnit4.class)
public class CollectorsTest {

    @Test
    public void counting_countsNulls() {
        long count = Stream.of(null, null, null).collect(counting());

        assertEquals(3, count);
    }

    @Test
    public void counting_emptyStream() {
        assertEquals(0L, (long) Stream.empty().collect(counting()));
    }

    @Test
    public void counting_nonEmptyStream() {
        long count = Stream.of(null, 1, 2, "").collect(counting());

        assertEquals(4, count);
    }

    @Test
    public void counting_largeStream() {
        int size = 10_000_000;

        long actual = Stream.generate(() -> 1)
                .limit(size)
                .collect(counting());

        assertEquals(size, actual);
    }

    @Test
    public void collectorOf() {
        Collector<Integer, int[], int[]> sqSumCollector =
                Collector.of(
                        () -> new int[] {0},
                        (sum, next) -> sum[0] = sum[0] + next * next,
                        (a, b) -> new int[] {a[0] + b[0]},
                        Collector.Characteristics.UNORDERED);

        int[] anArray = new int[] {10};
        assertSame("Finisher is identity fn", anArray, sqSumCollector.finisher().apply(anArray));

        assertArrayEquals(new int[]{0}, sqSumCollector.supplier().get());
        assertArrayEquals(new int[] {20}, sqSumCollector.combiner().apply(anArray, anArray));

        sqSumCollector.accumulator().accept(anArray, 10);
        assertArrayEquals(new int[] {110}, anArray);
        assertTrue(sqSumCollector.characteristics().contains(Collector.Characteristics.UNORDERED));

        assertArrayEquals(new int[] {30}, Stream.of(1, 2, 3, 4).collect(sqSumCollector));
    }
}
