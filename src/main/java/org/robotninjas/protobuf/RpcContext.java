package org.robotninjas.protobuf;

import java.net.SocketAddress;

public interface RpcContext {
  SocketAddress getRemote();
}
