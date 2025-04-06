package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class SotfRevealSkill implements BaseCommand {

	@Override
	public CommandResult runCommand(String args, CommandContext context)
	{
		if (!context.isInCampaign())
		{
			// Show a default error message
			Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
			// Return the 'wrong context' result, this will alert the player by playing a special sound
			return CommandResult.WRONG_CONTEXT;
		}
		if (args.isEmpty())
		{
			return CommandResult.BAD_SYNTAX;
		}
		SkillSpecAPI skill = Global.getSettings().getSkillSpec(args);
		if (skill != null)
		{
			String skillName = skill.getName();
			String skillOrAptitude = " skill";
			if (skillName.isEmpty()) {
				skillName = skill.getGoverningAptitudeName();
				skillOrAptitude = " aptitude";
			}
			if (skill.hasTag(Skills.TAG_NPC_ONLY)) {
				Console.showMessage("The " + skillName + skillOrAptitude + " is no longer NPC-only. Enjoy!");
				skill.getTags().remove(Skills.TAG_NPC_ONLY);
			} else {
				Console.showMessage("The " + skill.getName() + " skill is now NPC-only.");
				skill.addTag(Skills.TAG_NPC_ONLY);
			}
			return CommandResult.SUCCESS;
		} else {
			Console.showMessage("Found no skill with ID: " + args);
			return CommandResult.ERROR;
		}
	}
}