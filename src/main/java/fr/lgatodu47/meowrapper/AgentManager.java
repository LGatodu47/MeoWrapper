package fr.lgatodu47.meowrapper;

import ca.cgjennings.jvm.JarLoader;
import net.bytebuddy.agent.ByteBuddyAgent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.jar.Manifest;

import static fr.lgatodu47.meowrapper.Logger.error;
import static fr.lgatodu47.meowrapper.Logger.info;

/**
 * A class that handles all the processes related to the java agent.<br>
 * The java agent I'm using allows me to add to the classpath.<br>
 * You can find the GitHub page here: <a href="https://github.com/CGJennings/jar-loader">https://github.com/CGJennings/jar-loader</a>
 */
class AgentManager {
    /**
     * Adds the libraries and the version jar to the classpath dynamically using {@link java.lang.instrument.Instrumentation}.
     *
     * @param mcJar     The version jar file
     * @param libraries A set with all the libraries.
     */
    static void tryAddJars(File mcJar, Set<File> libraries) {
        libraries.add(mcJar);

        for (File file : libraries) {
            try {
                JarLoader.addToClassPath(file);
            } catch (IOException e) {
                error("Failed to add library at location '%s' to the classpath!", file.toPath());
                e.printStackTrace();
            }
        }
    }

    /**
     * Attaches the agent to the running VM.
     *
     * @param agent The file pointing to the java agent.
     * @throws RuntimeException If the process fails for some reason
     */
    private static void loadAgent(File agent) throws RuntimeException {
        info("Loading agent...");
        String runningVm = ManagementFactory.getRuntimeMXBean().getName();
        String pid = runningVm.substring(0, runningVm.indexOf('@'));

        try {
            ByteBuddyAgent.attach(agent, pid);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Cache
    private static String agentVersion;

    /**
     * This method is unused for now.
     * Reads the jar manifest and gets the Agent version.
     *
     * @return A String representation of the Agent version.
     */
    private static String getAgentVersion() {
        if(agentVersion != null) return agentVersion;

        InputStream stream = AgentManager.class.getResourceAsStream("/META-INF/MANIFEST.MF");
        if(stream != null) {
            try {
                Manifest manifest = new Manifest(stream);
                String agentVersion = manifest.getMainAttributes().getValue("Agent-Version");
                if(agentVersion != null) return AgentManager.agentVersion = agentVersion;
            } catch (IOException e) {
                error("Caught an error when trying to read jar manifest");
                e.printStackTrace();
            }
        }
        return "1.0.0";
    }

    /**
     * This method sets up the java agent.
     * If we are currently running in a jar, we want the agent to be extracted, otherwise we can't get a File instance.
     *
     * @return {@code false} if the initialization encountered a problem, otherwise {@code true}
     */
    static boolean init() {
        File agent;

        URL agentUrl;
        try {
            agentUrl = Class.forName("ca.cgjennings.jvm.JarLoader").getProtectionDomain().getCodeSource().getLocation();
        } catch (ClassNotFoundException e) {
            error("Java agent is missing from jar!");
            return false;
        }

        if (agentUrl == null) {
            error("Java agent file not found!");
            return false;
        }

        URI uri;
        try {
            uri = agentUrl.toURI();
        } catch (URISyntaxException e) {
            error("Failed to parse java agent URL to URI!");
            e.printStackTrace();
            return false;
        }

        if (uri.getPath().isEmpty()) {
            error("Java agent file not found!");
            return false;
        }

        agent = new File(uri);

        try {
            loadAgent(agent);
        } catch (RuntimeException e) {
            error("Failed to load java agent!");
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
