package dev.eugheist.tc4modern;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class ChampionClient {
 public static int particles;
 @SubscribeEvent static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){Champions.visual=ChampionClient::show;}
 static void show(Mob boss){switch(Champions.type(boss)){
case 0 -> {
        if (boss.getRandom().nextBoolean()) {
            return;
        }
        float w = boss.getRandom().nextFloat() * boss.getBbWidth();
        float d = boss.getRandom().nextFloat() * boss.getBbWidth();
        float h = boss.getRandom().nextFloat() * boss.getBbHeight() / 3.0f;
        spark(boss.getBoundingBox().minX + (double)w, boss.getBoundingBox().minY + (double)h, boss.getBoundingBox().minZ + (double)d, 0.3f - boss.getRandom().nextFloat() * 0.1f, 0.0f, 0.8f + boss.getRandom().nextFloat() * 0.2f);
}
case 1 -> {
        if (boss.getRandom().nextBoolean()) {
            return;
        }
        float w = boss.getRandom().nextFloat() * boss.getBbWidth();
        float d = boss.getRandom().nextFloat() * boss.getBbWidth();
        float h = boss.getRandom().nextFloat() * boss.getBbHeight();
        int p = 176 + boss.getRandom().nextInt(4) * 3;
        generic( boss.getBoundingBox().minX + (double)w, boss.getBoundingBox().minY + (double)h, boss.getBoundingBox().minZ + (double)d, 0.0, 0.0, 0.0, 0.5f + boss.getRandom().nextFloat() * 0.2f, 0.1f + boss.getRandom().nextFloat() * 0.2f, 0.1f + boss.getRandom().nextFloat() * 0.2f, 0.7f, false, p, 3, 1, 3, 0, 1.2f + boss.getRandom().nextFloat() * 0.3f);
}
case 2 -> {
        if (boss.getRandom().nextInt(4) != 0) {
            return;
        }
        float w = boss.getRandom().nextFloat() * boss.getBbWidth();
        float d = boss.getRandom().nextFloat() * boss.getBbWidth();
        float h = boss.getRandom().nextFloat() * boss.getBbHeight();
        generic( boss.getBoundingBox().minX + (double)w, boss.getBoundingBox().minY + (double)h, boss.getBoundingBox().minZ + (double)d, 0.0, 0.0, 0.0, 0.9f, 0.9f, 0.9f + boss.getRandom().nextFloat() * 0.1f, 0.7f, false, 112, 9, 1, 5 + boss.getRandom().nextInt(4), 0, 0.6f + boss.getRandom().nextFloat() * 0.2f);
}
case 3 -> {
        if (boss.getRandom().nextFloat() > 0.3f) {
            return;
        }
        float w = boss.getRandom().nextFloat() * boss.getBbWidth();
        float d = boss.getRandom().nextFloat() * boss.getBbWidth();
        float h = boss.getRandom().nextFloat() * boss.getBbHeight();
        int p = 176 + boss.getRandom().nextInt(4) * 3;
        generic( boss.getBoundingBox().minX + (double)w, boss.getBoundingBox().minY + (double)h, boss.getBoundingBox().minZ + (double)d, 0.0, 0.0, 0.0, 0.8f + boss.getRandom().nextFloat() * 0.2f, 0.8f + boss.getRandom().nextFloat() * 0.2f, 0.8f + boss.getRandom().nextFloat() * 0.2f, 0.7f, false, p, 3, 1, 4 + boss.getRandom().nextInt(3), 0, 1.0f + boss.getRandom().nextFloat() * 0.3f);
}
case 4 -> {
        if (boss.getRandom().nextBoolean()) {
            return;
        }
        float w = boss.getRandom().nextFloat() * boss.getBbWidth();
        float d = boss.getRandom().nextFloat() * boss.getBbWidth();
        float h = boss.getRandom().nextFloat() * boss.getBbHeight();
        generic( boss.getBoundingBox().minX + (double)w, boss.getBoundingBox().minY + (double)h, boss.getBoundingBox().minZ + (double)d, 0.0, -0.02, 0.0, boss.getRandom().nextFloat() * 0.2f, boss.getRandom().nextFloat() * 0.2f, boss.getRandom().nextFloat() * 0.2f, 0.8f, false, 160, 10, 1, 8 + boss.getRandom().nextInt(4), 0, 0.6f + boss.getRandom().nextFloat() * 0.4f);
}
case 5 -> {
        if (boss.getRandom().nextBoolean()) {
            return;
        }
        float w = boss.getRandom().nextFloat() * boss.getBbWidth();
        float d = boss.getRandom().nextFloat() * boss.getBbWidth();
        float h = boss.getRandom().nextFloat() * boss.getBbHeight();
        generic( boss.getBoundingBox().minX + (double)w, boss.getBoundingBox().minY + (double)h, boss.getBoundingBox().minZ + (double)d, 0.0, 0.0, 0.0, 0.5f + boss.getRandom().nextFloat() * 0.1f, 0.5f + boss.getRandom().nextFloat() * 0.1f, 0.5f + boss.getRandom().nextFloat() * 0.1f, 0.6f, true, 21, 4, 1, 4 + boss.getRandom().nextInt(4), 0, 0.8f + boss.getRandom().nextFloat() * 0.3f);
}
case 6 -> {
        if (boss.getRandom().nextBoolean()) {
            return;
        }
        float w = boss.getRandom().nextFloat() * boss.getBbWidth();
        float d = boss.getRandom().nextFloat() * boss.getBbWidth();
        float h = boss.getRandom().nextFloat() * boss.getBbHeight();
        generic( boss.getBoundingBox().minX + (double)w, boss.getBoundingBox().minY + (double)h, boss.getBoundingBox().minZ + (double)d, 0.0, 0.0, 0.0, 0.8f + boss.getRandom().nextFloat() * 0.2f, 0.0f, 0.9f + boss.getRandom().nextFloat() * 0.1f, 0.7f, true, 72, 8, 1, 10 + boss.getRandom().nextInt(4), 0, 0.6f + boss.getRandom().nextFloat() * 0.4f);
}
case 7 -> {
        if (boss.getRandom().nextBoolean()) {
            return;
        }
        float w = boss.getRandom().nextFloat() * boss.getBbWidth();
        float d = boss.getRandom().nextFloat() * boss.getBbWidth();
        float h = boss.getRandom().nextFloat() * boss.getBbHeight();
        generic( boss.getBoundingBox().minX + (double)w, boss.getBoundingBox().minY + (double)h, boss.getBoundingBox().minZ + (double)d, 0.0, 0.03, 0.0, 0.1f + boss.getRandom().nextFloat() * 0.1f, 0.8f + boss.getRandom().nextFloat() * 0.2f, 0.1f + boss.getRandom().nextFloat() * 0.1f, 0.9f, true, 21, 4, 1, 4 + boss.getRandom().nextInt(4), 0, 0.5f + boss.getRandom().nextFloat() * 0.2f);
}
case 8 -> {
        float w = boss.getRandom().nextFloat() * boss.getBbWidth();
        float d = boss.getRandom().nextFloat() * boss.getBbWidth();
        float h = boss.getRandom().nextFloat() * boss.getBbHeight();
        generic( boss.getBoundingBox().minX + (double)w, boss.getBoundingBox().minY + (double)h, boss.getBoundingBox().minZ + (double)d, 0.0, 0.03, 0.0, 0.9f + boss.getRandom().nextFloat() * 0.1f, 1.0f, 1.0f, 0.7f, false, 160, 10, 1, 8 + boss.getRandom().nextInt(4), 0, 0.7f + boss.getRandom().nextFloat() * 0.2f);
}
case 9 -> {
        if (boss.getRandom().nextBoolean()) {
            return;
        }
        float w = boss.getRandom().nextFloat() * boss.getBbWidth();
        float d = boss.getRandom().nextFloat() * boss.getBbWidth();
        float h = boss.getRandom().nextFloat() * boss.getBbHeight();
        generic( boss.getBoundingBox().minX + (double)w, boss.getBoundingBox().minY + (double)h, boss.getBoundingBox().minZ + (double)d, 0.0, -0.02, 0.0, 0.2f, 0.6f + boss.getRandom().nextFloat() * 0.1f, 0.2f + boss.getRandom().nextFloat() * 0.1f, 0.5f, false, 1, 4, 2, 5 + boss.getRandom().nextInt(4), 0, 0.9f + boss.getRandom().nextFloat() * 0.3f);
}
case 10 -> {
        if (boss.getRandom().nextBoolean()) {
            return;
        }
        float w = boss.getRandom().nextFloat() * boss.getBbWidth();
        float d = boss.getRandom().nextFloat() * boss.getBbWidth();
        float h = boss.getRandom().nextFloat() * boss.getBbHeight();
        generic( boss.getBoundingBox().minX + (double)w, boss.getBoundingBox().minY + (double)h, boss.getBoundingBox().minZ + (double)d, 0.0, 0.02, 0.0, 0.2f, 0.6f + boss.getRandom().nextFloat() * 0.1f, 0.2f + boss.getRandom().nextFloat() * 0.1f, 0.7f, false, 147, 4, 1, 8 + boss.getRandom().nextInt(4), 0, 0.5f + boss.getRandom().nextFloat() * 0.2f);
}
case 11 -> {
        if (boss.getRandom().nextFloat() > 0.2f) {
            return;
        }
        float w = boss.getRandom().nextFloat() * boss.getBbWidth();
        float d = boss.getRandom().nextFloat() * boss.getBbWidth();
        float h = boss.getRandom().nextFloat() * boss.getBbHeight();
        generic( boss.getBoundingBox().minX + (double)w, boss.getBoundingBox().minY + (double)h, boss.getBoundingBox().minZ + (double)d, 0.0, 0.0, 0.0, 0.9f + boss.getRandom().nextFloat() * 0.1f, 0.0f, 0.0f, 0.9f, false, 147, 4, 1, 8 + boss.getRandom().nextInt(4), 0, 0.5f + boss.getRandom().nextFloat() * 0.2f);
}
case 12 -> {
        if (boss.getRandom().nextBoolean()) {
            particles++;SporeSplooshParticle.infested(boss);
        }
}
}}
 static void generic(double x,double y,double z,double mx,double my,double mz,float r,float g,float b,float alpha,boolean loop,int start,int num,int inc,int age,int delay,float scale){particles++;FociFx.generic(x,y,z,mx,my,mz,r,g,b,alpha,loop,start,num,inc,age,delay,scale);}
 static void spark(double x,double y,double z,float r,float g,float b){particles++;net.minecraft.client.Minecraft.getInstance().particleEngine.add(new ChampionSpark(x,y,z,r,g,b));}
 @SubscribeEvent static void colors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e){for(var a:thaumcraft.api.aspects.Aspect.getPrimalAspects()){var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("primal_ring_"+a.getTag()));e.register((s,t)->0xff000000|a.getColor(),item);}}
}
