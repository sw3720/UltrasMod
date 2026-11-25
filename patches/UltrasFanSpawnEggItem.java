package kr.ultrasmod.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
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
        ArmorStandEntity supporter = EntityType.ARMOR_STAND.create(world, SpawnReason.SPAWN_ITEM_USE);
        if (supporter == null) {
            return ActionResult.FAIL;
        }

        supporter.refreshPositionAndAngles(
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                context.getPlayer() == null ? 0.0f : context.getPlayer().getYaw(),
                0.0f
        );
        supporter.setShowArms(true);
        supporter.setNoBasePlate(true);
        supporter.setSmall(false);
        supporter.setCustomName(net.minecraft.text.Text.literal("울트라스"));
        supporter.setCustomNameVisible(false);
        supporter.setSilent(true);

        supporter.equipStack(EquipmentSlot.HEAD, dyedLeather(Items.LEATHER_HELMET, teamColor));
        supporter.equipStack(EquipmentSlot.CHEST, dyedLeather(Items.LEATHER_CHESTPLATE, teamColor));
        supporter.equipStack(EquipmentSlot.LEGS, dyedLeather(Items.LEATHER_LEGGINGS, darken(teamColor)));
        supporter.equipStack(EquipmentSlot.FEET, dyedLeather(Items.LEATHER_BOOTS, darken(teamColor)));

        world.spawnEntity(supporter);

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().creativeMode) {
            context.getStack().decrement(1);
        }
        return ActionResult.CONSUME;
    }

    private static ItemStack dyedLeather(Item item, int color) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color & 0xFFFFFF, true));
        return stack;
    }

    private static int darken(int color) {
        int r = ((color >> 16) & 255) * 3 / 4;
        int g = ((color >> 8) & 255) * 3 / 4;
        int b = (color & 255) * 3 / 4;
        return (r << 16) | (g << 8) | b;
    }
}
