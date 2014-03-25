package org.threadly.concurrent.future;

import static org.junit.Assert.*;
import static org.threadly.TestConstants.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.threadly.ThreadlyTestUtil;
import org.threadly.concurrent.PriorityScheduledExecutor;
import org.threadly.concurrent.StrictPriorityScheduledExecutor;
import org.threadly.concurrent.TestRuntimeFailureRunnable;
import org.threadly.test.concurrent.AsyncVerifier;
import org.threadly.test.concurrent.TestRunnable;

@SuppressWarnings("javadoc")
public class FutureUtilsTest {
  private static PriorityScheduledExecutor scheduler;
  
  @BeforeClass
  public static void setupClass() {
    scheduler = new StrictPriorityScheduledExecutor(1, 1, 1000);
    
    ThreadlyTestUtil.setDefaultUncaughtExceptionHandler();
  }
  
  @AfterClass
  public static void tearDownClass() {
    scheduler.shutdownNow();
    scheduler = null;
  }
  
  private static List<ListenableFuture<?>> makeFutures(int count, int errorIndex) {
    List<ListenableFuture<?>> result = new ArrayList<ListenableFuture<?>>(count + 1);
    
    for (int i = 0; i < count; i++) {
      TestRunnable tr;
      if (i == errorIndex) {
        tr = new TestRuntimeFailureRunnable(DELAY_TIME);
      } else {
        tr = new TestRunnable(DELAY_TIME);
      }
      result.add(scheduler.submit(tr));
    }
    
    return result;
  }
  
  @Test
  public void blockTillAllCompleteNullTest() throws InterruptedException {
    FutureUtils.blockTillAllComplete(null); // should return immediately
  }
  
  @Test
  public void blockTillAllCompleteTest() throws InterruptedException {
    List<ListenableFuture<?>> futures = makeFutures(TEST_QTY, -1);
    
    FutureUtils.blockTillAllComplete(futures);
    
    Iterator<ListenableFuture<?>> it = futures.iterator();
    while (it.hasNext()) {
      assertTrue(it.next().isDone());
    }
  }
  
  @Test
  public void blockTillAllCompleteErrorTest() throws InterruptedException {
    int errorIndex = TEST_QTY / 2;
    
    List<ListenableFuture<?>> futures = makeFutures(TEST_QTY, errorIndex);
    
    FutureUtils.blockTillAllComplete(futures);
    
    Iterator<ListenableFuture<?>> it = futures.iterator();
    while (it.hasNext()) {
      assertTrue(it.next().isDone());
    }
  }
  
  @Test
  public void blockTillAllCompleteOrFirstErrorNullTest() throws InterruptedException, ExecutionException {
    FutureUtils.blockTillAllCompleteOrFirstError(null); // should return immediately
  }
  
  @Test
  public void blockTillAllCompleteOrFirstErrorTest() throws InterruptedException, ExecutionException {
    List<ListenableFuture<?>> futures = makeFutures(TEST_QTY, -1);
    
    FutureUtils.blockTillAllCompleteOrFirstError(futures);
    
    Iterator<ListenableFuture<?>> it = futures.iterator();
    while (it.hasNext()) {
      assertTrue(it.next().isDone());
    }
  }
  
  @Test
  public void blockTillAllCompleteOrFirstErrorErrorTest() throws InterruptedException {
    int errorIndex = TEST_QTY / 2;
    
    List<ListenableFuture<?>> futures = makeFutures(TEST_QTY, errorIndex);
    
    FutureUtils.blockTillAllComplete(futures);

    Iterator<ListenableFuture<?>> it = futures.iterator();
    for (int i = 0; i <= errorIndex; i++) {
      Future<?> f = it.next();
      
      if (i < errorIndex) {
        assertTrue(f.isDone());
      } else if (i == errorIndex) {
        try {
          f.get();
          fail("Exception should have thrown");
        } catch (ExecutionException e) {
          // expected
        }
      }
    }
  }
  
  @Test
  public void makeCompleteFutureNullTest() {
    ListenableFuture<?> f = FutureUtils.makeCompleteFuture(null);
    
    assertTrue(f.isDone());
  }
  
  @Test
  public void makeCompleteFutureEmptyListTest() {
    List<ListenableFuture<?>> futures = Collections.emptyList();
    ListenableFuture<?> f = FutureUtils.makeCompleteFuture(futures);
    
    assertTrue(f.isDone());
  }
  
  @Test
  public void makeCompleteFutureAlreadyDoneFuturesTest() {
    List<ListenableFuture<?>> futures = new ArrayList<ListenableFuture<?>>(TEST_QTY);
    
    for (int i = 0; i < TEST_QTY; i++) {
      ListenableFutureResult<?> future = new ListenableFutureResult<Object>();
      future.setResult(null);
      futures.add(future);
    }

    ListenableFuture<?> f = FutureUtils.makeCompleteFuture(futures);
    
    assertTrue(f.isDone());
  }
  
  @Test
  public void makeCompleteFutureTest() throws InterruptedException, TimeoutException {
    makeCompleteFutureTest(-1);
  }
  
