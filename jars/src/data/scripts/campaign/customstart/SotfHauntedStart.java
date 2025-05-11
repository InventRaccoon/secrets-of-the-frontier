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
import data.scripts.dialog.SotfGenericDialogScript;
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

public class SotfHauntedStart extends CustomStart {

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

        data.addScriptBeforeTimePass(new Script() {
            public void run() {
                CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
                Global.getSector().getMemoryWithoutUpdate().set("$sotf_hauntedStart", true);
                MemoryAPI char_mem = Global.getSector().getPlayerPerson().getMemoryWithoutUpdate();
                char_mem.set(SotfIDs.GUILT_KEY, SotfMisc.getHauntedGuilt());
                char_mem.set(MemFlags.PLAYER_ATROCITIES, 4f);
                //Global.getSector().getMemoryWithoutUpdate().set("$nex_startLocation", "nomios");
            }
        });

        PlayerFactionStore.setPlayerFactionIdNGC(Factions.PIRATES);

        CampaignFleetAPI tempFleet = FleetFactoryV3.createEmptyFleet(
                PlayerFactionStore.getPlayerFactionIdNGC(), FleetTypes.PATROL_SMALL, null);

        addFleetMember("manticore_pirates_Support", dialog, data, tempFleet, "flagship");
        addFleetMember("hound_d_pirates_Shielded", dialog, data, tempFleet, "none");
        addFleetMember("mudskipper2_Hellbore", dialog, data, tempFleet, "bait");

        MutableCharacterStatsAPI stats = data.getPerson().getStats();
        stats.addPoints(2);

        dialog.getTextPanel().setFontSmallInsignia();
        dialog.getTextPanel().addParagraph("Gained a reprehensible history of atrocities and the gnawing guilt that accompanies it", Misc.getNegativeHighlightColor());
        dialog.getTextPanel().highlightInLastPara(Misc.getHighlightColor(), "a reprehensible history of atrocities", "gnawing guilt");

        dialog.getTextPanel().addParagraph("Acquired a vengeful and relentless pursuer", Misc.getNegativeHighlightColor());
        dialog.getTextPanel().highlightInLastPara(Misc.getHighlightColor(), "vengeful and relentless pursuer");

        dialog.getTextPanel().addParagraph("Gained a bonus skill point", Misc.getPositiveHighlightColor());
        dialog.getTextPanel().highlightInLastPara(Misc.getHighlightColor(), "bonus skill point");
        dialog.getTextPanel().setFontInsignia();

        data.getStartingCargo().getCredits().add(25000);
        AddRemoveCommodity.addCreditsGainText(25000, dialog.getTextPanel());

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

        // enforce normal difficulty
        data.setDifficulty("normal");
        ExerelinSetupData.getInstance().easyMode = false;
        ExerelinSetupData.getInstance().hardMode = true;
        ExerelinSetupData.getInstance().freeStart = true;
        ExerelinSetupData.getInstance().randomStartLocation = true;
        ExerelinSetupData.getInstance().dModLevel = 3;

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

                // handled by SotfGuiltTracker
                //Global.getSector().addScript(new SotfGenericDialogScript("sotfHauntedIntro"));
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
        if (special.equals("bait")) {
            temp.setShipName("Torpedo Bait");
        }

        AddRemoveCommodity.addFleetMemberGainText(temp.getVariant(), dialog.getTextPanel());
    }

}
