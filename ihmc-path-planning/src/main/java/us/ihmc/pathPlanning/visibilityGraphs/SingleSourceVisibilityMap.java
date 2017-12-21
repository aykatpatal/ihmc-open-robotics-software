package us.ihmc.pathPlanning.visibilityGraphs;

import java.util.Set;

import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.VisibilityMapHolder;

public class SingleSourceVisibilityMap implements VisibilityMapHolder
{
   private final Point3D sourceInWorld, sourceInLocal;
   private final int mapId;
   private final RigidBodyTransform transformToWorld;
   private final NavigableRegion hostRegion;
   private final VisibilityMap visibilityMapInLocal;
   private final VisibilityMap visibilityMapInWorld;

   public SingleSourceVisibilityMap(Point3DReadOnly sourceInWorld, Set<Connection> connectionsInLocal, NavigableRegion hostRegion)
   {
      this.hostRegion = hostRegion;
      this.sourceInWorld = new Point3D(sourceInWorld);
      this.mapId = hostRegion.getMapId();
      this.transformToWorld = hostRegion.getTransformToWorld();

      sourceInLocal = new Point3D(sourceInWorld);
      hostRegion.transformFromWorldToLocal(sourceInLocal);

      visibilityMapInLocal = new VisibilityMap(connectionsInLocal);
      visibilityMapInWorld = new VisibilityMap(visibilityMapInLocal.getConnections());
      visibilityMapInWorld.applyTransform(transformToWorld);
      visibilityMapInWorld.computeVertices();
   }

   public Point3DReadOnly getSourceInWorld()
   {
      return sourceInWorld;
   }

   public Point3D getSourceInLocal3D()
   {
      return sourceInLocal;
   }
   
   public Point2D getSourceInLocal2D()
   {
      return new Point2D(sourceInLocal);
   }

   public NavigableRegion getHostRegion()
   {
      return hostRegion;
   }

   @Override
   public int getMapId()
   {
      return mapId;
   }

   @Override
   public VisibilityMap getVisibilityMapInLocal()
   {
      return visibilityMapInLocal;
   }

   @Override
   public VisibilityMap getVisibilityMapInWorld()
   {
      return visibilityMapInWorld;
   }
}
