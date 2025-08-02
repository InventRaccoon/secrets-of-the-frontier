package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceAbyssPluginImpl;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;

import java.util.List;
import java.util.Map;

public class SotfAllowHabitatBuilding extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap)
    {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        LocationAPI location = playerFleet.getContainingLocation();
        if (!(location instanceof StarSystemAPI system)) {
            return false;
        }
        if (system.getEntitiesWithTag(Tags.JUMP_POINT).isEmpty()) {
            return false;
        }
        if (system.hasTag(Tags.SYSTEM_ABYSSAL) || system.hasTag(Tags.THEME_HIDDEN) || system.hasTag(Tags.TEMPORARY_LOCATION) ||
                system.getType().equals(StarSystemGenerator.StarSystemType.DEEP_SPACE)) {
            return false;
        }
        return true;
    }
}
