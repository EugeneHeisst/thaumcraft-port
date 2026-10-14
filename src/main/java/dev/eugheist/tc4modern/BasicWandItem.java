package dev.eugheist.tc4modern;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;

/** First platform trigger adapter for original WandManager.createThaumonomicon; no vis cost in TC4. */
public class BasicWandItem extends Item {
    public final int capacity;
    public final float capModifier;
    public BasicWandItem(Properties properties) { this(properties,2500,1.1f); }
    public BasicWandItem(Properties properties,int capacity,float capModifier) { super(properties);this.capacity=capacity;this.capModifier=capModifier; }
    /** ItemWandCasting.addInformation: capacity line, compact coloured vis amounts or per-aspect cost with shift. */
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> tooltip,net.minecraft.world.item.TooltipFlag flag) {
        var fmt=new java.text.DecimalFormat("#######.##",java.text.DecimalFormatSymbols.getInstance(java.util.Locale.ROOT));
        var player=net.neoforged.fml.loading.FMLEnvironment.dist.isClient()?ClientSide.player():null;
        boolean shift=net.neoforged.fml.loading.FMLEnvironment.dist.isClient()&&ClientSide.shift();
        var lang=net.minecraft.locale.Language.getInstance();
        var focusItem=FociContent.getFocusItem(stack);
        StringBuilder tt=new StringBuilder();int tot=0,num=0;java.util.List<String> lines=new java.util.ArrayList<>();
        var tag=NoteData.tag(stack);
        for(var aspect:thaumcraft.api.aspects.Aspect.getPrimalAspects()){
            if(!tag.contains(aspect.getTag()))continue;
            // Fixed precision prevents the live tooltip from changing width every whole/tenth vis.
            String amount=String.format(java.util.Locale.ROOT,"%.2f",WandData.get(stack,aspect)/100f);
            float mod=WandData.craftingModifier(stack,player,aspect);
            num++;tot=(int)(tot+mod*100f);
            String text="";
            if(focusItem.getItem() instanceof FociContent.Focus f){int amt=f.getVisCost(focusItem).getAmount(aspect);
                if(amt>0)text="\u00a7r, "+fmt.format(amt*mod/100f)+" "+lang.getOrDefault(f.isVisCostPerTick(focusItem)?"item.Focus.cost2":"item.Focus.cost1");}
            if(shift){lines.add(" \u00a7"+aspect.getChatcolor()+aspect.getName()+"\u00a7r x "+amount+", \u00a7o("+fmt.format(mod*100f)+"% "+lang.getOrDefault("tc.vis.cost")+")"+text);continue;}
            if(tt.length()>0)tt.append(" | ");
            tt.append("\u00a7").append(aspect.getChatcolor()).append(amount).append("\u00a7r");
        }
        String tt2="";
        if(!shift&&num>0){lines.add(tt.toString());tt2=" ("+(tot/num)+"% "+lang.getOrDefault("tc.vis.costavg")+")";}
        tooltip.add(net.minecraft.network.chat.Component.literal("\u00a76"+lang.getOrDefault("item.capacity.text")+" "+WandData.capacity(stack)/100+"\u00a7r"+tt2));
        for(String l:lines)tooltip.add(net.minecraft.network.chat.Component.literal(l));
        if(!focusItem.isEmpty())tooltip.add(net.minecraft.network.chat.Component.empty().append(focusItem.getHoverName()).withStyle(net.minecraft.ChatFormatting.BOLD,net.minecraft.ChatFormatting.ITALIC,net.minecraft.ChatFormatting.GREEN));
    }
    static final class ClientSide{
        static net.minecraft.world.entity.player.Player player(){return net.minecraft.client.Minecraft.getInstance().player;}
        static boolean shift(){return net.minecraft.client.gui.screens.Screen.hasShiftDown();}
    }

    @Override public int getUseDuration(ItemStack stack,net.minecraft.world.entity.LivingEntity entity){return 72000;}
    @Override public net.minecraft.network.chat.Component getName(ItemStack stack){return this==WandParts.WAND.get()?WandParts.name(stack):super.getName(stack);}
    /** WandRodPrimalOnUpdate: elemental rods trickle 1 vis every 200 ticks while below a tenth of capacity. */
    @Override public void inventoryTick(ItemStack stack,net.minecraft.world.level.Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected){
        if(level.isClientSide||!(entity instanceof net.minecraft.world.entity.player.Player player))return;
        if(WandParts.rod(stack).runes() && player.tickCount%50==0) {
            var primals=new java.util.ArrayList<thaumcraft.api.aspects.Aspect>();
            for(var a:thaumcraft.api.aspects.Aspect.getPrimalAspects())if(WandData.get(stack,a)<WandData.capacity(stack)/10)primals.add(a);
            if(!primals.isEmpty())WandData.add(stack,primals.get(level.random.nextInt(primals.size())),1,true);
        }
        var regen=WandParts.rod(stack).regen();
        if(regen!=null&&player.tickCount%200==0&&WandData.get(stack,regen)<WandData.capacity(stack)/10)WandData.add(stack,regen,1,true);
    }
    /** 1.7 ItemRenderer only re-equipped on a different item/damage; vis and focus NBT changes kept the wand raised. */
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack,ItemStack newStack,boolean slotChanged){return slotChanged||!oldStack.is(newStack.getItem());}
    /** ItemWandCasting.getItemUseAction is EnumAction.bow. */
    @Override public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack){return net.minecraft.world.item.UseAnim.BOW;}
    @Override public void releaseUsing(ItemStack stack,net.minecraft.world.level.Level level,net.minecraft.world.entity.LivingEntity entity,int remaining){FociContent.release(stack,level,entity,remaining);}
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        var hit=getPlayerPOVHitResult(level,player,net.minecraft.world.level.ClipContext.Fluid.NONE);
        if(level.getBlockEntity(hit.getBlockPos()) instanceof AuraNodeEntity){player.startUsingItem(hand);return net.minecraft.world.InteractionResultHolder.consume(player.getItemInHand(hand));}
        var focus=FociContent.wandUse(level,player,hand);
        if(focus!=null)return focus;
        return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
    }
    @Override public void onUseTick(net.minecraft.world.level.Level level,net.minecraft.world.entity.LivingEntity entity,ItemStack stack,int remaining){
        if(!(entity instanceof net.minecraft.world.entity.player.Player player))return;
        if(FociContent.usingFocus(player)){FociContent.useTick(stack,player,remaining);return;}
        var hit=getPlayerPOVHitResult(level,player,net.minecraft.world.level.ClipContext.Fluid.NONE);
        if(hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK || !(level.getBlockEntity(hit.getBlockPos()) instanceof AuraNodeEntity node)){player.stopUsingItem();return;}
        if(level.isClientSide || remaining%5!=0)return;
        var research=ResearchProgress.completed(player.getPersistentData());
        int tap=1+(research.contains("NODETAPPER1")?1:0)+(research.contains("NODETAPPER2")?1:0);
        var tag=NoteData.tag(stack);
        // Source TileNode excludes wood rods and iron caps and allows sneaking to bypass preservation.
        boolean preserve=!player.isShiftKeyDown()&&research.contains("NODEPRESERVE")&&WandData.capacity(stack)>2500&&WandParts.cap(stack).baseCostModifier()<1.1f;
        var aspect=node.reservoir.charge(tag,WandData.capacity(stack),tap,preserve);
        if(aspect!=null){stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));node.drain(player,aspect.getColor());node.changed();}
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        var level=context.getLevel();var pos=context.getClickedPos();
        if(OuterActivation.use(context,stack))return InteractionResult.sidedSuccess(level.isClientSide);
        if(AdvancedFurnace.create(context,stack))return InteractionResult.sidedSuccess(level.isClientSide);
        if(Thaumatorium.create(context,stack))return InteractionResult.sidedSuccess(level.isClientSide);
        if(WardedArcana.removeWithWand(stack,context))return InteractionResult.sidedSuccess(level.isClientSide);
        if(context.getPlayer()!=null && NodeJarCreation.create(level,pos,context.getPlayer(),stack))return InteractionResult.sidedSuccess(level.isClientSide);
        if(level.getBlockEntity(pos) instanceof NodeJarEntity jar){if(!level.isClientSide)jar.release();return InteractionResult.sidedSuccess(level.isClientSide);}
        if(context.getPlayer()!=null&&(level.getBlockState(pos).is(Blocks.OBSIDIAN)||level.getBlockState(pos).is(Blocks.NETHER_BRICKS)||level.getBlockState(pos).is(Blocks.IRON_BARS))
                &&InfernalFurnaceCreation.create(level,pos,context.getPlayer(),stack))return InteractionResult.sidedSuccess(level.isClientSide);
        if(level.getBlockEntity(pos) instanceof EssentiaBufferEntity buffer){
            buffer.useWand(context);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(level.getBlockEntity(pos) instanceof EssentiaTubeEntity tube){
            tube.useWand(context);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(level.getBlockEntity(pos) instanceof VisRelayEntity relay){
            if(!level.isClientSide)relay.cycleAttunement();
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(level.getBlockEntity(pos) instanceof CrucibleEntity tile){
            if(context.getPlayer()!=null && context.getPlayer().isShiftKeyDown() && !level.isClientSide){
                tile.crucible.empty(tile::spill);tile.changed();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(level.getBlockState(pos).is(Blocks.CAULDRON) || level.getBlockState(pos).is(Blocks.WATER_CAULDRON)
                || level.getBlockState(pos).is(Blocks.LAVA_CAULDRON) || level.getBlockState(pos).is(Blocks.POWDER_SNOW_CAULDRON)){
            if(!level.isClientSide)level.setBlock(pos,TC4Modern.CRUCIBLE.get().defaultBlockState(),3);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(level.getBlockEntity(pos) instanceof AuraNodeEntity && context.getPlayer()!=null){context.getPlayer().startUsingItem(context.getHand());return InteractionResult.CONSUME;}
        if(level.getBlockState(pos).is(TC4Modern.TABLE.get()) && context.getPlayer()!=null){
            if(!level.isClientSide){
                level.setBlock(pos,TC4Modern.ARCANE_WORKBENCH.get().defaultBlockState(),3);
                if(level.getBlockEntity(pos) instanceof ArcaneWorkbenchEntity table){table.inventory.setItem(9,stack.copyWithCount(1));stack.shrink(1);table.setChanged();}
                level.playSound(null,pos,net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),net.minecraft.sounds.SoundSource.BLOCKS,.15f,.5f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(!level.getBlockState(pos).is(Blocks.BOOKSHELF)) return InteractionResult.PASS;
        if(!level.isClientSide) {
            // WandManager.createThaumonomicon: EntitySpecialItem, PacketFXBlockSparkle(-9999) within 32 blocks, thaumcraft:wand.
            level.removeBlock(pos,false);
            var book=new thaumcraft.common.entities.EntitySpecialItem(TC4Modern.SPECIAL_ITEM.get(),level);
            book.setPos(pos.getX()+.5,pos.getY()+.3,pos.getZ()+.5);book.setItem(new ItemStack(TC4Modern.THAUMONOMICON.get()));
            book.setDeltaMovement(0,0,0);level.addFreshEntity(book);
            FociWorld.sparkle((net.minecraft.server.level.ServerLevel)level,pos,-9999,7);
            level.playSound(null,pos,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("wand")),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
