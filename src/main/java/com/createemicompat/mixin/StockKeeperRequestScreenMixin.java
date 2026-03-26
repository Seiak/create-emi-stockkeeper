package com.createemicompat.mixin;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen.CategoryEntry;
import com.createemicompat.util.CategoryEntryHelper;
import com.createemicompat.util.ModConfig;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiFavorite;
import dev.emi.emi.runtime.EmiFavorites;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(value = StockKeeperRequestScreen.class, remap = false)
public class StockKeeperRequestScreenMixin {

    @Shadow public List<List<BigItemStack>> displayedItems;
    @Shadow public List<CategoryEntry> categories;
    @Shadow public List<BigItemStack> itemsToOrder;

    // --- Cache ---
    @Unique private static Map<Item, List<long[]>> createemicompat$cache = new HashMap<>();
    @Unique private static Map<Item, List<ItemStack>> createemicompat$cacheStacks = new HashMap<>();
    @Unique private static int createemicompat$lastSyntheticsSize = -1;
    @Unique private static int createemicompat$lastSyntheticsHash = 0;

    @Unique private static final int createemicompat$ROW_HEIGHT = 20;
    @Unique private static final int createemicompat$COLS = 9;
    @Unique private static final int createemicompat$PSEUDO_CATEGORY_ID = -2;

    // Colors
    @Unique private static final int createemicompat$GREEN = 0x7700CC00;
    @Unique private static final int createemicompat$YELLOW = 0x77CCAA00;
    @Unique private static final int createemicompat$RED = 0x77CC0000;

    // Settings panel dimensions
    @Unique private static final int createemicompat$PANEL_WIDTH = 130;
    @Unique private static final int createemicompat$PANEL_ENTRY_HEIGHT = 14;
    @Unique private static final int createemicompat$GEAR_SIZE = 10;

    // Gear icon position (set during rendering, used for click detection)
    @Unique private int createemicompat$gearX = 0;
    @Unique private int createemicompat$gearY = 0;
    @Unique private int createemicompat$panelX = 0;
    @Unique private int createemicompat$panelY = 0;
    @Unique private boolean createemicompat$hasCategory = false;

    // Reflection
    @Unique private static Field createemicompat$hiddenCategoriesField;
    @Unique private static Field createemicompat$itemsXField;
    @Unique private static Field createemicompat$itemsYField;
    @Unique private static Method createemicompat$getHoveredSlotMethod;
    @Unique private static Method createemicompat$getOrderForItemMethod;

    static {
        try {
            createemicompat$hiddenCategoriesField =
                StockKeeperRequestScreen.class.getDeclaredField("hiddenCategories");
            createemicompat$hiddenCategoriesField.setAccessible(true);
        } catch (NoSuchFieldException ignored) {}
        try {
            createemicompat$itemsXField =
                StockKeeperRequestScreen.class.getDeclaredField("itemsX");
            createemicompat$itemsXField.setAccessible(true);
        } catch (NoSuchFieldException ignored) {}
        try {
            createemicompat$itemsYField =
                StockKeeperRequestScreen.class.getDeclaredField("itemsY");
            createemicompat$itemsYField.setAccessible(true);
        } catch (NoSuchFieldException ignored) {}
        try {
            createemicompat$getHoveredSlotMethod =
                StockKeeperRequestScreen.class.getDeclaredMethod("getHoveredSlot", int.class, int.class);
            createemicompat$getHoveredSlotMethod.setAccessible(true);
        } catch (NoSuchMethodException ignored) {}
        try {
            createemicompat$getOrderForItemMethod =
                StockKeeperRequestScreen.class.getDeclaredMethod("getOrderForItem", ItemStack.class);
            createemicompat$getOrderForItemMethod.setAccessible(true);
        } catch (NoSuchMethodException ignored) {}
    }

    // =============================================
    // CATEGORY INSERTION
    // =============================================

