package io.github.lgatodu47.meowrapper;

/**
 * A package-private class containing logging methods for MeoWrapper.
 */
class Logger {
    static {
        if(!System.getProperties().containsKey("meowrapper.logger.disable_ansi"))
            System.out.println("[MeoWrapper/LOGGER] You can disable ANSI logging by setting the property 'meowrapper.logger.disable_ansi' to 'true'.");
    }

    static boolean DEBUG = false;
    static final boolean DISABLE_ANSI = Boolean.getBoolean("meowrapper.logger.disable_ansi");

    static final String INFO_COLOR = DISABLE_ANSI ? "" : "\033[0;32m"; // GREEN
    static final String DEBUG_COLOR = DISABLE_ANSI ? "" : "\033[0;36m"; // CYAN
    static final String WARN_COLOR = DISABLE_ANSI ? "" : "\033[0;33m"; // YELLOW
    static final String ERROR_COLOR = DISABLE_ANSI ? "" : "\033[0;31m"; // RED
    static final String RESET = DISABLE_ANSI ? "" : "\033[0m";

    static void info(String msg, Object... args) {
        log(INFO_COLOR, "INFO", msg, args);
    }

    static void debug(String msg, Object... args) {
        if(DEBUG) log(DEBUG_COLOR, "DEBUG", msg, args);
    }

    static void warn(String msg, Object... args) {
        log(WARN_COLOR, "WARN", msg, args);
    }

    static void error(String msg, Object... args) {
        log(ERROR_COLOR, "ERROR", msg, args);
    }

    private static void log(String color, String level, String msg, Object... args) {
        System.out.printf(color + "[MeoWrapper/" + level + "]: " + msg + "%n" + RESET, args);
    }

    static void nl() {
        System.out.println();
    }
}
