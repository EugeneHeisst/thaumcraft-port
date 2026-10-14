package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;

/** AIFish (core 11): cast at random nearby surface water, wait 300-500 ticks for a quality-driven bite, throw the catch back towards the golem. */
final class GolemFishGoal extends Goal {
    record Loot(ItemStack stack,int weight,float damage,boolean enchant){}
    static final List<Loot> CRAP=List.of(new Loot(new ItemStack(Items.LEATHER_BOOTS),10,.9f,false),new Loot(new ItemStack(Items.LEATHER),10,0,false),new Loot(new ItemStack(Items.BONE),10,0,false),new Loot(new ItemStack(Items.POTION),10,0,false),new Loot(new ItemStack(Items.STRING),5,0,false),new Loot(new ItemStack(Items.FISHING_ROD),2,.9f,false),new Loot(new ItemStack(Items.BOWL),10,0,false),new Loot(new ItemStack(Items.STICK),5,0,false),new Loot(new ItemStack(Items.INK_SAC,10),5,0,false),new Loot(new ItemStack(Blocks.TRIPWIRE_HOOK),10,0,false),new Loot(new ItemStack(Items.ROTTEN_FLESH),10,0,false));
    static final List<Loot> RARE=List.of(new Loot(new ItemStack(Blocks.LILY_PAD),1,0,false),new Loot(new ItemStack(Items.NAME_TAG),1,0,false),new Loot(new ItemStack(Items.SADDLE),1,0,false),new Loot(new ItemStack(Items.BOW),1,.25f,true),new Loot(new ItemStack(Items.FISHING_ROD),1,.25f,true),new Loot(new ItemStack(Items.BOOK),1,0,true));
    static final List<Loot> FISH=List.of(new Loot(new ItemStack(Items.COD),60,0,false),new Loot(new ItemStack(Items.SALMON),25,0,false),new Loot(new ItemStack(Items.TROPICAL_FISH),2,0,false),new Loot(new ItemStack(Items.PUFFERFISH),13,0,false));
    final StrawGolem g;float quality;int count;BlockPos target;GolemBobber bobber;static int casts,catches;
    GolemFishGoal(StrawGolem g){this.g=g;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    boolean active(){return g.core()==11&&g.working();}
    boolean water(BlockPos p){return g.level().getFluidState(p).is(FluidTags.WATER);}
    /** The four horizontal neighbours that are open water: +3e-5 each, +3e-5 under open sky, +1.5e-5 per water block up to 3 deep. */
    float quality(BlockPos at,float open,float sky,float deep){float q=0;for(var d:Direction.Plane.HORIZONTAL){var n=at.relative(d);if(!water(n)||!g.level().isEmptyBlock(n.above()))continue;q+=open;if(g.level().canSeeSky(n.above()))q+=sky;for(int depth=1;depth<=3;depth++)if(water(n.below(depth)))q+=deep;}return q;}
    @Override public boolean canUse(){
        if(!active()||target!=null||count>0||g.tickCount%GolemConfig.delay()>0||!g.getNavigation().isDone())return false;
        if(bobber!=null)bobber.discard();var water=find();if(water==null)return false;target=water;quality=quality(water,3e-5f,3e-5f,1.5e-5f);
        g.playSound(SoundEvents.FISHING_BOBBER_THROW,.5f,.4f/(g.getRandom().nextFloat()*.4f+.8f));bobber=new GolemBobber(g,water);casts++;return g.level().addFreshEntity(bobber);
    }
    @Override public boolean canContinueToUse(){return active()&&bobber!=null&&bobber.isAlive()&&target!=null&&count-->0;}
    @Override public void start(){count=300+g.getRandom().nextInt(200);g.act();}
    @Override public void stop(){if(bobber!=null)bobber.discard();target=null;count=-1;}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void tick(){
        if(target==null)return;var l=g.level();var r=g.getRandom();g.getLookControl().setLookAt(target.getX()+.5,target.getY()+1,target.getZ()+.5,30,30);
        if(r.nextFloat()>=quality+g.material().strength*1.5e-4f)return;
        g.act();int amount=1;if(g.upgrades(0)>0&&r.nextInt(10)<g.upgrades(0))amount++;
        for(int a=0;a<amount;a++){var fish=result();
            if(g.upgrades(3)>0&&l instanceof ServerLevel server){var cooked=server.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(fish),server).map(h->h.value().getResultItem(server.registryAccess()));if(cooked.isPresent()&&!cooked.get().isEmpty())fish=cooked.get().copy();}
            var item=new ItemEntity(l,target.getX()+.5,target.getY()+1,target.getZ()+.5,fish);if(g.upgrades(3)>0)item.igniteForSeconds(2);item.setPickUpDelay(20);
            /* original arithmetic, sign quirk included: golem + jitter - target + 0.5 */
            double dx=g.getX()+r.nextFloat()-r.nextFloat()-target.getX()+.5,dy=g.getY()-target.getY()+1,dz=g.getZ()+r.nextFloat()-r.nextFloat()-target.getZ()+.5,d=Math.sqrt(dx*dx+dy*dy+dz*dz);
            item.setDeltaMovement(dx*.1,dy*.1+Math.sqrt(d)*.08,dz*.1);l.addFreshEntity(item);catches++;}
        if(bobber!=null){bobber.playSound(SoundEvents.FISHING_BOBBER_SPLASH,.15f,1+(r.nextFloat()-r.nextFloat())*.4f);if(l instanceof ServerLevel server)server.sendParticles(ParticleTypes.SPLASH,bobber.getX(),bobber.getY()+.5,bobber.getZ(),20+r.nextInt(20),.1,0,.1,0);bobber.discard();}
        target=null;
    }
    /** findWater: distance*2 random probes in a box of +-ceil(range/2) around home (half that vertically) for water with air above. */
    BlockPos find(){float distance=Mth.ceil(g.range()/2f);var r=g.getRandom();var home=g.home;for(int i=0;i<distance*2;i++){var p=new BlockPos((int)(home.getX()+r.nextInt((int)(1+distance*2))-distance),(int)(home.getY()+r.nextInt((int)(1+distance))-distance/2),(int)(home.getZ()+r.nextInt((int)(1+distance*2))-distance));if(g.level().getFluidState(p).is(FluidTags.WATER)&&g.level().isEmptyBlock(p.above()))return p;}return null;}
    ItemStack result(){
        var r=g.getRandom();float f=r.nextFloat(),junk=.1f-g.upgrades(5)*.025f,treasure=.05f+g.upgrades(4)*.0125f;
        for(var d:Direction.Plane.HORIZONTAL){var n=target.relative(d);if(!water(n)||!g.level().isEmptyBlock(n.above()))continue;junk-=.005f;treasure+=.00125f;if(g.level().canSeeSky(n.above())){junk-=.005f;treasure+=.00125f;}for(int depth=1;depth<=3;depth++)if(water(n.below(depth)))treasure+=.001f;}
        junk=Mth.clamp(junk,0,1);treasure=Mth.clamp(treasure,0,1);
        if(f<junk)return roll(CRAP);if(f-junk<treasure)return roll(RARE);return roll(FISH);
    }
    /** WeightedRandomFishable: random damage up to the given share of durability, optional level-30 enchantment. */
    ItemStack roll(List<Loot> table){var r=g.getRandom();int total=table.stream().mapToInt(Loot::weight).sum(),pick=r.nextInt(total);Loot loot=table.get(0);for(var e:table){pick-=e.weight();if(pick<0){loot=e;break;}}
        var stack=loot.stack().copy();if(loot.damage()>0&&stack.isDamageableItem()){int max=(int)(stack.getMaxDamage()*loot.damage());int damage=stack.getMaxDamage()-r.nextInt(r.nextInt(Math.max(1,max))+1);stack.setDamageValue(Mth.clamp(damage,1,max));}
        if(loot.enchant())stack=net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(r,stack,30,g.level().registryAccess(),Optional.empty());return stack;}
}
