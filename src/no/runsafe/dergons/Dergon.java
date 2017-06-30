package no.runsafe.dergons;

import net.minecraft.server.v1_8_R3.*;
import no.runsafe.framework.api.ILocation;
import no.runsafe.framework.api.IWorld;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.framework.internal.wrapper.ObjectUnwrapper;
import no.runsafe.framework.minecraft.Item;
import no.runsafe.framework.minecraft.Sound;
import no.runsafe.framework.minecraft.entity.RunsafeFallingBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.*;

/*
 * Names of obfuscated variables in various spigot versions:
 * Type                 v1_8_R3  v1_9_R2  v1_10_R1 v1_11_R1
 * Entity.class:
 * public boolean       ah       ?        ?        ?           Unknown
 * public boolean       F        C        C        C           Checks if entity is collided with a vertical block.
 *
 * EntityLiving.Class:
 * protected int        bc       bh       bi       bh          Position rotation increment.
 * protected float      bb       bg       bh       bg          Random Yaw Velocity.
 */

public class Dergon extends EntityInsentient implements IComplex, IMonster
{
	public Dergon(IWorld world, DergonHandler handler, ILocation targetLocation, int dergonID)
	{
		super(ObjectUnwrapper.getMinecraft(world));

		this.children = new EntityComplexPart[]
		{
			this.dergonHead = new EntityComplexPart(this, "head", 6.0F, 6.0F),
			this.dergonBody = new EntityComplexPart(this, "body", 8.0F, 8.0F),
			this.dergonTailSection0 = new EntityComplexPart(this, "tail", 4.0F, 4.0F),
			this.dergonTailSection1 = new EntityComplexPart(this, "tail", 4.0F, 4.0F),
			this.dergonTailSection2 = new EntityComplexPart(this, "tail", 4.0F, 4.0F),
			this.dergonWingLeft = new EntityComplexPart(this, "wing", 4.0F, 4.0F),
			this.dergonWingRight = new EntityComplexPart(this, "wing", 4.0F, 4.0F)
		};
		this.setHealth(this.getMaxHealth());
		this.setSize(16, 8);
		this.noclip = true;
		this.fireProof = true;
		this.ah = true;

		this.handler = handler;
		this.targetLocation = targetLocation;
		this.targetWorld = targetLocation.getWorld();
		this.dergonID = dergonID;
	}

