// rulecommands for Annex-Wendigo
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SotfWendigoCMD extends BaseCommandPlugin

{
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap)
    {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        String cmd = null;

        cmd = params.get(0).getString(memoryMap);

        switch (cmd) {
            case "canOfferAFFW":
                return !getStations().isEmpty();
            default:
                return true;
        }
    }

    public List<CampaignFleetAPI> getStations() {
        List<CampaignFleetAPI> stations = new ArrayList<CampaignFleetAPI>();
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (!system.hasTag(Tags.THEME_REMNANT_MAIN)) continue;
            if (!system.hasTag(Tags.THEME_REMNANT_RESURGENT)) continue;

            for (CampaignFleetAPI fleet : system.getFleets()) {
                if (!fleet.isStationMode()) continue;
                if (!Factions.REMNANTS.equals(fleet.getFaction().getId())) continue;
                if (fleet.getMemoryWithoutUpdate().getBoolean("$damagedStation")) continue;
                stations.add(fleet);
            }
        }
        return stations;
    }
}
