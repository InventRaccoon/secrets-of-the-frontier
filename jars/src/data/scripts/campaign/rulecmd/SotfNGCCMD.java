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
import com.fs.starfarer.api.impl.SharedUnlockData;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.NGCAddStandardStartingScript;
import com.fs.starfarer.api.impl.campaign.tutorial.CampaignTutorialScript;
import com.fs.starfarer.api.util.DelayedActionScript;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.customstart.SotfChildOfTheLakeCampaignVFX;
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
                        Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_HAUNTED_START, true);
                        MemoryAPI char_mem = Global.getSector().getPlayerPerson().getMemoryWithoutUpdate();
                        char_mem.set(SotfIDs.GUILT_KEY, SotfMisc.getHauntedGuilt());
                        char_mem.set(MemFlags.PLAYER_ATROCITIES, 4f);
                    }
                });
                return true;
            case "cotlUnlocked":
                return SharedUnlockData.get().getSet("sotf_persistent").contains("sotf_haunted_completed") || !SotfMisc.getLockoutStarts();
            case "cotlSmallText":
                dialog.getTextPanel().setFontSmallInsignia();
                dialog.getTextPanel().addParagraph("Gained \"Invoke Her Blessing\"", Misc.getPositiveHighlightColor());
                dialog.getTextPanel().highlightInLastPara(SotfMisc.DAYDREAM_COLOR, "\"Invoke Her Blessing\"");
                dialog.getTextPanel().addParagraph("    - Use on echoes left by destroyed ships to create a mimic that fights for you", SotfMisc.DAYDREAM_COLOR);
                dialog.getTextPanel().addParagraph("    - Choose from upgrades as you level", SotfMisc.DAYDREAM_COLOR);
                dialog.getTextPanel().addParagraph("Learned \"Cult of the Daydream\" ships and weapons blueprints", Misc.getPositiveHighlightColor());
                dialog.getTextPanel().highlightInLastPara(SotfMisc.DAYDREAM_COLOR, "\"Cult of the Daydream\"");
                return true;
            case "cotlStartingScript":
                data.addScriptBeforeTimePass(new Script() {
                    public void run() {
                        Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_COTL_START, true);
                    }
                });
                data.addScript(new Script() {
                    public void run() {
                        Global.getSector().addScript(new DelayedActionScript(0.25f) {
                            @Override
                            public void doAction() {
                                SotfChildOfTheLakeCampaignVFX.fadeIn(1f);
                                Global.getSector().addScript(new DelayedActionScript(1f) {
                                    @Override
                                    public void doAction() {
                                        CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
                                        Misc.showRuleDialog(pf, "sotfCOTLIntro");

                                        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
                                        if (dialog != null) {
                                            dialog.setBackgroundDimAmount(0.4f);
                                        }

                                        SotfChildOfTheLakeCampaignVFX.fadeOut(1f);
                                    }
                                });
                            }
                        });
                    }
                });
                return true;
            default:
                return true;
        }
    }
}
