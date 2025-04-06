package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class SotfSpecialDefense extends BaseHullMod {

	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ShieldAPI shield = ship.getShield();
		if (shield == null) {
			ship.setShield(ShieldAPI.ShieldType.PHASE, 0, 0, 0);
		}
	}

}
