package org.threadly.concurrent.wrapper.intercepter;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import org.threadly.concurrent.SubmitterExecutor;
import org.threadly.concurrent.future.ListenableFuture;
import org.threadly.concurrent.future.ListenableFutureTask;
import org.threadly.util.ArgumentVerifier;

/**
 * <p>Class to wrap {@link Executor} pool so that tasks can be intercepted and either wrapped, or 
 * modified, before being submitted to the pool.  This abstract class needs to have 
 * {@link #wrapTask(Runnable)} overridden to provide the task which should be submitted to the 
 * {@link Executor}.  Please see the javadocs of {@link #wrapTask(Runnable)} for more details 
 * about ways a task can be modified or wrapped.</p>
 * 
 * <p>Other variants of task wrappers: {@link SubmitterSchedulerTaskIntercepter}, 
 * {@link SchedulerServiceTaskIntercepter}, {@link PrioritySchedulerTaskIntercepter}.</p>
 * 
 * @author jent - Mike Jensen
 * @since 4.6.0
 */
public abstract class ExecutorTaskIntercepter implements SubmitterExecutor {
  protected final Executor parentExecutor;
  
  protected ExecutorTaskIntercepter(Executor parentExecutor) {
    ArgumentVerifier.assertNotNull(parentExecutor, "parentExecutor");
    
    this.parentExecutor = parentExecutor;
  }
  
  /**
   * Implementation to modify a provided task.  The provided runnable will be the one submitted to 
   * the Executor, unless a {@link Callable} was submitted, in which case a 
   * {@link ListenableFutureTask} will be provided.  In the last condition the original callable 
   * can be retrieved by invoking {@link ListenableFutureTask#getContainedCallable()}.  The returned 
   * task can not be null, but could be either the original task, a modified task, a wrapper to the 
   * provided task, or if no action is desired 
   * {@link org.threadly.concurrent.DoNothingRunnable#instance()} may be provided.  However caution 
   * should be used in that if a {@link ListenableFutureTask} is provided, and then never returned 
   * (and not canceled), then the future will never complete (and thus possibly forever blocked).  
   * So if you are doing conditional checks for {@link ListenableFutureTask} and may not 
   * execute/return the provided task, then you should be careful to ensure 
   * {@link ListenableFutureTask#cancel(boolean)} is invoked.
   * 
   * Public visibility for javadoc visibility.   
   * 
   * @param task A runnable that was submitted for execution
   * @return A non-null task that will be provided to the parent executor
   */
  public abstract Runnable wrapTask(Runnable task);

  @Override
  public void execute(Runnable task) {
    parentExecutor.execute(task == null ? null : wrapTask(task));
  }

  @Override
  public ListenableFuture<?> submit(Runnable task) {
    return submit(task, null);
  }

  @Override
  public <T> ListenableFuture<T> submit(Runnable task, T result) {
    ArgumentVerifier.assertNotNull(task, "task");
    
    ListenableFutureTask<T> lft = new ListenableFutureTask<T>(false, wrapTask(task), result);
    
    parentExecutor.execute(lft);
    
    return lft;
  }

  @Override
  public <T> ListenableFuture<T> submit(Callable<T> task) {
    ArgumentVerifier.assertNotNull(task, "task");
    
    ListenableFutureTask<T> lft = new ListenableFutureTask<T>(false, task);

    parentExecutor.execute(wrapTask(lft));
    
    return lft;
  }
}