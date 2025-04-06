package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.MineStrikeStationStats;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
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

                    for (WeaponAPI weapon : ship.getAllWeapons()) {
                        if (!weapon.getSpec().getWeaponId().contains("sotf_dksparker") || !weapon.isDecorative()) continue;
                        final ShipAPI thisShip = ship;
                        final WeaponAPI thisWeapon = weapon;
                        final float slotAngle = weapon.getArcFacing() + ship.getFacing();
                        int baseNumSparksForSlot = 3 + weapon.getSlot().getSlotSize().ordinal();
                        if (weapon.getSlot().getSlotSize().ordinal() == 2) {
                            baseNumSparksForSlot += 2;
                        }
                        final int numSparksForSlot = baseNumSparksForSlot;
                        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                            ShipAPI ship = thisShip;
                            WeaponAPI weapon = thisWeapon;
                            float angle = slotAngle;

                            float timer = 0;
                            float numSparks = numSparksForSlot * 2f;
                            float sparksDone = 0;
                            float overDuration = 0.4f;
                            @Override
                            public void advance(float amount, List<InputEventAPI> events) {
                                timer += amount * Global.getCombatEngine().getTimeMult().getModifiedValue();
                                for (int i = 0; i < numSparks; i++) {
                                    if (i < sparksDone) continue;
                                    if (timer >= ((overDuration / numSparks) * i)) {
                                        Global.getCombatEngine().addHitParticle(MathUtils.getPoint(
                                                weapon.getLocation(), 5f * sparksDone, weapon.getArcFacing() + ship.getFacing()),
                                                ship.getVelocity(),
                                                12f,
                                                7f,
                                                0.2f,
                                                new Color(100,165,255));
                                        sparksDone++;
                                    }
                                }
                                if (sparksDone == numSparks) {
                                    Global.getCombatEngine().removePlugin(this);
                                }
                            }
                        });
                    }
                }
            }

        } else if (state == State.OUT ) {
        }
    }

    private Vector2f findClearLocation(ShipAPI ship, Vector2f dest) {
        if (isLocationClear(dest)) return dest;

        float incr = 50f;

        WeightedRandomPicker<Vector2f> tested = new WeightedRandomPicker<Vector2f>();
        for (float distIndex = 1; distIndex <= 32f; distIndex *= 2f) {
            float start = (float) Math.random() * 360f;
            for (float angle = start; angle < start + 360; angle += 60f) {
                Vector2f loc = Misc.getUnitVectorAtDegreeAngle(angle);
                loc.scale(incr * distIndex);
                Vector2f.add(dest, loc, loc);
                tested.add(loc);
                if (isLocationClear(loc)) {
                    return loc;
                }
            }
        }

        if (tested.isEmpty()) return dest; // shouldn't happen

        return tested.pick();
    }

    private boolean isLocationClear(Vector2f loc) {
        for (ShipAPI other : Global.getCombatEngine().getShips()) {
            if (other.isShuttlePod()) continue;
            if (other.isFighter()) continue;

//			Vector2f otherLoc = other.getLocation();
//			float otherR = other.getCollisionRadius();

//			if (other.isPiece()) {
//				System.out.println("ewfewfewfwe");
//			}
            Vector2f otherLoc = other.getShieldCenterEvenIfNoShield();
            float otherR = other.getShieldRadiusEvenIfNoShield();
            if (other.isPiece()) {
                otherLoc = other.getLocation();
                otherR = other.getCollisionRadius();
            }


//			float dist = Misc.getDistance(loc, other.getLocation());
//			float r = other.getCollisionRadius();
            float dist = Misc.getDistance(loc, otherLoc);
            float r = otherR;
            //r = Math.min(r, Misc.getTargetingRadius(loc, other, false) + r * 0.25f);
            float checkDist = MIN_SPAWN_DIST;
            if (other.isFrigate()) checkDist = MIN_SPAWN_DIST_FRIGATE;
            if (dist < r + checkDist) {
                return false;
            }
        }
        for (CombatEntityAPI other : Global.getCombatEngine().getAsteroids()) {
            float dist = Misc.getDistance(loc, other.getLocation());
            if (dist < other.getCollisionRadius() + MIN_SPAWN_DIST) {
                return false;
            }
        }

        return true;
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
