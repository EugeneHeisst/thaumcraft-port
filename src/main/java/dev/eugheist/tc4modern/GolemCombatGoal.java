package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;

/** Original guard predicates and butcher population floor, using modern navigation. */
final class GolemCombatGoal extends Goal {
    final StrawGolem g;int cooldown,dartTime;boolean darting;
    GolemCombatGoal(StrawGolem g){this.g=g;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    boolean valid(LivingEntity e){
        if(e==g||!e.isAlive()||e instanceof StrawGolem||e instanceof net.minecraft.world.entity.npc.Villager||e instanceof net.minecraft.world.entity.ambient.Bat)return false;
        if(e instanceof Player p&&(p.getUUID().equals(g.owner)||p.isCreative()||p.isSpectator()))return false;
        if(!g.isWithinRestriction(e.blockPosition()))return false;
        if(g.core()==9)return animal(e)&&!(e instanceof Animal a&&a.isBaby())&&!(e instanceof TamableAnimal t&&t.isTame());
        int disabled=g.getEntityData().get(StrawGolem.ATTACK_FLAGS);
        if(e instanceof Creeper)return g.upgrades(4)>0&&(disabled&8)==0;
        if(e instanceof Enemy)return (disabled&1)==0;
        if(g.upgrades(4)==0)return false;
        if(e instanceof Player)return (disabled&4)==0;
        return (disabled&2)==0&&animal(e)&&!(e instanceof TamableAnimal t&&t.isTame());
    }
    static boolean animal(LivingEntity e){return e instanceof Animal||e instanceof net.minecraft.world.entity.animal.WaterAnimal;}
    boolean enough(LivingEntity e){return g.core()!=9||g.level().getEntitiesOfClass(LivingEntity.class,g.getBoundingBox().inflate(g.range(),4,g.range()),a->a.getClass()==e.getClass()&&valid(a)).size()>2;}
    @Override public boolean canUse(){
        if(!g.working()||(g.core()!=4&&g.core()!=9))return false;
        var targets=g.level().getEntitiesOfClass(LivingEntity.class,g.getBoundingBox().inflate(g.range(),4,g.range()),e->valid(e)&&g.getSensing().hasLineOfSight(e));
        targets.sort(g.core()==9?Comparator.comparingInt((LivingEntity e)->e.tickCount).reversed():Comparator.comparingDouble(g::distanceToSqr));
        for(var target:targets)if(enough(target)){g.setTarget(target);return true;}return false;
    }
    @Override public boolean canContinueToUse(){return (g.core()==4||g.core()==9)&&g.working()&&g.getTarget()!=null&&valid(g.getTarget())&&enough(g.getTarget());}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void stop(){g.setTarget(null);g.getNavigation().stop();}
    /** AIDartAttack (priority above melee): beyond 3 blocks a dart-launcher golem chases and fires every 30-8*air ticks within 80% of its range. */
    int dartMax(){return 30-g.upgrades(0)*8;}
    @Override public void start(){dartTime=dartMax()/2;darting=false;}
    @Override public void tick(){var target=g.getTarget();if(target==null)return;if(cooldown>0)cooldown--;double dist=g.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ());
        if(g.deco('R')&&dist>=9){darting=true;if(g.tickCount%5==0)g.getNavigation().moveTo(target,1);if(g.getSensing().hasLineOfSight(target)){g.getLookControl().setLookAt(target,30,30);dartTime=Math.max(dartTime-1,0);float r=g.range()*.8f;if(dartTime<=0&&dist<=r*r){g.shootDart(target);dartTime=dartMax();}}return;}
        if(darting){darting=false;dartTime=dartMax()/2;}
        g.getLookControl().setLookAt(target,30,30);if(g.tickCount%5==0)g.getNavigation().moveTo(target,1);double reach=target.getBbWidth()*target.getBbWidth()*4+1;if(g.distanceToSqr(target)<=reach&&cooldown==0){cooldown=g.attackSpeed();g.act();g.doHurtTarget(target);}}
}
