package data.scripts.console;

import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static org.lazywizard.console.CommandUtils.*;

public class SotfLevelUpSierra implements BaseCommand {

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
		if (!SotfModPlugin.WATCHER) {
			Console.showMessage("Secrets of the Frontier's \"Watcher Beyond the Walls\" module must be turned on to use this command!");
			return CommandResult.ERROR;
		}
		if (SotfPeople.getPerson(SotfPeople.SIERRA).getStats().getLevel() >= 8) {
			Console.showMessage("Sierra has already reached her full potential... for now.");
			return CommandResult.ERROR;
		}
		SotfMisc.levelUpSierra(8);
		Console.showMessage("Sierra's mastery grows. She is now level " + format(SotfPeople.getPerson(SotfPeople.SIERRA).getStats().getLevel()) + ".");
		return CommandResult.SUCCESS;
	}
}