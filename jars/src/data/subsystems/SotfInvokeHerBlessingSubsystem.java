package data.subsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.SotfNaniteSynthesized;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.combat.special.SotfFelInvasionPlugin;
import data.scripts.combat.special.SotfInvokeHerBlessingPlugin;
import data.scripts.combat.special.SotfInvokeHerBlessingPlugin.SotfInvokeHerBlessingEchoScript;
import data.scripts.combat.special.SotfInvokeHerBlessingPlugin.SotfMimicLifespanListener;
import data.scripts.utils.SotfMisc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.CombatUI;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;
import org.magiclib.subsystems.drones.DroneFormation;
import org.magiclib.subsystems.drones.MagicDroneSubsystem;
import org.magiclib.subsystems.drones.PIDController;
import org.magiclib.util.MagicTxt;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static data.scripts.combat.special.SotfInvokeHerBlessingPlugin.*;
import static data.shipsystems.SotfGravispatialSurgeSystem.*;
import static org.lwjgl.opengl.GL11.GL_ONE;

public class SotfInvokeHerBlessingSubsystem extends MagicDroneSubsystem {

    public static float BASE_COOLDOWN = 0.25f;
    public static float ECHO_FP_COOLDOWN_MULT = 5f;
    public static final float ECHO_SELECT_RANGE = 25f;

    IntervalUtil interval = new IntervalUtil(0.5f, 0.65f);

    public SotfInvokeHerBlessingEchoScript echo;

    public SotfInvokeHerBlessingSubsystem(ShipAPI ship) {
        super(ship);
    }

    // sort before Dream Eater
    public int getOrder() {
        return 3;
    }

    @Override
    public float getBaseInDuration() {
        return 0f;
    }

    @Override
    public float getBaseActiveDuration() {
        return 0f;
    }

    @Override
    public float getBaseCooldownDuration() {
        return BASE_COOLDOWN;
    }

    // manual player usage only
    @Override
    public boolean shouldActivateAI(float amount) {
        return false;
    }

    @Override
    public boolean canActivate() {
        if (ship.getShipTarget() != null) {
            if (ship.getShipTarget().getVariant().hasTag(SotfPeople.SIRIUS_MIMIC) && !ship.getShipTarget().hasListenerOfClass(SotfMimicDecayListener.class) && ship.getShipTarget().isAlive()) {
                return Misc.getDistance(ship.getLocation(), ship.getShipTarget().getLocation()) <= SotfInvokeHerBlessingPlugin.ECHO_CREATION_RANGE;
            }
        }
        if (echo == null) return false;
        return Misc.getDistance(ship.getLocation(), echo.loc) <= SotfInvokeHerBlessingPlugin.ECHO_CREATION_RANGE;
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        ShipAPI flagship = Global.getCombatEngine().getPlayerShip();

        // Subsystems don't run every frame while paused: moved this code to SotfInvokeHerBlessingPlugin's advance func
//        if (state == State.READY) {
//            echo = findValidEcho();
//            if (echo != null) {
//                echo.select();
//            }
//        }
        if (!haveUpgrade(SotfIDs.COTL_AUTOPILOT)) return;

        interval.advance(amount);
        if (!interval.intervalElapsed()) return;

        if (flagship != null) {
            if (flagship == ship && !Global.getCombatEngine().isUIAutopilotOn()) {
                SotfInvokeHerBlessingEchoScript autoEcho = findAutopilotEcho();
                if (autoEcho != null && canActivateInternal()) {
                    echo = autoEcho;
                    ship.setShipTarget(null);
                    onActivate();
                }
            }
        }
    }

