package io.github.lgatodu47.meowrapper;

import java.util.Locale;

/**
 * Common Utility enum to identify the Operating System on which the JVM is running.
 */
public enum Os {
    WINDOWS("windows", "win"),
    LINUX("linux", "linux", "unix"),
    OSX("osx", "mac"),
    UNKNOWN("unknown");

    /**
     * The official name of the OS.
     */
    private final String name;
    /**
     * Aliases for the name of the OS.
     */
    private final String[] aliases;

    Os(String name, String... aliases) {
        this.name = name;
        this.aliases = aliases;
    }

    /**
     * Getter method for the official name of this operating system instance.
     * @return A String with the name of the os in lowercase.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Obtains the name of the current OS and tries to match it to one of the declared OSs above.
     * @return an enum value of {@link Os} describing the current OS.
     */
    public static Os getCurrent() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        for (Os os : Os.values()) {
            for (String alias : os.aliases) {
                if (osName.contains(alias)) {
                    return os;
                }
            }
        }
        return UNKNOWN;
    }
}
