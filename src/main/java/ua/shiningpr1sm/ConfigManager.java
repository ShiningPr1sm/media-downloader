package ua.shiningpr1sm;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.Properties;

public class ConfigManager {
    private static final String APP_NAME = "ShiningPr1sm/MediaDownloader";
    private static final String CONFIG_FILE = "config.properties";

    public static Path getConfigPath() {
        return Paths.get(System.getenv("APPDATA"), APP_NAME, CONFIG_FILE);
    }

    public static void initConfig() {
        Path path = getConfigPath();
        try {
            if (Files.notExists(path.getParent())) Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Properties props = new Properties();
                props.setProperty("version", "1.0.0");
                try (OutputStream out = Files.newOutputStream(path)) {
                    props.store(out, null);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Properties loadConfig() {
        Properties props = new Properties();
        Path path = getConfigPath();
        try (InputStream is = Files.newInputStream(path)) {
            props.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }

    public static void saveConfig(Properties props) {
        Path path = getConfigPath();
        try (OutputStream out = Files.newOutputStream(path)) {
            props.store(out, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String loadSkippedVersion() {
        return loadConfig().getProperty("skippedVersion", "");
    }

    public static void saveSkippedVersion(String version) {
        Properties props = loadConfig();
        props.setProperty("skippedVersion", version);
        saveConfig(props);
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
            e.printStackTrace();
        }
        return "UNKNOWN_VERSION";
    }
}