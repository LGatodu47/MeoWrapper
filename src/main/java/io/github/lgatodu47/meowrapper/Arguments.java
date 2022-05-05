package io.github.lgatodu47.meowrapper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.lgatodu47.meowrapper.Logger.error;

/**
 * Utility class that parses String arrays into arguments and vice-versa.
 */
public class Arguments {
    /**
     * Unmodifiable Map that holds all the keys and values of every argument.
     */
    private final Map<String, String> argumentMap;

    /**
     * Simple constructor for Arguments that directly accepts a map of arguments.<br>
     * The specified {@code argumentMap} is copied and no change of the input map will be reflected on the internal {@link Arguments#argumentMap}.
     * @param argumentMap The map containing all the arguments mapped to keys and values.
     */
    public Arguments(Map<String, String> argumentMap) {
        this.argumentMap = new HashMap<>(argumentMap);
    }

    /**
     * Constructor for an Arguments instance that accepts a String array this time that contains all the arguments.
     * The specified array will be parsed to an argument map with no variable substitution applied its content.
     * @param args An array specifying all the arguments.
     * @see Arguments#Arguments(String[], Map) The third 'Arguments' constructor to understand the two handled arguments schemes.
     */
    public Arguments(String[] args) {
        this(args, null);
    }

    /**
     * Constructor for Arguments that accepts a String array and a variable substitution map.<br>
     * All the arguments are parsed in the constructor. There are two common parsing schemes for arguments:
     * <ul>
     *     <li>The first one being {@code '-argument=value'}</li>
     *     <li>The second one being {@code '--argument value'}</li>
     * </ul>
     * These are both handled by this constructor, and are also handle when they are mixed together.
     * @param args An array specifying all the arguments.
     * @param substitutionMap A map with all the variable declaration as map-key and their value as map-value. This map may be {@code null}.
     */
    public Arguments(String[] args, Map<String, String> substitutionMap) {
        this.argumentMap = new HashMap<>();
        for(int i = 0; i < args.length; i++) {
            String arg = args[i];
            if(arg.startsWith("--")) { // first argument scheme
                String key = arg.substring(2);
                String value = null;
                try {
                    value = args[i + 1];
                    if(!value.startsWith("--")) {
                        i++; // if it isn't a single argument we skip the value of this argument

                        if(substitutionMap != null) {
                            String formatted = MeoWrapperUtils.replaceSubstitutes(value, substitutionMap);
                            if(formatted != null) { // If a substitute was found
                                if(value.equals(formatted)) { // If no change we skip the argument
                                    continue;
                                }
                                value = formatted; // Even if everything wasn't resolved we can still accept the argument
                            }
                        }
                    }
                } catch (IndexOutOfBoundsException ignored) {
                }

                if(key.isEmpty()) {
                    error("Found invalid argument '%s' at index %d", arg, i);
                    continue;
                }

                argumentMap.put(key, value);
            } else if(arg.startsWith("-")) { // second argument scheme
                String key;
                String value;
                if(arg.contains("=")) {
                    key = arg.substring(1, arg.indexOf('='));
                    value = arg.substring(arg.indexOf('=') + 1);

                    if(value.isEmpty()) {
                        error("Found invalid argument '%s' at index %d", arg, i);
                        continue;
                    }

                    // Same thing as above
                    if(substitutionMap != null) {
                        String formatted = MeoWrapperUtils.replaceSubstitutes(value, substitutionMap);
                        if(formatted != null) {
                            if(value.equals(formatted)) {
                                continue;
                            }
                            value = formatted;
                        }
                    }
                } else {
                    key = arg.substring(1);
                    value = null;
                }

                if(key.isEmpty()) {
                    error("Found invalid argument '%s' at index %d", arg, i);
                    continue;
                }

                argumentMap.put(key, value);
            } else { // no argument
                error("Found invalid argument '%s' at index %d", arg, i);
            }
        }
    }

    /**
     * Gets a value from the argument map.
     *
     * @param key The corresponding key of the value to get.
     * @return An {@link Optional} containing the value if it exists.
     */
    public Optional<String> get(String key) {
        return Optional.ofNullable(argumentMap.get(key));
    }

    /**
     * Gets a path from the argument map. Overload method for {@link Arguments#get(String)}
     *
     * @param key The corresponding key of the path to get.
     * @param orElse The path to return if the value is absent.
     * @return The found path if it exists, otherwise the specified path.
     */
    public Path getPath(String key, Path orElse) {
        return get(key).map(Paths::get).orElse(orElse);
    }

    /**
     * Tries to obtain the specified launch arguments, defaults with an empty array if it fails.
     * @return A String array with all the launch arguments, otherwise and empty array.
     */
    public String[] getLaunchArguments() {
        return get("args").map(MeoWrapperUtils::smartSplit).orElseGet(() -> new String[0]);
    }

    /**
     * Checks if the given argument is present in the map. Useful for 'null' arguments (such as {@code --nogui}, {@code --debug}, {@code --updateVersionManifest}, and many other).
     *
     * @param argument The name of the argument.
     * @return {@code true} if the specified argument is present in this map (if it was specified, value may be {@code null}).
     */
    public boolean contains(String argument) {
        return argumentMap.containsKey(argument);
    }

    /**
     * Overloading method for {@link Arguments#toNonnullArgs(Map)}.
     * @return A String array containing the parsed arguments.
     */
    public String[] toNonnullArgs() {
        return toNonnullArgs(argumentMap);
    }

    /**
     * Overloading method for {@link Arguments#toArgs(Map)}.
     * @return A String array containing the parsed arguments.
     */
    public String[] toArgs() {
        return toArgs(argumentMap);
    }

    /**
     * Parses the stored argument map into a String array. <strong>This method omits keys mapped to {@code null} values.</strong><br>
     * Here is an example of an expected output: {@code ["--arg", "value", "--argument", "value"]}
     * @param argumentMap The map containing all the specified arguments.
     * @return A String array containing the parsed arguments.
     */
    public static String[] toNonnullArgs(Map<String, String> argumentMap) {
        String[] result = new String[(int) argumentMap.values().stream().filter(Objects::nonNull).count() * 2]; // we only want non-null entries
        List<Map.Entry<String, String>> list = argumentMap.entrySet().stream().filter(entry -> entry.getValue() != null).collect(Collectors.toList());

        for(int i = 0; i < list.size(); i++) {
            Map.Entry<String, String> entry = list.get(i);
            result[i * 2] = "--".concat(entry.getKey());
            result[i * 2 + 1] = entry.getValue();
        }

        return result;
    }

    /**
     * Parses the stored argument map into a String array. <strong>This method doesn't omit keys mapped to {@code null} values.</strong><br>
     * Here is an example of an expected output: {@code ["--arg", "value", "--null_arg", "--argument", "value"]}
     * @param argumentMap The map containing all the specified arguments.
     * @return A String array containing the parsed arguments.
     */
    public static String[] toArgs(Map<String, String> argumentMap) {
        String[] result = new String[argumentMap.values().stream().mapToInt(s -> s == null ? 1 : 2).sum()]; // Allocate 2 slots when a value exists, otherwise 1
        List<Map.Entry<String, String>> list = new ArrayList<>(argumentMap.entrySet());

        for(int i = 0, j = 0; i < list.size(); i++) {
            Map.Entry<String, String> entry = list.get(i);
            result[j++] = "--".concat(entry.getKey());
            if(entry.getValue() != null) result[j++] = entry.getValue();
        }

        return result;
    }
}
