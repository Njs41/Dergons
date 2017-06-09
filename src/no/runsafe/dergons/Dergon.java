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
 * public double[][]    bk       b        b         b           Buffer array for the last 64 Y-positions and yaw rotations.
 * public int           bl       c        c         c           Ring buffer index for yaw/Y-position buffer.
 * public float         bu       bD       bE        bD          Previous animation time.
 * public float         bv       bE       bF        bE          Animation time.
 * public boolean       bx       bF       bG        bF          True when slowed.
 * public int           by       bG       bH        bG          Death Ticks.
 *
 * Entity.class:
 * public boolean       F        C        C         C           Checks if entity is collided with a vertical block.
 *
 * EntityLiving.Class:
 * protected int        bc       bh       bi        bh          Position rotation increment
 * protected double     bd       bi       bj        bi          X position entity will be set to.
 * protected double     be       bj       bk        bj          Y position entity will be set to.
 * protected double     bf       bk       bl        bk          Z position entity will be set to.
 * protected double     bg       bl       bm        bl          Yaw position entity will be set to.
 * protected double     bh       bm       bn        bm          Pitch position entity will be set to.
 * public float         aI       aN       aO        aN          Might have something to do with the yaw offset?
 * public float         aJ       aO       aP        aO          Definitely has something to do with the yaw offset.
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
	}

	/**
	 * Target coordinates to fly to.
	 */
	private double targetX = 0;
	private double targetY = 100;
	private double targetZ = 0;

	/*
	 * Dergon bodily appendages.
	 * Only their hitboxes.
	 * Names in various spigot versions:
	 * v1_8_R3    v1_9_R2    v1_10_R1    v1_11_R1
	 * bn         bv         bw          bv        Head
	 * bo         bx         by          bx        Body
	 * bs         bB         bC          bB        Wing
	 * bt         bC         bD          bC        Wing
	 * bp         by         bz          by        Tail section closest to body
	 * bq         bz         bA          bz        Middle tail section
	 * br         bA         bB          bA        Last tail section
	 * N/A        bw         bx          bw        Neck (Only in 1.9+)
	 */
	private EntityComplexPart dergonHead = bn;
	private EntityComplexPart dergonBody = bo;
	private EntityComplexPart dergonWing0 = bs;
	private EntityComplexPart dergonWing1 = bt;
	private EntityComplexPart dergonTailSection0 = bp;
	private EntityComplexPart dergonTailSection1 = bq;
	private EntityComplexPart dergonTailSection2 = br;

	/**
	 * Selects new player target.
	 */
	private void updateCurrentTarget()
	{
		bw = false;

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
						ridingPlayer = rawChum;
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
			ridingPlayer.mount(null);
			ridingPlayer = null;
		}


		// Update the health bar to show the percentage of the dergon
		long pct = Math.round((getHealth() / getMaxHealth()) * 100);
		setCustomName("Dergon (" + pct + "%)");

		ILocation dergonLocation = getLocation();
		if (targetEntity != null && dergonLocation != null && random.nextFloat() < 0.2F)
			((RunsafeFallingBlock) targetWorld.spawnFallingBlock(dergonLocation, Item.Unavailable.Fire)).setDropItem(false);

		if (world.isClientSide)
		{
			float animationPoint = (float) cos(bv * PI * 2.0F);
			float previousAnimationPoint = (float) cos(bu * PI * 2.0F);
			if (previousAnimationPoint <= -0.3F && animationPoint >= -0.3F)
				world.a(locX, locY, locZ, "mob.enderdragon.wings", 5.0F, 0.8F + random.nextFloat() * 0.3F, false);
		}

		bu = bv;

		if (getHealth() <= 0.0F) // Check if the dragon is dead.
		{
			// If we're dead, play a random explosion effect at a random offset to it's corpse.
			world.addParticle(
					EnumParticle.EXPLOSION_LARGE,
					locX + (double) (random.nextFloat() - 0.5F) * 8.0F,
					locY + (double) (random.nextFloat() - 0.5F) * 4.0F + 2.0D,
					locZ + (double) (random.nextFloat() - 0.5F) * 8.0F,
					0.0D,
					0.0D,
					0.0D
			);
		}
		else
		{
			float f = 0.2F / ((float) sqrt(motX * motX + motZ * motZ) * 10.0F + 1.0F);
			f *= (float) pow(2.0D, motY);
			bv += (bx ? f * 0.5F : f);

			yaw = (float) trimDegrees(yaw);
			if (bl < 0)
			{
				for (int i = 0; i < bk.length; ++i)
				{
					bk[i][0] = (double) yaw;
					bk[i][1] = locY;
				}
			}

			if (++bl == bk.length)
				bl = 0;

			bk[bl][0] = (double) yaw;
			bk[bl][1] = locY;

			if (world.isClientSide)
			{
				if (bc > 0)
				{
					double newXPosition = locX + (bd - locX) / bc;
					double newYPosition = locY + (be - locY) / bc;
					double newZPosition = locZ + (bf - locZ) / bc;
					double newYawIncrement = trimDegrees(bg - (double) yaw);
					yaw = (float) ((double) yaw + newYawIncrement / bc);
					pitch = (float) ((double) pitch + (bh - (double) pitch) / bc);
					--bc;
					setPosition(newXPosition, newYPosition, newZPosition);
					setYawPitch(yaw, pitch);
				}
			}
			else
			{
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

				if (bw || targetDistance < 100.0D || targetDistance > 22500.0D || positionChanged || F)
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

				Vec3D vec3d = new Vec3D(
						targetX - locX,
						targetY - locY,
						targetZ - locZ
				).a();// .a() -> Normalize values
				Vec3D vec3d1 = new Vec3D(
						sin(toRadians(yaw)),
						motY,
						(-cos(toRadians(yaw)))
				).a();// .a() -> Normalize values
				float f4 = (float) (vec3d1.b(vec3d) + 0.5D) / 1.5F;

				if (f4 < 0.0F)
					f4 = 0.0F;

				bb *= 0.8F;
				float f5 = (float) sqrt(motX * motX + motZ * motZ) + 1.0F;
				double d10 = sqrt(motX * motX + motZ * motZ) + 1.0D;

				if (d10 > 40.0D)
					d10 = 40.0D;

				bb = (float) ((double) bb + targetHeadingDifference * (0.699999988079071D / d10 / (double) f5));
				yaw += bb * 0.1F;
				float f6 = (float) (2.0D / (d10 + 1.0D));
				float f7 = 0.06F;

				a(0.0F, -1.0F, f7 * (f4 * f6 + (1.0F - f6)));
				if (bx)
					move(motX * 0.800000011920929D,
						motY * 0.800000011920929D,
						motZ * 0.800000011920929D
					);
				else
					move(motX, motY, motZ);

				Vec3D vec3d2 = new Vec3D(motX, motY, motZ).a();
				float f8 = (float) (vec3d2.b(vec3d1) + 1.0D) / 2.0F;

				f8 = 0.8F + 0.15F * f8;
				motX *= (double) f8;
				motZ *= (double) f8;
				motY *= 0.9100000262260437D;
			}

			aJ = yaw;
			dergonHead.width = dergonHead.length = 3.0F;
			dergonTailSection0.width = dergonTailSection0.length = 2.0F;
			dergonTailSection1.width = dergonTailSection1.length = 2.0F;
			dergonTailSection2.width = dergonTailSection2.length = 2.0F;
			dergonBody.length = 3.0F;
			dergonBody.width = 5.0F;
			dergonWing0.length = 2.0F;
			dergonWing0.width = 4.0F;
			dergonWing1.length = 3.0F;
			dergonWing1.width = 4.0F;

			float f1 = (float) toRadians((
				getMovementOffset(5, 1.0F)[1]
				- getMovementOffset(10, 1.0F)[1]
			) * 10.0F);
			float cosF1 = (float) cos(f1);
			float sinF1 = (float) -sin(f1);
			float yawRad = (float) toRadians(yaw);
			float sinYaw = (float) sin(yawRad);
			float cosYaw = (float) cos(yawRad);

			//t_() means on update.
			dergonBody.t_();
			dergonBody.setPositionRotation(
					locX + (double) (sinYaw * 0.5F),
					locY,
					locZ - (double) (cosYaw * 0.5F),
					0.0F,
					0.0F
			);

			dergonWing0.t_();
			dergonWing0.setPositionRotation(
					locX + (double) (cosYaw * 4.5F),
					locY + 2.0D,
					locZ + (double) (sinYaw * 4.5F),
					0.0F,
					0.0F
			);

			dergonWing1.t_();
			dergonWing1.setPositionRotation(
					locX - (double) (cosYaw * 4.5F),
					locY + 2.0D,
					locZ - (double) (sinYaw * 4.5F),
					0.0F,
					0.0F
			);

			if (!world.isClientSide && hurtTicks == 0)
			{
				launchEntities(world.getEntities(this, dergonWing0.getBoundingBox().grow(4.0D, 2.0D, 4.0D).shrink(0.0D, -2.0D, 0.0D)));
				launchEntities(world.getEntities(this, dergonWing1.getBoundingBox().grow(4.0D, 2.0D, 4.0D).shrink(0.0D, -2.0D, 0.0D)));
				hitEntities(world.getEntities(this, dergonHead.getBoundingBox().grow(1.0D, 1.0D, 1.0D)));
			}

			double[] adouble = getMovementOffset(5, 1.0F);
			double[] adouble1 = getMovementOffset(0, 1.0F);

			float f3 = (float) sin(toRadians(yaw) - bc * 0.01F);
			float f13 = (float) cos(toRadians(yaw) - bc * 0.01F);

			dergonHead.t_();
			dergonHead.setPositionRotation(
					locX + (double) (f3 * 5.5F * cosF1),
					locY + (adouble1[1] - adouble[1]) + (double) (sinF1 * 5.5F),
					locZ - (double) (f13 * 5.5F * cosF1),
					0.0F,
					0.0F
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

				double[] adouble2 = getMovementOffset(12 + tailNumber * 2, 1.0F);
				float f14 = (float) toRadians(yaw + trimDegrees(adouble2[0] - adouble[0]));
				float sinF14 = (float) sin(f14);
				float cosF14 = (float) cos(f14);
				final float ONE_POINT_FIVE = 1.5F;
				float movementMultiplier = (tailNumber + 1) * 2.0F; // 2, 4, 6

				tailSection.t_();
				tailSection.setPositionRotation(
						locX - (double) ((sinYaw * ONE_POINT_FIVE + sinF14 * movementMultiplier) * cosF1),
						locY + (adouble2[1] - adouble[1]) - (double) ((movementMultiplier + ONE_POINT_FIVE) * sinF1) + 1.5D,
						locZ + (double) ((cosYaw * ONE_POINT_FIVE + cosF14 * movementMultiplier) * cosF1),
						0.0F,
						0.0F
				);
			}

			if (!world.isClientSide)
				bx = breakBlocks(dergonHead.getBoundingBox()) | breakBlocks(dergonBody.getBoundingBox());
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
			if (entity instanceof EntityLiving)
			{
				double xDistance = entity.locX - bodyBoundingBoxValue0;
				double zDistance = entity.locZ - bodyBoundingBoxValue1;
				double distanceSquared = xDistance * xDistance + zDistance * zDistance;

				entity.g(xDistance / distanceSquared * 4.0D,
						0.20000000298023224D,
						zDistance / distanceSquared * 4.0D
				);
			}
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
	 * Handles breaking blocks; blocks are not to be broken.
	 * @param axisalignedbb Does nothing.
	 * @return True if blocks destroyed, false if no blocks destroyed. Always returns false.
	 */
	private boolean breakBlocks(AxisAlignedBB axisalignedbb)
	{
		return false;
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
	 * @param partialTicks Ticks that are partial.
	 * @return A double [2] array with movement offsets.
	 * [0] = yaw offset, [1] = y offset
	 */
	private double[] getMovementOffset(int bufferIndexOffset, float partialTicks)
	{
		if(getHealth() <= 0.0F)
			partialTicks = 0.0F;

		partialTicks = 1.0F - partialTicks;
		int j = bl - bufferIndexOffset & 63;
		int k = bl - bufferIndexOffset - 1 & 63;

		double[] movementOffset = new double[2];
		//Set yaw offset
		movementOffset[0] = bk[j][0] + trimDegrees(bk[k][0] - bk[j][0]) * partialTicks;
		//set y offset.
		movementOffset[1] = bk[j][1] + ((bk[k][1] - bk[j][1]) * partialTicks);

		return movementOffset;
	}

	/**
	 * Damage the dergon.
	 * Overrides method in EntityLiving.class
	 * Names of this function in various spigot versions:
	 * v1_8_R3: d
	 * v1_9_R2/v1_10_R1/v1_11_R1: damageEntity0
	 * @param source damage source
	 * @param f Damage amount
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
		by++;
		// Make explosion particles when the dergon is almost dead.
		if (by >= 180 && by <= 200)
		{
			world.addParticle(
				EnumParticle.EXPLOSION_HUGE,
				locX + (random.nextFloat() - 0.5F) * 8.0F,
				locY + (random.nextFloat() - 0.5F) * 4.0F + 2.0D,
				locZ + (random.nextFloat() - 0.5F) * 8.0F,
				0.0D,
				0.0D,
				0.0D
			);
		}

		//Play dragon death sound as dergon death animation starts.
		if (by == 1)
			getLocation().playSound(
				Sound.Creature.EnderDragon.Death, 32.0F, 1.0F
			);

		// Slowly move dergon upwards as it dies.
		move(0, 0.1, 0);
		aI = yaw += 20.0F;

		// When animation is finished, slay the dergon.
		if(by == 200)
		{
			if(!world.isClientSide)
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
	private EntityHuman ridingPlayer = null;
	private final int dergonID;
}
