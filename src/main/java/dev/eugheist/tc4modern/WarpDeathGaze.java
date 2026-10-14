package dev.eugheist.tc4modern;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Original server gaze cadence and finite cone, independent of the warp-event roll. */
@EventBusSubscriber(modid = TC4Modern.MODID)
public final class WarpDeathGaze {
    private WarpDeathGaze() {}
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) return;
        var effect = player.getEffect(WarpEffects.DEATH_GAZE);
        if (effect == null) return;
        int range = Math.min(8 + effect.getAmplifier() * 3, 24);
        for (var entity : player.level().getEntities(player, player.getBoundingBox().inflate(range))) {
            if (!entity.isPickable() || !(entity instanceof LivingEntity target) || !target.isAlive()
                || !inCone(player, target, range) || !player.hasLineOfSight(target)
                || target instanceof Player && !player.server.isPvpAllowed() || target.hasEffect(MobEffects.WITHER)) continue;
            target.setLastHurtByMob(player);
            target.setLastHurtByPlayer(player);
            if (target instanceof PathfinderMob mob) mob.setTarget(player);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80));
        }
    }
    private static boolean inCone(Player viewer, LivingEntity target, int range) {
        Vec3 offset = new Vec3(target.getX(), target.getBoundingBox().minY + target.getBbHeight() / 2f, target.getZ())
            .subtract(viewer.getEyePosition());
        return inCone(offset, viewer.getLookAngle().scale(range));
    }
    static boolean inCone(Vec3 offset, Vec3 axis) {
        double dot = offset.dot(axis);
        return dot / offset.length() / axis.length() > Math.cos(0.75f / 2f)
            && dot / axis.length() < axis.length();
    }
}
