package us.ihmc.robotics.geometry;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import us.ihmc.robotics.MathTools;

public class PlanarRegion
{
   public static final int NO_REGION_ID = -1;
   public static final double DEFAULT_BOUNDING_BOX_EPSILON = 1e-15;

   private int regionId = NO_REGION_ID;
   private final RigidBodyTransform fromLocalToWorldTransform = new RigidBodyTransform();
   private final RigidBodyTransform fromWorldToLocalTransform = new RigidBodyTransform();
   private final List<Point2d> concaveHullVertices;
   /**
    * List of the convex polygons representing this planar region.
    * They are in the local frame of the plane.
    */
   private final List<ConvexPolygon2d> convexPolygons;

   private final BoundingBox3d boundingBox3dInWorld = new BoundingBox3d(new Point3d(Double.NaN, Double.NaN, Double.NaN),
         new Point3d(Double.NaN, Double.NaN, Double.NaN));
   private final Point3d tempPointForConvexPolygonProjection = new Point3d();

   private final ConvexPolygon2d convexHull = new ConvexPolygon2d();

   /**
    * Create a new, empty planar region.
    */
   public PlanarRegion()
   {
      concaveHullVertices = new ArrayList<>();
      convexPolygons = new ArrayList<>();
      boundingBox3dInWorld.setEpsilonToGrow(DEFAULT_BOUNDING_BOX_EPSILON);
      updateBoundingBox();
      updateConvexHull();
   }

   /**
    * Create a new planar region.
    * @param transformToWorld transform from the region local coordinate system to world.
    * @param planarRegionConvexPolygons the list of convex polygon that represents the planar region. Expressed in local coordinate system.
    */
   public PlanarRegion(RigidBodyTransform transformToWorld, List<ConvexPolygon2d> planarRegionConvexPolygons)
   {
      fromLocalToWorldTransform.set(transformToWorld);
      fromWorldToLocalTransform.invert(fromLocalToWorldTransform);
      concaveHullVertices = new ArrayList<>();
      convexPolygons = planarRegionConvexPolygons;
      boundingBox3dInWorld.setEpsilonToGrow(DEFAULT_BOUNDING_BOX_EPSILON);
      updateBoundingBox();
      updateConvexHull();
   }

   /**
    * Create a new planar region.
    * @param transformToWorld transform from the region local coordinate system to world.
    * @param concaveHullVertices vertices of the concave hull of the region.
    * @param planarRegionConvexPolygons the list of convex polygon that represents the planar region. Expressed in local coordinate system.
    */
   public PlanarRegion(RigidBodyTransform transformToWorld, List<Point2d> concaveHullVertices, List<ConvexPolygon2d> planarRegionConvexPolygons)
   {
      fromLocalToWorldTransform.set(transformToWorld);
      fromWorldToLocalTransform.invert(fromLocalToWorldTransform);
      this.concaveHullVertices = concaveHullVertices;
      convexPolygons = planarRegionConvexPolygons;
      boundingBox3dInWorld.setEpsilonToGrow(DEFAULT_BOUNDING_BOX_EPSILON);
      updateBoundingBox();
      updateConvexHull();
   }

   /**
    * Create a new planar region.
    * @param transformToWorld transform from the region local coordinate system to world.
    * @param convexPolygon a single convex polygon that represents the planar region. Expressed in local coordinate system.
    */
   public PlanarRegion(RigidBodyTransform transformToWorld, ConvexPolygon2d convexPolygon)
   {
      fromLocalToWorldTransform.set(transformToWorld);
      fromWorldToLocalTransform.invert(fromLocalToWorldTransform);
      concaveHullVertices = new ArrayList<>();
      convexPolygons = new ArrayList<>();
      convexPolygons.add(convexPolygon);
      boundingBox3dInWorld.setEpsilonToGrow(DEFAULT_BOUNDING_BOX_EPSILON);
      updateBoundingBox();
      updateConvexHull();
   }

   /**
    * Verify if the given polygon intersects this region projected onto the XY-plane.
    * @param convexPolygon2d
    * @return
    */
   public boolean isPolygonIntersecting(ConvexPolygon2d convexPolygon2d)
   {
      // Instead of projecting all the polygons of this region onto the world XY-plane,
      // the given convex polygon is projected along the z-world axis to be snapped onto plane.
      ConvexPolygon2d projectedPolygon = projectPolygonVerticallyToRegion(convexPolygon2d);
      ConvexPolygon2d dummyPolygon = new ConvexPolygon2d();

      // Now, just need to go through each polygon of this region and see there is at least one intersection
      for (int i = 0; i < getNumberOfConvexPolygons(); i++)
      {
         ConvexPolygon2d polygonToCheck = convexPolygons.get(i);
         boolean hasIntersection = polygonToCheck.intersectionWith(projectedPolygon, dummyPolygon);
         if (hasIntersection)
            return true;
      }
      // Did not find any intersection
      return false;
   }

