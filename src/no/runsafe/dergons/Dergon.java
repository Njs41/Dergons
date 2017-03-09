package no.runsafe.dergons;

import net.minecraft.server.v1_8_R3.*;
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

/*
* Names of obfuscated variables and their names in various spigot versions:
*
* Variables in EntityEnderDragon:
* Type							Name given	v1_7_R3		v1_8_R3		v1_9_R2
* public double					var1		h			a			?
* public double					var2		i			b			?
* public double					var3		bm			c			?
* public double[][]				bk			bn			bk			?
* public int					var5		bo			bl			c
* public EntityComplexPart		var6		bq			bn			bv
* public EntityComplexPart		var7		br			bo			bw
* public EntityComplexPart		var8		bs			bp			bx
* public EntityComplexPart		var9		bt			bq			by
* public EntityComplexPart		var10		bu			br			bz
* public EntityComplexPart		var11		bv			bs			bA
* public EntityComplexPart		var12		bw			bt			bB
* public EntityComplexPart		N/A			N/A			bC 	//Don't know what this is, all of the EntityComplexParts for 1.9 could be off.
* public float					var13		bx			bu			bD
* public float					var14		by			bv			bE
* public boolean				var15		bz			bw			bF(maybe, probably not)
* public boolean				var16		bA			bx			bF(maybe, probably not)
* public int					var17		bB			by			bG
*
*Entity.class:
*public double 					var18		j			j
*public boolean					var26		G			F
*
* EntityLiving.Class:
* protected int					var19		bg			bc
* protected double				var20		bh			bd
* protected double 				var21		bi			be
* protected double				var22		bj			bf
* public float					var23		aN			aJ
* protected double				var24		bk			bg
* protected float				var26		bf			bb
*
* More obfuscated variables in 1.9.
 */
public class Dergon extends EntityEnderDragon
{
	//Please forgive me gods of Java

	//Variables apart of EntityEnderDragon
	private double var1 = a;
	private double var2 = b;
	private double var3 = c;
	private int var5 = bl;
	private EntityComplexPart var6 = bn;
	private EntityComplexPart var7 = bo;
	private EntityComplexPart var8 = bp;
	private EntityComplexPart var9 = bq;
	private EntityComplexPart var10 = br;
	private EntityComplexPart var11 = bs;
	private EntityComplexPart var12 = bt;
	private float var13 = bu;
	private float var14 = bv;
	private boolean var15 = bw;
	private boolean var16 = bx;
	private int var17 = by;

	//Variables a part of Entity.class
	private double var18 = j;
	private boolean var26 = F;

	//variables a part of entityLiving.class
	private int var19 = bc;
	private double var20 = bd;
	private double var21 = be;
	private double var22 = bf;
	private float var23 = aJ;
	private double var24 = bg;
	private float var25 = bb;


	public Dergon(IWorld world, DergonHandler handler, ILocation targetLocation, int dergonID)
	{
		super(ObjectUnwrapper.getMinecraft(world));
		this.handler = handler;
		this.targetLocation = targetLocation;
		this.targetWorld = targetLocation.getWorld();
		this.dergonID = dergonID;
	}

