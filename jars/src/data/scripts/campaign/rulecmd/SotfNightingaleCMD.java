// rulecommands for Lost Thread/Nightingale stuff
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddShip;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.ShowDefaultVisual;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageSpecialInteraction;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SotfNightingaleCMD extends BaseCommandPlugin

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
        CampaignFleetAPI inkyFleet = Global.getSector().getMemoryWithoutUpdate().getFleet(SotfIDs.MEM_NIGHTINGALE_FLEET);
        String cmd = null;

        cmd = params.get(0).getString(memoryMap);
        String param = null;
        if (params.size() > 1) {
            param = params.get(1).getString(memoryMap);
        }

        final TextPanelAPI text = dialog.getTextPanel();

        PersonAPI inky = Global.getSector().getImportantPeople().getPerson(SotfPeople.NIGHTINGALE);

        switch (cmd) {
            case "checkLostThreadMissionStart":
                return inkyFleet != null;
            case "hasAutomated":
                return Misc.getAllowedRecoveryTags().contains(Tags.AUTOMATED_RECOVERABLE) || SotfMisc.playerHasNoAutoPenaltyShip();
            case "chatterSemiVerbal":
                inky.getMemoryWithoutUpdate().set("$chatterChar", "sotf_nightingale_2");
                return true;
            case "recoverInky":
                if (inkyFleet == null) return false;
                FleetMemberAPI inkyMember = Global.getSector().getMemoryWithoutUpdate().getFleet(SotfIDs.MEM_NIGHTINGALE_FLEET).getFlagship();
                List<FleetMemberAPI> pool = new ArrayList<FleetMemberAPI>();
                pool.add(inkyMember);

                dialog.showFleetMemberRecoveryDialog("Select ships to recover", pool, new ArrayList<FleetMemberAPI>(),
                        new FleetMemberPickerListener() {
                            public void pickedFleetMembers(List<FleetMemberAPI> selected) {
                                if (selected.isEmpty()) return;

                                for (FleetMemberAPI member : selected) {
                                    member.getRepairTracker().setSuspendRepairs(false);

                                    CampaignFleetAPI inkyFleet = Global.getSector().getMemoryWithoutUpdate().getFleet(SotfIDs.MEM_NIGHTINGALE_FLEET);
                                    if (inkyFleet != null && inkyFleet.getFleetData().getMembersListCopy().contains(member)) {
                                        inkyFleet.getFleetData().removeFleetMember(member);
                                    }
                                    playerFleet.getFleetData().addFleetMember(member);
                                    member.setCaptain(SotfPeople.getPerson(SotfPeople.NIGHTINGALE));
                                    AddShip.addShipGainText(member, text);
                                }

                                FireBest.fire(null, dialog, memoryMap, "sotfLTinkyRecovered");
                                Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_NIGHTINGALE_FLEET, null);
                            }
                            public void cancelledFleetMemberPicking() {
                                FireBest.fire(null, dialog, memoryMap, "sotfLTinkyCancel");
                            }
                        });
                return true;
            default:
                return true;
        }
    }
}
