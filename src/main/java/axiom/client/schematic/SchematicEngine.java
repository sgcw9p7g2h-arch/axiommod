package axiom.client.schematic;
import java.util.*;
public final class SchematicEngine {
 public enum Rotation{NONE,CLOCKWISE_90,CLOCKWISE_180,CLOCKWISE_270}
 private final String name; private final List<SchematicBlock> blocks; private Rotation rotation=Rotation.NONE; private boolean mirrorX,mirrorZ;
 public SchematicEngine(String n,List<SchematicBlock>b){name=n;blocks=new ArrayList<>(b);}
 public String name(){return name;} public List<SchematicBlock> blocks(){return Collections.unmodifiableList(blocks);} public Rotation rotation(){return rotation;} public boolean mirrorX(){return mirrorX;} public boolean mirrorZ(){return mirrorZ;}
 public void rotateClockwise(){rotation=Rotation.values()[(rotation.ordinal()+1)%4];} public void toggleMirrorX(){mirrorX=!mirrorX;} public void toggleMirrorZ(){mirrorZ=!mirrorZ;} public void resetTransform(){rotation=Rotation.NONE;mirrorX=mirrorZ=false;}
 public List<SchematicBlock> transformedBlocks(){List<SchematicBlock> out=new ArrayList<>();for(var b:blocks){int x=b.x(),y=b.y(),z=b.z();if(mirrorX)x=-x;if(mirrorZ)z=-z;int a=x,q=z;switch(rotation){case CLOCKWISE_90->{x=-q;z=a;}case CLOCKWISE_180->{x=-a;z=-q;}case CLOCKWISE_270->{x=q;z=-a;}default->{}}out.add(new SchematicBlock(x,y,z,b.blockState()));}out.sort(Comparator.comparingInt(SchematicBlock::y).thenComparingInt(SchematicBlock::x).thenComparingInt(SchematicBlock::z));return out;}
 public int size(){return blocks.size();}
}
