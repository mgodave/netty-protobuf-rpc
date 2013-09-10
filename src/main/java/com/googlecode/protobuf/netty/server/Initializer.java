package com.googlecode.protobuf.netty.server;

import com.googlecode.protobuf.netty.NettyRpcProto;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

class Initializer extends ChannelInitializer<ServerSocketChannel> {

  private final ServerHandler handler;

  Initializer(ServerHandler handler) {
    this.handler = handler;
  }

  @Override
  protected void initChannel(ServerSocketChannel ch) throws Exception {
    ChannelPipeline p = ch.pipeline();

    p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
    p.addLast("protobufDecoder", new ProtobufDecoder(NettyRpcProto.RpcResponse.getDefaultInstance()));

    p.addLast("frameEncoder", new LengthFieldPrepender(4));
    p.addLast("protobufEncoder", new ProtobufEncoder());

    p.addLast("serverHandler", handler);
  }

}
