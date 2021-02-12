package com.revteth;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.VpnService;
import android.util.Log;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class RelayTunnel implements Tunnel {
  private static final String TAG = RelayTunnel.class.getSimpleName();
  private static final String LOCAL_ABSTRACT_NAME = "revteth";
  private final LocalSocket localSocket = new LocalSocket();
  private RelayTunnel() {

  }

  @SuppressWarnings("ununsed")
  public static RelayTunnel open(VpnService vpnService) throws IOException {
    Log.d(TAG, "Opening a new relay tunnel....");
    return new RelayTunnel();
  }

  public void connect() throws IOException {
    localSocket.connect(new LocalSocketAddress(LOCAL_ABSTRACT_NAME));
    readClientId(localSocket.getInputStream());
  }

  /**
  * The relay server is accessible through an "adb reverse" port redirection.
  * <p>
  * If the port redirection is enabled but the relay server is not started, then the call to
  * channel.connect() will succeed, but the first read() will return -1.
  * <p>
  * As a consequence, the connection state of the relay server would be invalid temporarily (we
  * would switch to CONNECTED state then switch back to DISCONNECTED).
  * <p>
  * To avoid this problem, we must actually read from the server, so that an error occurs
  * immediately if the relay server is not accessible.
  * <p>
  * Therefore, the relay server immediately sends the client id: consume it and log it.
  * @param inputStream the input stream to receive data from the relay server
  * @throws IOException if an I/O error occurs
  */

  private static void readClientId(InputStream inputStream) throws IOException {
    Log.d(TAG, "Requesting client id");
    int clientID = new DataInputStream(inputStream).readInt();
    Log.d(TAG, "Connected to the relay server as #" + Binary.unsigned(clientId));
  }

  @Override
  public void send(byte[] packet, int len) throws IOException {
    if (revtethService.VERBOSE) {
      Log.v(TAG, "Sending packet: " + Binary.buildPacketString(packet, len));
    }
    localSocket.getOutputStream().write(packet, 0, len);
  }

  @Override
  public int receive(byte[] packet) throws IOException {
    int r = localSocket.getInputStream().read(packet);
    if (revtethService.VERBOSE) {
      Log.v(TAG, "Receiving packet: " + Binary.buildPacketString(packet, r));
    }
    return r;
  }

  @Override
  public void close() {
    try {
      if (localSocket.getFileDescriptor() != null) {
        localSocket.shutdownInput();
        localSocket.shutdownOutput();
      }
      localSocket.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
