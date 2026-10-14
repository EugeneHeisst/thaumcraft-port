package dev.eugheist.tc4modern;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** EntityDart: the dart-launcher golem's projectile, a small arrow that is never picked up. */
public final class GolemDart extends AbstractArrow {
    boolean first=true;
    GolemDart(EntityType<? extends GolemDart> type,Level l){super(type,l);pickup=Pickup.DISALLOWED;}
    /** Original aiming: from eye height-0.1, a fifth of a block towards the target, lifted by distance*0.2. */
    GolemDart(StrawGolem shooter,LivingEntity target,float velocity,float inaccuracy){
        this(Golemancy.DART.get(),shooter.level());setOwner(shooter);
        double y=shooter.getY()+shooter.getEyeHeight()-.1,dx=target.getX()-shooter.getX(),dy=target.getY()+target.getEyeHeight()-.7-y,dz=target.getZ()-shooter.getZ(),flat=Math.sqrt(dx*dx+dz*dz);
        setPos(shooter.getX(),y,shooter.getZ());
        if(flat>=1e-7){moveTo(shooter.getX()+dx/flat/5,y,shooter.getZ()+dz/flat/5,(float)(Math.atan2(dz,dx)*180/Math.PI)-90,(float)-(Math.atan2(dy,flat)*180/Math.PI));shoot(dx,dy+flat*.2f,dz,velocity,inaccuracy);}
    }
    @Override protected ItemStack getDefaultPickupItem(){return new ItemStack(net.minecraft.world.item.Items.ARROW);}
    @Override public void playerTouch(Player p){}
    @Override public void tick(){
        if(first&&level().isClientSide){first=false;var v=getDeltaMovement();for(int a=0;a<5;a++)level().addParticle(ParticleTypes.SMOKE,getX()-v.x/1.5,getY()-v.y/1.5,getZ()-v.z/1.5,v.x/9+random.nextGaussian()*.01,v.y/9+random.nextGaussian()*.01,v.z/9+random.nextGaussian()*.01);}
        super.tick();
    }
}
