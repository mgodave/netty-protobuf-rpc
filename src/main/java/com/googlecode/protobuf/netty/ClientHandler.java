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

import com.google.common.collect.Maps;
import com.googlecode.protobuf.netty.exception.NoRequestIdException;
import org.jboss.netty.channel.*;

import java.util.Map;

import static com.googlecode.protobuf.netty.proto.NettyRpcProto.RpcResponse;

class ClientHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler {

  private final Map<Integer, RpcCall> calls = Maps.newConcurrentMap();

  public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
    if (e instanceof MessageEvent) {
      MessageEvent event = (MessageEvent) e;
      if (event.getMessage() instanceof RpcCall) {
        RpcCall call = (RpcCall) event.getMessage();
        calls.put(call.getRequest().getId(), call);
        DownstreamMessageEvent downstreamEvent =
          new DownstreamMessageEvent(e.getChannel(), e.getFuture(), call.getRequest(),
            ((MessageEvent) e).getRemoteAddress());
        ctx.sendDownstream(downstreamEvent);
      }
    } else {
      ctx.sendDownstream(e);
    }
  }

  public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
    if (e instanceof MessageEvent) {
      MessageEvent event = (MessageEvent) e;
      if (event.getMessage() instanceof RpcResponse) {
        RpcResponse response = (RpcResponse) event.getMessage();
        RpcCall call = calls.remove(response.getId());
        if (call != null) {
          throw new NoRequestIdException();
        }
        call.complete(response);
      }
    } else if (e instanceof ChannelStateEvent) {
      ChannelStateEvent event = (ChannelStateEvent) e;
      if (event.getState().equals(ChannelState.OPEN) &&
        ((Boolean) event.getValue()) == false) {
        synchronized (calls) {
          for (RpcCall call : calls.values()) {
            call.cancel(true);
          }
          calls.clear();
        }
      }
      ctx.sendUpstream(e);
    } else {
      ctx.sendUpstream(e);
    }
  }

}
