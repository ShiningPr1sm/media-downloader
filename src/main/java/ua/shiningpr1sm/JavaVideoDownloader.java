package ua.shiningpr1sm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formdev.flatlaf.FlatLightLaf;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JavaVideoDownloader {
    private static final Logger LOG = Logger.getLogger(JavaVideoDownloader.class.getName());
    private static final String USER_AGENT = "MediaDownloader/" + ConfigManager.getInternalVersion();

    private static final String COMPANY_NAME = "ShiningPr1sm";
    private static final String APPDATA = System.getenv("APPDATA");
    private static final File SHARED_ROOT = new File(APPDATA, COMPANY_NAME);

    private static final File YTDLP_DIR = new File(SHARED_ROOT, "yt-dlp");
    private static final File YTDLP_EXE = new File(YTDLP_DIR, "yt-dlp.exe");
    private static final String YTDLP_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";

    private static final File FFMPEG_DIR = new File(SHARED_ROOT, "ffmpeg");
    private static final File FFMPEG_EXE = new File(FFMPEG_DIR, "ffmpeg.exe");
    private static final String FFMPEG_ZIP_URL = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";

    private volatile Process currentProcess;
    private volatile boolean cancelled;
    private volatile boolean busy;

    public JavaVideoDownloader() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            LOG.log(Level.SEVERE, "Error setting LookAndFeel", e);
        }

        String currentVer = ConfigManager.getInternalVersion();
        JFrame frame = new JFrame();
        if (ConfigManager.isDevMode()) {
            frame.setTitle(String.format("Media Downloader  |  %s", currentVer));
        } else {
            frame.setTitle(String.format("Media Downloader  |  v%s", currentVer));
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(550, 300);
        frame.setLocation(
                (screenSize.width - frame.getWidth()) / 2,
                (screenSize.height - frame.getHeight()) / 2
        );
        frame.setLayout(new BorderLayout());
        frame.setResizable(false);

        try {
            var resource = JavaVideoDownloader.class.getResource("/project_icon.png");
            if (resource != null) {
                Image icon = ImageIO.read(resource);
                frame.setIconImage(icon);
            } else {
                LOG.warning("Icon resource not found: /project_icon.png");
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error loading icon", e);
        }

        final File[] downloadFolder = {new File(System.getProperty("user.home"), "Downloads/")};
        if (!downloadFolder[0].exists()) {
            downloadFolder[0].mkdirs();
            LOG.info("Created download directory: " + downloadFolder[0].getAbsolutePath());
        } else {
            LOG.info("Download directory exists: " + downloadFolder[0].getAbsolutePath());
        }

        JTextArea textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        String textarea_placeholder = "Paste links to the media you want to download here...";
        textArea.setForeground(Color.GRAY);
        textArea.setText(textarea_placeholder);
        textArea.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (textArea.getText().equals(textarea_placeholder)) {
                    textArea.setText("");
                    textArea.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (textArea.getText().isEmpty()) {
                    textArea.setForeground(Color.GRAY);
                    textArea.setText(textarea_placeholder);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 180));
        frame.add(scrollPane, BorderLayout.NORTH);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(500, 20));

        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        progressPanel.add(progressBar, BorderLayout.CENTER);

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setForeground(Color.DARK_GRAY);
        logArea.setBackground(new Color(250, 250, 250));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setPreferredSize(new Dimension(500, 100));
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log"));

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(progressPanel, BorderLayout.NORTH);
        centerPanel.add(logScrollPane, BorderLayout.CENTER);
        frame.add(centerPanel, BorderLayout.CENTER);

        setupLogRedirect(logArea);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        final int ROW_HEIGHT = 30;

        String[] formats = {
                "Video + Audio",
                "Video only (muted)",
                "Audio only (mp3)"
        };

        JComboBox<String> formatBox = new JComboBox<>(formats);
        formatBox.setMaximumSize(new Dimension(200, ROW_HEIGHT));
        formatBox.setPreferredSize(new Dimension(200, ROW_HEIGHT));

        JButton downloadButton = new JButton("Download");
        downloadButton.setPreferredSize(new Dimension(120, ROW_HEIGHT));
        downloadButton.setMaximumSize(new Dimension(120, ROW_HEIGHT));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(70, ROW_HEIGHT));
        cancelButton.setMaximumSize(new Dimension(70, ROW_HEIGHT));
        cancelButton.setEnabled(false);

        ImageIcon thumbIcon = new ImageIcon(
                Objects.requireNonNull(JavaVideoDownloader.class.getResource("/thumbnail_icon.png"))
        );
        Image scaled = thumbIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        JButton thumbnailButton = new JButton(new ImageIcon(scaled));
        thumbnailButton.setToolTipText("Download thumbnail");
        thumbnailButton.setPreferredSize(new Dimension(ROW_HEIGHT, ROW_HEIGHT));
        thumbnailButton.setMaximumSize(new Dimension(ROW_HEIGHT, ROW_HEIGHT));

        String[] browsers = {"None", "Firefox", "Chrome", "Edge", "Opera", "Brave"};
        JComboBox<String> browserComboBox = new JComboBox<>(browsers);
        browserComboBox.setMaximumSize(new Dimension(90, ROW_HEIGHT));
        browserComboBox.setPreferredSize(new Dimension(90, ROW_HEIGHT));

        bottomPanel.add(formatBox);
        bottomPanel.add(Box.createHorizontalStrut(5));
        bottomPanel.add(downloadButton);
        bottomPanel.add(Box.createHorizontalStrut(5));
        bottomPanel.add(cancelButton);
        bottomPanel.add(Box.createHorizontalStrut(7));
        bottomPanel.add(thumbnailButton);
        bottomPanel.add(Box.createHorizontalStrut(7));
        bottomPanel.add(browserComboBox);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        Runnable setBusyUi = () -> {
            busy = true;
            downloadButton.setEnabled(false);
            thumbnailButton.setEnabled(false);
            formatBox.setEnabled(false);
            browserComboBox.setEnabled(false);
            cancelButton.setEnabled(true);
        };

        Runnable resetUi = () -> {
            busy = false;
            downloadButton.setEnabled(true);
            thumbnailButton.setEnabled(true);
            formatBox.setEnabled(true);
            browserComboBox.setEnabled(true);
            cancelButton.setEnabled(false);
        };

        cancelButton.addActionListener(ev -> {
            cancelled = true;
            Process p = currentProcess;
            if (p != null && p.isAlive()) {
                p.destroyForcibly();
            }
            cancelButton.setEnabled(false);
            LOG.info("Download cancelled by user");
        });

        downloadButton.addActionListener(e -> {
            if (busy) {
                LOG.warning("Download ignored: another operation is already running");
                return;
            }
            String input = textArea.getText().trim();
            if (input.isEmpty() || input.equals(textarea_placeholder)) {
                JOptionPane.showMessageDialog(frame,
                        "Please enter at least one video URL!",
                        "Empty input",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String[] urls = input.split("\\r?\\n");
            List<String> videoUrls = new ArrayList<>();
            for (String url : urls) {
                String trimmed = url.trim();
                if (!trimmed.isEmpty()) {
                    if (!isValidUrl(trimmed)) {
                        JOptionPane.showMessageDialog(frame,
                                "Invalid URL: " + trimmed,
                                "Invalid URL",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    videoUrls.add(trimmed);
                }
            }
            if (videoUrls.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No valid URLs found!");
                LOG.info("No valid URLs found after parsing.");
                return;
            }

            String browser = Objects.requireNonNull(browserComboBox.getSelectedItem()).toString().toLowerCase();
            if (!browser.equals("none")) {
                int result = JOptionPane.showConfirmDialog(frame,
                        "The app will extract cookies from " + browser + ".\n" +
                                "This is needed to access age-restricted or logged-in content.\n\n" +
                                "Continue?",
                        "Cookie Access",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (result != JOptionPane.OK_OPTION) {
                    return;
                }
            }

            cancelled = false;
            setBusyUi.run();
            progressBar.setValue(0);
            progressBar.setString("Starting download...");
            LOG.info("Starting download process for " + videoUrls.size() + " URLs.");

            new Thread(() -> {
                try {
                    if (cancelled) {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(0);
                            progressBar.setString("Cancelled");
                            resetUi.run();
                        });
                        return;
                    }
                    checkAndDownloadYTDLP();
                    if (cancelled) {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(0);
                            progressBar.setString("Cancelled");
                            resetUi.run();
                        });
                        return;
                    }
                    checkAndDownloadFFMPEG();
                    String selectedFormat = (String) formatBox.getSelectedItem();
                    LOG.info("Selected format: " + selectedFormat + ", Browser for cookies: " + browser);

                    for (int i = 0; i < videoUrls.size(); i++) {
                        if (cancelled) break;
                        String videoUrl = videoUrls.get(i);
                        LOG.info("Processing URL " + (i + 1) + "/" + videoUrls.size() + ": " + videoUrl);
                        List<String> command = new ArrayList<>();
                        command.add(YTDLP_EXE.getAbsolutePath());

                        command.add("--remote-components");
                        command.add("ejs:github");
                        LOG.fine("Adding --remote-components ejs:github for YouTube challenge solving.");

                        switch (selectedFormat) {
                            case "Video + Audio":
                                command.add("-f");
                                command.add("bestvideo[ext=mp4]+bestaudio[ext=m4a]/bestvideo+bestaudio/best");
                                command.add("--merge-output-format");
                                command.add("mp4");
                                command.add("--remux-video");
                                command.add("mp4");
                                command.add("--ffmpeg-location");
                                command.add(FFMPEG_EXE.getAbsolutePath());
                                if (!browser.isEmpty() && !browser.equals("none")) {
                                    command.add("--cookies-from-browser");
                                    command.add(browser);
                                    LOG.fine("Adding --cookies-from-browser " + browser);
                                }
                                break;
                            case "Video only (muted)":
                                command.add("-f");
                                command.add("bestvideo[ext=mp4]/bestvideo/best");
                                command.add("--remux-video");
                                command.add("mp4");
                                command.add("--ffmpeg-location");
                                command.add(FFMPEG_EXE.getAbsolutePath());
                                if (!browser.isEmpty() && !browser.equals("none")) {
                                    command.add("--cookies-from-browser");
                                    command.add(browser);
                                    LOG.fine("Adding --cookies-from-browser " + browser);
                                }
                                break;
                            case "Audio only (mp3)":
                                command.add("-f");
                                command.add("bestaudio/best");
                                command.add("--extract-audio");
                                command.add("--audio-format");
                                command.add("mp3");
                                command.add("--ffmpeg-location");
                                command.add(FFMPEG_EXE.getAbsolutePath());
                                if (!browser.isEmpty() && !browser.equals("none")) {
                                    command.add("--cookies-from-browser");
                                    command.add(browser);
                                    LOG.fine("Adding --cookies-from-browser " + browser);
                                }
                                break;
                        }

                        command.add("--impersonate");
                        command.add("chrome");

                        String timeStamp = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date());
                        command.add("-o");
                        command.add(downloadFolder[0].getAbsolutePath() + "/%(title)s_%(id)s_" + timeStamp + ".%(ext)s");
                        command.add(videoUrl);

                        LOG.info("Executing command: " + String.join(" ", command));

                        ProcessBuilder pb = new ProcessBuilder(command);
                        pb.redirectErrorStream(true);
                        Process process = pb.start();
                        currentProcess = process;

                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        Pattern pattern = Pattern.compile("(\\d{1,3}\\.\\d)%");
                        int videoIndex = i + 1;
                        while ((line = reader.readLine()) != null) {
                            LOG.fine("yt-dlp output: " + line);
                            Matcher matcher = pattern.matcher(line);
                            if (matcher.find()) {
                                int progress = (int) Float.parseFloat(matcher.group(1));
                                int totalProgress = (int) (((i + progress / 100.0) / videoUrls.size()) * 100);
                                SwingUtilities.invokeLater(() -> {
                                    progressBar.setValue(totalProgress);
                                    progressBar.setString("Video " + videoIndex + " of " + videoUrls.size() + " - " + progress + "%");
                                });
                            } else {
                                String finalLine = line;
                                SwingUtilities.invokeLater(() -> progressBar.setString("Video " + videoIndex + " of " + videoUrls.size() + " - " + finalLine.trim()));
                            }
                        }
                        int exitCode = process.waitFor();
                        currentProcess = null;
                        LOG.info("yt-dlp process for " + videoUrl + " finished with exit code: " + exitCode);

                        if (exitCode != 0) {
                            LOG.warning("yt-dlp exited with code " + exitCode + " for URL: " + videoUrl + ", continuing with next URL");
                            String finalLine2 = "Video " + videoIndex + " failed (exit " + exitCode + "), continuing...";
                            SwingUtilities.invokeLater(() -> progressBar.setString(finalLine2));
                        }
                    }
                    boolean wasCancelled = cancelled;
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(wasCancelled ? 0 : 100);
                        progressBar.setString(wasCancelled ? "Download cancelled" : "All downloads completed!");
                        resetUi.run();
                        LOG.info(wasCancelled ? "Download cancelled" : "All downloads completed!");
                    });
                } catch (IOException | InterruptedException ex) {
                    LOG.log(Level.SEVERE, "An error occurred during download", ex);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setString("Error: " + ex.getMessage());
                        resetUi.run();
                        JOptionPane.showMessageDialog(frame, "An error occurred: " + ex.getMessage());
                    });
                }
            }).start();
        });

        thumbnailButton.addActionListener(e -> {
            if (busy) {
                LOG.warning("Thumbnail download ignored: another operation is already running");
                return;
            }
            String input = textArea.getText().trim();
            if (input.isEmpty() || input.equals(textarea_placeholder)) {
                JOptionPane.showMessageDialog(frame,
                        "Please enter at least one video URL!",
                        "Empty input",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String[] urls = input.split("\\r?\\n");
            List<String> videoUrls = new ArrayList<>();
            for (String url : urls) {
                String trimmed = url.trim();
                if (!trimmed.isEmpty()) {
                    if (!isValidUrl(trimmed)) {
                        JOptionPane.showMessageDialog(frame,
                                "Invalid URL: " + trimmed,
                                "Invalid URL",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    videoUrls.add(trimmed);
                }
            }
            if (videoUrls.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No valid URLs found!");
                return;
            }

            String browser = Objects.requireNonNull(browserComboBox.getSelectedItem()).toString().toLowerCase();
            if (!browser.equals("none")) {
                int result = JOptionPane.showConfirmDialog(frame,
                        "The app will extract cookies from " + browser + ".\n" +
                                "This is needed to access age-restricted or logged-in content.\n\n" +
                                "Continue?",
                        "Cookie Access",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (result != JOptionPane.OK_OPTION) {
                    return;
                }
            }

            cancelled = false;
            setBusyUi.run();
            progressBar.setValue(0);
            progressBar.setString("Starting thumbnail download...");
            LOG.info("Starting thumbnail download for " + videoUrls.size() + " URLs.");

            new Thread(() -> {
                try {
                    if (cancelled) {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(0);
                            progressBar.setString("Cancelled");
                            resetUi.run();
                        });
                        return;
                    }
                    checkAndDownloadYTDLP();

                    for (int i = 0; i < videoUrls.size(); i++) {
                        if (cancelled) break;
                        String videoUrl = videoUrls.get(i);
                        int videoIndex = i + 1;
                        LOG.info("Downloading thumbnail for URL " + videoIndex + "/" + videoUrls.size() + ": " + videoUrl);

                        List<String> command = new ArrayList<>();
                        command.add(YTDLP_EXE.getAbsolutePath());
                        command.add("--write-thumbnail");
                        command.add("--skip-download");
                        command.add("--convert-thumbnails");
                        command.add("jpg");
                        command.add("--impersonate");
                        command.add("chrome");
                        if (!browser.isEmpty() && !browser.equals("none")) {
                            command.add("--cookies-from-browser");
                            command.add(browser);
                        }

                        String timeStamp = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date());
                        command.add("-o");
                        command.add(downloadFolder[0].getAbsolutePath() + "/%(title)s_%(id)s_" + timeStamp + ".%(ext)s");
                        command.add(videoUrl);

                        LOG.info("Executing thumbnail command: " + String.join(" ", command));

                        ProcessBuilder pb = new ProcessBuilder(command);
                        pb.redirectErrorStream(true);
                        Process process = pb.start();
                        currentProcess = process;

                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            LOG.fine("yt-dlp thumbnail output: " + line);
                            String finalLine = line;
                            SwingUtilities.invokeLater(() -> progressBar.setString(
                                    "Thumbnail " + videoIndex + " of " + videoUrls.size() + " - " + finalLine.trim()));
                        }
                        int exitCode = process.waitFor();
                        currentProcess = null;
                        LOG.info("Thumbnail download for " + videoUrl + " finished with exit code: " + exitCode);
                        if (exitCode != 0) {
                            LOG.warning("Thumbnail download failed with exit code " + exitCode + " for " + videoUrl);
                        }

                        int totalProgress = (int) (((double) (i + 1) / videoUrls.size()) * 100);
                        SwingUtilities.invokeLater(() -> progressBar.setValue(totalProgress));
                    }

                    boolean wasCancelled = cancelled;
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(wasCancelled ? 0 : 100);
                        progressBar.setString(wasCancelled ? "Thumbnail download cancelled" : "All thumbnails downloaded!");
                        resetUi.run();
                        LOG.info(wasCancelled ? "Thumbnail download cancelled" : "All thumbnails downloaded!");
                    });
                } catch (IOException | InterruptedException ex) {
                    LOG.log(Level.SEVERE, "Thumbnail download error", ex);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setString("Error: " + ex.getMessage());
                        resetUi.run();
                        JOptionPane.showMessageDialog(frame, "An error occurred: " + ex.getMessage());
                    });
                }
            }).start();
        });
    }

    private static void setupLogRedirect(JTextArea logArea) {
        PrintStream originalOut = System.out;
        OutputStream out = new OutputStream() {
            @Override
            public void write(byte[] b, int off, int len) {
                originalOut.write(b, off, len);
                String text = new String(b, off, len);
                SwingUtilities.invokeLater(() -> {
                    logArea.append(text);
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }

            @Override
            public void write(int b) {
                originalOut.write(b);
                SwingUtilities.invokeLater(() -> {
                    logArea.append(String.valueOf((char) b));
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }
        };
        PrintStream ps = new PrintStream(out, true);
        System.setOut(ps);
        System.setErr(ps);
    }

    private static boolean isValidUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equals("http") || scheme.equals("https"));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static void downloadFile(String urlStr, File target) throws IOException {
        Path targetPath = target.toPath();
        Path temp = Files.createTempFile(targetPath.getParent(), target.getName() + ".", ".part");
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(temp.toFile())) {
                in.transferTo(out);
            }
            Files.move(temp, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void checkAndDownloadYTDLP() throws IOException, InterruptedException {
        LOG.info("Checking yt-dlp existence and version...");
        if (!YTDLP_DIR.exists()) {
            YTDLP_DIR.mkdirs();
            LOG.info("Created yt-dlp directory: " + YTDLP_DIR.getAbsolutePath());
        }

        String currentInstalledVersion = null;
        if (YTDLP_EXE.exists()) {
            try {
                Process process = new ProcessBuilder(YTDLP_EXE.getAbsolutePath(), "--version").start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    currentInstalledVersion = reader.readLine();
                }
                process.waitFor();
                LOG.info("Currently installed yt-dlp version: " + currentInstalledVersion);
            } catch (Exception e) {
                LOG.warning("Failed to get installed yt-dlp version. Will re-download. Error: " + e.getMessage());
            }
        } else {
            LOG.info("yt-dlp.exe not found. Needs download.");
        }

        String latestVersion = getLatestYtDlpVersion();
        if (latestVersion == null) {
            if (YTDLP_EXE.exists()) {
                LOG.warning("Could not fetch latest yt-dlp version (offline?). Using existing executable.");
                return;
            }
            LOG.warning("Could not fetch latest yt-dlp version and no executable present. Attempting download.");
            downloadFile(YTDLP_URL, YTDLP_EXE);
            return;
        }

        if (YTDLP_EXE.exists() && latestVersion.equals(currentInstalledVersion)) {
            LOG.info("yt-dlp is up to date (" + latestVersion + "). No download needed.");
            return;
        }

        LOG.info("yt-dlp update required: installed=" + currentInstalledVersion + ", latest=" + latestVersion);
        downloadFile(YTDLP_URL, YTDLP_EXE);
        YTDLP_EXE.setExecutable(true);
        LOG.info("yt-dlp updated successfully.");
    }

    private static String getLatestYtDlpVersion() {
        LOG.info("Fetching latest yt-dlp version from GitHub API...");
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest").openConnection();
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (InputStream in = conn.getInputStream()) {
                JsonNode root = new ObjectMapper().readTree(in);
                String version = root.path("tag_name").asText(null);
                LOG.info("Latest yt-dlp version found on GitHub: " + version);
                return version;
            }
        } catch (IOException e) {
            LOG.warning("Could not fetch latest yt-dlp version from GitHub API: " + e.getMessage());
        }
        return null;
    }

    private static void checkAndDownloadFFMPEG() throws IOException {
        LOG.info("Checking ffmpeg existence and version...");
        if (!FFMPEG_DIR.exists()) {
            FFMPEG_DIR.mkdirs();
            LOG.info("Created ffmpeg directory: " + FFMPEG_DIR.getAbsolutePath());
        }
        cleanupOldFfmpegExtracts();

        if (FFMPEG_EXE.exists()) {
            LOG.info("ffmpeg.exe already exists at: " + FFMPEG_EXE.getAbsolutePath());
            return;
        }

        LOG.info("ffmpeg not found, downloading zip from: " + FFMPEG_ZIP_URL);

        File zipFile = new File(FFMPEG_DIR, "ffmpeg.zip");
        downloadFile(FFMPEG_ZIP_URL, zipFile);
        LOG.info("ffmpeg zip downloaded to: " + zipFile.getAbsolutePath());

        LOG.info("Extracting ffmpeg from zip...");
        extractFfmpegFromZip(zipFile, FFMPEG_EXE);
        zipFile.delete();
        LOG.info("ffmpeg zip deleted.");

        if (!FFMPEG_EXE.exists()) {
            throw new IOException("Не найден ffmpeg.exe внутри архива.");
        }
        FFMPEG_EXE.setExecutable(true, false);

        LOG.info("ffmpeg installed to: " + FFMPEG_EXE.getAbsolutePath());
    }

    private static void extractFfmpegFromZip(File zipFile, File outFile) throws IOException {
        LOG.info("Attempting to extract ffmpeg.exe from " + zipFile.getName());
        try (ZipFile zf = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            ZipEntry candidate = null;

            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName().replace('\\','/').toLowerCase();
                if (!e.isDirectory() && name.endsWith("/bin/ffmpeg.exe")) {
                    candidate = e;
                    LOG.info("Found ffmpeg.exe at: " + e.getName());
                    break;
                }
            }

            if (candidate == null) {
                LOG.info("ffmpeg.exe not found in /bin/ path, searching root...");
                Enumeration<? extends ZipEntry> entries2 = zf.entries();
                while (entries2.hasMoreElements()) {
                    ZipEntry e = entries2.nextElement();
                    String name = e.getName().replace('\\','/').toLowerCase();
                    if (!e.isDirectory() && name.endsWith("ffmpeg.exe")) {
                        candidate = e;
                        LOG.info("Found ffmpeg.exe at: " + e.getName() + " (root level)");
                        break;
                    }
                }
            }

            if (candidate == null) {
                throw new IOException("В архиве не найден ffmpeg.exe");
            }

            try (InputStream is = zf.getInputStream(candidate);
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) > 0) fos.write(buffer, 0, len);
                LOG.info("ffmpeg.exe extracted successfully to: " + outFile.getAbsolutePath());
            }
        }
    }

    private static void cleanupOldFfmpegExtracts() {
        File dir = FFMPEG_DIR;
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        LOG.info("Cleaning up old ffmpeg extracts in: " + dir.getAbsolutePath());
        for (File f : files) {
            if (f.getName().equalsIgnoreCase("ffmpeg.exe") || f.getName().equalsIgnoreCase("ffmpeg.zip")) {
                continue;
            }
            LOG.info("Deleting old ffmpeg related file/directory: " + f.getAbsolutePath());
            deleteRecursivelyQuiet(f);
        }
    }

    private static void deleteRecursivelyQuiet(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    deleteRecursivelyQuiet(c);
                }
            }
        }
        try {
            f.delete();
            LOG.fine("Successfully deleted: " + f.getAbsolutePath());
        } catch (Exception ignored) {
            LOG.warning("Failed to delete: " + f.getAbsolutePath());
        }
    }
}