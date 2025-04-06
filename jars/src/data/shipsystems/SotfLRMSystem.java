package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.MineStrikeStationStats;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

public class SotfLRMSystem extends MineStrikeStationStats {

    public static float MINE_RANGE_STATION = 4000;

    @Override
    public float getMineRange(ShipAPI ship) {
        //return MINE_RANGE_STATION;
        if (ship == null) return MINE_RANGE_STATION;
        return ship.getMutableStats().getSystemRangeBonus().computeEffective(MINE_RANGE_STATION);
    }

    public void spawnMine(ShipAPI source, Vector2f mineLoc) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f currLoc = Misc.getPointAtRadius(mineLoc, 2000f + (float) Math.random() * 500f);
        //Vector2f currLoc = null;
        float start = (float) Math.random() * 360f;
        for (float angle = start; angle < start + 390; angle += 30f) {
            if (angle != start) {
                Vector2f loc = Misc.getUnitVectorAtDegreeAngle(angle);
                loc.scale(50f + (float) Math.random() * 30f);
                currLoc = Vector2f.add(mineLoc, loc, new Vector2f());
            }
            for (MissileAPI other : Global.getCombatEngine().getMissiles()) {
                if (!other.isMine()) continue;

                float dist = Misc.getDistance(currLoc, other.getLocation());
                if (dist < other.getCollisionRadius() + 40f) {
                    currLoc = null;
                    break;
                }
            }
            if (currLoc != null) {
                break;
            }
        }
        if (currLoc == null) {
            currLoc = Misc.getPointAtRadius(mineLoc, 1200f + (float) Math.random() * 30f);
        }

        Vector2f spawnLoc = Misc.getPointAtRadius(currLoc, 0f + (float) Math.random() * 10f);
        float mineDir = Misc.getAngleInDegrees(spawnLoc, mineLoc);

        MissileAPI shot = (MissileAPI) engine.spawnProjectile(source, null,
                        "pilum",
                        spawnLoc,
                        mineDir, null);

        shot.fadeOutThenIn(1f);

        Global.getSoundPlayer().playSound("pilum_lrm_fire", 1f, 1f, shot.getLocation(), shot.getVelocity());
    }

}
