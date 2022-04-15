package io.github.lgatodu47.meowrapper;

import java.util.Locale;

public enum Os {
    WINDOWS("windows", "win"),
    LINUX("linux", "linux", "unix"),
    OSX("osx", "mac"),
    UNKNOWN("unknown");

    private final String name;
    private final String[] aliases;

    Os(String name, String... aliases) {
        this.name = name;
        this.aliases = aliases;
    }

    public String getName() {
        return this.name;
    }

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
