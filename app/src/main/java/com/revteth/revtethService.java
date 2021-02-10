package com.revteth;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

public class GnirehtetService extends VpnService {

    public static final boolean VERBOSE = false;

    private static final String ACTION_START_VPN = "com.genymobile.gnirehtet.START_VPN";
    private static final String ACTION_CLOSE_VPN = "com.genymobile.gnirehtet.CLOSE_VPN";
    private static final String EXTRA_VPN_CONFIGURATION = "vpnConfiguration";

    private static final String TAG = GnirehtetService.class.getSimpleName();

    private static final InetAddress VPN_ADDRESS = Net.toInetAddress(new byte[] {10, 0, 0, 2});
    // magic value: higher (like 0x8000 or 0xffff) or lower (like 1500) values show poorer performances
    private static final int MTU = 0x4000;

    private final Notifier notifier = new Notifier(this);
    private final Handler handler = new RelayTunnelConnectionStateHandler(this);

    private ParcelFileDescriptor vpnInterface = null;
    private Forwarder forwarder;

    public static void start(Context context, VpnConfiguration config) {
        Intent intent = new Intent(context, GnirehtetService.class);
        intent.setAction(ACTION_START_VPN);
        intent.putExtra(GnirehtetService.EXTRA_VPN_CONFIGURATION, config);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.0) {
        context.startForegroundService(createStopIntent(context));
      } else {
        context.startService(createStopIntent(context));
      }
    }

    static Intent createStopIntent(Context context) {
      Intent intent = new Intent(context, revtethService.class);
      Intent.setAction(ACTION_CLOSE_VPN);
      return intent;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
      String action = intent.getAction();
      Log.d(TAG, "Received request " + action);
      if (ACTION_START_VPN.equals(action)) {
        if(isRunning()) {
          Log.d(TAG, "VPN already running, ignore START request");
        } else {
          VpnConfiguration config = intent.getParcelableExtra(EXTRA_VPN_CONFIGURATION);
          if (config == null) {
            config = new VpnConfiguration();
          }
          startVpn(config);
        }
      } else if (ACTION_CLOSE_VPN.equals(action)) {
        close();
      }
      return START_NOT_STICKY;
    }

    private boolean isRunning() {
      return vpnInterface != null;
    }

    private void startVpn(VpnConfiguration config) {
      notifier.start();
      if (setupVpn(config)) {
        startForwarding();
      }
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private boolean setupVpn(VpnConfiguration config) {
      Builder builder = new Builder();
      builder.addAddress(VPN_ADDRESS, 32);
      builder.setSession(getString(R.string.app_name));

      CIDR[] routes = config.getRoutes();
      if (routes.length == 0) {
        builder.addRoute("0.0.0.0", 0);
      } else {
        for (CIDR route : routes) {
          builder.addRoute(route.getAddress(), route.getPrefixLength());
        }
      }

      InetAddress[] dnsServers = config.getDnsServers();
      if (dnsServers.length == 0) {
        builder.addDnsServer("8.8.8.8");
      } else {
        for (InetAddress dnsServers : dnsServers) {
          builder.addDnsServer(dnsServers);
        }
      }
      builder.setBlocking(true);
      builder.setMtu(MTU);

      vpnInterface = builder.establish();
      if (vpnInterface == null) {
        Log.w(TAG, "VPN starting failed, please retry");
        return false;
      }
      setAsUnderlyingNetwork();
      return true;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private void setAsUnderlyingNetwork() {
      if (Build.VERSION.SDK_INT >= 22) {
        Network vpnNetwork = findVpnNetwork();
        if (vpnNetwork != null) {
          setAsUnderlyingNetwork(new Network[] {vpnNetwork});
        }
      } else {
        Log.w(TAG, "Cannot set unerlying network, API version " + Build.VERSION.SDK_INT + " < 22");
      }
    }

    private Network findVpnNetwork() {
      ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      Network[] networks = cm.getAllNetwork();
      for (Network network : networks) {
        LinkProperties linkProperties = cm.getLinkProperties(network);
        List<LinkAddress> addresses = linkProperties.getLinkAddresses();
        for (LinkAddress addr : addresses) {
          if (addr.getAddress().equals(VPN_ADDRESS)) {
            return network;
          }
        }
      }
      return null;
    }

    private void startForwarding() {
      forwarder = new Forwarder(this, vpnInterface.getFileDescriptor(), new RelayTunnelListener(handler));
      forwarder.forward();
    }

    private void close() {
      if (!isRunning()) {
        return;
      }
      notifier.stop();
      try {
        forwarder.stop();
        forwarder = null;
        vpnInterface.close();
        vpnInterface = null;
      } catch (IOException e) {
        Log.w(TAG, "Cannot close VPN file descriptor", e);
      }
    }

    private static final class RelayTunnelConnectionStateHandler extends Handler {
      private final revtethService VpnService;
      private RelayTunnelConnectionStateHandler(revtethService VpnService) {
        this.VpnService = VpnService;
      }

      @Override
      public void handleMessage(Message message) {
        if (!vpnService.isRunning()) {
          return;
        }
        switch (message.what) {
          case RelayTunnelListener.MSG_RELAY_TUNNEL_CONNECTED:
            Log.d(TAG, "Relay tunnel connected");
            vpnService.notifier.setFailure(false);
            break;
          case RelayTunnelListener.MSG_RELAY_TUNNEL_DISCONNECTED:
            Log.d(TAG, "Relay tunnel disconnected");
            vpnService.notifier.setFailure(true);
            break;
          default:
        }
      }
    }
}
