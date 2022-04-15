package io.github.lgatodu47.meowrapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import static io.github.lgatodu47.meowrapper.Logger.*;

public class MeoWrapper {
    public static final Path RUNNING_PATH = tryDetectRunPath();

    private void start(Arguments args) {
        Optional<RuntimeEnvironment> optEnvironment = args.get("environment").flatMap(RuntimeEnvironment::byName);
        if(optEnvironment.isEmpty()) {
            error("Missing or invalid 'environment' argument! Type 'help' for more info about the needed arguments");
            return;
        }

        RuntimeEnvironment env = optEnvironment.get();

        Optional<String> optVersion = args.get("version");
        if(optVersion.isEmpty()) {
            error("Missing 'version' argument! Type 'help-<%s>' to have the list of arguments for the current environment", env.getName().toLowerCase(Locale.ROOT));
            return;
        }

        String version = optVersion.get();

        DEBUG = args.contains("debug");

        if(!env.preSetup()) return;
        if(env.setup(args, version)) return;

        launch(env);
    }

    private void launch(RuntimeEnvironment env) {
        if(!env.preLaunch()) return;

        info("Launching Minecraft on '%s' with main '%s' and with args '%s'!", env.getName(), env.getMainClassName(), Arrays.toString(env.getLaunchArguments()));
        ClassLoader loader = ClassLoader.getSystemClassLoader();

        Class<?> clazz;
        try {
            clazz = loader.loadClass(env.getMainClassName());
        } catch (ClassNotFoundException e) {
            error("No class '%s' has been found in classpath!", env.getMainClassName());
            e.printStackTrace();
            return;
        }

        Method method;
        try {
            method = clazz.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            error("No main method found in class '%s'!", env.getMainClassName());
            e.printStackTrace();
            return;
        }

        try {
            method.invoke(null, (Object) env.getLaunchArguments());
        } catch (IllegalAccessException e) {
            error("Can't access main method in class '%s'!", env.getMainClassName());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            error("Caught an exception when launching game!");
            e.printStackTrace();
        }
    }

    private static boolean logHelp(String firstArg) {
        if(firstArg.contains("help") || "?".equals(firstArg)) {
            if(firstArg.contains("help-")) {
                String envString = firstArg.substring(firstArg.indexOf('-') + 1);
                RuntimeEnvironment.byName(envString).ifPresentOrElse(RuntimeEnvironment::logArgumentsList, () -> error("Could not find environment with name '%s'!", envString));
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
