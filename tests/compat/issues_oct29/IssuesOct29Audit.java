package dev.eugheist.compat;

import dev.eugheist.tc4modern.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import thaumcraft.api.aspects.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Native regression + rendered addon scan icons, on disposable full-pack worlds. */
@EventBusSubscriber(modid="compat_audit", value=Dist.CLIENT)
public final class IssuesOct29Audit {
    static boolean old(){return "old".equals(System.getProperty("issues29.variant"));}
    static int ticks, frames; static CompletableFuture<Void> work;
    static final Map<String,Object> results=new LinkedHashMap<>();
    static final String[] TAGS={"infernus","superbia","luxuria","gula","invidia","desidia","ira"};
    static void check(boolean good,String label){results.put(label,good);System.out.println("ISSUES29 "+(good?"PASS ":"FAIL ")+label);if(!good)throw new IllegalStateException(label);}
    static void observed(boolean good,String label){results.put(label,good);System.out.println("ISSUES29 OBSERVED "+label+"="+good);if(!old())check(good,label);}
    static Item item(String id){return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));}
    static Mob crab(net.minecraft.server.level.ServerPlayer p){var c=(Mob)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("tc4modern:eldritch_crab")).create(p.level());c.setNoAi(true);c.setNoGravity(true);c.moveTo(p.getX(),p.getY()+2,p.getZ(),0,0);p.serverLevel().addFreshEntity(c);c.startRiding(p,true);p.positionRider(c);return c;}
    static Arrow arrow(net.minecraft.server.level.ServerPlayer p){var a=new Arrow(EntityType.ARROW,p.level());a.setOwner(p);a.setPos(p.getEyePosition().add(0,-0.1,0));a.setBaseDamage(4);a.shoot(0,1,0,2,0);p.serverLevel().addFreshEntity(a);return a;}
    static boolean canHit(Projectile a,Entity target)throws Exception{var m=Projectile.class.getDeclaredMethod("canHitEntity",Entity.class);m.setAccessible(true);return (boolean)m.invoke(a,target);}
    static void mechanics(net.minecraft.server.level.ServerPlayer p)throws Exception{
        var l=p.serverLevel();p.getInventory().selected=0;p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setInvulnerable(true);p.getInventory().clearContent();p.teleportTo(l,96.5,200,96.5,0,-90);
        l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,l.getServer());l.setDayTime(6000);l.setWeatherParameters(100000,0,false,false);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-1;y<5;y++)l.setBlock(new BlockPos(96+x,200+y,96+z),(y==-1?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),3);
        var c=crab(p);var start=p.getEyePosition();var end=start.add(0,3,0);
        var hit=ProjectileUtil.getEntityHitResult(p,start,end,p.getBoundingBox().expandTowards(0,3,0).inflate(1),e->e.isPickable()&&!e.isSpectator(),9);
        boolean selected=hit!=null&&hit.getEntity()==c;observed(selected,"mounted_crab_native_melee_ray");
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));float health=c.getHealth();if(selected)p.attack(c);observed(c.getHealth()<health,"mounted_crab_sword_damage");
        c.invulnerableTime=0;c.setHealth(c.getMaxHealth());var a=arrow(p);observed(canHit(a,c),"own_arrow_can_hit_attached_crab");health=c.getHealth();System.out.println("ISSUES29 ARROW BEFORE eye="+p.getEyePosition()+" crab="+c.getBoundingBox()+" vel="+a.getDeltaMovement()+" health="+health+" rider="+c.getVehicle()+" nearby="+l.getEntities(a,a.getBoundingBox().expandTowards(a.getDeltaMovement()).inflate(1)));a.tick();System.out.println("ISSUES29 ARROW AFTER pos="+a.position()+" removed="+a.isRemoved()+" vel="+a.getDeltaMovement()+" health="+c.getHealth());observed(c.getHealth()<health,"own_arrow_actual_collision_damage");a.discard();c.discard();
        var pig=EntityType.PIG.create(l);pig.setNoAi(true);pig.moveTo(p.position());l.addFreshEntity(pig);pig.startRiding(p,true);a=arrow(p);check(!canHit(a,pig),"ordinary_passenger_protection_preserved");a.discard();pig.discard();
        var loose=(Mob)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("tc4modern:eldritch_crab")).create(l);loose.moveTo(p.getX()+3,p.getY(),p.getZ(),0,0);l.addFreshEntity(loose);a=arrow(p);check(canHit(a,loose),"unmounted_crab_remains_hittable");a.discard();loose.discard();
        var focus=item("ttinkerer:focus_smelt");check(focus!=Items.AIR,"native_tinkerer_smelt_focus_present");
        var smelt=focus.getClass().getDeclaredMethod("smelt",ItemStack.class,net.minecraft.world.entity.player.Player.class,BlockPos.class);smelt.setAccessible(true);
        var fake=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(l);fake.getAbilities().instabuild=false;fake.setPos(96,200,96);
        var wand=new ItemStack(TC4Modern.GOLD_GREATWOOD_WAND.get());for(var aspect:Aspect.getPrimalAspects())WandData.add(wand,aspect,1000,true);FociContent.setFocus(wand,new ItemStack(focus));fake.setItemSlot(EquipmentSlot.MAINHAND,wand);
        var pos=new BlockPos(98,200,96);Block[][] cases={{Blocks.RAW_IRON_BLOCK,Blocks.IRON_BLOCK},{Blocks.RAW_GOLD_BLOCK,Blocks.GOLD_BLOCK},{Blocks.RAW_COPPER_BLOCK,Blocks.COPPER_BLOCK},{Blocks.SAND,Blocks.GLASS}};
        for(var pair:cases){l.setBlockAndUpdate(pos,pair[0].defaultBlockState());int fire=WandData.get(wand,Aspect.FIRE),entropy=WandData.get(wand,Aspect.ENTROPY);boolean early=false;for(int i=0;i<20;i++)early|=(boolean)smelt.invoke(focus,wand,fake,pos);check(!early&&l.getBlockState(pos).is(pair[0]),"warmup_"+pair[0]);boolean done=(boolean)smelt.invoke(focus,wand,fake,pos)&&l.getBlockState(pos).is(pair[1]);if(pair[0]==Blocks.SAND)check(done,"existing_sand_glass_recipe");else observed(done,"conversion_"+pair[0]);check(WandData.get(wand,Aspect.FIRE)==fire-(done?45:0)&&WandData.get(wand,Aspect.ENTROPY)==entropy-(done?12:0),"single_vis_debit_"+pair[0]);}
        var empty=new ItemStack(TC4Modern.GOLD_GREATWOOD_WAND.get());FociContent.setFocus(empty,new ItemStack(focus));l.setBlockAndUpdate(pos,Blocks.RAW_IRON_BLOCK.defaultBlockState());for(int i=0;i<30;i++)check(!(boolean)smelt.invoke(focus,empty,fake,pos),"empty_wand_no_conversion_"+i);check(l.getBlockState(pos).is(Blocks.RAW_IRON_BLOCK),"empty_wand_preserves_raw_block");
        l.setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());for(int i=0;i<25;i++)smelt.invoke(focus,wand,fake,pos);check(l.getBlockState(pos).is(Blocks.CHEST),"block_entity_preserved");l.removeBlock(pos,false);
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> deny=e->{if(e.getPos().equals(pos))e.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.NORMAL,false,net.neoforged.neoforge.event.level.BlockEvent.BreakEvent.class,deny);
        l.setBlockAndUpdate(pos,Blocks.SAND.defaultBlockState());int before=WandData.get(wand,Aspect.FIRE);for(int i=0;i<30;i++)smelt.invoke(focus,wand,fake,pos);check(l.getBlockState(pos).is(Blocks.SAND)&&WandData.get(wand,Aspect.FIRE)==before,"protection_event_cancels_without_vis_debit");net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(deny);l.removeBlock(pos,false);
        // A test-only scan target carrying all addon aspects exercises the real host renderers.
        var tags=new AspectList();for(String tag:TAGS)tags.add(Aspect.getAspect(tag),2);
        Class.forName("dev.eugheist.tc4modern.api.TC4Addons").getMethod("registerObjectTag",ResourceLocation.class,AspectList.class).invoke(null,ResourceLocation.parse("minecraft:netherrack"),tags);
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(TC4Modern.THAUMOMETER.get()));l.setBlockAndUpdate(new BlockPos(96,201,99),Blocks.NETHERRACK.defaultBlockState());p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.teleportTo(l,96.5,200,96.5,0,0);
    }
    static class ScanScreen extends AbstractContainerScreen<InventoryMenu>{
        ScanScreen(Minecraft mc){super(mc.player.inventoryMenu,mc.player.getInventory(),Component.literal("Native inventory scanning: all seven Forbidden Magic aspects"));}
        @Override public Slot getSlotUnderMouse(){return menu.getSlot(36);}
        @Override protected void renderBg(GuiGraphics g,float pt,int mx,int my){g.fill(0,0,width,height,0xff292333);g.drawString(font,getTitle(),12,12,0xffffff);}
        @Override protected void renderLabels(GuiGraphics g,int mx,int my){}
        @Override public boolean isPauseScreen(){return false;}
    }
    static void prepareKnowledge(){ClientKnowledge.knowledge.putBoolean("debugAllItemScans",true);var tags=ClientKnowledge.knowledge.getCompound("scanned");tags.putBoolean("minecraft:netherrack",true);ClientKnowledge.knowledge.put("scanned",tags);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){if(System.getProperty("issues29.variant")==null)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null)return;
        try{
            if(++ticks==40){work=CompletableFuture.runAsync(()->{try{mechanics(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()));}catch(Exception e){throw new CompletionException(e);}},mc.getSingleplayerServer());}
            if(work==null)return;if(!work.isDone()){ticks=40;return;}work.join();prepareKnowledge();mc.player.setYRot(0);mc.player.setXRot(0);
            if(ticks==80){for(String tag:TAGS){var aspect=Aspect.getAspect(tag);check(aspect!=null&&mc.getResourceManager().getResource(aspect.getImage()).isPresent(),"registered_texture_exists_"+tag);}check(ScanTarget.item(new ItemStack(Items.NETHERRACK),mc.level).aspects().keySet().containsAll(List.of(TAGS)),"scan_target_contains_all_seven_addon_aspects");mc.player.getInventory().setItem(0,new ItemStack(Items.NETHERRACK));mc.player.inventoryMenu.setCarried(new ItemStack(TC4Modern.THAUMOMETER.get()));mc.setScreen(new ScanScreen(mc));}
            if(ticks==105)shot(mc,"01-native-inventory-aspects");
            if(ticks==110){mc.setScreen(null);mc.player.inventoryMenu.setCarried(ItemStack.EMPTY);mc.player.getInventory().setItem(0,new ItemStack(TC4Modern.THAUMOMETER.get()));}
            if(ticks==140)shot(mc,"02-thaumometer-first-person");
            if(ticks==145){mc.player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);mc.player.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(TC4Modern.THAUMOMETER.get()));var server=mc.getSingleplayerServer();server.execute(()->{var p=server.getPlayerList().getPlayer(mc.player.getUUID());p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(TC4Modern.THAUMOMETER.get()));});ThaumometerFirstPerson.readoutVisible=false;}
            if(ticks==170)shot(mc,"03-thaumometer-hud");
            if(ticks==180){Files.writeString(out().resolve("results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(results));System.out.println("ISSUES29 DONE "+System.getProperty("issues29.variant"));System.out.flush();Runtime.getRuntime().halt(0);}
        }catch(Throwable x){x.printStackTrace();System.out.println("ISSUES29 CRASH");System.out.flush();Runtime.getRuntime().halt(3);}
    }
    static Path out(){return Path.of(System.getProperty("issues29.out"));}
    static void shot(Minecraft mc,String name)throws Exception{Files.createDirectories(out());try(var img=Screenshot.takeScreenshot(mc.getMainRenderTarget())){img.writeToFile(out().resolve(name+".png"));}System.out.println("ISSUES29 SCREENSHOT "+name);}
}
