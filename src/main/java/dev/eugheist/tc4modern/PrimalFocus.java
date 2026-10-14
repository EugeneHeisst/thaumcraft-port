package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
import thaumcraft.api.aspects.*;
import java.util.Random;

/** ItemFocusPrimal and EntityPrimalOrb, including the original squared-distance seeker and rare world effects. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class PrimalFocus {
    private PrimalFocus() {}
    public static final DeferredHolder<Item,Item> FOCUS=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("focus_primal"));
    public static final DeferredHolder<EntityType<?>,EntityType<Orb>> ORB=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("primal_orb"));
    @SubscribeEvent static void register(RegisterEvent e) {
        e.register(Registries.ITEM,h->h.register(FOCUS.getId(),new Focus()));
        e.register(Registries.ENTITY_TYPE,h->h.register(ORB.getId(),EntityType.Builder.<Orb>of(Orb::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(1).build("primal_orb")));
    }
    public static final class Focus extends FociContent.Focus {
        @Override public String getSortingHelperBase(){return "FP";}
        @Override public int getActivationCooldown(ItemStack focus){return 500;}
        @Override public int getFocusColor(ItemStack focus){return 10854849;}
        @Override public String getDepthTexture(ItemStack focus){return "focus_primal_depth";}
        @Override public AspectList getVisCost(ItemStack stack){
            var r=new Random(System.currentTimeMillis()/200L);
            return new AspectList().add(Aspect.WATER,50+r.nextInt(5)*50).add(Aspect.AIR,50+r.nextInt(5)*50)
                .add(Aspect.EARTH,50+r.nextInt(5)*50).add(Aspect.FIRE,50+r.nextInt(5)*50).add(Aspect.ORDER,50+r.nextInt(5)*50).add(Aspect.ENTROPY,50+r.nextInt(5)*50);
        }
        @Override public int[] getPossibleUpgradesByRank(ItemStack focus,int rank){return FociContent.byRank(rank,new int[]{1},new int[]{1},new int[]{1,16},new int[]{1},new int[]{1});}
        @Override public boolean onFocusRightClick(ItemStack wand,Level level,Player player){
            var orb=new Orb(level,player,isUpgradedWith(FociContent.getFocusItem(wand),16));
            if(!level.isClientSide && FociContent.consumeAllVis(wand,player,getVisCost(wand),true)) {
                level.addFreshEntity(orb);level.playSound(null,orb,FociContent.sound("ice"),SoundSource.PLAYERS,.3f,.8f+level.random.nextFloat()*.1f);
            }
            player.swing(InteractionHand.MAIN_HAND);return true;
        }
    }
    public static final class Orb extends ThrowableProjectile {
        private static final EntityDataAccessor<Boolean> SEEKER=SynchedEntityData.defineId(Orb.class,EntityDataSerializers.BOOLEAN);
        private static final EntityDataAccessor<Integer> OWNER_ID=SynchedEntityData.defineId(Orb.class,EntityDataSerializers.INT);
        private int count;
        public Orb(EntityType<? extends Orb> type,Level level){super(type,level);}
        public Orb(Level level,LivingEntity player,boolean seeker){
            this(ORB.get(),level);setOwner(player);entityData.set(SEEKER,seeker);entityData.set(OWNER_ID,player.getId());FociEntities.launch(this,player,.5f,1);
        }
        @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(SEEKER,false);b.define(OWNER_ID,0);}
        public boolean seeker(){return entityData.get(SEEKER);}
        @Override protected double getDefaultGravity(){return .001f;}
        private boolean inPortal(){var b=level().getBlockState(blockPosition()).getBlock();return b instanceof NetherPortalBlock || b instanceof EndPortalBlock;}
        @Override public void tick(){
            ++count;
            // Material.portal (not lava). Original passes an ENTITY hit here, so its block-only larger blast is not selected.
            if(inPortal())onHit(new EntityHitResult(this));
            if(level().isClientSide)PrimalFocusClient.trail(this);
            var r=new Random(getId()+count);
            if(tickCount>20){
                var v=getDeltaMovement();
                if(!seeker())setDeltaMovement(v.add((r.nextFloat()-r.nextFloat())*.01f,(r.nextFloat()-r.nextFloat())*.01f,(r.nextFloat()-r.nextFloat())*.01f));
                else {
                    LivingEntity target=null;double distance=Double.MAX_VALUE;
                    for(var e:level().getEntitiesOfClass(LivingEntity.class,new AABB(position(),position()).inflate(16))) {
                        double d=distanceToSqr(e);if(e.getId()==entityData.get(OWNER_ID)||e.isRemoved()||d>=distance)continue;distance=d;target=e;
                    }
                    if(target!=null){
                        // Deliberately divide by distance SQUARED, matching EntityPrimalOrb, with no LOS/faction filter.
                        double dx=(target.getX()-getX())/distance,dy=(target.getBoundingBox().minY+target.getBbHeight()*.9-getY())/distance,dz=(target.getZ()-getZ())/distance;
                        setDeltaMovement(Mth.clamp((float)(v.x+dx*.2),-.2f,.2f),Mth.clamp((float)(v.y+dy*.2),-.2f,.2f),Mth.clamp((float)(v.z+dz*.2),-.2f,.2f));
                    }
                }
            }
            super.tick();if(tickCount>5000)discard();
        }
        @Override protected void onHit(HitResult hit){
            if(level().isClientSide){PrimalFocusClient.burst(this);return;}
            float chance=1,blast=2;
            if(hit.getType()==HitResult.Type.BLOCK&&inPortal()){blast=4;chance=10;}
            // Original newExplosion(null,..., true) destroys terrain, independent of mobGriefing.
            level().explode(null,getX(),getY(),getZ(),blast,Level.ExplosionInteraction.TNT);
            if(!seeker() && random.nextInt(100)<=chance){
                if(random.nextBoolean())taintSplosion();
                else {
                    // MovingObjectPosition(Entity) leaves block coordinates at 0,0,0 in 1.7.10.
                    BlockPos at=hit instanceof BlockHitResult b?b.getBlockPos():BlockPos.ZERO;
                    AuraNodeFeature.createRandomNodeAt((ServerLevel)level(),at,random,false,false,true);
                }
            }
            discard();
        }
        void taintSplosion(){
            var level=(ServerLevel)level();int x=(int)getX(),z=(int)getZ();
            for(int a=0;a<10;a++){
                int xx=x+(int)(random.nextFloat()-random.nextFloat()*6f),zz=z+(int)(random.nextFloat()-random.nextFloat()*6f);
                var at=new BlockPos(xx,(int)getY(),zz);
                if(!random.nextBoolean()||TaintBiomes.isTainted(level,at))continue;
                TaintBiomes.spread(level,at);
                int yy=level.getHeight(Heightmap.Types.MOTION_BLOCKING,xx,zz);
                var surface=new BlockPos(xx,yy,zz);
                if(!level.isEmptyBlock(surface.below()))level.setBlock(surface,TC4Modern.TAINT_FIBRES.get().defaultBlockState(),3);
            }
        }
        // Original only sends seeker/oi in spawn data, not NBT; preserve their reset on world reload.
    }
}
