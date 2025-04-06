package data.scripts.console;

import com.fs.starfarer.api.Global;
import data.scripts.utils.SotfMisc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static org.lazywizard.console.CommandUtils.*;

public class SotfPrintGuilt implements BaseCommand {

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
		float baseGuilt = SotfMisc.getPlayerBaseGuilt();
		float bonusGuilt = Global.getSettings().getFloat("sotf_bonusGuilt");
		String guiltString = "You have accumulated " + format(baseGuilt) + " guilt.";
		if (bonusGuilt != 0) {
			guiltString += " You also have " + format(bonusGuilt) + " bonus guilt.";
		}
		if (baseGuilt <= 0) {
			guiltString += " Squeaky clean! Good job.";
		} else if (baseGuilt < 4f) {
			guiltString += " Nobody's perfect.";
		} else if (baseGuilt < 7f) {
			guiltString += " Your soul is tainted.";
		} else if (baseGuilt < 10f) {
			guiltString += " You're a monster.";
		} else {
			guiltString += " The gates are dead. We are abandoned in the great Everwinter. Who is anyone to judge us?";
		}

		if (SotfMisc.getPlayerGuilt() >= SotfMisc.getInvasionThreshold()) {
			guiltString += "      ... and you feel a terrible chill.";
		}

		Console.showMessage(guiltString);
		return CommandResult.SUCCESS;
	}
}