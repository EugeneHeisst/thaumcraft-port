#!/usr/bin/env python3
"""Convert literal BlockArcaneFurnace/renderer texture indices and planes into local models."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[2]/'thaumcraft4-reference/ported-resources/assets/tc4modern'
faces=['down','up','north','south','west','east']
directions={'north':(0,-1),'south':(0,1),'west':(-1,0),'east':(1,0)}
def icon(part,layer,side,outlet):
    level=(2-layer)*9
    x,z=(part-1)%3,(part-1)//3
    dx,dz=directions[outlet];exitpos=(1+dx,1+dz)
    step={'north':(0,-1),'south':(0,1),'west':(-1,0),'east':(1,0)}.get(side,(0,0))
    add=3 if layer==1 and side in directions and (x+step[0],z+step[1])==exitpos else 0
    if side in ('up','down'):
        if side=='up' and level==18 and part in (2,4,6,8):return {2:16,4:17,6:26,8:25}[part]
        if add==0:return 10 if part==5 else (part-1)%3+(part-1)//3*9
        return 6
    mapping={'north':{1:2,2:1,3:0},'south':{7:0,8:1,9:2},'west':{1:0,4:1,7:2},'east':{3:2,6:1,9:0}}[side]
    return mapping[part]+level+add if part in mapping else (6 if level==9 else 7)
def write(path,data):
    p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
variants={}
for part in range(11):
 for layer in range(3):
  for outlet in directions:
   name=f'infernal_furnace/{part}_{layer}_{outlet}'
   if part==0:
    model={'parent':'minecraft:block/cube_all','textures':{'all':'tc4modern:blocks/original_lava_still'}}
   elif part==10:
    elements=[];textures={'particle':'tc4modern:blocks/furnace13'};positive=outlet in ('east','south');axis=0 if outlet in ('east','west') else 2
    for texture,offset,height in [('tc4modern:blocks/furnace13',.625,16),('tc4modern:blocks/furnace15',.8,16),('tc4modern:blocks/original_fire_layer_0',.9,24)]:
     coordinate=(1-offset if positive else offset)*16
     lo=[0,0,0];hi=[16,height,16];lo[axis]=hi[axis]=coordinate
     key='plane'+str(len(elements));textures[key]=texture
     elements.append({'from':lo,'to':hi,'shade':False,'faces':{outlet:{'uv':[0,0,16,16],'texture':'#'+key}}})
    model={'render_type':'minecraft:cutout','textures':textures,'elements':elements}
   else:
    model={'parent':'minecraft:block/cube','textures':{face:f'tc4modern:blocks/furnace{icon(part,layer,face,outlet)}' for face in faces}}
    model['textures']['particle']=model['textures']['north']
   write(Path('models/block')/(name+'.json'),model)
   variants[f'part={part},layer={layer},facing={outlet}']={'model':'tc4modern:block/'+name}
write(Path('blockstates/infernal_furnace.json'),{'variants':variants})
print(f'Wrote {len(variants)} local original furnace model variants')

# Original BlockArcaneFurnace.getItemDropped: metadata-specific base construction materials.
pools=[]
for part in range(1,11):
 item='minecraft:iron_bars' if part==10 else 'minecraft:obsidian' if part%2==0 or part==5 else 'minecraft:nether_bricks'
 pools.append({'rolls':1,'entries':[{'type':'minecraft:item','name':item}],
  'conditions':[{'condition':'minecraft:block_state_property','block':'tc4modern:infernal_furnace','properties':{'part':str(part)}},{'condition':'minecraft:survives_explosion'}]})
loot=root.parents[1]/'data/tc4modern/loot_table/blocks/infernal_furnace.json'
loot.parent.mkdir(parents=True,exist_ok=True)
loot.write_text(json.dumps({'type':'minecraft:block','pools':pools},indent=2)+'\n')
