package thaumcraft.common.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;

/** Original EntitySpecialItem buoyancy and fire immunity, with the modern entity constructor. */
public final class EntitySpecialItem extends ItemEntity {
    public EntitySpecialItem(EntityType<? extends ItemEntity> type, Level level) { super(type, level); }
    @Override public void tick() {
        var motion = getDeltaMovement();
        double y = motion.y;
        if (y > 0) y *= (double).9f;
        setDeltaMovement(motion.x, y + (double).04f, motion.z);
        super.tick();
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) return false;
        return super.hurt(source, amount);
    }
}
