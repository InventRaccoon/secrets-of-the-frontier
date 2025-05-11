package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.SotfMisc;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.magiclib.util.MagicAnim;

import java.awt.*;

import static com.fs.starfarer.api.impl.campaign.skills.PolarizedArmor.NON_SHIELD_FLUX_LEVEL;

public class SotfPolarizedNanorepair {

	public static float MAX_CR_BONUS = 15;

	public static float MAX_REGEN_LEVEL = 1f;
	public static float REGEN_RATE = 0.005f;
	public static float TOTAL_REGEN_MAX_HULL = 2000f;
	public static float TOTAL_REGEN_MAX_HULL_FRACTION = 0.5f;

	// nvm this crap is too complicated
//	public static float ARMOR_REGEN_RATE = 0.1f;
//	public static float TOTAL_ARMOR_REGEN_MAX_POINTS = 900f;
//	public static float TOTAL_ARMOR_REGEN_MAX_FRACTION = 1.25f;
//
//	// as % of maximum regen rate
//	public static float MIN_REGEN_RATE = 0.5f;
//	// armor regen only if flux at this level or higher
//	public static float MIN_FLUX_FOR_ARMOR_REGEN = 0.25f;
//	// armor regen reaches 100% at this flux level
//	public static float FLUX_FOR_MAX_REGEN = 0.75f;

//	// flux level at which shieldless nonphase ships are always considered to be
//	public static float NON_SHIELD_FLUX_LEVEL = 0.5f;

	public static float ARMOR_REGEN_RATE = 0.02f;
	public static float TOTAL_ARMOR_REGEN_MAX_POINTS = 650f;
	public static float TOTAL_ARMOR_REGEN_MAX_FRACTION = 0.8f;

	public static class Desc implements DescriptionSkillEffect {
		public String getString() {
			return "\n*Considers shieldless, cloakless ships as 50% hard flux. Typically, there is a 5 second delay on repairing damaged modules.";
		}
		public Color[] getHighlightColors() {
			Color h = Misc.getHighlightColor();
			h = Misc.getDarkHighlightColor();
			return new Color[] {h};
		}
		public String[] getHighlights() {
			return new String [] {"" + (int) NON_SHIELD_FLUX_LEVEL + "%"};
		}
		public Color getTextColor() {
			return null;
		}
	}

