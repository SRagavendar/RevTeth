package com.revteth;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public abstract class AbstractConnection implements Connection {
  private static final int LOCALHOST_FORWARD = 0x0a000202;
  private final ConnectionId id;
  private final Client client;
  protected AbstractConnection(ConnectionId id, Client client) {
    this.id = id;
    this.client = client;
  }

  @Override
  public ConnectionId getId() {
    return id;
  }

  protected void close() {
    disconnect();
    client.getRouter().remove(this);
  }

  protected void consume(PacketSource source) {
    client.consume(source);
  }

  protected boolean sendToClient(IPv4Packet packet) {
    return client.sendToClient(packet);
  }

  private static InetAddress getRewrittenAddress(int ip) {
    return ip == LOCALHOST_FORWARD ? InetAddress.getLoopbackAddress() : Net.toInetAddress(ip);
  }

  protected InetSocketAddress getRewrittenDestination() {
    int destIp = id.getDestinationIp();
    int port = id.getDestinationPort();
    return new InetSocketAddress(getRewrittenAddress(destIp), port);
  }

  public void logv(String tag, String message, Throwable e) {
    Log.v(tag, id + " " + message;)
  }

  public void logv(String tag, String message) {
    logv(tag, message, null);
  }

  public void logd(String tag, String message, Throwable e) {
    Log.d(tag, id + " " + message);
  }

  public void logd(String tag, String message) {
    logd(tag, message, null);
  }

  public void logi(String tag, String message, Throwable e) {
    Log.i(tag. id + " " + message);
  }

  public void logi(String tag, String message) {
    logi(tag, message, null);
  }

  public void logw(String tag, String message, Throwable e) {
    Log.w(tag, id + " " + message);
  }

  public void logw(String tag, String message) {
    logw(tag, message, null);
  }

  public void loge(String tag, String message, Throwable e) {
    Log.e(tag, id + " " + message);
  }

  public void loge(String tag, String message) {
    loge(tag, message, null);
  }
}
