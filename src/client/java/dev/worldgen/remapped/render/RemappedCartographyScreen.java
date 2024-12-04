package dev.worldgen.remapped.render;

import dev.worldgen.remapped.duck.RemappedRendererAccess;
import dev.worldgen.remapped.map.RemappedState;
import dev.worldgen.remapped.map.RemappedUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class RemappedCartographyScreen {
    private static final Identifier ERROR_TEXTURE = Identifier.ofVanilla("container/cartography_table/error");
    private static final Identifier SCALED_MAP_TEXTURE = Identifier.ofVanilla("container/cartography_table/scaled_map");
    private static final Identifier DUPLICATED_MAP_TEXTURE = Identifier.ofVanilla("container/cartography_table/duplicated_map");
    private static final Identifier MAP_TEXTURE = Identifier.ofVanilla("container/cartography_table/map");
    private static final Identifier LOCKED_TEXTURE = Identifier.ofVanilla("container/cartography_table/locked");

    public static void render(DrawContext context, int x, int y, MinecraftClient client, MapRenderState mapRenderState, ItemStack firstSlot, ItemStack secondSlot) {
        boolean clone = secondSlot.isOf(Items.MAP);
        boolean zoom = secondSlot.isOf(Items.PAPER);
        boolean lock = secondSlot.isOf(Items.GLASS_PANE);
        MapIdComponent id = firstSlot.get(DataComponentTypes.MAP_ID);
        boolean cannotExpand = false;
        RemappedState state;
        if (id != null) {
            state = RemappedUtils.getState(id, client.world);
            if (state != null) {
                if (state.locked()) {
                    cannotExpand = true;
                    if (zoom || lock) {
                        context.drawGuiTexture(RenderLayer::getGuiTextured, ERROR_TEXTURE, x + 35, y + 31, 28, 21);
                    }
                }

                if (zoom && state.scale() >= 4) {
                    cannotExpand = true;
                    context.drawGuiTexture(RenderLayer::getGuiTextured, ERROR_TEXTURE, x + 35, y + 31, 28, 21);
                }
            }
        } else {
            state = null;
        }

        drawMap(context, x, y, client, mapRenderState, id, state, clone, zoom, lock, cannotExpand);
    }

    private static void drawMap(DrawContext context, int x, int y, MinecraftClient client, MapRenderState mapRenderState, @Nullable MapIdComponent id, @Nullable RemappedState state, boolean clone, boolean zoom, boolean lock, boolean cannotExpand) {
        if (zoom && !cannotExpand) {
            context.drawGuiTexture(RenderLayer::getGuiTextured, SCALED_MAP_TEXTURE, x + 67, y + 13, 66, 66);
            drawMap(context, client, mapRenderState, id, state, x + 85, y + 31, 0.226F);
        } else if (clone) {
            context.drawGuiTexture(RenderLayer::getGuiTextured, DUPLICATED_MAP_TEXTURE, x + 67 + 16, y + 13, 50, 66);
            drawMap(context, client, mapRenderState, id, state, x + 86, y + 16, 0.34F);
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, 0.0F, 1.0F);
            context.drawGuiTexture(RenderLayer::getGuiTextured, DUPLICATED_MAP_TEXTURE, x + 67, y + 13 + 16, 50, 66);
            drawMap(context, client, mapRenderState, id, state, x + 70, y + 32, 0.34F);
            context.getMatrices().pop();
        } else if (lock) {
            context.drawGuiTexture(RenderLayer::getGuiTextured, MAP_TEXTURE, x + 67, y + 13, 66, 66);
            drawMap(context, client, mapRenderState, id, state, x + 71, y + 17, 0.45F);
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, 0.0F, 1.0F);
            context.drawGuiTexture(RenderLayer::getGuiTextured, LOCKED_TEXTURE, x + 118, y + 60, 10, 14);
            context.getMatrices().pop();
        } else {
            context.drawGuiTexture(RenderLayer::getGuiTextured, MAP_TEXTURE, x + 67, y + 13, 66, 66);
            drawMap(context, client, mapRenderState, id, state, x + 71, y + 17, 0.45F);
        }

    }

    private static void drawMap(DrawContext context, MinecraftClient client, MapRenderState mapRenderState, @Nullable MapIdComponent id, @Nullable RemappedState state, int x, int y, float scale) {
        if (id != null && state != null) {
            context.getMatrices().push();
            context.getMatrices().translate((float)x, (float)y, 1.0F);
            context.getMatrices().scale(scale, scale, 1.0F);
            RemappedMapRenderer mapRenderer = ((RemappedRendererAccess)client).remapped$getRenderer();
            mapRenderer.update(id, state, mapRenderState);
            context.draw((vertexConsumers) -> {
                mapRenderer.draw(mapRenderState, context.getMatrices(), vertexConsumers, true, 15728880);
            });
            context.getMatrices().pop();
        }
    }
}
