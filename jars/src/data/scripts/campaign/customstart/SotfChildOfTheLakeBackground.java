package data.scripts.campaign.customstart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.SharedUnlockData;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.DelayedActionScript;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.utils.SotfMisc;
import exerelin.campaign.backgrounds.BaseCharacterBackground;
import exerelin.utilities.NexFactionConfig;

import static data.scripts.SotfModPlugin.WATCHER;

/**
 *	CHILD OF THE LAKE: Gain an occult technological heirloom who serves you faithfully
 *  May include necromancy
 */

public class SotfChildOfTheLakeBackground extends BaseCharacterBackground {

    private final boolean unlocked;

    public SotfChildOfTheLakeBackground(){
        unlocked = SharedUnlockData.get().getSet("sotf_persistent").contains("sotf_haunted_completed") || !SotfMisc.getLockoutStarts();
    }

    @Override
    public float getOrder() {
        if (!unlocked) return 10000;
        return 101;
    }

    @Override
    public boolean shouldShowInSelection(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        return WATCHER;
        //return WATCHER && SharedUnlockData.get().getSet("sotf_persistent").contains("sotf_haunted_completed");
    }

    @Override
    public boolean canBeSelected(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        return unlocked;
    }

    @Override
    public void canNotBeSelectedReason(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        if (!unlocked) tooltip.addPara("[LOCKED]", Misc.getNegativeHighlightColor(), 0f);
    }

    @Override
    public String getTitle(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        if (!unlocked) return spec.title + " [LOCKED]";
        return spec.title;
    }

    @Override
    public String getShortDescription(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        if (!unlocked) return "Complete \"The Haunted\" to unlock this background.";
        return spec.shortDescription;
    }

    @Override
    public String getLongDescription(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        if (!unlocked) return "An endgame task will be given at level 15 during The Haunted: complete it to unlock this " +
                "background.";
        return spec.longDescription;
    }

    public void onNewGameAfterTimePass(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_COTL_START, true);
        MemoryAPI char_mem = Global.getSector().getPlayerPerson().getMemoryWithoutUpdate();
        //char_mem.set(SotfIDs.GUILT_KEY, SotfMisc.getHauntedGuilt());
        //Global.getSector().addScript(new SotfGenericDialogScript("sotfCOTLIntro"));
        if (Global.getSector().getPlayerPerson().getStats().getPoints() > 0) {
            Global.getSector().getPlayerPerson().getStats().addPoints(-1);
        } else if (!Global.getSector().getPlayerPerson().getStats().getSkillsCopy().isEmpty()) {
            String skill = Global.getSector().getPlayerPerson().getStats().getSkillsCopy().get(0).getSkill().getId();
            Global.getSector().getPlayerPerson().getStats().setSkillLevel(skill, 0);
        }

        //Intro
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

    @Override
    public void addTooltipForSelection(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig, Boolean expanded) {
        super.addTooltipForSelection(tooltip, factionSpec, factionConfig, expanded);
        if (unlocked) {
            tooltip.addSpacer(10f);
            tooltip.addPara("Trade in your starting skill point for the Invoke Her Blessing subsystem. You can use it on " +
                            "an echo left by a destroyed ship in combat to order Sirius, your fell protector, to fashion a mimic in its image and take command of it.", 10f,
                    Misc.getTextColor(), Misc.getHighlightColor(), "Invoke Her Blessing", "echo", "Sirius");
            tooltip.addPara("As you level up, Invoke Her Blessing becomes more powerful and you unlock the ability to " +
                    "select special upgrades for Sirius.", 10f, Misc.getHighlightColor(), "level up", "becomes more powerful", "special upgrades");
            tooltip.addSpacer(10f);
            tooltip.addPara("Also gain access to several advanced blueprints designed by the Cult of the Daydream.", 10f,
                    Misc.getTextColor(), SotfMisc.DAYDREAM_COLOR, "Cult of the Daydream");
        }
    }

    @Override
    public void addTooltipForIntel(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        super.addTooltipForIntel(tooltip, factionSpec, factionConfig);
        float pad = 3f;
        float opad = 10f;

        tooltip.addSectionHeading("Sirius", Alignment.MID, opad);

        tooltip.addPara("See the %s intel entry for detailed information on Sirius' abilities, " +
                "skills and upgrades.", opad, SotfMisc.DAYDREAM_COLOR, "\"Boons of the Daydream\"");

//        PersonAPI sirius = SotfPeople.getPerson(SotfPeople.SIRIUS_MIMIC);
//
//        tooltip.addSectionHeading("Sirius", Alignment.MID, opad);
//        tooltip.addImages(tooltip.getWidthSoFar(), 128, opad, opad, sirius.getPortraitSprite());
//        tooltip.addPara("Sirius' mimics use the skillset below:", opad, Misc.getHighlightColor(), "Sirius");
//        tooltip.addSkillPanelOneColumn(sirius, opad);
    }

}
