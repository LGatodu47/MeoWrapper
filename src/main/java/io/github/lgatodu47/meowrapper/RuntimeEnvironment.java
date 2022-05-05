package io.github.lgatodu47.meowrapper;

import io.github.lgatodu47.meowrapper.json.Version;
import io.github.lgatodu47.meowrapper.json.VersionManifest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.lgatodu47.meowrapper.Logger.*;

/**
 * Probably the most important class of the program.<br>
 * This class is separated in 3 inner subclasses: Client, Data and Server<br>
 * Each subclass corresponds to an available running environment for Minecraft.
 */
public abstract class RuntimeEnvironment {
    // Marked them unused so IntelliJ doesn't bother
    @SuppressWarnings("unused")
    public static final RuntimeEnvironment CLIENT = new Client();
    @SuppressWarnings("unused")
    public static final RuntimeEnvironment DATA = new Data();
    @SuppressWarnings("unused")
    public static final RuntimeEnvironment SERVER = new Server();

    /**
     * Similar to {@link Enum#valueOf(Class, String)} but with reflection to get all the fields.
     *
     * @param name The name of the environment
     * @return An Optional holding the Runtime environment if it was found.
     */
    public static Optional<RuntimeEnvironment> byName(String name) {
        Field[] fields = RuntimeEnvironment.class.getFields();

        for(Field field : fields) {
            if(field.getName().equalsIgnoreCase(name) && field.getType().equals(RuntimeEnvironment.class)) {
                try {
                    return Optional.of((RuntimeEnvironment) field.get(null));
                } catch (IllegalAccessException e) {
                    error("'%s' can't access its own fields: contact the author", RuntimeEnvironment.class.getName());
                    e.printStackTrace();
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Logs the list of all the available arguments for the current environment
     */
    public final void logArgumentsList() {
        info("Here is the list of all the arguments:");
        logArg("debug", "If specified, debug messages will be logged.", false);
        logArg("install", "If specified, the game will successfully be installed and won't be launched.", false);
        logArg("environment", "The running environment of the game: Client, Server or Data.", true);
        logArg("version", "The minecraft version you're using.", true);
        logArg("javaPath", "The path to a custom java executable for Minecraft.", false);
        logArg("vmOptions", "The JVM options for Minecraft launch.", false);
        logArg("mainClass", "The fully qualified name of the main class of jar (for custom jars or old versions).", false);
        logArg("args", "The additional arguments of the launching target.", false);
        info("Specific arguments to '%s' environment:", getName());
        logAdditionalArgs();
        nl();
        info("Specify arguments using '-argument=value' or '--argument value'");
    }

    /**
     * Logs the additional arguments specific to the current environment.
     */
    protected abstract void logAdditionalArgs();

    /**
     * @return The name of the main class for the current environment.
     */
    public abstract String getMainClassName();

    /**
     * Setup method for environments. Used to process the specified arguments and to prepare the launch.
     *
     * @param args The Arguments instance containing all the specified program arguments.
     * @param version The game version. Useful for downloads.
     * @return {@code true} if no problem was encountered.
     */
    public abstract boolean setup(Arguments args, String version);

    /**
     * @return An Optional holding a String representation of the absolute path of the java executable.
     */
    public abstract Optional<String> getJavaPath();

    /**
     * @return A file representation of the run directory for a specific environment.
     */
    public abstract File getRunDir();

    /**
     * @return A list containing all the files to add in the classpath (minecraft jar and libraries).
     */
    public abstract List<File> getClassPath();

    /**
     * @return A list of all the JVM options to specify for the process.
     */
    public abstract List<String> getVMOptions();

    /**
     * @return A String array containing all the launch arguments for the game.
     */
    public abstract String[] getLaunchArguments();

    /**
     * @return The name of the current environment.
     */
    public abstract String getName();

    /**
     * Prints in the console information about a program argument.
     *
     * @param name The name of the argument.
     * @param description The description of the argument.
     * @param mandatory If the argument is mandatory or not.
     */
    protected static void logArg(String name, String description, boolean mandatory) {
        info("'%s' - %s (%s)", name, description, mandatory ? "Mandatory" : "Optional");
    }

    /**
     * Parses a String into a String array.
     *
     * @param input The String representation of the array.
     * @return The parsed array.
     */
    protected static String[] parseArray(String input) {
        if(input.startsWith("[") && input.endsWith("]")) {
            String str = input.substring(1, input.length() - 1).replace(" ", "");
            return str.split(",");
        } else {
            return MeoWrapperUtils.smartSplit(input);
        }
    }

    /**
     * Client implementation of the RuntimeEnvironment
     */
    private static final class Client extends ClientJarEnvironment {
        private String mainClassName = "net.minecraft.client.main.Main";
        private String[] arguments;

        @Override
        public void logAdditionalArgs() {
            super.logAdditionalArgs();
            logArg("assetsDir", "The assets directory (downloads the assets in 'mcDir' if not set).", false);
            logArg("accessToken", "The access token for minecraft. Defaults to '0' if not set.", false);
            logArg("username", "The minecraft offline username you want.", false);
            logArg("width", "The width of the window.", false);
            logArg("height", "The height of the window.", false);
        }

        @Override
        public String getMainClassName() {
            return mainClassName;
        }

        @Override
        public boolean setup(Arguments args, String version) {
            if(!super.setup(args, version)) return false;

            String assetIndex = versionJson.assets;
            Path assetsDir = FileManager.gatherAssets(args.getPath("assetsDir", mcDir.resolve("assets")).toAbsolutePath(), versionJson);
            Optional<String> main = args.get("mainClass");
            if(!main.isPresent()) {
                main = Optional.ofNullable(versionJson.mainClass);
            }
            main.ifPresent(name -> mainClassName = name);
            arguments = MeoWrapperUtils.concat(args.getLaunchArguments(), new Arguments(versionJson.getGameArguments(), createArgsSubstitutionMap(assetsDir, assetIndex, args, runDir, version, libDir, nativesDir)).toArgs());

            return true;
        }

        @Override
        public String[] getLaunchArguments() {
            return arguments;
        }

        @Override
        public String getName() {
            return "Client";
        }

        /**
         * Makes a variable substitution map for version json game arguments parsing.
         * @param assetsDir The game's assets directory.
         * @param assetIndex The asset index name.
         * @param args The program arguments.
         * @param runDir The game's run directory.
         * @param versionName The name of the game version.
         * @param libDir The game's libraries directory.
         * @param nativesDir The game's natives directory.
         * @return A map with the variable names mapped to their corresponding value.
         */
        private static Map<String, String> createArgsSubstitutionMap(Path assetsDir, String assetIndex, Arguments args, Path runDir, String versionName, Path libDir, Path nativesDir) {
            Map<String, String> map = createVMArgsSubstitutionMap(runDir, versionName, libDir, nativesDir);
            map.put("assets_root", pathString(assetsDir));
            map.put("assets_index_name", assetIndex);
            map.put("auth_access_token", args.get("accessToken").orElse("0"));
            // These are the optional arguments
            args.get("username").ifPresent(username -> map.put("auth_player_name", username));
            args.get("width").ifPresent(width -> map.put("resolution_width", width));
            args.get("height").ifPresent(height -> map.put("resolution_height", height));
            return map;
        }
    }

    /**
     * Data implementation of the RuntimeEnvironment
     */
    private static final class Data extends ClientJarEnvironment {
        private String mainClassName = "net.minecraft.data.Main";
        private String[] arguments;

        @Override
        protected void logAdditionalArgs() {
            super.logAdditionalArgs();
            logArg("include", "The generators to include ('client', 'server', 'dev', 'reports' or 'all', defaults to 'all'). You must specify them with quotation marks if there are multiple ('\"reports, server\"'). They need to be comma-separated.", false);
            logArg("inputDirs", "The input directories (Useful for some data converting such as Nbt to SNbt).", false);
            logArg("outputDir", "The output directory for data (defaults to '<runDir>/generated').", false);
        }

        @Override
        public String getMainClassName() {
            return mainClassName;
        }

        @Override
        public boolean setup(Arguments args, String version) {
            if(MeoWrapperUtils.compareVersions(version, "1.13") > 0) {
                error("No data environment is available before 1.13!");
                return false;
            }

            if(!super.setup(args, version)) return false;

            args.get("mainClass").ifPresent(name -> mainClassName = name);
            arguments = MeoWrapperUtils.concat(args.get("args").map(s -> s.split(" ")).orElse(new String[0]), createDataRunArgs(
                    args.get("include").map(RuntimeEnvironment::parseArray).orElse(new String[] {"all"}),
                    FileManager.createDirectory(args.getPath("outputDir", runDir.resolve("generated"))),
                    args
            ));

            return true;
        }

        @Override
        public String[] getLaunchArguments() {
            return arguments;
        }

        @Override
        public String getName() {
            return "Data";
        }

        private static String[] createDataRunArgs(String[] generators, Path outputDir, Arguments args) {
            Map<String, String> map = new LinkedHashMap<>();
            for(String generator : generators) {
                map.put(generator, null);
            }
            map.put("output", pathString(outputDir));
            args.get("inputDirs").ifPresent(inputDirs -> map.put("input", inputDirs));
            return Arguments.toArgs(map);
        }
    }

    /**
     * Server implementation of the RuntimeEnvironment
     */
    private static final class Server extends RuntimeEnvironment {
        private Path javaPath;
        private Path serverDir;
        private File serverJar;
        private String mainClassName = "net.minecraft.server.Main";
        private String[] arguments;
        private List<File> libraries;
        private List<String> vmOptions;

        @Override
        public void logAdditionalArgs() {
            logArg("serverDir", "The running directory of your server (if absent will create a new 'server' folder in the jar folder).", false);
            logArg("serverJar", "The jar file of your server (if not specified, the program will download minecraft's bundled server jar in <serverDir>).", false);
            logArg("bundled", "If the specified jar is a bundled server jar (which contains all the libraries). Only used when <serverJar> is specified.", false);
            logArg("libDir", "The libraries directory (needed when a non-bundled <serverJar> is specified).", false);
            logArg("nativesDir", "The natives directory (Used in special cases such as for lwjgl natives).", false);
            logArg("nogui", "Disables the server info gui.", false);
            logArg("port", "The port of the running server.", false);
        }

        @Override
        public String getMainClassName() {
            return mainClassName;
        }

        @Override
        public boolean setup(Arguments args, String version) {
            javaPath = args.getPath("javaPath", null);
            serverDir = FileManager.createDirectory(args.getPath("serverDir", MeoWrapper.RUNNING_PATH.resolve("minecraft-server")));

            vmOptions = new ArrayList<>();
            Optional<Path> serverJarOpt = args.get("serverJar").map(Paths::get).filter(Files::exists).filter(Files::isRegularFile);
            if(args.contains("bundled") && serverJarOpt.isPresent()) {
                serverJar = serverJarOpt.get().toFile();
                mainClassName = "net.minecraft.bundler.Main";
                libraries = new ArrayList<>();
            } else {
                Version versionJson;
                Optional<VersionManifest> manifestOpt = FileManager.getVersionManifestData();
                if(!manifestOpt.isPresent()) {
                    return false;
                } else {
                    try {
                        versionJson = FileManager.downloadVersionJson(args.getPath("versionJson", serverDir.resolve(version.concat(".json"))), version, manifestOpt.get(), name -> serverDir.resolve(name + ".json"));
                    } catch (IOException e) {
                        error("Error when downloading version Json file!");
                        e.printStackTrace();
                        return false;
                    }
                }

                if(versionJson.javaVersion != null && javaPath == null) {
                    if(MeoWrapperUtils.getJavaVersion() < versionJson.javaVersion.majorVersion) {
                        warn("According to the version json, you are not running on a recent-enough java major version. The game may crash! (Current major java version: '%s', required: '%s')", MeoWrapperUtils.getJavaVersion(), versionJson.javaVersion.majorVersion);
                    }
                }

                if(serverJarOpt.isPresent()) {
                    List<Path> extractedNatives = new ArrayList<>();
                    serverJar = serverJarOpt.get().toFile();
                    libraries = FileManager.getLibraries(args.getPath("libDir", serverDir.resolve("libraries")), args.getPath("nativesDir", serverDir.resolve("natives")), versionJson, extractedNatives::add);
                    extractedNatives.stream().map(Path::toAbsolutePath).map(Path::toString).reduce((path, path2) -> path + ";" + path2).map("-Djava.library.path="::concat).ifPresent(vmOptions::add);
                } else {
                    serverJar = FileManager.downloadVersionJar(serverDir.resolve("bundled-".concat(version).concat(".jar")), versionJson, true);
                    mainClassName = "net.minecraft.bundler.Main";
                    libraries = new ArrayList<>();
                }
            }

            args.get("vmOptions").map(RuntimeEnvironment::parseArray).map(Arrays::asList).ifPresent(vmOptions::addAll);
            args.get("mainClass").ifPresent(name -> mainClassName = name);
            arguments = MeoWrapperUtils.concat(args.get("args").map(s -> s.split(" ")).orElse(new String[0]), createServerRunArgs(args));

            return true;
        }

        @Override
        public Optional<String> getJavaPath() {
            return Optional.ofNullable(javaPath).map(Path::toAbsolutePath).map(Path::toString);
        }

        @Override
        public File getRunDir() {
            return serverDir.toFile();
        }

        @Override
        public List<File> getClassPath() {
            List<File> classpath = new ArrayList<>(libraries);
            classpath.add(serverJar);
            return classpath;
        }

        @Override
        public List<String> getVMOptions() {
            return vmOptions;
        }

        @Override
        public String[] getLaunchArguments() {
            return arguments;
        }

        @Override
        public String getName() {
            return "Server";
        }

        private static String[] createServerRunArgs(Arguments args) {
            Map<String, String> map = new LinkedHashMap<>();
            if(args.contains("nogui")) map.put("nogui", null);
            args.get("port").ifPresent(port -> map.put("port", port));
            return Arguments.toArgs(map);
        }
    }

    /**
     * An implementation of RuntimeEnvironment for the environments using the client jar.
     */
    private static abstract class ClientJarEnvironment extends RuntimeEnvironment {
        protected Path javaPath;
        protected Path mcDir;
        protected Path runDir;
        protected Version versionJson;
        protected File mcJar;
        protected Path libDir;
        protected Path nativesDir;
        protected List<File> libraries;
        protected List<String> vmOptions;

        @Override
        protected void logAdditionalArgs() {
            logArg("mcDir", "The .minecraft directory (leave empty to create one where the jar is located, you can specify an existent minecraft home).", false);
            logArg("runDir", "The run directory of your game (by default it's the same as the minecraft home dir).", false);
            logArg("updateVersionManifest", "If specified version manifest will be re-downloaded (as it isn't updated by default: for e.g. if you download the version manifest on mc 1.18.2 and 1.19 comes out a month after, you will need to update the version manifest to download the version).", false);
            logArg("versionJson", "The x.x.x.json file of the game (Downloaded if file is non-existent or if not set). Mandatory for custom jars.", false);
            logArg("mcJar", "The client/server jar file of the game (useful for custom jars).", false);
            logArg("libDir", "The libraries directory (downloads the libraries in 'mcDir' if not set).", false);
            logArg("nativesDir", "The natives directory (Used in special cases such as for lwjgl natives).", false);
        }

        @Override
        public boolean setup(Arguments args, String version) {
            javaPath = args.getPath("javaPath", null);
            mcDir = FileManager.createDirectory(args.getPath("mcDir", MeoWrapper.RUNNING_PATH.resolve(".minecraft")));
            runDir = FileManager.createDirectory(args.getPath("runDir", mcDir));
            FileManager.createFakeLauncherProfiles(mcDir);

            try {
                VersionManifest manifest = VersionManifest.read(FileManager.downloadVersionManifest(mcDir.resolve("versions/version_manifest.json"), args.contains("updateVersionManifest")));
                versionJson = FileManager.downloadVersionJson(args.getPath("versionJson", mcDir.resolve("versions/" + version + "/" + version + ".json")), version, manifest, name -> mcDir.resolve("versions/" + name + '/' + name + ".json"));
            } catch (IOException e) {
                error("Error when loading version Json file!");
                e.printStackTrace();
                return false;
            }

            if(versionJson.javaVersion != null && javaPath == null) {
                if(MeoWrapperUtils.getJavaVersion() < versionJson.javaVersion.majorVersion) {
                    warn("According to the version json, you are not running on a recent-enough java major version. The game may crash! (Current major java version: '%s', required: '%s')", MeoWrapperUtils.getJavaVersion(), versionJson.javaVersion.majorVersion);
                }
            }

            mcJar = FileManager.downloadVersionJar(args.getPath("mcJar", mcDir.resolve("versions/" + version + "/" + version + ".jar")), versionJson, false);

            List<Path> extractedNatives = new ArrayList<>();
            libDir = args.getPath("libDir", mcDir.resolve("libraries")).toAbsolutePath();
            nativesDir = args.getPath("nativesDir", mcDir.resolve("natives")).toAbsolutePath();

            libraries = FileManager.getLibraries(libDir, nativesDir, versionJson, extractedNatives::add);
            vmOptions = MeoWrapperUtils.replaceSubstitutes(versionJson.getJVMArguments().stream().filter(arg -> !arg.equals("-cp") && !arg.startsWith("-Djava.library.path")).collect(Collectors.toList()), createVMArgsSubstitutionMap(runDir, version, libDir, nativesDir));

            extractedNatives.stream().map(Path::toAbsolutePath).map(Path::toString).reduce((path, path2) -> path + ";" + path2).map("-Djava.library.path="::concat).ifPresent(vmOptions::add);
            args.get("vmOptions").map(RuntimeEnvironment::parseArray).map(Arrays::asList).ifPresent(vmOptions::addAll);
            return true;
        }

        @Override
        public Optional<String> getJavaPath() {
            return Optional.ofNullable(javaPath).map(Path::toAbsolutePath).map(Path::toString);
        }

        @Override
        public File getRunDir() {
            return runDir.toFile();
        }

        @Override
        public List<File> getClassPath() {
            List<File> classpath = new ArrayList<>(libraries);
            classpath.add(mcJar);
            return classpath;
        }

        @Override
        public List<String> getVMOptions() {
            return vmOptions;
        }

        /**
         * Makes a variable substitution map for version json VM arguments parsing.
         * @param runDir The run directory of the game.
         * @param versionName The name of the minecraft version.
         * @param libDir The game's library directory.
         * @param nativesDir The game's natives directory.
         * @return A map with the variable names mapped to their corresponding value.
         */
        protected static Map<String, String> createVMArgsSubstitutionMap(Path runDir, String versionName, Path libDir, Path nativesDir) {
            Map<String, String> substitutionMap = new HashMap<>();
            substitutionMap.put("game_directory", pathString(runDir));
            substitutionMap.put("version_name", versionName);
            substitutionMap.put("library_directory", pathString(libDir));
            substitutionMap.put("natives_directory", pathString(nativesDir));
            substitutionMap.put("classpath_separator", ";");
            return substitutionMap;
        }

        /**
         * Parses the specified path to an absolute path string.
         * @param path The path to parse.
         * @return A string representation of the absolute input path with unix file separators.
         */
        protected static String pathString(Path path) {
            String str = path.toAbsolutePath().toString();
            return Os.WINDOWS.equals(Os.getCurrent()) ? str.replace(File.separatorChar, '/') : str;
        }
    }
}