   /**
    * Returns all of the intersections when the convexPolygon is projected vertically onto this PlanarRegion.
    * @param convexPolygonInWorld Polygon to project vertically.
    * @param intersectionsInPlaneFrameToPack ArrayList of ConvexPolygon2d to pack with the intersections.
    */
   public void getPolygonIntersectionsWhenProjectedVertically(ConvexPolygon2d convexPolygonInWorld, ArrayList<ConvexPolygon2d> intersectionsInPlaneFrameToPack)
   {
      // Instead of projecting all the polygons of this region onto the world XY-plane,
      // the given convex polygon is projected along the z-world axis to be snapped onto plane.
      ConvexPolygon2d projectedPolygon = projectPolygonVerticallyToRegion(convexPolygonInWorld);

      // Now, just need to go through each polygon of this region and see there is at least one intersection
      for (int i = 0; i < getNumberOfConvexPolygons(); i++)
      {
         ConvexPolygon2d intersectingPolygon = convexPolygons.get(i).intersectionWith(projectedPolygon);

         if (intersectingPolygon != null)
         {
            intersectionsInPlaneFrameToPack.add(intersectingPolygon);
         }
      }
   }

   /**
    * Returns all of the intersections when the convexPolygon is snapped onto this PlanarRegion with the snappingTransform.
    * @param convexPolygon2d Polygon to snap.
    * @param snappingTransform RigidBodyTransform that snaps the polygon onto this region. Must have same surface normal as this region.
    * @param intersectionsToPack ArrayList of ConvexPolygon2d to pack with the intersections.
    */
   public void getPolygonIntersectionsWhenSnapped(ConvexPolygon2d convexPolygon2d, RigidBodyTransform snappingTransform,
         ArrayList<ConvexPolygon2d> intersectionsToPack)
   {
      ConvexPolygon2d projectedPolygon = snapPolygonIntoRegionAndChangeFrameToRegionFrame(convexPolygon2d, snappingTransform);

      // Now, just need to go through each polygon of this region and see there is at least one intersection
      for (int i = 0; i < getNumberOfConvexPolygons(); i++)
      {
         ConvexPolygon2d intersectingPolygon = convexPolygons.get(i).intersectionWith(projectedPolygon);

         if (intersectingPolygon != null)
         {
            intersectionsToPack.add(intersectingPolygon);
         }
      }
   }

   /**
    * Snaps the given polygon to the frame of this planar region and then transforms it to be in this planar region.
    * If the snappingTransform is not consistent with this PlanarRegion, then it prints an error message.
    *
    * @param polygonToSnap
    * @param snappingTransform
    * @return ConvexPolygon2d Snapped polygon in the frame of this PlanarRegion.
    */
   public ConvexPolygon2d snapPolygonIntoRegionAndChangeFrameToRegionFrame(ConvexPolygon2d polygonToSnap, RigidBodyTransform snappingTransform)
   {
      RigidBodyTransform fromPolygonToPlanarRegionTransform = new RigidBodyTransform();
      fromPolygonToPlanarRegionTransform.multiply(fromWorldToLocalTransform, snappingTransform);

      double m02 = Math.abs(fromPolygonToPlanarRegionTransform.getM02());
      double m12 = Math.abs(fromPolygonToPlanarRegionTransform.getM12());

      if ((Math.abs(m02) > 1e-4) || (Math.abs(m12) > 1e-4))
      {
         System.err.println("Snapping transform does not seem consistent with PlanarRegion transform!");
      }

      ConvexPolygon2d snappedPolygonToReturn = new ConvexPolygon2d(polygonToSnap);
      snappedPolygonToReturn.applyTransformAndProjectToXYPlane(fromPolygonToPlanarRegionTransform);

      return snappedPolygonToReturn;
   }

