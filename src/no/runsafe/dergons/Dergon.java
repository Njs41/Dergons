package no.runsafe.dergons;

import net.minecraft.server.v1_7_R3.*;
import no.runsafe.framework.api.ILocation;
import no.runsafe.framework.api.IWorld;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.framework.internal.wrapper.ObjectUnwrapper;
import no.runsafe.framework.minecraft.Item;
import no.runsafe.framework.minecraft.entity.RunsafeFallingBlock;
import org.bukkit.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Dergon extends EntityEnderDragon
{
	private final float PI_FLOAT = 3.1415927F;
	private final double PI_DOUBLE = 3.1415927410125732D;

	//Dergon X Accessor and Mutator
	private double getDergonX()
	{
		return h;
	}

	private void setDergonX(double x)
	{
		h = x;
	}

	//Dergon Y Accessor and Mutator
	private double getDergonY()
	{
		return i;
	}

	private void setDergonY(double y)
	{
		i = y;
	}

	//Dergon Z Accessor and Mutator
	private double getDergonZ()
	{
		return bm;
	}

	private void setDergonZ(double z)
	{
		bm = z;
	}

    //Unknown float0 in EntityInsentient
	private float unknownFloat0 = f;

	private void setUnknownFloat0(float x)
	{
		f = x;
	}

	/**
	 * Dergon constructor.
	 * @param world World to spawn in.
	 * @param handler
	 * @param targetLocation Coordinates of the dergon's dinner.
	 * @param dergonID Dergon's ID.
	 */
	public Dergon(IWorld world, DergonHandler handler, ILocation targetLocation, int dergonID)
	{
		super(ObjectUnwrapper.getMinecraft(world));
		this.handler = handler;
		this.targetLocation = targetLocation;
		this.targetWorld = targetLocation.getWorld();
		this.dergonID = dergonID;
	}

	/**
	 * Selects someone new for the dergon to eat.
	 */
	private void updateCurrentTarget()
	{
		bz = false;

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

				if (!unluckyChum.isVanished()
					&& !unluckyChum.isDead()
					&& unluckyChum.getGameMode() != GameMode.CREATIVE
				)//TODO: in 1.8 also check for spectator mode
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
			setDergonX(locX + random.nextInt(200) + -100);
			setDergonY(random.nextInt(100) + 70); // Somewhere above 70 to prevent floor clipping.
			setDergonZ(locZ + random.nextInt(200) + -100);
			flyOffLocation = targetWorld.getLocation(getDergonX(), getDergonY(), getDergonZ());// Store the target fly-off location.
			return;
		}
		else
		{
			List<IPlayer> players = targetLocation.getPlayersInRange(200); // Grab all players in 200 blocks.
			List<IPlayer> targets = new ArrayList<IPlayer>(0);

			for (IPlayer player : players)
			{
				// Skip the player if we're vanished or in creative mode.
				if (player.isVanished()
					|| player.getGameMode() == GameMode.CREATIVE
					|| isRidingPlayer(player.getName())
				)//TODO: in 1.8 also check for spectator mode
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
		setDergonX(targetLocation.getX());
		setDergonY(targetLocation.getY());
		setDergonZ(targetLocation.getZ());

		targetEntity = null;
	}

	/**
	 * Update function for Dergons.
	 * Names of this function in various spigot versions:
	 * v1_7_R3: e
	 * v1_8_R3: m
	 */
	@Override
	public void e()
	{
		// Throw a player off it's back if we're high up.
		if (ridingPlayer != null && locY >= 90)
		{
			ridingPlayer.mount(null);
			ridingPlayer = null;
		}

		// Update the health bar to show the percentage of the dergon
		long healthPercentage = Math.round((getHealth() / getMaxHealth()) * 100);
		setCustomName("Dergon (" + healthPercentage + "%)");

		ILocation dergonLocation = targetWorld.getLocation(locX, locY, locZ);
		if (targetEntity != null && dergonLocation != null && random.nextFloat() < 0.2F)
			((RunsafeFallingBlock) targetWorld.spawnFallingBlock(dergonLocation, Item.Unavailable.Fire)).setDropItem(false);

		if (world.isStatic)//TODO: In 1.8 replace with .isClientSide
		{
			setUnknownFloat0(MathHelper.cos(by * PI_FLOAT * 2.0F));
			float floatValue0 = MathHelper.cos(bx * PI_FLOAT * 2.0F);
			if (floatValue0 <= -0.3F && unknownFloat0 >= -0.3F)
				world.a(locX, locY, locZ, "mob.enderdragon.wings", 5.0F, 0.8F + random.nextFloat() * 0.3F, false);
		}

		bx = by;

		if (getHealth() <= 0.0F) // Check if the dragon is dead.
		{
			// If we're dead, play a random explosion effect at a random offset to it's corpse.
			world.addParticle(
					"largeexplode",
					locX + (double) ((random.nextFloat() - 0.5F) * 8.0F),
					locY + (double) ((random.nextFloat() - 0.5F) * 4.0F) + 2.0D,
					locZ + (double) ((random.nextFloat() - 0.5F) * 8.0F),
					0.0D,
					0.0D,
					0.0D
			);
		}
		else
		{ // Function ends right after this code block.
			this.bN();//Function name changed to .cc() in 1.8. Getter for a boolean value.
			setUnknownFloat0(0.2F / (MathHelper.sqrt(motX * motX + motZ * motZ) * 10.0F + 1.0F));
			setUnknownFloat0(unknownFloat0 * (float) Math.pow(2.0D, motY));
			by += (bA ? unknownFloat0 * 0.5F : unknownFloat0);

			yaw = MathHelper.g(yaw);
			if (bo < 0)
			{
				for (int forLoopIndex = 0; forLoopIndex < bn.length; ++forLoopIndex)
				{
					bn[forLoopIndex][0] = (double) yaw;
					bn[forLoopIndex][1] = locY;
				}
			}

			if (++bo == bn.length)
				bo = 0;

			bn[bo][0] = (double) yaw;
			bn[bo][1] = locY;
			double valueX;
			double valueY;
			double valueZ;
			double doubleValue3;
			float floatValue3;

			if (world.isStatic)//TODO: In 1.8 replace with .isClientSide
			{
				if (bg > 0)
				{
					valueX = locX + (bh - locX) / bg;
					valueY = locY + (bi - locY) / bg;
					valueZ = locZ + (bj - locZ) / bg;
					doubleValue3 = MathHelper.g(bk - (double) yaw);//.g trims degree value to within -180 to 180
					yaw = (float) ((double) yaw + doubleValue3 / bg);
					pitch = (float) ((double) pitch + (bl - (double) pitch) / bg);
					--bg;
					setPosition(valueX, valueY, valueZ);
					b(yaw, pitch);
				}
			}
			else
			{
				valueX = getDergonX() - locX;
				valueY = getDergonY() - locY;
				valueZ = getDergonZ() - locZ;
				doubleValue3 = valueX * valueX + valueY * valueY + valueZ * valueZ;
				if (targetEntity != null)
				{
					setDergonX(targetEntity.locX);
					setDergonZ(targetEntity.locZ);
					double doubleValue4 = getDergonX() - locX;
					double doubleValue5 = getDergonZ() - locZ;
					double doubleValue6 = Math.sqrt(doubleValue4 * doubleValue4 + doubleValue5 * doubleValue5);
					double doubleValue7 = 0.4000000059604645D + doubleValue6 / 80.0D - 1.0D;

					if (doubleValue7 > 10.0D)
						doubleValue7 = 10.0D;

					setDergonY(targetEntity.boundingBox.b + doubleValue7);
				}
				else
				{
					setDergonX(getDergonX() + random.nextGaussian() * 2.0D);
					bm += random.nextGaussian() * 2.0D;
				}

				if (bz || doubleValue3 < 100.0D || doubleValue3 > 22500.0D || positionChanged || G)
					updateCurrentTarget();

				valueY /= (double) MathHelper.sqrt(valueX * valueX + valueZ * valueZ);
				floatValue3 = 0.6F;
				if (valueY < (double) (-floatValue3))
					valueY = (double) (-floatValue3);

				if (valueY > (double) floatValue3)
					valueY = (double) floatValue3;

				motY += valueY * 0.10000000149011612D;
				yaw = MathHelper.g(yaw);
				double doubleValue8 = 180.0D - Math.atan2(valueX, valueZ) * 180.0D / PI_DOUBLE;
				double doubleValue9 = MathHelper.g(doubleValue8 - (double) yaw);

				if (doubleValue9 > 50.0D)
					doubleValue9 = 50.0D;

				if (doubleValue9 < -50.0D)
					doubleValue9 = -50.0D;

				float yawRadians = yaw * PI_FLOAT / 180.0F; //Convert yaw from degrees to radians
				Vec3D vec3d = Vec3D.a(getDergonX() - locX, getDergonY() - locY, bm - locZ).a();
				Vec3D vec3d1 = Vec3D.a((double) MathHelper.sin(yawRadians), motY, (double) (-MathHelper.cos(yawRadians))).a();
				float floatValue4 = (float) (vec3d1.b(vec3d) + 0.5D) / 1.5F;

				if (floatValue4 < 0.0F)
					floatValue4 = 0.0F;

				bf *= 0.8F;
				float floatValue5 = MathHelper.sqrt(motX * motX + motZ * motZ) * 1.0F + 1.0F;
				double doubleValue10 = Math.sqrt(motX * motX + motZ * motZ) * 1.0D + 1.0D;

				if (doubleValue10 > 40.0D)
					doubleValue10 = 40.0D;

				bf = (float) ((double) bf + doubleValue9 * (0.699999988079071D / doubleValue10 / (double) floatValue5));
				yaw += bf * 0.1F;
				float floatValue6 = (float) (2.0D / (doubleValue10 + 1.0D));
				float floatValue7 = 0.06F;

				a(0.0F, -1.0F, floatValue7 * (floatValue4 * floatValue6 + (1.0F - floatValue6)));
				if (bA)
					move(motX * 0.800000011920929D, motY * 0.800000011920929D, motZ * 0.800000011920929D);
				else
					move(motX, motY, motZ);

				Vec3D vec3d2 = Vec3D.a(motX, motY, motZ).a();
				float floatValue8 = (float) (vec3d2.b(vec3d1) + 1.0D) / 2.0F;

				floatValue8 = 0.8F + 0.15F * floatValue8;
				motX *= (double) floatValue8;
				motZ *= (double) floatValue8;
				motY *= 0.9100000262260437D;
			}

			aN = yaw;
			bq.width = bq.length = 3.0F;//Head
			bs.width = bs.length = 2.0F;//Tail
			bt.width = bt.length = 2.0F;//Tail
			bu.width = bu.length = 2.0F;//Tail
			br.length = 3.0F;//Body
			br.width = 5.0F;
			bv.length = 2.0F;//Wing
			bv.width = 4.0F;
			bw.length = 3.0F;//Wing
			bw.width = 4.0F;
			float floatValue1 = (float) (b(5, 1.0F)[1] - b(10, 1.0F)[1]) * 10.0F / 180.0F * PI_FLOAT;
			float cosFloat1 = MathHelper.cos(floatValue1);
			float negSinFloat1 = -MathHelper.sin(floatValue1);
			float yawRadians = yaw * PI_FLOAT / 180.0F; //Convert yaw from degrees to radians
			float sinYaw = MathHelper.sin(yawRadians);//11
			float cosYaw = MathHelper.cos(yawRadians);//12

			//Rotate that body
			br.h();
			br.setPositionRotation(
					locX + (double) (sinYaw * 0.5F),
					locY,
					locZ - (double) (cosYaw * 0.5F),
					0.0F,
					0.0F
			);
			//Rotate wing
			bv.h();
			bv.setPositionRotation(
					locX + (double) (cosYaw * 4.5F),
					locY + 2.0D,
					locZ + (double) (sinYaw * 4.5F),
					0.0F,
					0.0F
			);
			//Rotate wing
			bw.h();
			bw.setPositionRotation(
					locX - (double) (cosYaw * 4.5F),
					locY + 2.0D,
					locZ - (double) (sinYaw * 4.5F),
					0.0F,
					0.0F
			);

			if (!world.isStatic && hurtTicks == 0)//TODO: In 1.8 replace .isStatic with .isClientSide
			{
				a(world.getEntities(this, bv.boundingBox.grow(4.0D, 2.0D, 4.0D).d(0.0D, -2.0D, 0.0D)));
				a(world.getEntities(this, bw.boundingBox.grow(4.0D, 2.0D, 4.0D).d(0.0D, -2.0D, 0.0D)));
				b(world.getEntities(this, bq.boundingBox.grow(1.0D, 1.0D, 1.0D)));
			}

			double[] adouble = b(5, 1.0F);
			double[] adouble1 = b(0, 1.0F);

			floatValue3 = MathHelper.sin(yaw * PI_FLOAT / 180.0F - bg * 0.01F);
			float floatValue13 = MathHelper.cos((yaw * PI_FLOAT / 180.0F) - bg * 0.01F);

			//Rotate head
			bq.h();
			bq.setPositionRotation(
					locX + (double) (floatValue3 * 5.5F * cosFloat1),
					locY + (double) (negSinFloat1 * 5.5F) + (adouble1[1] - adouble[1]) * 1.0D,
					locZ - (double) (floatValue13 * 5.5F * cosFloat1),
					0.0F,
					0.0F
			);

			//Move the tail
			for (int tailNumber = 0; tailNumber < 3; ++tailNumber)
			{
				EntityComplexPart tailSection = null;

				switch (tailNumber)
				{
					case 0: tailSection = bs; break;
					case 1: tailSection = bt; break;
					case 2: tailSection = bu; break;
				}

				double[] adouble2 = b(12 + tailNumber * 2, 1.0F);
				float float14 = yawRadians + b(adouble2[0] - adouble[0]) * (PI_FLOAT / 180.0F) * 1.0F;
				float sinFloat14 = MathHelper.sin(float14);
				float cosFloat14 = MathHelper.cos(float14);
				float constValue17 = 1.5F;
				float floatValue18 = (float) (tailNumber + 1) * 2.0F; // 2, 4, 6

				tailSection.h();
				tailSection.setPositionRotation(
						locX - (double) ((sinYaw * constValue17 + sinFloat14 * floatValue18) * cosFloat1),
						locY + (adouble2[1] - adouble[1]) * 1.0D - (double) ((floatValue18 + constValue17) * negSinFloat1) + 1.5D,
						locZ + (double) ((cosYaw * constValue17 + cosFloat14 * floatValue18) * cosFloat1),
						0.0F,
						0.0F
				);
			}

			if (!world.isStatic) //TODO: In 1.8 replace with .isClientSide
				bA = a(bq.boundingBox) | a(br.boundingBox);
		}
	}

	private void a(List list)
	{
		double bodyBoundingBoxValue0 = (br.boundingBox.a + br.boundingBox.d) / 2.0D;
		double bodyBoundingBoxValue1 = (br.boundingBox.c + br.boundingBox.f) / 2.0D;

		for (Object rawEntity : list)
		{
			Entity entity = (Entity) rawEntity;
			if (entity instanceof EntityLiving)
			{
				double doubleValue2 = entity.locX - bodyBoundingBoxValue0;
				double doubleValue3 = entity.locZ - bodyBoundingBoxValue1;
				double LineLengthSquared = doubleValue2 * doubleValue2 + doubleValue3 * doubleValue3;

				entity.g(//Change Velocity
						doubleValue2 / LineLengthSquared * 4.0D,
						0.20000000298023224D,
						doubleValue3 / LineLengthSquared  * 4.0D
				);
			}
		}
	}

	/**
	 * Attack list of EntityLiving with 20.0F damage.
	 * @param list Liest of entities to attack.
	 */
	private void b(List list)
	{
		for (Object rawEntity : list)
		{
			Entity entity = (Entity) rawEntity;

			if (entity instanceof EntityLiving)
				entity.damageEntity(DamageSource.mobAttack(this), 20.0F);
		}
	}

	private boolean a(AxisAlignedBB axisalignedbb)
	{
		return false;
	}

	private float b(double d0)
	{
		return (float) MathHelper.g(d0);
	}

	/**
	 * Damage the dergon.
	 * Names of this function in various spigot versions:
	 * v1_7_R3: d, returns void
	 * v1_8_R3: d, returns boolean
	 * @param source damage source
	 * @param f Damage amount
	 * @return In 1.8 true if damaged, false if not damaged. Returns void in 1.7.
	 */
	@Override
	protected void d(DamageSource source, float f)
	{
		if (ridingPlayer == null || !isRidingPlayer(source.getEntity().getName()))
			super.d(source, handler.handleDergonDamage(this, source, f));
	}

	/**
	 * Handle dergon death.
	 * Names of this function in various spigot versions:
	 * v1_7_R3: aE
	 * v1_8_R3: aZ
 	 */
	@Override
	protected void aE()
	{
		super.aE();
		if (this.bB == 200)
			handler.handleDergonDeath(this);
	}

	public IWorld getWorld()
	{
		return targetWorld;
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
