package data.scripts.combat.special;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

/**
 *	Force-deploys Dustkeeper flagships
 */

public class SotfDustkeeperFlagDeployPlugin extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI engine;

    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) return;
        if (engine.isSimulation()) return;
        if (engine.isMission()) return;
        if (Global.getCurrentState() == GameState.TITLE) return;
        if (Global.getSector() == null) {return;}
        if (engine.getFleetManager(FleetSide.PLAYER).getGoal().equals(FleetGoal.ESCAPE)) return;
        if (engine.getFleetManager(FleetSide.ENEMY).getGoal().equals(FleetGoal.ESCAPE)) return;

        final List<ShipAPI> deployed = new ArrayList<>();

        for (FleetMemberAPI member : engine.getFleetManager(0).getReservesCopy()) {
            if (member.isAlly() && member.getHullSpec().getBuiltInMods().contains(SotfIDs.HULLMOD_CWARSUITE)) {
                deployed.add(engine.getFleetManager(0).spawnFleetMember(member, new Vector2f(), 90f, 3f));
            }
        }
        moveToSpawnLocations(deployed);
    }

    private static final float MIN_OFFSET = 300f;

    // From Histidine's modification of Console Commands' ForceDeployAll
    public void moveToSpawnLocations(List<ShipAPI> toMove)
    {
        float startingOffset = toMove.size()/2f * MIN_OFFSET;

        final Vector2f spawnLoc = new Vector2f(
                -startingOffset, -engine.getMapHeight() * 0.4f);

        final List<ShipAPI> ships = engine.getShips();
        for (ShipAPI ship : toMove)
        {
            final float radius = ship.getCollisionRadius() + MIN_OFFSET;
            for (int i = 0; i < ships.size(); i++)
            {
                final ShipAPI other = ships.get(i);
                if (MathUtils.isWithinRange(other, spawnLoc, radius))
                {
                    spawnLoc.x += radius;
                    if (spawnLoc.x >= engine.getMapWidth() / 2f)
                    {
                        spawnLoc.x = -engine.getMapWidth();
                        spawnLoc.y -= radius;
                    }

                    // We need to recheck for collisions in our new position
                    i = 0;
                }
            }
            ship.getLocation().set(spawnLoc.x, spawnLoc.y);
            spawnLoc.x += radius;
        }
    }
}