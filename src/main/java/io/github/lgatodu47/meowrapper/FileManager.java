package io.github.lgatodu47.meowrapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import io.github.lgatodu47.meowrapper.json.Assets;
import io.github.lgatodu47.meowrapper.json.Version;
import io.github.lgatodu47.meowrapper.json.VersionManifest;

import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static io.github.lgatodu47.meowrapper.Logger.*;

/**
 * Class everything related to File managing (File download, creation, etc.)
 */
public class FileManager {
    /**
     * Unique instance of Gson for this class.
     */
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    /**
     * Creates a directory at the specified location if it doesn't exist.
     *
     * @param dir The path of the directory to create.
     * @return The directory that was just created.
     */
    public static Path createDirectory(Path dir) {
        try {
            Files.createDirectories(Files.exists(dir) ? dir.toRealPath() : dir);
        } catch (IOException e) {
            error("Failed to create directory with path '%s'!", dir);
            e.printStackTrace();
            System.exit(1);
        }
        return dir;
    }

    /**
     * Creates a fake {@code launcher_profiles.json} for mod loader installers (such as Optifine Installer or Forge Installer).
     * If the file is absent these installers won't work.
     * @param mcDir Minecraft's home directory.
     */
    public static void createFakeLauncherProfiles(Path mcDir) {
        Path file = mcDir.resolve("launcher_profiles.json");

        if(Files.notExists(file)) {
            try {
                JsonWriter writer = GSON.newJsonWriter(new FileWriter(file.toFile()));

                writer.beginObject();
                writer.name("profiles"); writer.beginObject();
                writer.endObject();
                writer.name("version"); writer.value(3); // Version 3 seems to be the most recent one
                writer.name("selectedProfile"); writer.value("");
                writer.endObject();

                writer.close();
            } catch (IOException e) {
                error("Error when writing fake 'launcher_profiles.json'. This may cause issues when installing mod loaders!");
                e.printStackTrace();
            }
        }
    }

    /**
     * URL to the official version manifest for Minecraft.
     */
    private static final String MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";

