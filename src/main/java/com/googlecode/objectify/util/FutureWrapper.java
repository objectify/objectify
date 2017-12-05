// Copyright 2009 Google Inc. All rights reserved.

package com.googlecode.objectify.util;

import com.google.common.base.Preconditions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JMS: Copied verbatum from the original GAE SDK. Probably something we can eliminate.
 *
 * {@code FutureWrapper} is a simple {@link Future} that wraps a
 * parent {@code Future}.  This class is thread-safe.
 *
 * @param <K> The type of this {@link Future}
 * @param <V> The type of the wrapped {@link Future}
 */
public abstract class FutureWrapper<K, V> implements Future<V> {

  private final Future<K> parent;

  private boolean hasResult;
  private V successResult;
  private ExecutionException exceptionResult;

  private final Lock lock = new ReentrantLock();

  public FutureWrapper(Future<K> parent) {
    this.parent = Preconditions.checkNotNull(parent);
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return parent.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return parent.isCancelled();
  }

  @Override
  public boolean isDone() {
    return parent.isDone();
  }

  private V handleParentException(Throwable cause) throws Throwable {
    return setSuccessResult(absorbParentException(cause));
  }

  private V wrapAndCache(K data) throws Exception {
    return setSuccessResult(wrap(data));
  }

  private V setSuccessResult(V result) {
    successResult = result;
    hasResult = true;
    return result;
  }

  private ExecutionException setExceptionResult(Throwable ex) {
    exceptionResult = new ExecutionException(ex);
    hasResult = true;
    return exceptionResult;
  }

  private V getCachedResult() throws ExecutionException {
    if (exceptionResult != null) {
      throw exceptionResult;
    }
    return successResult;
  }

  @Override
  public V get() throws ExecutionException, InterruptedException {
    lock.lock();
    try {
      if (hasResult) {
        return getCachedResult();
      }

      try {
        K value;
        try {
          value = parent.get();
        } catch (ExecutionException ex) {
          return handleParentException(ex.getCause());
        }
        return wrapAndCache(value);
      } catch (InterruptedException ex) {
        throw ex;
      } catch (Throwable ex) {
        throw setExceptionResult(convertException(ex));
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public V get(long timeout, TimeUnit unit)
      throws InterruptedException, TimeoutException, ExecutionException {
    long tryLockStart = System.currentTimeMillis();
    if (!lock.tryLock(timeout, unit)) {
      throw new TimeoutException();
    }
    try {
      if (hasResult) {
        return getCachedResult();
      }
      long remainingDeadline = TimeUnit.MILLISECONDS.convert(timeout, unit) -
          (System.currentTimeMillis() - tryLockStart);
      try {
        K value;
        try {
          value = parent.get(remainingDeadline, TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex) {
          return handleParentException(ex.getCause());
        }
        return wrapAndCache(value);
      } catch (InterruptedException ex) {
        throw ex;
      } catch (TimeoutException ex) {
        throw ex;
      } catch (Throwable ex) {
        throw setExceptionResult(convertException(ex));
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  protected abstract V wrap(K key) throws Exception;

  /**
   * Override this method if you want to suppress an exception thrown by the
   * parent and return a value instead.
   */
  protected V absorbParentException(Throwable cause) throws Throwable {
    throw cause;
  }

  protected abstract Throwable convertException(Throwable cause);
}
