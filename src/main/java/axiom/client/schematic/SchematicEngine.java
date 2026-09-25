package axiom.client.schematic;

import java.util.*;

public final class SchematicEngine {
 public enum Rotation{NONE,CLOCKWISE_90,CLOCKWISE_180,CLOCKWISE_270}
 private final String name; private final List<SchematicBlock> blocks;
 private Rotation rotation=Rotation.NONE; private boolean mirrorX,mirrorZ;
 private List<SchematicBlock> transformedCache; private boolean transformDirty=true;

 public SchematicEngine(String n,List<SchematicBlock>b){name=n;blocks=new ArrayList<>(b);}
 public String name(){return name;} public List<SchematicBlock> blocks(){return Collections.unmodifiableList(blocks);}
 public Rotation rotation(){return rotation;} public boolean mirrorX(){return mirrorX;} public boolean mirrorZ(){return mirrorZ;}
 public void rotateClockwise(){rotation=Rotation.values()[(rotation.ordinal()+1)%4];invalidate();}
 public void toggleMirrorX(){mirrorX=!mirrorX;invalidate();}
 public void toggleMirrorZ(){mirrorZ=!mirrorZ;invalidate();}
 public void resetTransform(){rotation=Rotation.NONE;mirrorX=mirrorZ=false;invalidate();}
 private void invalidate(){transformDirty=true;transformedCache=null;}

 public List<SchematicBlock> transformedBlocks(){
  if(!transformDirty&&transformedCache!=null)return transformedCache;
  List<SchematicBlock> out=new ArrayList<>(blocks.size());
  for(var b:blocks){
   int x=b.x(),y=b.y(),z=b.z();
   if(mirrorX)x=-x;if(mirrorZ)z=-z;
   int a=x,q=z;
   switch(rotation){
    case CLOCKWISE_90->{x=-q;z=a;}
    case CLOCKWISE_180->{x=-a;z=-q;}
    case CLOCKWISE_270->{x=q;z=-a;}
    default->{}
   }
   out.add(new SchematicBlock(x,y,z,b.blockState()));
  }
  out.sort(Comparator.comparingInt(SchematicBlock::y).thenComparingInt(SchematicBlock::x).thenComparingInt(SchematicBlock::z));
  transformedCache=Collections.unmodifiableList(out); transformDirty=false; return transformedCache;
 }
 public int size(){return blocks.size();}
}