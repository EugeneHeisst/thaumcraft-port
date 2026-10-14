package dev.eugheist.tc4modern;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class AlchemyMachines {
    static DeferredHolder<Block,Block> block(String n){return DeferredHolder.create(Registries.BLOCK,AlchemyContent.id(n));}
    public static final DeferredHolder<Block,Block> CENTRIFUGE=block("centrifuge"), CRYSTALIZER=block("essentia_crystalizer"), VOID_JAR=block("void_jar"), CONSTRUCT=block("alchemical_construct");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<AlchemyMachineEntity>> ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("alchemy_machine"));
    @SubscribeEvent static void register(RegisterEvent e){
        e.register(Registries.BLOCK,h->{
            h.register(CENTRIFUGE.getId(),new MachineBlock(BlockBehaviour.Properties.of().strength(.5f,TC4SoundTypes.res(5)).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
            h.register(CRYSTALIZER.getId(),new MachineBlock(BlockBehaviour.Properties.of().strength(.5f,TC4SoundTypes.res(5)).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
            h.register(CONSTRUCT.getId(),new Block(BlockBehaviour.Properties.of().strength(3,TC4SoundTypes.res(17)).sound(SoundType.METAL).requiresCorrectToolForDrops()));
            h.register(VOID_JAR.getId(),new WardedJarBlock(BlockBehaviour.Properties.of().strength(.3f).sound(TC4SoundTypes.JAR).lightLevel(s->9).noOcclusion()));
        });
        e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ENTITY.getId(),BlockEntityType.Builder.of(AlchemyMachineEntity::new,CENTRIFUGE.get(),CRYSTALIZER.get()).build(null)));
        e.register(Registries.ITEM,h->{for(var b:java.util.List.of(CENTRIFUGE,CRYSTALIZER,CONSTRUCT))h.register(b.getId(),new BlockItem(b.get(),new Item.Properties()){ @Override public boolean placeBlock(net.minecraft.world.item.context.BlockPlaceContext c,BlockState s){boolean ok=super.placeBlock(c,s);if(ok&&c.getLevel().getBlockEntity(c.getClickedPos()) instanceof AlchemyMachineEntity t){t.state.facing=c.getClickedFace().getOpposite();t.changed();}return ok;} });h.register(VOID_JAR.getId(),new WardedJarBlock.JarItem(VOID_JAR.get(),new Item.Properties()));});
    }
    public static final class MachineBlock extends BaseEntityBlock {
        public static final MapCodec<MachineBlock> CODEC=simpleCodec(MachineBlock::new);
        MachineBlock(Properties p){super(p);}
        @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
        @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new AlchemyMachineEntity(p,s);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.is(CENTRIFUGE.get())?Block.box(4,0,4,12,16,12):Shapes.block();}
        @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,net.minecraft.world.entity.LivingEntity e,ItemStack stack){if(l.getBlockEntity(p) instanceof AlchemyMachineEntity t)t.changed();}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return createTickerHelper(t,ENTITY.get(),AlchemyMachineEntity::tick);}
    }
}
