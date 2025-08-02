package data.scripts.campaign.customstart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.CharacterCreationData;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.SharedUnlockData;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.NGCAddStandardStartingScript;
import com.fs.starfarer.api.util.DelayedActionScript;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.utils.SotfMisc;
import exerelin.campaign.ExerelinSetupData;
import exerelin.campaign.PlayerFactionStore;
import exerelin.campaign.customstart.CustomStart;
import exerelin.utilities.StringHelper;

import java.util.Map;

import static data.scripts.SotfModPlugin.WATCHER;

/**
 *	THE HAUNTED: Begin with a pathetic pirate fleet, and enough guilt that Felcesis will invade you regularly
 *  BUT you also gain a free skill point - effectively 1 permanent bonus level
 */

public class SotfChildOfTheLakeStart extends CustomStart {

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

        if (!(SharedUnlockData.get().getSet("sotf_persistent").contains("sotf_haunted_completed") || !SotfMisc.getLockoutStarts())) {
            dialog.getTextPanel().addParagraph("This start option is currently locked. Complete the finale of the " +
                    "\"The Haunted\" custom start or background to unlock it.", Misc.getNegativeHighlightColor());
            dialog.getTextPanel().highlightInLastPara(Misc.getHighlightColor(), "locked", "\"The Haunted\"");
            dialog.getOptionPanel().addOption(StringHelper.getString("back", true), "nex_NGCStartBack");
            return;
        }

        data.addScriptBeforeTimePass(new Script() {
            public void run() {
                Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_COTL_START, true);
                //Global.getSector().getMemoryWithoutUpdate().set("$nex_startLocation", "nomios");
            }
        });

        PlayerFactionStore.setPlayerFactionIdNGC(Factions.PLAYER);

        CampaignFleetAPI tempFleet = FleetFactoryV3.createEmptyFleet(
                PlayerFactionStore.getPlayerFactionIdNGC(), FleetTypes.PATROL_SMALL, null);

        addFleetMember("sotf_thorn_Gatekeeper", dialog, data, tempFleet, "flagship");
        addFleetMember("wolf_Starting", dialog, data, tempFleet, "none");
        addFleetMember("dram_Light", dialog, data, tempFleet, "none");

        data.getStartingCargo().getCredits().add(30000);
        AddRemoveCommodity.addCreditsGainText(30000, dialog.getTextPanel());
        //MutableCharacterStatsAPI stats = data.getPerson().getStats();
        //stats.addPoints(1);

        dialog.getTextPanel().setFontSmallInsignia();
        dialog.getTextPanel().addParagraph("Gained \"Invoke Her Blessing\"", Misc.getPositiveHighlightColor());
        dialog.getTextPanel().highlightInLastPara(SotfMisc.DAYDREAM_COLOR, "\"Invoke Her Blessing\"");
        dialog.getTextPanel().addParagraph("    - Use on echoes left by destroyed ships to create a mimic that fights for you", SotfMisc.DAYDREAM_COLOR);
        dialog.getTextPanel().addParagraph("    - Choose from upgrades as you level", SotfMisc.DAYDREAM_COLOR);
        dialog.getTextPanel().addParagraph("Learned \"Cult of the Daydream\" ships and weapons blueprints", Misc.getPositiveHighlightColor());
        dialog.getTextPanel().highlightInLastPara(SotfMisc.DAYDREAM_COLOR, "\"Cult of the Daydream\"");

        tempFleet.getFleetData().setSyncNeeded();
        tempFleet.getFleetData().syncIfNeeded();
        tempFleet.forceSync();

        int crew = 0;
        int fuel = 0;
        int supplies = 0;
        for (FleetMemberAPI member : tempFleet.getFleetData().getMembersListCopy()) {
            crew += member.getMinCrew() + (int) ((member.getMaxCrew() - member.getMinCrew()) * 0.1f);
            fuel += (int) member.getFuelCapacity() * 0.35f;
            supplies += (int) member.getBaseDeploymentCostSupplies() * 2;
        }
        data.getStartingCargo().addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.CREW, crew);
        data.getStartingCargo().addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.FUEL, fuel);
        data.getStartingCargo().addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.SUPPLIES, supplies);

        AddRemoveCommodity.addCommodityGainText(Commodities.CREW, crew, dialog.getTextPanel());
        AddRemoveCommodity.addCommodityGainText(Commodities.FUEL, fuel, dialog.getTextPanel());
        AddRemoveCommodity.addCommodityGainText(Commodities.SUPPLIES, supplies, dialog.getTextPanel());
        AddRemoveCommodity.addCommodityGainText(Commodities.HEAVY_MACHINERY, 15, dialog.getTextPanel());

        PlayerFactionStore.setPlayerFactionIdNGC(Factions.PLAYER);
        ExerelinSetupData.getInstance().freeStart = true;

        data.addScript(new Script() {
            public void run() {
                CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();

                NGCAddStandardStartingScript.adjustStartingHulls(fleet);

                fleet.getFleetData().ensureHasFlagship();

                for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                    float max = member.getRepairTracker().getMaxCR();
                    member.getRepairTracker().setCR(max);
                }
                fleet.getFleetData().setSyncNeeded();

                //Intro
                Global.getSector().addScript(new DelayedActionScript(0.25f) {
                    @Override
                    public void doAction() {

                        SotfChildOfTheLakeCampaignVFX.fadeIn(1f);
                        Global.getSector().addScript(new DelayedActionScript(1f) {
                            @Override
                            public void doAction() {
                                CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
                                Misc.showRuleDialog(pf, "sotfCOTLIntro");

                                InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
                                if (dialog != null) {
                                    dialog.setBackgroundDimAmount(0.4f);
                                }

                                SotfChildOfTheLakeCampaignVFX.fadeOut(1f);
                            }
                        });

                    }
                });
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

        AddRemoveCommodity.addFleetMemberGainText(temp.getVariant(), dialog.getTextPanel());
    }

}