	public static class ArmorRegen extends BaseSkillEffectDescription implements ShipSkillEffect, AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new SotfPolarizedNanorepairArmorRegen(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(SotfPolarizedNanorepairArmorRegen.class);
		}


		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}

		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
											TooltipMakerAPI info, float width) {
			initElite(stats, skill);

//			info.addPara("While above %s flux, regenerate up to %s armor per second; maximum repair rate at %s flux; maximum total repair is " +
//							"the higher of %s armor points or %s of maximum armor", 0f, hc, hc,
//					"" + Misc.getRoundedValueMaxOneAfterDecimal(MIN_FLUX_FOR_ARMOR_REGEN * 100f) + "%",
//					"" + Misc.getRoundedValueMaxOneAfterDecimal(ARMOR_REGEN_RATE * 100f) + "%",
//					"" + Misc.getRoundedValueMaxOneAfterDecimal(FLUX_FOR_MAX_REGEN * 100f) + "%",
//					"" + (int)Math.round(TOTAL_ARMOR_REGEN_MAX_POINTS) + "",
//					"" + (int)Math.round(TOTAL_ARMOR_REGEN_MAX_FRACTION * 100f) + "%"
//			);
			info.addPara("Repair up to %s of armor rating per second; maximum total repair is " +
							"the higher of %s armor points or %s of maximum armor", 0f, hc, hc,
					"" + Misc.getRoundedValueMaxOneAfterDecimal(ARMOR_REGEN_RATE * 100f) + "%",
					"" + (int)Math.round(TOTAL_ARMOR_REGEN_MAX_POINTS) + "",
					"" + (int)Math.round(TOTAL_ARMOR_REGEN_MAX_FRACTION * 100f) + "%"
			);
		}
	}

	public static class SotfPolarizedNanorepairArmorRegen implements AdvanceableListener {
		protected ShipAPI ship;
		protected boolean inited = false;
		protected float limit = 0f;
		protected float repaired = 0f;
		protected String repKey1;
		public SotfPolarizedNanorepairArmorRegen(ShipAPI ship) {
			this.ship = ship;
		}

		protected void init() {
			inited = true;

			float maxArmor = ship.getArmorGrid().getArmorRating();
			limit = Math.max(TOTAL_ARMOR_REGEN_MAX_POINTS, TOTAL_ARMOR_REGEN_MAX_FRACTION * maxArmor);

			repKey1 = "sotf_polarizednanorepair_armor_ " + ship.getId() + "_repaired";
			float r1 = getRepaired(repKey1);

			repaired = Math.max(repaired, r1);
		}

		protected float getRepaired(String key) {
			Float r = (Float) Global.getCombatEngine().getCustomData().get(key);
			if (r == null) r = 0f;
			return r;
		}

		public void advance(float amount) {
			if (!inited) {
				init();
			}

			if (repaired >= limit) return;
			if (ship.isHulk()) return;
			if (DefenseUtils.getMostDamagedArmorCell(ship) == null) return;

//			float fluxLevel = ship.getFluxLevel();
//
//			if (ship.getShield() == null && !ship.getHullSpec().isPhase() &&
//					(ship.getPhaseCloak() == null || !ship.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.PHASE))) {
//				fluxLevel = NON_SHIELD_FLUX_LEVEL;
//			}
//
//			// scale repair rate from MIN_REGEN_RATE at MIN_FLUX_FOR_ARMOR_REGEN to 100% at FLUX_FOR_MAX_REGEN
//			float repairRate = (fluxLevel - MIN_FLUX_FOR_ARMOR_REGEN) / (FLUX_FOR_MAX_REGEN - MIN_FLUX_FOR_ARMOR_REGEN);
//			if (repairRate < 0f) repairRate = 0f;
//			if (repairRate > 1f) repairRate = 1f;
//
//			repairRate = Misc.interpolate(0f, 1f, (repairRate * (1f - MIN_REGEN_RATE)) + MIN_REGEN_RATE);
//
//			float repairAmount = Math.min(limit - repaired, ship.getArmorGrid().getArmorRating() * ARMOR_REGEN_RATE * repairRate * amount);

			float repairAmount = Math.min(limit - repaired, ship.getArmorGrid().getArmorRating() * ARMOR_REGEN_RATE * amount);
			float left = repairAmount;

			final float[][] grid = ship.getArmorGrid().getGrid();
			final float max = ship.getArmorGrid().getMaxArmorInCell();

			int numToRepair = 0;
			// Iterate through all armor cells and find any that aren't at max
			for (int x = 0; x < grid.length; x++)
			{
				for (int y = 0; y < grid[0].length; y++)
				{
					if (grid[x][y] < max)
					{
						numToRepair++;
					}
				}
			}

			float perCell = repairAmount / numToRepair;

			for (int x = 0; x < grid.length; x++)
			{
				for (int y = 0; y < grid[0].length; y++)
				{
					if (grid[x][y] < max)
					{
						float cur = grid[x][y];
						float repairable = Math.min(max - cur, perCell);
						repairable = Math.min(repairable, left);
						ship.getArmorGrid().setArmorValue(x, y, cur + repairable);
						//ship.getArmorGrid().setArmorValue(x, y, max);
						left -= repairable;
					}
				}
			}

			if (left > 0) {
				float singleCell = SotfMisc.repairSingleMostDamaged(ship, left);
				left -= singleCell;
			}

			ship.syncWithArmorGridState();
			ship.syncWeaponDecalsWithArmorDamage();

			if (repairAmount > left) {
				repaired += (repairAmount - left);
				Global.getCombatEngine().getCustomData().put(repKey1, repaired);
			}
		}

	}
}
