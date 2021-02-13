package com.revteth.relay;
import java.nio.ByteBuffer;

@SuppressWarnings("checkstyle:MagicNumber")
public final class Binary {
  private static final int MAX_STRING_PACKET_SIZE = 20;
  private Binary() {

  }

  public static String buildPacketString(byte[] data, int offset, int len) {
    int limit = Math.min(MAX_STRING_PACKET_SIZE, len);
    StringBuilder builder = new StringBuilder();
    builder.append('[').append(len).append(" bytes] ");
    for (int i = 0; i < limit; ++i) {
      if (i != 0) {
        String sep = i % 4 == 0 ? " " : " ";
        builder.append(sep);
      }
      builder.append(String.format("%02X", data[offset + i] & 0xff));
    }
    if (limit < len) {
      builder.append(" ...").append(len - limit).append(" bytes");
    }
    return builder.toString();
  }

  public static String buildPacketString(ByteBuffer buffer) {
    return buildPacketString(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
  }

  public static ByteBuffer copy(ByteBuffer buffer) {
    buffer.rewind();
    ByteBuffer result = ByteBuffer.allocate(buffer.remaining());
    result.put(buffer);
    buffer.rewind();
    result.flip();
    return result;
  }

  public static ByteBuffer slice(ByteBuffer buffer, int offset, int length) {
    int position  = buffer.position();
    int limit = buffer.limit();
    buffer.limit(offset + length).position(offset);
    ByteBuffer result = buffer.slice();
    buffer.limit(limit).position(position);
    return result;
  }
}
