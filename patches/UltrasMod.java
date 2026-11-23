package kr.ultrasmod;

import kr.ultrasmod.block.SupporterFlagBlock;
import kr.ultrasmod.entity.UltrasFanEntity;
import kr.ultrasmod.item.SupporterSmokeItem;
import kr.ultrasmod.item.SupporterSoundItem;
import kr.ultrasmod.item.UltrasFanSpawnEggItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class UltrasMod implements ModInitializer {
    public static final String MOD_ID = "ultrasmod";
    private static final RegistryKey<ItemGroup> FUNCTIONAL_GROUP =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.ofVanilla("functional"));
    private static final RegistryKey<ItemGroup> SPAWN_EGGS_GROUP =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.ofVanilla("spawn_eggs"));
    private static final RegistryKey<ItemGroup> TOOLS_GROUP =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.ofVanilla("tools"));

    public static final Block SUPPORTER_FLAG_BLOCK = registerBlock("supporter_flag",
            new SupporterFlagBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, id("supporter_flag")))
                    .strength(0.7f).nonOpaque().noCollision()));
    public static final Block SUPPORTER_BANNER_BLOCK = registerBlock("supporter_banner",
            new SupporterFlagBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, id("supporter_banner")))
                    .strength(0.7f).nonOpaque().noCollision()));

    public static final EntityType<UltrasFanEntity> ULTRAS_FAN = Registry.register(
            Registries.ENTITY_TYPE, id("ultras_fan"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, UltrasFanEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .trackRangeBlocks(12)
                    .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, id("ultras_fan")))
    );

    public static final Item EGG_GREEN = registerEgg("ultras_fan_spawn_egg_green", 0x168A45, 0xFFFFFF);
    public static final Item EGG_BLUE = registerEgg("ultras_fan_spawn_egg_blue", 0x1769AA, 0xE7F4FF);
    public static final Item EGG_RED = registerEgg("ultras_fan_spawn_egg_red", 0xC62828, 0xFFFFFF);
    public static final Item EGG_YELLOW = registerEgg("ultras_fan_spawn_egg_yellow", 0xF4C430, 0x202020);
    public static final Item EGG_BLACK = registerEgg("ultras_fan_spawn_egg_black", 0x202020, 0xEEEEEE);
    public static final Item EGG_WHITE = registerEgg("ultras_fan_spawn_egg_white", 0xEEEEEE, 0x333333);
    public static final Item EGG_PURPLE = registerEgg("ultras_fan_spawn_egg_purple", 0x7B3FA1, 0xFFFFFF);
    public static final Item EGG_ORANGE = registerEgg("ultras_fan_spawn_egg_orange", 0xE87519, 0xFFFFFF);

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

    private static Item registerEgg(String name, int teamColor, int spotColor) {
        return registerItem(name, new UltrasFanSpawnEggItem(
                teamColor, spotColor, itemSettings(name).maxCount(16)));
    }

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(ULTRAS_FAN, UltrasFanEntity.createAttributes());

        ItemGroupEvents.modifyEntriesEvent(FUNCTIONAL_GROUP).register(entries -> {
            entries.add(SUPPORTER_FLAG_BLOCK);
            entries.add(SUPPORTER_BANNER_BLOCK);
        });
        ItemGroupEvents.modifyEntriesEvent(SPAWN_EGGS_GROUP).register(entries -> {
            entries.add(EGG_GREEN);
            entries.add(EGG_BLUE);
            entries.add(EGG_RED);
            entries.add(EGG_YELLOW);
            entries.add(EGG_BLACK);
            entries.add(EGG_WHITE);
            entries.add(EGG_PURPLE);
            entries.add(EGG_ORANGE);
        });
        ItemGroupEvents.modifyEntriesEvent(TOOLS_GROUP).register(entries -> {
            entries.add(SUPPORTER_SCARF);
            entries.add(MEGAPHONE);
            entries.add(SUPPORTER_DRUM);
            entries.add(CLAPPER);
            entries.add(RED_SUPPORTER_SMOKE);
            entries.add(DESIGN_PALETTE);
        });
    }
}
