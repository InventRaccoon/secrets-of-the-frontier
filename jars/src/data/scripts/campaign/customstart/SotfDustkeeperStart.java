package data.scripts.campaign.customstart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.CharacterCreationData;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.NGCAddStandardStartingScript;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;
import exerelin.campaign.ExerelinSetupData;
import exerelin.campaign.PlayerFactionStore;
import exerelin.campaign.customstart.CustomStart;
import exerelin.utilities.StringHelper;

import java.util.Map;

import static data.scripts.SotfModPlugin.WATCHER;

/**
 *	BANSHEE'S LOST THREAD: Begin with a small fleet, Automated Ships at level 1, and Inky-Echo-Nightingale
 */

public class SotfDustkeeperStart extends CustomStart {

    @Override
    public void execute(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        CharacterCreationData data = (CharacterCreationData) memoryMap.get(MemKeys.LOCAL).get("$characterData");

        if (!WATCHER) {
            dialog.getTextPanel().addParagraph("This start option is part of Secrets of the Frontier's " +
                    "\"Watcher Beyond the Walls\" submodule, which you have disabled. Turn it on to access this content.", Misc.getNegativeHighlightColor());
            dialog.getTextPanel().highlightInLastPara(Misc.getHighlightColor(), "\"Watcher Beyond the Walls\"");
            dialog.getOptionPanel().addOption(StringHelper.getString("back", true), "nex_NGCStartBack");
            return;
        }

        if (Global.getSettings().getMissionScore("dawnanddust") < 80 && SotfMisc.getLockoutStarts()) {
            dialog.getTextPanel().addParagraph("This start option is currently locked. Complete the " +
                    "\"Dawn and Dust\" combat mission with a score of at least 80% to unlock it.", Misc.getNegativeHighlightColor());
            dialog.getTextPanel().highlightInLastPara(Misc.getHighlightColor(), "locked", "\"Dawn and Dust\"");
            dialog.getOptionPanel().addOption(StringHelper.getString("back", true), "nex_NGCStartBack");
            return;
        }

        PlayerFactionStore.setPlayerFactionIdNGC(Factions.PLAYER);

        CampaignFleetAPI tempFleet = FleetFactoryV3.createEmptyFleet(
                PlayerFactionStore.getPlayerFactionIdNGC(), FleetTypes.PATROL_SMALL, null);

        addFleetMember("mule_Standard", dialog, data, tempFleet, "flagship");
        addFleetMember("wayfarer_Starting", dialog, data, tempFleet, "none");
        addFleetMember("shepherd_Starting", dialog, data, tempFleet, "none");
        addFleetMember("dram_Light", dialog, data, tempFleet, "none");
        //addFleetMember("sotf_glimmer_Nightingale", dialog, data, tempFleet, "nightingale");
        //addFleetMember("sotf_lumen_Nightingale", dialog, data, tempFleet, "nightingale");
        addFleetMember("sotf_memoir_Nightingale", dialog, data, tempFleet, "nightingale");

        // can't do it like this, if you back out then pick another start, you'll still have A.S.
        //MutableCharacterStatsAPI stats = data.getPerson().getStats();
        //stats.setSkillLevel(Skills.AUTOMATED_SHIPS, 1);

        dialog.getTextPanel().setFontSmallInsignia();
        if (!Global.getSettings().getModManager().isModEnabled("second_in_command")) {
            dialog.getTextPanel().addParagraph("Gained skill: Automated Ships", Misc.getPositiveHighlightColor());
            dialog.getTextPanel().highlightInLastPara(Global.getSettings().getSkillSpec(Skills.AUTOMATED_SHIPS).getGoverningAptitudeColor().brighter(), "Automated Ships");
        } else {
            dialog.getTextPanel().addParagraph("Gained an executive officer with the Automated specialty");
            dialog.getTextPanel().highlightInLastPara("executive officer", "Automated");
            dialog.getTextPanel().setHighlightColorsInLastPara(Misc.getHighlightColor(), Global.getSettings().getSkillSpec(Skills.AUTOMATED_SHIPS).getGoverningAptitudeColor().brighter());
        }
        dialog.getTextPanel().setFontInsignia();

        // Second in Command skill rework - need to give player the skill point they should get
        // since the skill will be converted into an executive officer
        if (Global.getSettings().getModManager().isModEnabled("second_in_command")) {
            data.getPerson().getStats().addPoints(1);
        }
        data.getStartingCargo().getCredits().add(15000);
        AddRemoveCommodity.addCreditsGainText(15000, dialog.getTextPanel());

        tempFleet.getFleetData().setSyncNeeded();
        tempFleet.getFleetData().syncIfNeeded();
        tempFleet.forceSync();

        int crew = 0;
        int fuel = 0;
        int supplies = 0;
        for (FleetMemberAPI member : tempFleet.getFleetData().getMembersListCopy()) {
            crew += member.getMinCrew() + (int) ((member.getMaxCrew() - member.getMinCrew()) * 0.3f);
            fuel += (int) member.getFuelCapacity() * 0.5f;
            supplies += (int) member.getBaseDeploymentCostSupplies() * 3;
        }
        data.getStartingCargo().addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.CREW, crew);
        data.getStartingCargo().addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.FUEL, fuel);
        data.getStartingCargo().addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.SUPPLIES, supplies);
        data.getStartingCargo().addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.HEAVY_MACHINERY, 40);

        AddRemoveCommodity.addCommodityGainText(Commodities.CREW, crew, dialog.getTextPanel());
        AddRemoveCommodity.addCommodityGainText(Commodities.FUEL, fuel, dialog.getTextPanel());
        AddRemoveCommodity.addCommodityGainText(Commodities.SUPPLIES, supplies, dialog.getTextPanel());
        AddRemoveCommodity.addCommodityGainText(Commodities.HEAVY_MACHINERY, 40, dialog.getTextPanel());

        PlayerFactionStore.setPlayerFactionIdNGC(Factions.PLAYER);
        ExerelinSetupData.getInstance().freeStart = true;

        data.addScript(new Script() {
            public void run() {
                CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();

                // SEQUENCE BREAK!
                Global.getSector().getPlayerPerson().getStats().setSkillLevel(Skills.AUTOMATED_SHIPS, 1);
                Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().set(SotfIDs.MEM_HEARD_ABOUT_BANSHEE, true);
                Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_BEGAN_WITH_NIGHTINGALE, true);
                // Dustkeepers like you a bit already, and you know of them
                Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).setShowInIntelTab(true);
                Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).setRelationship(Factions.PLAYER, RepLevel.FAVORABLE);
                Global.getSector().getPlayerMemoryWithoutUpdate().set("$sotf_knowDustkeepers", true);
                // bcs Nex will override our ODS Songless name, we need a script to change it back
                Global.getSector().addScript(new SotfNightingaleNameFixer());

                NGCAddStandardStartingScript.adjustStartingHulls(fleet);

                fleet.getFleetData().ensureHasFlagship();

                // generate Nightingale and add her to the player's fleet
                for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                    if (member.getVariant().hasHullMod(HullMods.AUTOMATED)) {
                        // she doesn't actually exist yet (SotfPeople is run later than this code) so let's get her generated
                        if (SotfPeople.getPerson(SotfPeople.NIGHTINGALE) == null) {
                            PersonAPI person = SotfPeople.genNightingale();
                            person.getRelToPlayer().setLevel(RepLevel.COOPERATIVE);
                            person.getMemoryWithoutUpdate().set(SotfIDs.MEM_WARMIND_NO_TRAITOR, true);
                            person.setFaction(Factions.PLAYER); // player-faction, since she's loyal primarily to them now
                            Global.getSector().getImportantPeople().addPerson(person);
                        }
                        member.setShipName("ODS Songless");
                        member.setCaptain(SotfPeople.getPerson(SotfPeople.NIGHTINGALE));
                        member.getVariant().addTag(Tags.TAG_AUTOMATED_NO_PENALTY);
                    }
                }

                for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                    float max = member.getRepairTracker().getMaxCR();
                    member.getRepairTracker().setCR(max);
                }
                fleet.getFleetData().setSyncNeeded();
            }
        });

        dialog.getVisualPanel().showFleetInfo(StringHelper.getString("exerelin_ngc", "playerFleet", true),
                tempFleet, null, null);

        dialog.getOptionPanel().addOption(StringHelper.getString("done", true), "nex_NGCDone");
        dialog.getOptionPanel().addOption(StringHelper.getString("back", true), "nex_NGCStartBack");
    }

    public void addFleetMember(String vid, InteractionDialogAPI dialog, CharacterCreationData data, CampaignFleetAPI fleet, String special) {
        data.addStartingFleetMember(vid, FleetMemberType.SHIP);
        FleetMemberAPI temp = Global.getFactory().createFleetMember(FleetMemberType.SHIP, vid);
        fleet.getFleetData().addFleetMember(temp);
        temp.getRepairTracker().setCR(0.7f);

        if (special.equals("flagship")) {
            fleet.getFleetData().setFlagship(temp);
            temp.setCaptain(data.getPerson());
        }
        if (special.equals("nightingale")) {
            PersonAPI nightingale = SotfPeople.genNightingale();
            temp.setCaptain(nightingale);
            temp.getRepairTracker().setCR(0.85f);
            temp.setShipName("ODS Songless");
            dialog.getTextPanel().setFontSmallInsignia();
            dialog.getTextPanel().addParagraph("Gained a silent companion", Misc.getPositiveHighlightColor());
            dialog.getTextPanel().highlightInLastPara(nightingale.getFaction().getBaseUIColor(), "silent companion");
            dialog.getTextPanel().setFontInsignia();
            nightingale.setFaction(Factions.PLAYER);
        } else {
            AddRemoveCommodity.addFleetMemberGainText(temp.getVariant(), dialog.getTextPanel());
        }
    }
}
