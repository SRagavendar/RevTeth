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
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    IPPacketOutputStream pos = new IPPacketOutputStream(bos);

    byte[] rawPacket = createMockPacket().array();

    pos.write(rawPacket, 0, 14);
        Assert.assertEquals("Partial packet should not be written", 0, bos.size());

        pos.write(rawPacket, 14, 14);
        Assert.assertEquals("Partial packet should not be written", 0, bos.size());

        pos.write(rawPacket, 28, 4);
        Assert.assertEquals("Complete packet should be written", 32, bos.size());

        byte[] result = bos.toByteArray();
        Assert.assertTrue("Resulting array must be identical", Arrays.equals(rawPacket, result));
    }

    @Test
    public void testSeveralPacketsAtOnce() throws IOException {
        class CapturingOutputStream extends ByteArrayOutputStream {
            private int packetCount;

            @Override
            public void write(byte[] b, int off, int len) {
                super.write(b, off, len);
                ++packetCount;
            }
        }
        CapturingOutputStream cos = new CapturingOutputStream();
        IPPacketOutputStream pos = new IPPacketOutputStream(cos);

        ByteBuffer buffer = ByteBuffer.allocate(3 * 32);
        for (int i = 0; i < 3; ++i) {
            writeMockPacketTo(buffer);
        }
        byte[] rawPackets = buffer.array();

        pos.write(rawPackets, 0, 70); // 2 packets + 6 bytes
        Assert.assertEquals("Exactly 2 packets should have been written", 64, cos.size());
        Assert.assertEquals("Packets should be written individually to the target", 2, cos.packetCount);

        pos.write(rawPackets, 70, 26);
        Assert.assertEquals("Exactly 3 packets should have been written", 96, cos.size());
        Assert.assertEquals("Packets should be written individually to the target", 3, cos.packetCount);
  }
}
