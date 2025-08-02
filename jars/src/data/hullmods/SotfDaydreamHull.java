// Built-in for Dustkeeper Proxy drones
// Has no actual effect nowadays - upgrades are baked into the hull
package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.RealityDisruptorChargeGlow;
import com.fs.starfarer.api.impl.combat.RealityDisruptorChargeGlow.RDRepairRateDebuff;
import com.fs.starfarer.api.impl.combat.RealityDisruptorEffect;
import data.scripts.utils.SotfMisc;

import java.awt.*;

public class SotfDaydreamHull extends BaseHullMod {

	public static float SHIELD_RESIST = 0.12f;
	public static float EMP_DAMAGE_RESIST = 0.35f;
	public static float PIERCE_MULT = 0.5f;

	public void advanceInCombat(ShipAPI ship, float amount) {
		if (!Global.getCurrentState().equals(GameState.COMBAT)) {
			return;
		}

		// halve the effect of Reality Disruptor/Inimical Emanation
		if (ship.hasListenerOfClass(RDRepairRateDebuff.class)) {
			RDRepairRateDebuff debuff = ship.getListeners(RDRepairRateDebuff.class).get(0);
			if (debuff.dur > 2.5f) {
				debuff.dur = 2.5f;
			}
		}
	}

	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		float sizeMult = 1f;
		if (hullSize.equals(ShipAPI.HullSize.FIGHTER)) {
			sizeMult = 2;
		}
		stats.getEnergyShieldDamageTakenMult().modifyMult(id, 1f - (SHIELD_RESIST * sizeMult));
		//stats.getFragmentationShieldDamageTakenMult().modifyMult(id, 1f - (SHIELD_RESIST * 2f));
		stats.getDynamic().getStat(Stats.SHIELD_PIERCED_MULT).modifyMult(id, 1f - (PIERCE_MULT * sizeMult));
		stats.getEmpDamageTakenMult().modifyMult(id, 1f - (EMP_DAMAGE_RESIST * sizeMult));
	}

	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return "" + Math.round(SHIELD_RESIST * 100f) + "%";
		//if (index == 1) return "" + Math.round(SHIELD_RESIST * 2f * 100f) + "%";
		if (index == 1) return "" + Math.round(PIERCE_MULT * 100f) + "%";
		if (index == 2) return "" + Math.round(EMP_DAMAGE_RESIST * 100f) + "%";
		if (index == 3) return "doubled";
		if (index == 4) return "halves";
		return null;
	}
}
