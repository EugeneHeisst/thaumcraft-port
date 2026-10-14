package dev.eugheist.tc4modern;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
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
import thaumcraft.api.aspects.*;

/** BlockFluidDeath / ItemBucketDeath: four finite quanta, never infinite water sources. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class LiquidDeath {
    public static java.util.function.BiConsumer<Level,BlockPos> bubbles=(l,p)->{};
    public static final IntegerProperty AMOUNT=IntegerProperty.create("amount",1,4);
    public static final DeferredHolder<Block,DeathBlock> BLOCK=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("liquid_death"));
    public static final DeferredHolder<Fluid,DeathFluid> FLUID=DeferredHolder.create(Registries.FLUID,AlchemyContent.id("liquid_death"));
    public static final DeferredHolder<net.neoforged.neoforge.fluids.FluidType,net.neoforged.neoforge.fluids.FluidType> TYPE=DeferredHolder.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES,AlchemyContent.id("liquid_death"));
    public static final DeferredHolder<Item,Item> BUCKET=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("bucket_death"));
    public static final DeferredHolder<Item,Item> CRYSTAL=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("crystal_essence"));
    public static final ResourceKey<net.minecraft.world.damagesource.DamageType> DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,AlchemyContent.id("dissolve"));
    @SubscribeEvent static void register(RegisterEvent event){
        event.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES,h->h.register(TYPE.getId(),new net.neoforged.neoforge.fluids.FluidType(net.neoforged.neoforge.fluids.FluidType.Properties.create().lightLevel(8).viscosity(1500).density(1000).rarity(Rarity.RARE))));
        event.register(Registries.FLUID,h->h.register(FLUID.getId(),new DeathFluid()));
        event.register(Registries.BLOCK,h->h.register(BLOCK.getId(),new DeathBlock()));
        event.register(Registries.ITEM,h->{h.register(BUCKET.getId(),new DeathBucket());h.register(CRYSTAL.getId(),new Item(new Item.Properties()){
            @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){if(!level.isClientSide&&!stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)){var all=Aspect.aspects.values().toArray(Aspect[]::new);EssentiaItems.setAspects(stack,new AspectList().add(all[level.random.nextInt(all.length)],1));}}
            @Override public void onCraftedBy(ItemStack stack,Level level,Player player){inventoryTick(stack,level,player,0,false);}
            @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){EssentiaItems.tooltip(stack,lines);}
        });});
    }
    public static ItemStack crystal(Aspect aspect,int count){var stack=new ItemStack(CRYSTAL.get(),count);EssentiaItems.setAspects(stack,new AspectList().add(aspect,1));return stack;}
    public static final class DeathBucket extends Item {
        DeathBucket(){super(new Properties().stacksTo(1));}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
            var stack=player.getItemInHand(hand);var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.ANY);
            if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResultHolder.pass(stack);
            var pos=hit.getBlockPos().relative(hit.getDirection());var state=level.getBlockState(pos);
            if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),stack)||(!state.isAir()&&state.blocksMotion()))return InteractionResultHolder.fail(stack);
            if(!level.isClientSide){if(!state.isAir()&&state.getFluidState().isEmpty())level.destroyBlock(pos,true);level.setBlock(pos,BLOCK.get().defaultBlockState().setValue(AMOUNT,4),3);}
            return InteractionResultHolder.sidedSuccess(player.getAbilities().instabuild?stack:new ItemStack(Items.BUCKET),level.isClientSide);
        }
    }
    public static final class DeathBlock extends Block implements BucketPickup {
    // Like LiquidBlock: hand targeting ignores the block; buckets use the fluid shape.
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(
            net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }
        DeathBlock(){super(Properties.of().noCollission().noOcclusion().replaceable().strength(100).lightLevel(s->(s.getValue(AMOUNT)-1)*2).randomTicks().noLootTable());registerDefaultState(stateDefinition.any().setValue(AMOUNT,4));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(AMOUNT);}
        @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
        @Override protected FluidState getFluidState(BlockState state){return FLUID.get().defaultFluidState().setValue(AMOUNT,state.getValue(AMOUNT));}
        @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){level.scheduleTick(pos,this,7);}
        @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){level.scheduleTick(pos,this,7);}
        @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){bubbles.accept(level,pos);}
        @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){if(!level.isClientSide&&entity instanceof LivingEntity)entity.hurt(new net.minecraft.world.damagesource.DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE)),state.getValue(AMOUNT));}
        @Override public ItemStack pickupBlock(Player player,LevelAccessor level,BlockPos pos,BlockState state){if(state.getValue(AMOUNT)!=4)return ItemStack.EMPTY;level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);return new ItemStack(BUCKET.get());}
        @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getPickupSound(){return java.util.Optional.empty();}
        @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){tick(state,level,pos,random);}
        @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
            thaumcraft.common.blocks.FiniteFlux.tick(new thaumcraft.common.blocks.FiniteFlux.World(){
                public int quantity(int x,int y,int z){var s=level.getBlockState(new BlockPos(x,y,z));return s.isAir()?0:s.is(DeathBlock.this)?s.getValue(AMOUNT):-1;}
                public boolean displace(int x,int y,int z){var p=new BlockPos(x,y,z);var s=level.getBlockState(p);if(s.isAir())return true;if(s.is(DeathBlock.this)||s.blocksMotion()||s.is(Blocks.NETHER_PORTAL))return false;if(s.getBlock() instanceof FluxBlock)return y==pos.getY();if(s.getFluidState().isEmpty())Block.dropResources(s,level,p);return true;}
                public boolean outside(int y){return y<level.getMinBuildHeight()||y>=level.getMaxBuildHeight();}
                public void set(int x,int y,int z,int n){level.setBlock(new BlockPos(x,y,z),n==0?Blocks.AIR.defaultBlockState():defaultBlockState().setValue(AMOUNT,n),3);}
                public void schedule(int x,int y,int z){level.scheduleTick(new BlockPos(x,y,z),DeathBlock.this,7);}
                public boolean swapVertical(int x,int y,int z,int d,int n){var from=new BlockPos(x,y,z);var to=from.offset(0,d,0);var other=level.getBlockState(to);if(!(other.getBlock() instanceof FluxBlock flux))return false;level.setBlock(to,defaultBlockState().setValue(AMOUNT,n),3);level.setBlock(from,other,3);level.scheduleTick(to,DeathBlock.this,7);level.scheduleTick(from,flux,flux.direction>0?12:30);return true;}
            },pos.getX(),pos.getY(),pos.getZ(),-1,4,new java.util.random.RandomGenerator(){public long nextLong(){return random.nextLong();}public int nextInt(int bound){return random.nextInt(bound);}});
        }
    }
    public static final class DeathFluid extends Fluid {
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid,FluidState> b){b.add(AMOUNT);}
        @Override public net.neoforged.neoforge.fluids.FluidType getFluidType(){return TYPE.get();}
        @Override public Item getBucket(){return BUCKET.get();}
        @Override protected boolean canBeReplacedWith(FluidState s,BlockGetter l,BlockPos p,Fluid f,Direction d){return false;}
        private int quantity(BlockGetter level,BlockPos pos){var state=level.getBlockState(pos);return state.isAir()?0:state.is(BLOCK.get())?state.getValue(AMOUNT):-1;}
        @Override protected Vec3 getFlow(BlockGetter level,BlockPos pos,FluidState state){
            Vec3 flow=Vec3.ZERO;int decay=4-state.getValue(AMOUNT);
            for(Direction direction:Direction.Plane.HORIZONTAL){
                var other=pos.relative(direction);int otherDecay=4-quantity(level,other),power=0;
                if(otherDecay>=4){if(!level.getBlockState(other).blocksMotion()){otherDecay=4-quantity(level,other.below());if(otherDecay>=0)power=otherDecay-(decay-4);}}
                else if(otherDecay>=0)power=otherDecay-decay;
                flow=flow.add(direction.getStepX()*power,0,direction.getStepZ()*power);
            }
            if(level.getFluidState(pos.above()).getType()==this)for(Direction direction:Direction.Plane.HORIZONTAL){var other=pos.relative(direction);if(level.getBlockState(other).blocksMotion()||level.getBlockState(other.above()).blocksMotion()){flow=flow.normalize().add(0,-6,0);break;}}
            return flow.normalize();
        }
        @Override public int getTickDelay(LevelReader l){return 7;}
        @Override protected float getExplosionResistance(){return 100;}
        @Override public float getHeight(FluidState s,BlockGetter l,BlockPos p){return l.getFluidState(p.above()).getType()==this?1:getOwnHeight(s);}
        @Override public float getOwnHeight(FluidState s){return s.getValue(AMOUNT)*.875f/4;}
        @Override protected BlockState createLegacyBlock(FluidState s){return BLOCK.get().defaultBlockState().setValue(AMOUNT,s.getValue(AMOUNT));}
        @Override public boolean isSource(FluidState s){return s.getValue(AMOUNT)==4;}
        @Override public int getAmount(FluidState s){return s.getValue(AMOUNT)*2;}
        @Override public VoxelShape getShape(FluidState s,BlockGetter l,BlockPos p){return Shapes.box(0,0,0,1,getHeight(s,l,p),1);}
    }
}
