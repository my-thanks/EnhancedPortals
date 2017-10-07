package enhancedportals.portal;

import cpw.mods.fml.common.FMLCommonHandler;

import enhancedportals.tile.TileController;
import enhancedportals.tile.TilePortalManipulator;
import enhancedportals.portal.EntityManager;

import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;


import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.common.util.ForgeDirection;

import com.flansmod.common.vector.Vector3f;
import com.flansmod.common.RotatedAxes;

import com.flansmod.common.driveables.DriveableData;
import com.flansmod.common.driveables.DriveableType;
import com.flansmod.common.driveables.DriveablePosition;
import com.flansmod.common.driveables.PlaneType;
import com.flansmod.common.driveables.VehicleType;
import com.flansmod.common.driveables.mechas.MechaType;

import com.flansmod.common.driveables.ItemPlane;
import com.flansmod.common.driveables.ItemVehicle;
import com.flansmod.common.driveables.mechas.ItemMecha;

import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EntityPlane;
import com.flansmod.common.driveables.EntityVehicle;
import com.flansmod.common.driveables.EntityWheel;
import com.flansmod.common.driveables.EntitySeat;
import com.flansmod.common.driveables.mechas.EntityMecha;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.HashMap;

public class EntityManagerFlans
{
	
	//public static Entity[] passengers;

	public static boolean checkEntityType(Entity entity)
	{
		if (entity instanceof EntitySeat)
		{			
			return true;
		} else if (entity instanceof EntityWheel || entity.ridingEntity instanceof EntitySeat)
		{
			return true;
		} else
		{
			return false;
		}
	}

	public static boolean checkDriveableType(Entity entity)
	{	
		if (entity instanceof EntityDriveable)
		{
			return true;
		} else
		{
			return false;
		}	
	} 
	
	public static Entity transferDriveable(Entity entity, TileController entry, TileController exit) throws PortalException
	{
		//Get exit coordinates and rotation
		ChunkCoordinates exitLoc = EntityManager.getActualExitLocation(entity, exit);
		
		float calcRotationAngle = EntityManager.getRotation(entity, exit, exitLoc);
		calcRotationAngle += 90F;
		
		if (calcRotationAngle > 180F)
			calcRotationAngle = -(360F - calcRotationAngle);
		
		ChunkCoordinates exitForDriveable = getExitDriveableCoords(entity, exitLoc, exit, calcRotationAngle);
		
		//We must check rotation again
		calcRotationAngle = EntityManager.getRotation(entity, exit, exitLoc);
		calcRotationAngle += 90F;
		
		if (calcRotationAngle > 180F)
			calcRotationAngle = -(360F - calcRotationAngle);
		
		if (exitForDriveable == null) return entity;
		
            boolean keepMomentum = false;
            TilePortalManipulator manip = exit.getModuleManipulator();

            if (manip != null)
            {
                keepMomentum = manip.shouldKeepMomentumOnTeleport();
            }

            int instability = exit.getDimensionalBridgeStabilizer().instability;
			
			
			if (!EntityManager.isEntityFitForTravel(entity))
			{
				return entity;
			}				
			
			return transferDriveableEntityWithPassengers(entity, exitForDriveable.posX, exitForDriveable.posY, exitForDriveable.posZ, calcRotationAngle, (WorldServer) exit.getWorldObj(), entry.portalType, exit.portalType, keepMomentum, instability);
        			
	
	}
	
	static Entity transferDriveableEntityWithPassengers(Entity entity, double x, double y, double z, float yaw, WorldServer world, int touchedPortalType, int exitPortalType, boolean keepMomentum, int instability)
    {
		Entity transferedEntity = entity;
		
		if (entity instanceof EntityVehicle)
		{
			transferedEntity = transferEntityVehicleWithPassengers(entity, x, y, z, yaw, world, touchedPortalType, exitPortalType, keepMomentum, instability);
		} else if (entity instanceof EntityPlane)
		{
			transferedEntity = transferEntityPlaneWithPassengers(entity, x, y, z, yaw, world, touchedPortalType, exitPortalType, keepMomentum, instability);
		} else if (entity instanceof EntityMecha)
		{
			transferedEntity = transferEntityMechaWithPassengers(entity, x, y, z, yaw, world, touchedPortalType, exitPortalType, keepMomentum, instability);
		} else transferedEntity = null;		
		
		return transferedEntity;
	}
	
