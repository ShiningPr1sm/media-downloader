package ua.shiningpr1sm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigManager {

    private ConfigManager() {
        /* This utility class should not be instantiated */
    }

    private static final Logger LOG = Logger.getLogger(ConfigManager.class.getName());

    private static final String APP_NAME = "ShiningPr1sm/MediaDownloader";
    private static final String CONFIG_FILE = "config.properties";
    private static final String VERSION_FILE = "version.txt";

    public static Path getConfigPath() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            appData = System.getProperty("user.home");
        }
        return Paths.get(appData, APP_NAME, CONFIG_FILE);
    }

    public static Path getVersionFilePath() {
        return getConfigPath().getParent().resolve(VERSION_FILE);
    }

    public static void initConfig() {
        Path dir = getConfigPath().getParent();
        try {
            if (Files.notExists(dir)) Files.createDirectories(dir);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to create config directory at " + dir, e);
        }
    }

    public static String loadSkippedVersion() {
        Path file = getVersionFilePath();
        try {
            if (Files.exists(file)) {
                return Files.readString(file).trim();
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to read skipped version from " + file, e);
        }
        return "";
    }

    public static void saveSkippedVersion(String version) {
        Path file = getVersionFilePath();
        try {
            initConfig();
            Files.writeString(file, version);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save skipped version to " + file, e);
        }
    }

    public static boolean isDevMode() {
        try {
            URL location = ConfigManager.class.getProtectionDomain().getCodeSource().getLocation();
            return location != null && !location.getPath().toLowerCase().endsWith(".jar");
        } catch (Exception e) {
            return false;
        }
    }

    public static String getInternalVersion() {
        if (isDevMode()) return "dev";

        Properties props = new Properties();
        InputStream is = ConfigManager.class.getResourceAsStream("/project.properties");
        if (is == null) {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream("project.properties");
        }
        if (is == null) {
            is = ClassLoader.getSystemResourceAsStream("project.properties");
        }

        try {
            if (is != null) {
                try (InputStream input = is) {
                    props.load(input);
                    String version = props.getProperty("app.version");
                    if (version != null) return version.trim();
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to read app version from project.properties", e);
        }
        return "UNKNOWN_VERSION";
    }
}