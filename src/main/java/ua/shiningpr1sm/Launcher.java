package ua.shiningpr1sm;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Launcher {
    public static void main(String[] args) {
        ConfigManager.initConfig();
        String currentVer = ConfigManager.getInternalVersion();

        UpdateManager updateManager = new UpdateManager();
        UpdateManager.ReleaseInfo release = updateManager.fetchLatestRelease();

        if (release != null && updateManager.compareVersions(release.version(), currentVer) > 0) {
            String skippedVersion = ConfigManager.loadSkippedVersion();
            if (release.version().equals(skippedVersion)) {
                System.out.println("Version " + release.version() + " skipped by user");
            } else {
                String notesHtml = MarkdownUtil.toHtml(release.notesMarkdown());
                SwingUpdatePrompt.Choice choice = SwingUpdatePrompt.show(currentVer, release.version(), notesHtml);

                if (choice == SwingUpdatePrompt.Choice.UPDATE) {
                    try {
                        Path tempJar = Files.createTempFile("MediaDownloader-", ".jar");
                        updateManager.downloadRelease(release, tempJar);

                        long size = Files.size(tempJar);
                        if (size == 0) {
                            throw new IOException("Downloaded file is empty");
                        }

                        UpdateApplier updateApplier = new UpdateApplier();
                        updateApplier.restartWithNewJar(tempJar);
                        return;
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                                null,
                                "Не удалось установить обновление:\n" + e.getMessage() +
                                        "\n\nПодробности в update.log",
                                "Update failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }
        }
        SwingUtilities.invokeLater(JavaVideoDownloader::new);
    }
}