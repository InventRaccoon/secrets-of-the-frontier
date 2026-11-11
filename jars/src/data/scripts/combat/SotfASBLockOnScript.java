// Handles the ASB's mechanics besides the mark application and visuals (e.g the lidar drone and the actual firing)
package data.scripts.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.RealityDisruptorChargeGlow;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.utils.SotfMisc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import data.scripts.weapons.SotfASBEveryFrameScript;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;
import org.magiclib.util.MagicUI;

import java.awt.*;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glPopAttrib;

public class SotfASBLockOnScript extends BaseEveryFrameCombatPlugin {

    public ShipAPI drone;
    public boolean fired = false;
    public boolean expiring = false;
    public boolean showStatus = true;
    public float expireProgress = 0f;
    public float progress = 0f;
    public Vector2f firingLocation;
    public DamagingProjectileAPI shot;

    public static class ASBParams implements Cloneable {
        // REQUIRED
        public ShipAPI user;
        public int owner;
        public ShipAPI target;
        public String key;

        //public List<ShipAPI> users = new ArrayList<>();

        // EXTRAS
        public String weaponId = "sotf_asb"; // weapon used for projectile
        public String threatIndicatorId = "sotf_threatmarker"; // weapon used to deploy a hidden mine for AI purposes
        public String lidarId = "sotf_lidardrone_Variant";
        public String acquiringTarget = "!! ASC TRACING TARGET !!";
        public String targetLocked = "!! ASC TARGET LOCKED !!";

        // displayed below target ship in-world
        public String targetLockingString = "!! ASC TRACING TARGET !!";
        public String targetLockingStringHostile = "!! WARNING: TRACED BY HOSTILE LIDAR !!";
        public String targetLockedString = "!! ASC TARGET LOCKED !!";
        public String targetLockedStringHostile = "!! WARNING: HOSTILE ASC TARGET LOCKED !!";

        public boolean showStatusMessage = false;
        public String statusTitle = "!! WARNING: TRACED BY LIDAR !!"; // top line of status when player is being locked
        public String statusSubtext = "!! ASC LOCK IMMINENT !!"; // bottom line of status
        public String lockedString = "TARGET LOCKED!"; // popup when fully locked
        public Color lockedColor = Misc.getNegativeHighlightColor();
        public String acquiredSound = "sotf_asb_acquired"; // sound when beginning lock
        public String lockSound = "sotf_asb_locked"; // sound when fully locked
        public String fireSound = "sotf_asb_fire"; // global sound on firing
        public String lidarLoop = "system_high_energy_focus_loop"; // loop emitted by lidar laser

        public boolean showIndicators = true;
        public boolean suppressLockedAlert = false; // prevents lockedAlert message from being shown
        public String lockedAlert = "target-locked by anti-ship cannon"; // at top of screen, Shipname (x-class) target-locked by...
        public Color uiColor = Misc.getNegativeHighlightColor();
        public float chargeTime = 6f; // time taken by lidar to lock the ship

        //public float decayRate = 18f; // seconds over which a mark decays if not maintained

        public ASBParams(ShipAPI first, int owner, ShipAPI target, String key) {
            super();
            //this.users.add(first);
            this.user = first;
            this.owner = owner;
            this.target = target;
            this.key = key;
        }

        @Override
        protected ASBParams clone() {
            try {
                return (ASBParams) super.clone();
            } catch (CloneNotSupportedException e) {
                return null; // should never happen
            }
        }

    }

    public ASBParams p;

    protected float fadeIn = 0f;
    protected float fadeBounce = 0f;
    protected boolean bounceUp = true;
    protected boolean flicker = false;

    private static DrawableString TODRAW17;
    private static DrawableString TODRAW14;
    private static DrawableString TODRAW10;

    protected SpriteAPI cornerSprite = Global.getSettings().getSprite("ui", "sotf_targetui_corner");

    public void init(CombatEngineAPI engine) {
        try {
            int fontMult = 2;
            LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/insignia17LTAaa.fnt");
            TODRAW17 = fontdraw.createText();
            TODRAW17.setBlendSrc(GL_ONE);
            TODRAW17.setFontSize(17 * fontMult);

            fontdraw = LazyFont.loadFont("graphics/fonts/victor14.fnt");
            TODRAW14 = fontdraw.createText();
            TODRAW14.setBlendSrc(GL_ONE);
            TODRAW14.setFontSize(14 * fontMult);

            fontdraw = LazyFont.loadFont("graphics/fonts/victor10.fnt");
            TODRAW10 = fontdraw.createText();
            TODRAW10.setBlendSrc(GL_ONE);
            TODRAW10.setFontSize(10 * fontMult);

        } catch (FontException ignored) {

        }
    }