	static Entity transferEntityVehicleWithPassengers(Entity entity, double x, double y, double z, float yaw, WorldServer world, int touchedPortalType, int exitPortalType, boolean keepMomentum, int instability)
    {
        EntityVehicle transferTarget = (EntityVehicle) entity;
		EntityDriveable driveable = (EntityDriveable) entity;
		
		Vector3f keepMoments = getActualMoments(entity, yaw);
		
		Entity[] passengers = new Entity[transferTarget.seats.length];

		// Transfer riders
		for (int i = 0; i < transferTarget.seats.length; i++)
		{
			if(transferTarget.seats[i] != null)
			{
				passengers[i] = transferTarget.seats[i].riddenByEntity;
				
				if(passengers[i] != null)
				{
					passengers[i].mountEntity(null);
					passengers[i] = EntityManager.transferEntityWithRider(passengers[i], x, y, z, yaw, world, touchedPortalType, exitPortalType, keepMomentum, instability); // yaw - 1.57F
				}
			}
		}
			
		// Transfer the entity.		
		VehicleType type = transferTarget.getVehicleType();
		ItemStack vehicleStack = new ItemStack(type.item, 1, 0);
		vehicleStack.stackTagCompound = new NBTTagCompound();
		transferTarget.driveableData.writeToNBT(vehicleStack.stackTagCompound);
	 	transferTarget.setDead();
		ItemVehicle vehItem = (ItemVehicle) vehicleStack.getItem();		
		DriveableData data = vehItem.getData(vehicleStack, world);
		
    	if(data != null)
    	{
			entity = new EntityVehicle(world, x, y, z, vehItem.type, data);
			if(!world.isRemote)
			{
				transferTarget = (EntityVehicle) entity;
				driveable = (EntityDriveable) entity;
				transferTarget.rotateYaw(yaw);
				world.spawnEntityInWorld(entity);
			}
    	}
		for(EntityWheel wheel : transferTarget.wheels)
		{
			if(wheel == null)
				continue;
			transferTarget.allWheelsOnGround = true;
			transferTarget.throttle = keepMoments.y;
			wheel.motionX = (double) keepMoments.x;
			wheel.motionZ = (double) keepMoments.z;
			wheel.moveEntity(wheel.motionX, wheel.motionY, wheel.motionZ);
		}
		//transferTarget.setVelocity((double) keepMoments.x, 0D, (double) keepMoments.z);
		
		
		
		// Remount entity with riders.
        for (int i = 0; i < transferTarget.seats.length; i++)
		{
			if(transferTarget.seats[i] != null)
			{
				if(passengers[i] != null)
				{
					passengers[i].mountEntity(transferTarget.seats[i]);
					EntityManager.setEntityPortalCooldown(passengers[i]);
					passengers[i] = null;
				}
			}
		}
        
		EntityManager.setEntityPortalCooldown(entity);
		return entity;
    }
	
	static Entity transferEntityPlaneWithPassengers(Entity entity, double x, double y, double z, float yaw, WorldServer world, int touchedPortalType, int exitPortalType, boolean keepMomentum, int instability)
    {
        EntityPlane transferTarget = (EntityPlane) entity;
		EntityDriveable driveable = (EntityDriveable) entity;
		
		Vector3f keepMoments = getActualMoments(entity, yaw);
		
		Entity[] passengers = new Entity[transferTarget.seats.length];

		// Transfer riders
		for (int i = 0; i < transferTarget.seats.length; i++)
		{
			if(transferTarget.seats[i] != null)
			{
					passengers[i] = transferTarget.seats[i].riddenByEntity;
				
				if(passengers[i] != null)
				{
					passengers[i].mountEntity(null);
					passengers[i] = EntityManager.transferEntityWithRider(passengers[i], x, y, z, yaw, world, touchedPortalType, exitPortalType, keepMomentum, instability);
				}
			}
				
		}
		
		// Transfer the entity.
		
		PlaneType type = transferTarget.getPlaneType();
		ItemStack planeStack = new ItemStack(type.item, 1, 0);
		planeStack.stackTagCompound = new NBTTagCompound();
		transferTarget.driveableData.writeToNBT(planeStack.stackTagCompound);
	 	transferTarget.setDead();
		ItemPlane plaItem = (ItemPlane) planeStack.getItem();
		DriveableData data = plaItem.getPlaneData(planeStack, world);
		
    	if(data != null)
    	{
	    	entity = new EntityPlane(world, x, y, z, plaItem.type, data);
	    	if(!world.isRemote)
	        {
				transferTarget = (EntityPlane) entity;
				driveable = (EntityDriveable) entity;
				transferTarget.rotateYaw(yaw);
				world.spawnEntityInWorld(entity);
	        }
    	}
		
		transferTarget.setVelocity((double) keepMoments.x, 0D, (double) keepMoments.z);
		transferTarget.throttle = keepMoments.y;
		
		// Remount entity with riders.
        for (int i = 0; i < transferTarget.seats.length; i++)
		{
			if(transferTarget.seats[i] != null)
			{
				if(passengers[i] != null)
				{
					passengers[i].mountEntity(transferTarget.seats[i]);
					EntityManager.setEntityPortalCooldown(passengers[i]);
					passengers[i] = null;
				}
			}
		}
		
		EntityManager.setEntityPortalCooldown(entity);
        return entity;
    }
	
