// rulecommands for Infix-Wendigo
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.missions.dkcontact.SotfDKContactIntel;
import data.scripts.campaign.plugins.wendigo.SotfWendigoEncounterManager;

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

        PersonAPI wendigo = SotfPeople.getPerson(SotfPeople.WENDIGO);
        MarketAPI market;

        switch (cmd) {
            case "canOfferAFFW":
                return !getStations().isEmpty();
            case "addContact":
                market = dialog.getInteractionTarget().getMarket();
                SotfDKContactIntel intel = new SotfDKContactIntel(wendigo, market);
                Global.getSector().getIntelManager().addIntel(intel, false, dialog.getTextPanel());
                Global.getSector().getMemoryWithoutUpdate().set("$sotf_gotWendigoContact", true);

                market.getCommDirectory().addPerson(wendigo);
                return true;
            case "sendHatredHunter":
                market = dialog.getInteractionTarget().getMarket();
                for (IntelInfoPlugin contact : Global.getSector().getIntelManager().getIntel(SotfDKContactIntel.class)) {
                    SotfDKContactIntel contactIntel = (SotfDKContactIntel) contact;
                    if (contactIntel.getPerson().getId().equals(SotfPeople.WENDIGO)) {
                        contactIntel.loseContact(dialog);
                    }
                }
                market.getCommDirectory().removePerson(wendigo);

                SotfWendigoEncounterManager.sendWendigoHatredHunt();
                return true;
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
