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

public class SupporterMegaphoneItem extends Item {
    private static final double VIEW_RANGE = 32.0;
    private static final double MIN_VIEW_DOT = 0.70;
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
        Box searchArea = user.getBoundingBox()
                .stretch(look.multiply(VIEW_RANGE))
                .expand(10.0, 7.0, 10.0);

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

        boolean stop = visible.stream()
                .anyMatch(stand -> stand.getCommandTags().contains("ultrasmod_cheering"));
        if (stop) {
            for (ArmorStandEntity stand : visible) clearCheering(stand);
            user.sendMessage(Text.literal("시야 안 응원단의 동작을 멈췄습니다."), true);
        } else {
            for (int i = visible.size() - 1; i > 0; i--) {
                int j = world.random.nextInt(i + 1);
                ArmorStandEntity swap = visible.get(i);
                visible.set(i, visible.get(j));
                visible.set(j, swap);
            }

            int jumpers = visible.size() >= 4
                    ? Math.max(1, Math.round(visible.size() * 0.15f)) : 0;
            int flagWavers = Math.round(visible.size() * 0.30f);
            int scarfWavers = Math.round(visible.size() * 0.25f);
            if (visible.size() > 1) {
                while (jumpers + flagWavers + scarfWavers > visible.size() - 1) {
                    if (scarfWavers > 0) scarfWavers--;
                    else if (flagWavers > 0) flagWavers--;
                    else if (jumpers > 0) jumpers--;
                }
            } else {
                jumpers = 0;
                flagWavers = 0;
                scarfWavers = 0;
            }

            for (int i = 0; i < visible.size(); i++) {
                ArmorStandEntity stand = visible.get(i);
                clearCheering(stand);
                stand.addCommandTag("ultrasmod_cheering");
                if (i < jumpers) {
                    stand.addCommandTag("ultrasmod_jump");
                    stand.setNoGravity(true);
                } else if (i < jumpers + flagWavers) {
                    stand.addCommandTag("ultrasmod_flagwave");
                    stand.equipStack(EquipmentSlot.MAINHAND,
                            new ItemStack(UltrasMod.handFlagForSupporter(stand)));
                } else if (i < jumpers + flagWavers + scarfWavers) {
                    stand.addCommandTag("ultrasmod_scarfwave");
                    stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(UltrasMod.SUPPORTER_SCARF));
                    stand.equipStack(EquipmentSlot.OFFHAND, new ItemStack(UltrasMod.SUPPORTER_SCARF));
                } else {
                    stand.addCommandTag("ultrasmod_clap");
                }
                stand.setPosition(stand.getX(), Math.floor(stand.getY()), stand.getZ());
            }

            user.sendMessage(Text.literal(
                    "응원 시작! 점프 " + jumpers + "명 · 대형기 " + flagWavers
                            + "명 · 머플러 " + scarfWavers + "명 · 박수 "
                            + (visible.size() - jumpers - flagWavers - scarfWavers) + "명"
            ), true);
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                sound, SoundCategory.PLAYERS, 1.0f, 1.0f);
        user.getItemCooldownManager().set(user.getStackInHand(hand), 10);
        return ActionResult.SUCCESS;
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
