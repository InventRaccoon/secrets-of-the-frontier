package data.subsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.NeuralTransferVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.combat.SotfSoulbondBondVisual;
import data.scripts.utils.SotfMisc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.CombatUI;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;
import org.magiclib.util.MagicAnim;
import org.magiclib.util.MagicRender;
import org.magiclib.util.MagicTxt;

import java.awt.*;
import java.util.List;

public class SotfSoulbondSubsystem extends MagicSubsystem {

    public static float FLUX_BUILDUP_PERCENT = 0.3f;
    public static float BASE_COOLDOWN = 45f;
    //public static float BASE_COOLDOWN = 1f;

    // static dash speed - not affected by ship speed or time mult
    public static float DRAG_SPEED = 1800f;

    public ShipAPI sierra = null;
    public boolean recall = false;

    public SotfSoulbondSubsystem(ShipAPI ship) {
        super(ship);
    }

    @Override
    public float getBaseInDuration() {
        return 0.5f;
    }

    @Override
    public float getBaseActiveDuration() {
        return 0.5f;
    }

    @Override
    public float getBaseCooldownDuration() {
        return BASE_COOLDOWN;
    }

    // we want to build flux on Sierra, not the flagship
//    @Override
//    public float getFluxCostPercentOnActivation() {
//        return FLUX_BUILDUP_PERCENT;
//    }

    // manual player usage only
    @Override
    public boolean shouldActivateAI(float amount) {
        return false;
    }

    @Override
    public boolean canActivate() {
        ShipAPI sierra = getSierrasShip();
        if (sierra == null) return false;
        return sierra != null;
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        ShipAPI flagship = Global.getCombatEngine().getPlayerShip();
        if (flagship != null && ship != flagship) {
            MagicSubsystemsManager.removeSubsystemFromShip(ship, SotfSoulbondSubsystem.class);
        }
        if (sierra != null && state == State.IN && !sierra.getSystem().isOn() && recall) {
            if (sierra.getSystem() != null && sierra.getSystem().getSpecAPI().isPhaseCloak()) {
                sierra.getSystem().forceState(ShipSystemAPI.SystemState.IN, stateInterval.getElapsed());
            }
        }
    }

    @Override
    public void onActivate() {
        sierra = getSierrasShip();
        if (sierra == null) return;

        float visualDuration = (Misc.getDistance(ship.getLocation(), sierra.getLocation()) / DRAG_SPEED);
        Global.getCombatEngine().addLayeredRenderingPlugin(new SotfSoulbondBondVisual(ship, sierra, visualDuration));
        Global.getCombatEngine().addLayeredRenderingPlugin(new SotfSoulbondBondVisual(sierra, ship, visualDuration));

        // if the system only has one valid function, use it regardless
        if (sierra.isCapital()) {
            prepKinskip();
            return;
        }

        if (ship.isCapital()) {
            prepRecall();
            return;
        }

        // otherwise, depends on if we're targeting Sierra (skip to her) or something else (drag her in)
        if (ship.getShipTarget() != null && ship.getShipTarget().equals(sierra)) {
            prepKinskip();
        } else {
            prepRecall();
        }
    }

    public void buildFlux() {
        if (sierra == null) return;
        float maxAddableFlux = sierra.getMaxFlux() - sierra.getCurrFlux() - 100;
        if (maxAddableFlux < 0) {
            maxAddableFlux = 0;
        }
        float fluxToAdd = sierra.getHullSpec().getFluxCapacity() * FLUX_BUILDUP_PERCENT;
        if (fluxToAdd >= maxAddableFlux) {
            fluxToAdd = maxAddableFlux;
        }
        sierra.getFluxTracker().increaseFlux(fluxToAdd, true);
    }

    public void prepRecall() {
        recall = true;
        Global.getSoundPlayer().playSound("system_recall_device", 1f, 1f, ship.getLocation(), ship.getVelocity());
        Global.getSoundPlayer().playSound("system_phase_teleporter", 1f, 1f, sierra.getLocation(), sierra.getVelocity());
        Global.getSoundPlayer().playSound("sotf_system_concordshift_fire", 1f, 1f, sierra.getLocation(), sierra.getVelocity());

        buildFlux();
    }

