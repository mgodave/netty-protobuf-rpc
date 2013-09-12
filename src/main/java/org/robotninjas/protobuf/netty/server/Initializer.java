package org.robotninjas.protobuf.netty.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import org.robotninjas.protobuf.netty.NettyRpcProto;

class Initializer<T extends SocketChannel> extends ChannelInitializer<T> {

  private final ServerHandler handler;

  Initializer(ServerHandler handler) {
    this.handler = handler;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline p = ch.pipeline();

    p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
    p.addLast("protobufDecoder", new ProtobufDecoder(NettyRpcProto.RpcContainer.getDefaultInstance()));

    p.addLast("frameEncoder", new LengthFieldPrepender(4));
    p.addLast("protobufEncoder", new ProtobufEncoder());

    p.addLast("serverHandler", handler);
  }

}
