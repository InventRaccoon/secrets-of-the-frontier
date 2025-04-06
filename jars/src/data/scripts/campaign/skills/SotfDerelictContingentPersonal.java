// BROKEN HULLS BECOME JAGGED SWORDS. 0.95a era Derelict Contingent for the piloted ship
// Dmods = + chance to almost entirely negate hull damage, unshielded ships gain additional damage reduction
package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.DescriptionSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.text.DecimalFormat;

public class SotfDerelictContingentPersonal {

	public static float AVOID_DAMAGE_CHANCE_PER_DMOD = 10f; // nerfed from 15f of old vanilla version
	public static float AVOID_DAMAGE_DAMAGE_MULT = 0.1f;

	public static float SHIELDLESS_ARMOR_BONUS_PER_DMOD = 0.04f; // nerfed from 0.05f
	public static float MAX_DMODS = 4; // 36% reduction to hull damage taken, on average

	public static float MULTIPLIER_REDUCTION_PER_DMOD = 0.25f;

	public static class Level0 implements DescriptionSkillEffect {
		public String getString() {
			String min = "" + (int) Math.round(Global.getSettings().getMinArmorFraction() * 100f) + "%";
			return
					"*Maximum effect reached " +
							"at " + (int) MAX_DMODS + " d-mods." +
							"**The effective armor value can not go below a percentage of " +
							"its original value for calculating the amount of damage reduction it provides. " +
							"The base minimum armor value is " + min + " of the original value. "
					;
		}
		public Color[] getHighlightColors() {
			Color h = Misc.getHighlightColor();
			h = Misc.getDarkHighlightColor();
			return new Color[] {h, h};
		}
		public String[] getHighlights() {
			String min = "" + (int) Math.round(Global.getSettings().getMinArmorFraction() * 100f) + "%";
			return new String [] {"" + (int) MAX_DMODS, min};
		}
		public Color getTextColor() {
			return null;
		}
	}

