
package com.intellij.plugins.bodhi.pmd;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.plugins.bodhi.pmd.tree.Severity;
import com.intellij.ui.JBColor;
import com.intellij.ui.OnePixelSplitter;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
                        adjustSplitterBasedOnContentHeight(height, detailsSplit);
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
            htmlBuilder.append("  <div class=\"title\">").append(message).append("</div>\n");
            RulePriority rulePriority = rule.getPriority();
            Severity severity = Severity.of(rulePriority);
            String severityName = severity.getName();
            String severityColor = colorToHex(severity.getColor());

            htmlBuilder.append("  <div class='severity' style='background-color: ").append(severityColor)
                .append(";'>").append(rulePriority.getPriority()).append(":").append(severityName).append("</div>\n");
            htmlBuilder.append("</div>");

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
        htmlBuilder.append("</div>");
    }

    /**
     * Converts a Color object to its hexadecimal representation for HTML/CSS.
     */
    private static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static void adjustSplitterBasedOnContentHeight(float htmlHeight,
                                                           @NotNull OnePixelSplitter detailsSplit) {
        ApplicationManager.getApplication().invokeLater(() -> {
            int visibleHeight = detailsSplit.getHeight();
            if (visibleHeight > 20) {
                detailsSplit.setProportion(calculateSplitProportion(htmlHeight, visibleHeight));
                detailsSplit.repaint();
            }
        });
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