package thaumcraft.common.items.armor;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.*;
import thaumcraft.api.aspects.Aspect;
/** Original robe armor properties, colors, repair and washing adapted to modern components. */
public final class ItemRobeArmor extends ArmorItem implements IVisDiscountGear,IRunicArmor {
    public static final int DEFAULT_COLOR=6961280,ENCHANTMENT_VALUE=25;
    public ItemRobeArmor(Holder<ArmorMaterial> material,Type type){
        super(material,type,new Item.Properties().durability(type.getDurability(25)).rarity(Rarity.UNCOMMON).component(DataComponents.DYED_COLOR,new DyedItemColor(DEFAULT_COLOR,false)));
    }
    public static int color(ItemStack stack){return DyedItemColor.getOrDefault(stack,DEFAULT_COLOR);}
    public static void setColor(ItemStack stack,int color){stack.set(DataComponents.DYED_COLOR,new DyedItemColor(color,true));}
    public static void clearColor(ItemStack stack){stack.set(DataComponents.DYED_COLOR,new DyedItemColor(DEFAULT_COLOR,false));}
    @Override public ResourceLocation getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,ArmorMaterial.Layer layer,boolean inner){
        return ResourceLocation.fromNamespaceAndPath("tc4modern","textures/models/robes_"+(slot==EquipmentSlot.LEGS?"2":"1")+(layer.dyeable()?"":"_overlay")+".png");
    }
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack material){return material.is(dev.eugheist.tc4modern.TC4Modern.ENCHANTED_FABRIC.get())||super.isValidRepairItem(stack,material);}
    @Override public int getVisDiscount(ItemStack stack,Player player,Aspect aspect){return getType()==Type.BOOTS?1:2;}
    @Override public int getRunicCharge(ItemStack stack){return 0;}
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,context,lines,flag);
        lines.add(Component.translatable("tc.visdiscount").append(": "+getVisDiscount(stack,null,null)+"%").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        var world=context.getLevel();var pos=context.getClickedPos();var state=world.getBlockState(pos);
        if(state.is(Blocks.WATER_CAULDRON)){
            if(!world.isClientSide){clearColor(stack);LayeredCauldronBlock.lowerFillLevel(state,world,pos);}
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
