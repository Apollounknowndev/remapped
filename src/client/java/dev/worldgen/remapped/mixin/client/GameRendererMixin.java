package dev.worldgen.remapped.mixin.client;

import dev.worldgen.remapped.duck.RemappedRendererAccess;
import dev.worldgen.remapped.render.RemappedMapRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin implements RemappedRendererAccess {
	@Unique
	public RemappedMapRenderer remappedMapRenderer;

	@Inject(at = @At("TAIL"), method = "<init>")
	private void remapped$createRenderer(MinecraftClient client, HeldItemRenderer heldItemRenderer, ResourceManager resourceManager, BufferBuilderStorage buffers, CallbackInfo ci) {
		this.remappedMapRenderer = new RemappedMapRenderer(client.getTextureManager(), client.getMapDecorationsAtlasManager());
	}

	@Inject(at = @At("TAIL"), method = "close")
	private void remapped$closeRenderer(CallbackInfo ci) {
		this.remappedMapRenderer.close();
	}

	@Override
	public RemappedMapRenderer remapped$getRenderer() {
		return this.remappedMapRenderer;
	}
}