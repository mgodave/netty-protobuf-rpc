package com.googlecode.protobuf.netty;

import com.google.common.util.concurrent.AbstractFuture;
import com.googlecode.protobuf.netty.proto.NettyRpcProto;

class RpcCall extends AbstractFuture<NettyRpcProto.RpcResponse> {

  private final NettyRpcProto.RpcRequest request;

  public RpcCall(NettyRpcProto.RpcRequest request) {
    this.request = request;
  }

  public NettyRpcProto.RpcRequest getRequest() {
    return request;
  }

  public void complete(NettyRpcProto.RpcResponse response) {
    set(response);
  }

}