    public void prepKinskip() {
        recall = false;
        Global.getSoundPlayer().playSound("system_recall_device", 1f, 1f, sierra.getLocation(), sierra.getVelocity());
        Global.getSoundPlayer().playSound("system_phase_teleporter", 1f, 1f, ship.getLocation(), ship.getVelocity());

        buildFlux();
    }

    @Override
    public void onFinished() {
        if (sierra == null) return;
        float padding = sierra.getShieldRadiusEvenIfNoShield() + ship.getShieldRadiusEvenIfNoShield() + 250f;
        // Kinskip: bring us to Sierra
        if (!recall) {
            float angle = Misc.getAngleInDegrees(sierra.getLocation(), ship.getLocation());
            // not actual destination since it updates each frame - but used to calc how long the skip should take
            Vector2f dest = MathUtils.getPointOnCircumference(sierra.getLocation(), padding, angle);
            ship.addListener(new SotfSoulbondRecall(ship, sierra, dest, DRAG_SPEED));
            Global.getSoundPlayer().playSound("sotf_system_concordshift_fire", 1f, 1f, ship.getLocation(), ship.getVelocity());

            NegativeExplosionVisual.NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(new Color(200,125,255,155), ship.getShieldRadiusEvenIfNoShield() * 0.25f);
            p.fadeOut = 0.15f;
            p.hitGlowSizeMult = 0.25f;
            p.underglow = new Color(255,175,255, 50);
            p.withHitGlow = false;
            p.noiseMag = 1.25f;

            CombatEntityAPI e = Global.getCombatEngine().addLayeredRenderingPlugin(new NegativeExplosionVisual(p));
            e.getLocation().set(ship.getLocation());

            //if (SotfModPlugin.GLIB) {
            if (SotfModPlugin.GLIB) {
                RippleDistortion ripple = new RippleDistortion(ship.getLocation(), ship.getVelocity());
                ripple.setIntensity(ship.getCollisionRadius() * 0.75f);
                ripple.setSize(ship.getShieldRadiusEvenIfNoShield());
                ripple.fadeInSize(0.15f);
                ripple.fadeOutIntensity(0.5f);
                DistortionShader.addDistortion(ripple);
            }
        }
        // Recall: bring Sierra to us
        else {
            float angle = Misc.getAngleInDegrees(ship.getLocation(), sierra.getLocation());
            // not actual destination since it updates each frame - but used to calc how long the skip should take
            Vector2f dest = MathUtils.getPointOnCircumference(ship.getLocation(), padding, angle);
            sierra.addListener(new SotfSoulbondRecall(sierra, ship, dest, DRAG_SPEED));
        }
    }

    @Override
    public String getDisplayText() {
        ShipAPI sierra = getSierrasShip();
        if (sierra == null) return "Soulbond: Inactive";
        if (sierra.isCapital()) return "Soulbond: Kinskip";
        if (ship.isCapital()) return "Soulbond: Recall";

        if (ship.getShipTarget() != null) {
            if (ship.getShipTarget() == sierra) {
                return "Soulbond: Kinskip";
            }
        }
        return "Soulbond: Recall";
    }

    @Override
    public Color getHUDColor() {
        return SotfMisc.getSierraColor();
    }

    @Override
    public Color getExtraInfoColor() {
        return getHUDColor().darker().darker();
    }

