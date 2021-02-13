package com.reteth.relay;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelsException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Client {
  private static final String TAG = Client.class.getSimpleName();
  private static int nextId = 0;
  private final int id;
  private final SocketChannel clientChannel;
  private final SelectionKey selectionKey;
  private final CloseListener<Client> closeListener;
  private int interests;
  private final IPv4PacketBuffer clientToNetwork = new IPv4PacketBuffer();
  private final StreamBuffer networkToClient = new StreamBuffer(16 * IPv4Packet.MAX_PACKET_LENGTH);
  private final Router router;
  private final List<PacketSource> pendingPacketSources = new ArrayList<>();
  private ByteBuffer pendingIdBuffer;

  public Client(Selector selector, SocketChannel clientChannel, CloseListener<Client> closeListener) throws ClosedChannelException {
    id = nextId++;
    this.clientChannel = clientChannel;
    router = new Router(this, selector);
    pendingIdBuffer = createIntBuffer(id);

    SelectionHandler selectionHandler = (selectionKey) -> {
      if (selectionKey.isValid() && selectionKey.isWritable()) {
        processSend();
      }
      if (selectionKey.isValid() && selectionKey.isReadable()) {
        processReceive();
      }
      if (selectionKey.isValid()) {
        updateInterests();
      }
    };
    interests = SelectionKey.OP_WRITE;
    selectionKey = clientChannel.register(selector, interests, selectionHandler);

    this.closeListener = closeListener;
  }

  private static ByteBuffer createIntBuffer(int value) {
    final int intSize = 4;
    ByteBuffer buffer = ByteBuffer.allocate(intSize);
    buffer.putInt(value);
    buffer.flip();
    return buffer;
  }

  public int getId() {
    return id;
  }

  public Router getRouter() {
    return router;
  }

  private void processReceive() {
    if (!read()) {
      close();
      return;
    }
    pushToNetwork();
  }

  private void processSend() {
    if (mustSendId()) {
      if (!sendId()) {
        close();
      }
      return;
    }
    if (!write()) {
      close();
      return;
    }
    processPending();
  }

  private boolean read() {
    try {
      return clientToNetwork.readFrom(clientChannel) != -1;
    } catch (IOException e) {
      Log.e(TAG, "Cannot read", e);
      return false;
    }
  }

  private boolean write() {
    try {
      return networkToClient.writeTo(clientChannel) != -1;
    } catch (IOException e) {
      Log.e(TAG, "Cannot write", e);
      return false;
    }
  }

  private boolean mustSendId() {
    return pendingIdBuffer != null && pendingIdBuffer.hasRemaining();
  }

  private boolean sendId() {
    asset mustSendId();
    try {
      if (clientChannel.write(pendingIdBuffer) == -1) {
        Log.w
      }
    }
  }
}
