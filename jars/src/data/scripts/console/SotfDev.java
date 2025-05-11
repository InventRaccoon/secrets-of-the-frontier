package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.misc.FleetLogIntel;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import data.scripts.campaign.abilities.SotfCourserProtocolAbility;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.intel.misc.SotfDustkeeperHatred;
import data.scripts.campaign.intel.misc.SotfSiriusIntel;
import data.scripts.campaign.intel.quests.SotfWaywardStarIntel;
import data.scripts.campaign.missions.SotfHauntedFinale;
import data.scripts.campaign.missions.hallowhall.SotfHFHDevFactor;
import data.scripts.campaign.missions.hallowhall.SotfHFHInadMissionCompletedFactor;
import data.scripts.campaign.missions.hallowhall.SotfHopeForHallowhallEventIntel;
import data.scripts.campaign.plugins.wendigo.SotfWendigoEncounterManager;
import data.scripts.dialog.SotfGenericDialogScript;
import data.scripts.dialog.SotfGenericSierraDialogScript;
import data.scripts.utils.SotfMisc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import second_in_command.SCData;
import second_in_command.SCUtils;
import second_in_command.specs.SCOfficer;

public class SotfDev implements BaseCommand {

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
		if (!args.isEmpty()) {
			if (args.equals("hatred") || args.equals("dkh")) {
				Global.getSector().getMemoryWithoutUpdate().set("$sotf_dustkeeperHatredCause", "Jangala");
				Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_DUSTKEEPER_HATRED, true);
				Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).setRelationship(Factions.PLAYER, -1f);
				SotfDustkeeperHatred plugin = (SotfDustkeeperHatred) Global.getSector().getIntelManager().getFirstIntel(SotfDustkeeperHatred.class);
				plugin.sendUpdateIfPlayerHasIntel(SotfDustkeeperHatred.UPDATE_NEW_HATRED, null);
				Console.showMessage("Congrats! The Dustkeepers hate you.");
				return CommandResult.SUCCESS;
			}
			else if (args.equals("sierraAtrocity") || args.equals("sa")) {
				Global.getSector().getMemoryWithoutUpdate().set("$sierraLatestSatbombMkt", "Jangala");
				Global.getSector().getScripts().add(new SotfGenericSierraDialogScript("sotfSierraSatbombExtreme"));
				Console.showMessage("Congrats! Sierra hates you.");
				return CommandResult.SUCCESS;
			}
			else if (args.equals("sendWendigo") || args.equals("wch")) {
				SotfWendigoEncounterManager.sendWendigoCourserHunt();
				Console.showMessage("Congrats! You are being hunted.");
				Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_COURSER_SENT_HUNTERKILLERS, true);
				return CommandResult.SUCCESS;
			}
			else if (args.equals("sendWendigoHostile") || args.equals("wchh")) {
				SotfWendigoEncounterManager.sendWendigoCourserHunt();
				Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).setRelationship(Factions.PLAYER, -0.75f);
				Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_COURSER_SENT_HUNTERKILLERS, true);
				Console.showMessage("Congrats! You are being hunted. With malicious intent.");
				return CommandResult.SUCCESS;
			}
			else if (args.equals("hallowhallPoints") || args.equals("hhp")) {
				SotfHFHDevFactor factor = new SotfHFHDevFactor(100);
				SotfHopeForHallowhallEventIntel.addFactorIfAvailable(factor, null);
				Console.showMessage("Enjoy your points!");
				return CommandResult.SUCCESS;
			}
			else if (args.equals("hallowhallPoints+") || args.equals("hhp+")) {
				SotfHFHDevFactor factor = new SotfHFHDevFactor(300);
				SotfHopeForHallowhallEventIntel.addFactorIfAvailable(factor, null);
				Console.showMessage("Enjoy LOTS of points!");
				return CommandResult.SUCCESS;
			}
			else if (args.equals("wsRevMeal") || args.equals("wsrm")) {
				StarSystemAPI waywardStar = Global.getSector().getStarSystem("sotf_waywardstar");
				if (waywardStar == null) {
					Console.showMessage("Failed to find Wayward Star.");
					return CommandResult.ERROR;
				}
				SotfWaywardStarIntel.addReveriesMeal(waywardStar);
				Console.showMessage("Something has been uncovered by the wayward star...");
				return CommandResult.SUCCESS;
			}
			else if (args.equals("sierraXO") || args.equals("sxo")) {
				if (SotfMisc.isSecondInCommandEnabled()) {
					if (!SotfMisc.playerHasSierra()) {
						Global.getSector().getPlayerFleet().getFleetData().addFleetMember("sotf_pledge_Base");
					}

					Global.getSector().getMemoryWithoutUpdate().set("$sotf_gotSierraXO", true);
					SCOfficer officer = new SCOfficer(SotfPeople.getPerson(SotfPeople.SIERRA), "sotf_witchcraft");
					officer.increaseLevel(4);

					SCData data = SCUtils.getPlayerData();
					data.addOfficerToFleet(officer);
					data.setOfficerInEmptySlotIfAvailable(officer);
					Global.getSector().getMemoryWithoutUpdate().set("$sotf_sierraXO", officer);

					Console.showMessage("Let's dance, kindred!");
					return CommandResult.SUCCESS;
				}
			}
			else if (args.equals("hfinale")) {
				Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_DID_HAUNTED_INTRO, true);
				Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_DID_HAUNTED_MILE1, true);
				Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_DID_HAUNTED_PENULT, true);
				Global.getSector().getMemoryWithoutUpdate().unset(SotfIDs.MEM_DID_HAUNTED_ULT);
				int level = Global.getSector().getPlayerPerson().getStats().getLevel();
				Global.getSector().getPlayerPerson().getStats().setLevel(15);
				Global.getSector().getPlayerPerson().getStats().addPoints(15 - level);
				Global.getSector().getPlayerPerson().getStats().addStoryPoints(25);
				Console.showMessage("The end draws close...");
				return CommandResult.SUCCESS;
			}
			else if (args.equals("hfinfel")) {
				for (CampaignFleetAPI fleet : Global.getSector().getStarSystem("sotf_lotl").getFleets()) {
					if (!fleet.isPlayerFleet()) {
						fleet.despawn();
					}
				}
				SotfHauntedFinale.spawnFleet();
				Console.showMessage("Respawned Fel fleet, let's try again!");
				return CommandResult.SUCCESS;
			}
			else if (args.equals("cotl")) {
				CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
				Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_COTL_START, true);
				MemoryAPI char_mem = Global.getSector().getPlayerPerson().getMemoryWithoutUpdate();
				//char_mem.set(SotfIDs.GUILT_KEY, SotfMisc.getHauntedGuilt());
				//Global.getSector().addScript(new SotfGenericDialogScript("sotfCOTLIntro"));
				if (!Global.getSector().getIntelManager().hasIntelOfClass(SotfSiriusIntel.class)) {
					Global.getSector().getIntelManager().addIntel(new SotfSiriusIntel(), false);
				}
				Console.showMessage("Her blessings upon us...");
				return CommandResult.SUCCESS;
			}
			Console.showMessage("Invalid command!");
			return CommandResult.ERROR;
		} else {
			Console.showMessage("Invalid command!");
			return CommandResult.ERROR;
		}
	}
}