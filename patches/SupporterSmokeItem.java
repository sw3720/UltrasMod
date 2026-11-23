package kr.ultrasmod.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Harmless, purely visual red supporter-colour particles.
 * No fire, explosive effect, damage, or real-world device is involved.
 */
public class SupporterSmokeItem extends Item {
    public SupporterSmokeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            DustParticleEffect red = new DustParticleEffect(0xD52C38, 1.6f);
            serverWorld.spawnParticles(red, user.getX(), user.getY() + 0.8, user.getZ(),
                    52, 0.42, 0.55, 0.42, 0.015);
            serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    user.getX(), user.getY() + 0.8, user.getZ(),
                    16, 0.25, 0.35, 0.25, 0.012);
        }
        user.getItemCooldownManager().set(user.getStackInHand(hand), 35);
        return ActionResult.SUCCESS;
    }
}
