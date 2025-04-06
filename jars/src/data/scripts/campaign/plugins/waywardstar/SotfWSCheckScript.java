// Wayward Star: start the encounter if the player makes a distress call or leaves their neutrino detector on
package data.scripts.campaign.plugins.waywardstar;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import data.scripts.dialog.SotfWSSierraSignalsPlugin;
import data.scripts.dialog.SotfWSEidolonOpen;
import data.scripts.utils.SotfMisc;

import java.util.Map;

public class SotfWSCheckScript implements EveryFrameScript {

    private float counter = 0;
    private float time_since_last = 5;
    private float sierraConvTimer = 0;

    public void advance(float amount) {
        if (!Global.getCurrentState().equals(GameState.CAMPAIGN) || Global.getSector().isPaused()) {
            return;
        }
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (Global.getSector().getMemoryWithoutUpdate().contains("$sotf_WSCombatWon")) {
            Global.getSector().removeScript(this);
            return;
        }
        if (!playerFleet.getContainingLocation().getMemoryWithoutUpdate().contains("$sotf_waywardstar") || !SotfMisc.playerHasSierra()) {
            return;
        }
        // Sierra eventually suggests you try some abilities
        if (!Global.getSector().getMemoryWithoutUpdate().contains("$sotf_WSSierraSignals") && !Global.getSector().getMemoryWithoutUpdate().contains("$sotf_SierraCommsDenied")) {
            sierraConvTimer += Global.getSector().getClock().convertToDays(amount);
            if (sierraConvTimer > 5) {
                Global.getSector().getMemoryWithoutUpdate().set("$sotf_WSSierraSignals", true);
                Global.getSector().getCampaignUI().showInteractionDialog(new SotfWSSierraSignalsPlugin(), null);
            }
        }
        time_since_last += amount;
        if (time_since_last < 15f) {
            return;
        }
        boolean can_tick = playerFleet.getAbility(Abilities.DISTRESS_CALL).isOnCooldown();
        //if (!can_tick && playerFleet.hasAbility(Abilities.GRAVITIC_SCAN)) {
        //    can_tick = playerFleet.getAbility(Abilities.GRAVITIC_SCAN).isActive();
        //}
        Map<String, AbilityPlugin> ability_map = playerFleet.getAbilities();
        for (String id : ability_map.keySet()) {
            if (!id.contains("neutrino") && !id.contains("scan")) {
                continue;
            }
            if (playerFleet.getAbility(id).isActive()) {
                can_tick = true;
            }
        }
        if (playerFleet.getAbility(Abilities.DISTRESS_CALL).isOnCooldown()) {
            if (counter < 4) {
                counter = 4;
            }
        }
        if (can_tick) {
            counter += amount;
        }
        if (counter < 5) {
            return;
        }
        time_since_last = 0;
        counter = 0;
        Global.getSector().getCampaignUI().showInteractionDialog(new SotfWSEidolonOpen(), null);
    }

    public boolean isDone() {
        return false;
    }

    public boolean runWhilePaused() {
        return false;
    }

}