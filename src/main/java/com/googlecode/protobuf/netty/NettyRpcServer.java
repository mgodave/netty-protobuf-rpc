/*
 * Copyright (c) 2009 Stephen Tu <stephen_tu@berkeley.edu>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.protobuf.netty;

import com.google.common.base.Suppliers;
import com.google.protobuf.BlockingService;
import com.google.protobuf.Service;
import com.googlecode.protobuf.netty.NettyRpcProto.RpcRequest;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

import javax.inject.Inject;
import java.net.SocketAddress;

public class NettyRpcServer {


  private final ServerBootstrap bootstrap;
  private final ChannelGroup allChannels = new DefaultChannelGroup();
  private final ServerHandler handler = new ServerHandler(allChannels);

  @Inject
  public NettyRpcServer(ChannelFactory channelFactory) {
    bootstrap = new ServerBootstrap(channelFactory);
    bootstrap.setPipelineFactory(
      new PipelineFactory(
        Suppliers.<ChannelHandler>ofInstance(handler),
        RpcRequest.getDefaultInstance()));
  }

  public void registerService(Service service) {
    handler.registerService(service);
  }

  public void unregisterService(Service service) {
    handler.unregisterService(service);
  }

  public void registerBlockingService(BlockingService service) {
    handler.registerBlockingService(service);
  }

  public void unregisterBlockingService(BlockingService service) {
    handler.unregisterBlockingService(service);
  }

  public void serve() {
    bootstrap.bind();
  }

  public void serve(SocketAddress sa) {
    bootstrap.bind(sa);
  }

  public void shutdown() {
    allChannels.close().awaitUninterruptibly();
    bootstrap.releaseExternalResources();
  }
}
