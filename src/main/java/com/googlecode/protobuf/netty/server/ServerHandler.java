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
package com.googlecode.protobuf.netty.server;

import com.google.protobuf.*;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.googlecode.protobuf.netty.*;
import com.googlecode.protobuf.netty.NettyRpcProto.ErrorCode;
import com.googlecode.protobuf.netty.NettyRpcProto.RpcRequest;
import com.googlecode.protobuf.netty.NettyRpcProto.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ServerHandler extends ChannelInboundHandlerAdapter {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Map<Integer, ServerController> controllers = new ConcurrentHashMap<Integer, ServerController>();
  private final Map<String, Service> serviceMap = new ConcurrentHashMap<String, Service>();
  private final Map<String, BlockingService> blockingServiceMap = new ConcurrentHashMap<String, BlockingService>();

  private final ChannelGroup allChannels;

  public ServerHandler(ChannelGroup allChannels) {
    this.allChannels = allChannels;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    allChannels.add(ctx.channel());
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    final RpcRequest request = (RpcRequest) msg;

    String serviceName = request.getServiceName();
    String methodName = request.getMethodName();

    logger.debug("Received request for serviceName: " + serviceName + ", method: " + methodName);

    if (request.getIsBlockingService()) {
      BlockingService blockingService = blockingServiceMap.get(serviceName);
      if (blockingService == null) {
        throw new NoSuchServiceException(request, serviceName);
      } else if (blockingService.getDescriptorForType().findMethodByName(methodName) == null) {
        throw new NoSuchServiceMethodException(request, methodName);
      } else if (!request.hasId()) {
        // All blocking services need to have a request ID since well, they are
        // blocking (hence they need a response!)
        throw new NoRequestIdException();
      } else {
        MethodDescriptor methodDescriptor = blockingService.getDescriptorForType().findMethodByName(methodName);
        Message methodRequest = null;
        try {
          methodRequest = buildMessageFromPrototype(
            blockingService.getRequestPrototype(methodDescriptor),
            request.getRequestMessage());
        } catch (InvalidProtocolBufferException ex) {
          throw new InvalidRpcRequestException(ex, request, "Could not build method request message");
        }
        ServerController controller = new ServerController();
        controllers.put(request.getId(), controller);
        Message methodResponse = null;
        try {
          methodResponse = blockingService.callBlockingMethod(methodDescriptor, controller, methodRequest);
        } catch (ServiceException ex) {
          throw new RpcServiceException(ex, request, "BlockingService RPC call threw ServiceException");
        } catch (Exception ex) {
          throw new RpcException(ex, request, "BlockingService threw unexpected exception");
        }
        if (controller.failed()) {
          throw new RpcException(request, "BlockingService RPC failed: " + controller.errorText());
        } else if (methodResponse == null) {
          throw new RpcException(request, "BlockingService RPC returned null response");
        }
        controllers.remove(request.getId());
        RpcResponse response = NettyRpcProto.RpcResponse.newBuilder()
          .setId(request.getId())
          .setResponseMessage(methodResponse.toByteString())
          .build();
        ctx.channel().writeAndFlush(response);
      }
    } else {
      Service service = serviceMap.get(serviceName);
      if (service == null) {
        throw new NoSuchServiceException(request, serviceName);
      } else if (service.getDescriptorForType().findMethodByName(methodName) == null) {
        throw new NoSuchServiceMethodException(request, methodName);
      } else {
        MethodDescriptor methodDescriptor = service.getDescriptorForType().findMethodByName(methodName);
        Message methodRequest = null;
        try {
          methodRequest = buildMessageFromPrototype(
            service.getRequestPrototype(methodDescriptor),
            request.getRequestMessage());
        } catch (InvalidProtocolBufferException ex) {
          throw new InvalidRpcRequestException(ex, request, "Could not build method request message");
        }
        final Channel channel = ctx.channel();
        final RpcController controller = new ServerController();
        RpcCallback<Message> callback = !request.hasId() ? null : new RpcCallback<Message>() {
          public void run(Message methodResponse) {
            if (methodResponse != null) {
              channel.writeAndFlush(RpcResponse.newBuilder()
                .setId(request.getId())
                .setResponseMessage(methodResponse.toByteString())
                .build());
            } else {
              logger.info("service callback returned null message");
              RpcResponse.Builder builder = RpcResponse.newBuilder()
                .setId(request.getId())
                .setErrorCode(ErrorCode.RPC_ERROR);
              if (controller.errorText() != null) {
                builder.setErrorMessage(controller.errorText());
              }
              channel.writeAndFlush(builder.build());
            }
          }
        };
        try {
          service.callMethod(methodDescriptor, controller, methodRequest, callback);
        } catch (Exception ex) {
          throw new RpcException(ex, request, "Service threw unexpected exception");
        }
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    logger.warn("exceptionCaught", cause);
    RpcResponse.Builder responseBuilder = RpcResponse.newBuilder();
    if (cause instanceof NoSuchServiceException) {
      responseBuilder.setErrorCode(ErrorCode.SERVICE_NOT_FOUND);
    } else if (cause instanceof NoSuchServiceMethodException) {
      responseBuilder.setErrorCode(ErrorCode.METHOD_NOT_FOUND);
    } else if (cause instanceof InvalidRpcRequestException) {
      responseBuilder.setErrorCode(ErrorCode.BAD_REQUEST_PROTO);
    } else if (cause instanceof RpcServiceException) {
      responseBuilder.setErrorCode(ErrorCode.RPC_ERROR);
    } else if (cause instanceof RpcException) {
      responseBuilder.setErrorCode(ErrorCode.RPC_FAILED);
    } else {
      /* Cannot respond to this exception, because it is not tied
       * to a request */
      logger.info("Cannot respond to handler exception", cause);
      return;
    }
    RpcException ex = (RpcException) cause;
    if (ex.getRpcRequest() != null && ex.getRpcRequest().hasId()) {
      responseBuilder.setId(ex.getRpcRequest().getId());
      responseBuilder.setErrorMessage(ex.getMessage());
      ctx.channel().writeAndFlush(responseBuilder.build());
    } else {
      logger.info("Cannot respond to handler exception", ex);
    }
  }


  private Message buildMessageFromPrototype(Message prototype, ByteString messageToBuild) throws InvalidProtocolBufferException {
    return prototype.newBuilderForType().mergeFrom(messageToBuild).build();
  }

  synchronized void registerService(Service service) {
    if (serviceMap.containsKey(service.getDescriptorForType().getFullName())) {
      throw new IllegalArgumentException("Service already registered");
    }
    serviceMap.put(service.getDescriptorForType().getFullName(), service);
  }

  synchronized void unregisterService(Service service) {
    if (!serviceMap.containsKey(service.getDescriptorForType().getFullName())) {
      throw new IllegalArgumentException("Service not already registered");
    }
    serviceMap.remove(service.getDescriptorForType().getFullName());
  }

  synchronized void registerBlockingService(BlockingService service) {
    if (blockingServiceMap.containsKey(service.getDescriptorForType().getFullName())) {
      throw new IllegalArgumentException("BlockingService already registered");
    }
    blockingServiceMap.put(service.getDescriptorForType().getFullName(), service);
  }

  synchronized void unregisterBlockingService(BlockingService service) {
    if (!blockingServiceMap.containsKey(service.getDescriptorForType().getFullName())) {
      throw new IllegalArgumentException("BlockingService not already registered");
    }
    blockingServiceMap.remove(service.getDescriptorForType().getFullName());
  }
}
