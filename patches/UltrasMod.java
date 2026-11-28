package kr.ultrasmod;

import kr.ultrasmod.block.SupporterFlagBlock;
import kr.ultrasmod.item.SupporterSmokeItem;
import kr.ultrasmod.item.SupporterSoundItem;
import kr.ultrasmod.item.SupporterCanvasItem;
import kr.ultrasmod.item.UltrasFanStandItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.EulerAngle;
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
import java.util.List;

public class UltrasMod implements ModInitializer {
    public static final String MOD_ID = "ultrasmod";

    private static final RegistryKey<ItemGroup> ULTRAS_GROUP_KEY =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, id("ultras"));

    // 깃발은 3×5칸, 걸개는 12×3칸으로 한 번에 펼쳐진다.
    public static final Block SUPPORTER_FLAG_BLOCK = registerCanvasBlock("supporter_flag",
            new SupporterFlagBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, id("supporter_flag")))
                    .strength(0.7f).nonOpaque().noCollision()), 3, 5);
    public static final Block SUPPORTER_BANNER_BLOCK = registerCanvasBlock("supporter_banner",
            new SupporterFlagBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, id("supporter_banner")))
                    .strength(0.7f).nonOpaque().noCollision()), 12, 3);

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
    public static final Item SUPPORTER_HAND_FLAG = registerItem("supporter_hand_flag");
    public static final Item MEGAPHONE = registerItem("megaphone",
            new SupporterSoundItem(MEGAPHONE_SOUND, itemSettings("megaphone").maxCount(1)));
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

                        // 그림판
                        entries.add(DESIGN_PALETTE);

                        // 응원 도구
                        entries.add(SUPPORTER_SCARF);
                        entries.add(SUPPORTER_HAND_FLAG);
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

    @Override
    public void onInitialize() {
        // One megaphone click randomly assigns distinct jobs to nearby supporters.
        // Jumping is a minority role; others wave a handheld flag, wave scarves, or clap.
        UseEntityCallback.EVENT.register((player, world, hand, target, hitResult) -> {
            if (!player.getStackInHand(hand).isOf(MEGAPHONE)) return ActionResult.PASS;
            if (!(target instanceof ArmorStandEntity clicked)
                    || !clicked.getCommandTags().contains("ultrasmod_supporter")) {
                return ActionResult.PASS;
            }
            if (world.isClient) return ActionResult.SUCCESS;

            Box groupArea = clicked.getBoundingBox().expand(20.0, 12.0, 20.0);
            List<ArmorStandEntity> group = world.getEntitiesByType(
                    EntityType.ARMOR_STAND,
                    groupArea,
                    stand -> stand.getCommandTags().contains("ultrasmod_supporter")
            );
            if (group.isEmpty()) return ActionResult.PASS;

            boolean stopGroup = group.stream()
                    .anyMatch(stand -> stand.getCommandTags().contains("ultrasmod_cheering"));
            if (stopGroup) {
                for (ArmorStandEntity stand : group) {
                    stand.removeCommandTag("ultrasmod_cheering");
                    stand.removeCommandTag("ultrasmod_jump");
                    stand.removeCommandTag("ultrasmod_clap");
                    stand.removeCommandTag("ultrasmod_flagwave");
                    stand.removeCommandTag("ultrasmod_scarfwave");
                    stand.setNoGravity(false);
                    stand.setPosition(stand.getX(), Math.floor(stand.getY()), stand.getZ());
                    stand.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    stand.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    stand.setLeftArmRotation(new EulerAngle(-1.25f, 0.0f, -0.18f));
                    stand.setRightArmRotation(new EulerAngle(-1.25f, 0.0f, 0.18f));
                }
                player.sendMessage(Text.literal("주변 응원단 동작을 모두 멈췄습니다."), true);
            } else {
                for (int i = group.size() - 1; i > 0; i--) {
                    int j = world.random.nextInt(i + 1);
                    ArmorStandEntity swap = group.get(i);
                    group.set(i, group.get(j));
                    group.set(j, swap);
                }

                int jumpers = group.size() >= 4
                        ? Math.max(1, Math.round(group.size() * 0.15f)) : 0;
                int flagWavers = Math.round(group.size() * 0.30f);
                int scarfWavers = Math.round(group.size() * 0.30f);
                if (group.size() > 1) {
                    while (jumpers + flagWavers + scarfWavers > group.size() - 1) {
                        if (scarfWavers > 0) scarfWavers--;
                        else if (flagWavers > 0) flagWavers--;
                        else if (jumpers > 0) jumpers--;
                    }
                } else {
                    jumpers = 0;
                    flagWavers = 0;
                    scarfWavers = 0;
                }

                for (int i = 0; i < group.size(); i++) {
                    ArmorStandEntity stand = group.get(i);
                    stand.addCommandTag("ultrasmod_cheering");
                    stand.removeCommandTag("ultrasmod_jump");
                    stand.removeCommandTag("ultrasmod_clap");
                    stand.removeCommandTag("ultrasmod_flagwave");
                    stand.removeCommandTag("ultrasmod_scarfwave");
                    stand.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    stand.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

                    if (i < jumpers) {
                        stand.addCommandTag("ultrasmod_jump");
                        stand.setNoGravity(true);
                    } else if (i < jumpers + flagWavers) {
                        stand.addCommandTag("ultrasmod_flagwave");
                        stand.setNoGravity(false);
                        stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(SUPPORTER_HAND_FLAG));
                    } else if (i < jumpers + flagWavers + scarfWavers) {
                        stand.addCommandTag("ultrasmod_scarfwave");
                        stand.setNoGravity(false);
                        stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(SUPPORTER_SCARF));
                        stand.equipStack(EquipmentSlot.OFFHAND, new ItemStack(SUPPORTER_SCARF));
                    } else {
                        stand.addCommandTag("ultrasmod_clap");
                        stand.setNoGravity(false);
                    }
                    stand.setPosition(stand.getX(), Math.floor(stand.getY()), stand.getZ());
                }

                player.sendMessage(Text.literal(
                        "응원 시작! 점프 " + jumpers + "명 · 깃발 " + flagWavers
                                + "명 · 머플러 " + scarfWavers + "명 · 박수 "
                                + (group.size() - jumpers - flagWavers - scarfWavers) + "명"
                ), true);
            }

            world.playSound(null, clicked.getX(), clicked.getY(), clicked.getZ(),
                    MEGAPHONE_SOUND, SoundCategory.PLAYERS, 1.0f, 1.0f);
            return ActionResult.SUCCESS;
        });

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            Box loadedWorld = new Box(-30_000_000.0, -2048.0, -30_000_000.0,
                    30_000_000.0, 2048.0, 30_000_000.0);
            long tick = world.getTime();
            for (ArmorStandEntity stand : world.getEntitiesByType(EntityType.ARMOR_STAND, loadedWorld,
                    entity -> entity.getCommandTags().contains("ultrasmod_cheering"))) {
                if (stand.getCommandTags().contains("ultrasmod_jump")) {
                    stand.setLeftArmRotation(new EulerAngle(-1.75f, 0.0f, -0.30f));
                    stand.setRightArmRotation(new EulerAngle(-1.75f, 0.0f, 0.30f));
                    double jump = Math.max(0.0,
                            Math.sin((tick + stand.getId() * 3.0) * Math.PI / 12.0)) * 0.24;
                    stand.setPosition(stand.getX(), Math.floor(stand.getY()) + jump, stand.getZ());
                } else if (stand.getCommandTags().contains("ultrasmod_flagwave")) {
                    double phase = (tick + stand.getId() * 5.0) * Math.PI / 18.0;
                    float side = (float) Math.sin(phase);
                    float lift = (float) Math.cos(phase);
                    stand.setRightArmRotation(new EulerAngle(
                            -2.05f + side * 0.48f, side * 0.62f, -0.25f + lift * 0.42f));
                    stand.setLeftArmRotation(new EulerAngle(-1.55f, 0.0f, 0.12f));
                    stand.setPosition(stand.getX(), Math.floor(stand.getY()), stand.getZ());
                } else if (stand.getCommandTags().contains("ultrasmod_scarfwave")) {
                    double phase = (tick + stand.getId() * 4.0) * Math.PI / 24.0;
                    float sway = (float) Math.sin(phase);
                    float lift = (float) Math.cos(phase);
                    stand.setLeftArmRotation(new EulerAngle(
                            -2.72f + lift * 0.16f, -0.18f + sway * 0.16f, -0.58f - sway * 0.18f));
                    stand.setRightArmRotation(new EulerAngle(
                            -2.72f - lift * 0.16f, 0.18f + sway * 0.16f, 0.58f + sway * 0.18f));
                    stand.setPosition(stand.getX(), Math.floor(stand.getY()), stand.getZ());
                } else if (stand.getCommandTags().contains("ultrasmod_clap")) {
                    boolean handsTogether = (tick % 12L) < 5L;
                    float roll = handsTogether ? 0.10f : 0.58f;
                    stand.setLeftArmRotation(new EulerAngle(-1.65f, 0.0f, -roll));
                    stand.setRightArmRotation(new EulerAngle(-1.65f, 0.0f, roll));
                    stand.setPosition(stand.getX(), Math.floor(stand.getY()), stand.getZ());
                }
            }
        });
    }
}
