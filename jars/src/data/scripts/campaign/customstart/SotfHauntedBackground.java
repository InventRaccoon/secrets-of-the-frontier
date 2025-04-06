package data.scripts.campaign.customstart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.dialog.SotfGenericDialogScript;
import data.scripts.utils.SotfMisc;
import exerelin.campaign.backgrounds.BaseCharacterBackground;
import exerelin.utilities.NexFactionConfig;

import static data.scripts.SotfModPlugin.WATCHER;

/**
 *	THE HAUNTED - BACKGROUND EDITION: Begin with enough guilt that Felcesis will invade you regularly
 *  BUT you also gain a free skill point - effectively 1 permanent bonus level
 */

public class SotfHauntedBackground extends BaseCharacterBackground {

    @Override
    public float getOrder() {
        return 100;
    }

    @Override
    public boolean shouldShowInSelection(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        return WATCHER;
    }

    public void onNewGameAfterTimePass(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        Global.getSector().getMemoryWithoutUpdate().set("$sotf_hauntedStart", true);
        MemoryAPI char_mem = Global.getSector().getPlayerPerson().getMemoryWithoutUpdate();
        char_mem.set(SotfIDs.GUILT_KEY, SotfMisc.getHauntedGuilt());
        char_mem.set(MemFlags.PLAYER_ATROCITIES, 4f);
        Global.getSector().getPlayerPerson().getStats().addPoints(1);
        // handled by SotfGuiltTracker
        //Global.getSector().addScript(new SotfGenericDialogScript("sotfHauntedIntro"));
    }

}
