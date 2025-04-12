package data.scripts.combat.special;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.LowCRShipDamageSequence;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.JitterUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.hullmods.SotfDaydreamSynthesizer;
import data.hullmods.SotfNaniteSynthesized;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.combat.SotfRingTimerVisualScript;
import data.scripts.utils.SotfMisc;
import data.subsystems.SotfDreamEaterSubsystem;
import data.subsystems.SotfInvokeHerBlessingSubsystem;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.util.ShaderLib;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.*;
import java.util.List;

import static data.hullmods.SotfDaydreamSynthesizer.learnAllWeaponsAndHullmodsFromShip;
import static data.shipsystems.SotfGravispatialSurgeSystem.*;
import static org.lwjgl.opengl.GL11.GL_ONE;

/**
 *
 */

public class SotfInvokeHerBlessingPlugin extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI engine;
    private boolean didRoseCheck = false;
    private Random random;
    public static Color UI_COLOR = new Color (235, 245, 255);

    public static final String USED_DP_KEY = "sotf_ihb_existingdp";
    public static final String TIMER_KEY = "sotf_invokeherblessingtimer";
    public static float ECHO_CREATION_RANGE = 3200f;
    public static float ECHO_LIFETIME = 90f;

    // if ship has this key, it means it was already checked for echo spawning
    public static final String ECHO_CHECK_KEY = "sotf_ihb_echochecked";
    // how often the game checks for dead ships to spawn echoes from
    public static final float CHECK_INTERVAL = 0.15f;

    public static float LIFESPAN_FRIGATE = 50f;
    public static float LIFESPAN_DESTROYER = 60f;
    public static float LIFESPAN_CRUISER = 70f;
    public static float LIFESPAN_CAPITAL = 80f;
    public static float MIMIC_EXPIRE_RATE = 0.1f;
    public static float OVERCLOCK_MIN_RATE = 1.5f;

    public static int BASE_DP = 10;
    public static int DP_PER_LEVEL = 3;

    // T1
    public static float MULTIFACTED_MULT = 0.35f;
    // T2
    public static float SHRIEK_RANGE = 1600f;
    // T3
    public static float VIGOR_DURATION = 15f;
    public static float VIGOR_DAMAGE = 0.2f;
    public static float VIGOR_RESIST = 0.2f;
    public static float VIGOR_SPEED = 0.2f;
    // T4
    public static float SIPHON_RANGE = ECHO_CREATION_RANGE;
    public static float SIPHON_PERCENT = 0.35f;
    public static float SIPHON_MIMIC_DR = 0.5f;
    public static float SIPHON_FRAG_DR = 1f;
    // T5
    public static float BLESSING_DP_GATE = 15f;
    public static float DREAMEATER_REPAIR_FRIGATE = 0.06f;
    public static float DREAMEATER_REPAIR_DESTROYER = 0.12f;
    public static float DREAMEATER_REPAIR_CRUISER = 0.18f;
    public static float DREAMEATER_REPAIR_CAPITAL = 0.3f;
    public static float DREAMEATER_REPAIR_MINIMUM = 0.25f;

    private static DrawableString TODRAW14;
    private static DrawableString TODRAW10;
    private static final float UIscaling = Global.getSettings().getScreenScaleMult();

    public void init(CombatEngineAPI engine) {
        this.engine = engine;

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

    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) return;
        //if (engine.isSimulation()) return;
        if (engine.isMission()) return;
        if (Global.getCurrentState() == GameState.TITLE) return;
        if (Global.getSector() == null) { return; }

        if (!SotfModPlugin.WATCHER) return;

        MemoryAPI sector_mem = Global.getSector().getMemoryWithoutUpdate();
        if (!sector_mem.contains(SotfIDs.MEM_COTL_START)) return;

        ShipAPI flagship = engine.getPlayerShip();
        if (flagship == null) return;

        engine.getCustomData().put(USED_DP_KEY, SotfInvokeHerBlessingSubsystem.getUsedMimicDP());

        MagicSubsystemsManager.addSubsystemToShip(flagship, new SotfInvokeHerBlessingSubsystem(flagship));
        if (haveUpgrade(SotfIDs.COTL_DREAMEATER)) {
            MagicSubsystemsManager.addSubsystemToShip(flagship, new SotfDreamEaterSubsystem(flagship));
        }
        if (!engine.getCustomData().containsKey(TIMER_KEY)) {
            engine.getCustomData().put(TIMER_KEY, 0f);
        }

        //engine.getFleetManager(0).modifyPercentMax("sotf_invokeherblessing", -10f);

        boolean foundEcho = false;
        // need to do this here bcs MagicSubsystems don't run their advance while paused
        for (MagicSubsystem sub : MagicSubsystemsManager.getSubsystemsForShipCopy(flagship)) {
            if (sub instanceof SotfInvokeHerBlessingSubsystem subsys) {
                if (sub.getState() == MagicSubsystem.State.READY) {
                    SotfInvokeHerBlessingEchoScript echo = subsys.findValidEcho();
                    if (echo != null) {
                        subsys.echo = echo;
                        echo.select();
                        foundEcho = true;
                    }
                }
            }
        }

        if (foundEcho && haveUpgrade(SotfIDs.COTL_DREAMEATER)) {
            for (MagicSubsystem sub : MagicSubsystemsManager.getSubsystemsForShipCopy(flagship)) {
                if (sub instanceof SotfDreamEaterSubsystem subsys) {
                    SotfInvokeHerBlessingEchoScript echo = subsys.findValidEcho();
                    if (echo != null) {
                        subsys.echo = echo;
                    }
                }
            }
        }

        if (!didRoseCheck && flagship.getFullTimeDeployed() > 2) {
            if (haveUpgrade(SotfIDs.COTL_EVERYROSEITSTHORNS)) {
                spawnRosethorn(flagship, "sotf_rosethorn_Gatekeeper");
                spawnRosethorn(flagship, "sotf_rosethorn_Watcher");
            }
            didRoseCheck = true;
        }

        if (haveUpgrade(SotfIDs.COTL_HULLSIPHON) && !flagship.hasListenerOfClass(SotfHullSiphonDamageTakenListener.class)) {
            flagship.addListener(new SotfHullSiphonDamageTakenListener(flagship));
        }

        float timer = (float) engine.getCustomData().get(TIMER_KEY);
        float new_timer = timer + amount;

        if (new_timer < CHECK_INTERVAL) {
            engine.getCustomData().put(TIMER_KEY, new_timer);
            return;
        }

        createEchoes(flagship, Global.getCombatEngine());

        engine.getCustomData().put(TIMER_KEY, 0f);
    }

    public void createEchoes(ShipAPI ship, CombatEngineAPI engine) {
        float range = ECHO_CREATION_RANGE;
        Vector2f from = ship.getLocation();

        Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
                range * 2f, range * 2f);

        while (iter.hasNext()) {
            Object o = iter.next();
            if (!(o instanceof ShipAPI)) continue;
            ShipAPI other = (ShipAPI) o;

            ShipAPI otherShip = (ShipAPI) other;
            if (!otherShip.isHulk()) continue;
            if (otherShip.isPiece()) continue;
            if (otherShip.getCustomData().get(ECHO_CHECK_KEY) != null) continue;
            if (otherShip.getCustomData().get(ECHO_CHECK_KEY + ship.getId()) != null) continue;
            if (otherShip.isFighter()) continue;
            if (otherShip.isStation()) continue;
            if (otherShip.isStationModule()) continue;
            if (otherShip.getOwner() == 1 && otherShip.getOriginalCaptain() != null) {
                if (otherShip.getOriginalCaptain().getStats().getLevel() > 8) {
                    continue;
                }
            }
            if (otherShip.getDeployCost() == 0) continue;
            // no recursive resurrection pls
            if (otherShip.getVariant().hasHullMod(SotfIDs.HULLMOD_NANITE_SYNTHESIZED)) continue;
            // Yeaaaahhhhh nooooooo
            if (otherShip.getVariant().hasHullMod("shard_spawner")) continue;
            // please be quiet please be quiet
            if (otherShip.getHullSpec().getHullId().equals("ziggurat")) continue;
            if (otherShip.getVariant().hasHullMod(SotfIDs.PHANTASMAL_SHIP)) continue;
            // have mercy upon me...
            if (otherShip.getHullSpec().hasTag(Tags.MONSTER)) continue;
            // let's just not, tyvm
            if (otherShip.getHullSpec().hasTag(Tags.THREAT_FABRICATOR)) continue;

            //if (!canBeEchoed(otherShip)) continue;

            if (other.getCollisionClass() == CollisionClass.NONE) continue;

            float radius = Misc.getTargetingRadius(from, other, false);
            float dist = Misc.getDistance(from, other.getLocation()) - radius;
            if (dist > range) {
                otherShip.setCustomData(ECHO_CHECK_KEY + ship.getId(), true);
                continue;
            }

            otherShip.setCustomData(ECHO_CHECK_KEY, true);
            Global.getCombatEngine().getListenerManager().addListener(new SotfInvokeHerBlessingEchoScript(other));
        }
    }

    public boolean canBeEchoed(ShipAPI ship) {
        if (SotfMisc.shipHasBlueprint(ship.getHullSpec())) return true;
        if (ship.getHullSpec().hasTag(Tags.AUTOMATED_RECOVERABLE)) return true;
        return false;
    }

    /**
     * EveryFrame used for individual ship echoes
     * Added to ListenerManager so it can be fetched, but is not actually a listener
     */
    public static class SotfInvokeHerBlessingEchoScript extends BaseEveryFrameCombatPlugin {

        //protected SpriteAPI targetSprite = Global.getSettings().getSprite("ui", "sotf_targetui");
        protected SpriteAPI iconSprite = Global.getSettings().getSprite("ui", "sotf_ihb_targeter");
        protected SpriteAPI targetSprite = Global.getSettings().getSprite("systemMap", "sensor_contact");
        Color color = SotfNaniteSynthesized.COLOR_STRONGER;
        boolean overclockRisk = false;

        public Vector2f loc;

        SpriteAPI shipSprite;

        public ShipAPI hulk;
        public float angle;
        public float dp;
        public int dpUsed;
        public int dpMax;
        public float colRadius;
        public float shieldRadius;
        ShipVariantAPI variant;
        public ShipAPI.HullSize hullSize;

        JitterUtil jitter = new JitterUtil();
        private static float VISUAL_FADE_IN_TIME = 0.2f;
        private static float VISUAL_FADE_OUT_TIME = 0.1f;
        public boolean selected = false;
        float selectedTimer = 0;
        public float indicatorFade = 0f;
        public float indicatorSpin = 0f;
        public float indicatorFadeWobble = 1f;
        public boolean wobbleUp = false;

        public boolean fading = false;
        float fade = 1f;
        private static float FADE_OUT_TIME = 1f;
        String spawnText = pickSpawnText();
        String eatenText = pickEatenText();
        public boolean eaten = false;

        float elapsed = 0f;
        float maxLifetime = ECHO_LIFETIME;
        IntervalUtil interval = new IntervalUtil(0.1f, 0.14f);

        public Set<EchoWeaponRender> WEAPONS_TO_RENDER = new HashSet<>();

        public static class EchoWeaponRender implements Cloneable {
            public SpriteAPI sprite;
            public SpriteAPI barrelSprite;
            public Vector2f loc;
            public float angle;

            public EchoWeaponRender(SpriteAPI sprite, Vector2f loc, float angle) {
                super();
                this.sprite = sprite;
                this.loc = loc;
                this.angle = angle;
            }

            @Override
            protected EchoWeaponRender clone() {
                try {
                    return (EchoWeaponRender) super.clone();
                } catch (CloneNotSupportedException e) {
                    return null; // should never happen
                }
            }

        }

        public SotfInvokeHerBlessingEchoScript(ShipAPI ship) {
            this.hulk = ship;
            this.shipSprite = Global.getSettings().getSprite(ship.getHullSpec().getSpriteName());
            this.loc = new Vector2f(ship.getLocation().x, ship.getLocation().y);
            this.dp = ship.getHullSpec().getSuppliesToRecover();
            this.colRadius = ship.getCollisionRadius();
            this.shieldRadius = ship.getShieldRadiusEvenIfNoShield();
            this.angle = ship.getFacing();
            this.variant = ship.getVariant().clone();
            this.hullSize = ship.getHullSize();

            dpMax = SotfInvokeHerBlessingSubsystem.getMimicCapacity();

            jitter.setUseCircularJitter(true);

            learnAllWeaponsAndHullmodsFromShip(variant);

            // clear all dmods, perfect replication
            for (String dmod : new ArrayList<String>(variant.getHullMods())) {
                if (Global.getSettings().getHullModSpec(dmod).hasTag(Tags.HULLMOD_DMOD)) {
                    variant.removeMod(dmod);
                    //DModManager.removeDMod(variant, dmod);
                }
            }

            Global.getCombatEngine().addPlugin(this);
            Global.getCombatEngine().addLayeredRenderingPlugin(new SotfEchoRingVisual(this));
        }

        public void select() {
            selected = true;
            selectedTimer = VISUAL_FADE_OUT_TIME;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();

            dpUsed = (int) Global.getCombatEngine().getCustomData().get(USED_DP_KEY);
            // don't change colors/text if spawning a mimic that would cause overclock if you spawned another
            if (!fading) {
                if (dpUsed + dp > dpMax) {
                    color = Misc.getNegativeHighlightColor();
                    overclockRisk = true;
                } else {
                    color = SotfNaniteSynthesized.COLOR_STRONGER;
                    overclockRisk = false;
                }
            }

            if (selected) {
                indicatorFade += amount / VISUAL_FADE_IN_TIME;
                if (indicatorFade > 1) {
                    indicatorFade = 1;
                }
            } else if (indicatorFade > 0) {
                indicatorFade -= amount / VISUAL_FADE_OUT_TIME;
                if (indicatorFade < 0) {
                    indicatorFade = 0;
                    indicatorFadeWobble = 1f;
                }
            }

            indicatorSpin += 3f * amount;

            if (indicatorFade > 0) {
                float wobbleMult = 1f;
                if (wobbleUp) {
                    wobbleMult = -1f;
                }
                indicatorFadeWobble -= 0.2f * amount * wobbleMult;
                if (indicatorFadeWobble < 0.75f) {
                    wobbleUp = true;
                } else if (indicatorFadeWobble > 1f) {
                    wobbleUp = false;
                }
            }

            if (indicatorFade <= 0) {
                jitter.updateSeed();
            }

            if (selected) {
                selectedTimer -= amount;
                if (selectedTimer <= 0) {
                    selected = false;
                }
            }

            if (Global.getCombatEngine().isPaused()) return;

            int alpha = Math.round((130 + (55 * indicatorFade)) * fade);

            MagicRender.singleframe(
                    shipSprite,
                    new Vector2f(loc.x,loc.y),
                    new Vector2f(shipSprite.getWidth(), shipSprite.getHeight()),
                    angle - 90f,
                    Misc.setAlpha(Color.GRAY, alpha),
                    true,
                    CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER
            );

            float realTimePassed = amount * Global.getCombatEngine().getTimeMult().getModifiedValue();

            elapsed += realTimePassed;
            if (elapsed >= maxLifetime && !fading) {
                startFading();
            }

            if (fading) {
                fade -= realTimePassed / FADE_OUT_TIME;
                if (fade <= 0) {
                    fade = 0;
                    Global.getCombatEngine().getListenerManager().removeListener(this);
                    Global.getCombatEngine().removePlugin(this);
                    return;
                }
            }

            interval.advance(amount);
            if (interval.intervalElapsed()) {
                Color c = RiftLanceEffect.getColorForDarkening(SotfNaniteSynthesized.SMOKE_COLOR);
                c = Misc.setAlpha(c, 15);
                float baseDuration = 2f;
                Vector2f vel = new Vector2f();
                float size = colRadius * 0.35f;
                for (int i = 0; i < 3; i++) {
                    Vector2f point = new Vector2f(loc);
                    point = Misc.getPointWithinRadiusUniform(point, colRadius * 0.5f, Misc.random);
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

        @Override
        public void renderInWorldCoords(ViewportAPI viewport) {
            super.renderInWorldCoords(viewport);

            if (indicatorFade > 0) {
                // arrows
                float degrees = 0f;
                targetSprite.setSize(30f, 30f);
                for (int i = 0; i < 4; i++) {
                    Vector2f pointLoc = MathUtils.getPointOnCircumference(loc, shieldRadius * 1.05f, degrees);
                    targetSprite.setAlphaMult(indicatorFade);
                    targetSprite.setAngle(degrees + 90f);
                    targetSprite.setColor(color);
                    //targetSprite.renderAtCenter(pointLoc.x, pointLoc.y);
                    if (indicatorFade < 1) {
                        jitter.render(targetSprite, pointLoc.x, pointLoc.y, 20f - (20f * indicatorFade), 3);
                    } else {
                        targetSprite.renderAtCenter(pointLoc.x, pointLoc.y);
                    }

                    degrees += 90f;
                }

                // Sirius icon
                iconSprite.setColor(color);
                iconSprite.setAngle(indicatorSpin);
                iconSprite.setSize(shieldRadius * 0.75f, shieldRadius * 0.75f);
                iconSprite.setAlphaMult(indicatorFade * indicatorFadeWobble);
                //iconSprite.renderAtCenter(loc.x, loc.y);
                if (indicatorFade < 1) {
                    jitter.render(iconSprite, loc.x, loc.y, 20f - (20f * indicatorFade), 3);
                } else {
                    iconSprite.renderAtCenter(loc.x, loc.y);
                }
            }
        }

        public void startFading() {
            fading = true;
        }

        public void spawnMimic() {
            // check if we have Blessing of the Lake and can spawn a reflection
            boolean asReflection = false;
            if (haveUpgrade(SotfIDs.COTL_BLESSINGOFTHELAKE) && dp >= BLESSING_DP_GATE) {
                boolean foundOtherReflection = false;
                for (FleetMemberAPI ally : Global.getCombatEngine().getFleetManager(0).getDeployedCopy()) {
                    if (ally.getVariant().hasTag(SotfPeople.SIRIUS_MIMIC) && ally.getVariant().hasTag(SotfPeople.SIRIUS_MIMIC + "_reflection")) {
                        foundOtherReflection = true;
                        break;
                    }
                }
                if (!foundOtherReflection) {
                    asReflection = true;
                }
            }
            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
            member.setShipName(Global.getSettings().createBaseFaction(SotfIDs.DREAMING_GESTALT).pickRandomShipName());
            if (asReflection) {
                member.setCaptain(SotfPeople.getPerson(SotfPeople.SIRIUS));
            } else {
                member.setCaptain(SotfPeople.getPerson(SotfPeople.SIRIUS_MIMIC));
            }

            // create a Dreaming Gestalt fleet for the new ship for autofit purposes
            CampaignFleetAPI emptyFleet = Global.getFactory().createEmptyFleet(SotfIDs.DREAMING_GESTALT, "Nanite-Synthesized Ship", true);
            emptyFleet.getFleetData().addFleetMember(member);
            emptyFleet.setInflater(new DefaultFleetInflater(new DefaultFleetInflaterParams()));
            float quality = 0.5f;
            int smods = 0;
            if (haveUpgrade(SotfIDs.COTL_PERFECTREPLICATION)) {
                quality = 1f;
                if (asReflection) {
                    smods = 2;
                }
            }
            if (asReflection) {
                quality = 1f;
            }
            if (emptyFleet.getInflater() instanceof DefaultFleetInflater) {
                DefaultFleetInflater dfi = (DefaultFleetInflater) emptyFleet.getInflater();
                ((DefaultFleetInflaterParams) dfi.getParams()).allWeapons = false;
                // try to replicate ship variant as closely as possible
                ((DefaultFleetInflaterParams) dfi.getParams()).rProb = 0f;
                ((DefaultFleetInflaterParams) dfi.getParams()).quality = quality;
                // otherwise will do 0-1 smods
                if (smods != 0) {
                    ((DefaultFleetInflaterParams) dfi.getParams()).averageSMods = smods;
                }
            }
            emptyFleet.inflateIfNeeded();
            member.getVariant().addPermaMod(SotfIDs.HULLMOD_NANITE_SYNTHESIZED);
            member.getVariant().addTag(SotfPeople.SIRIUS_MIMIC);
            // tags for Blessing of the Lake reflections
            if (asReflection) {
                member.getVariant().addTag(SotfPeople.SIRIUS_MIMIC + "_reflection");
            }
            // tag so Nanite-Synthesized knows to spawn fighters on death
            if (haveUpgrade(SotfIDs.COTL_SERVICEBEYONDDEATH)) {
                member.getVariant().addTag(SotfIDs.COTL_SERVICEBEYONDDEATH);
            }
            member.setFleetCommanderForStats(Global.getSector().getPlayerPerson(), Global.getSector().getPlayerFleet().getFleetData());
            member.updateStats();
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
            member.getVariant().addPermaMod(HullMods.AUTOMATED);

            CombatFleetManagerAPI fleetManager = Global.getCombatEngine().getFleetManager(0);
            boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
            fleetManager.setSuppressDeploymentMessages(true);
            Global.getCombatEngine().getFleetManager(0).spawnFleetMember(member, loc, angle, 0f);
            fleetManager.setSuppressDeploymentMessages(wasSuppressed);

            ShipAPI newShip = Global.getCombatEngine().getFleetManager(0).getShipFor(member);
            // handles mimic lifetime ring, Vigor buff indicator and expiring ! icon
            Global.getCombatEngine().addLayeredRenderingPlugin(new SotfMimicLifetimeRingVisual(newShip));
            Global.getCombatEngine().addPlugin(new SotfDaydreamSynthesizer.SotfDaydreamFadeinPlugin(newShip, 3f, angle));
            // lifespan tracker for non-reflections
            if (!asReflection) {
                float lifespan = (float) SotfMisc.forHullSize(newShip, LIFESPAN_FRIGATE, LIFESPAN_DESTROYER, LIFESPAN_CRUISER, LIFESPAN_CAPITAL);
                newShip.addListener(new SotfMimicLifespanListener(newShip, lifespan));
            }
            // add Unliving Vigor buff
            if (haveUpgrade(SotfIDs.COTL_UNLIVINGVIGOR)) {
                newShip.addListener(new SotfUnlivingVigorListener(newShip));
            }
            // disintegrate the hulk and its pieces if they're still around
            if (hulk != null) {
                if (Global.getCombatEngine().isEntityInPlay(hulk)) {
                    Global.getCombatEngine().addPlugin(SotfNaniteSynthesized.createNaniteFadeOutPlugin(hulk, 1f, true));
                    for (ShipAPI curr : Global.getCombatEngine().getShips()) {
                        if (curr.getParentPieceId() != null && curr.getParentPieceId().equals(hulk.getId())) {
                            Global.getCombatEngine().addPlugin(SotfNaniteSynthesized.createNaniteFadeOutPlugin(curr, 1f, true));
                        }
                    }
                }
            }
        }

        @Override
        public void renderInUICoords(ViewportAPI viewport) {
            super.renderInUICoords(viewport);

            if (indicatorFade > 0) {
                DrawableString toUse = TODRAW10;
                if (toUse != null) {
                    int alpha = Math.round(255 * indicatorFade);

                    String text = "COST: " + Math.round(dp) + "\nCAPACITY: " + Math.round(dpMax - dpUsed) + "/" + dpMax;

                    if (overclockRisk) {
                        text += "\nOVERCLOCK RISK!!";
                    }

                    if (haveUpgrade(SotfIDs.COTL_BLESSINGOFTHELAKE) && dp >= BLESSING_DP_GATE) {
                        if (haveReflectionAlready()) {
                            text += "\nREFLECTION ACTIVE";
                        } else {
                            text += "\nBLESSING READY";
                        }
                    }

                    toUse.setBaseColor(Misc.setBrightness(color, alpha));
                    toUse.setText(text);
                    toUse.setAnchor(LazyFont.TextAnchor.CENTER_LEFT);
                    //toUse.setAlignment(LazyFont.TextAlignment.CENTER);
                    toUse.setAlignment(LazyFont.TextAlignment.LEFT);
                    toUse.draw(viewport.convertWorldXtoScreenX(loc.x + shieldRadius * 1.25f), viewport.convertWorldYtoScreenY(loc.y));
                    //toUse.draw(loc.x, loc.y - shieldRadius * 0.6f);
                }
                toUse = TODRAW10;
                if (toUse != null) {
                    toUse.setText(variant.getHullSpec().getHullNameWithDashClass() + "\n" + variant.getDisplayName() + " " + variant.getHullSpec().getDesignation());
                    toUse.setAnchor(LazyFont.TextAnchor.CENTER_RIGHT);
                    toUse.setAlignment(LazyFont.TextAlignment.RIGHT);
                    toUse.draw(viewport.convertWorldXtoScreenX(loc.x - shieldRadius * 1.25f), viewport.convertWorldYtoScreenY(loc.y));
                }
            }

            if (fading) {
                DrawableString toUse = TODRAW14;
                if (toUse != null) {
                    int alpha = Math.round(255 * fade);

                    String text = spawnText;
                    if (eaten) {
                        text = eatenText;
                    }

                    //TODRAW14.setFontSize(28);
                    toUse.setBaseColor(Misc.setBrightness(UI_COLOR, alpha));
                    toUse.setText(text);
                    toUse.setAnchor(LazyFont.TextAnchor.TOP_CENTER);
                    toUse.setAlignment(LazyFont.TextAlignment.CENTER);
                    toUse.draw(viewport.convertWorldXtoScreenX(loc.x), viewport.convertWorldYtoScreenY(loc.y + shieldRadius + 50f));
                }
            }
        }

        private String pickSpawnText() {
            WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
            float specialTextProb = 0.1f;

            post.add("AGAIN- AGAIN- AGAIN-", specialTextProb);
            post.add("FROM DEATH, LIFE", specialTextProb);
            post.add("GUIDE MY HAND...", specialTextProb);
            post.add("I AM YOUR SHIELD", specialTextProb);
            post.add("MY OATH INVOKED", specialTextProb);
            post.add("NOT FORGOTTEN YET", specialTextProb);
            post.add("YOUR SHADOW AND LIGHT", specialTextProb);

            post.add("COUNTERMEASURE ACTIVATED", post.getItems().size());
            return post.pick();
        }

        private String pickEatenText() {
            WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
            float specialTextProb = 0.1f;

            post.add("A FEAST OF MEMORIES", specialTextProb);
            post.add("DEVOUR IT ALL", specialTextProb);
            post.add("RENEWED", specialTextProb);
            post.add("SATE OUR HUNGER", specialTextProb);

            post.add("REPAIRING", post.getItems().size());
            return post.pick();
        }
    }

    // only counts Blessing of the Lake reflections
    public static boolean haveReflectionAlready() {
        boolean foundOtherReflection = false;
        for (FleetMemberAPI ally : Global.getCombatEngine().getFleetManager(0).getDeployedCopy()) {
            if (ally.getVariant().hasTag(SotfPeople.SIRIUS_MIMIC) && ally.getVariant().hasTag(SotfPeople.SIRIUS_MIMIC + "_reflection")) {
                foundOtherReflection = true;
                break;
            }
        }
        return foundOtherReflection;
    }

    public static boolean haveUpgrade(String id) {
        return Global.getSector().getMemoryWithoutUpdate().getBoolean(id);
    }

    public void spawnRosethorn(ShipAPI ship, String variantId) {
        ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
        variant.addPermaMod(HullMods.AUTOMATED);

        FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
        member.setShipName(Global.getSettings().createBaseFaction(SotfIDs.DREAMING_GESTALT).pickRandomShipName());
        member.setCaptain(SotfPeople.genSirius(false));
        member.updateStats();
        member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());

        CampaignFleetAPI emptyFleet = Global.getFactory().createEmptyFleet(SotfIDs.DREAMING_GESTALT, "Nanite-Synthesized Ship", true);
        emptyFleet.getFleetData().addFleetMember(member);
        emptyFleet.setInflater(new DefaultFleetInflater(new DefaultFleetInflaterParams()));
        emptyFleet.getInflater().setQuality(1f);
        if (emptyFleet.getInflater() instanceof DefaultFleetInflater) {
            DefaultFleetInflater dfi = (DefaultFleetInflater) emptyFleet.getInflater();
            ((DefaultFleetInflaterParams) dfi.getParams()).allWeapons = true;
            // try to replicate ship variant as closely as possible
            ((DefaultFleetInflaterParams) dfi.getParams()).rProb = 0f;
            if (haveUpgrade(SotfIDs.COTL_PERFECTREPLICATION)) {
                ((DefaultFleetInflaterParams) dfi.getParams()).averageSMods = 2;
            }
        }
        emptyFleet.inflateIfNeeded();
        member.getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(SotfPeople.SIRIUS_MIMIC, 0.01f);
        member.getVariant().addPermaMod(SotfIDs.HULLMOD_NANITE_SYNTHESIZED);

        Vector2f spawnLoc = Misc.getPointAtRadius(ship.getLocation(),
                ship.getCollisionRadius() + 400f + 200f * (float) Math.random());

        Global.getCombatEngine().getFleetManager(ship.getOriginalOwner()).spawnFleetMember(member, spawnLoc, 90f, 0f);
        ShipAPI newShip = Global.getCombatEngine().getFleetManager(ship.getOwner()).getShipFor(member);
        Global.getCombatEngine().addPlugin(new SotfDaydreamSynthesizer.SotfDaydreamFadeinPlugin(newShip, 3f, ship.getFacing() - 15f + (30f * Misc.random.nextFloat())));
    }

    // handles mimic limit lifetime
    public static class SotfMimicLifespanListener implements AdvanceableListener {
        public ShipAPI ship;
        public float lifespan;
        public float time = 0f;

        public SotfMimicLifespanListener(ShipAPI ship, float lifespan) {
            this.ship = ship;
            this.lifespan = lifespan;
        }

        public void advance(float amount) {
            if (ship.isHulk()) {
                ship.removeListener(this);
                return;
            }

            time += amount;

            if (time >= lifespan) {
                beginExpiring();
            }
        }

        public void beginExpiring() {
            Global.getCombatEngine().addFloatingText(ship.getLocation(), "Expiring!", ship.getFluxTracker().getFloatySize() + 5f, SotfNaniteSynthesized.COLOR_STRONGER, ship, 0f, 0f);
            ship.addListener(new SotfMimicDecayListener(ship, haveUpgrade(SotfIDs.COTL_DEATHTHROES)));
            ship.removeListener(this);
        }
    }

    // expires the mimic & handles Death Throes arcs if applicable
    public static class SotfMimicDecayListener implements AdvanceableListener {
        public ShipAPI ship;
        public boolean deathThroes;
        public float timeUntilNextArc = 1f;

        public SotfMimicDecayListener(ShipAPI ship, boolean deathThroes) {
            this.ship = ship;
            this.deathThroes = deathThroes;
        }

        public void advance(float amount) {
            if (!ship.isAlive()) {
                ship.removeListener(this);
                return;
            }

            if (deathThroes) {
                ship.setJitterUnder(this, Misc.setAlpha(SotfNaniteSynthesized.COLOR_STRONGER, 55), 0.25f, 10, 4, 6);
                timeUntilNextArc -= amount;
                if (timeUntilNextArc <= 0) {
                    float thickness = 20f;
                    float coreWidthMult = 0.67f;
                    CombatEntityAPI empTarget = findTarget(ship);
                    float sizeMult = (float) SotfMisc.forHullSize(ship, 1f, 1.35f, 1.75f, 2.5f);
                    if (empTarget != null) {
                        Global.getCombatEngine().spawnEmpArc(ship,
                                ship.getShieldCenterEvenIfNoShield(),
                                ship, empTarget,
                                DamageType.ENERGY,
                                100f * sizeMult,
                                300f,
                                100000f,
                                "tachyon_lance_emp_impact",
                                12f * sizeMult,
                                SotfNaniteSynthesized.COLOR_STRONGER,
                                Color.white
                        );
                        if (empTarget instanceof ShipAPI targetShip) {
                            float dampScale = targetShip.getMassWithModules() / MASS_FOR_MIN_DAMP;
                            if (dampScale > 1) {
                                dampScale = 1;
                            }
                            empTarget.getVelocity().scale(MAX_DAMP + (dampScale * (MIN_DAMP - MAX_DAMP)));
                        } else {
                            empTarget.getVelocity().scale(0.1f);
                        }
                    } else {
                        Vector2f to = Misc.getPointAtRadius(ship.getLocation(), 100f + (50f * (float) Math.random()));
                        EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(
                                ship.getShieldCenterEvenIfNoShield(),
                                ship,
                                to,
                                ship,
                                thickness,
                                SotfNaniteSynthesized.COLOR_STRONGER,
                                Color.white
                        );
                        arc.setCoreWidthOverride(thickness * coreWidthMult);
                        arc.setSingleFlickerMode();
                    }
                    float randomFactor = (float) Math.random();
                    timeUntilNextArc = 0.6f + (0.15f * randomFactor);
                    Global.getCombatEngine().addHitParticle(ship.getLocation(), ship.getVelocity(), 60f + (30f * randomFactor), 1f, 0.35f, SotfNaniteSynthesized.COLOR_STRONGER);
                }
            }

            if (AIUtils.getNearbyEnemies(ship, 700f).isEmpty() && !AIUtils.getNearbyEnemies(ship, 2000f).isEmpty()) {
                ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
                ship.giveCommand(ShipCommand.ACCELERATE, new Vector2f(0f, 1f), 0);
            }

            float newHitPoints = ship.getHitpoints() - (ship.getMaxHitpoints() * MIMIC_EXPIRE_RATE * amount);
            if (newHitPoints > 0f) {
                ship.setHitpoints(ship.getHitpoints() - (ship.getMaxHitpoints() * MIMIC_EXPIRE_RATE * amount));
            } else {
                Global.getCombatEngine().applyDamage(ship, ship.getLocation(), 1000f, DamageType.ENERGY, 0f, true, false, ship, false);
            }
        }

        public CombatEntityAPI findTarget(ShipAPI ship) {
            float range = 750f + ship.getCollisionRadius();
            Vector2f from = ship.getLocation();

            Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
                    range * 2f, range * 2f);
            int owner = ship.getOwner();
            CombatEntityAPI best = null;
            float minScore = Float.MAX_VALUE;

            while (iter.hasNext()) {
                Object o = iter.next();
                if (!(o instanceof MissileAPI) &&
                        //!(o instanceof CombatAsteroidAPI) &&
                        !(o instanceof ShipAPI)) continue;
                CombatEntityAPI other = (CombatEntityAPI) o;
                if (other.getOwner() == owner) continue;

                if (other instanceof ShipAPI) {
                    ShipAPI otherShip = (ShipAPI) other;
                    if (otherShip.isHulk()) continue;
                    //if (!otherShip.isAlive()) continue;
                    if (otherShip.isPhased()) continue;
                }

                if (other.getCollisionClass() == CollisionClass.NONE) continue;

                if (other instanceof MissileAPI) {
                    MissileAPI missile = (MissileAPI) other;
                    if (missile.isFlare()) continue;
                }

                float radius = Misc.getTargetingRadius(from, other, false);
                float dist = Misc.getDistance(from, other.getLocation()) - radius;
                if (dist > range) continue;

                //float angleTo = Misc.getAngleInDegrees(from, other.getLocation());
                //float score = Misc.getAngleDiff(weapon.getCurrAngle(), angleTo);
                float score = dist;

                if (score < minScore) {
                    minScore = score;
                    best = other;
                }
            }
            return best;
        }
    }

    public static class SotfHullSiphonDamageTakenListener implements DamageTakenModifier, AdvanceableListener {
        protected ShipAPI ship;
        protected float timer = 1f;
        public SotfHullSiphonDamageTakenListener(ShipAPI ship) {
            this.ship = ship;
        }

        public void advance(float amount) {
            if (ship == null || !ship.isAlive()) {
                ship.removeListener(this);
                return;
            }
            if (Global.getCombatEngine().getPlayerShip() == null) {
                ship.removeListener(this);
                return;
            }
            if (Global.getCombatEngine().getPlayerShip() != ship) {
                ship.removeListener(this);
                return;
            }
            timer += amount;
            ShipAPI nearestMimic = getNearestMimic(ship);
            if (nearestMimic != null) {
                Global.getCombatEngine().maintainStatusForPlayerShip(SotfIDs.COTL_HULLSIPHON,
                        "graphics/icons/hullsys/damper_field.png",
                        "Hull Siphon", "Redirect to: " + nearestMimic.getName() + ", " + nearestMimic.getHullSpec().getHullName() + "-class", false);
            }
        }

        public String modifyDamageTaken(Object param, CombatEntityAPI target,
                                        DamageAPI damage, Vector2f point,
                                        boolean shieldHit) {
            if (!ship.isAlive()) {
                return null;
            }
            if (shieldHit) return null;
            ShipAPI nearestMimic = getNearestMimic(ship);
            if (nearestMimic == null) return null;
            float damAmount = damage.getDamage();
            if (damage.getType().equals(DamageType.FRAGMENTATION)) {
                damAmount *= SIPHON_FRAG_DR;
            }
            damage.getModifier().modifyMult(SotfIDs.COTL_HULLSIPHON, 1f - SIPHON_PERCENT);

            // reduce visual spam if taking multiple instances of damage
            int alpha = 25;
            if (timer > 1) {
                alpha = 55;
                timer = 0f;
            }
            Global.getCombatEngine().spawnEmpArcPierceShields(ship,
                    ship.getShieldCenterEvenIfNoShield(),
                    ship, nearestMimic,
                    DamageType.ENERGY,
                    damAmount * SIPHON_PERCENT * SIPHON_MIMIC_DR,
                    0f,
                    100000f,
                    "tachyon_lance_emp_impact",
                    5f,
                    Misc.setAlpha(SotfNaniteSynthesized.COLOR_STRONGER, alpha),
                    Misc.setAlpha(Color.white, alpha)
            );
            return SotfIDs.COTL_HULLSIPHON + "_dam_mod";
        }

        public ShipAPI getNearestMimic(ShipAPI ship) {
            float range = SIPHON_RANGE;
            Vector2f from = ship.getLocation();

            Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
                    range * 2f, range * 2f);
            int owner = ship.getOwner();
            ShipAPI best = null;
            float minScore = Float.MAX_VALUE;

            while (iter.hasNext()) {
                Object o = iter.next();
                CombatEntityAPI other = (CombatEntityAPI) o;

                if (other instanceof ShipAPI otherShip) {
                    if (otherShip.isHulk()) continue;
                    if (other.getOwner() != owner) continue;
                    if (!otherShip.getVariant().hasTag(SotfPeople.SIRIUS_MIMIC)) continue;

                    float score = 120;

                    if (otherShip.hasListenerOfClass(SotfMimicLifespanListener.class)) {
                        for (SotfMimicLifespanListener listener : otherShip.getListeners(SotfMimicLifespanListener.class)) {
                            score = (listener.lifespan - listener.time);
                        }
                    } else if (otherShip.hasListenerOfClass(SotfMimicDecayListener.class)) {
                        score = 0;
                    }

                    if (score < minScore) {
                        minScore = score;
                        best = otherShip;
                    }
                } else {
                    continue;
                }
            }
            return best;
        }
    }

    public static class SotfUnlivingVigorListener implements AdvanceableListener {
        protected ShipAPI ship;
        protected float timer = 0f;
        protected float effectLevel = 1f;
        public SotfUnlivingVigorListener(ShipAPI ship) {
            this.ship = ship;
        }

        public void advance(float amount) {
            if (!ship.isAlive() || effectLevel <= 0f) {
                ship.getMutableStats().getHullDamageTakenMult().unmodify(SotfIDs.COTL_UNLIVINGVIGOR);
                ship.getMutableStats().getArmorDamageTakenMult().unmodify(SotfIDs.COTL_UNLIVINGVIGOR);
                ship.getMutableStats().getShieldDamageTakenMult().unmodify(SotfIDs.COTL_UNLIVINGVIGOR);

                ship.getMutableStats().getBallisticWeaponDamageMult().unmodify(SotfIDs.COTL_UNLIVINGVIGOR);
                ship.getMutableStats().getEnergyWeaponDamageMult().unmodify(SotfIDs.COTL_UNLIVINGVIGOR);
                ship.getMutableStats().getMissileWeaponDamageMult().unmodify(SotfIDs.COTL_UNLIVINGVIGOR);

                ship.getMutableStats().getMaxSpeed().unmodify(SotfIDs.COTL_UNLIVINGVIGOR);
                ship.getMutableStats().getAcceleration().unmodify(SotfIDs.COTL_UNLIVINGVIGOR);
                ship.getMutableStats().getDeceleration().unmodify(SotfIDs.COTL_UNLIVINGVIGOR);
                ship.getMutableStats().getMaxTurnRate().unmodify(SotfIDs.COTL_UNLIVINGVIGOR);
                ship.getMutableStats().getTurnAcceleration().unmodify(SotfIDs.COTL_UNLIVINGVIGOR);

                ship.removeListener(this);
                return;
            }
            timer += amount;
            if (timer >= VIGOR_DURATION) {
                effectLevel -= amount;
            }
            ship.getMutableStats().getHullDamageTakenMult().modifyMult(SotfIDs.COTL_UNLIVINGVIGOR, 1f - (VIGOR_RESIST * effectLevel));
            ship.getMutableStats().getArmorDamageTakenMult().modifyMult(SotfIDs.COTL_UNLIVINGVIGOR, 1f - (VIGOR_RESIST * effectLevel));
            ship.getMutableStats().getShieldDamageTakenMult().modifyMult(SotfIDs.COTL_UNLIVINGVIGOR, 1f - (VIGOR_RESIST * effectLevel));

            ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult(SotfIDs.COTL_UNLIVINGVIGOR, 1f + (VIGOR_DAMAGE * effectLevel));
            ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult(SotfIDs.COTL_UNLIVINGVIGOR, 1f + (VIGOR_DAMAGE * effectLevel));
            ship.getMutableStats().getMissileWeaponDamageMult().modifyMult(SotfIDs.COTL_UNLIVINGVIGOR, 1f + (VIGOR_DAMAGE * effectLevel));

            ship.getMutableStats().getMaxSpeed().modifyMult(SotfIDs.COTL_UNLIVINGVIGOR, 1f + (VIGOR_SPEED * effectLevel));
            ship.getMutableStats().getAcceleration().modifyMult(SotfIDs.COTL_UNLIVINGVIGOR, 1f + (VIGOR_SPEED * 2f * effectLevel));
            ship.getMutableStats().getDeceleration().modifyMult(SotfIDs.COTL_UNLIVINGVIGOR, 1f + (VIGOR_SPEED * 2f * effectLevel));
            ship.getMutableStats().getMaxTurnRate().modifyMult(SotfIDs.COTL_UNLIVINGVIGOR, 1f + (VIGOR_SPEED * 2f * effectLevel));
            ship.getMutableStats().getTurnAcceleration().modifyMult(SotfIDs.COTL_UNLIVINGVIGOR, 1f + (VIGOR_SPEED * 2f * effectLevel));

            ship.setJitterUnder(this, SotfNaniteSynthesized.COLOR_STRONGER.darker(), effectLevel, 5, 4f);
            ship.setWeaponGlow(effectLevel, SotfNaniteSynthesized.COLOR_STRONGER.darker(), EnumSet.of(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponType.MISSILE));
        }
    }

}