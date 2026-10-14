package dev.eugheist.tc4modern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Original KeyHandler's complete registry, translated controls UI and remapping. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class OriginalKeysAudit {
    static int ticks;
    static void check(boolean ok,String message){if(!ok)throw new IllegalStateException("TC4_KEYS FAIL "+message);System.out.println("TC4_KEYS PASS "+message);}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
        if(!Boolean.getBoolean("tc4modern.keysAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        if(++ticks==40){
            var keys=java.util.List.of(FociClient.KEY_F,WardingClient.KEY_G,HoverHarnessClient.KEY);
            var defaults=java.util.List.of(70,71,72);
            for(int i=0;i<keys.size();i++){
                var key=keys.get(i);
                check(java.util.Arrays.stream(mc.options.keyMappings).filter(k->k==key).count()==1,"registered exactly once: "+key.getName());
                check(key.getDefaultKey().getValue()==defaults.get(i),"original default "+key.getDefaultKey().getName());
                check(key.getCategory().equals("key.categories.misc"),"original Misc category");
                check(!net.minecraft.client.resources.language.I18n.get(key.getName()).equals(key.getName()),"localized label: "+net.minecraft.client.resources.language.I18n.get(key.getName()));
                var saved=key.getKey();var remapped=InputConstants.Type.KEYSYM.getOrCreate(299+i);
                try{
                    key.setKey(remapped);KeyMapping.resetMapping();
                    KeyMapping.click(saved);check(!key.consumeClick(),"old key no longer triggers after rebind");
                    KeyMapping.click(remapped);check(key.consumeClick(),"new key triggers after rebind");
                }finally{key.setKey(saved);KeyMapping.resetMapping();}
            }
            mc.options.guiScale().set(2);mc.resizeDisplay();
            mc.setScreen(new net.minecraft.client.gui.screens.options.controls.KeyBindsScreen(new net.minecraft.client.gui.screens.options.controls.ControlsScreen(null,mc.options),mc.options));
            System.out.println("TC4_KEYS ALL PASS");
        }
        if(ticks==45)for(var child:mc.screen.children())if(child instanceof net.minecraft.client.gui.components.AbstractSelectionList<?> list)list.setScrollAmount(10000);
        if(ticks==55)LiquidDeathAudit.shot(mc,"original-keybindings.png");
        if(ticks==65)mc.stop();
    }
}
