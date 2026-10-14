package dev.eugheist.tc4modern;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class StaffAudit {
 static boolean enabled(){return Boolean.getBoolean("tc4modern.staffAudit");}
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_STAFF_AUDIT FAIL "+s);System.out.println("TC4_STAFF_AUDIT PASS "+s);}
 @SubscribeEvent static void join(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(!enabled()||!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p))return;
 p.getAbilities().invulnerable=true;p.onUpdateAbilities();for(var mob:p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(p.blockPosition()).inflate(80)))mob.discard();
 p.getInventory().clearContent();p.getInventory().selected=0;p.connection.teleport(.5,120,.5,0,0);
 var cap=WandParts.CAPS.get("thaumium");var sceptre=WandParts.sceptre(cap,WandParts.RODDEFS.get("silverwood"));
 check(WandData.capacity(sceptre)==15000,"sceptre capacity 150 vis");check(Math.abs(WandData.craftingModifier(sceptre,p,Aspect.AIR)-.8f)<.001,"sceptre discount 10 percent");
 var entry=WandParts.sceptreEntry(cap,WandParts.RODDEFS.get("silverwood"));var grid=new java.util.ArrayList<ItemStack>();for(var i:entry.ingredients())grid.add(i.isEmpty()?ItemStack.EMPTY:i.getItems()[0].copy());
 var research=java.util.Set.of("SCEPTRE","CAP_thaumium","ROD_silverwood");check(WandParts.recipe(grid,research)!=null&&WandParts.recipe(grid,java.util.Set.of())==null,"sceptre recipe and research gate");check(entry.cost().getAmount(Aspect.AIR)==81,"original floor crafting cost");
 /* CAP_iron and ROD_wood are auto-unlocked in the original: an iron-capped wooden sceptre needs only SCEPTRE */
 var plain=WandParts.sceptreEntry(WandParts.CAPS.get("iron"),WandParts.RODDEFS.get("wood"));var plainGrid=new java.util.ArrayList<ItemStack>();for(var i:plain.ingredients())plainGrid.add(i.isEmpty()?ItemStack.EMPTY:i.getItems()[0].copy());
 var made=WandParts.recipe(plainGrid,java.util.Set.of("SCEPTRE"));check(made!=null&&WandParts.isSceptre(made.output())&&WandParts.recipe(plainGrid,java.util.Set.of())==null,"iron caps and the wooden rod need only SCEPTRE");
 var menu=new ArcaneWorkbenchMenu(0,p.getInventory());
 for(var rod:WandParts.RODDEFS.values())if(rod.staff()){var s=WandParts.make(cap,rod);check(!menu.getSlot(1).mayPlace(s),"workbench rejects "+rod.tag());}
 var staff=WandParts.make(cap,WandParts.RODDEFS.get("primal_staff"));int oldTicks=p.tickCount;p.tickCount=50;staff.getItem().inventoryTick(staff,p.level(),p,0,true);p.tickCount=oldTicks;int restored=0;for(var a:Aspect.getPrimalAspects())restored+=WandData.get(staff,a);check(restored==100,"exactly one vis on regeneration tick");FociContent.setFocus(staff,new ItemStack(PrimalFocus.FOCUS.get()));check(FociContent.getFocusPotency(staff)==1,"primal staff potency bonus");
 check(staff.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).modifiers().getFirst().modifier().amount()==6,"staff attack damage +6");
 check(InfusionItemRecipes.all().stream().anyMatch(r->r.key().equals("WandRodPrimalStaff")&&r.components().size()==8&&r.instability()==8),"primal staff infusion");
 for(var a:Aspect.getPrimalAspects())WandData.add(sceptre,a,15000,true);
 p.getInventory().setItem(0,staff);p.getInventory().setItem(1,sceptre);int slot=2;
 for(var r:WandParts.RODDEFS.values())if(r.staff()&&!r.runes())p.getInventory().setItem(slot++,WandParts.make(cap,r));
 }
 @EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
 static class Client {static int ticks;
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!enabled())return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;
 if(ticks==20)mc.player.getInventory().selected=0;
 if(ticks==80){var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{var p=server.getPlayerList().getPlayer(id);int sum=0;for(var a:Aspect.getPrimalAspects())sum+=WandData.get(p.getMainHandItem(),a);check(sum>0&&sum<2500&&sum%100==0,"primal regeneration one vis per fifty ticks; sum="+sum);});shot(mc,"primal-staff-hand.png");}
 if(ticks==90)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
 if(ticks==110)shot(mc,"staff-inventory.png");if(ticks==120)mc.setScreen(null);
 if(ticks==130)mc.player.getInventory().selected=1;
 if(ticks==150)shot(mc,"sceptre-hand.png");if(ticks==170)mc.stop();}
 static void shot(net.minecraft.client.Minecraft mc,String name){try{var p=java.nio.file.Path.of("staff-audit");java.nio.file.Files.createDirectories(p);try(var img=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){img.writeToFile(p.resolve(name));}}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
 }
}
