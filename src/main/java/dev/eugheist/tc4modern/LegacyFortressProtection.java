/** This software is provided under the terms of the Minecraft Forge Public License v1.0. */
package dev.eugheist.tc4modern;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.*;
/** Forge 1.7.10 ISpecialArmor.ArmorProperties ordering, caps and normalization.
 * Adapted from MinecraftForge 1.7.10 (Minecraft Forge Public License v1.0), src/main/java/net/minecraftforge/common/ISpecialArmor.java.
 */
final class LegacyFortressProtection {
 static final class Property implements Comparable<Property> {
  int Priority,AbsorbMax;double AbsorbRatio;ItemStack stack;EquipmentSlot slot;
  Property(int priority,double ratio,int max,ItemStack stack,EquipmentSlot slot){Priority=priority;AbsorbRatio=ratio;AbsorbMax=max;this.stack=stack;this.slot=slot;}
  public int compareTo(Property other){if(other.Priority!=Priority)return other.Priority-Priority;double left=AbsorbRatio==0?0:AbsorbMax*100d/AbsorbRatio,right=other.AbsorbRatio==0?0:other.AbsorbMax*100d/other.AbsorbRatio;return (int)(left-right);}
 }
 static float apply(LivingEntity entity,DamageSource source,float amount,List<Property> properties){
  if(properties.isEmpty()||amount<=0)return amount;double damage=amount*25d;var props=properties.toArray(Property[]::new);StandardizeList(props,damage);int level=props[0].Priority;double ratio=0;
  for(var prop:props){if(level!=prop.Priority){damage-=damage*ratio;ratio=0;level=prop.Priority;}ratio+=prop.AbsorbRatio;double absorb=damage*prop.AbsorbRatio;if(absorb>0&&(!(prop.stack.getItem() instanceof FortressArmor.Piece)||!source.is(DamageTypes.FALL)))prop.stack.hurtAndBreak(Math.max(1,(int)(absorb/25d)),entity,prop.slot);}
  return (float)((damage-damage*ratio)/25d);
 }
        private static void StandardizeList(Property[] armor, double damage)
        {
            Arrays.sort(armor);

            int     start     = 0;
            double  total     = 0;
            int     priority  = armor[0].Priority;
            int     pStart    = 0;
            boolean pChange   = false;
            boolean pFinished = false;



            for (int x = 0; x < armor.length; x++)
            {
                total += armor[x].AbsorbRatio;
                if (x == armor.length - 1 || armor[x].Priority != priority)
                {
                    if (armor[x].Priority != priority)
                    {
                        total -= armor[x].AbsorbRatio;
                        x--;
                        pChange = true;
                    }
                    if (total > 1)
                    {
                        for (int y = start; y <= x; y++)
                        {
                            double newRatio = armor[y].AbsorbRatio / total;
                            if (newRatio * damage > armor[y].AbsorbMax)
                            {
                                armor[y].AbsorbRatio = (double)armor[y].AbsorbMax / damage;
                                total = 0;
                                for (int z = pStart; z <= y; z++)
                                {
                                    total += armor[z].AbsorbRatio;
                                }
                                start = y + 1;
                                x = y;
                                break;
                            }
                            else
                            {
                                armor[y].AbsorbRatio = newRatio;
                                pFinished = true;
                            }
                        }
                        if (pChange && pFinished)
                        {
                            damage -= (damage * total);
                            total = 0;
                            start = x + 1;
                            priority = armor[start].Priority;
                            pStart = start;
                            pChange = false;
                            pFinished = false;
                            if (damage <= 0)
                            {
                                for (int y = x + 1; y < armor.length; y++)
                                {
                                    armor[y].AbsorbRatio = 0;
                                }
                                break;
                            }
                        }
                    }
                    else
                    {
                        for (int y = start; y <= x; y++)
                        {
                            total -= armor[y].AbsorbRatio;
                            if (damage * armor[y].AbsorbRatio > armor[y].AbsorbMax)
                            {
                                armor[y].AbsorbRatio = (double)armor[y].AbsorbMax / damage;
                            }
                            total += armor[y].AbsorbRatio;
                        }
                        damage -= (damage * total);
                        total = 0;
                        if (x != armor.length - 1)
                        {
                            start = x + 1;
                            priority = armor[start].Priority;
                            pStart = start;
                            pChange = false;
                            if (damage <= 0)
                            {
                                for (int y = x + 1; y < armor.length; y++)
                                {
                                    armor[y].AbsorbRatio = 0;
                                }
                                break;
                            }
                        }
                    }
                }
            }

        }

}