	static Entity transferEntityMechaWithPassengers(Entity entity, double x, double y, double z, float yaw, WorldServer world, int touchedPortalType, int exitPortalType, boolean keepMomentum, int instability)
    {
        EntityMecha transferTarget = (EntityMecha) entity;
		EntityDriveable driveable = (EntityDriveable) entity;
		
		Vector3f keepMoments = getActualMoments(entity, yaw);
		
		Entity[] passengers = new Entity[transferTarget.seats.length];

		// Transfer riders
		for (int i = 0; i < transferTarget.seats.length; i++)
			{
				if(transferTarget.seats[i] != null)
				{
					passengers[i] = transferTarget.seats[i].riddenByEntity;
				
					if(passengers[i] != null)
					{
						passengers[i].mountEntity(null);
						passengers[i] = EntityManager.transferEntityWithRider(passengers[i], x, y, z, yaw, world, touchedPortalType, exitPortalType, keepMomentum, instability);
					}
				}			
			}
		// Transfer the entity.
		MechaType type = transferTarget.getMechaType();	
		ItemStack mechaStack = new ItemStack(type.item, 1, 0);
		mechaStack.stackTagCompound = new NBTTagCompound();
		transferTarget.driveableData.writeToNBT(mechaStack.stackTagCompound);
		transferTarget.inventory.writeToNBT(mechaStack.stackTagCompound);
	 	transferTarget.setDead();
		
		ItemMecha mecItem = (ItemMecha) mechaStack.getItem();
		DriveableData data = mecItem.getData(mechaStack, world);
    	if(data != null)
    	{
	    	entity = new EntityMecha(world, x, y, z, mecItem.type, data, mechaStack.stackTagCompound);
	    	if(!world.isRemote)
	        {
				transferTarget = (EntityMecha) entity;
				driveable = (EntityDriveable) entity;
				transferTarget.rotateYaw(yaw);
				world.spawnEntityInWorld(entity);
	        }
	    	
    	}
		
		transferTarget.setVelocity((double) keepMoments.x, 0D, (double) keepMoments.z);
		//transferTarget.throttle = keepMoments.y;
		
		// Remount entity with riders.
        for (int i = 0; i < transferTarget.seats.length; i++)
			{
				if(transferTarget.seats[i] != null)
				{
					if(passengers[i] != null)
					{
						passengers[i].mountEntity(transferTarget.seats[i]);
						EntityManager.setEntityPortalCooldown(passengers[i]);
						passengers[i] = null;
					}
				}
			}
		EntityManager.setEntityPortalCooldown(entity);
        return entity;
    }
	
