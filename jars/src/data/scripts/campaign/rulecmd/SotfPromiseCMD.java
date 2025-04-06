// checks if we can start A Promise's bar event
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.SotfModPlugin;
import lunalib.lunaSettings.LunaSettings;

import java.util.List;
import java.util.Map;

public class SotfPromiseCMD extends BaseCommandPlugin {

    protected CampaignFleetAPI playerFleet;
    protected MemoryAPI memory;
    protected InteractionDialogAPI dialog;
    protected Map<String, MemoryAPI> memoryMap;

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        this.dialog = dialog;
        this.memoryMap = memoryMap;

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        String cmd = null;
        cmd = params.get(0).getString(memoryMap);
        String param = null;
        if (params.size() > 1) {
            param = params.get(1).getString(memoryMap);
        }

        TextPanelAPI text = dialog.getTextPanel();

        switch (cmd) {
            case "checkStart":
                return SotfModPlugin.WATCHER && Global.getSector().getPlayerPerson().getStats().getLevel() >= 5 && !Global.getSector().getMemoryWithoutUpdate().contains("$sotf_apromiseCompleted");
            case "checkLowFreq":
                return getLowFreqPromise();
            default:
                return true;
        }
    }

    public static boolean getLowFreqPromise() {
        boolean lowFreq = Global.getSettings().getBoolean("sotf_lowFreqPromise");
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            lowFreq = LunaSettings.getBoolean("secretsofthefrontier", "sotf_lowFreqPromise");
        }
        return lowFreq;
    }
}