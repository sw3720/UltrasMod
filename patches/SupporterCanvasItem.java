package kr.ultrasmod.item;

import kr.ultrasmod.block.SupporterFlagBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class SupporterCanvasItem extends BlockItem {
    private final int width;
    private final int height;

    public SupporterCanvasItem(Block block, int width, int height, Settings settings) {
        super(block, settings);
        this.width = width;
        this.height = height;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        BlockPos anchor = context.getBlockPos().offset(context.getSide());
        Direction facing = context.getHorizontalPlayerFacing().getOpposite();
        Direction across = facing.rotateYClockwise();

        // First check the entire footprint so a failed placement never leaves half a banner.
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                BlockPos pos = anchor.offset(across, column).up(height - 1 - row);
                if (!world.getBlockState(pos).isReplaceable()) {
                    return ActionResult.FAIL;
                }
            }
        }

        SupporterFlagBlock canvas = (SupporterFlagBlock) getBlock();
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                BlockPos pos = anchor.offset(across, column).up(height - 1 - row);
                BlockState state = canvas.getDefaultState()
                        .with(SupporterFlagBlock.FACING, facing)
                        .with(SupporterFlagBlock.PART, row * width + column);
                world.setBlockState(pos, state, Block.NOTIFY_ALL);
            }
        }

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().creativeMode) {
            context.getStack().decrement(1);
        }
        return ActionResult.CONSUME;
    }
}
