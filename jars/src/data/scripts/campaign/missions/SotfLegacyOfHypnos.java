package data.scripts.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.missions.academy.GAProbePackage;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;

import java.awt.*;
import java.util.Map;

/**
 *	LEGACY OF HYPNOS (lead from Inadvertent or Wendigo)
 */

public class SotfLegacyOfHypnos extends HubMissionWithSearch {

    public static enum Stage {
        GO_TO_SYSTEM,
        RETURN_TO_GIVER,
        COMPLETED
    }

    protected SectorEntityToken lab;
    protected StarSystemAPI system;

    protected boolean fromInad = false;

    // run when the bar event starts / when we ask a contact about the mission
    protected boolean create(MarketAPI createdAt, boolean barEvent) {
        if (!setGlobalReference("$sotf_loh_ref")) return false;

        fromInad = getPerson() != null && getPerson().getId().equals(SotfPeople.INADVERTENT);

        setName("Legacy of Hypnos");
        setRepFactionChangesMedium();
        if (fromInad) {
            setRepPersonChangesMedium();
        } else {
            setRepPersonChangesHigh();
        }
        setCreditReward(CreditReward.HIGH);
        completedKey = "$sotf_hypnosCompleted";

        // never give the mission if the player has already found the Hypnos lab
        // can't finish its interaction without getting the below memory flag
        if (Global.getSector().getPlayerMemoryWithoutUpdate().contains("$sotf_knowDkScans")) return false;

        lab = (SectorEntityToken) Global.getSector().getMemoryWithoutUpdate().get(SotfIDs.HYPNOS_LAB);
        // this really shouldn't happen but ok
        if (lab == null) return false;

        system = lab.getStarSystem();

        if (system == null) return false;

        setStartingStage(Stage.GO_TO_SYSTEM);
        setSuccessStage(Stage.COMPLETED);

        makeImportant(lab, "$sotf_loh", Stage.GO_TO_SYSTEM);
        makeImportant(getPerson(), "$sotf_loh_return", Stage.RETURN_TO_GIVER);

        // set stage transitions when certain global flags are set
        setStageOnGlobalFlag(Stage.RETURN_TO_GIVER, "$sotf_loh_gotData");
        setStageOnGlobalFlag(Stage.COMPLETED, "$sotf_loh_completed");

        setMapMarkerNameColorBasedOnStar(system);

        setNoAbandon();
        return true;
    }

    @Override
    protected void endSuccessImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        if (fromInad) {
            CoreReputationPlugin.MissionCompletionRep completionRepPerson = new CoreReputationPlugin.MissionCompletionRep(
                    getRepRewardSuccessPerson(), getRewardLimitPerson(),
                    -getRepPenaltyFailurePerson(), getPenaltyLimitPerson());
            ReputationActionResponsePlugin.ReputationAdjustmentResult rep = Global.getSector().adjustPlayerReputation(
                    new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.MISSION_SUCCESS, completionRepPerson,
                            dialog.getTextPanel(), true, true),
                    SotfPeople.getPerson(SotfPeople.WENDIGO));
            completionRepPerson.successDelta = 0;
        }
    }

    protected void updateInteractionDataImpl() {
        set("$sotf_loh_systemName", system.getNameWithLowercaseType());
        set("$sotf_loh_dist", getDistanceLY(lab));
        set("$sotf_loh_reward", getCreditsReward());
        set("$sotf_loh_stage", getCurrentStage());
        if (fromInad) {
            set("$sotf_loh_fromInad", true);
        }
    }

    //protected String getMissionTypeNoun() {
    //    return "lead";
    //}

    // description when selected in intel screen
    @Override
    public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
        float opad = 10f;
        Color h = Misc.getHighlightColor();

        if (!fromInad) {
            info.addPara("Annex-Wendigo informed you about a Domain-era laboratory located in " + system.getNameWithNoType() + ". They " +
                    "requested you find the station and recover any important data you can find.", opad);
        } else {
            info.addPara("Echo-Inadvertent informed you about a Domain-era laboratory located in " + system.getNameWithNoType() + ". He " +
                    "requested you find the station and recover any important data you can find.", opad);
        }

        if (currentStage == Stage.GO_TO_SYSTEM) {
            info.addPara("Go to "+ system.getNameWithNoType() + " and locate the laboratory.", opad);
        } else if (currentStage == Stage.RETURN_TO_GIVER) {
            info.addPara(getReturnTextShort(getPerson().getMarket()) + " with the data you obtained.", opad);
        }
    }

    // short description in popups and the intel entry
    @Override
    public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
        Color h = Misc.getHighlightColor();
        if (currentStage == Stage.GO_TO_SYSTEM) {
            info.addPara("Search the " +
                    system.getNameWithLowercaseTypeShort(), tc, pad);
            return true;
        } else if (currentStage == Stage.RETURN_TO_GIVER) {
            info.addPara(getReturnTextShort(getPerson().getMarket()), tc, pad);
            return true;
        }
        return false;
    }

}
