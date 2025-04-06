package data.scripts.campaign.plugins;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;
import second_in_command.SCUtils;

import java.util.ArrayList;

/**
 *	... Look, I just decided to be honest with how I name this one.
 *  Handily makes sure that things go as they should in regard to officers, e.g:
 *  - Make sure Sierra is where she should be, and that there's only 1 active Concord ship at a time
 *  - Make sure that ships piloted by officers with Derelict Contingent (i.e Barrow) are properly reset to being
 *  restorable once the officer is gone
 */

public class SotfOfficerJankHandler implements EveryFrameScript {

    public void advance(float amount) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) return;
        ArrayList<FleetMemberAPI> sierraShips = new ArrayList<>();
        // delete Sierra from non-Concord ships: failsafe to prevent duping and the like
        if (SotfModPlugin.WATCHER) {
            for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
                // modded Sierra ship compat
                if (member.getHullSpec().hasTag("sotf_add_concord") && !member.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD)) {
                    member.getVariant().addPermaMod(SotfIDs.SIERRAS_CONCORD);
                }
                if (member.getCaptain() == SotfPeople.getPerson(SotfPeople.SIERRA) && !member.getVariant().getHullMods().contains(SotfIDs.SIERRAS_CONCORD)) {
                    member.setCaptain(null);
                }
                if (member.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD) && !member.getVariant().hasTag(SotfIDs.TAG_INERT)) {
                    sierraShips.add(member);
                }

                if (member.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD) && SotfModPlugin.NEW_SIERRA_MECHANICS) {
                    // piloted by Sierra
                    if (member.getCaptain() != null && member.getCaptain().getId().equals(SotfPeople.SIERRA)) {
                        member.getVariant().removeTag(SotfIDs.TAG_INERT);
                    }
                    // piloted by player, Sierra on standby
                    else if (member.getCaptain() != null && member.getCaptain().isPlayer() &&
                            playerFleet.getCargo().getCommodityQuantity(SotfIDs.SIERRA_CORE_OFFICER) > 0) {
                        member.getVariant().removeTag(SotfIDs.TAG_INERT);
                    }
                    // Sierra is not around, but the SiC skill is active.
                   /* else if (SotfMisc.isSecondInCommandEnabled() && SCUtils.getPlayerData().isSkillActive("sotf_dance_between_realms")) {
                        member.getVariant().removeTag(SotfIDs.TAG_INERT);
                    }*/
                    // Sierra is not around
                    else {
                        member.getVariant().addTag(SotfIDs.TAG_INERT);
                    }
                }
            }
        }

        // only 1 active ship at a time
        if (sierraShips.size() > 1 && !SotfModPlugin.NEW_SIERRA_MECHANICS) {
            for (FleetMemberAPI extra : sierraShips) {
                if (sierraShips.indexOf(extra) != 0) {
                    SotfMisc.toggleSierra(extra, null);
                }
            }
        }

        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (member.getVariant().hasTag("sotf_dcontingent_unrestorable")) {
                if (member.getCaptain() != null) {
                    if (!member.getCaptain().getStats().hasSkill(SotfIDs.SKILL_DERELICTCONTINGENTP)) {
                        member.getVariant().removeTag("sotf_dcontingent_unrestorable");
                        member.getVariant().removeTag(Tags.VARIANT_UNRESTORABLE);
                    }
                } else {
                    member.getVariant().removeTag("sotf_dcontingent_unrestorable");
                    member.getVariant().removeTag(Tags.VARIANT_UNRESTORABLE);
                }
            }
        }
    }

    public boolean isDone() {
        return false;
    }

    public boolean runWhilePaused() {
        return true;
    }

}
