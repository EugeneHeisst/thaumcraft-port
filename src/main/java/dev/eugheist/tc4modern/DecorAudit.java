package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Isolated native rendering check for the original decorative block variants. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class DecorAudit {
    static int ticks;static volatile boolean ready;static volatile String failure;
    static final BlockPos BASE=new BlockPos(1600,250,1600);
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("tc4modern.decorAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        if(ticks==40&&!ready&&failure==null)return;
        ticks++;var server=mc.getSingleplayerServer();
        if(ticks==40)server.execute(()->{try {
            var player=server.getPlayerList().getPlayer(mc.player.getUUID());var level=player.serverLevel();level.setDayTime(6000);
            for(int x=-7;x<=7;x++)for(int z=-4;z<=7;z++)level.setBlock(BASE.offset(x,-1,z),Blocks.STONE_BRICKS.defaultBlockState(),3);
            for(int i=0;i<3;i++)for(int y=0;y<2;y++)for(int x=0;x<2;x++)level.setBlock(BASE.offset(-4+i*3+x,y,3),BuiltInRegistries.BLOCK.get(AlchemyContent.id(new String[]{"amber_block","amber_bricks","obsidian_tile"}[i])).defaultBlockState(),3);
            TotemAudit.setup(player,BASE);HilltopAudit.setup(player,BASE);MoundAudit.setup(player,BASE);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.getInventory().clearContent();
            for(int i=0;i<4;i++) {
                var block=new net.minecraft.world.level.block.Block[]{MagicalWoodShapes.GREAT_STAIRS.get(),MagicalWoodShapes.SILVER_STAIRS.get(),MagicalWoodShapes.GREAT_SLAB.get(),MagicalWoodShapes.SILVER_SLAB.get()}[i];
                level.setBlock(BASE.offset(-4+i*3,0,0),block.defaultBlockState(),3);
            }
            for(var slab:java.util.List.of(MagicalWoodShapes.GREAT_SLAB.get(),MagicalWoodShapes.SILVER_SLAB.get())) {
                var pos=BASE.offset(slab==MagicalWoodShapes.GREAT_SLAB.get()?-3:0,0,0);
                level.setBlock(pos,slab.defaultBlockState(),3);player.getInventory().selected=0;player.getInventory().setItem(0,new ItemStack(slab));
                var hit=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5),Direction.UP,pos,false);
                player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
                var state=level.getBlockState(pos);
                if(!state.is(slab)||state.getValue(net.minecraft.world.level.block.SlabBlock.TYPE)!=net.minecraft.world.level.block.state.properties.SlabType.DOUBLE)throw new IllegalStateException("Actual wooden slabs do not merge");
                var drops=net.minecraft.world.level.block.Block.getDrops(state,level,pos,null);
                if(drops.size()!=1||!drops.getFirst().is(slab.asItem())||drops.getFirst().getCount()!=2)throw new IllegalStateException("Double wooden slab must drop two slabs");
            }
            System.out.println("TC4_WOOD_PLACEMENT ALL PASS actual slab item placement merges both woods and double slabs drop two");
            player.getInventory().setItem(0,new ItemStack(MagicalWoodShapes.GREAT_STAIRS.get()));player.getInventory().setItem(1,new ItemStack(ArcaneLamps.AMBER.get()));player.getInventory().setItem(2,new ItemStack(CrimsonContent.OBSIDIAN_TILE.get()));player.teleportTo(level,1600.5,252,1595.5,0,17);ready=true;
        }catch(Throwable ex){failure=ex.toString();ex.printStackTrace();}});
        if(ticks==85)mc.player.getInventory().selected=0;
        if(ticks==100) {
            if(!ready||failure!=null)throw new IllegalStateException("Decor setup failed: "+failure);
            for(String name:new String[]{"amber_block","amber_bricks","obsidian_tile"}) {
                var state=BuiltInRegistries.BLOCK.get(AlchemyContent.id(name)).defaultBlockState();var model=mc.getBlockRenderer().getBlockModel(state);
                for(var face:Direction.values()) {
                    var quads=model.getQuads(state,face,net.minecraft.util.RandomSource.create());
                    String wanted=name.equals("amber_block")?(face.getAxis()==Direction.Axis.Y?"amberblock_top":"amberblock"):name.equals("amber_bricks")?"amberbrick":"obsidiantile";
                    if(quads.isEmpty()||quads.stream().anyMatch(q->!q.getSprite().contents().name().equals(AlchemyContent.id("blocks/"+wanted))))throw new IllegalStateException("Wrong original face texture: "+name+" "+face+" expected "+wanted);
                }
            }
            for(var block:java.util.List.of(MagicalWoodShapes.GREAT_STAIRS.get(),MagicalWoodShapes.SILVER_STAIRS.get(),MagicalWoodShapes.GREAT_SLAB.get(),MagicalWoodShapes.SILVER_SLAB.get())) {
                for(var state:block.getStateDefinition().getPossibleStates()) {
                    var model=mc.getBlockRenderer().getBlockModel(state);
                    for(var direction:Direction.values())if(model.getQuads(state,direction,net.minecraft.util.RandomSource.create()).stream().anyMatch(q->q.getSprite().contents().name().getPath().equals("missingno")))throw new IllegalStateException("Missing wooden shape model: "+state);
                    if(model==mc.getModelManager().getMissingModel())throw new IllegalStateException("Missing wooden variant: "+state);
                }
            }
            TotemAudit.render(mc,BASE);
            LiquidDeathAudit.shot(mc,"decorative-obsidian-totems.png");System.out.println("TC4_DECOR_RENDER ALL PASS original six faces of amber, amber bricks and obsidian tiles");
        }
        if(ticks==110)server.execute(()->server.getPlayerList().getPlayer(mc.player.getUUID()).setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(TC4Modern.GOGGLES.get())));
        if(ticks==135)LiquidDeathAudit.shot(mc,"decorative-totem-revealed.png");
        if(ticks==150)server.execute(()->{var player=server.getPlayerList().getPlayer(mc.player.getUUID());var pos=HilltopAudit.gallery(BASE);player.teleportTo(player.serverLevel(),pos.getX()+.5,pos.getY()+6,pos.getZ()-10,0,25);});
        if(ticks==180)LiquidDeathAudit.shot(mc,"hilltop-stones.png");
        if(ticks==190)server.execute(()->{var player=server.getPlayerList().getPlayer(mc.player.getUUID());var pos=MoundAudit.gallery(BASE);player.teleportTo(player.serverLevel(),pos.getX()+9.5,pos.getY()+24,pos.getZ()-12,0,32);});
        if(ticks==220)LiquidDeathAudit.shot(mc,"mound-exterior.png");
        if(ticks==230)server.execute(()->{var player=server.getPlayerList().getPlayer(mc.player.getUUID());var pos=MoundAudit.gallery(BASE);player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,400,0,false,false));player.teleportTo(player.serverLevel(),pos.getX()+7.5,pos.getY()+1,pos.getZ()+9.5,-90,12);});
        if(ticks==260)LiquidDeathAudit.shot(mc,"mound-treasure-room.png");
        if(ticks==270)mc.stop();
    }
}
