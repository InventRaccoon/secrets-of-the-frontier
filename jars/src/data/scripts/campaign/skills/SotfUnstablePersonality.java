// Causes an officer to randomly change personality mid-combat
package data.scripts.campaign.skills;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MarketSkillEffect;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfPeople;

public class SotfUnstablePersonality {

	// base time between personality changes
	public static final float TIME_BETWEEN_SWITCH = 15f;
	// up to this many seconds are randomly added to the personality change timer each time it switches
	public static final float TIME_VARIANCE = 15f;

	public static float STABILITY_PENALTY = 2;

	public static class Level1 implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new SotfUnstableOfficerScript(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(SotfUnstableOfficerScript.class);
		}

		public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
			//stats.getBreakProb().modifyMult(id, 0f);
		}
		public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
			//stats.getBreakProb().unmodify(id);
		}

		public String getEffectDescription(float level) {
			return "Severe personality instability in combat";
		}

		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static class SotfUnstableOfficerScript implements AdvanceableListener {
		public ShipAPI ship;
		public float switchTimer = 15f;
		public float switchTime = TIME_BETWEEN_SWITCH;

		public SotfUnstableOfficerScript(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {
			if (!Global.getCurrentState().equals(GameState.COMBAT)) {
				return;
			}
			if (ship.getShipAI() == null) {
				return;
			}
			switchTimer += amount;
			if (switchTimer < switchTime || !ship.isAlive() || ship == Global.getCombatEngine().getPlayerShip()) {
				return;
			}
			if (ship.getShipAI().getConfig().personalityOverride == null) {
				ship.getShipAI().getConfig().personalityOverride = "reckless";
			}
			WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
			post.add("timid", 0.35f);
			post.add("cautious", 1.75f);
			//post.add("steady"); // nah, funnier if it's not an option
			post.add("aggressive");
			post.add("reckless");
			post.add("fearless"); // aka AI core behavior

			// don't pick our current personality
			if (ship.getShipAI().getConfig().alwaysStrafeOffensively) {
				post.remove("fearless");
			} else {
				post.remove(ship.getShipAI().getConfig().personalityOverride);
			}
			String personality = post.pick();
			boolean fearlessMode = false;
			if (ship.getOwner() == 0) {
				Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(),Misc.ucFirst(personality) + "!", NeuralLinkScript.getFloatySize(ship), Misc.getNegativeHighlightColor(), ship,
						0f, 0f, 0.5f, 0.5f, 0.5f, 1f);
			}
			if (personality.equals("fearless")) {
				fearlessMode = true;
				personality = "reckless";
			}
			ship.getShipAI().getConfig().personalityOverride = personality;
			if (fearlessMode) {
				ship.getShipAI().getConfig().alwaysStrafeOffensively = true;
				ship.getShipAI().getConfig().backingOffWhileNotVentingAllowed = false;
				ship.getShipAI().getConfig().turnToFaceWithUndamagedArmor = false;
				ship.getShipAI().getConfig().burnDriveIgnoreEnemies = true;
			} else {
				ship.getShipAI().getConfig().alwaysStrafeOffensively = false;
				ship.getShipAI().getConfig().backingOffWhileNotVentingAllowed = true;
				ship.getShipAI().getConfig().turnToFaceWithUndamagedArmor = true;
				ship.getShipAI().getConfig().burnDriveIgnoreEnemies = false;
			}
			ship.getShipAI().cancelCurrentManeuver();
			ship.getShipAI().forceCircumstanceEvaluation();
			switchTimer = 0;
			switchTime = TIME_BETWEEN_SWITCH + ((float) Math.random() * TIME_VARIANCE);

			if (ship.getCaptain().isDefault()) return;

			if (!SotfModPlugin.IS_CHATTER) return;
			// For Cerulean: change combat chatter personality
			if (ship.getCaptain().getId().equals(SotfPeople.CERULEAN)) {
				if (personality.equals("timid") || personality.equals("cautious")) {
					ship.getCaptain().getMemoryWithoutUpdate().set("$chatterChar", "sotf_cerulean_passive");
				} else {
					ship.getCaptain().getMemoryWithoutUpdate().set("$chatterChar", "sotf_cerulean_aggressive");
				}
			}
		}
	}

	public static class Level2 implements MarketSkillEffect {
		public void apply(MarketAPI market, String id, float level) {
			market.getStability().modifyFlat(id, STABILITY_PENALTY * -1, "Admin instability");
		}

		public void unapply(MarketAPI market, String id) {
			market.getStability().unmodifyFlat(id);
		}

		public String getEffectDescription(float level) {
			return "-" + (int)STABILITY_PENALTY + " stability";
		}

		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.GOVERNED_OUTPOST;
		}
	}
}
