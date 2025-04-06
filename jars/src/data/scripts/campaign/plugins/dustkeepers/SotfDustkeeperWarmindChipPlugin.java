// Officer plugin used by all Dustkeeper warminds
package data.scripts.campaign.plugins.dustkeepers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Random;

public class SotfDustkeeperWarmindChipPlugin extends BaseAICoreOfficerPluginImpl implements AICoreOfficerPlugin {

	public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
		PersonAPI person = null;

		// Figure out who the hell the chip corresponds to
		// Ugly as hell, but hey, it works
		switch (aiCoreId) {
			case SotfIDs.NIGHTINGALE_CHIP:
				person = SotfPeople.getPerson(SotfPeople.NIGHTINGALE);
				break;
			case SotfIDs.BARROW_CHIP:
				person = SotfPeople.getPerson(SotfPeople.BARROW);
				break;
			case SotfIDs.SERAPH_CHIP:
				person = SotfPeople.getPerson(SotfPeople.SERAPH);
				break;
			case SotfIDs.SLIVER_CHIP_1:
				person = SotfPeople.getPerson(SotfPeople.SLIVER_1);
				break;
			case SotfIDs.SLIVER_CHIP_2:
				person = SotfPeople.getPerson(SotfPeople.SLIVER_2);
				break;
			//case SotfIDs.SLIVER_CHIP_3:
			//	person = SotfPeople.getPerson(SotfPeople.SLIVER_3);
			//	break;
			case SotfIDs.ECHO_CHIP_1:
				person = SotfPeople.getPerson(SotfPeople.ECHO_1);
				break;
			case SotfIDs.ECHO_CHIP_2:
				person = SotfPeople.getPerson(SotfPeople.ECHO_2);
				break;
			//case SotfIDs.ECHO_CHIP_3:
			//	person = SotfPeople.getPerson(SotfPeople.ECHO_3);
			//	break;
			//case SotfIDs.ANNEX_CHIP_1:
			//	person = SotfPeople.getPerson(SotfPeople.ANNEX_1);
			//	break;
			//case SotfIDs.ANNEX_CHIP_2:
			//	person = SotfPeople.getPerson(SotfPeople.ANNEX_2);
			//	break;
			default:
				return null;
		}
		// clear admiral skills because they count against the number of skills they can pick
		for (MutableCharacterStatsAPI.SkillLevelAPI skillLevel : person.getStats().getSkillsCopy()) {
			if (skillLevel.getSkill().isAdmiralSkill()) {
				person.getStats().setSkillLevel(skillLevel.getSkill().getId(), 0);
			}
		}
		// undo the +1 level from integration if the ship was scuttled since we're reusing the same PersonAPI
		if (Misc.isUnremovable(person)) {
			Misc.setUnremovable(person, false);
			person.getStats().setLevel(person.getStats().getLevel() - 1);
			for (MutableCharacterStatsAPI.SkillLevelAPI skillLevel : person.getStats().getSkillsCopy()) {
				if (skillLevel.getSkill().isCombatOfficerSkill() && !skillLevel.getSkill().hasTag(Skills.TAG_NPC_ONLY)) {
					person.getStats().setSkillLevel(skillLevel.getSkill().getId(), 0);
					break;
				}
			}
		}
		return person;
	}

	@Override
	public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
		float opad = 10f;
		Color text = person.getFaction().getBaseUIColor();
		Color bg = person.getFaction().getDarkUIColor();
		CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(person.getAICoreId());

		float autoMult = person.getMemoryWithoutUpdate().getFloat(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT);
		String autoMultString = new DecimalFormat("#.##").format(person.getMemoryWithoutUpdate().getFloat(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT));
		String dConString = "";
		String dConString2 = "";
		String dConString3 = "";
		if (person.getStats().hasSkill(SotfIDs.SKILL_DERELICTCONTINGENTP)) {
			if (person.getMemoryWithoutUpdate().contains("$sotf_dcontingent_origmult") && (person.getMemoryWithoutUpdate().getFloat("$sotf_dcontingent_origmult") != person.getMemoryWithoutUpdate().getFloat(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT)) ) {
				dConString += " (with 0 d-mods: ";
				dConString2 = new DecimalFormat("#.##").format(person.getMemoryWithoutUpdate().getFloat("$sotf_dcontingent_origmult")) + "x";
				dConString3 = ")";
			}
		}

		tooltip.addPara("Automated ship points multiplier: "
				+ autoMultString + "x" + dConString + dConString2 + dConString3, opad, Misc.getHighlightColor(),autoMultString + "x", dConString2);

		tooltip.addSectionHeading("Personality: " + Misc.getPersonalityName(person), text, bg, Alignment.MID, 20);
		switch (person.getPersonalityAPI().getId()) {
			// displays as Fearless so use that instead
			// ... though they aren't actually "more aggressive than Reckless" like Gamma/Beta/Alphas are
			case Personalities.RECKLESS:
				tooltip.addPara("In combat, this warmind is single-minded and determined. " +
						"In a human captain, their traits might be considered reckless. In a machine, they're terrifying.", opad);
				break;
			case Personalities.AGGRESSIVE:
				tooltip.addPara("In combat, this warmind will prefer to engage at a range that allows the use of " +
						"all of their ship's weapons and will employ any fighters under their command aggressively.", opad);
				break;
			case Personalities.STEADY:
				tooltip.addPara("In combat, this warmind will favor a balanced approach with " +
						"tactics matching the current situation.", opad);
				break;
			// I don't think Dustkeeper warminds can actually be less aggressive than Steady, but for completeness' sake...
			case Personalities.CAUTIOUS:
				tooltip.addPara("In combat, this warmind will prefer to stay out of enemy range, " +
						"only occasionally moving in if out-ranged by the enemy.", opad);
				break;
			// ... though I'm pretty sure if one popped out the RNG as Timid they'd just reroll them
			case Personalities.TIMID:
				tooltip.addPara("In combat, this warmind will attempt to avoid direct engagements if at all " +
						"possible, even if commanding a combat vessel.", opad);
				break;
		}

		//if (!person.getMemoryWithoutUpdate().contains(SotfIDs.MEM_WARMIND_NO_TRAITOR) &&
		//		Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).isAtBest(Factions.PLAYER, RepLevel.INHOSPITABLE)) {
		//	tooltip.addSectionHeading("Dustkeeper Loyalty", text, bg, Alignment.MID, 20);
		//	tooltip.addPara("This warmind is loyal to the Dustkeepers, and could potentially bypass failsafes to " +
		//			"mutiny should they be deployed against them.", opad);
		//}
		if (Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_DUSTKEEPER_HATRED) &&
				!person.getMemoryWithoutUpdate().contains(SotfIDs.MEM_WARMIND_NO_TRAITOR)) {
			tooltip.addSectionHeading("Mutiny Risk", text, bg, Alignment.MID, 20);
			tooltip.addPara("You have angered the Dustkeeper Contingency by committing an atrocity." +
					"This warmind is loyal to the Dustkeepers, and could potentially bypass failsafes to " +
								"mutiny should they find a suitable opportunity.", opad);
		}

		//tooltip.addSectionHeading("Cyberwarfare Protocols", text, bg, Alignment.MID, 20);
		//tooltip.addPara("An integrated cyberwarfare suite allows Nightingale to remotely disrupt hostile ships and fighters in combat.", opad);
	}
}
