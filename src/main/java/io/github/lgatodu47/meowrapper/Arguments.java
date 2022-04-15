package io.github.lgatodu47.meowrapper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static io.github.lgatodu47.meowrapper.Logger.*;

/**
 * Utility class that parses String arrays into arguments and vice-versa.
 * The {@link Arguments#argumentMap} should remain unmodifiable.
 */
public class Arguments {
    private final Map<String, String> argumentMap;

    public Arguments(Map<String, String> argumentMap) {
        this.argumentMap = new HashMap<>(argumentMap);
    }

    public Arguments(String[] args) {
        this.argumentMap = new HashMap<>();
        for(int i = 0; i < args.length; i++) {
            String arg = args[i];
            if(arg.startsWith("--")) { // first argument scheme
                String key = arg.substring(2);
                String value = null;
                try {
                    value = args[i + 1];
                    if(!value.startsWith("--")) i++; // if it isn't a single argument we skip the value of this argument
                } catch (IndexOutOfBoundsException ignored) {
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
     * Gets a path from the argument map.
     *
     * @param key The corresponding key of the path to get.
     * @param orElse The path to return if the value is absent.
     * @return The found path if it exists, otherwise the specified path.
     */
    public Path getPath(String key, Path orElse) {
        return get(key).map(Paths::get).orElse(orElse);
    }

    /**
     * @param argument The name of the argument.
     * @return {@code true} if the specified argument is present in this map (if it was specified, value may be {@code null}).
     */
    public boolean contains(String argument) {
        return argumentMap.containsKey(argument);
    }

    /**
     * Parses the stored argument map into a String array. <strong>This method omits keys mapped to {@code null} values.</strong><br>
     * Here is an example of an expected output: {@code ["--arg", "value", "--argument", "value"]}
     * @return A String array containing the parsed arguments.
     */
    public String[] toNonnullArgs() {
        String[] result = new String[(int) argumentMap.values().stream().filter(Objects::nonNull).count() * 2]; // we only want non-null entries
        List<Map.Entry<String, String>> list = argumentMap.entrySet().stream().filter(entry -> entry.getValue() != null).toList();

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
     * @return A String array containing the parsed arguments.
     */
    public String[] toArgs() {
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
