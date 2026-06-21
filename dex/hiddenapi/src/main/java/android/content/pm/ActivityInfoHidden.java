package android.content.pm;

import dev.rikka.tools.refine.RefineAs;

/**
 * @see <a href="https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:frameworks/base/core/java/android/content/pm/ActivityInfo.java">ActivityInfo.java</a>
 */
@RefineAs(ActivityInfo.class)
public class ActivityInfoHidden {

    /**
     * @RequiresApi(api = Build.VERSION_CODES.P)
     * @see <a href="https://cs.android.com/android/platform/superproject/+/android-8.1.0_r71:frameworks/base/core/java/android/content/pm/ActivityInfo.java;l=440">ActivityInfo.java</a>
     */
    public static final int FLAG_SUPPORTS_PICTURE_IN_PICTURE = 0x400000;
}
