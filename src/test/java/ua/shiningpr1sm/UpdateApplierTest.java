package ua.shiningpr1sm;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class UpdateApplierTest {

    @Test
    void buildScript_containsReplaceAndRestartCommands() {
        String script = UpdateApplier.buildScript(
                Paths.get("C:/apps/MediaDownloader.jar"),
                Paths.get("C:/temp/MediaDownloader-xyz.jar"));

        assertTrue(script.contains("set \"JAR=C:\\apps\\MediaDownloader.jar\""));
        assertTrue(script.contains("set \"TMP=C:\\temp\\MediaDownloader-xyz.jar\""));
        assertTrue(script.contains("del /f /q \"%JAR%\" 2>nul"));
        assertTrue(script.contains("move /y \"%TMP%\" \"%JAR%\""));
        assertTrue(script.contains("start \"\" javaw -jar \"%JAR%\""));
    }
}
