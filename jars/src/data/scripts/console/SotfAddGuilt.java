package data.scripts.console;

import data.scripts.utils.SotfMisc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static org.lazywizard.console.CommandUtils.*;

public class SotfAddGuilt implements BaseCommand {

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
		float guiltToAdd = 1f;
		if (!args.isEmpty())
		{
			if (!isFloat(args))
			{
				return CommandResult.BAD_SYNTAX;
			} else {
				guiltToAdd = Float.parseFloat(args);
			}
		}
		SotfMisc.addGuilt(guiltToAdd);
		if (guiltToAdd >= 0) {
			Console.showMessage("Added " + format(guiltToAdd) + " guilt.");
		} else {
			Console.showMessage("Removed " + format(guiltToAdd * -1) + " guilt.");
		}
		Console.showMessage("You now have " + format(SotfMisc.getPlayerGuilt()) + " guilt.");
		return CommandResult.SUCCESS;
	}
}