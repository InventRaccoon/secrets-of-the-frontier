package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Handles Cult of the Daydream drone launchers
 * One script to handle multiple launchers - just need to add entries to WING_IDS
 */

public class SotfBarbMountEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {

	public static Map<String, String> WING_IDS = new HashMap<>();

	static {
		WING_IDS.put("sotf_barbmount", "sotf_barb_launched_wing");
		WING_IDS.put("sotf_barbrail", "sotf_barb_launched_wing");
		WING_IDS.put("sotf_nettlerail", "sotf_nettle_launched_wing");
	}

	protected FighterWingAPI currWing = null;
	
	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		ShipAPI ship = weapon.getShip();
		if (ship == null) return;

//		if (currWing != null) {
//			if (currWing.isDestroyed()) {
//				engine.getFleetManager(currWing.getWingOwner()).removeDeployed(currWing, false);
//				currWing = null;
//			}
//		}
	}
	
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		
		//FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(ATTACK_SWARM_WING);

		CombatFleetManagerAPI manager = engine.getFleetManager(projectile.getOwner());
		manager.setSuppressDeploymentMessages(true);
		ShipAPI leader = manager.spawnShipOrWing(getWingId(weapon),
								projectile.getLocation(), projectile.getFacing(), 0f, null);
		leader.getWing().setSourceShip(projectile.getSource());
		manager.removeDeployed(leader.getWing(), false);
		manager.setSuppressDeploymentMessages(false);
		
		//Global.getSoundPlayer().playSound("threat_swarm_launched", 1f, 1f, projectile.getLocation(), takeoffVel);

		for (ShipAPI curr : leader.getWing().getWingMembers()) {
			// try join another nearby drone wing
			FighterWingAPI toJoin = findAlliedWing(projectile.getLocation(), weapon);
			if (toJoin != null) {
				if (curr.getWing() != null) {
					curr.getWing().removeMember(curr);
					// really important, otherwise this doesn't get cleaned up
					manager.removeDeployed(curr.getWing(), false);
				}
				curr.setWing(toJoin);
				toJoin.addMember(curr);
			}

			// inherit projectile velocity
			Vector2f.add(curr.getVelocity(), projectile.getVelocity(), curr.getVelocity());
		}
		
		engine.removeEntity(projectile);
	}

	public String getWingId(WeaponAPI weapon) {
		return WING_IDS.get(weapon.getId());
	}

	public FighterWingAPI findAlliedWing(Vector2f from, WeaponAPI weapon) {
		float range = weapon.getRange();
		Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
				range * 2f, range * 2f);
		int owner = weapon.getShip().getOwner();
		FighterWingAPI best = null;
		float minScore = Float.MAX_VALUE;

		ShipAPI ship = weapon.getShip();

		while (iter.hasNext()) {
			Object o = iter.next();
			if (!(o instanceof ShipAPI otherShip)) continue;
			if (otherShip.isHulk()) continue;
			if (!otherShip.isFighter()) continue;
			if (otherShip.getWing() == null) continue;
			if (!otherShip.isWingLeader()) continue;
			if (!otherShip.getWing().getWingId().equals(getWingId(weapon))) continue;
			if (!otherShip.getWing().getSourceShip().equals(ship)) continue;
			if (otherShip.getWing().getWingMembers().size() > 5) continue;

			float dist = Misc.getDistance(from, otherShip.getLocation());
			if (dist > range) continue;

			float score = dist;

			if (score < minScore) {
				minScore = score;
				best = otherShip.getWing();
			}
		}
		return best;
	}
}








