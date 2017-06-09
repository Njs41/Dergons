package no.runsafe.dergons;

import net.minecraft.server.v1_8_R3.*;
import no.runsafe.framework.api.ILocation;
import no.runsafe.framework.api.IWorld;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.framework.internal.wrapper.ObjectUnwrapper;
import no.runsafe.framework.minecraft.Item;
import no.runsafe.framework.minecraft.Sound;
import no.runsafe.framework.minecraft.entity.RunsafeFallingBlock;
import org.bukkit.GameMode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static java.lang.Math.PI;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

/*
 * Names of obfuscated variables in various spigot versions:
 *
 * Variables in EntityEnderDragon:
 * Type                 v1_8_R3  v1_9_R2  v1_10_R1  v1_11_R1
 * public float         bu       bD       bE        bD          Previous animation time.
 * public float         bv       bE       bF        bE          Animation time.
 *
 * Entity.class:
 * public boolean       F        C        C         C           Checks if entity is collided with a vertical block.
 *
 * EntityLiving.Class:
 * protected int        bc       bh       bi        bh          Position rotation increment
 * public float         aI       aN       aO        aN          Might have something to do with the yaw offset?
 * protected float      bb       bg       bh        bg          Random Yaw Velocity
 */

public class Dergon extends EntityEnderDragon
{
	public Dergon(IWorld world, DergonHandler handler, ILocation targetLocation, int dergonID)
	{
		super(ObjectUnwrapper.getMinecraft(world));
		this.handler = handler;
		this.targetLocation = targetLocation;
		this.targetWorld = targetLocation.getWorld();
		this.dergonID = dergonID;

		// Load up the position buffer with current values.
		for (int i = 0; i < positionBuffer.length; ++i)
		{
			positionBuffer[i][0] = (double) yaw;
			positionBuffer[i][1] = locY;
		}
	}

	/**
	 * Target coordinates to fly to.
	 */
	private double targetX = 0;
	private double targetY = 100;
	private double targetZ = 0;

	// Forces selection of a new flight target when true.
	private boolean changeTarget = false;

	private int deathTicks = 0;

	// Store the dergon's last 64 vertical and yaw positions.
	private double[][] positionBuffer = new double[64][2];
	private int positionBufferIndex = -1;

	/*
	 * Dergon bodily appendages.
	 * Only their hitboxes.
	 * Names in various spigot versions:
	 * v1_8_R3    v1_9_R2    v1_10_R1    v1_11_R1
	 * bn         bv         bw          bv        Head
	 * bo         bx         by          bx        Body
	 * bs         bB         bC          bB        Right Wing
	 * bt         bC         bD          bC        Left Wing
	 * bp         by         bz          by        Tail section closest to body
	 * bq         bz         bA          bz        Middle tail section
	 * br         bA         bB          bA        Last tail section
	 * N/A        bw         bx          bw        Neck (Only in 1.9+)
	 */
	private EntityComplexPart dergonHead = bn;
	private EntityComplexPart dergonBody = bo;
	private EntityComplexPart dergonWingRight = bs;
	private EntityComplexPart dergonWingLeft = bt;
	private EntityComplexPart dergonTailSection0 = bp;
	private EntityComplexPart dergonTailSection1 = bq;
	private EntityComplexPart dergonTailSection2 = br;