  @Test
  public void makeCompleteFutureWithErrorTest() throws InterruptedException, TimeoutException {
    makeCompleteFutureTest(TEST_QTY / 2);
  }
  
  private static void makeCompleteFutureTest(int errorIndex) throws InterruptedException, TimeoutException {
    final List<ListenableFuture<?>> futures = makeFutures(TEST_QTY, errorIndex);

    final ListenableFuture<?> f = FutureUtils.makeCompleteFuture(futures);
    
    final AsyncVerifier av = new AsyncVerifier();
    f.addListener(new Runnable() {
      @Override
      public void run() {
        av.assertTrue(f.isDone());
        
        Iterator<ListenableFuture<?>> it = futures.iterator();
        while (it.hasNext()) {
          av.assertTrue(it.next().isDone());
        }
        
        av.signalComplete();
      }
    });
    
    av.waitForTest();
  }
  
  @Test
  public void makeCompleteListFutureNullTest() {
    ListenableFuture<List<ListenableFuture<?>>> f = FutureUtils.makeCompleteListFuture(null);
    
    assertTrue(f.isDone());
  }
  
  @Test
  public void makeCompleteListFutureEmptyListTest() {
    List<ListenableFuture<?>> futures = Collections.emptyList();
    ListenableFuture<List<ListenableFuture<?>>> f = FutureUtils.makeCompleteListFuture(futures);
    
    assertTrue(f.isDone());
  }
  
  @Test
  public void makeCompleteListFutureAlreadyDoneFuturesTest() throws InterruptedException, ExecutionException {
    List<ListenableFuture<?>> futures = new ArrayList<ListenableFuture<?>>(TEST_QTY);
    
    for (int i = 0; i < TEST_QTY; i++) {
      ListenableFutureResult<?> future = new ListenableFutureResult<Object>();
      if (i == TEST_QTY / 2) {
        future.setFailure(null);
      } else {
        future.setResult(null);
      }
      futures.add(future);
    }

    ListenableFuture<List<ListenableFuture<?>>> f = FutureUtils.makeCompleteListFuture(futures);
    
    assertTrue(f.isDone());
    
    verifyAllIncluded(futures, f.get(), null);
  }
  
  private static void verifyAllIncluded(List<ListenableFuture<?>> expected, 
                                        List<ListenableFuture<?>> result, 
                                        ListenableFuture<?> excludedFuture) {
    Iterator<ListenableFuture<?>> it = expected.iterator();
    while (it.hasNext()) {
      ListenableFuture<?> f = it.next();
      if (f != excludedFuture) {
        assertTrue(result.contains(f));
      }
    }
    
    assertFalse(result.contains(excludedFuture));
  }
  
  @Test
  public void makeCompleteListFutureTest() throws ExecutionException, InterruptedException, TimeoutException {
    makeCompleteListFutureTest(-1);
  }
  
  @Test
  public void makeCompleteListFutureWithErrorTest() throws ExecutionException, InterruptedException, TimeoutException {
    makeCompleteListFutureTest(TEST_QTY / 2);
  }
  
  private static void makeCompleteListFutureTest(int errorIndex) throws ExecutionException, InterruptedException, TimeoutException {
    final List<ListenableFuture<?>> futures = makeFutures(TEST_QTY, errorIndex);

    final ListenableFuture<List<ListenableFuture<?>>> f = FutureUtils.makeCompleteListFuture(futures);
    
    final AsyncVerifier av = new AsyncVerifier();
    f.addListener(new Runnable() {
      @Override
      public void run() {
        av.assertTrue(f.isDone());
        
        Iterator<ListenableFuture<?>> it = futures.iterator();
        while (it.hasNext()) {
          av.assertTrue(it.next().isDone());
        }
        
        av.signalComplete();
      }
    });
    
    av.waitForTest();
    
    verifyAllIncluded(futures, f.get(), null);
  }
  
  @Test
  public void makeSuccessListFutureNullTest() {
    ListenableFuture<List<ListenableFuture<?>>> f = FutureUtils.makeSuccessListFuture(null);
    
    assertTrue(f.isDone());
  }
  
  @Test
  public void makeSuccessListFutureEmptyListTest() {
    List<ListenableFuture<?>> futures = Collections.emptyList();
    ListenableFuture<List<ListenableFuture<?>>> f = FutureUtils.makeSuccessListFuture(futures);
    
    assertTrue(f.isDone());
  }
  
  @Test
  public void makeSuccessListFutureAlreadyDoneFuturesTest() throws InterruptedException, ExecutionException {
    List<ListenableFuture<?>> futures = new ArrayList<ListenableFuture<?>>(TEST_QTY);
    ListenableFuture<?> failureFuture = null;
    
    for (int i = 0; i < TEST_QTY; i++) {
      ListenableFutureResult<?> future = new ListenableFutureResult<Object>();
      if (i == TEST_QTY / 2) {
        failureFuture = future;
        future.setFailure(null);
      } else {
        future.setResult(null);
      }
      futures.add(future);
    }

    ListenableFuture<List<ListenableFuture<?>>> f = FutureUtils.makeSuccessListFuture(futures);
    
    assertTrue(f.isDone());
    
    verifyAllIncluded(futures, f.get(), failureFuture);
  }
  