	public static ChunkCoordinates getExitDriveableCoords(Entity entityForTransport, ChunkCoordinates dirtLoc, TileController portExController, float actualRotationAngle)
	{
		
		World world = portExController.getWorldObj();
		ArrayList<ChunkCoordinates> portBlocks = portExController.getPortals();
		EntityDriveable entDrvLocal = (EntityDriveable) entityForTransport;
		
		int minX = dirtLoc.posX;
		int minY = dirtLoc.posY;
		int minZ = dirtLoc.posZ;
		int maxX = dirtLoc.posX;
		int maxY = dirtLoc.posY;
		int maxZ = dirtLoc.posZ;
		
		if (portBlocks != null && portBlocks.get(0) != null)
			for (int i = 0; i < portBlocks.size(); i++)
			{
				maxX = Math.max(portBlocks.get(i).posX, maxX);
				minX = Math.min(portBlocks.get(i).posX, minX);
				maxY = Math.max(portBlocks.get(i).posY, maxY);
				minY = Math.min(portBlocks.get(i).posY, minY);
				maxZ = Math.max(portBlocks.get(i).posZ, maxZ);
				minZ = Math.min(portBlocks.get(i).posZ, minZ);
			}
		
		
		int avCoordX = Math.abs(maxX - minX);
		int avCoordY = Math.abs(maxY - minY);
		int avCoordZ = Math.abs(maxZ - minZ);
		
		int midCoordX = minX + Math.round(avCoordX / 2);
		int midCoordY = minY + Math.round(avCoordY / 2);
		int midCoordZ = minZ + Math.round(avCoordZ / 2);
		
		if (!(entityForTransport instanceof EntityPlane))
			midCoordY = minY + 2;	
		ChunkCoordinates calcExLocationTest = new ChunkCoordinates(midCoordX, midCoordY, midCoordZ);
		
		
		boolean canDriveableTravel = false;
		double drvTestRot = entDrvLocal.serverYaw;
		
		float actualRotationAngleNeg = actualRotationAngle + 180F;
		if (actualRotationAngleNeg > 180F)
			actualRotationAngleNeg = -(360F - actualRotationAngleNeg);					
		
		
		switch(portExController.portalType)
		{
			case 1 :
					{
						for (int i = 0; i < 100; i++)
						{
							if (actualRotationAngle == 90F) calcExLocationTest.posZ++;
								else calcExLocationTest.posZ--;
							canDriveableTravel = !getDriveablePortalDamage(entDrvLocal, calcExLocationTest, actualRotationAngle, world);
							if(canDriveableTravel)
								break;
						}
						if(!canDriveableTravel)
						{
							actualRotationAngle = actualRotationAngleNeg;
							for (int i = 0; i < 100; i++)
							{
								if (actualRotationAngle == 90F) calcExLocationTest.posZ++;
										else calcExLocationTest.posZ--;
								canDriveableTravel = !getDriveablePortalDamage(entDrvLocal, calcExLocationTest, actualRotationAngle, world);
								if(canDriveableTravel)
									break;
							}
						}
						break;
					}
			case 2 :
					{
						for (int i = 0; i < 100; i++)
						{
							if (actualRotationAngle == 0F) calcExLocationTest.posX++;
										else calcExLocationTest.posX--;
							canDriveableTravel = !getDriveablePortalDamage(entDrvLocal, calcExLocationTest, actualRotationAngle, world);
							if(canDriveableTravel)
								break;
						}
						if(!canDriveableTravel)
						{
							actualRotationAngle = actualRotationAngleNeg;
							for (int i = 0; i < 100; i++)
							{								
								if (actualRotationAngle == 0F) calcExLocationTest.posX++;
										else calcExLocationTest.posX--;
								canDriveableTravel = !getDriveablePortalDamage(entDrvLocal, calcExLocationTest, actualRotationAngle, world);
								if(canDriveableTravel)
									break;
							}
						}
						break;
					}
			
			
			case 4 :
					{
						for (int i = 0; i < 100; i++)
						{
							if (actualRotationAngle == -45F)
							{
								calcExLocationTest.posX++;
								calcExLocationTest.posZ++;
							}
							else
							{
								calcExLocationTest.posX--;
								calcExLocationTest.posZ--;
							}
							canDriveableTravel = !getDriveablePortalDamage(entDrvLocal, calcExLocationTest, actualRotationAngle, world);
							if(canDriveableTravel)
								break;
						}
						if(!canDriveableTravel)
						{
							actualRotationAngle = actualRotationAngleNeg;
							for (int i = 0; i < 100; i++)
							{
								if (actualRotationAngle == -45F)
								{
									calcExLocationTest.posX++;
									calcExLocationTest.posZ++;
								}
								else
								{
									calcExLocationTest.posX--;
									calcExLocationTest.posZ--;
								}
								canDriveableTravel = !getDriveablePortalDamage(entDrvLocal, calcExLocationTest, actualRotationAngle, world);
								if(canDriveableTravel)
									break;
							}
						}
						break;
					}
			case 5 :
					{
						for (int i = 0; i < 100; i++)
						{
							if (actualRotationAngle == 45F)
							{
								calcExLocationTest.posX++;
								calcExLocationTest.posZ--;
							}
							else
							{
								calcExLocationTest.posX--;
								calcExLocationTest.posZ++;
							}
							canDriveableTravel = !getDriveablePortalDamage(entDrvLocal, calcExLocationTest, actualRotationAngle, world);
							if(canDriveableTravel)
								break;
						}
						if(!canDriveableTravel)
						{
							actualRotationAngle = actualRotationAngleNeg;
							for (int i = 0; i < 100; i++)
							{
								if (actualRotationAngle == 45F)
								{
									calcExLocationTest.posX++;
									calcExLocationTest.posZ--;
								}
								else
								{
									calcExLocationTest.posX--;
									calcExLocationTest.posZ++;
								}
								canDriveableTravel = !getDriveablePortalDamage(entDrvLocal, calcExLocationTest, actualRotationAngle, world);
								if(!canDriveableTravel)
									break;
							}
						}
						break;
					}
			
			default:
				{
					break;
				}
		}
	return calcExLocationTest;
	
	}
	
