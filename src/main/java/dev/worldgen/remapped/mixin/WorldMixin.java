package dev.worldgen.remapped.mixin;

import dev.worldgen.remapped.duck.RemappedWorldAccess;
import dev.worldgen.remapped.map.RemappedState;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(World.class)
public class WorldMixin implements RemappedWorldAccess {
	@Override
	public RemappedState remapped$getState(MapIdComponent id) {
		throw new IllegalStateException("You borked something up, how did you manage to not call from ClientWorld or ServerWorld?");
	}

	@Override
	public void remapped$setState(MapIdComponent id, RemappedState state) {
		throw new IllegalStateException("You borked something up, how did you manage to not call from ClientWorld or ServerWorld?");
	}
}