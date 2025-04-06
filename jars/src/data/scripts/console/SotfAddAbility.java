package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.AbilitySpecAPI;
import data.scripts.utils.SotfMisc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static org.lazywizard.console.CommandUtils.format;
import static org.lazywizard.console.CommandUtils.isFloat;

public class SotfAddAbility implements BaseCommand {

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
		if (args.isEmpty())
		{
			return CommandResult.BAD_SYNTAX;
		}
		AbilitySpecAPI ability = Global.getSettings().getAbilitySpec(args);
		if (ability == null) {
			Console.showMessage("Could not find an ability with ID " + args);
			return CommandResult.ERROR;
		}
		Console.showMessage("Added ability: " + ability.getName());
		Global.getSector().getCharacterData().addAbility(ability.getId());
		return CommandResult.SUCCESS;
	}
}