package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class ChargerAudit {
 static boolean enabled(){return Boolean.getBoolean("tc4modern.chargerAudit");}
 static final BlockPos TABLE=new BlockPos(0,120,5),SOURCE=new BlockPos(-4,122,5);
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_CHARGER_AUDIT FAIL "+s);System.out.println("TC4_CHARGER_AUDIT PASS "+s);}
 @SubscribeEvent static void join(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(!enabled()||!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p))return;
 var l=p.serverLevel();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();p.getAbilities().invulnerable=true;p.onUpdateAbilities();
 for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)for(int y=-1;y<=6;y++)l.setBlock(TABLE.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
 l.setBlock(TABLE,TC4Modern.ARCANE_WORKBENCH.get().defaultBlockState(),3);l.setBlock(TABLE.above(),VisChargeRelay.BLOCK.get().defaultBlockState(),3);l.setBlock(SOURCE,TC4Modern.ENERGIZED_NODE.get().defaultBlockState(),3);
 var node=(EnergizedNodeEntity)l.getBlockEntity(SOURCE);for(var a:Aspect.getPrimalAspects())node.auraBase.add(a,64);node.energy.setup(node.auraBase,node.nodeType,node.modifier,new java.util.Random(0));node.changed();
 var table=(ArcaneWorkbenchEntity)l.getBlockEntity(TABLE);table.inventory.setItem(9,WandParts.make(WandParts.CAPS.get("gold"),WandParts.RODDEFS.get("greatwood")));
 p.getInventory().setItem(0,new ItemStack(VisChargeRelay.ITEM.get()));p.getInventory().selected=2;p.connection.teleport(3.5,120,.5,30,0);
 var recipe=ArcaneRecipes.entries().stream().filter(r->r.key().equals("NodeChargeRelay")).findFirst().orElseThrow();var grid=new java.util.ArrayList<ItemStack>();for(var i:recipe.ingredients())grid.add(i.isEmpty()?ItemStack.EMPTY:i.getItems()[0].copy());check(recipe.matches(grid,java.util.Set.of("VISCHARGERELAY"))&&!recipe.matches(grid,java.util.Set.of()),"arcane recipe research gate");check(recipe.cost().visSize()==48,"original three aspects16");
 var ward=InfusionItemRecipes.all().stream().filter(r->r.key().equals("FocusWarding")).findFirst().orElseThrow();check(ward.central().get()==net.minecraft.world.item.Items.NETHER_STAR&&ward.components().getFirst().get()==AlchemyContent.QUICKSILVER.get(),"MCP verified warding nether star and quicksilver");
 var adv=InfusionItemRecipes.all().stream().filter(r->r.key().equals("NodeStabilizerAdv")).findFirst().orElseThrow();check(adv.components().get(1).get()==net.minecraft.world.item.Items.REDSTONE_BLOCK,"MCP verified advanced stabilizer redstone blocks");
 }
 @EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
 static class Client {static int ticks,lastSeen=-1,samples,changes,rollbacks;
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!enabled())return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
 if(ticks==20){mc.player.getInventory().selected=2;server.execute(()->{var l=server.overworld();for(var mob:l.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(TABLE).inflate(80)))mob.discard();for(var item:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(TABLE).inflate(80)))item.discard();});}
 if(ticks==80){server.execute(()->{var l=server.overworld();var table=(ArcaneWorkbenchEntity)l.getBlockEntity(TABLE);var charger=(VisChargeRelay.Entity)l.getBlockEntity(TABLE.above());var source=(EnergizedNodeEntity)l.getBlockEntity(SOURCE);check(charger.parent()!=null,"native vis network connected");var wand=table.inventory.getItem(9);check(WandData.get(wand,Aspect.AIR)>0,"native ticking charges workbench wand");
 for(var a:Aspect.getPrimalAspects()){net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,wand,t->t.putInt(a.getTag(),0));}source.energy.vis=source.energy.visBase.copy();VisChargeRelay.Entity.tick(l,TABLE.above(),charger.getBlockState(),charger);for(var a:Aspect.getPrimalAspects())check(WandData.get(wand,a)==5,"exact five centivis per tick "+a.getTag());
 for(var a:Aspect.getPrimalAspects()){net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,wand,t->t.putInt(a.getTag(),WandData.capacity(wand)-2));}source.energy.vis=source.energy.visBase.copy();VisChargeRelay.Entity.tick(l,TABLE.above(),charger.getBlockState(),charger);for(var a:Aspect.getPrimalAspects())check(WandData.get(wand,a)==WandData.capacity(wand),"capacity clamp "+a.getTag());int before=source.energy.vis.visSize();VisChargeRelay.Entity.tick(l,TABLE.above(),charger.getBlockState(),charger);check(source.energy.vis.visSize()==before,"full wand does not drain source");
 table.inventory.setItem(9,WandParts.make(WandParts.CAPS.get("gold"),WandParts.RODDEFS.get("greatwood")));});shot(mc,"charger-connected.png");}
 if(ticks==90){tooltipWidth(mc);var table=(ArcaneWorkbenchEntity)mc.level.getBlockEntity(TABLE);check(table!=null&&WandData.isWand(table.inventory.getItem(9)),"workbench wand synchronized to client BER");shot(mc,"workbench-wand-visible.png");}
 if(ticks==100)server.execute(()->{var p=server.getPlayerList().getPlayer(id);var table=(ArcaneWorkbenchEntity)p.level().getBlockEntity(TABLE);p.openMenu(new net.minecraft.world.SimpleMenuProvider((n,i,u)->new ArcaneWorkbenchMenu(n,i,net.minecraft.world.inventory.ContainerLevelAccess.create(p.level(),TABLE),table.inventory),net.minecraft.network.chat.Component.translatable("block.tc4modern.arcane_workbench")));});
 // Live client view while charging: the wand in the open workbench menu must never show a lower amount than the previous frame.
 if(ticks>=105&&ticks<150&&mc.player.containerMenu instanceof ArcaneWorkbenchMenu menu)for(var slot:menu.slots)if(WandData.isWand(slot.getItem())){int now=WandData.get(slot.getItem(),Aspect.AIR);if(now<lastSeen){rollbacks++;System.out.println("TC4_CHARGER_ROLLBACK tick="+ticks+" "+lastSeen+"->"+now);}if(now!=lastSeen)changes++;lastSeen=now;samples++;break;}
 if(ticks==140)shot(mc,"charging-workbench.png");if(ticks==150){System.out.println("TC4_CHARGER_LIVE samples="+samples+" changes="+changes+" rollbacks="+rollbacks+" last="+lastSeen);check(samples>=30&&changes>=10,"open workbench shows the wand charging live");check(rollbacks==0,"client wand amount never rolls back while charging");mc.player.closeContainer();}
 if(ticks==155)server.execute(()->server.getPlayerList().getPlayer(id).connection.teleport(2.2,122,2.5,45,40));
 if(ticks==180)shot(mc,"workbench-wand-close.png");
 if(ticks==160)server.execute(()->{var l=server.overworld();var charger=(VisChargeRelay.Entity)l.getBlockEntity(TABLE.above());charger.setShardAttunement(1);var saved=charger.saveWithoutMetadata(l.registryAccess());var loaded=new VisChargeRelay.Entity(TABLE.above(),charger.getBlockState());loaded.loadWithComponents(saved,l.registryAccess());check(loaded.relay.color==1,"attunement NBT persists");l.removeBlock(SOURCE,false);});
 if(ticks==210){server.execute(()->{var l=server.overworld();var charger=(VisChargeRelay.Entity)l.getBlockEntity(TABLE.above());check(charger.parent()==null,"source removal disconnects charger");var table=(ArcaneWorkbenchEntity)l.getBlockEntity(TABLE);int before=WandData.get(table.inventory.getItem(9),Aspect.AIR);VisChargeRelay.Entity.tick(l,TABLE.above(),charger.getBlockState(),charger);check(WandData.get(table.inventory.getItem(9),Aspect.AIR)==before,"disconnected charger adds no vis");});shot(mc,"charger-disconnected.png");}
 if(ticks==220)server.execute(()->{var l=server.overworld();((ArcaneWorkbenchEntity)l.getBlockEntity(TABLE)).inventory.setItem(9,ItemStack.EMPTY);});
 if(ticks==230){check(((ArcaneWorkbenchEntity)mc.level.getBlockEntity(TABLE)).inventory.getItem(9).isEmpty(),"wand removal synchronized to client BER");shot(mc,"workbench-wand-removed.png");}
 if(ticks==240)server.execute(()->{var l=server.overworld();l.removeBlock(TABLE.above(),false);((ArcaneWorkbenchEntity)l.getBlockEntity(TABLE)).inventory.setItem(9,WandParts.make(WandParts.CAPS.get("gold"),WandParts.RODDEFS.get("greatwood")));});
 if(ticks==260)shot(mc,"workbench-wand-without-charger.png");
 if(ticks==270)mc.stop();}
 static void tooltipWidth(net.minecraft.client.Minecraft mc){
 var wand=WandParts.make(WandParts.CAPS.get("gold"),WandParts.RODDEFS.get("greatwood"));int min=Integer.MAX_VALUE,max=0;
 for(int amount=1000;amount<1100;amount+=5){
 final int value=amount;
 net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,wand,t->{for(var a:Aspect.getPrimalAspects())t.putInt(a.getTag(),value);});
 var lines=new java.util.ArrayList<net.minecraft.network.chat.Component>();wand.getItem().appendHoverText(wand,net.minecraft.world.item.Item.TooltipContext.of(mc.level),lines,net.minecraft.world.item.TooltipFlag.NORMAL);
 int width=mc.font.width(lines.get(1));min=Math.min(min,width);max=Math.max(max,width);
 }
 System.out.println("TC4_CHARGER_TOOLTIP_WIDTH min="+min+" max="+max);
 check(min==max,"charging tooltip numeric line width stable across whole/tenth/hundredth vis");
 }
 static void shot(net.minecraft.client.Minecraft mc,String name){try{var p=java.nio.file.Path.of("charger-audit");java.nio.file.Files.createDirectories(p);try(var img=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){img.writeToFile(p.resolve(name));}}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
 }
}
