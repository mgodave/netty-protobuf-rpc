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
package org.googlecode.protobuf.netty.example;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.googlecode.protobuf.netty.example.Calculator.CalcService;
import org.robotninjas.protobuf.netty.server.RpcServer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class CalculatorServer {

	public static void main(String[] args) throws InterruptedException {

    NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
    EventExecutorGroup eventExecutorGroup = new DefaultEventExecutorGroup(1);
    SocketAddress saddr = new InetSocketAddress("localhost", 8080);
		RpcServer server = new RpcServer(eventLoopGroup, eventExecutorGroup, saddr);
		
		server.registerService(CalcService.newReflectiveService(new CalculatorServiceImpl()));
		
		server.registerBlockingService(CalcService.newReflectiveBlockingService(new CalculatorServiceImpl()));

    server.startAndWait();

    Thread.sleep(10000000);
		
	}
	
}
