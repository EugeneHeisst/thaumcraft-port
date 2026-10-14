package dev.eugheist.tc4modern;

import java.util.*;
import thaumcraft.common.blocks.FiniteFlux;
import thaumcraft.common.blocks.CrucibleFlux;

/** Conservation and original spill-placement checks, independent of rendering. */
public final class FluxStateTest {
    record Pos(int x,int y,int z) {}
    static final class Grid implements FiniteFlux.World,CrucibleFlux.World {
        Map<Pos,Integer> cells=new HashMap<>(); Set<Pos> walls=new HashSet<>();
        boolean lastGas; int placements;
        public int quantity(int x,int y,int z){var p=new Pos(x,y,z);return walls.contains(p)?-1:cells.getOrDefault(p,0);}
        public boolean displace(int x,int y,int z){return quantity(x,y,z)==0;}
        public boolean outside(int y){return y<0||y>=20;}
        public void set(int x,int y,int z,int n){if(n<0||n>9)throw new AssertionError("invalid volume "+n);var p=new Pos(x,y,z);if(n==0)cells.remove(p);else cells.put(p,n);}
        public void schedule(int x,int y,int z){}
        public boolean swapVertical(int x,int y,int z,int d,int n){return false;}
        public boolean air(int x,int y,int z){return quantity(x,y,z)==0;}
        public int fluxMetadata(int x,int y,int z){int n=quantity(x,y,z);return n>0?n-1:-1;}
        public void increment(int x,int y,int z,int md){set(x,y,z,md+1);}
        public void place(int x,int y,int z,boolean gas){set(x,y,z,1);placements++;lastGas=gas;}
        int total(){return cells.values().stream().mapToInt(Integer::intValue).sum();}
    }
    static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        for(int dir:new int[]{-1,1}){
            Grid g=new Grid();g.set(0,10,0,3);FiniteFlux.tick(g,0,10,0,dir,new Random(1));
            require(g.quantity(0,10+dir,0)==3&&g.quantity(0,10,0)==0,"vertical volume transfer");
            g=new Grid();g.set(0,10,0,8);g.set(0,10+dir,0,7);
            FiniteFlux.tick(g,0,10,0,dir,new Random(1));
            require(g.total()==15&&g.quantity(0,10+dir,0)==8,"vertical overflow conservation");
            for(int seed=0;seed<100;seed++){
                g=new Grid();g.set(0,10,0,8);g.walls.add(new Pos(0,10+dir,0));
                FiniteFlux.tick(g,0,10,0,dir,new Random(seed));
                require(g.total()==8&&g.cells.size()==5,"horizontal conservation seed "+seed);
            }
        }
        // Liquid death uses four finite quanta: a bucket must never create sources.
        for(int seed=0;seed<100;seed++){
            Grid death=new Grid();death.set(0,10,0,4);death.set(0,9,0,3);
            FiniteFlux.tick(death,0,10,0,-1,4,new Random(seed));
            require(death.total()==7&&death.quantity(0,9,0)==4,"death vertical merge cap");
            death=new Grid();death.set(0,10,0,4);
            for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)death.walls.add(new Pos(x,9,z));
            Random random=new Random(seed);
            for(int step=0;step<40;step++)for(Pos pos:new ArrayList<>(death.cells.keySet()))
                FiniteFlux.tick(death,pos.x(),pos.y(),pos.z(),-1,4,random);
            require(death.total()==4&&death.cells.values().stream().allMatch(n->n<=4),"death finite volume over repeated spreading");
        }
        Grid overfull=new Grid();overfull.set(0,10,0,9);
        FiniteFlux.tick(overfull,0,10,0,-1,new Random(2));
        require(overfull.total()==9&&overfull.quantity(0,9,0)==8&&overfull.quantity(0,10,0)==1,"original taint conversion metadata 8 retains ninth quantum");
        final int[] converted={0,0};
        var decay=new thaumcraft.common.blocks.TaintBlockDecay.World(){
            public void goo(int x,int y,int z){converted[0]++;}
            public void dirt(int x,int y,int z){converted[1]++;}
        };
        var zero=new Random(){@Override public int nextInt(int bound){return 0;}};
        thaumcraft.common.blocks.TaintBlockDecay.outsideTargetBiome(decay,0,0,0,0,zero);
        thaumcraft.common.blocks.TaintBlockDecay.outsideTargetBiome(decay,0,0,0,1,zero);
        thaumcraft.common.blocks.TaintBlockDecay.outsideTargetBiome(decay,0,0,0,2,zero);
        require(converted[0]==1&&converted[1]==1,"crust becomes goo, soil becomes dirt, flesh stays unchanged");
        class Infested implements thaumcraft.common.blocks.TaintBlockDecay.InfestedWorld {
            boolean nearby;int spawned,goo;
            public boolean airAbove(){return true;}public boolean nearbySwarmer(){return nearby;}public boolean encased(){return true;}
            public void swarmer(){spawned++;}public void goo(int x,int y,int z){goo++;}public void dirt(int x,int y,int z){}
        }
        var infested=new Infested();infested.nearby=true;
        thaumcraft.common.blocks.TaintBlockDecay.insideTargetBiome(infested,0,0,0,0,true,zero);
        require(infested.spawned==0&&infested.goo==0,"existing swarmer suppresses both spawn and fallback on a successful spawn roll");
        infested.nearby=false;
        thaumcraft.common.blocks.TaintBlockDecay.insideTargetBiome(infested,0,0,0,0,true,zero);
        require(infested.spawned==1&&infested.goo==0,"successful original swarmer spawn branch");
        thaumcraft.common.blocks.TaintBlockDecay.insideTargetBiome(infested,0,0,0,0,false,zero);
        require(infested.goo==1,"disabled spores still convert an encased crust block");
        Grid g=new Grid();
        var always=new Random(){@Override public int nextInt(int bound){return 0;} @Override public boolean nextBoolean(){return true;}};
        CrucibleFlux.spill(g,0,10,0,always);
        require(g.quantity(0,11,0)==1&&g.lastGas,"new gas above crucible");
        for(int i=0;i<7;i++)CrucibleFlux.spill(g,0,10,0,always);
        require(g.quantity(0,11,0)==8&&g.placements==1,"original metadata increment cap");
        CrucibleFlux.spill(g,0,10,0,always);
        require(g.quantity(-1,9,-1)==1,"full block spill chooses original offset");
        int before=g.total();
        CrucibleFlux.spill(g,0,10,0,new Random(){@Override public int nextInt(int bound){return 1;}});
        require(g.total()==before,"three in four spills do nothing");
        System.out.println("Flux volume and crucible spill checks passed");
    }
}