  @Test
  public void makeSuccessListFutureWithErrorTest() throws ExecutionException, InterruptedException, TimeoutException {
    final List<ListenableFuture<?>> futures = makeFutures(TEST_QTY, -1);
    ListenableFutureResult<?> failureFuture = new ListenableFutureResult<Object>();
    failureFuture.setFailure(null);
    futures.add(failureFuture);

    final ListenableFuture<List<ListenableFuture<?>>> f = FutureUtils.makeSuccessListFuture(futures);
    
    final AsyncVerifier av = new AsyncVerifier();
    f.addListener(new Runnable() {
      @Override
      public void run() {
        av.assertTrue(f.isDone());
        
        Iterator<ListenableFuture<?>> it = futures.iterator();
        while (it.hasNext()) {
          av.assertTrue(it.next().isDone());
        }
        
        av.signalComplete();
      }
    });
    
    av.waitForTest();
    
    verifyAllIncluded(futures, f.get(), failureFuture);
  }
  
  @Test
  public void makeFailureListFutureNullTest() {
    ListenableFuture<List<ListenableFuture<?>>> f = FutureUtils.makeFailureListFuture(null);
    
    assertTrue(f.isDone());
  }
  
  @Test
  public void makeFailureListFutureEmptyListTest() {
    List<ListenableFuture<?>> futures = Collections.emptyList();
    ListenableFuture<List<ListenableFuture<?>>> f = FutureUtils.makeFailureListFuture(futures);
    
    assertTrue(f.isDone());
  }
  
  @Test
  public void makeFailureListFutureAlreadyDoneFuturesTest() throws InterruptedException, ExecutionException {
    List<ListenableFuture<?>> futures = new ArrayList<ListenableFuture<?>>(TEST_QTY);
    ListenableFuture<?> failureFuture = null;
    
    for (int i = 0; i < TEST_QTY; i++) {
      ListenableFutureResult<?> future = new ListenableFutureResult<Object>();
      if (i == TEST_QTY / 2) {
        failureFuture = future;
        future.setFailure(null);
      } else {
        future.setResult(null);
      }
      futures.add(future);
    }

    ListenableFuture<List<ListenableFuture<?>>> f = FutureUtils.makeFailureListFuture(futures);
    
    assertTrue(f.isDone());
    
    verifyNoneIncluded(futures, f.get(), failureFuture);
  }
  
  private static void verifyNoneIncluded(List<ListenableFuture<?>> exempt, 
                                         List<ListenableFuture<?>> result, 
                                         ListenableFuture<?> includedFuture) {
    Iterator<ListenableFuture<?>> it = exempt.iterator();
    while (it.hasNext()) {
      ListenableFuture<?> f = it.next();
      if (f != includedFuture) {
        assertFalse(result.contains(f));
      }
    }
    
    assertTrue(result.contains(includedFuture));
  }
  
  @Test
  public void makeFailureListFutureWithErrorTest() throws ExecutionException, InterruptedException, TimeoutException {
    final List<ListenableFuture<?>> futures = makeFutures(TEST_QTY, -1);
    ListenableFutureResult<?> failureFuture = new ListenableFutureResult<Object>();
    failureFuture.setFailure(null);
    futures.add(failureFuture);

    final ListenableFuture<List<ListenableFuture<?>>> f = FutureUtils.makeFailureListFuture(futures);
    
    final AsyncVerifier av = new AsyncVerifier();
    f.addListener(new Runnable() {
      @Override
      public void run() {
        av.assertTrue(f.isDone());
        
        Iterator<ListenableFuture<?>> it = futures.iterator();
        while (it.hasNext()) {
          av.assertTrue(it.next().isDone());
        }
        
        av.signalComplete();
      }
    });
    
    av.waitForTest();
    
    verifyNoneIncluded(futures, f.get(), failureFuture);
  }
  
  @Test
  public void immediateResultFutureNullResultTest() throws InterruptedException, ExecutionException {
    ListenableFuture<?> testFuture = FutureUtils.immediateResultFuture(null);
    
    assertTrue(testFuture.isDone());
    assertFalse(testFuture.isCancelled());
    assertNull(testFuture.get());
  }
  
  @Test
  public void immediateResultFutureTest() throws InterruptedException, ExecutionException {
    Object result = new Object();
    ListenableFuture<?> testFuture = FutureUtils.immediateResultFuture(result);
    
    assertTrue(testFuture.isDone());
    assertFalse(testFuture.isCancelled());
    assertTrue(testFuture.get() == result);
  }
  
  @Test
  public void immediateFailureFutureTest() {
    Exception failure = new Exception();
    ListenableFuture<?> testFuture = FutureUtils.immediateFailureFuture(failure);
    
    assertTrue(testFuture.isDone());
    assertFalse(testFuture.isCancelled());
    try {
      testFuture.get();
      fail("Exception should have thrown");
    } catch (InterruptedException e) {
      fail("ExecutionException should have thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() == failure);
    }
  }
}
