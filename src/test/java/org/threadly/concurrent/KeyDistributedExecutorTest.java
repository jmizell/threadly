package org.threadly.concurrent;

import static org.junit.Assert.*;
import static org.threadly.TestConstants.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.threadly.BlockingTestRunnable;
import org.threadly.concurrent.lock.StripedLock;
import org.threadly.concurrent.wrapper.limiter.ExecutorLimiter;
import org.threadly.test.concurrent.TestCondition;
import org.threadly.test.concurrent.TestRunnable;
import org.threadly.test.concurrent.TestUtils;

@SuppressWarnings({"javadoc", "deprecation"})
public class KeyDistributedExecutorTest extends org.threadly.concurrent.wrapper.KeyDistributedExecutorTest {
  private static final int PARALLEL_LEVEL = TEST_QTY;
  private static final int RUNNABLE_COUNT_PER_LEVEL = TEST_QTY * 2;
  
  private KeyDistributedExecutor localDistributor;
  
  @Before
  @Override
  public void setup() {
    StripedLock sLock = new StripedLock(1);
    agentLock = sLock.getLock(null);  // there should be only one lock
    localDistributor = new KeyDistributedExecutor(executor, sLock, Integer.MAX_VALUE, false);
    super.distributor = localDistributor;
  }
  
  @After
  @Override
  public void cleanup() {
    localDistributor = null;
    super.cleanup();
  }
  
  @Test
  @Override
  @SuppressWarnings("unused")
  public void constructorTest() {
    // none should throw exception
    new KeyDistributedExecutor(executor);
    new KeyDistributedExecutor(executor, true);
    new KeyDistributedExecutor(executor, 1);
    new KeyDistributedExecutor(executor, 1, true);
    new KeyDistributedExecutor(1, executor);
    new KeyDistributedExecutor(1, executor, true);
    new KeyDistributedExecutor(1, executor, 1);
    new KeyDistributedExecutor(1, executor, 1, true);
    StripedLock sLock = new StripedLock(1);
    new KeyDistributedExecutor(executor, sLock, 1, false);
  }
  
