package ua.shiningpr1sm;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class UpdateManagerTest {

    private HttpServer server;
    private String apiUrl;
    private byte[] jarBytes;

    @BeforeEach
    void setUp() throws IOException {
        jarBytes = createJarBytes();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        String downloadUrl = base + "/app.jar";
        server.createContext("/releases/latest",
                exchange -> respond(exchange, releaseJson(downloadUrl).getBytes(StandardCharsets.UTF_8)));
        server.createContext("/app.jar", exchange -> respond(exchange, jarBytes));
        server.start();
        apiUrl = base + "/releases/latest";
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        System.clearProperty("media-downloader.apiUrl");
    }

    @Test
    void fetchAndDownload_endToEnd_againstLocalServer() throws Exception {
        System.setProperty("media-downloader.apiUrl", apiUrl);
        UpdateManager manager = new UpdateManager();

        UpdateManager.ReleaseInfo release = manager.fetchLatestRelease();
        assertNotNull(release, "fetchLatestRelease should not return null against the test server");
        assertEquals("3.1.2", release.version());
        assertEquals("Test release notes", release.notesMarkdown());
        assertTrue(release.downloadUrl().endsWith("/app.jar"));

        Path target = Files.createTempFile("download-test", ".jar");
        try {
            manager.downloadRelease(release, target);
            assertTrue(Files.size(target) > 0, "downloaded file must not be empty");
            assertArrayEquals(jarBytes, Files.readAllBytes(target));
        } finally {
            Files.deleteIfExists(target);
        }
    }

    @Test
    void parseRelease_picksFirstJarAsset() {
        String json = "{"
                + "\"tag_name\":\"v3.1.2\","
                + "\"body\":\"Some notes\","
                + "\"assets\":["
                + "{\"name\":\"setup.exe\",\"browser_download_url\":\"https://x/setup.exe\"},"
                + "{\"name\":\"MediaDownloader-3.1.2.jar\",\"browser_download_url\":\"https://x/app.jar\"}"
                + "]}";
        UpdateManager.ReleaseInfo release = UpdateManager.parseRelease(json);
        assertNotNull(release);
        assertEquals("3.1.2", release.version());
        assertEquals("Some notes", release.notesMarkdown());
        assertEquals("https://x/app.jar", release.downloadUrl());
    }

    @Test
    void parseRelease_stripsVPrefix() {
        String json = "{\"tag_name\":\"v3.1.2\",\"body\":\"\","
                + "\"assets\":[{\"name\":\"app.jar\",\"browser_download_url\":\"https://x/app.jar\"}]}";
        assertEquals("3.1.2", UpdateManager.parseRelease(json).version());
    }

    @Test
    void parseRelease_returnsNullWithoutJarAsset() {
        String json = "{\"tag_name\":\"v3.1.2\",\"body\":\"\","
                + "\"assets\":[{\"name\":\"setup.exe\",\"browser_download_url\":\"https://x/setup.exe\"}]}";
        assertNull(UpdateManager.parseRelease(json));
    }

    @Test
    void parseRelease_returnsNullWithoutTag() {
        assertNull(UpdateManager.parseRelease("{}"));
    }

    @Test
    void parseRelease_returnsNullOnInvalidJson() {
        assertNull(UpdateManager.parseRelease("not-json"));
    }

    @Test
    void compareVersions() {
        UpdateManager manager = new UpdateManager();
        assertTrue(manager.compareVersions("3.1.2", "3.1.1") > 0);
        assertTrue(manager.compareVersions("3.0.1", "3.0.2") < 0);
        assertEquals(0, manager.compareVersions("3.1.1", "3.1.1"));
        assertTrue(manager.compareVersions("3.2", "3.1.9") > 0);
        assertTrue(manager.compareVersions("3.1.9", "3.2") < 0);
        assertEquals(0, manager.compareVersions("3.1", "3.1.0"));
        assertTrue(manager.compareVersions("v3.1.2", "3.1.1") > 0);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, byte[] body) throws IOException {
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static String releaseJson(String downloadUrl) {
        return "{"
                + "\"tag_name\":\"v3.1.2\","
                + "\"body\":\"Test release notes\","
                + "\"assets\":[{"
                + "\"name\":\"MediaDownloader-3.1.2.jar\","
                + "\"browser_download_url\":\"" + downloadUrl + "\""
                + "}]}";
    }

    private static byte[] createJarBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(baos)) {
            JarEntry manifest = new JarEntry("META-INF/MANIFEST.MF");
            jar.putNextEntry(manifest);
            jar.write("Manifest-Version: 1.0\nMain-Class: ua.shiningpr1sm.Launcher\n"
                    .getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();

            JarEntry launcher = new JarEntry("ua/shiningpr1sm/Launcher.class");
            jar.putNextEntry(launcher);
            jar.write(new byte[]{0, 0, 0});
            jar.closeEntry();
        }
        return baos.toByteArray();
    }
}
