package org.robotninjas.protobuf.netty.client;

import com.google.common.util.concurrent.AbstractFuture;
import org.robotninjas.protobuf.netty.NettyRpcProto;
import org.robotninjas.protobuf.netty.RpcException;
import org.robotninjas.protobuf.netty.NettyRpcProto;
import org.robotninjas.protobuf.netty.RpcException;

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
