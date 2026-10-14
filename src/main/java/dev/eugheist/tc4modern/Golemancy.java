package dev.eugheist.tc4modern;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

/** Original material placers, inventory cores and bell, with modern entity/item transport. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class Golemancy {
    static DeferredHolder<Item,Item> item(String name){return DeferredHolder.create(Registries.ITEM,AlchemyContent.id(name));}
    public static final DeferredHolder<Item,Item> STRAW=item("golem_straw"),BLANK=item("golem_core_blank"),GATHER=item("golem_core_gather"),BELL=item("golem_bell");
    public static final DeferredHolder<Item,Item> HARVEST=item("golem_core_harvest"),FILL=item("golem_core_fill"),EMPTY=item("golem_core_empty"),FLESH_BLOCK=item("flesh_block");
    public static final DeferredHolder<Item,Item> GUARD=item("golem_core_guard"),BUTCHER=item("golem_core_butcher"),LUMBER=item("golem_core_lumber"),LIQUID=item("golem_core_liquid"),ESSENTIA=item("golem_core_essentia");
    public static final DeferredHolder<Item,Item> USE=item("golem_core_use"),SORTING=item("golem_core_sorting"),FISHING=item("golem_core_fishing");
    /** ItemGolemDecoration metadata order and decoration letters. */
    static final String DECO="HGBFRVPM";static final String[] DECO_NAMES={"hat","glasses","bowtie","fez","dart","visor","armor","hammer"};
    public static Item decoration(int i){return item("golem_tiny_"+DECO_NAMES[i]).get();}
    static final String[] UPGRADES={"air","earth","water","fire","order","entropy"};
    static Item upgrade(int i){return item("golem_upgrade_"+UPGRADES[i]).get();}
    static int upgradeId(ItemStack s){for(int i=0;i<UPGRADES.length;i++)if(s.is(upgrade(i)))return i;return -1;}
    public static DeferredHolder<Item,Item> placer(GolemMaterial m){return item("golem_"+m.id());}
    public static Item core(int id){return switch(id){case 0->FILL.get();case 1->EMPTY.get();case 2->GATHER.get();case 3->HARVEST.get();case 4->GUARD.get();case 9->BUTCHER.get();case 7->LUMBER.get();case 5->LIQUID.get();case 6->ESSENTIA.get();case 11->FISHING.get();case 10->SORTING.get();case 8->USE.get();default->net.minecraft.world.item.Items.AIR;};}
    static int coreId(ItemStack stack){return stack.is(FILL.get())?0:stack.is(EMPTY.get())?1:stack.is(GATHER.get())?2:stack.is(HARVEST.get())?3:stack.is(GUARD.get())?4:stack.is(BUTCHER.get())?9:stack.is(LUMBER.get())?7:stack.is(LIQUID.get())?5:stack.is(ESSENTIA.get())?6:stack.is(FISHING.get())?11:stack.is(SORTING.get())?10:stack.is(USE.get())?8:-1;}
    public static final DeferredHolder<EntityType<?>,EntityType<StrawGolem>> ENTITY=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("golem"));
    public static final DeferredHolder<EntityType<?>,EntityType<GolemDart>> DART=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("golem_dart"));
    public static final DeferredHolder<EntityType<?>,EntityType<GolemBobber>> BOBBER=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("golem_bobber"));
    public static final DeferredHolder<MenuType<?>,MenuType<GolemMenu>> MENU=DeferredHolder.create(Registries.MENU,AlchemyContent.id("golem"));
    @SubscribeEvent static void register(RegisterEvent e){
        e.register(Registries.ITEM,h->{h.register(LIQUID.getId(),new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));h.register(ESSENTIA.getId(),new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));for(var core:java.util.List.of(USE,SORTING,FISHING))h.register(core.getId(),new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));for(int i=0;i<DECO.length();i++)h.register(AlchemyContent.id("golem_tiny_"+DECO_NAMES[i]),new Decoration(DECO.charAt(i)));h.register(LUMBER.getId(),new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));h.register(GUARD.getId(),new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));h.register(BUTCHER.getId(),new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));for(var m:GolemMaterial.values())h.register(placer(m).getId(),new Placer(m));h.register(HARVEST.getId(),new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));for(String name:UPGRADES)h.register(item("golem_upgrade_"+name).getId(),new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));h.register(FILL.getId(),new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));h.register(EMPTY.getId(),new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));h.register(FLESH_BLOCK.getId(),new FleshBlockItem());h.register(BLANK.getId(),new Item(new Item.Properties()));h.register(GATHER.getId(),new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));h.register(BELL.getId(),new Bell());});
        e.register(Registries.ENTITY_TYPE,h->{h.register(ENTITY.getId(),EntityType.Builder.<StrawGolem>of(StrawGolem::new,MobCategory.MISC).sized(.4f,.95f).clientTrackingRange(8).build(ENTITY.getId().toString()));h.register(DART.getId(),EntityType.Builder.<GolemDart>of(GolemDart::new,MobCategory.MISC).sized(.5f,.5f).clientTrackingRange(4).updateInterval(20).build(DART.getId().toString()));h.register(BOBBER.getId(),EntityType.Builder.<GolemBobber>of(GolemBobber::new,MobCategory.MISC).sized(.25f,.25f).noSave().clientTrackingRange(4).updateInterval(5).build(BOBBER.getId().toString()));});
        e.register(Registries.MENU,h->h.register(MENU.getId(),net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inv,buf)->new GolemMenu(id,inv,(StrawGolem)inv.player.level().getEntity(buf.readVarInt())))));
    }
    @SubscribeEvent static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e){e.put(ENTITY.get(),StrawGolem.attributes().build());}
    public record Reset() implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        static final Type<Reset> TYPE=new Type<>(AlchemyContent.id("golem_reset"));
        static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf,Reset> CODEC=net.minecraft.network.codec.StreamCodec.unit(new Reset());
        public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type(){return TYPE;}
    }
    @SubscribeEvent static void network(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e){e.registrar("tc4-golem-1").playToServer(Reset.TYPE,Reset.CODEC,(packet,context)->context.enqueueWork(()->Bell.reset(context.player())));}
    static void tag(ItemStack stack,CompoundTag tag){stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    static void sound(Entity e,String id){e.playSound(InfusionContent.MatrixEntity.sound(id),.5f,1);}
    public static final class Placer extends Item {
        final GolemMaterial material;Placer(GolemMaterial material){super(new Properties().stacksTo(1));this.material=material;}
        @Override public boolean doesSneakBypassUse(ItemStack stack,net.minecraft.world.level.LevelReader l,BlockPos pos,Player p){return true;}
        @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext c){return place(c);}
        @Override public InteractionResult useOn(UseOnContext c){return place(c);}
        private InteractionResult place(UseOnContext c){
            var p=c.getPlayer();if(p==null||p.isShiftKeyDown())return InteractionResult.PASS;
            if(c.getLevel().isClientSide)return InteractionResult.SUCCESS;
            var pos=c.getClickedPos().relative(c.getClickedFace());var g=new StrawGolem(ENTITY.get(),c.getLevel());g.material(material,true);
            double extra=c.getClickedFace()==Direction.UP&&c.getLevel().getBlockState(c.getClickedPos()).getBlock() instanceof net.minecraft.world.level.block.FenceBlock?.5:0;
            g.moveTo(pos.getX()+.5,pos.getY()+extra,pos.getZ()+.5,c.getLevel().random.nextFloat()*360,0);g.home=pos;g.homeFace=c.getClickedFace();g.owner=p.getUUID();g.restoreItem(c.getItemInHand());g.restrictTo(pos,(int)Math.ceil(g.range()));
            g.setHealth(g.getMaxHealth());if(stackHasName(c.getItemInHand()))g.setCustomName(c.getItemInHand().getHoverName());
            c.getLevel().addFreshEntity(g);if(!p.getAbilities().instabuild)c.getItemInHand().shrink(1);return InteractionResult.CONSUME;
        }
        private boolean stackHasName(ItemStack s){return s.has(DataComponents.CUSTOM_NAME);}
        @Override public void appendHoverText(ItemStack stack,TooltipContext c,java.util.List<Component> text,TooltipFlag f){var t=NoteData.tag(stack);if(t.contains("core")&&core(t.getByte("core"))!=Items.AIR)text.add(core(t.getByte("core")).getDescription().copy().withStyle(net.minecraft.ChatFormatting.GOLD));if(t.contains("advanced"))text.add(Component.translatable("tc.adv"));String deco=t.getString("deco");if(!deco.isEmpty()){var line=Component.empty().withStyle(net.minecraft.ChatFormatting.DARK_GREEN);for(int i=0;i<7;i++)if(deco.indexOf(DECO.charAt(i))>=0)line.append(decoration(i).getDescription()).append(" ");text.add(line);}int upgrades=t.getInt("Upgrades");for(int i=0;i<UPGRADES.length;i++){int count=(upgrades>>(i*2))&3;if(count>0)text.add(upgrade(i).getDescription().copy().append(" x"+count).withStyle(net.minecraft.ChatFormatting.BLUE));}}
    }
    /** Flatten only the original flesh metadata2; preserve other taint states' existing item mapping. */
    public static final class FleshBlockItem extends BlockItem {
        FleshBlockItem(){super(TC4Modern.TAINT.get(),new Item.Properties());}
        @Override public String getDescriptionId(){return "block.tc4modern.flesh_block";}
        @Override protected net.minecraft.world.level.block.state.BlockState getPlacementState(net.minecraft.world.item.context.BlockPlaceContext c){var state=super.getPlacementState(c);return state==null?null:state.setValue(TaintBlock.VARIANT,2);}
        @Override public void registerBlocks(java.util.Map<net.minecraft.world.level.block.Block,Item> map,Item item){}
    }
    /** ItemGolemDecoration: right-click a golem to add it (StrawGolem.addDecoration). */
    public static final class Decoration extends Item {
        final char code;Decoration(char code){super(new Properties());this.code=code;}
    }
    public static final class Bell extends Item {
        Bell(){super(new Properties().stacksTo(1));}
        static StrawGolem selected(ItemStack stack,Level l){var t=NoteData.tag(stack);if(!t.hasUUID("GolemUUID")||!t.getString("dimension").equals(l.dimension().location().toString()))return null;var id=t.getUUID("GolemUUID");var e=l.getEntity(t.getInt("golemid"));if(e instanceof StrawGolem g&&g.getUUID().equals(id))return g;
            /* Entity ids change after a reload; fall back to the stored UUID so the bell keeps its golem. */
            if(l instanceof net.minecraft.server.level.ServerLevel server)return server.getEntity(id) instanceof StrawGolem g?g:null;
            return null;}
        @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity e,InteractionHand hand){
            if(!(e instanceof StrawGolem g))return InteractionResult.PASS;
            if(!player.level().isClientSide){var t=NoteData.tag(stack);t.putUUID("GolemUUID",g.getUUID());t.putInt("golemid",g.getId());t.putLong("homePos",g.home.asLong());t.putByte("homeFace",(byte)g.homeFace.get3DDataValue());t.putString("dimension",g.level().dimension().location().toString());t.put("markers",g.markers.copy());tag(stack,t);g.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,.7f,1+g.getRandom().nextFloat()*.1f);}
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        @Override public boolean onLeftClickEntity(ItemStack stack,Player player,Entity e){
            if(e instanceof TravelingTrunk trunk){if(trunk.isRemoved()||trunk.upgrade()==3&&!trunk.isOwner(player))return false;if(e.level().isClientSide)return true;return trunk.retrieve(player);}
            if(!(e instanceof StrawGolem g))return false;
            if(!e.level().isClientSide&&g.isAlive()){
                var dropped=g.asItem();if(player.isShiftKeyDown()){tag(dropped,new CompoundTag());dropped.remove(DataComponents.CUSTOM_NAME);if(g.core()>=0)g.spawnAtLocation(core(g.core()));for(int i=0;i<UPGRADES.length;i++)for(int n=0;n<g.upgrades(i);n++)if(g.getRandom().nextBoolean())g.spawnAtLocation(upgrade(i));}
                g.spawnAtLocation(dropped,.5f);g.dropCarried();for(var hook:dev.eugheist.tc4modern.api.TC4Addons.golemHooks())hook.removed(g,player,player.isShiftKeyDown());sound(g,"zap");g.discard();
            }
            return true;
        }
        @Override public boolean doesSneakBypassUse(ItemStack s,net.minecraft.world.level.LevelReader l,BlockPos pos,Player p){return true;}
        @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext c){
            var g=selected(stack,c.getLevel());
            if(!c.getLevel().isClientSide){if(g!=null){int next=changeMarkers(g.markers,c.getClickedPos(),c.getClickedFace(),g.upgrades(4)>0,c.getPlayer()!=null&&c.getPlayer().isShiftKeyDown());
                var t=NoteData.tag(stack);t.put("markers",g.markers.copy());tag(stack,t);g.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,.7f,1);}}
            else if(g!=null&&g.upgrades(4)>0){/* ItemGolemBell.changeMarkers notifies the client about the new colour */int next=changeMarkers(NoteData.tag(stack).getList("markers",10).copy(),c.getClickedPos(),c.getClickedFace(),true,c.getPlayer()!=null&&c.getPlayer().isShiftKeyDown());if(next>=0)GolemBellClient.markerChanged(next);}
            return InteractionResult.sidedSuccess(c.getLevel().isClientSide);
        }
        /**
         * ItemGolemBell.changeMarkers: without the order upgrade a click toggles an any-colour marker; with it a marked face steps
         * through the sixteen colours and is removed after black or when sneaking. Returns the new colour, or -2 when none was set.
         */
        static int changeMarkers(net.minecraft.nbt.ListTag markers,BlockPos pos,net.minecraft.core.Direction face,boolean multiple,boolean sneaking){
            int found=-1,color=-1;for(int i=0;i<markers.size();i++){var t=markers.getCompound(i);if(t.getLong("pos")==pos.asLong()&&t.getByte("side")==face.get3DDataValue()&&(multiple||t.getByte("color")==-1)){found=i;color=t.getByte("color");break;}}
            if(found<0){var t=new CompoundTag();t.putLong("pos",pos.asLong());t.putByte("side",(byte)face.get3DDataValue());t.putByte("color",(byte)-1);markers.add(t);return -2;}
            markers.remove(found);if(!multiple||sneaking||++color>15)return -2;
            var t=new CompoundTag();t.putLong("pos",pos.asLong());t.putByte("side",(byte)face.get3DDataValue());t.putByte("color",(byte)color);markers.add(t);return color;
        }
        static void reset(Player p){var stack=p.getMainHandItem();if(!stack.is(BELL.get()))return;var g=selected(stack,p.level());if(g!=null){g.markers.clear();var t=NoteData.tag(stack);t.put("markers",g.markers.copy());tag(stack,t);g.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,.7f,1);}}
    }
}
