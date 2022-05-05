package io.github.lgatodu47.meowrapper;

import java.io.File;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.lgatodu47.meowrapper.Logger.debug;

/**
 * Utility class with random methods going from Java-related to String-related.
 */
public class MeoWrapperUtils {
    // I'm just a beginner with regex, so these might be incorrect
    /**
     * Pattern matching for versions strings (commonly: 'x.y.z')
     */
    public static final Pattern VERSION_SCHEME = Pattern.compile("(\\d+\\.\\d+(\\.?(\\d+)?)+)");
    /**
     * Pattern matching for variable references declared with the following syntax: '${variable_name}'
     */
    public static final Pattern SUBSTITUTE_SCHEME = Pattern.compile("(\\$\\{([^}])+?})");

    /**
     * @return An Optional String representation of the path to the Java executable used to run the current program.
     */
    @SuppressWarnings("unchecked")
    public static Optional<String> getJavaExecutable() {
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
     * Gets the process id of the given process. This method is only effective on JVMs running on Java 9+.
     * @param process The process from which the pid will be obtained.
     * @return An Optional Long, empty if running on Java 1.8 or below, otherwise holding the pid.
     */
    public static Optional<Long> getProcessId(Process process) {
        try {
            return Optional.of((long) Process.class.getMethod("pid").invoke(process));
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    /**
     * Gets the Java version on which is running the current JVM. Used to check compatibility with Minecraft's required Java version.
     * @return The integer representing the major version of Java the program is currently running on.
     */
    public static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        }
        return Integer.parseInt(version);
    }

    /**
     * Concat two arrays of type T into one.
     * @param first The preceding array.
     * @param second The following array.
     * @param <T> The type of the two arrays to concat.
     * @return An array of length {@code first.length + second.length} containing all the elements in {@code first} and in {@code second}.
     */
    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    /**
     * Compares two version strings.
     * @param v1 The first version to compare.
     * @param v2 The second version to compare.
     * @return the value {@code 0} if {@code v1 == v2}; a value less than {@code 0} if {@code v2 < v1}; and a value greater than {@code 0} if {@code v2 > v1}
     */
    public static int compareVersions(String v1, String v2)
    {
        String[] versionNumbers1 = v1.split("\\.");
        String[] versionNumbers2 = v2.split("\\.");

        int[] maxVerNumbs = new int[Math.max(versionNumbers1.length, versionNumbers2.length)];
        Arrays.fill(maxVerNumbs, 0);

        // We copy the content of the strings into an array of the same size
        int[] verNumbs1 = maxVerNumbs.clone();
        int[] verNumbs2 = maxVerNumbs.clone();
        System.arraycopy(Arrays.stream(versionNumbers1).mapToInt(Integer::parseInt).toArray(), 0, verNumbs1, 0, versionNumbers1.length);
        System.arraycopy(Arrays.stream(versionNumbers2).mapToInt(Integer::parseInt).toArray(), 0, verNumbs2, 0, versionNumbers2.length);

        for(int i = 0; i < maxVerNumbs.length; i++)
        {
            int result = Integer.compare(verNumbs2[i], verNumbs1[i]); // we want to sort into descending order so the two values are inverted
            if(result == 0) continue;
            return result;
        }

        return 0;
    }

    /**
     * Replaces the variables references in the given String with the variables values defined in the substitution map.
     * Used for argument parsing in version Json files.
     *
     * @param str The String with the variable references to replace.
     * @param substitutionMap The map with all the declared variables assigned to their value.
     * @return {@code null} if no variable reference was found, otherwise the input String with all it's variables substituted.
     */
    public static String replaceSubstitutes(String str, Map<String, String> substitutionMap) {
        Matcher matcher = SUBSTITUTE_SCHEME.matcher(str);
        boolean hasResult = matcher.find();

        if (hasResult) {
            StringBuilder sb = new StringBuilder();
            int previousEnd = 0; // The end index of the previous match

            while (hasResult) {
                String replacement = matcher.group();
                String variable = replacement.substring(2, replacement.length() - 1);
                if(substitutionMap.containsKey(variable)) {
                    replacement = substitutionMap.get(variable);
                }
                sb.append(str, previousEnd, matcher.start());
                previousEnd = matcher.end();
                sb.append(replacement);
                hasResult = matcher.find();
            }

            sb.append(str.substring(previousEnd));
            return sb.toString();
        }

        return null;
    }

    /**
     * Similarly to {@link MeoWrapperUtils#replaceSubstitutes(String, Map)}, this method replaces variable references.
     * The only difference is that the substitution is applied to all the Strings in the {@code arguments} list.
     * It is mainly used for JVM argument substitution.
     *
     * @param arguments The list holding the Strings to check for variable references.
     * @param substitutionMap The same {@code substitutionMap} used in {@link MeoWrapperUtils#replaceSubstitutes(String, Map)}.
     * @return A new List holding the Strings with all variable references replaced.
     */
    public static List<String> replaceSubstitutes(List<String> arguments, Map<String, String> substitutionMap) {
        List<String> result = new ArrayList<>();

        for(String arg : arguments) {
            if(arg.equals("-cp") || arg.startsWith("-Djava.library.path")) {
                continue;
            }

            String formatted = replaceSubstitutes(arg, substitutionMap);
            if(formatted != null) {
                if(arg.equals(formatted)) {
                    continue;
                }
                arg = formatted;
            }

            result.add(arg);
        }

        return result;
    }

    /**
     * Splits the specified String into an array of Strings. It is called 'smart' because <strong>it omits the quoted character sequences</strong>.
     * This method is very useful for splitting program arguments (for example when you specify a String, the spaces in the String will be kept).
     *
     * @param text The String to split.
     * @return An array of Strings holding every part of the initial String.
     */
    public static String[] smartSplit(String text) {
        List<String> result = new ArrayList<>();

        for(String part = text; !part.isEmpty();) {
            int nextSpace = part.indexOf(' ');
            int nextQuote = part.indexOf('"');
            int nextQuote2 = part.indexOf('"', nextQuote  + 1);

            if(nextSpace == -1) { // last argument
                result.add(part);
                break;
            }

            if(nextQuote < nextSpace && nextQuote2 > -1) { // if the argument is fully quoted
                result.add(part.substring(0, nextQuote2 + 1));
                part = part.substring(nextQuote2);
            } else {
                result.add(part.substring(0, nextSpace));
                part = part.substring(nextSpace + 1);
            }
        }

        return result.toArray(new String[0]);
    }

    /**
     * A safe reference to a member that may throw a NullPointerException when normally referencing it.
     * Useful for Json-parsed values handling.
     *
     * @param ref A supplier holding the reference to that member.
     * @param <T> The type of the reference.
     * @return The value of the reference, or {@code null} if the reference failed or if it just returned null.
     * @see io.github.lgatodu47.meowrapper.json.Version Version Json parsing for a better understanding.
     */
    public static <T> T safeReference(Supplier<T> ref) {
        try {
            return ref.get();
        } catch (NullPointerException ignored) {
        }
        return null;
    }
}
