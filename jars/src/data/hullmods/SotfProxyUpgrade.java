// Assault Package for Dustkeeper Proxy drones
package data.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class SotfProxyUpgrade extends SotfBaseAuxPackage {

	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return "active defense system";
		if (index == 1) return "shield generator";
		if (index == 2) return "ordnance capacity";
		return null;
	}
}
