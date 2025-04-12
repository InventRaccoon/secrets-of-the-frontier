// rulecommands for Sierra stuff
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.RemoveShip;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.customstart.SotfHauntedDreamCampaignVFX;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SotfGuiltCMD extends BaseCommandPlugin

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

        switch (cmd) {
            case "addGuilt":
                SotfMisc.addGuilt(1f);
                return true;
            case "addHalfGuilt":
                SotfMisc.addGuilt(0.5f);
                return true;
            case "milestoneGuilt":
                SotfMisc.addGuilt(3.5f);
                return true;
            case "removeHalfGuilt":
                SotfMisc.addGuilt(-0.5f);
                return true;
            case "removeGuilt":
                SotfMisc.addGuilt(-1f);
                return true;
            case "remove2Guilt":
                SotfMisc.addGuilt(-2f);
                return true;
            case "kickIdol":
                SotfMisc.addGuilt(3f);
                return true;
            case "kickIdolH":
                SotfMisc.addGuilt(7f);
                return true;
            case "showFelPersonVisual":
                dialog.getInteractionTarget().getMemoryWithoutUpdate().set(FleetInteractionDialogPluginImpl.DO_NOT_AUTO_SHOW_FC_PORTRAIT, true, 0f);
                dialog.getVisualPanel().showPersonInfo(SotfPeople.getPerson(SotfPeople.FEL), false, false);
                dialog.getVisualPanel().hideRankNamePost();
                return true;
            case "fadeOutFelVFX":
                SotfHauntedDreamCampaignVFX.fadeOutFromCurrent(1f);
                return true;
            default:
                return true;
        }
    }
}
