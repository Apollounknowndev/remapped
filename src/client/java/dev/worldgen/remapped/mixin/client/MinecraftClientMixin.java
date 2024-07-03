package dev.worldgen.remapped.mixin.client;

import dev.worldgen.remapped.Remapped;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(
        method = "onDisconnected",
        at = @At("HEAD")
    )
    private void remapped$disableOnDisconnection(CallbackInfo ci) {
        Remapped.set(false);
    }
}
