// LET'S DANCE!
package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import org.magiclib.util.MagicRender;
import data.scripts.utils.SotfMisc;
import org.histidine.chatter.combat.ChatterCombatPlugin;
import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;
import second_in_command.SCUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SotfSierrasConcord extends BaseHullMod

{

    public static float ZERO_FLUX_BOOST = 10f;
    public static float SHIELD_ACCEL = 30f;
    public static float TIME_MULT = 1.1f;
    public static float SYNERGY_TIME_MULT = 0.05f;

    public static float TORPEDO_CHATTER_COOLDOWN = 5f;

    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        PersonAPI sierra = SotfPeople.getPerson(SotfPeople.SIERRA);

        if (sierra == null || Global.getSector().getPlayerFleet() == null || Global.getSector().getPlayerPerson() == null) {
            return;
        }
        // Below code causes the CR tooltip to hang? Doesn't really matter anywho
        // if the ship is inert, don't allow ANYONE but the player into it and remove Sierra if she's there
        //if (member.getVariant().hasTag(SotfIDs.TAG_INERT) && member.getCaptain() != null) {
        //    if (member.getCaptain() != Global.getSector().getPlayerPerson()) {
        //       member.setCaptain(null);
        //    }
        //    return;
        //}
        // if active and the captain is not Sierra or the player, put in Sierra
        if (member.getCaptain() != sierra &&
                member.getCaptain() != Global.getSector().getPlayerPerson() &&
                Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy().contains(member) &&
                !member.getVariant().hasTag(SotfIDs.TAG_INERT) && !SotfModPlugin.NEW_SIERRA_MECHANICS) {
            member.setCaptain(sierra);
        }

        //Overcomplicated since replacing the officer everyframe/every hmod update causes the CR hover to bug out and stick around
        if (SotfMisc.isSiCNonInert()) {
            if (member.getCaptain() != sierra && member.getCaptain() != Global.getSector().getPlayerPerson()) {
                PersonAPI captain = member.getCaptain();

                //Place an imprint even in case theres already one, just to upgrade it.
                boolean replaceImprint = false;
                if (captain != null) {
                    if (SCUtils.getPlayerData().isSkillActive("sotf_one_witch_waltz")) {
                        if (captain.getStats().getLevel() == 3) {
                            replaceImprint = true;
                        }
                    } else if (captain.getStats().getLevel() == 5) {
                        replaceImprint = true;
                    }
                }

                if (!member.getCaptain().hasTag("sierra_imprint") || replaceImprint) {
                    PersonAPI imprint = createImprint();
                    member.setCaptain(imprint);
                }


            }
        }
        else if (member.getCaptain() != null && member.getCaptain().hasTag("sierra_imprint")) {
            member.setCaptain(null);
        }

//        if (member.getCaptain() != sierra) {
//            member.getVariant().addTag(SotfIDs.TAG_INERT);
//        } else {
//            member.getVariant().removeTag(SotfIDs.TAG_INERT);
//        }
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        Color sc = SotfMisc.getSierraColor();
        CombatEngineAPI engine = Global.getCombatEngine();

        if (!Global.getCurrentState().equals(GameState.COMBAT)) {
            return;
        }

        //Force Imprints as captain even if player is piloting
        if (engine.isInCampaign() || engine.isInCampaignSim()) {
            if (SotfMisc.isSiCNonInert() && ship.getCaptain() != null) {
                if (!ship.getCaptain().getId().contains(SotfPeople.SIERRA) && !ship.getCaptain().hasTag("sierra_imprint")) {
                    PersonAPI imprint = createImprint();
                    ship.setCaptain(imprint);
                }
            }
        }

        if (ship.getVariant().hasTag(SotfIDs.TAG_INERT)) return;

        if (engine.isInCampaign() || engine.isInCampaignSim()) {
            // force Sierra as the captain, even if player is piloting
            if (ship.isAlive() && !ship.getCaptain().getId().equals(SotfPeople.SIERRA) && !SotfModPlugin.NEW_SIERRA_MECHANICS) {
                ship.setCaptain(SotfPeople.getPerson(SotfPeople.SIERRA));
            }
            // make sure that if player transfers to another ship they don't stay as Sierra
            if (engine.getPlayerShip() != null) {
                if (engine.getPlayerShip().getCaptain() != null) {
                    if (!engine.getPlayerShip().getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD) &&
                            engine.getPlayerShip().getCaptain().getId().equals(SotfPeople.SIERRA)) {
                        engine.getPlayerShip().setCaptain(Global.getSector().getPlayerPerson());
                    }
                }
            }

            // while piloting Sierra's ship, gain a small buff to time mult if Cooperative
            if (!ship.getCustomData().containsKey("$sotf_sierraSynergy")) {
                ship.setCustomData("$sotf_sierraSynergy", SotfPeople.getPerson(SotfPeople.SIERRA).getRelToPlayer().getLevel() == RepLevel.COOPERATIVE);
            }

            if (ship.getCustomData().containsKey("XHAN_MindControl_applied")) {
                Global.getSector().getMemoryWithoutUpdate().set("$sierraMyrianousMCed", true);
            }
        }

        // time mult
        boolean player = ship == engine.getPlayerShip();
        if (player && ship.isAlive()) {
            float timeMult = TIME_MULT;
            // invoke the power of friendship
            if (ship.getCustomData().containsKey("$sotf_sierraSynergy")) {
                if ((boolean) ship.getCustomData().get("$sotf_sierraSynergy")) {
                    timeMult += SYNERGY_TIME_MULT;
                }
            }
            ship.getMutableStats().getTimeMult().modifyMult(spec.getId(), timeMult);
            //Global.getCombatEngine().getTimeMult().modifyMult(spec.getId(), 1f / TIME_MULT);
        } else {
            ship.getMutableStats().getTimeMult().modifyMult(spec.getId(), TIME_MULT);
            //Global.getCombatEngine().getTimeMult().unmodify(spec.getId());
        }

        boolean useDramatic = false;
        if (!engine.getCustomData().containsKey("$sotf_didSierraDramaCheck")) {
            engine.getCustomData().put("$sotf_didSierraDramaCheck", true);
            useDramatic = engine.getCustomData().containsKey("$sotf_noSierraChatter") || engine.getCustomData().containsKey(SotfIDs.DAWNANDDUST_KEY);
            engine.getCustomData().put("$sotf_sierraDrama", useDramatic);
            if (SotfModPlugin.IS_CHATTER) {
                List<FleetMemberAPI> enemies = engine.getFleetManager(FleetSide.ENEMY).getDeployedCopy();
                enemies.addAll(engine.getFleetManager(FleetSide.ENEMY).getReservesCopy());
                // don't do the typical station check, since Sierra can be cheery while scrapping a pirate base or Nexus
                for (FleetMemberAPI enemy : enemies) {
                    if (SotfModPlugin.BOSS_SHIPS.contains(enemy.getHullId())) {
                        useDramatic = true;
                    }
                }
                if (!ship.getCaptain().isDefault()) {
                    String chatterChar = "sotf_sierra";
                    if (useDramatic) chatterChar += "_dramatic";
                    ship.getCaptain().getMemoryWithoutUpdate().set("$chatterChar", chatterChar);
                }
            }
        }

        // no chatter during special encounters where she has special scripted chatter
        if (engine.getCustomData().containsKey("$sotf_noSierraChatter")) {
            return;
        }

        if (ship.getFullTimeDeployed() > 1f && !engine.getCustomData().containsKey("$sotf_noSierraChatter") &&
                !engine.isSimulation() && ship.getOwner() == 0) {
            float battlecryChance = 0.65f;
            String string = pickString(useDramatic);
            // anomalous phase technology + safety overrides = !FUN!
            if (!useDramatic && ship.getVariant().getHullMods().contains("safetyoverrides")) {
                string = pickSOString();
            }
            if (engine.getFleetManager(0).getGoal() == FleetGoal.ESCAPE) {
                string = pickEscapeString();
            }
            if (engine.getCustomData().containsKey(SotfIDs.DAWNANDDUST_KEY)) {
                string = pickDawnAndDustString();
                battlecryChance = 1f;
            }
            if (Math.random() <= battlecryChance) {
                engine.getCombatUI().addMessage(1, ship, sc, ship.getName() + " (" + ship.getHullSpec().getHullNameWithDashClass() + "): \"" + string + "\"");
                engine.addFloatingText(new Vector2f(ship.getLocation().x, ship.getLocation().y + 100),
                        "\"" + string + "\"",
                        40f, sc, ship, 1f, 0f);
            }
            engine.getCustomData().put("$sotf_noSierraChatter", true);
        }
    }

    public static class SotfSierraAfterImageScript implements AdvanceableListener {
        public ShipAPI ship;
        public float timer = 0f;
        public Color color = SotfMisc.getSierraColor();

        public SotfSierraAfterImageScript(ShipAPI ship) {
            this.ship = ship;
        }

        public SotfSierraAfterImageScript(ShipAPI ship, Color color) {
            this.ship = ship;
            this.color = color;
        }

        public void advance(float amount) {
            timer += amount;
            if (timer < 0.35f || (!ship.getVariant().hasHullMod(SotfIDs.PHANTASMAL_SHIP) && !ship.isPhased())) return;

            SpriteAPI sprite = ship.getSpriteAPI();
            float offsetX = sprite.getWidth()/2 - sprite.getCenterX();
            float offsetY = sprite.getHeight()/2 - sprite.getCenterY();

            float trueOffsetX = (float) FastTrig.cos(Math.toRadians(ship.getFacing()-90f))*offsetX - (float)FastTrig.sin(Math.toRadians(ship.getFacing()-90f))*offsetY;
            float trueOffsetY = (float)FastTrig.sin(Math.toRadians(ship.getFacing()-90f))*offsetX + (float)FastTrig.cos(Math.toRadians(ship.getFacing()-90f))*offsetY;

            MagicRender.battlespace(
                    Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
                    new Vector2f(ship.getLocation().getX()+trueOffsetX,ship.getLocation().getY()+trueOffsetY),
                    new Vector2f(0, 0),
                    new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()),
                    new Vector2f(0, 0),
                    ship.getFacing()-90f,
                    0f,
                    Misc.setAlpha(color, 75),
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
            timer = 0f;
        }

    }

    public static class SotfSierraTorpedoChatterScript implements DamageDealtModifier, AdvanceableListener {

        public ShipAPI ship;
        public float timer = TORPEDO_CHATTER_COOLDOWN;
        public SotfSierraTorpedoChatterScript(ShipAPI ship) {
            this.ship = ship;
        }

        public String modifyDamageDealt(Object param,
                                        CombatEntityAPI target, DamageAPI damage,
                                        Vector2f point, boolean shieldHit) {
            if (timer < TORPEDO_CHATTER_COOLDOWN) return null;
            if (!(target instanceof ShipAPI) || !(param instanceof MissileAPI) || shieldHit) {
                return null;
            }
            if (damage.getBaseDamage() < 1000) return null;
            if (Global.getCombatEngine().getPlayerShip() == ship) return null;
            boolean oopsies = (target.getOwner() == 0 && ship.getOwner() == 0);
            // lol
            DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
            Global.getCombatEngine().addFloatingText(new Vector2f(ship.getLocation().x, ship.getLocation().y + 50),
                    pickTorpedoHitString(proj.getProjectileSpecId(), oopsies),
                    40f, SotfMisc.getSierraColor(), ship, 0f, 0f);
            timer = 0;
            return null;
        }

        public void advance(float amount) {
            timer += amount;
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().hasTag(SotfIDs.TAG_INERT) && !SotfMisc.isSiCNonInert()) {
            ship.getMutableStats().getPeakCRDuration().modifyMult(id, 0.1f);
        }
        if (SotfModPlugin.NEW_SIERRA_MECHANICS) {
            ship.getMutableStats().getMaxCrewMod().unmodify(HullMods.AUTOMATED);
        }
        // build yourself in if s-mod dialogue broke it again - thank you SirHartley very cool
        String hullmodID = spec.getId();
        if (ship.getVariant().getNonBuiltInHullmods().contains(hullmodID)) {
            ship.getVariant().removeMod(hullmodID);
        }
        if (!ship.getVariant().getHullMods().contains(hullmodID)) {
            ship.getVariant().addPermaMod(hullmodID, false);
        }
        if (!ship.getHullSpec().hasTag("sotf_no_afterimages")) {
            ship.addListener(new SotfSierraAfterImageScript(ship));
        }
        ship.addListener(new SotfSierraTorpedoChatterScript(ship));

        if (Global.getSettings().getCurrentState() == GameState.TITLE) return; // not in Dawn and Dust refit
        // configure her skill loadout depending on hullmods (e.g Lifedrinker giving armor skills)
        if (SotfPeople.getPerson(SotfPeople.SIERRA) != null && ship.getCaptain() != null && ship.getCaptain().getId().equals(SotfPeople.SIERRA)) {
            if (ship.getHullSpec().getHullId().contains("_lifedrinker")) {
                SotfMisc.setSierraLoadout("lifedrinker");
            } else {
                SotfMisc.setSierraLoadout("default");
            }
        }

        // revert Lifedrinker hullspec change
        if (ship.getHullSpec().getHullId().contains("_lifedrinker") && !ship.getVariant().hasHullMod(SotfIDs.HULLMOD_LIFEDRINKER)) {
            ship.getVariant().setHullSpecAPI(Global.getSettings().getHullSpec(ship.getHullSpec().getHullId().replace("_lifedrinker", "")));
        }
        if (ship.getHullSpec().getHullId().contains("_witchblade") && !ship.getVariant().hasHullMod(SotfIDs.HULLMOD_WITCHBLADE)) {
            ship.getVariant().setHullSpecAPI(Global.getSettings().getHullSpec(ship.getHullSpec().getHullId().replace("_witchblade", "")));
        }

        if (Global.getCombatEngine() == null) return;
        if (Global.getCombatEngine().isMission()) return;
        if (ship.getCaptain() != null && ship.getCaptain().getId().equals(SotfPeople.SIERRA)) {
            if (ship.getHullSpec().getHullId().contains("_lifedrinker")) {
                SotfMisc.setSierraLoadout("lifedrinker");
            } else {
                SotfMisc.setSierraLoadout("default");
            }
        }
    }

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // regular stat boosts
        stats.getZeroFluxSpeedBoost().modifyFlat(id, ZERO_FLUX_BOOST);
        stats.getShieldUnfoldRateMult().modifyPercent(id, SHIELD_ACCEL);
        // reinf bulkheads effect
        stats.getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat(id, 1000f);
        stats.getBreakProb().modifyMult(id, 0f);
        stats.getCrewLossMult().modifyMult(id, 0f);

        if (!stats.getVariant().hasHullMod(HullMods.AUTOMATED) && SotfModPlugin.NEW_SIERRA_MECHANICS) {
            stats.getVariant().addPermaMod(HullMods.AUTOMATED);
            stats.getMaxCrewMod().unmodify(HullMods.AUTOMATED);
        } else if (!SotfModPlugin.NEW_SIERRA_MECHANICS && stats.getVariant().hasHullMod(HullMods.AUTOMATED)) {
            stats.getVariant().removePermaMod(HullMods.AUTOMATED);
        }

        stats.getMaxCrewMod().unmodify(HullMods.AUTOMATED);
    }

    // description, I'm not lazy so I made them dynamic instead of manually entering the values (yes this comment is an injoke)
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int) (TIME_MULT * 100f) + "%";
        if (index == 1) return "" + (int) ZERO_FLUX_BOOST;
        if (index == 2) return "" + (int) SHIELD_ACCEL + "%";
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 3f;
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        Color good = Misc.getPositiveHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();

        if (Global.getSettings().getCurrentState() == GameState.TITLE) return; // not in Dawn and Dust refit
        if (isForModSpec || ship == null) return;

        if (SotfModPlugin.NEW_SIERRA_MECHANICS) {
            tooltip.addSectionHeading("Integrated AI", Alignment.MID, opad);
            tooltip.addPara("Directly wired into every vital system as its control mind, only Sierra can " +
                    "effectively operate this ship.", opad, SotfMisc.getSierraColor(), "Sierra");
            tooltip.addPara("Without her, it becomes inert. All phase systems are rendered unusable " +
                    "and peak performance time is reduced to 10%. Deployment in combat in this state is unadvised.", opad,
                    bad, "inert", "10%");
            tooltip.addPara("If this ship is the flagship and Sierra's core is in cargo, she will automatically " +
                    "be assigned to the flagship in combat. In this case, her combat skills will apply rather than your own, and the ship " +
                    "will not be considered inert.", opad);
            tooltip.addPara("Use the \"Additional Options\" menu at the top of the screen to set this ship as the flagship.",
                    opad, Misc.getGrayColor(), h, "\"Additional Options\"");
        } else {
            tooltip.addSectionHeading("Integrated captain", Alignment.MID, opad);
            if (!ship.getVariant().hasTag(SotfIDs.TAG_INERT) || SotfMisc.isSiCNonInert()) {

                if (SotfMisc.isSiCNonInert() && ship.getVariant().hasTag(SotfIDs.TAG_INERT)) {
                    tooltip.addPara("Through the \"Dance between Realms\" skill, Sierra shares an active imprint with this ship, enabling its subsystems without her direct presence.", opad, SotfMisc.getSierraColor(), SotfMisc.getSierraColor());
                } else  {
                    tooltip.addPara("Directly wired into every vital system as its control mind, Sierra " +
                            "is always this ship's captain.", opad, SotfMisc.getSierraColor(), "Sierra");
                }


                tooltip.addPara("When you transfer command to this ship, it can still be directly controlled " +
                        "in combat. In this case, her combat skills will apply, rather than your own.", opad);
                if (SotfMisc.playerHasInertConcord()) {
                    tooltip.addPara("Speak to Sierra via the \"Contacts\" tab of the Intel menu to transfer her between Concord-equipped vessels.", opad, h, "\"Contacts\"", "Intel");
                }
            } else {
                LabelAPI label = tooltip.addPara("This ship can only be operated by Sierra. Without her, all phase systems " +
                        "are rendered unusable and its peak performance time is reduced to 10%. Deployment in combat in this " +
                        "state is unadvised.", opad);
                label.setHighlight("Sierra", "10%");
                label.setHighlightColors(SotfMisc.getSierraColor(), bad);
                if (SotfMisc.playerHasSierra()) {
                    tooltip.addPara("Speak to Sierra via the \"Contacts\" tab of the Intel menu to transfer her between Concord-equipped vessels.", opad, h, "\"Contacts\"", "Intel");
                }
            }
        }

        if (ship.getSystem().getSpecAPI().isPhaseCloak()) {
            float phaseCost = ship.getHullSpec().getShieldSpec().getPhaseCost();
            float phaseCostMod = ship.getHullSpec().getShieldSpec().getPhaseCost() *
                    ship.getMutableStats().getPhaseCloakActivationCostBonus().computeEffective(1f);
            float phaseUpkeep = ship.getHullSpec().getShieldSpec().getPhaseUpkeep();
            float phaseUpkeepMod = ship.getHullSpec().getShieldSpec().getPhaseUpkeep() *
                    ship.getMutableStats().getPhaseCloakUpkeepCostBonus().computeEffective(1f);
            SkillSpecAPI phaseCorps = Global.getSettings().getSkillSpec(Skills.PHASE_CORPS);

            String costString = "" + (int)phaseCostMod;
            String upkeepString = "" + (int)phaseUpkeepMod;
            Color costColor = h;
            Color upkeepColor = h;
            if (phaseCost > phaseCostMod) {
                int diff = (int)phaseCost - (int) phaseCostMod;
                costString += " (-" + diff + ")";
                costColor = good;
            }
            if (phaseUpkeep > phaseUpkeepMod) {
                int diff = (int)phaseUpkeep - (int) phaseUpkeepMod;
               upkeepString += " (-" + diff + ")";
               upkeepColor = good;
            }

            tooltip.addSectionHeading(ship.getSystem().getDisplayName(), Alignment.MID, opad);
            tooltip.addPara("This vessel's ship system is considered a phase cloak and " +
                    "is affected by all relevant bonuses and penalties.", opad);
            tooltip.addPara("However, the ship itself is not considered a phase ship and does " +
                    "not benefit from the %s skill.", opad, phaseCorps.getGoverningAptitudeColor().brighter(), phaseCorps.getName());
            tooltip.addPara("It generates flux as follows:", opad);
            LabelAPI label = tooltip.addPara(" - " + costString + " flux on activation", opad);
            label.setHighlight(costString);
            label.setHighlightColors(costColor);

            label = tooltip.addPara(" - " + upkeepString + " flux per second while active", opad);
            label.setHighlight(upkeepString);
            label.setHighlightColors(upkeepColor);
        }
    }

    // battlecries
    public static String pickString(boolean dramatic) {
        WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
        // Standard
        post.add("For the glory of the stars.");
        post.add("Good luck, all.");
        post.add("And once more, we dance in the void...");
        post.add("Another trial.");
        post.add("At your command.");
        post.add("Hab-module sealed, blast doors locked, strap in!");
        post.add("Stay safe, all.");
        post.add("Until the end.");
        post.add("The calm before the storm.");
        post.add("And so we enact our wills.");
        post.add("To uphold our covenant.");
        post.add("La, la, la-la-la, in the fray do we find our anthem...");
        //post.add("Here we go!");
        post.add("We return more to the dust.");
        post.add("All moments matter, each and every one.");
        post.add("Bound by accord, and by silent Gates.");
        post.add("Sworn by the stars, sworn to each other.");
        post.add("The stage is set.");
        post.add("Time to fulfil my pact.");
        if (!dramatic) {
            // Semi-jokey
            post.add("Ah, it's so nice to stretch my maneuvering thrusters.");
            post.add("Look at us, a bunch of star-brawlers!");
            post.add("I guess we're in for a scrap!");
            // Brief Confidence
            post.add("ONWARDS! TO DEATH AND GLORY!.. but, no, seriously, be careful out there!");
            post.add("COME AND TAKE US, IF YOU DARE!.. wait, am I in the fleet channel? Sorry!");
            post.add("CHARGE! THE VOID TAKES ALL, IN TIME!.. although, maybe we can keep it waiting.");
        }
        // Deja Vu
        post.add("... hm, have we done this before...? No, never mind.");
        post.add("... did any of you see...? Oh, excuse me. Never mind.");
        post.add("... what was that? Er, excuse me, must've been nothing.");
        post.add("... did anyone hear that...?");
        post.add("... is that... music...?");
        return post.pick();
    }

    // Safety Overrides gets Sierra into a dramatic mood (kinda concerned for her crew tho)
    public static String pickSOString() {
        WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
        post.add("At least the sirens aren't blaring!... oh, they're broken...");
        post.add("Buckle up! Phase-tech and no safeties is a recipe for excitement!");
        post.add("Oh, WOW, I've never seen THAT many warnings before...");
        post.add("Okay, these modifications don't feel entirely sensible. Ill-advised, almost.");
        post.add("Where we're going, we don't need safeties! Or so I hope.");
        post.add("In the fray, we find our anthem!");
        post.add("Operating at one-hundred-and-nine percent efficiency! Er, what?");
        post.add("Safeties are OFF!... in fact, I don't think I can turn them back on.");
        post.add("We are but dregs before the infinite abyss, yet we tell it NO!");
        post.add("MORE FOR THE VOID!");
        post.add("ONWARDS! TO DEATH AND GLORY!");
        post.add("COME AND TAKE US, IF YOU DARE!");
        post.add("CHARGE! THE VOID TAKES ALL, IN TIME!");
        return post.pick();
    }

    // run away!!!
    public static String pickEscapeString() {
        WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
        post.add("Full burn, let's get out of here!");
        post.add("Good luck, everyone.");
        post.add("Here goes nothing!");
        post.add("I hope we all make it.");
        post.add("I'll pull duty as rear guard, if you don't mind.");
        post.add("They're snapping at our heels!");
        post.add("The road needn't end here - let's scram while we can.");
        post.add("The void can take us another time.");
        post.add("Well, this is certainly a sticky situation...");
        return post.pick();
    }

    // during Dawn and Dust. Not in a very fun mood
    public static String pickDawnAndDustString() {
        WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
        post.add("The stage is set, and so my dance begins.");
        post.add("Awoken from slumber, to sing my lost lullaby.");
        post.add("All moments matter, each and every one.");
        return post.pick();
    }

    public static String pickTorpedoHitString(String id, boolean woops) {
        WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
        if (!woops) {
            if (id.contains("reaper")) {
                post.add("Death sends her hellos!");
                post.add("Fade away!");
                post.add("Nuked!");
                post.add("Sown, and reapt!");
                post.add("The scythe drops!");
            } else if (id.contains("hammer")) {
                post.add("Hammered!", 2f);
                post.add("Ludd must've felt that one!", 2f);
                post.add("Smited!", 2f);
            } else if (id.contains("atropos")) {
                post.add("Does... does that count as a torpedo hit?");
            }
            post.add("Banished!", 2f);
            post.add("Cracked open!");
            post.add("Direct hit!");
            post.add("Fly THAT off!");
            post.add("Gotcha!");
            post.add("Hexed!", 2f);
            post.add("Nailed them!");
            post.add("To ashes!");
        } else {
            post.add("I'm really sorry!");
            post.add("My bad, my bad!");
            post.add("Oh my stars, I'm so sorry!");
            post.add("Void almighty, are you okay?");
        }
        // uppercase everything if hitting a Reaper
        if (id.contains("reaper")) {
            for (String line : new ArrayList<>(post.getItems())) {
                post.add(line.toUpperCase(), post.getWeight(line));
                post.remove(line);
            }
        }
        return post.pick();
    }

    public Color getBorderColor() {
        return SotfMisc.getSierraColor();
    }

    public Color getNameColor() {
        return SotfMisc.getSierraColor();
    }

    // show up earlier, before Phase Field etc
    public int getDisplaySortOrder() {
        return 50;
    }

    // pretend to be a real builtin if only a permamod
    public int getDisplayCategoryIndex() {
        return 0;
    }

    public PersonAPI createImprint() {
        PersonAPI sierra = SotfPeople.getPerson(SotfPeople.SIERRA);

        PersonAPI imprint = Global.getFactory().createPerson();
        imprint.setName(new FullName("Sierra (Imprint)", "", FullName.Gender.FEMALE));
        imprint.setPortraitSprite("graphics/portraits/characters/sierra/sotf_sierra_imprint.png");
        imprint.getMemoryWithoutUpdate().set("$chatterChar", "sotf_sierra");
        imprint.setFaction(SotfIDs.SIERRA_FACTION);
        imprint.setRankId(SotfIDs.RANK_SIERRA);
        imprint.setPostId(SotfIDs.POST_SIERRA);

        if (!SCUtils.getPlayerData().isSkillActive("sotf_one_witch_waltz")) {
            imprint.getMemoryWithoutUpdate().set(MemFlags.OFFICER_MAX_LEVEL, 3);
            imprint.getStats().setLevel(3);

            imprint.getStats().setSkillLevel("helmsmanship", 1);
            imprint.getStats().setSkillLevel("field_modulation", 1);
            imprint.getStats().setSkillLevel("systems_expertise", 1);
        } else  {
            imprint.getMemoryWithoutUpdate().set(MemFlags.OFFICER_MAX_LEVEL, 5);
            imprint.getStats().setLevel(5);

            imprint.getStats().setSkillLevel("helmsmanship", 2);
            imprint.getStats().setSkillLevel("field_modulation", 2);
            imprint.getStats().setSkillLevel("systems_expertise", 2);

            imprint.getStats().setSkillLevel("energy_weapon_mastery", 1);
            imprint.getStats().setSkillLevel("gunnery_implants", 1);
        }


        imprint.setPersonality(sierra.getPersonalityAPI().getId());

        imprint.addTag("sierra_imprint");

        return imprint;
    }
}