	public static boolean getDriveablePortalDamage(EntityDriveable drvLocal1, ChunkCoordinates calcExLocationTst, float testRot, World world)
	{
		for(DriveablePosition p : drvLocal1.getDriveableType().collisionPoints)
		{
			if(drvLocal1.driveableData.parts.get(p.part).dead)
				continue;
			
			RotatedAxes testRotatedAxes = drvLocal1.axes.clone();
			testRotatedAxes.setAngles(testRot, 0F, 0F); //setAngles(float yaw, float pitch, float roll)
			Vector3f calcRotatedPos = testRotatedAxes.findLocalVectorGlobally(p.position);
			Vec3 testLocalPos = Vec3.createVectorHelper(calcExLocationTst.posX + calcRotatedPos.x, calcExLocationTst.posY + calcRotatedPos.y, calcExLocationTst.posZ + calcRotatedPos.z);
			Vec3 testLocalPos1 = Vec3.createVectorHelper(calcExLocationTst.posX + calcRotatedPos.x + 0.5, calcExLocationTst.posY + calcRotatedPos.y + 0.5, calcExLocationTst.posZ + 0.5);
			MovingObjectPosition hitBlock = world.rayTraceBlocks(testLocalPos, testLocalPos1, true);
			if(hitBlock != null && hitBlock.typeOfHit == MovingObjectType.BLOCK)
				return true;
		}
		return false;
	}
	
	public static Vector3f getActualMoments(Entity entity, float calcFinalAngle)
	{
		EntityDriveable drvToCalcMomentum = (EntityDriveable) entity;
		float driveableHorizSpeed = (float) drvToCalcMomentum.getSpeedXZ();
		if (entity instanceof EntityVehicle)
		{
			EntityVehicle vehCalc = (EntityVehicle) entity;
			for(EntityWheel wheel : vehCalc.wheels)
			{
				if(wheel == null)
				continue;
				driveableHorizSpeed = Math.max((float) wheel.getSpeedXZ(), driveableHorizSpeed);
			}
			
		}
		Vector3f momentsCalc = new Vector3f(0F, 0F, 0F);
		if (calcFinalAngle == 0F)
			momentsCalc.set(driveableHorizSpeed, drvToCalcMomentum.throttle, 0F);
				else if (calcFinalAngle == 180F)
					momentsCalc.set(-driveableHorizSpeed, drvToCalcMomentum.throttle, 0F);
				else if (calcFinalAngle == -90F)
					momentsCalc.set(0F, drvToCalcMomentum.throttle, -driveableHorizSpeed);
				else if (calcFinalAngle == 90F)
					momentsCalc.set(0F, drvToCalcMomentum.throttle, driveableHorizSpeed);
				else if (calcFinalAngle == -45F)
					momentsCalc.set(driveableHorizSpeed / 2F, drvToCalcMomentum.throttle, -driveableHorizSpeed / 2F);
				else if (calcFinalAngle == 45F)
					momentsCalc.set(driveableHorizSpeed / 2F, drvToCalcMomentum.throttle, driveableHorizSpeed / 2F);
				else if (calcFinalAngle == -135F)
					momentsCalc.set(-driveableHorizSpeed / 2F, drvToCalcMomentum.throttle, -driveableHorizSpeed / 2F);
				else if (calcFinalAngle == 135F)
					momentsCalc.set(-driveableHorizSpeed / 2F, drvToCalcMomentum.throttle, driveableHorizSpeed / 2F);
		return momentsCalc;
	}
}
	