   /**
    * Projects the input ConvexPolygon2d to the plane defined by this PlanarRegion by translating each vertex in world z.
    * Then puts each vertex in local frame. In doing so, the area of the rotated polygon will actually increase on tilted PlanarRegions.
    * @param convexPolygonInWorld Polygon to project
    * @return new projected ConvexPolygon2d
    */
   private ConvexPolygon2d projectPolygonVerticallyToRegion(ConvexPolygon2d convexPolygonInWorld)
   {
      ConvexPolygon2d projectedPolygon = new ConvexPolygon2d();

      Point3d snappedVertex3d = new Point3d();

      for (int i = 0; i < convexPolygonInWorld.getNumberOfVertices(); i++)
      {
         Point2d originalVertex = convexPolygonInWorld.getVertex(i);
         // Find the vertex 3d that is snapped to the plane following z-world.
         snappedVertex3d.setX(originalVertex.getX());
         snappedVertex3d.setY(originalVertex.getY());
         snappedVertex3d.setZ(getPlaneZGivenXY(originalVertex.getX(), originalVertex.getY()));

         // Transform to local coordinates
         fromWorldToLocalTransform.transform(snappedVertex3d);
         // Add the snapped vertex to the snapped polygon
         projectedPolygon.addVertex(snappedVertex3d.getX(), snappedVertex3d.getY());
      }
      projectedPolygon.update();
      return projectedPolygon;
   }

   /**
    * Computes if the point is in the region projected onto the world xy-plane.
    * Note that the z-coordinate of the query is ignored.
    * @param point3d query coordinates.
    * @return true if the point is inside this region, false otherwise.
    */
   public boolean isPointInsideByProjectionOntoXYPlane(Point3d point3d)
   {
      return isPointInsideByProjectionOntoXYPlane(point3d.getX(), point3d.getY());
   }

   /**
    * Computes if the point is in the region projected onto the world xy-plane.
    * @param point2d query coordinates.
    * @return true if the point is inside this region, false otherwise.
    */
   public boolean isPointInsideByProjectionOntoXYPlane(Point2d point2d)
   {
      return isPointInsideByProjectionOntoXYPlane(point2d.getX(), point2d.getY());
   }

   /**
    * Computes if the point is in the region projected onto the world xy-plane.
    * @param x x-coordinate of the query.
    * @param y y-coordinate of the query.
    * @return true if the point is inside this region, false otherwise.
    */
   public boolean isPointInsideByProjectionOntoXYPlane(double x, double y)
   {
      Point3d localPoint = new Point3d();
      localPoint.setX(x);
      localPoint.setY(y);
      localPoint.setZ(getPlaneZGivenXY(x, y));

      fromWorldToLocalTransform.transform(localPoint);

      return isPointInside(new Point2d(localPoint.getX(), localPoint.getY()));
   }

   /**
    * Given a 3D point in world coordinates, computes whether the point is in this region.
    * @param point3dInWorld query expressed in world coordinates.
    * @param maximumOrthogonalDistance tolerance expressed as maximum orthogonal distance from the region.
    * @return true if the point is inside this region, false otherwise.
    */
   public boolean isPointInside(Point3d point3dInWorld, double maximumOrthogonalDistance)
   {
      Point3d localPoint = new Point3d();
      fromWorldToLocalTransform.transform(point3dInWorld, localPoint);

      if (!MathTools.isInsideBoundsInclusive(localPoint.getZ(), maximumOrthogonalDistance))
         return false;
      else
         return isPointInside(new Point2d(localPoint.getX(), localPoint.getY()));
   }

   /**
    * Given a 2D point expressed in the plane local frame, computes whether the point is in this region.
    * @param point2dInLocal query expressed in local coordinates.
    * @return true if the point is inside this region, false otherwise.
    */
   public boolean isPointInside(Point2d point2dInLocal)
   {
      for (int i = 0; i < convexPolygons.size(); i++)
      {
         if (convexPolygons.get(i).isPointInside(point2dInLocal))
            return true;
      }
      return false;
   }

   /**
    * Computes the z-coordinate in world of the plane for a given xy-coordinates in world.
    * @param xWorld x-coordinate of the query
    * @param yWorld y-coordinate of the query
    * @return the z-coordinate
    */
   public double getPlaneZGivenXY(double xWorld, double yWorld)
   {
      // The three components of the plane origin
      double x0 = fromLocalToWorldTransform.getM03();
      double y0 = fromLocalToWorldTransform.getM13();
      double z0 = fromLocalToWorldTransform.getM23();
      // The three components of the plane normal
      double a = fromLocalToWorldTransform.getM02();
      double b = fromLocalToWorldTransform.getM12();
      double c = fromLocalToWorldTransform.getM22();

      // Given the plane equation: a*x + b*y + c*z + d = 0, with d = -(a*x0 + b*y0 + c*z0), we find z:
      double z = a / c * (x0 - xWorld) + b / c * (y0 - yWorld) + z0;
      return z;
   }

