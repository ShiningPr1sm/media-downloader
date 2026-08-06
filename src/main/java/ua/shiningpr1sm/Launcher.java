package ua.shiningpr1sm;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Launcher {
    public static void main(String[] args) {
        ConfigManager.initConfig();
        String currentVer = ConfigManager.getInternalVersion();

        SwingUtilities.invokeLater(JavaVideoDownloader::new);

        Thread updateCheck = new Thread(() -> checkForUpdates(currentVer), "update-check");
        updateCheck.start();
    }

    private static void checkForUpdates(String currentVer) {
        try {
            UpdateManager updateManager = new UpdateManager();
            UpdateManager.ReleaseInfo release = updateManager.fetchLatestRelease();
            if (release == null || updateManager.compareVersions(release.version(), currentVer) <= 0) {
                return;


            if (release.version().equals(ConfigManager.loadSkippedVersion())) {
                System.out.println("Version " + release.version() + " skipped by user");
                return;
            }

            String notesHtml = MarkdownUtil.toHtml(release.notesMarkdown());
            SwingUpdatePrompt.Choice[] choice = {SwingUpdatePrompt.Choice.CANCEL};
            try {
                SwingUtilities.invokeAndWait(() -> choice[0] = SwingUpdatePrompt.show(currentVer, release.version(), notesHtml));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (choice[0] != SwingUpdatePrompt.Choice.UPDATE) {
                return;
            }

            Path tempJar = Files.createTempFile("MediaDownloader-", ".jar");
            updateManager.downloadRelease(release, tempJar);

            if (Files.size(tempJar) == 0) {
                throw new IOException("Downloaded file is empty");
            }

            new UpdateApplier().restartWithNewJar(tempJar);
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    "Не удалось установить обновление:\n" + e.getMessage() +
                            "\n\nПодробности в update.log",
                    "Update failed",
                    JOptionPane.ERROR_MESSAGE
            ));
        }
    }
}
