package android.content.pm;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(ApplicationInfo.class)
public class ApplicationInfoHidden {
    /**
     * Full path to the credential-protected directory assigned to the package
     * for its persistent data.
     *
     * @hide
     */
    public String credentialProtectedDataDir;

    /**
     * String retrieved from the seinfo tag found in selinux policy. This value
     * can be overridden with a value set through the mac_permissions.xml policy
     * construct. This value is useful in setting an SELinux security context on
     * the process as well as its data directory. The String default is being used
     * here to represent a catchall label when no policy matches.
     * <p>
     * {@hide}
     * <p>
     * see <a href="https://cs.android.com/android/platform/superproject/+/android-7.0.0_r36:frameworks/base/core/java/android/content/pm/ApplicationInfo.java;l=618">ApplicationInfo.java</a>
     */
    public String seinfo;

    /**
     * String retrieved from the seinfo tag found in selinux policy. This value
     * can be overridden with a value set through the mac_permissions.xml policy
     * construct. This value is useful in setting an SELinux security context on
     * the process as well as its data directory. The String default is being used
     * here to represent a catchall label when no policy matches.
     * <p>
     * {@hide}
     * <p>
     * see <a href="https://cs.android.com/android/platform/superproject/+/android-8.0.0_r51:frameworks/base/core/java/android/content/pm/ApplicationInfo.java;l=713">ApplicationInfo.java</a>
     */
    public String seInfo;

    /**
     * The seinfo tag generated per-user. This value may change based upon the
     * user's configuration. For example, when an instant app is installed for
     * a user. It is an error if this field is ever {@code null} when trying to
     * start a new process.
     * <p>NOTE: We need to separate this out because we modify per-user values
     * multiple times. This needs to be refactored since we're performing more
     * work than necessary and these values should only be set once. When that
     * happens, we can merge the per-user value with the seInfo state above.
     * <p>
     * {@hide}
     * <p>
     * see <a href="https://cs.android.com/android/platform/superproject/+/android-8.0.0_r51:frameworks/base/core/java/android/content/pm/ApplicationInfo.java;l=727">ApplicationInfo.java</a>
     */
    public String seInfoUser;
}
