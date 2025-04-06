// rulecommands for Sierra stuff
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.RemoveShip;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SotfCourserCMD extends BaseCommandPlugin

{
    protected InteractionDialogAPI dialog;
    protected Map<String, MemoryAPI> memoryMap;

    @Override
    public boolean execute(String ruleId, final InteractionDialogAPI dialog, List<Misc.Token> params, final Map<String, MemoryAPI> memoryMap)
    {
        this.dialog = dialog;
        this.memoryMap = memoryMap;
        final MemoryAPI memory = getEntityMemory(memoryMap);
        final CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        String cmd = null;

        cmd = params.get(0).getString(memoryMap);
        String param = null;
        if (params.size() > 1) {
            param = params.get(1).getString(memoryMap);
        }

        final TextPanelAPI text = dialog.getTextPanel();

        PersonAPI sierra = Global.getSector().getImportantPeople().getPerson(SotfPeople.SIERRA);

        switch (cmd) {
            case "checkCourser":
                boolean courser = false;
                for (CampaignFleetAPI fleet : Global.getSector().getPlayerFleet().getContainingLocation().getFleets()) {
                    if (Misc.getDistance(fleet, Global.getSector().getPlayerFleet()) < 2000 && fleet.getMemoryWithoutUpdate().contains(SotfIDs.MEM_COURSER_FLEET)) {
                        if (fleet.getCommander() != null && fleet.getCommander().getId().equals(SotfPeople.COURSER)) {
                            courser = true;
                            break;
                        }
                    }
                }
                return courser;
            default:
                return true;
        }
    }
}
