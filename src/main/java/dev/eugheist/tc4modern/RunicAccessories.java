package dev.eugheist.tc4modern;

import baubles.api.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.IRunicArmor;

/** ItemRingRunic, ItemAmuletRunic and ItemGirdleRunic, split metadata into registry ids. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class RunicAccessories {
    public enum Kind {
        RING("runic_ring",BaubleType.RING,5,0), CHARGED("runic_ring_charged",BaubleType.RING,4,1),
        HEALING("runic_ring_regen",BaubleType.RING,4,3), AMULET("runic_amulet",BaubleType.AMULET,8,0),
        EMERGENCY("runic_amulet_emergency",BaubleType.AMULET,7,4), GIRDLE("runic_girdle",BaubleType.BELT,10,0),
        KINETIC("runic_girdle_kinetic",BaubleType.BELT,9,2);
        final String id;final BaubleType slot;final int capacity,upgrade;
        Kind(String id,BaubleType slot,int capacity,int upgrade){this.id=id;this.slot=slot;this.capacity=capacity;this.upgrade=upgrade;}
        Item item(){return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id(id));}
    }
    @SubscribeEvent static void register(RegisterEvent event){event.register(Registries.ITEM,h->{for(var k:Kind.values())h.register(AlchemyContent.id(k.id),new RunicItem(k));});}
    public static final class RunicItem extends Item implements IBauble,IRunicArmor {
        final Kind kind;
        RunicItem(Kind kind){super(new Properties().stacksTo(1).rarity(Rarity.RARE));this.kind=kind;}
        @Override public BaubleType getBaubleType(ItemStack s){return kind.slot;}
        @Override public int getRunicCharge(ItemStack s){return kind.capacity;}
        @Override public void onEquipped(ItemStack s,LivingEntity p){RunicRingShield.dirty=true;}
        @Override public void onUnequipped(ItemStack s,LivingEntity p){RunicRingShield.dirty=true;}
    }
    static int charge(ItemStack stack){return stack.getItem() instanceof IRunicArmor armor?armor.getRunicCharge(stack)+NoteData.tag(stack).getByte("RS.HARDEN"):0;}
}
