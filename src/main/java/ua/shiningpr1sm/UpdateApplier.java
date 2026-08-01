package ua.shiningpr1sm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UpdateApplier {
    public void restartWithNewJar(Path tempJar) throws IOException, InterruptedException {
        Path currentJarPath;
        try {
            currentJarPath = Paths.get(
                    Launcher.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI()
            ).toAbsolutePath();
        } catch (Exception e) {
            throw new IOException("Could not locate current JAR path", e);
        }

        Path scriptPath = currentJarPath.getParent().resolve("update.bat");
        String current = currentJarPath.toString();
        String temp = tempJar.toAbsolutePath().toString();

        String script = "@echo off\r\n"
                + "set \"JAR=" + current + "\"\r\n"
                + "set \"TMP=" + temp + "\"\r\n"
                + ":loop\r\n"
                + "del /f /q \"%JAR%\" 2>nul\r\n"
                + "if exist \"%JAR%\" (\r\n"
                + "  timeout /t 1 /nobreak >nul\r\n"
                + "  goto loop\r\n"
                + ")\r\n"
                + "move /y \"%TMP%\" \"%JAR%\"\r\n"
                + "start \"\" javaw -jar \"%JAR%\"\r\n"
                + "start /b \"\" cmd /c del \"%~f0\" & exit\r\n";
        Files.writeString(scriptPath, script);
        new ProcessBuilder("cmd", "/c", scriptPath.toString()).start();
        System.exit(0);
    }
}