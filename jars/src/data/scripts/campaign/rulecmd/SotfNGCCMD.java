// rulecommands for Sierra stuff
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.CharacterCreationData;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.NGCAddStandardStartingScript;
import com.fs.starfarer.api.impl.campaign.tutorial.CampaignTutorialScript;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.customstart.SotfNightingaleNameFixer;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.dialog.SotfGenericDialogScript;
import data.scripts.utils.SotfMisc;

import java.util.List;
import java.util.Map;

public class SotfNGCCMD extends BaseCommandPlugin

{
    protected InteractionDialogAPI dialog;
    protected Map<String, MemoryAPI> memoryMap;

    @Override
    public boolean execute(String ruleId, final InteractionDialogAPI dialog, List<Misc.Token> params, final Map<String, MemoryAPI> memoryMap)
    {
        if (dialog == null) return false;

        CharacterCreationData data = (CharacterCreationData) memoryMap.get(MemKeys.LOCAL).get("$characterData");
        final MemoryAPI memory = memoryMap.get(MemKeys.LOCAL);
        String cmd = null;

        cmd = params.get(0).getString(memoryMap);
        String param = null;
        if (params.size() > 1) {
            param = params.get(1).getString(memoryMap);
        }

        final TextPanelAPI text = dialog.getTextPanel();

        switch (cmd) {
            case "hauntedSmallText":
                dialog.getTextPanel().setFontSmallInsignia();
                dialog.getTextPanel().addParagraph("Gained a reprehensible history of atrocities and the gnawing guilt that accompanies it", Misc.getNegativeHighlightColor());
                dialog.getTextPanel().highlightInLastPara(Misc.getHighlightColor(), "a reprehensible history of atrocities", "gnawing guilt");

                dialog.getTextPanel().addParagraph("Acquired a vengeful and relentless pursuer", Misc.getNegativeHighlightColor());
                dialog.getTextPanel().highlightInLastPara(Misc.getHighlightColor(), "vengeful and relentless pursuer");

                dialog.getTextPanel().addParagraph("Gained a bonus skill point", Misc.getPositiveHighlightColor());
                dialog.getTextPanel().highlightInLastPara(Misc.getHighlightColor(), "bonus skill point");
                dialog.getTextPanel().setFontInsignia();
                return true;
            case "hauntedStartingScript":
                data.addScriptBeforeTimePass(new Script() {
                    public void run() {
                        Global.getSector().getMemoryWithoutUpdate().set("$sotf_hauntedStart", true);
                        MemoryAPI char_mem = Global.getSector().getPlayerPerson().getMemoryWithoutUpdate();
                        char_mem.set(SotfIDs.GUILT_KEY, SotfMisc.getHauntedGuilt());
                        char_mem.set(MemFlags.PLAYER_ATROCITIES, 4f);
                    }
                });
                return true;
            default:
                return true;
        }
    }
}
