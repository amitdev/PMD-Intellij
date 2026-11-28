
package com.intellij.plugins.bodhi.pmd;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.plugins.bodhi.pmd.core.RuleSetScore;
import com.intellij.plugins.bodhi.pmd.tree.PMDRuleSetNode;
import com.intellij.plugins.bodhi.pmd.tree.Severity;
import com.intellij.plugins.bodhi.pmd.tree.SustainabilityLeaves;
import com.intellij.ui.JBColor;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.jcef.JBCefClient;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.lang.rule.RulePriority;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class PMDHTMLUtil {

    public static final String HTML_INITIAL_BODY_CONTENT = "<html><body>Select a violation for details.</body></html>";
    private static final Logger log = Logger.getInstance(PMDHTMLUtil.class);
    private static final Pattern BRACED_RULES_NAME_PATTERN = Pattern.compile("\\([\\w-]+-rules\\)\\s*$", Pattern.MULTILINE);

    /**
     * JavaScript to handle link clicks in the JCEF browser.
     */
    private static final String LINK_CLICK_HANDLER_JS =
            """
                    (function() {
                        document.addEventListener('click', function(event) {
                            if (event.target.tagName === 'A' && event.target.href) {
                                event.preventDefault();
                                window.location.href = 'about:blank#' + encodeURIComponent(event.target.href);
                            }
                        });
                    })();""";

    /**
     * Base CSS styles that apply to both themes.
     */
    private static final String BASE_CSS_STYLES =
            """
                    /* Base styles */
                    html {
                        height: 100%;
                        overflow: hidden;
                    }
                    
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                        margin: 3px;
                        padding-right: 2px;
                        line-height: 1.5;
                        font-size: 13px;
                        height: 100%;
                        overflow-x: hidden;
                        overflow-y: auto;
                        word-wrap: break-word;
                        box-sizing: border-box;
                    }
                    
                    p {
                        margin-top: 0.5em;
                        margin-bottom: 0.7em;
                    }
                    
                    a {
                        text-decoration: none;
                    }
                    
                    a:hover {
                        text-decoration: underline;
                    }
                    
                    pre {
                        white-space: pre-wrap;
                        word-wrap: break-word;
                        max-width: 100%;
                        padding: 4px;
                        border-radius: 4px;
                        box-sizing: border-box;
                    }
                    
                    code {
                        padding: 2px 4px;
                        border-radius: 2px;
                    }
                    
                    img {
                        max-width: 100%;
                        height: auto;
                    }
                    
                    /* Customize scrollbar for better visibility */
                    ::-webkit-scrollbar {
                        width: 6px;
                    }
                    
                    ::-webkit-scrollbar-track {
                        background-color: transparent;
                    }
                    
                    .content-wrapper {
                        padding-right: 1px;
                    }
                    
                    .severity {
                        display: inline-block;
                        padding: 2px 5px;
                        border-radius: 4px;
                        font-weight: bold;
                        margin-left: 4px;
                        color: #FFFFFF !important;
                    }
                    
                    .tag {
                        font-size: 9px;
                        display: inline-block;
                        padding: 1px 3px;
                        border-radius: 4px;
                        margin-left: 4px;
                    }
                    
                    .rule-id {
                        font-size: 9px;
                        padding: 0px 0px;
                        margin-left: 0px;
                    }\
                    
                    .title-severity {
                      display: flex;
                      justify-content: space-between;
                      align-items: center;
                      padding: 1px 0px;
                      margin-bottom: 3px;
                    }
                    
                    .title {
                      flex: 1;
                      margin-right: 6px;
                      font-weight: bold;
                      word-wrap: break-word;
                      word-break: break-word;
                      overflow-wrap: break-word;
                    }
                    """;

    /**
     * CSS styles specific to light theme.
     */
    private static final String LIGHT_THEME_CSS =
            """
                    body {
                        color: #000000;
                        background-color: #ffffff;
                    }
                    
                    a {
                        color: #2470B3;
                    }
                    
                    pre, code {
                        background-color: #f5f5f5;
                        border: 1px solid #e0e0e0;
                        color: #000000;
                    }
                    
                    td, th, h1, h2, h3, h4, h5, h6 {
                        color: #000000;
                    }
                    
                    .title-severity {
                      border-bottom: 1px solid var(--border-color, #e0e0e0);
                    }
                    
                    ::-webkit-scrollbar-thumb {
                        background-color: rgba(128, 128, 128, 0.5);
                        border-radius: 4px;
                    }""";

    /**
     * CSS styles specific to dark theme.
     */
    private static final String DARK_THEME_CSS =
            """
                    body {
                        color: #e8e8e8;
                        background-color: #2b2b2b;
                    }
                    
                    a {
                        color: #7dacf7;
                    }
                    
                    pre, code {
                        background-color: #383838;
                        border: 1px solid #555555;
                        color: #e8e8e8;
                    }
                    
                    td, th, h1, h2, h3, h4, h5, h6 {
                        color: #e8e8e8;
                    }
                    
                    .title-severity {
                      border-bottom: 1px solid var(--border-color, #202020);
                    }
                    
                    ::-webkit-scrollbar-thumb {
                        background-color: rgba(200, 200, 200, 0.5);
                        border-radius: 4px;
                    }""";

    private static final Map<String, String> SVG_BASE64 = new ConcurrentHashMap<>();
    private static final String DIV_END = "</div>";
    private static final String DIV_END_LINE = DIV_END + "\n";
    private static final String P_END_LINE = "</p>\n";

    private PMDHTMLUtil() {
        // utility class not to be instantiated
    }

    /**
     * Builds complete HTML document with the provided content, including appropriate styles for the current theme.
     */
    public static @NotNull String buildCompleteHtml(String htmlContent) {
        // Detect which theme is active
        boolean isDarkTheme = isDarkTheme();

        // Combine base CSS with theme-specific CSS
        String themeCSS = isDarkTheme ? DARK_THEME_CSS : LIGHT_THEME_CSS;
        String combinedCSS = BASE_CSS_STYLES + "\n\n" + themeCSS;

        return "<!DOCTYPE html>" +
                "<html style='height:100%; overflow:hidden;'>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>" + combinedCSS + "</style>" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"content-wrapper\">\n" + htmlContent + "\n</div>\n" +
                "<script>" + LINK_CLICK_HANDLER_JS + "</script>" +
                "</body>" +
                "</html>";
    }


    /**
     * Configures the HTML panel that displays rule documentation and adjusts the splitter proportion when  refreshed.
     */
    public static @NotNull JComponent configureHtmlPanel(@NotNull JCEFHtmlPanel htmlPanel, @NotNull OnePixelSplitter detailsSplit, @NotNull HTMLReloadable resultPanel) {

        // Get browser instance
        CefBrowser cefBrowser = htmlPanel.getCefBrowser();
        JBCefClient jbCefClient = htmlPanel.getJBCefClient();
        // Register a handler for JavaScript callbacks
        jbCefClient.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                resultPanel.onHtmlLoaded();
                // Inject JavaScript to measure HTML content height (first element: DIV) after load completes
                String measureHeightJs =
                        "setTimeout(function() {" +
                                //"  const height = document.documentElement.scrollHeight;" +
                        "   let height = 0;\n" +
                        "   const elements = document.body.children;\n" +
                        "   if (elements.length > 0) {\n" +
                        "       const height = elements[0].getBoundingClientRect().bottom;\n" +
                        "       console.log('CONTENT_HEIGHT:' + height);\n" +
                        "   }\n" +
                        "}, 20);"; // Small delay to ensure content is rendered

                browser.executeJavaScript(measureHeightJs, browser.getURL(), 0);
            }
        }, cefBrowser);

        // Setup console message handler to intercept height measurements
        jbCefClient.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
                if (message.startsWith("CONTENT_HEIGHT:")) {
                    try {
                        String heightStr = message.substring("CONTENT_HEIGHT:".length());
                        float height = Float.parseFloat(heightStr.trim());
                        adjustSplitterBasedOnContentHeight(height, detailsSplit, resultPanel.isFitSplitToContent(), browser);
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse content height, not adjusting splitter.", e);
                    }
                }
                return false; // Allow the message to be logged
            }
        }, cefBrowser);

        // Add URL change handler to intercept navigation to link URLs
        jbCefClient.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                if (url.startsWith("about:blank#")) {
                    // Extract the actual URL from the hash part
                    String actualUrl;
                    try {
                        actualUrl = URLDecoder.decode(
                                url.substring("about:blank#".length()),
                                StandardCharsets.UTF_8
                        );

                        // Open the URL in the default system browser
                        ApplicationManager.getApplication().invokeLater(() -> BrowserUtil.browse(actualUrl));

                        // Reload the content to prevent navigation away
                        // Reload the last content
                        ApplicationManager.getApplication().invokeLater(resultPanel::reloadHTML);
                    } catch (Exception e) {
                        // Log the error and continue
                        log.warn("Error handling link click: " + e.getMessage(), e);
                    }
                }
            }
        }, cefBrowser);

        // refresh when the theme changes
        ApplicationManager.getApplication().getMessageBus().connect()
                .subscribe(LafManagerListener.TOPIC, (LafManagerListener) source -> resultPanel.reloadHTML());

        // Initialize the HTML content with base styling
        String completeHtml = buildCompleteHtml(HTML_INITIAL_BODY_CONTENT);

        // Load the HTML content with our styling
        htmlPanel.loadHTML(completeHtml);

        return htmlPanel.getComponent();
    }

    /**
     * Determines if the current IDE theme is dark.
     */
    private static boolean isDarkTheme() {
        return !JBColor.isBright();
    }

    /**
     * Generates HTML content for displaying rule information or message.
     */
    public static @NotNull String getHtmlText(@NotNull String message, @Nullable Rule rule) {
        StringBuilder htmlBuilder = new StringBuilder();

        if (rule == null) {
            appendWithOptionalPreTo(htmlBuilder, message);
        }
        else {
            // Start with the title and severity on the same line
            htmlBuilder.append("<div class=\"title-severity\">\n");
            htmlBuilder.append("  <div class=\"title\">").append(message).append(DIV_END_LINE);
            RulePriority rulePriority = rule.getPriority();
            Severity severity = Severity.of(rulePriority);
            String severityName = severity.getName();
            String severityColor = colorToHex(severity.getColor());

            htmlBuilder.append("  <div class='severity' style='background-color: ").append(severityColor)
                .append(";'>").append(rulePriority.getPriority()).append(":").append(severityName).append(DIV_END_LINE);
            htmlBuilder.append(DIV_END);

            // Add rule name and tags in badge format
            appendRuleNameAndTagsTo(htmlBuilder, rule);

            String descMd = rule.getDescription();
            descMd = BRACED_RULES_NAME_PATTERN.matcher(descMd).replaceAll("");
            String descHtml = MdToHtmlConverter.convertToHtml(descMd);

            htmlBuilder.append(descHtml);

            String url = rule.getExternalInfoUrl();
            String linkHtml = "";
            if (url != null && !url.isEmpty()) {
                linkHtml = "<p><a href=\"" + url + "\">Full documentation</a></p>";
            }
            htmlBuilder.append(linkHtml);
        }
        return htmlBuilder.toString();
    }

    /**
     * Generates HTML content for the scores of a rule set: its description (without the score definitions) in normal
     * text, then the score block, then the sustainability block, each with its legend.
     *
     * @param description the rule set description without the score marker lines; null or empty for none
     */
    public static @NotNull String getScoreDetailsHtml(@Nullable String description, @NotNull Optional<RuleSetScore> score,
                                                      @NotNull Optional<RuleSetScore> sustainability) {
        String descriptionHtml = description == null || description.isBlank() ? "" : "<p>" + escapeHtml(description) + P_END_LINE;
        return descriptionHtml + scoresHtml(score, sustainability, true);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Generates HTML content with the scores of all rule sets that have one (for the root node): per rule set its
     * name and score labels, without calculation and legend.
     *
     * @return the HTML, empty when no rule set has a score
     */
    public static @NotNull String getRuleSetScoresHtml(@NotNull List<PMDRuleSetNode> ruleSetNodes) {
        StringBuilder html = new StringBuilder();
        for (PMDRuleSetNode node : ruleSetNodes) {
            Optional<RuleSetScore> score = node.getScore();
            Optional<RuleSetScore> sustainability = node.getSustainabilityScore();
            if (score.isPresent() || sustainability.isPresent()) {
                html.append(html.isEmpty() ? "" : "\n<div style='margin-top: 16px;'></div>\n")
                        .append("<p>").append(node.getNodeName()).append(P_END_LINE)
                        .append(scoresHtml(score, sustainability, false));
            }
        }
        return html.toString();
    }

    private static String scoresHtml(Optional<RuleSetScore> score, Optional<RuleSetScore> sustainability, boolean withDetails) {
        StringBuilder html = new StringBuilder();
        score.ifPresent(s -> html.append(getScoreHtml(s, withDetails)));
        sustainability.ifPresent(s -> html.append(html.isEmpty() || !withDetails ? "" : "\n<div style='margin-top: 12px;'></div>\n")
                .append(getSustainabilityHtml(s, isDarkTheme(), withDetails)));
        return html.toString();
    }

    static @NotNull String getSustainabilityHtml(@NotNull RuleSetScore score, boolean dark) {
        return getSustainabilityHtml(score, dark, true);
    }

    /**
     * Generates HTML content for the sustainability score of a rule set: the label with the leaves in the leaf color,
     * and optionally the details: the formula and the legend (the scale of gray leaves, current level framed).
     *
     * @param dark whether to use the leaf images for a dark theme
     */
    static @NotNull String getSustainabilityHtml(@NotNull RuleSetScore score, boolean dark, boolean withDetails) {
        String labelColor = colorToHex(SustainabilityLeaves.labelColor(score.gradeIndex()));
        String gray = colorToHex(SimpleTextAttributes.GRAYED_ATTRIBUTES.getFgColor());
        String legend = "";
        if (withDetails) {
            // '<' only occurs as separator " < ", so escaping it there leaves the tags intact
            legend = score.gradeScale(level -> leavesHtml(level, dark, true), leaves -> framed(leaves, gray))
                    .replace(" < ", " &lt; ");
        }
        return "<div class=\"title\" style='color: " + labelColor + ";'>" + score.spec().name() + ": "
                + leavesHtml(score.gradeIndex(), dark, false) + DIV_END_LINE +
                (withDetails ? detailsHtml(score, gray, legend) : "");
    }

    /**
     * @return the content in a small frame, marking the current grade or level in a legend
     */
    private static String framed(String content, String color) {
        return "<span style='border: 1px solid " + color + "; border-radius: 3px; padding: 1px 2px;'>" + content + "</span>";
    }

    private static String detailsHtml(RuleSetScore score, String gray, String legend) {
        return "<div style='color: " + gray + ";'>\n" +
                "  <p>" + score.formula() + P_END_LINE +
                (legend.isEmpty() ? "" : "  <p>" + legend + P_END_LINE) +
                DIV_END;
    }

    private static String leavesHtml(int level, boolean dark, boolean gray) {
        String img = "<img src='data:image/svg+xml;base64," + svgBase64(SustainabilityLeaves.svgResource(level, dark))
                + "' width='16' height='16' style='vertical-align: middle;" + (gray ? " filter: grayscale(100%);" : "") + "'>";
        return img.repeat(SustainabilityLeaves.count(level));
    }

    private static String svgBase64(String resource) {
        return SVG_BASE64.computeIfAbsent(resource, r -> {
            try (InputStream in = SustainabilityLeaves.class.getResourceAsStream(r)) {
                return in == null ? "" : Base64.getEncoder().encodeToString(in.readAllBytes());
            } catch (IOException e) {
                log.warn("Cannot read " + r, e);
                return "";
            }
        });
    }

    public static @NotNull String getScoreHtml(@NotNull RuleSetScore score) {
        return getScoreHtml(score, true);
    }

    /**
     * Generates HTML content for the score of a rule set: the score label in the grade color,
     * and optionally the details: the formula and the legend (the grade scale, current grade framed), in gray like the
     * counts in the tree.
     */
    static @NotNull String getScoreHtml(@NotNull RuleSetScore score, boolean withDetails) {
        String gradeColor = colorToHex(PMDRuleSetNode.gradeColor(score.gradeIndex()));
        String gray = colorToHex(SimpleTextAttributes.GRAYED_ATTRIBUTES.getFgColor());
        // '<' only occurs as separator " < ", so escaping it there leaves the tags intact
        String legend = withDetails ? score.gradeScale(grade -> framed(grade, gray)).replace(" < ", " &lt; ") : "";
        return "<div class=\"title\" style='color: " + gradeColor + ";'>" + score.gradeLabel() + DIV_END_LINE +
                (withDetails ? detailsHtml(score, gray, legend) : "");
    }

    /**
     * Appends message to HTML builder, wrapping stack traces in <pre> tags if present.
     */
    private static void appendWithOptionalPreTo(StringBuilder htmlBuilder, @NotNull String message) {
        String[] splits = message.split("\n\n", 2); // optional part 2, for instance, a stack trace
        htmlBuilder.append("<div class='title'><strong>").append(splits[0]).append("</strong></div>");
        if (splits.length > 1) {
            htmlBuilder.append("<p><pre>").append(splits[1]).append("</pre></p>");
        }
    }

    /**
     * Appends rule name and tags to HTML builder.
     */
    private static void appendRuleNameAndTagsTo(StringBuilder htmlBuilder, @NotNull Rule rule) {
        htmlBuilder.append("<div class='rule-id'>");

        // Add rule name / ID
        String ruleName = rule.getName();
        if (ruleName != null && !ruleName.isEmpty()) {
            htmlBuilder.append("Rule ID: ").append(ruleName).append(" ");
        }

        // Tag badges always use off-white on gray
        String bgColor = "#808080";
        String textColor = "#E8E8E8";
        // Add tags as badges
        PropertyDescriptor<?> tagsDescriptor = rule.getPropertyDescriptor("tags");
        if (tagsDescriptor != null) {
            Object value = rule.getProperty(tagsDescriptor);
            if (value != null) {
                String tagsString = value.toString();
                String[] tags = tagsString.split(",");
                for (String tag : tags) {
                    tag = tag.trim();
                    if (!tag.isEmpty() && !tag.endsWith("-rule")) {
                        htmlBuilder.append(" <span class='tag' style='background-color: ").append(bgColor).append("; color: ").append(textColor);
                        htmlBuilder.append(";'>").append(tag).append("</span> ");
                    }
                }
            }
        }
        htmlBuilder.append(DIV_END);
    }

    /**
     * Converts a Color object to its hexadecimal representation for HTML/CSS.
     */
    private static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static void adjustSplitterBasedOnContentHeight(float htmlHeight,
                                                           @NotNull OnePixelSplitter detailsSplit,
                                                           boolean fitSplitToContent,
                                                           @NotNull CefBrowser browser) {
        ApplicationManager.getApplication().invokeLater(() -> {
            int visibleHeight = detailsSplit.getHeight();
            if (fitSplitToContent && visibleHeight > 20) {
                float proportion = calculateSplitProportion(htmlHeight, visibleHeight);
                if (shouldMoveSplitter(detailsSplit.getProportion(), proportion, visibleHeight)) {
                    detailsSplit.setProportion(proportion);
                    detailsSplit.repaint();
                }
            }
            browser.executeJavaScript(RESTORE_SCROLLBAR_JS, browser.getURL(), 0);
        });
    }

    /**
     * Decides whether to move the splitter to fit new content, keeping resizes of the browser (which flicker) rare:
     * grow when the content needs at least a few pixels more, shrink only when that frees a large part of the height.
     */
    static boolean shouldMoveSplitter(float currentProportion, float wantedProportion, int visibleHeight) {
        float extraPixelsNeeded = (wantedProportion - currentProportion) * visibleHeight;
        return extraPixelsNeeded >= 4 || -extraPixelsNeeded >= 0.15f * visibleHeight;
    }

    /**
     * Restores the scrollbar hidden by {@link #replaceContentJs(String)}, after the browser got its new size.
     */
    static final String RESTORE_SCROLLBAR_JS = "setTimeout(function() { document.body.style.overflowY = ''; }, 100);";

    static @NotNull String replaceContentJs(@NotNull String htmlContent) {
        String base64 = Base64.getEncoder().encodeToString(htmlContent.getBytes(StandardCharsets.UTF_8));
        return "(function() {" +
                "  const wrapper = document.querySelector('.content-wrapper');" +
                "  if (!wrapper) { return; }" +
                // no scrollbar while the splitter isn't fitted to the new content yet: avoids a scrollbar flash and reflow
                "  document.body.style.overflowY = 'hidden';" +
                "  wrapper.innerHTML = new TextDecoder().decode(Uint8Array.from(atob('" + base64 + "'), c => c.charCodeAt(0)));" +
                "  window.scrollTo(0, 0);" +
                "  requestAnimationFrame(function() {" +
                "    console.log('CONTENT_HEIGHT:' + wrapper.getBoundingClientRect().bottom);" +
                "  });" +
                "})();";
    }

    /**
     * Calculates the optimal proportion for the splitter based on the HTML height and visible height.
     * Optimal means everything of HTML, and the rest for examples; unless only less than 10-15% is left for examples, then keep 10-15% for examples.
     * @param htmlHeight the actual height of the HTML content
     * @param visibleHeight the visible height of the splitter
     * @return the optimal proportion for the splitter
     */
    private static float calculateSplitProportion(float htmlHeight, int visibleHeight) {
            float padding = 12.0f;
            float optimalProportion = (htmlHeight + padding) / visibleHeight;
        if (optimalProportion > 0.8f && optimalProportion < 0.85f) {
            // add a few pixels to remove the slider
            optimalProportion = (float) (htmlHeight + 14.0) / visibleHeight;
            optimalProportion = Math.min(0.9f, optimalProportion);
        }
        else {
            // Ensure proportion is within reasonable bounds
            optimalProportion = Math.min(0.85f, Math.max(0.15f, optimalProportion));
        }
        return optimalProportion;
    }
}