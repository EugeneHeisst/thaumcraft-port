package dev.eugheist.tc4modern;

import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;

/** All registered icons and content-sensitive variants; paginated to keep every slot in view. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class InventoryClientAudit {
    private static boolean started;
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){var mc=Minecraft.getInstance();if(Boolean.getBoolean("tc4modern.inventoryAudit")&&!started&&mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen&&mc.getOverlay()==null){started=true;mc.setScreen(new Audit());}}
    private record Entry(String key,ItemStack stack){}
    private static final class Audit extends Screen {
        private final List<Entry> items=new ArrayList<>(),details=new ArrayList<>();
        private final List<Map<String,String>> report=new ArrayList<>();
        private boolean finished;private long pageStarted=System.nanoTime();private int frames,page,columns,rows;private Path directory;
        Audit(){super(Component.literal("Inventory comparison"));}
        private void add(String key,ItemStack stack){items.add(new Entry(key,stack));}
        @Override protected void init(){
            for(var item:BuiltInRegistries.ITEM){var id=BuiltInRegistries.ITEM.getKey(item);if(!id.getNamespace().equals(TC4Modern.MODID))continue;var stack=new ItemStack(item);if(stack.is(EssentiaDevices.ESSENTIA_PHIAL.get()))stack=EssentiaItems.phialOf(Aspect.AIR,8);add(id.toString(),stack);}
            items.sort(Comparator.comparing(Entry::key));
            for(var cap:WandParts.CAPS.values())for(var rod:WandParts.RODDEFS.values())add("wand#"+cap.tag()+"/"+rod.tag(),WandParts.make(cap,rod));
            for(var aspect:Aspect.aspects.values())add("phial#"+aspect.getTag(),EssentiaItems.phialOf(aspect,8));
            for(var focus:List.of(FociContent.FIRE.get(),FociContent.FROST.get(),FociContent.SHOCK.get())){var wand=WandParts.make(WandParts.CAPS.get("gold"),WandParts.RODDEFS.get("greatwood"));FociContent.setFocus(wand,new ItemStack(focus));add("wand#focus/"+BuiltInRegistries.ITEM.getKey(focus).getPath(),wand);}
            for(var aspect:List.of(Aspect.AIR,Aspect.FIRE,Aspect.WATER)){var jar=new ItemStack(EssentiaDevices.JAR.get());EssentiaItems.setAspects(jar,new thaumcraft.api.aspects.AspectList().add(aspect,64));net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,jar,t->t.putString("AspectFilter",aspect.getTag()));add("jar#"+aspect.getTag(),jar);var label=new ItemStack(EssentiaDevices.LABEL.get());EssentiaItems.setAspects(label,new thaumcraft.api.aspects.AspectList().add(aspect,1));add("label#"+aspect.getTag(),label);}
            for(var entry:items)if(entry.key.equals("tc4modern:aura_node")||entry.key.equals("tc4modern:node_converter")||entry.key.equals("tc4modern:node_stabilizer")||entry.key.endsWith("_infused_stone")||entry.key.equals("tc4modern:deconstructor")||entry.key.equals("tc4modern:node_jar")||entry.key.endsWith("_crystal_cluster")||entry.key.equals("tc4modern:thaumometer")||entry.key.equals("tc4modern:essentia_tube")||entry.key.equals("tc4modern:alembic")||entry.key.equals("tc4modern:brain_jar")||entry.key.equals("tc4modern:vis_relay")||entry.key.equals("tc4modern:bellows")||entry.key.equals("tc4modern:research_table")||entry.key.equals("wand#iron/wood")||entry.key.equals("wand#gold/greatwood")||entry.key.equals("wand#thaumium/silverwood")||entry.key.startsWith("wand#focus/")||entry.key.startsWith("jar#"))details.add(entry);
            columns=Math.max(1,(width-20)/60);rows=Math.max(1,(height-30)/44);
            directory=minecraft.gameDirectory.toPath().resolve("inventory-audit");try{Files.createDirectories(directory);}catch(Exception e){throw new IllegalStateException(e);}
            for(var entry:items){var stack=entry.stack;String name=stack.getHoverName().getString();if(name.contains("tc4modern.")||name.contains("прототип")||name.contains("prototype"))throw new AssertionError("Unresolved item name "+entry.key+" "+name);
                var model=minecraft.getItemRenderer().getModel(stack,null,null,0);if(model==minecraft.getModelManager().getMissingModel()||model.getParticleIcon().contents().name().toString().equals("minecraft:missingno"))throw new AssertionError("Missing inventory model "+entry.key);
                report.add(Map.of("id",entry.key,"description",stack.getDescriptionId(),"name",name));}
        }
        @Override public void render(GuiGraphics gui,int mx,int my,float partial){
            gui.fill(0,0,width,height,0xff252525);int pages=(items.size()+columns*rows-1)/(columns*rows);boolean detail=page==pages;
            gui.drawString(font,detail?"Inventory models (3x)":"Inventory "+(page+1)+"/"+pages,10,4,0xffffff,false);
            var entries=detail?details:items;int count=detail?details.size():Math.min(columns*rows,items.size()-page*columns*rows);
            int cellW=detail?100:60,cellH=detail?100:44,cols=detail?Math.max(1,(width-20)/100):columns;
            for(int i=0;i<count;i++){var entry=entries.get(detail?i:page*columns*rows+i);int x=10+i%cols*cellW,y=20+i/cols*cellH;gui.fill(x,y,x+cellW-4,y+cellH-4,0xff383838);
                gui.pose().pushPose();gui.pose().translate(x+(cellW-(detail?48:16))/2f,y+4,0);if(detail)gui.pose().scale(3,3,3);gui.renderItem(entry.stack,0,0);gui.pose().popPose();
                gui.pose().pushPose();gui.pose().scale(.5f,.5f,1);String label=font.plainSubstrByWidth(entry.stack.getHoverName().getString(),(cellW-8)*2);gui.drawCenteredString(font,label,(x+cellW/2)*2,(y+(detail?58:24))*2,0xffffff);gui.pose().popPose();}
            if(finished)return;if(Boolean.getBoolean("tc4modern.visibleAudit")){if(System.nanoTime()-pageStarted<8_000_000_000L)return;pageStarted=System.nanoTime();}else if(++frames<30)return;frames=0;gui.flush();try(var image=net.minecraft.client.Screenshot.takeScreenshot(minecraft.getMainRenderTarget())){image.writeToFile(directory.resolve(detail?"inventory-details.png":"inventory-page-"+page+".png"));}catch(Exception e){throw new IllegalStateException(e);}
            if(!detail){page++;return;}
            try{Files.writeString(directory.resolve("items.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));Files.writeString(directory.resolve("items.tsv"),String.join("\n",report.stream().map(r->r.get("id")+"\t"+r.get("name")).toList()));Files.writeString(directory.resolve("result.txt"),"PASS: "+items.size()+" registered/variant icons; "+pages+" pages and enlarged models; no missing models or names\n");}catch(Exception e){throw new IllegalStateException(e);}if(Boolean.getBoolean("tc4modern.visibleAudit"))finished=true;else minecraft.stop();
        }
    }
}
