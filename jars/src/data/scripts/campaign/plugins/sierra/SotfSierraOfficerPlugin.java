package data.scripts.campaign.plugins.sierra;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;

import java.awt.*;
import java.util.Random;

/**
 *	Sierra's officer plugin. Fixed person, special personality text
 */

public class SotfSierraOfficerPlugin extends BaseAICoreOfficerPluginImpl implements AICoreOfficerPlugin {

	public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
		PersonAPI person = SotfPeople.getPerson(SotfPeople.SIERRA);
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

		tooltip.addSectionHeading("Personality: " + person.getPersonalityAPI().getDisplayName(), text, bg, Alignment.MID, 20);
		switch (person.getPersonalityAPI().getId()) {
			case "cautious":
				tooltip.addPara("In combat, Sierra will prefer to stay out of enemy range, only occasionally moving in if out-ranged by the enemy.", opad);
				break;
			case "steady":
				tooltip.addPara("In combat, Sierra will favor a balanced approach with tactics matching the current situation.", opad);
				break;
			case "aggressive":
				tooltip.addPara("In combat, Sierra will prefer to engage at a range that allows the use of all of her ship's weapons.", opad);
				break;
			case "reckless":
				tooltip.addPara("In combat, Sierra will disregard the safety of her ship entirely in an effort to engage the enemy.", opad);
				break;
		}
		String heading = "Switch Personality";
		if (SotfMisc.playerHasInertConcord() && !SotfModPlugin.NEW_SIERRA_MECHANICS) {
			heading += " or Ship";
		}
		String main = "ask Sierra to adopt a different combat style";
		if (SotfMisc.playerHasInertConcord() && !SotfModPlugin.NEW_SIERRA_MECHANICS) {
			main += " or transfer to an inert Concord ship in your fleet";
		}
		tooltip.addSectionHeading(heading, text, bg, Alignment.MID, 20);
		tooltip.addPara("You can " + main + " by speaking to her " +
				"via the \"Contacts\" tab of the Intel screen.", opad);
	}

}
