package fr.lgatodu47.meowrapper.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

public class VersionManifest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public VersionInfo[] versions;

    public static class VersionInfo {
        public String id;
        public URL url;
    }

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

    public static VersionManifest read(File file) throws IOException {
        return GSON.fromJson(new FileReader(file), VersionManifest.class);
    }
}
