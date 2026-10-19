package dev.eugheist.tc4modern;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in isolated server check, after real recipes and tags have loaded. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class AspectCoverageAudit {
    public static Map<String,Object> report(Level level) {
        Map<String,Object> report = new TreeMap<>();
        Map<String,Map<String,Integer>> items = new TreeMap<>(), blocks = new TreeMap<>(), entities = new TreeMap<>();
        var missing = new ArrayList<String>();
        for (var item : BuiltInRegistries.ITEM) {
            String id=BuiltInRegistries.ITEM.getKey(item).toString();
            if (!id.startsWith("minecraft:") && !id.startsWith("tc4modern:")) continue;
            var tags=RecipeAspects.get(new ItemStack(item),level); items.put(id,tags);
            if (tags.isEmpty() && !id.equals("minecraft:air") && !id.equals("tc4modern:essentia_phial")) missing.add("item:"+id);
        }
        for (var block : BuiltInRegistries.BLOCK) {
            String id=BuiltInRegistries.BLOCK.getKey(block).toString();
            if (!id.startsWith("minecraft:") && !id.startsWith("tc4modern:")) continue;
            var tags=BlockAspectBridge.get(block.defaultBlockState(),level);
            blocks.put(id,tags);
            if(tags.isEmpty() && !id.matches("minecraft:(air|cave_air|void_air)") && !id.equals("tc4modern:aura_node")) missing.add("block:"+id);
        }
        for (var type : BuiltInRegistries.ENTITY_TYPE) {
            String id=BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            if (!id.startsWith("minecraft:") && !id.startsWith("tc4modern:")) continue;
            var list=ModernAspectCatalog.entity(id); Map<String,Integer> tags=new TreeMap<>();
            for(var aspect:list.getAspects())if(list.getAmount(aspect)>0)tags.put(aspect.getTag(),list.getAmount(aspect));
            entities.put(id,tags);
            if(tags.isEmpty() && !id.equals("tc4modern:special_item"))missing.add("entity:"+id);
        }
        report.put("items",items);report.put("blocks",blocks);report.put("entities",entities);report.put("missing",missing);
        report.put("dynamic",Map.of("tc4modern:aura_node","Actual node reservoir", "tc4modern:special_item","Contained ItemStack", "minecraft:item","Contained ItemStack", "minecraft:player","Player-specific original tags", "tc4modern:essentia_phial","Actual contained essentia; default empty stack is invalid"));
        Map<String,Map<String,Integer>> variants=new TreeMap<>();
        for(var block:new net.minecraft.world.level.block.Block[]{TC4Modern.TAINT.get(),TC4Modern.TAINT_FIBRES.get()}) {
            var property=block instanceof TaintBlock ? TaintBlock.VARIANT : TaintFibresBlock.VARIANT;
            for(int variant:property.getPossibleValues()) {
                var state=block.defaultBlockState().setValue(property,variant);
                variants.put(BlockAspectBridge.key(state),BlockAspectBridge.get(state,level));
            }
        }
        for(var aspect:thaumcraft.api.aspects.Aspect.aspects.values()) {
            var tags=RecipeAspects.get(EssentiaItems.phialOf(aspect,8),level);
            if(tags.getOrDefault(aspect.getTag(),0)!=8)throw new IllegalStateException("Filled phial lost content: "+aspect.getTag());
            variants.put("tc4modern:essentia_phial#"+aspect.getTag(),tags);
        }
        for(var cap:WandParts.CAPS.values())for(var rod:WandParts.RODDEFS.values()) {
            var stack=WandParts.make(cap,rod);
            var tags=RecipeAspects.get(stack,level);
            if(tags.getOrDefault("praecantatio",0)<(cap.craftCost()+rod.craftCost())/2)throw new IllegalStateException("Missing original wand bonuses");
            variants.put("tc4modern:wand#"+cap.tag()+"/"+rod.tag(),tags);
        }
        for(var potion:BuiltInRegistries.POTION.holders().toList()) {
            var stack=new ItemStack(net.minecraft.world.item.Items.POTION);
            stack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,new net.minecraft.world.item.alchemy.PotionContents(potion));
            var tags=RecipeAspects.get(stack,level);
            if(tags.getOrDefault("aqua",0)!=1)throw new IllegalStateException("Missing original potion water");
            if(potion.value().getEffects().size()>0 && tags.getOrDefault("praecantatio",0)<2)throw new IllegalStateException("Missing effect magic");
            variants.put(StackAspectBridge.scanKey(stack,"minecraft:potion"),tags);
        }
        if(!ModernAspectCatalog.entity("minecraft:zombie").aspects.equals(thaumcraft.common.config.ConfigEntityAspects.get("minecraft:zombie").aspects))throw new IllegalStateException("Original entity tags replaced");
        var sword=new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
        var clean=RecipeAspects.get(sword,level);
        sword.setDamageValue(50);
        if(!clean.equals(RecipeAspects.get(sword,level)))throw new IllegalStateException("Durability changes scan tags");
        var enchantments=new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        enchantments.set(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS),5);
        sword.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS,enchantments.toImmutable());
        var enchanted=RecipeAspects.get(sword,level);
        if(enchanted.getOrDefault("praecantatio",0)<5 || enchanted.getOrDefault("telum",0)<5)throw new IllegalStateException("Missing original enchantment bonuses");
        if(!clean.equals(RecipeAspects.get(new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD),level)))throw new IllegalStateException("Stack bonuses polluted recipe cache");
        variants.put("minecraft:diamond_sword#sharpness5",enchanted);
        woodenShapeRecipes(level,variants);
        candleRecipes(level,variants);
        blackDyeRecipes(level);
        originalItemTags(level,variants);
        entityVariants(level,variants);
        report.put("variants",variants);
        for(var flow:net.minecraft.network.protocol.PacketFlow.values()) {
            String path=flow==net.minecraft.network.protocol.PacketFlow.SERVERBOUND?"open_baubles":"baubles_sync";
            if(net.neoforged.neoforge.network.registration.NetworkRegistry.getCodec(AlchemyContent.id(path),net.minecraft.network.ConnectionProtocol.PLAY,flow)==null)throw new IllegalStateException("Baubles dedicated-server channel missing: "+path+" "+flow);
        }
        System.out.println("TC4_BAUBLES_NETWORK ALL PASS dedicated-server open and sync channels registered");
        return report;
    }
    static void woodenShapeRecipes(Level level,Map<String,Map<String,Integer>> variants) {
        for(String wood:new String[]{"greatwood","silverwood"})for(String shape:new String[]{"stairs","slab"}) {
            String name=wood+"_"+shape;var source=BuiltInRegistries.ITEM.get(AlchemyContent.id(wood+"_planks"));
            var planks=new ItemStack(source);
            var grid=shape.equals("slab")?net.minecraft.world.item.crafting.CraftingInput.of(3,1,java.util.Collections.nCopies(3,planks)):
                net.minecraft.world.item.crafting.CraftingInput.of(3,3,java.util.List.of(planks,ItemStack.EMPTY,ItemStack.EMPTY,planks,planks,ItemStack.EMPTY,planks,planks,planks));
            var recipe=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,level).orElseThrow().value();var result=recipe.assemble(grid,level.registryAccess());
            if(!result.is(BuiltInRegistries.ITEM.get(AlchemyContent.id(name)))||result.getCount()!=(shape.equals("slab")?6:4))throw new IllegalStateException("Original wooden shape recipe missing: "+name);
            if(!result.is(shape.equals("slab")?net.minecraft.tags.ItemTags.WOODEN_SLABS:net.minecraft.tags.ItemTags.WOODEN_STAIRS))throw new IllegalStateException("Wooden recipe integration missing: "+name);
            var state=net.minecraft.world.level.block.Block.byItem(result.getItem()).defaultBlockState();
            if(!state.is(shape.equals("slab")?net.minecraft.tags.BlockTags.WOODEN_SLABS:net.minecraft.tags.BlockTags.WOODEN_STAIRS)||!state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE))throw new IllegalStateException("Wooden block integration missing: "+name);
            var tags=RecipeAspects.get(result,level);if(tags.isEmpty())throw new IllegalStateException("Wooden shape cannot be scanned: "+name);
            if(shape.equals("slab")&&!tags.equals(Map.of("arbor",1)))throw new IllegalStateException("Original slabWood ore aspects differ: "+tags);
            variants.put("tc4modern:"+name,tags);
        }
        System.out.println("TC4_WOOD_SHAPES ALL PASS four real recipes, original yields, wood/axe tags and aspects");
    }
    static void candleRecipes(Level level,Map<String,Map<String,Integer>> variants) {
        for(int color=0;color<16;color++) {
            var candle=BuiltInRegistries.ITEM.get(AlchemyContent.id("candle_"+color));
            var input=net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(new ItemStack(candle),new ItemStack(net.minecraft.world.item.Items.WHITE_DYE)));
            var clear=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,level).orElseThrow().value();
            if(!clear.assemble(input,level.registryAccess()).is(BuiltInRegistries.ITEM.get(AlchemyContent.id("candle_0"))))throw new IllegalStateException("Candle cannot be cleared: "+color);
            if(color>0) {
                var dye=net.minecraft.world.item.DyeItem.byColor(net.minecraft.world.item.DyeColor.byId(color));
                var white=BuiltInRegistries.ITEM.get(AlchemyContent.id("candle_0"));
                var grid=net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(new ItemStack(dye),new ItemStack(white)));
                var recipe=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,level).orElseThrow().value();
                var result=recipe.assemble(grid,level.registryAccess());
                if(!result.is(candle)||result.getCount()!=1)throw new IllegalStateException("Wrong dyed candle: "+color);
                var tags=RecipeAspects.get(result,level);
                if(tags.isEmpty()||!tags.equals(BlockAspectBridge.get(net.minecraft.world.level.block.Block.byItem(candle).defaultBlockState(),level)))throw new IllegalStateException("Colored candle lost generated aspects: "+color+" item="+tags+" block="+BlockAspectBridge.get(net.minecraft.world.level.block.Block.byItem(candle).defaultBlockState(),level));
                variants.put("tc4modern:candle_"+color,tags);
            }
        }
        for(String name:new String[]{"air_arrow","goggles","arcane_ear"}) {
            var tags=RecipeAspects.get(new ItemStack(BuiltInRegistries.ITEM.get(AlchemyContent.id(name))),level);
            if(tags.getOrDefault(name.equals("air_arrow")?"telum":"sensus",0)<(name.equals("air_arrow")?1:4))throw new IllegalStateException("Missing original complex tags: "+name);
        }
        for(String name:new String[]{"obsidian_tile","amber_bricks"}) {
            var source=name.equals("obsidian_tile")?net.minecraft.world.item.Items.OBSIDIAN:ArcaneLamps.AMBER.get().asItem();
            var grid=net.minecraft.world.item.crafting.CraftingInput.of(2,2,java.util.Collections.nCopies(4,new ItemStack(source)));
            var recipe=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,level).orElseThrow().value();
            var result=recipe.assemble(grid,level.registryAccess());
            if(!result.is(BuiltInRegistries.ITEM.get(AlchemyContent.id(name)))||result.getCount()!=4)throw new IllegalStateException("Missing decorative recipe: "+name);
            if(RecipeAspects.get(result,level).isEmpty())throw new IllegalStateException("Missing decorative aspects: "+name);
            variants.put("tc4modern:"+name,RecipeAspects.get(result,level));
        }
        if(!RecipeAspects.get(new ItemStack(BuiltInRegistries.ITEM.get(AlchemyContent.id("coin"))),level).equals(Map.of("metallum",1)))throw new IllegalStateException("Original coin tags missing");
        System.out.println("TC4_DECORATIVE_RECIPES ALL PASS: four obsidian tiles, four amber bricks and original coin tags");
        System.out.println("TC4_CANDLE_RECIPES ALL PASS: 15 dyes, 16 clearing inputs, generated item/block tags and original complex bonuses");
    }
    /** Legacy dyeBlack accepts ink sacs; modern black dye must remain usable as well. */
    static void blackDyeRecipes(Level level) {
        var registry=BuiltInRegistries.ITEM;
        var tools=registry.get(AlchemyContent.id("scribing_tools"));
        for(var ink:java.util.List.of(net.minecraft.world.item.Items.INK_SAC,net.minecraft.world.item.Items.BLACK_DYE)) {
            for(var bottle:java.util.List.of(net.minecraft.world.item.Items.GLASS_BOTTLE,registry.get(AlchemyContent.id("phial")))) {
                var input=net.minecraft.world.item.crafting.CraftingInput.of(3,1,java.util.List.of(new ItemStack(bottle),new ItemStack(net.minecraft.world.item.Items.FEATHER),new ItemStack(ink)));
                var recipe=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,level).orElseThrow();
                var result=recipe.value().assemble(input,level.registryAccess());
                if(!result.is(tools)||result.getCount()!=1||result.getDamageValue()!=0)throw new IllegalStateException("Original scribing recipe failed: "+ink+" / "+bottle);
            }
            var worn=new ItemStack(tools);worn.setDamageValue(12);
            var refill=net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(worn,new ItemStack(ink)));
            var repaired=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,refill,level).orElseThrow().value().assemble(refill,level.registryAccess());
            if(!repaired.is(tools)||repaired.getDamageValue()!=0||worn.getDamageValue()!=12)throw new IllegalStateException("Original scribing refill failed: "+ink);
            var labels=net.minecraft.world.item.crafting.CraftingInput.of(3,2,java.util.List.of(new ItemStack(ink),new ItemStack(net.minecraft.world.item.Items.SLIME_BALL),new ItemStack(net.minecraft.world.item.Items.PAPER),new ItemStack(net.minecraft.world.item.Items.PAPER),new ItemStack(net.minecraft.world.item.Items.PAPER),new ItemStack(net.minecraft.world.item.Items.PAPER)));
            var labelled=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,labels,level).orElseThrow().value().assemble(labels,level.registryAccess());
            if(!labelled.is(registry.get(AlchemyContent.id("jar_label")))||labelled.getCount()!=4)throw new IllegalStateException("Original four-label recipe failed: "+ink);
            var candle=net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(new ItemStack(ink),new ItemStack(registry.get(AlchemyContent.id("candle_0")))));
            var dyed=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,candle,level).orElseThrow().value().assemble(candle,level.registryAccess());
            if(!dyed.is(registry.get(AlchemyContent.id("candle_15")))||dyed.getCount()!=1)throw new IllegalStateException("Original black candle recipe failed: "+ink);
        }
        System.out.println("TC4_BLACK_DYE_RECIPES ALL PASS ink sacs and modern black dye: bottle/phial scribing, worn-tool refill, four labels and black candles");
    }
    static void originalItemTags(Level level,Map<String,Map<String,Integer>> variants) {
        var expected=Map.of(
            "focus_pech",Map.of("praecantatio",5,"venenum",5,"perditio",5,"alienis",5,"telum",5),
            "cinnabar_cluster",Map.of("ordo",1,"metallum",4,"terra",1,"permutatio",4,"venenum",2),
            "wisp_essence",Map.of("auram",2));
        for(var entry:expected.entrySet()) {
            var id=AlchemyContent.id(entry.getKey());
            var tags=RecipeAspects.get(new ItemStack(BuiltInRegistries.ITEM.get(id)),level);
            if(!tags.equals(entry.getValue()))throw new IllegalStateException("Original item tags lost: "+id+" "+tags);
            variants.put(id.toString(),tags);
        }
        for(var aspect:thaumcraft.api.aspects.Aspect.aspects.values()) {
            var tags=RecipeAspects.get(WispEssenceItem.of(aspect,2),level);
            var wanted=new java.util.HashMap<>(Map.of("auram",2));wanted.merge(aspect.getTag(),2,Integer::sum);
            if(!tags.equals(wanted))throw new IllegalStateException("Wisp essence content lost: "+aspect.getTag()+" "+tags);
            variants.put("tc4modern:wisp_essence#"+aspect.getTag(),tags);
        }
        for(String name:new String[]{"eldritch_altar","eldritch_obelisk","eldritch_obelisk_body","eldritch_cap","runed_stone","crusted_opening"}) {
            var tags=BlockAspectBridge.get(BuiltInRegistries.BLOCK.get(AlchemyContent.id(name)).defaultBlockState(),level);
            Map<String,Integer> wanted=name.equals("eldritch_cap")?Map.of("vacuos",4,"alienis",4):Map.of("vacuos",8,"alienis",8,"sensus",4);
            if(!tags.equals(wanted))throw new IllegalStateException("Original device tags lost: "+name+" "+tags);
            variants.put("tc4modern:"+name,tags);
        }
        if(!RecipeAspects.get(new ItemStack(BuiltInRegistries.ITEM.get(AlchemyContent.id("primal_ring_aer"))),level).equals(Map.of("praecantatio",5)))throw new IllegalStateException("Original air-ring tags missing");
        for(int part=0;part<=4;part++) {
            var tags=BlockAspectBridge.get(AdvancedFurnace.BLOCK.get().defaultBlockState().setValue(AdvancedFurnace.PART,part),level);
            if(!tags.equals(Map.of("praecantatio",8,"aqua",8,"fabrico",8)))throw new IllegalStateException("Original advanced furnace part lost wildcard aspects: "+part);
            variants.put("tc4modern:advanced_alchemical_furnace#part="+part,tags);
        }
        // Issue #5: a placed powder snow block is scanned as the block, not as its bucket item.
        var snow=net.minecraft.world.level.block.Blocks.POWDER_SNOW.defaultBlockState();
        if(!BlockAspectBridge.bucketForm(new ItemStack(snow.getBlock()))||!BlockAspectBridge.stack(snow).isEmpty())throw new IllegalStateException("Powder snow block still resolves to its bucket item");
        if(!BlockAspectBridge.key(snow).equals("minecraft:powder_snow"))throw new IllegalStateException("Powder snow block shares the bucket scan key");
        if(!BlockAspectBridge.get(snow,level).equals(ModernAspectCatalog.get("blocks","minecraft:powder_snow")))throw new IllegalStateException("Powder snow block lost its block aspects: "+BlockAspectBridge.get(snow,level));
        for(var block:java.util.List.of(net.minecraft.world.level.block.Blocks.STONE,net.minecraft.world.level.block.Blocks.SNOW_BLOCK,net.minecraft.world.level.block.Blocks.OAK_LOG))if(BlockAspectBridge.stack(block.defaultBlockState()).isEmpty())throw new IllegalStateException("Ordinary block lost its item form: "+block);
        System.out.println("TC4_POWDER_SNOW_SCAN ALL PASS block key/aspects "+BlockAspectBridge.get(snow,level)+", name source block, bucket separate");
        // Issue #8: every wood family carries the original oak bonus; warped and nether wart blocks match.
        var woods=new java.util.TreeMap<String,Map<String,Integer>>();
        for(String kind:List.of("door","trapdoor","pressure_plate","fence_gate","button","boat")){
            var oakTags=RecipeAspects.get(new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("oak_"+kind))),level);
            for(String wood:List.of("spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","bamboo","crimson","warped")){
                String id=wood.equals("bamboo")&&kind.equals("boat")?"bamboo_raft":wood+"_"+kind;var item=BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(id));
                if(item==Items.AIR)continue;var tags=RecipeAspects.get(new ItemStack(item),level);woods.put(id,tags);
                for(var aspect:oakTags.keySet())if(!aspect.equals("arbor")&&!tags.containsKey(aspect))throw new IllegalStateException(id+" lacks the original oak "+kind+" bonus "+aspect+": "+tags+" vs oak "+oakTags);
                var block=net.minecraft.world.level.block.Block.byItem(item);if(block!=net.minecraft.world.level.block.Blocks.AIR&&!BlockAspectBridge.get(block.defaultBlockState(),level).equals(tags))throw new IllegalStateException("placed "+id+" differs from its item: "+BlockAspectBridge.get(block.defaultBlockState(),level)+" vs "+tags);
            }
        }
        if(!RecipeAspects.get(new ItemStack(Items.IRON_DOOR),level).containsKey("metallum")||RecipeAspects.get(new ItemStack(Items.STONE_BUTTON),level).containsKey("arbor"))throw new IllegalStateException("non-wood doors/buttons changed");
        var nether=RecipeAspects.get(new ItemStack(Items.NETHER_WART_BLOCK),level);var warped=RecipeAspects.get(new ItemStack(Items.WARPED_WART_BLOCK),level);
        if(!nether.equals(warped))throw new IllegalStateException("wart blocks differ: nether "+nether+" warped "+warped);
        System.out.println("TC4_WOOD_VARIANT_ASPECTS ALL PASS "+woods.size()+" wood variants carry oak bonuses, e.g. spruce_door="+woods.get("spruce_door")+" birch_boat="+woods.get("birch_boat")+"; wart blocks "+warped);
        System.out.println("TC4_FURNACE_RING_ASPECTS ALL PASS original wildcard tags for five furnace parts and metadata-three air ring");
        System.out.println("TC4_ORIGINAL_ITEM_ASPECTS ALL PASS: static items, 48 filled wisp essences and five obelisk/device blocks");
    }
    static void verifyEntity(ScanTarget target,Map<String,Integer> expected,net.minecraft.nbt.CompoundTag knowledge,java.util.Set<String> keys,Map<String,Map<String,Integer>> variants){
        if(!target.aspects().equals(expected))throw new IllegalStateException("Wrong original entity aspects: "+target.knowledgeKey()+" "+target.aspects()+" expected "+expected);
        if(!keys.add(target.knowledgeKey()))throw new IllegalStateException("Entity variants share scan history: "+target.knowledgeKey());
        if(!ResearchKnowledge.scan(knowledge,target.knowledgeKey(),target.aspects()).equals("scanned")||!ResearchKnowledge.scan(knowledge,target.knowledgeKey(),target.aspects()).equals("already"))throw new IllegalStateException("Entity variant cannot be scanned independently: "+target.knowledgeKey());
        variants.put(target.knowledgeKey(),target.aspects());
    }
    static void entityVariants(Level level,Map<String,Map<String,Integer>> variants){var knowledge=new net.minecraft.nbt.CompoundTag();ResearchKnowledge.discoverAll(knowledge);var keys=new java.util.HashSet<String>();
        var expected=Map.of("pig",Map.of("vitium",2,"terra",2),"sheep",Map.of("vitium",2,"terra",2),"cow",Map.of("vitium",3,"terra",3),"chicken",Map.of("vitium",2,"volatus",2,"aer",1),"villager",Map.of("vitium",3,"aer",2),"creeper",Map.of("vitium",2,"ignis",2));
        for(var entry:TaintedContent.TYPES.entrySet())verifyEntity(ScanTarget.entity(entry.getValue().get().create(level)),expected.get(entry.getKey()),knowledge,keys,variants);
        var pech=PechContent.PECH.get().create(level);for(int type=0;type<3;type++){pech.variant(type);var tags=new java.util.HashMap<>(Map.of("humanus",2,"praecantatio",2,"permutatio",2));if(type==0)tags.put("lucrum",2);else if(type==1)tags.put("telum",2);else tags.put("praecantatio",4);verifyEntity(ScanTarget.entity(pech),tags,knowledge,keys,variants);}
        var wisp=WispContent.WISP.get().create(level);for(var aspect:thaumcraft.api.aspects.Aspect.aspects.values()){wisp.aspect(aspect);var tags=new java.util.HashMap<>(Map.of("praecantatio",1,"aer",1));tags.merge(aspect.getTag(),2,Integer::sum);verifyEntity(ScanTarget.entity(wisp),tags,knowledge,keys,variants);}
        var swarmer=TC4Modern.TAINT_SWARMER.get().create(level);verifyEntity(ScanTarget.entity(swarmer),Map.of("vitium",2,"aer",2),knowledge,keys,variants);
        System.out.println("TC4_ENTITY_ASPECTS ALL PASS "+keys.size()+" actual variants, original tags and independent scan history");
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if(!Boolean.getBoolean("tc4modern.aspectAudit"))return;
        try {
            SpawnConfigAudit.defaults(event.getServer().overworld());
            var report=report(event.getServer().overworld());
            Files.writeString(java.nio.file.Path.of("aspect-coverage.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
            System.out.println("TC4_ASPECT_AUDIT missing="+report.get("missing"));
        } catch(Exception ex) { throw new IllegalStateException("Aspect coverage audit failed",ex); }
        finally { event.getServer().halt(false); }
    }
}
