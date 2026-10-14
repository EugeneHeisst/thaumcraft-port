package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Real creative GUI hover packets, debug command, and actual BlockItem placement in an isolated save. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class CreativeGameplayAudit {
    static int ticks;static volatile boolean ready,debugged;static volatile String failure;
    static final BlockPos POS=new BlockPos(35,180,35);
    static void check(boolean ok,String text){if(!ok)throw new IllegalStateException("TC4_CREATIVE_GAME FAIL "+text);System.out.println("TC4_CREATIVE_GAME PASS "+text);}
    static void prepare(net.minecraft.server.level.ServerPlayer p){
        var l=p.serverLevel();p.closeContainer();p.removeAllEffects();p.setHealth(p.getMaxHealth());p.setInvulnerable(true);p.setGameMode(GameType.CREATIVE);l.setDayTime(6000);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-1;y<=4;y++)l.setBlock(POS.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        p.connection.teleport(35.5,180,31.5,0,0);
        for(String id:java.util.List.of("greatwood_leaves","flux_goo","flux_gas","taint","taint_soil","taint_fibres","taint_grass","taint_plant","taint_spore")) {
            l.removeBlock(POS,false);var item=BuiltInRegistries.ITEM.get(AlchemyContent.id(id));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
            var result=p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(POS.below().getCenter().add(0,.5,0),Direction.UP,POS.below(),false)));
            check(result.consumesAction()&&l.getBlockState(POS).is(((BlockItem)item).getBlock()),"new creative block form places: "+id);
            if(item instanceof CreativeBlocks.Variant variant)check(l.getBlockState(POS).getValue(variant.property)==variant.value,"original block variant preserved: "+id);
            if(id.equals("taint_soil"))check(RecipeAspects.get(new ItemStack(item),l).equals(java.util.Map.of("terra",1,"vitium",3)),"new tainted soil retains original aspects");
        }
        var variants=new HashSet<String>();var types=new HashSet<String>();var modifiers=new HashSet<String>();
        for(int i=0;i<128;i++){
            l.removeBlock(POS,false);l.random.setSeed(8300+i);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TC4Modern.AURA_NODE_ITEM.get()));
            var result=p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(POS.below().getCenter().add(0,.5,0),Direction.UP,POS.below(),false)));
            check(result.consumesAction()&&l.getBlockEntity(POS) instanceof AuraNodeEntity,"creative BlockItem places node "+i);
            var n=(AuraNodeEntity)l.getBlockEntity(POS);var aspects=n.reservoir.aspects;check(aspects.size()>0&&aspects.visSize()>0,"generated node has aspects");
            check(java.util.Arrays.stream(aspects.getAspects()).allMatch(a->aspects.getAmount(a)==n.reservoir.aspectsBase.getAmount(a)),"base reservoir initialized");
            variants.add(java.util.Arrays.stream(aspects.getAspects()).map(a->a.getTag()+"="+aspects.getAmount(a)).sorted().collect(java.util.stream.Collectors.joining(",")));types.add(n.nodeType.name());modifiers.add(String.valueOf(n.reservoir.modifier));
        }
        check(variants.size()>80&&types.size()>1&&modifiers.size()>1,"128 placements vary aspects, node type and modifier: "+variants.size()+" / "+types+" / "+modifiers);
        // Preserve explicit custom node stacks while ordinary creative placement uses world generation.
        l.removeBlock(POS,false);var stack=new ItemStack(TC4Modern.AURA_NODE_ITEM.get());net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,stack,t->new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.AIR,73).writeToNBT(t));p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        stack.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(POS.below().getCenter().add(0,.5,0),Direction.UP,POS.below(),false)));
        check(((AuraNodeEntity)l.getBlockEntity(POS)).reservoir.aspects.getAmount(thaumcraft.api.aspects.Aspect.AIR)==73,"explicit node aspect component retained");
        l.removeBlock(POS,false);AuraNodeFeature.createRandomNodeAt(l,POS,l.random,false,false,false);
        p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(TC4Modern.THAUMOMETER.get()));p.getInventory().setItem(1,new ItemStack(TC4Modern.SHARD_ORE_ITEMS.get(0).get(),64));
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(TC4Modern.GOGGLES.get()));
        p.getPersistentData().remove(ResearchKnowledge.KEY);ResearchKnowledge.discoverAll(p.getPersistentData());ThaumometerItem.sync(p,"");p.inventoryMenu.broadcastChanges();ready=true;
    }
    static void hover(Minecraft mc,int slot){
        var screen=(CreativeModeInventoryScreen)mc.screen;var s=screen.getMenu().getSlot(slot);
        double x=(screen.width-195)/2.0+s.x+8,y=(screen.height-136)/2.0+s.y+8;
        try{var callback=net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove",long.class,double.class,double.class);callback.setAccessible(true);callback.invoke(mc.mouseHandler,mc.getWindow().getWindow(),x*mc.getWindow().getScreenWidth()/screen.width,y*mc.getWindow().getScreenHeight()/screen.height);}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
    }
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.creativeGameplayAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;if(failure!=null){mc.stop();throw new IllegalStateException(failure);}ticks++;
        var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
        if(ticks==40)server.execute(()->{try{prepare(server.getPlayerList().getPlayer(uuid));}catch(Throwable ex){failure=ex.toString();ex.printStackTrace();}});
        if(ticks==100){check(ready,"server fixture ready");LiquidDeathAudit.shot(mc,"creative-random-node.png");mc.setScreen(new CreativeModeInventoryScreen(mc.player,mc.player.connection.enabledFeatures(),false));
            var screen=(CreativeModeInventoryScreen)mc.screen;
            try{var select=CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab",CreativeModeTab.class);select.setAccessible(true);select.invoke(screen,TC4Modern.THAUMCRAFT_TAB.get());}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
            mc.player.containerMenu.setCarried(new ItemStack(TC4Modern.THAUMOMETER.get()));hover(mc,46);
        }
        if(ticks==110){var screen=(CreativeModeInventoryScreen)mc.screen;check(screen.getSlotUnderMouse()!=null&&screen.getSlotUnderMouse().index==46,"real creative cursor hovers hotbar ore");check(mc.player.containerMenu.getCarried().is(TC4Modern.THAUMOMETER.get()),"creative client cursor holds scanner");}
        if(ticks==160){var ore=new ItemStack(TC4Modern.SHARD_ORE_ITEMS.get(0).get());check(ResearchKnowledge.isScanned(ClientKnowledge.knowledge,ScanTarget.item(ore,mc.level).knowledgeKey()),"creative scan completed through actual hover packets");LiquidDeathAudit.shot(mc,"creative-scanned-ore.png");
            server.execute(()->{try{var p=server.getPlayerList().getPlayer(uuid);check(p.getPersistentData().getCompound(ResearchKnowledge.KEY).getCompound("scanned").getBoolean(ScanTarget.item(ore,p.level()).knowledgeKey()),"creative scan stored on server");
                check(server.getCommands().getDispatcher().execute("tc debug all",p.createCommandSourceStack().withPermission(4))==1,"actual debug command succeeds");debugged=true;
            }catch(Throwable ex){failure=ex.toString();ex.printStackTrace();}});
        }
        if(ticks==200){check(debugged,"debug command finished");
            int covered=0;for(var item:BuiltInRegistries.ITEM)if(item!=Items.AIR){check(ResearchKnowledge.isScanned(ClientKnowledge.knowledge,ScanTarget.item(new ItemStack(item),mc.level).knowledgeKey()),"debug scanned "+BuiltInRegistries.ITEM.getKey(item));covered++;}
            for(var item:CheatSheetAudit.tab())check(ResearchKnowledge.isScanned(ClientKnowledge.knowledge,ScanTarget.item(item,mc.level).knowledgeKey()),"debug covers creative variants");
            check(!ItemAspectOverlay.tags(new ItemStack(Items.DIAMOND),mc.level,ClientKnowledge.knowledge).isEmpty(),"debug immediately exposes item aspect tags");
            check(!ResearchKnowledge.isScanned(ClientKnowledge.knowledge,"node:never-scanned")&&!ResearchKnowledge.isScanned(ClientKnowledge.knowledge,"entity:never-scanned"),"debug item scan flag does not scan unseen nodes/entities");
            mc.player.containerMenu.setCarried(ItemStack.EMPTY);WarpClientConfig.DISPLAY_ASPECTS.set(true);hover(mc,46);System.out.println("TC4_CREATIVE_GAME debug covers "+covered+" registered items");
        }
        if(ticks==225){check(!ItemAspectOverlay.last.isEmpty()&&ItemAspectOverlay.last.stream().allMatch(ItemAspectOverlay.Tag::known),"actual creative tooltip renders known aspects after debug");LiquidDeathAudit.shot(mc,"creative-debug-aspects.png");mc.player.closeContainer();mc.setScreen(null);net.neoforged.neoforge.network.PacketDistributor.sendToServer(BaublesNetwork.Open.INSTANCE);}
        if(ticks==260){check(mc.screen instanceof BaublesScreen,"actual Baubles screen opens");check(mc.player.containerMenu.slots.subList(9,13).stream().allMatch(s->s.getItem().isEmpty()),"four empty accessory slots visible");LiquidDeathAudit.shot(mc,"baubles-empty-icons.png");System.out.println("TC4_CREATIVE_GAME ALL PASS");mc.stop();}
    }
}
