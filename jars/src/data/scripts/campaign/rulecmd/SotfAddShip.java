// adds a ship to the player's fleet, with appropriate text
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class SotfAddShip extends BaseCommandPlugin

{
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap)
    {
        final MemoryAPI localMemory = memoryMap.get(MemKeys.LOCAL);
        if (localMemory == null) return false;
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        String id = params.get(0).string;

        FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, id);
        playerFleet.getFleetData().addFleetMember(member);
        AddRemoveCommodity.addFleetMemberGainText(member, dialog.getTextPanel());

        return true;
    }
}
