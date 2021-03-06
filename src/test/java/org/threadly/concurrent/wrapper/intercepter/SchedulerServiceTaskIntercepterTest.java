package org.threadly.concurrent.wrapper.intercepter;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.threadly.concurrent.DoNothingRunnable;
import org.threadly.concurrent.SchedulerService;
import org.threadly.test.concurrent.TestRunnable;
import org.threadly.test.concurrent.TestableScheduler;

@SuppressWarnings("javadoc")
public class SchedulerServiceTaskIntercepterTest extends SubmitterSchedulerTaskIntercepterTest {
  @Before
  @Override
  public void setup() {
    scheduler = new TestableScheduler();
    submitterSchedulerInterceptor = new TestSchedulerServiceInterceptor(scheduler);
    executorInterceptor = submitterSchedulerInterceptor;
    testInterceptor = (TestInterceptor)executorInterceptor;
    tr = new TestRunnable();
  }

  private static class TestSchedulerServiceInterceptor extends SchedulerServiceTaskIntercepter 
                                                       implements TestInterceptor {
    private final List<Runnable> interceptedTasks;
    
    public TestSchedulerServiceInterceptor(SchedulerService parentScheduler) {
      super(parentScheduler);
      
      interceptedTasks = new ArrayList<Runnable>(1);
    }

    @Override
    public List<Runnable> getInterceptedTasks() {
      return interceptedTasks;
    }

    @Override
    public Runnable wrapTask(Runnable task, boolean recurring) {
      interceptedTasks.add(task);
      
      return DoNothingRunnable.instance();
    }
  }
}
