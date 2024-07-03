package dev.worldgen.remapped.render;

import dev.worldgen.remapped.map.RemappedState;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.texture.MapDecorationsAtlasManager;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.Objects;

public class RemappedMapRenderer implements AutoCloseable {
    final TextureManager textureManager;
    final MapDecorationsAtlasManager mapDecorationsAtlasManager;
    private final Int2ObjectMap<RemappedMapRenderer.MapTexture> mapTextures = new Int2ObjectOpenHashMap<>();

    public RemappedMapRenderer(TextureManager textureManager, MapDecorationsAtlasManager mapDecorationsAtlasManager) {
        this.textureManager = textureManager;
        this.mapDecorationsAtlasManager = mapDecorationsAtlasManager;
    }

    public void updateTexture(MapIdComponent id, RemappedState state) {
        this.getMapTexture(id, state).setNeedsUpdate();
    }

    public void draw(MatrixStack matrices, VertexConsumerProvider vertexConsumers, MapIdComponent id, RemappedState state, boolean hidePlayerIcons, int light) {
        this.getMapTexture(id, state).draw(matrices, vertexConsumers, hidePlayerIcons, light);
    }

    private RemappedMapRenderer.MapTexture getMapTexture(MapIdComponent id, RemappedState state) {
        return this.mapTextures.compute(id.id(), (id2, texture) -> {
            if (texture == null) {
                return new MapTexture(id2, state);
            } else {
                texture.setState(state);
                return texture;
            }
        });
    }

    public void clearStateTextures() {
        for (RemappedMapRenderer.MapTexture texture : this.mapTextures.values()) {
            texture.close();
        }

        this.mapTextures.clear();
    }

    public void close() {
        this.clearStateTextures();
    }

    @Environment(EnvType.CLIENT)
    private class MapTexture implements AutoCloseable {
        private RemappedState state;
        private final NativeImageBackedTexture texture;
        private final RenderLayer renderLayer;
        private boolean needsUpdate = true;

        MapTexture(final int id, final RemappedState state) {
            this.state = state;
            this.texture = new NativeImageBackedTexture(128, 128, true);
            Identifier identifier = RemappedMapRenderer.this.textureManager.registerDynamicTexture("map/" + id, this.texture);
            this.renderLayer = RenderLayer.getText(identifier);
        }

        void setState(RemappedState state) {
            boolean bl = this.state != state;
            this.state = state;
            this.needsUpdate |= bl;
        }

        public void setNeedsUpdate() {
            this.needsUpdate = true;
        }

        private void updateTexture() {
            for(int i = 0; i < 128; ++i) {
                for(int j = 0; j < 128; ++j) {
                    int k = j + i * 128;
                    this.texture.getImage().setColor(j, i, this.state.getPixelColor(k));
                }
            }

            this.texture.upload();
        }

        void draw(MatrixStack matrices, VertexConsumerProvider vertexConsumers, boolean hidePlayerIcons, int light) {
            if (this.needsUpdate) {
                this.updateTexture();
                this.needsUpdate = false;
            }

            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.renderLayer);
            vertexConsumer.vertex(matrix4f, 0.0F, 128.0F, -0.01F).color(Colors.WHITE).texture(0.0F, 1.0F).light(light);
            vertexConsumer.vertex(matrix4f, 128.0F, 128.0F, -0.01F).color(Colors.WHITE).texture(1.0F, 1.0F).light(light);
            vertexConsumer.vertex(matrix4f, 128.0F, 0.0F, -0.01F).color(Colors.WHITE).texture(1.0F, 0.0F).light(light);
            vertexConsumer.vertex(matrix4f, 0.0F, 0.0F, -0.01F).color(Colors.WHITE).texture(0.0F, 0.0F).light(light);
            int k = 0;

            for (MapDecoration mapDecoration : this.state.getDecorations()) {
                if (hidePlayerIcons && !mapDecoration.isAlwaysRendered()) {
                    continue;
                }

                matrices.push();
                matrices.translate(0.0F + mapDecoration.x() / 2.0F + 64.0F, 0.0F +mapDecoration.z() / 2.0F + 64.0F, -0.02F);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((mapDecoration.rotation() * 360) / 16.0F));
                matrices.scale(4.0F, 4.0F, 3.0F);
                matrices.translate(-0.125F, 0.125F, 0.0F);
                Matrix4f matrix4f2 = matrices.peek().getPositionMatrix();
                float g = -0.001F;
                Sprite sprite = RemappedMapRenderer.this.mapDecorationsAtlasManager.getSprite(mapDecoration);
                float h = sprite.getMinU();
                float l = sprite.getMinV();
                float m = sprite.getMaxU();
                float n = sprite.getMaxV();
                VertexConsumer vertexConsumer2 = vertexConsumers.getBuffer(RenderLayer.getText(sprite.getAtlasId()));
                vertexConsumer2.vertex(matrix4f2, -1.0F, 1.0F, (float)k * -0.001F).color(Colors.WHITE).texture(h, l).light(light);
                vertexConsumer2.vertex(matrix4f2, 1.0F, 1.0F, (float)k * -0.001F).color(Colors.WHITE).texture(m, l).light(light);
                vertexConsumer2.vertex(matrix4f2, 1.0F, -1.0F, (float)k * -0.001F).color(Colors.WHITE).texture(m, n).light(light);
                vertexConsumer2.vertex(matrix4f2, -1.0F, -1.0F, (float)k * -0.001F).color(Colors.WHITE).texture(h, n).light(light);
                matrices.pop();
                if (mapDecoration.name().isPresent()) {
                    TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                    Text text = mapDecoration.name().get();
                    float o = (float)textRenderer.getWidth(text);
                    float var10000 = 25.0F / o;
                    Objects.requireNonNull(textRenderer);
                    float p = MathHelper.clamp(var10000, 0.0F, 6.0F / 9.0F);
                    matrices.push();
                    matrices.translate(0.0F + (float)mapDecoration.x() / 2.0F + 64.0F - o * p / 2.0F, 0.0F + (float)mapDecoration.z() / 2.0F + 64.0F + 4.0F, -0.025F);
                    matrices.scale(p, p, 1.0F);
                    matrices.translate(0.0F, 0.0F, -0.1F);
                    textRenderer.draw(text, 0.0F, 0.0F, Colors.WHITE, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, Integer.MIN_VALUE, light);
                    matrices.pop();
                }

                ++k;
            }
        }

        public void close() {
            this.texture.close();
        }
    }
}

