package org.robotninjas.protobuf.netty.server;

import com.google.common.base.Optional;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.util.concurrent.EventExecutorGroup;
import org.robotninjas.protobuf.netty.NettyRpcProto;

class ServerInitializer<T extends SocketChannel> extends ChannelInitializer<T> {

  private final Optional<EventExecutorGroup> eventExecutor;
  private final ServerHandler handler;

  private ServerInitializer(Optional<EventExecutorGroup> eventExecutor, ServerHandler handler) {
    this.eventExecutor = eventExecutor;
    this.handler = handler;
  }

  ServerInitializer(EventExecutorGroup eventExecutor, ServerHandler handler) {
    this(Optional.of(eventExecutor), handler);
  }

  ServerInitializer(ServerHandler handler) {
    this(Optional.<EventExecutorGroup>absent(), handler);
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline p = ch.pipeline();

    p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
    p.addLast("protobufDecoder", new ProtobufDecoder(NettyRpcProto.RpcContainer.getDefaultInstance()));

    p.addLast("frameEncoder", new LengthFieldPrepender(4));
    p.addLast("protobufEncoder", new ProtobufEncoder());

    if (eventExecutor.isPresent()) {
      p.addLast(eventExecutor.get(), "serverHandler", handler);
    } else {
      p.addLast("serverHandler", handler);
    }
  }

}
