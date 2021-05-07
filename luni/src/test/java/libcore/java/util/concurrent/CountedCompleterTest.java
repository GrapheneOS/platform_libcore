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
 * limitations under the License
 */

package libcore.java.util.concurrent;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CountedCompleterTest {

    /**
     * Exercises the completion of all tasks once one of them has a result.
     *
     * Instead of needing all tasks to be performed to get a result, this will only expect one of
     * them to provide it. That task, given by choiceIndex, will just complete the root completer.
     * All other tasks will remain "unfinished".
     *
     * The result is passed through setRawResult().
     */
    private static int chooseOne(Integer[] array, int choiceIndex) {
        class Task extends CountedCompleter<Integer> {
            final int lo;
            final int hi;
            AtomicInteger ai = new AtomicInteger(0);

            Task(Task parent, int lo, int hi) {
                super(parent);
                this.lo = lo;
                this.hi = hi;
            }

            @Override
            public void compute() {
                if (hi - lo >= 2) {
                    int mid = (lo + hi) >>> 1;
                    // must set pending count before fork
                    setPendingCount(2);
                    new Task(this, mid, hi).fork(); // right child
                    new Task(this, lo, mid).fork(); // left child
                } else if (hi > lo) {
                    if (choiceIndex == lo) {
                        final CountedCompleter root = getRoot();
                        final Integer val = Integer.valueOf(array[lo]);
                        if (root != null) {
                            root.complete(val);
                        } else {
                            complete(val); // the current task is the root
                        }
                    }
                }
            }

            public Integer getRawResult() {
                return new Integer(ai.intValue());
            }

            protected void setRawResult(Integer val) {
                ai.addAndGet(val.intValue());
            }
        }
        return new Task(null, 0, array.length).invoke().intValue();
    }

    /**
     * complete marks a task as complete regardless of the pending count.
     *
     * The test will only require one task to complete.
     */
    @Test
    public void testRecursiveChoice() {
        int n = 7;
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) {
            a[i] = i + 1;
        }
        for (int chosenOne = 0; chosenOne < n; ++chosenOne) {
            final int result = chooseOne(a, chosenOne);
            assertEquals(chosenOne + 1, result);
        }
    }

    /**
     * Forces a task to wait for its children to complete before calling tryComplete.
     *
     * This implementation makes use of helpComplete to ensure that all the children tasks are
     * resolved before completing itself.
     *
     * The function performs an action on all the elements of the array and it does so by creating
     * sub-tasks for each element of the array. For each element, the action is applied to it,
     * resolving the tasks.
     */
    private static void completeAllChildren(Integer[] array, Consumer<Integer> action) {
        class Task extends CountedCompleter<Integer> {
            final int idx;

            Task(CountedCompleter<Integer> parent, int idx) {
                super(parent);
                this.idx = idx;
            }

            @Override
            public void compute() {
                action.accept(array[idx]);

                if (getPendingCount() == 0) {
                    tryComplete();
                }
            }
        }

        class MainTask extends CountedCompleter<Integer> {
            final int lo;
            final int hi;

            MainTask(CountedCompleter<Integer> parent, int lo, int hi) {
                super(parent);
                this.lo = lo;
                this.hi = hi;
            }

            @Override
            public void compute() {
                for (int idx = lo; idx + 1 < hi; idx += 2) {
                    // must set pending count before fork
                    setPendingCount(2);
                    new Task(this, idx).fork();
                    new Task(this, idx+1).fork();
                    helpComplete(2);
                }

                if ( (hi - lo) % 2 == 1 ) {
                    action.accept(array[hi-1]);
                }

                if (getPendingCount() == 0) {
                    tryComplete();
                }
            }
        }

        new MainTask(null, 0, array.length).invoke();
    }

    /**
     * helpComplete attempts to process at most a given number of unprocessed children tasks.
     */
    @Test
    public void testHelpComplete() {
        int n = 7;
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) {
            a[i] = i + 1;
        }
        AtomicInteger ai = new AtomicInteger(0);
        // Use an atomic add as the action for each task. This will add all the elements of the
        // array into the ai variable. Since the elements are between 1 and 7, the number does not
        // overflow.
        completeAllChildren(a, ai::addAndGet);
        assertEquals(n * (n + 1) / 2, ai.get());
    }
}
