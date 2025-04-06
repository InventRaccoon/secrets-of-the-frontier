// handles guilt gain from atrocities
// Sierra's responses to such are handled in her conversation intel
package data.scripts.campaign.plugins.fel;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyPlayerHostileActListener;
import com.fs.starfarer.api.campaign.listeners.FleetInflationListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.campaign.fleet.FleetMember;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.dialog.SotfGenericDialogScript;
import data.scripts.utils.SotfMisc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks guilt gain from atrocities and handles the dreams during The Haunted
 */

public class SotfGuiltTracker extends BaseCampaignEventListener implements EveryFrameScript, ColonyPlayerHostileActListener, FleetInflationListener {

    // so we know if Reality Breaker is OK to use
    public float timeSinceSave = 0f;

    // no guilt buildup for these ones - factions who don't really have civilians to satbomb
    public static List<String> GUILTY_FACTIONS = new ArrayList<>();
    static {
        GUILTY_FACTIONS.add(SotfIDs.DUSTKEEPERS); // when the singularity happens, they're gonna kill me for this one
        GUILTY_FACTIONS.add("nex_derelict"); // Derelict Empire
        GUILTY_FACTIONS.add("fang"); // werewolves
        GUILTY_FACTIONS.add("draco"); // vampires
        GUILTY_FACTIONS.add("HIVER"); // bugs
        GUILTY_FACTIONS.add("enigma"); // ???
    }

    public SotfGuiltTracker() {
        super(true);
    }

    public void advance(float amount) {
        timeSinceSave += amount;
        PersonAPI player = Global.getSector().getPlayerPerson();
        MemoryAPI sector_mem = Global.getSector().getMemoryWithoutUpdate();

        // Haunted start dreams handling
        if (!sector_mem.contains(SotfIDs.MEM_HAUNTED_START)) return;

        if (!sector_mem.contains(SotfIDs.MEM_DID_HAUNTED_INTRO)) {
            sector_mem.set(SotfIDs.MEM_DID_HAUNTED_INTRO, true);
            Global.getSector().addScript(new SotfGenericDialogScript("sotfHauntedIntro"));
        } else if (player.getStats().getLevel() >= 5 && !sector_mem.contains(SotfIDs.MEM_DID_HAUNTED_MILE1)) {
            sector_mem.set(SotfIDs.MEM_DID_HAUNTED_MILE1, true);
            Global.getSector().addScript(new SotfGenericDialogScript("sotfHauntedMilestone1"));
        } else if (player.getStats().getLevel() >= 10 && !sector_mem.contains(SotfIDs.MEM_DID_HAUNTED_PENULT)) {
            sector_mem.set(SotfIDs.MEM_DID_HAUNTED_PENULT, true);
            Global.getSector().addScript(new SotfGenericDialogScript("sotfHauntedPenultimate"));
        }
//        } else if (player.getStats().getLevel() >= 15 && !sector_mem.contains(SotfIDs.MEM_DID_HAUNTED_ULT)) {
//            sector_mem.set(SotfIDs.MEM_DID_HAUNTED_ULT, true);
//            Global.getSector().addScript(new SotfGenericDialogScript("sotfHauntedUltimate"));
//        }
    }

    public boolean isDone() {
        return false;
    }

    public boolean runWhilePaused() {
        return false;
    }

    public void reportRaidForValuablesFinishedBeforeCargoShown(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData, CargoAPI cargo) {
        //
    }

    public void reportFleetInflated(CampaignFleetAPI fleet, FleetInflater inflater) {
        //
    }

    public void reportRaidToDisruptFinished(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData, Industry industry) {
        //
    }

    public void reportTacticalBombardmentFinished(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData) {
        //
    }
    public void reportSaturationBombardmentFinished(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData) {
        if (GUILTY_FACTIONS.contains(market.getFactionId())) {
            return;
        }
        float guiltToAdd = 1f;
        if (market.getSize() >= 4) {
            guiltToAdd++;
        }
        if (market.getSize() >= 6) {
            guiltToAdd++;
        }
        if (market.getSize() >= 8) {
            guiltToAdd++;
        }
        SotfMisc.addGuilt(guiltToAdd);
    }

}
