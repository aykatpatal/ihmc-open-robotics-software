package us.ihmc.humanoidRobotics.communication.toolbox.heightQuadTree.command;

import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.TrackablePacket;

public class HeightQuadTreeToolboxRequestMessage extends TrackablePacket<HeightQuadTreeToolboxRequestMessage>
{
   public boolean requestClearQuadTree;
   public boolean requestQuadTreeUpdate;

   public HeightQuadTreeToolboxRequestMessage()
   {
   }

   public static HeightQuadTreeToolboxRequestMessage clearRequest(PacketDestination destination)
   {
      HeightQuadTreeToolboxRequestMessage clearMessage = new HeightQuadTreeToolboxRequestMessage();
      clearMessage.setDestination(destination);
      clearMessage.requestClearQuadTree = true;
      clearMessage.requestQuadTreeUpdate = false;
      return clearMessage;
   }

   public static HeightQuadTreeToolboxRequestMessage requestQuadTreeUpdate(PacketDestination destination)
   {
      HeightQuadTreeToolboxRequestMessage clearMessage = new HeightQuadTreeToolboxRequestMessage();
      clearMessage.setDestination(destination);
      clearMessage.requestClearQuadTree = false;
      clearMessage.requestQuadTreeUpdate = true;
      return clearMessage;
   }

   public boolean isClearQuadTreeRequested()
   {
      return requestClearQuadTree;
   }

   public boolean isQuadTreeUpdateRequested()
   {
      return requestQuadTreeUpdate;
   }

   @Override
   public boolean epsilonEquals(HeightQuadTreeToolboxRequestMessage other, double epsilon)
   {
      if (requestClearQuadTree != other.requestClearQuadTree)
         return false;
      if (requestQuadTreeUpdate != other.requestQuadTreeUpdate)
         return false;
      return true;
   }
}