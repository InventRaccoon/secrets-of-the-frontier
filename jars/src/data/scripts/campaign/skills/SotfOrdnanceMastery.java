package data.scripts.campaign.skills;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;

import java.awt.*;
import java.util.EnumSet;

public class SotfOrdnanceMastery {

	public static float BALLISTIC_ROF_MULT = 0.3f;

	public static class BallisticFluxBoost extends BaseSkillEffectDescription implements ShipSkillEffect, AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new SotfOrdnanceMasteryROFMod(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(SotfOrdnanceMasteryROFMod.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}

		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
											TooltipMakerAPI info, float width) {
			initElite(stats, skill);

			info.addPara("Ballistic weapons fire up to %s faster, based on the firing ship's flux level, with a corresponding decrease in flux cost",
					0f, hc, hc,
					"+" + (int) (BALLISTIC_ROF_MULT * 100f) + "%"
			);
		}
	}

	public static class SotfOrdnanceMasteryROFMod implements AdvanceableListener {
		protected ShipAPI ship;
		public SotfOrdnanceMasteryROFMod(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {
			if (Global.getCurrentState() == GameState.COMBAT &&
					Global.getCombatEngine() != null) {
				float fluxLevel = ship.getFluxLevel();

				ship.getMutableStats().getBallisticRoFMult().modifyMult(SotfIDs.SKILL_ORDNANCEMASTERY, 1f + (BALLISTIC_ROF_MULT * fluxLevel));
				ship.getMutableStats().getBallisticWeaponFluxCostMod().modifyMult(SotfIDs.SKILL_ORDNANCEMASTERY, 1 / (1f + (BALLISTIC_ROF_MULT * fluxLevel)));

				ship.setWeaponGlow(fluxLevel, new Color(255,200,0, 155), EnumSet.of(WeaponAPI.WeaponType.BALLISTIC));

				int rofBonus = Math.round(BALLISTIC_ROF_MULT * fluxLevel * 100f);
				int fluxReduction = Math.round(1 / (1f + (BALLISTIC_ROF_MULT * fluxLevel)) * 100f);
				if (rofBonus > 0 && Global.getCombatEngine().getPlayerShip() == ship) {
					Global.getCombatEngine().maintainStatusForPlayerShip(SotfIDs.SKILL_ORDNANCEMASTERY + "_rofMod",
							Global.getSettings().getSpriteName("ui", "icon_kinetic"),
							"Ordnance mastery",
							"+" + rofBonus + "% ballistic fire rate", false);
					Global.getCombatEngine().maintainStatusForPlayerShip(SotfIDs.SKILL_ORDNANCEMASTERY + "_fluxMod",
							Global.getSettings().getSpriteName("ui", "icon_kinetic"),
							"Ordnance mastery",
							"-" + fluxReduction + "% ballistic flux cost", false);
				}

			}
		}
	}
}
