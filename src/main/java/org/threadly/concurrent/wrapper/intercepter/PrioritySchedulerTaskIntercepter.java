package org.threadly.concurrent.wrapper.intercepter;

import java.util.concurrent.Callable;

import org.threadly.concurrent.PrioritySchedulerService;
import org.threadly.concurrent.TaskPriority;
import org.threadly.concurrent.future.ListenableFuture;
import org.threadly.concurrent.future.ListenableFutureTask;
import org.threadly.util.ArgumentVerifier;

/**
 * <p>Class to wrap {@link PrioritySchedulerService} pool so that tasks can be intercepted and either 
 * wrapped, or modified, before being submitted to the pool.  This abstract class needs to have 
 * {@link #wrapTask(Runnable, boolean)} overridden to provide the task which should be submitted to the 
 * {@link PrioritySchedulerService}.  Please see the javadocs of {@link #wrapTask(Runnable, boolean)} for 
 * more details about ways a task can be modified or wrapped.</p>
 * 
 * <p>Other variants of task wrappers: {@link ExecutorTaskIntercepter}, 
 * {@link SubmitterSchedulerTaskIntercepter}, {@link PrioritySchedulerTaskIntercepter}.</p>
 * 
 * @author jent - Mike Jensen
 * @since 4.6.0
 */
public abstract class PrioritySchedulerTaskIntercepter extends SchedulerServiceTaskIntercepter 
                                                       implements PrioritySchedulerService {
  protected final PrioritySchedulerService parentScheduler;
  
  protected PrioritySchedulerTaskIntercepter(PrioritySchedulerService parentScheduler) {
    super(parentScheduler);
    
    this.parentScheduler = parentScheduler;
  }

  @Override
  public void execute(Runnable task, TaskPriority priority) {
    parentScheduler.execute(task == null ? null : wrapTask(task, false), priority);
  }

  @Override
  public ListenableFuture<?> submit(Runnable task, TaskPriority priority) {
    return submit(task, null, priority);
  }

  @Override
  public <T> ListenableFuture<T> submit(Runnable task, T result, TaskPriority priority) {
    return parentScheduler.submit(task == null ? null : wrapTask(task, false), result, priority);
  }

  @Override
  public <T> ListenableFuture<T> submit(Callable<T> task, TaskPriority priority) {
    ArgumentVerifier.assertNotNull(task, "task");
    
    ListenableFutureTask<T> lft = new ListenableFutureTask<T>(false, task);

    parentScheduler.execute(wrapTask(lft, false), priority);
    
    return lft;
  }

  @Override
  public void schedule(Runnable task, long delayInMs, TaskPriority priority) {
    parentScheduler.schedule(task == null ? null : wrapTask(task, false), delayInMs, priority);
  }

  @Override
  public ListenableFuture<?> submitScheduled(Runnable task, long delayInMs, TaskPriority priority) {
    return submitScheduled(task, null, delayInMs, priority);
  }

  @Override
  public <T> ListenableFuture<T> submitScheduled(Runnable task, T result, long delayInMs,
                                                 TaskPriority priority) {
    return parentScheduler.submitScheduled(task == null ? null : wrapTask(task, false), 
                                           result, delayInMs, priority);
  }

  @Override
  public <T> ListenableFuture<T> submitScheduled(Callable<T> task, long delayInMs,
                                                 TaskPriority priority) {
    ArgumentVerifier.assertNotNull(task, "task");
    
    ListenableFutureTask<T> lft = new ListenableFutureTask<T>(false, task);

    parentScheduler.schedule(wrapTask(lft, false), delayInMs, priority);
    
    return lft;
  }

  @Override
  public void scheduleWithFixedDelay(Runnable task, long initialDelay, long recurringDelay,
                                     TaskPriority priority) {
    parentScheduler.scheduleWithFixedDelay(task == null ? null : wrapTask(task, true), 
                                           initialDelay, recurringDelay, priority);
  }

  @Override
  public void scheduleAtFixedRate(Runnable task, long initialDelay, long period,
                                  TaskPriority priority) {
    parentScheduler.scheduleAtFixedRate(task == null ? null : wrapTask(task, true), 
                                        initialDelay, period, priority);
  }

  @Override
  public TaskPriority getDefaultPriority() {
    return parentScheduler.getDefaultPriority();
  }

  @Override
  public long getMaxWaitForLowPriority() {
    return parentScheduler.getMaxWaitForLowPriority();
  }
}
