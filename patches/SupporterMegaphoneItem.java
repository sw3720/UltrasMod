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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player-controlled bulk supporter choreography.
 * Sneak-use cycles the active action; normal use applies it to every visible supporter.
 */
public class SupporterMegaphoneItem extends Item {
    private static final double VIEW_RANGE = 512.0;
    private static final double MIN_VIEW_DOT = 0.70;

    private enum CheerMode {
        CLAP("박수"),
        JUMP("점프"),
        BIG_FLAG("대형기 흔들기"),
        SCARF("머플러 흔들기"),
        STOP("정지");

        private final String label;
        CheerMode(String label) { this.label = label; }
    }

    private static final Map<UUID, Integer> PLAYER_MODES = new ConcurrentHashMap<>();
    private final SoundEvent sound;

    public SupporterMegaphoneItem(SoundEvent sound, Settings settings) {
        super(settings);
        this.sound = sound;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) return ActionResult.SUCCESS;

        int selected = PLAYER_MODES.getOrDefault(user.getUuid(), 0);
        if (user.isSneaking()) {
            selected = (selected + 1) % CheerMode.values().length;
            PLAYER_MODES.put(user.getUuid(), selected);
            user.sendMessage(Text.literal(
                    "확성기 동작 선택: " + CheerMode.values()[selected].label
                            + " · 일반 우클릭으로 시야 안 응원단에 적용"
            ), true);
            playMegaphone(world, user);
            user.getItemCooldownManager().set(user.getStackInHand(hand), 5);
            return ActionResult.SUCCESS;
        }

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
            user.getItemCooldownManager().set(user.getStackInHand(hand), 5);
            return ActionResult.SUCCESS;
        }

        CheerMode mode = CheerMode.values()[selected];
        for (ArmorStandEntity stand : visible) {
            clearCheering(stand);
            if (mode == CheerMode.STOP) continue;

            stand.addCommandTag("ultrasmod_cheering");
            switch (mode) {
                case CLAP -> stand.addCommandTag("ultrasmod_clap");
                case JUMP -> {
                    stand.addCommandTag("ultrasmod_jump");
                    stand.setNoGravity(true);
                }
                case BIG_FLAG -> {
                    stand.addCommandTag("ultrasmod_flagwave");
                    stand.equipStack(EquipmentSlot.MAINHAND,
                            new ItemStack(UltrasMod.handFlagForSupporter(stand)));
                }
                case SCARF -> {
                    stand.addCommandTag("ultrasmod_scarfwave");
                    stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(UltrasMod.SUPPORTER_SCARF));
                    stand.equipStack(EquipmentSlot.OFFHAND, new ItemStack(UltrasMod.SUPPORTER_SCARF));
                }
                case STOP -> { }
            }
            stand.setPosition(stand.getX(), Math.floor(stand.getY()), stand.getZ());
        }

        user.sendMessage(Text.literal(
                "시야 안 응원 거치대 " + visible.size() + "개에 '" + mode.label + "' 적용"
        ), true);
        playMegaphone(world, user);
        user.getItemCooldownManager().set(user.getStackInHand(hand), 5);
        return ActionResult.SUCCESS;
    }

    private void playMegaphone(World world, PlayerEntity user) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                sound, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private static void clearCheering(ArmorStandEntity stand) {
        stand.removeCommandTag("ultrasmod_cheering");
        stand.removeCommandTag("ultrasmod_jump");
        stand.removeCommandTag("ultrasmod_clap");
        stand.removeCommandTag("ultrasmod_flagwave");
        stand.removeCommandTag("ultrasmod_scarfwave");
        stand.setNoGravity(false);
        stand.setPosition(stand.getX(), Math.floor(stand.getY()), stand.getZ());
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
