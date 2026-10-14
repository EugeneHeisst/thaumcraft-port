package dev.eugheist.tc4modern;

import java.util.Random;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.tiles.CrucibleState;

public final class CrucibleStateTest {
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        require(thaumcraft.common.blocks.InfernalFurnaceRestoration.intact((x,y,z)->!(x==0&&z==0&&y>=0),0,0,0),"Original core validation skips core and top intake");
        require(thaumcraft.common.blocks.InfernalFurnaceRestoration.intact((x,y,z)->!(x==0&&z==1&&y>=0),0,0,0),"Source loop also skips south centre above bottom layer");
        require(!thaumcraft.common.blocks.InfernalFurnaceRestoration.intact((x,y,z)->!(x==1&&y==1&&z==1),0,0,0),"Missing corner invalidates furnace");
        require(thaumcraft.common.blocks.InfernalFurnaceRestoration.restored(0)==thaumcraft.common.tiles.InfernalFurnaceStructure.Material.AIR,"Dismantling core removes lava");
        require(thaumcraft.common.blocks.InfernalFurnaceRestoration.restored(5)==thaumcraft.common.tiles.InfernalFurnaceStructure.Material.OBSIDIAN,"Centre bottom restores obsidian despite odd metadata");
        require(thaumcraft.common.blocks.InfernalFurnaceRestoration.restored(10)==thaumcraft.common.tiles.InfernalFurnaceStructure.Material.IRON_BARS,"Outlet restores iron bars");
        record NetworkPoint(int x,int y,int z,int range,boolean loaded,java.util.List<thaumcraft.api.visnet.VisNearbyRules.Node> children) implements thaumcraft.api.visnet.VisNearbyRules.Node {
            public boolean valid(){return true;}
            public int consumeVis(Aspect aspect,int amount){return 0;}
        }
        var closeRelay=new NetworkPoint(2,0,0,8,true,java.util.List.of());
        var unloadedRelay=new NetworkPoint(1,0,0,8,false,java.util.List.of());
        var root=new NetworkPoint(5,0,0,8,true,java.util.List.of(unloadedRelay,closeRelay));
        var separateRoot=new NetworkPoint(8,0,0,8,true,java.util.List.of());
        var chosen=thaumcraft.api.visnet.VisNearbyRules.select(java.util.List.of(root,separateRoot),0,0,0);
        require(chosen.size()==2&&chosen.get(0)==closeRelay&&chosen.get(1)==separateRoot,"Visnet chooses one nearest loaded endpoint per root, including exact range boundary");
        var coincident=new NetworkPoint(0,0,0,8,true,java.util.List.of());
        var distant=new NetworkPoint(9,0,0,8,true,java.util.List.of());
        require(thaumcraft.api.visnet.VisNearbyRules.select(java.util.List.of(coincident,distant),0,0,0).isEmpty(),"Original network excludes zero distance and out of range endpoints");
        int[] visRequests={0},visEffects={0};
        java.util.List<thaumcraft.api.visnet.VisDrainRules.Node> endpoints=new java.util.ArrayList<>();
        endpoints.add(null);
        endpoints.add((aspect,amount)->{require(aspect==Aspect.FIRE&&amount==5,"Visnet first endpoint sees full request");return 2;});
        endpoints.add((aspect,amount)->{require(amount==3,"Visnet next endpoint sees remaining request");return 3;});
        endpoints.add((aspect,amount)->{visRequests[0]++;return 1;});
        require(thaumcraft.api.visnet.VisDrainRules.drain(endpoints,Aspect.FIRE,5,(node,color)->{require(color==Aspect.getPrimalAspects().indexOf(Aspect.FIRE),"Visnet effect uses original primal color index");visEffects[0]++;})==5&&visEffects[0]==2&&visRequests[0]==0,"Visnet skips vanished endpoints and stops when fulfilled");
        require(thaumcraft.api.visnet.VisDrainRules.drain(java.util.List.of(),Aspect.FIRE,5,(node,color)->{})==0,"Absent energized network provides zero vis");
        var furnaceTextures=new thaumcraft.common.blocks.InfernalFurnaceTextures();
        var textureWorld=new thaumcraft.common.blocks.InfernalFurnaceTextures.World(){
            public int metadata(int x,int y,int z){return 2;}
            public boolean furnace(int x,int y,int z){return y>=0&&y<=2;}
            public boolean outletOnSide(int x,int y,int z,int side){return side==2;}
        };
        require(furnaceTextures.calculateLevel(textureWorld,0,0,0)==18,"Original bottom texture layer has offset18");
        require(furnaceTextures.calculateLevel(textureWorld,0,1,0)==9,"Original middle texture layer has offset9");
        require(furnaceTextures.calculateLevel(textureWorld,0,2,0)==0,"Original top texture layer has offset0");
        require(furnaceTextures.calculateTexture(textureWorld,0,0,0,1)==16,"Original metadata2 bottom top-face special icon16");
        require(furnaceTextures.calculateTexture(textureWorld,0,1,0,2)==13,"Outlet adjacency adds three to original north middle icon");
        int[] queue=new int[32];boolean[] matching=new boolean[32];
        int[] incoming={4};boolean[] smeltable={true};int[] changes={0},destroyed={0};
        var queueHost=new thaumcraft.common.tiles.InfernalFurnaceInventory.Host(){
            public boolean empty(int slot){return queue[slot]==0;}
            public boolean sameItem(int slot){return matching[slot];}
            public int count(int slot){return queue[slot];}
            public int incomingCount(){return incoming[0];}
            public int incomingMaxStackSize(){return 64;}
            public void merge(int slot){queue[slot]+=incoming[0];}
            public void assign(int slot,int limit){queue[slot]=Math.min(limit,incoming[0]);matching[slot]=true;}
            public boolean canSmelt(int slot){return smeltable[0];}
            public void destroy(int slot){queue[slot]=0;destroyed[0]++;}
            public void changed(){changes[0]++;}
        };
        queue[1]=20;matching[1]=true;
        require(thaumcraft.common.tiles.InfernalFurnaceInventory.insert(queueHost)&&queue[0]==4&&queue[1]==20,"Source uses first empty slot before later matching stacks");
        require(thaumcraft.common.tiles.InfernalFurnaceInventory.insert(queueHost)&&queue[0]==8,"Matching first slot merges entire incoming stack");
        smeltable[0]=false;
        require(thaumcraft.common.tiles.InfernalFurnaceInventory.insert(queueHost)&&queue[0]==0&&destroyed[0]==1,"Unsmeltable incoming stack destroys merged contents and is accepted");
        java.util.Arrays.fill(queue,64);
        require(!thaumcraft.common.tiles.InfernalFurnaceInventory.insert(queueHost)&&changes[0]==3,"Full queue refuses incoming entity without dirtying inventory");
        var bonusRandom=new java.util.Random(){
            int step;
            @Override public int nextInt(int bound){require(bound==4,"No-bellows bonus uses one in four");return 0;}
            @Override public float nextFloat(){return new float[]{.439f,.44f,.9f}[step++];}
        };
        require(thaumcraft.common.tiles.InfernalFurnaceOutput.bonusCount(0,0,bonusRandom)==1,"No-bellows lucky bonus adds one");
        require(thaumcraft.common.tiles.InfernalFurnaceOutput.bonusCount(2,3,bonusRandom)==3,"Bellows preserve base bonus and use strict44percent threshold");
        require(thaumcraft.common.tiles.InfernalFurnaceOutput.experience(2,.35f,.69)==1,"Fractional smelting XP rounds up below remainder");
        require(thaumcraft.common.tiles.InfernalFurnaceOutput.experience(2,.35f,.71)==0,"Fractional smelting XP stays down above remainder");
        require(thaumcraft.common.tiles.InfernalFurnaceOutput.experience(2,2f,0)==2,"Source XP rate above one retains output count");
        require(thaumcraft.common.tiles.InfernalFurnaceOutput.experience(4,0,0)==0,"Zero-experience recipes produce no orbs");
        var furnaceBlocks=new thaumcraft.common.tiles.InfernalFurnaceStructure.Material[3][3][3];
        for(int y=0;y<3;y++)for(int x=0;x<3;x++)for(int z=0;z<3;z++)
            furnaceBlocks[x][y][z]=x==1&&z==1?(y==2?thaumcraft.common.tiles.InfernalFurnaceStructure.Material.AIR:y==1?thaumcraft.common.tiles.InfernalFurnaceStructure.Material.LAVA:thaumcraft.common.tiles.InfernalFurnaceStructure.Material.OBSIDIAN)
                :x%2==0&&z%2==0?thaumcraft.common.tiles.InfernalFurnaceStructure.Material.NETHER_BRICK:thaumcraft.common.tiles.InfernalFurnaceStructure.Material.OBSIDIAN;
        thaumcraft.common.tiles.InfernalFurnaceStructure.View furnaceView=(x,y,z)->furnaceBlocks[x][y][z];
        require(!thaumcraft.common.tiles.InfernalFurnaceStructure.fits(furnaceView,0,0,0),"Infernal requires one iron-bars outlet");
        for(int[] outlet:new int[][]{{0,1},{2,1},{1,0},{1,2}}){
            furnaceBlocks[outlet[0]][1][outlet[1]]=thaumcraft.common.tiles.InfernalFurnaceStructure.Material.IRON_BARS;
            require(thaumcraft.common.tiles.InfernalFurnaceStructure.fits(furnaceView,0,0,0),"All four horizontal infernal outlets fit");
            furnaceBlocks[1][2][1]=thaumcraft.common.tiles.InfernalFurnaceStructure.Material.OBSIDIAN;
            require(!thaumcraft.common.tiles.InfernalFurnaceStructure.fits(furnaceView,0,0,0),"Infernal top intake must be air");
            furnaceBlocks[1][2][1]=thaumcraft.common.tiles.InfernalFurnaceStructure.Material.AIR;
            furnaceBlocks[outlet[0]][1][outlet[1]]=thaumcraft.common.tiles.InfernalFurnaceStructure.Material.OBSIDIAN;
        }
        furnaceBlocks[0][1][1]=furnaceBlocks[2][1][1]=thaumcraft.common.tiles.InfernalFurnaceStructure.Material.IRON_BARS;
        require(!thaumcraft.common.tiles.InfernalFurnaceStructure.fits(furnaceView,0,0,0),"Two infernal outlets invalid");
        require(thaumcraft.common.tiles.InfernalFurnaceStructure.convertedMetadata(thaumcraft.common.tiles.InfernalFurnaceStructure.Material.OBSIDIAN,2,2)==9,"Original X/Z metadata order");
        var energized=new thaumcraft.common.tiles.EnergizedNodeState();
        var energyAura=new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.FIRE,100);
        int[] energyChanges={0};
        energized.tick(energyAura,thaumcraft.api.nodes.NodeType.NORMAL,null,new java.util.Random(1),()->energyChanges[0]++);
        require(energized.getRange()==8 && energized.isSource() && energized.visBase.getAmount(thaumcraft.api.aspects.Aspect.FIRE)==10,"Energized node range8 and sqrt100 gives10 per tick");
        require(energized.consumeVis(thaumcraft.api.aspects.Aspect.FIRE,7)==7 && energized.consumeVis(thaumcraft.api.aspects.Aspect.FIRE,7)==3,"Consumers share one bounded per-tick supply");
        energized.tick(energyAura,thaumcraft.api.nodes.NodeType.NORMAL,null,new java.util.Random(1),()->energyChanges[0]++);
        require(energized.vis.getAmount(thaumcraft.api.aspects.Aspect.FIRE)==10 && energyChanges[0]==1,"Next tick replenishes without rebuilding cached base");
        energized.setup(new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.FIRE,21),thaumcraft.api.nodes.NodeType.NORMAL,thaumcraft.api.nodes.NodeModifier.BRIGHT,new java.util.Random(1));
        require(energized.visBase.getAmount(thaumcraft.api.aspects.Aspect.FIRE)==5,"Bright multiplier applied before square-root flooring");
        energized.setup(new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.FIRE,1),thaumcraft.api.nodes.NodeType.NORMAL,thaumcraft.api.nodes.NodeModifier.FADING,new java.util.Random(1));
        require(energized.visBase.size()==0,"Fading integer truncation can remove tiny supply");
        energized.setup(new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.FIRE,36).add(thaumcraft.api.aspects.Aspect.ENERGY,81),thaumcraft.api.nodes.NodeType.NORMAL,null,new java.util.Random(1));
        require(energized.visBase.getAmount(thaumcraft.api.aspects.Aspect.FIRE)==9,"Compound primal reduction uses maximum contribution rather than sum");
        var unstableRandom=new java.util.Random(){@Override public int nextInt(int bound){return bound==500?1:0;}};
        energized.tick(new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.FIRE,1),thaumcraft.api.nodes.NodeType.UNSTABLE,null,unstableRandom,()->energyChanges[0]++);
        require(energized.visBase.size()==0 && energized.vis.size()==0,"Unstable reroll clears old supply and removes nonpositive shifted values");
        energized.setup(energyAura,thaumcraft.api.nodes.NodeType.NORMAL,null,new java.util.Random(1));
        var energySaved=new net.minecraft.nbt.CompoundTag();energized.writeVisBase(energySaved);
        var energyLoaded=new thaumcraft.common.tiles.EnergizedNodeState();energyLoaded.visBase.add(thaumcraft.api.aspects.Aspect.WATER,999);energyLoaded.readVisBase(energySaved);
        require(energyLoaded.visBase.getAmount(thaumcraft.api.aspects.Aspect.FIRE)==10 && energyLoaded.visBase.getAmount(thaumcraft.api.aspects.Aspect.WATER)==0 && energyLoaded.vis.size()==0,"Original AEB persistence replaces cached base without saving transient tick supply");
        energyLoaded.tick(energyAura,thaumcraft.api.nodes.NodeType.NORMAL,null,new java.util.Random(1),()->{throw new AssertionError("Persisted base must not rebuild");});
        require(energyLoaded.consumeVis(thaumcraft.api.aspects.Aspect.FIRE,20)==10,"Persisted source resumes with bounded original supply");
        var converter=new thaumcraft.common.tiles.NodeConverterState();
        int[] conversion={0,1,1,0,0,0,0}; // node kind, power, stabilizer, drains, energize, revert, explode
        var conversionHost=new thaumcraft.common.tiles.NodeConverterState.Host(){
            public boolean powered(){return conversion[1]==1;}public boolean hasStabilizer(){return conversion[2]==1;}
            public boolean ordinaryNode(){return conversion[0]==0;}public boolean energizedNode(){return conversion[0]==1;}
            public void energize(){conversion[0]=1;conversion[4]++;}public void revertAndEmpty(){conversion[0]=0;conversion[5]++;}
            public void drainRandomAspect(int count){conversion[3]++;}public void explodeNode(){conversion[0]=2;conversion[6]++;}
            public void changed(){}public void conversionEvent(){}
        };
        converter.tick(conversionHost);
        require(converter.status==1 && converter.count==1 && conversion[3]==1,"Converter initializes then consumes one aspect on first powered tick");
        converter.count=999;converter.tick(conversionHost);
        require(converter.count==1000 && conversion[4]==0,"Conversion waits until next tick after reaching1000");
        converter.tick(conversionHost);
        require(converter.status==2 && conversion[4]==1 && converter.count==1000,"Original charged converter creates energized node and checks status");
        conversion[1]=0;converter.tick(conversionHost);
        require(converter.count==999,"Unpowered energized converter counts down");
        converter.count=50;converter.tick(conversionHost);
        require(conversion[5]==1 && converter.status==0 && converter.count==49,"At50 revert and empty node before countdown decrement");
        conversion[0]=1;conversion[2]=0;converter.status=2;converter.count=1000;converter.checkStatus(conversionHost);
        require(conversion[6]==1 && converter.status==0 && converter.count==50,"Missing stabilizer explodes energized node before reset");
        var converterSaved=new net.minecraft.nbt.CompoundTag();converter.write(converterSaved);
        var converterLoaded=new thaumcraft.common.tiles.NodeConverterState();converterLoaded.read(converterSaved);
        require(converterLoaded.status==0 && converterLoaded.count==50,"Original converter status/count persistence");
        var infernal=new thaumcraft.common.tiles.InfernalFurnaceState();
        int[] furnaceCalls={0,0};
        var furnaceHost=new thaumcraft.common.tiles.InfernalFurnaceState.Host(){
            public int drainFireVis(int requested){require(requested==5,"Original infernal requests five fire vis");furnaceCalls[0]++;return 0;}
            public int activeBellows(){return 3;}
            public boolean hasSmeltableInput(){return true;}
            public void smeltFirstInput(){furnaceCalls[1]++;}
        };
        infernal.tick(furnaceHost);
        require(infernal.cookTime==80,"Three bellows reduce ordinary infernal cycle140 to80");
        for(int i=0;i<79;i++)infernal.tick(furnaceHost);
        require(infernal.cookTime==1&&furnaceCalls[1]==0,"Infernal does not eject before countdown reaches zero");
        infernal.tick(furnaceHost);
        require(infernal.cookTime==0&&furnaceCalls[1]==1,"Infernal ejects on final countdown tick");
        infernal.tick(furnaceHost);
        require(infernal.cookTime==80&&furnaceCalls[1]==1,"Next batch starts on separate tick after ejection");
        require(thaumcraft.common.tiles.InfernalFurnaceState.cookDuration(true,5)==20,"Original bellows cap3 and speedy80 yield20");
        infernal.speedyTime=1;infernal.cookTime=0;
        infernal.tick(furnaceHost);
        require(infernal.speedyTime==1&&infernal.cookTime==20,"Idle speedy charge remains and next batch uses speedy duration");
        var bellows=new thaumcraft.common.tiles.BellowsState();
        bellows.onVanillaFurnace=true;
        require(bellows.furnaceTick(false,198)==198,"Bellows needs two active ticks before acceleration");
        require(bellows.furnaceTick(true,198)==198&&bellows.delay==1,"Redstone pauses accumulated bellows delay");
        require(bellows.furnaceTick(false,198)==199,"Unpowering resumes unfinished bellows cycle");
        bellows.furnaceTick(false,199);
        require(bellows.furnaceTick(false,199)==199,"Original bellows never advances cook progress past 199");
        bellows.furnaceTick(false,0);
        require(bellows.furnaceTick(false,0)==0,"Bellows cannot start an idle furnace");
        bellows.clientTick(true,new Random(){@Override public float nextFloat(){throw new AssertionError("Powered bellows consumed animation randomness");}},pitch->{throw new AssertionError("Powered bellows played puff");});
        require(bellows.inflation==1&&bellows.firstRun,"Powered bellows leaves first animation initialization pending");
        var signal=new CrucibleState();
        require(signal.comparator()==0,"Empty aspects comparator zero even if filled with water");
        signal.water=1000;signal.aspects.add(Aspect.FIRE,1);
        require(signal.comparator()==1,"One aspect produces first comparator step");
        signal.aspects=new AspectList().add(Aspect.FIRE,50);
        require(signal.comparator()==8,"Half aspect capacity comparator eight");
        signal.aspects=new AspectList().add(Aspect.FIRE,100);
        require(signal.comparator()==15,"Full aspect capacity comparator fifteen");
        Random random = new Random(42);
        int[] spills = {0};
        Runnable spill = () -> ++spills[0];
        CrucibleState state = new CrucibleState();
        require(state.fill(1000) == 1000 && state.fill(1000) == 0, "Bucket capacity");
        for (int i = 0; i < 150; ++i) state.tick(true, 0, random, spill);
        require(state.heat == 150 && !state.boiling(), "150 is not boiling");
        state.tick(true, 0, random, spill);
        require(state.boiling() && state.heat == 151, "151 starts boiling");
        for (int i = 0; i < 100; ++i) state.tick(true, 0, random, spill);
        require(state.heat == 200 && state.water == 1000, "Empty boiling water does not evaporate");
        state.tick(false, 0, random, spill);
        require(state.heat == 199, "Removed heat source cools one per tick");
        state.water = 0;
        state.tick(true, 4, random, spill);
        require(state.heat == 198, "Dry crucible cools even above heat source");
        state.water = 1000;
        state.heat = 198;
        state.tick(true, 1, random, spill);
        require(state.heat == 201, "Original bellows increment may overshoot 200");
        state.aspects = new AspectList().add(Aspect.MAGIC, 1);
        state.counter = 100;
        state.tick(true, 0, random, spill);
        require(state.aspects.getAmount(Aspect.MAGIC) == 0 && state.tagAmount() == 1,
                "Decay splits one compound into one component");
        require(state.water == 998 && state.counter == 0, "Decay consumes two millibuckets");
        state.aspects = new AspectList().add(Aspect.AIR, 1);
        state.counter = 100;
        int before = spills[0];
        state.tick(true, 0, random, spill);
        require(state.tagAmount() == 0 && spills[0] == before + 1, "Primal decay spills flux");
        state.dissolve(new AspectList().add(Aspect.EARTH, 101));
        state.counter = -1;
        before = spills[0];
        state.tick(true, 0, random, spill);
        require(state.tagAmount() == 100 && spills[0] == before + 1, "Overflow removes one aspect every five ticks");
        require(state.fluidHeight() == .9999f, "Exactly full surface avoids z fighting");
        state.aspects.add(Aspect.EARTH, 1);
        require(state.fluidHeight() == 1.001f, "Overflow surface");
        state.water = 25;
        state.crafted(new AspectList().add(Aspect.EARTH, 3));
        require(state.water == 0 && state.counter == -250 && state.tagAmount() == 3,
                "Original recipe accepts last partial water charge and drains up to 50");
        before = spills[0];
        state.empty(spill);
        require(state.water == 0 && state.tagAmount() == 0 && spills[0] == before + 1,
                "Empty spills half remaining aspects");
        System.out.println("CrucibleStateTest PASS");
    }
}
