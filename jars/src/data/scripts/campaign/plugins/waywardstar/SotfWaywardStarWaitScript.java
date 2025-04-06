// waits a while after A Memory and then begins Wayward Star
package data.scripts.campaign.plugins.waywardstar;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import data.scripts.campaign.intel.quests.SotfWaywardStarIntel;

public class SotfWaywardStarWaitScript implements EveryFrameScript {

    private float counter = 0f;

    public void advance(float amount) {
        counter += Global.getSector().getClock().convertToDays(amount);

        if (counter < 14f) {
            return;
        }

        SotfWaywardStarIntel intel = new SotfWaywardStarIntel();
        intel.setImportant(true);
        Global.getSector().getIntelManager().queueIntel(intel);
        Global.getSector().removeScript(this);
    }

    public boolean isDone() {
        return false;
    }

    public boolean runWhilePaused() {
        return false;
    }

}