    public SotfASBLockOnScript(ASBParams p) {
        this.p = p;
        float xSide = (float) Math.random();
        if (Math.random() > 0.5f) {
            xSide *= -1f;
        }
        float y = Global.getCombatEngine().getMapHeight() * 0.5f + 600f;
        if (p.owner == 0) {
            y *= -1f;
        }
        firingLocation = new Vector2f((Global.getCombatEngine().getMapWidth() * 0.15f * (xSide)), y);
        p.target.setCustomData(SotfIDs.ASB_TARGET, true);
        //Global.getCombatEngine().addLayeredRenderingPlugin(new SotfASBLockRingsScript(this));
        Global.getSoundPlayer().playSound(p.acquiredSound, 1f, 2f, p.target.getLocation(), p.target.getVelocity());
        Global.getCombatEngine().addPlugin(new SotfASBWarnScript(p.target, this));
        if (p.target.getOwner() == 0 && !p.suppressLockedAlert) {
            Global.getCombatEngine().getCombatUI().addMessage(0, p.target,
                    SotfMisc.getShipNameColor(p.target), p.target.getName() + " (" + p.target.getHullSpec().getHullNameWithDashClass() + ")",
                    Misc.getTextColor(), " " + p.lockedAlert);
        }
    }

    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!engine.isPaused()) {
            progress += (amount * engine.getTimeMult().getModifiedValue()) / p.chargeTime;
        }

        float bounceMult = 1f;

        if (expiring) {
            progress = 1f;
            bounceMult = 6f;
        }
        if (expireProgress >= 1f) {
            p.target.getCustomData().remove(SotfIDs.ASB_TARGET);
            killDrone();
            Global.getCombatEngine().removePlugin(this);
            return;
        }
        if (!p.target.isAlive()) {
            expiring = true;
        }

        if (fadeIn <= 1f) fadeIn += amount * 4f;
        if (fadeIn > 1f) fadeIn = 1f;

        if (fadeIn >= 1f) {
            if (bounceUp) {
                fadeBounce += amount * 4f * bounceMult;
                if (fadeBounce > 1f) {
                    fadeBounce = 1f;
                    bounceUp = false;
                }
            } else {
                fadeBounce -= amount * bounceMult;
                if (fadeBounce < 0f) {
                    fadeBounce = 0f;
                    bounceUp = true;
                }
            }
        }

        if (expiring) {
            //MagicUI.drawHUDStatusBar(engine.getPlayerShip(), 1, Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(), 0, null, null, false);
            expireProgress += amount * 0.5f;
            if (expireProgress > 1f) {
                expireProgress = 1f;
            }
        }

        // Lidar drone with a rangefinding laser
        if (drone == null && !fired && !expiring) {
            CombatFleetManagerAPI fleetManager = engine.getFleetManager(p.owner);
            boolean wasSuppressing = fleetManager.isSuppressDeploymentMessages();
            fleetManager.setSuppressDeploymentMessages(true);

            drone = engine.getFleetManager(p.owner).spawnShipOrWing(p.lidarId, firingLocation, Misc.getAngleInDegrees(firingLocation, p.target.getLocation()));
            drone.getMutableStats().getDynamic().getMod(Stats.ELECTRONIC_WARFARE_PENALTY_MOD).modifyMult("sotf_ecmimmune", 0f);
            drone.setAlly(true);
            drone.getFleetMember().setAlly(true);
            drone.setCollisionClass(CollisionClass.NONE);

            //drone = engine.createFXDrone(Global.getSettings().getVariant(p.lidarId));
            //drone.setDrone(true);
            //drone.setLayer(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            //drone.setOwner(p.owner);
            //drone.getMutableStats().getHullDamageTakenMult().modifyMult("dem", 0f); // so it's non-targetable
            //drone.setCollisionClass(CollisionClass.NONE);

            fleetManager.removeDeployed(drone.getWing(), true);
            fleetManager.setSuppressDeploymentMessages(wasSuppressing);
            drone.getMutableStats().getSightRadiusMod().modifyMult(p.key, 0f);
        }
        // point drone at target and fire the laser
        else if (drone != null && !fired && !expiring) {
            drone.setShipAI(null);
            drone.getLocation().set(firingLocation.x, firingLocation.y);
            drone.blockCommandForOneFrame(ShipCommand.TURN_LEFT);
            drone.blockCommandForOneFrame(ShipCommand.TOGGLE_AUTOFIRE);
            drone.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
            // lidar lasers only have 1 range - increase that so they reach the target
            drone.getMutableStats().getBeamWeaponRangeBonus().modifyFlat(p.key, Misc.getDistance(drone.getLocation(), p.target.getLocation()) + 50f);
            // need to use the accurate method, otherwise beam is shaky/imprecise
            drone.setFacing(Misc.getAngleInDegreesStrict(drone.getLocation(), p.target.getLocation()));
            // by default, use a pulsing laser until 50% and then switch to a constant one
            if (progress <= 0.5f) {
                drone.giveCommand(ShipCommand.SELECT_GROUP, p.target.getLocation(), 0);
                drone.giveCommand(ShipCommand.FIRE, p.target.getLocation(), 0);
            } else {
                drone.giveCommand(ShipCommand.SELECT_GROUP, p.target.getLocation(), 1);
                drone.giveCommand(ShipCommand.FIRE, p.target.getLocation(), 1);
            }
            Global.getSoundPlayer().playLoop(p.lidarLoop, p.target, (1f + progress) * 0.5f, 1f, p.target.getLocation(), p.target.getVelocity());
            // blinking status icon
            if (p.showStatusMessage && p.target.equals(engine.getPlayerShip())) {
                if (showStatus) {
                    engine.maintainStatusForPlayerShip(p.key, "graphics/icons/hullsys/lidar_barrage.png", p.statusTitle, p.statusSubtext, true);
                    showStatus = false;
                } else {
                    showStatus = true;
                }
            }
        }
        // delete the drone if it isn't doing anything
        else {
            killDrone();
        }

        if (progress < 1f) {
            return;
        }
        if (!fired) {
            //shot = (MissileAPI) Global.getCombatEngine().spawnProjectile(user, null, weaponId, firingLocation, Misc.getAngleInDegrees(firingLocation, target.getLocation()), null);
            Vector2f target_loc = engine.getAimPointWithLeadForAutofire(drone, 1f, p.target, 4000f);
            shot = (DamagingProjectileAPI) Global.getCombatEngine().spawnProjectile(p.user, null, p.weaponId, firingLocation, Misc.getAngleInDegrees(firingLocation, target_loc), null);
            Global.getCombatEngine().addPlugin(new SotfASBEveryFrameScript(shot, p.target));
            // ANGRY BLOOP
            Global.getSoundPlayer().playSound(p.lockSound, 1f, 1f, p.target.getLocation(), p.target.getVelocity());

            // BOOM
            Vector2f fireSoundLoc = new Vector2f(engine.getViewport().convertScreenXToWorldX(engine.getViewport().getCenter().x),
                    engine.getViewport().convertScreenYToWorldY(engine.getViewport().getCenter().y));
            Global.getSoundPlayer().playSound(p.fireSound, 1f, 1f, fireSoundLoc, new Vector2f(0f,0f));
            //Global.getCombatEngine().addFloatingText(new Vector2f(p.target.getShieldCenterEvenIfNoShield().x, p.target.getShieldCenterEvenIfNoShield().y + p.target.getShieldRadiusEvenIfNoShield() + 50f),
            //        "-" + p.lockedString + "-",
            //         60f, p.lockedColor, p.target, 1f, 0.2f);
            expiring = true;
            fired = true;
        }
    }

    private void killDrone() {
        if (drone != null) {
            if (drone.isAlive()) {
                Global.getCombatEngine().applyDamage(drone, drone.getLocation(), 10000000, DamageType.HIGH_EXPLOSIVE, 0, true, false, null);
            }
            Global.getCombatEngine().removeEntity(drone);
            Global.getCombatEngine().getFleetManager(drone.getOwner()).removeDeployed(drone, false);
        }
    }

    public void renderInUICoords(ViewportAPI viewport) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!engine.isUIShowingHUD()) return;
        if (p.target == null || engine.getPlayerShip() == null) return;
        if (p.target != engine.getPlayerShip()) return;
        if (engine.getViewport().isNearViewport(p.target.getLocation(), -150f)) return;

        float alphaMult = fadeIn * (1f - fadeBounce * 0.5f) * (1f - expireProgress);
        int alpha = Math.round(255 * alphaMult);

        Color color = new Color(p.uiColor.getRed() / 255f, p.uiColor.getGreen() / 255f, p.uiColor.getBlue() / 255f, alphaMult);

        LazyFont.DrawableString toUse = TODRAW17;
        if (TODRAW17 != null && TODRAW14 != null) {
            String text = p.targetLockingString;
            String text2 = p.targetLockedString;
            if (p.target.getOwner() == 0) {
                text = p.targetLockingStringHostile;
                text2 = p.targetLockedStringHostile;
            }

            String textToUse = text;
            if (progress >= 1) {
                textToUse = text2;
            }

            toUse.setText(textToUse);
            toUse.setAnchor(LazyFont.TextAnchor.CENTER);
            //toUse.setAlignment(LazyFont.TextAlignment.CENTER);
            toUse.setAlignment(LazyFont.TextAlignment.CENTER);
            Vector2f pos = new Vector2f(
                    engine.getViewport().getCenter().x,
                    engine.getViewport().getCenter().y + engine.getViewport().getVisibleHeight() * 0.3f
            );
            toUse.setBaseColor(Misc.setBrightness(Color.BLACK, alpha));
            toUse.draw(pos.x + 1, pos.y - 1);
            toUse.setBaseColor(Misc.setBrightness(color, alpha));
            //toUse.setBaseColor(color);
            toUse.draw(pos);

            toUse = TODRAW14;
            toUse.setText("LOCKING: " + Math.round(progress * 100f) + "%");
            toUse.setAnchor(LazyFont.TextAnchor.CENTER);
            //toUse.setAlignment(LazyFont.TextAlignment.CENTER);
            toUse.setAlignment(LazyFont.TextAlignment.CENTER);
            Vector2f pos2 = new Vector2f(
                    pos.x,
                    pos.y - 24f
            );
            toUse.setBaseColor(Misc.setBrightness(Color.BLACK, alpha));
            toUse.draw(pos2.x + 1, pos2.y - 1);
            toUse.setBaseColor(Misc.setBrightness(color, alpha));
            //toUse.setBaseColor(color);
            toUse.draw(pos2);

        }
    }

    public void renderInWorldCoords(ViewportAPI viewport) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (p.target == null) return;
        if (!engine.isUIShowingHUD()) return;

        float alphaMult = fadeIn * (1f - fadeBounce * 0.5f) * (1f - expireProgress);
        int alpha = Math.round(255 * alphaMult);

        Color color = new Color(p.uiColor.getRed() / 255f, p.uiColor.getGreen() / 255f, p.uiColor.getBlue() / 255f, alphaMult);

        if (p.showIndicators) {
            float degrees = 45f;
            cornerSprite.setSize(30f, 30f);
            for (int i = 0; i < 4; i++) {
                Vector2f pointLoc = MathUtils.getPointOnCircumference(p.target.getLocation(), p.target.getShieldRadiusEvenIfNoShield() * 1.25f, degrees);
                //cornerSprite.setAlphaMult(alphaMult);
                cornerSprite.setAngle(degrees + 45f);
                cornerSprite.setColor(color);
                cornerSprite.renderAtCenter(pointLoc.x, pointLoc.y);

                degrees += 90f;
            }
        }

        LazyFont.DrawableString toUse = TODRAW17;
        if (TODRAW17 != null && TODRAW14 != null) {

            String text = p.targetLockingString;
            String text2 = p.targetLockedString;
            if (p.target.getOwner() == 0) {
                text = p.targetLockingStringHostile;
                text2 = p.targetLockedStringHostile;
            }

            String textToUse = text;
            if (progress >= 1) {
                textToUse = text2;
            }

            toUse.setText(textToUse);
            toUse.setAnchor(LazyFont.TextAnchor.CENTER);
            //toUse.setAlignment(LazyFont.TextAlignment.CENTER);
            toUse.setAlignment(LazyFont.TextAlignment.CENTER);
            Vector2f pos = new Vector2f(
                    p.target.getLocation().x,
                    p.target.getLocation().y - p.target.getShieldRadiusEvenIfNoShield() * 1.3f
            );
            toUse.setBaseColor(Misc.setBrightness(Color.BLACK, alpha));
            toUse.draw(pos.x + 1, pos.y - 1);
            toUse.setBaseColor(Misc.setBrightness(color, alpha));
            //toUse.setBaseColor(color);
            toUse.draw(pos);

            toUse = TODRAW14;
            toUse.setText("LOCKING: " + Math.round(progress * 100f) + "%");
            toUse.setAnchor(LazyFont.TextAnchor.CENTER);
            //toUse.setAlignment(LazyFont.TextAlignment.CENTER);
            toUse.setAlignment(LazyFont.TextAlignment.CENTER);
            Vector2f pos2 = new Vector2f(
                    pos.x,
                    pos.y - 24f
            );
            toUse.setBaseColor(Misc.setBrightness(Color.BLACK, alpha));
            toUse.draw(pos2.x + 1, pos2.y - 1);
            toUse.setBaseColor(Misc.setBrightness(color, alpha));
            //toUse.setBaseColor(color);
            toUse.draw(pos2);

        }
    }

    /**
     *  Code by zoeythegirlgal, modified by Inventor Raccoon
     */
    public static class SotfASBWarnScript extends BaseEveryFrameCombatPlugin {

        private ShipAPI target;
        private SotfASBLockOnScript ASBScript;
        private DamagingProjectileAPI shot;
        private CombatEntityAPI threatIndicator = null;
        private final IntervalUtil repositionInterval = new IntervalUtil(0.1f, 0.1f);
        private final CombatEngineAPI engine = Global.getCombatEngine();
        private float timer = 0f;
        private float responseTime;
        private float damage = 4000f;
        private DamageType damageType = DamageType.ENERGY;
        private boolean shotWasFired = false;

        public SotfASBWarnScript(ShipAPI target, SotfASBLockOnScript ASBScript) {
            this.target = target;
            this.ASBScript = ASBScript;

            responseTime = 1.5f;
            if (target.getShield() != null) {
                if (target.getShield().getType().equals(ShieldAPI.ShieldType.FRONT)) {
                    responseTime = 0.5f;
                }
            }

            WeaponSpecAPI wpnSpec = Global.getSettings().getWeaponSpec(ASBScript.p.weaponId);
            if (wpnSpec != null) {
                damage = wpnSpec.getDerivedStats().getDamagePerShot();
                damageType = wpnSpec.getDamageType();
            }
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            super.advance(amount, events);

            if (!shotWasFired && ASBScript.fired) {
                if (ASBScript.shot != null) {
                    shot = ASBScript.shot;
                }
                shotWasFired = true;
            }

            if (shotWasFired && (shot == null || shot.didDamage() || shot.isExpired())) {
                cleanup();
                return;
            }

            timer += amount * Global.getCombatEngine().getTimeMult().getModifiedValue();
            // delay reactions somewhat
            if (timer < responseTime) return;

            if (threatIndicator == null) {
                threatIndicator = createThreatIndicator();
            }
            repositionThreatIndicator(amount);
        }

        private void repositionThreatIndicator(float amount) {
            repositionInterval.advance(amount);
            if (repositionInterval.intervalElapsed()) {
                deleteThreatIndicator();
                threatIndicator = createThreatIndicator();
            }
        }

        private void deleteThreatIndicator() {
            if (threatIndicator != null) {
                threatIndicator.setHitpoints(0.00001f);
                engine.removeEntity(threatIndicator);
            }
        }

        private void cleanup() {
            deleteThreatIndicator();
            engine.removePlugin(this);
        }

        public CombatEntityAPI createThreatIndicator() {
            CombatEntityAPI threatIndicator = engine.spawnProjectile(
                    null,
                    null,
                    ASBScript.p.threatIndicatorId,
                    getThreatIndicatorPlacement(),
                    0f,
                    null
            );
            if (threatIndicator instanceof MissileAPI) {
                MissileAPI missile = (MissileAPI) threatIndicator;
                // these don't actually change behaviour, just makes the AI *think* this is a giant mine that's about to explode
                missile.setNoMineFFConcerns(true);
                missile.setMineExplosionRange(target.getShieldRadiusEvenIfNoShield() + 300f);
                missile.setMinePrimed(true);
                missile.setUntilMineExplosion(0f);
                // these actually do change its damage but the mine will never actually explode or hit anything
                missile.setDamageAmount(damage);
                missile.getDamage().setType(damageType);
            }
            threatIndicator.setOwner(0);
            if (target.getOwner() == 0) {
                threatIndicator.setOwner(1);
            }
            return threatIndicator;
        }

        private Vector2f getThreatIndicatorPlacement() {
            float radius = target.getShieldRadiusEvenIfNoShield() + 30f;
            Vector2f targetLocation = target.getLocation();
            Vector2f direction = VectorUtils.getDirectionalVector(targetLocation, ASBScript.firingLocation);
            Vector2f directionToReturn = new Vector2f(targetLocation.x + (direction.x * radius), targetLocation.y + (direction.y * radius));
            return directionToReturn;
        }
    }
}