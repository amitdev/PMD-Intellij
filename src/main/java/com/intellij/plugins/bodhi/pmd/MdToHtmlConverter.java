package com.intellij.plugins.bodhi.pmd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts Markdown text to HTML format.
 * <p>
 * This class supports the following Markdown elements:
 * <ul>
 *   <li>Paragraphs</li>
 *   <li>Ordered lists (including multi-line items)</li>
 *   <li>Unordered lists (using * as bullet points)</li>
 *   <li>Sections with titles (e.g. "Problem:", "Solution:", "Note:", "Notes:", "Exceptions:")</li>
 *   <li>Inline code blocks (marked with backticks `code`)</li>
 *   <li>Multi-line code blocks (marked with triple backticks ```code```)</li>
 *   <li>Code blocks with language specification (```java code```)</li>
 *   <li>Rule references (in the form {% rule "RuleName" %})</li>
 * </ul>
 * <p>
 * Special handling is provided for common section patterns like "Problem:" followed by 
 * "Solution:" on a new line within the same paragraph. These are split into separate 
 * paragraphs with properly formatted titles.
 * <p>
 * The converter uses pre-compiled regex patterns for optimal performance when processing
 * large volumes of text.
 * <p>
 * Example usage:
 * <pre>{@code
 * String markdown = "Problem: This is a problem description.\n" +
 *                   "Solution: This is the solution.";
 * String html = MdToHtmlConverter.convertToHtml(markdown);
 * }</pre>
 */
public class MdToHtmlConverter {
    // Regex patterns
    private static final Pattern PARAGRAPH_SPLITTER_PATTERN = Pattern.compile("\n\\s*\n");
    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\n");
    private static final Pattern NEWLINE_LIMIT_PATTERN = Pattern.compile("\n", Pattern.MULTILINE);
    private static final Pattern ORDERED_LIST_PARAGRAPH_PATTERN = Pattern.compile("(?s)\\s*1\\..*");
    private static final Pattern ORDERED_LIST_ITEM_START_PATTERN = Pattern.compile("(?m)^\\s*(\\d+)\\.\\s");
    private static final Pattern UNORDERED_LIST_PARAGRAPH_PATTERN = Pattern.compile("(?s)\\s*\\*.*");
    private static final Pattern SECTION_PARAGRAPH_PATTERN = Pattern.compile("(?s)\\s*[A-Za-z]+:\\s*.*");
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("(\\d+)\\.(\\s+)(.*)");
    private static final Pattern UNORDERED_LIST_ITEM_PATTERN = Pattern.compile("\\*(\\s+)(.*)");
    private static final Pattern TITLE_PATTERN = Pattern.compile("([A-Za-z]+):(\\s*)(.*)");
    private static final Pattern INLINE_TITLE_PATTERN = Pattern.compile("\\b([A-Za-z]+):(\\s*)");
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern RULE_REFERENCE_PATTERN = Pattern.compile("\\{%\\s*rule\\s*\"([^\"]+)\"\\s*%\\}");
    private static final Pattern SECTION_PATTERN = Pattern.compile("(?s)(Problem|Solution|Note|Notes|Exceptions):(.*?)(?=\\s+(Problem|Solution|Note|Notes|Exceptions):|$)", Pattern.DOTALL);
    private static final Pattern MULTI_LINE_CODE_BLOCK_PATTERN = Pattern.compile("(?s)```(\\w*)\\s*([\\s\\S]*?)```");
    private static final Pattern QUADRUPLE_BACKTICK_CODE_BLOCK_PATTERN = Pattern.compile("(?s)````(\\w*)\\s*([\\s\\S]*?)````");
    private static final Pattern SECTION_WITH_LIST_PATTERN = Pattern.compile("(?m)^(\\s*)(Problem|Solution|Note|Notes|Exceptions):(\\s*)(.*)(?:\\n(?:\\1\\s+.*)?)*");

