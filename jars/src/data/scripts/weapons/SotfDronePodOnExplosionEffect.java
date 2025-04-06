package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual.NEParams;
import com.fs.starfarer.api.impl.combat.RiftCascadeEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SotfDronePodOnExplosionEffect implements ProximityExplosionEffect {
	
	public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {
		ArrayList<ShipAPI> fighters = new ArrayList<ShipAPI>();

		String variantId = "sotf_spark_dronestrike_wing";
		FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(variantId);
		ShipAPI [] ships = new ShipAPI[spec.getNumFighters()];

		CombatFleetManagerAPI fleetManager = Global.getCombatEngine().getFleetManager(explosion.getSource().getOriginalOwner());
		boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
		fleetManager.setSuppressDeploymentMessages(true);
		ShipAPI leader = fleetManager.spawnShipOrWing(variantId, explosion.getLocation(), 0f, 0f);
		fighters.add(leader);
		for (int i = 0; i < ships.length; i++) {
			ships[i] = leader.getWing().getWingMembers().get(i);
			ships[i].getLocation().set(leader.getLocation());
			fighters.add(ships[i]);
		}

		fleetManager.setSuppressDeploymentMessages(wasSuppressed);

		for (final ShipAPI fighter : fighters) {
			Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
				float timer = 0;
				// time until drone self-destructs
				final float max = 30f + ((float) Math.random());
				// drone begins with 50% damage resistance (from IR smoke), fading over this time
				final float resistanceTime = 1.5f;
				@Override
				public void advance(float amount, List<InputEventAPI> events) {
					if (timer <= resistanceTime) {
						fighter.getMutableStats().getHullDamageTakenMult().modifyMult("sotf_dronestrike", (timer * (0.5f / resistanceTime)) + 0.5f);
						fighter.getMutableStats().getArmorDamageTakenMult().modifyMult("sotf_dronestrike", (timer * (0.5f / resistanceTime)) + 0.5f);
						fighter.getMutableStats().getShieldDamageTakenMult().modifyMult("sotf_dronestrike", (timer * (0.5f / resistanceTime)) + 0.5f);
					} else {
						fighter.getMutableStats().getHullDamageTakenMult().unmodify("sotf_dronestrike");
						fighter.getMutableStats().getArmorDamageTakenMult().unmodify("sotf_dronestrike");
						fighter.getMutableStats().getShieldDamageTakenMult().unmodify("sotf_dronestrike");
					}
					if (Global.getCombatEngine().isPaused()) return;
					timer += amount * Global.getCombatEngine().getTimeMult().getModifiedValue();
					if (timer >= max) {
						Global.getCombatEngine().applyDamage(fighter, fighter.getLocation(), 1000, DamageType.HIGH_EXPLOSIVE, 0f, true, false, fighter);
						Global.getCombatEngine().removePlugin(this);
					}
				}
			});
		}

		// deploy a few decoy flares
		for (int i = 0; i < 5; i++) {
			Vector2f flareLoc = Misc.getPointWithinRadius(explosion.getLocation(), 50);
			Global.getCombatEngine().spawnProjectile(
					explosion.getSource(),
					null,
					"flarelauncher2",
					flareLoc,
					Misc.getAngleInDegrees(explosion.getLocation(), flareLoc),
					null
			);
		}

		// IR smoke visual
		for (int i = 0; i < 18; i++) {
			float dur = 2f + (float) Math.random();
			Vector2f loc = new Vector2f(explosion.getLocation());
			loc = Misc.getPointWithinRadius(loc, 100f);
			float s = 275f * (0.25f + (float) Math.random() * 0.25f);
			Global.getCombatEngine().addNebulaParticle(loc, explosion.getVelocity(), s, 1.5f, 0.1f, 0f, dur, new Color(35, 35, 35));
		}
	}
}



