package org.threadly.concurrent;

/**
 * <p>Interface to implement if any classes are containing a runnable.  This interface must be 
 * implemented in order for the {@link PriorityScheduler} (and others) remove function to work 
 * correctly if that wrapper is ever provided to the thread pool.</p>
 * 
 * @deprecated Please use {@link RunnableContainer}
 * 
 * @author jent - Mike Jensen
 * @since 1.0.0
 */
@Deprecated
public interface RunnableContainerInterface extends RunnableContainer {
  // nothing to be removed with this deprecated interface
}
