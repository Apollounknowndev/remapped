package dev.worldgen.remapped.mixin;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.item.Item$TooltipContext$2")
public interface TooltipContextAccessor {
    @Accessor("field_51354")
    World world();
}