    @Override
    public void onActivate() {
        if (ship.getShipTarget() != null) {
            if (ship.getShipTarget().getVariant().hasTag(SotfPeople.SIRIUS_MIMIC) && !ship.hasListenerOfClass(SotfMimicDecayListener.class) && ship.isAlive()) {
                EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(ship.getShieldCenterEvenIfNoShield(),
                        ship,
                        ship.getShipTarget().getLocation(), null, 10f, Color.DARK_GRAY, Color.WHITE);
                arc.setFadedOutAtStart(true);
                arc.setCoreWidthOverride(7.5f);
                Global.getSoundPlayer().playSound("sotf_invokeherblessing", 1f, 1f, ship.getLocation(), ship.getVelocity());
                Global.getSoundPlayer().playSound("mote_attractor_impact_damage", 1, 1f, ship.getShipTarget().getLocation(), new Vector2f());

                float lifespanMult = 1f;
                if (ship.getShipTarget().hasListenerOfClass(SotfMimicLifespanListener.class)) {
                    for (SotfMimicLifespanListener listener : new ArrayList<SotfMimicLifespanListener>(ship.getShipTarget().getListeners(SotfMimicLifespanListener.class))) {
                        listener.beginExpiring();
                        lifespanMult = Math.max(1f - (listener.time / listener.lifespan), DREAMEATER_REPAIR_MINIMUM);
                    }
                } else {
                    // reflections don't have the lifespan listener, so just add the decay listener
                    Global.getCombatEngine().addFloatingText(ship.getLocation(), "Expiring!", ship.getFluxTracker().getFloatySize() + 5f, SotfNaniteSynthesized.COLOR_STRONGER, ship, 0f, 0f);
                    ship.getShipTarget().addListener(new SotfMimicDecayListener(ship.getShipTarget(), haveUpgrade(SotfIDs.COTL_DEATHTHROES)));
                }

                if (haveUpgrade(SotfIDs.COTL_DREAMEATER)) {
                    float percentHeal = (float) SotfMisc.forHullSize(ship.getShipTarget(), DREAMEATER_REPAIR_FRIGATE, DREAMEATER_REPAIR_DESTROYER, DREAMEATER_REPAIR_CRUISER, DREAMEATER_REPAIR_CAPITAL);
                    percentHeal *= lifespanMult;
                    ship.setHitpoints(Math.min(ship.getHitpoints() + (ship.getMaxHitpoints() * percentHeal), ship.getMaxHitpoints()));
                    if (DefenseUtils.getMostDamagedArmorCell(ship) != null) {
                        SotfMisc.repairMostDamaged(ship, ship.getArmorGrid().getArmorRating() * percentHeal);
                        ship.syncWithArmorGridState();
                        ship.syncWeaponDecalsWithArmorDamage();
                    }
                    EmpArcEntityAPI arc2 = Global.getCombatEngine().spawnEmpArcVisual(ship.getShieldCenterEvenIfNoShield(),
                            ship,
                            ship.getShipTarget().getLocation(), null, 12f, Misc.getPositiveHighlightColor(), Color.WHITE);
                    arc2.setFadedOutAtStart(true);
                    arc2.setCoreWidthOverride(6f);
                }
                return;
            }
        }
        if (echo == null) return;
        int usedDp = (int) Global.getCombatEngine().getCustomData().get(USED_DP_KEY);
        int maxDp = getMimicCapacity();
        if (echo.dp + usedDp > maxDp) {
            EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(ship.getShieldCenterEvenIfNoShield(),
                    ship,
                    echo.loc, null, 5f, Misc.getNegativeHighlightColor(), Color.WHITE);
            arc.setCoreWidthOverride(2.5f);
            arc.setFadedOutAtStart(true);
            Global.getSoundPlayer().playSound("sotf_invokeherblessing", 0.75f, 0.75f, ship.getLocation(), ship.getVelocity());
            Global.getSoundPlayer().playSound("mote_attractor_impact_normal", 0.75f, 1f, echo.loc, new Vector2f());
        } else {
            EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(ship.getShieldCenterEvenIfNoShield(),
                    ship,
                    echo.loc, null, 10f, Color.DARK_GRAY, Color.WHITE);
            arc.setFadedOutAtStart(true);
            arc.setCoreWidthOverride(5f);
            Global.getSoundPlayer().playSound("sotf_invokeherblessing", 1f, 1f, ship.getLocation(), ship.getVelocity());
            Global.getSoundPlayer().playSound("mote_attractor_impact_damage", 1, 1f, echo.loc, new Vector2f());
        }

        if (haveUpgrade(SotfIDs.COTL_SHRIEKOFTHEDAMNED)) {
            shriek(echo.loc, ship);
        }

        //setCooldownDuration(echo.fp * ECHO_FP_COOLDOWN_MULT, true);
        echo.startFading();
        echo.readyToSpawn();
        echo.selected = false;
        echo = null;
    }