	private void bO()
	{
		var15 = false;

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

				if (!unluckyChum.isVanished() && !unluckyChum.isDead() && unluckyChum.getGameMode() != GameMode.CREATIVE)
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
			var1 = locX + random.nextInt(200) + -100;
			var2 = random.nextInt(100) + 70; // Somewhere above 70 to prevent floor clipping.
			var18 = locZ + random.nextInt(200) + -100;
			flyOffLocation = targetWorld.getLocation(var1, var2, var18); // Store the target fly-off location.
			return;
		}
		else
		{
			List<IPlayer> players = targetLocation.getPlayersInRange(200); // Grab all players in 200 blocks.
			List<IPlayer> targets = new ArrayList<IPlayer>(0);

			for (IPlayer player : players)
			{
				// Skip the player if we're vanished or in creative or spectator mode.
				if (
						player.isVanished() ||
						player.getGameMode() == GameMode.CREATIVE ||
						player.getGameMode() == GameMode.SPECTATOR ||
						isRidingPlayer(player.getName())
				)
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
		var1 = targetLocation.getX();
		var2 = targetLocation.getY();
		var18 = targetLocation.getZ();

		targetEntity = null;
	}

	/*
	* Names of this function in various spigot versions:
	* v1_7_R3: e
	* v1_8_R3: m
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

		ILocation dergonLocation = targetWorld.getLocation(locX, locY, locZ);
		if (targetEntity != null && dergonLocation != null && random.nextFloat() < 0.2F)
			((RunsafeFallingBlock) targetWorld.spawnFallingBlock(dergonLocation, Item.Unavailable.Fire)).setDropItem(false);

		float floatVar0;
		float floatVar1;

		if (world.isClientSide)
		{
			floatVar0 = MathHelper.cos(var14 * 3.1415927F * 2.0F);
			floatVar1 = MathHelper.cos(var13 * 3.1415927F * 2.0F);
			if (floatVar1 <= -0.3F && floatVar0 >= -0.3F)
				world.a(locX, locY, locZ, "mob.enderdragon.wings", 5.0F, 0.8F + random.nextFloat() * 0.3F, false);
		}

		var13 = var14;
		float floatVar2;

		if (getHealth() <= 0.0F) // Check if the dragon is dead.
		{
			// If we're dead, play a random explosion effect at a random offset to it's corpse.
			floatVar0 = (random.nextFloat() - 0.5F) * 8.0F;
			floatVar1 = (random.nextFloat() - 0.5F) * 4.0F;
			floatVar2 = (random.nextFloat() - 0.5F) * 8.0F;
			world.addParticle(
					EnumParticle.EXPLOSION_LARGE,
					locX + (double) floatVar0,
					locY + 2.0D + (double) floatVar1,
					locZ + (double) floatVar2,
					0.0D,
					0.0D,
					0.0D);
		}
		else
		{
			//this.bN();//TODO: find out what bN(); was changed to what it does.
			floatVar0 = 0.2F / (MathHelper.sqrt(motX * motX + motZ * motZ) * 10.0F + 1.0F);
			floatVar0 *= (float) Math.pow(2.0D, motY);
			var14 += (var16 ? floatVar0 * 0.5F : floatVar0);

			yaw = MathHelper.g(yaw);
			if (var5 < 0)
			{
				for (int d05 = 0; d05 < bk.length; ++d05)
				{
					bk[d05][0] = (double) yaw;
					bk[d05][1] = locY;
				}
			}

			if (++var5 == bk.length)
				var5 = 0;

			bk[var5][0] = (double) yaw;
			bk[var5][1] = locY;
			double double0;
			double double1;
			double double2;
			double double3;
			float floatValue3;

			if (world.isClientSide)
			{
				if (var19 > 0)
				{
					double0 = locX + (var20 - locX) / var19;
					double1 = locY + (var21 - locY) / var19;
					double2 = locZ + (var22 - locZ) / var19;
					double3 = MathHelper.g(var24 - (double) yaw);
					yaw = (float) ((double) yaw + double3 / var19);
					pitch = (float) ((double) pitch + (bl - (double) pitch) / var19);
					--var19;
					setPosition(double0, double1, double2);
					b(yaw, pitch);//TODO: Fix error
				}
			}
			else
			{
				double0 = var1 - locX;
				double1 = var2 - locY;
				double2 = var18 - locZ;
				double3 = double0 * double0 + double1 * double1 + double2 * double2;
				if (targetEntity != null)
				{
					var1 = targetEntity.locX;
					var18 = targetEntity.locZ;
					double double4 = var1 - locX;
					double double5 = var18 - locZ;
					double double6 = Math.sqrt(double4 * double4 + double5 * double5);
					double double7 = 0.4000000059604645D + double6 / 80.0D - 1.0D;

					if (double7 > 10.0D)
						double7 = 10.0D;

					var2 = targetEntity.getBoundingBox().b + double7;
				}
				else
				{
					var1 += random.nextGaussian() * 2.0D;
					var3 += random.nextGaussian() * 2.0D;
				}

				if (var15 || double3 < 100.0D || double3 > 22500.0D || positionChanged || var26)
					bO();/* TODO */

				double1 /= (double) MathHelper.sqrt(double0 * double0 + double2 * double2);
				floatValue3 = 0.6F;
				if (double1 < (double) (-floatValue3))
					double1 = (double) (-floatValue3);

				if (double1 > (double) floatValue3)
					double1 = (double) floatValue3;

				motY += double1 * 0.10000000149011612D;
				yaw = MathHelper.g(yaw);
				double d8 = 180.0D - Math.atan2(double0, double2) * 180.0D / 3.1415927410125732D;
				double d9 = MathHelper.g(d8 - (double) yaw);

				if (d9 > 50.0D)
					d9 = 50.0D;

				if (d9 < -50.0D)
					d9 = -50.0D;

				Vec3D vec3d = Vec3D.a(//TODO: Fix Error:(322, 52) java: non-static method a(double,double,double) cannot be referenced from a static context
						var1 - locX, 
						var2 - locY, 
						var3 - locZ
				).a();
				
				Vec3D vec3d1 = Vec3D.a(//TODO: Fix Error:(328, 52) java: non-static method a(double,double,double) cannot be referenced from a static context
						(double) MathHelper.sin(yaw * 3.1415927F / 180.0F),
						motY,
						(double) (-MathHelper.cos(yaw * 3.1415927F / 180.0F))
				).a();
				
				float floatValue4 = (float) (vec3d1.b(vec3d) + 0.5D) / 1.5F;

				if (floatValue4 < 0.0F)
					floatValue4 = 0.0F;

				var25 *= 0.8F;
				float floatValue5 = MathHelper.sqrt(motX * motX + motZ * motZ) * 1.0F + 1.0F;
				double double10 = Math.sqrt(motX * motX + motZ * motZ) * 1.0D + 1.0D;

				if (double10 > 40.0D)
					double10 = 40.0D;

				var25 = (float) ((double) var25 + d9 * (0.699999988079071D / double10 / (double) floatValue5));
				yaw += var25 * 0.1F;
				float floatValue6 = (float) (2.0D / (double10 + 1.0D));
				float floatValue7 = 0.06F;

				a(0.0F, -1.0F, floatValue7 * (floatValue4 * floatValue6 + (1.0F - floatValue6)));
				if (var16)
					move(motX * 0.800000011920929D, motY * 0.800000011920929D, motZ * 0.800000011920929D);
				else
					move(motX, motY, motZ);

				Vec3D vec3d2 = Vec3D.a(motX, motY, motZ).a();//TODO: Fix Error:(357, 53) java: non-static method a(double,double,double) cannot be referenced from a static context
				float floatValue8 = (float) (vec3d2.b(vec3d1) + 1.0D) / 2.0F;

				floatValue8 = 0.8F + 0.15F * floatValue8;
				motX *= (double) floatValue8;
				motZ *= (double) floatValue8;
				motY *= 0.9100000262260437D;
			}

