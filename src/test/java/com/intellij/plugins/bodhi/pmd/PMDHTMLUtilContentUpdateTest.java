package com.intellij.plugins.bodhi.pmd;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.*;

public class PMDHTMLUtilContentUpdateTest {

    @Test
    public void replaceContentJsCarriesUtf8ContentAsBase64AndReportsHeight() {
        String html = "<p>A = 0 &lt; B &lt; 5 ≤ C 'quoted' \"double\" \\ backslash</p>";
        String js = PMDHTMLUtil.replaceContentJs(html);
        assertTrue(js, js.contains(Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8))));
        assertTrue(js, js.contains(".content-wrapper"));
        assertTrue(js, js.contains("TextDecoder"));
        assertTrue(js, js.contains("CONTENT_HEIGHT:"));
        assertFalse("raw content not embedded: " + js, js.contains("≤"));
    }

    @Test
    public void scrollbarHiddenDuringContentReplaceAndRestoredAfterwards() {
        String js = PMDHTMLUtil.replaceContentJs("<p>x</p>");
        int hideAt = js.indexOf("document.body.style.overflowY = 'hidden'");
        assertTrue("scrollbar hidden before the content is replaced: " + js, hideAt >= 0 && hideAt < js.indexOf("innerHTML"));
        assertTrue(PMDHTMLUtil.RESTORE_SCROLLBAR_JS.contains("document.body.style.overflowY = ''"));
    }

    @Test
    public void splitterGrowsWhenContentDoesNotFit() {
        assertTrue(PMDHTMLUtil.shouldMoveSplitter(0.50f, 0.52f, 400));   // needs 8 px more
        assertFalse(PMDHTMLUtil.shouldMoveSplitter(0.50f, 0.505f, 400)); // needs 2 px more: negligible
    }

    @Test
    public void splitterShrinksOnlyWhenMuchSpaceIsLeft() {
        assertFalse(PMDHTMLUtil.shouldMoveSplitter(0.60f, 0.50f, 400)); // 10% empty: keep, avoid resizing
        assertTrue(PMDHTMLUtil.shouldMoveSplitter(0.66f, 0.50f, 400));  // 16% empty: shrink
    }
}
