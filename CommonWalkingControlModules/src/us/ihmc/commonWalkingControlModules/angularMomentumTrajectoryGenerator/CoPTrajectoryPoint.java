package us.ihmc.commonWalkingControlModules.angularMomentumTrajectoryGenerator;

import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.trajectories.waypoints.YoFrameEuclideanTrajectoryPoint;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.yoVariables.listener.VariableChangedListener;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoVariable;

/**
 * This class is a hack to provide all setter methods of {@link YoFramePointInMultipleFrames} for {@link YoFrameEuclideanTrajectoryPoint}
 * and then some more ;)
 *
 */
public class CoPTrajectoryPoint extends YoFrameEuclideanTrajectoryPoint
{  
   protected final YoVariableRegistry registry;
   FramePoint localPosition = new FramePoint(); // dummy variable to hand out data with    
   private YoFramePoint yoFramePointInWorld;
   
   
   public CoPTrajectoryPoint(String namePrefix, String nameSuffix, YoVariableRegistry registry, ReferenceFrame[] referenceFrames)
   {
      super(namePrefix, nameSuffix, registry, referenceFrames);
      this.registry = registry;
   }
   
   public FramePoint getFrameTuple()
   {
      getPositionIncludingFrame(localPosition);
      return localPosition;
   }
   
   public void getFrameTuple2dIncludingFrame(FramePoint2d pointToPack)
   {
      pointToPack.setIncludingFrame(getPosition().getReferenceFrame(), getPosition().getX(), getPosition().getY());
   }
   
   public void set(FramePoint position)
   {
      getPosition().set(position);
      putYoValuesIntoFrameWaypoint();
   }
   
   public void set(YoFramePoint position)
   {
      getPosition().set(position);
      putYoValuesIntoFrameWaypoint();
   }
   
   public void set(CoPTrajectoryPoint position)
   {
      getPosition().set(position.getPosition());
      putYoValuesIntoFrameWaypoint();
   }
   
   public void setIncludingFrame(FramePoint position)
   {      
      switchCurrentReferenceFrame(position.getReferenceFrame());
      getPosition().set(position);
      putYoValuesIntoFrameWaypoint();
   }
   
   public void setIncludingFrame(YoFramePoint position)
   {
      switchCurrentReferenceFrame(position.getReferenceFrame());
      getPosition().set(position);
      putYoValuesIntoFrameWaypoint();
   }
   
   public void setIncludingFrame(CoPTrajectoryPoint position)
   {
      switchCurrentReferenceFrame(position.getReferenceFrame());
      getPosition().set(position.getFrameTuple());
      putYoValuesIntoFrameWaypoint();
   }
      
   public boolean epsilonEquals(FramePoint2d point, double threshold)
   {
      return getPosition().epsilonEquals(point, threshold);
   }
   
   @Override
   public String toString()
   {
      return "Time: " + getTime() + " Location: " + getPosition().toString();
   }   
   
   
   public YoFramePoint buildUpdatedYoFramePointForVisualizationOnly()
   {
      if(!isReferenceFrameRegistered(ReferenceFrame.getWorldFrame()))
         registerReferenceFrame(ReferenceFrame.getWorldFrame());
      yoFramePointInWorld = new YoFramePoint(super.getNamePrefix() + "Viz", getReferenceFrame(), registry);
      getPosition().attachVariableChangedListener(new VariableChangedListener()
      {         
         private final FramePoint localFramePoint = new FramePoint();
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            getPosition().getFrameTupleIncludingFrame(localFramePoint);
            yoFramePointInWorld.setAndMatchFrame(localFramePoint);
         }
      });
      return yoFramePointInWorld;
   }
   
   public void notifyVariableChangedListeners()
   {
      getPosition().notifyVariableChangedListeners();
   }   
}
