package io.github.lgatodu47.meowrapper;

/**
 * A package-private class containing logging methods for MeoWrapper.
 */
class Logger {
    static boolean DEBUG = false;

    static final String INFO_COLOR = "\033[0;32m"; // GREEN
    static final String DEBUG_COLOR = "\033[0;36m"; // CYAN
    static final String ERROR_COLOR = "\033[0;31m"; // RED

    static void info(String msg, Object... args) {
        System.out.printf(INFO_COLOR + "[MeoWrapper/INFO]: " + msg + "%n\033[0m", args);
    }

    static void debug(String msg, Object... args) {
        if(DEBUG) System.out.printf(DEBUG_COLOR + "[MeoWrapper/DEBUG]: " + msg + "%n\033[0m", args);
    }

    static void error(String msg, Object... args) {
        System.out.printf(ERROR_COLOR + "[MeoWrapper/ERROR]: " + msg + "%n\033[0m", args);
    }

    static void nl() {
        System.out.println();
    }
}
