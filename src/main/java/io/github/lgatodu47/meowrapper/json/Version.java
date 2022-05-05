package io.github.lgatodu47.meowrapper.json;

import com.google.gson.*;
import io.github.lgatodu47.meowrapper.MeoWrapperUtils;
import io.github.lgatodu47.meowrapper.Os;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.github.lgatodu47.meowrapper.MeoWrapperUtils.safeReference;

/**
 * Java class representing the client version Json file.
 */
public class Version {
    /**
     * Unique instance of Gson with a custom type adapter for {@link ArgumentInfo}.
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(ArgumentInfo.class, new ArgumentInfo.Deserializer()).create();

    /**
     * VM and program args.
     */
    public RuntimeArgs arguments;
    /**
     * The legacy game arguments represented in a String.
     */
    public String minecraftArguments;
    /**
     * Asset Index info.
     */
    public AssetIndex assetIndex;
    /**
     * Asset index id.
     */
    public String assets;
    /**
     * Map having the version jars download info as value and their name as key.
     */
    public Map<String, DownloadInfo> downloads;
    /**
     * Version name.
     */
    public String id;
    /**
     * Minecraft Java version info.
     */
    public JavaVersion javaVersion;
    /**
     * List of required libraries.
     */
    public Library[] libraries;
    /**
     * Path to the main class.
     */
    public String mainClass;
    /**
     * Name of the parent version.
     */
    public String inheritsFrom;

    /**
     * Gathers all the allowed game arguments specified in the version Json file.
     * @return a String array with all the game arguments.
     */
    public String[] getGameArguments() {
        if(minecraftArguments != null) { // 1.12 and below
            return MeoWrapperUtils.smartSplit(minecraftArguments);
        }

        if(safeReference(() -> arguments.game) == null) return new String[0];
        return Arrays.stream(arguments.game).filter(ArgumentInfo::allowed).map(arg -> arg.args).flatMap(Arrays::stream).toArray(String[]::new);
    }

    /**
     * Gathers all the allowed JVM arguments specified in the version Json file.
     * @return a String array with all the JVM arguments.
     */
    public List<String> getJVMArguments() {
        if(safeReference(() -> arguments.jvm) == null) return Collections.emptyList();
        return Arrays.stream(arguments.jvm).filter(ArgumentInfo::allowed).map(arg -> arg.args).flatMap(Arrays::stream).collect(Collectors.toList());
    }

    /**
     * Class holding the game arguments and the JVM arguments.
     */
    public static class RuntimeArgs {
        /**
         * Game arguments info.
         */
        public ArgumentInfo[] game;
        /**
         * JVM argument info.
         */
        public ArgumentInfo[] jvm;

        /**
         * Merges two RuntimeArgs instance into one.
         * @param parent The parent instance.
         * @param child The child instance.
         * @return a new RuntimeArgs object with all argument info merged.
         */
        public static RuntimeArgs merge(RuntimeArgs parent, RuntimeArgs child) {
            RuntimeArgs merged = new RuntimeArgs();

            if(parent == null) {
                parent = new RuntimeArgs();
            }

            if(child == null) {
                child = new RuntimeArgs();
            }

            if(parent.game == null) {
                merged.game = child.game;
            } else {
                if(child.game == null) {
                    merged.game = parent.game;
                } else {
                    merged.game = MeoWrapperUtils.concat(parent.game, child.game);
                }
            }

            if(parent.jvm == null) {
                merged.jvm = child.jvm;
            } else {
                if(child.jvm == null) {
                    merged.jvm = parent.jvm;
                } else {
                    merged.jvm = MeoWrapperUtils.concat(parent.jvm, child.jvm);
                }
            }

            return merged;
        }
    }

    /**
     * Info describing an argument.
     */
    public static class ArgumentInfo {
        /**
         * Conditions that the argument must check to be allowed.
         */
        public Rule[] rules;
        /**
         * The literal arguments.
         */
        public String[] args;

        /**
         * Constructor for deserialization.
         * @param rules The argument rules.
         * @param args The literal arguments.
         */
        public ArgumentInfo(Rule[] rules, String[] args) {
            this.rules = rules;
            this.args = args;
        }

        /**
         * Checks if this argument is allowed to be in the runtime arguments.
         * @return {@code true} if the argument is allowed.
         */
        public boolean allowed() {
            if (rules != null) {
                for (Rule rule : rules) {
                    if (rule.allows()) {
                        continue;
                    }
                    return false;
                }
            }
            return true;
        }

