package dev.eugheist.tc4modern;

import net.minecraft.nbt.CompoundTag;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.research.ScanManager;

public final class OriginalPortTest {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        class Supply implements thaumcraft.api.visnet.VisNearbyRules.Node {
            final int pos;int stock;Supply parentSupply;boolean active=true;
            java.util.List<Supply> children=new java.util.ArrayList<>();
            Supply(int x,int n){pos=x;stock=n;}
            public int x(){return pos;}public int y(){return 0;}public int z(){return 0;}public int range(){return 8;}
            public boolean valid(){return active;}public boolean loaded(){return active;}public Iterable<Supply> children(){return children;}
            public int consumeVis(Aspect aspect,int n){if(!active)return 0;if(parentSupply!=null)return parentSupply.consumeVis(aspect,n);int taken=Math.min(stock,n);stock-=taken;return taken;}
        }
        var sourceSupply=new Supply(0,3);var firstRelay=new Supply(7,0);var secondRelay=new Supply(14,0);
        sourceSupply.children.add(firstRelay);firstRelay.parentSupply=sourceSupply;firstRelay.children.add(secondRelay);secondRelay.parentSupply=firstRelay;
        var supplyEndpoints=thaumcraft.api.visnet.VisNearbyRules.select(java.util.List.of(sourceSupply),20,0,0);
        require(supplyEndpoints.size()==1&&supplyEndpoints.get(0)==secondRelay,"Device outside source range selects reachable relay endpoint");
        int[] supplyEffects={0};
        require(thaumcraft.api.visnet.VisDrainRules.drain(supplyEndpoints,Aspect.FIRE,5,(node,color)->supplyEffects[0]++)==3&&sourceSupply.stock==0&&supplyEffects[0]==1,"Relay chain cannot create vis or consume source twice");
        sourceSupply.stock=3;var extraSupply=new Supply(19,4);
        supplyEndpoints=thaumcraft.api.visnet.VisNearbyRules.select(java.util.List.of(sourceSupply,extraSupply),20,0,0);
        require(thaumcraft.api.visnet.VisDrainRules.drain(supplyEndpoints,Aspect.FIRE,5,(node,color)->{})==5&&sourceSupply.stock==0&&extraSupply.stock==2,"Independent second source supplies only remaining demand");
        secondRelay.active=false;
        require(thaumcraft.api.visnet.VisNearbyRules.select(java.util.List.of(sourceSupply),20,0,0).isEmpty(),"Unloaded relay cannot remain a device endpoint");
        class VisStore implements thaumcraft.common.items.baubles.AmuletVisRules.Store {
            final int max;final java.util.Map<Aspect,Integer> amounts=new java.util.HashMap<>();
            VisStore(int max){this.max=max;}public int capacity(){return max;}
            public int get(Aspect a){return amounts.getOrDefault(a,0);}public void set(Aspect a,int n){amounts.put(a,n);}
        }
        var amulet=new VisStore(2500);var heldWand=new VisStore(5000);
        amulet.set(Aspect.FIRE,3);heldWand.set(Aspect.FIRE,4999);
        var draws=new java.util.HashMap<Aspect,Integer>();
        thaumcraft.common.items.baubles.AmuletVisRules.Relay chargingRelay=(a,n)->{draws.put(a,n);return n;};
        thaumcraft.common.items.baubles.AmuletVisRules.wornTick(4,amulet,heldWand,chargingRelay);
        require(draws.isEmpty()&&amulet.get(Aspect.FIRE)==3,"Amulet charging waits for the fifth tick");
        thaumcraft.common.items.baubles.AmuletVisRules.wornTick(5,amulet,heldWand,chargingRelay);
        require(heldWand.get(Aspect.FIRE)==5000&&amulet.get(Aspect.FIRE)==7,"Amulet transfers only wand room before relay refill");
        require(heldWand.get(Aspect.AIR)==0&&amulet.get(Aspect.AIR)==5,"Newly received relay vis reaches the wand on the next pulse");
        amulet.set(Aspect.AIR,2499);
        thaumcraft.common.items.baubles.AmuletVisRules.wornTick(10,amulet,null,chargingRelay);
        require(draws.get(Aspect.AIR)==1&&amulet.get(Aspect.AIR)==2500,"Relay draws are limited by amulet room");
        require(thaumcraft.common.items.baubles.AmuletVisRules.capacity(true)==25000&&thaumcraft.common.items.baubles.AmuletVisRules.capacity(false)==2500,"Original greater and lesser amulet capacities");
        var beamLife=new thaumcraft.client.fx.beams.BeamPowerState();
        require(beamLife.lifetime==8&&beamLife.red==.5f&&beamLife.opacity==.3f,"Original beam starts neutral with eight-tick lifetime");
        for(int i=0;i<12;i++){beamLife.update(false,1,1,1);beamLife.tick();}
        require(!beamLife.dead&&beamLife.lifetime-beamLife.age==3,"Active relay keeps original beam alive through four-tick extension");
        beamLife.update(true,1,.2f,0);beamLife.tick();
        require(Math.abs(beamLife.opacity-.775f)<.00001f&&beamLife.green==.2f,"Original beam tick decays refreshed coloured pulse");
        for(int i=0;i<3;i++)beamLife.tick();
        require(!beamLife.dead,"Disconnected beam survives remaining ticks");
        beamLife.tick();require(beamLife.dead,"Disconnected beam expires at original age threshold");
        var relayState=new thaumcraft.common.tiles.VisRelayState();
        require(relayState.color==-1&&relayState.orientation==1,"Original relay defaults");
        for(int i=0;i<7;i++)relayState.cycleColor();
        require(relayState.color==-1,"Seven wand attunements return wildcard");
        require(relayState.consumePulse(Aspect.AIR)==0&&relayState.consumePulse(Aspect.FIRE)==-1,"Relay pulses throttle to five ticks");
        for(int i=0;i<5;i++)relayState.tick(false);
        require(relayState.consumePulse(Aspect.FIRE)==1,"Relay pulse resets after five server ticks");
        relayState.receivePulse(2);relayState.tick(true);
        require(relayState.pulse==4&&relayState.red==.025f,"Original water pulse fades toward white");
        require(Math.abs(relayState.beamOpacity-.775f)<.00001f,"Original beam pulse starts at .8 and decays .025 per client tick");
        for(int i=0;i<4;i++)relayState.tick(true);
        require(relayState.pulse==0&&Math.abs(relayState.beamOpacity-.775f)<.00001f,"Original proxy refreshes beam opacity throughout the five-tick pulse");
        relayState.tick(true);
        require(Math.abs(relayState.beamOpacity-.75f)<.00001f,"Beam fades after the pulse ends");
        for(int i=0;i<40;i++)relayState.tick(true);
        require(relayState.beamOpacity==.3f,"Original beam opacity clamps at .3");
        relayState.px=-8;relayState.py=2;relayState.pz=7;relayState.color=3;
        var relayNbt=new CompoundTag();relayState.write(relayNbt);
        var restoredRelay=new thaumcraft.common.tiles.VisRelayState();restoredRelay.read(relayNbt);
        require(restoredRelay.px==-8&&restoredRelay.py==2&&restoredRelay.pz==7&&restoredRelay.color==3,"Original relay byte offsets and attunement persist");
        var rayStart=new net.minecraft.world.phys.Vec3(.5,.5,.5);
        var rayExit=VisNetwork.outsideSource(rayStart,new net.minecraft.world.phys.Vec3(4.5,4.5,4.5));
        require(rayExit.x>1&&rayExit.y>1&&rayExit.z>1&&rayExit.x<1.000001,"Diagonal vis ray exits source cell without skipping next block");
        var negativeExit=VisNetwork.outsideSource(rayStart,new net.minecraft.world.phys.Vec3(-3.5,.5,.5));
        require(negativeExit.x<0&&negativeExit.x>-.000001&&negativeExit.y==.5,"Negative vis ray starts outside source cell");
        class Relay implements thaumcraft.api.visnet.VisParentRules.Node {
            int px,color;java.util.List<Relay> links=new java.util.ArrayList<>();
            Relay(int x,int c){px=x;color=c;}
            public int x(){return px;}public int y(){return 0;}public int z(){return 0;}public int range(){return 8;}
            public int attunement(){return color;}public boolean valid(){return true;}public boolean loaded(){return true;}
            public Iterable<Relay> children(){return links;}public int consumeVis(Aspect aspect,int n){return 0;}
        }
        var relayTarget=new Relay(0,1);var source=new Relay(4,-1);var child=new Relay(3,1);source.links.add(child);
        require(thaumcraft.api.visnet.VisParentRules.select(relayTarget,java.util.List.of(source),(a,b)->true)==source,"Original source preference subtracts sixteen from squared distance");
        require(thaumcraft.api.visnet.VisParentRules.select(relayTarget,java.util.List.of(source),(a,b)->b!=source)==child,"Invisible source permits visible relay parent");
        child.color=2;
        require(thaumcraft.api.visnet.VisParentRules.select(relayTarget,java.util.List.of(source),(a,b)->b!=source)==null,"Different attuned colors cannot connect");
        child.color=-1;child.links.add(source);
        require(thaumcraft.api.visnet.VisParentRules.select(relayTarget,java.util.List.of(source),(a,b)->b!=source)==child,"Wildcard relay connects and cyclic traversal terminates");
        source.px=9;child.px=8;
        require(thaumcraft.api.visnet.VisParentRules.select(relayTarget,java.util.List.of(source),(a,b)->true)==child,"Relay range includes exact eight-block boundary");

