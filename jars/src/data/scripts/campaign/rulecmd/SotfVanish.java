// this makes the thing the player is interacting with fade out of existence
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

import java.util.List;
import java.util.Map;

public class SotfVanish extends BaseCommandPlugin {

    protected SectorEntityToken entity;

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        entity = dialog.getInteractionTarget();
        Misc.fadeAndExpire(entity, 1f);
        return true;
    }
}
