package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.utils.HexUtils;

/**
 * Dedicated progression fixture without granted research or wand vis. Supplied materials, placed ores/nodes and scan targets
 * stand in for exploration and gathering; this is not a fresh-world survival playthrough. Vanilla recipe lookup of the
 * first tools, mining shards, the wand-made Thaumonomicon/workbench/crucible, thaumometer scans, the research table from two
 * tables and scribing tools, notes requested from the book, a solved hex puzzle placed through the real packet handlers,
 * and the discovery read into completed research. Continues through upgraded wands, distillation, infusion and a gather golem.
 */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class ProgressionAudit {
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_PROGRESSION FAIL "+text);System.out.println("TC4_PROGRESSION PASS "+text);}
    static final BlockPos BASE=new BlockPos(64,200,64);
    static net.neoforged.neoforge.network.handling.IPayloadContext context(ServerPlayer p){
        return (net.neoforged.neoforge.network.handling.IPayloadContext)java.lang.reflect.Proxy.newProxyInstance(ProgressionAudit.class.getClassLoader(),new Class<?>[]{net.neoforged.neoforge.network.handling.IPayloadContext.class},(proxy,method,args)->method.getName().equals("player")?p:null);
    }
    /** Real vanilla recipe lookup: the grid must craft exactly this item. */
    static ItemStack craft(ServerLevel l,ServerPlayer p,int w,int h,Item... grid){
        var items=new ArrayList<ItemStack>();for(var i:grid)items.add(i==null?ItemStack.EMPTY:new ItemStack(i));while(items.size()<w*h)items.add(ItemStack.EMPTY);
        var input=CraftingInput.of(w,h,items);var recipe=l.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,l).orElse(null);
        return recipe==null?ItemStack.EMPTY:recipe.value().assemble(input,l.registryAccess());
    }
    static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    static void pickUp(ServerLevel l,ServerPlayer p,BlockPos around){for(var e:l.getEntitiesOfClass(ItemEntity.class,new AABB(around).inflate(4))){p.getInventory().add(e.getItem().copy());e.discard();}}
    static boolean use(ServerPlayer p,ItemStack stack,BlockPos pos){p.setItemInHand(InteractionHand.MAIN_HAND,stack);var ctx=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        var r=stack.getItem().onItemUseFirst(stack,ctx);if(r.consumesAction())return true;return stack.useOn(ctx).consumesAction();}

    /** Breadth-first linking of every anchor through empty hexes with aspects the player knows and can pay for. */
    static List<Object[]> solve(thaumcraft.common.lib.research.ResearchNoteData note,Set<Aspect> known){
        var tree=new HashMap<String,Aspect>();var anchors=new ArrayList<String>();
        for(var e:note.hexEntries.entrySet())if(e.getValue().type==1)anchors.add(e.getKey());
        if(anchors.isEmpty())return List.of();
        tree.put(anchors.get(0),note.hexEntries.get(anchors.get(0)).aspect);var remaining=new ArrayList<>(anchors.subList(1,anchors.size()));var placed=new ArrayList<Object[]>();var used=new HashSet<String>(anchors);
        while(!remaining.isEmpty()){
            record State(String cell,Aspect aspect){}
            var prev=new HashMap<State,State>();var queue=new ArrayDeque<State>();
            for(var e:tree.entrySet()){var s=new State(e.getKey(),e.getValue());prev.put(s,null);queue.add(s);}
            State goal=null;
            while(!queue.isEmpty()&&goal==null){var s=queue.poll();var hex=note.hexes.get(s.cell());
                for(int d=0;d<6&&goal==null;d++){var n=hex.getNeighbour(d).toString();var entry=note.hexEntries.get(n);if(entry==null)continue;
                    if(entry.type==1&&remaining.contains(n)&&ResearchTableGraph.linked(s.aspect(),entry.aspect)){goal=new State(n,entry.aspect);prev.put(goal,s);continue;}
                    if(entry.type!=0||used.contains(n))continue;
                    for(var a:known){if(!ResearchTableGraph.linked(s.aspect(),a))continue;var next=new State(n,a);if(prev.containsKey(next))continue;prev.put(next,s);queue.add(next);}}}
            if(goal==null)return null;
            remaining.remove(goal.cell());tree.put(goal.cell(),goal.aspect());
            for(var s=prev.get(goal);s!=null&&!tree.containsKey(s.cell());s=prev.get(s)){tree.put(s.cell(),s.aspect());used.add(s.cell());placed.add(new Object[]{note.hexes.get(s.cell()),s.aspect()});}
        }
        return placed;
    }

    static int ticks=-1;
    @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event){
        if(!Boolean.getBoolean("tc4modern.progressionAudit"))return;var l=event.getServer().overworld();
        /* no real player keeps these chunks entity-loaded on a dedicated server */for(int cx=-1;cx<=1;cx++)for(int cz=-1;cz<=1;cz++)l.setChunkForced((BASE.getX()>>4)+cx,(BASE.getZ()>>4)+cz,true);
        ticks=0;
    }
    static ServerPlayer novice;static ItemStack heldWand=ItemStack.EMPTY,earnedNitor=ItemStack.EMPTY;static BlockPos crucibleAt,benchAt;static int stage,stageTick;
    @SubscribeEvent static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        if(ticks<0)return;ticks++;var server=event.getServer();
        try{
            if(ticks==40){run(server);phaseTwoSetup(server.overworld());stage=1;stageTick=ticks;return;}
            if(stage>0&&phaseTwo(server.overworld())){System.out.println("TC4_PROGRESSION ALL PASS");ticks=-1;server.halt(false);}
            else if(ticks>90000)throw new IllegalStateException("TC4_PROGRESSION FAIL timed out in stage "+stage);
        }catch(RuntimeException e){System.out.println("TC4_PROGRESSION FAILED "+e);e.printStackTrace();ticks=-1;server.halt(false);}
    }
    static void phaseTwoSetup(ServerLevel l){
        var p=novice;l.setBlock(crucibleAt.below(),Blocks.LAVA.defaultBlockState(),3);
        var bucket=new ItemStack(Items.WATER_BUCKET);p.setItemInHand(InteractionHand.MAIN_HAND,bucket);
        l.getBlockState(crucibleAt).useItemOn(bucket,l,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(crucibleAt),Direction.UP,crucibleAt,false));
        var tile=(CrucibleEntity)l.getBlockEntity(crucibleAt);check(tile.crucible.water>=1000&&p.getMainHandItem().is(Items.BUCKET),"a water bucket fills the crucible and comes back empty");
    }
    static int vis(ItemStack wand){int n=0;for(var a:Aspect.getPrimalAspects())n+=WandData.get(wand,a);return n;}
    /** Charge the wand from a wild node by holding use on it, then craft the first arcane recipe the novice may make. */
    static void phaseThree(ServerLevel l){
        var p=novice;var wand=heldWand;
        check(vis(wand)==0,"the crafted novice wand starts without granted vis");
        var completed=ResearchProgress.completed(p.getPersistentData());ArcaneRecipes.Entry chosen=null;var open=new ArrayList<String>();
        var candidates=new ArrayList<ArcaneRecipes.Entry>();
        for(var e:ArcaneRecipes.entries()){
            if(!e.research().isEmpty()&&!completed.contains(e.research()))continue;open.add(e.key());
            boolean plain=e.ingredients().stream().allMatch(i->i.isEmpty()||i.getItems().length>0&&(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(i.getItems()[0].getItem()).getNamespace().equals("minecraft")||TC4Modern.SHARDS.stream().anyMatch(sh->i.getItems()[0].is(sh.get()))));
            if(plain)candidates.add(e);
        }
        check(!candidates.isEmpty(),"the novice has an open arcane recipe with ordinary ingredients: "+open);
        // Several ordinary seeded nodes stand in for exploration; never inject aspects or vis into the wand.
        for(int attempt=0;attempt<16&&chosen==null;attempt++){
            var at=BASE.offset(-4+(attempt%4)*3,2,-4-(attempt/4)*3);l.removeBlock(at,false);
            check(AuraNodeFeature.createRandomNodeAt(l,at,net.minecraft.util.RandomSource.create(11+attempt),false,false,false)&&l.getBlockEntity(at) instanceof AuraNodeEntity,"wild node "+(attempt+1)+" stands in reach");
            p.moveTo(at.getX()+.5,at.getY()-1.1,at.getZ()+3.5,180,0);p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(at));
            p.setItemInHand(InteractionHand.MAIN_HAND,wand);int before=vis(wand);
            wand.getItem().use(l,p,InteractionHand.MAIN_HAND);check(p.isUsingItem(),"right-clicking node "+(attempt+1)+" starts drawing vis");
            for(int remaining=200;remaining>0;remaining--)wand.getItem().onUseTick(l,p,wand,remaining);p.stopUsingItem();
            int after=vis(wand);check(after>=before&&(attempt>0||after>before),"node charging: "+before+" -> "+after+" centivis");
            // Preview uses exactly the same cap/gear modifier as the real workbench (iron caps cost 110%).
            for(var e:candidates)if(WandData.consumeCrafting(wand,e.cost(),false,p)){chosen=e;break;}
        }
        System.out.println("TC4_PROGRESSION arcane recipes open "+open+" wand "+Aspect.getPrimalAspects().stream().map(a->a.getTag()+"="+WandData.get(wand,a)).toList());
        check(chosen!=null,"an open arcane recipe is affordable including the iron cap modifier");
        var bench=(ArcaneWorkbenchEntity)l.getBlockEntity(benchAt);p.moveTo(benchAt.getX()+.5,benchAt.getY(),benchAt.getZ()+1.5);
        var menu=new ArcaneWorkbenchMenu(3,p.getInventory(),ContainerLevelAccess.create(l,benchAt),bench.inventory);p.containerMenu=menu;
        bench.inventory.setItem(9,wand);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        int w=chosen.width();for(int i=0;i<chosen.ingredients().size();i++){var ing=chosen.ingredients().get(i);if(ing.isEmpty())continue;bench.inventory.setItem(i/w*3+i%w,ing.getItems()[0].copyWithCount(1));}
        var shown=menu.getSlot(0).getItem().copy();check(ItemStack.isSameItemSameComponents(shown,chosen.output()),"the workbench shows "+chosen.key()+" once wand and ingredients are in: "+shown);
        var visBefore=new HashMap<Aspect,Integer>();for(var a:Aspect.getPrimalAspects())visBefore.put(a,WandData.get(bench.inventory.getItem(9),a));
        var remainders=new ArrayList<ItemStack>();for(int i=0;i<9;i++)remainders.add(bench.inventory.getItem(i).getCraftingRemainingItem());
        menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,p);var carried=menu.getCarried();
        check(ItemStack.isSameItemSameComponents(carried,chosen.output()),"taking the result gives "+carried);
        check(carried.getCount()==chosen.output().getCount(),"the result has the original recipe output count");
        int spent=0;for(var a:Aspect.getPrimalAspects()){
            int expected=(int)(chosen.cost().getAmount(a)*100*1.1f),actual=visBefore.get(a)-WandData.get(bench.inventory.getItem(9),a);
            check(actual==expected,"actual "+a.getTag()+" cost includes iron caps: "+actual+" centivis");spent+=actual;
        }
        check(spent>0,"the real result-slot click spends wand vis");
        for(int i=0;i<9;i++)check(ItemStack.matches(bench.inventory.getItem(i),remainders.get(i)),"ingredient slot "+i+" retains only its crafting remainder");
        menu.removed(p);p.containerMenu=p.inventoryMenu;

    }
    static void toss(ServerLevel l,Item item,int count){var e=new ItemEntity(l,crucibleAt.getX()+.5,crucibleAt.getY()+.6,crucibleAt.getZ()+.5,new ItemStack(item,count));e.setDeltaMovement(Vec3.ZERO);e.setThrower(novice);l.addFreshEntity(e);}
    /** Ticked part: heat the crucible over lava, melt fuel and light into essentia, then drop glowstone as the catalyst. */
    static boolean phaseTwo(ServerLevel l){
        var tile=(CrucibleEntity)l.getBlockEntity(crucibleAt);
        switch(stage){
            case 4->{if(ProgressionInfusionAudit.tick()){ProgressionGolemAudit.begin();stage=5;}return false;}
            case 5->{if(ProgressionGolemAudit.tick()){ProgressionOuterAudit.begin();stage=6;}return false;}
            case 6->{return ProgressionOuterAudit.tick();}
            case 1->{if(!tile.crucible.boiling())return false;System.out.println("TC4_PROGRESSION PASS lava under the crucible brings the water to the boil after "+(ticks-stageTick)+" ticks");
                toss(l,Items.COAL,2);toss(l,Items.TORCH,8);stage=2;stageTick=ticks;}
            case 2->{if(ticks-stageTick<80)return false;var a=tile.crucible.aspects;
                check(a.getAmount(Aspect.ENERGY)>=3&&a.getAmount(Aspect.FIRE)>=3&&a.getAmount(Aspect.LIGHT)>=3,"coal and torches melt into potentia, ignis and lux: "+a.getAmount(Aspect.ENERGY)+"/"+a.getAmount(Aspect.FIRE)+"/"+a.getAmount(Aspect.LIGHT));
                toss(l,Items.GLOWSTONE_DUST,1);stage=3;stageTick=ticks;}
            case 3->{if(ticks-stageTick<60)return false;int nitor=0;for(var e:l.getEntitiesOfClass(ItemEntity.class,new AABB(crucibleAt).inflate(3)))if(e.getItem().is(TC4Modern.NITOR.get()))nitor+=e.getItem().getCount();
                check(nitor==1,"glowstone dust thrown by the researcher becomes nitor");for(var drop:l.getEntitiesOfClass(ItemEntity.class,new AABB(crucibleAt).inflate(3)))if(drop.getItem().is(TC4Modern.NITOR.get())){earnedNitor=drop.getItem().copy();drop.discard();}phaseThree(l);ProgressionInfusionAudit.begin(l);stage=4;return false;}
        }
        return false;
    }
    static void run(net.minecraft.server.MinecraftServer server){
        var l=server.overworld();thaumcraft.common.config.ConfigResearchData.init();
        l.getEntitiesOfClass(ItemEntity.class,new AABB(BASE).inflate(24)).forEach(ItemEntity::discard);
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=-1;y<4;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
        var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(l,new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("tc4-novice".getBytes()),"Novice"));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();p.moveTo(BASE.getX()+.5,BASE.getY(),BASE.getZ()+.5);
        novice=p;var data=p.getPersistentData();data.remove(ResearchKnowledge.KEY);ResearchKnowledge.initialize(data);
        var completed=ResearchProgress.completed(data);var auto=new HashSet<String>();for(var c:ResearchCategories.researchCategories.values())for(var r:c.research.values())if(r.isAutoUnlock())auto.add(r.key);
        check(completed.containsAll(auto)&&completed.size()==auto.size(),"a new player knows only the "+auto.size()+" auto-unlocked entries");
        var k=ResearchKnowledge.load(data);check(Arrays.stream(Aspect.getPrimalAspects().toArray(new Aspect[0])).allMatch(a->k.hasDiscoveredAspect("current",a)),"a new player starts with the six primal aspects discovered");

        // vanilla crafting of the first tools
        var cap=craft(l,p,3,2,Items.IRON_NUGGET,Items.IRON_NUGGET,Items.IRON_NUGGET,Items.IRON_NUGGET,null,Items.IRON_NUGGET);
        check(cap.is(WandParts.CAPS.get("iron").item().get()),"iron nuggets craft iron wand caps: "+cap);
        var capItem=cap.getItem();var wand=craft(l,p,3,3,null,null,capItem,null,Items.STICK,null,capItem,null,null);
        check(WandData.isWand(wand)&&WandData.capacity(wand)>0,"caps and a stick craft the iron-capped wooden wand: "+wand);
        var table=craft(l,p,3,2,Items.OAK_SLAB,Items.OAK_SLAB,Items.OAK_SLAB,Items.OAK_PLANKS,null,Items.OAK_PLANKS);
        check(table.is(TC4Modern.TABLE_ITEM.get()),"slabs and planks craft the table: "+table);
        var tools=ItemStack.EMPTY;for(var shape:List.of(new Item[]{Items.INK_SAC,Items.FEATHER,Items.GLASS_BOTTLE},new Item[]{Items.FEATHER,Items.INK_SAC,Items.GLASS_BOTTLE},new Item[]{Items.GLASS_BOTTLE,Items.INK_SAC,Items.FEATHER}))if(tools.isEmpty())tools=craft(l,p,3,1,shape);
        if(tools.isEmpty())tools=craft(l,p,2,2,Items.INK_SAC,Items.FEATHER,Items.GLASS_BOTTLE,null);
        check(tools.is(TC4Modern.SCRIBING_TOOLS.get()),"ink, feather and a bottle craft scribing tools: "+tools);

        // mine shards from infused stone with an ordinary pickaxe
        int shards=0;var shardItems=new HashSet<Item>();
        for(int i=0;i<6;i++){var ore=BASE.offset(-5+i*2,0,-5);var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(AlchemyContent.id(new String[]{"air","fire","water","earth","order","entropy"}[i]+"_infused_stone"));
            l.setBlock(ore,block.defaultBlockState(),3);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));p.gameMode.destroyBlock(ore);pickUp(l,p,ore);}
        for(int i=0;i<6;i++){int n=count(p,TC4Modern.SHARDS.get(i).get());shards+=n;if(n>0)shardItems.add(TC4Modern.SHARDS.get(i).get());}
        check(shardItems.size()==6,"mining each infused stone with an iron pickaxe yields its shard ("+shards+" shards)");
        var thaumometer=craft(l,p,3,3,null,TC4Modern.SHARDS.get(0).get(),null,Items.GOLD_INGOT,Items.GLASS,Items.GOLD_INGOT,null,TC4Modern.SHARDS.get(1).get(),null);
        check(thaumometer.getItem() instanceof ThaumometerItem,"two shards, gold and glass craft the thaumometer: "+thaumometer);

        // wand conversions: bookshelf, table, cauldron
        var shelf=BASE.offset(0,0,3);l.setBlock(shelf,Blocks.BOOKSHELF.defaultBlockState(),3);use(p,wand,shelf);pickUp(l,p,shelf);
        check(count(p,TC4Modern.THAUMONOMICON.get())==1&&l.getBlockState(shelf).isAir(),"the wand turns a bookshelf into the Thaumonomicon: books "+count(p,TC4Modern.THAUMONOMICON.get())+" shelf "+l.getBlockState(shelf));
        var bench=BASE.offset(2,0,3);l.setBlock(bench,TC4Modern.TABLE.get().defaultBlockState(),3);use(p,wand,bench);
        var benchEntity=(ArcaneWorkbenchEntity)l.getBlockEntity(bench);
        check(l.getBlockState(bench).is(TC4Modern.ARCANE_WORKBENCH.get())&&WandData.isWand(benchEntity.inventory.getItem(9))&&wand.isEmpty(),"the wand turns a table into the arcane workbench and rests in its wand slot");
        wand=benchEntity.inventory.removeItemNoUpdate(9);check(WandData.isWand(wand),"the novice takes the wand back out of the workbench");
        var cauldron=BASE.offset(4,0,3);crucibleAt=cauldron;benchAt=bench;l.setBlock(cauldron,Blocks.CAULDRON.defaultBlockState(),3);use(p,wand,cauldron);
        check(!l.getBlockState(cauldron).is(Blocks.CAULDRON),"the wand turns a cauldron into the crucible: "+l.getBlockState(cauldron));

        // thaumometer scans of everyday things award aspect points
        var scanned=new ArrayList<String>();var targets=new ArrayList<ScanTarget>();
        for(var item:List.of(Items.OAK_LOG,Items.COBBLESTONE,Items.DIRT,Items.SAND,Items.GLASS,Items.TORCH,Items.WHEAT,Items.APPLE,Items.FEATHER,Items.BONE,Items.ROTTEN_FLESH,Items.STRING,Items.GUNPOWDER,Items.IRON_INGOT,Items.GOLD_INGOT,Items.REDSTONE,Items.COAL,Items.FLINT,Items.LEATHER,Items.BEEF,Items.PORKCHOP,Items.EGG,Items.SUGAR_CANE,Items.CACTUS,Items.SNOWBALL,Items.ICE,Items.CLAY_BALL,Items.WATER_BUCKET,Items.LAVA_BUCKET,Items.ENDER_PEARL,Items.SLIME_BALL,Items.SPIDER_EYE,Items.GLOWSTONE_DUST,Items.OBSIDIAN,Items.BOOK,Items.PAPER,Items.DIAMOND,Items.EMERALD,Items.BLAZE_ROD,Items.MAGMA_CREAM,Items.OAK_SAPLING,Items.WHITE_WOOL,Items.MILK_BUCKET,Items.BREAD,Items.ARROW,Items.CLOCK,Items.COMPASS,Items.MUSHROOM_STEW,Items.NETHERRACK,Items.SOUL_SAND,Items.QUARTZ,Items.CHEST,Items.FURNACE,Items.CRAFTING_TABLE,Items.TNT,Items.RAIL,Items.SADDLE,Items.MOSSY_COBBLESTONE,Items.VINE,Items.LILY_PAD)){
            var stack=new ItemStack(item);var target=ScanTarget.item(stack,l);if(target!=null)targets.add(target);}
        /* wild nodes and ordinary mobs: the novice's main sources of the airy and orderly primals */
        var nodeRandom=net.minecraft.util.RandomSource.create(5);
        for(int i=0;i<4;i++){var at=BASE.offset(-5+i*3,3,-2);l.removeBlock(at,false);AuraNodeFeature.createRandomNodeAt(l,at,nodeRandom,false,false,false);
            if(l.getBlockEntity(at) instanceof AuraNodeEntity node){var entries=new LinkedHashMap<String,Integer>();var scan=node.reservoir.scanAspects(node.nodeType);for(var a:scan.getAspects())entries.put(a.getTag(),scan.getAmount(a));targets.add(new ScanTarget("node:"+node.nodeId,"node:"+node.nodeId,entries));}}
        for(var type:List.<net.minecraft.world.entity.EntityType<?>>of(net.minecraft.world.entity.EntityType.COW,net.minecraft.world.entity.EntityType.PIG,net.minecraft.world.entity.EntityType.CHICKEN,net.minecraft.world.entity.EntityType.SHEEP,net.minecraft.world.entity.EntityType.ZOMBIE,net.minecraft.world.entity.EntityType.SKELETON,net.minecraft.world.entity.EntityType.SPIDER,net.minecraft.world.entity.EntityType.CREEPER,net.minecraft.world.entity.EntityType.SQUID,net.minecraft.world.entity.EntityType.BAT,net.minecraft.world.entity.EntityType.VILLAGER,net.minecraft.world.entity.EntityType.ENDERMAN)){
            var mob=type.create(l);if(mob==null)continue;mob.moveTo(BASE.getX()+.5,BASE.getY(),BASE.getZ()-4.5);var target=ScanTarget.entity(mob);if(target!=null)targets.add(target);}
        int nodesAdded=(int)targets.stream().filter(t->t.knowledgeKey().startsWith("node:")).count();check(nodesAdded>=3,"wild nodes are there to scan: "+nodesAdded);

        // research table from two tables and the scribing tools
        var t1=BASE.offset(-2,0,0);var t2=BASE.offset(-1,0,0);l.setBlock(t1,TC4Modern.TABLE.get().defaultBlockState(),3);l.setBlock(t2,TC4Modern.TABLE.get().defaultBlockState(),3);
        use(p,tools.copy(),t1);check(l.getBlockState(t1).is(TC4Modern.RESEARCH_TABLE.get())&&l.getBlockState(t2).is(TC4Modern.RESEARCH_TABLE.get()),"scribing tools join two tables into the research table");
        var tableEntity=(ResearchTableEntity)l.getBlockEntity(t1);check(tableEntity.inventory.getItem(0).is(TC4Modern.SCRIBING_TOOLS.get()),"the scribing tools sit in the table");
        var tableMenu=new ResearchTableMenu(1,p.getInventory(),ContainerLevelAccess.create(l,t1),tableEntity.inventory,tableEntity.bonus.bonusAspects);p.containerMenu=tableMenu;

        // choose the first research a novice can start with notes
        completed=ResearchProgress.completed(data);String key=null;var candidates=new ArrayList<String>();
        for(var c:ResearchCategories.researchCategories.values())for(var r:c.research.values()){
            if(completed.contains(r.key)||r.isVirtual()||r.isStub()||r.isLost()||r.isHidden()||r.isAutoUnlock()||!ResearchProgress.requisites(completed,r))continue;
            if(ResearchConfig.purchaseMode(r,ResearchConfig.difficulty()))continue;candidates.add(r.key);}
        System.out.println("TC4_PROGRESSION candidates "+candidates);
        check(!candidates.isEmpty(),"some research is open to a novice: "+candidates.size());
        for(var preferred:List.of("NITOR","ALUMENTUM","CRUCIBLE","GOGGLES","THAUMIUM","NODETAPPER1"))if(key==null&&candidates.contains(preferred))key=preferred;if(key==null)key=candidates.get(0);
        var research=ResearchCategories.getResearch(key);

        // scan, combine newly reachable compounds at the table, and rescan what was blocked, until nothing changes
        for(int cycle=0;cycle<10;cycle++){int scansBefore=scanned.size(),knownBefore=ResearchKnowledge.load(data).getAspectsDiscovered("current").size();
            /* objects whose compounds still lack a known parent fail like the original; rescan after each round of discoveries */
            var reasons=new TreeMap<String,String>();
            for(int round=0;round<8;round++){int before=scanned.size();
                for(var target:targets){if(scanned.contains(target.knowledgeKey()))continue;var result=ResearchKnowledge.scan(data,target.knowledgeKey(),target.aspects(),awarded->ResearchClues.revealClue(p,target.entityClue(),target.knowledgeKey().startsWith("entity:")||target.knowledgeKey().startsWith("node:")?null:target.knowledgeKey(),awarded));
                        if(result.equals("scanned"))scanned.add(target.knowledgeKey());else reasons.put(target.knowledgeKey(),result);}
                if(scanned.size()==before)break;}
            scanned.forEach(reasons::remove);System.out.println("TC4_PROGRESSION unscanned "+reasons);
            // compounds needed by the costs are discovered by combining in the research table
            {var kp=ResearchKnowledge.load(data);var pools=new StringBuilder();for(var a:Aspect.getPrimalAspects())pools.append(a.getTag()).append('=').append(kp.getAspectPoolFor("current",a)).append(' ');System.out.println("TC4_PROGRESSION cycle "+cycle+" pools "+pools+" valid "+tableMenu.stillValid(p)+" menu "+(p.containerMenu==tableMenu));}
            
            for(var a:Aspect.getCompoundAspects()){var kn=ResearchKnowledge.load(data);if(kn.hasDiscoveredAspect("current",a))continue;var parts=a.getComponents();
                if(kn.hasDiscoveredAspect("current",parts[0])&&kn.hasDiscoveredAspect("current",parts[1])&&kn.getAspectPoolFor("current",parts[0])>1&&kn.getAspectPoolFor("current",parts[1])>1)CombinePayload.handle(new CombinePayload(tableMenu.containerId,parts[0].getTag(),parts[1].getTag()),context(p));}
            if(scanned.size()==scansBefore&&ResearchKnowledge.load(data).getAspectsDiscovered("current").size()==knownBefore)break;}
        var known=ResearchKnowledge.load(data).getAspectsDiscovered("current");
        check(scanned.size()>=30,"thaumometer scans of "+scanned.size()+" everyday objects succeed once compounds are combined");
        known=ResearchKnowledge.load(data).getAspectsDiscovered("current");System.out.println("TC4_PROGRESSION known "+known.size()+" aspects");
        check(known.size()>12,"combining at the research table discovers compounds: "+known.size()+" aspects known");

        // notes requested from the Thaumonomicon cost paper and ink
        p.getInventory().add(new ItemStack(Items.PAPER,4));var ink=tableEntity.inventory.getItem(0).copy();p.getInventory().add(ink);
        p.containerMenu=new ThaumonomiconMenu(2,p.getInventory());
        ResearchRequestPayload.handle(new ResearchRequestPayload(2,key),context(p));
        ItemStack notes=ItemStack.EMPTY;for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);var n=NoteData.read(s);if(n!=null&&n.key.equals(key))notes=s;}
        check(!notes.isEmpty()&&count(p,Items.PAPER)==3,"the book writes "+key+" notes onto one paper");

        // solve the puzzle through the real placement packets
        tableEntity.inventory.setItem(1,notes.copy());notes.shrink(1);p.containerMenu=tableMenu;
        var note=NoteData.read(tableEntity.inventory.getItem(1));var usable=new HashSet<Aspect>();var kn=ResearchKnowledge.load(data);for(var a:kn.getAspectsDiscovered("current").getAspects())if(kn.getAspectPoolFor("current",a)>0)usable.add(a);
        var plan=solve(note,usable);check(plan!=null,"the "+key+" puzzle has a solution with the aspects the novice knows: "+(plan==null?"none":plan.size()+" placements"));
        for(var step:plan){var hex=(HexUtils.Hex)step[0];PlaceAspectPayload.handle(new PlaceAspectPayload(tableMenu.containerId,hex.q,hex.r,((Aspect)step[1]).getTag()),context(p));}
        var solved=NoteData.read(tableEntity.inventory.getItem(1));check(solved!=null&&solved.complete,"placing the planned aspects completes the "+key+" notes");

        // reading the discovery completes the research
        var discovery=tableEntity.inventory.getItem(1).copy();tableEntity.inventory.setItem(1,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,discovery);
        discovery.getItem().use(l,p,InteractionHand.MAIN_HAND);
        check(ResearchProgress.completed(data).contains(key),"reading the discovery completes "+key);
        heldWand=wand;System.out.println("TC4_PROGRESSION phase one reached "+key+" ("+research.category+")");
    }
}
