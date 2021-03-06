package org.threadly.concurrent.limiter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.threadly.TestConstants.TEST_QTY;

import java.util.concurrent.RejectedExecutionException;

import org.junit.Test;
import org.threadly.concurrent.DoNothingRunnable;
import org.threadly.concurrent.PrioritySchedulerTest.PrioritySchedulerFactory;
import org.threadly.concurrent.SchedulerService;
import org.threadly.concurrent.SchedulerServiceInterfaceTest;
import org.threadly.concurrent.SubmitterExecutor;
import org.threadly.concurrent.SubmitterScheduler;
import org.threadly.test.concurrent.TestableScheduler;

@SuppressWarnings({"javadoc", "deprecation"})
public class SchedulerServiceQueueLimitRejectorTest extends SchedulerServiceInterfaceTest {
  @Override
  protected SchedulerServiceFactory getSchedulerServiceFactory() {
    return new SchedulerServiceQueueRejectorFactory();
  }
  
  @SuppressWarnings("unused")
  @Test (expected = IllegalArgumentException.class)
  public void constructorFail() {
    new SchedulerServiceQueueLimitRejector(null, TEST_QTY);
  }
  
  @Test
  public void getCurrentQueueSizeTest() {
    TestableScheduler testableScheduler = new TestableScheduler();
    SchedulerServiceQueueLimitRejector queueRejector = new SchedulerServiceQueueLimitRejector(testableScheduler, TEST_QTY);

    for (int i = 0; i < TEST_QTY; i++) {
      assertEquals(i, queueRejector.getCurrentQueueSize());
      queueRejector.execute(DoNothingRunnable.instance());
    }
    
    testableScheduler.tick();

    assertEquals(0, queueRejector.getCurrentQueueSize());
  }
  
  @Test
  public void getSetQueueLimitTest() {
    TestableScheduler testableScheduler = new TestableScheduler();
    SchedulerServiceQueueLimitRejector queueRejector = new SchedulerServiceQueueLimitRejector(testableScheduler, TEST_QTY);
    
    assertEquals(TEST_QTY, queueRejector.getQueueLimit());
    
    queueRejector.setQueueLimit(TEST_QTY * 2);
    assertEquals(TEST_QTY * 2, queueRejector.getQueueLimit());
  }
  
  @Test
  public void getQueuedTaskCountTest() {
    TestableScheduler testableScheduler = new TestableScheduler();
    SchedulerServiceQueueLimitRejector queueRejector = new SchedulerServiceQueueLimitRejector(testableScheduler, TEST_QTY);

    for (int i = 0; i < TEST_QTY; i++) {
      assertEquals(i, queueRejector.getQueuedTaskCount());
      queueRejector.execute(DoNothingRunnable.instance());
    }
    
    testableScheduler.tick();

    assertEquals(0, queueRejector.getQueuedTaskCount());
  }
  
  @Test
  public void getScheduledTaskCountTest() {
    TestableScheduler testableScheduler = new TestableScheduler();
    SchedulerServiceQueueLimitRejector queueRejector = new SchedulerServiceQueueLimitRejector(testableScheduler, TEST_QTY);

    for (int i = 0; i < TEST_QTY; i++) {
      assertEquals(i, queueRejector.getScheduledTaskCount());
      queueRejector.execute(DoNothingRunnable.instance());
    }
    
    testableScheduler.tick();

    assertEquals(0, queueRejector.getScheduledTaskCount());
  }
  
  @Test
  public void rejectTest() {
    TestableScheduler testableScheduler = new TestableScheduler();
    SchedulerServiceQueueLimitRejector queueRejector = new SchedulerServiceQueueLimitRejector(testableScheduler, TEST_QTY);
    
    for (int i = 0; i < TEST_QTY; i++) {
      queueRejector.execute(DoNothingRunnable.instance());
    }
    
    try {
      queueRejector.execute(DoNothingRunnable.instance());
      fail("Exception should have thrown");
    } catch (RejectedExecutionException e) {
      // expected
    }
    
    // verify the task was never added
    assertEquals(TEST_QTY, testableScheduler.tick());
    
    // we should be able to add again now
    for (int i = 0; i < TEST_QTY; i++) {
      queueRejector.execute(DoNothingRunnable.instance());
    }
  }
  
  private static class SchedulerServiceQueueRejectorFactory implements SchedulerServiceFactory {
    private final PrioritySchedulerFactory schedulerFactory = new PrioritySchedulerFactory();

    @Override
    public SubmitterExecutor makeSubmitterExecutor(int poolSize, boolean prestartIfAvailable) {
      return makeSubmitterScheduler(poolSize, prestartIfAvailable);
    }
    
    @Override
    public SubmitterScheduler makeSubmitterScheduler(int poolSize, boolean prestartIfAvailable) {
      return makeSchedulerService(poolSize, prestartIfAvailable);
    }

    @Override
    public SchedulerService makeSchedulerService(int poolSize, boolean prestartIfAvailable) {
      SchedulerService scheduler = schedulerFactory.makeSchedulerService(poolSize, prestartIfAvailable);
      
      return new SchedulerServiceQueueLimitRejector(scheduler, Integer.MAX_VALUE);
    }

    @Override
    public void shutdown() {
      schedulerFactory.shutdown();
    }
  }
}
