package fr.lgatodu47.meowrapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.lgatodu47.meowrapper.json.Assets;
import fr.lgatodu47.meowrapper.json.Version;
import fr.lgatodu47.meowrapper.json.VersionManifest;

import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static fr.lgatodu47.meowrapper.Logger.*;

/**
 * Class everything related to File managing (File download, creation, etc.)
 */
public class FileManager {
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    /**
     * Creates a directory if it doesn't exist.
     *
     * @param dir The path to the directory to create.
     * @return The input directory.
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
     * @param version The game version.
     * @param manifest The version manifest.
     * @return A File representation of the version json path.
     */
    public static File downloadVersionJson(Path versionJsonPath, String version, VersionManifest manifest) {
        try {
            Files.createDirectories(versionJsonPath.getParent());

            if(Files.notExists(versionJsonPath)) {
                URL versionUrl = manifest.getUrl(version);
                if (versionUrl != null) {
                    try (InputStream versionData = versionUrl.openStream()) {
                        Files.copy(versionData, versionJsonPath);
                        debug("Downloaded version json for minecraft " + version);
                    }
                } else {
                    error("Missing version from manifest: " + version);
                }
            }
        } catch (IOException e) {
            error("Failed to download version json file");
            e.printStackTrace();
        }
        return versionJsonPath.toFile();
    }

