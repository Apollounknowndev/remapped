package dev.worldgen.remapped.mixin;

import com.google.common.collect.Maps;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.item.map.MapState;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;

@Mixin(PersistentStateManager.class)
public class PersistentStateManagerMixin {
    @Shadow
    @Final
    private final Map<String, Optional<PersistentState>> loadedStates = Maps.newHashMap();

    @Inject(
        method = "get(Lnet/minecraft/world/PersistentState$Type;Ljava/lang/String;)Lnet/minecraft/world/PersistentState;",
        at = @At("HEAD"),
        cancellable = true
    )
    private <T extends PersistentState> void remapped$fixCastingIssues(PersistentState.Type<T> type, String id, CallbackInfoReturnable<T> cir) {
        // Fix getting RemappedState when querying MapState
        if (type.type() == DataFixTypes.SAVED_DATA_MAP_DATA) {
            cir.setReturnValue(null);
        }

        // Fix getting MapState when querying RemappedState
        Optional<PersistentState> persistentState = this.loadedStates.get(id);
        if (persistentState != null && persistentState.isPresent() && persistentState.get() instanceof MapState) {
            this.loadedStates.remove(id);
            cir.setReturnValue(null);
        }
    }
}
