package io.github.lgatodu47.meowrapper;

import java.io.File;
import java.util.Optional;

import static io.github.lgatodu47.meowrapper.Logger.debug;

class Utils {
    /**
     * @return An Optional String representation of the path to the Java executable used to run the current program.
     */
    @SuppressWarnings("unchecked")
    static Optional<String> getJavaExecutable() {
        try { // For Java 9+
            Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
            Class<?> infoClass = Class.forName("java.lang.ProcessHandle$Info");
            Object handle = processHandleClass.getMethod("current").invoke(null);
            Object info = processHandleClass.getMethod("info").invoke(handle);
            Optional<String> result = (Optional<String>) infoClass.getMethod("command").invoke(info);

            debug("Got java executable from ProcessHandle");
            return result;
        } catch (Throwable ignored) { // For Java 1.8 and below
            String javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + (Os.getCurrent().equals(Os.WINDOWS) ? "java.exe" : "java");
            return new File(javaExe).isFile() ? Optional.of(javaExe) : Optional.empty();
        }
    }

    /**
     * Gets the process id of the given process.
     * @param process The given process.
     * @return An Optional holding the pid. The optional may only present if the current Java version is above 1.8.
     */
    static Optional<Long> getProcessId(Process process) {
        try {
            return Optional.of((long) Process.class.getMethod("pid").invoke(process));
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    /**
     * @return The integer representing the major version of Java the program is currently running on.
     */
    static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        }
        return Integer.parseInt(version);
    }
}
