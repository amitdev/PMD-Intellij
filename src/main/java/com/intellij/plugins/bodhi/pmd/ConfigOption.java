package com.intellij.plugins.bodhi.pmd;

/**
 * Configuration options enumeration. Separation between key for persistent state and description to show in the UI.
 */
public enum ConfigOption {
    TARGET_JDK("Target JDK", "Target JDK (max: 20-preview)", "20-preview"),
    STATISTICS_URL("Statistics URL", "Statistics URL to export usage anonymously", ""),
    THREADS("Threads", "Threads (fastest: " + PMDUtil.AVAILABLE_PROCESSORS + ")", String.valueOf(PMDUtil.AVAILABLE_PROCESSORS));

    /**
     * keys are used for persisting
     */
    //private static final List<String> keys;

    /**
     * description is used in the UI
     */
    //private static final List<String> descriptions;
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

    /*static {
        List<String> ks = new ArrayList<>();
        List<String> descs = new ArrayList<>();
        for (ConfigOption value : ConfigOption.values()) {
            ks.add(value.getKey());
            descs.add(value.getDescription());
        }
        keys = List.copyOf(ks);
        descriptions = List.copyOf(descs);
    }*/
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
    /*public static List<ConfigOption> asList() {
        return List.of(ConfigOption.values());
    }*/
    /*public static List<String> keys() {
        return keys;
    }*/
    /*public static List<String> descriptions() {
        return descriptions;
    }*/

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
