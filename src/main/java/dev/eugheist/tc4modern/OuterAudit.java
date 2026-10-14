package dev.eugheist.tc4modern;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class OuterAudit {
    static int ticks;static volatile String failed;
    static void check(boolean value,String message){if(!value)failed=message;System.out.println("TC4_OUTER "+(value?"PASS ":"FAIL ")+message);}
    static void setup(net.minecraft.server.level.ServerPlayer player){
        var server=player.server;var level=server.getLevel(OuterDimension.KEY);check(level!=null,"Outer Lands dimension loads");
        check(!level.dimensionType().hasSkyLight()&&level.getMinBuildHeight()==0&&level.getMaxBuildHeight()==256,"original sky and world height");
        var data=new OuterMazeData();
        for(int seed=0;seed<40;seed++){int x=seed*100,z=-seed*100;int width=15+seed%8*2,height=15+(seed*3)%8*2;check(data.create(x,z,width,height,seed),"original maze "+seed+" generated");var portal=data.cell(x,z);check(portal!=null&&portal.feature==1,"portal maps to altar chunk "+seed);int size=data.size();check(!data.create(x+1,z+1,15,15,seed)&&data.size()==size,"overlap refused without changing saved cells "+seed);}
        var tag=data.save(new net.minecraft.nbt.CompoundTag(),level.registryAccess());var restored=OuterMazeData.load(tag,level.registryAccess());check(restored.size()==data.size()&&restored.cell(0,0).pack()==data.cell(0,0).pack(),"all original packed cells survive NBT roundtrip");
        var saved=OuterMazeData.get(level);check(saved==OuterMazeData.get(server.overworld()),"maze data belongs to this server save");if(saved.cell(2400,2400)==null)saved.create(2400,2400,15,15,2026);server.overworld().getDataStorage().save();check(saved.cell(2400,2400).feature==1,"server storage owns persistent portal cell");
        var base=new BlockPos(0,51,0);for(int x=-8;x<=8;x++)for(int z=-6;z<=10;z++)level.setBlock(base.offset(x,-1,z),OuterContent.NO_SPAWN.get().defaultBlockState(),3);
        var blocks=java.util.List.of(OuterContent.STONE,OuterContent.ROCK,OuterContent.CRUST,OuterContent.GLOWING,OuterContent.GLYPHED,OuterContent.PEDESTAL);
        for(int i=0;i<blocks.size();i++)for(int y=0;y<3;y++)level.setBlock(base.offset(-6+i*2,y,4),blocks.get(i).get().defaultBlockState(),3);
        for(int x=-6;x<=6;x++)level.setBlock(base.offset(x,-1,2),Blocks.GLOWSTONE.defaultBlockState(),3);
        level.setBlock(base.offset(-2,0,7),OuterContent.STAIRS.get().defaultBlockState(),3);level.setBlock(base.offset(0,0,7),OuterContent.SLAB.get().defaultBlockState(),3);
        var glyph=base.offset(2,0,4);var drops=net.minecraft.world.level.block.Block.getDrops(OuterContent.GLYPHED.get().defaultBlockState(),level,glyph,null);check(drops.size()==1&&drops.getFirst().is(BasicsContent.KNOWLEDGE_FRAGMENT.get()),"glyphed stone drops original knowledge fragment");
        var voidState=OuterContent.VOID.get().defaultBlockState();check(voidState.getShape(level,base).isEmpty()&&!voidState.getCollisionShape(level,base).isEmpty(),"void has no selection outline but original inset collision");
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.getInventory().clearContent();player.containerMenu.broadcastChanges();player.teleportTo(level,.5,53,-4,0,12);
        for(var group:java.util.List.of(OuterLoot.URNS,OuterLoot.CRATES))for(int rarity=0;rarity<3;rarity++){var loot=net.minecraft.world.level.block.Block.getDrops(group.get(rarity).get().defaultBlockState(),level,base,null);check(loot.size()>=1+rarity&&loot.size()<=3+rarity&&loot.stream().noneMatch(net.minecraft.world.item.ItemStack::isEmpty),"urn/crate original generated loot rarity "+rarity);level.setBlock(base.offset(-6+rarity*2,0,7+(group==OuterLoot.URNS?0:2)),group.get(rarity).get().defaultBlockState(),3);}
        for(var type:java.util.List.of(BrainyContent.NORMAL,BrainyContent.GIANT)){var zombie=type.get().create(level);var extra=new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();for(int i=0;i<100;i++)ZombieBrainDrops.drops(new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(zombie,level.damageSources().playerAttack(player),extra,true));check(extra.isEmpty(),"global zombie drop handler excludes "+type.getId());}

        var ventPos=base.offset(5,1,7);level.setBlock(ventPos,OuterDevices.VENT.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,net.minecraft.core.Direction.NORTH),3);var vent=(OuterDevices.Device)level.getBlockEntity(ventPos);for(int i=0;i<7;i++){vent.count=1;OuterDevices.Device.tick(level,ventPos,vent.getBlockState(),vent);}var crabs=level.getEntitiesOfClass(EldritchCrab.class,new net.minecraft.world.phys.AABB(ventPos).inflate(32));check(crabs.size()==6&&crabs.stream().noneMatch(EldritchCrab::hasHelm),"crab opening emits six unarmoured crabs then stops");crabs.forEach(net.minecraft.world.entity.Entity::discard);vent.count=150;
        player.getAbilities().flying=true;player.onUpdateAbilities();
    }
    static void trap(net.minecraft.server.level.ServerPlayer player){var level=player.serverLevel();var base=new BlockPos(0,51,0);
        var trapPos=base.offset(0,2,-3);level.setBlock(trapPos,OuterDevices.TRAP.get().defaultBlockState(),3);var trap=(OuterDevices.Device)level.getBlockEntity(trapPos);player.hasChangedDimension();player.invulnerableTime=0;player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setHealth(20);trap.count=0;OuterDevices.Device.tick(level,trapPos,trap.getBlockState(),trap);check(player.getHealth()==18,"runed trap deals original two magic damage: health="+player.getHealth());player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.setHealth(20);level.removeBlock(trapPos,false);
player.getAbilities().flying=true;player.onUpdateAbilities();}
    static void rooms(net.minecraft.server.level.ServerPlayer player){
        var level=player.server.getLevel(OuterDimension.KEY);
        for(int feature:new int[]{0,9,10,11,12,13,14,7,8}){
            int cx=120+feature*3,cz=120;var random=new java.util.Random(4000+feature);
            var cell=new thaumcraft.common.lib.world.dim.Cell();cell.feature=(byte)feature;cell.north=cell.south=cell.east=cell.west=true;
            for(int x=0;x<=16;x++)for(int z=0;z<=16;z++)for(int y=49;y<=64;y++)level.setBlock(new BlockPos(cx*16+x,y,cz*16+z),Blocks.AIR.defaultBlockState(),2);
            if(feature==7)OuterGenNestRoom.generateRoom(level,random,cx,cz,50,cell);
            else if(feature==8)OuterGenLibraryRoom.generateRoom(level,random,cx,cz,50,cell);
            else OuterGenPassage.generateDefaultPassage(level,random,cx,cz,50,cell);
            OuterGenCommon.processDecorations(level);
            int solid=0,vents=0,crystals=0,webs=0;
            for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=49;y<=64;y++){
                var state=level.getBlockState(new BlockPos(cx*16+x,y,cz*16+z));if(!state.isAir())solid++;
                if(state.is(OuterDevices.VENT.get()))vents++;if(state.is(OuterCrystal.BLOCK.get()))crystals++;if(state.is(Blocks.COBWEB))webs++;
            }
            check(solid>500,"room feature "+feature+" original masonry: "+solid+" blocks");
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){var entrance=new BlockPos(cx*16+8,55,cz*16+8).relative(direction,8);check(level.isEmptyBlock(entrance),"room "+feature+" connects "+direction);}
            if(feature==7)check(vents>0&&crystals>0,"nest contains crab openings and ancient crystals: "+vents+" / "+crystals);
            if(feature==8)check(level.getBlockState(new BlockPos(cx*16+8,53,cz*16+8)).is(OuterContent.GLYPHED.get()),"library centre contains knowledge fragment stone");
            if(feature==14){var pos=new BlockPos(cx*16+8,54,cz*16+8);check(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity && webs>0,"spider room contains spawner and webs");}
        }
        var crystalPos=new BlockPos((120+7*3)*16+8,58,120*16+8);
        var crystal=OuterCrystal.BLOCK.get().defaultBlockState();var loot=net.minecraft.world.level.block.Block.getDrops(crystal,level,crystalPos,null);
        check(loot.size()==1&&loot.getFirst().is(TC4Modern.BALANCED_SHARD.get()),"ancient crystal drops balanced shard");
        level.setBlock(crystalPos,crystal,3);check(level.getBlockState(crystalPos).getBlock() instanceof OuterCrystal.CrystalBlock,"ancient crystal stabilises infusion");
        player.teleportTo(level,(120+8*3)*16+8.5,53,120*16+5.5,0,0);player.getAbilities().flying=true;player.onUpdateAbilities();
    }
    static void terrain(net.minecraft.server.level.ServerPlayer player){
        var level=player.server.getLevel(OuterDimension.KEY);check(level.getChunkSource().getGenerator() instanceof OuterChunkGenerator,"dimension uses original maze terrain provider");
        int centre=8000+(int)((System.currentTimeMillis()/1000)%10000)*32;var data=OuterMazeData.get(level);check(data.create(centre,centre,15,15,20261011),"fresh maze published before chunk generation");
        int generated=0,keys=0,bosses=0;var types=new java.util.HashSet<Byte>();
        for(int x=centre-8;x<centre+7;x++)for(int z=centre-8;z<centre+7;z++){var cell=data.cell(x,z);if(cell==null)continue;level.getChunk(x,z);generated++;types.add(cell.feature);if(cell.feature==6)keys++;if(cell.feature>=2&&cell.feature<=5)bosses++;
            var floor=new BlockPos(x*16+8,51,z*16+8);check(!level.isEmptyBlock(floor),"generated floor at maze cell "+x+","+z+" feature "+cell.feature);
        }
        check(generated>50&&keys==1&&bosses==4,"maze terrain contains all mapped rooms: "+generated+" cells, one key, four boss quadrants; features "+types);
        var portal=new BlockPos(centre*16+8,53,centre*16+8);check(level.getBlockEntity(portal) instanceof OuterPortal.Portal,"terrain generation creates functional return portal");
        player.teleportTo(level,portal.getX()+.5,53,portal.getZ()-3.5,0,0);player.getAbilities().flying=true;player.onUpdateAbilities();
    }
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){if(!Boolean.getBoolean("tc4modern.outerAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();if(ticks==100)server.execute(()->{try{setup(server.getPlayerList().getPlayer(id));}catch(Throwable ex){failed=ex.toString();ex.printStackTrace();}});if(ticks==150)server.execute(()->{try{trap(server.getPlayerList().getPlayer(id));}catch(Throwable ex){failed=ex.toString();ex.printStackTrace();}});if(ticks==200)LiquidDeathAudit.shot(mc,"outer-masonry.png");if(ticks==220)server.execute(()->{try{rooms(server.getPlayerList().getPlayer(id));}catch(Throwable ex){failed=ex.toString();ex.printStackTrace();}});if(ticks==300)LiquidDeathAudit.shot(mc,"outer-library.png");if(ticks==320)server.execute(()->{try{terrain(server.getPlayerList().getPlayer(id));}catch(Throwable ex){failed=ex.toString();ex.printStackTrace();}});if(ticks==430)LiquidDeathAudit.shot(mc,"outer-maze-portal.png");if(ticks==460)server.execute(()->server.getPlayerList().getPlayer(id).teleportTo(server.overworld(),940,123,-95,0,0));if(ticks==480){System.out.println(failed==null?"TC4_OUTER ALL PASS":"TC4_OUTER FAILED "+failed);mc.stop();}}
}
