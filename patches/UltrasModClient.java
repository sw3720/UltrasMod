package kr.ultrasmod;

import kr.ultrasmod.client.FlagArtScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;

public class UltrasModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Keep the in-game flag and banner design editor available.
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient) return ActionResult.PASS;
            if (!player.getStackInHand(hand).isOf(UltrasMod.DESIGN_PALETTE)) return ActionResult.PASS;

            var state = world.getBlockState(hitResult.getBlockPos());
            boolean flag = state.isOf(UltrasMod.SUPPORTER_FLAG_BLOCK);
            boolean banner = state.isOf(UltrasMod.SUPPORTER_BANNER_BLOCK);
            if (!flag && !banner) return ActionResult.PASS;

            MinecraftClient.getInstance().setScreen(new FlagArtScreen(banner));
            return ActionResult.SUCCESS;
        });
    }
}
