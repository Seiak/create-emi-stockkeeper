package com.createemicompat.util;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;

import java.lang.reflect.Field;

/**
 * Reflection-based accessor for CategoryEntry.y to avoid mixin conflicts
 * with Create Factory Logistics which already has its own accessor.
 */
public class CategoryEntryHelper {

    private static Field yField;
    private static Field hiddenField;

    static {
        try {
            yField = StockKeeperRequestScreen.CategoryEntry.class.getDeclaredField("y");
            yField.setAccessible(true);
            hiddenField = StockKeeperRequestScreen.CategoryEntry.class.getDeclaredField("hidden");
            hiddenField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to find CategoryEntry field", e);
        }
    }

    public static int getY(StockKeeperRequestScreen.CategoryEntry entry) {
        try {
            return yField.getInt(entry);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setY(StockKeeperRequestScreen.CategoryEntry entry, int value) {
        try {
            yField.setInt(entry, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setHidden(StockKeeperRequestScreen.CategoryEntry entry, boolean value) {
        try {
            hiddenField.setBoolean(entry, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
