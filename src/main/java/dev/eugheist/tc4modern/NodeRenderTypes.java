package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
/** Submission state for the original source-alpha/additive node strips. */
public abstract class NodeRenderTypes extends RenderType {
    private NodeRenderTypes(){super("node",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,true,()->{},()->{});}
    public static net.minecraft.client.renderer.ShaderInstance shader;
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/misc/nodes.png");
    private static final TransparencyStateShard ADD=new TransparencyStateShard("tc4_node_add",()->{RenderSystem.enableBlend();RenderSystem.blendFunc(770,1);},()->{RenderSystem.disableBlend();RenderSystem.defaultBlendFunc();});
    /**
     * glDisable(GL_DEPTH_TEST) of the original see-through effects. Vanilla NO_DEPTH_TEST only skips enabling the test and
     * relies on it being off already; at AFTER_LEVEL and between batches it is often still on, hiding these behind blocks.
     */
    static final DepthTestStateShard SEE_THROUGH=new DepthTestStateShard("tc4_see_through",519){
        private boolean was;
        @Override public void setupRenderState(){was=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();}
        @Override public void clearRenderState(){if(was)com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();}
    };
    private static final RenderType[] TYPES={make(false,false),make(true,false),make(false,true),make(true,true)};
    private static final RenderType BEAM=makeBeam();
    private static final RenderType WISP=create("tc4_wisp",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder().setShaderState(new ShaderStateShard(()->shader)).setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/misc/particles.png"),false,false)).setTransparencyState(ADD).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));
    private static final RenderType ENERGY_RING=create("tc4_energy_ring",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder().setShaderState(new ShaderStateShard(()->shader)).setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/items/lightningringv.png"),false,false)).setTransparencyState(ADD).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));
    private static final java.util.Map<ResourceLocation,RenderType> ADDITIVE=new java.util.HashMap<>();
    /** Original blend (SRC_ALPHA, ONE) for an arbitrary unblurred texture, e.g. aspect tags on the scanner screen. */
    public static RenderType additive(ResourceLocation texture){return ADDITIVE.computeIfAbsent(texture,t->create("tc4_additive_"+t.toString().replace(':','_'),DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder().setShaderState(new ShaderStateShard(()->shader)).setTextureState(new TextureStateShard(t,false,false)).setTransparencyState(ADD).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false)));}
    private static final java.util.Map<ResourceLocation,RenderType> TRANSLUCENT=new java.util.HashMap<>();
    public static RenderType translucent(ResourceLocation texture){return TRANSLUCENT.computeIfAbsent(texture,t->create("tc4_translucent_"+t.toString().replace(':','_'),DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder().setShaderState(new ShaderStateShard(()->shader)).setTextureState(new TextureStateShard(t,false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false)));}
    private static final java.util.Map<ResourceLocation,RenderType> ADDITIVE_THROUGH=new java.util.HashMap<>();
    /** Additive and drawn through everything (depth test off), e.g. the architect area preview. */
    public static RenderType additiveThrough(ResourceLocation texture){return ADDITIVE_THROUGH.computeIfAbsent(texture,t->create("tc4_additive_through_"+t.toString().replace(':','_'),DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder().setShaderState(new ShaderStateShard(()->shader)).setTextureState(new TextureStateShard(t,false,false)).setTransparencyState(ADD).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setDepthTestState(SEE_THROUGH).createCompositeState(false)));}
    /** Writes depth only: seals see-through faces (mirror star field) so translucent terrain behind them cannot draw through. */
    private static final RenderType DEPTH_ONLY=create("tc4_depth_only",DefaultVertexFormat.POSITION,VertexFormat.Mode.QUADS,256,false,false,CompositeState.builder().setShaderState(POSITION_SHADER).setCullState(NO_CULL).setWriteMaskState(DEPTH_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));
    public static RenderType depthOnly(){return DEPTH_ONLY;}
    public static RenderType energyRing(){return ENERGY_RING;}
    public static RenderType wisp(){return WISP;}
    public static RenderType beam(){return BEAM;}
    private static RenderType makeBeam(){return create("tc4_node_beam",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder().setShaderState(new ShaderStateShard(()->shader)).setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/misc/wispy.png"),false,false)).setTransparencyState(ADD).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));}
    private static RenderType make(boolean additive,boolean through){
        return create("tc4_node_"+additive+"_"+through,DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,false,CompositeState.builder().setShaderState(new ShaderStateShard(()->shader)).setTextureState(new TextureStateShard(TEXTURE,false,false)).setTransparencyState(additive?ADD:TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setDepthTestState(through?SEE_THROUGH:LEQUAL_DEPTH_TEST).createCompositeState(false));
    }
    public static RenderType get(boolean additive,boolean through){return TYPES[(additive?1:0)+(through?2:0)];}
}
