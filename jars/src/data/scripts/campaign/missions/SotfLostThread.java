package data.scripts.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;

import java.awt.*;
import java.util.Map;

/**
 *	A LOST THREAD
 */

public class SotfLostThread extends HubMissionWithSearch implements FleetEventListener {

    public static enum Stage {
        GO_TO_SYSTEM,
        FIND_NIGHTINGALE,
        MAKE_DECISION,
        COMPLETED,
        DESTROYED_NIGHTINGALE
    }

    // Nightingale's fleet
    protected CampaignFleetAPI nightingale;
    protected StarSystemAPI system;

    protected boolean fromInad = false;

    // run when the bar event starts / when we ask a contact about the mission
    protected boolean create(MarketAPI createdAt, boolean barEvent) {
        setName("A Lost Thread");
        setRepFactionChangesNone();
        setRepPersonChangesNone();
        completedKey = "$sotf_lostthreadCompleted";

        system = (StarSystemAPI) Global.getSector().getMemoryWithoutUpdate().get(SotfIDs.MEM_NIGHTINGALE_SYSTEM);
        fromInad = getPerson() != null && getPerson().getId().equals(SotfPeople.INADVERTENT);

        if (Global.getSector().getMemoryWithoutUpdate().contains("$sotf_didLostThread")) return false;

        if (system == null) return false;

        nightingale = (CampaignFleetAPI) Global.getSector().getMemoryWithoutUpdate().get(SotfIDs.MEM_NIGHTINGALE_FLEET);

        if (nightingale == null) return false;

        nightingale.addEventListener(this);

        if (!setGlobalReference("$sotf_lostthread_ref")) return false;

        // set our starting, success and failure stages
        if (!fromInad) {
            setStartingStage(Stage.FIND_NIGHTINGALE);
        } else {
            setStartingStage(Stage.GO_TO_SYSTEM);
        }
        setSuccessStage(Stage.COMPLETED);
        setSuccessStage(Stage.DESTROYED_NIGHTINGALE);

        // set stage transitions when certain global flags are set
        setStageOnGlobalFlag(Stage.MAKE_DECISION, "$sotf_lostthread_acceptNightingale");
        setStageOnGlobalFlag(Stage.COMPLETED, "$sotf_lostthread_completed");
        setStageOnGlobalFlag(Stage.DESTROYED_NIGHTINGALE, "$sotf_lostthread_destroyedNightingale");

        setNoAbandon();
        return true;
    }

    public void acceptImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        if (Global.getSector().getMemoryWithoutUpdate().contains("$sotf_lostthread_acceptNightingale")) {
            currentStage = Stage.MAKE_DECISION;
        }
    }

    protected void updateInteractionDataImpl() {
        set("$sotf_lostthread_systemName", system.getNameWithLowercaseType());
    }

    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
    }

    // if the fleet despawns for whatever reason, end the mission
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
        if (isDone() || result != null) return;

        if (fleet == nightingale && currentStage != Stage.COMPLETED) {
            Global.getSector().getMemoryWithoutUpdate().set("$sotf_lostthread_destroyedNightingale", true);
            checkStageChangesAndTriggers(null, null);
        }
    }

    protected String getMissionTypeNoun() {
        return "lead";
    }

    // description when selected in intel screen
    @Override
    public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
        float opad = 10f;
        Color h = Misc.getHighlightColor();

        if (!fromInad || currentStage == Stage.FIND_NIGHTINGALE) {
            info.addPara("Investigating a beacon in hyperspace near " + system.getNameWithNoType() + ", " +
                    "you uncovered a remotely-uploaded distress signal.", opad);
        }
        if (fromInad) {
            info.addPara("Echo-Inadvertent informed you about a beacon in hyperspace near " + system.getNameWithNoType() + ", " +
                    "which was reported to have been subject to an unknown Dustkeeper override.", opad);
        }

        if (currentStage == Stage.GO_TO_SYSTEM) {
            info.addPara("The beacon may yield answers if inspected, or you could simply jump into the system to investigate yourself.", opad);
        } else if (currentStage == Stage.FIND_NIGHTINGALE) {
            info.addPara("The source of the distress signal is likely somewhere in the system.", opad);
        } else if (currentStage == Stage.MAKE_DECISION) {
            info.addPara("You traced the signal to a damaged AI designated Inky-Echo-Nightingale. She seemed " +
                    "willing to join your fleet, and should still be there waiting for you.", opad);
        }
    }

    // short description in popups and the intel entry
    @Override
    public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
        Color h = Misc.getHighlightColor();
        if (currentStage == Stage.GO_TO_SYSTEM) {
            info.addPara("Investigate the " +
                    system.getNameWithLowercaseTypeShort(), tc, pad);
            return true;
        } else if (currentStage == Stage.FIND_NIGHTINGALE) {
            info.addPara("Search the " +
                    system.getNameWithLowercaseTypeShort() + " for the signal's source", tc, pad);
            return true;
        } else if (currentStage == Stage.MAKE_DECISION) {
            info.addPara("Decide what to do with Inky-Echo-Nightingale", tc, pad);
            return true;
        }
        return false;
    }

    // where on the map the intel screen tells us to go
    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return getMapLocationFor(system.getCenter());
    }

}