	/**
	 * Selects new player target.
	 */
	private void updateCurrentTarget()
	{
		ILocation dergonLocation = targetWorld.getLocation(locX, locY, locZ);

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
		// Throw a player off its back if we're high up.
		if (ridingPlayer != null && locY >= 90)
		{
			ridingPlayer.leaveVehicle();
			ridingPlayer = null;
		}

		// Update the health bar to show the percentage of the dergon
		long pct = round((getHealth() / getMaxHealth()) * 100);
		setCustomName("Dergon (" + pct + "%)");

		ILocation dergonLocation = targetWorld.getLocation(locX, locY, locZ);
		if (targetEntity != null && dergonLocation != null && random.nextFloat() < 0.2F)
			((RunsafeFallingBlock) targetWorld.spawnFallingBlock(dergonLocation, Item.Unavailable.Fire)).setDropItem(false);

		if (getHealth() <= 0.0F) // Check if the dragon is dead.
		{
			// If we're dead, play a random explosion effect at a random offset to its corpse.
			world.addParticle(
				EnumParticle.EXPLOSION_LARGE,
				locX + (random.nextFloat() - 0.5F) * 8.0F,
				locY + (random.nextFloat() - 0.5F) * 4.0F + 2.0D,
				locZ + (random.nextFloat() - 0.5F) * 8.0F,
				0.0D,
				0.0D,
				0.0D
			);
			return;
		}

		yaw = (float) trimDegrees(yaw);
		if (positionBufferIndex < 0)
		{
			for (int i = 0; i < positionBuffer.length; ++i)
			{
				positionBuffer[i][0] = yaw;
				positionBuffer[i][1] = locY;
			}
		}

		if (++positionBufferIndex == positionBuffer.length)
			positionBufferIndex = 0;

		positionBuffer[positionBufferIndex][0] = yaw;
		positionBuffer[positionBufferIndex][1] = locY;

		// Get target position relative to Dergon
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

		if (targetDistance < 100.0D || targetDistance > 22500.0D || positionChanged || F)
			updateCurrentTarget();

		targetPosY /= sqrt(targetPosX * targetPosX + targetPosZ * targetPosZ);
		final float Y_LIMIT = 0.6F;
		if (targetPosY < (-Y_LIMIT))
			targetPosY = (-Y_LIMIT);

		if (targetPosY > Y_LIMIT)
			targetPosY = Y_LIMIT;

		motY += targetPosY * 0.10000000149011612D;
		yaw = (float) trimDegrees(yaw);
		double targetDirection = 180.0D - toDegrees(atan2(targetPosX, targetPosZ));
		double targetHeadingDifference = trimDegrees(targetDirection - yaw);

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

		bb *= 0.8F;
		float movementSpeedStart = (float) sqrt(motX * motX + motZ * motZ) + 1.0F;
		double movementSpeedTrimmed = sqrt(motX * motX + motZ * motZ) + 1.0D;

		if (movementSpeedTrimmed > 40.0D)
			movementSpeedTrimmed = 40.0D;

		bb = (float) (bb + targetHeadingDifference * (0.699999988079071D / movementSpeedTrimmed / movementSpeedStart));
		yaw += bb * 0.1F;
		float f6 = (float) (2.0D / (movementSpeedTrimmed + 1.0D));
		float f7 = 0.06F;

		a(0.0F, -1.0F, f7 * (f4 * f6 + (1.0F - f6)));
		move(motX, motY, motZ);

		Vec3D movementVector = new Vec3D(motX, motY, motZ).a();
		float f8 = (float) (movementVector.b(vec3d1) + 1.0D) / 2.0F;

		f8 = 0.8F + 0.15F * f8;
		motX *= f8;
		motZ *= f8;
		motY *= 0.9100000262260437D;

		dergonHead.width = dergonHead.length = 3.0F;
		dergonTailSection0.width = dergonTailSection0.length = 2.0F;
		dergonTailSection1.width = dergonTailSection1.length = 2.0F;
		dergonTailSection2.width = dergonTailSection2.length = 2.0F;
		dergonBody.length = 3.0F;
		dergonBody.width = 5.0F;
		dergonWingRight.length = dergonWingLeft.length = 2.0F;
		dergonWingRight.width = dergonWingLeft.width = 4.0F;

		float f1 = (float) toRadians((
			getMovementOffset(5)[1]
			- getMovementOffset(10)[1]
		) * 10.0F);
		float cosF1 = (float) cos(f1);
		float sinF1 = (float) -sin(f1);
		float yawRad = (float) toRadians(yaw);
		float sinYaw = (float) sin(yawRad);
		float cosYaw = (float) cos(yawRad);

		incrementHitboxLocation(
			dergonBody,
			(sinYaw / 2),
			0,
			-(cosYaw / 2)
		);

		incrementHitboxLocation(
			dergonWingRight,
			(cosYaw * 4.5),
			1,
			(sinYaw * 4.5)
		);

		incrementHitboxLocation(
			dergonWingLeft,
			-(cosYaw * 4.5),
			1,
			-(sinYaw * 4.5)
		);

		if (hurtTicks == 0)
		{
			launchEntities(world.getEntities(this, dergonWingRight.getBoundingBox().grow(4.0D, 2.0D, 4.0D).shrink(0.0D, -2.0D, 0.0D)));
			launchEntities(world.getEntities(this, dergonWingLeft.getBoundingBox().grow(4.0D, 2.0D, 4.0D).shrink(0.0D, -2.0D, 0.0D)));
			hitEntities(world.getEntities(this, dergonHead.getBoundingBox().grow(1.0D, 1.0D, 1.0D)));
		}

		double[] olderPosition = getMovementOffset(5);
		double[] currentPosition = getMovementOffset(0);

		float xHeadDirectionIncremented = (float) sin(toRadians(yaw) - bc * 0.01F);
		float zHeadDirectionIncremented = (float) cos(toRadians(yaw) - bc * 0.01F);

		incrementHitboxLocation(
			dergonHead,
			(xHeadDirectionIncremented * 5.5 * cosF1),
			(currentPosition[1] - olderPosition[1]) + (sinF1 * 5.5),
			-(zHeadDirectionIncremented * 5.5 * cosF1)
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

			incrementHitboxLocation(
				tailSection,
				-(sinYaw * ONE_POINT_FIVE + sinF14 * movementMultiplier) * cosF1,
				(oldPosition[1] - olderPosition[1]) - ((movementMultiplier + ONE_POINT_FIVE) * sinF1) + 1.5D,
				(cosYaw * ONE_POINT_FIVE + cosF14 * movementMultiplier) * cosF1
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
		targetEntity = null;

		// Only apply damage if the source is a player or an explosion.
		if (damager.getEntity() instanceof EntityHuman || damager.isExplosion())
		{
			// Do more damage for head shots
			if(bodyPart != dergonHead)
				damageValue = (damageValue / 4) + 1;
			damageEntity(damager, damageValue);
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
		// Set yaw offset
		movementOffset[0] = positionBuffer[j][0];
		// Set y offset.
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
	 * @param f Amount of damage
	 * @return True if damaged, false if not damaged.
	 */
	@Override
	protected boolean d(DamageSource source, float f)
	{
		if (ridingPlayer == null || !isRidingPlayer(source.getEntity().getName()))
			return super.d(source, handler.handleDergonDamage(this, source, f));

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
		this.deathTicks++;

		// Make explosion particles when the dergon is almost dead.
		if (this.deathTicks >= 180 && this.deathTicks <= 200)
			world.addParticle(
				EnumParticle.EXPLOSION_HUGE,
				locX + (random.nextFloat() - 0.5) * 8,
				locY + (random.nextFloat() - 0.5) * 4 + 2,
				locZ + (random.nextFloat() - 0.5) * 8,
				0, 0, 0
			);

		// Play the death sound as the death animation starts.
		if (this.deathTicks == 1)
			targetWorld.getLocation(locX, locY, locZ).playSound(
				Sound.Creature.EnderDragon.Death, 32.0F, 1.0F
			);

		// When animation is finished, slay the dergon.
		if(this.deathTicks == 200)
		{
			die();
			handler.handleDergonDeath(this);
		}
	}

	/**
	 * Despawns entity if it gets too far away.
	 * Names of this function in various spigot versions:
	 * v1_8_R3: D
	 */
	@Override
	protected void D()
	{
	}

	/**
	 * Gets all hitboxes.
	 * Required by the IComplex interface.
	 * Names of this function in various spigot versions:
	 * v1_8_R3: aB
	 * @return All hitboxes.
	 */
	@Override
	public Entity[] aB()
	{
		return this.children;
	}

	/**
	 * Gets the current world.
	 * Required by the IComplex interface.
	 * Names of this function in various spigot versions:
	 * v1_8_R3: a
	 * @return The current world.
	 */
	@Override
	public World a()
	{
		return this.world;
	}

	/**
	 * Gets the idle sound.
	 * Names of this function in different spigot versions:
	 * v1_8_R3: z
	 * v1_9_R2/v1_10_R1: G, returns SoundEffect
	 * @return Idle sound.
	 */
	@Override
	protected String z()
	{
		targetWorld.getLocation(locX, locY, locZ).playSound(
			Sound.Creature.EnderDragon.Growl, 5, 1
		);
		return null;
	}

	/**
	 * Get the hurt sound.
	 * Names of this function in various spigot versions:
	 * v1_8_R3: bo
	 * v1_9_R2: bS, returns SoundEffect
	 * v1_10_R1: bV, returns SoundEffect
	 * @return string "none"
	 */
	@Override
	protected String bo()
	{
		targetWorld.getLocation(locX, locY, locZ).playSound(
			Sound.Creature.EnderDragon.Hit, 5, 1
		);
		return null;
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
	 * Increments the hitbox location of a dergon's body part.
	 * @param bodyPart Part to change the location of.
	 * @param xIncrement How far to move in the X direction.
	 * @param yIncrement How far to move in the Y direction.
	 * @param zIncrement How far to move in the Z direction.
	 */
	private void incrementHitboxLocation(EntityComplexPart bodyPart, double xIncrement, double yIncrement, double zIncrement)
	{
		bodyPart.t_(); //t_() means on update.
		bodyPart.setPositionRotation(
			locX + xIncrement, locY + yIncrement, locZ + zIncrement, 0, 0
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
			&& player.isSurvivalist();
	}

	private boolean isRidingPlayer(String playerName)
	{
		return ridingPlayer != null && ridingPlayer.getName().equals(playerName);
	}

	public int getDergonID()
	{
		return dergonID;
	}

	/*
	 * Dergon bodily appendages.
	 * Only their hitboxes.
	 */
	private EntityComplexPart[] children;
	private EntityComplexPart dergonHead;
	private EntityComplexPart dergonBody;
	private EntityComplexPart dergonWingRight;
	private EntityComplexPart dergonWingLeft;
	private EntityComplexPart dergonTailSection0;
	private EntityComplexPart dergonTailSection1;
	private EntityComplexPart dergonTailSection2;

	// Target coordinates to fly to.
	private double targetX = 0;
	private double targetY = 100;
	private double targetZ = 0;

	// Store the dergon's last 64 vertical and yaw positions.
	private double[][] positionBuffer = new double[64][2];
	private int positionBufferIndex = -1;

	private int deathTicks = 0;
	private Entity targetEntity;
	private final DergonHandler handler;
	private final ILocation targetLocation;
	private ILocation flyOffLocation;
	private final IWorld targetWorld;
	private final Random random = new Random();
	private IPlayer ridingPlayer = null;
	private final int dergonID;
}
