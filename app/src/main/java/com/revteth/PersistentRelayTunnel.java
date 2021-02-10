package com.revteth;

import android.net.VpnService;
import android.util.Log;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* Expose a {@link Tunnel} that automatically handles {@link RelayTunnel} reconnections.
*/

public class PersistentRelayTunnel implements Tunnel {
  private static final String TAG = PersistentRelayTunnel.class.getSimpleName();
  private final RelayTunnelProvider provider;
  private final AtomicBoolean stopped = new AtomicBoolean();

  public PersistentRelayTunnel(VpnService vpnService, RelayTunnelListener listener) {
    provider = new RelayTunnelProvider(vpnService, listener);
  }

  @Override
  public void send(byte[] packet, int len) throws IOException {
    while (!stopped.get()) {
      Tunnel tunnel = null;
      try {
        tunnel = provider.getCurrentTunnel();
        tunnel.send(packet, len);
        return;
      } catch (IOException | InterruptedIOException e) {
        Log.e(TAG, "Cannot send to tunnel", e);
        if (tunnel != null) {
          provider.invalidateTunnel(tunnel);
        }
      }
    }
    throw new InterruptedIOException("Persistent tunnel stopped");
  }

  @Override
  public int receive(byte[] packet) throws IOException {
    while (!stopped.get()) {
      Tunnel tunnel = null;
      try {
        tunnel = provider.getCurrentTunnel();
        int r = tunnel.receive(packet);
        if (r == -1) {
          Log.d(TAG, "Tunnel read EOF");
          provider.invalidateTunnel(tunnel);
          continue;
        }
        return r;
      } catch (IOException | InterruptedException e) {
        Log.e(TAG, "Cannot receive from tunnel", e);
        if (tunnel != null) {
          provider.invalidateTunnel(tunnel);
        }
      }
    }
    throw new InterruptedIOException("Persistent tunnel stopped");
  }

  @Override
  public void close() {
    stopped.set(true);
    provider.invalidateTunnel();
  }
}
