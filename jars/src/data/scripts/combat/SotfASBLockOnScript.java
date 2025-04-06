// Handles the ASB's mechanics besides the mark application and visuals (e.g the lidar drone and the actual firing)
package data.scripts.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import org.magiclib.util.MagicUI;
import data.scripts.weapons.SotfASBEveryFrameScript;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;

import java.awt.*;
import java.util.List;

public class SotfASBLockOnScript extends BaseEveryFrameCombatPlugin {

    public ShipAPI drone;
    public boolean fired = false;
    public boolean expiring = false;
    public boolean showStatus = true;
    public float expireProgress = 0f;
    public float progress = 0f;
    public Vector2f firingLocation;
    public DamagingProjectileAPI shot;
    //public MissileAPI shot;

    public static class ASBParams implements Cloneable {
        // REQUIRED
        public ShipAPI user;
        public int owner;
        public ShipAPI target;
        public String key;

        //public List<ShipAPI> users = new ArrayList<>();

        // EXTRAS
        public String weaponId = "sotf_asb"; // weapon used for projectile
        public String lidarId = "sotf_lidardrone_Variant";
        public String acquiringTarget = "ASB TRACING TARGET";
        public String targetLocked = "!! ASB TARGET LOCKED !!";
        public String statusTitle = "!! WARNING: TRACED BY LIDAR !!"; // top line of status when player is being locked
        public String statusSubtext = "!! ASB LOCK IMMINENT !!"; // bottom line of status
        public String lockedString = "TARGET LOCKED!"; // popup when fully locked
        public Color lockedColor = Misc.getNegativeHighlightColor();
        public String acquiredSound = "sotf_asb_acquired"; // sound when beginning lock
        public String lockSound = "sotf_asb_locked"; // sound when fully locked
        public String fireSound = "sotf_asb_fire"; // global sound on firing
        public String lidarLoop = "system_high_energy_focus_loop"; // loop emitted by lidar laser
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

    private static DrawableString TODRAW14;
    private static DrawableString TODRAW10;
    private static final float UIscaling = Global.getSettings().getScreenScaleMult();

    static {
        try {
            LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/victor14.fnt");
            TODRAW14 = fontdraw.createText();

            fontdraw = LazyFont.loadFont("graphics/fonts/victor10.fnt");
            TODRAW10 = fontdraw.createText();

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
        Global.getCombatEngine().addLayeredRenderingPlugin(new SotfASBLockRingsScript(this));
        Global.getSoundPlayer().playSound(p.acquiredSound, 1f, 2f, p.target.getLocation(), p.target.getVelocity());
    }

    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!engine.isPaused()) {
            progress += (amount * engine.getTimeMult().getModifiedValue()) / p.chargeTime;
        }

        if (expiring) {
            progress = 1f;
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

        if (expiring) {
            //MagicUI.drawHUDStatusBar(engine.getPlayerShip(), 1, Misc.getPositiveHighlightColor(), Misc.getPositiveHighlightColor(), 0, null, null, false);
            expireProgress += amount;
        }

        // Lidar drone with a rangefinding laser
        if (drone == null && !fired && !expiring) {
            CombatFleetManagerAPI fleetManager = engine.getFleetManager(p.owner);
            boolean wasSuppressing = fleetManager.isSuppressDeploymentMessages();
            fleetManager.setSuppressDeploymentMessages(true);

            drone = engine.getFleetManager(p.owner).spawnShipOrWing(p.lidarId, firingLocation, Misc.getAngleInDegrees(firingLocation, p.target.getLocation()));
            drone.setAlly(true);
            drone.getFleetMember().setAlly(true);
            drone.setCollisionClass(CollisionClass.NONE);

            //drone = engine.createFXDrone(Global.getSettings().getVariant(p.lidarId));
            //drone.setDrone(true);
            //drone.setLayer(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
            //drone.setOwner(p.owner);
            //drone.getMutableStats().getHullDamageTakenMult().modifyMult("dem", 0f); // so it's non-targetable
            //drone.setCollisionClass(CollisionClass.NONE);

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
            drone.getMutableStats().getBeamWeaponRangeBonus().modifyFlat(p.key, Misc.getDistance(drone.getLocation(), p.target.getLocation()) - 1f);
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
            if (p.target.equals(engine.getPlayerShip())) {
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
            Global.getCombatEngine().addFloatingText(new Vector2f(p.target.getShieldCenterEvenIfNoShield().x, p.target.getShieldCenterEvenIfNoShield().y + p.target.getShieldRadiusEvenIfNoShield() + 50f),
                    "-" + p.lockedString + "-",
                    60f, p.lockedColor, p.target, 1f, 0.2f);
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

    // old code I couldn't figure out how to work

    private static void addHUDStatusText(ShipAPI ship, Color textColor, Vector2f screenPos) {
        Color borderCol = textColor == null ? MagicUI.GREENCOLOR : textColor;
        if (!ship.isAlive()) {
            borderCol = MagicUI.BLUCOLOR;
        }
        float alpha = 1f;
        if (Global.getCombatEngine().isUIShowingDialog()) {
            return;
        }
        Color shadowcolor = new Color(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
        Color color = new Color(borderCol.getRed() / 255f, borderCol.getGreen() / 255f, borderCol.getBlue() / 255f,
                alpha * (borderCol.getAlpha() / 255f)
                        * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));

        final Vector2f boxLoc = new Vector2f(screenPos.getX(), screenPos.getY());
        final Vector2f shadowLoc = new Vector2f(screenPos.getX() + 1f, screenPos.getY() - 1f);
        if (UIscaling !=1 ) {
            boxLoc.scale(UIscaling);
            shadowLoc.scale(UIscaling);
            TODRAW10.setFontSize(10*UIscaling);
        }

        // Global.getCombatEngine().getViewport().
        openGL11ForText();
        // TODRAW10.setText(text);
        // TODRAW10.setMaxHeight(26);
        TODRAW10.setText("TARGET ACQUIRED");
        TODRAW10.setBaseColor(shadowcolor);
        TODRAW10.draw(shadowLoc);
        TODRAW10.setBaseColor(color);
        TODRAW10.draw(boxLoc);
        closeGL11ForText();
    }

    /**
     * GL11 to start, when you want render text of Lazyfont.
     */
    private static void openGL11ForText() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        GL11.glOrtho(0.0, Display.getWidth(), 0.0, Display.getHeight(), -1.0, 1.0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * GL11 to close, when you want render text of Lazyfont.
     */
    private static void closeGL11ForText() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

}