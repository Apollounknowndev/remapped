package dev.worldgen.remapped.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import dev.worldgen.remapped.duck.RemappedRendererAccess;
import dev.worldgen.remapped.map.RemappedState;
import dev.worldgen.remapped.map.RemappedUtils;
import dev.worldgen.remapped.render.RemappedMapRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Shadow
    @Final
    private MapRenderState field_53067 = new MapRenderState();

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(
        method = "renderFirstPersonMap",
        at = @At("TAIL")
    )
    private void remapped$renderRemappedMap(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int swingProgress, ItemStack stack, CallbackInfo ci, @Local(ordinal = 0) MapIdComponent id) {
        RemappedState state = RemappedUtils.getState(id, this.client.world);
        if (state != null) {
            RemappedMapRenderer mapRenderer = ((RemappedRendererAccess)this.client).remapped$getRenderer();
            mapRenderer.update(id, state, this.field_53067);
            mapRenderer.draw(this.field_53067, matrices, vertexConsumers, false, swingProgress);
        }

    }
}