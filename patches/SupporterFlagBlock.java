package kr.ultrasmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** One tile of a multi-block supporter flag or banner canvas. */
public class SupporterFlagBlock extends Block {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final IntProperty PART = IntProperty.of("part", 0, 35);
    public static final IntProperty DESIGN = IntProperty.of("design", 0, 63);

    private static final int FLAG_WIDTH = 3;
    private static final int FLAG_HEIGHT = 5;
    private static final int MAX_LIFT = 4;
    private static final int MOTION_INTERVAL_TICKS = 6;
    private static final Map<World, Map<BlockPos, FlagMotion>> ACTIVE_MOTIONS = new WeakHashMap<>();

    public enum MotionToggleResult {
        STARTED, STOPPED, BLOCKED, INVALID
    }

    private static final class FlagMotion {
        private int lift;
        private int direction;

        private FlagMotion(int lift, int direction) {
            this.lift = lift;
            this.direction = direction;
        }
    }

    public SupporterFlagBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH).with(PART, 0).with(DESIGN, 0));
    }

    @Override
    public BlockState getPlacementState(net.minecraft.item.ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, DESIGN);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> VoxelShapes.cuboid(0.0, 0.0, 0.9375, 1.0, 1.0, 1.0);
            case SOUTH -> VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 1.0, 0.0625);
            case EAST -> VoxelShapes.cuboid(0.0, 0.0, 0.0, 0.0625, 1.0, 1.0);
            case WEST -> VoxelShapes.cuboid(0.9375, 0.0, 0.0, 1.0, 1.0, 1.0);
            default -> VoxelShapes.fullCube();
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    public MotionToggleResult toggleMotion(World world, BlockPos clickedPos) {
        BlockState clicked = world.getBlockState(clickedPos);
        if (!clicked.isOf(this)) return MotionToggleResult.INVALID;

        int part = clicked.get(PART);
        int row = part / FLAG_WIDTH;
        int column = part % FLAG_WIDTH;
        if (part >= FLAG_WIDTH * FLAG_HEIGHT) return MotionToggleResult.INVALID;

        Direction facing = clicked.get(FACING);
        Direction across = facing.rotateYClockwise();
        BlockPos topLeft = clickedPos.offset(across, -column).up(row);

        Map<BlockPos, FlagMotion> motions = ACTIVE_MOTIONS.computeIfAbsent(world, key -> new HashMap<>());
        if (motions.remove(topLeft) != null) {
            if (motions.isEmpty()) ACTIVE_MOTIONS.remove(world);
            return MotionToggleResult.STOPPED;
        }

        int design = clicked.get(DESIGN);
        if (!isCompleteAssembly(world, topLeft, facing, design)) return MotionToggleResult.INVALID;

        // The assembly rises by four blocks. Check the full upper travel corridor once.
        for (int lift = 1; lift <= MAX_LIFT; lift++) {
            for (int col = 0; col < FLAG_WIDTH; col++) {
                if (!world.getBlockState(topLeft.offset(across, col).up(lift)).isReplaceable()) {
                    return MotionToggleResult.BLOCKED;
                }
            }
        }

        motions.put(topLeft, new FlagMotion(0, 1));
        world.scheduleBlockTick(topLeft, this, MOTION_INTERVAL_TICKS);
        return MotionToggleResult.STARTED;
    }

    private boolean isCompleteAssembly(World world, BlockPos topLeft, Direction facing, int design) {
        Direction across = facing.rotateYClockwise();
        for (int row = 0; row < FLAG_HEIGHT; row++) {
            for (int column = 0; column < FLAG_WIDTH; column++) {
                BlockPos piecePos = topLeft.offset(across, column).down(row);
                BlockState piece = world.getBlockState(piecePos);
                int expectedPart = row * FLAG_WIDTH + column;
                if (!piece.isOf(this)
                        || piece.get(PART) != expectedPart
                        || piece.get(DESIGN) != design
                        || piece.get(FACING) != facing) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        Map<BlockPos, FlagMotion> motions = ACTIVE_MOTIONS.get(world);
        if (motions == null) return;

        FlagMotion motion = motions.get(pos);
        if (motion == null) return;
        if (!state.isOf(this) || state.get(PART) != 0) {
            stopMotion(world, motions, pos);
            return;
        }

        if (motion.direction > 0 && motion.lift >= MAX_LIFT) motion.direction = -1;
        else if (motion.direction < 0 && motion.lift <= 0) motion.direction = 1;
        int dy = motion.direction;

        Direction facing = state.get(FACING);
        Direction across = facing.rotateYClockwise();
        int design = state.get(DESIGN);
        List<BlockPos> sources = new ArrayList<>(FLAG_WIDTH * FLAG_HEIGHT);
        List<BlockState> states = new ArrayList<>(FLAG_WIDTH * FLAG_HEIGHT);

        for (int row = 0; row < FLAG_HEIGHT; row++) {
            for (int column = 0; column < FLAG_WIDTH; column++) {
                BlockPos source = pos.offset(across, column).down(row);
                BlockState piece = world.getBlockState(source);
                int expectedPart = row * FLAG_WIDTH + column;
                if (!piece.isOf(this)
                        || piece.get(PART) != expectedPart
                        || piece.get(DESIGN) != design
                        || piece.get(FACING) != facing) {
                    stopMotion(world, motions, pos);
                    return;
                }
                sources.add(source);
                states.add(piece);
            }
        }

        for (BlockPos source : sources) {
            BlockPos destination = source.up(dy);
            if (!sources.contains(destination) && !world.getBlockState(destination).isReplaceable()) {
                stopMotion(world, motions, pos);
                return;
            }
        }

        // Clear the old footprint first, then place the exact same 15 states one block away.
        for (BlockPos source : sources) {
            world.setBlockState(source, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
        for (int i = 0; i < sources.size(); i++) {
            world.setBlockState(sources.get(i).up(dy), states.get(i), Block.NOTIFY_ALL);
        }

        motions.remove(pos);
        motion.lift += dy;
        BlockPos newTopLeft = pos.up(dy);
        motions.put(newTopLeft, motion);
        world.scheduleBlockTick(newTopLeft, this, MOTION_INTERVAL_TICKS);
    }

    private static void stopMotion(ServerWorld world, Map<BlockPos, FlagMotion> motions, BlockPos pos) {
        motions.remove(pos);
        if (motions.isEmpty()) ACTIVE_MOTIONS.remove(world);
    }
}
