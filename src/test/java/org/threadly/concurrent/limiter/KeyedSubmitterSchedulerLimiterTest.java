package org.threadly.concurrent.limiter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.threadly.TestConstants.TEST_QTY;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.threadly.BlockingTestRunnable;
import org.threadly.concurrent.DoNothingRunnable;
import org.threadly.concurrent.PriorityScheduler;
import org.threadly.util.StringUtils;

@SuppressWarnings({"javadoc", "deprecation"})
public class KeyedSubmitterSchedulerLimiterTest {
  protected PriorityScheduler scheduler;
  
  @Before
  public void setup() {
    scheduler = new PriorityScheduler(10);
  }
  
  @After
  public void cleanup() {
    scheduler.shutdownNow();
    scheduler = null;
  }

  protected KeyedSubmitterSchedulerLimiter makeLimiter(int limit) {
    return new KeyedSubmitterSchedulerLimiter(scheduler, limit, null, true, 1);
  }
  
  @Test
  @SuppressWarnings("unused")
  public void constructorFail() {
    try {
      new KeyedSubmitterSchedulerLimiter(null, 10);
      fail("Exception should have thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }
    try {
      new KeyedSubmitterSchedulerLimiter(scheduler, 0);
      fail("Exception should have thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
  
  @Test
  public void getMaxConcurrencyPerKeyTest() {
    assertEquals(1, makeLimiter(1).getMaxConcurrencyPerKey());
    int val = 10;
    assertEquals(val, makeLimiter(val).getMaxConcurrencyPerKey());
  }
  
  @Test
  public void getUnsubmittedTaskCountTest() {
    KeyedSubmitterSchedulerLimiter singleConcurrencyLimiter = makeLimiter(1);
    String key = StringUtils.makeRandomString(5);
    BlockingTestRunnable btr = new BlockingTestRunnable();
    try {
      assertEquals(0, singleConcurrencyLimiter.getUnsubmittedTaskCount(key));
      singleConcurrencyLimiter.execute(key, btr);
      btr.blockTillStarted();
      // should not be queued any more
      assertEquals(0, singleConcurrencyLimiter.getUnsubmittedTaskCount(key));
      
      for (int i = 1; i < TEST_QTY; i++) {
        singleConcurrencyLimiter.submit(key, DoNothingRunnable.instance());
        assertEquals(i, singleConcurrencyLimiter.getUnsubmittedTaskCount(key));
      }
    } finally {
      btr.unblock();
    }
  }
  
  @Test (expected = IllegalArgumentException.class)
  public void getUnsubmittedTaskCountNullFail() {
    makeLimiter(1).getUnsubmittedTaskCount(null);
  }
  
  @Test
  public void getUnsubmittedTaskCountMapTest() {
    KeyedSubmitterSchedulerLimiter singleConcurrencyLimiter = makeLimiter(1);
    String key = StringUtils.makeRandomString(5);
    BlockingTestRunnable btr = new BlockingTestRunnable();
    try {
      assertTrue(singleConcurrencyLimiter.getUnsubmittedTaskCountMap().isEmpty());
      singleConcurrencyLimiter.execute(key, btr);
      btr.blockTillStarted();

      // should not be queued any more
      assertTrue(singleConcurrencyLimiter.getUnsubmittedTaskCountMap().isEmpty());
      
      for (int i = 1; i < TEST_QTY; i++) {
        singleConcurrencyLimiter.submit(key, DoNothingRunnable.instance());
        Map<?, ?> taskCountMap = singleConcurrencyLimiter.getUnsubmittedTaskCountMap();
        assertEquals(1, taskCountMap.size());
        assertEquals(i, taskCountMap.get(key));
      }
    } finally {
      btr.unblock();
    }
  }
}
