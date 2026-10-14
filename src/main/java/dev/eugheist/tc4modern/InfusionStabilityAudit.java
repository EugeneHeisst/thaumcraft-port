package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.*;
import thaumcraft.common.tiles.infusion.InfusionMatrixLogic;

/** Original 4.2.3.5 oracle: seeded Java Random odds, surroundings edge cases, all registered stabilisers and real boots craft. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class InfusionStabilityAudit {
    static final BlockPos POS=InfusionAudit.POS,SCAN=POS.east(40);
    static int ticks;static volatile boolean ready,finished;static volatile String failed;static long start;
    static final List<BlockPos> placed=new ArrayList<>(),ingredients=new ArrayList<>(),jars=new ArrayList<>();
    static void check(boolean ok,String message){if(!ok)throw new IllegalStateException("TC4_STABILITY FAIL "+message);System.out.println("TC4_STABILITY PASS "+message);}
    static void clear(net.minecraft.server.level.ServerLevel l){for(var at:placed)l.setBlock(at,Blocks.AIR.defaultBlockState(),2);placed.clear();}
    static void put(net.minecraft.server.level.ServerLevel l,BlockPos at,Block block){if(block instanceof CrystalClusterBlock||block instanceof ThaumicCandleBlock){l.setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);placed.add(at.below());}l.setBlock(at,block.defaultBlockState(),2);placed.add(at);}
    static void pedestal(net.minecraft.server.level.ServerLevel l,BlockPos at,boolean full){put(l,at,InfusionContent.PEDESTAL.get());if(full)((InfusionContent.PedestalEntity)l.getBlockEntity(at)).setItem(new ItemStack(Items.FEATHER));}
    static void symmetry(InfusionContent.MatrixEntity m,int expected,String message){m.logic.getSurroundings(m);check(m.logic.symmetry==expected,message+" = "+m.logic.symmetry);}
    static void surroundings(ServerPlayer p){
        var l=p.serverLevel();for(int x=-13;x<=13;x++)for(int z=-13;z<=13;z++)for(int y=-11;y<=6;y++)l.setBlock(SCAN.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
        l.setBlock(SCAN,InfusionContent.MATRIX.get().defaultBlockState(),2);var m=(InfusionContent.MatrixEntity)l.getBlockEntity(SCAN);
        symmetry(m,0,"empty surroundings");
        pedestal(l,SCAN.offset(3,-2,0),false);symmetry(m,2,"unpaired empty pedestal");((InfusionContent.PedestalEntity)l.getBlockEntity(SCAN.offset(3,-2,0))).setItem(new ItemStack(Items.FEATHER));symmetry(m,3,"unpaired occupied pedestal");
        pedestal(l,SCAN.offset(-3,-2,0),false);symmetry(m,1,"paired pedestals with one occupied");((InfusionContent.PedestalEntity)l.getBlockEntity(SCAN.offset(-3,-2,0))).setItem(new ItemStack(Items.COD));symmetry(m,0,"opposite occupied pedestals balance even with different items");clear(l);
        pedestal(l,SCAN.offset(3,-2,0),true);pedestal(l,SCAN.offset(-3,-3,0),true);symmetry(m,6,"pedestals on different heights are unpaired");clear(l);
        pedestal(l,SCAN.offset(3,-1,0),false);pedestal(l,SCAN.offset(3,-2,0),true);symmetry(m,2,"only highest pedestal per column");check(m.logic.pedestals.size()==1,"lower pedestal excluded");clear(l);
        pedestal(l,SCAN.offset(8,-10,8),true);pedestal(l,SCAN.offset(9,-2,0),true);pedestal(l,SCAN.offset(2,0,2),true);symmetry(m,3,"pedestal square radius8 and lower boundary10");check(m.logic.pedestals.size()==1,"outside or level-height pedestals excluded");clear(l);
        for(int y:new int[]{5,-10}){put(l,SCAN.offset(12,y,12),Blocks.SKELETON_SKULL);put(l,SCAN.offset(-12,y,-12),Blocks.SKELETON_SKULL);}symmetry(m,0,"four boundary stabilisers retain original fractional truncation");check(m.stabilises(SCAN.offset(12,5,12)),"upper corner skull accepted");clear(l);
        for(int x=2;x<=11;x++){put(l,SCAN.offset(x,-2,0),Blocks.SKELETON_SKULL);put(l,SCAN.offset(-x,-2,0),Blocks.SKELETON_SKULL);}symmetry(m,-2,"twenty paired skulls reduce stability by original two points");
        for(var at:List.copyOf(placed))l.setBlock(at,Blocks.AIR.defaultBlockState(),2);placed.clear();
        for(int x=2;x<=11;x++){put(l,SCAN.offset(x,6,0),Blocks.SKELETON_SKULL);put(l,SCAN.offset(-x,6,0),Blocks.SKELETON_SKULL);}symmetry(m,0,"above upper boundary excluded");clear(l);
        for(int x=2;x<=11;x++){put(l,SCAN.offset(x,-11,0),Blocks.SKELETON_SKULL);put(l,SCAN.offset(-x,-11,0),Blocks.SKELETON_SKULL);}symmetry(m,0,"below lower boundary excluded");clear(l);
        for(int y=-10;y<=5;y++)if(y!=0)put(l,SCAN.offset(0,y,0),Blocks.SKELETON_SKULL);symmetry(m,0,"matrix centre column never stabilises");clear(l);
        for(int y:new int[]{5,-10}){for(int x=2;x<=11;x++){put(l,SCAN.offset(x,y,12),Blocks.SKELETON_SKULL);put(l,SCAN.offset(-x,y,-12),Blocks.SKELETON_SKULL);}symmetry(m,-2,"inclusive horizontal12 /vertical boundary "+y);clear(l);}
        for(int z=2;z<=11;z++){put(l,SCAN.offset(13,-2,z),Blocks.SKELETON_SKULL);put(l,SCAN.offset(-13,-2,-z),Blocks.SKELETON_SKULL);}symmetry(m,0,"horizontal13 excluded");clear(l);
        var blocks=new ArrayList<Block>();for(var b:BuiltInRegistries.BLOCK)if(b instanceof AbstractSkullBlock||b instanceof CrystalClusterBlock||b instanceof OuterCrystal.CrystalBlock||b instanceof ThaumicCandleBlock)blocks.add(b);
        check(blocks.stream().filter(b->b instanceof ThaumicCandleBlock).count()==16&&blocks.stream().filter(b->b instanceof CrystalClusterBlock).count()==7&&blocks.stream().filter(b->b instanceof OuterCrystal.CrystalBlock).count()==1,"all16 candle colours, seven clusters and eldritch crystal registered");
        for(var b:blocks){for(int x=2;x<=11;x++){put(l,SCAN.offset(x,-2,0),b);put(l,SCAN.offset(-x,-2,0),b);}check(m.stabilises(SCAN.offset(2,-2,0)),"actual block recognised "+BuiltInRegistries.BLOCK.getKey(b));symmetry(m,-2,"same original stabilising strength "+BuiltInRegistries.BLOCK.getKey(b));clear(l);}
        for(var b:List.of(Blocks.TORCH,Blocks.LANTERN,Blocks.GLOWSTONE,Blocks.BOOKSHELF,Blocks.AMETHYST_CLUSTER,Blocks.CANDLE,EssentiaDevices.JAR.get())){put(l,SCAN.offset(3,-2,0),b);check(!m.stabilises(SCAN.offset(3,-2,0)),"non-original stabiliser rejected "+BuiltInRegistries.BLOCK.getKey(b));clear(l);}
        for(int x=2;x<=11;x++){put(l,SCAN.offset(x,-2,0),Blocks.SKELETON_SKULL);put(l,SCAN.offset(-x,-2,0),BuiltInRegistries.BLOCK.get(AlchemyContent.id("candle_0")));}symmetry(m,-2,"mixed skull and candle pairs also balance");m.logic.instability=7;m.logic.getSurroundings(m);check(m.logic.instability==7,"adding stabilisers rescans symmetry but does not erase already accumulated instability, as original");clear(l);
        System.out.println("TC4_STABILITY STABILISERS "+blocks.size());
    }
    /** Uses the real craftCycle; only source availability and effects are isolated from the player's fixture. */
    static final class Probe implements InfusionMatrixLogic.Host {
        final InfusionMatrixLogic logic=new InfusionMatrixLogic();final net.minecraft.world.level.Level world;
        final BlockPos location=POS.east(256);int drains;boolean available=true;
        Probe(ServerPlayer p){world=p.serverLevel();}
        public net.minecraft.world.level.Level level(){return world;}
        public BlockPos pos(){return location;}
        public boolean isPedestal(BlockPos at){return at.equals(pos().below(2));}
        public boolean isPillar(BlockPos at){return false;}
        public ItemStack pedestalItem(BlockPos at){return isPedestal(at)?new ItemStack(Items.LEATHER_BOOTS):ItemStack.EMPTY;}
        public void setPedestalItem(BlockPos at,ItemStack stack){}
        public boolean stabilises(BlockPos at){return false;}
        public boolean drainEssentia(Aspect a){drains++;return available;}
        public boolean researchComplete(net.minecraft.world.entity.player.Player p,String key){return true;}
        public InfusionMatrixLogic.InfusionRecipeMatch findRecipe(List<ItemStack> c,ItemStack in,net.minecraft.world.entity.player.Player p){return null;}
        public void sync(){}
        public void sound(String event,float volume,float pitch){}
        public void fxSource(BlockPos from,int id){}
        public void fxZap(double a,double b,double c,double d,double e,double f){}
        public void fxSparkle(BlockPos at){}
        public void fxFinish(BlockPos at){}
        public void fluxGoo(BlockPos at){}
        public void fluxGas(BlockPos at){}
        public void taint(net.minecraft.world.entity.LivingEntity e){}
        public void visExhaust(net.minecraft.world.entity.LivingEntity e){}
        public void warp(net.minecraft.world.entity.player.Player p,int n,boolean sticky){}
        public void playerCrafted(net.minecraft.world.entity.player.Player p,ItemStack result){}
    }
    static void odds(ServerPlayer p){
        var h=new Probe(p);var a=h.logic;a.recipeInput=new ItemStack(Items.LEATHER_BOOTS);a.recipeIngredients=new ArrayList<>();a.recipeInstability=1;
        for(int level:new int[]{-5,0,1,5,10,25}){int hits=0,expected=0;for(int seed=0;seed<6000;seed++){long value=seed+0x4a392bL;boolean event=level>0&&new Random(value).nextInt(500)<=level;if(event)expected++;
            a.instability=level;a.recipeEssentia=new AspectList().add(Aspect.AIR,1);h.drains=0;h.level().random.setSeed(value);a.craftCycle(h);boolean hit=h.drains==0;checkSilent(hit==event,"original <= probability boundary at level="+level+" seed="+seed);if(hit)hits++;}
            check(hits==expected,"6000 original-Java-Random seeds at instability="+level+" events="+hits);}
        h.available=false;
        for(int recipe:new int[]{1,3,8}){a.recipeInstability=recipe;int increments=0;for(int seed=0;seed<3000;seed++){long value=seed+0x38a77L;var reference=new Random(value);int expected=(reference.nextInt(100-recipe*3)==0?1:0)+(reference.nextInt(100-recipe*3)==0?1:0);a.instability=0;a.recipeEssentia=new AspectList().add(Aspect.AIR,1).add(Aspect.WATER,1);h.level().random.setSeed(value);a.craftCycle(h);checkSilent(a.instability==expected,"original missing-essentia escalation recipe="+recipe+" seed="+seed);increments+=expected;}
            check(increments>0,"3000 starvation seeds match original 100-3*recipe formula recipe="+recipe+" increases="+increments);}
        a.recipeInstability=1;a.recipeIngredients=List.of(new ItemStack(Items.FEATHER));
        for(int seed=0;seed<3000;seed++){long value=seed+0xa123L;var r=new Random(value);r.nextInt(1);r.nextInt(1);int expected=r.nextInt(48)==0?1:0;a.instability=0;a.recipeEssentia=new AspectList().add(Aspect.AIR,0);h.level().random.setSeed(value);a.craftCycle(h);checkSilent(a.instability==expected&&a.recipeEssentia.getAmount(Aspect.AIR)==1,"original missing ingredient restarts essentia and uses 50-2*recipe");}
        check(true,"3000 missing ingredient seeds match original essence replenishment/escalation");a.recipeIngredients=new ArrayList<>();
        long seed=0;while(true){var r=new Random(seed);if(r.nextInt(500)>25&&r.nextInt(97)==0)break;seed++;}a.instability=25;a.recipeEssentia=new AspectList().add(Aspect.AIR,1);h.level().random.setSeed(seed);a.craftCycle(h);check(a.instability==25,"starvation instability capped at original25");
    }
    static void checkSilent(boolean ok,String text){if(!ok)check(false,text);}
    static void consequences(ServerPlayer p){
        var l=p.serverLevel();var m=InfusionAudit.matrix;var target=net.minecraft.world.entity.EntityType.HUSK.create(l);
        m.taint(target);var poison=target.getEffect(TC4Modern.FLUX_TAINT);check(poison!=null&&poison.getDuration()==120&&poison.getAmplifier()==0&&!poison.isAmbient(),"original taint120ticks level0");
        m.visExhaust(target);var exhaust=target.getEffect(TC4Modern.VIS_EXHAUST);check(exhaust!=null&&exhaust.getDuration()==2400&&exhaust.getAmplifier()==0&&exhaust.isAmbient()&&exhaust.getCures().isEmpty(),"original vis exhaustion2400ticks level0 and no milk cure");
        var at=InfusionAudit.components.get(0).above();l.setBlock(at,Blocks.STONE.defaultBlockState(),3);m.fluxGoo(at);boolean goo=l.getBlockState(at).is(TC4Modern.FLUX_GOO.get());
        l.setBlock(at,Blocks.STONE.defaultBlockState(),3);m.fluxGas(at);boolean gas=l.getBlockState(at).is(TC4Modern.FLUX_GAS.get());l.setBlock(at,Blocks.AIR.defaultBlockState(),3);
        System.out.println("TC4_STABILITY OCCUPIED_FLUX goo="+goo+" gas="+gas);check(goo&&gas,"original instability flux replaces an occupied block above the pedestal");
    }
    static void boots(ServerPlayer p){
        InfusionAudit.prepare(p);InfusionAudit.matrix.logic.crafting=false;InfusionFxAudit.instability(p);consequences(p);
        var l=p.serverLevel();l.getEntitiesOfClass(Entity.class,new AABB(POS).inflate(20)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);PlayerWarp.save(p,new WarpState());
        for(var at:InfusionAudit.components)l.setBlock(at,Blocks.AIR.defaultBlockState(),3);for(int dx:new int[]{-1,1})l.setBlock(POS.offset(dx,-2,-4),Blocks.AIR.defaultBlockState(),3);
        var recipe=InfusionItemRecipes.all().stream().filter(r->r.key().equals("BootsTraveller")).findFirst().orElseThrow();check(recipe.instability()==1&&recipe.aspects().getAmount(Aspect.FLIGHT)==25&&recipe.aspects().getAmount(Aspect.TRAVEL)==25&&recipe.componentStacks().size()==6,"traveller boots recipe exactly original1 /25Volatus /25Iter /six components");
        var parts=recipe.componentStacks();int[][] offsets={{-3,0},{3,0},{0,-3},{0,3},{-3,-3},{3,3}};
        ((InfusionContent.PedestalEntity)l.getBlockEntity(POS.below(2))).setItem(new ItemStack(Items.LEATHER_BOOTS));
        for(int i=0;i<6;i++){var at=POS.offset(offsets[i][0],-2,offsets[i][1]);ingredients.add(at);l.setBlock(at,InfusionContent.PEDESTAL.get().defaultBlockState(),3);((InfusionContent.PedestalEntity)l.getBlockEntity(at)).setItem(parts.get(i));}
        int n=0;for(var aspect:recipe.aspects().getAspects()){var at=POS.offset(n++==0?-2:2,-2,-5);jars.add(at);l.setBlock(at,EssentiaDevices.JAR.get().defaultBlockState(),3);var jar=(WardedJarEntity)l.getBlockEntity(at);jar.state.aspect=aspect;jar.state.amount=25;jar.changed();}
        var m=InfusionAudit.matrix;m.logic.getSurroundings(m);check(m.logic.symmetry< -1,"paired original skull stabilisers outweigh boots base instability");
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.removeAllEffects();var wand=p.getInventory().items.stream().filter(WandData::isWand).findFirst().orElseThrow();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);InfusionAudit.click(p,wand);
        check(m.logic.crafting&&m.logic.recipeInstability==1&&m.logic.instability==m.logic.symmetry+1&&m.logic.instability<=0,"actual wand starts boots at symmetry+recipe, no hidden positive instability");start=l.getGameTime();ready=true;p.connection.teleport(POS.getX()+5,POS.getY()+2,POS.getZ()-7,35,24);
    }
    static void verify(ServerPlayer p){var m=InfusionAudit.matrix;if(m.logic.crafting){checkSilent(p.level().getGameTime()-start<1400,"native boots craft timed out");return;}var l=p.serverLevel();check(((InfusionContent.PedestalEntity)l.getBlockEntity(POS.below(2))).item().is(TravellerBoots.item()),"natural server ticks finish traveller boots");for(var at:ingredients)check(((InfusionContent.PedestalEntity)l.getBlockEntity(at)).item().isEmpty(),"component consumed "+at);for(var at:jars)check(((WardedJarEntity)l.getBlockEntity(at)).state.amount==0,"exact25essentia drained "+at);var drops=l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(POS).inflate(12));System.out.println("TC4_STABILITY FIXTURE_OTHER_DROPS "+drops.stream().map(e->e.getItem().toString()).toList());var recipe=InfusionItemRecipes.all().stream().filter(r->r.key().equals("BootsTraveller")).findFirst().orElseThrow();check(drops.stream().noneMatch(e->recipe.componentStacks().stream().anyMatch(ing->InfusionMatrixLogic.sameIngredient(e.getItem(),ing))),"stabilised recipe drops no ingredients");System.out.println("TC4_STABILITY ALL PASS");finished=true;}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){if(!Boolean.getBoolean("tc4modern.infusionStabilityAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();if(failed!=null)throw new IllegalStateException(failed);
        if(ticks==60)server.execute(()->{try{var p=server.getPlayerList().getPlayer(id);surroundings(p);odds(p);boots(p);}catch(Exception ex){failed=ex.toString();ex.printStackTrace();}});
        if(ready&&!finished&&ticks%20==0)server.execute(()->{try{verify(server.getPlayerList().getPlayer(id));}catch(Exception ex){failed=ex.toString();ex.printStackTrace();}});
        if(finished){LiquidDeathAudit.shot(mc,"infusion-stability-boots.png");mc.stop();}
    }
}