        for(long seed:new long[]{0,1,42,123456789}){
            var bolt=new thaumcraft.client.fx.bolt.FXLightningBoltCommon(null,0,.5,0,0,-.5,0,seed,10,4f,5);
            bolt.defaultFractal();bolt.finalizeBolt();
            require(bolt.numsegments0==128,"Original seven binary subdivisions");
            require(bolt.segments.stream().filter(segment->segment.splitno==0).count()==128,"Complete main lightning path");
            require(bolt.particleAge==-3&&bolt.increment==5&&bolt.particleMaxAge>=5&&bolt.particleMaxAge<=14,"Original node bolt timing");
            for(var segment:bolt.segments){
                require(Float.isFinite(segment.diff.length())&&Float.isFinite(segment.sinprev)&&Float.isFinite(segment.sinnext),"Finite original bolt geometry");
            }
            while(bolt.particleAge<bolt.particleMaxAge)bolt.onUpdate();
            require(bolt.particleAge==bolt.particleMaxAge,"Lightning expiration clamps to lifetime");
        }

        if(Math.abs(thaumcraft.common.lib.vis.VisGearDiscount.withExhaustion(.1f,2,0)-(-.2f))>.0001f)throw new AssertionError("Exhaustion strongest amplifier penalty");
        if(thaumcraft.common.lib.vis.VisGearDiscount.withExhaustion(.1f,-1,-1)!=.1f)throw new AssertionError("No exhaustion penalty");

