package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.tiles.InfernalFurnaceState;

/** Native core host for the original infernal furnace countdown and inventory. */
public final class InfernalFurnaceEntity extends BlockEntity {
    public final InfernalFurnaceState furnace=new InfernalFurnaceState();
    public final InfernalFurnaceContents contents=new InfernalFurnaceContents();
    public InfernalFurnaceEntity(BlockPos pos,BlockState state){super(TC4Modern.INFERNAL_ENTITY.get(),pos,state);}
    public int bellows(){
        int count=0;
        for(var direction:Direction.values()){
            if(direction==Direction.UP)continue;
            var pos=worldPosition.relative(direction,2);
            if(level.getBlockEntity(pos) instanceof BellowsEntity tile&&tile.bellows.orientation==direction.getOpposite().get3DDataValue()&&!level.hasNeighborSignal(pos))count++;
        }
        return Math.min(3,count);
    }
    private Direction outlet(){
        for(var direction:new Direction[]{Direction.WEST,Direction.EAST,Direction.NORTH}){
            var state=level.getBlockState(worldPosition.relative(direction));
            if(state.is(TC4Modern.INFERNAL_FURNACE.get())&&state.getValue(InfernalFurnaceBlock.PART)==10)return direction;
        }
        return Direction.SOUTH;
    }
    public boolean accept(ItemStack stack){
        return contents.insert(level,stack,()->{
            level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.3f,2.6f+(level.random.nextFloat()-level.random.nextFloat())*.8f);
            if(level instanceof ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,worldPosition.getX()+level.random.nextFloat(),worldPosition.getY()+1,worldPosition.getZ()+level.random.nextFloat(),1,0,0,0,0);
        },this::setChanged);
    }
    private ItemStack bonus(ItemStack input){
        if(input.is(Items.CHICKEN))return new ItemStack(TC4Modern.CHICKEN_NUGGET.get());
        if(input.is(Items.BEEF))return new ItemStack(TC4Modern.BEEF_NUGGET.get());
        if(input.is(Items.PORKCHOP))return new ItemStack(TC4Modern.PORK_NUGGET.get());
        if(input.is(Items.COD)||input.is(Items.SALMON))return new ItemStack(TC4Modern.FISH_NUGGET.get());
        if(input.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c","ores/gold")))||input.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c","raw_materials/gold"))))return new ItemStack(Items.GOLD_NUGGET);
        if(input.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c","ores/iron")))||input.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c","raw_materials/iron"))))return new ItemStack(Items.IRON_NUGGET);
        return ItemStack.EMPTY;
    }
    private static boolean meatBonus(ItemStack input){
        return input.is(Items.CHICKEN)||input.is(Items.BEEF)||input.is(Items.PORKCHOP)||input.is(Items.COD)||input.is(Items.SALMON);
    }
    public static void tick(Level level,BlockPos pos,BlockState state,InfernalFurnaceEntity tile){
        if(!(level instanceof ServerLevel server))return;
        tile.furnace.tick(new InfernalFurnaceState.Host(){
            public int drainFireVis(int requested){return VisNetwork.drain(server,pos,Aspect.FIRE,requested,(node,color)->{});}
            public int activeBellows(){return tile.bellows();}
            public boolean hasSmeltableInput(){return tile.contents.hasSmeltableInput(level);}
            public void smeltFirstInput(){tile.contents.smeltFirst(level,(output,input,xp)->{
                InfernalFurnaceEjection.eject(level,pos,tile.outlet(),output,tile.bonus(input),meatBonus(input)?1:0,tile.bellows(),xp);
                level.blockEvent(pos,state.getBlock(),3,0);
            });}
        });
        tile.setChanged();
    }
    @Override public boolean triggerEvent(int id,int value){
        if(id==3){
            if(level!=null&&level.isClientSide)InfernalFurnaceClient.smelt(level,worldPosition,outlet());
            return true;
        }
        return super.triggerEvent(id,value);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);contents.save(tag,registries);
        tag.putShort("CookTime",(short)furnace.cookTime);tag.putShort("SpeedyTime",(short)furnace.speedyTime);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);contents.load(tag,registries);
        furnace.cookTime=tag.getShort("CookTime");furnace.speedyTime=tag.getShort("SpeedyTime");furnace.maxCookTime=0;
    }
}
