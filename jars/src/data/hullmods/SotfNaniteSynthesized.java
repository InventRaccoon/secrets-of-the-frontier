// Ship has Reverie nanite visual flair
package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.util.ColorShifterUtil;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.combat.SotfClingingFlareVisualScript;
//import data.scripts.combat.special.SotfInvokeHerBlessingPlugin;
import org.dark.graphics.plugins.ShipDestructionEffects;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SotfNaniteSynthesized extends BaseHullMod {

    public static String NANITE_SWARM_EXCHANGE_CLASS = "sotf_nanite_swarm_exchange_class";

    public static Color JITTER_COLOR = new Color(50, 60, 68,255);
    public static Color SMOKE_COLOR = new Color(105,125,205,255);

    public static Color COLOR = new Color(235,245,255,255);
    public static Color COLOR_STRONGER = new Color(215,235,255,255);

    public static class SotfNaniteSynthesizedListener implements AdvanceableListener {
        public ShipAPI ship;
        private boolean startedFadingOut = false;
        IntervalUtil interval = new IntervalUtil(0.075f, 0.125f);
        IntervalUtil flareInterval = new IntervalUtil(0.2f, 0.25f);
        public float fadeProgress = 0f;
        public FaderUtil fader = new FaderUtil(0.4f, 1f);
        public SotfNaniteSynthesizedListener(ShipAPI ship) {
            this.ship = ship;
            // Pulsing white glow from the ship's center
            Global.getCombatEngine().addLayeredRenderingPlugin(
                    new SotfClingingFlareVisualScript(
                            new SotfClingingFlareVisualScript.SotfClingingFlareParams(
                                    ship,
                                    COLOR,
                                    ship.getCollisionRadius() * 2.4f,
                                    ship.getCollisionRadius()
                            )
                    )
            );
        }

        public void advance(float amount) {
            if (ship.isHulk() && !startedFadingOut && Global.getCombatEngine().isEntityInPlay(ship)) {
                Global.getCombatEngine().addPlugin(createNaniteFadeOutPlugin(ship, ship.getHullSize().ordinal() * 1.5f, false));
                if (ship.getVariant().hasTag(SotfIDs.COTL_SERVICEBEYONDDEATH) && !ship.isFighter()) {
                    String wing_id;
                    switch (ship.getHullSize().ordinal() - 1) {
                        case 2:
                            wing_id = "sotf_sbd_wing_des";
                            break;
                        case 3:
                            wing_id = "sotf_sbd_wing_cru";
                            break;
                        case 4:
                            wing_id = "sotf_sbd_wing_cap";
                            break;
                        default:
                            wing_id = "sotf_sbd_wing_frigate";
                            break;
                    }
                    Global.getCombatEngine().addPlugin(
                            new NaniteShipFadeInPlugin(wing_id,
                                    ship, 0.5f + (ship.getHullSize().ordinal() / 2f), 0.5f, ship.getFacing()));
                }
                startedFadingOut = true;
            }

            RoilingSwarmEffect swarm = RoilingSwarmEffect.getSwarmFor(ship);
            if (swarm == null) {
                swarm = createSwarmFor(ship);
            }

            if (ship.isFighter()) return;

            boolean playerShip = Global.getCurrentState() == GameState.COMBAT &&
                    Global.getCombatEngine() != null && Global.getCombatEngine().getPlayerShip() == ship;


            RoilingSwarmEffect.RoilingSwarmParams params = swarm.getParams();
            params.baseMembersToMaintain = (int) ship.getMutableStats().getDynamic().getValue(
                    Stats.FRAGMENT_SWARM_SIZE_MOD, getBaseSwarmSize(ship.getHullSize()));
            params.memberRespawnRate = getBaseSwarmRespawnRateMult(ship.getHullSize()) *
                    ship.getMutableStats().getDynamic().getValue(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT);

            params.maxNumMembersToAlwaysRemoveAbove = (int) (params.baseMembersToMaintain * 1.5f);
            params.initialMembers = params.baseMembersToMaintain;

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
                toJitter.getSpriteAPI().setColor(JITTER_COLOR);
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    if (weapon.getSprite() != null) {
                        weapon.getSprite().setColor(JITTER_COLOR.brighter());
                    }
                    if (weapon.getBarrelSpriteAPI() != null) {
                        weapon.getBarrelSpriteAPI().setColor(JITTER_COLOR.brighter());
                    }
                    if (weapon.getUnderSpriteAPI() != null) {
                        weapon.getUnderSpriteAPI().setColor(JITTER_COLOR.brighter());
                    }
                    if (weapon.getMissileRenderData() != null) {
                        for (MissileRenderDataAPI missile : weapon.getMissileRenderData()) {
                            missile.getSprite().setColor(JITTER_COLOR.brighter().brighter().brighter());
                        }
                    }
                }
            }

            ship.getEngineController().fadeToOtherColor("sotf_nanitesynthesized", COLOR_STRONGER, Misc.setAlpha(COLOR_STRONGER, 25), 1f, 0.9f);
            if (ship.getShield() != null) {
                ship.getShield().setInnerColor(Misc.setAlpha(COLOR_STRONGER, 125));
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
                    float size = toSmoke.getCollisionRadius() * 0.3f;
                    for (int i = 0; i < 4; i++) {
                        Vector2f point = new Vector2f(toSmoke.getLocation());
                        point = Misc.getPointWithinRadiusUniform(point, toSmoke.getCollisionRadius() * 0.5f, Misc.random);
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
        }

        public static RoilingSwarmEffect createSwarmFor(ShipAPI ship) {
            RoilingSwarmEffect existing = RoilingSwarmEffect.getSwarmFor(ship);
            if (existing != null) return existing;

            float mult = 3;

            RoilingSwarmEffect.RoilingSwarmParams params = new RoilingSwarmEffect.RoilingSwarmParams();
            params.spriteCat = "skills";
            params.spriteKey = "sotf_naniteswarm_sheet";
            params.memberExchangeClass = NANITE_SWARM_EXCHANGE_CLASS;
            params.maxSpeed = ship.getMaxSpeedWithoutBoost() +
                    Math.max(ship.getMaxSpeedWithoutBoost() * 0.25f + 50f, 100f) +
                    ship.getMutableStats().getZeroFluxSpeedBoost().getModifiedValue();

            params.springStretchMult = 0.5f;
            params.baseSpriteSize = 30f / mult;

            params.flashProbability = 0.5f;
            params.flashRateMult = 4f;
            params.flashCoreRadiusMult = 0.25f;
            params.flashRadius = 50f / mult;
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

            params.baseMembersToMaintain = Math.round(getBaseSwarmSize(ship.getHullSize()) * mult);
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
            switch (size) {
                case CAPITAL_SHIP: return 32;
                case CRUISER: return 24;
                case DESTROYER: return 16;
                case FRIGATE: return 8;
                case FIGHTER: return 4;
                case DEFAULT: return 4;
                default: return 4;
            }
        }

        public static float getBaseSwarmRespawnRateMult(HullSize size) {
            switch (size) {
                case CAPITAL_SHIP: return 5f;
                case CRUISER: return 3f;
                case DESTROYER: return 2f;
                case FRIGATE: return 1f;
                case FIGHTER: return 0.5f;
                case DEFAULT: return 1f;
                default: return 1f;
            }
        }

    }

    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        // Fighters are also nanomechanical
        fighter.getVariant().addMod(id);
        if (!fighter.getVariant().getDisplayName().contains("Drone")) {
            fighter.getVariant().setVariantDisplayName(fighter.getVariant().getDisplayName() + " Drone");
        }
    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new SotfNaniteSynthesizedListener(ship));
        ship.setExtraOverlay(Global.getSettings().getSpriteName("misc", "sotf_overlay_nanitesynthesized"));
        ship.setExtraOverlayMatchHullColor(false);
        ship.setExtraOverlayShadowOpacity(0.5f);
    }

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // prob will have Automated Ship but w/e
        stats.getMinCrewMod().modifyMult(id, 0f);
        stats.getMaxCrewMod().modifyMult(id, 0f);
        // so it doesn't break before it fades away
        stats.getBreakProb().modifyMult(id, 0f);
    }

    public static EveryFrameCombatPlugin createNaniteFadeOutPlugin(final ShipAPI ship, final float fadeOutTime, boolean withSmoke) {
        return new BaseEveryFrameCombatPlugin() {
            float elapsed = 0f;

            IntervalUtil interval = new IntervalUtil(0.075f, 0.125f);

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (Global.getCombatEngine().isPaused()) return;

                elapsed += amount;

                float progress = elapsed / fadeOutTime;
                if (progress > 1f) progress = 1f;
                ship.setExtraAlphaMult2(1f - progress);

                if (progress > 0.5f) {
                    ship.setCollisionClass(CollisionClass.NONE);
                }

                if (elapsed > fadeOutTime) {
                    //ship.setHitpoints(0f);
                    ShipDestructionEffects.suppressEffects(ship, true, true);
                    Global.getCombatEngine().removeEntity(ship);
                    ship.setAlphaMult(0f);
                    Global.getCombatEngine().removePlugin(this);
                }

                if (!withSmoke) return;

                List<ShipAPI> shipAndModules = ship.getChildModulesCopy();
                shipAndModules.add(ship);

                // nanite fog effect
                interval.advance(amount);
                if (interval.intervalElapsed()) {
                    CombatEngineAPI engine = Global.getCombatEngine();

                    Color c = RiftLanceEffect.getColorForDarkening(SMOKE_COLOR);
                    c = Misc.setAlpha(c, 35);
                    float baseDuration = 2f;
                    for (ShipAPI toSmoke : shipAndModules) {
                        Vector2f vel = new Vector2f(toSmoke.getVelocity());
                        float size = toSmoke.getCollisionRadius() * 0.3f;
                        for (int i = 0; i < 4; i++) {
                            Vector2f point = new Vector2f(toSmoke.getLocation());
                            point = Misc.getPointWithinRadiusUniform(point, toSmoke.getCollisionRadius() * 0.5f, Misc.random);
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
            }
        };
    }

    public static class NaniteShipFadeInPlugin extends BaseEveryFrameCombatPlugin {
        float elapsed = 0f;
        ShipAPI [] ships = null;
        CollisionClass collisionClass;

        String variantId;
        ShipAPI source;
        float delay;
        float fadeInTime;
        float angle;

        public NaniteShipFadeInPlugin(String variantId, ShipAPI source, float delay, float fadeInTime, float angle) {
            this.variantId = variantId;
            this.source = source;
            this.delay = delay;
            this.fadeInTime = fadeInTime;
            this.angle = angle;

        }


        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (Global.getCombatEngine().isPaused()) return;

            elapsed += amount;
            if (elapsed < delay) return;

            CombatEngineAPI engine = Global.getCombatEngine();

            if (ships == null) {
                float facing = source.getFacing() + 15f * ((float) Math.random() - 0.5f);
//					Vector2f loc = new Vector2f();
//					loc = Misc.getPointWithinRadius(loc, source.getCollisionRadius() * 0.25f);
                Vector2f loc = Misc.getUnitVectorAtDegreeAngle(angle);
                loc.scale(source.getCollisionRadius() * 0.1f);
                Vector2f.add(loc, source.getLocation(), loc);
                CombatFleetManagerAPI fleetManager = engine.getFleetManager(source.getOriginalOwner());
                boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
                fleetManager.setSuppressDeploymentMessages(true);
                if (variantId.endsWith("_wing")) {
                    FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(variantId);
                    ships = new ShipAPI[spec.getNumFighters()];
                    PersonAPI captain = SotfPeople.genSirius(true);
                    ShipAPI leader = engine.getFleetManager(source.getOriginalOwner()).spawnShipOrWing(variantId, loc, facing, 0f, captain);
                    for (int i = 0; i < ships.length; i++) {
                        ships[i] = leader.getWing().getWingMembers().get(i);
                        ships[i].getLocation().set(loc);
                    }
                    collisionClass = ships[0].getCollisionClass();
                } else {
                    ships = new ShipAPI[1];
                    ships[0] = engine.getFleetManager(source.getOriginalOwner()).spawnShipOrWing(variantId, loc, facing, 0f, source.getOriginalCaptain());
                }
                for (int i = 0; i < ships.length; i++) {
                    ships[i].cloneVariant();

                    if (Global.getCombatEngine().isInCampaign() || Global.getCombatEngine().isInCampaignSim()) {
                        FactionAPI faction = Global.getSector().getFaction(SotfIDs.DREAMING_GESTALT);
                        if (faction != null) {
                            String name = faction.pickRandomShipName();
                            ships[i].setName(name);
                        }
                    }
                }
                fleetManager.setSuppressDeploymentMessages(wasSuppressed);
                collisionClass = ships[0].getCollisionClass();

            }



            float progress = (elapsed - delay) / fadeInTime;
            if (progress > 1f) progress = 1f;

            for (int i = 0; i < ships.length; i++) {
                ShipAPI ship = ships[i];
                ship.setAlphaMult(progress);

                if (progress < 0.5f) {
                    ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
                    ship.blockCommandForOneFrame(ShipCommand.TURN_LEFT);
                    ship.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
                    ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
                    ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
                }

                ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
                ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
                ship.blockCommandForOneFrame(ShipCommand.FIRE);
                ship.blockCommandForOneFrame(ShipCommand.PULL_BACK_FIGHTERS);
                ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
                ship.setHoldFireOneFrame(true);
                ship.setHoldFire(true);


                ship.setCollisionClass(CollisionClass.NONE);
                ship.getMutableStats().getHullDamageTakenMult().modifyMult("ShardSpawnerInvuln", 0f);
                if (progress < 0.5f) {
                    ship.getVelocity().set(source.getVelocity());
                } else if (progress > 0.75f){
                    ship.setCollisionClass(collisionClass);
                    ship.getMutableStats().getHullDamageTakenMult().unmodifyMult("ShardSpawnerInvuln");
                }

//					Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(source.getLocation(), ship.getLocation()));
//					dir.scale(amount * 50f * progress);
//					Vector2f.add(ship.getLocation(), dir, ship.getLocation());


                float jitterLevel = progress;
                if (jitterLevel < 0.5f) {
                    jitterLevel *= 2f;
                } else {
                    jitterLevel = (1f - jitterLevel) * 2f;
                }

                float jitterRange = 1f - progress;
                float maxRangeBonus = 50f;
                float jitterRangeBonus = jitterRange * maxRangeBonus;
                Color c = JITTER_COLOR;

                ship.setJitter(this, c, jitterLevel, 25, 0f, jitterRangeBonus);
            }

            if (elapsed > fadeInTime) {
                for (int i = 0; i < ships.length; i++) {
                    ShipAPI ship = ships[i];
                    ship.setAlphaMult(1f);
                    ship.setHoldFire(false);
                    ship.setCollisionClass(collisionClass);
                    ship.getMutableStats().getHullDamageTakenMult().unmodifyMult("ShardSpawnerInvuln");
                }
                engine.removePlugin(this);
            }
        }
    }

}