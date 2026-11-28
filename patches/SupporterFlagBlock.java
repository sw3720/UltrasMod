package kr.ultrasmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class SupporterFlagBlock extends Block {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final IntProperty PART = IntProperty.of("part", 0, 35);
    public static final IntProperty DESIGN = IntProperty.of("design", 0, 63);

    public SupporterFlagBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH).with(PART, 0).with(DESIGN, 0));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
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
}
