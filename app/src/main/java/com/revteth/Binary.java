package com.revteth;

@SuppressWarnings("checkstyle:MagicNumber")
public final class Binary {

    private static final int MAX_STRING_PACKET_SIZE = 20;

    private Binary() {
        // not instantiable
    }

    public static int unsigned(byte value) {
        return value & 0xff;
    }

    public static int unsigned(short value) {
        return value & 0xffff;
    }

    public static long unsigned(int value) {
        return value & 0xffffffffL;
    }

    public static String buildPacketString(byte[] data, int len) {
        int limit = Math.min(MAX_STRING_PACKET_SIZE, len);
        StringBuilder builder = new StringBuilder();
        builder.append('[').append(len).append(" bytes] ");
        for (int i = 0; i < limit; ++i) {
            if (i != 0) {
                String sep = i % 4 == 0 ? "  " : " ";
                builder.append(sep);
            }
            builder.append(String.format("%02X", data[i] & 0xff));
        }
        if (limit < len) {
            builder.append(" ... +").append(len - limit).append(" bytes");
        }
        return builder.toString();
    }
}
