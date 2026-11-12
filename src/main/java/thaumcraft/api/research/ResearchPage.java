package thaumcraft.api.research;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import thaumcraft.api.aspects.AspectList;
public class ResearchPage {
 public enum PageType { TEXT, TEXT_CONCEALED, IMAGE, ASPECTS, NORMAL_CRAFTING, COMPOUND_CRAFTING, ARCANE_CRAFTING, CRUCIBLE_CRAFTING, INFUSION_CRAFTING }
 public PageType type = PageType.TEXT;
 public String text, research;
 public ResourceLocation image;
 public AspectList aspects;
 public String recipeKey;
public ResearchPage(String text) {
        this.type = PageType.TEXT;
        this.text = text;
    }
public ResearchPage(String research, String text) {
        this.type = PageType.TEXT_CONCEALED;
        this.research = research;
        this.text = text;
    }
public ResearchPage(ResourceLocation image, String caption) {
        this.type = PageType.IMAGE;
        this.image = image;
        this.text = caption;
    }
public ResearchPage(AspectList as) {
        this.type = PageType.ASPECTS;
        this.aspects = as;
    }
public String getTranslatedText() { return text == null ? "" : Component.translatable(text).getString(); }
}
