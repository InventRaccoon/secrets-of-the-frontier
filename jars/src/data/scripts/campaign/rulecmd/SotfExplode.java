// this makes the thing the player is interacting with explode and leave a small debris field
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;

public class SotfExplode extends BaseCommandPlugin {

    protected SectorEntityToken entity;

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        entity = dialog.getInteractionTarget();
        Global.getSoundPlayer().playSound("explosion_from_damage", 1, 1, Global.getSoundPlayer().getListenerPos(), new Vector2f());
        Misc.fadeAndExpire(entity, 1f);

        DebrisFieldParams debrisparams = new DebrisFieldParams(150f, 0.5f, 10f, 5f);
        debrisparams.source = DebrisFieldSource.PLAYER_SALVAGE;
        SectorEntityToken debris = Misc.addDebrisField(entity.getContainingLocation(), debrisparams, null);
        debris.setSensorProfile(null);
        debris.setDiscoverable(null);
        debris.setFaction(Factions.NEUTRAL);
        if (entity.getOrbit() != null) {
            debris.setOrbit(entity.getOrbit().makeCopy());
        } else {
            debris.getLocation().set(entity.getLocation());
        }
        return true;
    }
}
