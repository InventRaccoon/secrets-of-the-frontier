package data.scripts.campaign.skills;

import java.awt.Color;

import com.fs.starfarer.api.characters.DescriptionSkillEffect;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.util.Misc;

public class SotfSecretAptitudeDesc {

    public static class Level0 implements DescriptionSkillEffect {
        public String getString() {
            return BaseIntelPlugin.BULLET + "Skills under this aptitude begin hidden\n"
                    +BaseIntelPlugin.BULLET + "They cannot be learned with skill points\n"
                    +BaseIntelPlugin.BULLET + "You will have to find your own way to these paths\n"
                    ;
        }
        public Color[] getHighlightColors() {
            Color h = Misc.getHighlightColor();
            Color s = Misc.getStoryOptionColor();
            return new Color[] {s};
        }
        public String[] getHighlights() {
            return new String[] {"" + Misc.STORY + " point"};
        }
        public Color getTextColor() {
            return Misc.getTextColor();
            //return null;
        }
    }

}
