package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.utils.SotfMisc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static org.lazywizard.console.CommandUtils.format;
import static org.lazywizard.console.CommandUtils.isFloat;

public class SotfBecomeHaunted implements BaseCommand {

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
		Global.getSector().getMemoryWithoutUpdate().set("$sotf_hauntedStart", true);
		SotfMisc.addGuilt(SotfMisc.getHauntedGuilt());
		boolean addSkillPoint = true;
		if (!args.isEmpty()) {
			if (args.equals("false")) {
				addSkillPoint = false;
			}
		}
		if (addSkillPoint) Global.getSector().getPlayerPerson().getStats().addPoints(1);
		return CommandResult.SUCCESS;
	}
}