	/**
	 * Selects new player target.
	 */
	private void updateCurrentTarget()
	{
		changeTarget = false;

		ILocation dergonLocation = getLocation();

		if (dergonLocation != null && flyOffLocation != null && random.nextFloat() == 0.1F)
			return;
		else
			flyOffLocation = null;

		// Check if we have any close players, if we do, fly away.
		if (dergonLocation != null && !dergonLocation.getPlayersInRange(10).isEmpty())
		{
			if (ridingPlayer == null && random.nextFloat() < 0.5F)
			{
				List<IPlayer> closePlayers = dergonLocation.getPlayersInRange(10);
				IPlayer unluckyChum = closePlayers.get(random.nextInt(closePlayers.size()));

				if (isValidTarget(unluckyChum))
				{
					EntityHuman rawChum = ObjectUnwrapper.getMinecraft(unluckyChum);

					if (rawChum != null)
					{
						rawChum.mount(this);
						ridingPlayer = unluckyChum;
						handler.handleDergonMount(ridingPlayer.getName());
					}
				}
			}

			targetEntity = null;
			targetX = locX + random.nextInt(200) + -100;
			targetY = random.nextInt(100) + 70; // Somewhere above 70 to prevent floor clipping.
			targetZ = locZ + random.nextInt(200) + -100;
			flyOffLocation = targetWorld.getLocation(targetX, targetY, targetZ); // Store the target fly-off location.
			return;
		}
		else
		{
			List<IPlayer> players = targetLocation.getPlayersInRange(200); // Grab all players in 200 blocks.
			List<IPlayer> targets = new ArrayList<IPlayer>(0);

			for (IPlayer player : players)
			{
				// Skip the player if we're vanished, in creative mode, or in spectator mode.
				if (!isValidTarget(player) || isRidingPlayer(player.getName()))
					continue;

				ILocation playerLocation = player.getLocation();

				// If the player is greater than 50 blocks, we can target them.
				if (playerLocation != null && playerLocation.distance(targetLocation) > 50)
					targets.add(player);
			}

			if (!targets.isEmpty())
			{
				// Target a random player in 200 blocks.
				targetEntity = ObjectUnwrapper.getMinecraft(players.get(random.nextInt(players.size())));
				return;
			}
		}

		// Send the dergon back to the start point.
		targetX = targetLocation.getX();
		targetY = targetLocation.getY();
		targetZ = targetLocation.getZ();

		targetEntity = null;
	}

