package dev.worldgen.remapped.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class EmptyTrackerlessMap extends Item {
    public EmptyTrackerlessMap(Settings settings) {
        super(settings);
    }

    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack emptyMapStack = user.getStackInHand(hand);
        if (world.isClient) {
            return ActionResult.SUCCESS;
        } else {
            emptyMapStack.decrementUnlessCreative(1, user);
            user.incrementStat(Stats.USED.getOrCreateStat(this));
            user.getWorld().playSoundFromEntity(null, user, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, user.getSoundCategory(), 1.0F, 1.0F);
            ItemStack filledMapStack = FilledMapItem.createMap(world, user.getBlockX(), user.getBlockZ(), (byte)0, false, false);
            if (emptyMapStack.isEmpty()) {
                return ActionResult.SUCCESS.withNewHandStack(filledMapStack);
            } else {
                if (!user.getInventory().insertStack(filledMapStack.copy())) {
                    user.dropItem(filledMapStack, false);
                }

                return ActionResult.SUCCESS;
            }
        }
    }
}
