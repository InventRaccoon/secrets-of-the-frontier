// play an explosion sound
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;

public class SotfExplosionSound extends BaseCommandPlugin {

    protected SectorEntityToken entity;

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        Global.getSoundPlayer().playSound("explosion_from_damage", 1, 1, Global.getSoundPlayer().getListenerPos(), new Vector2f());
        return true;
    }
}
