package dev.eugheist.tc4modern;

import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Captures the actual registered items, block item forms and populated creative tab after resource loading. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class CreativeTabAudit {
    static boolean done;
    static void check(boolean ok,String text){if(!ok)throw new IllegalStateException("TC4_CREATIVE FAIL "+text);System.out.println("TC4_CREATIVE PASS "+text);}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("tc4modern.creativeTabAuditOnly")||done)return;
        var mc=Minecraft.getInstance();if(!(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen)||mc.getOverlay()!=null)return;
        done=true;
        try {
            var rows=new ArrayList<String>();var tab=CheatSheetAudit.tab();
            for(var item:BuiltInRegistries.ITEM){var id=BuiltInRegistries.ITEM.getKey(item);if(!id.getNamespace().equals(TC4Modern.MODID))continue;
                var s=new ItemStack(item);rows.add("ITEM\t"+id.getPath()+"\t"+s.getDescriptionId()+"\t"+s.getHoverName().getString()+"\t"+tab.stream().anyMatch(t->t.is(item)));}
            for(var block:BuiltInRegistries.BLOCK){var id=BuiltInRegistries.BLOCK.getKey(block);if(id.getNamespace().equals(TC4Modern.MODID))rows.add("BLOCK\t"+id.getPath()+"\t"+BuiltInRegistries.ITEM.getKey(block.asItem()));}
            var excluded=java.util.Set.of("thaumonomicon_cheat","iron_capped_wooden_wand","gold_capped_greatwood_wand","research_notes_complete");
            for(var item:BuiltInRegistries.ITEM){var id=BuiltInRegistries.ITEM.getKey(item);if(!id.getNamespace().equals(TC4Modern.MODID)||excluded.contains(id.getPath()))continue;check(tab.stream().anyMatch(s->s.is(item)),"registered item present: "+id);}
            double previous=-1;int colored=0;
            for(var stack:tab){
                double rank=ModCreativeTab.rank(stack.getItem());check(rank>=previous,"original order: "+BuiltInRegistries.ITEM.getKey(stack.getItem()));previous=rank;
                String name=stack.getHoverName().getString();check(!name.contains("tc4modern.")&&!name.contains("прототип")&&!name.contains("prototype"),"resolved name: "+name);
                var model=mc.getItemRenderer().getModel(stack,null,null,0);check(model!=mc.getModelManager().getMissingModel()&&model.getQuads(null,null,net.minecraft.util.RandomSource.create(1)).stream().noneMatch(q->q.getSprite().contents().name().toString().equals("minecraft:missingno")),"inventory model: "+BuiltInRegistries.ITEM.getKey(stack.getItem()));
                if(stack.is(CrimsonContent.BANNER.get().asItem())&&NoteData.tag(stack).contains("color"))colored++;
            }
            check(colored==16,"all sixteen colored banners");
            check(tab.stream().filter(s->BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().startsWith("candle_")).map(s->s.getHoverName().getString()).distinct().count()==16,"sixteen distinct candle names");
            int i=0;for(var s:tab)rows.add("TAB\t"+(i++)+"\t"+BuiltInRegistries.ITEM.getKey(s.getItem()).getPath()+"\t"+s.getHoverName().getString()+"\t"+s);
            Files.write(mc.gameDirectory.toPath().resolve("creative-tab.tsv"),rows);
            Files.copy(mc.gameDirectory.toPath().resolve("creative-tab.tsv"),mc.gameDirectory.toPath().resolve("creative-tab-"+mc.options.languageCode+".tsv"),StandardCopyOption.REPLACE_EXISTING);
            System.out.println("TC4_CREATIVE ALL PASS "+tab.size()+" entries, "+mc.options.languageCode);
        } catch(Exception ex){throw new IllegalStateException("TC4_CREATIVE FAIL",ex);}finally{mc.stop();}
    }
}