    @Inject(method = "refreshSearchResults", at = @At("TAIL"))
    private void createemicompat$insertRecipeCategory(boolean scrollBackUp, CallbackInfo ci) {
        createemicompat$hasCategory = false;

        // Safety: ensure lists are in sync before we touch them
        if (displayedItems == null || categories == null) return;
        if (displayedItems.size() != categories.size()) return;

        List<EmiFavorite.Synthetic> synthetics = EmiFavorites.syntheticFavorites;
        if (synthetics == null || synthetics.isEmpty()) return;
        if (displayedItems.isEmpty()) return;

        int size = synthetics.size();
        int hash = System.identityHashCode(synthetics);
        if (size != createemicompat$lastSyntheticsSize || hash != createemicompat$lastSyntheticsHash) {
            createemicompat$rebuildCache(synthetics);
            createemicompat$lastSyntheticsSize = size;
            createemicompat$lastSyntheticsHash = hash;
        }

        Set<String> foundKeys = new HashSet<>();
        List<BigItemStack> recipeItems = new ArrayList<>();

        for (List<BigItemStack> category : displayedItems) {
            if (category == null) continue;
            for (BigItemStack bis : category) {
                if (createemicompat$isInCache(bis.stack)) {
                    // Create a new BigItemStack referencing the same stack but independent count
                    recipeItems.add(new BigItemStack(bis.stack, bis.count));
                    foundKeys.add(createemicompat$itemKey(bis.stack));
                }
            }
        }

        // Add missing items if enabled
        if (ModConfig.showMissingItems) {
            for (Map.Entry<Item, List<ItemStack>> entry : createemicompat$cacheStacks.entrySet()) {
                for (ItemStack cachedStack : entry.getValue()) {
                    if (cachedStack.isDamageableItem()) continue;
                    String key = createemicompat$itemKey(cachedStack);
                    if (!foundKeys.contains(key)) {
                        ItemStack displayStack = cachedStack.copy();
                        displayStack.setCount(1);
                        recipeItems.add(new BigItemStack(displayStack, 0));
                        foundKeys.add(key);
                    }
                }
            }
        }

        if (recipeItems.isEmpty()) return;

        boolean isHidden = false;
        try {
            if (createemicompat$hiddenCategoriesField != null) {
                @SuppressWarnings("unchecked")
                Set<Integer> hiddenCategories =
                    (Set<Integer>) createemicompat$hiddenCategoriesField.get(this);
                isHidden = hiddenCategories.contains(createemicompat$PSEUDO_CATEGORY_ID);
            }
        } catch (IllegalAccessException ignored) {}

        int newCategoryHeight = createemicompat$ROW_HEIGHT;
        if (!isHidden) {
            int itemRows = (recipeItems.size() + createemicompat$COLS - 1) / createemicompat$COLS;
            newCategoryHeight += itemRows * createemicompat$ROW_HEIGHT;
        }

        for (CategoryEntry entry : categories) {
            CategoryEntryHelper.setY(entry, CategoryEntryHelper.getY(entry) + newCategoryHeight);
        }

        CategoryEntry recipeCategory = new CategoryEntry(
            createemicompat$PSEUDO_CATEGORY_ID, "Recipe Ingredients", 0
        );
        CategoryEntryHelper.setHidden(recipeCategory, isHidden);
        categories.add(0, recipeCategory);
        displayedItems.add(0, recipeItems);
        createemicompat$hasCategory = true;
    }

    // =============================================
    // GEAR ICON + SETTINGS PANEL RENDERING
    // =============================================

    @Inject(method = "m_7286_", at = @At("HEAD"))
    private void createemicompat$ensureListSync(GuiGraphics graphics, float partialTick,
                                                 int mouseX, int mouseY, CallbackInfo ci) {
        // Safety: if lists got out of sync, remove our injected category
        if (createemicompat$hasCategory && displayedItems != null && categories != null
                && displayedItems.size() != categories.size()) {
            if (!displayedItems.isEmpty()) displayedItems.remove(0);
            if (!categories.isEmpty()) categories.remove(0);
            createemicompat$hasCategory = false;
        }
    }

