package thaumcraft.common.entities.monster;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.Attributes;
/** Original tiny tainted spider properties and loot on modern Spider navigation. */
public final class EntityTaintSpider extends net.minecraft.world.entity.monster.Spider implements thaumcraft.api.entities.ITaintedMob {
    public static java.util.function.Supplier<net.minecraft.world.item.Item> goo,tendril;
    public EntityTaintSpider(EntityType<? extends EntityTaintSpider> type,Level level){super(type,level);xpReward=2;}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){return net.minecraft.world.entity.monster.Spider.createAttributes().add(Attributes.MAX_HEALTH,5).add(Attributes.ATTACK_DAMAGE,2).add(Attributes.FOLLOW_RANGE,12);}
    @Override protected void registerGoals(){
        super.registerGoals();
        goalSelector.removeAllGoals(goal->goal instanceof net.minecraft.world.entity.ai.goal.AvoidEntityGoal<?>||goal instanceof net.minecraft.world.entity.ai.goal.LeapAtTargetGoal);
        targetSelector.removeAllGoals(goal->true);
        targetSelector.addGoal(1,new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        targetSelector.addGoal(2,new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,net.minecraft.world.entity.player.Player.class,true));
        goalSelector.addGoal(3,new net.minecraft.world.entity.ai.goal.Goal(){
            {setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.JUMP));}
            @Override public boolean requiresUpdateEveryTick(){return true;}
            @Override public boolean canUse(){var target=getTarget();return target!=null&&distanceTo(target)>2&&distanceTo(target)<6&&random.nextInt(10)==0&&onGround();}
            @Override public boolean canContinueToUse(){return !onGround();}
            @Override public void start(){
                var target=getTarget();double dx=target.getX()-getX(),dz=target.getZ()-getZ();float length=net.minecraft.util.Mth.sqrt((float)(dx*dx+dz*dz));
                var old=getDeltaMovement();setDeltaMovement(dx/length*.5*(double).8f+old.x*.2,.4f,dz/length*.5*(double).8f+old.z*.2);
            }
        });
    }
    @Override protected float getJumpPower(){return .7f;}
    @Override protected float getSoundVolume(){return .1f;}
    @Override protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean recent){
        if(random.nextInt(6)==0)spawnAtLocation(new net.minecraft.world.item.ItemStack(random.nextBoolean()?goo.get():tendril.get()),getBbHeight()/2);
    }
}
