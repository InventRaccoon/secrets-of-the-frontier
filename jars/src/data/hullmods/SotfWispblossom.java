// Ship spawns lesser wisps on death
package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SotfWispblossom extends BaseHullMod

{

    private static final String WISPERED_KEY = "sotf_releasedwisps";

    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship.isHulk() && ship.getCustomData().get(WISPERED_KEY) == null) {
            ship.setCustomData(WISPERED_KEY, true);
            Vector2f to = Misc.getPointAtRadius(ship.getLocation(), ship.getCollisionRadius() + 25f);

            CombatEngineAPI engine = Global.getCombatEngine();
            // 1/4/6/8/10
            int wisps_to_spawn = ship.getHullSize().ordinal();
            if (!ship.isFighter()) {
                wisps_to_spawn *= 2;
            }

            for (int i = 0; i < wisps_to_spawn; i++) {
                CombatFleetManagerAPI fleetManager = engine.getFleetManager(ship.getOriginalOwner());
                boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
                fleetManager.setSuppressDeploymentMessages(true);
                CombatEntityAPI wisp = engine.getFleetManager(ship.getOriginalOwner()).spawnShipOrWing("sotf_lwisp_wing", to, Misc.getAngleInDegrees(ship.getLocation(), to), 0f);
                Global.getSoundPlayer().playSound("mote_attractor_launch_mote", 1f, 1f, wisp.getLocation(), new Vector2f(0,0));
                Global.getCombatEngine().spawnEmpArcVisual(Misc.getPointWithinRadius(ship.getLocation(), 50f), ship, wisp.getLocation(), wisp, 10f, new Color(100,25,155,255), Color.white);
                fleetManager.setSuppressDeploymentMessages(wasSuppressed);
            }
        }
    }
}