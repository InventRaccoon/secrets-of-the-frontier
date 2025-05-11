// Ship has Reverie nanite visual flair
package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.util.ColorShifterUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.combat.SotfClingingFlareVisualScript;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SotfVolitionSwarm extends BaseHullMod {

    public static String NANITE_SWARM_EXCHANGE_CLASS = "sotf_nanite_swarm_exchange_class";

    public static Color JITTER_COLOR = new Color(50, 60, 68,255);
    public static Color SMOKE_COLOR = new Color(105,125,205,255);

    public static Color COLOR = new Color(235,245,255,255);
    public static Color COLOR_STRONGER = new Color(215,235,255,255);

    public static class SotfVolitionSwarmListener implements AdvanceableListener {
        public ShipAPI ship;
        IntervalUtil interval = new IntervalUtil(0.075f, 0.125f);
        public SotfVolitionSwarmListener(ShipAPI ship) {
            this.ship = ship;
            // Pulsing white glow from the ship's center
            Global.getCombatEngine().addLayeredRenderingPlugin(
                    new SotfClingingFlareVisualScript(
                            new SotfClingingFlareVisualScript.SotfClingingFlareParams(
                                    ship,
                                    COLOR,
                                    ship.getCollisionRadius() * 7.5f,
                                    ship.getCollisionRadius() * 3f
                            )
                    )
            );
        }

        public void advance(float amount) {
            // anti-mind-control
            if ((ship.getOwner() == 0 || ship.getOwner() == 1) && ship.getOwner() != ship.getOriginalOwner()) {
                ship.setOwner(ship.getOriginalOwner());
            }

            Color c = JITTER_COLOR;
            int alpha = c.getAlpha();
            alpha += 100f;
            if (alpha > 255) alpha = 255;
            c = Misc.setAlpha(c, alpha);

            List<ShipAPI> shipAndModules = ship.getChildModulesCopy();
            shipAndModules.add(ship);

            // ship and weapons are darkened
            for (ShipAPI toJitter : shipAndModules) {
                toJitter.getEngineController().fadeToOtherColor("sotf_nanitesynthesized", COLOR_STRONGER, Misc.setAlpha(COLOR_STRONGER, 25), 1f, 0.9f);
                if (toJitter.getShield() != null) {
                    toJitter.getShield().setInnerColor(Misc.setAlpha(COLOR_STRONGER, 125));
                }
            }

            // nanite fog effect
            interval.advance(amount);
            if (interval.intervalElapsed()) {
                CombatEngineAPI engine = Global.getCombatEngine();

                c = RiftLanceEffect.getColorForDarkening(SMOKE_COLOR);
                c = Misc.setAlpha(c, 35);
                float baseDuration = 2f;
                for (ShipAPI toSmoke : shipAndModules) {
                    Vector2f vel = new Vector2f(toSmoke.getVelocity());
                    float size = 30f;
                    for (int i = 0; i < 4; i++) {
                        Vector2f point = new Vector2f(toSmoke.getLocation());
                        point = Misc.getPointWithinRadiusUniform(point, 15f, Misc.random);
                        float dur = baseDuration + baseDuration * (float) Math.random();
                        float nSize = size;
                        Vector2f pt = Misc.getPointWithinRadius(point, nSize * 0.5f);
                        Vector2f v = Misc.getUnitVectorAtDegreeAngle((float) Math.random() * 360f);
                        v.scale(nSize + nSize * (float) Math.random() * 0.5f);
                        v.scale(0.2f);
                        Vector2f.add(vel, v, v);

                        float maxSpeed = nSize * 1.5f * 0.2f;
                        float minSpeed = nSize * 1f * 0.2f;
                        float overMin = v.length() - minSpeed;
                        if (overMin > 0) {
                            float durMult = 1f - overMin / (maxSpeed - minSpeed);
                            if (durMult < 0.1f) durMult = 0.1f;
                            dur *= 0.5f + 0.5f * durMult;
                        }
                        engine.addNegativeNebulaParticle(pt, v, nSize * 1f, 2f,
                                0.5f / dur, 0f, dur, c);
                    }
                }
            }

            RoilingSwarmEffect swarm = RoilingSwarmEffect.getSwarmFor(ship);
            if (swarm == null) {
                swarm = createSwarmFor(ship);
            }

            RoilingSwarmEffect.RoilingSwarmParams params = swarm.getParams();
            params.baseMembersToMaintain = (int) ship.getMutableStats().getDynamic().getValue(
                    Stats.FRAGMENT_SWARM_SIZE_MOD, getBaseSwarmSize(ship.getHullSize()));
            params.memberRespawnRate = getBaseSwarmRespawnRateMult(ship.getHullSize()) *
                    ship.getMutableStats().getDynamic().getValue(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT);

            params.maxNumMembersToAlwaysRemoveAbove = (int) (params.baseMembersToMaintain * 1.5f);
            params.initialMembers = params.baseMembersToMaintain;
        }

        public static RoilingSwarmEffect createSwarmFor(ShipAPI ship) {
            RoilingSwarmEffect existing = RoilingSwarmEffect.getSwarmFor(ship);
            if (existing != null) return existing;

            RoilingSwarmEffect.RoilingSwarmParams params = new RoilingSwarmEffect.RoilingSwarmParams();
            params.spriteCat = "skills";
            params.spriteKey = "sotf_naniteswarm_sheet";
            params.memberExchangeClass = NANITE_SWARM_EXCHANGE_CLASS;
            params.maxSpeed = ship.getMaxSpeedWithoutBoost() +
                    Math.max(ship.getMaxSpeedWithoutBoost() * 0.25f + 50f, 100f);

            params.springStretchMult = 10f;
            params.baseSpriteSize = 8f;

            params.flashProbability = 0.5f;
            params.flashRateMult = 8f;
            params.flashCoreRadiusMult = 0.5f;
            params.flashRadius = 25f;
            params.flashFringeColor = new Color(0, 175, 255, 40);
            params.flashCoreColor = COLOR_STRONGER;

            // if this is set to true and the swarm is glowing, missile-fragments pop over the glow and it looks bad
            //params.renderFlashOnSameLayer = true;

            params.minOffset = 0f;
            params.maxOffset = Math.min(20f, ship.getCollisionRadius() * 0.75f);
            params.generateOffsetAroundAttachedEntityOval = true;
            params.despawnSound = null; // ship explosion does the job instead
            params.spawnOffsetMult = 0.33f;
            params.spawnOffsetMultForInitialSpawn = 1f;


            params.baseMembersToMaintain = Math.round(getBaseSwarmSize(ship.getHullSize()));
            params.memberRespawnRate = getBaseSwarmRespawnRateMult(ship.getHullSize());

            params.maxNumMembersToAlwaysRemoveAbove = params.baseMembersToMaintain * 2;

            //params.offsetRerollFractionOnMemberRespawn = 0.05f;

            params.initialMembers = 0;
            params.initialMembers = params.baseMembersToMaintain;
            params.removeMembersAboveMaintainLevel = false;

            List<WeaponAPI> glowWeapons = new ArrayList<>();
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.usesAmmo() && w.getSpec().hasTag(Tags.FRAGMENT_GLOW)) {
                    glowWeapons.add(w);
                }
            }

            return new RoilingSwarmEffect(ship, params) {
                protected ColorShifterUtil glowColorShifter = new ColorShifterUtil(new Color(0, 0, 0, 0));

                @Override
                public int getNumMembersToMaintain() {
                    if (ship.isFighter()) {
                        return (int) Math.round(((0.2f + 0.8f * ship.getHullLevel()) * super.getNumMembersToMaintain()));
                    }
                    return super.getNumMembersToMaintain();
                }

                @Override
                public void advance(float amount) {
                    super.advance(amount);

                    glowColorShifter.advance(amount);
                }
            };
        }

        public static int getBaseSwarmSize(HullSize size) {
            return 16;
        }

        public static float getBaseSwarmRespawnRateMult(HullSize size) {
            return 5f;
        }

    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new SotfVolitionSwarmListener(ship));
        ship.setForceHideFFOverlay(true);
    }

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getBreakProb().modifyMult(id, 0f);
    }
}