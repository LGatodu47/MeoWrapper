package io.github.lgatodu47.meowrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static io.github.lgatodu47.meowrapper.Logger.*;

public class MeoWrapper {
    public static final Path RUNNING_PATH = tryDetectRunPath();

    private void start(Arguments args) {
        Optional<RuntimeEnvironment> optEnvironment = args.get("environment").flatMap(RuntimeEnvironment::byName);
        if(!optEnvironment.isPresent()) {
            error("Missing or invalid 'environment' argument! Type 'help' for more info about the needed arguments");
            return;
        }

        RuntimeEnvironment env = optEnvironment.get();

        Optional<String> optVersion = args.get("version");
        if(!optVersion.isPresent()) {
            error("Missing 'version' argument! Type 'help-<%s>' to have the list of arguments for the current environment", env.getName().toLowerCase(Locale.ROOT));
            return;
        }

        String version = optVersion.get();

        DEBUG = args.contains("debug");

        if(env.setup(args, version))
            launch(env);
    }

    private void launch(RuntimeEnvironment env) {
        List<String> commands = new ArrayList<>();
        // Java executable
        Optional<String> javaPath = env.getJavaPath();
        if(!javaPath.isPresent()) {
            javaPath = Utils.getJavaExecutable();
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
        commands.addAll(env.getVMOptions());
        commands.add(env.getMainClassName());
        commands.addAll(Arrays.asList(env.getLaunchArguments()));

        info("Launching Minecraft on '%s' with main '%s' and with args '%s'!", env.getName(), env.getMainClassName(), withoutAccessToken(env.getLaunchArguments()));

        Process process;
        try {
            process = new ProcessBuilder(commands).directory(env.getRunDir()).inheritIO().start();
        } catch (IOException e) {
            error("Failed to start process!");
            e.printStackTrace();
            return;
        }

        Utils.getProcessId(process).ifPresent(pid -> debug("Process with pid '%d' started.", pid));
        nl();

        try {
            debug("Process ended with exit code: '%d'", process.waitFor());
        } catch (InterruptedException e) {
            error("Process interrupted");
            e.printStackTrace();
        }
    }

    private static String withoutAccessToken(String[] args) {
        StringBuilder sb = new StringBuilder();

        sb.append('[');
        for(int i = 0; i < args.length; i++) {
            String arg = args[i];

            if(arg.substring(2).equals("accessToken")) {
                i++;
                continue;
            }

            sb.append(arg);
            if(i != args.length - 1)
                sb.append(", ");
        }
        sb.append(']');

        return sb.toString();
    }

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

    public static void main(String[] args) {
        if(args == null || args.length == 0) throw new NullPointerException("No arguments specified! Type 'help' for more info about the needed arguments!");
        if(logHelp(args[0])) return;
        new MeoWrapper().start(new Arguments(args));
    }
}