    public static void shriek(Vector2f loc, ShipAPI ship) {
        float baseChance = SHRIEK_PD_BASE_CHANCE;
        float perTarget = SHRIEK_PD_DECAY_PER_TARGET;
        int pdStrikeCount = 0;
        for (ShipAPI otherShip : CombatUtils.getShipsWithinRange(loc, SHRIEK_RANGE)) {
            if (otherShip.getOwner() != 1) continue;
            if (otherShip.isFighter()) {
                if (Misc.random.nextFloat() >= baseChance - (perTarget * pdStrikeCount)) continue;
                pdStrikeCount++;
            }
            strikeShip(loc, ship, otherShip);
        }
        for (MissileAPI missile : CombatUtils.getMissilesWithinRange(loc, SHRIEK_RANGE)) {
            if (missile.getOwner() != 1) continue;
            if (Misc.random.nextFloat() >= baseChance - (perTarget * pdStrikeCount)) continue;
            pdStrikeCount++;
            strikeMissile(loc, ship, missile);
        }
        Global.getSoundPlayer().playUISound("sotf_perfectstorm_blast", 0.65f, 1f);

        if (SotfModPlugin.GLIB) {
            RippleDistortion ripple = new RippleDistortion(loc, new Vector2f());
            ripple.setIntensity(75f * 0.75f);
            ripple.setSize(100f);
            ripple.fadeInSize(0.15f);
            ripple.fadeOutIntensity(0.5f);
            DistortionShader.addDistortion(ripple);
        }

        for (int i = 0; i < 6; i++) {
            Global.getCombatEngine().spawnEmpArcVisual(
                    Misc.getPointWithinRadius(loc, 200f * 0.5f),
                    null,
                    Misc.getPointWithinRadius(loc, 200f * 5),
                    null,
                    6f,
                    SotfNaniteSynthesized.COLOR_STRONGER,
                    Color.WHITE
            );
        }
    }

