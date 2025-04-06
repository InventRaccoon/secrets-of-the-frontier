// this is pretty self-explanatory
package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;

public class SotfNoVenting extends BaseHullMod {

	public void applyEffectsBeforeShipCreation(com.fs.starfarer.api.combat.ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getVentRateMult().modifyMult(id, 0f);
	}
}