			var23= yaw;
			var6.width = var6.length = 3.0F;
			var8.width = var8.length = 2.0F;
			var9.width = var9.length = 2.0F;
			var10.width = var10.length = 2.0F;
			var7.length = 3.0F;
			var7.width = 5.0F;
			var11.length = 2.0F;
			var11.width = 4.0F;
			var12.length = 3.0F;
			var12.width = 4.0F;
			floatVar1 = (float) (b(5, 1.0F)[1] - b(10, 1.0F)[1]) * 10.0F / 180.0F * 3.1415927F;
			floatVar2 = MathHelper.cos(floatVar1);
			float f9 = -MathHelper.sin(floatVar1);
			float f10 = yaw * 3.1415927F / 180.0F;
			float f11 = MathHelper.sin(f10);
			float f12 = MathHelper.cos(f10);

			var7.t_();
			var7.setPositionRotation(locX + (double) (f11 * 0.5F), locY, locZ - (double) (f12 * 0.5F), 0.0F, 0.0F);
			var11.t_();
			var11.setPositionRotation(locX + (double) (f12 * 4.5F), locY + 2.0D, locZ + (double) (f11 * 4.5F), 0.0F, 0.0F);
			var12.t_();
			var12.setPositionRotation(locX - (double) (f12 * 4.5F), locY + 2.0D, locZ - (double) (f11 * 4.5F), 0.0F, 0.0F);

			if (!world.isClientSide && hurtTicks == 0)
			{
				a(world.getEntities(this, var11.getBoundingBox().grow(4.0D, 2.0D, 4.0D).d(0.0D, -2.0D, 0.0D)));//TODO: Fix error
				a(world.getEntities(this, var12.getBoundingBox().grow(4.0D, 2.0D, 4.0D).d(0.0D, -2.0D, 0.0D)));
				b(world.getEntities(this, var6.getBoundingBox().grow(1.0D, 1.0D, 1.0D)));
			}

			double[] adouble = b(5, 1.0F);
			double[] adouble1 = b(0, 1.0F);