    public static String convertToHtml(String markdownText) {
        StringBuilder html = new StringBuilder();
        boolean firstParagraph = true;

        // First, handle code blocks with quadruple backticks (to avoid nesting issues)
        markdownText = handleMultiLineCodeBlocks(markdownText, QUADRUPLE_BACKTICK_CODE_BLOCK_PATTERN);

        // Then, handle code blocks with triple backticks
        markdownText = handleMultiLineCodeBlocks(markdownText, MULTI_LINE_CODE_BLOCK_PATTERN);

        // Look for sections that might contain lists (like Notes: followed by numbered items)
        Matcher sectionWithListMatcher = SECTION_WITH_LIST_PATTERN.matcher(markdownText);
        boolean processedSections = false;

        StringBuilder modifiedText = new StringBuilder();
        while (sectionWithListMatcher.find()) {
            processedSections = true;
            String sectionText = sectionWithListMatcher.group(0);
            String sectionType = sectionWithListMatcher.group(2);

            // Check if this section might contain a list
            if (sectionText.contains("\n1.") || sectionText.contains("\n *")) {
                String processedSection = processSectionWithList(sectionText, sectionType);
                sectionWithListMatcher.appendReplacement(modifiedText, Matcher.quoteReplacement(processedSection));
            } else {
                sectionWithListMatcher.appendReplacement(modifiedText, Matcher.quoteReplacement(sectionText));
            }
        }

        if (processedSections) {
            sectionWithListMatcher.appendTail(modifiedText);
            markdownText = modifiedText.toString();
        }

        // Special case for sections like Problem:/Solution:/Note:/Exceptions: appearing in the same paragraph
        boolean foundSections = false;
        Matcher sectionMatcher = SECTION_PATTERN.matcher(markdownText);

        // Find all sections and process them separately
        while (sectionMatcher.find()) {
            foundSections = true;
            String sectionType = sectionMatcher.group(1);
            String sectionContent = sectionMatcher.group(2).trim();

            // Skip if it's just a header with no content
            if (sectionContent.isEmpty()) {
                continue;
            }

            // Check if section content starts with a list
            if (sectionContent.startsWith("1.") || (sectionContent.length() > 2 && sectionContent.substring(0, 2).equals("1."))) {
                html.append("<p><strong>").append(sectionType).append(":</strong></p>\n");
                html.append(convertOrderedList(sectionContent));
            } else {
                if (firstParagraph) { // no empty line before the first paragraph needed
                    html.append("<strong>").append(sectionType).append(":</strong> ")
                            .append(formatInlineElements(sectionContent)).append("<br>\n");
                    firstParagraph = false;
                }
                else {
                    html.append("<p><strong>").append(sectionType).append(":</strong> ")
                            .append(formatInlineElements(sectionContent)).append("</p>\n");
                }
            }
        }

        // If we found and processed sections, return the result
        if (foundSections) {
            return html.toString();
        }

        // Continue with the normal processing for other cases
        String[] paragraphs = PARAGRAPH_SPLITTER_PATTERN.split(markdownText);

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();

            // Skip empty paragraphs
            if (paragraph.isEmpty()) {
                continue;
            }

            // Check if this paragraph is a code block (already processed)
            if (paragraph.startsWith("<pre><code") && paragraph.endsWith("</code></pre>")) {
                html.append(paragraph).append("\n\n");
                continue;
            }

            // Check if this is a standalone ordered list (starts with a number followed by a period)
            if (ORDERED_LIST_PARAGRAPH_PATTERN.matcher(paragraph).matches()) {
                // Convert numbered list
                html.append(convertOrderedList(paragraph));
            }
            // Check if this is an unordered list (starts with asterisks)
            else if (UNORDERED_LIST_PARAGRAPH_PATTERN.matcher(paragraph).matches()) {
                // Convert bullet list
                html.append(convertUnorderedList(paragraph));
            }
            // Check if this is a section with a title (like "Limitations:")
            else if (SECTION_PARAGRAPH_PATTERN.matcher(paragraph).matches()) {
                html.append(convertSection(paragraph));
            }
            // Check if the paragraph contains a numbered list inside it
            else if (ORDERED_LIST_ITEM_START_PATTERN.matcher(paragraph).find()) {
                html.append(convertParagraphWithOrderedList(paragraph));
            }
            // Regular paragraph
            else {
                if (firstParagraph) { // no empty line before the first paragraph needed
                    html.append(formatInlineElements(paragraph)).append("<br>\n\n");
                    firstParagraph = false;
                }
                else {
                    html.append("<p>").append(formatInlineElements(paragraph)).append("</p>\n\n");
                }
            }
        }

