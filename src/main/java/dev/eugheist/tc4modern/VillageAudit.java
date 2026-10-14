package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Actual entity persistence, native merchant interaction/payment and original delayed replenishment. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class VillageAudit {
    static int ticks;static volatile boolean ready,paid,restocked;static ThaumcraftVillager banker,wizard;static int price;
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_VILLAGE FAIL "+text);System.out.println("TC4_VILLAGE PASS "+text);}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.villageAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;
        if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
        if(ticks==50)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);p.closeContainer();p.removeAllEffects();p.setHealth(p.getMaxHealth());p.setInvulnerable(true);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();
            var base=new BlockPos(48,180,48);for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)p.serverLevel().setBlock(base.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            p.connection.teleport(48.5,180,53.5,180,0);
            banker=VillageContent.BANKER.get().create(p.serverLevel());wizard=VillageContent.WIZARD.get().create(p.serverLevel());
            banker.moveTo(47,180,48,0,0);wizard.moveTo(50,180,48,0,0);banker.setNoAi(true);wizard.setNoAi(true);p.serverLevel().addFreshEntity(banker);p.serverLevel().addFreshEntity(wizard);
            check(wizard.getOffers().size()==1&&banker.getOffers().size()==1,"each original profession begins with exactly one random offer");
            check(ScanTarget.entity(wizard).knowledgeKey().equals("entity:minecraft:villager")&&ScanTarget.entity(wizard).aspects().equals(ScanTarget.entity(net.minecraft.world.entity.EntityType.VILLAGER.create(p.serverLevel())).aspects()),"both professions share original vanilla villager scan identity and aspects");
            var candidates=VillageTrades.candidates(false,net.minecraft.util.RandomSource.create(5));var offers=new MerchantOffers();var arrow=VillageTrades.trade(VillageTrades.mod("coin",2),new ItemStack(Items.ARROW));for(int i=0;i<7;i++)arrow.increaseUses();offers.add(arrow);offers.add(candidates.getLast());banker.setOffers(offers);price=offers.getLast().getBaseCostA().getCount();
            var saved=new net.minecraft.nbt.CompoundTag();banker.saveWithoutId(saved);var copy=VillageContent.BANKER.get().create(p.serverLevel());copy.load(saved);check(copy.getOffers().size()==2&&copy.getOffers().getFirst().isOutOfStock()&&copy.getVillagerData().getProfession()==VillageContent.BANKER_PROFESSION.get(),"actual entity NBT restores profession, offers, prices and exhausted uses");copy.discard();
            p.getInventory().setItem(9,VillageTrades.mod("coin",64));p.inventoryMenu.broadcastChanges();ready=true;
        });
        if(ticks==100){check(ready,"server fixture ready");for(var e:mc.level.entitiesForRendering())if(e instanceof ThaumcraftVillager){e.setYRot(0);((ThaumcraftVillager)e).yBodyRot=0;((ThaumcraftVillager)e).yHeadRot=0;}LiquidDeathAudit.shot(mc,"village-professions.png");check(VillageClient.rendered>0,"original wizard and moneychanger skins rendered");server.execute(()->banker.mobInteract(server.getPlayerList().getPlayer(uuid),net.minecraft.world.InteractionHand.MAIN_HAND));}
        if(ticks==140){check(mc.player.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu,"actual villager interaction opened merchant container");check(mc.screen.mouseClicked((mc.getWindow().getGuiScaledWidth()-276)/2.0+40,(mc.getWindow().getGuiScaledHeight()-166)/2.0+48,0),"actual merchant screen selects its second offer");}
        if(ticks==165){var menu=(net.minecraft.world.inventory.MerchantMenu)mc.player.containerMenu;check(menu.getSlot(2).getItem().is(Items.SADDLE),"real selection packet prepares original saddle trade");LiquidDeathAudit.shot(mc,"village-banker-trade.png");mc.gameMode.handleInventoryMouseClick(menu.containerId,2,0,net.minecraft.world.inventory.ClickType.PICKUP,mc.player);}
        if(ticks==195){check(mc.player.containerMenu.getCarried().is(Items.SADDLE),"native merchant result click receives one saddle");server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);int coins=p.getInventory().items.stream().filter(s->s.is(PechTrades.mod("coin").getItem())).mapToInt(ItemStack::getCount).sum()+p.containerMenu.getSlot(0).getItem().getCount();check(coins==64-price,"actual trade consumes exact original coin price "+price);check(banker.tradeTimer==40&&banker.getOffers().getLast().getUses()==1,"last-offer trade starts original forty-tick timer while merchant remains open");paid=true;});}
        if(ticks==210){check(paid,"server payment check complete");mc.player.closeContainer();}
        if(ticks==285)server.execute(()->{check(banker.tradeTimer==0&&banker.getOffers().getFirst().getMaxUses()>7&&!banker.getOffers().getFirst().isOutOfStock()&&banker.getOffers().getFirst().getUses()==7,"forty idle ticks replenish exhausted offers by increasing their seven-use limit");check(banker.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"original completion grants regeneration for two hundred ticks");var p=server.getPlayerList().getPlayer(uuid);p.setInvulnerable(false);banker.discard();wizard.discard();restocked=true;});
        if(ticks==310){check(restocked,"delayed merchant checks complete");System.out.println("TC4_VILLAGE ALL PASS original professions, scans, NBT, native coin trade and delayed replenishment");mc.stop();}
    }
}
