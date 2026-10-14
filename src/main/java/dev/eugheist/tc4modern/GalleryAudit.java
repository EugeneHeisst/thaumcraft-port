package dev.eugheist.tc4modern;

import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Hidden client sweep: every tc4modern block state and item must resolve to a real model; item icons are photographed page by page for inspection. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class GalleryAudit {
    private static boolean started;
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){var mc=Minecraft.getInstance();if(Boolean.getBoolean("tc4modern.galleryAudit")&&!started&&mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen&&mc.getOverlay()==null){started=true;mc.setScreen(new Gallery());}}
    private static final class Gallery extends Screen {
        final List<ItemStack> items=new ArrayList<>();int page,frame;Path path;
        Gallery(){super(Component.literal("TC4 gallery"));}
        @Override protected void init(){
            path=minecraft.gameDirectory.toPath().resolve("gallery");try{Files.createDirectories(path);}catch(Exception e){throw new IllegalStateException(e);}
            var missing=minecraft.getModelManager().getMissingModel();var bad=new ArrayList<String>();int states=0;
            for(var block:BuiltInRegistries.BLOCK){var id=BuiltInRegistries.BLOCK.getKey(block);if(!id.getNamespace().equals(TC4Modern.MODID))continue;
                for(var state:block.getStateDefinition().getPossibleStates()){if(state.getRenderShape()!=RenderShape.MODEL)continue;states++;
                    if(minecraft.getBlockRenderer().getBlockModel(state)==missing)bad.add("block "+state);}}
            for(var item:BuiltInRegistries.ITEM){var id=BuiltInRegistries.ITEM.getKey(item);if(!id.getNamespace().equals(TC4Modern.MODID))continue;var stack=new ItemStack(item);items.add(stack);
                if(minecraft.getItemRenderer().getModel(stack,null,null,0)==missing)bad.add("item "+id);}
            var report=new StringBuilder("states "+states+" items "+items.size()+" missing "+bad.size()+"\n");bad.forEach(b->report.append(b).append('\n'));
            try{Files.writeString(path.resolve("missing.txt"),report);}catch(Exception e){throw new IllegalStateException(e);}
            System.out.println("TC4_GALLERY "+report.toString().replace('\n',';'));
        }
        static final int CELL=20;
        @Override public void render(GuiGraphics gui,int mx,int my,float partial){
            int COLS=(width-16)/CELL,ROWS=(height-24)/CELL;gui.fill(0,0,width,height,0xff6b6b6b);int per=COLS*ROWS,from=page*per;
            for(int i=from;i<Math.min(items.size(),from+per);i++){int x=8+(i-from)%COLS*CELL,y=8+(i-from)/COLS*CELL;gui.fill(x-1,y-1,x+17,y+17,0xff8b8b8b);gui.renderItem(items.get(i),x,y);}
            gui.drawString(font,"page "+(page+1)+" items "+from+".."+Math.min(items.size(),from+per),8,height-12,0xffffff);
            if(++frame<20)return;frame=0;gui.flush();
            try(var image=net.minecraft.client.Screenshot.takeScreenshot(minecraft.getMainRenderTarget())){image.writeToFile(path.resolve("items-"+page+".png"));}catch(Exception e){throw new IllegalStateException(e);}
            try{Files.writeString(path.resolve("page-"+page+".txt"),String.join("\n",items.subList(from,Math.min(items.size(),from+per)).stream().map(s->BuiltInRegistries.ITEM.getKey(s.getItem()).getPath()).toList()));}catch(Exception e){throw new IllegalStateException(e);}
            if(++page*per>=items.size()){System.out.println("TC4_GALLERY DONE pages "+page);minecraft.stop();}
        }
    }
}
