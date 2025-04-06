// FURY OF THE FALLEN. On destruction, ship enters an undying frenzy, then hurls itself at an enemy and detonates
package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.impl.combat.LowCRShipDamageSequence;
import com.fs.starfarer.api.impl.combat.ShipExplosionFlareVisual;
import com.fs.starfarer.api.impl.combat.ShockwaveVisual;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class SotfElegyOfOpis {

    public static float ANATHEMA_HEAL_PERCENT = 0.45f;
    public static float ANATHEMA_DECAY_PERCENT = 0.0075f;

    public static Color ANATHEMA_BRIGHT = new Color(215, 85, 55, 155);
    public static Color ANATHEMA_DARK = new Color(45, 25, 0, 255);

    public static Map<ShipAPI.HullSize, Float> SPEED_BONUS = new HashMap<ShipAPI.HullSize, Float>();
    static {
        SPEED_BONUS.put(ShipAPI.HullSize.FIGHTER, 50f); // just in case
        SPEED_BONUS.put(ShipAPI.HullSize.FRIGATE, 50f);
        SPEED_BONUS.put(ShipAPI.HullSize.DESTROYER, 30f);
        SPEED_BONUS.put(ShipAPI.HullSize.CRUISER, 20f);
        SPEED_BONUS.put(ShipAPI.HullSize.CAPITAL_SHIP, 20f); // works out to 10f since halved
    }
    private static final float FLUX_DISSIPATION_MULT = 2f;
    private static final float RANGE_THRESHOLD = 450f;
    private static final float RANGE_MULT = 0.25f;

    // ship has already triggered Anathema - so that Spite knows it can trigger now
    private static final String ANATHEMA_TRIGGERED_KEY = "sotf_triggeredAnathema";

    public static Map<ShipAPI.HullSize, Float> SPITE_DAMAGE = new HashMap<ShipAPI.HullSize, Float>();
    static {
        SPITE_DAMAGE.put(ShipAPI.HullSize.FIGHTER, 2000f); // just in case
        SPITE_DAMAGE.put(ShipAPI.HullSize.FRIGATE, 4000f);
        SPITE_DAMAGE.put(ShipAPI.HullSize.DESTROYER, 5000f);
        SPITE_DAMAGE.put(ShipAPI.HullSize.CRUISER, 6000f);
        SPITE_DAMAGE.put(ShipAPI.HullSize.CAPITAL_SHIP, 8000f);
    }

    public static class Anathema extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            ship.addListener(new SotfAnathemaUndyingScript(ship));
        }

        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            // do not remove if undying nanites have been triggered
            if (ship.getCustomData().containsKey(ANATHEMA_TRIGGERED_KEY)) return;
            ship.removeListenerOfClass(SotfAnathemaUndyingScript.class);
        }

        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            //stats.getBreakProb().modifyMult(id, 0f);
        }
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            //stats.getBreakProb().unmodify(id);
        }

        public String getEffectDescription(float level) {
            return null;
        }

        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);

            Color c = hc;
            info.addPara("Once per battle, when first reduced to zero hull points, piloted ship triggers a parasitic nanite swarm which " +
                            "instantly restores %s of maximum hull, cancels any ongoing overload and repairs all weapons and engines",
                    0f, c, c, "" + (int) (ANATHEMA_HEAL_PERCENT * 100f) + "%");
            info.addPara("Active nanites consume %s of max hull per second and override the ship's safeties (capital ships receive half the override bonuses of a cruiser)",
                    0f, c, c, "" + ANATHEMA_DECAY_PERCENT * 100f + "%");
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }

    // Instead of dying, regain a large amount of decaying hull, activate Safety Overrides and charge
    public static class SotfAnathemaUndyingScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
        public ShipAPI ship;
        public boolean undying = false;
        public float progress = 0f;

        public SotfAnathemaUndyingScript(ShipAPI ship) {
            this.ship = ship;
        }

        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if (!undying) {
                float hull = ship.getHitpoints();
                if (damageAmount >= hull) {
                    // heal up
                    ship.setHitpoints(ship.getMaxHitpoints() * ANATHEMA_HEAL_PERCENT);
                    // stop overloading
                    if (ship.getFluxTracker().isOverloaded()) {
                        ship.getFluxTracker().setOverloadDuration(0.25f);
                    }
                    ship.getFluxTracker().setCurrFlux(ship.getCurrFlux() * 0.5f);
                    // repair weapons and engines
                    for (WeaponAPI weapon : ship.getAllWeapons()) {
                        weapon.repair();
                    }
                    for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                        engine.setHitpoints(engine.getMaxHitpoints());
                    }

                    undying = true;
                    Global.getSoundPlayer().playSound("sotf_lastlaughprimed", 1f, 1f, ship.getLocation(), ship.getVelocity());
                    return true;
                }
            }
            // immune to hull damage for 0.25 seconds after triggering nanites (unless it gets hit by a torp or something)
            else if (progress < 0.25f && !(damageAmount >= 1000f)) {
                return true;
            }
            return false;
        }

        public void advance(float amount) {
            String id = "sotf_anathema_modifier";
            if (undying && ship.isAlive()) {
                float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
                int brighten = 120;
                Color ventColor = Misc.setBrightness(ship.getVentFringeColor(), 100);
                if (progress == 0f) {
                    if (ship.getFluxTracker().showFloaty()) {
                        Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(),
                                "Elegy of Opis: Undying!",
                                NeuralLinkScript.getFloatySize(ship) + 15f,
                                ANATHEMA_BRIGHT,
                                ship,
                                16f * timeMult,
                                1f/timeMult,
                                1f/timeMult,
                                0.5f,
                                0.5f,
                                1f);
                    }

                    ship.setCustomData(ANATHEMA_TRIGGERED_KEY, true);

                    ship.setHeavyDHullOverlay();

                    // assign a new ship AI. Maximum aggression, do NOT back down!
                    if (ship != Global.getCombatEngine().getPlayerShip()) {
                        ShipAIConfig config = new ShipAIConfig();
                        config.alwaysStrafeOffensively = true;
                        config.backingOffWhileNotVentingAllowed = false;
                        config.turnToFaceWithUndamagedArmor = false;
                        config.burnDriveIgnoreEnemies = true;
                        config.personalityOverride = Personalities.RECKLESS;

                        ship.setShipAI(Global.getSettings().createDefaultShipAI(ship, config));
                    }
                }

                ship.fadeToColor(id, Color.BLACK, 0.1f, 0.1f, Math.min(progress * 2f, 0.9f));
                ship.setJitter(id, ANATHEMA_DARK, Math.min(progress * 2f, 0.25f), 15, 10, 12f);
                ship.setCircularJitter(true);

                // apply Safety Overrides effect
                float sizeMult = 1f;
                if (ship.isCapital()) sizeMult = 0.5f;
                if (!ship.getVariant().hasHullMod(HullMods.SAFETYOVERRIDES)) {
                    ship.getMutableStats().getMaxSpeed().modifyFlat(HullMods.SAFETYOVERRIDES, (Float) SPEED_BONUS.get(ship.getHullSize()) * sizeMult);
                    ship.getMutableStats().getAcceleration().modifyFlat(HullMods.SAFETYOVERRIDES, (Float) SPEED_BONUS.get(ship.getHullSize()) * sizeMult);
                    ship.getMutableStats().getDeceleration().modifyFlat(HullMods.SAFETYOVERRIDES, (Float) SPEED_BONUS.get(ship.getHullSize()) * sizeMult);
                    ship.getMutableStats().getZeroFluxMinimumFluxLevel().modifyFlat(HullMods.SAFETYOVERRIDES, 2f * sizeMult); // set to two, meaning boost is always on

                    ship.getMutableStats().getFluxDissipation().modifyMult(HullMods.SAFETYOVERRIDES, ((FLUX_DISSIPATION_MULT - 1f) * sizeMult) + 1f);
                    // capitals can still vent at half the speed
                    ship.getMutableStats().getVentRateMult().modifyMult(HullMods.SAFETYOVERRIDES, 1f - sizeMult);
                    // capitals: threshold doubled, penalty halved
                    ship.getMutableStats().getWeaponRangeThreshold().modifyFlat(HullMods.SAFETYOVERRIDES, RANGE_THRESHOLD / sizeMult);
                    ship.getMutableStats().getWeaponRangeMultPastThreshold().modifyMult(HullMods.SAFETYOVERRIDES, RANGE_MULT / sizeMult);

                    // engine color change
                    float visualProgress = Math.min(progress, 1f) * sizeMult;
                    ship.getEngineController().fadeToOtherColor(this, new Color(255,100,255,255), null, visualProgress, 0.4f);
                    ship.getEngineController().extendFlame(this, 0.25f * visualProgress, 0.25f * visualProgress, 0.25f * visualProgress);
                }

                float newHitPoints = ship.getHitpoints() - (ship.getMaxHitpoints() * ANATHEMA_DECAY_PERCENT * amount);
                if (newHitPoints > 0f) {
                    ship.setHitpoints(ship.getHitpoints() - (ship.getMaxHitpoints() * ANATHEMA_DECAY_PERCENT * amount));
                } else {
                    LowCRShipDamageSequence malfunctions = new LowCRShipDamageSequence(ship, 100f);
                    // 1 to kill, next are cosmetic
                    for (int i = 0; i < 10; i++) {
                        malfunctions.disableNext();
                        ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0f);
                    }
                }

                if (Global.getCombatEngine().getPlayerShip().equals(ship)) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(id, "graphics/icons/status/sotf_elegyofopis_anathema.png", "Elegy of Opis", "Nanites active - make them pay", true);
                }

                progress += amount;
            }
        }
    }

    public static class Spite extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            if (ship.isFighter()) return;
            if (ship.getHullSpec().getHullId().contains("higgs")) return;
            ship.addListener(new SotfSpiteExplosion(ship));
        }

        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            if (ship.isFighter()) return;
            if (ship.getHullSpec().getHullId().contains("higgs")) return;
            // do not remove if undying nanites have been triggered
            if (ship.getCustomData().containsKey(ANATHEMA_TRIGGERED_KEY)) return;
            ship.removeListenerOfClass(SotfSpiteExplosion.class);
        }

        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            stats.getBreakProb().modifyMult(id, 0f);
        }
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getBreakProb().unmodify(id);
        }

        public String getEffectDescription(float level) {
            return null;
        }

        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);

            WeaponSpecAPI lastLaughWpn = Global.getSettings().getWeaponSpec("sotf_spite");

            Color c = hc;
            float level = stats.getSkillLevel(skill.getId());
            if (level < 2) {
                c = dhc;
            }
            info.addPara("Upon being disabled, piloted ship overloads its reactor and then violently hurls itself at its "  +
                            "killer in a final act of vengeance, detonating brilliantly for %s/%s/%s/%s %s damage " +
                            "based on its hull size to all nearby enemies",
                    0f, c, c,
                    "" + SPITE_DAMAGE.get(ShipAPI.HullSize.FRIGATE).intValue(),
                    "" + SPITE_DAMAGE.get(ShipAPI.HullSize.DESTROYER).intValue(),
                    "" + SPITE_DAMAGE.get(ShipAPI.HullSize.CRUISER).intValue(),
                    "" + SPITE_DAMAGE.get(ShipAPI.HullSize.CAPITAL_SHIP).intValue(),
                    lastLaughWpn.getDamageType().getDisplayName().toLowerCase());

        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }

    // Instead of dying normally, meltdown, hurl self at killer and then detonate
    public static class SotfSpiteExplosion implements AdvanceableListener, DamageListener {
        public ShipAPI ship;
        // ship that killed this one, if any
        public ShipAPI victim = null;
        // hidden mine used for the explosion
        public MissileAPI mine;
        public boolean exploding = false;
        public float explodeProgress = 0f;
        public boolean hurled = false;
        public float timeUntilNextArc = 0.5f;

        public SotfSpiteExplosion(ShipAPI ship) {
            this.ship = ship;
        }

        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (!ship.isAlive()) {
                return;
            }
            if (source == null) {
                return;
            }
            if (source instanceof ShipAPI) {
                victim = (ShipAPI) source;
            }
//            if (source instanceof DamagingProjectileAPI) {
//                DamagingProjectileAPI proj = (DamagingProjectileAPI) source;
//                if (proj.getSource() != null) {
//                    victim = proj.getSource();
//                }
//            } else if (source instanceof BeamAPI) {
//                BeamAPI beam = (BeamAPI) source;
//                if (beam.getSource() != null) {
//                    victim = beam.getSource();
//                }
//            }
            if (victim == null) {
                return;
            }
            if (victim.getWing() != null) {
                // could be non-carrier wing like e.g Anamnesis drones
                if (victim.getWing().getSourceShip() != null) {
                    victim = victim.getWing().getSourceShip();
                }
            }
        }

        public void advance(float amount) {
            if (ship == null) return;
            String id = "sotf_spite_modifier";
            if (!exploding && ship.isHulk()) {
                exploding = true;
                Global.getSoundPlayer().playSound("sotf_lastlaughprimed", 1f, 1f, ship.getLocation(), ship.getVelocity());
            }
            if (exploding) {
                // on getting destroyed
                float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
                int brighten = 120;
                //Color ventColor = ship.getVentFringeColor().brighter();
                Color ventColor = ANATHEMA_BRIGHT.brighter();
                if (explodeProgress == 0f) {
                    Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(),
                                "Critical reactor failure!",
                                NeuralLinkScript.getFloatySize(ship) * 2f,
                                ventColor,
                                ship,
                                16f * timeMult,
                                1f/timeMult,
                                1f/timeMult,
                            0.5f,
                                0.5f,
                                1f);
                    mine = (MissileAPI) Global.getCombatEngine().spawnProjectile(ship, null,
                            "sotf_spite",
                            ship.getLocation(),
                            0f, null);

                    mine.setDamageAmount(SPITE_DAMAGE.get(ship.getHullSize()));

                    // data flags for the AI
                    mine.setMinePrimed(true);
                    mine.setUntilMineExplosion(0.1f);
                    mine.setMineExplosionRange(3000f);

                    // so it can't be destroyed/disarmed
                    mine.setHitpoints(9999f);
                    mine.setEmpResistance(9999);
                    if (victim != null) {
                        for (int i = 0; i < 2; i++) {
                            Global.getCombatEngine().spawnEmpArcVisual(
                                    Misc.getPointWithinRadius(ship.getLocation(), ship.getCollisionRadius() * 0.25f),
                                    ship, Misc.getPointWithinRadius(victim.getLocation(), victim.getCollisionRadius() * 0.25f),
                                    victim,
                                    20f,
                                    ventColor,
                                    Color.WHITE);
                        }
                    }
                }

                // HAHAHAHAHAHAHAHAHA
                if (Global.getCombatEngine().getPlayerShip().equals(victim)) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(id, "graphics/icons/hullsys/entropy_amplifier.png", "Elegy of Opis", "Two Are Taken", true);
                } else if (Global.getCombatEngine().getPlayerShip().equals(ship)) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(id, "graphics/icons/hullsys/entropy_amplifier.png", "Elegy of Opis", "One last laugh", false);
                }

                // sync mine with ship. Need to sync velocity so the AI understands the mine is approaching them
                if (mine != null) {
                    mine.getLocation().set(ship.getLocation().x, ship.getLocation().y);
                    mine.getVelocity().set(ship.getVelocity().x, ship.getVelocity().y);
                }

                timeUntilNextArc -= amount;
                if (timeUntilNextArc <= 0) {
                    float thickness = 40f;
                    Vector2f to = Misc.getPointAtRadius(ship.getLocation(), 200f + (50f * (float) Math.random()));
                    EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(
                            ship.getShieldCenterEvenIfNoShield(),
                            ship,
                            to,
                            ship,
                            thickness,
                            ANATHEMA_BRIGHT,
                            Color.white
                    );
                    arc.setSingleFlickerMode();

                    float randomFactor = (float) Math.random();
                    timeUntilNextArc = 0.3f + (0.15f * randomFactor);
                    Global.getCombatEngine().addHitParticle(ship.getLocation(), ship.getVelocity(), 60f + (30f * randomFactor), 1f, 0.35f, ANATHEMA_BRIGHT.brighter());
                }

                ship.setJitter(this, ventColor, explodeProgress / 2f, 10, 2f);

                //ship.setControlsLocked(true);
                //ship.getFluxTracker().beginOverloadWithTotalBaseDuration(10f);
                //ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
                //ship.setHoldFireOneFrame(true);
                //if (!hurled) {
                //    ship.giveCommand(ShipCommand.DECELERATE, null, 0);
                //}
                //ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
                //ship.getMutableStats().getVentRateMult().modifyMult(id, 0.01f);
                //ship.getMutableStats().getFluxDissipation().modifyMult(id, 0f);
                //ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0f);

                explodeProgress += amount;

                // YEET!
                if (explodeProgress >= 2 && !hurled && victim != null) {
                    CombatUtils.applyForce(ship, Misc.getAngleInDegrees(ship.getLocation(), victim.getLocation()), 25f * ship.getMassWithModules());
                    //Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(),
                    //        "THE LAST LAUGH!",
                    //        NeuralLinkScript.getFloatySize(ship) * 2f, ventColor, ship, 32f * timeMult, 1.6f/timeMult, 0.5f/timeMult, 0f, 0f,
                    //        1f);
                    Global.getSoundPlayer().playSound("mote_attractor_targeted_ship", 1.5f, 1f, ship.getLocation(), ship.getVelocity());

                    Global.getCombatEngine().addLayeredRenderingPlugin(
                            new ShipExplosionFlareVisual(
                                    new ShipExplosionFlareVisual.ShipExplosionFlareParams(
                                            ship,
                                            Color.WHITE,
                                            ship.getShieldRadiusEvenIfNoShield(),
                                            ship.getShieldRadiusEvenIfNoShield()
                                    )
                            )
                    );

                    hurled = true;
                }

                boolean shouldExplode = false;
                if (explodeProgress >= 4f) {
                    shouldExplode = true;
                }
                if (victim != null) {
                    if (Misc.getDistance(ship.getLocation(), victim.getLocation()) <= (ship.getCollisionRadius() + victim.getShieldRadiusEvenIfNoShield())) {
                        shouldExplode = true;
                    }
                }

                // KAAAABBBBLOOOOOEEEEEEEEEEY!!!
                if (shouldExplode) {
                    if (mine != null) {
                        Global.getCombatEngine().spawnExplosion(ship.getLocation(), new Vector2f(0f,0f), ventColor, ship.getCollisionRadius() * 5f, 5f);

                        DamagingExplosionSpec explosionSpec = DamagingExplosionSpec.explosionSpecForShip(ship);
                        explosionSpec.setParticleCount(200);

                        explosionSpec.setMinDamage(SPITE_DAMAGE.get(ship.getHullSize()) * 0.5f);
                        explosionSpec.setMaxDamage(SPITE_DAMAGE.get(ship.getHullSize()));
                        explosionSpec.setCoreRadius(ship.getCollisionRadius() * 3f);
                        explosionSpec.setRadius(ship.getCollisionRadius() * 4f);
                        explosionSpec.setCollisionClass(CollisionClass.MISSILE_FF);
                        explosionSpec.setCollisionClassByFighter(CollisionClass.MISSILE_FF);
                        explosionSpec.setDamageType(mine.getDamageType());

                        explosionSpec.setParticleDuration(2f);
                        explosionSpec.setParticleCount(125);
                        explosionSpec.setParticleColor(Misc.scaleColorSaturate(ventColor.brighter(), 0.65f));

                        explosionSpec.setUseDetailedExplosion(true);
                        explosionSpec.setExplosionColor(Misc.scaleColorSaturate(ventColor, 0.65f));
                        explosionSpec.setDetailedExplosionRadius(ship.getCollisionRadius() * 4f);
                        explosionSpec.setDetailedExplosionFlashRadius(0.5f);
                        explosionSpec.setDetailedExplosionFlashColorCore(Color.WHITE);
                        explosionSpec.setDetailedExplosionFlashColorFringe(ventColor.darker());
                        explosionSpec.setShowGraphic(true);
                        explosionSpec.setSoundSetId("mine_explosion");

                        Global.getCombatEngine().addLayeredRenderingPlugin(
                                new ShipExplosionFlareVisual(
                                        new ShipExplosionFlareVisual.ShipExplosionFlareParams(
                                                ship,
                                                Color.WHITE,
                                                ship.getShieldRadiusEvenIfNoShield(),
                                                ship.getShieldRadiusEvenIfNoShield()
                                        )
                                )
                        );

                        if (SotfModPlugin.GLIB) {
                            RippleDistortion ripple = new RippleDistortion(ship.getLocation(), ship.getLocation());
                            ripple.setSize(ship.getCollisionRadius() * 4.5F);
                            ripple.setIntensity(ship.getCollisionRadius());
                            ripple.setFrameRate(60.0F / ((ship.getHullSize().ordinal() + 1) * 0.25f));
                            ripple.fadeInSize((ship.getHullSize().ordinal() + 1) * 0.375f);
                            ripple.fadeOutIntensity((ship.getHullSize().ordinal() + 1) * 0.25f);
                            ripple.setSize(ship.getCollisionRadius() * 1.5F);
                            DistortionShader.addDistortion(ripple);
                        }

                        Global.getCombatEngine().applyDamage(ship, mine.getLocation(), 100000f, DamageType.HIGH_EXPLOSIVE, 0f, true, false, ship, false);
                        ship.splitShip();
                        Global.getCombatEngine().spawnDamagingExplosion(explosionSpec, ship, ship.getLocation(), true);
                        Global.getCombatEngine().removeEntity(mine);
                        ship.removeListener(this);
                        Global.getCombatEngine().removeEntity(ship);
                    }
                }

                //if (mine.getUntilMineExplosion() < 0.1f) {
                //    ship.setHitpoints(10f);
                //    ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
                //}
            }
        }
    }
}