	/**
	 * Update method for Dergons.
	 * Names of this function in various spigot versions:
	 * v1_8_R3: m
	 * v1_9_R2/v1_10_R1/v1_11_R1: n
	 */
	@Override
	public void m()
	{
		// Throw a player off it's back if we're high up.
		if (ridingPlayer != null && locY >= 90)
		{
			ridingPlayer.leaveVehicle();
			ridingPlayer = null;
		}


		// Update the health bar to show the percentage of the dergon
		long pct = Math.round((getHealth() / getMaxHealth()) * 100);
		setCustomName("Dergon (" + pct + "%)");

		ILocation dergonLocation = getLocation();
		if (targetEntity != null && dergonLocation != null && random.nextFloat() < 0.2F)
			((RunsafeFallingBlock) targetWorld.spawnFallingBlock(dergonLocation, Item.Unavailable.Fire)).setDropItem(false);

		bu = bv; // Update previous animation time.

		if (getHealth() <= 0.0F) // Check if the dragon is dead.
		{
			randomExplosion(); // If we're dead, play a random explosion effect at a random offset to it's corpse.
			return;
		}

		float f = 0.2F / ((float) sqrt(motX * motX + motZ * motZ) * 10.0F + 1.0F);
		f *= (float) pow(2.0D, motY);
		bv += f;

		yaw = (float) trimDegrees(yaw);

		// Increment the buffer index.
		if (++positionBufferIndex == positionBuffer.length)
			positionBufferIndex = 0;

		positionBuffer[positionBufferIndex][0] = (double) yaw;
		positionBuffer[positionBufferIndex][1] = locY;

		//Get target position relative to Dergon
		double targetPosX = targetX - locX;
		double targetPosY = targetY - locY;
		double targetPosZ = targetZ - locZ;
		double targetDistance = targetPosX * targetPosX + targetPosY * targetPosY + targetPosZ * targetPosZ;
		if (targetEntity != null)
		{
			targetX = targetEntity.locX;
			targetZ = targetEntity.locZ;
			double xDistanceToTarget = targetX - locX;
			double yDistanceToTarget = targetZ - locZ;
			double distanceToTarget = sqrt(xDistanceToTarget * xDistanceToTarget + yDistanceToTarget * yDistanceToTarget);
			double ascendDistance = 0.4000000059604645D + distanceToTarget / 80.0D - 1.0D;

			if (ascendDistance > 10.0D)
				ascendDistance = 10.0D;

			targetY = targetEntity.getBoundingBox().b + ascendDistance;
		}
		else
		{
			targetX += random.nextGaussian() * 2.0D;
			targetZ += random.nextGaussian() * 2.0D;
		}

		if (changeTarget || targetDistance < 100.0D || targetDistance > 22500.0D || positionChanged || F)
			updateCurrentTarget();

		targetPosY /= sqrt(targetPosX * targetPosX + targetPosZ * targetPosZ);
		final float Y_LIMIT = 0.6F;
		if (targetPosY < (double) (-Y_LIMIT))
			targetPosY = (double) (-Y_LIMIT);

		if (targetPosY > (double) Y_LIMIT)
			targetPosY = (double) Y_LIMIT;

		motY += targetPosY * 0.10000000149011612D;
		yaw = (float) trimDegrees(yaw);
		double targetDirection = 180.0D - toDegrees(atan2(targetPosX, targetPosZ));
		double targetHeadingDifference = trimDegrees(targetDirection - (double) yaw);

		if (targetHeadingDifference > 50.0D)
			targetHeadingDifference = 50.0D;

		if (targetHeadingDifference < -50.0D)
			targetHeadingDifference = -50.0D;

		Vec3D relativeTargetCoordinates = new Vec3D(
			targetX - locX,
			targetY - locY,
			targetZ - locZ
		).a();// .a() -> Normalize values
		Vec3D vec3d1 = new Vec3D(
			sin(toRadians(yaw)),
			motY,
			(-cos(toRadians(yaw)))
		).a();// .a() -> Normalize values
		float f4 = (float) (vec3d1.b(relativeTargetCoordinates) + 0.5D) / 1.5F;

		if (f4 < 0.0F)
			f4 = 0.0F;

		bb *= 0.8F;  // Dampen the random yaw velocity.
		double movementScalarTrimmed = sqrt(motX * motX + motZ * motZ) + 1;
		float movementScalarStart = (float) movementScalarTrimmed;

		if (movementScalarTrimmed > 40.0D)
			movementScalarTrimmed = 40.0D;

		bb = (float) ((double) bb + targetHeadingDifference * (0.699999988079071D / movementScalarTrimmed / movementScalarStart));
		yaw += bb * 0.1F;
		float f6 = (float) (2.0D / (movementScalarTrimmed + 1.0D));
		float dampenerValue = 0.06F;

		a(0.0F, -1.0F, dampenerValue * (f4 * f6 + (1.0F - f6)));
		move(motX, motY, motZ);

		Vec3D movementVector = new Vec3D(motX, motY, motZ).a();
		float f8 = (float) (movementVector.b(vec3d1) + 1.0D) / 2.0F;

		f8 = 0.8F + 0.15F * f8;
		motX *= (double) f8;
		motZ *= (double) f8;
		motY *= 0.9100000262260437D;

		aJ = yaw;
		dergonHead.width = dergonHead.length = 3.0F;
		dergonTailSection0.width = dergonTailSection0.length = 2.0F;
		dergonTailSection1.width = dergonTailSection1.length = 2.0F;
		dergonTailSection2.width = dergonTailSection2.length = 2.0F;
		dergonBody.length = 3.0F;
		dergonBody.width = 5.0F;
		dergonWingRight.length = 3.0F;
		dergonWingRight.width = 4.0F;
		dergonWingLeft.length = 3.0F;
		dergonWingLeft.width = 4.0F;

		float f1 = (float) toRadians((
			getMovementOffset(5)[1]
			- getMovementOffset(10)[1]
		) * 10.0F);
		float cosF1 = (float) cos(f1);
		float sinF1 = (float) -sin(f1);
		float yawRad = (float) toRadians(yaw);
		float sinYaw = (float) sin(yawRad);
		float cosYaw = (float) cos(yawRad);

		incrementLocation(dergonBody, (sinYaw / 2), 0, -(cosYaw / 2));
		incrementLocation(dergonWingRight, (cosYaw * 4.5), 1.5, (sinYaw * 4.5));
		incrementLocation(dergonWingLeft, -(cosYaw * 4.5), 1.5, -(sinYaw * 4.5));

		if (hurtTicks == 0)
		{
			launchEntities(world.getEntities(this, dergonWingRight.getBoundingBox().grow(4.0D, 2.0D, 4.0D).shrink(0.0D, -2.0D, 0.0D)));
			launchEntities(world.getEntities(this, dergonWingLeft.getBoundingBox().grow(4.0D, 2.0D, 4.0D).shrink(0.0D, -2.0D, 0.0D)));
			hitEntities(world.getEntities(this, dergonHead.getBoundingBox().grow(1.0D, 1.0D, 1.0D)));
		}

		double[] olderPosition = getMovementOffset(5);
		double[] currentPosition = getMovementOffset(0);

		float xDirectionIncremented = (float) sin(toRadians(yaw) - bc * 0.01F);
		float zDirectionIncremented = (float) cos(toRadians(yaw) - bc * 0.01F);

		// Make dergon's head hitbox point in the direction of motion.
		incrementLocation(
			dergonHead,
			(xDirectionIncremented * 5.5F * cosF1),
			((currentPosition[1] - olderPosition[1]) + (sinF1 * 5.5F)),
			-(zDirectionIncremented * 5.5F * cosF1)
		);

		//Move the tail
		for (int tailNumber = 0; tailNumber < 3; ++tailNumber)
		{
			EntityComplexPart tailSection = null;

			switch (tailNumber)
			{
				case 0: tailSection = dergonTailSection0; break;
				case 1: tailSection = dergonTailSection1; break;
				case 2: tailSection = dergonTailSection2; break;
			}

			double[] oldPosition = getMovementOffset(12 + tailNumber * 2);
			float f14 = (float) toRadians(yaw + trimDegrees(oldPosition[0] - olderPosition[0]));
			float sinF14 = (float) sin(f14);
			float cosF14 = (float) cos(f14);
			final float ONE_POINT_FIVE = 1.5F;
			float movementMultiplier = (tailNumber + 1) * 2.0F; // 2, 4, 6

			incrementLocation(
				tailSection,
				-((sinYaw * ONE_POINT_FIVE + sinF14 * movementMultiplier) * cosF1),
				((oldPosition[1] - olderPosition[1]) - ((movementMultiplier + ONE_POINT_FIVE) * sinF1) + 1.5),
				((cosYaw * ONE_POINT_FIVE + cosF14 * movementMultiplier) * cosF1)
			);
		}
	}

