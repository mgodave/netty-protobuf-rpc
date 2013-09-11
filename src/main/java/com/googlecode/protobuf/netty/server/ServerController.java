package com.googlecode.protobuf.netty.server;

import com.google.common.base.Optional;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkNotNull;

class ServerController implements RpcController {

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private volatile boolean canceled = false;
  private volatile Optional<String> failed = Optional.absent();
  private volatile Optional<RpcCallback<Object>> callback = Optional.absent();

  @Override
  public void reset() {
    throw new UnsupportedOperationException("Can only be called from client");
  }

  @Override
  public boolean failed() {
    lock.readLock().lock();
    try {
      return failed.isPresent();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public String errorText() {
    lock.readLock().lock();
    try {
      return failed.get();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void startCancel() {
    throw new UnsupportedOperationException("Can only be called from client");
  }

  @Override
  public void setFailed(String reason) {
    checkNotNull(reason);
    lock.writeLock().lock();
    try {
      failed = Optional.of(reason);
    } finally {
      lock.writeLock().unlock();
    }
  }

  void notifyCanceled() {
    lock.writeLock().lock();
    try {
      canceled = true;
      if (callback.isPresent()) {
        callback.get().run(null);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean isCanceled() {
    lock.readLock().lock();
    try {
      return canceled;
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void notifyOnCancel(RpcCallback<Object> callback) {
    checkNotNull(callback);
    lock.writeLock().lock();
    try {
      if (isCanceled()) {
        callback.run(null);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }
}