        /**
         * Deserializer class for ArgumentInfo.
         */
        public static class Deserializer implements JsonDeserializer<ArgumentInfo> {
            @Override
            public ArgumentInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (json.isJsonPrimitive()) {
                    return new ArgumentInfo(null, new String[] {json.getAsString()});
                }

                JsonObject object = json.getAsJsonObject();
                if (!object.has("value")) {
                    throw new JsonParseException("Error parsing arguments in version Json. Missing member 'value'.");
                }
                JsonElement value = object.get("value");

                return new ArgumentInfo(GSON.fromJson(object.get("rules"), Rule[].class), value.isJsonPrimitive() ? new String[] {value.getAsString()} : GSON.fromJson(value, String[].class));
            }
        }
    }

    /**
     * Class describing a set of conditions to match.
     */
    public static class Rule {
        /**
         * The action of the rule: "allow" or "disallow".
         */
        public String action;
        /**
         * The os condition to match.
         */
        public OsCondition os;
        /**
         * The features to match (don't really know a lot about these).
         */
        public Map<String, Object> features;

        /**
         * Checks if the conditions described by this rule are matched.
         * @return {@code true} if the conditions are matched.
         */
        public boolean allows() {
            return (features == null || features.containsKey("has_custom_resolution")) && (os == null || os.platformCondition() == action.equals("allow"));
        }
    }

    /**
     * Represents a condition for a specific operating system.
     */
    public static class OsCondition {
        /**
         * Name of the Os.
         */
        public String name;
        /**
         * Version of the Os.
         */
        public String version;
        /**
         * Architecture of the Os.
         */
        public String arch;

        /**
         * Checks if the name corresponds to current's Os name.
         * @return {@code true} if the names matches or if no name was specified.
         */
        public boolean nameEquals() {
            return name == null || Os.getCurrent().getName().equals(name);
        }

        /**
         * Checks if the version corresponds to current's Os version.
         * @return {@code true} if the versions matches or if no version was specified.
         */
        public boolean versionEquals() {
            return version == null || Pattern.compile(version).matcher(System.getProperty("os.version")).find();
        }

        /**
         * Checks if the architecture corresponds to current's Os architecture.
         * @return {@code true} if the architectures matches or if no name was specified.
         */
        public boolean archEquals() {
            return arch == null || Pattern.compile(arch).matcher(System.getProperty("os.arch")).find();
        }

        /**
         * Checks if everything matches with the current Os.
         * @return {@code true} if the required Os configuration matches.
         */
        public boolean platformCondition() {
            return nameEquals() && versionEquals() && archEquals();
        }
    }

    /**
     * Class describing a library info.
     */
    public static class Library {
        /**
         * Name of that library (following the scheme 'group:name:version').
         */
        public String name;
        /**
         * The URL leading to the maven repository where this library can be found.
         * Used for Fabric.
         */
        public URL url;
        /**
         * A map with as key the name of the Os and as value the classifier corresponding to that Os.
         */
        public Map<String, String> natives;
        /**
         * Download info of the library
         */
        public Downloads downloads;
        /**
         * Download rules of the library.
         */
        public Rule[] rules;
        /**
         * Natives extraction info for this library.
         */
        public ExtractInfo extract;

        /**
         * Cache value for the path of the main library artifact.
         */
        private String _artifactPath;

        /**
         * Gets the artifact path specified in the download info, or guesses it with the name of the library if undefined.<br>
         * This method is used for version Json that doesn't respect version Json conventions (such as Optifine's one -_-).
         * @return The path of the artifact if it was found, otherwise {@code null}.
         */
        public String getArtifactPath() {
            if(_artifactPath != null) return _artifactPath;

            String artifactPath = safeReference(() -> downloads.artifact.path);
            if(artifactPath != null) {
                return (_artifactPath = artifactPath);
            }

            String[] splitName = name.split(":");
            try {
                String artifactName = splitName[1];
                String artifactVersion = splitName[2];

                if(artifactName != null && artifactVersion != null) {
                    return (_artifactPath = (splitName[0] + '/' + artifactName + '/' + artifactVersion + '/' + artifactName + '-' + artifactVersion + ".jar"));
                }
            } catch (IndexOutOfBoundsException ignored) {
            }

            return null;
        }

        /**
         * Cache value for the URL of the main library artifact.
         */
        private URL _artifactUrl;

        /**
         * Gets the artifact URL in the download info if it is specified, otherwise tries to find it with the maven repository url specified in the library.
         * @return The URL of the artifact if it was found, otherwise {@code null}.
         */
        public URL getDownloadURL() {
            if(_artifactUrl != null) return _artifactUrl;

            URL artifactUrl = safeReference(() -> downloads.artifact.url);
            if(artifactUrl != null) {
                return (_artifactUrl = artifactUrl);
            }

            String[] splitName = name.split(":");
            try {
                String artifactLocation = splitName[0].replace('.', '/');
                String artifactName = splitName[1];
                String artifactVersion = splitName[2];

                if(artifactName != null && artifactVersion != null && url != null) {
                    return (_artifactUrl = new URL(url, artifactLocation + '/' + artifactName + '/' + artifactVersion + '/' + artifactName + '-' + artifactVersion + ".jar"));
                }
            } catch (Throwable ignored) {
            }

            return null;
        }

        /**
         * Checks if the library is allowed on runtime.
         * @return {@code true} if all the rules conditions matches and the library is allowed.
         */
        public boolean isAllowed() {
            if(rules != null) {
                for(Rule rule : rules) {
                    if(!rule.allows()) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    /**
     * Download info of a library.
     */
    public static class LibraryDownloadInfo extends DownloadInfo {
        /**
         * Path where the library will be downloaded (concatenated to the 'libraries' dir path).
         */
        public String path;
    }

    /**
     * Asset Index info.
     */
    public static class AssetIndex extends DownloadInfo {
        /**
         * Name of that asset index.
         */
        public String id;
    }

    /**
     * Basic info about a download.
     */
    public static class DownloadInfo {
        /**
         * Sha1 hash of the file to download.
         */
        public String sha1;
        /**
         * URL leading to the file to download.
         */
        public URL url;
    }

    /**
     * Object that holds info about the files to download for the library.
     */
    public static class Downloads {
        /**
         * Map with a download info associated to each classifier (classifiers may be natives or sources).
         */
        public Map<String, LibraryDownloadInfo> classifiers;
        /**
         * Download info of the main artifact.
         */
        public LibraryDownloadInfo artifact;
    }

    /**
     * Info about the library's natives to extract.
     */
    public static class ExtractInfo {
        /**
         * A list of paths to exclude from extraction.
         */
        public List<String> exclude;
    }

    /**
     * Java version info.
     */
    public static class JavaVersion {
        /**
         * Integer representing the major version of Java.
         */
        public int majorVersion;
    }

    /**
     * Merges two Versions instance into one.
     * @param parent The parent version.
     * @param child The child version.
     * @return a new Version object with all version info merged.
     */
    public static Version merge(Version parent, Version child) {
        Version merged = new Version();

        merged.arguments = RuntimeArgs.merge(parent.arguments, child.arguments);
        merged.minecraftArguments = child.minecraftArguments == null ? parent.minecraftArguments : child.minecraftArguments;
        merged.assetIndex = child.assetIndex == null ? parent.assetIndex : child.assetIndex;
        merged.assets = child.assets == null ? parent.assets : child.assets;

        if(child.downloads == null) {
            merged.downloads = parent.downloads == null ? null : new HashMap<>(parent.downloads);
        } else {
            if(parent.downloads == null) {
                merged.downloads = new HashMap<>(child.downloads);
            } else {
                Map<String, DownloadInfo> map = new HashMap<>(parent.downloads);
                map.putAll(child.downloads);
                merged.downloads = map;
            }
        }

        merged.id = child.id == null ? parent.id : child.id;
        merged.javaVersion = child.javaVersion == null ? parent.javaVersion : child.javaVersion;

        if(child.libraries == null) {
            merged.libraries = parent.libraries == null ? null : Arrays.copyOf(parent.libraries, parent.libraries.length);
        } else {
            if(parent.libraries == null) {
                merged.libraries = Arrays.copyOf(child.libraries, child.libraries.length);
            } else {
                merged.libraries = MeoWrapperUtils.concat(parent.libraries, child.libraries);
            }
        }

        merged.mainClass = child.mainClass == null ? parent.mainClass : child.mainClass;
        merged.inheritsFrom = child.inheritsFrom;

        return merged;
    }

    /**
     * Parses the specified file's content into a Version object.
     * @param file The version Json file.
     * @return A Version object parsed from the version Json file.
     * @throws IOException If some I/O error occurs when creating the FileReader.
     */
    public static Version read(File file) throws IOException {
        return GSON.fromJson(new FileReader(file), Version.class);
    }
}
