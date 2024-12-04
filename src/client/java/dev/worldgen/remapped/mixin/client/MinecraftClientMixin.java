package dev.worldgen.remapped.mixin.client;

import dev.worldgen.remapped.Remapped;
import dev.worldgen.remapped.duck.RemappedRendererAccess;
import dev.worldgen.remapped.render.RemappedMapRenderer;
import dev.worldgen.remapped.render.RemappedMapTextureManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.texture.MapDecorationsAtlasManager;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin implements RemappedRendererAccess {
    @Shadow @Final private TextureManager textureManager;
    @Shadow @Final private MapDecorationsAtlasManager mapDecorationsAtlasManager;
    @Unique
    public RemappedMapRenderer remappedMapRenderer;
    @Unique
    public RemappedMapTextureManager remappedMapTextureManager;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void remapped$createRenderer(RunArgs args, CallbackInfo ci) {
        this.remappedMapTextureManager = new RemappedMapTextureManager(this.textureManager);
        this.remappedMapRenderer = new RemappedMapRenderer(this.mapDecorationsAtlasManager, this.remappedMapTextureManager);
    }

    @Override
    public RemappedMapRenderer remapped$getRenderer() {
        return this.remappedMapRenderer;
    }
    @Inject(
        method = "onDisconnected",
        at = @At("HEAD")
    )
    private void remapped$disableOnDisconnection(CallbackInfo ci) {
        Remapped.set(false);
    }
}
