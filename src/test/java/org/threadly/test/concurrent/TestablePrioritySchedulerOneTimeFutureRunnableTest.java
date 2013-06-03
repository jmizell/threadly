package org.threadly.test.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.threadly.concurrent.FutureTest;
import org.threadly.concurrent.TaskPriority;
import org.threadly.concurrent.FutureTest.FutureFactory;
import org.threadly.concurrent.lock.VirtualLock;
import org.threadly.test.concurrent.TestablePriorityScheduler.OneTimeFutureRunnable;

@SuppressWarnings("javadoc")
public class TestablePrioritySchedulerOneTimeFutureRunnableTest {
  private static final Executor executor = Executors.newFixedThreadPool(10);
  
  @Test
  public void blockTillCompletedTest() {
    FutureTest.blockTillCompletedTest(new Factory());
  }
  
  @Test
  public void blockTillCompletedFail() {
    FutureTest.blockTillCompletedFail(new Factory());
  }
  
  @Test
  public void getTimeoutFail() throws InterruptedException, ExecutionException {
    FutureTest.getTimeoutFail(new Factory());
  }
  
  @Test
  public void cancelTest() {
    FutureTest.cancelTest(new Factory());
  }
  
  @Test
  public void isDoneTest() {
    FutureTest.isDoneTest(new Factory());
  }
  
  @Test
  public void isDoneFail() {
    FutureTest.isDoneFail(new Factory());
  }
  
  private class Factory implements FutureFactory {
    @Override
    public Future<?> make(Runnable run, VirtualLock lock) {
      return new FutureRunnable<Object>(run, lock);
    }

    @Override
    public <T> Future<T> make(Callable<T> callable, VirtualLock lock) {
      return new FutureRunnable<T>(callable, lock);
    }
  }
  
  private class FutureRunnable<T> implements Future<T>, Runnable {
    private final TestablePriorityScheduler scheduler;
    private final OneTimeFutureRunnable<T> otfr;
    
    private FutureRunnable(Runnable run, VirtualLock lock) {
      scheduler = new TestablePriorityScheduler(executor, 
                                                TaskPriority.High);
      this.otfr = scheduler.new OneTimeFutureRunnable<T>(run, 0, 
                                                         TaskPriority.High, 
                                                         lock);
    }
    
    private FutureRunnable(Callable<T> callable, VirtualLock lock) {
      scheduler = new TestablePriorityScheduler(executor, 
                                                TaskPriority.High);
      this.otfr = scheduler.new OneTimeFutureRunnable<T>(callable, 0, 
                                                         TaskPriority.High, 
                                                         lock);
    }

    @Override
    public void run() {
      otfr.run(scheduler);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return otfr.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
      return otfr.isCancelled();
    }

    @Override
    public boolean isDone() {
      return otfr.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
      return otfr.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException,
                                                     ExecutionException,
                                                     TimeoutException {
      return otfr.get(timeout, unit);
    }
  }
}