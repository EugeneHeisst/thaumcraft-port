package dev.eugheist.tc4modern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Issue #11: every original arcane workbench recipe is also a real recipe, tc4modern:arcane/&lt;name&gt;, pointing at its
 * original definition. Removing that recipe (datapack override, KubeJS remove by id or output) disables it in the
 * workbench and in JEI; crafting itself still follows the original ArcanePattern.
 */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class ArcaneRecipeIds {
    public static final DeferredHolder<RecipeType<?>,RecipeType<Pointer>> TYPE=DeferredHolder.create(Registries.RECIPE_TYPE,AlchemyContent.id("arcane"));
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<Pointer>> SERIALIZER=DeferredHolder.create(Registries.RECIPE_SERIALIZER,AlchemyContent.id("arcane"));
    @SubscribeEvent static void register(RegisterEvent e){
        e.register(Registries.RECIPE_TYPE,h->h.register(TYPE.getId(),RecipeType.simple(TYPE.getId())));
        e.register(Registries.RECIPE_SERIALIZER,h->h.register(SERIALIZER.getId(),new Serializer()));
    }
    /** The datapack face of one original recipe; the vanilla crafting systems never match it. */
    public record Pointer(String recipe) implements Recipe<RecipeInput> {
        @Override public boolean matches(RecipeInput input,Level level){return false;}
        @Override public ItemStack assemble(RecipeInput input,HolderLookup.Provider registries){return getResultItem(registries).copy();}
        @Override public boolean canCraftInDimensions(int width,int height){return width*height>=9;}
        @Override public ItemStack getResultItem(HolderLookup.Provider registries){var entry=ArcaneRecipes.byName(recipe);return entry==null?ItemStack.EMPTY:entry.output();}
        @Override public boolean isSpecial(){return true;}
        @Override public RecipeSerializer<?> getSerializer(){return SERIALIZER.get();}
        @Override public RecipeType<?> getType(){return TYPE.get();}
    }
    static final class Serializer implements RecipeSerializer<Pointer> {
        static final MapCodec<Pointer> CODEC=Codec.STRING.validate(name->ArcaneRecipes.names().contains(name)?DataResult.success(name):DataResult.error(()->"Unknown original arcane recipe "+name)).fieldOf("recipe").xmap(Pointer::new,Pointer::recipe);
        static final StreamCodec<RegistryFriendlyByteBuf,Pointer> STREAM=ByteBufCodecs.STRING_UTF8.<RegistryFriendlyByteBuf>cast().map(Pointer::new,Pointer::recipe);
        @Override public MapCodec<Pointer> codec(){return CODEC;}
        @Override public StreamCodec<RegistryFriendlyByteBuf,Pointer> streamCodec(){return STREAM;}
    }
    /** Names of the original recipes still present in this side's recipe manager, or null when none is available yet. */
    static java.util.Set<String> enabled(){
        boolean server=net.neoforged.fml.util.thread.EffectiveSide.get().isServer();RecipeManager manager=null;
        if(server){var s=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();if(s!=null)manager=s.getRecipeManager();}
        else if(net.neoforged.fml.loading.FMLEnvironment.dist.isClient())manager=Client.manager();
        if(manager==null)return null;
        // The per-type list is rebuilt whenever the manager's recipes are replaced (reload, KubeJS), so it keys the cache.
        var recipes=manager.getAllRecipesFor(TYPE.get());var cache=CACHE[server?0:1];
        if(cache==null||cache.recipes()!=recipes){var set=new java.util.HashSet<String>();for(var holder:recipes)set.add(holder.value().recipe());cache=new Cache(recipes,java.util.Set.copyOf(set));CACHE[server?0:1]=cache;}
        return cache.names();
    }
    private record Cache(Object recipes,java.util.Set<String> names){}
    private static final Cache[] CACHE=new Cache[2];
    static void invalidate(){CACHE[0]=null;CACHE[1]=null;}
    static final class Client {
        static RecipeManager manager(){var connection=net.minecraft.client.Minecraft.getInstance().getConnection();return connection==null?null:connection.getRecipeManager();}
    }
    @EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
    static final class ClientEvents {
        @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST) static void recipes(net.neoforged.neoforge.client.event.RecipesUpdatedEvent event){invalidate();}
    }
    @EventBusSubscriber(modid=TC4Modern.MODID)
    static final class ServerEvents {
        @SubscribeEvent static void reload(net.neoforged.neoforge.event.OnDatapackSyncEvent event){if(event.getPlayer()==null)invalidate();}
    }
}
