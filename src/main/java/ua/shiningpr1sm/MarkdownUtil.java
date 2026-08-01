package ua.shiningpr1sm;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class MarkdownUtil {
    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder()
            .softbreak("<br/>\n")
            .build();

    public static String toPlainText(String markdown) {
        if (markdown == null) return "(no release notes)";
        String result = markdown
                .replaceAll("(?m)^#{1,6}\\s*", "")
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("\\*(.*?)\\*", "$1")
                .replaceAll("`([^`]*)`", "$1")
                .replaceAll("(?m)^[-*]\\s+", "• ")
                .trim();
        return result.isEmpty() ? "(no release notes)" : result;
    }

    public static String toHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) return "<p>(no release notes)</p>";
        Node document = PARSER.parse(markdown);
        String html = RENDERER.render(document);
        return html
                .replace("<blockquote><p>", "<div style='margin: 2px 0; padding-left: 10px; border-left: 2px solid #9e9e9e; color: #666666;'")
                .replace("</p></blockquote>", "</div>")
                .replace("<blockquote>", "<div style='margin: 2px 0; padding-left: 10px; border-left: 2px solid #9e9e9e; color: #666666;'")
                .replace("</blockquote>", "</div>");
    }
}