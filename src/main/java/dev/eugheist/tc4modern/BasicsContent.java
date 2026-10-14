package dev.eugheist.tc4modern;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class BasicsContent {
 static <T> DeferredHolder<T,T> ref(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> type,String name){return DeferredHolder.create(type,AlchemyContent.id(name));}
 public static final DeferredHolder<Block,Block> DECONSTRUCTOR=ref(Registries.BLOCK,"deconstructor"),ADVANCED_DECONSTRUCTOR=ref(Registries.BLOCK,"advanced_deconstructor"),NODE_JAR=ref(Registries.BLOCK,"node_jar");
 public static final DeferredHolder<Block,Block> CINDERPEARL=ref(Registries.BLOCK,"cinderpearl"),MANA_POD=ref(Registries.BLOCK,"mana_pod");
 public static final DeferredHolder<Item,Item> MANA_BEAN=ref(Registries.ITEM,"mana_bean"),WISP_ESSENCE=ref(Registries.ITEM,"wisp_essence");
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ManaPodEntity>> MANA_POD_ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("mana_pod"));
 public static final java.util.List<DeferredHolder<Block,Block>> CLUSTERS=new java.util.ArrayList<>();
 public static final DeferredHolder<Item,Item> KNOWLEDGE_FRAGMENT=ref(Registries.ITEM,"knowledge_fragment"),FRAGMENT_NOTES=ref(Registries.ITEM,"fragment_notes"),CRIMSON_RITES=ref(Registries.ITEM,"crimson_rites");
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<DeconstructorEntity>> DECONSTRUCTOR_ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("deconstructor"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<NodeJarEntity>> NODE_JAR_ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("node_jar"));
 public static final DeferredHolder<MenuType<?>,MenuType<DeconstructorMenu>> DECONSTRUCTOR_MENU=DeferredHolder.create(Registries.MENU,AlchemyContent.id("deconstructor"));
 static{for(String e:new String[]{"air","fire","water","earth","order","entropy","mixed"})CLUSTERS.add(ref(Registries.BLOCK,e+"_crystal_cluster"));}
 @SubscribeEvent static void register(RegisterEvent e){
  e.register(Registries.BLOCK,h->{h.register(CINDERPEARL.getId(),new CinderpearlBlock(Block.Properties.of().instabreak().noCollission().sound(SoundType.GRASS).lightLevel(s->8)));h.register(MANA_POD.getId(),new ManaPodBlock(Block.Properties.of().strength(.5f).noOcclusion().sound(SoundType.STONE).randomTicks().lightLevel(s->s.getValue(ManaPodBlock.AGE))));h.register(DECONSTRUCTOR.getId(),new DeconstructorBlock(Block.Properties.of().strength(2.5f).sound(SoundType.WOOD).noOcclusion()));h.register(ADVANCED_DECONSTRUCTOR.getId(),new DeconstructorBlock(Block.Properties.of().strength(2.5f).sound(SoundType.WOOD).noOcclusion()));h.register(NODE_JAR.getId(),new NodeJarBlock(Block.Properties.of().strength(.3f).sound(TC4SoundTypes.JAR).lightLevel(s->11).noOcclusion()));for(int i=0;i<7;i++)h.register(CLUSTERS.get(i).getId(),new CrystalClusterBlock(Block.Properties.of().strength(.7f,TC4SoundTypes.res(1)).lightLevel(s->7).sound(TC4SoundTypes.CRYSTAL).noOcclusion(),i));});
  e.register(Registries.ITEM,h->{h.register(CINDERPEARL.getId(),new BlockItem(CINDERPEARL.get(),new Item.Properties()));h.register(MANA_BEAN.getId(),new ManaBeanItem(new Item.Properties()));h.register(WISP_ESSENCE.getId(),new WispEssenceItem());h.register(DECONSTRUCTOR.getId(),new BlockItem(DECONSTRUCTOR.get(),new Item.Properties()));h.register(ADVANCED_DECONSTRUCTOR.getId(),new BlockItem(ADVANCED_DECONSTRUCTOR.get(),new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));h.register(NODE_JAR.getId(),new NodeJarItem(NODE_JAR.get(),new Item.Properties().stacksTo(1)));for(var b:CLUSTERS)h.register(b.getId(),new BlockItem(b.get(),new Item.Properties()));h.register(KNOWLEDGE_FRAGMENT.getId(),new KnowledgeFragmentItem(new Item.Properties()));h.register(FRAGMENT_NOTES.getId(),new FragmentNotesItem(new Item.Properties().stacksTo(1)));h.register(CRIMSON_RITES.getId(),new CrimsonRitesItem(new Item.Properties().stacksTo(1)));});
  e.register(Registries.BLOCK_ENTITY_TYPE,h->{h.register(MANA_POD_ENTITY.getId(),BlockEntityType.Builder.of(ManaPodEntity::new,MANA_POD.get()).build(null));h.register(DECONSTRUCTOR_ENTITY.getId(),BlockEntityType.Builder.of(DeconstructorEntity::new,DECONSTRUCTOR.get(),ADVANCED_DECONSTRUCTOR.get()).build(null));h.register(NODE_JAR_ENTITY.getId(),BlockEntityType.Builder.of(NodeJarEntity::new,NODE_JAR.get()).build(null));});
  e.register(Registries.FEATURE,h->{h.register(AlchemyContent.id("cinderpearl"),new CinderpearlFeature());h.register(AlchemyContent.id("mana_pods"),new ManaPodFeature());h.register(AlchemyContent.id("magical_forest_trees"),new MagicalForestTrees());});
  e.register(Registries.MENU,h->h.register(DECONSTRUCTOR_MENU.getId(),new MenuType<>(DeconstructorMenu::new,net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS)));
 }
}
