package kr.ultrasmod;

import kr.ultrasmod.block.SupporterFlagBlock;
import kr.ultrasmod.item.SupporterFlagControllerItem;
import kr.ultrasmod.item.SupporterSmokeItem;
import kr.ultrasmod.item.SupporterSoundItem;
import kr.ultrasmod.item.SupporterMegaphoneItem;
import kr.ultrasmod.item.SupporterCanvasItem;
import kr.ultrasmod.item.UltrasFanStandItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class UltrasMod implements ModInitializer {
    public static final String MOD_ID = "ultrasmod";

    private static final RegistryKey<ItemGroup> ULTRAS_GROUP_KEY =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, id("ultras"));

    private static final Map<ServerWorld, Map<UUID, CheerState>> ACTIVE_SUPPORTERS = new WeakHashMap<>();

    private static final int CHEER_JUMP = 0;
    private static final int CHEER_CLAP = 1;
    private static final int CHEER_HANDS = 2;
    private static final int CHEER_SCARF = 3;

    private static final class CheerState {
        private final ArmorStandEntity stand;
        private final double baseY;
        private final long jumpPhaseOffset;
        private int mode;

        private CheerState(ArmorStandEntity stand, double baseY, int mode, long jumpPhaseOffset) {
            this.stand = stand;
            this.baseY = baseY;
            this.mode = mode;
            this.jumpPhaseOffset = jumpPhaseOffset;
        }
    }

    private static final long JUMP_CYCLE_TICKS = 14L;

    // 깃발은 3×5칸, 걸개는 12×3칸으로 한 번에 펼쳐진다.
    public static final Block SUPPORTER_FLAG_BLOCK = registerCanvasBlock("supporter_flag",
            new SupporterFlagBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, id("supporter_flag")))
                    .strength(0.7f).nonOpaque().noCollision()), 3, 5);
    public static final Block SUPPORTER_BANNER_BLOCK = registerCanvasBlock("supporter_banner",
            new SupporterFlagBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, id("supporter_banner")))
                    .strength(0.7f).nonOpaque().noCollision()), 12, 3);
    public static final Item SUPPORTER_FLAG_CONTROLLER = registerItem("supporter_flag_controller",
            new SupporterFlagControllerItem(itemSettings("supporter_flag_controller").maxCount(1)));

    // Holding one of these items shows a clothed supporter stand; right-click to place it.
    public static final Item SUPPORTER_GREEN = registerSupporterStand("ultras_supporter_stand_green", 0x168A45, 0xFFFFFF);
    public static final Item SUPPORTER_BLUE = registerSupporterStand("ultras_supporter_stand_blue", 0x1769AA, 0xE7F4FF);
    public static final Item SUPPORTER_RED = registerSupporterStand("ultras_supporter_stand_red", 0xC62828, 0xFFFFFF);
    public static final Item SUPPORTER_YELLOW = registerSupporterStand("ultras_supporter_stand_yellow", 0xF4C430, 0x202020);
    public static final Item SUPPORTER_BLACK = registerSupporterStand("ultras_supporter_stand_black", 0x202020, 0xEEEEEE);
    public static final Item SUPPORTER_WHITE = registerSupporterStand("ultras_supporter_stand_white", 0xEEEEEE, 0x333333);
    public static final Item SUPPORTER_PURPLE = registerSupporterStand("ultras_supporter_stand_purple", 0x7B3FA1, 0xFFFFFF);
    public static final Item SUPPORTER_ORANGE = registerSupporterStand("ultras_supporter_stand_orange", 0xE87519, 0xFFFFFF);

    public static final SoundEvent MEGAPHONE_SOUND = registerSound("megaphone_call");
    public static final SoundEvent DRUM_SOUND = registerSound("drum_hit");
    public static final SoundEvent CLAPPER_SOUND = registerSound("clapper");

    public static final Item SUPPORTER_SCARF = registerItem("supporter_scarf");
    // Green is the default large flag; other variants match the stand's uniform color.
    public static final Item SUPPORTER_HAND_FLAG = registerItem("supporter_hand_flag");
    public static final Item SUPPORTER_HAND_FLAG_BLUE = registerItem("supporter_hand_flag_blue");
    public static final Item SUPPORTER_HAND_FLAG_RED = registerItem("supporter_hand_flag_red");
    public static final Item SUPPORTER_HAND_FLAG_YELLOW = registerItem("supporter_hand_flag_yellow");
    public static final Item SUPPORTER_HAND_FLAG_BLACK = registerItem("supporter_hand_flag_black");
    public static final Item SUPPORTER_HAND_FLAG_WHITE = registerItem("supporter_hand_flag_white");
    public static final Item SUPPORTER_HAND_FLAG_PURPLE = registerItem("supporter_hand_flag_purple");
    public static final Item SUPPORTER_HAND_FLAG_ORANGE = registerItem("supporter_hand_flag_orange");
    public static final Item MEGAPHONE = registerItem("megaphone",
            new SupporterMegaphoneItem(MEGAPHONE_SOUND, itemSettings("megaphone").maxCount(1)));
    public static final Item SUPPORTER_DRUM = registerItem("supporter_drum",
            new SupporterSoundItem(DRUM_SOUND, itemSettings("supporter_drum").maxCount(1)));
    public static final Item CLAPPER = registerItem("clapper",
            new SupporterSoundItem(CLAPPER_SOUND, itemSettings("clapper").maxCount(1)));
    public static final Item RED_SUPPORTER_SMOKE = registerItem("red_supporter_smoke",
            new SupporterSmokeItem(itemSettings("red_supporter_smoke").maxCount(16)));
    public static final Item DESIGN_PALETTE = registerItem("design_palette",
            new Item(itemSettings("design_palette").maxCount(1)));

    public static final ItemGroup ULTRAS_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            ULTRAS_GROUP_KEY,
            FabricItemGroup.builder()
                    .displayName(Text.literal("울트라스"))
                    .icon(() -> new ItemStack(SUPPORTER_FLAG_BLOCK))
                    .entries((displayContext, entries) -> {
                        // 깃발과 걸개
                        entries.add(SUPPORTER_FLAG_BLOCK);
                        entries.add(SUPPORTER_BANNER_BLOCK);
                        entries.add(SUPPORTER_FLAG_CONTROLLER);

                        // 그림판
                        entries.add(DESIGN_PALETTE);

                        // 응원 도구
                        entries.add(SUPPORTER_SCARF);
                        entries.add(MEGAPHONE);
                        entries.add(SUPPORTER_DRUM);
                        entries.add(CLAPPER);
                        entries.add(RED_SUPPORTER_SMOKE);

                        // 팀 유니폼을 입힌 갑옷 거치대 설치 아이템
                        entries.add(SUPPORTER_GREEN);
                        entries.add(SUPPORTER_BLUE);
                        entries.add(SUPPORTER_RED);
                        entries.add(SUPPORTER_YELLOW);
                        entries.add(SUPPORTER_BLACK);
                        entries.add(SUPPORTER_WHITE);
                        entries.add(SUPPORTER_PURPLE);
                        entries.add(SUPPORTER_ORANGE);
                    })
                    .build()
    );

    private static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    private static SoundEvent registerSound(String path) {
        Identifier soundId = id(path);
        return Registry.register(Registries.SOUND_EVENT, soundId, SoundEvent.of(soundId));
    }

    private static Item.Settings itemSettings(String name) {
        return new Item.Settings()
                .registryKey(RegistryKey.of(RegistryKeys.ITEM, id(name)));
    }

    private static Block registerCanvasBlock(String name, Block block, int width, int height) {
        Block registered = Registry.register(Registries.BLOCK, id(name), block);
        Item.Settings settings = itemSettings(name).useBlockPrefixedTranslationKey();
        Registry.register(Registries.ITEM, id(name),
                new SupporterCanvasItem(registered, width, height, settings));
        return registered;
    }

    private static Block registerBlock(String name, Block block) {
        Block registered = Registry.register(Registries.BLOCK, id(name), block);
        Item.Settings settings = itemSettings(name).useBlockPrefixedTranslationKey();
        Registry.register(Registries.ITEM, id(name), new BlockItem(registered, settings));
        return registered;
    }

    private static Item registerItem(String name) {
        return registerItem(name, new Item(itemSettings(name).maxCount(1)));
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, id(name), item);
    }

    private static Item registerSupporterStand(String name, int teamColor, int trimColor) {
        return registerItem(name, new UltrasFanStandItem(
                teamColor, trimColor, itemSettings(name).maxCount(16)));
    }

    public static Item handFlagForSupporter(ArmorStandEntity stand) {
        if (stand.getCommandTags().contains("ultrasmod_team_blue")) return SUPPORTER_HAND_FLAG_BLUE;
        if (stand.getCommandTags().contains("ultrasmod_team_red")) return SUPPORTER_HAND_FLAG_RED;
        if (stand.getCommandTags().contains("ultrasmod_team_yellow")) return SUPPORTER_HAND_FLAG_YELLOW;
        if (stand.getCommandTags().contains("ultrasmod_team_black")) return SUPPORTER_HAND_FLAG_BLACK;
        if (stand.getCommandTags().contains("ultrasmod_team_white")) return SUPPORTER_HAND_FLAG_WHITE;
        if (stand.getCommandTags().contains("ultrasmod_team_purple")) return SUPPORTER_HAND_FLAG_PURPLE;
        if (stand.getCommandTags().contains("ultrasmod_team_orange")) return SUPPORTER_HAND_FLAG_ORANGE;
        return SUPPORTER_HAND_FLAG;
    }

    /**
     * Starts a deterministic animation for one supporter stand.
     * The stand is kept at a fixed baseline and its animation is calculated
     * from elapsed ticks, so no accumulated velocity can make it drift upward.
     */
    public static void setCheeringMode(ServerWorld world, ArmorStandEntity stand, int mode) {
        Map<UUID, CheerState> active = ACTIVE_SUPPORTERS.computeIfAbsent(world, key -> new java.util.HashMap<>());
        CheerState state = active.get(stand.getUuid());

        if (state == null) {
            // Recover legacy v0.4.9 stands that may have been left floating above their installation point.
            double baseY = stand.getCommandTags().contains("ultrasmod_jump")
                    ? findGroundPosition(world, stand) : stand.getY();
            // Give each placed stand its own stable phase so a crowd does not jump together.
            long jumpPhaseOffset = Math.floorMod((long) stand.getUuid().hashCode(), JUMP_CYCLE_TICKS);
            state = new CheerState(stand, baseY, mode, jumpPhaseOffset);
            active.put(stand.getUuid(), state);
        } else {
            state.mode = mode;
        }

        stand.removeCommandTag("ultrasmod_cheering");
        stand.removeCommandTag("ultrasmod_jump");
        stand.removeCommandTag("ultrasmod_clap");
        stand.removeCommandTag("ultrasmod_hands");
        stand.removeCommandTag("ultrasmod_scarfwave");
        stand.addCommandTag("ultrasmod_cheering");

        switch (mode) {
            case CHEER_JUMP -> stand.addCommandTag("ultrasmod_jump");
            case CHEER_CLAP -> stand.addCommandTag("ultrasmod_clap");
            case CHEER_HANDS -> stand.addCommandTag("ultrasmod_hands");
            case CHEER_SCARF -> stand.addCommandTag("ultrasmod_scarfwave");
            default -> throw new IllegalArgumentException("Unknown cheer mode " + mode);
        }

        // Every supporter uses explicit poses; gravity and residual velocity never affect timing.
        stand.setShowArms(true);
        stand.setNoGravity(true);
        stand.setVelocity(0.0, 0.0, 0.0);
        stand.setPosition(stand.getX(), state.baseY, stand.getZ());

        if (mode == CHEER_SCARF) {
            ItemStack scarf = new ItemStack(SUPPORTER_SCARF);
            stand.equipStack(EquipmentSlot.MAINHAND, scarf.copy());
            stand.equipStack(EquipmentSlot.OFFHAND, scarf.copy());
        } else {
            stand.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            stand.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    private static double findGroundPosition(ServerWorld world, ArmorStandEntity stand) {
        int x = net.minecraft.util.math.MathHelper.floor(stand.getX());
        int z = net.minecraft.util.math.MathHelper.floor(stand.getZ());
        int startY = net.minecraft.util.math.MathHelper.floor(stand.getY()) - 1;
        int bottomY = Math.max(world.getBottomY(), startY - 512);
        BlockPos.Mutable pos = new BlockPos.Mutable(x, startY, z);
        for (int y = startY; y >= bottomY; y--) {
            pos.set(x, y, z);
            if (world.getBlockState(pos).isSolidBlock(world, pos)) return y + 1.0;
        }
        return stand.getY();
    }

    private static void tickActiveSupporters(ServerWorld world) {
        Map<UUID, CheerState> active = ACTIVE_SUPPORTERS.get(world);
        if (active == null || active.isEmpty()) return;

        // A shared clock keeps animation smooth, while each stand applies its own
        // stable phase offset to create staggered, out-of-sync jumping.
        long time = world.getTime();
        float wave = (float) Math.sin(time * 0.38);
        float clapCycle = (float) (0.5 - 0.5 * Math.cos(time * 0.70));

        var iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            CheerState state = iterator.next().getValue();
            ArmorStandEntity stand = state.stand;
            if (stand.isRemoved() || stand.getWorld() != world) {
                iterator.remove();
                continue;
            }

            stand.setShowArms(true);
            stand.setNoGravity(true);
            if (stand.getVelocity().lengthSquared() != 0.0) {
                stand.setVelocity(0.0, 0.0, 0.0);
            }

            switch (state.mode) {
                case CHEER_JUMP -> {
                    // Each stand jumps at a different point in the cycle, like a real crowd.
                    // The curve gives a short rise, fall, and grounded beat.
                    long jumpPhase = Math.floorMod(time + state.jumpPhaseOffset, JUMP_CYCLE_TICKS);
                    double jumpHeight = Math.max(0.0,
                            0.42 * jumpPhase - 0.04 * jumpPhase * jumpPhase);
                    stand.setPosition(stand.getX(), state.baseY + jumpHeight, stand.getZ());

                    float lift = (float) (150.0 + 10.0 * Math.sin(time * 0.38));
                    stand.setLeftArmRotation(new EulerAngle(-8.0f, 0.0f, -lift));
                    stand.setRightArmRotation(new EulerAngle(-8.0f, 0.0f, lift));
                }
                case CHEER_CLAP -> {
                    stand.setPosition(stand.getX(), state.baseY, stand.getZ());

                    // Sweep the forearms inward to touch in front of the chest,
                    // then outward again. EulerAngle values are DEGREES.
                    float angle = 28.0f + clapCycle * 62.0f;
                    stand.setLeftArmRotation(new EulerAngle(-18.0f, 0.0f, -angle));
                    stand.setRightArmRotation(new EulerAngle(-18.0f, 0.0f, angle));
                }
                case CHEER_HANDS -> {
                    stand.setPosition(stand.getX(), state.baseY, stand.getZ());

                    // Both arms are raised; small opposite yaw changes create a
                    // clear synchronized left-right hand wave without moving the body.
                    float yaw = wave * 20.0f;
                    float roll = 155.0f + wave * 8.0f;
                    stand.setLeftArmRotation(new EulerAngle(-8.0f, yaw, -roll));
                    stand.setRightArmRotation(new EulerAngle(-8.0f, -yaw, roll));
                }
                case CHEER_SCARF -> {
                    stand.setPosition(stand.getX(), state.baseY, stand.getZ());

                    float yaw = wave * 22.0f;
                    float roll = 150.0f + wave * 10.0f;
                    stand.setLeftArmRotation(new EulerAngle(-8.0f, yaw, -roll));
                    stand.setRightArmRotation(new EulerAngle(-8.0f, -yaw, roll));
                }
                default -> {
                    stand.setPosition(stand.getX(), state.baseY, stand.getZ());
                    iterator.remove();
                }
            }
        }
        if (active.isEmpty()) ACTIVE_SUPPORTERS.remove(world);
    }

    @Override
    public void onInitialize() {
        // Only actively cheering stands are updated. Idle stands are never scanned.
        ServerTickEvents.END_WORLD_TICK.register(UltrasMod::tickActiveSupporters);
    }
}
