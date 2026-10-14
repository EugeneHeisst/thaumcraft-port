package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;

/** Actual book requests across original easy/normal/hard rules and client knowledge synchronization. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class ResearchRulesAudit {
    static int ticks;static volatile int checkpoint;
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_RESEARCH_RULES FAIL "+text);System.out.println("TC4_RESEARCH_RULES PASS "+text);}
    static int notes(net.minecraft.server.level.ServerPlayer player,String key){int count=0;for(ItemStack stack:player.getInventory().items){var note=NoteData.read(stack);if(note!=null&&note.key.equals(key))count+=stack.getCount();}return count;}
    static void request(Minecraft mc,String key){PacketDistributor.sendToServer(new ResearchRequestPayload(mc.player.containerMenu.containerId,key));}
    static void hoverCenter(Minecraft mc){try{var callback=net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove",long.class,double.class,double.class);callback.setAccessible(true);callback.invoke(mc.mouseHandler,mc.getWindow().getWindow(),mc.getWindow().getScreenWidth()/2.0+5*mc.getWindow().getGuiScale(),mc.getWindow().getScreenHeight()/2.0+5*mc.getWindow().getGuiScale());}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.researchRulesAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        ticks++;if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
        if(ticks==50)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);thaumcraft.common.config.ConfigResearchData.init();p.removeAllEffects();p.setHealth(p.getMaxHealth());p.setInvulnerable(true);p.getFoodData().setFoodLevel(20);var pos=new net.minecraft.core.BlockPos(35,180,35);for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-1;y<=4;y++)p.serverLevel().setBlock(pos.offset(x,y,z),y==-1?net.minecraft.world.level.block.Blocks.STONE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);p.connection.teleport(35.5,180,35.5,0,0);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(TC4Modern.THAUMONOMICON.get()));
            p.getPersistentData().remove(ResearchKnowledge.KEY);var data=new CompoundTag();var list=new ListTag();
            for(var category:ResearchCategories.researchCategories.values())for(var item:category.research.values())if(!java.util.Set.of("GOGGLES","BELLOWS","GOLEMSTRAW","COREGATHER","GOLEMBELL","DISTILESSENTIA","JARLABEL").contains(item.key))list.add(StringTag.valueOf(item.key));data.put("Research",list);p.getPersistentData().put(ResearchKnowledge.KEY,data);
            ResearchConfig.DIFFICULTY.set(-1);ResearchConfig.ASPECT_CAP.set(9);
            for(int initial:new int[]{8,9,12}){
                var scan=new CompoundTag();ResearchKnowledge.initialize(scan);var known=ResearchKnowledge.load(scan);known.getAspectsDiscovered("current").aspects.put(Aspect.AIR,initial);ResearchKnowledge.save(scan,known);
                check(ResearchKnowledge.scan(scan,"cap:"+initial,java.util.Map.of("aer",25)).equals("scanned"),"actual scan accepted at pool "+initial);
                int expected=initial==8?25:initial==9?5:1;check(ResearchKnowledge.points(scan,"aer")==initial+expected,"original soft-cap award "+expected+" at pool "+initial);
            }
            ThaumometerItem.sync(p,"");check(!p.getPersistentData().getCompound(ResearchKnowledge.KEY).contains("Rules"),"server rules are transient snapshot metadata");
            p.openMenu(new SimpleMenuProvider(ThaumonomiconMenu::new,Component.literal("Research rules audit")));checkpoint=1;
        });
        if(ticks==90){check(checkpoint==1,"server fixture ready");check(ClientKnowledge.researchDifficulty()==-1&&ClientKnowledge.knowledge.getCompound("Rules").getInt("aspectTotalCap")==9,"client received authoritative easy mode and soft cap");check(ResearchConfig.purchaseMode(ResearchCategories.getResearch("GOGGLES"),ClientKnowledge.researchDifficulty()),"primary research becomes purchasable in easy mode");request(mc,"GOGGLES");}
        if(ticks==120)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);check(!ResearchProgress.completed(p.getPersistentData()).contains("GOGGLES"),"insufficient easy purchase refuses completion");check(notes(p,"GOGGLES")==0,"failed purchase does not issue a note");ResearchKnowledge.grantAll(p.getPersistentData());ThaumometerItem.sync(p,"");checkpoint=2;});
        if(ticks==150){check(checkpoint==2,"aspect funding ready");check(ResearchBrowserTooltip.enough(ResearchCategories.getResearch("GOGGLES")),"cost tooltip reports sufficient discovered aspects");var screen=(ThaumonomiconScreen)mc.screen;screen.auditBrowser("ARTIFICE",4,1);hoverCenter(mc);}
        if(ticks==160){LiquidDeathAudit.shot(mc,"research-easy-cost.png");request(mc,"GOGGLES");}
        if(ticks==190)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);check(ResearchProgress.completed(p.getPersistentData()).contains("GOGGLES"),"actual easy request completes primary research without paper or ink");for(Aspect a:ResearchCategories.getResearch("GOGGLES").tags.getAspects())check(ResearchKnowledge.points(p.getPersistentData(),a.getTag())==6,"easy purchase exact cost "+a.getTag());check(notes(p,"GOGGLES")==0,"easy purchase never issues research notes");ResearchConfig.DIFFICULTY.set(1);p.getInventory().setItem(1,new ItemStack(Items.PAPER,2));p.getInventory().setItem(2,new ItemStack(TC4Modern.SCRIBING_TOOLS.get()));ThaumometerItem.sync(p,"");p.inventoryMenu.broadcastChanges();checkpoint=3;});
        if(ticks==220){check(ResearchPopup.highlighted.contains("GOGGLES")&&ResearchPopup.highlighted.contains("ARTIFICE"),"actual easy purchase sends completion popup and category highlight");check(checkpoint==3&&ClientKnowledge.researchDifficulty()==1,"client receives hard mode update");check(!ResearchConfig.purchaseMode(ResearchCategories.getResearch("BELLOWS"),ClientKnowledge.researchDifficulty()),"secondary research needs a note in hard mode");check(ResearchBrowserTooltip.noteHint(ResearchCategories.getResearch("BELLOWS"),mc.player.getInventory()).equals("tc.research.getprim"),"primary tooltip detects paper and usable ink");request(mc,"BELLOWS");}
        if(ticks==250)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);check(!ResearchProgress.completed(p.getPersistentData()).contains("BELLOWS")&&notes(p,"BELLOWS")==1,"actual hard request creates secondary note without completion; notes="+notes(p,"BELLOWS")+" completed="+ResearchProgress.completed(p.getPersistentData()).contains("BELLOWS")+" clientStatus="+ClientKnowledge.status+" menu="+p.containerMenu+" inventory="+p.getInventory().items);check(p.getInventory().getItem(1).getCount()==1&&p.getInventory().getItem(2).getDamageValue()==1,"note spends exactly one paper and one ink durability");for(Aspect a:ResearchCategories.getResearch("BELLOWS").tags.getAspects())check(ResearchKnowledge.points(p.getPersistentData(),a.getTag())==9,"hard note preserves research points "+a.getTag());checkpoint=4;});
        if(ticks==280){check(checkpoint==4,"hard request server checked");check(ResearchBrowserTooltip.noteHint(ResearchCategories.getResearch("BELLOWS"),mc.player.getInventory()).equals("tc.research.hasnote"),"tooltip recognizes an existing note");request(mc,"BELLOWS");}
        if(ticks==310)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);check(notes(p,"BELLOWS")==1&&p.getInventory().getItem(1).getCount()==1&&p.getInventory().getItem(2).getDamageValue()==1,"duplicate note request spends no materials");for(int i=1;i<p.getInventory().getContainerSize();i++)p.getInventory().setItem(i,ItemStack.EMPTY);ResearchConfig.DIFFICULTY.set(0);ThaumometerItem.sync(p,"");p.inventoryMenu.broadcastChanges();checkpoint=5;});
        if(ticks==340){check(checkpoint==5&&ClientKnowledge.researchDifficulty()==0,"normal rules synchronized");check(ResearchConfig.purchaseMode(ResearchCategories.getResearch("BELLOWS"),0)&&!ResearchConfig.purchaseMode(ResearchCategories.getResearch("GOGGLES"),0),"normal mode retains original secondary/primary distinction");request(mc,"BELLOWS");}
        if(ticks==370)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);check(ResearchProgress.completed(p.getPersistentData()).contains("BELLOWS"),"actual normal request purchases secondary research");for(Aspect a:ResearchCategories.getResearch("BELLOWS").tags.getAspects())check(ResearchKnowledge.points(p.getPersistentData(),a.getTag())==9-ResearchCategories.getResearch("BELLOWS").tags.getAmount(a),"normal purchase exact cost "+a.getTag());checkpoint=6;});
        if(ticks==400){check(ResearchPopup.highlighted.contains("BELLOWS"),"actual normal purchase sends completion popup");ResearchPopup.highlighted.clear();check(checkpoint==6,"normal request checked");request(mc,"BELLOWS");}
        if(ticks==430)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);for(Aspect a:ResearchCategories.getResearch("BELLOWS").tags.getAspects())check(ResearchKnowledge.points(p.getPersistentData(),a.getTag())==9-ResearchCategories.getResearch("BELLOWS").tags.getAmount(a),"replayed completed request cannot charge twice "+a.getTag());ResearchConfig.DIFFICULTY.set(-1);ResearchConfig.ASPECT_CAP.set(100);ResearchKnowledge.grantAll(p.getPersistentData());ThaumometerItem.sync(p,"");checkpoint=7;});
        if(ticks==460){check(checkpoint==7,"all initial server phases finished");check(!ResearchPopup.highlighted.contains("BELLOWS"),"replayed purchase does not issue duplicate completion popup");request(mc,"GOLEMSTRAW");}
        if(ticks==490)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);check(ResearchProgress.completed(p.getPersistentData()).containsAll(java.util.Set.of("GOLEMSTRAW","COREGATHER","GOLEMBELL")),"actual purchase completes both original sibling entries");checkpoint=8;});
        if(ticks==520){check(checkpoint==8&&ResearchPopup.highlighted.containsAll(java.util.Set.of("GOLEMSTRAW","COREGATHER","GOLEMBELL","GOLEMANCY")),"server sends original completion notifications for main research and both siblings");var screen=(ThaumonomiconScreen)mc.screen;screen.auditBrowser("ARTIFICE",-4,-1);hoverCenter(mc);}
        if(ticks==530)LiquidDeathAudit.shot(mc,"research-forbidden-warning.png");
        if(ticks==550)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);ResearchConfig.DIFFICULTY.set(0);ResearchConfig.ASPECT_CAP.set(100);p.setInvulnerable(false);p.closeContainer();
            var tag=thaumcraft.common.lib.research.ResearchManager.createNote(new CompoundTag(),"DISTILESSENTIA",new java.util.Random(426351));tag.putBoolean("complete",true);
            var note=new ItemStack(TC4Modern.RESEARCH_NOTES.get());note.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));p.getInventory().setItem(4,note);p.getInventory().setItem(5,note.copy());p.inventoryMenu.broadcastChanges();checkpoint=9;
        });
        if(ticks==580){check(checkpoint==9&&mc.player.getInventory().getItem(4).is(TC4Modern.RESEARCH_NOTES.get()),"completed-note fixture synchronized");mc.player.getInventory().selected=4;mc.gameMode.useItem(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND);}
        if(ticks==610)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);check(ResearchProgress.completed(p.getPersistentData()).containsAll(java.util.Set.of("DISTILESSENTIA","JARLABEL")),"actual completed-note item use learns main entry and original stub sibling");check(p.getInventory().getItem(4).isEmpty()&&p.getInventory().getItem(5).getCount()==1,"actual completed-note use consumes exactly one item");checkpoint=10;});
        if(ticks==640){check(checkpoint==10&&ResearchPopup.highlighted.containsAll(java.util.Set.of("DISTILESSENTIA","JARLABEL","ALCHEMY")),"actual completed-note use notifies main entry and stub sibling");ResearchPopup.highlighted.clear();mc.player.getInventory().selected=5;mc.gameMode.useItem(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND);}
        if(ticks==670)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);check(p.getInventory().getItem(5).getCount()==1,"reusing an already learned completed note preserves the item");checkpoint=11;});
        if(ticks==700){check(checkpoint==11&&ResearchPopup.highlighted.isEmpty(),"duplicate completed-note use cannot notify twice");System.out.println("TC4_RESEARCH_RULES ALL PASS server-authoritative difficulty, actual book/note requests, exact costs, duplicate refusal, sibling notifications and scanning soft cap");mc.stop();}
    }
}
