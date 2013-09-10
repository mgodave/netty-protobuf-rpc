package com.googlecode.protobuf.netty.client;

import com.google.common.util.concurrent.AbstractFuture;
import com.googlecode.protobuf.netty.NettyRpcProto;
import com.googlecode.protobuf.netty.RpcException;

class RpcCall extends AbstractFuture<NettyRpcProto.RpcResponse> {

  private final NettyRpcProto.RpcRequest request;

  public RpcCall(NettyRpcProto.RpcRequest request) {
    this.request = request;
  }

  public NettyRpcProto.RpcRequest getRequest() {
    return request;
  }

  public void complete(NettyRpcProto.RpcResponse response) {
    if (response.hasErrorCode()) {
      setException(new RpcException(getRequest(), response.getErrorMessage()));
    }
    set(response);
  }

  public void fail(Throwable e) {
    setException(e);
  }

}
