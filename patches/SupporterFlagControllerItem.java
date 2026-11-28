package kr.ultrasmod.item;

import kr.ultrasmod.UltrasMod;
import kr.ultrasmod.block.SupporterFlagBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/** Dedicated hand-held controller for raising and lowering installed supporter flags. */
public class SupporterFlagControllerItem extends Item {
    public SupporterFlagControllerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (!world.getBlockState(context.getBlockPos()).isOf(UltrasMod.SUPPORTER_FLAG_BLOCK)) {
            return ActionResult.PASS;
        }
        if (world.isClient) return ActionResult.SUCCESS;

        SupporterFlagBlock block = (SupporterFlagBlock) world.getBlockState(context.getBlockPos()).getBlock();
        SupporterFlagBlock.MotionToggleResult result = block.toggleMotion(world, context.getBlockPos());
        if (context.getPlayer() != null) {
            String message = switch (result) {
                case STARTED -> "깃발 승강을 시작했습니다.";
                case STOPPED -> "깃발 승강을 멈췄습니다.";
                case BLOCKED -> "깃발 위쪽 4블록의 공간을 비워야 작동합니다.";
                case INVALID -> "완성된 3×5 깃발을 바라보고 사용하세요.";
            };
            context.getPlayer().sendMessage(Text.literal(message), true);
        }
        return result == SupporterFlagBlock.MotionToggleResult.INVALID
                ? ActionResult.PASS : ActionResult.CONSUME;
    }
}
