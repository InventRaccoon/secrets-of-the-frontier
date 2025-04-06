// BY THE LIGHT OF LUDD! Ship gains a chance to critically hit "Servants of Moloch"
package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.DescriptionSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SotfOathswornZeal {

	// chance for non-missile weapons to crit on hit
	public static float CRIT_CHANCE = 15f;
	// percent bonus damage added by a crit
	public static float CRIT_BONUS = 100f;
	// average % damage increase, used for missile damage bonus
	public static float AVERAGE_CRIT = CRIT_CHANCE * (CRIT_BONUS * 0.01f);

	// yes I DO insist on writing out all of it every time
	public static String SHIP_UNHOLY_SERVANT_OF_MOLOCH_KEY = "$sotf_unholyServantOfMoloch";
	//public static String SHIP_SINFUL_ENERGY_KEY = "$sotf_sinfulEnergyUsage";

	public static class ServantsOfMoloch implements DescriptionSkillEffect {
		public String getString() {
			return "*\"Servants of Moloch\" includes vessels satisfying any of the following conditions: " +
					"automated ship, phase ship, more energy weapons than ballistic, captain has Gunnery Implants, " +
					"belongs to major Sector power who is hostile to the Luddic Church";
			// also includes any phantom ship e.g Eidolon's fleet
		}
		public Color[] getHighlightColors() {
			Color h = Misc.getHighlightColor();
			h = Misc.getDarkHighlightColor();
			Color bad = Misc.getNegativeHighlightColor();
			bad = Misc.setAlpha(bad, 200);
			Color phase = new Color(155,50,155,200);
			Color energy = Misc.getEnergyMountColor();
			energy = Misc.setAlpha(energy, 200);
			Color implants = Global.getSettings().getSkillSpec(Skills.GUNNERY_IMPLANTS).getGoverningAptitudeColor();
			Color luddic = Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getBaseUIColor();
			//return new Color[] {bad, bad, phase, energy, implants, bad, luddic};
			return new Color[] {bad};
		}
		public String[] getHighlights() {
			// honestly, it looks way better with no highlights
			//return new String [] {"\"Servants of Moloch\"", "automated", "phase-capable", "energy weapons", "Gunnery Implants", "hostile", "Luddic Church"};
			return new String [] {"\"Servants of Moloch\""};
		}
		public Color getTextColor() {
			return null;
		}
	}

	public static class SmiteTheWicked extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new SotfSmiteTheWickedDamageDealtMod());
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(SotfSmiteTheWickedDamageDealtMod.class);
		}

		public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {

		}
		public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {

		}

		public String getEffectDescription(float level) {
			return null;
		}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
											TooltipMakerAPI info, float width) {
			init(stats, skill);

			LabelAPI label = info.addPara("Ballistic and energy weapons have a %s chance to deal %s damage against Servants of Moloch*",
					0f, hc, hc,
					"" + (int) CRIT_CHANCE + "%",
					"+" + (int) CRIT_BONUS + "%");
			label.setHighlight("Servants of Moloch");
			label.setHighlightColors(Misc.getNegativeHighlightColor());

			label = info.addPara("Missiles deal %s damage on every hit against Servants of Moloch",
					10f, hc, hc, "+" + (int) AVERAGE_CRIT + "%");
			label.setHighlight("Servants of Moloch");
			label.setHighlightColors(Misc.getNegativeHighlightColor());

			label = info.addPara("*\"Servants of Moloch\" includes vessels satisfying any of the following conditions: " +
					"automated ship, phase ship, more energy weapons than ballistic, captain has Gunnery Implants, " +
					"belongs to major Sector power who is hostile to the Luddic Church", 0f, dtc, dtc);
			label.setHighlight("Servants of Moloch");
			label.setHighlightColors(Misc.getNegativeHighlightColor());
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static class SotfSmiteTheWickedDamageDealtMod implements DamageDealtModifier {
		public String modifyDamageDealt(Object param,
										CombatEntityAPI target, DamageAPI damage,
										Vector2f point, boolean shieldHit) {
			if (!(target instanceof ShipAPI)) {
				return null;
			}
			// avoid excess RNG: missiles can't crit, just gain an average damage boost
			boolean crit = !(param instanceof MissileAPI);
			// roll the dice
			if (crit && (float) Math.random() > CRIT_CHANCE * 0.01f) return null;

			ShipAPI targetShip = (ShipAPI) target;
			boolean isUnholyServantOfMoloch;
			// only do the check ONCE per ship - it's not like it'll suddenly become a phase ship or grow 2 Tachyon Lances
			if (targetShip.getCustomData().get(SHIP_UNHOLY_SERVANT_OF_MOLOCH_KEY) != null) {
				isUnholyServantOfMoloch = (boolean) targetShip.getCustomData().get(SHIP_UNHOLY_SERVANT_OF_MOLOCH_KEY);
			} else {
				// also count phantasmal ships as Servants of Moloch
				isUnholyServantOfMoloch = (targetShip.getVariant().hasHullMod(HullMods.AUTOMATED) ||
						(targetShip.isFighter() && targetShip.getHullSpec().getMinCrew() == 0) ||
						targetShip.getHullSpec().isPhase() ||
						sinfulEnergyUsage(targetShip) ||
						targetShip.getVariant().hasHullMod(SotfIDs.PHANTASMAL_SHIP));

				if (targetShip.getCaptain() != null) {
					// ship might be scripted to have an AI captain but is not automated
					if (targetShip.getCaptain().isAICore() || targetShip.getCaptain().getStats().hasSkill(Skills.GUNNERY_IMPLANTS)) {
						isUnholyServantOfMoloch = true;
					}
				}
				if (targetShip.getFleetMember().getFleetData() != null) {
					FactionAPI targetFaction = targetShip.getFleetMember().getFleetData().getFleet().getFaction();
					// "major sector power" = not hidden, offers commissions or engages in hostilities with other factions
					if (targetFaction.isHostileTo(Factions.LUDDIC_CHURCH) &&
							targetFaction.isShowInIntelTab() &&
							(targetFaction.getCustomBoolean("offersCommissions") || targetFaction.getCustomBoolean("engagesInHostilities"))) {
						isUnholyServantOfMoloch = true;
					}
				}
				targetShip.setCustomData(SHIP_UNHOLY_SERVANT_OF_MOLOCH_KEY, isUnholyServantOfMoloch);
			}
			if (!isUnholyServantOfMoloch) return null;
			String id = "sotf_oathswornzeal_dam_mod";
			if (crit) {
				damage.getModifier().modifyPercent(id, CRIT_BONUS);
			} else {
				damage.getModifier().modifyPercent(id, AVERAGE_CRIT);
			}
			// lol
			if (param instanceof DamagingProjectileAPI) {
				DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
				if (proj.getProjectileSpecId().contains("hammer") && !shieldHit) {
					Global.getCombatEngine().addFloatingText(point,
							"Smited!",
							10f, Misc.getNegativeHighlightColor(), target, 0f, 0f);
				}
			}
			return id;
		}
	}

	// gets if a ship has more energy weapons than ballistic
	public static boolean sinfulEnergyUsage(ShipAPI ship) {
		int energy = 0;
		int ballistic = 0;
		// 1/2/4 for each weapon. Count hybrids as sinful because they're usually advanced weapons
		for (WeaponAPI weapon : ship.getAllWeapons()) {
			if (weapon.getSpec().getType().equals(WeaponAPI.WeaponType.ENERGY) ||
					weapon.getSpec().getType().equals(WeaponAPI.WeaponType.SYNERGY) ||
					weapon.getSpec().getType().equals(WeaponAPI.WeaponType.HYBRID)) {
				energy += weapon.getSize().ordinal() + 1;
				if (weapon.getSize().equals(WeaponAPI.WeaponSize.LARGE)) {
					energy++;
				}
			} else if (weapon.getSpec().getType().equals(WeaponAPI.WeaponType.BALLISTIC) || weapon.getSpec().getType().equals(WeaponAPI.WeaponType.COMPOSITE)) {
				ballistic += weapon.getSize().ordinal() + 1;
				if (weapon.getSize().equals(WeaponAPI.WeaponSize.LARGE)) {
					ballistic++;
				}
			}
		}
		return energy > ballistic;
	}

}
