// just a placeholder line, the actual effect is handled in the reinforcer objective code
package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.FleetStatsSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;

public class SotfAutomatedShipsOfficers {

    public static class SotfLevel1 extends BaseSkillEffectDescription implements FleetStatsSkillEffect {

        public void apply(MutableFleetStatsAPI stats, String id, float level) {

        }

        public void unapply(MutableFleetStatsAPI stats, String id) {

        }

        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);

            if (SotfModPlugin.TACTICAL) {
                info.addPara("Enables AI cores in cargo to remotely captain automated reinforcements from battle objectives",  Misc.getHighlightColor(), 0f);
            }
            if (Global.getSector() != null) {
                if (Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_BEGAN_WITH_NIGHTINGALE)) {
                    info.addPara("When reassigning skills, gain the option to replace any skill with this skill", Misc.getHighlightColor(),0f);
                }
            }

        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.FLEET;
        }
    }
}
