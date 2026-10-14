package dev.eugheist.tc4modern;
import thaumcraft.common.entities.monster.ThaumicSlimeState;
import thaumcraft.common.blocks.FluxGooDecay;
public final class ThaumicSlimeStateTest {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    static final class Goo implements FluxGooDecay.World {
        int md,slime,gas;boolean air=true,removed,taint;
        Goo(int md){this.md=md;}
        public int metadata(){return md;} public boolean airAbove(){return air;}
        public void remove(){removed=true;}public void slime(int size){slime=size;}
        public void taint(){taint=true;}public void decrement(){md--;}public void gasAbove(){gas++;}
    }
    static void checkTaintSpread(){
        var calls=new java.util.ArrayList<String>();
        var values=new java.util.HashMap<String,Object>();
        var world=(thaumcraft.common.blocks.TaintFibresSpread.World)java.lang.reflect.Proxy.newProxyInstance(
            ThaumicSlimeStateTest.class.getClassLoader(),new Class[]{thaumcraft.common.blocks.TaintFibresSpread.World.class},(proxy,method,args)->{
                if(method.getReturnType()==void.class){calls.add(method.getName()+java.util.Arrays.toString(args));return null;}
                return values.getOrDefault(method.getName(),method.getReturnType()==boolean.class?false:0);
            });
        var zero=new java.util.Random(){public int nextInt(int bound){return 0;}};
        values.put("adjacentSolid",true);values.put("fibreReplaceable",true);values.put("air",true);values.put("solidTop",true);
        check(thaumcraft.common.blocks.TaintFibresSpread.spread(world,10,20,30,zero));
        check(calls.equals(java.util.List.of("fibres[10, 20, 30, 1]","schedule[10, 20, 30]")));
        calls.clear();values.put("liquid",true);
        check(!thaumcraft.common.blocks.TaintFibresSpread.spread(world,10,20,30,zero)&&calls.isEmpty());
        values.put("adjacentTaint",2);
        thaumcraft.common.blocks.TaintFibresSpread.biomeSpread(world,10,20,30,1,zero);
        check(calls.equals(java.util.List.of("taintBiome[9, 29]","schedule[10, 20, 30]")));
        calls.clear();
        thaumcraft.common.blocks.TaintFibresSpread.biomeSpread(world,10,20,30,0,zero);check(calls.isEmpty());
        thaumcraft.common.blocks.TaintFibresSpread.tick(world,10,20,30,0,0,true,zero);
        check(calls.equals(java.util.List.of("remove[10, 20, 30]")));
    }
    public static void main(String[] args){
        check(ThaumicSlimeState.serverWidth(1)==.5f&&ThaumicSlimeState.serverWidth(64)==2.25f);
        check(Math.abs(ThaumicSlimeState.clientWidth(64)-4.8f)<.001);
        check(ThaumicSlimeState.attack(64)==64&&ThaumicSlimeState.experience(64)==8);
        check(ThaumicSlimeState.deathChildren(3)==0&&ThaumicSlimeState.deathChildren(9)==3);
        check(ThaumicSlimeState.merge(90,20)==100);
        var zero=new java.util.Random(){public int nextInt(int bound){return 0;}public boolean nextBoolean(){return true;}};
        var g=new Goo(3);FluxGooDecay.tick(g,true,zero);check(g.removed&&g.slime==1&&!g.taint);
        g=new Goo(7);FluxGooDecay.tick(g,true,zero);check(g.removed&&g.slime==2&&!g.taint);
        g=new Goo(7);FluxGooDecay.tick(g,true,new java.util.Random(){public int nextInt(int bound){return bound==25?1:0;}});check(g.taint&&!g.removed);
        g=new Goo(1);FluxGooDecay.tick(g,true,zero);check(g.md==0&&g.gas==1);
        g=new Goo(0);FluxGooDecay.tick(g,true,zero);check(g.removed&&g.slime==0);
        g=new Goo(7);g.air=false;FluxGooDecay.tick(g,true,zero);check(g.md==6&&!g.taint&&g.slime==0);
        checkTaintSpread();
        check(thaumcraft.client.fx.particles.SwarmMotion.rotate(0,90,5)==5);
        check(thaumcraft.client.fx.particles.SwarmMotion.rotate(0,-90,5)==-5);
        // Original sequential clamps with a negative limit always choose the positive turn.
        check(thaumcraft.client.fx.particles.SwarmMotion.rotate(0,-90,-5)==5);
        var motion=new thaumcraft.client.fx.particles.SwarmMotion();
        for(int i=0;i<50;i++)motion.targetMissing(0);
        check(motion.alive&&motion.alpha()==0);
        motion.targetMissing(0);check(!motion.alive);
        check(thaumcraft.client.fx.particles.SwarmMotion.sprite(0)==7&&thaumcraft.client.fx.particles.SwarmMotion.sprite(8)==7);
        System.out.println("Original Thaumic slime size, flux goo decay and taint spread checks passed");
    }
}
