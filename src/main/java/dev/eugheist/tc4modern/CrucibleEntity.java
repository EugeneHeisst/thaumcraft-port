package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.tiles.CrucibleState;

/** Minecraft persistence/sync bridge for the original crucible state. */
public final class CrucibleEntity extends BlockEntity {
    public final java.util.List<thaumcraft.client.fx.particles.FXBubble> bubbles=new java.util.ArrayList<>();
    public final CrucibleFluidHandler fluid=new CrucibleFluidHandler(this);
    public final CrucibleState crucible = new CrucibleState();
    // Legacy diagnostic retained for reading existing development-world saves.
    public int pendingFluxSpills;
    public CrucibleEntity(BlockPos pos, BlockState state) { super(TC4Modern.CRUCIBLE_ENTITY.get(), pos, state); }
    public void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());
        }
    }
    public void spill() {
        if(level==null||level.isClientSide)return;
        thaumcraft.common.blocks.CrucibleFlux.spill(new thaumcraft.common.blocks.CrucibleFlux.World(){
            public boolean air(int x,int y,int z){return level.isEmptyBlock(new BlockPos(x,y,z));}
            public int fluxMetadata(int x,int y,int z){var s=level.getBlockState(new BlockPos(x,y,z));return s.getBlock() instanceof FluxBlock?s.getValue(FluxBlock.AMOUNT)-1:-1;}
            public void increment(int x,int y,int z,int md){var p=new BlockPos(x,y,z);level.setBlock(p,level.getBlockState(p).setValue(FluxBlock.AMOUNT,md+1),3);}
            public void place(int x,int y,int z,boolean gas){level.setBlock(new BlockPos(x,y,z),(gas?TC4Modern.FLUX_GAS:TC4Modern.FLUX_GOO).get().defaultBlockState(),3);}
        },worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),new java.util.random.RandomGenerator(){
            public long nextLong(){return level.random.nextLong();}
            public int nextInt(int bound){return level.random.nextInt(bound);}
            public boolean nextBoolean(){return level.random.nextBoolean();}
        });
    }
    public static void tick(Level level, BlockPos pos, BlockState state, CrucibleEntity tile) {
        if (level.isClientSide) { tile.clientEffects(); return; }
        var below = level.getBlockState(pos.below());
        boolean heated = below.is(TC4Modern.NITOR_BLOCK.get()) || below.is(net.minecraft.world.level.block.Blocks.LAVA)
                || below.is(net.minecraft.world.level.block.Blocks.FIRE);
        short previous = tile.crucible.heat;
        int water = tile.crucible.water, tags = tile.crucible.tagAmount();
        int bellows=0;
        for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)if(level.getBlockState(pos.relative(direction)).is(TC4Modern.BELLOWS.get()))bellows++;
        tile.crucible.tick(heated, bellows, new java.util.random.RandomGenerator() {
            @Override public long nextLong() { return level.random.nextLong(); }
            @Override public int nextInt(int bound) { return level.random.nextInt(bound); }
            @Override public boolean nextBoolean() { return level.random.nextBoolean(); }
        }, tile::spill);
        if (previous != tile.crucible.heat || water != tile.crucible.water
                || tags != tile.crucible.tagAmount() || tile.crucible.counter == 0) tile.changed();
        else tile.setChanged();
    }
    private java.util.random.RandomGenerator random(){return new java.util.random.RandomGenerator(){
        public long nextLong(){return level.random.nextLong();}
        public int nextInt(int bound){return level.random.nextInt(bound);}
        public float nextFloat(){return level.random.nextFloat();}
        public double nextDouble(){return level.random.nextDouble();}
    };}
    private void bubble(double x,double y,double z,int age,int color,int froth,double speed){
        var rng=random();var b=new thaumcraft.client.fx.particles.FXBubble(x,y,z,age,rng);
        b.red=((color>>16)&255)/255f;b.green=((color>>8)&255)/255f;b.blue=(color&255)/255f;
        if(froth!=0)b.froth(rng,froth==2);else b.bubblespeed=speed;
        bubbles.add(b);
    }
    private void clientEffects(){
        var rng=random();for(var b:bubbles)b.tick(rng);bubbles.removeIf(b->b.dead);
        if(crucible.water<=0||!NitorEntity.visible.test(worldPosition))return;
        if(crucible.boiling()){
            bubble(.2+level.random.nextFloat()*.6,crucible.fluidHeight(),.2+level.random.nextFloat()*.6,-4,0x7f7fb2,1,0);
            if(crucible.tagAmount()>100)for(int i=0;i<2;i++){
                bubble(0,1,level.random.nextFloat(),-4,0x7f7fb2,2,0);
                bubble(1,1,level.random.nextFloat(),-4,0x7f7fb2,2,0);
                bubble(level.random.nextFloat(),1,0,-4,0x7f7fb2,2,0);
                bubble(level.random.nextFloat(),1,1,-4,0x7f7fb2,2,0);
            }
        }
        if(level.random.nextInt(6)==0 && crucible.aspects.size()>0){
            int color=crucible.aspects.getAspects()[level.random.nextInt(crucible.aspects.size())].getColor();
            double x=(5+level.random.nextInt(22))/32.0+.015625,z=(5+level.random.nextInt(22))/32.0+.015625;
            level.random.nextInt(10); // Original unused delay field consumes this random value.
            bubble(x,.05+crucible.fluidHeight(),z,1,color,0,.002);
        }
    }
    @Override public boolean triggerEvent(int id,int parameter){
        if(id!=2)return super.triggerEvent(id,parameter);
        if(level!=null && level.isClientSide){
            level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,TC4Modern.SPILL.get(),net.minecraft.sounds.SoundSource.BLOCKS,.2f,1,false);
            int setting=NitorEntity.particleSetting.getAsInt(),count=setting==0?2:setting==1?1:0;
            for(int q=0;q<10;q++){
                level.random.nextInt(22);level.random.nextInt(22);
                for(int i=0;i<count;i++){
                    double x=.2+level.random.nextFloat()*.6,z=.2+level.random.nextFloat()*.6;
                    int color=crucible.aspects.size()==0?0xffffff:crucible.aspects.getAspects()[level.random.nextInt(crucible.aspects.size())].getColor();
                    bubble(x,.1+crucible.fluidHeight(),z,3,color,0,.003*parameter);
                }
            }
        }
        return true;
    }
    public void attemptSmelt(net.minecraft.world.entity.item.ItemEntity entity) {
        if (level == null || level.isClientSide || !crucible.boiling() || crucible.water <= 0
                || entity instanceof thaumcraft.common.entities.EntitySpecialItem) return;
        var stack = entity.getItem();
        var owner = entity.getOwner();
        var completed = owner instanceof net.minecraft.world.entity.player.Player player
                ? ResearchProgress.completed(player.getPersistentData()) : java.util.Set.<String>of();
        // Preserve original loop, including the decreasing stack-size loop bound.
        boolean dissolved=false, crafted=false;
        int stacksize = stack.getCount();
        for (int a = 0; a < stacksize; ++a) {
            var recipe = thaumcraft.api.crafting.CrucibleRecipe.findMatching(
                    CrucibleRecipes.entries(), completed, crucible.aspects, stack);
            if (recipe != null && crucible.water > 0) {
                if(owner instanceof net.minecraft.world.entity.player.Player player)
                    net.neoforged.neoforge.event.EventHooks.firePlayerCraftingEvent(player,recipe.getRecipeOutput().copy(),new net.minecraft.world.SimpleContainer(stack.copy()));
                crucible.crafted(recipe.removeMatching(crucible.aspects));
                eject(recipe.getRecipeOutput().copy());
                crafted=true;
                --stacksize;
                continue;
            }
            var tags = RecipeAspects.get(stack, level);
            if (tags.isEmpty()) {
                entity.setDeltaMovement((level.random.nextFloat()-level.random.nextFloat())*.2f,
                        .35f,(level.random.nextFloat()-level.random.nextFloat())*.2f);
                // Apply partial consumption before returning so rejected stacks cannot duplicate earlier crafts.
                stack.setCount(stacksize);
                entity.setItem(stack);
                changed();
                return;
            }
            var aspects = new thaumcraft.api.aspects.AspectList();
            tags.forEach((tag, amount) -> aspects.add(thaumcraft.api.aspects.Aspect.getAspect(tag),amount));
            crucible.dissolve(aspects);
            dissolved=true;
            --stacksize;
        }
        if(dissolved)level.playSound(null,entity.getX(),entity.getY(),entity.getZ(),TC4Modern.BUBBLE.get(),net.minecraft.sounds.SoundSource.BLOCKS,.2f,1+level.random.nextFloat()*.4f);
        if(dissolved)level.blockEvent(worldPosition,getBlockState().getBlock(),2,1);
        if(crafted)level.blockEvent(worldPosition,getBlockState().getBlock(),2,5);
        if (stacksize <= 0) entity.discard();
        else { stack.setCount(stacksize); entity.setItem(stack); }
        changed();
    }
    private void eject(net.minecraft.world.item.ItemStack output) {
        boolean first = true;
        while (!output.isEmpty()) {
            var part = output.split(Math.min(output.getCount(), output.getMaxStackSize()));
            var entity = new thaumcraft.common.entities.EntitySpecialItem(TC4Modern.SPECIAL_ITEM.get(), level);
            entity.setPos(worldPosition.getX()+.5,worldPosition.getY()+.71,worldPosition.getZ()+.5);
            entity.setItem(part);
            entity.setDeltaMovement(first ? 0 : (level.random.nextFloat()-level.random.nextFloat())*.01f,
                    .1f,first ? 0 : (level.random.nextFloat()-level.random.nextFloat())*.01f);
            level.addFreshEntity(entity);
            first = false;
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putShort("Heat", crucible.heat);
        tag.putInt("Water", crucible.water);
        tag.putInt("PendingFluxSpills", pendingFluxSpills);
        crucible.aspects.writeToNBT(tag);
        // Original counter intentionally restarts at -100 after loading.
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        crucible.heat = tag.getShort("Heat");
        crucible.water = Math.clamp(tag.getInt("Water"), 0, CrucibleState.CAPACITY);
        pendingFluxSpills = tag.getInt("PendingFluxSpills");
        crucible.aspects.readFromNBT(tag);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
