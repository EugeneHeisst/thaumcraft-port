package dev.eugheist.tc4modern;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

/** BlockFluidPure with Forge1.7.10 BlockFluidClassic flow; sources never multiply. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class PureWater {
 public static final IntegerProperty DECAY=IntegerProperty.create("decay",0,7);
 public static final DeferredHolder<Block,PureBlock> BLOCK=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("purifying_fluid"));
 public static final DeferredHolder<Fluid,PureFluid> FLUID=DeferredHolder.create(Registries.FLUID,AlchemyContent.id("purifying_fluid"));
 public static final DeferredHolder<net.neoforged.neoforge.fluids.FluidType,net.neoforged.neoforge.fluids.FluidType> TYPE=DeferredHolder.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES,AlchemyContent.id("purifying_fluid"));
 public static final DeferredHolder<Item,Item> BUCKET=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("bucket_pure"));
 @SubscribeEvent static void register(RegisterEvent event){
  event.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES,h->h.register(TYPE.getId(),new net.neoforged.neoforge.fluids.FluidType(net.neoforged.neoforge.fluids.FluidType.Properties.create().lightLevel(10).density(1000).viscosity(1000).rarity(Rarity.RARE).canSwim(true).canDrown(true).canExtinguish(true))));
  event.register(Registries.FLUID,h->h.register(FLUID.getId(),new PureFluid()));event.register(Registries.BLOCK,h->h.register(BLOCK.getId(),new PureBlock()));event.register(Registries.ITEM,h->h.register(BUCKET.getId(),new PureBucket()));
 }
 public static final class PureBucket extends Item {
  PureBucket(){super(new Properties().stacksTo(1));}
  @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){var stack=player.getItemInHand(hand);var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.ANY);if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResultHolder.pass(stack);var pos=hit.getBlockPos().relative(hit.getDirection());var state=level.getBlockState(pos);if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),stack)||(!state.isAir()&&state.blocksMotion()))return InteractionResultHolder.fail(stack);if(!level.isClientSide){if(!state.isAir()&&state.getFluidState().isEmpty())level.destroyBlock(pos,true);level.setBlock(pos,BLOCK.get().defaultBlockState(),3);}return InteractionResultHolder.sidedSuccess(player.getAbilities().instabuild?stack:new ItemStack(Items.BUCKET),level.isClientSide);}
 }
 public static final class PureBlock extends Block implements BucketPickup {
    // Like LiquidBlock: hand targeting ignores the block; buckets use the fluid shape.
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(
            net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }
  PureBlock(){super(Properties.of().noCollission().noOcclusion().replaceable().strength(100).randomTicks().noLootTable().lightLevel(s->(int)((7-s.getValue(DECAY))/8f*10)));registerDefaultState(stateDefinition.any().setValue(DECAY,0));}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(DECAY);}
  @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
  @Override protected FluidState getFluidState(BlockState s){return FLUID.get().defaultFluidState().setValue(DECAY,s.getValue(DECAY));}
  @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){l.scheduleTick(p,this,5);}
  @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){l.scheduleTick(p,this,5);}
  @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){Hygiene.pureBubbles.accept(l,p);}
  @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity entity){if(!l.isClientSide&&s.getValue(DECAY)==0&&entity instanceof Player player&&!player.hasEffect(WarpEffects.WARP_WARD)){int warp=PlayerWarp.load(player).get(WarpState.Kind.PERMANENT),div=warp>0?Math.max(1,(int)Math.sqrt(warp)):1;player.addEffect(new net.minecraft.world.effect.MobEffectInstance(WarpEffects.WARP_WARD,Math.min(32000,200000/div),0,true,true));l.setBlock(p,Blocks.AIR.defaultBlockState(),3);}}
  @Override public ItemStack pickupBlock(Player player,LevelAccessor l,BlockPos p,BlockState s){if(s.getValue(DECAY)!=0)return ItemStack.EMPTY;l.setBlock(p,Blocks.AIR.defaultBlockState(),3);return new ItemStack(BUCKET.get());}
  @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getPickupSound(){return java.util.Optional.empty();}
  private int quantity(BlockGetter l,BlockPos p){var s=l.getBlockState(p);return s.isAir()?0:s.is(this)?8-s.getValue(DECAY):-1;}
  private boolean source(BlockGetter l,BlockPos p){return quantity(l,p)==8;}
  private boolean flowable(BlockGetter l,BlockPos p){var s=l.getBlockState(p);if(s.isAir()||s.is(this))return true;if(s.blocksMotion()||s.is(Blocks.NETHER_PORTAL))return false;if(!s.getFluidState().isEmpty()){var f=s.getFluidState();if(f.is(net.minecraft.tags.FluidTags.WATER)||f.is(net.minecraft.tags.FluidTags.LAVA))return false;return TYPE.get().getDensity()>f.getFluidType().getDensity();}return true;}
  private boolean displace(BlockGetter l,BlockPos p){return !l.getBlockState(p).is(this)&&flowable(l,p);}
  private void flow(ServerLevel l,BlockPos p,int decay){if(decay<0||!displace(l,p))return;var s=l.getBlockState(p);if(!s.isAir()&&s.getFluidState().isEmpty())Block.dropResources(s,l,p);l.setBlock(p,defaultBlockState().setValue(DECAY,decay),3);}
  private static final Direction[] SIDES={Direction.WEST,Direction.EAST,Direction.NORTH,Direction.SOUTH};
  private int flowCost(ServerLevel l,BlockPos p,int depth,Direction previous){int cost=1000;for(var d:SIDES){if(d==previous.getOpposite())continue;var next=p.relative(d);if(!flowable(l,next)||source(l,next))continue;if(flowable(l,next.below()))return depth;if(depth<4)cost=Math.min(cost,flowCost(l,next,depth+1,d));}return cost;}
  @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){tick(s,l,p,r);}
  @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){
   int remaining=8-s.getValue(DECAY);if(remaining<8){int expected=-101;var above=p.above();boolean fed=l.getBlockState(above).is(this);for(var d:SIDES)fed|=l.getBlockState(above.relative(d)).is(this);if(fed)expected=7;else {int max=-100;for(var d:SIDES){int q=quantity(l,p.relative(d));if(q>0)max=Math.max(max,q);}expected=max-1;}if(expected!=remaining){remaining=expected;if(expected<=0)l.setBlock(p,Blocks.AIR.defaultBlockState(),3);else {l.setBlock(p,s.setValue(DECAY,8-expected),3);l.scheduleTick(p,this,5);l.updateNeighborsAt(p,this);}}}else l.setBlock(p,s.setValue(DECAY,0),2);
   if(displace(l,p.below())){flow(l,p.below(),1);return;}int meta=8-remaining+1;if(meta>=8)return;boolean vertical=l.getBlockState(p.below()).is(this)||(l.getBlockState(p).is(this)&&flowable(l,p.below()));if(source(l,p)||!vertical){if(l.getBlockState(p.above()).is(this))meta=1;int[] costs=new int[4];int min=1000;for(int i=0;i<4;i++){var next=p.relative(SIDES[i]);costs[i]=!flowable(l,next)||source(l,next)?1000:flowable(l,next.below())?0:flowCost(l,next,1,SIDES[i]);min=Math.min(min,costs[i]);}for(int i=0;i<4;i++)if(costs[i]==min)flow(l,p.relative(SIDES[i]),meta);}
  }
 }
 public static final class PureFluid extends Fluid {
  @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid,FluidState> b){b.add(DECAY);}
  @Override public net.neoforged.neoforge.fluids.FluidType getFluidType(){return TYPE.get();}
  @Override public Item getBucket(){return BUCKET.get();}
  @Override protected boolean canBeReplacedWith(FluidState s,BlockGetter l,BlockPos p,Fluid f,Direction d){return false;}
  @Override protected Vec3 getFlow(BlockGetter l,BlockPos p,FluidState s){Vec3 flow=Vec3.ZERO;int decay=s.getValue(DECAY);for(var d:Direction.Plane.HORIZONTAL){var next=p.relative(d);var other=l.getFluidState(next);if(other.getType()==this)flow=flow.add(d.getStepX()*(other.getValue(DECAY)-decay),0,d.getStepZ()*(other.getValue(DECAY)-decay));else if(!l.getBlockState(next).blocksMotion()){other=l.getFluidState(next.below());if(other.getType()==this){int power=other.getValue(DECAY)-(decay-8);flow=flow.add(d.getStepX()*power,0,d.getStepZ()*power);}}}if(l.getFluidState(p.above()).getType()==this)for(var d:Direction.Plane.HORIZONTAL)if(l.getBlockState(p.relative(d)).blocksMotion()||l.getBlockState(p.relative(d).above()).blocksMotion()){flow=flow.normalize().add(0,-6,0);break;}return flow.normalize();}
  @Override public int getTickDelay(LevelReader l){return 5;}@Override protected float getExplosionResistance(){return 100;}
  @Override public float getHeight(FluidState s,BlockGetter l,BlockPos p){return l.getFluidState(p.above()).getType()==this?1:getOwnHeight(s);}
  @Override public float getOwnHeight(FluidState s){return (8-s.getValue(DECAY))*.875f/8;}
  @Override protected BlockState createLegacyBlock(FluidState s){return BLOCK.get().defaultBlockState().setValue(DECAY,s.getValue(DECAY));}
  @Override public boolean isSource(FluidState s){return s.getValue(DECAY)==0;}@Override public int getAmount(FluidState s){return 8-s.getValue(DECAY);}
  @Override public VoxelShape getShape(FluidState s,BlockGetter l,BlockPos p){return Shapes.box(0,0,0,1,getHeight(s,l,p),1);}
 }
}
