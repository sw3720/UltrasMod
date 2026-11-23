package kr.ultrasmod.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class SupporterSoundItem extends Item {
    private final SoundEvent sound;

    public SupporterSoundItem(SoundEvent sound, Settings settings) {
        super(settings);
        this.sound = sound;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient) {
            world.playSound(user, user.getX(), user.getY(), user.getZ(), sound,
                    SoundCategory.PLAYERS, 1.0f, 0.85f + world.random.nextFloat() * 0.3f);
        }
        user.getItemCooldownManager().set(user.getStackInHand(hand), 8);
        return ActionResult.SUCCESS;
    }
}
