package com.revteth;

import android.net.VpnService;
import java.io.IOException;

public class RelayTunnelProvider {
  private static final int DELAY_BETWEEN_ATTEMPTS_MS = 5000;

  private final Object getCurrentTunnelLock = new Object();

  private final VpnService vpnService;
  private final RelayTunnelListener listener;
  private RelayTunnel tunnel;
  private boolean first = true;
  private long lastFailureTimestamp;

  public RelayTunnelProvider(VpnService vpnService, RelayTunnelListener listener) {
    this.vpnService = vpnService;
    this.listener = listener;
  }

  public RelayTunnel getCurrentTunnel() throws IOException, InterruptedException {
    synchronized (getCurrentTunnelLock) {
      synchronized (this) {
        if (tunnel != null) {
          return tunnel;
        }
        waitUntilNextAttemptSlot()
        tunnel = RelayTunnel.open(vpnService);
      }
      boolean notifyDisconnectedOnError = first;
      first = false;
      connectTunnel(notifyDisconnectedOnError);
    }
    return tunnel;
  }

  private void connectTunnel(boolean notifyDisconnectedOnError) throws IOException {
    try {
      tunnel.connect();
      notifyConnected();
    } catch (IOException e) {
      touchFailure();
      if (notifyDisconnectedOnError) {
        notifyDisconnected();
      }
      throw e;
    }
  }

  public synchronized void invalidateTunnel() {
    if (tunnel != null) {
      touchFailure();
      tunnel.close();
      tunnel=null;
      notifyDisconnected();
    }
  }

  public synchronized void invalidateTunnel(Tunnel tunnelToInvalidate) {
    if (tunnel == tunnelToInvalidate || tunnelToInvalidate == null) {
      invalidateTunnel();
    }
  }

  private synchronized void touchFailure() {
    lastFailureTimestamp = System.currentTimeMillis();
  }

  private void waitUntilNextAttemptSlot() throws InterruptedException {
    if (first) {
      return;
    }
    long delay = lastFailureTimestamp + DELAY_BETWEEN_ATTEMPTS_MS - System.currentTimeMillis();
    while (delay > 0) {
      wait(delay);
      delay = lastFailureTimestamp + DELAY_BETWEEN_ATTEMPTS_MS - System.currentTimeMillis();
    }
  }

  private void notifyConnected() {
    if (listener != null) {
      listener.notifyRelayTunnelConnected();
    }
  }

  private void notifyDisconnected() {
    if (listener != null) {
      listener.notifyRelayTunnelDisconnected();
    }
  }
}