    /**
     * Checks and downloads the version json file if it is not present.
     *
     * @param versionJarPath The path to the version jar.
     * @param versionJson The version info.
     * @param isServer If we download the server jar or the client jar.
     * @return A File representation of the version jar path.
     */
    public static File downloadVersionJar(Path versionJarPath, Version versionJson, boolean isServer) {
        if(Files.notExists(versionJarPath)) {
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
     * @param versionJson The version info.
     * @return A set containing File references to all the libraries.
     */
    public static Set<File> getLibraries(Path libDir, Path nativesDir, Version versionJson) {
        info("Gathering libraries...");
        Set<File> result = new HashSet<>();

        for(Version.Library lib : versionJson.libraries) {
            if(lib.isAllowed()) { // Some libraries are only allowed on some operating systems
                Version.LibraryDownloadInfo download = lib.downloads.artifact;
                if(download == null) continue;

                Path path = tryFindMoreRecent(libDir, download, lib.getVersion());

                if(path == null) {
                    path = libDir.resolve(download.path);

                    if(Files.notExists(path)) {
                        try {
                            download(path, download.url, download.sha1);
                        } catch (IOException e) {
                            error("An error occurred when downloading library with path '%s'", download.path);
                            e.printStackTrace();
                            continue;
                        }
                    }
                }

                result.add(path.toFile());

                if(lib.natives != null) {
                    Version.LibraryDownloadInfo nativeDownload = lib.downloads.classifiers.get(lib.natives.get(Os.getCurrent().getName()));

                    if(nativeDownload != null) {
                        Path nativePath = tryFindMoreRecent(libDir, nativeDownload, lib.getVersion());

                        if(nativePath == null) {
                            nativePath = libDir.resolve(nativeDownload.path);

                            if(Files.notExists(nativePath)) {
                                try {
                                    download(nativePath, nativeDownload.url, nativeDownload.sha1);
                                } catch (IOException e) {
                                    error("An error occurred when downloading native library with path '%s'", nativeDownload.path);
                                    e.printStackTrace();
                                    continue;
                                }
                            }
                        }

                        // extract lwjgl natives
                        if(nativePath.getFileName().toString().contains("lwjgl-platform") && compareVersions("3.0.0", lib.getVersion()) < 0) {
                            System.setProperty("org.lwjgl.librarypath", extractNatives(nativePath.toFile(), nativesDir).toString());
                        }

                        result.add(nativePath.toFile());
                    }
                }
            }
        }

        return result;
    }

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
        boolean downloadedAssets = false; // Let's let the user know when we're downloading assets
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
                String localSha1 = sha1(new FileInputStream(target.toFile()));
                if (sha1.equals(localSha1)) {
                    debug("Successfully downloaded '%s'!", target.toFile().getName());
                    return;
                }
                debug("Failed to download '%s': Invalid checksum:", target.toFile().getName());
                debug("Expected: %s", sha1);
                debug("Actual: %s", localSha1);
                if (!target.toFile().delete()) {
                    debug("Failed to delete file, aborting.");
                    return;
                }
            }
            debug("Successfully downloaded '%s'! No checksum, assuming valid.", target.toFile().getName());
        }
    }

    /**
     * Methods that extracts a file containing natives into the 'natives' directory.
     *
     * @param sourceFile The file to extract.
     * @param nativesDir The 'natives' directory.
     * @return A path representing the destination directory of the source file's content.
     */
    private static Path extractNatives(File sourceFile, Path nativesDir) {
        Path destinationDir = nativesDir.resolve(sourceFile.getName().substring(0, sourceFile.getName().indexOf('.')));
        try {
            Files.createDirectories(destinationDir);

            try(Stream<Path> files = Files.list(destinationDir)) {
                if(Files.notExists(destinationDir) || files.findAny().isEmpty()) {
                    extract(sourceFile, destinationDir);
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
     * @throws IOException If some I/O error occurs.
     */
    private static void extract(File srcFile, Path destination) throws IOException {
        JarFile jar = new JarFile(srcFile);
        Enumeration<JarEntry> entries = jar.entries();

        while(entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            if(entry.isDirectory()) continue;

            Path file = destination.resolve(entry.getName());

            if(Files.notExists(file))
                Files.createDirectories(file.getParent());

            try(InputStream input = jar.getInputStream(entry); FileOutputStream output = new FileOutputStream(file.toFile())) {
                while(input.available() > 0) output.write(input.read());
            }
        }

        debug("Successfully extracted library '%s' to dir '%s'", srcFile.getName(), destination.toString());
    }

    /**
     * Unused method that originally checked for more recent version of the library to download.
     * Did this before I realised that there were download rules on libraries.<br>
     * May get re-implemented later-on for something else.
     *
     * @return {@code null}
     */
    private static Path tryFindMoreRecent(Path libDir, Version.LibraryDownloadInfo download, String version) {
//        Path parentDir = libDir.resolve(download.path.substring(0, download.path.indexOf(version)));
//        if(Files.exists(parentDir)) {
//            try {
//                Path path = Files.list(parentDir).filter(dir -> VERSION_SCHEME.matcher(dir.getFileName().toString()).find()).min((o1, o2) -> {
//                    Matcher matcher0 = VERSION_SCHEME.matcher(o1.getFileName().toString());
//                    Matcher matcher1 = VERSION_SCHEME.matcher(o2.getFileName().toString());
//                    matcher0.find();
//                    matcher1.find();
//                    return compareVersions(matcher0.group(), matcher1.group());
//                }).map(parentDir::resolve).orElse(null);
//                if(path != null && Files.exists(path)) return path.resolve(download.path.substring(download.path.indexOf(version) + version.length() + 1).replace(version, path.getFileName().toString()));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        return null;
    }

    /**
     * Compares two version strings.
     *
     * @return the value {@code 0} if {@code v1 == v2}; a value less than {@code 0} if {@code v2 < v1}; and a value greater than {@code 0} if {@code v2 > v1}
     */
    private static int compareVersions(String v1, String v2)
    {
        String[] versionNumbers1 = v1.split("\\.");
        String[] versionNumbers2 = v2.split("\\.");

        int[] maxVerNumbs = new int[Math.max(versionNumbers1.length, versionNumbers2.length)];
        Arrays.fill(maxVerNumbs, 0);

        // We copy the content of the strings into an array of the same size
        int[] verNumbs1 = maxVerNumbs.clone();
        int[] verNumbs2 = maxVerNumbs.clone();
        System.arraycopy(Arrays.stream(versionNumbers1).mapToInt(Integer::parseInt).toArray(), 0, verNumbs1, 0, versionNumbers1.length);
        System.arraycopy(Arrays.stream(versionNumbers2).mapToInt(Integer::parseInt).toArray(), 0, verNumbs2, 0, versionNumbers2.length);

        for(int i = 0; i < maxVerNumbs.length; i++)
        {
            int result = Integer.compare(verNumbs2[i], verNumbs1[i]); // we want to sort into descending order so the two values are inverted
            if(result == 0) continue;
            return result;
        }

        return 0;
    }

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
            e.printStackTrace();
            throw new RuntimeException("SHA-1 is somehow missing.", e);
        }
        byte[] buf = new byte[1024];
        int count;
        while ((count = stream.read(buf)) != -1)
            hash.update(buf, 0, count);
        stream.close();
        String hashStr = new BigInteger(1, hash.digest()).toString(16);
        return ("0".repeat(40) + hashStr).substring(hashStr.length());
    }
}
