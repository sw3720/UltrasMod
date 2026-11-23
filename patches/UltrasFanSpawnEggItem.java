package kr.ultrasmod.item;

import kr.ultrasmod.UltrasMod;
import kr.ultrasmod.entity.UltrasFanEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class UltrasFanSpawnEggItem extends Item {
    private final int teamColor;
    private final int spotColor;

    public UltrasFanSpawnEggItem(int teamColor, int spotColor, Settings settings) {
        super(settings);
        this.teamColor = teamColor;
        this.spotColor = spotColor;
    }

    public int getTeamColor() {
        return teamColor;
    }

    public int getSpotColor() {
        return spotColor;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        BlockPos spawnPos = context.getBlockPos().offset(context.getSide());
        UltrasFanEntity fan = UltrasMod.ULTRAS_FAN.create(world, SpawnReason.SPAWN_EGG);
        if (fan == null) {
            return ActionResult.FAIL;
        }

        fan.refreshPositionAndAngles(
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                context.getPlayer() == null ? 0.0f : context.getPlayer().getYaw(),
                0.0f
        );
        fan.setTeamColor(teamColor);
        world.spawnEntity(fan);

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().creativeMode) {
            context.getStack().decrement(1);
        }
        return ActionResult.CONSUME;
    }
}
