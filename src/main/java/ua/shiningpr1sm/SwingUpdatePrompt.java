package ua.shiningpr1sm;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class SwingUpdatePrompt {
    public enum Choice { UPDATE, SKIP, CANCEL }

    private static final String FONT_NAME = "Segoe UI";

    public static Choice show(String currentVersion, String newVersion, String notesHtml) {
        Choice[] result = { Choice.CANCEL };

        JDialog dialog = new JDialog();
        dialog.setTitle("Update Available");
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel header = new JLabel("New Update Available", SwingConstants.CENTER);
        header.setFont(new Font(FONT_NAME, Font.BOLD, 18));
        header.setForeground(new Color(26, 115, 232));

        JLabel versionLabel = new JLabel(currentVersion + "  →  " + newVersion, SwingConstants.CENTER);
        versionLabel.setFont(new Font(FONT_NAME, Font.BOLD, 14));
        versionLabel.setForeground(new Color(80, 80, 80));

        JLabel whatsNewLabel = new JLabel("What's new?");
        whatsNewLabel.setFont(new Font(FONT_NAME, Font.BOLD, 12));
        whatsNewLabel.setBorder(new EmptyBorder(8, 0, 4, 0));

        String styledHtml = "<html><head><style>"
                + "body { font-family: Segoe UI, Segoe UI Emoji; font-size: 12px; "
                + "color: #333333; background-color: #f5f5f5; margin: 4px; }"
                + "ul, ol { margin: 0; padding-left: 22px; }"
                + "li { margin-left: 0; }"
                + "p { margin: 2px 0; }"
                + "code { font-family: Consolas, monospace; background-color: #e8e8e8; }"
                + "</style></head><body>"
                + notesHtml
                + "</body></html>";

        JEditorPane editorPane = new JEditorPane("text/html", styledHtml);
        editorPane.setEditable(false);
        editorPane.setOpaque(false);
        editorPane.setCaret(new javax.swing.text.DefaultCaret() {
            @Override public boolean isVisible() { return false; }
        });
        editorPane.setHighlighter(null);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setPreferredSize(new Dimension(460, 280));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        styleScrollBar(scrollPane.getVerticalScrollBar());
        styleScrollBar(scrollPane.getHorizontalScrollBar());

        JButton updateButton = new JButton("Update Now");
        updateButton.setBackground(new Color(26, 115, 232));
        updateButton.setForeground(Color.WHITE);
        updateButton.setFocusPainted(false);
        updateButton.setBorderPainted(false);
        updateButton.setFont(new Font(FONT_NAME, Font.BOLD, 13));
        updateButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton skipButton = new JButton("Skip this version");
        skipButton.setBackground(new Color(200, 200, 200));
        skipButton.setFocusPainted(false);
        skipButton.setBorderPainted(false);
        skipButton.setFont(new Font(FONT_NAME, Font.PLAIN, 12));
        skipButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        updateButton.addActionListener(e -> {
            result[0] = Choice.UPDATE;
            dialog.dispose();
        });
        skipButton.addActionListener(e -> {
            result[0] = Choice.SKIP;
            dialog.dispose();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttonPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        buttonPanel.add(skipButton);
        buttonPanel.add(updateButton);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        whatsNewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(header);
        topPanel.add(versionLabel);
        topPanel.add(whatsNewLabel);

        root.add(topPanel, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);
        root.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        return result[0];
    }

    private static void styleScrollBar(JScrollBar scrollBar) {
        scrollBar.setUnitIncrement(16);
        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                trackColor = new Color(0, 0, 0, 0);
                thumbColor = new Color(26, 115, 232, 180);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                Dimension zero = new Dimension(0, 0);
                button.setPreferredSize(zero);
                button.setMinimumSize(zero);
                button.setMaximumSize(zero);
                return button;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (!c.isEnabled() || thumbBounds.isEmpty()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 8, 8);
                g2.dispose();
            }
        });
    }
}