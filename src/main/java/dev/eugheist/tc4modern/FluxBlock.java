package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import thaumcraft.common.blocks.FiniteFlux;

/** Modern world adapter for the original finite-flux volume algorithm. */
public final class FluxBlock extends Block {
    // Like LiquidBlock: hand targeting ignores the block; buckets use the fluid shape.
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(
            net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }
    public static final IntegerProperty AMOUNT=IntegerProperty.create("amount",1,9);
    public final int direction;
    public FluxBlock(Properties properties,int direction){super(properties);this.direction=direction;registerDefaultState(stateDefinition.any().setValue(AMOUNT,1));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(AMOUNT);}
    @Override protected boolean canBeReplaced(BlockState state,net.minecraft.world.item.context.BlockPlaceContext context){return state.getValue(AMOUNT)<3;}
    private int delay(){return direction>0?12:30;}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){tick(state,level,pos,random);}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){level.scheduleTick(pos,this,delay());}
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.Entity entity){
        if(level.isClientSide)return;
        int md=state.getValue(AMOUNT)-1;
        if(direction<0&&entity instanceof thaumcraft.common.entities.monster.EntityThaumicSlime slime){
            if(slime.getSlimeSize()<md&&level.random.nextBoolean()){
                slime.setSlimeSize(slime.getSlimeSize()+1);
                if(md>1)level.setBlock(pos,state.setValue(AMOUNT,md),3);else level.removeBlock(pos,false);
            }
        }else if(direction<0){
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1-(md+1)/8.0,1,1-(md+1)/8.0));
            if(entity instanceof net.minecraft.world.entity.LivingEntity living)living.addEffect(new net.minecraft.world.effect.MobEffectInstance(TC4Modern.VIS_EXHAUST,600,md/3,true,true));
        }else if(level.random.nextInt(10)==0 && entity instanceof net.minecraft.world.entity.LivingEntity living
                && !(living instanceof thaumcraft.api.entities.ITaintedMob) && !living.isInvertedHealAndHarm() && !living.hasEffect(TC4Modern.VIS_EXHAUST) && !living.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION)){
            if(level.random.nextBoolean())living.addEffect(new net.minecraft.world.effect.MobEffectInstance(TC4Modern.VIS_EXHAUST,1200,md/3,true,true));
            else living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION,80+md*20,0,false,true));
            if(md>0)level.setBlock(pos,state.setValue(AMOUNT,md),3);else level.removeBlock(pos,false);
        }
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        FiniteFlux.tick(new FiniteFlux.World(){
            public int quantity(int x,int y,int z){var s=level.getBlockState(new BlockPos(x,y,z));return s.isAir()?0:s.is(FluxBlock.this)?s.getValue(AMOUNT):-1;}
            public boolean displace(int x,int y,int z){
                var p=new BlockPos(x,y,z);var s=level.getBlockState(p);
                if(s.isAir())return true;
                if(s.getBlock() instanceof FluxBlock||!s.getFluidState().isEmpty()||s.blocksMotion()||s.is(net.minecraft.world.level.block.Blocks.NETHER_PORTAL))return false;
                Block.dropResources(s,level,p);return true;
            }
            public boolean outside(int y){return y<level.getMinBuildHeight()||y>=level.getMaxBuildHeight();}
            public void set(int x,int y,int z,int n){level.setBlock(new BlockPos(x,y,z),n==0?net.minecraft.world.level.block.Blocks.AIR.defaultBlockState():defaultBlockState().setValue(AMOUNT,n),3);}
            public void schedule(int x,int y,int z){level.scheduleTick(new BlockPos(x,y,z),FluxBlock.this,delay());}
            public boolean swapVertical(int x,int y,int z,int d,int n){
                var from=new BlockPos(x,y,z);var to=from.offset(0,d,0);var other=level.getBlockState(to);
                if(!(other.getBlock() instanceof FluxBlock flux)||flux.direction==direction)return false;
                level.setBlock(to,defaultBlockState().setValue(AMOUNT,n),3);level.setBlock(from,other,3);
                level.scheduleTick(to,FluxBlock.this,delay());level.scheduleTick(from,flux,flux.delay());return true;
            }
        },pos.getX(),pos.getY(),pos.getZ(),direction,new java.util.random.RandomGenerator(){
            public long nextLong(){return random.nextLong();}
            public int nextInt(int bound){return random.nextInt(bound);}
        });
        if(direction<0)thaumcraft.common.blocks.FluxGooDecay.tick(new thaumcraft.common.blocks.FluxGooDecay.World(){
            public int metadata(){var current=level.getBlockState(pos);return current.hasProperty(AMOUNT)?current.getValue(AMOUNT)-1:0;}
            public boolean airAbove(){return level.isEmptyBlock(pos.above());}
            public void remove(){level.removeBlock(pos,false);}
            public void slime(int size){
                var slime=new thaumcraft.common.entities.monster.EntityThaumicSlime(TC4Modern.THAUMIC_SLIME.get(),level);
                slime.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);slime.setSlimeSize(size);
                level.addFreshEntity(slime);slime.playSound(TC4Modern.GORE.get(),1,1);
            }
            public void taint(){
                TaintBiomes.spread(level,pos);
                level.setBlock(pos,TC4Modern.TAINT_FIBRES.get().defaultBlockState(),3);
                level.scheduleTick(pos,TC4Modern.TAINT_FIBRES.get(),1);
            }
            public void decrement(){var current=level.getBlockState(pos);level.setBlock(pos,current.setValue(AMOUNT,current.getValue(AMOUNT)-1),3);}
            public void gasAbove(){level.setBlock(pos.above(),TC4Modern.FLUX_GAS.get().defaultBlockState(),3);}
        },TaintConfig.fromFlux(),new java.util.random.RandomGenerator(){
            public long nextLong(){return random.nextLong();}
            public int nextInt(int bound){return random.nextInt(bound);}
            public boolean nextBoolean(){return random.nextBoolean();}
        });
    }
}
