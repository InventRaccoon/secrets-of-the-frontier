// DASH THEM AGAINST THE STORM-WROUGHT ROCKS.
// Jubilant Tech-Siren uses Cyberwarfare's code for its actual hacking effect
package data.scripts.campaign.skills;

import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class SotfJubilantSiren {

	public static float EW_CAPITALS = 4f;
	public static float EW_CRUISERS = 3f;
	public static float EW_OTHER = 2f;
	
	public static class SeaOfCiphers implements ShipSkillEffect {
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			float bonus = 0f;
			if (hullSize == HullSize.CAPITAL_SHIP) bonus = EW_CAPITALS;
			if (hullSize == HullSize.DESTROYER) bonus = EW_CRUISERS;
			if (hullSize == HullSize.DESTROYER || hullSize == HullSize.FRIGATE) bonus = EW_OTHER;
			if (bonus > 0f) {
				stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyFlat(id, bonus);
			}
		}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_FLAT).unmodify(id);
		}
		public String getEffectDescription(float level) {
			return "+" + (int) EW_CAPITALS + "% to ECM rating of fleet when piloting a capital ship, " +
				   "+" + (int) EW_CRUISERS + "% when piloting a cruiser, " +
				   "+" + (int) EW_OTHER + "% for smaller hulls";
		}
		public String getEffectPerLevelDescription() {
			return null;
		}
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
}