   /**
    * Every can be given a unique. The default value is {@value #NO_REGION_ID} which corresponds to no id.
    * @param regionId set the unique id of this region.
    */
   public void setRegionId(int regionId)
   {
      this.regionId = regionId;
   }

   /**
    * @return the unique id of this regions. It is equal to {@value #NO_REGION_ID} when no id has been attributed.
    */
   public int getRegionId()
   {
      return regionId;
   }

   /**
    * @return whether a unique id has been attributed to this region or not.
    */
   public boolean hasARegionId()
   {
      return regionId != NO_REGION_ID;
   }

   /**
    * Returns true only if there is no polygons in this planar region. Does not check for empty polygons.
    */
   public boolean isEmpty()
   {
      return convexPolygons.isEmpty();
   }

   public List<Point2d> getConcaveHull()
   {
      return concaveHullVertices;
   }

   /** Returns the number of convex polygons representing this region. */
   public int getNumberOfConvexPolygons()
   {
      return convexPolygons.size();
   }

   /**
    * Returns the i<sup>th</sup> convex polygon representing a portion of this region.
    * The polygon is expressed in the region local coordinates.
    */
   public ConvexPolygon2d getConvexPolygon(int i)
   {
      return convexPolygons.get(i);
   }

   /**
    * Returns the last convex polygon representing a portion of this region.
    * Special case: returns null when this region is empty.
    * The polygon is expressed in the region local coordinates.
    */
   public ConvexPolygon2d getLastConvexPolygon()
   {
      if (isEmpty())
         return null;
      else
         return getConvexPolygon(getNumberOfConvexPolygons() - 1);
   }

   /**
    * Returns the i<sup>th</sup> convex polygon representing a portion of this region and removes it from this planar region.
    * The polygon is expressed in the region local coordinates.
    */
   public ConvexPolygon2d pollConvexPolygon(int i)
   {
      ConvexPolygon2d polledPolygon = convexPolygons.remove(i);
      updateBoundingBox();
      updateConvexHull();
      return polledPolygon;
   }

   /**
    * Returns the last convex polygon representing a portion of this region and removes it from this planar region.
    * Special case: returns null when this region is empty.
    * The polygon is expressed in the region local coordinates.
    */
   public ConvexPolygon2d pollLastConvexPolygon()
   {
      if (isEmpty())
         return null;
      else
         return pollConvexPolygon(getNumberOfConvexPolygons() - 1);
   }

   /**
    * Retrieves the normal of this planar region and stores it in the given {@link Vector3d}.
    * @param normalToPack used to store the normal of this planar region.
    */
   public void getNormal(Vector3d normalToPack)
   {
      normalToPack.setX(fromLocalToWorldTransform.getM02());
      normalToPack.setY(fromLocalToWorldTransform.getM12());
      normalToPack.setZ(fromLocalToWorldTransform.getM22());
   }

   /**
    * Retrieves the normal of this planar region and stores it in the given {@link Vector3f}.
    * @param normalToPack used to store the normal of this planar region.
    */
   public void getNormal(Vector3f normalToPack)
   {
      normalToPack.setX((float) fromLocalToWorldTransform.getM02());
      normalToPack.setY((float) fromLocalToWorldTransform.getM12());
      normalToPack.setZ((float) fromLocalToWorldTransform.getM22());
   }

   /**
    * Returns true if this PlanarRegion is purely vertical, as far as numerical roundoff is concerned.
    * Checks z component of surface normal. If absolute value is really small, then returns true.
    * @return true if vertical. false otherwise.
    */
   public boolean isVertical()
   {
      return (Math.abs(fromLocalToWorldTransform.getM22()) < 1e-10);
   }

   /**
    * Retrieves a point that lies in this planar region.
    * This point is also used as the origin of the local coordinate system of this planar region.
    * @param pointToPack used to store the point coordinates.
    */
   public void getPointInRegion(Point3d pointToPack)
   {
      fromLocalToWorldTransform.getTranslation(pointToPack);
   }

   /**
    * Retrieves a point that lies in this planar region.
    * This point is also used as the origin of the local coordinate system of this planar region.
    * @param pointToPack used to store the point coordinates.
    */
   public void getPointInRegion(Point3f pointToPack)
   {
      fromLocalToWorldTransform.getTranslation(pointToPack);
   }

   /**
    * Get the transform from local coordinates to world coordinates.
    * @param transformToPack used to store the transform.
    */
   public void getTransformToWorld(RigidBodyTransform transformToPack)
   {
      transformToPack.set(fromLocalToWorldTransform);
   }

