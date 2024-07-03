package dev.worldgen.remapped.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.worldgen.remapped.map.RemappedState;
import dev.worldgen.remapped.map.RemappedUtils;
import dev.worldgen.remapped.network.RemappedMapUpdatePacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntryMixin {
    @Shadow
    @Final
    private ServerWorld world;

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/FilledMapItem;getMapState(Lnet/minecraft/component/type/MapIdComponent;Lnet/minecraft/world/World;)Lnet/minecraft/item/map/MapState;",
            shift = At.Shift.BEFORE
        )
    )
    private void remapped$updateMapTracker(CallbackInfo ci, @Local(ordinal = 0) ItemStack stack, @Local(ordinal = 0) MapIdComponent id) {
        RemappedState state = RemappedUtils.getState(id, world);
        if (state != null) {
            for (ServerPlayerEntity player : this.world.getPlayers()) {
                state.update(player, stack);
                RemappedMapUpdatePacket packet = state.getPlayerMarkerPacket(id, player);
                if (packet != null) {
                    ServerPlayNetworking.send(player, packet);
                }
            }
        }
    }
}
