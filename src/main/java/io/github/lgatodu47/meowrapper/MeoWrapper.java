package io.github.lgatodu47.meowrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static io.github.lgatodu47.meowrapper.Logger.*;

/**
 * Main class for MeoWrapper.
 * Everything from the start of the program to the launch of the game is detailed in this class.
 */
public class MeoWrapper {
    /**
     * Constant resolved when the class is loaded, even before the main method.
     * The program stops if it fails to find the run directory.
     */
    public static final Path RUNNING_PATH = tryDetectRunPath();

    /**
     * First method invoked by the {@link MeoWrapper#main(String[])} method.
     * It starts processing the specified program arguments and performs checks for mandatory arguments.
     * It finally sets up the runtime environment before launching the game.
     * @param args The input arguments parsed to an {@link Arguments} instance.
     */
    private void start(Arguments args) {
        Optional<RuntimeEnvironment> optEnvironment = args.get("environment").flatMap(RuntimeEnvironment::byName);
        if(!optEnvironment.isPresent()) {
            error("Missing or invalid 'environment' argument! Type 'help' for more info about the needed arguments");
            return;
        }

        RuntimeEnvironment env = optEnvironment.get();

        Optional<String> optVersion = args.get("version");
        if(!optVersion.isPresent()) {
            error("Missing 'version' argument! Type 'help-%s' to have the list of arguments for the current environment", env.getName().toLowerCase(Locale.ROOT));
            return;
        }

        String version = optVersion.get();

        // Debug logging works from there
        DEBUG = args.contains("debug");

        if(env.setup(args, version)) {
            if(args.contains("install")) { // We stop here on 'installation' mode
                info("Successfully installed Minecraft %s with all it's libraries and assets.", version);
                return;
            }

            launch(env);
        }
    }

    /**
     * Method called by the {@link MeoWrapper#start(Arguments)} method.
     * It will prepare the launch arguments and create a new process for Minecraft.
     * @param env The environment in which the game will be running.
     */
    private void launch(RuntimeEnvironment env) {
        List<String> commands = new ArrayList<>();
        // Java executable
        Optional<String> javaPath = env.getJavaPath();
        if(!javaPath.isPresent()) { // if no custom java executable specified in the arguments we use the one currently running MeoWrapper
            javaPath = MeoWrapperUtils.getJavaExecutable();
        }
        commands.add(javaPath.orElseGet(() -> {
            error("Could not find the current running java executable: setting process executable to 'java'");
            return "java";
        }));
        // Classpath
        debug("Listing classpath...");
        Optional<String> classpath = env.getClassPath().stream().map(File::getAbsolutePath).peek(Logger::debug).reduce((path, path2) -> path + ";" + path2);
        if(classpath.isPresent()) {
            commands.add("-cp");
            commands.add(classpath.get());
        } else {
            error("Classpath is absent!");
        }
        // VM options
        env.getVMOptions().stream().reduce((s, s2) -> s + " " + s2).ifPresent(s -> debug("Launching with VM Options: '%s'", s));
        commands.addAll(env.getVMOptions());
        commands.add(env.getMainClassName());
        commands.addAll(Arrays.asList(env.getLaunchArguments()));

        info("Launching Minecraft on '%s' with main '%s' and with args '%s'!", env.getName(), env.getMainClassName(), withoutAccessToken(env.getLaunchArguments()));

        Process process;
        try {
            process = new ProcessBuilder(commands).directory(env.getRunDir()).inheritIO().start(); // Process directly started
        } catch (IOException e) {
            error("Failed to start process!");
            e.printStackTrace();
            return;
        }

        MeoWrapperUtils.getProcessId(process).ifPresent(pid -> debug("Process with pid '%d' started.", pid));
        nl();

        try {
            debug("Process ended with exit code: '%d'", process.waitFor());
        } catch (InterruptedException e) {
            error("Process interrupted");
            e.printStackTrace();
        }
    }

    /**
     * A utility method that aims to hide the access token from the output.
     * @param args An Array of arguments that may contain an access token argument.
     * @return A String representation of the specified array with no access token revealed.
     */
    private static String withoutAccessToken(String[] args) {
        StringBuilder sb = new StringBuilder();

        sb.append('[');
        for(int i = 0; i < args.length; i++) {
            String arg = args[i];

            if(arg.substring(2).equals("accessToken")) {
                i++; // We therefore also skip the value
                continue;
            }

            sb.append(arg);
            if(i != args.length - 1)
                sb.append(", ");
        }
        sb.append(']');

        return sb.toString();
    }

    /**
     * Logs help messages about the multiple Minecraft environments and the required program arguments to launch MeoWrapper.<br>
     * When this method returns true the program stops.
     * @param firstArg The first program argument.
     * @return {@code true} if help was logged, otherwise {@code false}.
     */
    private static boolean logHelp(String firstArg) {
        if(firstArg.contains("help") || "?".equals(firstArg)) {
            if(firstArg.contains("help-")) {
                String envString = firstArg.substring(firstArg.lastIndexOf('-') + 1);
                Optional<RuntimeEnvironment> env = RuntimeEnvironment.byName(envString);

                if(env.isPresent()) {
                    env.get().logArgumentsList();
                } else {
                    error("Could not find environment with name '%s'!", envString);
                }
            } else {
                info("Welcome in MeoWrapper! To run the program, you will need to specify arguments depending on the running environment you want. There are currently 3 run environments: Client, Server and Data. To learn more about the required arguments of the environment of your choice, use the argument 'help-<env>'!");
            }
            return true;
        }
        return false;
    }


    /**
     * This method tries to detect the directory in which the program jar is running.
     * The run path is defined only once in the program in the constant {@link MeoWrapper#RUNNING_PATH}.<br>
     * Note that the program stops if this method couldn't locate the path.
     * @return A Path leading to the directory in which the program jar is located.
     */
    private static Path tryDetectRunPath() {
        try {
            Path runningPath = Paths.get(MeoWrapper.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            while (!Files.isDirectory(runningPath)) {
                runningPath = runningPath.getParent();
            }
            return runningPath;
        } catch (Exception e) {
            error("Failed to locate running dir! Try move the jar in another place");
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    /**
     * Main entrypoint for MeoWrapper.
     * @param args The program arguments.
     */
    public static void main(String[] args) {
        if(args == null || args.length == 0) throw new NullPointerException("No arguments specified! Type 'help' for more info about the needed arguments!");
        if(logHelp(args[0])) return;
        new MeoWrapper().start(new Arguments(args));
    }
}