        require(thaumcraft.common.items.armor.RobeDyeColors.blend(6961280,new int[]{14})==0x8c3960,"Original robe red dye blends default purple with legacy sheep red");
        require(thaumcraft.common.items.armor.RobeDyeColors.blend(6961280,new int[]{0})==0xb49bbf,"Original white dye retains brightness and base color");
        var recipeInputs=java.util.List.of(new thaumcraft.common.lib.crafting.RecipeAspectMath.IngredientTags(new AspectList().add(Aspect.EARTH,10),null,false));
        var recipeTags=thaumcraft.common.lib.crafting.RecipeAspectMath.fromIngredients(recipeInputs,3);
        require(recipeTags.getAmount(Aspect.EARTH)==2,"Original recipe arithmetic floors 75 percent per output");
        require(thaumcraft.common.lib.crafting.RecipeAspectMath.fromIngredients(recipeInputs,9).size()==0,"Sub-unit recipe aspects are removed");
        var recipeCost=thaumcraft.common.lib.crafting.RecipeAspectMath.addCost(recipeTags,new AspectList().add(Aspect.FIRE,16),3);
        require(recipeCost.getAmount(Aspect.FIRE)==1 && recipeTags.getAmount(Aspect.FIRE)==0,"Arcane cost uses sqrt divided by output count without mutating input");
        require(thaumcraft.common.lib.crafting.RecipeAspectMath.fromIngredients(java.util.List.of(new thaumcraft.common.lib.crafting.RecipeAspectMath.IngredientTags(new AspectList().add(Aspect.EARTH,10),null,true)),1).size()==0,"Self-returning ingredient contributes no aspects");
        var containerInput=new thaumcraft.common.lib.crafting.RecipeAspectMath.IngredientTags(new AspectList().add(Aspect.EARTH,10),new AspectList().add(Aspect.EARTH,2),false);
        require(thaumcraft.common.lib.crafting.RecipeAspectMath.fromIngredients(java.util.List.of(containerInput),1).getAmount(Aspect.EARTH)==7,"Original container reduction acts on initially empty output, preserving original arithmetic");
        var alchemy=thaumcraft.common.lib.crafting.RecipeAspectMath.fromCrucible(new AspectList().add(Aspect.METAL,3),new AspectList().add(Aspect.MAGIC,4),1);
        require(alchemy.getAmount(Aspect.METAL)==3 && alchemy.getAmount(Aspect.MAGIC)==2,"Thaumium inherits full iron tags plus sqrt alchemy cost");
        var multiple=thaumcraft.common.lib.crafting.RecipeAspectMath.fromCrucible(new AspectList().add(Aspect.EARTH,7),new AspectList().add(Aspect.FIRE,16),2);
        require(multiple.getAmount(Aspect.EARTH)==7 && multiple.getAmount(Aspect.FIRE)==2,"Crucible output count divides only cost, never catalyst tags");
        require(thaumcraft.common.lib.crafting.RecipeAspectMath.capAspects(new AspectList().add(Aspect.EARTH,100),64).getAmount(Aspect.EARTH)==64,"Generated recipe aspects capped at64");
        require(thaumcraft.common.config.ConfigOreAspects.ENTRIES.size()==56,"All forty original literal ore records and sixteen dyes ported");
        var logEntry=thaumcraft.common.config.ConfigOreAspects.ENTRIES.stream().filter(e->e.legacyTag().equals("logWood")).findFirst().orElseThrow();
        require(logEntry.aspects().getAmount(Aspect.TREE)==4 && OreTagBridge.modern("logWood").toString().equals("minecraft:logs"),"Original log tag uses Arbor4 and modern log category");
        require(OreTagBridge.modern("dyeLightBlue").toString().equals("c:dyes/light_blue"),"Compound dye names map to modern tags");
        require(thaumcraft.common.config.ConfigOreAspects.ENTRIES.stream().filter(e->e.legacyTag().equals("oreCopper")).findFirst().orElseThrow().requiredIngot().equals("copper"),"Original optional metal requires an available ingot");
        require(thaumcraft.common.config.ConfigComplexAspects.ENTRIES.size()==56,"Original complex records, table variants and meat treat preserved");
        var treatExtra=thaumcraft.common.config.ConfigComplexAspects.get("tc4modern:triple_meat_treat").additional();
        require(treatExtra.getAmount(Aspect.HEAL)==1 && treatExtra.getAmount(Aspect.HUNGER)==0,"Treat adds healing; original removal on empty list leaves no negative hunger");
        for(var meat:new String[]{"beef","chicken","pork","fish"})require(AspectTable.get("tc4modern:"+meat+"_nugget").equals(java.util.Map.of("fames",1)),"Original edible nuggets contain Hunger1");
        require(thaumcraft.common.config.ConfigComplexAspects.get("minecraft:paper").additional().getAmount(Aspect.MIND)==1,"Original paper gains Cognitio1 over recipe-derived tags");
        require(thaumcraft.common.config.ConfigComplexAspects.get("minecraft:book").additional().getAmount(Aspect.MIND)==3,"Original book gains Cognitio3 over recipe-derived tags");
        require(thaumcraft.common.config.ConfigComplexAspects.get("minecraft:enchanted_book").inherited().equals("minecraft:book"),"Original enchanted book inherits ordinary book aspects");
        require(thaumcraft.common.items.armor.ItemGoggles.DURABILITY==350 && thaumcraft.common.items.armor.ItemGoggles.DISCOUNT==5,"Original goggles durability and vis discount");
        require(thaumcraft.common.items.armor.ItemGoggles.HEAD_DEFENSE==1 && thaumcraft.common.items.armor.ItemGoggles.ENCHANTMENT_VALUE==25,"Original special armor material stats");
        require(Math.abs(thaumcraft.common.lib.vis.VisStorage.consumptionModifier(1.1f,.05f,0,false,false,true)-1.05f)<.00001f,"Goggles reduce iron-cap crafting multiplier by five percent");
        require(Aspect.aspects.size() == 48, "TC4 has 48 built-in aspects");
        require(Aspect.getPrimalAspects().size() == 6, "TC4 has six primals");
        require(Aspect.getCompoundAspects().size() == 42, "TC4 has 42 compounds");
        for (Aspect aspect : Aspect.getCompoundAspects()) {
            Aspect[] parents = aspect.getComponents();
            require(ResearchManager.getCombinationResult(parents[0], parents[1]) == aspect, "Original combination result " + aspect.getTag());
            require(ResearchManager.getCombinationResult(parents[1], parents[0]) == aspect, "Original combinations are symmetric");
        }
        AspectList primals = ResearchManager.reduceToPrimals(new AspectList().add(Aspect.PLANT, 2));
        require(primals.getAmount(Aspect.EARTH) == 4 && primals.getAmount(Aspect.WATER) == 2, "Herba recursively reduces to 2 terra + 1 aqua");
        AspectList list = new AspectList().add(Aspect.EARTH, 2).merge(Aspect.EARTH, 1).add(Aspect.WATER, 0);
        require(list.getAmount(Aspect.EARTH) == 2, "Original merge takes max, not sum");
        CompoundTag encoded = new CompoundTag(); list.writeToNBT(encoded);
        AspectList decoded = new AspectList(); decoded.readFromNBT(encoded);
        require(decoded.getAmount(Aspect.EARTH) == 2 && decoded.aspects.containsKey(Aspect.WATER), "NBT preserves discovered aspects with zero points");
        PlayerKnowledge player = new PlayerKnowledge();
        require(player.hasDiscoveredAspect("one", Aspect.AIR), "Primal discovery is automatic");
        require(!ScanManager.validScan(new AspectList().add(Aspect.PLANT, 1), player, "one"), "Herba blocked until Victus is discovered");
        require(ScanManager.checkAndSyncAspectKnowledge(player, "one", Aspect.LIFE, 1, 100) == 3, "First compound discovery gives original +2 bonus");
        require(ScanManager.validScan(new AspectList().add(Aspect.PLANT, 1), player, "one"), "Herba unlocked after Victus");
        player.setAspectPool("one", Aspect.EARTH, (short)100);
        require(ScanManager.checkAndSyncAspectKnowledge(player, "one", Aspect.EARTH, 16, 100) == 4, "At original cap awards are square rooted");
        player.setAspectPool("one", Aspect.EARTH, (short)125);
        require(ScanManager.checkAndSyncAspectKnowledge(player, "one", Aspect.EARTH, 16, 100) == 1, "At 125 percent of cap awards reduce to one");
        CompoundTag data = new CompoundTag();
        require(ResearchKnowledge.scan(data, "minecraft:grass_block", AspectTable.get("minecraft:grass_block")).equals("missing:victus"), "Bridge must report missing component");
        require(!data.getCompound(ResearchKnowledge.KEY).getCompound("scanned").getBoolean("minecraft:grass_block"), "Blocked scan must not mark object studied");
        ResearchKnowledge.scan(data, "minecraft:stone", AspectTable.get("minecraft:stone"));
        ResearchKnowledge.scan(data, "minecraft:water", AspectTable.get("minecraft:water"));
        require(ResearchKnowledge.combine(data, "aqua", "terra").equals("combined:victus"), "Original combination reached through bridge");
        require(ResearchKnowledge.points(data, "victus") == 3, "Bridge keeps discovery bonus");
        require(ResearchKnowledge.scan(data, "minecraft:grass_block", AspectTable.get("minecraft:grass_block")).equals("scanned"), "Bridge unlocks original scanning chain");
        require(ResearchKnowledge.points(data, "herba") == 3, "First Herba receives original bonus");
        CompoundTag clone = data.copy();
        require(ResearchKnowledge.scan(clone, "minecraft:grass_block", AspectTable.get("minecraft:grass_block")).equals("already"), "Port keeps duplicate state through NBT");
        var pig=thaumcraft.common.config.ConfigEntityAspects.get("minecraft:pig");
        require(pig.getAmount(Aspect.BEAST)==2 && pig.getAmount(Aspect.EARTH)==2,"Original pig tags");
        require(thaumcraft.common.config.ConfigEntityAspects.get("minecraft:charged_creeper").getAmount(Aspect.ENERGY)==3,"Powered creeper variant");
        require(thaumcraft.common.config.ConfigEntityAspects.get("minecraft:wither_skeleton").getAmount(Aspect.FIRE)==2,"Old SkeletonType maps to wither skeleton");
        pig.add(Aspect.EARTH,99);
        require(thaumcraft.common.config.ConfigEntityAspects.get("minecraft:pig").getAmount(Aspect.EARTH)==2,"Entity registrations are not mutated by callers");
        var azanor=thaumcraft.common.config.ConfigEntityAspects.player("Azanor");
        require(azanor.getAmount(Aspect.ELDRITCH)==20 && azanor.getAmount(Aspect.MAN)==4,"Original named-player aspects");
        var playerTags=thaumcraft.common.config.ConfigEntityAspects.player("Dev");
        require(playerTags.visSize()==16,"Original player aspects total four human plus three four-point random tags");
        var again=thaumcraft.common.config.ConfigEntityAspects.player("Dev");
        for(var aspect:playerTags.getAspects())require(playerTags.getAmount(aspect)==again.getAmount(aspect),"Player aspects are deterministic");
        CompoundTag vis=new CompoundTag();
        require(thaumcraft.common.lib.vis.VisStorage.addVis(vis,Aspect.EARTH,24,true,2500)==0,"24 vis fits wood rod");
        require(thaumcraft.common.lib.vis.VisStorage.getVis(vis,Aspect.EARTH)==2400,"Original hundredths representation");
        require(thaumcraft.common.lib.vis.VisStorage.addVis(vis,Aspect.EARTH,3,false,2500)==2,"Simulation returns excess whole vis");
        require(vis.getInt("terra")==2400,"Simulation does not mutate vis");
        require(thaumcraft.common.lib.vis.VisStorage.addVis(vis,Aspect.EARTH,3,true,2500)==2 && vis.getInt("terra")==2500,"Capacity limits stored vis");
        require(thaumcraft.common.lib.vis.VisStorage.addVis(vis,Aspect.LIFE,1,true,2500)==0 && !vis.contains("victus"),"Original wand rejects compound vis");
        require(thaumcraft.common.lib.vis.VisStorage.addRealVis(vis,Aspect.WATER,99,true,2500)==0 && vis.getInt("aqua")==99,"Fractional vis stored without rounding");
        var node=new thaumcraft.common.lib.vis.NodeReservoir(new java.util.Random(1));
        node.setAspects(new AspectList().add(Aspect.EARTH,5));
        CompoundTag wand=new CompoundTag();
        require(node.charge(wand,2500,2,false)==Aspect.EARTH && wand.getInt("terra")==200 && node.aspects.getAmount(Aspect.EARTH)==3,"Node transfers whole vis to wand");
        require(node.aspectsBase.getAmount(Aspect.EARTH)==5,"Draining does not reduce node base immediately");
        require(node.charge(wand,2500,3,true)==Aspect.EARTH && node.aspects.getAmount(Aspect.EARTH)==1,"Preservation leaves last unit");
        require(node.charge(wand,2500,1,true)==null,"Preservation excludes last unit");
        require(node.charge(wand,2500,1,false)==Aspect.EARTH && node.aspects.getAmount(Aspect.EARTH)==0,"Basic iron wood wand can exhaust node");
        node.setAspects(new AspectList().add(Aspect.EARTH,5));wand.putInt("terra",2400);
        require(node.charge(wand,2500,3,false)==Aspect.EARTH && wand.getInt("terra")==2500 && node.aspects.getAmount(Aspect.EARTH)==4,"Unused overflow remains in node");
        require(node.charge(wand,2500,1,false)==null && node.aspects.getAmount(Aspect.EARTH)==4,"Full wand does not drain node");
        var recharge=new thaumcraft.common.lib.vis.NodeReservoir(new java.util.Random(2));
        recharge.setAspects(new AspectList().add(Aspect.WATER,10));recharge.takeFromContainer(Aspect.WATER,2);
        recharge.count=599;require(!recharge.recharge(100000),"Normal node waits 600 ticks");
        recharge.count=600;require(recharge.recharge(100000) && recharge.aspects.getAmount(Aspect.WATER)==9,"Normal node restores one aspect at 600 ticks");
        recharge.modifier=thaumcraft.api.nodes.NodeModifier.BRIGHT;recharge.regeneration=-1;recharge.count=400;
        require(recharge.recharge(100001) && recharge.aspects.getAmount(Aspect.WATER)==10,"Bright node restores at 400 ticks");
        recharge.modifier=thaumcraft.api.nodes.NodeModifier.FADING;recharge.regeneration=-1;recharge.takeFromContainer(Aspect.WATER,1);recharge.count=600;
        require(!recharge.recharge(100002) && recharge.aspects.getAmount(Aspect.WATER)==9,"Fading nodes never recharge");
        recharge.modifier=null;recharge.regeneration=-1;recharge.lastActive=1000;recharge.catchUp=true;recharge.count=1;
        recharge.recharge(91001);require(recharge.aspects.getAmount(Aspect.WATER)==10,"Original unloaded catch-up restores bounded deficit");
        var emptyNode=new thaumcraft.common.lib.vis.NodeReservoir(new java.util.Random(3));emptyNode.setAspects(new AspectList().add(Aspect.EARTH,1));emptyNode.takeFromContainer(Aspect.EARTH,1);emptyNode.count=1200;
        require(emptyNode.recharge(100000) && emptyNode.destroyed && emptyNode.aspectsBase.getAmount(Aspect.EARTH)==0,"Exhaustion erodes base and removes dead node");
        var scanNode=new thaumcraft.common.lib.vis.NodeReservoir(new java.util.Random(4));
        scanNode.setAspects(new AspectList().add(Aspect.AIR,5).add(Aspect.EARTH,100));
        require(scanNode.scanAspects().getAmount(Aspect.AIR)==4 && scanNode.scanAspects().getAmount(Aspect.EARTH)==10,"Original normal-node scan uses minimum four or one tenth current vis");
        require(thaumcraft.common.lib.vis.BiomeAura.aura(java.util.List.of("OCEAN","WET"))==100,"Original biome aura averages types");
        require(thaumcraft.common.lib.vis.BiomeAura.aura(java.util.List.of())==100,"Original biome fallback");
        require(thaumcraft.common.lib.vis.BiomeAura.tag(java.util.List.of("PLAINS"),new java.util.Random(1))==Aspect.AIR,"Original plains aspect");
        var generated=thaumcraft.common.lib.vis.NodeGenerator.generate(new java.util.Random(1),100,()->Aspect.WATER,101,0,0,0,false,false,false);
        var generatedAgain=thaumcraft.common.lib.vis.NodeGenerator.generate(new java.util.Random(1),100,()->Aspect.WATER,101,0,0,0,false,false,false);
        require(generated.aspects().getAmount(Aspect.WATER)>0,"Water environment retained");
        require(generated.type()==generatedAgain.type() && generated.modifier()==generatedAgain.modifier(),"Original node generation deterministic");
        for(var aspect:generated.aspects().getAspects())require(generated.aspects().getAmount(aspect)==generatedAgain.aspects().getAmount(aspect),"Seeded node allocation deterministic");
        require(scanNode.scanAspects(thaumcraft.api.nodes.NodeType.HUNGRY).getAmount(Aspect.HUNGER)==4,"Hungry node scan bonus");
        var origin=new net.minecraft.world.phys.Vec3(3,2,1);var target=new net.minecraft.world.phys.Vec3(0,0,0);
        var beam=thaumcraft.common.lib.vis.NodeBeam.strips(origin,target,1000000000L,10);
        require(beam.size()==2,"Original beam crosses two strips");
        require(thaumcraft.common.lib.vis.NodeBeam.strips(target,target,1000000000L,10).isEmpty(),"Zero-length beam has no invalid vertices");
        require(thaumcraft.common.lib.vis.NodeBeam.strips(origin,target,1000000000L,1).get(0).size()<beam.get(0).size(),"Beam grows during first ten use ticks");
        for(var strip:beam)for(var vertex:strip)require(Float.isFinite(vertex.x()) && Float.isFinite(vertex.y()) && Float.isFinite(vertex.z()) && vertex.alpha()>=0 && vertex.alpha()<=1,"Beam vertices finite with bounded opacity");
        require(thaumcraft.common.lib.vis.NodeBeam.smoothColor(0xFFFFFF,0x00FF00)==0xCCFFCC,"Original color interpolates one fifth per tick");
        var craftVis=new CompoundTag();craftVis.putInt("aer",1000);craftVis.putInt("ignis",100);
        var craftCost=new AspectList().add(Aspect.AIR,2).add(Aspect.FIRE,2);
        require(!thaumcraft.common.lib.vis.VisStorage.consumeAllVisCrafting(craftVis,craftCost,true,a->1.1f) && craftVis.getInt("aer")==1000 && craftVis.getInt("ignis")==100,"Failed crafting cannot partly consume other aspects");
        craftVis.putInt("ignis",1000);
        require(thaumcraft.common.lib.vis.VisStorage.consumeAllVisCrafting(craftVis,craftCost,false,a->1.1f) && craftVis.getInt("aer")==1000,"Crafting preview does not consume vis");
        require(thaumcraft.common.lib.vis.VisStorage.consumeAllVisCrafting(craftVis,craftCost,true,a->1.1f) && craftVis.getInt("aer")==780 && craftVis.getInt("ignis")==780,"Iron caps add original ten percent crafting cost in hundredths");
        require(!thaumcraft.common.lib.vis.VisStorage.consumeAllVisCrafting(craftVis,new AspectList(),true,a->1f),"Original empty vis cost rejects crafting consumer");
        require(Math.abs(thaumcraft.common.lib.vis.VisStorage.consumptionModifier(1.1f,.2f,3,true,false,true)-.9f)<.0001f,"Frugal focus does not discount crafting");
        require(Math.abs(thaumcraft.common.lib.vis.VisStorage.consumptionModifier(1.1f,.2f,3,true,true,false)-.5f)<.0001f,"Focus and sceptre discounts apply outside crafting");
        var arcaneShape=new thaumcraft.api.crafting.ArcanePattern<String>("TEST",2,1,java.util.List.of(s1->s1.equals("gold"),s1->s1.equals("string")));
        var arcaneGrid=new java.util.ArrayList<String>(java.util.Arrays.asList(null,null,null,null,null,null,null,"string","gold"));
        require(arcaneShape.matches(arcaneGrid,key->true),"Original arcane shape supports shifted mirrored grid");
        require(!arcaneShape.matches(arcaneGrid,key->false),"Arcane recipes require completed research");
        require(!arcaneShape.setMirrored(false).matches(arcaneGrid,key->true),"Disabled mirroring rejects reversed ingredients");
        arcaneGrid.set(7,"gold");arcaneGrid.set(8,"string");
        require(arcaneShape.matches(arcaneGrid,key->true),"Unmirrored shifted original shape");
        arcaneGrid.set(0,"extra");require(!arcaneShape.matches(arcaneGrid,key->true),"Items outside arcane shape reject recipe");
        int[] leafLogDistance={4};
        var leafView=new thaumcraft.common.lib.world.GreatwoodLeafDecay.View(){
            public boolean sustains(int x,int y,int z){return x==leafLogDistance[0]&&y==0&&z==0;}
            public boolean leaves(int x,int y,int z){return x>=0&&x<leafLogDistance[0]&&y==0&&z==0;}
        };
        require(thaumcraft.common.lib.world.GreatwoodLeafDecay.supported(leafView),"Original leaves reach a log through four edges");
        leafLogDistance[0]=5;
        require(!thaumcraft.common.lib.world.GreatwoodLeafDecay.supported(leafView),"Original leaves cannot reach five-block distant log");
        require(!thaumcraft.common.lib.world.GreatwoodLeafDecay.supported(new thaumcraft.common.lib.world.GreatwoodLeafDecay.View(){
            public boolean sustains(int x,int y,int z){return x==1&&y==1&&z==0;}
            public boolean leaves(int x,int y,int z){return x==0&&y==0&&z==0;}
        }),"Diagonal log alone does not support original leaf connectivity");
        System.out.println("Original TC4 port tests passed: 48 aspects, 42 combinations, NBT, prerequisites, bonuses, caps, bridge discovery chain.");
    }
}
