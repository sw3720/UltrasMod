package kr.ultrasmod.item;

import kr.ultrasmod.block.SupporterFlagBlock;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SupporterCanvasItem extends BlockItem {
    private static final int DESIGN_SLOTS = 64;
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
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(world.getBlockState(context.getBlockPos()).getBlock() instanceof FenceBlock)
                || context.getSide().getAxis() == Direction.Axis.Y) {
            if (context.getPlayer() != null) context.getPlayer().sendMessage(
                    Text.literal("울타리의 수직 면을 클릭하면 깃대와 고정대가 연결된 깃발 한 세트가 설치됩니다."), false);
            return ActionResult.FAIL;
        }
        BlockPos anchor = context.getBlockPos().offset(context.getSide());
        Direction facing = context.getHorizontalPlayerFacing().getOpposite();
        Direction across = facing.rotateYCounterclockwise();

        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                BlockPos pos = anchor.offset(across, column).up(height - 1 - row);
                if (!world.getBlockState(pos).isReplaceable()) return ActionResult.FAIL;
            }
        }

        int designId;
        try {
            designId = reserveDesignSlot();
        } catch (IOException | NumberFormatException exception) {
            if (context.getPlayer() != null) context.getPlayer().sendMessage(
                    Text.literal("깃발 디자인 슬롯 저장 실패: " + exception.getMessage()), false);
            return ActionResult.FAIL;
        }
        if (designId < 0) {
            if (context.getPlayer() != null) context.getPlayer().sendMessage(
                    Text.literal("개별 디자인 슬롯 64개를 모두 사용했습니다."), false);
            return ActionResult.FAIL;
        }

        SupporterFlagBlock canvas = (SupporterFlagBlock) getBlock();
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                BlockPos pos = anchor.offset(across, column).up(height - 1 - row);
                BlockState state = canvas.getDefaultState()
                        .with(SupporterFlagBlock.FACING, facing)
                        .with(SupporterFlagBlock.PART, row * width + column)
                        .with(SupporterFlagBlock.DESIGN, designId);
                world.setBlockState(pos, state, Block.NOTIFY_ALL);
            }
        }
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().creativeMode)
            context.getStack().decrement(1);
        return ActionResult.CONSUME;
    }

    /** Places one complete pole-mounted flag assembly; each installation receives its own editable design slot. */
    private static int reserveDesignSlot() throws IOException {
        Path counter = FabricLoader.getInstance().getConfigDir().resolve("ultrasmod-design-slot.txt");
        Files.createDirectories(counter.getParent());
        int next = Files.exists(counter) ? Integer.parseInt(Files.readString(counter).trim()) : 0;
        if (next >= DESIGN_SLOTS) return -1;
        Files.writeString(counter, Integer.toString(next + 1));
        return next;
    }
}
