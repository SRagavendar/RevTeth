package com.revteth;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

@SuppressWarnings("checkstyle:MagicNumber")
public class TestIPPacketOutputSteam {
  private ByteBuffer createMockPacket() {
    ByteBuffer buffer = ByteBuffer.allocate(32);
    writeMockPacketTo(buffer);
    buffer.flip();
    return buffer;
  }

  private void writeMockPacketTo(ByteBuffer buffer) {
    buffer.put((byte) (4 << 4) | 5));
    buffer.put((byte) 0);
    buffer.putShort((short) 32);
    buffer.putInt(0);
    buffer.put((byte) 17);
    buffer.putShort((short) 0);
    buffer.putInt(0x12345678);
    buffer.putInt(0x42424242);
    buffer.putShort((short) 1234);
    buffer.putShort((short) 5678);
    buffer.putShort((short) 12);
    buffer.putShort((short) 0);

    buffer.putInt(0x11223344);
  }

  @Test
  public void testSimplePacket() throws IOException {
    ByteArrayOutputStream bos
  }
}
