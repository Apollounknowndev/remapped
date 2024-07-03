package dev.worldgen.remapped.mixin.entity;

import com.llamalad7.mixinextras.sugar.Local;
import dev.worldgen.remapped.map.RemappedState;
import dev.worldgen.remapped.map.RemappedUtils;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameEntity.class)
public abstract class ItemFrameEntityMixin extends AbstractDecorationEntity {
    protected ItemFrameEntityMixin(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(
        method = "removeFromFrame",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/FilledMapItem;getMapState(Lnet/minecraft/component/type/MapIdComponent;Lnet/minecraft/world/World;)Lnet/minecraft/item/map/MapState;",
            shift = At.Shift.BEFORE
        )
    )
    private void remapped$removeMapMarker(ItemStack stack, CallbackInfo ci, @Local(ordinal = 0) MapIdComponent id) {
        RemappedState state = RemappedUtils.getState(id, this.getWorld());
        if (state != null) {
            state.removeFrame(this.attachedBlockPos, this.getId());
            state.setDirty(true);
        }
    }
}
