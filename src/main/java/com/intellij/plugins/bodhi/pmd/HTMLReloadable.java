package com.intellij.plugins.bodhi.pmd;

public interface HTMLReloadable {
    void reloadHTML();

    /**
     * @return whether the splitter should be fitted to the height of the loaded HTML content
     */
    default boolean isFitSplitToContent() {
        return true;
    }

    /**
     * Called when a complete HTML page has been loaded, so its content can be replaced without reloading the page.
     */
    default void onHtmlLoaded() {
    }
}
