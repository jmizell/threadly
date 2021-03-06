package org.threadly.concurrent.future;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class AbstractFutureCallbackResultHandlerTest {
  @Test
  public void failureIgnoredTest() {
    final AtomicBoolean resultProvided = new AtomicBoolean(false);
    new AbstractFutureCallbackResultHandler<Object>() {
      @Override
      public void handleResult(Object result) {
        resultProvided.set(true);
      }
    }.handleFailure(new Exception());
    // no exception should throw or other weird behavior
    
    assertFalse(resultProvided.get());
  }
  
  @Test
  public void resultProvidedTest() {
    final Object testResult = new Object();
    final AtomicReference<Object> resultProvided = new AtomicReference<Object>(null);
    new AbstractFutureCallbackResultHandler<Object>() {
      @Override
      public void handleResult(Object result) {
        resultProvided.set(result);
      }
    }.handleResult(testResult);
    
    assertTrue(testResult == resultProvided.get());
  }
}