        return html.toString();
    }

    private static String processSectionWithList(String sectionText, String sectionType) {
        StringBuilder result = new StringBuilder();

        // Extract the section title line
        String[] lines = NEWLINE_PATTERN.split(sectionText);
        String titleLine = lines[0].trim();

        // Check if the title line has content after the colon
        Matcher titleMatcher = Pattern.compile(sectionType + ":(\\s*)(.*)").matcher(titleLine);
        if (titleMatcher.find()) {
            String contentAfterTitle = titleMatcher.group(2).trim();

            // Add the section title
            result.append("<p><strong>").append(sectionType).append(":</strong>");

            // If there is content on the title line, add it
            if (!contentAfterTitle.isEmpty()) {
                result.append(" ").append(formatInlineElements(contentAfterTitle));
                result.append("</p>\n");
            } else {
                // No content after title, just close the paragraph
                result.append("</p>\n");

                // Check if the next lines form a list
                if (lines.length > 1) {
                    StringBuilder listContent = new StringBuilder();
                    for (int i = 1; i < lines.length; i++) {
                        listContent.append(lines[i]).append("\n");
                    }

                    String listText = listContent.toString().trim();
                    if (listText.startsWith("1.")) {
                        result.append(convertOrderedList(listText));
                    } else if (listText.startsWith("*")) {
                        result.append(convertUnorderedList(listText));
                    } else {
                        result.append("<p>").append(formatInlineElements(listText)).append("</p>\n");
                    }
                }
            }
        }

        return result.toString();
    }

    private static String handleMultiLineCodeBlocks(String markdownText, Pattern pattern) {
        Matcher codeMatcher = pattern.matcher(markdownText);
        StringBuilder result = new StringBuilder();

        while (codeMatcher.find()) {
            String language = codeMatcher.group(1).trim();
            String codeContent = codeMatcher.group(2);

            // Escape HTML entities in code
            codeContent = escapeHtml(codeContent);

            // Build HTML for code block
            StringBuilder codeHtml = new StringBuilder();
            codeHtml.append("<pre><code");

            // Add language class if specified
            if (!language.isEmpty()) {
                codeHtml.append(" class=\"language-").append(language).append("\"");
            }

            codeHtml.append(">").append(codeContent).append("</code></pre>");

            // Replace original code block with HTML
            codeMatcher.appendReplacement(result, Matcher.quoteReplacement(codeHtml.toString()));
        }
        codeMatcher.appendTail(result);

        return result.toString();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String convertParagraphWithOrderedList(String paragraph) {
        StringBuilder html = new StringBuilder();

        // Split into lines
        String[] lines = NEWLINE_PATTERN.split(paragraph);

        int listStartIndex = -1;
        // Find where the list starts
        for (int i = 0; i < lines.length; i++) {
            if (LIST_ITEM_PATTERN.matcher(lines[i].trim()).matches()) {
                listStartIndex = i;
                break;
            }
        }

        if (listStartIndex > 0) {
            // There's text before the list
            StringBuilder initialText = new StringBuilder();
            for (int i = 0; i < listStartIndex; i++) {
                if (i > 0) initialText.append(" ");
                initialText.append(lines[i].trim());
            }
            html.append("<p>").append(formatInlineElements(initialText.toString())).append("</p>\n");
        }

        // Process the list - improved to handle multi-line list items
        html.append("<ol>\n");
        StringBuilder currentItemContent = new StringBuilder();
        int currentItemNumber = -1;

        for (int i = listStartIndex; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher matcher = LIST_ITEM_PATTERN.matcher(line);

            if (matcher.find()) {
                // If we already have content from a previous item, add it first
                if (currentItemNumber > 0 && currentItemContent.length() > 0) {
                    html.append("    <li>").append(formatInlineElements(currentItemContent.toString())).append("</li>\n");
                    currentItemContent = new StringBuilder();
                }

                // Start a new item
                currentItemNumber = Integer.parseInt(matcher.group(1));
                currentItemContent.append(matcher.group(3));
            } else if (!line.isEmpty() && currentItemContent.length() > 0) {
                // This is a continuation of the current list item
                // Add a space if the current content doesn't end with a space
                if (!currentItemContent.toString().endsWith(" ")) {
                    currentItemContent.append(" ");
                }
                currentItemContent.append(line);
            }
        }

        // Don't forget to add the last item
        if (currentItemContent.length() > 0) {
            html.append("    <li>").append(formatInlineElements(currentItemContent.toString())).append("</li>\n");
        }

        html.append("</ol>\n\n");

        return html.toString();
    }

    private static String convertOrderedList(String listText) {
        StringBuilder html = new StringBuilder("<ol>\n");

        // Split into lines
        String[] lines = NEWLINE_PATTERN.split(listText);
        StringBuilder currentItemContent = new StringBuilder();
        boolean hasStarted = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher matcher = LIST_ITEM_PATTERN.matcher(line);

            if (matcher.find()) {
                // If we haven't started the list yet
                if (!hasStarted) {
                    hasStarted = true;
                } else if (currentItemContent.length() > 0) {
                    // Output the previous item before starting a new one
                    html.append("    <li>").append(formatInlineElements(currentItemContent.toString())).append("</li>\n");
                    currentItemContent = new StringBuilder();
                }

                // Start a new item
                currentItemContent.append(matcher.group(3));
            } else if (!line.isEmpty()) {
                if (hasStarted && currentItemContent.length() > 0) {
                    // This is a continuation of the current list item
                    if (!currentItemContent.toString().endsWith(" ")) {
                        currentItemContent.append(" ");
                    }
                    currentItemContent.append(line);
                } else {
                    // This is text before the list starts
                    html.insert(0, "<p>" + formatInlineElements(line) + "</p>\n");
                }
            }
        }

        // Don't forget to add the last item
        if (currentItemContent.length() > 0) {
            html.append("    <li>").append(formatInlineElements(currentItemContent.toString())).append("</li>\n");
        }

        html.append("</ol>\n\n");
        return html.toString();
    }

    private static String convertUnorderedList(String listText) {
        StringBuilder html = new StringBuilder("<ul>\n");

        // Split into lines
        String[] lines = NEWLINE_PATTERN.split(listText);
        StringBuilder currentItemContent = new StringBuilder();
        boolean hasStarted = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher matcher = UNORDERED_LIST_ITEM_PATTERN.matcher(line);

            if (matcher.find()) {
                // If we've already started a list item, output it before starting a new one
                if (hasStarted && currentItemContent.length() > 0) {
                    html.append("    <li>").append(formatInlineElements(currentItemContent.toString())).append("</li>\n");
                    currentItemContent = new StringBuilder();
                }

                // Start a new item
                hasStarted = true;
                currentItemContent.append(matcher.group(2));
            } else if (!line.isEmpty() && hasStarted) {
                // This is a continuation of the current list item
                if (!currentItemContent.toString().endsWith(" ")) {
                    currentItemContent.append(" ");
                }
                currentItemContent.append(line);
            }
        }

        // Don't forget to add the last item
        if (currentItemContent.length() > 0) {
            html.append("    <li>").append(formatInlineElements(currentItemContent.toString())).append("</li>\n");
        }

        html.append("</ul>\n\n");
        return html.toString();
    }

    private static String convertSection(String sectionText) {
        StringBuilder html = new StringBuilder();

        // Split into lines - limited to 2 parts (title line and rest)
        String[] lines = NEWLINE_LIMIT_PATTERN.split(sectionText, 2);

        // Extract the title and make it bold
        String firstLine = lines[0].trim();
        Matcher matcher = TITLE_PATTERN.matcher(firstLine);

        if (matcher.find()) {
            String title = matcher.group(1);
            String remainingText = matcher.group(3);

            html.append("<p><strong>").append(title).append(":</strong>");

            if (!remainingText.trim().isEmpty()) {
                html.append(" ").append(formatInlineElements(remainingText));
            }

            html.append("</p>\n");
        }

        // If there's more content after the title line
        if (lines.length > 1 && !lines[1].trim().isEmpty()) {
            String content = lines[1].trim();

            // Check if content is a list
            if (content.startsWith("*")) {
                html.append(convertUnorderedList(content));
            } else if (content.startsWith("1.") || ORDERED_LIST_ITEM_START_PATTERN.matcher(content).find()) {
                html.append(convertOrderedList(content));
            } else {
                html.append("<p>").append(formatInlineElements(content)).append("</p>\n\n");
            }
        }

        return html.toString();
    }

    private static String formatInlineElements(String text) {
        // Check if the text contains already processed code blocks
        if (text.contains("<pre><code")) {
            return text;
        }

        // Format inline titles like "Solution:" in bold
        String formatted = formatInlineTitles(text);

        // Format inline code blocks with backticks
        formatted = formatCodeBlocks(formatted);

        // Format rule references
        formatted = formatRuleReferences(formatted);

        return formatted;
    }

    private static String formatInlineTitles(String text) {
        Matcher matcher = INLINE_TITLE_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String title = matcher.group(1);
            String spacing = matcher.group(2);
            matcher.appendReplacement(result, "<strong>" + title + ":</strong>" + spacing);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String formatCodeBlocks(String text) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String codeContent = matcher.group(1);
            // Escape HTML entities in code
            codeContent = escapeHtml(codeContent);
            matcher.appendReplacement(result, "<code>" + codeContent + "</code>");
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String formatRuleReferences(String text) {
        Matcher matcher = RULE_REFERENCE_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String ruleName = matcher.group(1);
            matcher.appendReplacement(result, "<code>" + ruleName + "</code>");
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
