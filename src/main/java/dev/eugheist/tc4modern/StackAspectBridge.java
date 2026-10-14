package dev.eugheist.tc4modern;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.crafting.StackAspectRules;

/** Supplies modern item components and equipment properties to original stack tag rules. */
final class StackAspectBridge {
    static AspectList apply(ItemStack stack, AspectList base) {
        var tags=base==null ? new AspectList() : base.copy();
        var item=stack.getItem();
        if(item instanceof ManaBeanItem){var a=ManaBeanItem.aspect(stack);if(a!=null)tags.add(a,1);}
        if(item instanceof BasicWandItem) StackAspectRules.wand(tags,WandParts.rod(stack).craftCost(),WandParts.cap(stack).craftCost());
        if(item instanceof PotionItem || item instanceof TippedArrowItem) {
            // Original potion tags start with water rather than a generic magic fallback.
            if(item instanceof PotionItem) { tags=new AspectList().add(Aspect.WATER,1); }
            var contents=stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
            if((item instanceof SplashPotionItem || item instanceof LingeringPotionItem) && contents.hasEffects()) tags.merge(Aspect.ENTROPY,2);
            for(var effect:contents.getAllEffects()) {
                var key=effect.getEffect().unwrapKey().orElse(null);
                if(key!=null)StackAspectRules.effect(tags,key.location().toString(),effect.getAmplifier());
                if(key!=null)modernEffect(tags,key.location().getPath(),effect.getAmplifier()+1);
            }
        }
        if(item instanceof EssentiaItems.Phial||item instanceof WispEssenceItem||stack.is(LiquidDeath.CRYSTAL.get())) {
            var contents=EssentiaItems.getAspects(stack);
            if(contents!=null) tags.add(contents);
        }
        if(item instanceof ArmorItem armor) tags.merge(Aspect.ARMOR,armor.getDefense());
        else if(item instanceof SwordItem sword) tags.merge(Aspect.WEAPON,(int)(sword.getTier().getAttackDamageBonus()+1));
        else if(item instanceof BowItem || item instanceof CrossbowItem) tags.merge(Aspect.WEAPON,3).merge(Aspect.FLIGHT,1);
        else if(item instanceof PickaxeItem pick) tags.merge(Aspect.MINE,harvestLevel(pick.getTier())+1);
        else if(item instanceof HoeItem || item instanceof ShearsItem) {
            int durability=stack.getMaxDamage();
            tags.merge(Aspect.HARVEST,durability<=59 ? 1 : durability<=131 ? 2 : durability<=250 ? 3 : 4);
        } else if(item instanceof DiggerItem tool) tags.merge(Aspect.TOOL,harvestLevel(tool.getTier())+1);
        var enchantments=stack.getOrDefault(item instanceof EnchantedBookItem ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);
        int total=0;
        for(var entry:enchantments.entrySet()) {
            int level=entry.getIntValue(); total+=level;
            var key=entry.getKey().unwrapKey().orElse(null);
            if(key!=null)StackAspectRules.enchantment(tags,key.location().toString(),level);
            if(key!=null)modernEnchantment(tags,key.location().getPath(),level);
        }
        if(total>0)tags.merge(Aspect.MAGIC,total);
        return thaumcraft.common.lib.crafting.RecipeAspectMath.capAspects(tags,64);
    }
    private static int harvestLevel(Tier tier) {
        if(tier==Tiers.NETHERITE)return 4;
        if(tier==Tiers.DIAMOND)return 3;
        if(tier==Tiers.IRON)return 2;
        if(tier==Tiers.STONE)return 1;
        return 0;
    }
    private static void modernEffect(AspectList tags,String id,int n) {
        switch(id) {
            case "levitation", "slow_falling" -> tags.merge(Aspect.FLIGHT,n*3);
            case "glowing", "darkness" -> tags.merge(id.equals("glowing") ? Aspect.LIGHT : Aspect.DARKNESS,n*3);
            case "absorption", "health_boost" -> tags.merge(Aspect.HEAL,n*3);
            case "luck", "unluck" -> tags.merge(Aspect.GREED,n*3);
            case "wither" -> tags.merge(Aspect.DEATH,n*3);
            case "wind_charged" -> tags.merge(Aspect.AIR,n*3);
            case "weaving" -> tags.merge(Aspect.TRAP,n*3);
            case "oozing" -> tags.merge(Aspect.SLIME,n*3);
            case "infested" -> tags.merge(Aspect.BEAST,n*3);
            case "bad_omen", "raid_omen", "trial_omen" -> tags.merge(Aspect.ELDRITCH,n*3);
            case "dolphins_grace", "conduit_power" -> tags.merge(Aspect.WATER,n*3);
        }
    }
    private static void modernEnchantment(AspectList tags,String id,int n) {
        switch(id) {
            case "mending" -> tags.merge(Aspect.TOOL,n);
            case "frost_walker" -> tags.merge(Aspect.COLD,n);
            case "depth_strider", "impaling" -> tags.merge(Aspect.WATER,n);
            case "soul_speed" -> tags.merge(Aspect.SOUL,n).merge(Aspect.MOTION,n);
            case "swift_sneak" -> tags.merge(Aspect.MOTION,n);
            case "binding_curse" -> tags.merge(Aspect.TRAP,n);
            case "vanishing_curse" -> tags.merge(Aspect.ENTROPY,n);
            case "sweeping_edge", "piercing", "density", "breach" -> tags.merge(Aspect.WEAPON,n);
            case "loyalty", "multishot" -> tags.merge(Aspect.FLIGHT,n);
            case "riptide", "wind_burst" -> tags.merge(Aspect.AIR,n);
            case "channeling" -> tags.merge(Aspect.ENERGY,n);
            case "quick_charge" -> tags.merge(Aspect.MOTION,n);
        }
    }
    static String scanKey(ItemStack stack,String id) {
        if(stack.getItem() instanceof PotionItem || stack.getItem() instanceof TippedArrowItem) {
            var contents=stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
            String potion=contents.potion().flatMap(holder->holder.unwrapKey()).map(key->key.location().toString()).orElse("empty");
            return id+"#"+potion;
        }
        return id;
    }
}
