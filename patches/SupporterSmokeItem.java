package kr.ultrasmod.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** A purely visual, non-damaging supporter flare effect in Minecraft. */
public class SupporterSmokeItem extends Item {
    private static final long DURATION_TICKS = 20L * 20L;
    private static final int PLUME_HEIGHT = 32;
    private static final Map<ServerWorld, List<Flare>> ACTIVE_FLARES = new WeakHashMap<>();

    private static final class Flare {
        private final double x;
        private final double y;
        private final double z;
        private final long startedAt;

        private Flare(double x, double y, double z, long startedAt) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.startedAt = startedAt;
        }
    }

    public SupporterSmokeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            ACTIVE_FLARES.computeIfAbsent(serverWorld, key -> new ArrayList<>()).add(
                    new Flare(user.getX(), user.getY(), user.getZ(), serverWorld.getTime()));
        }
        user.getItemCooldownManager().set(user.getStackInHand(hand), (int) DURATION_TICKS);
        return ActionResult.SUCCESS;
    }

    /** Called once per server-world tick; particles never place fire or cause damage. */
    public static void tick(ServerWorld world) {
        List<Flare> flares = ACTIVE_FLARES.get(world);
        if (flares == null || flares.isEmpty()) return;

        for (int i = flares.size() - 1; i >= 0; i--) {
            Flare flare = flares.get(i);
            long age = world.getTime() - flare.startedAt;
            if (age >= DURATION_TICKS) {
                flares.remove(i);
                continue;
            }

            float fade = age > DURATION_TICKS - 20
                    ? Math.max(0.12f, (DURATION_TICKS - age) / 20.0f) : 1.0f;

            // Saturated scarlet particles form a vertical column up to stadium-roof height.
            if (age % 4 == 0) {
                for (int height = 0; height <= PLUME_HEIGHT; height += 4) {
                    float size = (2.9f - height * 0.018f) * fade;
                    DustParticleEffect scarlet = new DustParticleEffect(0xFF1018, Math.max(1.2f, size));
                    world.spawnParticles(scarlet,
                            flare.x, flare.y + 1.0 + height, flare.z,
                            2, 0.32, 0.48, 0.32, 0.012);
                }

                for (int height = 3; height <= PLUME_HEIGHT + 2; height += 8) {
                    world.spawnParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                            flare.x, flare.y + height, flare.z,
                            1, 0.16, 0.38, 0.16, 0.008);
                }
                world.spawnParticles(ParticleTypes.END_ROD,
                        flare.x, flare.y + 1.2, flare.z,
                        1, 0.20, 0.28, 0.20, 0.018);
            }

            // White-hot center with a vivid orange-red halo; this is a particle effect only.
            if (age % 2 == 0) {
                DustParticleEffect whiteCore = new DustParticleEffect(0xFFF8F0, 2.15f * fade);
                DustParticleEffect hotOrange = new DustParticleEffect(0xFF6A28, 2.7f * fade);
                world.spawnParticles(whiteCore,
                        flare.x, flare.y + 0.75, flare.z,
                        3, 0.22, 0.24, 0.22, 0.006);
                world.spawnParticles(hotOrange,
                        flare.x, flare.y + 0.95, flare.z,
                        4, 0.42, 0.30, 0.42, 0.009);
            }
        }

        if (flares.isEmpty()) ACTIVE_FLARES.remove(world);
    }
}
