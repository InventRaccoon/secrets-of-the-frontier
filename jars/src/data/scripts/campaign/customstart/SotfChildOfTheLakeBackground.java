package data.scripts.campaign.customstart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.dialog.SotfGenericDialogScript;
import data.scripts.utils.SotfMisc;
import exerelin.campaign.backgrounds.BaseCharacterBackground;
import exerelin.utilities.NexFactionConfig;

import static data.scripts.SotfModPlugin.WATCHER;

/**
 *	CHILD OF THE LAKE: You're haunted by a spectre of your mysterious past.
 *  ... and protected by another.
 */

public class SotfChildOfTheLakeBackground extends BaseCharacterBackground {

    @Override
    public float getOrder() {
        return 100;
    }

    @Override
    public boolean shouldShowInSelection(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        return WATCHER;
    }

    public void onNewGameAfterTimePass(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_COTL_START, true);
        MemoryAPI char_mem = Global.getSector().getPlayerPerson().getMemoryWithoutUpdate();
        //char_mem.set(SotfIDs.GUILT_KEY, SotfMisc.getHauntedGuilt());
        //Global.getSector().addScript(new SotfGenericDialogScript("sotfCOTLIntro"));
        Global.getSector().getPlayerPerson().getStats().addPoints(-1);
    }

    @Override
    public void addTooltipForSelection(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig, Boolean expanded) {
        super.addTooltipForSelection(tooltip, factionSpec, factionConfig, expanded);

        tooltip.addSpacer(10f);
        tooltip.addPara("Trade in your starting skill point for the Invoke Her Blessing subsystem. You can use it on " +
                "an echo left by a destroyed ship in combat to order Sirius, your fell protector, to fashion a mimic in its image and take command of it.", 10f,
                Misc.getTextColor(), Misc.getHighlightColor(), "Invoke Her Blessing", "echo", "Sirius");
        tooltip.addPara("As you level up, Invoke Her Blessing becomes more powerful and you unlock the ability to " +
                "select special upgrades for Sirius.", 10f, Misc.getHighlightColor(), "level up", "becomes more powerful", "special upgrades");
    }

    @Override
    public void addTooltipForIntel(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        super.addTooltipForIntel(tooltip, factionSpec, factionConfig);
        float pad = 3f;
        float opad = 10f;

        tooltip.addSectionHeading("Sirius", Alignment.MID, opad);

        tooltip.addPara("See the %s intel entry for detailed information on Sirius' abilities, " +
                "skills and upgrades.", opad, Misc.getHighlightColor(), "\"Boons of the Lake\"");

//        PersonAPI sirius = SotfPeople.getPerson(SotfPeople.SIRIUS_MIMIC);
//
//        tooltip.addSectionHeading("Sirius", Alignment.MID, opad);
//        tooltip.addImages(tooltip.getWidthSoFar(), 128, opad, opad, sirius.getPortraitSprite());
//        tooltip.addPara("Sirius' mimics use the skillset below:", opad, Misc.getHighlightColor(), "Sirius");
//        tooltip.addSkillPanelOneColumn(sirius, opad);
    }

}
