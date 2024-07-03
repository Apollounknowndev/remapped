package dev.worldgen.remapped.mixin;

import dev.worldgen.remapped.duck.RemappedWorldAccess;
import dev.worldgen.remapped.map.RemappedState;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerWorld.class)
public class ServerWorldMixin implements RemappedWorldAccess {
    @Override
    public RemappedState remapped$getState(MapIdComponent id) {
        return ((ServerWorld)(Object)this).getServer().getOverworld().getPersistentStateManager().get(RemappedState.getPersistentStateType(), id.asString());
    }

    @Override
    public void remapped$setState(MapIdComponent id, RemappedState state) {
        ((ServerWorld)(Object)this).getServer().getOverworld().getPersistentStateManager().set(id.asString(), state);
    }
}