	/**
	 * Damages the dergon based on a specific body part.
	 * Also recalculates the dergon's target location.
	 * Names of this function in various spigot versions:
	 * v1_8_R3/v1_9_R2/v1_10_R1/v1_11_R1: a
	 * @param bodyPart Part of the dergon hit.
	 * @param damager Source of the damage.
	 * @param damageValue Amount of damage.
	 * @return true
	 */
	@Override
	public boolean a(EntityComplexPart bodyPart, DamageSource damager, float damageValue)
	{
		// Recalculate target location
		double yawRadian = toRadians(yaw);
		double xDirection = sin(yawRadian);
		double zDirection = cos(yawRadian);
		targetX = locX + ((random.nextDouble() - 0.5) * 2) + (xDirection * 5);
		targetY = locY + (random.nextDouble() * 3) + 1;
		targetZ = locZ + ((random.nextDouble() - 0.5) * 2) - (zDirection * 5);
		target = null;

		// Only apply damage if the source is a player or an explosion.
		if (damager.getEntity() instanceof EntityHuman || damager.isExplosion())
		{
			// Do more damage for head shots
			if(bodyPart != dergonHead)
				damageValue = (damageValue / 4) + 1;
			dealDamage(damager, damageValue);
		}

		return true;
	}

	/**
	 * Launches entities a short distance.
	 * @param list Entities to launch
	 */
	private void launchEntities(List list)
	{
		double bodyBoundingBoxValue0 = (dergonBody.getBoundingBox().a + dergonBody.getBoundingBox().d) / 2.0D;
		double bodyBoundingBoxValue1 = (dergonBody.getBoundingBox().c + dergonBody.getBoundingBox().f) / 2.0D;

		for (Object rawEntity : list)
		{
			Entity entity = (Entity) rawEntity;
			if (!(entity instanceof EntityLiving))
				continue;

			double xDistance = entity.locX - bodyBoundingBoxValue0;
			double zDistance = entity.locZ - bodyBoundingBoxValue1;
			double distanceSquared = xDistance * xDistance + zDistance * zDistance;

			entity.g(
				xDistance / distanceSquared * 4.0D,
				0.20000000298023224D,
				zDistance / distanceSquared * 4.0D
			);
		}
	}

	/**
	 * Attack list of EntityLiving with 20.0F damage.
	 * @param list Entities to hit
	 */
	private void hitEntities(List list)
	{
		for (Object rawEntity : list)
		{
			Entity entity = (Entity) rawEntity;

			if (entity instanceof EntityLiving)
				entity.damageEntity(DamageSource.mobAttack(this), 20.0F);
		}
	}

	/**
	 * Trims down a degree value to between -180 and 180.
	 * @param degreeValue Number to trim.
	 * @return Trimmed degree value.
	 */
	private double trimDegrees(double degreeValue)
	{
		degreeValue %= 360.0D;

		if(degreeValue >= 180.0D)
			return degreeValue - 360.0D;

		if(degreeValue < -180.0D)
			return degreeValue + 360.0D;

		return degreeValue;
	}