			floatValue3 = MathHelper.sin(yaw * 3.1415927F / 180.0F - var19 * 0.01F);
			float f13 = MathHelper.cos(yaw * 3.1415927F / 180.0F - var19 * 0.01F);

			var6.t_();
			var6.setPositionRotation(
					locX + (double) (floatValue3 * 5.5F * floatVar2),
					locY + (adouble1[1] - adouble[1]) * 1.0D + (double) (f9 * 5.5F),
					locZ - (double) (f13 * 5.5F * floatVar2),
					0.0F,
					0.0F
			);

			for (int indexForLoop = 0; indexForLoop < 3; ++indexForLoop)
			{
				EntityComplexPart entitycomplexpart = null;

				if (indexForLoop == 0)
					entitycomplexpart = var8;

				if (indexForLoop == 1)
					entitycomplexpart = var9;

				if (indexForLoop == 2)
					entitycomplexpart = var10;

				double[] adouble2 = b(12 + indexForLoop * 2, 1.0F);
				float f14 = yaw * 3.1415927F / 180.0F + b(adouble2[0] - adouble[0]) * 3.1415927F / 180.0F * 1.0F;
				float f15 = MathHelper.sin(f14);
				float f16 = MathHelper.cos(f14);
				float f17 = 1.5F;
				float f18 = (float) (indexForLoop + 1) * 2.0F;

				entitycomplexpart.t_();
				entitycomplexpart.setPositionRotation(
						locX - (double) ((f11 * f17 + f15 * f18) * floatVar2),
						locY + (adouble2[1] - adouble[1]) * 1.0D - (double) ((f18 + f17) * f9) + 1.5D, locZ + (double) ((f12 * f17 + f16 * f18) * floatVar2),
						0.0F,
						0.0F
				);
			}

			if (world.isClientSide)
				var16 = a(var6.getBoundingBox()) | a(var7.getBoundingBox());
		}
	}

	private void a(List list)
	{
		double double0 = (var7.getBoundingBox().a + var7.getBoundingBox().d) / 2.0D;
		double double1 = (var7.getBoundingBox().c + var7.getBoundingBox().f) / 2.0D;

		for (Object rawEntity : list)
		{
			Entity entity = (Entity) rawEntity;
			if (entity instanceof EntityLiving)
			{
				double double2 = entity.locX - double0;
				double double3 = entity.locZ - double1;
				double double4 = double2 * double2 + double3 * double3;

				entity.g(double2 / double4 * 4.0D, 0.20000000298023224D, double3 / double4 * 4.0D);
			}
		}
	}

	private void b(List list)
	{
		for (Object rawEntity : list)
		{
			Entity entity = (Entity) rawEntity;

			if (entity instanceof EntityLiving)
				entity.damageEntity(DamageSource.mobAttack(this), 20.0F);
		}
	}

	/*
	* TODO: Fix error
	* Error:(477, 25) java: a(net.minecraft.server.v1_8_R3.AxisAlignedBB) in no.runsafe.dergons.Dergon cannot override a(net.minecraft.server.v1_8_R3.AxisAlignedBB) in net.minecraft.server.v1_8_R3.Entity
 	* attempting to assign weaker access privileges; was public
	 */
	private boolean a(AxisAlignedBB axisalignedbb)
	{
		return false;
	}

	private float b(double d0)
	{
		return (float) MathHelper.g(d0);
	}

	/*
	* Names of this function in various spigot versions:
	* v1_7_R3: d, returns void
	* v1_8_R3: d, returns boolean
	 */
	@Override
	protected boolean d(DamageSource source, float f)
	{
		if (ridingPlayer == null || !isRidingPlayer(source.getEntity().getName()))
			super.d(source, handler.handleDergonDamage(this, source, f));
		return true;//TODO: Decide on what to return
	}

	/*
	* Names of this function in various spigot versions:
	* v1_7_R3: aE
	* v1_8_R3: aZ
	 */
	@Override
	protected void aZ()
	{
		super.aE();
		if (this.var17 == 200)
			handler.handleDergonDeath(this);
	}

	/*
	* TODO: Fix error
	* Error:(513, 23) java: getWorld() in no.runsafe.dergons.Dergon cannot implement getWorld() in net.minecraft.server.v1_8_R3.ICommandListener
 	* return type no.runsafe.framework.api.IWorld is not compatible with net.minecraft.server.v1_8_R3.World
	 */
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