    private static void strikeShip(Vector2f loc, ShipAPI ship, final ShipAPI target) {
        float maxDamp = MAX_DAMP;
        float damage = DAMAGE;
        float emp = EMP;
        if (target.isFighter()) {
            damage *= FIGHTER_DAMAGE_MULT;
            emp *= FIGHTER_DAMAGE_MULT;
            maxDamp = FIGHTER_MAX_DAMP;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        for (int i = 0; i < 2; i++) {
            EmpArcEntityAPI arc = engine.spawnEmpArcPierceShields(
                    ship,
                    Misc.getPointWithinRadius(loc, 60f),
                    null,
                    target,
                    DamageType.FRAGMENTATION,
                    damage,
                    emp,
                    99999f,
                    "tachyon_lance_emp_impact",
                    12f,
                    SotfNaniteSynthesized.COLOR_STRONGER,
                    Color.WHITE
            );

            if (SotfModPlugin.GLIB) {
                RippleDistortion ripple = new RippleDistortion(arc.getLocation(), target.getVelocity());
                ripple.setIntensity(30f);
                ripple.setSize(20f);
                ripple.fadeInSize(0.1f);
                ripple.fadeOutIntensity(0.3f);
                DistortionShader.addDistortion(ripple);
            }
        }

        float dampScale = target.getMassWithModules() / MASS_FOR_MIN_DAMP;
        if (dampScale > 1) {
            dampScale = 1;
        }
        target.getVelocity().scale(MAX_DAMP + (dampScale * (MIN_DAMP - maxDamp)));

        if (target.isFighter() && target.getHullLevel() < COLLAPSE_THRESHOLD && !target.hasListenerOfClass(SotfFighterGraviticCollapseScript.class)) {
            target.addListener(new SotfFighterGraviticCollapseScript(target));
        }
    }

    protected static void strikeMissile(Vector2f loc, ShipAPI ship, final MissileAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();

        float damage = 10f;
        if (target.getProjectileSpecId().contains("mote") || target.getProjectileSpecId().contains("kol_sparkle")) {
            damage = 300f;
        }

        // flame out, disarm (if possible), and damp the hell out of it
        target.flameOut();
        target.setArmedWhileFizzling(false);
        // slow zoned like a Belter kid in a makeshift racing ship
        target.getVelocity().scale(0.15f);

        engine.spawnEmpArcPierceShields(
                ship,
                Misc.getPointWithinRadius(loc, 60f),
                null,
                target,
                DamageType.FRAGMENTATION,
                damage,
                0f,
                99999f,
                "tachyon_lance_emp_impact",
                8f,
                SotfNaniteSynthesized.COLOR_STRONGER,
                Color.WHITE
        );

        if (SotfModPlugin.GLIB) {
            RippleDistortion ripple = new RippleDistortion(target.getLocation(), target.getVelocity());
            ripple.setIntensity(30f);
            ripple.setSize(20f);
            ripple.fadeInSize(0.1f);
            ripple.fadeOutIntensity(0.3f);
            DistortionShader.addDistortion(ripple);
        }
    }

    public SotfInvokeHerBlessingEchoScript findValidEcho() {
        Vector2f from = ship.getMouseTarget();

        SotfInvokeHerBlessingEchoScript best = null;
        float minScore = Float.MAX_VALUE;

        for (SotfInvokeHerBlessingEchoScript echo : Global.getCombatEngine().getListenerManager().getListeners(SotfInvokeHerBlessingEchoScript.class)) {
            if (echo.fading) continue;
            float dist = Misc.getDistance(from, echo.loc);
            if (dist < (echo.shieldRadius * 1.5f) && dist < minScore) {
                minScore = dist;
                best = echo;
            }
        }
        return best;
    }

    public SotfInvokeHerBlessingEchoScript findAutopilotEcho() {
        SotfInvokeHerBlessingEchoScript best = null;
        float minScore = Float.MAX_VALUE;

        for (SotfInvokeHerBlessingEchoScript echo : Global.getCombatEngine().getListenerManager().getListeners(SotfInvokeHerBlessingEchoScript.class)) {
            if (echo.fading) continue;
            float dist = Misc.getDistance(echo.loc, ship.getLocation());
            if (dist > ECHO_CREATION_RANGE) continue;
            if (echo.dp + echo.dpUsed > echo.dpMax) continue;
            if (dist < minScore) {
                minScore = dist;
                best = echo;
            }
        }
        return best;
    }

    // total DP of mimics (only counts those created by Invoke Her Blessing)
    public static int getUsedMimicDP() {
        float usedDp = 0;
        for (FleetMemberAPI ally : Global.getCombatEngine().getFleetManager(0).getDeployedCopy()) {
            ShipAPI shipForAlly = Global.getCombatEngine().getFleetManager(0).getShipFor(ally);
            if (shipForAlly == null) continue;
            if (!shipForAlly.isAlive()) continue;
            if (ally.getVariant().hasTag(SotfPeople.SIRIUS_MIMIC) && !shipForAlly.hasListenerOfClass(SotfMimicDecayListener.class)) {
                usedDp += ally.getHullSpec().getSuppliesToRecover();
            }
        }
        return Math.round(usedDp);
    }

    // get actual player mimic capacity
    public static int getMimicCapacity() {
        int maxDp = SotfInvokeHerBlessingPlugin.BASE_DP;
        maxDp += (Global.getSector().getPlayerPerson().getStats().getLevel() - 1) * SotfInvokeHerBlessingPlugin.DP_PER_LEVEL;
        if (haveUpgrade(SotfIDs.COTL_MULTIFACETED)) {
            maxDp *= (1f + SotfInvokeHerBlessingPlugin.MULTIFACTED_MULT);
        }
        return Math.round(maxDp);
    }

    // get player mimics capacity with or without Multifaceted
    public static int getMimicCapacityTheoretical(boolean withMultifaceted) {
        int maxDp = SotfInvokeHerBlessingPlugin.BASE_DP;
        maxDp += (Global.getSector().getPlayerPerson().getStats().getLevel() - 1) * SotfInvokeHerBlessingPlugin.DP_PER_LEVEL;
        if (withMultifaceted) {
            maxDp *= (1f + SotfInvokeHerBlessingPlugin.MULTIFACTED_MULT);
        }
        return Math.round(maxDp);
    }

    @Override
    public void onFinished() {
//        if (echo == null) return;
//        echo.spawnMimic();
//        echo = null;
    }

    @Override
    public String getDisplayText() {
        String append = "no echo selected";
        if (echo != null) {
            if (Misc.getDistance(ship.getLocation(), echo.loc) > SotfInvokeHerBlessingPlugin.ECHO_CREATION_RANGE) {
                append = "out of range";
            } else if (state == State.READY) {
                append = "ready";
            }
        }
        if (ship.getShipTarget() != null) {
            if (ship.getShipTarget().getVariant().hasTag(SotfPeople.SIRIUS_MIMIC) && !ship.getShipTarget().hasListenerOfClass(SotfMimicDecayListener.class)) {
                if (Misc.getDistance(ship.getLocation(), ship.getShipTarget().getLocation()) <= SotfInvokeHerBlessingPlugin.ECHO_CREATION_RANGE) {
                    append = "expire order ready";
                } else {
                    append = "out of expire order range";
                }
            }
        }
        append += " " + getUsedMimicDP() + "/" + getMimicCapacity();
        if (getUsedMimicDP() > getMimicCapacity()) {
            append += " - OVERCLOCKED!";
        }
        return "Invoke Her Blessing - " + append;
    }

    @Override
    public Color getHUDColor() {
        return SotfNaniteSynthesized.COLOR_STRONGER;
    }

    @Override
    public Color getExtraInfoColor() {
        return getHUDColor().darker();
    }

    @Override
    public int getMaxCharges() {
        return 0;
    }

    @Override
    public int getMaxDeployedDrones() {
        return 1;
    }

    @Override
    public boolean usesChargesOnActivate() {
        return false;
    }

    @Override
    public @NotNull ShipAPI spawnDrone() {
        ShipAPI fighter = super.spawnDrone();

//        fighter.getVariant().addMod(SotfIDs.HULLMOD_NANITE_SYNTHESIZED);
        fighter.setCollisionClass(CollisionClass.NONE);

//        PersonAPI fighterCaptain = Global.getFactory().createPerson();
//        fighterCaptain.getStats().setSkillLevel(SotfIDs.SKILL_ADVANCEDCOUNTERMEASURES, 2);
//        fighter.setCaptain(fighterCaptain);

        SpriteAPI sprite = Global.getSettings().getSprite("skills", "sotf_naniteswarm_sheet");
        float i = Misc.random.nextInt(4);
        float j = Misc.random.nextInt(4);
        sprite.setTexWidth(0.25f);
        sprite.setTexHeight(0.25f);
        sprite.setTexX(i * 0.25f);
        sprite.setTexY(j * 0.25f);
        fighter.setSprite(sprite);

        Global.getCombatEngine().addPlugin(new SotfIHBFollowerDisplay(fighter, ship));

        return fighter;
    }

    public static class SotfIHBFollowerDisplay extends BaseEveryFrameCombatPlugin {
        ShipAPI ship;
        ShipAPI mothership;

        protected float fadeIn = 0f;
        protected float fadeOut = 1f;
        protected float fadeBounce = 0f;
        protected boolean bounceUp = true;

        protected float elapsed = 0f;

        private static LazyFont.DrawableString TODRAW14;
        private static LazyFont.DrawableString TODRAW10;

        public SotfIHBFollowerDisplay(ShipAPI ship, ShipAPI mothership) {
            this.ship = ship;
            this.mothership = mothership;

            try {
                LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/victor14.fnt");
                TODRAW14 = fontdraw.createText();
                TODRAW14.setBlendSrc(GL_ONE);

                fontdraw = LazyFont.loadFont("graphics/fonts/victor10.fnt");
                TODRAW10 = fontdraw.createText();
                TODRAW10.setBlendSrc(GL_ONE);

            } catch (FontException ignored) {
            }
        }

        @Override
        public void advance(float amount, java.util.List<InputEventAPI> events) {
//            if (!ship.isAlive()) {
//                cleanup();
//                return;
//            }
            if (mothership != null) {
                if (mothership != Global.getCombatEngine().getPlayerShip() || !mothership.isAlive() ||
                        MagicSubsystemsManager.getSubsystemsForShipCopy(mothership) == null) {
                    cleanup();
                    return;
                }
                boolean foundSubsystem = false;
                for (MagicSubsystem sub : MagicSubsystemsManager.getSubsystemsForShipCopy(mothership)) {
                    if (sub instanceof SotfInvokeHerBlessingSubsystem) {
                        foundSubsystem = true;
                        break;
                    }
                }
                if (!foundSubsystem) {
                    cleanup();
                    return;
                }
            }

            if (fadeIn <= 1f) fadeIn += amount;
            if (fadeIn > 1f) fadeIn = 1f;

            if (fadeIn >= 1f) {
                if (bounceUp) {
                    fadeBounce += amount * 2f;
                    if (fadeBounce > 1f) {
                        bounceUp = false;
                    }
                } else {
                    fadeBounce -= amount * 2f;
                    if (fadeBounce < 0f) {
                        bounceUp = true;
                    }
                }
            }

            if (Global.getCombatEngine().isPaused()) return;

            elapsed += amount;
        }

        public void cleanup() {
            Global.getCombatEngine().removeEntity(ship);
            Global.getCombatEngine().removePlugin(this);
        }

        @Override
        public void renderInUICoords(ViewportAPI viewport) {
            super.renderInUICoords(viewport);

            if (!Global.getCombatEngine().isUIShowingHUD()) return;

            LazyFont.DrawableString toUse = TODRAW14;
            if (toUse != null) {
                int alpha = Math.round(255 * fadeIn * fadeOut * (1f - (fadeBounce * 0.2f)));

                int dpUsed = (int) Global.getCombatEngine().getCustomData().get(USED_DP_KEY);
                int capacity = (int) Global.getCombatEngine().getCustomData().get(CAPACITY_KEY);

                String text = "CAPACITY: " + (capacity - dpUsed) + "/" + capacity;
                Color colorToUse = UI_COLOR;

                if (dpUsed > capacity) {
                    float overclock = 1f;
                    if (dpUsed > capacity) {
                        overclock = (float) dpUsed / capacity;
                        if (overclock < OVERCLOCK_MIN_RATE) {
                            overclock = OVERCLOCK_MIN_RATE;
                        }
                    }
                    text += "\nOVERCLOCKED: " + Misc.getRoundedValueMaxOneAfterDecimal(overclock) + "x DECAY";
                    text = SotfMisc.glitchify(text, 0.003f);
                    colorToUse = Misc.getNegativeHighlightColor();
                }

                //TODRAW14.setFontSize(28);
                toUse.setBaseColor(Misc.setBrightness(colorToUse, alpha));
                toUse.setText(text);
                toUse.setAnchor(LazyFont.TextAnchor.CENTER_RIGHT);
                toUse.setAlignment(LazyFont.TextAlignment.RIGHT);
                Vector2f pos = new Vector2f(viewport.convertWorldXtoScreenX(ship.getShieldCenterEvenIfNoShield().x - ship.getShieldRadiusEvenIfNoShield() - 25f),
                        viewport.convertWorldYtoScreenY(ship.getShieldCenterEvenIfNoShield().y));
                toUse.setBaseColor(Misc.setBrightness(Color.BLACK, alpha));
                toUse.draw(pos.x + 1, pos.y - 1);
                toUse.setBaseColor(Misc.setBrightness(colorToUse, alpha));
                toUse.draw(pos);
            }
        }
    }

    @Override
    public @NotNull String getDroneVariant() {
        return "sotf_volitionswarm_wing";
    }

    @Override
    public DroneFormation getDroneFormation() {
        return new SotfSiriusFollowerFormation();
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
