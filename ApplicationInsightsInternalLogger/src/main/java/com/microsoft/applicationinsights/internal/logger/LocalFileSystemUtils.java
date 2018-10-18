package com.microsoft.applicationinsights.internal.logger;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Helper methods for dealing with files and folders.
 */
public class LocalFileSystemUtils {

    private static final List<String> CANDIDATE_USERNAME_ENVIRONMENT_VARIABLES =
            Collections.unmodifiableList(Arrays.asList("USER", "LOGNAME", "USERNAME"));

    /**
     * Finds a suitable folder to use for temporary files,
     * while avoiding the risk of collision when multiple users
     * are running applications that make use of Application Insights.
     *
     * See the third paragraph at http://www.chiark.greenend.org.uk/~peterb/uxsup/project/tmp-per-user/
     * for a great explanation of the motivation behind this method.
     *
     * @return a {@link File} representing a folder in which temporary files will be stored
     * for the current user.
     */
    public static File getTempDir() {
        final String tempDirectory = System.getProperty("java.io.tmpdir");
        final String currentUserName = determineCurrentUserName();

        final File result = getTempDir(tempDirectory, currentUserName);
        if (!result.isDirectory()) {
            // Noinspection ResultOfMethodCallIgnored
            result.mkdirs();
        }
        return result;
    }

    static File getTempDir(final String initialValue, final String userName) {
        String tempDirectory = initialValue;

        // does it look shared?
        // TODO: this only catches the Linux case; I think a few system users on Windows might share c:\Windows\Temp
        if ("/tmp".contentEquals(tempDirectory)) {
            final File candidate = new File(tempDirectory, userName);
            tempDirectory = candidate.getAbsolutePath();
        }

        final File result = new File(tempDirectory);
        return result;
    }

    /**
     *  Attempts to find the login/sign-in name of the user.
     *
     *  @return the best guess at what the current user's login name is.
     */
    public static String determineCurrentUserName() {
        String userName;
        // Start with the value of the "user.name" property
        userName = System.getProperty("user.name");

        if (StringUtils.isNotBlank(userName)) {
            // Try some environment variables
            for (final String candidate : CANDIDATE_USERNAME_ENVIRONMENT_VARIABLES) {
                userName = System.getenv(candidate);
                if (!StringUtils.isNotBlank(userName)) {
                    break;
                }
            }
        }

        if (StringUtils.isNotBlank(userName)) {
            // TODO: it might be nice to use a unique-ish value, such as the current process ID
            userName = "unknown";
        }

        return userName;
    }
}
