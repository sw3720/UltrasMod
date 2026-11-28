package kr.ultrasmod.item;

import kr.ultrasmod.UltrasMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/** Cycles visible supporter stands through all crowd-cheering modes. */
public class SupporterMegaphoneItem extends Item {
    private static final double VIEW_RANGE = 512.0;
    private static final double MIN_VIEW_DOT = 0.70;
    private static final int JUMP = 0;
    private static final int CLAP_2 = 1;
    private static final int HANDS = 2;
    private static final int SCARF = 3;
    private static final int SYNC_JUMP = 4;
    private static final int CLAP_3 = 5;
    private static final int CLAP_4 = 6;
    private static final String[] MODE_NAMES = {
            "엇박 점핑", "2박 박수", "오른팔 응원", "머플러 응원",
            "정박 점핑", "3박 박수", "4박 박수"
    };

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
        int nextMode = currentMode < 0 ? JUMP : (currentMode + 1) % MODE_NAMES.length;
        if (!(world instanceof ServerWorld serverWorld)) return ActionResult.PASS;
        for (ArmorStandEntity stand : visible) {
            UltrasMod.setCheeringMode(serverWorld, stand, nextMode);
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
        if (stand.getCommandTags().contains("ultrasmod_clap")) return CLAP_2;
        if (stand.getCommandTags().contains("ultrasmod_hands")) return HANDS;
        if (stand.getCommandTags().contains("ultrasmod_scarfwave")) return SCARF;
        if (stand.getCommandTags().contains("ultrasmod_sync_jump")) return SYNC_JUMP;
        if (stand.getCommandTags().contains("ultrasmod_clap3")) return CLAP_3;
        if (stand.getCommandTags().contains("ultrasmod_clap4")) return CLAP_4;
        return -1;
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
