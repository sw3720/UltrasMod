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
import net.minecraft.util.math.EulerAngle;
import net.minecraft.world.World;

/** A placeable supporter display: an armor stand already dressed in its team's kit. */
public class UltrasFanStandItem extends Item {
    private final int teamColor;
    private final int trimColor;

    public UltrasFanStandItem(int teamColor, int trimColor, Settings settings) {
        super(settings);
        this.teamColor = teamColor & 0xFFFFFF;
        this.trimColor = trimColor & 0xFFFFFF;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        BlockPos pos = context.getBlockPos().offset(context.getSide());
        ArmorStandEntity stand = EntityType.ARMOR_STAND.create(world, SpawnReason.SPAWN_ITEM_USE);
        if (stand == null) {
            return ActionResult.FAIL;
        }

        stand.refreshPositionAndAngles(
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                context.getPlayer() == null ? 0.0f : context.getPlayer().getYaw(),
                0.0f
        );
        stand.setShowArms(true);
        stand.setHideBasePlate(true);
        stand.setCustomName(net.minecraft.text.Text.literal("울트라스"));
        stand.setCustomNameVisible(false);
        stand.setSilent(true);
        stand.addCommandTag("ultrasmod_supporter");
        stand.addCommandTag("ultrasmod_team_" + teamName());

        // Fixed raised-arm cheering pose. It is a real armor stand, not a spawn-egg mob.
        // Vanilla-like relaxed arm pose. ArmorStand EulerAngle values are degrees.
        stand.setLeftArmRotation(new EulerAngle(-10.0f, 0.0f, -10.0f));
        stand.setRightArmRotation(new EulerAngle(-10.0f, 0.0f, 10.0f));

        stand.equipStack(EquipmentSlot.HEAD, dyedLeather(Items.LEATHER_HELMET, teamColor));
        stand.equipStack(EquipmentSlot.CHEST, dyedLeather(Items.LEATHER_CHESTPLATE, teamColor));
        stand.equipStack(EquipmentSlot.LEGS, dyedLeather(Items.LEATHER_LEGGINGS, darken(teamColor)));
        stand.equipStack(EquipmentSlot.FEET, dyedLeather(Items.LEATHER_BOOTS, darken(trimColor)));

        if (!world.spawnEntity(stand)) {
            return ActionResult.FAIL;
        }
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().creativeMode) {
            context.getStack().decrement(1);
        }
        return ActionResult.CONSUME;
    }

    private String teamName() {
        return switch (teamColor) {
            case 0x1769AA -> "blue";
            case 0xC62828 -> "red";
            case 0xF4C430 -> "yellow";
            case 0x202020 -> "black";
            case 0xEEEEEE -> "white";
            case 0x7B3FA1 -> "purple";
            case 0xE87519 -> "orange";
            default -> "green";
        };
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
