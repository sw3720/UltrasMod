package kr.ultrasmod.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Large, long-lived visual red supporter smoke. It has no fire or damage effect. */
public class SupporterSmokeItem extends Item {
    public SupporterSmokeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            DustParticleEffect red = new DustParticleEffect(0xD52C38, 2.5f);
            serverWorld.spawnParticles(red, user.getX(), user.getY() + 0.9, user.getZ(),
                    140, 0.78, 1.0, 0.78, 0.018);
            serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                    user.getX(), user.getY() + 0.9, user.getZ(),
                    42, 0.42, 0.65, 0.42, 0.018);
        }
        user.getItemCooldownManager().set(user.getStackInHand(hand), 35);
        return ActionResult.SUCCESS;
    }
}
