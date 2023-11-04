/*
 * Copyright (C) 2023 The Android Open Source Project
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

package libcore.java.util.random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import static java.util.stream.Collectors.toSet;

import libcore.test.annotation.NonCts;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigInteger;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;


@RunWith(JUnit4.class)
public class RandomGeneratorFactoryTest {

    @Test
    public void defaultFactoryInstance() {
        RandomGeneratorFactory<RandomGenerator> defaultFactory = RandomGeneratorFactory.getDefault();

        assertNotNull(defaultFactory);

        int stateBits = defaultFactory.stateBits();
        assertTrue("stateBits() for default was " + stateBits, stateBits >= 64);
    }

    @Test
    @NonCts(reason = "Algorithm might not be available in the future")
    public void createsL64X256MixRandomInstance() {
        RandomGeneratorFactory<RandomGenerator> specificAlgoFactory =
                RandomGeneratorFactory.of("L64X256MixRandom");

        assertNotNull(specificAlgoFactory);
        // These are coming from package-summary page.
        assertEquals( "L64X256MixRandom", specificAlgoFactory.name());
        assertEquals("LXM", specificAlgoFactory.group());
        assertEquals(4, specificAlgoFactory.equidistribution());
        BigInteger expectedPeriod =
                BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE).shiftLeft(64);
        assertEquals(expectedPeriod, specificAlgoFactory.period());
        assertTrue(specificAlgoFactory.isSplittable());
        assertTrue(specificAlgoFactory.isStreamable());
    }

    @Test
    public void throwsOnUnsupported() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RandomGeneratorFactory.of("something something"));
        assertThrows(NullPointerException.class, () -> RandomGeneratorFactory.of(null));
    }

    /**
     * Goal is to ensure that META-INF/services/java.util.random.RandomGenerator is present and
     * is read.
     */
    @Test
    @NonCts(reason = "No guarantees are provided around available implementations")
    public void checkCurrentlyAvailableImplementations() {
        Set<String> actual = RandomGeneratorFactory.all()
                .map(RandomGeneratorFactory::name)
                .collect(toSet());

        Set<String> expected = Set.of(
                "L32X64MixRandom",
                "L64X128MixRandom",
                "L64X128StarStarRandom",
                "L64X256MixRandom",
                "L64X1024MixRandom",
                "L128X128MixRandom",
                "L128X256MixRandom",
                "L128X1024MixRandom",
                "Xoroshiro128PlusPlus",
                "Xoshiro256PlusPlus");

        assertEquals(expected, actual);
    }


}
