// Lash enemies to rot them
package data.scripts.campaign.skills;

import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.subsystems.SotfTouchOfDecaySubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;

public class SotfDecayingScourge {

	public static class Rot extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			if (ship.getHullSpec().getHullId().contains("higgs")) return;
			MagicSubsystemsManager.addSubsystemToShip(ship, new SotfTouchOfDecaySubsystem(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			if (ship.getHullSpec().getHullId().contains("higgs")) return;
			MagicSubsystemsManager.removeSubsystemFromShip(ship, SotfTouchOfDecaySubsystem.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {

		}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {

		}

		public String getEffectDescription(float level) {
			return null;
		}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
											TooltipMakerAPI info, float width) {
			init(stats, skill);

			float cooldown = SotfTouchOfDecaySubsystem.BASE_COOLDOWN;
			float range = SotfTouchOfDecaySubsystem.RANGE;

			String seconds = "seconds";
			if (cooldown == 1f) {
				seconds = "second";
			}

			info.addPara("Ship gains \"Touch of Decay\" subsystem with a %s unit range and %s cooldown",0f, hc, hc, "" + range, (int) cooldown + "s");
			info.addPara("Subsystem lashes a targeted hostile, halting it and dealing energy damage equal to %s of its current hull points at time of impact", 0f, hc, hc, (int) (SotfTouchOfDecaySubsystem.PERCENT_DAMAGE * 100f) + "%");
			info.addPara("Lash's impact overloads nanite-synthesized ships and deals %s of their current hull as energy damage instantly", 0f, hc, hc, (int) (SotfTouchOfDecaySubsystem.PERCENT_DAMAGE * 100f) + "%");
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
}
