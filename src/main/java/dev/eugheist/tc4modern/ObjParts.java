package dev.eugheist.tc4modern;
import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
/** Native submission of unchanged original OBJ groups, including their UVs and face normals. */
public final class ObjParts {
    private final Map<String,List<float[][]>> groups=new HashMap<>();
    private Object loadedResources;
    private static int generation;
    private int loadedGeneration=-1;
    public static void invalidate(){generation++;}
    private final ResourceLocation model;
    public ObjParts(ResourceLocation model){this.model=model;}
    private void load(){
        var resources=Minecraft.getInstance().getResourceManager();
        if(loadedResources==resources&&loadedGeneration==generation)return;
        groups.clear();
        try(var reader=resources.openAsReader(model)){
            List<float[]> positions=new ArrayList<>(),uvs=new ArrayList<>(),normals=new ArrayList<>();
            String group="";
            for(String line;(line=reader.readLine())!=null;){
                String[] fields=line.trim().split("\\s+");
                switch(fields[0]){
                    case "v","vt","vn"->{
                        float[] values=new float[fields.length-1];for(int i=1;i<fields.length;i++)values[i-1]=Float.parseFloat(fields[i]);
                        (fields[0].equals("v")?positions:fields[0].equals("vt")?uvs:normals).add(values);
                    }
                    case "g","o"->group=fields[1];
                    case "f"->{
                        float[][] face=new float[fields.length-1][];
                        for(int i=1;i<fields.length;i++){
                            String[] indices=fields[i].split("/");
                            var p=positions.get(index(indices[0],positions.size()));var uv=uvs.get(index(indices[1],uvs.size()));var n=normals.get(index(indices[2],normals.size()));
                            face[i-1]=new float[]{p[0],p[1],p[2],uv[0],1-uv[1],n[0],n[1],n[2]};
                        }
                        groups.computeIfAbsent(group,key->new ArrayList<>()).add(face);
                    }
                }
            }
            loadedResources=resources;loadedGeneration=generation;
        }catch(java.io.IOException ex){
            // A stale local resource pack must not crash the client: draw nothing and say so once per reload.
            com.mojang.logging.LogUtils.getLogger().warn("Missing original OBJ {} (re-run tools/import_tc4_resources.py): {}",model,ex.toString());
            loadedResources=resources;loadedGeneration=generation;
        }
    }
    private static int index(String value,int size){int i=Integer.parseInt(value);return i<0?size+i:i-1;}
    public void render(String group,ResourceLocation texture,PoseStack poses,MultiBufferSource buffers,int light,int overlay,int color){
        render(group,RenderType.entityTranslucent(texture),poses,buffers,light,overlay,color);
    }
    public void render(String group,RenderType type,PoseStack poses,MultiBufferSource buffers,int light,int overlay,int color){
        load();var vertices=buffers.getBuffer(type);var pose=poses.last();
        for(var face:groups.getOrDefault(group,List.of())){
            if(face.length==3||face.length==4){for(int i=0;i<4;i++)vertex(vertices,pose,face[Math.min(i,face.length-1)],light,overlay,color);}
            else for(int i=1;i<face.length-1;i++){vertex(vertices,pose,face[0],light,overlay,color);vertex(vertices,pose,face[i],light,overlay,color);vertex(vertices,pose,face[i+1],light,overlay,color);vertex(vertices,pose,face[i+1],light,overlay,color);}
        }
    }
    public void renderAll(RenderType type,PoseStack poses,MultiBufferSource buffers,int light,int overlay,int color){load();for(var group:groups.keySet())render(group,type,poses,buffers,light,overlay,color);}
    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer out,PoseStack.Pose pose,float[] v,int light,int overlay,int color){
        out.addVertex(pose,v[0],v[1],v[2]).setColor(color).setUv(v[3],v[4]).setOverlay(overlay).setLight(light).setNormal(pose,v[5],v[6],v[7]);
    }
}
