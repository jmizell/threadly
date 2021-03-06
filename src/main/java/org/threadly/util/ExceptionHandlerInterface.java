package org.threadly.util;

/**
 * <p>Interface for implementation to handle exceptions which occur.  This is similar to 
 * {@link java.lang.Thread.UncaughtExceptionHandler}, except that exceptions provided to this 
 * interface are handled on the same thread that threw the exception, and the thread that threw it 
 * likely WONT die.</p>
 * 
 * @deprecated Please use {@link ExceptionHandler}
 * 
 * @author jent - Mike Jensen
 * @since 2.4.0
 */
@Deprecated
public interface ExceptionHandlerInterface extends ExceptionHandler {
  // nothing to be removed with this deprecated interface
}
