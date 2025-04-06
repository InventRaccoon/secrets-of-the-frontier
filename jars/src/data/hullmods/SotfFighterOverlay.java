// applies a d-hull overlay to spawned fighters, does nothing else
package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class SotfFighterOverlay extends BaseHullMod {

	public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
		fighter.setMediumDHullOverlay();
	}
}




