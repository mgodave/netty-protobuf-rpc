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
package com.googlecode.protobuf.netty.client;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.*;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.googlecode.protobuf.netty.NettyRpcProto.RpcRequest;
import com.googlecode.protobuf.netty.NettyRpcProto.RpcResponse;
import io.netty.channel.Channel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.Futures.transform;

public class RpcChannel implements com.google.protobuf.RpcChannel, BlockingRpcChannel {

  private final Channel channel;
  private final AtomicInteger sequence = new AtomicInteger(0);

  RpcChannel(Channel channel) {
    this.channel = channel;
  }

  public boolean isOpen() {
    return channel.isOpen();
  }

  private ListenableFuture<Message> doCallMethod(
    MethodDescriptor method, final RpcController controller,
    Message request, final Message responsePrototype, boolean blocking) {

    ListenableFuture<RpcResponse> result = new RpcCall(buildRequest(blocking, method, request));
    channel.write(result);
    return transform(result, new AsyncFunction<RpcResponse, Message>() {
      public ListenableFuture<Message> apply(RpcResponse input) {
        SettableFuture<Message> response = SettableFuture.create();
        try {
          final Message.Builder builder = responsePrototype.newBuilderForType();
          Message result = builder.mergeFrom(input.getResponseMessage()).build();
          response.set(result);
        } catch (InvalidProtocolBufferException e) {
          controller.setFailed(e.getMessage());
          response.setException(e);
        }
        return response;
      }
    });

  }

  public void callMethod(
    MethodDescriptor method, final RpcController controller,
    Message request, Message responsePrototype, final RpcCallback<Message> done) {

    ListenableFuture<Message> result = doCallMethod(method, controller, request, responsePrototype, false);

    addCallback(result, new FutureCallback<Message>() {
      public void onSuccess(Message result) {
        done.run(result);
      }

      public void onFailure(Throwable t) {
        controller.setFailed(t.getMessage());
        done.run(null);
      }
    });

  }

  public Message callBlockingMethod(
    MethodDescriptor method, RpcController controller, Message request,
    Message responsePrototype) throws ServiceException {

    try {
      return doCallMethod(method, controller, request, responsePrototype, true).get();
    } catch (InterruptedException e) {
      throw propagate(e);
    } catch (ExecutionException e) {
      throw new ServiceException(e.getMessage());
    }

  }

  public void close() {
    channel.close().awaitUninterruptibly();
  }

  private RpcRequest buildRequest(boolean isBlocking, MethodDescriptor method, Message request) {
    RpcRequest.Builder requestBuilder = RpcRequest.newBuilder();
    return requestBuilder
      .setId(sequence.incrementAndGet())
      .setIsBlockingService(isBlocking)
      .setServiceName(method.getService().getFullName())
      .setMethodName(method.getName())
      .setRequestMessage(request.toByteString())
      .build();
  }

  void requestCancel() {

  }

}
