package dev.eugheist.tc4modern;

import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.research.*;

/** Opt-in, isolated native rendering audit; never opens or changes a saved world. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class BookClientAudit {
    private static boolean started;
    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.bookAudit")||started)return;var mc=Minecraft.getInstance();
        if(!(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen)||mc.getOverlay()!=null)return;
        started=true;mc.setScreen(new AuditScreen());
    }
    private static final class AuditScreen extends Screen {
        private ThaumonomiconScreen book;
        private final List<ResearchItem> research=new ArrayList<>();
        private final List<String> report=new ArrayList<>();
        private int entry=-3,page,frames;
        private Path directory;
        AuditScreen(){super(Component.literal("TC4 book audit"));}
        @Override protected void init(){
            thaumcraft.common.config.ConfigResearchData.init();research.addAll(ResearchCategories.getResearchList(System.getenv().getOrDefault("TC4_BOOK_CATEGORY","BASICS")).research.values());
            var data=new net.minecraft.nbt.CompoundTag();ResearchKnowledge.grantAll(data);var list=new net.minecraft.nbt.ListTag();for(var item:research)list.add(net.minecraft.nbt.StringTag.valueOf(item.key));data.getCompound(ResearchKnowledge.KEY).put("Research",list);ClientKnowledge.knowledge=data.getCompound(ResearchKnowledge.KEY);
            ClientKnowledge.knowledge.put("Research",new net.minecraft.nbt.ListTag());ResearchPopup.highlighted.add("RESEARCH");ResearchPopup.highlighted.add("BASICS");
            var inventory=new Inventory(null);book=new ThaumonomiconScreen(new ThaumonomiconMenu(0,inventory),inventory,Component.empty());book.init(minecraft,width,height);
            directory=minecraft.gameDirectory.toPath().resolve("book-audit");try{Files.createDirectories(directory);}catch(Exception e){throw new IllegalStateException(e);}
        }
        @Override public void render(GuiGraphics gui,int mx,int my,float partial){
            gui.fill(0,0,width,height,0xff252525);book.renderBg(gui,partial,-100,-100);
            if(++frames<8)return;frames=0;gui.flush();
            String name=entry<0?"00-browser-effects-"+(entry+3):String.format(java.util.Locale.ROOT,"%02d-%s-%02d",entry+1,research.get(entry).key,page);
            try(var image=net.minecraft.client.Screenshot.takeScreenshot(minecraft.getMainRenderTarget())){image.writeToFile(directory.resolve(name+".png"));}catch(Exception e){throw new IllegalStateException(e);}
            report.add(name);
            if(entry<0){entry++;page=0;
                var completed=new net.minecraft.nbt.ListTag();
                if(entry==-2){completed.add(net.minecraft.nbt.StringTag.valueOf("NODETAPPER1"));completed.add(net.minecraft.nbt.StringTag.valueOf("RESEARCHER1"));}
                if(entry>=-1)for(var item:research)completed.add(net.minecraft.nbt.StringTag.valueOf(item.key));
                ClientKnowledge.knowledge.put("Research",completed);
                if(entry<0)return;
                ResearchPopup.highlighted.clear();
            }else{page+=2;if(page>=book.auditPageCount()){entry++;page=0;}}
            if(entry>=research.size()){
                // Every render above used the real native GUI and imported resources.
                try{Files.writeString(directory.resolve("result.txt"),"PASS: "+research.size()+" original BASICS entries; "+report.size()+" native rendered spreads\n"+String.join("\n",report));}catch(Exception e){throw new IllegalStateException(e);}minecraft.stop();return;
            }
            book.auditOpen(research.get(entry),page);
            if(page==0&&book.auditPageCount()>2){int x=(width-256)/2,y=(height-181)/2;book.mouseClicked(x+266,y+194,0);book.mouseClicked(x-12,y+194,0);}
        }
        @Override public boolean isPauseScreen(){return false;}
    }
}
