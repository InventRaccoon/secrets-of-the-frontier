package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.MineStrikeStationStats;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.utils.SotfMisc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class SotfDroneStrikeSystem extends MineStrikeStationStats {

    public static float MINE_RANGE_STATION = 3500;

    @Override
    public float getMineRange(ShipAPI ship) {
        //return MINE_RANGE_STATION;
        if (ship == null) return MINE_RANGE_STATION;
        return ship.getMutableStats().getSystemRangeBonus().computeEffective(MINE_RANGE_STATION);
    }

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        //boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }


        float jitterLevel = effectLevel;
        if (state == State.OUT) {
            jitterLevel *= jitterLevel;
        }
        float maxRangeBonus = 25f;
        float jitterRangeBonus = jitterLevel * maxRangeBonus;
        if (state == State.OUT) {
        }

        ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 11, 0f, 3f + jitterRangeBonus);
        ship.setJitter(this, JITTER_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeBonus);

        if (state == State.IN) {
        } else if (effectLevel >= 1) {
            Vector2f target = ship.getMouseTarget();
            if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.SYSTEM_TARGET_COORDS)){
                target = (Vector2f) ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.SYSTEM_TARGET_COORDS);
            }
            if (target != null) {
                float dist = Misc.getDistance(ship.getLocation(), target);
                float max = getMaxRange(ship) + ship.getCollisionRadius();
                if (dist > max) {
                    float dir = Misc.getAngleInDegrees(ship.getLocation(), target);
                    target = Misc.getUnitVectorAtDegreeAngle(dir);
                    target.scale(max);
                    Vector2f.add(target, ship.getLocation(), target);
                }

                target = findClearLocation(ship, target);

                if (target != null) {
                    spawnMine(ship, target);
                    SotfMisc.ewarChargeVisuals(ship);
                }
            }

        } else if (state == State.OUT ) {
        }
    }

    public void spawnMine(ShipAPI source, Vector2f mineLoc) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f currLoc = Misc.getPointAtRadius(mineLoc, 30f + (float) Math.random() * 30f);
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
            currLoc = Misc.getPointAtRadius(mineLoc, 30f + (float) Math.random() * 30f);
        }

        //Vector2f currLoc = mineLoc;
        MissileAPI mine = (MissileAPI) engine.spawnProjectile(source, null,
                "sotf_dronestrikelayer",
                currLoc,
                (float) Math.random() * 360f, null);
        if (source != null) {
            Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
                    source, WeaponAPI.WeaponType.MISSILE, false, mine.getDamage());
//			float extraDamageMult = source.getMutableStats().getMissileWeaponDamageMult().getModifiedValue();
//			mine.getDamage().setMultiplier(mine.getDamage().getMultiplier() * extraDamageMult);
        }


        float fadeInTime = 0.5f;
        mine.getVelocity().scale(0);
        mine.fadeOutThenIn(fadeInTime);

        Global.getCombatEngine().addPlugin(createMissileJitterPlugin(mine, fadeInTime));

        //mine.setFlightTime((float) Math.random());
        float liveTime = LIVE_TIME;
        //liveTime = 0.01f;
        mine.setFlightTime(mine.getMaxFlightTime() - liveTime);

        Global.getSoundPlayer().playSound("mine_teleport", 1f, 1f, mine.getLocation(), mine.getVelocity());
    }

}