   /**
    * Get a reference to the PlanarRegion's axis-aligned minimal bounding box (AABB) in world.
    * @return the axis-aligned minimal bounding box for the planar region, in world coordinates.
    */
   public BoundingBox3d getBoundingBox3dInWorld()
   {
      return this.boundingBox3dInWorld;
   }

   /**
    * Get a deep copy of this PlanarRegion's axis-aligned minimal bounding box (AABB) in world
    * @return a deep copy of the axis-aligned minimal bounding box for the planar region, in world coordinates.
    */
   public BoundingBox3d getBoundingBox3dInWorldCopy()
   {
      return new BoundingBox3d(this.boundingBox3dInWorld);
   }

   /**
    * Set defining points of the passed-in BoundingBox3d to the same as
    * those in this PlanarRegion's axis-aligned minimal bounding box (AABB) in world coordinates.
    *
    * @param boundingBox3dToPack the bounding box that will be updated to reflect this PlanarRegion's AABB
    */
   public void getBoundingBox3dInWorld(BoundingBox3d boundingBox3dToPack)
   {
      boundingBox3dToPack.set(this.boundingBox3dInWorld);
   }

   public boolean epsilonEquals(PlanarRegion other, double epsilon)
   {
      if (!fromLocalToWorldTransform.epsilonEquals(other.fromLocalToWorldTransform, epsilon))
         return false;
      // Not necessary, but just in case
      if (!fromWorldToLocalTransform.epsilonEquals(other.fromWorldToLocalTransform, epsilon))
         return false;

      if (getNumberOfConvexPolygons() != other.getNumberOfConvexPolygons())
         return false;

      for (int i = 0; i < getNumberOfConvexPolygons(); i++)
      {
         if (!convexPolygons.get(i).epsilonEquals(other.convexPolygons.get(i), epsilon))
            return false;
      }
      return true;
   }

   public void set(PlanarRegion other)
   {
      fromLocalToWorldTransform.set(other.fromLocalToWorldTransform);
      fromWorldToLocalTransform.set(other.fromWorldToLocalTransform);
      convexPolygons.clear();
      for (int i = 0; i < other.getNumberOfConvexPolygons(); i++)
         convexPolygons.add(new ConvexPolygon2d(other.convexPolygons.get(i)));

      updateBoundingBox();
      convexHull.setAndUpdate(other.convexHull);
   }

   private void updateBoundingBox()
   {
      boundingBox3dInWorld.set(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
      for (int i = 0; i < this.getNumberOfConvexPolygons(); i++)
      {
         ConvexPolygon2d convexPolygon = this.getConvexPolygon(i);

         for (int j = 0; j < convexPolygon.getNumberOfVertices(); j++)
         {
            Point2d vertex = convexPolygon.getVertex(j);
            tempPointForConvexPolygonProjection.set(vertex.x, vertex.y, 0.0);
            fromLocalToWorldTransform.transform(tempPointForConvexPolygonProjection);

            this.boundingBox3dInWorld.updateToIncludePoint(tempPointForConvexPolygonProjection);
         }
      }
   }

   private void updateConvexHull()
   {
      convexHull.clear();
      for (int i = 0; i < this.getNumberOfConvexPolygons(); i++)
      {
         ConvexPolygon2d convexPolygon = this.getConvexPolygon(i);
         for (int j = 0; j < convexPolygon.getNumberOfVertices(); j++)
            convexHull.addVertex(convexPolygon.getVertex(j));
      }
      convexHull.update();
   }

   /**
    * @return a full depth copy of this region. The copy can be entirely modified without interfering with this region.
    */
   public PlanarRegion copy()
   {
      RigidBodyTransform transformToWorldCopy = new RigidBodyTransform(fromLocalToWorldTransform);
      List<Point2d> concaveHullCopy = new ArrayList<>();
      for (int i = 0; i < concaveHullVertices.size(); i++)
         concaveHullCopy.add(new Point2d(concaveHullVertices.get(i)));

      List<ConvexPolygon2d> convexPolygonsCopy = new ArrayList<>();
      for (int i = 0; i < getNumberOfConvexPolygons(); i++)
         convexPolygonsCopy.add(new ConvexPolygon2d(convexPolygons.get(i)));

      PlanarRegion planarRegion = new PlanarRegion(transformToWorldCopy, convexPolygonsCopy);
      planarRegion.setRegionId(regionId);
      return planarRegion;
   }

   /**
    * @return the convex hull of the region.
    */
   public ConvexPolygon2d getConvexHull()
   {
      return convexHull;
   }
}