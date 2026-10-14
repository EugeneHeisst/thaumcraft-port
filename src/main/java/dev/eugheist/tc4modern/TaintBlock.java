package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Original crust, tainted soil and flesh variants in one modern block state. */
public final class TaintBlock extends Block {
    public static final IntegerProperty VARIANT=IntegerProperty.create("variant",0,2);
    public TaintBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(VARIANT,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(VARIANT);}
    @Override protected void randomTick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){tick(state,level,pos,random);}
    @Override protected void tick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){
        int variant=state.getValue(VARIANT);if(variant==2)return;
        var rng=new java.util.random.RandomGenerator(){public long nextLong(){return random.nextLong();}public int nextInt(int bound){return random.nextInt(bound);}};
        thaumcraft.common.blocks.TaintFibresSpread.biomeSpread(new TaintWorld(level,pos.getY()),pos.getX(),pos.getY(),pos.getZ(),TaintConfig.spreadRate(),rng);
        if(variant==0&&thaumcraft.common.blocks.TaintFallingRules.crustFall(new TaintFallingBridge(level),pos.getX(),pos.getY(),pos.getZ(),rng))return;
        int x=pos.getX()+random.nextInt(3)-1,y=pos.getY()+random.nextInt(5)-3,z=pos.getZ()+random.nextInt(3)-1;
        var world=new TaintWorld(level,pos.getY());
        if(!world.taintedBiome(x,z))thaumcraft.common.blocks.TaintBlockDecay.outsideTargetBiome(new thaumcraft.common.blocks.TaintBlockDecay.World(){
            public void goo(int bx,int by,int bz){level.setBlock(new BlockPos(bx,by,bz),TC4Modern.FLUX_GOO.get().defaultBlockState().setValue(FluxBlock.AMOUNT,9),3);}
            public void dirt(int bx,int by,int bz){level.setBlock(new BlockPos(bx,by,bz),net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(),3);}
        },pos.getX(),pos.getY(),pos.getZ(),variant,rng);
        else{
            thaumcraft.common.blocks.TaintFibresSpread.spread(world,x,y,z,rng);
            thaumcraft.common.blocks.TaintBlockDecay.insideTargetBiome(new thaumcraft.common.blocks.TaintBlockDecay.InfestedWorld(){
                public void goo(int bx,int by,int bz){level.setBlock(new BlockPos(bx,by,bz),TC4Modern.FLUX_GOO.get().defaultBlockState().setValue(FluxBlock.AMOUNT,9),3);}
                public void dirt(int bx,int by,int bz){level.setBlock(new BlockPos(bx,by,bz),net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(),3);}
                public boolean airAbove(){return level.isEmptyBlock(pos.above());}
                public boolean nearbySwarmer(){return !level.getEntitiesOfClass(thaumcraft.common.entities.monster.EntityTaintSporeSwarmer.class,new net.minecraft.world.phys.AABB(pos).inflate(16)).isEmpty();}
                public boolean encased(){
                    if(!level.getBlockState(pos.above()).is(TaintBlock.this))return false;
                    for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)if(!level.getBlockState(pos.relative(direction)).is(TaintBlock.this))return false;
                    return true;
                }
                public void swarmer(){
                    level.removeBlock(pos,false);
                    var entity=new thaumcraft.common.entities.monster.EntityTaintSporeSwarmer(TC4Modern.TAINT_SWARMER.get(),level);
                    entity.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);level.addFreshEntity(entity);
                    level.playSound(null,entity.blockPosition(),TC4Modern.ROOTS.get(),net.minecraft.sounds.SoundSource.HOSTILE,.1f,.9f+random.nextFloat()*.2f);
                }
            },pos.getX(),pos.getY(),pos.getZ(),variant,SpawnConfig.spores(),rng);
        }
    }
    @Override public void stepOn(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.Entity entity){
        if(state.getValue(VARIANT)==2)return;
        if(!level.isClientSide&&entity instanceof net.minecraft.world.entity.LivingEntity living&&!living.isInvertedHealAndHarm()){
            int chance=entity instanceof Player?100:20;
            if(level.random.nextInt(chance)==0)living.addEffect(new net.minecraft.world.effect.MobEffectInstance(TC4Modern.FLUX_TAINT,entity instanceof Player?80:160,0,false,true));
        }
        super.stepOn(level,pos,state,entity);
    }
    @Override protected float getDestroyProgress(BlockState state,Player player,BlockGetter level,BlockPos pos){
        float hardness=thaumcraft.common.blocks.TaintBlockState.hardness(state.getValue(VARIANT));
        return player.getDestroySpeed(state)/hardness/(player.hasCorrectToolForDrops(state)?30:100);
    }
}
