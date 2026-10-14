package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Original fibre variants and surface attachment, represented by modern multipart state. */
public final class TaintFibresBlock extends Block {
    public static final IntegerProperty VARIANT=IntegerProperty.create("variant",0,4);
    public static final java.util.Map<Direction,BooleanProperty> FACES=java.util.Arrays.stream(Direction.values()).collect(java.util.stream.Collectors.toMap(d->d,d->BooleanProperty.create(d.getName())));
    public TaintFibresBlock(Properties properties){
        super(properties);
        var state=stateDefinition.any().setValue(VARIANT,0);
        for(var property:FACES.values())state=state.setValue(property,false);
        registerDefaultState(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(VARIANT);for(var property:FACES.values())builder.add(property);}
    public BlockState attach(BlockState state,BlockGetter level,BlockPos pos){
        for(var direction:Direction.values()){
            var neighbor=pos.relative(direction);
            state=state.setValue(FACES.get(direction),!level.getBlockState(neighbor).is(TC4Modern.TAINT.get())&&level.getBlockState(neighbor).isFaceSturdy(level,neighbor,direction.getOpposite()));
        }
        return state;
    }
    @Override protected void onPlace(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,BlockState old,boolean moving){
        if(!old.is(this)){
            var attached=attach(state,level,pos);
            if(attached!=state)level.setBlock(pos,attached,3);
        }
    }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){return attach(state,level,pos);}
    @Override protected void neighborChanged(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,Block block,BlockPos from,boolean moving){
        if(level instanceof net.minecraft.server.level.ServerLevel server&&new TaintWorld(server,pos.getY()).onlyAdjacentToTaint(pos.getX(),pos.getY(),pos.getZ()))level.removeBlock(pos,false);
    }
    @Override protected void entityInside(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.Entity entity){
        if(!level.isClientSide&&entity instanceof net.minecraft.world.entity.LivingEntity living&&!living.isInvertedHealAndHarm()){
            if(entity instanceof net.minecraft.world.entity.player.Player&&level.random.nextInt(1000)==0)living.addEffect(new net.minecraft.world.effect.MobEffectInstance(TC4Modern.FLUX_TAINT,80,0,false,true));
            else if(!(entity instanceof net.minecraft.world.entity.player.Player)&&level.random.nextInt(500)==0)living.addEffect(new net.minecraft.world.effect.MobEffectInstance(TC4Modern.FLUX_TAINT,160,0,false,true));
        }
    }
    @Override protected void randomTick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){tick(state,level,pos,random);}
    @Override protected void tick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){
        thaumcraft.common.blocks.TaintFibresSpread.tick(new TaintWorld(level,pos.getY()),pos.getX(),pos.getY(),pos.getZ(),state.getValue(VARIANT),TaintConfig.spreadRate(),SpawnConfig.spores(),new java.util.random.RandomGenerator(){public long nextLong(){return random.nextLong();}public int nextInt(int bound){return random.nextInt(bound);}});
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        if(state.getValue(VARIANT)>0)return Block.box(3.2,0,3.2,12.8,12.8,12.8);
        // The original selection box chooses the first solid neighbor, rather than their union.
        for(var direction:Direction.values())if(level.getBlockState(pos.relative(direction)).isFaceSturdy(level,pos.relative(direction),direction.getOpposite()))return switch(direction){
            case DOWN->Block.box(0,0,0,16,1,16);case UP->Block.box(0,15,0,16,16,16);
            case NORTH->Block.box(0,0,0,16,16,1);case SOUTH->Block.box(0,0,15,16,16,16);
            case WEST->Block.box(0,0,0,1,16,16);case EAST->Block.box(15,0,0,16,16,16);
        };
        return Block.box(0,0,0,16,1,16);
    }
}
