package com.intellij.plugins.bodhi.pmd;

import net.sourceforge.pmd.lang.LanguageRegistry;

/**
 * Configuration options enumeration. Separation between key for persistent state and description to show in the UI.
 */
public enum ConfigOption {
    TARGET_JDK("Target JDK", "Target JDK (max: " + latestSupportJavaVersionByPmd() + ")", latestSupportJavaVersionByPmd()),
    STATISTICS_URL("Statistics URL", "Statistics URL to export usage anonymously", ""),
    THREADS("Threads", "Threads (fastest: " + PMDUtil.AVAILABLE_PROCESSORS + ")", String.valueOf(PMDUtil.AVAILABLE_PROCESSORS));

    /**
     * key is used for persisting
     */
    private final String key;

    /**
     * description is used in the UI
     */
    private final String description;

    /**
     * defaultValue is used in the UI if no value is provided yet by the user
     */
    private final String defaultValue;

    private static String latestSupportJavaVersionByPmd() {
        return LanguageRegistry.PMD.getLanguageById("java").getLatestVersion().toString();
    }

    public static ConfigOption fromKey(String key) {
        for (ConfigOption option : ConfigOption.values()) {
            if (option.getKey().equals(key)) {
                return option;
            }
        }
        throw new IllegalArgumentException("Unknown config option key: " + key);
    }
    public static ConfigOption fromDescription(String desc) {
        for (ConfigOption option : ConfigOption.values()) {
            if (option.getDescription().equals(desc)) {
                return option;
            }
        }
        throw new IllegalArgumentException("Unknown config option description: " + desc);
    }
    public static int size() {
        return ConfigOption.values().length;
    }

    ConfigOption(String key, String description, String defaultValue) {
        this.key = key;
        this.description = description;
        this.defaultValue = defaultValue;
    }
    public String getKey() {
        return key;
    }
    public String getDescription() {
        return description;
    }
    public String getDefaultValue() {
        return defaultValue;
    }
}
