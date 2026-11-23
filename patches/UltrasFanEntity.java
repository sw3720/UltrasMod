package kr.ultrasmod.entity;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class UltrasFanEntity extends ZombieEntity {
    private int teamColor = 0x168A45;
    private int cheerTick = 0;

    public UltrasFanEntity(EntityType<? extends ZombieEntity> type, World world) {
        super(type, world);
        setAiDisabled(true);
        setPersistent();
        setCustomName(Text.literal("울트라스"));
        setCustomNameVisible(true);
        applyTeamClothing();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return ZombieEntity.createZombieAttributes();
    }

    public void setTeamColor(int color) {
        this.teamColor = color & 0xFFFFFF;
        applyTeamClothing();
    }

    private void applyTeamClothing() {
        ItemStack chest = new ItemStack(Items.LEATHER_CHESTPLATE);
        ItemStack legs = new ItemStack(Items.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
        ItemStack helmet = new ItemStack(Items.LEATHER_HELMET);

        chest.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(teamColor, true));
        legs.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(darken(teamColor), true));
        boots.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(darken(teamColor), true));
        helmet.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(teamColor, true));

        equipStack(EquipmentSlot.CHEST, chest);
        equipStack(EquipmentSlot.LEGS, legs);
        equipStack(EquipmentSlot.FEET, boots);
        equipStack(EquipmentSlot.HEAD, helmet);

        setEquipmentDropChance(EquipmentSlot.CHEST, 0.0f);
        setEquipmentDropChance(EquipmentSlot.LEGS, 0.0f);
        setEquipmentDropChance(EquipmentSlot.FEET, 0.0f);
        setEquipmentDropChance(EquipmentSlot.HEAD, 0.0f);
    }

    private static int darken(int color) {
        int r = ((color >> 16) & 255) * 3 / 4;
        int g = ((color >> 8) & 255) * 3 / 4;
        int b = (color & 255) * 3 / 4;
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient) {
            cheerTick++;
            setVelocity(0.0, getVelocity().y, 0.0);
            if (isOnGround() && cheerTick % 14 == 0) {
                jump();
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("UltrasTeamColor", teamColor);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("UltrasTeamColor")) {
            teamColor = nbt.getInt("UltrasTeamColor") & 0xFFFFFF;
            applyTeamClothing();
        }
    }
}