    public ShipAPI getSierrasShip() {
        for (ShipAPI other : Global.getCombatEngine().getShips()) {
            if (other.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD) && !other.getVariant().hasTag(SotfIDs.TAG_INERT) && other.getVariant().hasHullMod(SotfIDs.HULLMOD_SOULBOND) && other.isAlive()) {
                return other;
            }
        }
        return null;
    }

    public static class SotfSoulbondRecall implements AdvanceableListener {
        public ShipAPI ship;
        public ShipAPI other;
        public Vector2f from;
        public Vector2f to;
        public float duration;
        public float time = 0f;
        public float distance = 1000f;
        public float padding = 0f;

        public float afterimageTimer = 0f;
        public float afterimageInterval = 0.35f;

        public float arcTimer = 0f;
        public float arcInterval = 0.2f;

        public float smokeTimer = 0f;
        public float smokeInterval = 0.05f;

        public boolean isRecall = false;
        public boolean wasInDanger = false;

        public Color smokeColor = new Color(20, 10, 50, 175);
        public Color phaseColor = new Color(155, 105, 205, 55);

        public SotfSoulbondRecall(ShipAPI ship, ShipAPI other, Vector2f to, float speed) {
            this.ship = ship;
            this.other = other;
            this.to = to;
            from = new Vector2f(ship.getLocation().getX(), ship.getLocation().getY());
            distance = Misc.getDistance(from, to);
            duration = (distance / speed) + 0.15f;
            padding = other.getShieldRadiusEvenIfNoShield() + ship.getShieldRadiusEvenIfNoShield() + 250f;

            isRecall = !other.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD);
            wasInDanger = ship.getHardFluxLevel() > 0.75f;
            //duration = 1;
            if (ship.getFluxTracker().isVenting()) {
                ship.getFluxTracker().stopVenting();
            }
        }

        public void advance(float amount) {
            // bcs we also need to phase the modules of a non-Sierra ship
            List<ShipAPI> shipAndModules = ship.getChildModulesCopy();
            shipAndModules.add(ship);

            // use existing phase system if possible because otherwise collision/phase state is really wonky
            boolean useExisting = false;
            if (ship.getSystem() != null) {
                if (ship.getSystem().getSpecAPI().isPhaseCloak()) {
                    ship.getSystem().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
                    useExisting = true;
                }
            } else if (ship.getPhaseCloak() != null) {
                if (ship.getPhaseCloak().getSpecAPI().isPhaseCloak()) {
                    ship.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
                    useExisting = true;
                }
            }

            if (!ship.isAlive() || time >= duration || other == null || !other.isAlive()) {
                if (!useExisting) {
                    for (ShipAPI toPhase : shipAndModules) {
                        toPhase.addListener(new SotfSoulbondFadeIn(toPhase));
                    }
                }
                //Global.getCombatEngine().getViewport().setExternalControl(false);

                if (!isRecall) {
                    Global.getCombatEngine().addFloatingText(new Vector2f(other.getLocation().x, other.getLocation().y + 75),
                            pickCompleteChatter(false, wasInDanger),
                            40f, SotfMisc.getSierraColor(), other, 0f, 0f);
                } else {
                    Global.getCombatEngine().addFloatingText(new Vector2f(ship.getLocation().x, ship.getLocation().y + 75),
                            pickCompleteChatter(true, wasInDanger),
                            40f, SotfMisc.getSierraColor(), ship, 0f, 0f);
                }

                ship.getMutableStats().getTimeMult().unmodify(SotfIDs.HULLMOD_SOULBOND);
                ship.getMutableStats().getPhaseCloakUpkeepCostBonus().unmodify(SotfIDs.HULLMOD_SOULBOND);

                ship.removeListenerOfClass(SotfSoulbondRecall.class);
                return;
            }
            float angle = Misc.getAngleInDegrees(other.getLocation(), from);
            Vector2f dest = MathUtils.getPointOnCircumference(other.getLocation(), padding, angle);

            float timeMult = (Global.getCombatEngine().getTimeMult().getModifiedValue() / ship.getMutableStats().getTimeMult().getModifiedValue());
            time += amount * timeMult;
            float progress = time / duration;
            //progress += amount;
            if (progress > 1) {
                progress = 1;
            }
            if (!useExisting) {
                for (ShipAPI toPhase : shipAndModules) {
                    toPhase.setPhased(true);
                    toPhase.setExtraAlphaMult(0.35f);
                    toPhase.setJitter(this, smokeColor, 1f, 5, 3f);
                    toPhase.getMutableStats().getTimeMult().modifyMult(SotfIDs.HULLMOD_SOULBOND, 3f);
                }
            }
            ship.getMutableStats().getPhaseCloakUpkeepCostBonus().modifyMult(SotfIDs.HULLMOD_SOULBOND, 0f);
            ship.getLocation().set(Misc.interpolateVector(from, dest, MagicAnim.smooth(progress)));
            if (useExisting) {
                ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
            }
            ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
            ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
            ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
            ship.getVelocity().set(0,0);

            Global.getSoundPlayer().playLoop("system_entropy_loop", ship, 0.4f + (progress * 0.8f), 0.7f + (progress * 0.3f), ship.getLocation(), ship.getVelocity());

            // YEAH THIS SUCKS ACTUALLY
            // smoothly transition the camera from A to B along with the ship
            //if (Global.getCombatEngine().getPlayerShip() != null && Global.getCombatEngine().getPlayerShip() == ship) {
                //Vector2f inter = Misc.interpolateVector(from, dest, MagicAnim.smooth(progress));
                //Global.getCombatEngine().getViewport().setExternalControl(true);
                //Global.getCombatEngine().getViewport().setCenter(inter);
            //}

            String data = "By Concord, Return";
            if (isRecall) {
                data = "And Thus I Return";
            }

            Global.getCombatEngine().maintainStatusForPlayerShip("$sotf_soulbond_travelling",
                    "graphics/icons/hullsys/displacer.png",
                    "Soulbond - Displacement In Progress",
                    data,
                    false);

            if (progress > 0.85f) return;
            smokeTimer += (amount * timeMult);
            if (smokeTimer > smokeInterval) {
                CombatEngineAPI engine = Global.getCombatEngine();
                float baseDuration = 3f;
                Vector2f vel = new Vector2f(ship.getVelocity());
                float size = ship.getCollisionRadius() * 0.5f;
                for (int i = 0; i < 6; i++) {
                    Vector2f point = new Vector2f(ship.getLocation());
                    point = Misc.getPointWithinRadiusUniform(point, ship.getCollisionRadius() * 0.75f, Misc.random);
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
                    engine.addNebulaParticle(pt, v, nSize * 1f, 2f,
                                0.5f / dur, 0f, dur, smokeColor);
                }
                smokeTimer -= smokeInterval;
            }

            arcTimer += (amount * timeMult);
            if (arcTimer >= arcInterval) {
                if (SotfModPlugin.GLIB) {
                    RippleDistortion ripple = new RippleDistortion(Misc.interpolateVector(from, dest, MagicAnim.smooth(progress + 0.08f)), ship.getVelocity());
                    ripple.setIntensity(ship.getCollisionRadius() * 0.5f);
                    ripple.setSize(ship.getShieldRadiusEvenIfNoShield() * 1.2f);
                    ripple.fadeInSize(0.15f);
                    ripple.fadeOutIntensity(0.5f);
                    ripple.setArc(angle - 60f, angle + 60f);
                    DistortionShader.addDistortion(ripple);
                }

                for (int i = 0; i < 2; i++) {
                    float arcAngle = angle - 45f + ((float) Math.random() * 90f);
                    float toAngle = angle + 180f - 25f + ((float) Math.random() * 50f);

                    Global.getCombatEngine().spawnEmpArcVisual(
                            MathUtils.getPointOnCircumference(
                                    ship.getShieldCenterEvenIfNoShield(),
                                    ship.getShieldRadiusEvenIfNoShield() + 35f + ((float) Math.random() * 20f),
                                    arcAngle),
                            ship,
                            MathUtils.getPointOnCircumference(
                                    ship.getShieldCenterEvenIfNoShield(),
                                    ship.getShieldRadiusEvenIfNoShield() + 15f + ((float) Math.random() * 15f),
                                    toAngle),
                            ship,
                            30f, new Color(100,25,155,255), Color.white
                    );
                }

                arcTimer -= arcInterval;
                arcTimer -= ((float) Math.random() * 0.05f);
            }

            if (ship.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD)) return;

            afterimageTimer += (amount * timeMult * ship.getMutableStats().getTimeMult().getModifiedValue());
            if (afterimageTimer > afterimageInterval) {
                SpriteAPI sprite = ship.getSpriteAPI();
                float offsetX = sprite.getWidth() / 2 - sprite.getCenterX();
                float offsetY = sprite.getHeight() / 2 - sprite.getCenterY();

                float trueOffsetX = (float) FastTrig.cos(Math.toRadians(ship.getFacing() - 90f)) * offsetX - (float) FastTrig.sin(Math.toRadians(ship.getFacing() - 90f)) * offsetY;
                float trueOffsetY = (float) FastTrig.sin(Math.toRadians(ship.getFacing() - 90f)) * offsetX + (float) FastTrig.cos(Math.toRadians(ship.getFacing() - 90f)) * offsetY;

                MagicRender.battlespace(
                        Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
                        new Vector2f(ship.getLocation().getX() + trueOffsetX, ship.getLocation().getY() + trueOffsetY),
                        new Vector2f(0, 0),
                        new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()),
                        new Vector2f(0, 0),
                        ship.getFacing() - 90f,
                        0f,
                        Misc.setAlpha(SotfMisc.getEidolonColor(), 75),
                        false,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0.1f,
                        0.5f,
                        CombatEngineLayers.BELOW_SHIPS_LAYER);
                afterimageTimer -= afterimageInterval;
            }
        }

        public static String pickCompleteChatter(boolean isRecall, boolean inDanger) {
            WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();

            if (inDanger) {
                if (isRecall) {
                    post.add("Phew. Thanks.");
                    post.add("Oh, hello!");
                    post.add("Wha- oh! Thank you.");
                    post.add("Bailing me out again?");
                    post.add("Thanks, kindred.");
                    post.add("Thus I am invoked.");
                } else {
                    post.add("You are not taken yet.");
                    post.add("Watch yourself, okay?");
                    post.add("The void refuses you.");
                    post.add("Careful, now!");
                    post.add("Got you.");
                    post.add("Safely in my grasp.");
                    post.add("They will not have you.");
                }
                post.add("Our bond preserves.");
            } else {
                if (isRecall) {
                    post.add("Thy call, answered.");
                    post.add("Thus I am invoked.");
                    post.add("Hmm~ hmm~, hello!");
                    post.add("I am called! Who needs banishing?");
                } else {
                    post.add("Welcome to here.");
                    post.add("Reeled in!");
                }
                post.add("Hey!");
                post.add("Oh, hello!");
                post.add("Missed me?");
                post.add("Kindred! HI!");
                post.add("Returned.");
                post.add("Recalled.");
                post.add("Summoned.");
                post.add("Invoked.");
            }

            return post.pick();
        }
    }

    // simple listener to fade in the ship over 1s
    public static class SotfSoulbondFadeIn implements AdvanceableListener {
        public ShipAPI ship;
        public float time = 0f;

        public SotfSoulbondFadeIn(ShipAPI ship) {
            this.ship = ship;
        }

        public void advance(float amount) {
            float timeMult = (Global.getCombatEngine().getTimeMult().getModifiedValue() / ship.getMutableStats().getTimeMult().getModifiedValue());
            time += amount * timeMult;

            if (time > 1f) {
                time = 1f;
            }

            if (time > 0.5f) {
                ship.setPhased(false);
            }

            ship.setExtraAlphaMult(0.35f + (time * 0.65f));

            if (time == 1f) {
                ship.removeListener(this);
            }
        }
    }

    @Override
    public void drawHUDBar(ViewportAPI viewport, Vector2f rootLoc, Vector2f barLoc, boolean displayAdditionalInfo, float longestNameWidth) {
        String nameText = getDisplayText();
        String keyText = getKeyText();

        if (!displayAdditionalInfo && !keyText.equals(BLANK_KEY)) {
            nameText = MagicTxt.getString("subsystemNameWithKeyText", nameText, keyText);
        }

        boolean displayStateText = true;
        if (requiresTarget()) {
            if (ship.getShipTarget() == null) {
                displayStateText = false;
            } else if (targetOnlyEnemies() && ship.getOwner() == ship.getShipTarget().getOwner()) {
                displayStateText = false;
            } else if (calcRange() >= 0 && MathUtils.getDistance(ship, ship.getShipTarget()) > calcRange()) {
                displayStateText = false;
            }
        }

        if (getFluxCostFlatOnActivation() > 0f) {
            if (ship.getFluxTracker().getCurrFlux() + getFluxCostFlatOnActivation() >= ship.getFluxTracker().getMaxFlux()) {
                displayStateText = false;
            }
        }

        if (getFluxCostPercentOnActivation() > 0f) {
            if (ship.getFluxTracker().getCurrFlux() + getFluxCostPercentOnActivation() * ship.getHullSpec().getFluxCapacity() >= ship.getFluxTracker().getMaxFlux()) {
                displayStateText = false;
            }
        }

        String stateText = getStateText();
        if (!displayStateText) {
            stateText = null;
        }

        float additionalBarPadding = Math.max(0f, longestNameWidth - CombatUI.STATUS_BAR_PADDING);
        CombatUI.drawSubsystemStatus(
                ship,
                getBarFill(),
                nameText,
                getHUDColor(),
                getExtraInfoText(),
                getExtraInfoColor(),
                stateText,
                keyText,
                getBriefText(),
                displayAdditionalInfo,
                getNumHUDBars(),
                barLoc,
                additionalBarPadding,
                rootLoc
        );
    }
}
