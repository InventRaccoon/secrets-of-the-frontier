// like Vanish but as the object fades out, it mimics the Go Dark ability
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.loading.AbilitySpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;

public class SotfGoDark extends BaseCommandPlugin {

    protected SectorEntityToken entity;

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        entity = dialog.getInteractionTarget();

        String soundId = null;
        AbilitySpecAPI spec = Global.getSettings().getAbilitySpec(Abilities.GO_DARK);
        if (spec != null) {
            soundId = spec.getWorldOn();
        }
        if (soundId != null) {
            Global.getSoundPlayer().playSound(soundId, 1, 1, Global.getSoundPlayer().getListenerPos(), new Vector2f());
        }
        entity.addFloatingText("Going dark", Misc.setAlpha(entity.getIndicatorColor(), 255), 0.5f);
        Misc.fadeAndExpire(entity, 1f);
        return true;
    }
}
