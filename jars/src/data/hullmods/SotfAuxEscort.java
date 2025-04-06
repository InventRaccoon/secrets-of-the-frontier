// Built-in for Dustkeeper Proxy drones
// Upgrades are baked into the hull, this hullmod is used to detect ships to receive Steady AI
package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;

import java.awt.*;

public class SotfAuxEscort extends SotfBaseAuxPackage {

	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return "active defense system";
		if (index == 1) return "shield generator";
		if (index == 2) return "ordnance capacity";
		if (index == 3) return "less aggressive behavior";
		return null;
	}

//	private static int BURN_LEVEL_BONUS = 1;
//	private static float FLUX_DISSIPATION_PERCENT = 15f;
//	private static float ARMOR_BONUS = 50;
//
//	private static float MANEUVER_PERCENT = 25f;
//	private static float PD_RANGE = 100;
//	public static float FIGHTER_DAMAGE_BONUS = 35f;
//	public static float MISSILE_DAMAGE_BONUS = 35f;
//
//	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
//		stats.getMaxBurnLevel().modifyFlat(id, BURN_LEVEL_BONUS);
//		stats.getFluxDissipation().modifyPercent(id, FLUX_DISSIPATION_PERCENT);
//		stats.getEffectiveArmorBonus().modifyFlat(id, ARMOR_BONUS);
//
//		stats.getDamageToFighters().modifyFlat(id, FIGHTER_DAMAGE_BONUS / 100f);
//		stats.getDamageToMissiles().modifyFlat(id, MISSILE_DAMAGE_BONUS / 100f);
//
//		stats.getBeamPDWeaponRangeBonus().modifyFlat(id, PD_RANGE);
//		stats.getNonBeamPDWeaponRangeBonus().modifyFlat(id, PD_RANGE);
//
//		stats.getAcceleration().modifyPercent(id, MANEUVER_PERCENT);
//		stats.getDeceleration().modifyPercent(id, MANEUVER_PERCENT);
//		stats.getTurnAcceleration().modifyPercent(id, MANEUVER_PERCENT * 2f);
//		stats.getMaxTurnRate().modifyPercent(id, MANEUVER_PERCENT);
//	}
//
//	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
//		if (index == 0) return "" + Math.round(MANEUVER_PERCENT) + "%";
//		if (index == 1) return "" + Math.round(PD_RANGE);
//		if (index == 2) return "" + Math.round(FIGHTER_DAMAGE_BONUS) + "%";
//
//		if (index == 3) return "" + BURN_LEVEL_BONUS;
//		if (index == 4) return "" + Math.round(FLUX_DISSIPATION_PERCENT) + "%";
//		if (index == 5) return "" + ARMOR_BONUS;
//		return null;
//	}
}
