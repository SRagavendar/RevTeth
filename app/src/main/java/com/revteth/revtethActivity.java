package com.revteth;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;

public class revtethActivity extends Activity {
  private static final String TAG = revtethActivity.class.getSimpleName();
  public static final String ACTION_REVTETH_START = "com.revteth.START";
  public static final String ACTION_REVTETH_STOP = "com.revteth.STOP";
  public static final String EXTRA_DNS_SERVERS = "dnsServers";
  public static final String EXTRA_ROUTES = "routes";
  private static final int VPN_REQUEST_CODE = 0;
  private VpnConfiguration requestedConfig;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    handleIntent(getIntent());
  }

  private void handleIntent(Intent intent) {
    String action = intent.getAction();
    Log.d(TAG, "Received request " + action);
    boolean finish = true;
    if (ACTION_REVTETH_START.equals(action)) {
      VpnConfiguration config = createConfig(intent);
      finish = startRevteth(config);
    } else if (ACTION_REVTETH_STOP.equals(action)) {
      stopRevteth();
    }
    if (finish) {
      finish();
    }
  }

  private static VpnConfiguration createConfig(Intent intent) {
    String[] dnsServers = intent.getStringArrayExtra(EXTRA_DNS_SERVERS);
    if (dnsServers == null) {
      dnsServers = new String[0];
    }
    String[] routes = intent.getStringArrayExtra(EXTRA_ROUTES);
    if (routes == null) {
      routes = new String[0];
    }
    return new VpnConfiguration(Net.toInetAddresses(dnsServers), Net.toCIDRs(routes));
  }

  private boolean startRevteth(VpnConfiguration config) {
    Intent vpnIntent = VpnService.prepare(this);
    if (vpnIntent == null) {
      Log.d(TAG, "VPN was already authorized");
      revtethService.start(this, config);
      return true;
    }
    Log.w(TAG, "VPN requires the authorization from the user, requesting.....");
    requestAuthorization(vpnIntent, config);
    return false;
  }

  private void stopRevteth() {
    revtethService.stop(this);
  }

  private void requestAuthorization(int requestCode, int resultCode, Intent data) {
    this.requestedConfig = config;
    startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
      revtethService.start(this, requestedConfig);
    }
    requestedConfig = null;
    finish();
  }
}
