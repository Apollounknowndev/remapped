package dev.worldgen.remapped.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import dev.worldgen.remapped.duck.RemappedRendererAccess;
import dev.worldgen.remapped.map.RemappedState;
import dev.worldgen.remapped.map.RemappedUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameEntityRenderer.class)
public abstract class ItemFrameEntityRendererMixin {
    @Inject(
        method = "render(Lnet/minecraft/entity/decoration/ItemFrameEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/item/FilledMapItem;getMapState(Lnet/minecraft/component/type/MapIdComponent;Lnet/minecraft/world/World;)Lnet/minecraft/item/map/MapState;"
            )
        )
    )
    private <T extends ItemFrameEntity> void remapped$renderRemappedMap(T itemFrameEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci, @Local(ordinal = 0) MapIdComponent id) {
        RemappedState state = RemappedUtils.getState(id, itemFrameEntity.getWorld());
        matrixStack.translate(0.0F, 0.0F, -1.0F);
        if (state != null) {
            int k = this.getLight(itemFrameEntity, LightmapTextureManager.MAX_SKY_LIGHT_COORDINATE | 210, i);
            ((RemappedRendererAccess)MinecraftClient.getInstance().gameRenderer).remapped$getRenderer().draw(matrixStack, vertexConsumerProvider, id, state, true, k);
        }
    }

    @Shadow
    private <T extends ItemFrameEntity> int getLight(T itemFrame, int glowLight, int regularLight) {
        throw new IllegalStateException("Implemented via mixin");
    }
}
