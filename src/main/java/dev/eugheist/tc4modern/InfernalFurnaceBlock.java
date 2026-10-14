package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Original arcane furnace metadata and collision forms, represented as modern block states. */
public final class InfernalFurnaceBlock extends net.minecraft.world.level.block.BaseEntityBlock {
    public static final IntegerProperty LAYER=IntegerProperty.create("layer",0,2);
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty OUTLET=net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART=IntegerProperty.create("part",0,10);
    public static final com.mojang.serialization.MapCodec<InfernalFurnaceBlock> CODEC=simpleCodec(InfernalFurnaceBlock::new);
    private static final VoxelShape CORE=box(0,0,0,16,4,16);
    public InfernalFurnaceBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(PART,0).setValue(LAYER,1).setValue(OUTLET,net.minecraft.core.Direction.SOUTH));}
    @Override protected com.mojang.serialization.MapCodec<InfernalFurnaceBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(PART,LAYER,OUTLET);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return net.minecraft.world.phys.shapes.Shapes.block();}
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){return state.getValue(PART)==0?new InfernalFurnaceEntity(pos,state):null;}
    @Override protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state){return net.minecraft.world.level.block.RenderShape.MODEL;}
    @Override public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){return state.getValue(PART)==0?createTickerHelper(type,TC4Modern.INFERNAL_ENTITY.get(),InfernalFurnaceEntity::tick):null;}
    @Override protected void entityInside(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.Entity entity){
        if(state.getValue(PART)==0){
            var motion=entity.getDeltaMovement();double dx=motion.x,dz=motion.z;
            if(entity.getX()<pos.getX()+.3f)dx+=1.0E-4f;
            if(entity.getX()>pos.getX()+.7f)dx-=1.0E-4f;
            if(entity.getZ()<pos.getZ()+.3f)dz+=1.0E-4f;
            if(entity.getZ()>pos.getZ()+.7f)dz-=1.0E-4f;
            entity.setDeltaMovement(dx,motion.y,dz);
            if(entity instanceof net.minecraft.world.entity.item.ItemEntity item){
                entity.setDeltaMovement(dx,.025f,dz);
                if(!level.isClientSide&&entity.onGround()&&level.getBlockEntity(pos) instanceof InfernalFurnaceEntity tile&&tile.accept(item.getItem()))entity.discard();
            }else if(entity instanceof net.minecraft.world.entity.LivingEntity&&!entity.fireImmune()){
                entity.hurt(level.damageSources().lava(),3);entity.igniteForSeconds(10);
            }
        }
    }
    @Override protected void neighborChanged(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,Block block,BlockPos from,boolean moving){
        if(state.getValue(PART)==0)InfernalFurnaceLifecycle.validate(level,pos);
        super.neighborChanged(state,level,pos,block,from,moving);
    }
    @Override protected void onRemove(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())){
            if(state.getValue(PART)==0&&!level.isClientSide&&level.getBlockEntity(pos) instanceof InfernalFurnaceEntity){
                var blaze=net.minecraft.world.entity.EntityType.BLAZE.create(level);
                if(blaze!=null){
                    blaze.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);
                    blaze.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION,6000,2));
                    blaze.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,12000,0));
                    level.addFreshEntity(blaze);
                }
                InfernalFurnaceLifecycle.restore(level,pos);
            }
            super.onRemove(state,level,pos,next,moving);
            InfernalFurnaceLifecycle.notifySurroundings(level,pos);
        }
    }
    private boolean core(BlockGetter level,BlockPos pos){var state=level.getBlockState(pos);return state.is(this)&&state.getValue(PART)==0;}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        int part=state.getValue(PART);
        if(part==0)return CORE;
        if(part==10){
            if(core(level,pos.west()))return box(0,0,0,8,16,16);
            if(core(level,pos.east()))return box(8,0,0,16,16,16);
            if(core(level,pos.north()))return box(0,0,0,16,16,8);
            return box(0,0,8,16,16,16);
        }
        return net.minecraft.world.phys.shapes.Shapes.block();
    }
}
