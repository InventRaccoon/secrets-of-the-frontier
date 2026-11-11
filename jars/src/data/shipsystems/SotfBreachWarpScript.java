package data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.campaign.skills.SotfCyberwarfare;
import data.scripts.utils.SotfMisc;

public class SotfBreachWarpScript extends BaseShipSystemScript {

	public boolean shownSparks = false;
	public boolean appliedRefresh = false;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		//boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
			//player = ship == Global.getCombatEngine().getPlayerShip();
		} else {
			return;
		}

		if (effectLevel == 0)  {
			shownSparks = false;
			if (appliedRefresh) {
				appliedRefresh = false;
				// if ship has no CWAR officer, need to do this to remove the key
				ship.getCustomData().remove(SotfCyberwarfare.CD_REFRESH_KEY);
			}
			return;
		}

		if (effectLevel > 0 && !shownSparks) {
			shownSparks = true;
			SotfMisc.ewarChargeVisuals(ship);
		}

		if (state == State.OUT && effectLevel < 0.5f && !appliedRefresh) {
			// set a key that the Cyberwarfare script will see, apply the CD refresh, and then remove
			// for compatibility with modded CWAR scripts like DK executive officer
			ship.setCustomData(SotfCyberwarfare.CD_REFRESH_KEY, true);
		}
	}

}