  @Test
  @Override
  @SuppressWarnings("unused")
  public void constructorFail() {
    try {
      new KeyDistributedExecutor(1, null);
      fail("Exception should have been thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }
    try {
      new KeyDistributedExecutor(executor, null, 
                                 Integer.MAX_VALUE, false);
      fail("Exception should have been thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }
    try {
      new KeyDistributedExecutor(executor, new StripedLock(1), -1, false);
      fail("Exception should have been thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
  
  @Test (expected = IllegalArgumentException.class)
  public void getSubmitterForKeyFail() {
    localDistributor.getSubmitterForKey(null);
  }
  
  @Test
  public void addTaskFail() {
    try {
      localDistributor.addTask(null, DoNothingRunnable.instance());
      fail("Exception should have been thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }

    try {
      localDistributor.addTask(new Object(), null);
      fail("Exception should have been thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
  
  @Test
  public void addTaskConsistentThreadTest() {
    List<KDRunnable> runs = populate(new AddHandler() {
      @Override
      public void addTDRunnable(Object key, KDRunnable tdr) {
        localDistributor.addTask(key, tdr);
      }
    });

    Iterator<KDRunnable> it = runs.iterator();
    while (it.hasNext()) {
      KDRunnable tr = it.next();
      tr.blockTillFinished(20 * 1000);
      assertEquals(1, tr.getRunCount()); // verify each only ran once
      assertTrue(tr.threadTracker.threadConsistent());  // verify that all threads for a given key ran in the same thread
      assertTrue(tr.previousRanFirst());  // verify runnables were run in order
    }
  }
  
  @Test
  public void submitTaskRunnableFail() {
    try {
      localDistributor.submitTask(null, DoNothingRunnable.instance());
      fail("Exception should have thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }
    try {
      localDistributor.submitTask(new Object(), null, null);
      fail("Exception should have thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
  
  @Test
  public void submitTaskRunnableConsistentThreadTest() {
    List<KDRunnable> runs = populate(new AddHandler() {
      @Override
      public void addTDRunnable(Object key, KDRunnable tdr) {
        localDistributor.submitTask(key, tdr);
      }
    });
    
    Iterator<KDRunnable> it = runs.iterator();
    while (it.hasNext()) {
      KDRunnable tr = it.next();
      tr.blockTillFinished(20 * 1000);
      assertEquals(1, tr.getRunCount()); // verify each only ran once
      assertTrue(tr.threadTracker.threadConsistent());  // verify that all threads for a given key ran in the same thread
      assertTrue(tr.previousRanFirst());  // verify runnables were run in order
    }
  }
  
  @Test
  public void submitTaskCallableConsistentThreadTest() {
    List<KDCallable> runs = new ArrayList<KDCallable>(PARALLEL_LEVEL * RUNNABLE_COUNT_PER_LEVEL);
    
    // hold agent lock to avoid execution till all are submitted
    synchronized (agentLock) {
      for (int i = 0; i < PARALLEL_LEVEL; i++) {
        ThreadContainer tc = new ThreadContainer();
        KDCallable previous = null;
        for (int j = 0; j < RUNNABLE_COUNT_PER_LEVEL; j++) {
          KDCallable tr = new KDCallable(tc, previous);
          runs.add(tr);
          localDistributor.submitTask(tc, tr);
          
          previous = tr;
        }
      }
    }
    
    Iterator<KDCallable> it = runs.iterator();
    while (it.hasNext()) {
      KDCallable tr = it.next();
      tr.blockTillFinished(20 * 1000);
      assertTrue(tr.threadTracker.threadConsistent());  // verify that all threads for a given key ran in the same thread
      assertTrue(tr.previousRanFirst());  // verify runnables were run in order
    }
  }
  
  @Test
  public void submitTaskCallableFail() {
    try {
      localDistributor.submitTask(null, new TestCallable());
      fail("Exception should have thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }
    try {
      localDistributor.submitTask(new Object(), (Callable<Void>)null);
      fail("Exception should have thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
  
  @Test
  @Override
  public void limitExecutionPerCycleStressTest() {
    PriorityScheduler scheduler = new StrictPriorityScheduler(3);
    final AtomicBoolean testComplete = new AtomicBoolean(false);
    try {
      final Integer key1 = 1;
      final Integer key2 = 2;
      Executor singleThreadedExecutor = new ExecutorLimiter(scheduler, 1);
      final KeyDistributedExecutor distributor = new KeyDistributedExecutor(2, singleThreadedExecutor, 2);
      final AtomicInteger waitingTasks = new AtomicInteger();
      final AtomicReference<TestRunnable> lastTestRunnable = new AtomicReference<TestRunnable>();
      scheduler.execute(new Runnable() {  // execute thread to add for key 1
        @Override
        public void run() {
          while (! testComplete.get()) {
            TestRunnable next = new TestRunnable() {
              @Override
              public void handleRunStart() {
                waitingTasks.decrementAndGet();
                
                TestUtils.sleep(20);  // wait to make sure producer is faster than executor
              }
            };
            lastTestRunnable.set(next);
            waitingTasks.incrementAndGet();
            distributor.execute(key1, next);
          }
        }
      });
      
      // block till there is for sure a backup of key1 tasks
      new TestCondition() {
        @Override
        public boolean get() {
          return waitingTasks.get() > 10;
        }
      }.blockTillTrue();
      
      TestRunnable key2Runnable = new TestRunnable();
      distributor.execute(key2, key2Runnable);
      TestRunnable lastKey1Runnable = lastTestRunnable.get();
      key2Runnable.blockTillStarted();  // will throw exception if not started
      // verify it ran before the lastKey1Runnable
      assertFalse(lastKey1Runnable.ranOnce());
    } finally {
      testComplete.set(true);
      scheduler.shutdownNow();
    }
  }
  
  private static void getTaskQueueSizeSimpleTest(boolean accurateDistributor) {
    final Object taskKey = new Object();
    KeyDistributedExecutor kde = new KeyDistributedExecutor(new Executor() {
      @Override
      public void execute(Runnable command) {
        // kidding, don't actually execute, haha
      }
    }, accurateDistributor);
    
    assertEquals(0, kde.getTaskQueueSize(taskKey));
    
    kde.execute(taskKey, DoNothingRunnable.instance());
    
    // should add as first task
    assertEquals(1, kde.getTaskQueueSize(taskKey));
    
    kde.execute(taskKey, DoNothingRunnable.instance());
    
    // will now add into the queue
    assertEquals(2, kde.getTaskQueueSize(taskKey));
  }
  
  private static void getTaskQueueSizeThreadedTest(boolean accurateDistributor) {
    final Object taskKey = new Object();
    KeyDistributedExecutor kde = new KeyDistributedExecutor(executor, accurateDistributor);
    
    assertEquals(0, kde.getTaskQueueSize(taskKey));
    
    BlockingTestRunnable btr = new BlockingTestRunnable();
    kde.execute(taskKey, btr);
    
    // add more tasks while remaining blocked
    kde.execute(taskKey, DoNothingRunnable.instance());
    kde.execute(taskKey, DoNothingRunnable.instance());
    
    btr.blockTillStarted();
    
    assertEquals(2, kde.getTaskQueueSize(taskKey));
    
    btr.unblock();
  }
  
  @Test
  @Override
  public void getTaskQueueSizeInaccurateTest() {
    getTaskQueueSizeSimpleTest(false);
    getTaskQueueSizeThreadedTest(false);
  }
  
  @Test
  @Override
  public void getTaskQueueSizeAccurateTest() {
    getTaskQueueSizeSimpleTest(true);
    getTaskQueueSizeThreadedTest(true);
  }
  
  private static void getTaskQueueSizeMapSimpleTest(boolean accurateDistributor) {
    final Object taskKey = new Object();
    KeyDistributedExecutor kde = new KeyDistributedExecutor(new Executor() {
      @Override
      public void execute(Runnable command) {
        // kidding, don't actually execute, haha
      }
    }, accurateDistributor);
    
    Map<?, Integer> result = kde.getTaskQueueSizeMap();
    assertTrue(result.isEmpty());
    
    kde.execute(taskKey, DoNothingRunnable.instance());
    
    // should add as first task
    result = kde.getTaskQueueSizeMap();
    assertEquals(1, result.size());
    assertEquals((Integer)1, result.get(taskKey));
    
    kde.execute(taskKey, DoNothingRunnable.instance());
    
    // will now add into the queue
    result = kde.getTaskQueueSizeMap();
    assertEquals(1, result.size());
    assertEquals((Integer)2, result.get(taskKey));
  }
  
  private static void getTaskQueueSizeMapThreadedTest(boolean accurateDistributor) {
    final Object taskKey = new Object();
    KeyDistributedExecutor kde = new KeyDistributedExecutor(executor, accurateDistributor);

    Map<?, Integer> result = kde.getTaskQueueSizeMap();
    assertTrue(result.isEmpty());
    
    BlockingTestRunnable btr = new BlockingTestRunnable();
    kde.execute(taskKey, btr);
    
    // add more tasks while remaining blocked
    kde.execute(taskKey, DoNothingRunnable.instance());
    kde.execute(taskKey, DoNothingRunnable.instance());
    
    btr.blockTillStarted();
    
    result = kde.getTaskQueueSizeMap();
    assertEquals(1, result.size());
    assertEquals((Integer)2, result.get(taskKey));
    
    btr.unblock();
  }
  
  @Test
  @Override
  public void getTaskQueueSizeMapInaccurateTest() {
    getTaskQueueSizeMapSimpleTest(false);
    getTaskQueueSizeMapThreadedTest(false);
  }
  
  @Test
  @Override
  public void getTaskQueueSizeMapAccurateTest() {
    getTaskQueueSizeMapSimpleTest(true);
    getTaskQueueSizeMapThreadedTest(true);
  }
}
