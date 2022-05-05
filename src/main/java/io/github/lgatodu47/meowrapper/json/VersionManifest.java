package io.github.lgatodu47.meowrapper.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

/**
 * Java representation of a version manifest Json file.
 */
public class VersionManifest {
    /**
     * Unique instance of Gson for this class.
     */
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * The list of the versions that can be downloaded in this manifest.
     */
    public VersionInfo[] versions;

    /**
     * A class that describes a minecraft version.
     */
    public static class VersionInfo {
        /**
         * The name of that version
         */
        public String id;
        /**
         * The download url of that version.
         */
        public URL url;
    }

    /**
     * Method that will return the URL corresponding to the specified version name.
     * @param version The name of the version.
     * @return A URL object specifying the path to the version json file.
     */
    public URL getUrl(String version) {
        if (version == null) {
            return null;
        }

        for (VersionInfo info : versions) {
            if (version.equals(info.id)) {
                return info.url;
            }
        }

        return null;
    }

    /**
     * Parses the specified file's content into a VersionManifest object.
     * @param file The version manifest file.
     * @return A VersionManifest object parsed from the version manifest file.
     * @throws IOException If some I/O error occurs when creating the FileReader.
     */
    public static VersionManifest read(File file) throws IOException {
        return GSON.fromJson(new FileReader(file), VersionManifest.class);
    }
}
