package com.googlecode.protobuf.netty.client;

import com.googlecode.protobuf.netty.NoRequestIdException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.googlecode.protobuf.netty.NettyRpcProto.RpcResponse;

class InboundHandler extends ChannelInboundHandlerAdapter {

  private final ConcurrentHashMap<Integer, RpcCall> callMap;

  InboundHandler(ConcurrentHashMap<Integer, RpcCall> callMap) {
    this.callMap = callMap;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    checkArgument(msg instanceof RpcResponse);

    RpcResponse response = (RpcResponse) msg;
    RpcCall call = callMap.remove(response.getId());
    if (call == null) {
      throw new NoRequestIdException();
    }
    call.complete(response);

  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (cause instanceof IOException) {
      synchronized (callMap) {
        for (RpcCall call : callMap.values()) {
          call.fail(cause);
        }
        callMap.clear();
      }
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    synchronized (callMap) {
      for (RpcCall call : callMap.values()) {
        call.cancel(true);
      }
      callMap.clear();
    }
  }
}
