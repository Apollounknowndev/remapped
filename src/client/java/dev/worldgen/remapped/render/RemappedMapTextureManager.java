package dev.worldgen.remapped.render;

import dev.worldgen.remapped.map.RemappedState;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.MapColor;
import net.minecraft.client.texture.*;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

public class RemappedMapTextureManager implements AutoCloseable {
    private final Int2ObjectMap<MapTexture> texturesByMapId = new Int2ObjectOpenHashMap<>();
    final TextureManager textureManager;

    public RemappedMapTextureManager(TextureManager textureManager) {
        this.textureManager = textureManager;
    }

    public void setNeedsUpdate(MapIdComponent mapIdComponent, RemappedState mapState) {
        this.getMapTexture(mapIdComponent, mapState).setNeedsUpdate();
    }

    public Identifier getTextureId(MapIdComponent mapIdComponent, RemappedState mapState) {
        MapTexture mapTexture = this.getMapTexture(mapIdComponent, mapState);
        mapTexture.updateTexture();
        return mapTexture.textureId;
    }

    public void clear() {
        for (MapTexture mapTexture : this.texturesByMapId.values()) {
            mapTexture.close();
        }

        this.texturesByMapId.clear();
    }

    private MapTexture getMapTexture(MapIdComponent mapIdComponent, RemappedState MapState) {
        return this.texturesByMapId.compute(mapIdComponent.id(), (id, mapTexture) -> {
            if (mapTexture == null) {
                return new MapTexture(this, id, MapState);
            } else {
                mapTexture.setState(MapState);
                return mapTexture;
            }
        });
    }

    public void close() {
        this.clear();
    }

    @Environment(EnvType.CLIENT)
    class MapTexture implements AutoCloseable {
        private RemappedState state;
        private final NativeImageBackedTexture texture;
        private boolean needsUpdate = true;
        final Identifier textureId;

        MapTexture(final RemappedMapTextureManager mapTextureManager, final int id, final RemappedState state) {
            this.state = state;
            this.texture = new NativeImageBackedTexture(128, 128, true);
            this.textureId = mapTextureManager.textureManager.registerDynamicTexture("map/" + id, this.texture);
        }

        void setState(RemappedState state) {
            boolean bl = this.state != state;
            this.state = state;
            this.needsUpdate |= bl;
        }

        public void setNeedsUpdate() {
            this.needsUpdate = true;
        }

        void updateTexture() {
            if (this.needsUpdate) {
                NativeImage nativeImage = this.texture.getImage();
                if (nativeImage != null) {
                    for(int i = 0; i < 128; ++i) {
                        for(int j = 0; j < 128; ++j) {
                            int k = j + i * 128;
                            nativeImage.setColorArgb(j, i, ColorHelper.toAbgr(this.state.getPixelColor(k))); // TODO: Don't double flip red and blue
                        }
                    }
                }

                this.texture.upload();
                this.needsUpdate = false;
            }

        }

        public void close() {
            this.texture.close();
        }
    }
}

