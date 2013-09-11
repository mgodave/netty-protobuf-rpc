package com.googlecode.protobuf.netty.client;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

public class ClientController implements RpcController {

  private final RpcChannel channel;
  private volatile String errorText = new String();
  private volatile boolean failed = false;
  private volatile boolean startCancelRequested;

  public ClientController(RpcChannel channel) {
    this.channel = channel;
  }

  @Override
  public void reset() {
    this.errorText = new String();
    this.failed = false;
  }

  @Override
  public boolean failed() {
    return failed;
  }

  @Override
  public String errorText() {
    return errorText;
  }

  @Override
  public void startCancel() {
    if (!startCancelRequested) {
      startCancelRequested = true;
      channel.requestCancel();
    }
  }

  @Override
  public void setFailed(String reason) {
    throw new UnsupportedOperationException("Can only be called from server");
  }

  @Override
  public boolean isCanceled() {
    throw new UnsupportedOperationException("Can only be called from server");
  }

  @Override
  public void notifyOnCancel(RpcCallback<Object> callback) {
    throw new UnsupportedOperationException("Can only be called from server");
  }

}
