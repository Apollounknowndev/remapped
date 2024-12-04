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
public class GameRendererMixin {

	@Inject(at = @At("TAIL"), method = "reset")
	private void remapped$clearRenderer(CallbackInfo ci) {
		((RemappedRendererAccess)((GameRenderer)(Object)this).getClient()).remapped$getRenderer().clear();
	}
}