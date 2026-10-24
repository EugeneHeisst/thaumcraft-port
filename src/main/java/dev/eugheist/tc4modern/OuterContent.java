package dev.eugheist.tc4modern;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

/** Original Outer Lands masonry; device blocks are registered separately from these building blocks. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class OuterContent {
    public static final DeferredHolder<Block,Block> STONE=block("ancient_stone"),ROCK=block("ancient_rock"),NO_SPAWN=block("ancient_stone_no_spawn"),CRUST=block("crusted_stone"),PEDESTAL=block("ancient_pedestal"),GLOWING=block("glowing_crusted_stone"),GLYPHED=block("glyphed_stone"),VOID=block("eldritch_void");
    public static final DeferredHolder<Block,StairBlock> STAIRS=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("ancient_stone_stairs"));
    public static final DeferredHolder<Block,SlabBlock> SLAB=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("ancient_stone_slab"));
    private static DeferredHolder<Block,Block> block(String name){return DeferredHolder.create(Registries.BLOCK,AlchemyContent.id(name));}
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,net.minecraft.world.level.block.entity.BlockEntityType<VoidEntity>> VOID_ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("eldritch_void"));
    static BlockBehaviour.Properties stone(){return BlockBehaviour.Properties.of().strength(2,TC4SoundTypes.res(10)).sound(SoundType.STONE).requiresCorrectToolForDrops();}
    @SubscribeEvent static void register(RegisterEvent event){
        event.register(Registries.BLOCK,h->{
            for(var ref:List.of(STONE,ROCK,NO_SPAWN,CRUST,PEDESTAL,GLOWING,GLYPHED)){
                var properties=stone();if(ref==NO_SPAWN||ref==GLOWING||ref==GLYPHED)properties.isValidSpawn((s,l,p,t)->false);
                if(ref==CRUST)properties.lightLevel(s->4);
                /* BlockEldritch.getBlockHardness/getExplosionResistance metadata 4/5 override the constructor 50/20000 with 2 and 30. */
                if(ref==GLOWING||ref==GLYPHED)properties.strength(2,30).lightLevel(s->12).noOcclusion();
                h.register(ref.getId(),ref==GLYPHED?new GlyphedStone(properties):new Block(properties));
            }
            h.register(STAIRS.getId(),new StairBlock(STONE.get().defaultBlockState(),stone()));
            h.register(SLAB.getId(),new SlabBlock(stone()));
            h.register(VOID.getId(),new VoidBlock(BlockBehaviour.Properties.of().strength(-1,TC4SoundTypes.res(6000000)).noOcclusion().noLootTable().lightLevel(s->3).isValidSpawn((s,l,p,t)->false)));
        });
        event.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(VOID_ENTITY.getId(),net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(VoidEntity::new,VOID.get()).build(null)));
        event.register(Registries.ITEM,h->{for(var ref:List.of(STONE,ROCK,NO_SPAWN,CRUST,PEDESTAL,GLOWING,STAIRS,SLAB))h.register(ref.getId(),new BlockItem(ref.get(),new Item.Properties()));});
    }
    static final class GlyphedStone extends Block {
        GlyphedStone(Properties p){super(p);}
        @Override protected java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){return List.of(new ItemStack(BasicsContent.KNOWLEDGE_FRAGMENT.get()));}
        @Override public int getExpDrop(BlockState state,LevelAccessor level,BlockPos pos,net.minecraft.world.level.block.entity.BlockEntity entity,Entity breaker,ItemStack tool){return 1+level.getRandom().nextInt(4);}
    }
    public static final class VoidEntity extends net.minecraft.world.level.block.entity.BlockEntity {VoidEntity(BlockPos p,BlockState s){super(VOID_ENTITY.get(),p,s);}
        /** Client render cache: bit per Direction ordinal of faces that border neither an opaque block nor more void. */
        int faces=-1;long facesChecked;}
    static final class VoidBlock extends BaseEntityBlock {
        VoidBlock(Properties p){super(p);}
        @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(VoidBlock::new);}
        @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos p,BlockState s){return new VoidEntity(p,s);}
        @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
        @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
        @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.box(.125,.125,.125,.875,.875,.875);}
        @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){if(entity.tickCount>20&&!(entity instanceof net.minecraft.world.entity.player.Player p&&p.getAbilities().instabuild))entity.hurt(level.damageSources().fellOutOfWorld(),8);}
    }
}
