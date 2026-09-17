package android.net.wifi;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import dev.rikka.tools.refine.RefineAs;

/**
 * A class representing a configured Wi-Fi network, including the
 * security configuration.
 */
@RefineAs(WifiConfiguration.class)
public class WifiConfigurationHidden {
    /**
     * @hide Auto-join is allowed by user for this network.
     * Default true.
     * <p>
     * see <a href="https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/wifi/java/android/net/wifi/WifiConfiguration.java;l=866">WifiConfiguration.java</a>
     */
    @RequiresApi(Build.VERSION_CODES.R)
    public boolean allowAutojoin;

    public static final Parcelable.Creator<WifiConfiguration> CREATOR =
            new Parcelable.Creator<WifiConfiguration>() {
                public WifiConfiguration createFromParcel(Parcel in) {
                    throw new RuntimeException("Stub!");
                }

                public WifiConfiguration[] newArray(int size) {
                    throw new RuntimeException("Stub!");
                }
            };
}

// https://cs.android.com/android/platform/superproject/+/android-7.0.0_r36:frameworks/base/wifi/java/android/net/wifi/WifiConfiguration.java