    @Inject(method = "m_7286_", at = @At("TAIL"))
    private void createemicompat$renderGearAndPanel(GuiGraphics graphics, float partialTick,
                                                     int mouseX, int mouseY, CallbackInfo ci) {
        if (!createemicompat$hasCategory) return;

        try {
            int itemsX = createemicompat$itemsXField != null ?
                createemicompat$itemsXField.getInt(this) : 0;
            int itemsY = createemicompat$itemsYField != null ?
                createemicompat$itemsYField.getInt(this) : 0;

            Font font = Minecraft.getInstance().font;
            int headerTextWidth = font.width("Recipe Ingredients");

            // Gear icon position: to the right of the header text, vertically centered
            createemicompat$gearX = itemsX + 14 + headerTextWidth + 4;
            createemicompat$gearY = itemsY + 8;

            // Draw gear icon (⚙ character)
            String gear = "\u2699";
            boolean hoveringGear = mouseX >= createemicompat$gearX
                && mouseX < createemicompat$gearX + createemicompat$GEAR_SIZE
                && mouseY >= createemicompat$gearY
                && mouseY < createemicompat$gearY + createemicompat$GEAR_SIZE;
            int gearColor = hoveringGear ? 0xFFFFFF00 : 0xFFC0C0C0;

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);
            graphics.drawString(font, gear, createemicompat$gearX, createemicompat$gearY, gearColor, true);

            // Settings panel
            if (ModConfig.settingsOpen) {
                createemicompat$panelX = createemicompat$gearX - createemicompat$PANEL_WIDTH + createemicompat$GEAR_SIZE;
                createemicompat$panelY = createemicompat$gearY + createemicompat$GEAR_SIZE + 2;

                int panelHeight = ModConfig.LABELS.length * createemicompat$PANEL_ENTRY_HEIGHT + 6;

                // Panel background
                graphics.fill(createemicompat$panelX - 2, createemicompat$panelY - 2,
                    createemicompat$panelX + createemicompat$PANEL_WIDTH + 2,
                    createemicompat$panelY + panelHeight + 2,
                    0xFF1A1A2E); // dark blue-gray border
                graphics.fill(createemicompat$panelX, createemicompat$panelY,
                    createemicompat$panelX + createemicompat$PANEL_WIDTH,
                    createemicompat$panelY + panelHeight,
                    0xFF2D2D44); // panel background

                // Draw toggle entries
                for (int i = 0; i < ModConfig.LABELS.length; i++) {
                    int entryY = createemicompat$panelY + 3 + i * createemicompat$PANEL_ENTRY_HEIGHT;
                    boolean enabled = ModConfig.get(i);
                    boolean hoveringEntry = mouseX >= createemicompat$panelX
                        && mouseX < createemicompat$panelX + createemicompat$PANEL_WIDTH
                        && mouseY >= entryY
                        && mouseY < entryY + createemicompat$PANEL_ENTRY_HEIGHT;

                    // Checkbox
                    String checkbox = enabled ? "\u2611" : "\u2610"; // ☑ or ☐
                    int checkColor = enabled ? 0xFF55FF55 : 0xFFAA5555;
                    graphics.drawString(font, checkbox, createemicompat$panelX + 3, entryY, checkColor, true);

                    // Label
                    int labelColor = hoveringEntry ? 0xFFFFFFFF : 0xFFC0C0C0;
                    graphics.drawString(font, ModConfig.LABELS[i], createemicompat$panelX + 15, entryY, labelColor, true);
                }
            }
            graphics.pose().popPose();
        } catch (IllegalAccessException ignored) {}
    }

    // =============================================
    // CLICK HANDLING (gear, panel, alt+click)
    // =============================================

    @Inject(method = "m_6375_", at = @At("HEAD"), cancellable = true)
    private void createemicompat$handleClicks(double mouseX, double mouseY, int button,
                                               CallbackInfoReturnable<Boolean> cir) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        // Check gear icon click
        if (button == 0 && createemicompat$hasCategory) {
            if (mx >= createemicompat$gearX && mx < createemicompat$gearX + createemicompat$GEAR_SIZE
                && my >= createemicompat$gearY && my < createemicompat$gearY + createemicompat$GEAR_SIZE) {
                ModConfig.settingsOpen = !ModConfig.settingsOpen;
                cir.setReturnValue(true);
                return;
            }
        }

        // Check settings panel clicks
        if (button == 0 && ModConfig.settingsOpen) {
            int panelHeight = ModConfig.LABELS.length * createemicompat$PANEL_ENTRY_HEIGHT + 6;
            if (mx >= createemicompat$panelX && mx < createemicompat$panelX + createemicompat$PANEL_WIDTH
                && my >= createemicompat$panelY && my < createemicompat$panelY + panelHeight) {
                int relY = my - createemicompat$panelY - 3;
                int index = relY / createemicompat$PANEL_ENTRY_HEIGHT;
                if (index >= 0 && index < ModConfig.LABELS.length) {
                    ModConfig.toggle(index);
                }
                cir.setReturnValue(true);
                return;
            }
            // Click outside panel closes it
            ModConfig.settingsOpen = false;
        }

        // Alt+Click auto-order
        if (button == 0 && Screen.hasAltDown() && ModConfig.altClickAutoOrder) {
            createemicompat$doAltClickOrder(mx, my, cir);
        }
    }

    @Unique
    private void createemicompat$doAltClickOrder(int mx, int my, CallbackInfoReturnable<Boolean> cir) {
        if (createemicompat$getHoveredSlotMethod == null || createemicompat$getOrderForItemMethod == null) return;

        List<EmiFavorite.Synthetic> synthetics = EmiFavorites.syntheticFavorites;
        if (synthetics == null || synthetics.isEmpty()) return;

        try {
            Object hoveredSlot = createemicompat$getHoveredSlotMethod.invoke(this, mx, my);
            Method getFirst = hoveredSlot.getClass().getMethod("getFirst");
            Method getSecond = hoveredSlot.getClass().getMethod("getSecond");
            int categoryIdx = (Integer) getFirst.invoke(hoveredSlot);
            int slotIdx = (Integer) getSecond.invoke(hoveredSlot);

            if (categoryIdx < 0 || categoryIdx >= displayedItems.size()) return;
            List<BigItemStack> category = displayedItems.get(categoryIdx);
            if (slotIdx < 0 || slotIdx >= category.size()) return;

            BigItemStack entry = category.get(slotIdx);
            ItemStack entryStack = entry.stack;

            long needed = createemicompat$getNeeded(entryStack);
            if (needed < 0) return;

            long alreadyOrdered = createemicompat$getOrderedCount(entryStack);
            long remaining = Math.max(0, needed - alreadyOrdered);
            if (remaining <= 0) { cir.setReturnValue(true); return; }

            int toOrder = (int) Math.min(remaining, entry.count);
            if (toOrder <= 0) { cir.setReturnValue(true); return; }

            BigItemStack existingOrder = (BigItemStack) createemicompat$getOrderForItemMethod
                .invoke(this, entryStack);

            if (existingOrder == null) {
                if (itemsToOrder.size() >= createemicompat$COLS) { cir.setReturnValue(true); return; }
                existingOrder = new BigItemStack(entryStack.copyWithCount(1), 0);
                itemsToOrder.add(existingOrder);
            }

            existingOrder.count = existingOrder.count + toOrder;
            if (existingOrder.count > entry.count) existingOrder.count = entry.count;

            cir.setReturnValue(true);
        } catch (Exception ignored) {}
    }

    // =============================================
    // ITEM HIGHLIGHTS + QUANTITY OVERLAY
    // =============================================

    @Inject(method = "renderItemEntry", at = @At("TAIL"))
    private void createemicompat$highlightSyntheticFavorites(
            GuiGraphics graphics, float scale, BigItemStack entry,
            boolean isStackHovered, boolean isRenderingOrders, CallbackInfo ci) {

        if (isRenderingOrders) return;

        List<EmiFavorite.Synthetic> synthetics = EmiFavorites.syntheticFavorites;
        if (synthetics == null || synthetics.isEmpty()) {
            createemicompat$lastSyntheticsSize = 0;
            return;
        }

        ItemStack entryStack = entry.stack;
        if (entryStack.isEmpty()) return;

        int size = synthetics.size();
        int hash = System.identityHashCode(synthetics);
        if (size != createemicompat$lastSyntheticsSize || hash != createemicompat$lastSyntheticsHash) {
            createemicompat$rebuildCache(synthetics);
            createemicompat$lastSyntheticsSize = size;
            createemicompat$lastSyntheticsHash = hash;
        }

        long needed = createemicompat$getNeeded(entryStack);
        if (needed < 0) return;

        long ordered = createemicompat$getOrderedCount(entryStack);
        long remaining = Math.max(0, needed - ordered);
        int inStock = entry.count;

        // Render missing item icons (Create skips rendering when count == 0)
        if (inStock == 0) {
            graphics.pose().pushPose();
            graphics.pose().translate(1, 1, 232);
            graphics.renderItem(entryStack, 0, 0);
            graphics.pose().popPose();
        }

        // Color highlights
        if (ModConfig.colorHighlights) {
            int highlightColor;
            if (remaining <= 0 || inStock >= remaining) {
                highlightColor = createemicompat$GREEN;
            } else if (inStock > 0) {
                highlightColor = createemicompat$YELLOW;
            } else {
                highlightColor = createemicompat$RED;
            }
            graphics.fill(0, 0, 18, 18, highlightColor);
        }

        // Needed quantity overlay
        if (ModConfig.showNeededQuantities && remaining > 0) {
            Font font = Minecraft.getInstance().font;
            String neededStr = "x" + createemicompat$formatCount(remaining);

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, inStock == 0 ? 500 : 300);
            float textScale = 0.65f;
            graphics.pose().scale(textScale, textScale, 1.0f);

            int shadowColor = 0xFF4A2D31;
            int textColor = 0xFFF8F8EC;
            graphics.drawString(font, neededStr, 2, 2, shadowColor, false);
            graphics.drawString(font, neededStr, 1, 1, textColor, false);

            graphics.pose().popPose();
        }
    }

    // =============================================
    // CACHE + HELPERS
    // =============================================

    @Unique
    private static void createemicompat$rebuildCache(List<EmiFavorite.Synthetic> synthetics) {
        createemicompat$cache.clear();
        createemicompat$cacheStacks.clear();
        for (EmiFavorite.Synthetic synthetic : synthetics) {
            long needed = synthetic.amount;
            for (EmiStack emiStack : synthetic.getEmiStacks()) {
                ItemStack stack = emiStack.getItemStack();
                if (!stack.isEmpty()) {
                    createemicompat$cacheStacks
                        .computeIfAbsent(stack.getItem(), k -> new ArrayList<>())
                        .add(stack);
                    createemicompat$cache
                        .computeIfAbsent(stack.getItem(), k -> new ArrayList<>())
                        .add(new long[]{needed});
                }
            }
        }
    }

    @Unique
    private static boolean createemicompat$isInCache(ItemStack target) {
        return createemicompat$getNeeded(target) >= 0;
    }

    @Unique
    private static long createemicompat$getNeeded(ItemStack target) {
        List<ItemStack> stacks = createemicompat$cacheStacks.get(target.getItem());
        if (stacks == null) return -1;
        List<long[]> amounts = createemicompat$cache.get(target.getItem());
        for (int i = 0; i < stacks.size(); i++) {
            if (ItemStack.isSameItemSameTags(target, stacks.get(i))) {
                return amounts.get(i)[0];
            }
        }
        return -1;
    }

    @Unique
    private long createemicompat$getOrderedCount(ItemStack target) {
        if (itemsToOrder == null || itemsToOrder.isEmpty()) return 0;
        long total = 0;
        for (BigItemStack order : itemsToOrder) {
            if (ItemStack.isSameItemSameTags(target, order.stack)) {
                total += order.count;
            }
        }
        return total;
    }

    @Unique
    private static String createemicompat$itemKey(ItemStack stack) {
        String key = stack.getItem().toString();
        if (stack.hasTag()) key += stack.getTag().toString();
        return key;
    }

    @Unique
    private static String createemicompat$formatCount(long count) {
        if (count >= 1000000) return String.format("%.1fm", count / 1000000.0);
        if (count >= 1000) return String.format("%.1fk", count / 1000.0);
        return String.valueOf(count);
    }
}
