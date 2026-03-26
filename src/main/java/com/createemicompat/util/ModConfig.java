package com.createemicompat.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Simple config for the Create-EMI Stockkeeper compat mod.
 * Settings persist to a file in the Minecraft config directory.
 */
public class ModConfig {

    public static boolean showMissingItems = true;
    public static boolean showNeededQuantities = true;
    public static boolean colorHighlights = true;
    public static boolean altClickAutoOrder = true;

    // Transient UI state (not saved)
    public static boolean settingsOpen = false;

    private static Path configPath;

    public static void init(Path gameDir) {
        configPath = gameDir.resolve("config").resolve("createemicompat.properties");
        load();
    }

    public static void load() {
        if (configPath == null || !Files.exists(configPath)) return;
        try {
            Properties props = new Properties();
            props.load(Files.newBufferedReader(configPath));
            showMissingItems = Boolean.parseBoolean(props.getProperty("showMissingItems", "true"));
            showNeededQuantities = Boolean.parseBoolean(props.getProperty("showNeededQuantities", "true"));
            colorHighlights = Boolean.parseBoolean(props.getProperty("colorHighlights", "true"));
            altClickAutoOrder = Boolean.parseBoolean(props.getProperty("altClickAutoOrder", "true"));
        } catch (IOException ignored) {}
    }

    public static void save() {
        if (configPath == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            Properties props = new Properties();
            props.setProperty("showMissingItems", String.valueOf(showMissingItems));
            props.setProperty("showNeededQuantities", String.valueOf(showNeededQuantities));
            props.setProperty("colorHighlights", String.valueOf(colorHighlights));
            props.setProperty("altClickAutoOrder", String.valueOf(altClickAutoOrder));
            props.store(Files.newBufferedWriter(configPath), "Create-EMI Stockkeeper Compat Settings");
        } catch (IOException ignored) {}
    }

    /** Toggle options in order, for the settings panel. */
    public static final String[] LABELS = {
        "Show Missing Items",
        "Show Needed Quantities",
        "Color Highlights",
        "Alt+Click Auto-Order"
    };

    public static boolean get(int index) {
        return switch (index) {
            case 0 -> showMissingItems;
            case 1 -> showNeededQuantities;
            case 2 -> colorHighlights;
            case 3 -> altClickAutoOrder;
            default -> false;
        };
    }

    public static void toggle(int index) {
        switch (index) {
            case 0 -> showMissingItems = !showMissingItems;
            case 1 -> showNeededQuantities = !showNeededQuantities;
            case 2 -> colorHighlights = !colorHighlights;
            case 3 -> altClickAutoOrder = !altClickAutoOrder;
        }
        save();
    }
}
