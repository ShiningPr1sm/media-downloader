package ua.shiningpr1sm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

public class UpdateManager {

    private static final String API_URL = "https://github.com/ShiningPr1sm/Media-Downloader/releases/latest";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record ReleaseInfo(String version, String notesMarkdown, String downloadUrl) {}

    public ReleaseInfo fetchLatestRelease() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Accept", "application/vnd.github+json")
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            JsonNode root = MAPPER.readTree(response.body());

            String tagName = root.path("tag_name").asText(null);
            if (tagName == null) return null;
            String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;

            String notes = root.path("body").asText("");

            String downloadUrl = null;
            for (JsonNode asset : root.path("assets")) {
                String name = asset.path("name").asText("");
                if (name.endsWith(".jar")) {
                    downloadUrl = asset.path("browser_download_url").asText(null);
                    break;
                }
            }
            if (downloadUrl == null) return null;

            return new ReleaseInfo(version, notes, downloadUrl);
        } catch (Exception e) {
            System.err.println("UpdateManager: failed to fetch release: " + e.getMessage());
            return null;
        }
    }

    public int compareVersions(String v1, String v2) {
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? parsePart(a[i]) : 0;
            int y = i < b.length ? parsePart(b[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }

    private int parsePart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void downloadRelease(ReleaseInfo release, Path target) throws IOException, InterruptedException {
        if (release.downloadUrl() == null) {
            throw new IOException("No download URL available in release data");
        }

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(release.downloadUrl()))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(target));

        if (response.statusCode() != 200) {
            throw new IOException("Download failed with code: " + response.statusCode());
        }
    }
}