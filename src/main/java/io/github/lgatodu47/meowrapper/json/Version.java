package io.github.lgatodu47.meowrapper.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.lgatodu47.meowrapper.Os;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version {
    // I'm just a beginner with regex, so it might be incorrect
    public static final Pattern VERSION_SCHEME = Pattern.compile("(\\d+\\.\\d+(\\.?(\\d+)?)+)");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public AssetIndex assetIndex;
    public String assets;
    public Map<String, DownloadInfo> downloads;
    public String id;
    public Library[] libraries;
    public String mainClass;

    public static class Rule {
        public String action;
        public OsCondition os;

        public boolean allows() {
            return os == null || os.platformCondition() == action.equals("allow");
        }
    }

    public static class OsCondition {
        public String name;
        public String version;
        public String arch;

        public boolean nameEquals() {
            return name == null || Os.getCurrent().getName().equals(name);
        }

        public boolean versionEquals() {
            return version == null || Pattern.compile(version).matcher(System.getProperty("os.version")).find();
        }

        public boolean archEquals() {
            return arch == null || Pattern.compile(arch).matcher(System.getProperty("os.arch")).find();
        }

        public boolean platformCondition() {
            return nameEquals() && versionEquals() && archEquals();
        }
    }

    public static class Library {
        public String name;
        public Map<String, String> natives;
        public Downloads downloads;
        public Rule[] rules;
        public ExtractInfo extract;

        private String _version;

        public String getVersion() {
            return _version == null ? (_version = getVersionFromString(name.split(":")[2])) : _version;
        }

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

        private static String getVersionFromString(String _version) {
            Matcher matcher = VERSION_SCHEME.matcher(_version);
            return matcher.find() ? matcher.group() : null;
        }
    }

    public static class LibraryDownloadInfo extends DownloadInfo {
        public String path;
    }

    public static class AssetIndex extends DownloadInfo {
        public String id;
    }

    public static class DownloadInfo {
        public String sha1;
        public URL url;
    }

    public static class Downloads {
        public Map<String, LibraryDownloadInfo> classifiers;
        public LibraryDownloadInfo artifact;
    }

    public static class ExtractInfo {
        public List<String> exclude;
    }

    public static Version read(File file) throws IOException {
        return GSON.fromJson(new FileReader(file), Version.class);
    }
}
