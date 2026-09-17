package android.net.wifi;

import java.util.List;

import dev.rikka.tools.refine.RefineAs;

/**
 * This class provides the primary API for managing all aspects of Wi-Fi
 * connectivity. Get an instance of this class by calling
 * {@link android.content.Context#getSystemService(String) Context.getSystemService(Context.WIFI_SERVICE)}.
 * <p>
 * It deals with several categories of items:
 * <ul>
 * <li>The list of configured networks. The list can be viewed and updated,
 * and attributes of individual entries can be modified.</li>
 * <li>The currently active Wi-Fi network, if any. Connectivity can be
 * established or torn down, and dynamic information about the state of
 * the network can be queried.</li>
 * <li>Results of access point scans, containing enough information to
 * make decisions about what access point to connect to.</li>
 * <li>It defines the names of various Intent actions that are broadcast
 * upon any sort of change in Wi-Fi state.
 * </ul>
 * This is the API to use when performing Wi-Fi specific operations. To
 * perform operations that pertain to network connectivity at an abstract
 * level, use {@link android.net.ConnectivityManager}.
 */
@RefineAs(WifiManager.class)
public class WifiManagerHidden {
    public List<WifiConfiguration> getPrivilegedConfiguredNetworks() {
        throw new RuntimeException("Stub!");
    }

    public int addNetwork(WifiConfiguration config) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Allow a previously configured network to be associated with. If
     * <code>disableOthers</code> is true, then all other configured
     * networks are disabled, and an attempt to connect to the selected
     * network is initiated. This may result in the asynchronous delivery
     * of state change events.
     * <p>
     * <b>Note:</b> If an application's target SDK version is
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP} or newer, network
     * communication may not use Wi-Fi even if Wi-Fi is connected; traffic may
     * instead be sent through another network, such as cellular data,
     * Bluetooth tethering, or Ethernet. For example, traffic will never use a
     * Wi-Fi network that does not provide Internet access (e.g. a wireless
     * printer), if another network that does offer Internet access (e.g.
     * cellular data) is available. Applications that need to ensure that their
     * network traffic uses Wi-Fi should use APIs such as
     * {@link Network#bindSocket(java.net.Socket)},
     * {@link Network#openConnection(java.net.URL)}, or
     * {@link ConnectivityManager#bindProcessToNetwork} to do so.
     *
     * @param netId         the ID of the network in the list of configured networks
     * @param disableOthers if true, disable all other networks. The way to
     *                      select a particular network to connect to is specify {@code true}
     *                      for this parameter.
     * @return {@code true} if the operation succeeded
     */
    public boolean enableNetwork(int netId, boolean disableOthers) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets the user choice for allowing auto-join to a network.
     * The updated choice will be made available through the updated config supplied by the
     * CONFIGURED_NETWORKS_CHANGED broadcast.
     * <p>
     * see <a href="https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/wifi/java/android/net/wifi/WifiManager.java;l=4360">WifiManager.java</a>
     *
     * @param netId         the id of the network to allow/disallow auto-join for.
     * @param allowAutojoin true to allow auto-join, false to disallow auto-join
     * @hide
     */
    public void allowAutojoin(int netId, boolean allowAutojoin) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Tell the supplicant to persist the current list of configured networks.
     * <p>
     * Note: It is possible for this method to change the network IDs of
     * existing networks. You should assume the network IDs can be different
     * after calling this method.
     *
     * @return {@code true} if the operation succeeded
     */
    public boolean saveConfiguration() {
        throw new RuntimeException("Stub!");
    }
}

// https://cs.android.com/android/platform/superproject/+/android-7.0.0_r36:frameworks/base/wifi/java/android/net/wifi/WifiManager.java
