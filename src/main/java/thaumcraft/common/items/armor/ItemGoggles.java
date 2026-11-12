package thaumcraft.common.items.armor;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.*;
import thaumcraft.api.nodes.IRevealer;
import thaumcraft.api.aspects.Aspect;
/** Original ItemGoggles behavior adapted to modern armor components and hooks. */
public final class ItemGoggles extends ArmorItem implements IVisDiscountGear,IRevealer,IGoggles,IRunicArmor {
    public static final int DURABILITY=350,DISCOUNT=5,ENCHANTMENT_VALUE=25,HEAD_DEFENSE=1;
    public ItemGoggles(Holder<ArmorMaterial> material){super(material,Type.HELMET,new Item.Properties().durability(DURABILITY).rarity(Rarity.RARE));}
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> lines,net.minecraft.world.item.TooltipFlag flag){
        super.appendHoverText(stack,context,lines,flag);
        lines.add(Component.translatable("tc.visdiscount").append(": "+DISCOUNT+"%").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
    }
    @Override public ResourceLocation getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,ArmorMaterial.Layer layer,boolean inner){return ResourceLocation.fromNamespaceAndPath("tc4modern","textures/models/goggles.png");}
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack material){return material.is(Items.GOLD_INGOT) || super.isValidRepairItem(stack,material);}
    @Override public int getVisDiscount(ItemStack stack,Player player,Aspect aspect){return DISCOUNT;}
    @Override public boolean showNodes(ItemStack stack,LivingEntity player){return true;}
    @Override public boolean showIngamePopups(ItemStack stack,LivingEntity player){return true;}
    @Override public int getRunicCharge(ItemStack stack){return 0;}
}