    /**
     * Gathers the version manifest content from the server without downloading it.
     * @return An Optional of the parsed version manifest.
     */
    public static Optional<VersionManifest> getVersionManifestData() {
        try (InputStream manifestData = new URL(MANIFEST_URL).openStream()) {
            return Optional.ofNullable(GSON.fromJson(new InputStreamReader(manifestData), VersionManifest.class));
        } catch (IOException e) {
            error("Failed to obtain version manifest data");
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Downloads the version manifest to the specified path.
     *
     * @param versionManifestPath The path where to download the version manifest.
     * @param updateManifest If the version manifest should be updated and re-downloaded.
     * @return The file representation of the version manifest path.
     */
    public static File downloadVersionManifest(Path versionManifestPath, boolean updateManifest) {
        try {
            Files.createDirectories(versionManifestPath.getParent());

            if(Files.notExists(versionManifestPath) || updateManifest) {
                try (InputStream manifestData = new URL(MANIFEST_URL).openStream()) {
                    Files.copy(manifestData, versionManifestPath, StandardCopyOption.REPLACE_EXISTING);
                    debug("Downloaded version manifest!");
                }
            }
        } catch (IOException e) {
            error("Failed to download version manifest");
            e.printStackTrace();
        }
        return versionManifestPath.toFile();
    }

    /**
     * Checks and downloads the version json file if it is not present.
     *
     * @param versionJsonPath The path leading to the version json file.
     * @param versionName The game version.
     * @param manifest The version manifest.
     * @param parentPathGetter A function taking the name of the parent version and returning the path it should be downloaded.
     * @return A {@link Version} instance representing the version Json file's content (may be {@code null}).
     * @throws IOException If an I/O error occurs when downloading the Json file or reading it.
     */
    public static Version downloadVersionJson(Path versionJsonPath, String versionName, VersionManifest manifest, Function<String, Path> parentPathGetter) throws IOException {
        if(Files.notExists(versionJsonPath)) {
            try {
                URL versionUrl = manifest.getUrl(versionName);
                if (versionUrl != null) {
                    Files.createDirectories(versionJsonPath.getParent());

                    try (InputStream versionData = versionUrl.openStream()) {
                        Files.copy(versionData, versionJsonPath);
                        debug("Downloaded version json for minecraft " + versionName);
                    }
                } else {
                    error("Missing version from manifest: %s. Your manifest may be outdated, try running with '--updateVersionManifest'.", versionName);
                }
            } catch (IOException e) {
                error("Failed to download version json file");
                throw e; // We handle this exception in the method call.
            }
        }

        Version version = Version.read(versionJsonPath.toFile());
        if(version.inheritsFrom != null) {
            String parentName = version.inheritsFrom;
            Version parent = downloadVersionJson(parentPathGetter.apply(parentName), parentName, manifest, parentPathGetter); // We recursively download and get the parent version Json.
            return Version.merge(parent, version); // And we finally return a merged version Json of the parent and the child.
        }

        return version;
    }

    /**
     * Checks and downloads the version Jar file if it is not present.
     *
     * @param versionJarPath The path to the version jar.
     * @param versionJson The version info.
     * @param isServer If we download the server jar or the client jar.
     * @return A File representation of the version jar path.
     */
    public static File downloadVersionJar(Path versionJarPath, Version versionJson, boolean isServer) {
        // We check for file size because Fabric installer creates an empty version jar (which is of course not readable) when installing.
        if(Files.notExists(versionJarPath) || getFileSize(versionJarPath) == 0) {
            Version.DownloadInfo download = versionJson.downloads.get(isServer ? "server" : "client");
            if(download != null) {
                try {
                    download(versionJarPath, download.url, download.sha1);
                } catch (IOException e) {
                    error("Failed to download version jar file!");
                    e.printStackTrace();
                }
            }
        }
        return versionJarPath.toFile();
    }

    /**
     * Checks and downloads the required libraries for the specified version.
     *
     * @param libDir The path to the 'libraries' directory.
     * @param nativesDir The path to the 'natives' directory.
     * @param versionJson The version info.
     * @param nativesAdder A consumer handling accumulation of the natives paths.
     * @return A set containing File references to all the libraries.
     */
    public static List<File> getLibraries(Path libDir, Path nativesDir, Version versionJson, Consumer<Path> nativesAdder) {
        info("Gathering libraries...");
        List<File> result = new ArrayList<>();

        for(Version.Library lib : versionJson.libraries) {
            if(lib.isAllowed()) { // Some libraries are only allowed on some operating systems
                // Version Json created by Optifine and Fabric don't have 'lib.downloads.artifact'.
                // We therefore must deduce the artifact info with the name of the library.
                // That's why there are now getters for each artifact info in Version.Library
                String artifactPath = lib.getArtifactPath();
                if(artifactPath != null) {
                    Path path = libDir.resolve(artifactPath);

                    if(Files.notExists(path)) {
                        URL url = lib.getDownloadURL();
                        if(url == null) {
                            if(lib.natives == null)
                                error("No download url for library '%s'!", lib.name);
                            continue;
                        }

                        String sha1 = MeoWrapperUtils.safeReference(() -> lib.downloads.artifact.sha1); // Here the sha1 is allowed to be null
                        try {
                            download(path, url, sha1);
                        } catch (IOException e) {
                            error("An error occurred when downloading library with path '%s'", artifactPath);
                            e.printStackTrace();
                            continue;
                        }
                    }

                    result.add(path.toFile());
                }

                if(lib.natives != null) {
                    Version.LibraryDownloadInfo nativeDownload = MeoWrapperUtils.safeReference(() -> lib.downloads.classifiers.get(lib.natives.get(Os.getCurrent().getName())));

                    if(nativeDownload != null) {
                        Path nativePath = libDir.resolve(nativeDownload.path);

                        if(Files.notExists(nativePath)) {
                            try {
                                download(nativePath, nativeDownload.url, nativeDownload.sha1);
                            } catch (IOException e) {
                                error("An error occurred when downloading native library with path '%s'", nativeDownload.path);
                                e.printStackTrace();
                                continue;
                            }
                        }

                        result.add(nativePath.toFile());

                        Version.ExtractInfo extractInfo = lib.extract;

                        if(extractInfo != null) {
                            // Extracts natives if specified
                            nativesAdder.accept(extractNatives(nativePath.toFile(), nativesDir, new HashSet<>(lib.extract.exclude)));
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * The official URL where assets can be downloaded.
     * Assets links are made of this link followed by the 2 first letters of the asset's hash followed by the asset's hash itself.
     */
    private static final String ASSETS_DOWNLOAD_URL = "https://resources.download.minecraft.net/";

    /**
     * Checks and downloads the asset index and all its assets corresponding to the current version.
     *
     * @param assetsDir The 'assets' directory.
     * @param versionJson The version info.
     * @return The given 'assets' directory.
     */
    public static Path gatherAssets(Path assetsDir, Version versionJson) {
        info("Gathering assets...");
        Path indexFile = assetsDir.resolve("indexes/" + versionJson.assetIndex.id + ".json");

        if(Files.notExists(indexFile)) {
            try {
                download(indexFile, versionJson.assetIndex.url, versionJson.assetIndex.sha1);
            } catch (IOException e) {
                error("Failed to download asset index for version %s", versionJson.id);
                e.printStackTrace();
                return assetsDir;
            }
        }

        Assets assets;
        try {
            assets = GSON.fromJson(new FileReader(indexFile.toFile()), Assets.class);
        } catch (FileNotFoundException e) {
            error("Index file which was just downloaded is now missing?!");
            e.printStackTrace();
            return assetsDir;
        }

        if(assets == null) return assetsDir;

        debug("Checking assets...");
        boolean downloadedAssets = false; // Let the user know when we're downloading assets
        int assetCount = 0;

        for(Assets.AssetInfo info : assets.objects.values()) {
            // The download path is composed of the 2 first letters of the hash followed by the hash itself
            String download = info.hash.substring(0, 2).concat("/").concat(info.hash);
            Path downloadPath = assetsDir.resolve("objects/".concat(download));
            URL downloadUrl;
            try {
                downloadUrl = new URL(ASSETS_DOWNLOAD_URL.concat(download));
            } catch (MalformedURLException e) {
                error("URL Error");
                e.printStackTrace();
                continue;
            }
            if(Files.notExists(downloadPath)) {
                if(!downloadedAssets) debug("Found missing assets, downloading!");

                try {
                    download(downloadPath, downloadUrl, info.hash);
                    downloadedAssets = true;
                    assetCount++;
                } catch (IOException e) {
                    error("Failed to download asset with hash '%s'", info.hash);
                    e.printStackTrace();
                }
            }
        }

        if(downloadedAssets) debug("Successfully downloaded %d assets!", assetCount);
        else debug("All assets are present");

        return assetsDir;
    }

    /**
     * Method that downloads an online file to the specified target.
     *
     * @param target The path of the downloaded file.
     * @param url The url of the online file to download.
     * @param sha1 The SHA-1 of the online file.
     * @throws IOException If some I/O error occurs.
     */
    private static void download(Path target, URL url, String sha1) throws IOException {
        debug("Downloading file at '%s'...", target.toString());
        URLConnection connection = url.openConnection();
        if (connection != null) {
            Files.createDirectories(target.getParent());
            Files.copy(connection.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            if (sha1 != null) {
                String localSha1 = sha1(Files.newInputStream(target));
                if (sha1.equals(localSha1)) {
                    debug("Successfully downloaded '%s'!", target.toAbsolutePath().toFile().getName());
                    return;
                }
                debug("Failed to download '%s': Invalid checksum:", target.toAbsolutePath().toFile().getName());
                debug("Expected: %s", sha1);
                debug("Actual: %s", localSha1);
                if (!target.toFile().delete()) {
                    debug("Failed to delete file, aborting.");
                    return;
                }
            }
            debug("Successfully downloaded '%s'! No checksum, assuming valid.", target.toAbsolutePath().toFile().getName());
        }
    }

    /**
     * Methods that extracts a file containing natives into the 'natives' directory.
     *
     * @param sourceFile The file to extract.
     * @param nativesDir The 'natives' directory.
     * @param exclusions A set containing String representations of all the excluded paths.
     * @return A path representing the destination directory of the source file's content.
     */
    private static Path extractNatives(File sourceFile, Path nativesDir, Set<String> exclusions) {
        Path destinationDir = nativesDir.resolve(sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.')));
        try {
            Files.createDirectories(destinationDir);

            try(Stream<Path> files = Files.list(destinationDir)) {
                if(Files.notExists(destinationDir) || !files.findAny().isPresent()) {
                    extract(sourceFile, destinationDir, exclusions);
                }
            }
        } catch (IOException e) {
            error("Error when trying to extract natives");
            e.printStackTrace();
        }
        return destinationDir;
    }

    /**
     * Method that extracts a file into a specified destination folder.
     *
     * @param srcFile The file to extract.
     * @param destination The directory where the content of the file will be extracted.
     * @param exclusions A set containing String representations of all the excluded paths.
     * @throws IOException If some I/O error occurs.
     */
    private static void extract(File srcFile, Path destination, Set<String> exclusions) throws IOException {
        try(JarFile jar = new JarFile(srcFile)) {
            Enumeration<JarEntry> entries = jar.entries();

            whileLoop:
            while(entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if(entry.isDirectory()) continue;

                for(String exclusionStr : exclusions) {
                    if(Paths.get(entry.getName()).startsWith(exclusionStr)) {
                        continue whileLoop;
                    }
                }

                Path file = destination.resolve(entry.getName());

                if(Files.notExists(file))
                    Files.createDirectories(file.getParent());

                try(InputStream input = jar.getInputStream(entry); FileOutputStream output = new FileOutputStream(file.toFile())) {
                    while(input.available() > 0) output.write(input.read());
                }
            }
        }

        debug("Successfully extracted library '%s' to dir '%s'", srcFile.getName(), destination.toString());
    }

    /**
     * An empty hash of 40 characters.
     */
    private static final String EMPTY_HASH = "0000000000000000000000000000000000000000";

    /**
     * Hashes the data of an input stream using SHA-1 algorithm.
     *
     * @param stream The input stream to hash.
     * @return A String representation of the hash.
     * @throws IOException If some error occurs when reading the input stream.
     */
    private static String sha1(InputStream stream) throws IOException {
        MessageDigest hash;
        try {
            hash = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 is somehow missing.", e);
        }
        byte[] buf = new byte[1024];
        int count;
        while ((count = stream.read(buf)) != -1)
            hash.update(buf, 0, count);
        stream.close();
        String hashStr = new BigInteger(1, hash.digest()).toString(16);
        return (EMPTY_HASH + hashStr).substring(hashStr.length());
    }

    /**
     * Gets the size of the file located at the specified path.
     * @param path The path of the file.
     * @return A long representation of the file size.
     */
    private static long getFileSize(Path path) {
        long size = 0;
        if(Files.exists(path)) {
            try {
                size = Files.size(path);
            } catch (IOException e) {
                error("Failed to obtain file size for file '%s'", path.toAbsolutePath());
                e.printStackTrace();
            }
        }
        return size;
    }
}
