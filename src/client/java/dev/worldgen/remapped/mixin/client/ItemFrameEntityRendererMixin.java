package dev.worldgen.remapped.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import dev.worldgen.remapped.duck.RemappedRendererAccess;
import dev.worldgen.remapped.map.RemappedState;
import dev.worldgen.remapped.render.RemappedMapRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.render.entity.state.ItemFrameEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameEntityRenderer.class)
public abstract class ItemFrameEntityRendererMixin {
    @Unique
    private final RemappedMapRenderer remappedMapRenderer = ((RemappedRendererAccess)MinecraftClient.getInstance()).remapped$getRenderer();

    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/ItemFrameEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/MapRenderer;draw(Lnet/minecraft/client/render/MapRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ZI)V"
        )
    )
    private void remapped$renderRemappedMap(ItemFrameEntityRenderState itemFrameEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci, @Local(ordinal = 0) MapIdComponent id) {
        int k = itemFrameEntityRenderState.glow ? 15728850 : i;
        this.remappedMapRenderer.draw(itemFrameEntityRenderState.mapRenderState, matrixStack, vertexConsumerProvider, true, k);
    }

    @Inject(
        method = "updateRenderState(Lnet/minecraft/entity/decoration/ItemFrameEntity;Lnet/minecraft/client/render/entity/state/ItemFrameEntityRenderState;F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;getMapState(Lnet/minecraft/component/type/MapIdComponent;)Lnet/minecraft/item/map/MapState;"
        )
    )
    private <T extends ItemFrameEntity> void remapped$updateMapRenderState(T itemFrameEntity, ItemFrameEntityRenderState itemFrameEntityRenderState, float f, CallbackInfo ci, @Local(ordinal = 0) MapIdComponent id) {
        RemappedState state = itemFrameEntity.getWorld().remapped$getState(id);
        if (state != null) {
            this.remappedMapRenderer.update(id, state, itemFrameEntityRenderState.mapRenderState);
            itemFrameEntityRenderState.mapId = id;
        }
    }
}
