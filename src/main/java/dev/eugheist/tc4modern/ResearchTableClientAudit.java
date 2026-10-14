package dev.eugheist.tc4modern;

import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.*;
import thaumcraft.common.lib.utils.HexUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class ResearchTableClientAudit {
    private static boolean started;
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){var mc=Minecraft.getInstance();if(Boolean.getBoolean("tc4modern.tableAudit")&&!started&&mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen&&mc.getOverlay()==null){started=true;mc.setScreen(new Audit());}}
    private static final class Audit extends Screen {
        private TestTable screen;private ResearchTableMenu menu;private Path path;private int frame,stage,mx=-100,my=-100;private HexUtils.Hex target;
        private final List<String> sounds=new ArrayList<>();private final net.minecraft.nbt.CompoundTag data=new net.minecraft.nbt.CompoundTag();
        Audit(){super(Component.literal("TC4 research-table audit"));}
        private final class TestTable extends ResearchTableScreen {
            TestTable(ResearchTableMenu menu,Inventory inventory){super(menu,inventory,Component.empty());}
            @Override protected void sound(String id,float volume,boolean vary){sounds.add(id);super.sound(id,volume,vary);}
            @Override protected void place(HexUtils.Hex hex,Aspect aspect){var note=NoteData.read(menu.table.getItem(1));String result=ResearchKnowledge.place(data,note,hex.q,hex.r,aspect,.9f,menu.bonus);if(!result.equals("placed"))throw new AssertionError(result);NoteData.write(menu.table.getItem(1),note);sync();}
            @Override protected void combine(Aspect a,Aspect b){String result=ResearchKnowledge.combine(data,a.getTag(),b.getTag(),menu.bonus);if(!result.startsWith("combined:"))throw new AssertionError(result);sync();}
        }
        private void sync(){ClientKnowledge.knowledge=data.getCompound(ResearchKnowledge.KEY).copy();}
        @Override protected void init(){
            thaumcraft.common.config.ConfigResearchData.init();ResearchKnowledge.grantAll(data);var research=new net.minecraft.nbt.ListTag();for(String key:List.of("RESEARCHER1","RESEARCHER2","RESEARCHDUPE"))research.add(net.minecraft.nbt.StringTag.valueOf(key));data.getCompound(ResearchKnowledge.KEY).put("Research",research);sync();
            var inventory=new Inventory(null);menu=new ResearchTableMenu(0,inventory);menu.table.setItem(0,new ItemStack(TC4Modern.SCRIBING_TOOLS.get()));var stack=new ItemStack(TC4Modern.RESEARCH_NOTES.get());var note=thaumcraft.common.lib.research.ResearchManager.createNote(new net.minecraft.nbt.CompoundTag(),"NODETAPPER1",new Random(123));stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(note));menu.table.setItem(1,stack);menu.bonus.add(Aspect.AIR,1);
            target=NoteData.read(stack).hexes.values().stream().filter(h->NoteData.read(stack).hexEntries.get(h.toString()).type==0).findFirst().orElseThrow();screen=new TestTable(menu,inventory);screen.init(minecraft,width,height);path=minecraft.gameDirectory.toPath().resolve("table-audit");try{Files.createDirectories(path);}catch(Exception e){throw new IllegalStateException(e);}
        }
        private void drag(Aspect aspect,int[] to,boolean release){int[] from=screen.auditAspect(aspect);screen.mouseClicked(from[0],from[1],0);mx=to[0];my=to[1];screen.mouseDragged(mx,my,0,mx-from[0],my-from[1]);if(release)screen.mouseReleased(mx,my,0);}
        @Override public void render(GuiGraphics gui,int ignoredX,int ignoredY,float partial){
            gui.fill(0,0,width,height,0xff252525);screen.auditDraw(gui,mx,my);if(++frame<16)return;frame=0;gui.flush();
            try(var image=net.minecraft.client.Screenshot.takeScreenshot(minecraft.getMainRenderTarget())){image.writeToFile(path.resolve("stage-"+stage+".png"));}catch(Exception e){throw new IllegalStateException(e);}
            switch(stage++){
                case 0->{/* original Config.colorBlind: anchor orbs were drawn at full brightness, then darkened by 1.8 */if(ResearchTableScreen.lastOrbShade!=1)throw new AssertionError("Default orb shade");WarpClientConfig.COLOR_BLIND.set(true);drag(Aspect.AIR,screen.auditCell(target),false);}
                case 1->{if(Math.abs(ResearchTableScreen.lastOrbShade-1/1.8f)>1e-6)throw new AssertionError("color_blind orb shade "+ResearchTableScreen.lastOrbShade);WarpClientConfig.COLOR_BLIND.set(false);screen.mouseReleased(mx,my,0);if(NoteData.read(menu.table.getItem(1)).hexEntries.get(target.toString()).aspect!=Aspect.AIR)throw new AssertionError("Drop not placed");if(ResearchKnowledge.points(data,Aspect.AIR.getTag())!=99)throw new AssertionError("Point spending");}
                case 2->{screen.mouseClicked(mx,my,1);if(NoteData.read(menu.table.getItem(1)).hexEntries.get(target.toString()).type!=0)throw new AssertionError("Erase failed");}
                case 3->{drag(Aspect.AIR,screen.auditCombineSlot(false),true);drag(Aspect.FIRE,screen.auditCombineSlot(true),true);}
                case 4->{int[] button=screen.auditCombineButton();screen.mouseClicked(button[0],button[1],0);}
                case 5->{
                    var knowledge=ResearchKnowledge.load(data);knowledge.setAspectPool("current",Aspect.AIR,(short)0);ResearchKnowledge.save(data,knowledge);sync();
                    drag(Aspect.AIR,screen.auditCell(target),true);
                    if(menu.bonus.getAmount(Aspect.AIR)!=0||ResearchKnowledge.points(data,"aer")!=0||NoteData.read(menu.table.getItem(1)).hexEntries.get(target.toString()).aspect!=Aspect.AIR)throw new AssertionError("Reserve spending");
                }
                case 6->{
                    var ink=menu.table.getItem(0);ink.setDamageValue(ink.getMaxDamage());
                    screen.mouseClicked(mx,my,1);
                    if(NoteData.read(menu.table.getItem(1)).hexEntries.get(target.toString()).type!=2)throw new AssertionError("Erasing without ink");
                }
                case 7->{
                    var note=NoteData.read(menu.table.getItem(1));note.hexes.clear();note.hexEntries.clear();
                    var anchor=new HexUtils.Hex(0,0);var middle=anchor.getNeighbour(0);var end=middle.getNeighbour(0);
                    int i=0;for(var hex:List.of(anchor,middle,end)){note.hexes.put(hex.toString(),hex);note.hexEntries.put(hex.toString(),new thaumcraft.common.lib.research.ResearchManager.HexEntry(new Aspect[]{Aspect.AIR,Aspect.LIGHT,Aspect.FIRE}[i],i++==0?1:2));}
                    NoteData.write(menu.table.getItem(1),note);
                    if(ResearchTableGraph.connected(note,ResearchKnowledge.load(data).getAspectsDiscovered("current")).edges().size()!=2)throw new AssertionError("Connected-chain highlight");
                }
                case 8->{var note=NoteData.read(menu.table.getItem(1));note.complete=true;note.copies=2;NoteData.write(menu.table.getItem(1),note);int[] button=screen.auditCopyButton();mx=button[0];my=button[1];}
                default->{if(!sounds.containsAll(List.of("hhoff","hhon","write","erase","cameraclack")))throw new AssertionError("Missing SFX: "+sounds);try{Files.writeString(path.resolve("result.txt"),"PASS: native render; color_blind orb shade; drag/drop; point spending; erase; combine slots; combine; reserve spending; no-ink refusal; original SFX requests\n"+sounds);}catch(Exception e){throw new IllegalStateException(e);}minecraft.stop();}
            }
        }
    }
}
