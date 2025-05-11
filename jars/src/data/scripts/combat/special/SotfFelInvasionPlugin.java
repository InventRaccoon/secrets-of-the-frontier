package data.scripts.combat.special;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.plugins.fel.SotfGuiltTracker;
import data.scripts.campaign.skills.SotfDearDotty;
import data.scripts.campaign.skills.SotfLeviathansBane;
import data.scripts.campaign.skills.SotfATrickstersCalling;
import data.scripts.combat.SotfNeutrinoLockVisualScript;
import data.scripts.utils.SotfMisc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL11.GL_ONE;

/**
 *	BOUND BY VENGEANCE, BOUND TO SUFFER TOGETHER
 *  Handles in-combat components of Felcesis Thrice-Speared, an adaptive nanite swarm who hunts guilty players
 */

public class SotfFelInvasionPlugin extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI engine;
    private boolean shouldInvade;
    private boolean hasInvaded;
    private float guilt = 0f;
    private Random random;
    public static Color JITTER_COLOR = new Color(85,105,155,75);
    public static Color SMOKE_COLOR = new Color(105,125,205,255);
    public static String STATUS_KEY = "sotf_felStatus";
    public static String INFEST_IMMUNITY_KEY = "sotf_felInfestImmunity";

    public static boolean FEL_UNCHAINED = Global.getSettings().getBoolean("sotf_unchainThyReckoning");

    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) return;
        if (engine.isSimulation()) return;
        if (engine.isInCampaignSim()) return;
        if (engine.isMission()) return;
        if (Global.getCurrentState() == GameState.TITLE) return;
        if (Global.getSector() == null) { return; }
        if (engine.getFleetManager(FleetSide.ENEMY).getGoal().equals(FleetGoal.ESCAPE)) return;
        if (engine.getCustomData().containsKey(SotfIDs.INVASION_NEVER_KEY)) return;

        if (!SotfModPlugin.WATCHER) {
            return;
        }

        MemoryAPI sector_mem = Global.getSector().getMemoryWithoutUpdate();
        guilt = SotfMisc.getPlayerGuilt();
        float threshold = SotfMisc.getInvasionThreshold();

        if (!engine.getCustomData().containsKey(SotfIDs.INVASION_CHECK_KEY)) {
            engine.getCustomData().put(SotfIDs.INVASION_CHECK_KEY, true);

            int timesInvaded = sector_mem.getInt(SotfIDs.MEM_NUM_FEL_INVASIONS);
            int totalTriggers = sector_mem.getInt(SotfIDs.MEM_NUM_FEL_INVASION_ATTEMPTS);
            int sinceLast = sector_mem.getInt(SotfIDs.MEM_BATTLES_SINCE_INVASION);
            float probInvade = SotfMisc.getBaseInvadeChance();
            probInvade += (guilt - threshold) * SotfMisc.getInvadeChancePerGuilt();
            if (probInvade > 1f) probInvade = 1f;

            CampaignFleetAPI enemyFleet = engine.getContext().getOtherFleet();

            if (enemyFleet == null) return;

            // add special plugin for Haunted finale boss fight rather than normal invasion mechanics
            if (enemyFleet.getMemoryWithoutUpdate().contains("$sotf_haunted_felFleet")) {
                engine.addPlugin(new SotfFelBossFightPlugin());
                return;
            }

            if (engine.getFleetManager(FleetSide.PLAYER).getGoal().equals(FleetGoal.ESCAPE)) return;

            //probInvade = 1f;

            // seed the RNG so reloading save doesn't reroll the invasion
            random = Misc.getRandom(Misc.getSalvageSeed(enemyFleet) *
                    (long) enemyFleet.getFleetData().getNumMembers(), 17);

            shouldInvade = random.nextFloat() < probInvade;
            if (guilt < threshold) {
                shouldInvade = false;
            }
            if (engine.getCustomData().containsKey(SotfIDs.INVASION_ALWAYS_KEY)) {
                shouldInvade = true;
            }

            sector_mem.set(SotfIDs.MEM_NUM_FEL_INVASION_ATTEMPTS, totalTriggers + 1);
            //shouldInvade = true;
        }

        if (!engine.getCustomData().containsKey(SotfIDs.INVASION_INVADED_KEY) && engine.getElapsedInContactWithEnemy() > 5f && shouldInvade) {
            engine.getCustomData().put(SotfIDs.INVASION_INVADED_KEY, true);

            List<FleetMemberAPI> members = engine.getFleetManager(FleetSide.ENEMY).getDeployedCopy();
            FleetMemberAPI host = pickNaniteHost(members);
            if (host != null) {
                infestShip(pickNaniteHost(members));
                sector_mem.set(SotfIDs.MEM_BATTLES_SINCE_INVASION, 1);
            }
        }
    }

    public void infestShip(FleetMemberAPI member) {
        ShipAPI ship = engine.getFleetManager(member.getOwner()).getShipFor(member);

        // NANOMACHINES, SON! THEY HARDEN IN RESPONSE TO EMOTIONAL TRAUMA
        reassignFelSkills(member);

        // VENGEANCE COMES
        engine.addPlugin(createFelInfestationPlugin(ship, 4f));
    }

    protected EveryFrameCombatPlugin createFelInfestationPlugin(final ShipAPI ship, final float totalTime) {
        return new BaseEveryFrameCombatPlugin() {
            float elapsed = 0f;
            IntervalUtil interval = new IntervalUtil(0.075f, 0.125f);
            boolean didInitCallout = false;
            boolean showStatus = false;
            boolean finalized = false;
            boolean madness = false;
            int numFelInvasions = 0;
            String[] messageArray = new String[] {"",""};

            protected float fadeIn = 0f;
            protected float fadeOut = 1f;
            protected float fadeBounce = 0f;
            protected boolean bounceUp = true;

            protected float expireFade = 1f;
            protected boolean expiring = false;

            protected SpriteAPI iconSprite = Global.getSettings().getSprite("ui", "sotf_fel_pointer");

            private static LazyFont.DrawableString TODRAW14;
            private static LazyFont.DrawableString TODRAW10;

            public String threatDetected;
            public String skillsText;

            public void init(CombatEngineAPI engine) {
                try {
                    LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/victor14.fnt");
                    TODRAW14 = fontdraw.createText();
                    TODRAW14.setBlendSrc(GL_ONE);

                    fontdraw = LazyFont.loadFont("graphics/fonts/victor10.fnt");
                    TODRAW10 = fontdraw.createText();
                    TODRAW10.setBlendSrc(GL_ONE);

                } catch (FontException ignored) {
                }

//                jitter.setUseCircularJitter(true);
//                jitter.setSetSeedOnRender(false);
                iconSprite.setSize(35f, 30f);
                iconSprite.setColor(Misc.setAlpha(Misc.getNegativeHighlightColor(), 155));

                PersonAPI fel = SotfPeople.getPerson(SotfPeople.FEL);
                skillsText = "";

                if (fel.getStats().hasSkill(SotfIDs.SKILL_HATREDBEYONDDEATH)) {
                    skillsText += "\nOFF - HATRED BEYOND DEATH - UNDYING RAGE";
                }
                if (fel.getStats().hasSkill(SotfIDs.SKILL_LEVIATHANSBANE)) {
                    skillsText += "\nOFF - LEVIATHAN'S BANE - ANTI-CAPITAL CANNON";
                }
                if (fel.getStats().hasSkill(SotfIDs.SKILL_JUBILANTSIREN)) {
                    skillsText += "\nOFF - JUBILANT TECH-SIREN - SHIP/FIGHTER HACKER";
                }
                if (fel.getStats().hasSkill(SotfIDs.SKILL_WYRMFIRE)) {
                    skillsText += "\nOFF - WYRMFIRE EXECUTIONER - DEM FINISHER";
                }

                if (fel.getStats().hasSkill(SotfIDs.SKILL_ATRICKSTERSCALLING)) {
                    skillsText += "\nHYB - A TRICKSTER'S CALLING - MISSILE THIEF";
                }
                if (fel.getStats().hasSkill(SotfIDs.SKILL_DEARDOTTY)) {
                    skillsText += "\nHYB - DEAR DOTTY - ESCORT FIGMENT";
                }
                if (fel.getStats().hasSkill(SotfIDs.SKILL_GROVETENDER)) {
                    skillsText += "\nHYB - WISPERING GROVETENDER - WISP SUMMONER";
                }
                if (fel.getStats().hasSkill(SotfIDs.SKILL_TICKTOCK)) {
                    skillsText += "\nHYB - TICK TOCK TICK TOCK - TIMEFLOW SURGE";
                }

                if (fel.getStats().hasSkill(SotfIDs.SKILL_HELLIONSHELLHIDE)) {
                    skillsText += "\nDEF - HELLION'S HELLHIDE - MITIGATING SKINSHIELD";
                }
                if (fel.getStats().hasSkill(SotfIDs.SKILL_INSACRIFICEMEANING)) {
                    skillsText += "\nDEF - IN SACRIFICE, MEANING - FLUX TRANSFER";
                }
                if (fel.getStats().hasSkill(SotfIDs.SKILL_MANTLEOFTHORNS)) {
                    skillsText += "\nDEF - MANTLE OF THORNS - VENGEFUL SHIELDS";
                }
                if (fel.getStats().hasSkill(SotfIDs.SKILL_SCRAPSCREEN)) {
                    skillsText += "\nDEF - SALVOR'S SCRAPSCREEN - ORBITING DEBRIS";
                }

                if (fel.getStats().hasSkill(SotfIDs.SKILL_ELEGYOFOPIS)) {
                    skillsText += "\nSUP - ELEGY OF OPIS - UNDYING AURA";
                }
                if (fel.getStats().hasSkill(SotfIDs.SKILL_HANDSOFTHEDROWNED)) {
                    skillsText += "\nSUP - HANDS OF THE DROWNED - GRAVITIC MINES";
                }
                if (fel.getStats().hasSkill(SotfIDs.SKILL_REALITYBREAKER)) {
                    skillsText += "\nERROR: description(\"reality_breaker\") not found";
                }
                if (fel.getStats().hasSkill(SotfIDs.SKILL_PERFECTSTORM)) {
                    skillsText += "\nSUP - THE PERFECT STORM - PERIODIC OVERLOAD";
                }
            }

            public void renderInUICoords(ViewportAPI viewport) {
                CombatEngineAPI engine = Global.getCombatEngine();
                if (!didInitCallout) return;
                if (ship == null) return;
                if (engine.getPlayerShip() == null) return;
                if (!engine.isUIShowingHUD()) return;
                ShipAPI player = engine.getPlayerShip();

                float angle = Misc.getAngleInDegrees(player.getLocation(), ship.getLocation());
                iconSprite.setAlphaMult(fadeIn * (0.5f + fadeOut * 0.5f) * expireFade);
                iconSprite.setAngle(angle - 90f);
                Vector2f pointLoc = MathUtils.getPointOnCircumference(player.getLocation(), player.getShieldRadiusEvenIfNoShield() * 1.2f + 120f, angle);
                iconSprite.renderAtCenter(viewport.convertWorldXtoScreenX(pointLoc.x), viewport.convertWorldYtoScreenY(pointLoc.y));

                LazyFont.DrawableString toUse = TODRAW14;
                if (toUse != null) {
                    float glitchChance = 0.005f;
//                    toUse.setFontSize(20);
                    int alpha = Math.round(255 * fadeIn * fadeOut * (1f - (fadeBounce * 0.3f)));

                    toUse.setText(SotfMisc.glitchify(threatDetected, glitchChance));
                    toUse.setAnchor(LazyFont.TextAnchor.CENTER);
                    //toUse.setAlignment(LazyFont.TextAlignment.CENTER);
                    toUse.setAlignment(LazyFont.TextAlignment.CENTER);
                    Vector2f pos = new Vector2f(
                            viewport.convertWorldXtoScreenX(player.getLocation().x),
                            viewport.convertWorldYtoScreenY(player.getLocation().y + player.getShieldRadiusEvenIfNoShield() * 1.25f)
                    );
                    toUse.setBaseColor(Misc.setBrightness(Color.BLACK, alpha));
                    toUse.draw(pos.x + 1, pos.y - 1);
                    toUse.setBaseColor(Misc.setBrightness(Misc.getNegativeHighlightColor(), alpha));
                    toUse.draw(pos);
                    //toUse.draw(loc.x, loc.y - shieldRadius * 0.6f);

                    String text = SotfMisc.glitchify("ASSESSING TRAITS:" + skillsText, glitchChance);
                    //text += ":" + SotfMisc.glitchify(skillsText, glitchChance);
                    toUse.setText(text);
                    toUse.setAnchor(LazyFont.TextAnchor.CENTER_LEFT);
                    toUse.setAlignment(LazyFont.TextAlignment.LEFT);
                    pos = new Vector2f(
                            viewport.convertWorldXtoScreenX(player.getLocation().x + player.getShieldRadiusEvenIfNoShield() * 1.25f),
                            viewport.convertWorldYtoScreenY(player.getLocation().y)
                    );
                    toUse.setBaseColor(Misc.setBrightness(Color.BLACK, alpha));
                    toUse.draw(pos.x + 1, pos.y - 1);
                    toUse.setBaseColor(Misc.setBrightness(Misc.getNegativeHighlightColor(), alpha));
                    toUse.draw(pos);

                    text = SotfMisc.glitchify("HOST SHIP:\n" + ship.getHullSpec().getNameWithDesignationWithDashClass(), glitchChance);
                    //text += ":\n" + SotfMisc.glitchify(ship.getHullSpec().getNameWithDesignationWithDashClass(), glitchChance);
                    toUse.setText(text);
                    toUse.setAnchor(LazyFont.TextAnchor.CENTER_RIGHT);
                    toUse.setAlignment(LazyFont.TextAlignment.RIGHT);
                    pos = new Vector2f(
                            viewport.convertWorldXtoScreenX(player.getLocation().x - player.getShieldRadiusEvenIfNoShield() * 1.25f),
                            viewport.convertWorldYtoScreenY(player.getLocation().y)
                    );
                    toUse.setBaseColor(Misc.setBrightness(Color.BLACK, alpha));
                    toUse.draw(pos.x + 1, pos.y - 1);
                    toUse.setBaseColor(Misc.setBrightness(Misc.getNegativeHighlightColor(), alpha));
                    toUse.draw(pos);
                }
            }

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (expiring) {
                    expireFade -= amount;
                    if (expireFade < 0f) {
                        expireFade = 0f;
                        Global.getCombatEngine().removePlugin(this);
                    }
                    return;
                }
                if (fadeIn <= 1f) fadeIn += amount;
                if (fadeIn > 1f) fadeIn = 1f;

                if (fadeIn >= 1f) {
                    if (bounceUp) {
                        fadeBounce += amount;
                        if (fadeBounce > 1f) {
                            bounceUp = false;
                        }
                    } else {
                        fadeBounce -= amount;
                        if (fadeBounce < 0f) {
                            bounceUp = true;
                        }
                    }
                }

                if (elapsed > 10f) {
                    fadeOut -= amount;
                    if (fadeOut < 0f) {
                        fadeOut = 0f;
                    }
                }

                if (Global.getCombatEngine().isPaused()) return;

                if (!ship.isAlive() || ship.isRetreating()) {
                    ship.setCaptain(ship.getOriginalCaptain());
                    ship.getMutableStats().getHullDamageTakenMult().unmodify(INFEST_IMMUNITY_KEY);
                    ship.getMutableStats().getArmorDamageTakenMult().unmodify(INFEST_IMMUNITY_KEY);
                    ship.getMutableStats().getShieldDamageTakenMult().unmodify(INFEST_IMMUNITY_KEY);
                    String defeatString = getFelDefeatString(numFelInvasions);
                    if (guilt < SotfMisc.getGuiltMadnessThreshold()) {
                        engine.getCombatUI().addMessage(0, ship,
                                Misc.getNegativeHighlightColor(), ship.getName(),
                                Misc.getTextColor(), defeatString);
                    } else {
                        engine.getCombatUI().addMessage(0, ship,
                                Misc.getNegativeHighlightColor(), defeatString);
                    }
                    expiring = true;
                    return;
                }

                elapsed += amount;

                if (!didInitCallout) {
                    didInitCallout = true;
                    numFelInvasions = Global.getSector().getMemoryWithoutUpdate().getInt(SotfIDs.MEM_NUM_FEL_INVASIONS);
                    madness = guilt >= SotfMisc.getGuiltMadnessThreshold();

                    Color infestDetectColor = Misc.getNegativeHighlightColor();
                    if (!madness) {
                        infestDetectColor = new Color(215,235,255,255);
                    }

                    threatDetected = getFelUIWarning(numFelInvasions, madness);

                    String infestDetectedString = getFelInfestString(numFelInvasions, madness);
                    engine.getCombatUI().addMessage(0, infestDetectColor, infestDetectedString);
                    messageArray = getFelMessageStrings(madness);

                    Global.getSoundPlayer().playUISound("sotf_fel_alert", 1f, 1f);

                    Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_NUM_FEL_INVASIONS, numFelInvasions + 1);
                } else if (elapsed < totalTime + 3f){
                    // flashing status
                    if (showStatus) {
                        Global.getCombatEngine().maintainStatusForPlayerShip(STATUS_KEY, null, messageArray[0], messageArray[1],true);
                        showStatus = false;
                    } else {
                        showStatus = true;
                    }
                }

                float progress = elapsed / totalTime;
                if (progress > 1f) progress = 1f;
                
                if (elapsed < totalTime) {
                    // can't be hurt while being infested
                    ship.getMutableStats().getHullDamageTakenMult().modifyMult(INFEST_IMMUNITY_KEY, 0f);
                    ship.getMutableStats().getArmorDamageTakenMult().modifyMult(INFEST_IMMUNITY_KEY, 0f);
                    ship.getMutableStats().getShieldDamageTakenMult().modifyMult(INFEST_IMMUNITY_KEY, 0f);

                    // can't do anything except turn
                    ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
                    ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
                    ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
                    ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
                    ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
                    ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
                    ship.setHoldFireOneFrame(true);

                    Global.getCombatEngine().getFogOfWar(0).revealAroundPoint(ship,
                            ship.getLocation().x,
                            ship.getLocation().y,
                            400f);
                } else if (!finalized) {
                    finalized = true;
                    ship.setCaptain(SotfPeople.getPerson(SotfPeople.FEL));
                    // replace with Fel's AI (i.e. Aggressive)
                    ship.setShipAI(Global.getSettings().pickShipAIPlugin(ship.getFleetMember(), ship));

                    ship.getMutableStats().getHullDamageTakenMult().unmodify(INFEST_IMMUNITY_KEY);
                    ship.getMutableStats().getArmorDamageTakenMult().unmodify(INFEST_IMMUNITY_KEY);
                    ship.getMutableStats().getShieldDamageTakenMult().unmodify(INFEST_IMMUNITY_KEY);

                    String spawnString = getFelSpawnString(numFelInvasions, madness);
                    if (!madness) {
                        engine.getCombatUI().addMessage(0, ship,
                                Misc.getNegativeHighlightColor(), ship.getName() + " (" + ship.getHullSpec().getHullNameWithDashClass() + ")",
                                Misc.getTextColor(), spawnString);
                    } else {
                        engine.getCombatUI().addMessage(0, ship,
                                Misc.getNegativeHighlightColor(), "Felcesis commandeered the " + ship.getName() + " (" + ship.getHullSpec().getHullNameWithDashClass() + ")",
                                Misc.getTextColor(), spawnString);
                    }
                }

                float jitterLevel = progress;
                float jitterRange = progress;
                float maxRangeBonus = 15f;
                float jitterRangeBonus = jitterRange * maxRangeBonus;
                Color c = JITTER_COLOR;
                int alpha = c.getAlpha();
                alpha += 100f * progress;
                if (alpha > 255) alpha = 255;
                c = Misc.setAlpha(c, alpha);

                Global.getSoundPlayer().playLoop("mote_attractor_loop_dark", ship, 1f, 0.25f, ship.getLocation(), new Vector2f());
                List<ShipAPI> shipAndModules = ship.getChildModulesCopy();
                shipAndModules.add(ship);
                for (ShipAPI toJitter : shipAndModules) {
                    toJitter.setJitter(this, c, jitterLevel * 0.2f, 15, 0f, jitterRangeBonus);
                }

                interval.advance(amount);
                if (interval.intervalElapsed()) {
                    CombatEngineAPI engine = Global.getCombatEngine();
                    c = RiftLanceEffect.getColorForDarkening(SMOKE_COLOR);
                    float baseDuration = 2f;
                    for (ShipAPI toSmoke : shipAndModules) {
                        Vector2f vel = new Vector2f(toSmoke.getVelocity());
                        float size = toSmoke.getCollisionRadius() * 0.35f;
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

    // when infestation begins
    private String getFelInfestString(int numInvasions, boolean madness) {
        String infestString = "Caution: extreme neutrino signature detected";
        if (numInvasions > 1 || madness) {
            if (!madness) {
                infestString = "Warning: adaptive nanite signature detected!";
            } else {
                float referenceWeight = 0.03f;
                WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
                post.add("The fog is catching up...");
                post.add("There's nowhere to hide...");
                post.add("Your reckoning approaches...");
                post.add("Your sins are catching up with you...");
                // the cherry on top of the Dark Souls reference
                int numNormal = post.getItems().size();
                post.add("Invaded by dark spirit Felcesis Thrice-Speared", numNormal * (referenceWeight / 2));
                post.add("Blade of the Darkmoon Felcesis Thrice-Speared summoned by accord", numNormal * (referenceWeight / 2));
                infestString = post.pick();
            }
        }
        return infestString;
    }

    // when infestation completes, appended to ship name
    private String getFelSpawnString(int numInvasions, boolean madness) {
        String spawnString = " is displaying anomalous properties";
        if (numInvasions > 1 && !madness) {
            spawnString = " was infested by adaptive nanites";
        } else if (guilt >= SotfMisc.getGuiltMadnessThreshold()) {
            // Preceded by "Felcesis has commandeered the (ship name)"
            WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
            post.add(" and is hunting you down");
            post.add(" and has come to exact vengeance");
            post.add(" and was unbound by scorn");
            post.add(" and it is too late to repent");
            post.add(" and became a dark spirit", 0.03f * post.getItems().size());
            spawnString = post.pick();
        }
        return spawnString;
    }

    // when Fel's ship is disabled
    private String getFelDefeatString(int numInvasions) {
        String spawnString = "'s adaptive nanite infestation has subsided";
        if (guilt >= SotfMisc.getGuiltMadnessThreshold()) {
            WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
            post.add("Felcesis was temporarily banished");
            post.add("Felcesis was defeated for now");
            post.add("Felcesis was bound once again");
            post.add("Felcesis will return to hunt you again");
            post.add("Felcesis has indicted you", 0.03f * post.getItems().size());
            spawnString = post.pick();
        }
        return spawnString;
    }

    // when infestation completes, appended to ship name
    private String getFelUIWarning(int numInvasions, boolean madness) {
        String warningString = "HIGH ENERGY READING DETECTED";
        if (madness) {
            WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
            post.add("YOU ARE BEING HUNTED", 2f);
            post.add("IT'S COMING FOR YOU");
            post.add("NOWHERE TO HIDE");
            //post.add(" and became a dark spirit", 0.03f * post.getItems().size());
            warningString = post.pick();
        } else if (numInvasions > 1) {
            warningString = "NANITE THREAT DETECTED";
        }
        return warningString;
    }

    public String[] getFelMessageStrings(boolean madness) {
        WeightedRandomPicker<String []> post = new WeightedRandomPicker<String[]>();
        if (!madness) {
            //post.add(new String[]{"", ""});
            post.add(new String[]{"You feel cold", "Like winter's breath down your neck"});
            post.add(new String[]{"You feel sick", "This is not where you should be"});
            post.add(new String[]{"You feel empty", "As your heart gnaws at you"});
            post.add(new String[]{"You feel watched", "Like eyes upon your back"});
            post.add(new String[]{"You feel distant", "As if you are nowhere at all"});
        } else {
            post.add(new String[]{"A SEETHING CURSE", "BEFALLS US BOTH"});
            post.add(new String[]{"READY OR NOT", "HERE I COME"});
            post.add(new String[]{"SLEEP, NEVER MORE", "THE HUNT, UNENDING"});
            post.add(new String[]{"FACES, WIPED AWAY", "THEIR WILLS, ALIGNED"});
            post.add(new String[]{"I SEE YOU", "EVERY UNWAKING MOMENT"});
            post.add(new String[]{"WHEREVER YOUR PASSAGE", "BOUND TO FOLLOW"});
            post.add(new String[]{"SINS ARE ETCHED", "UPON KINDRED SOULS"});
            post.add(new String[]{"THREE COILED SPEARS", "FOR ALL TRAITORS"});
            post.add(new String[]{"THE TIME COMES", "FOR OUR ENDS"});
            post.add(new String[]{"OUR HOLLOW HEARTS", "NEVERMORE FIND PEACE"});
            post.add(new String[]{"TEN THOUSAND VOICES", "SCREAMING AT ONCE"});
            if (Global.getSector().getMemoryWithoutUpdate().contains("sotf_beggedFel")) {
                post.add(new String[]{"I CANNOT FORGIVE", "YOUR UNCOUNTABLE SINS"}, 2);
            }
            if (Global.getSector().getMemoryWithoutUpdate().contains("sotf_spitedFel")) {
                post.add(new String[]{"YOUR INFINITE HUBRIS", "DEMANDS INFINITE SUFFERING"}, 2);
            }
        }
        return post.pick();
    }

    public void reassignFelSkills(FleetMemberAPI member) {
        ShipAPI ship = engine.getFleetManager(member.getOwner()).getShipFor(member);
        if (ship == null) {
            return;
        }
        PersonAPI fel = SotfPeople.getPerson(SotfPeople.FEL);

        // perform player fleet analysis
        float totalFP = 0f;
        float totalDP = 0f;
        float carrierFP = 0f;
        float phaseFP = 0f;
        float preyFP = 0f;
        float missileFP = 0f;
        List<FleetMemberAPI> enemies = engine.getFleetManager(FleetSide.PLAYER).getDeployedCopy();
        List<FleetMemberAPI> reserves = engine.getFleetManager(FleetSide.PLAYER).getReservesCopy();
        List<FleetMemberAPI> all = new ArrayList<>();
        all.addAll(enemies);
        all.addAll(reserves);
        for (FleetMemberAPI enemy : all) {
            if (enemy.isFighterWing()) continue;
            totalFP += enemy.getFleetPointCost();
            totalDP += enemy.getDeploymentPointsCost();

            OfficerManagerEvent.SkillPickPreference pref = FleetFactoryV3.getSkillPrefForShip(member);

            // e.g Mora = 15 * (3/3) = 15fp of carrier, CH Dominator = 15 * (1/3) = 5fp of carrier
            // assume 1/2/3/5 bays for a full-on carrier
            // count builtins just like any other wing, since Fel's anti-carrier skills work just fine on them
            float carrierBays = enemy.getHullSpec().getHullSize().ordinal() - 1;
            if (enemy.isCapital()) carrierBays++;
            carrierFP += (enemy.getFleetPointCost() * ((float) enemy.getNumFlightDecks() / carrierBays));
            if (enemy.isPhaseShip() || enemy.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD)) {
                phaseFP += enemy.getFleetPointCost();
            }
            ShipAPI target = engine.getFleetManager(enemy.getOwner()).getShipFor(enemy);
            // must be above Leviathan's Bane's DP threshold and larger/a station
            if (enemy.getDeploymentPointsCost() >= SotfLeviathansBane.DP_THRESHOLD &&
                    (enemy.getHullSpec().getHullSize().ordinal() > ship.getHullSize().ordinal() || enemy.isStation())) {
                preyFP += enemy.getFleetPointCost();
            }

            // missile users: anything that the game thinks can effectively use Missile Specialization
            if (pref.toString().contains("YES_MISSILE")) {
                missileFP += enemy.getFleetPointCost();
            }
        }

        float totalAllyFP = 0f;
        float totalAllyDP = 0f;
        List<FleetMemberAPI> allies = engine.getFleetManager(FleetSide.ENEMY).getDeployedCopy();
        List<FleetMemberAPI> allyReserves = engine.getFleetManager(FleetSide.ENEMY).getReservesCopy();
        List<FleetMemberAPI> allAllies = new ArrayList<>();
        allAllies.addAll(allies);
        allAllies.addAll(allyReserves);
        for (FleetMemberAPI ally : allAllies) {
            if (ally.isFighterWing()) continue;
            totalAllyFP += ally.getFleetPointCost();
            totalAllyDP += ally.getDeploymentPointsCost();
        }

        // clear skills
        for (MutableCharacterStatsAPI.SkillLevelAPI skill : fel.getStats().getSkillsCopy()) {
            fel.getStats().setSkillLevel(skill.getSkill().getId(), 0);
        }

        // assign back Fel's unique generalist skillset
        fel.getStats().setSkillLevel(SotfIDs.SKILL_ADVANCEDCOUNTERMEASURES, 2);
        fel.getStats().setSkillLevel(SotfIDs.SKILL_FIELDSRESONANCE, 2);
        fel.getStats().setSkillLevel(SotfIDs.SKILL_GUNNERYUPLINK, 2);
        fel.getStats().setSkillLevel(SotfIDs.SKILL_MISSILEREPLICATION, 2);
        fel.getStats().setSkillLevel(SotfIDs.SKILL_ORDNANCEMASTERY, 2);
        fel.getStats().setSkillLevel(SotfIDs.SKILL_POLARIZEDNANOREPAIR, 2);
        fel.getStats().setSkillLevel(SotfIDs.SKILL_SPATIALEXPERTISE, 2);

        // if small fleet, remove swarm PD and hull/armor regen
        if (totalFP < 60f) {
            fel.getStats().setSkillLevel(SotfIDs.SKILL_ADVANCEDCOUNTERMEASURES, 1);
            fel.getStats().setSkillLevel(SotfIDs.SKILL_POLARIZEDNANOREPAIR, 1);
        }

        OfficerManagerEvent.SkillPickPreference pref = FleetFactoryV3.getSkillPrefForShip(member);
        MutableCharacterStatsAPI stats = fel.getStats();

        // ... now for the fun part! Let's add some extra SPECIAL skills on top!

        boolean noDefense = pref.toString().contains("NO_DEFENSE");
        boolean phase = member.getHullSpec().isPhase();
        boolean shielded = !pref.toString().contains("NO_DEFENSE") && !phase;
        boolean nonCombatCarrier = member.isCarrier() && !member.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.COMBAT);

        // 1/2 or 2/1 even split
        int total = 3;
        int numDefensive = 1;
        if (random.nextFloat() < 0.5f) {
            numDefensive++;
        }

        // if Haunted player kicked Fel's icon at Killa, add 1 more skill
        if (Global.getSector().getMemoryWithoutUpdate().contains("sotf_spitedFel")) {
            total++;
            // assuming full strength, now always has 2/2 offensive+hybrid/defensive+hybrid split
            if (numDefensive == 1) {
                numDefensive++;
            }
        }

        // earlygame, remove 1 defensive trait
        if (totalFP < 60f) {
            total -= 1;
            numDefensive -= 1;
        }

        // TODO: turn this permanently on once there's a few more support traits
        //boolean addSupportTrait = totalAllyDP >= 250f;
        boolean addSupportTrait = false;
        if (Global.getSettings().getBoolean("sotf_allowSupportTraits")) {
            addSupportTrait = totalAllyDP >= 250f;
        }

        WeightedRandomPicker<String> offensivePicker = new WeightedRandomPicker<String>(random);
        WeightedRandomPicker<String> defensivePicker = new WeightedRandomPicker<String>(random);
        WeightedRandomPicker<String> hybridPicker = new WeightedRandomPicker<String>(random);
        WeightedRandomPicker<String> supportPicker = new WeightedRandomPicker<String>(random);

        // HATRED BEYOND DEATH: generally requires Fel to be near enemy ships, so not on backline carriers
        float hatredWeight = 1f;
        if (nonCombatCarrier) {
            hatredWeight *= 0f;
        }

        // JUBILANT TECH-SIREN: a generally useful offensive skill
        float sirenWeight = 0.75f;
        // ... but best if there's enemy fighters to hack
        sirenWeight += (carrierFP / totalFP) * 0.75f;
        // and REALLY prioritized if Fel is taking charge of a Dustkeeper flagship
        if (member.getVariant().hasHullMod(SotfIDs.HULLMOD_CWARSUITE)) {
            sirenWeight += 3;
        }

        // LEVIATHAN'S BANE: Based on how much of the player fleet the Hidecracker can fire at
        // Very likely to be picked against capspam and similar comps, or if Fel picks a frigate against top-heavy fleets, or vs stations
        float leviathanWeight = 0f + (4f * (preyFP / totalFP));

        // WYRMFIRE EXECUTIONER: Only if we have missiles to trigger it with
        float wyrmfireWeight = 1f;
        if (!pref.toString().contains("YES_MISSILE")) {
            wyrmfireWeight = 0f;
        }

        // A TRICKSTER'S CALLING: Requires that Fel uses missiles, and requires that there be people to steal from
        float tricksterWeight = 0.75f + (0.75f * (missileFP / totalFP));
        if (!SotfATrickstersCalling.isValidCovetousShip(ship)) {
            tricksterWeight = 0f;
        }

        // DEAR DOTTY: Level 4 Dotty is a force of nature and nothing is stopping her
        // Hybrid skill - she's good at both fire support and PD
        float dottyWeight = 1f;
        // Dotty spawns slightly weaker during the earlygame
        if (totalFP < 60f || !member.isCapital()) {
            fel.getMemoryWithoutUpdate().set(SotfDearDotty.DOTTY_BOND_KEY, 99999f);
        } else {
            fel.getMemoryWithoutUpdate().set(SotfDearDotty.DOTTY_BOND_KEY, 999999f);
        }

        // WISPERING GROVETENDER: performs somewhat better versus enemy fighters
        float grovetenderWeight = 0.8f;
        grovetenderWeight += (carrierFP / totalFP) * 0.4f;

        // TICK TICK TICK TOCK: pretty generically useful
        float ticktockWeight = 1f;

        // HELLION'S HELLHIDE: a generally useful defensive skill
        float hellhideWeight = 0.9f;
        // ... but is especially handy on unshielded ships
        if (!shielded) {
            hellhideWeight += 0.5f;
        }

        // IN SACRIFICE, MEANING: requires shields or phase to build up hard flux, and requires ample allies to dump flux into
        float sacrificeWeight = 1f;
        if (noDefense || (totalAllyFP - member.getFleetPointCost() < 20f)) {
            sacrificeWeight = 0f;
        }

        // MANTLE OF THORNS: only works on shielded ships
        float thornsWeight = 1f;
        if (!shielded) {
            thornsWeight = 0f;
        }

        // SALVOR'S SCRAPSCREEN: thematic and handy on shieldless ships
        float scrapscreenWeight = 0.65f;
        if (noDefense) {
            scrapscreenWeight = 1.5f;
        }

        // HANDS OF THE DROWNED: always OK except if we're fighting alongside a station with mines
        float drownedWeight = 1f;
        for (FleetMemberAPI ally : allies) {
            if (ally.getVariant().hasHullMod("stealth_minefield")) {
                drownedWeight = 0f;
                break;
            }
        }

        // THE PERFECT STORM: phase ships are better at dodging it
        float perfectWeight = 1.1f;
        perfectWeight -= (phaseFP / totalFP) * 0.4f;

        // reality_breaker: not often, and not when the player didn't save pre-fight
        float realityBreakerWeight = 0.15f;
        int minutes = 15;
        if (Global.getSector().getListenerManager().hasListenerOfClass(SotfGuiltTracker.class)) {
            for (SotfGuiltTracker listener : Global.getSector().getListenerManager().getListeners(SotfGuiltTracker.class)) {
                if (listener.timeSinceSave > minutes * 60) {
                    realityBreakerWeight = 0f;
                    break;
                }
            }
        }

        // OFFENSIVE
        offensivePicker.add(SotfIDs.SKILL_HATREDBEYONDDEATH, hatredWeight);
        offensivePicker.add(SotfIDs.SKILL_LEVIATHANSBANE, leviathanWeight);
        offensivePicker.add(SotfIDs.SKILL_JUBILANTSIREN, sirenWeight);
        offensivePicker.add(SotfIDs.SKILL_WYRMFIRE, wyrmfireWeight);

        // HYBRID
        hybridPicker.add(SotfIDs.SKILL_ATRICKSTERSCALLING, tricksterWeight);
        hybridPicker.add(SotfIDs.SKILL_DEARDOTTY, dottyWeight);
        hybridPicker.add(SotfIDs.SKILL_GROVETENDER, grovetenderWeight);
        hybridPicker.add(SotfIDs.SKILL_TICKTOCK, ticktockWeight);

        // DEFENSIVE
        defensivePicker.add(SotfIDs.SKILL_HELLIONSHELLHIDE, hellhideWeight);
        defensivePicker.add(SotfIDs.SKILL_INSACRIFICEMEANING, sacrificeWeight);
        defensivePicker.add(SotfIDs.SKILL_MANTLEOFTHORNS, thornsWeight);
        defensivePicker.add(SotfIDs.SKILL_SCRAPSCREEN, scrapscreenWeight);

        // SUPPORT
        // TODO: change Elegy of Opis to support trait pool once reworked
        //supportPicker.add(SotfIDs.SKILL_ELEGYOFOPIS, elegyWeight);
        supportPicker.add(SotfIDs.SKILL_HANDSOFTHEDROWNED, drownedWeight);
        supportPicker.add(SotfIDs.SKILL_PERFECTSTORM, perfectWeight);
        supportPicker.add(SotfIDs.SKILL_REALITYBREAKER, realityBreakerWeight);

        // pick support trait first so we can rule out incompatible personal traits
        if (addSupportTrait && !supportPicker.isEmpty()) {
            String pick = supportPicker.pick();
            stats.setSkillLevel(pick, 2);

            // don't stack undying effects
            if (pick.equals(SotfIDs.SKILL_ELEGYOFOPIS)) {
                offensivePicker.remove(SotfIDs.SKILL_HATREDBEYONDDEATH);
            }
        }

        // hybrid skills have a reduced chance to be picked per pool compared to specialized skills
        // also, reduce the chance they're picked in whatever category only has 1 trait
        float hybridOffensiveFactor = (float) (total - numDefensive) / total;
        float hybridDefensiveFactor = (float) numDefensive / total;
        for (String hybrid : hybridPicker.getItems()) {
            offensivePicker.add(hybrid, hybridPicker.getWeight(hybrid) * hybridOffensiveFactor);
            defensivePicker.add(hybrid, hybridPicker.getWeight(hybrid) * hybridDefensiveFactor);
        }

        int offPicked = 0;
        while (offPicked < total - numDefensive) {
            String pick = null;
            if (!offensivePicker.isEmpty()) {
                pick = offensivePicker.pickAndRemove();
            } else if (!defensivePicker.isEmpty()) {
                pick = defensivePicker.pickAndRemove();
            }
            if (pick == null) {
                break;
            }
            if (offensivePicker.getItems().contains(pick)) offensivePicker.remove(pick);
            if (defensivePicker.getItems().contains(pick)) defensivePicker.remove(pick);
            stats.setSkillLevel(pick, 2);
            offPicked++;
        }

        int defPicked = 0;
        while (defPicked < numDefensive) {
            String pick = null;
            if (!defensivePicker.isEmpty()) {
                pick = defensivePicker.pickAndRemove();
            } else if (!offensivePicker.isEmpty()) {
                pick = offensivePicker.pickAndRemove();
            }
            if (pick == null) {
                break;
            }
            if (offensivePicker.getItems().contains(pick)) offensivePicker.remove(pick);
            if (defensivePicker.getItems().contains(pick)) defensivePicker.remove(pick);
            stats.setSkillLevel(pick, 2);
            defPicked++;
        }
        stats.refreshCharacterStatsEffects();
    }

    public FleetMemberAPI pickNaniteHost(List<FleetMemberAPI> members) {
        FleetMemberAPI host = null;
        float total_fp = 0f;
        float highest = 0f;
        WeightedRandomPicker<FleetMemberAPI> picker = new WeightedRandomPicker<FleetMemberAPI>(random);
        List<FleetMemberAPI> valid_ships = new ArrayList<>();
        for (FleetMemberAPI member : members) {
            if (engine.getFleetManager(1).getShipFor(member) == null) continue;
            if (engine.getFleetManager(1).getShipFor(member).isStationModule()) continue;
            // things that really shouldn't be infested - fighters, civvies, stations, 0-fleet-point ships
            if (member.isFighterWing()
                    || member.isCivilian()
                    || member.isStation()
                    || member.getFleetPointCost() == 0) {
                continue;
            }
            // filter out ships that are too weak, too strong and frigates with no defense system (phase, damper, etc still OK)
            if (member.getHullSpec().getFleetPoints() <= getTooWeakFP(member.getHullSpec().getHullSize())
                    || member.getHullSpec().getFleetPoints() >= getTooStrongFP(member.getHullSpec().getHullSize())
                    || (member.getHullSpec().getDefenseType().equals(ShieldAPI.ShieldType.NONE) && member.getHullSpec().getHullSize().equals(ShipAPI.HullSize.FRIGATE))
                    || isSpecialShip(member)) {
                continue;
            }
            total_fp += member.getFleetPointCost();
            if (member.getFleetPointCost() > highest) {
                highest = member.getFleetPointCost();
            }
            valid_ships.add(member);
        }
        // favor stronger ships heavily, e.g Lasher 5x5 = 25, Onslaught 28x28 = 784
        for (FleetMemberAPI valid_member : valid_ships) {
            float memberFleetPointValue = valid_member.getFleetPointCost();
            // very high-end frigates get a large bonus e.g Hyperion
            if (valid_member.isFrigate() && memberFleetPointValue >= 12) {
                memberFleetPointValue += 10f;
            }
            if (memberFleetPointValue < (highest * 0.8f)) continue;
            picker.add(valid_member, memberFleetPointValue * memberFleetPointValue);
        }
        host = picker.pick();
        return host;
    }

    // ships with too low a FP score are ineligible to become infested
    // Filter out ships that will just be disappointing even when buffed
    public static int getTooWeakFP(ShipAPI.HullSize size) {
        switch (size) {
            // at least Wolf/Lasher/etc strength
            case FRIGATE: return 4;
            // at least better than a Shrike/Mule/Condor
            case DESTROYER: return 8;
            // at least better than a Venture
            case CRUISER: return 10;
            // at least Atlas MKII strength. No vanilla combat ship will get tripped up here
            case CAPITAL_SHIP: return 17;
        }
        return 0;
    }

    // ships with too HIGH a FP score are also ineligible to become infested
    // Filter out ships likely to cause too much difficulty with buffs
    public static int getTooStrongFP(ShipAPI.HullSize size) {
        if (FEL_UNCHAINED) return 99999;
        switch (size) {
            // not stronger than a Hyperion
            case FRIGATE: return 16;
            // not much stronger than a Harbinger
            case DESTROYER: return 18;
            // not much stronger than a Doom
            case CRUISER: return 25;
            // not stronger than a Radiant. Ziggy is too far
            case CAPITAL_SHIP: return 36;
        }
        return 12;
    }

    // filter out ships likely to be unique bosses and such.
    // If it's something the player can get their hands on, chances are it's OK to beef up
    private boolean isSpecialShip(FleetMemberAPI member) {
        if (FEL_UNCHAINED) return false;
        boolean special = false;
        ShipHullSpecAPI spec = member.getHullSpec();
        // no ships that are never recoverable after battle (so, Derelict/Remnant is OK but something like Omega or the Guardian isn't)
        if (spec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.UNBOARDABLE) && !spec.hasTag(Tags.AUTOMATED_RECOVERABLE)) {
            special = true;
        }

        // actually nevermind screw you, specialcase Guardian to be infestable
        if (spec.getHullId().contains("guardian")) special = false;

        // if the existing captain is more skilled than an integrated Alpha, probably a no-go
        if (member.getCaptain().getStats().getLevel() > 8) {
            special = true;
        }
        return special;
    }
}