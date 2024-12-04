package dev.worldgen.remapped.render;

import dev.worldgen.remapped.map.RemappedState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.Objects;

public class RemappedMapRenderer {
    private final RemappedMapTextureManager textureManager;
    private final MapDecorationsAtlasManager decorationsAtlasManager;

    public RemappedMapRenderer(MapDecorationsAtlasManager decorationsAtlasManager, RemappedMapTextureManager textureManager) {
        this.decorationsAtlasManager = decorationsAtlasManager;
        this.textureManager = textureManager;
    }

    public void draw(MapRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, boolean hidePlayerIcons, int light) {
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getText(state.texture));
        vertexConsumer.vertex(matrix4f, 0.0F, 128.0F, -0.01F).color(-1).texture(0.0F, 1.0F).light(light);
        vertexConsumer.vertex(matrix4f, 128.0F, 128.0F, -0.01F).color(-1).texture(1.0F, 1.0F).light(light);
        vertexConsumer.vertex(matrix4f, 128.0F, 0.0F, -0.01F).color(-1).texture(1.0F, 0.0F).light(light);
        vertexConsumer.vertex(matrix4f, 0.0F, 0.0F, -0.01F).color(-1).texture(0.0F, 0.0F).light(light);
        int i = 0;

        for (MapRenderState.Decoration decoration : state.decorations) {
            if (hidePlayerIcons && !decoration.alwaysRendered) {
                continue;
            }

            matrices.push();
            matrices.translate(decoration.x / 2.0F + 64.0F, decoration.z / 2.0F + 64.0F, -0.02F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(decoration.rotation * 360 / 16.0F));
            matrices.scale(4.0F, 4.0F, 3.0F);
            matrices.translate(-0.125F, 0.125F, 0.0F);
            Matrix4f matrix4f2 = matrices.peek().getPositionMatrix();
            Sprite sprite = decoration.sprite;
            if (sprite != null) {
                VertexConsumer vertexConsumer2 = vertexConsumers.getBuffer(RenderLayer.getText(sprite.getAtlasId()));
                vertexConsumer2.vertex(matrix4f2, -1.0F, 1.0F, (float)i * -0.001F).color(-1).texture(sprite.getMinU(), sprite.getMinV()).light(light);
                vertexConsumer2.vertex(matrix4f2, 1.0F, 1.0F, (float)i * -0.001F).color(-1).texture(sprite.getMaxU(), sprite.getMinV()).light(light);
                vertexConsumer2.vertex(matrix4f2, 1.0F, -1.0F, (float)i * -0.001F).color(-1).texture(sprite.getMaxU(), sprite.getMaxV()).light(light);
                vertexConsumer2.vertex(matrix4f2, -1.0F, -1.0F, (float)i * -0.001F).color(-1).texture(sprite.getMinU(), sprite.getMaxV()).light(light);
                matrices.pop();
            }

            if (decoration.name != null) {
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                float f = (float)textRenderer.getWidth(decoration.name);
                float var10000 = 25.0F / f;
                Objects.requireNonNull(textRenderer);
                float g = MathHelper.clamp(var10000, 0.0F, 6.0F / 9.0F);
                matrices.push();
                matrices.translate((float)decoration.x / 2.0F + 64.0F - f * g / 2.0F, (float)decoration.z / 2.0F + 64.0F + 4.0F, -0.025F);
                matrices.scale(g, g, 1.0F);
                matrices.translate(0.0F, 0.0F, -0.1F);
                textRenderer.draw(decoration.name, 0.0F, 0.0F, -1, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, Integer.MIN_VALUE, light, false);
                matrices.pop();
            }

            ++i;
        }
    }

    public void update(MapIdComponent mapId, RemappedState mapState, MapRenderState renderState) {
        renderState.texture = this.textureManager.getTextureId(mapId, mapState);
        renderState.decorations.clear();

        for (MapDecoration mapDecoration : mapState.getDecorations()) {
            renderState.decorations.add(this.createDecoration(mapDecoration));
        }

    }

    private MapRenderState.Decoration createDecoration(MapDecoration decoration) {
        MapRenderState.Decoration decoration2 = new MapRenderState.Decoration();
        decoration2.sprite = this.decorationsAtlasManager.getSprite(decoration);
        decoration2.x = decoration.x();
        decoration2.z = decoration.z();
        decoration2.rotation = decoration.rotation();
        decoration2.name = decoration.name().orElse(null);
        decoration2.alwaysRendered = decoration.isAlwaysRendered();
        return decoration2;
    }

    public RemappedMapTextureManager getTextureManager() {
        return this.textureManager;
    }

    public void clear() {
        this.textureManager.clear();
    }
}

