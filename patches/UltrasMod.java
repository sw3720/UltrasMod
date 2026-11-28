package kr.ultrasmod;

import kr.ultrasmod.block.SupporterFlagBlock;
import kr.ultrasmod.item.SupporterSmokeItem;
import kr.ultrasmod.item.SupporterSoundItem;
import kr.ultrasmod.item.SupporterCanvasItem;
import kr.ultrasmod.item.UltrasFanStandItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
        // Existing supporter figures remain real armor stands; animate their cheering in place.
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            Box loadedWorld = new Box(-30_000_000.0, -2048.0, -30_000_000.0,
                    30_000_000.0, 2048.0, 30_000_000.0);
            long tick = world.getTime();
            for (ArmorStandEntity stand : world.getEntitiesByType(EntityType.ARMOR_STAND, loadedWorld,
                    entity -> Text.literal("울트라스").equals(entity.getCustomName()))) {
                stand.setNoGravity(true);
                int phase = (int) (tick % 24L);
                if (phase < 6) {
                    stand.setLeftArmRotation(new EulerAngle(-92.0f, 0.0f, -10.0f));
                    stand.setRightArmRotation(new EulerAngle(-92.0f, 0.0f, 10.0f));
                } else {
                    stand.setLeftArmRotation(new EulerAngle(-155.0f, 0.0f, -24.0f));
                    stand.setRightArmRotation(new EulerAngle(-155.0f, 0.0f, 24.0f));
                }
                double jump = Math.max(0.0, Math.sin((tick + stand.getId() * 3.0) * Math.PI / 12.0)) * 0.22;
                stand.setPosition(stand.getX(), stand.getBlockPos().getY() + jump, stand.getZ());
            }
        });
    }
}
