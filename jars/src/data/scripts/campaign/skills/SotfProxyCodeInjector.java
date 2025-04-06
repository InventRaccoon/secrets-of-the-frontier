// just a placeholder line, the actual effect is handled in the reinforcer objective code
package data.scripts.campaign.skills;

import com.fs.starfarer.api.characters.FleetStatsSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class SotfProxyCodeInjector {

    public static class ProxyReinforcements extends BaseSkillEffectDescription implements FleetStatsSkillEffect {

        public void apply(MutableFleetStatsAPI stats, String id, float level) {

        }

        public void unapply(MutableFleetStatsAPI stats, String id) {

        }

        public String getEffectDescription(float level) {
            return null;
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);

            LabelAPI label = info.addPara("Injects an alternate broadcast key into captured Hyperwave Transmitter objectives, replacing " +
                            "Explorarium reinforcements with more powerful Dustkeeper Proxy drones",
                    0f, hc, hc);
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.FLEET;
        }
    }
}
