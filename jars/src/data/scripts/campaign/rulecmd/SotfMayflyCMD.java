// rulecommands for Mayfly encounter in Askonia
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.missions.goodhunting.MissionDefinition;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.plugins.mayfly.SotfAskoniaProbeExplosionPlugin;

import java.util.List;
import java.util.Map;

public class SotfMayflyCMD extends BaseCommandPlugin

{
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap)
    {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        String cmd = null;

        cmd = params.get(0).getString(memoryMap);
        String param = null;
        if (params.size() > 1) {
            param = params.get(1).getString(memoryMap);
        }

        TextPanelAPI text = dialog.getTextPanel();

        PersonAPI mayfly = Global.getSector().getImportantPeople().getPerson(SotfPeople.MAYFLY);

        switch (cmd) {
            case "revealMayfly":
                mayfly.getName().setFirst(SotfPeople.MAYFLY_FULL);
                dialog.getInteractionTarget().setCustomDescriptionId("sotf_askoniaprobe1");
                return true;
            case "kaboom":
                Global.getSector().getMemoryWithoutUpdate().set("$sotf_askoniaProbeDestroyed", true);
                dialog.getInteractionTarget().getContainingLocation().addScript(new SotfAskoniaProbeExplosionPlugin(dialog.getInteractionTarget()));
                return true;
            default:
                return true;
        }
    }
}
