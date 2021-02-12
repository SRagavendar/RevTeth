package com.revteth;

import java.io.IOException;

public interface Tunnel {
  void send(byte[] packet, int len) throws IOException;
  int receive (byte[] packet) throws IOException;
  void close();
}
