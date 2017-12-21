package us.ihmc.pathPlanning.visibilityGraphs.interfaces;

import us.ihmc.commons.MathTools;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.pathPlanning.visibilityGraphs.ConnectionPoint3D;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster;
import us.ihmc.pathPlanning.visibilityGraphs.tools.PlanarRegionTools;
import us.ihmc.robotics.geometry.PlanarRegion;

public interface VisibilityGraphsParameters
{
   public int getNumberOfForcedConnections();

   public double getMaxInterRegionConnectionLength();

   public double getNormalZThresholdForAccessibleRegions();

   public double getExtrusionDistance();

   public double getExtrusionDistanceIfNotTooHighToStep();

   public double getTooHighToStepDistance();

   public double getClusterResolution();

   default double getExplorationDistanceFromStartGoal()
   {
      return Double.POSITIVE_INFINITY;
   }

   default double getPlanarRegionMinArea()
   {
      return 0.0;
   }

   default int getPlanarRegionMinSize()
   {
      return 0;
   }

   default double getMaxDistanceToProjectStartGoalToClosestRegion()
   {
      return 0.15;
   }

   /**
    * Defines the angle from which two regions are considered orthogonal.
    * <p>
    * It is used to determine if a region should be projected onto another as a polygon or a line.
    * </p>
    * <p>
    * It should be close to 90 degrees.
    * </p>
    * 
    * @return the angle threshold to use to determine if a line or polygon projection method should
    *         be used.
    */
   default double getRegionOrthogonalAngle()
   {
      return Math.toRadians(75.0);
   }

   default ExtrusionDistanceCalculator getExtrusionDistanceCalculator()
   {
      return new ExtrusionDistanceCalculator()
      {
         @Override
         public double computeExtrusionDistance(Cluster clusterInExtrusion, Point2DReadOnly pointToExtrude, double obstacleHeight)
         {
            if (obstacleHeight < 0.0)
               return 0.0;
            if (obstacleHeight <= getTooHighToStepDistance())
            {
               double alpha = obstacleHeight / getTooHighToStepDistance();
               return EuclidCoreTools.interpolate(getExtrusionDistanceIfNotTooHighToStep(), getExtrusionDistance(), alpha);
            }
            return getExtrusionDistance();
         }
      };
   }

   default NavigableRegionFilter getNavigableRegionFilter()
   {
      return new NavigableRegionFilter()
      {
         @Override
         public boolean isPlanarRegionNavigable(PlanarRegion query)
         {
            return Math.abs(query.getNormal().getZ()) >= getNormalZThresholdForAccessibleRegions();
         }
      };
   }

   default InterRegionConnectionFilter getInterRegionConnectionFilter()
   {
      return new InterRegionConnectionFilter()
      {
         private final double maxLengthSquared = MathTools.square(getMaxInterRegionConnectionLength());
         private final double maxDeltaHeight = getTooHighToStepDistance();

         @Override
         public boolean isConnectionValid(ConnectionPoint3D source, ConnectionPoint3D target)
         {
            if (Math.abs(source.getZ() - target.getZ()) > maxDeltaHeight)
               return false;
            if (source.distanceSquared(target) > maxLengthSquared)
               return false;

            return true;
         }
      };
   }

   default PlanarRegionFilter getPlanarRegionFilter()
   {
      return new PlanarRegionFilter()
      {
         @Override
         public boolean isPlanarRegionRelevant(PlanarRegion region)
         {
            if (region.getConcaveHullSize() < getPlanarRegionMinSize())
               return false;
            if (!Double.isFinite(getPlanarRegionMinArea()) || getPlanarRegionMinArea() <= 0.0)
               return true;
            return PlanarRegionTools.computePlanarRegionArea(region) >= getPlanarRegionMinArea();
         }
      };
   }
}
