package kr.ultrasmod.item;

import kr.ultrasmod.UltrasMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

/** Cycles visible supporter stands through four crowd-cheering modes. */
public class SupporterMegaphoneItem extends Item {
    private static final double VIEW_RANGE = 512.0;
    private static final double MIN_VIEW_DOT = 0.70;
    private static final int JUMP = 0;
    private static final int CLAP = 1;
    private static final int HANDS = 2;
    private static final int SCARF = 3;
    private static final String[] MODE_NAMES = {"점핑 응원", "박수 응원", "손 응원", "머플러 응원"};

    private final SoundEvent sound;

    public SupporterMegaphoneItem(SoundEvent sound, Settings settings) {
        super(settings);
        this.sound = sound;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) return ActionResult.SUCCESS;

        Vec3d eye = user.getEyePos();
        Vec3d look = user.getRotationVec(1.0F).normalize();
        Box searchArea = user.getBoundingBox().expand(VIEW_RANGE);

        List<ArmorStandEntity> visible = world.getEntitiesByType(
                EntityType.ARMOR_STAND, searchArea,
                stand -> stand.getCommandTags().contains("ultrasmod_supporter")
                        && isVisibleToPlayer(world, user, eye, look, stand)
        );

        if (visible.isEmpty()) {
            user.sendMessage(Text.literal("시야 안에 설정할 응원 거치대가 없습니다."), true);
            user.getItemCooldownManager().set(user.getStackInHand(hand), 8);
            return ActionResult.SUCCESS;
        }

        int currentMode = getMode(visible.get(0));
        int nextMode = currentMode < 0 ? JUMP : (currentMode + 1) % 4;
        for (ArmorStandEntity stand : visible) {
            applyMode(stand, nextMode);
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                UltrasMod.trackCheeringStand(serverWorld, stand);
            }
        }

        user.sendMessage(Text.literal(
                MODE_NAMES[nextMode] + " · 시야 안 응원 거치대 " + visible.size() + "개"
        ), true);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                sound, SoundCategory.PLAYERS, 1.0f, 1.0f);
        user.getItemCooldownManager().set(user.getStackInHand(hand), 10);
        return ActionResult.SUCCESS;
    }

    private static int getMode(ArmorStandEntity stand) {
        if (stand.getCommandTags().contains("ultrasmod_jump")) return JUMP;
        if (stand.getCommandTags().contains("ultrasmod_clap")) return CLAP;
        if (stand.getCommandTags().contains("ultrasmod_hands")) return HANDS;
        if (stand.getCommandTags().contains("ultrasmod_scarfwave")) return SCARF;
        return -1;
    }

    private static void applyMode(ArmorStandEntity stand, int mode) {
        clearCheering(stand);
        stand.addCommandTag("ultrasmod_cheering");

        switch (mode) {
            case JUMP -> {
                stand.addCommandTag("ultrasmod_jump");
                stand.setNoGravity(false);
                stand.setVelocity(0.0, 0.0, 0.0);
                stand.setLeftArmRotation(new EulerAngle(-2.55f, 0.0f, -0.28f));
                stand.setRightArmRotation(new EulerAngle(-2.55f, 0.0f, 0.28f));
            }
            case CLAP -> {
                stand.addCommandTag("ultrasmod_clap");
                stand.setNoGravity(true);
            }
            case HANDS -> {
                stand.addCommandTag("ultrasmod_hands");
                stand.setNoGravity(true);
            }
            case SCARF -> {
                stand.addCommandTag("ultrasmod_scarfwave");
                stand.setNoGravity(true);
                ItemStack scarf = new ItemStack(UltrasMod.SUPPORTER_SCARF);
                stand.equipStack(EquipmentSlot.MAINHAND, scarf.copy());
                stand.equipStack(EquipmentSlot.OFFHAND, scarf.copy());
            }
            default -> throw new IllegalArgumentException("Unknown cheering mode: " + mode);
        }
    }

    private static void clearCheering(ArmorStandEntity stand) {
        stand.removeCommandTag("ultrasmod_cheering");
        stand.removeCommandTag("ultrasmod_jump");
        stand.removeCommandTag("ultrasmod_clap");
        stand.removeCommandTag("ultrasmod_hands");
        stand.removeCommandTag("ultrasmod_flagwave");
        stand.removeCommandTag("ultrasmod_scarfwave");
        stand.setNoGravity(false);
        stand.setVelocity(0.0, 0.0, 0.0);
        stand.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        stand.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        stand.setLeftArmRotation(new EulerAngle(-1.25f, 0.0f, -0.18f));
        stand.setRightArmRotation(new EulerAngle(-1.25f, 0.0f, 0.18f));
    }

    private static boolean isVisibleToPlayer(World world, PlayerEntity user, Vec3d eye,
                                             Vec3d look, ArmorStandEntity stand) {
        Vec3d target = stand.getPos().add(0.0, stand.getHeight() * 0.65, 0.0);
        Vec3d offset = target.subtract(eye);
        double distance = offset.length();
        if (distance < 0.1 || distance > VIEW_RANGE) return false;
        if (look.dotProduct(offset.multiply(1.0 / distance)) < MIN_VIEW_DOT) return false;

        BlockHitResult hit = world.raycast(new RaycastContext(
                eye, target, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, user
        ));
        return hit.getType() == HitResult.Type.MISS
                || hit.getPos().squaredDistanceTo(eye) >= distance * distance - 0.25;
    }
}
