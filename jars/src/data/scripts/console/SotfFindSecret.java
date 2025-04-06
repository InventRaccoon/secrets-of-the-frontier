package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.intel.misc.FleetLogIntel;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import exerelin.utilities.StringHelper;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static org.lazywizard.console.CommandUtils.isFloat;

public class SotfFindSecret implements BaseCommand {

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
		LocationAPI location = null;
		String text = "That secret can be found in ";
		if (!args.isEmpty()) {
			if (args.equals("hypnos") || args.equals("barrow")) {
				SectorEntityToken entity = (SectorEntityToken) Global.getSector().getMemoryWithoutUpdate().get(SotfIDs.HYPNOS_CRYO);
				location = entity.getContainingLocation();
			} else if (args.equals("athena") || args.contains("memory")) {
				SectorEntityToken entity = (SectorEntityToken) Global.getSector().getMemoryWithoutUpdate().get("sotf_athenaWreck");
				location = entity.getContainingLocation();
			} else if (args.equals("nightingale") || args.equals("inky")) {
				location = (StarSystemAPI) Global.getSector().getMemoryWithoutUpdate().get(SotfIDs.MEM_NIGHTINGALE_SYSTEM);
			} else if (args.equals("mia") || args.equals("hallowhall") || args.equals("seraph")) {
				location = Global.getSector().getStarSystem("sotf_mia");
				text = "That's not really a secret! But this is where to find ";
			} else if (args.equals("lightofthelake") || args.equals("reverie") || args.equals("lotl") || args.equals("sirius")) {
				location = Global.getSector().getStarSystem("sotf_lotl");
				if (location != null) {
					Console.showMessage("From the Sector's center, head directly north into the Abyss and keep going.");
				}
				return CommandResult.SUCCESS;
			} else {
				return CommandResult.BAD_SYNTAX;
			}
		}
		if (location != null) {
			Console.showMessage(text + location.getName());
			if (location instanceof StarSystemAPI) {
				Console.showMessage("A new fleet log entry has been added to remind you where that is.");
				if (args.contains("memory")) {
					args = "A Memory";
				}
				final String name = Misc.ucFirst(args);
				final SectorEntityToken finalLocation = ((StarSystemAPI) location).getCenter();
				FleetLogIntel intel = new FleetLogIntel() {
					final SectorEntityToken token = finalLocation;
					final String intelName = name;
					@Override
					public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
						info.addImage(getIcon(), 0);
						info.addPara("You made a console command search for a secret! Here's an intel item to find it easily.",0f);
						this.addDeleteButton(info, width);
					}

					@Override
					protected String getName() {
						return intelName;
					}

					@Override
					public SectorEntityToken getMapLocation(SectorMapAPI map) {
						return token;
					}
				};
				Global.getSector().getIntelManager().addIntel(intel);
			}
			return CommandResult.SUCCESS;
		} else {
			Console.showMessage("Couldn't find that secret!");
			return CommandResult.ERROR;
		}
	}
}