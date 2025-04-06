// Built-in for Dustkeeper Proxy drones
// Has no actual effect nowadays - upgrades are baked into the hull
package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;

import java.awt.*;

public class SotfAuxAssault extends SotfBaseAuxPackage {

	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return "active defense system";
		if (index == 1) return "shield generator";
		if (index == 2) return "ordnance capacity";
		return null;
	}

//	private static int BURN_LEVEL_BONUS = 1;
//	private static float FLUX_DISSIPATION_PERCENT = 10f;
//	private static float ARMOR_BONUS = 50;
//
//	public static float HULL_PERCENT = 12f;
//	public static float ARMOR_PERCENT = 10f;
//	private static float FLUX_CAPACITY_PERCENT = 15f;
//
//	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
//		stats.getMaxBurnLevel().modifyFlat(id, BURN_LEVEL_BONUS);
//		stats.getFluxDissipation().modifyPercent(id, FLUX_DISSIPATION_PERCENT);
//		stats.getEffectiveArmorBonus().modifyFlat(id, ARMOR_BONUS);
//
//		stats.getHullBonus().modifyPercent(id, HULL_PERCENT);
//		stats.getArmorBonus().modifyPercent(id, ARMOR_PERCENT);
//		stats.getFluxCapacity().modifyPercent(id, FLUX_CAPACITY_PERCENT);
//
//		stats.getDynamic().getMod(Stats.CAN_REPAIR_MODULES_UNDER_FIRE).modifyFlat(id, 1f);
//	}
//
//	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
//		if (index == 0) return "" + Math.round(HULL_PERCENT) + "%";
//		if (index == 1) return "" + Math.round(ARMOR_PERCENT) + "%";
//		if (index == 2) return "" + Math.round(FLUX_CAPACITY_PERCENT) + "%";
//
//		if (index == 3) return "" + BURN_LEVEL_BONUS;
//		if (index == 4) return "" + Math.round(FLUX_DISSIPATION_PERCENT) + "%";
//		if (index == 5) return "" + Math.round(ARMOR_BONUS);
//		return null;
//	}
}