	public static class Level1 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new DCDamageTakenMod(ship));
			if (ship.getShield() == null) {
				MutableShipStatsAPI stats = ship.getMutableStats();

				float dmods = DModManager.getNumDMods(ship.getVariant());
				if (dmods <= 0) return;
				if (dmods > MAX_DMODS) dmods = MAX_DMODS;

				stats.getMinArmorFraction().modifyFlat(id, SHIELDLESS_ARMOR_BONUS_PER_DMOD * dmods);
			}
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(DCDamageTakenMod.class);
			if (ship.getShield() == null) {
				MutableShipStatsAPI stats = ship.getMutableStats();
				stats.getMinArmorFraction().unmodifyFlat(id);
			}
		}

		public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
			if (stats.getFleetMember() != null) {
				FleetMemberAPI member = stats.getFleetMember();
				if (member.getVariant() != null && !member.getVariant().hasTag(Tags.VARIANT_UNRESTORABLE) && !member.getVariant().hasTag("sotf_dcontingent_unrestorable")) {
					member.getVariant().addTag("sotf_dcontingent_unrestorable");
					member.getVariant().addTag(Tags.VARIANT_UNRESTORABLE);
				}
				if (member.getCaptain() != null && member.getCaptain().isAICore()) {
					MemoryAPI captainMemory = member.getCaptain().getMemoryWithoutUpdate();
					if (!captainMemory.contains("$sotf_dcontingent_origmult") && captainMemory.contains(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT)) {
						captainMemory.set("$sotf_dcontingent_origmult",
								captainMemory.get(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT));
						float newMult = (float) captainMemory.get("$sotf_dcontingent_origmult") - (MULTIPLIER_REDUCTION_PER_DMOD * Math.min((float) DModManager.getNumDMods(stats.getVariant()), 4f));
						member.getCaptain().getMemoryWithoutUpdate().set(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT, newMult);
					} else if (captainMemory.contains("$sotf_dcontingent_origmult")) {
						float newMult = (float) captainMemory.get("$sotf_dcontingent_origmult") - (MULTIPLIER_REDUCTION_PER_DMOD * Math.min((float) DModManager.getNumDMods(stats.getVariant()), 4f));
						member.getCaptain().getMemoryWithoutUpdate().set(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT, newMult);
					}
				}
			}
		}

		// see: SotfOfficerJankHandler
		public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
			//if (stats.getFleetMember() != null) {
			//	FleetMemberAPI member = stats.getFleetMember();
			//	if (member.getVariant() != null && member.getVariant().hasTag(Tags.VARIANT_UNRESTORABLE) && member.getVariant().hasTag("sotf_dcontingent_unrestorable")) {
			//		member.getVariant().removeTag("sotf_dcontingent_unrestorable");
			//		member.getVariant().removeTag(Tags.VARIANT_UNRESTORABLE);
			//	}
			//}
		}

		public String getEffectDescription(float level) {
			return null;
		}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
											TooltipMakerAPI info, float width) {
			init(stats, skill);

			String min = "" + (int) Math.round(Global.getSettings().getMinArmorFraction() * 100f) + "%";

			info.addPara("%s chance per d-mod* to have incoming hull damage reduced by %s", 0f, hc, hc,
					"" + (int)AVOID_DAMAGE_CHANCE_PER_DMOD + "%",
					"" + (int)Math.round((1f - AVOID_DAMAGE_DAMAGE_MULT) * 100f) + "%"
			);

			info.addPara("%s minimum armor value** for damage reduction per d-mod for unshielded ships", 0f, hc, hc,
					"+" + (int)Math.round(SHIELDLESS_ARMOR_BONUS_PER_DMOD * 100f) + "%"
			);

			info.addPara("%s reduction per d-mod to captain's automated ship points multiplier", 0f, hc, hc,
					"-" + new DecimalFormat("#.##").format(MULTIPLIER_REDUCTION_PER_DMOD) + "x"
			);

			info.addPara("\nThis skill prevents the piloted ship's d-mods from being removed by restoration or the Hull Restoration skill.", dhc, 0f);
			info.addPara("*Maximum effect reached at %s d-mods.", 0f, dtc, dhc,
					"" + (int) MAX_DMODS);
			info.addPara("**The effective armor value can not go below a percentage of " +
							"its original value for calculating the amount of damage reduction it provides. " +
							"The base minimum armor value is %s of the original value.", 0f, dtc, dhc,
					"" + min);
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static String AVOID_HULL_DAMAGE_CHANCE = "avoid_hull_damage_chance";
	public static String DAMAGE_MOD_ID = "dc_dam_mod";

	public static class DCDamageTakenModRemover implements DamageTakenModifier, AdvanceableListener {
		protected ShipAPI ship;
		public DCDamageTakenModRemover(ShipAPI ship) {
			this.ship = ship;
		}
		public String modifyDamageTaken(Object param, CombatEntityAPI target,
										DamageAPI damage, Vector2f point, boolean shieldHit) {
			return null;
		}

		public void advance(float amount) {
			if (!ship.hasListenerOfClass(DCDamageTakenMod.class)) {
				ship.removeListener(this);
				ship.getMutableStats().getHullDamageTakenMult().unmodifyMult(DAMAGE_MOD_ID);
			}
		}

	}
	public static class DCDamageTakenMod implements DamageTakenModifier, AdvanceableListener {
		protected ShipAPI ship;
		public DCDamageTakenMod(ShipAPI ship) {
			this.ship = ship;
			ship.addListener(new DCDamageTakenModRemover(ship));
		}

		public void advance(float amount) {

		}

		public String modifyDamageTaken(Object param,
										CombatEntityAPI target, DamageAPI damage,
										Vector2f point, boolean shieldHit) {
			MutableShipStatsAPI stats = ship.getMutableStats();
			stats.getHullDamageTakenMult().unmodifyMult(DAMAGE_MOD_ID);

			if (shieldHit) return null;

			float chance = stats.getDynamic().getMod(AVOID_HULL_DAMAGE_CHANCE).computeEffective(0f);
			if (Math.random() >= chance) {
				return null;
			}

			stats.getHullDamageTakenMult().modifyMult(DAMAGE_MOD_ID, AVOID_DAMAGE_DAMAGE_MULT);

			return null;
		}

	}

}
