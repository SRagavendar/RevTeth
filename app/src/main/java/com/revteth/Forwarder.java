package com.revteth;

import android.net.VpnService;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Forwarder {
  private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(3);
  private static final String TAG = Forwarder.class.getSimpleName();
  private static final int BUFSIZE = 0x10000;
  private static final byte[] DUMMY_ADDRESS = {42, 42, 42, 42};
  private static final int DUMMY_PORT = 4242;

  private final FileDescriptor vpnFileDescriptor;
  private final PersistentRelayTunnel tunnel;

  private Future<?> deviceToTunnelFuture;
  private Future<?> tunnelToDeviceFuture;

  public Forwarder(VpnService vpnService, FileDescriptor vpnFileDescriptor, RelayTunnelListener listener) {
    this.vpnFileDescriptor = vpnFileDescriptor;
    tunnel = new PersistentRelayTunnel(vpnService, listener);
  }

  public void forward() {
    deviceToTunnelFuture = EXECUTOR_SERVICE.submit(new Runnable() {
      @Override
      public void run() {
        try {
          forwardDeviceToTunnel(tunnel);
        } catch (InterruptedIOException e) {
          Log.d(TAG, "Device to tunnel interrupted");
        } catch (IOException e) {
          Log.e(TAG, "Device to tunnel exception", e);
        }
      }
    });
    tunnelToDeviceFuture = EXECUTOR_SERVICE.submit(new Runnable() {
      @Override
      public void run() {
        try {
          forwardTunnelToDevice(tunnel);
        } catch (InterruptedIOException e) {
          Log.d(TAG, "Device to tunnel interrupted");
        } catch (IOException e) {
          Log.e(TAG, "Tunnel to device exception", e);
        }
      }
    });
  }

  public void stop() {
    tunnel.close();
    tunnelToDeviceFuture.cancel(true);
    deviceToTunnelFuture.cancel(true);
    wakeUpReadWorkaround();
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  private void forwardDeviceToTunnel(Tunnel tunnel) throws IOException {
    Log.d(TAG, "Device to tunnel forwarding started");
    FileInputStream vpnInput = new FileInputStream(vpnFileDescriptor);
    byte[] buffer = new byte[BUFSIZE];
    while (true) {
      int r = vpnInput.read(buffer);
      if (r == -1) {
        Log.d(TAG, "VPN Closed");
        break;
      }
      if (r > 0) {
        int version = buffer[0] >> 4;
        if (version == 4) {
          tunnel.send(buffer, r);
        } else {
          Log.w(TAG, "Unexpected packet IP version: " + version);
        }
      } else {
        Log.d(TAG, "Empty read");
      }
    }
    Log.d(TAG, "Device to tunnel forwarding stopped");
  }

  private void forwardTunnelToDevice(Tunnel tunnel) throws IOException {
    Log.d(TAG, "Tunnel to device forwarding started");
    FileOutputStream vpnOutput = new FileOutputStream(vpnFileDescriptor);
    IPPacketOutputStream packetOutputStream = new IPPacketOutputStream(vpnOutput);

    byte[] buffer = new byte[BUFSIZE];
    while (true) {
      int w = tunnel.receive(buffer);
      if (w == -1) {
        Log.d(TAG, "Tunnel closed");
        break;
      }
      if (w > 0) {
        packetOutputStream.write(buffer, 0, w);
      } else {
        Log.d(TAG, "Empty write");
      }
    }
    Log.d(TAG, "Tunnel to device forwarding stopped");
  }

  private void wakeUpReadWorkaround() {
    EXECUTOR_SERVICE.execute(new Runnable() {
      @Override
      public void run() {
        try {
          DatagramSocket socket = new DatagramSocket();
          InetAddress dummyAdr = InetAddress.getByAddress(DUMMY_ADDRESS);
          DatagramPacket packet = new DatagramPacket(new byte[0], 0, dummyAdr, DUMMY_PORT);
          socket.send(packet);
        } catch (IOException e) {
        }
      }
    });
  }
}
