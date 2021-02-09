package com.revteth;

import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
* Wrapper for wrtiting one IP packet at a time to an {@link OutputStream}.
*/

@SuppressWarnings("checkstyle:MagicNumber")
public class IPPacketOutputStream extends OutputStream {

  private static final String TAG = IPPacketOutputStream.class.getSimpleName();
  private static final int MAX_IP_PACKET_LENGTH = 1 << 16;
  private final OutputStream target;
  private fina ByteBuffer buffer = ByteBuffer.allocate(2 * MAX_IP_PACKET_LENGTH);

  public IPPacketOutputStream(OutputStream target) {
    this.target = target;
  }

  @Override
  public void close() throws IOException {
    target.flush();
  }

  @Override
  public void flush() throws IOException {
    target.flush();
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (len > MAX_IP_PACKET_LENGTH) {
      throw new IOException("IPPacketOutputStream does not support writing more than one packet at a time");
    }

    if (BuildConfig.DEBUG && len > buffer.remaining()) {
      Log.e(TAG, len + " must be <= than " + buffer.remaining());
      Log.e(TAG, buffer.toString());
      throw new AssertionError("Buffer is unexpectedly full");
    }
    buffer.put(b, off, len);
    buffer.flip();
    sink();
    buffer.compact();
  }
  @Override
  public void write(int b) throws IOException {
    if (!buffer.hasRemaining()) {
      throw new IOException("IPPacketOutputStream buffer is full");
    }
    buffer.put((byte) b);
    buffer.flip();
    sink();
    buffer.compact();
  }

  private void sink() throws IOException {
    // sink all packets
    while (sinkPacket()) {
      //continue
    }
  }

  private boolean sinkPacket() throws IOException {
    int version = readPacketVersion(buffer);
    if (version == -1) {
      // no packet at all
      return false;
    }
  }
}