	/**
	 * Gets a movement offset. Useful for calculating trailing tail and neck positions.
	 * @param bufferIndexOffset Offset for the ring buffer.
	 * @return A double [2] array with movement offsets.
	 * [0] = yaw offset, [1] = y offset
	 */
	private double[] getMovementOffset(int bufferIndexOffset)
	{
		int j = positionBufferIndex - bufferIndexOffset & 63;
		double[] movementOffset = new double[2];
		//Set yaw offset
		movementOffset[0] = positionBuffer[j][0];
		//set y offset.
		movementOffset[1] = positionBuffer[j][1];

		return movementOffset;
	}

	/**
	 * Damage the dergon.
	 * Overrides method in EntityLiving.class
	 * Names of this function in various spigot versions:
	 * v1_8_R3: d
	 * v1_9_R2/v1_10_R1/v1_11_R1: damageEntity0
	 * @param source damage source
	 * @param damageValue Damage amount
	 * @return True if damaged, false if not damaged.
	 */
	@Override
	protected boolean d(DamageSource source, float damageValue)
	{
		if (ridingPlayer == null || !isRidingPlayer(source.getEntity().getName()))
			return super.d(source, handler.handleDergonDamage(this, source, damageValue));

		return false;
	}

	/**
	 * Handles dergon death ticks.
	 * Overrides method in EntityEnderDragon which overrides method in EntityLiving
	 * Names of this function in various spigot versions:
	 * v1_8_R3: aZ
	 * v1_9_R2: bD
	 * v_10_R1: bF
	 * v_11_R1: bG
	 */
	@Override
	protected void aZ()
	{
		if (dead)
			return;

		// Increment death ticks.
		deathTicks++;
		// Make explosion particles when the dergon is almost dead.
		if (deathTicks >= 180 && deathTicks <= 200)
			randomExplosion();

		//Play dragon death sound as dergon death animation starts.
		if (deathTicks == 1)
			getLocation().playSound(
				Sound.Creature.EnderDragon.Death, 32.0F, 1.0F
			);

		// Slowly move dergon upwards as it dies.
		move(0, 0.1, 0);
		aI = yaw += 20.0F;

		// When animation is finished, slay the dergon.
		if(deathTicks == 200)
		{
			die();
			handler.handleDergonDeath(this);
		}
	}

	/**
	 * Gets the world the dergon is in.
	 * @return World the dergon is in.
	 */
	public IWorld getDergonWorld()
	{
		return targetWorld;
	}

	/**
	 * Gets the dergon's current ILocation.
	 * @return Location.
	 */
	public ILocation getLocation()
	{
		return targetWorld.getLocation(locX, locY, locZ);
	}

	/**
	 * Increments the hitbox location of a dergon's body part.
	 * @param bodyPart Part to change the location of.
	 * @param xIncrement How far to move in the X direction.
	 * @param yIncrement How far to move in the Y direction.
	 * @param zIncrement How far to move in the Z direction.
	 */
	private void incrementLocation(EntityComplexPart bodyPart, double xIncrement, double yIncrement, double zIncrement)
	{
		bodyPart.t_(); //t_() means on update.
		bodyPart.setPositionRotation(
			locX + xIncrement, locY + yIncrement, locZ + zIncrement, 0.0F, 0.0F
		);
	}

	/**
	 * Creates random explosions around the dergon.
	 */
	private void randomExplosion()
	{
		world.addParticle(
			EnumParticle.EXPLOSION_HUGE,
			locX + (random.nextFloat() - 0.5) * 8,
			locY + (random.nextFloat() - 0.5) * 4 + 2,
			locZ + (random.nextFloat() - 0.5) * 8,
			0.0D, 0.0D, 0.0D
		);
	}

	/**
	 * Checks if player should be targeted.
	 * Will not return true if player is vanished, dead, in creative, or in spectator mode.
	 * @param player Person to consider targeting.
	 * @return True if targetable.
	 */
	private boolean isValidTarget(IPlayer player)
	{
		return !player.isVanished()
				&& !player.isDead()
				&& player.getGameMode() != GameMode.CREATIVE
				&& player.getGameMode() != GameMode.SPECTATOR;
	}

	private boolean isRidingPlayer(String playerName)
	{
		return ridingPlayer != null && ridingPlayer.getName().equals(playerName);
	}

	public int getDergonID()
	{
		return dergonID;
	}

	private Entity targetEntity;
	private final DergonHandler handler;
	private final ILocation targetLocation;
	private ILocation flyOffLocation;
	private final IWorld targetWorld;
	private final Random random = new Random();
	private IPlayer ridingPlayer = null;
	private final int dergonID;
}
