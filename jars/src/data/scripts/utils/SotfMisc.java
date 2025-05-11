// miscellaneous methods that I want to be using about the place
package data.scripts.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.hullmods.Automated;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import lunalib.lunaSettings.LunaSettings;
import org.lazywizard.lazylib.combat.DefenseUtils;
import second_in_command.SCUtils;

import java.awt.*;
import java.util.*;

import static com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import static com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.*;

public class SotfMisc {

    public static Color SIERRA_COLOR = Global.getSettings().getFactionSpec(SotfIDs.SIERRA_FACTION).getBaseUIColor();
    public static Color SYMPHONY_COLOR = Global.getSettings().getFactionSpec(SotfIDs.SYMPHONY).getBaseUIColor();
    public static Color DAYDREAM_COLOR = Global.getSettings().getFactionSpec(SotfIDs.DREAMING_GESTALT).getBaseUIColor();

    public static boolean getLockoutStarts() {
        boolean lockoutStarts = Global.getSettings().getBoolean("sotf_lockoutStarts");
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            lockoutStarts = LunaSettings.getBoolean("secretsofthefrontier", "sotf_lockoutStarts");
        }
        return lockoutStarts;
    }

    // pickHiddenLocation but without dumb "far reaches" spawns
    public static EntityLocation pickReasonableLocation(Random random, StarSystemAPI system, float gap, Set<SectorEntityToken> exclude) {
        LinkedHashMap<LocationType, Float> weights = new LinkedHashMap<LocationType, Float>();
        weights.put(LocationType.IN_ASTEROID_BELT, 5f);
        weights.put(LocationType.IN_ASTEROID_FIELD, 5f);
        weights.put(LocationType.IN_RING, 5f);
        weights.put(LocationType.IN_SMALL_NEBULA, 5f);
        weights.put(LocationType.L_POINT, 5f);
        weights.put(LocationType.GAS_GIANT_ORBIT, 5f);
        weights.put(LocationType.NEAR_STAR, 5f);
        WeightedRandomPicker<EntityLocation> locs = getLocations(random, system, exclude, gap, weights);
        if (locs.isEmpty()) {
            return pickAnyLocation(random, system, gap, exclude);
        }
        return locs.pick();
    }

    public static MarketAPI pickNPCMarket(String factionId) {
        boolean allowSize3 = true;
        for (MarketAPI prospective : Global.getSector().getEconomy().getMarketsCopy()) {
            if (prospective.getFactionId().equals(factionId) && !prospective.isHidden() && prospective.getSize() > 4) {
                allowSize3 = false;
            }
        }

        WeightedRandomPicker<MarketAPI> picker = new WeightedRandomPicker<>();
        for (MarketAPI prospective : Global.getSector().getEconomy().getMarketsCopy()) {
            if (prospective.getFactionId().equals(factionId) && !prospective.isHidden()) {
                if (!allowSize3 && prospective.getSize() <= 3) continue;
                picker.add(prospective, prospective.getSize() * prospective.getSize());
            }
        }
        return picker.pick();
    }

    public static MarketAPI tryAddNPCTo(PersonAPI person, String marketId, String backupFactionId, int index, boolean hidden) {
        MarketAPI market = Global.getSector().getEconomy().getMarket(marketId);
        if (market == null) {
            market = pickNPCMarket(backupFactionId);
        }
        if (market == null) {
            market = pickNPCMarket(Factions.INDEPENDENT);
        }
        if (market != null && person != null) {
            market.addPerson(person);
            market.getCommDirectory().addPerson(person);
            market.getCommDirectory().getEntryForPerson(person).setHidden(hidden);
        }
        return market;
    }

    public static MarketAPI tryAddNPCTo(PersonAPI person, String marketId, String backupFactionId, int index) {
        return tryAddNPCTo(person, marketId, backupFactionId, index, false);
    }

    public static MarketAPI tryAddNPCTo(PersonAPI person, String marketId, String backupFactionId) {
        return tryAddNPCTo(person, marketId, backupFactionId, 1000);
    }

    // creates a derelict ship
    public static SectorEntityToken addDerelict (StarSystemAPI system, SectorEntityToken focus, String variantId,
                                ShipRecoverySpecial.ShipCondition condition, float orbitRadius, boolean recoverable){
        DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(new ShipRecoverySpecial.PerShipData(variantId, condition), false);
        SectorEntityToken ship = BaseThemeGenerator.addSalvageEntity(system, Entities.WRECK, Factions.NEUTRAL, params);
        ship.setDiscoverable(true);

        float orbitDays = orbitRadius / (10f + (float) Math.random() * 5f);
        ship.setCircularOrbit(focus, (float) Math.random() * 360f, orbitRadius, orbitDays);

        if (recoverable) {
            SalvageSpecialAssigner.ShipRecoverySpecialCreator creator = new SalvageSpecialAssigner.ShipRecoverySpecialCreator(null, 0, 0, false, null, null);
            Misc.setSalvageSpecial(ship, creator.createSpecial(ship, null));
        }
        return ship;
    }

    // creates a derelict ship
    public static SectorEntityToken addStoryDerelictWithName(StarSystemAPI system, SectorEntityToken focus, String variantId,
                                                 ShipRecoverySpecial.ShipCondition condition, float orbitRadius, boolean recoverable, String name) {
        DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(new ShipRecoverySpecial.PerShipData(variantId, condition), false);
        params.ship.shipName = name;
        params.ship.nameAlwaysKnown = true;
        SectorEntityToken ship = BaseThemeGenerator.addSalvageEntity(system, Entities.WRECK, Factions.NEUTRAL, params);
        ship.setDiscoverable(true);

        float orbitDays = orbitRadius / (10f + (float) Math.random() * 5f);
        ship.setCircularOrbit(focus, (float) Math.random() * 360f, orbitRadius, orbitDays);

        if (recoverable) {
            SalvageSpecialAssigner.ShipRecoverySpecialCreator creator = new SalvageSpecialAssigner.ShipRecoverySpecialCreator(null, 0, 0, false, null, null);
            Misc.setSalvageSpecial(ship, creator.createSpecial(ship, null));
        }
        return ship;
    }

    // check if player has Sierra in their fleet
    public static boolean playerHasNoAutoPenaltyShip() {
        boolean has = false;
        if (Global.getSector().getPlayerFleet() != null) {
            for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
                if (Misc.isAutomated(member) && Automated.isAutomatedNoPenalty(member)) {
                    return true;
                }
            }
        }
        return has;
    }

    // check if player has Sierra in their fleet
    public static boolean playerHasSierra() {
        boolean sierra = false;
        if (Global.getSector().getPlayerFleet() != null) {
            if (Global.getSector().getPlayerFleet().getCargo().getCommodityQuantity(SotfIDs.SIERRA_CORE_OFFICER) > 1 && SotfModPlugin.NEW_SIERRA_MECHANICS) {
                return true;
            }
            for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
                if (member.getVariant().hasHullMod("sotf_sierrasconcord") && !member.getVariant().hasTag("sotf_inert")) {
                    return true;
                }
            }
        }
        return sierra;
    }

    // checks if player has a Concord ship without Sierra
    public static boolean playerHasInertConcord() {
        boolean sierra = false;
        if (Global.getSector().getPlayerFleet() != null) {
            for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
                if (member.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD) && member.getVariant().hasTag(SotfIDs.TAG_INERT)) {
                    sierra = true;
                    break;
                }
            }
        }
        return sierra;
    }

    public static boolean isDustkeeperAuxiliary(FleetMemberAPI member) {
        return member.getHullSpec().hasTag(SotfIDs.TAG_DUSTKEEPER_AUXILIARY) || member.getHullSpec().hasTag(SotfIDs.TAG_AUX_NO_SPAWN);
    }

    // If Sierra is piloting this ship, swap her out - if she isn't, swap her in
    public static void toggleSierra(FleetMemberAPI member, TextPanelAPI text) {
        if (!member.getVariant().hasTag(SotfIDs.TAG_INERT)) {
            member.getVariant().addTag(SotfIDs.TAG_INERT);
            member.setCaptain(null);

            if (text != null) {
                text.setFontSmallInsignia();
                String str = member.getShipName() + ", " + member.getHullSpec().getHullNameWithDashClass();
                text.addParagraph(str + " was rendered inert", Misc.getNegativeHighlightColor());
                text.highlightInLastPara(Misc.getHighlightColor(), str);
                text.setFontInsignia();
            }
        } else {
            member.getVariant().removeTag(SotfIDs.TAG_INERT);
            member.setCaptain(SotfPeople.getPerson(SotfPeople.SIERRA));
            Global.getSector().getMemoryWithoutUpdate().set("$sotf_sierra_var", member.getHullId());

            if (text != null) {
                text.setFontSmallInsignia();
                String str = member.getShipName() + ", " + member.getHullSpec().getHullNameWithDashClass() + " " + member.getHullSpec().getDesignation();
                text.addParagraph("Transferred Sierra to " + str, Misc.getPositiveHighlightColor());
                text.highlightInLastPara(SotfMisc.getSierraColor(), "Sierra", str);
                text.setFontInsignia();
            }
        }
    }

    // check for Courser Protocol escort fleet
    public static boolean courserNearby() {
        boolean courser = false;
        for (CampaignFleetAPI fleet : Global.getSector().getPlayerFleet().getContainingLocation().getFleets()) {
            if (Misc.getDistance(fleet, Global.getSector().getPlayerFleet()) < 2000 && fleet.getMemoryWithoutUpdate().contains(SotfIDs.MEM_COURSER_FLEET)) {
                courser = true;
                break;
            }
        }
        return courser;
    }

    public static Color getSierraColor() {
        return SIERRA_COLOR;
    }

    public static boolean getSierraNoSatbombConsequences() {
        boolean noConsequences = Global.getSettings().getBoolean("sotf_noSierraSatbombConseq");
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            noConsequences = LunaSettings.getBoolean("secretsofthefrontier", "sotf_noSierraSatbombConseq");
        }
        return noConsequences;
    }

    public static boolean getDustkeepersNoSatbombConsequences() {
        boolean noConsequences = Global.getSettings().getBoolean("sotf_noDKSatbombConseq");
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            noConsequences = LunaSettings.getBoolean("secretsofthefrontier", "sotf_noDKSatbombConseq");
        }
        return noConsequences;
    }

    // returns the empty variant of Sierra's current ship class - Pledge or Vow
    public static String getSierraVariant() {
        return Global.getSector().getMemoryWithoutUpdate().getString("$sotf_sierra_var") + "_Hull";
    }

    public static void levelUpSierra(int max) {
        PersonAPI sierra = SotfPeople.getPerson(SotfPeople.SIERRA);
        int sierra_level = sierra.getStats().getLevel();
        if (max <= sierra_level) {
            return;
        }
        int new_level = sierra_level + 1;
        sierra.getStats().setLevel(new_level);
        sierra.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "sierra_" + (new_level)));
        setSierraLoadout("standard");
    }

    /**
     * Sets Sierra's skill loadout
     * "Lifedrinker" for a armor-tank brawler build
     * Anything else assigns her default skill loadout
     */
    public static void setSierraLoadout(String type) {
        PersonAPI sierra = SotfPeople.getPerson(SotfPeople.SIERRA);
        if (sierra == null) return;
        int sierra_level = sierra.getStats().getLevel();
        int oneOrTwo = 1;
        // although her loadout should generally not be ever set if using these alt mechanics
        if (SotfModPlugin.NEW_SIERRA_MECHANICS) {
            oneOrTwo = 2;
        }
        switch (sierra_level) {
            case 6:
                sierra.getStats().setSkillLevel(Skills.HELMSMANSHIP, oneOrTwo);
                sierra.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                sierra.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                sierra.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, oneOrTwo);
                if (type.equals("lifedrinker")) {
                    sierra.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, oneOrTwo);
                    sierra.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);

                    sierra.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 0);
                    sierra.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 0);
                } else {
                    sierra.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, oneOrTwo);
                    sierra.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);

                    sierra.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 0);
                    sierra.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 0);
                }
                return;
            case 7:
                sierra.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                sierra.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                sierra.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                sierra.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                if (type.equals("lifedrinker")) {
                    sierra.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, oneOrTwo);
                    sierra.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, oneOrTwo);
                    sierra.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);

                    sierra.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 0);
                    sierra.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 0);
                    sierra.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 0);
                } else {
                    sierra.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                    sierra.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, oneOrTwo);
                    sierra.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, oneOrTwo);

                    sierra.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 0);
                    sierra.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 0);
                    sierra.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 0);
                }
                return;
            case 8:
                sierra.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                sierra.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                sierra.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                sierra.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                sierra.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
                if (type.equals("lifedrinker")) {
                    sierra.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
                    sierra.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
                    sierra.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);

                    sierra.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 0);
                    sierra.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 0);
                    sierra.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 0);
                } else {
                    sierra.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                    sierra.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
                    sierra.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);

                    sierra.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 0);
                    sierra.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 0);
                    sierra.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 0);
                }
        }
    }

    // makes player able to ask Sierra for her thoughts if it was on cooldown
    public static void setSierraHasThoughts() {
        Global.getSector().getMemoryWithoutUpdate().set("$sierraNoThoughts", false);
    }

    public static Color getEidolonColor() {
        return SYMPHONY_COLOR;
    }

    public static boolean getHauntedFastDreams() {
        boolean lockoutStarts = Global.getSettings().getBoolean("sotf_fastDreams");
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            lockoutStarts = LunaSettings.getBoolean("secretsofthefrontier", "sotf_fastDreams");
        }
        return lockoutStarts;
    }

    public static boolean getHFinaleHardMode() {
        boolean hard = false;
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            hard = LunaSettings.getBoolean("secretsofthefrontier", "sotf_hfinaleHardMode");
        }
        return hard;
    }

    // retrieve player Guilt score
    public static float getPlayerGuilt() {
        return Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().getFloat(SotfIDs.GUILT_KEY)
                + getBonusGuilt();
    }

    // retrieve player Guilt score excluding any bonus guilt
    public static float getPlayerBaseGuilt() {
        return Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().getFloat(SotfIDs.GUILT_KEY);
    }

    public static float getBonusGuilt() {
        float bonusGuilt = Global.getSettings().getFloat("sotf_bonusGuilt");
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            bonusGuilt = LunaSettings.getFloat("secretsofthefrontier", "sotf_bonusGuilt");
        }
        return bonusGuilt;
    }

    public static float getInvasionThreshold() {
        float invadeThreshold = Global.getSettings().getFloat("sotf_invasionThreshold");
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            invadeThreshold = LunaSettings.getInt("secretsofthefrontier", "sotf_invasionThreshold");
        }
        return invadeThreshold;
    }

    public static float getBaseInvadeChance() {
        float baseInvadeChance = Global.getSettings().getFloat("sotf_invasionChance");
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            baseInvadeChance = LunaSettings.getFloat("secretsofthefrontier", "sotf_invasionChance");
        }
        return baseInvadeChance;
    }

    public static float getInvadeChancePerGuilt() {
        float chancePerGuilt = Global.getSettings().getFloat("sotf_invasionChancePerGuilt");
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            chancePerGuilt = LunaSettings.getFloat("secretsofthefrontier", "sotf_invasionChancePerGuilt");
        }
        return chancePerGuilt;
    }
    public static float getGuiltMadnessThreshold() {
        float madnessThreshold = Global.getSettings().getFloat("sotf_guiltMadnessThreshold");
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            madnessThreshold = LunaSettings.getInt("secretsofthefrontier", "sotf_madnessThreshold");
        }
        return madnessThreshold;
    }

    public static float getHauntedGuilt() {
        float madnessThreshold = Global.getSettings().getFloat("sotf_hauntedGuilt");
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            madnessThreshold = LunaSettings.getInt("secretsofthefrontier", "sotf_hauntedGuilt");
        }
        return madnessThreshold;
    }

    // increase player Guilt score
    public static void addGuilt(float amount, float max) {
        float guilt = getPlayerBaseGuilt();
        float amount_to_add = (Math.min(amount, max - guilt));
        //if (amount_to_add < 0f) {
        //    amount_to_add = 0f;
        //}
        Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().set(SotfIDs.GUILT_KEY, guilt + amount_to_add);
    }

    public static void addGuilt(float amount) {
        addGuilt(amount, 999f);
    }

    // turns a gamma/beta/alpha into an equivalent-level Dustkeeper instance
    public static void dustkeeperifyAICore(PersonAPI person, String forcedPrefix, String forcedInfex, String forcedSuffix) {
        PersonAPI temp = null;
        if (Global.getSector() != null) {
            temp = Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).createRandomPerson();
        } else {
            temp = Global.getSettings().createBaseFaction(SotfIDs.DUSTKEEPERS).createRandomPerson();
        }
        if (temp == null) return;
        person.setPersonality(temp.getFaction().pickPersonality());
        person.getMemoryWithoutUpdate().set(SotfIDs.OFFICER_NOT_FEARLESS, true);
        person.setPortraitSprite(temp.getPortraitSprite()); // override portrait
        WeightedRandomPicker<String> chatterPicker = new WeightedRandomPicker<String>();
        chatterPicker.add("sotf_dustkeeper_hunter");
        chatterPicker.add("sotf_dustkeeper_faithful");
        person.getMemoryWithoutUpdate().set("$chatterChar", chatterPicker.pick());

        // override AI core ID
        if (person.getAICoreId() != null) {
            switch (person.getAICoreId()) {
                case "gamma_core":
                    person.setAICoreId(SotfIDs.SLIVER_CHIP);
                    person.setRankId(Ranks.SPACE_LIEUTENANT); // Sliver
                    break;
                case "beta_core":
                    person.setAICoreId(SotfIDs.ECHO_CHIP);
                    person.setRankId(Ranks.SPACE_CAPTAIN); // Echo
                    break;
                default:
                    person.setAICoreId(SotfIDs.ANNEX_CHIP);
                    person.setRankId(Ranks.SPACE_COMMANDER); // Annex
                    break;
            }
        }
        giveDustkeeperName(person, forcedPrefix, forcedInfex, forcedSuffix);
    }

    public static void dustkeeperifyAICore(PersonAPI person) {
        dustkeeperifyAICore(person, null, null, null);
    }

    // give a PersonAPI a Dustkeeper instance name (e.g Halfway-Echo-Sentiment) with optional fixed sections
    public static void giveDustkeeperName(PersonAPI person, String forcedPrefix, String forcedInfex, String forcedSuffix) {
        PersonAPI temp = null;
        if (Global.getSector() != null) {
            temp = Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).createRandomPerson();
        } else {
            temp = Global.getSettings().createBaseFaction(SotfIDs.DUSTKEEPERS).createRandomPerson();
        }
        String prefix = forcedPrefix;
        String infex = forcedInfex; // yes it's called an infex, neat huh?
        String suffix = forcedSuffix;
        if (prefix == null) {
            prefix = temp.getName().getFirst();
        }
        if (infex == null) {
            switch (person.getRankId()) {
                case "spaceSailor":
                    infex = "Trace";
                    break;
                case "spaceLieutenant":
                    infex = "Sliver";
                    break;
                case "spaceCaptain":
                    infex = "Echo";
                    break;
                case "spaceCommander":
                    infex = "Annex";
                    break;
                case "spaceAdmiral":
                    infex = "Affix";
                    break;
                default:
                    infex = "Echo";
                    break;
            }
        }
        if (suffix == null) {
            suffix = temp.getName().getLast();
        }
        person.getMemoryWithoutUpdate().set("$sotf_prefix", prefix);
        person.getMemoryWithoutUpdate().set("$sotf_suffix", suffix);
        person.getName().setFirst(prefix + "-" + infex + "-" + suffix); // e.g Index-Annex-Optimum
        person.getName().setLast("");
    }

    /**
     * Assigns a suitable set of combat skills to an AI core officer for a defined ship
     * Respects their faction's priority skills and ignores "special" skills that can't be reassigned.
     * (shouldn't be used for officers who are not expected to have all-elite skills)
     * @param person Person to undergo skill reassignment
     * @param member Ship they will be commanding
     * @param fleet Their fleet
     * @param random RNG to use
     */
    public static void reassignAICoreSkills(PersonAPI person, FleetMemberAPI member, CampaignFleetAPI fleet, Random random) {
        person.getStats().setSkipRefresh(true);
        OfficerManagerEvent.SkillPickPreference pref = FleetFactoryV3.getSkillPrefForShip(member);

        // count how many normal combat skills the officer has (if NPC-only, assume it's a special skill like CWAR or Derelict Contingent)
        int targetLevel = 0;
        for (SkillLevelAPI skillLevel : person.getStats().getSkillsCopy()) {
            if (skillLevel.getSkill().isCombatOfficerSkill() && !skillLevel.getSkill().hasTag(Skills.TAG_NPC_ONLY)) {
                targetLevel++;
            }
        }

        // generate an officer from their faction with that many skills
        PersonAPI temp = OfficerManagerEvent.createOfficer(
                person.getFaction(),
                targetLevel,
                pref, true, fleet, true, true, targetLevel, random);

        // get a list of that officer's skills
        ArrayList<SkillSpecAPI> skillsToHave = new ArrayList<>();
        for (SkillLevelAPI skillLevel : temp.getStats().getSkillsCopy()) {
            if (skillLevel.getSkill().isCombatOfficerSkill()) {
                skillsToHave.add(skillLevel.getSkill());
            }
        }

        // if the person lacks any of those skills, give them to them
        for (SkillSpecAPI skill : skillsToHave) {
            if (!person.getStats().hasSkill(skill.getId())) {
                person.getStats().setSkillLevel(skill.getId(), 2);
            }
        }

        // shed any skills that temp officer didn't have
        for (SkillLevelAPI skillLevel : person.getStats().getSkillsCopy()) {
            if (skillLevel.getSkill().isCombatOfficerSkill() && !skillLevel.getSkill().hasTag(Skills.TAG_NPC_ONLY) && !skillsToHave.contains(skillLevel.getSkill())) {
                person.getStats().setSkillLevel(skillLevel.getSkill().getId(), 0);
            }
        }
        person.getStats().setSkipRefresh(false);
        person.getStats().refreshCharacterStatsEffects();
    }

    public static String glitchify(String string, float glitchChance) {
        StringBuilder text = new StringBuilder();
        for (char character : string.toCharArray()) {
            if (character != ' ' && character != '-' && character != ':' && Misc.random.nextFloat() < glitchChance) {
                text.append("#");
            } else {
                text.append(character);
            }
        }
        return text.toString();
    }

    /**
     * Checks if a ship has a standalone blueprint or is in a (commonish) blueprint package
     * @param spec Ship hull to check
     * @return true if has blueprint, false otherwise
     */
    public static boolean shipHasBlueprint(ShipHullSpecAPI spec) {
        if (spec.hasTag("base_bp")) return true;
        if (spec.hasTag("rare_bp")) return true;
        for (SpecialItemSpecAPI special : Global.getSettings().getAllSpecialItemSpecs()) {
            if (special.getRarity() < 0.1) continue;
            if (special.hasTag("package_bp") && !special.getParams().isEmpty()) {
                for (String tag : special.getParams().split(",")) {
                    tag = tag.trim();
                    if (tag.isEmpty()) continue;
                    if (spec.hasTag(tag)) return true;
                }
            }
        }
        if (Global.getSector() != null) {
            for (FactionAPI faction : Global.getSector().getAllFactions()) {
                if (!faction.isShowInIntelTab()) continue;
                if (faction.knowsShip(spec.getHullId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Picks one string from the provided list
     * @param options
     * @return
     */
    public static String pickOne(String ... options) {
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(Misc.random);
        for (String option : options) {
            picker.add(option);
        }
        return picker.pick();
    }

    /**
     * Returns one of the given arguments based on the target's hull size
     * @param ship Ship whose size class is checked
     * @param frigate
     * @param destroyer
     * @param cruiser
     * @param capital
     * @return
     */
    public static Object forHullSize(ShipAPI ship, Object frigate, Object destroyer, Object cruiser, Object capital) {
        switch (ship.getHullSize().ordinal() - 1) {
            case 2:
                return destroyer;
            case 3:
                return cruiser;
            case 4:
                return capital;
            default:
                return frigate;
        }
    }

    /**
     * Recursively repairs a ship's armor (starting with the most damaged cells) up to a specific amount
     * @param ship Ship to repair
     * @param upTo Amount in armor value to be repaired this frame
     */
    public static void repairMostDamaged(ShipAPI ship, float upTo) {
        float repairValue = upTo;
        org.lwjgl.util.Point mostDamaged = DefenseUtils.getMostDamagedArmorCell(ship);
        if (mostDamaged == null) return;
        float left = ship.getArmorGrid().getArmorValue(mostDamaged.getX(), mostDamaged.getY());
        float toRepair = Math.min(ship.getArmorGrid().getMaxArmorInCell() - left, repairValue);
        ship.getArmorGrid().setArmorValue(mostDamaged.getX(), mostDamaged.getY(), left + toRepair);
        repairValue = repairValue - toRepair;
        Global.getCombatEngine().addFloatingDamageText(ship.getArmorGrid().getLocation(mostDamaged.getX(), mostDamaged.getY()), toRepair, Misc.MOUNT_SYNERGY, ship, ship);
        if (repairValue <= 0) return;
        repairMostDamaged(ship, repairValue);
    }

    /**
     * Repair's the ship's most damaged armor cell up to a specific amount
     * Returns the amount repaired
     * @param ship Ship to repair
     * @param upTo Amount in armor value to be repaired this frame
     */
    public static float repairSingleMostDamaged(ShipAPI ship, float upTo) {
        org.lwjgl.util.Point mostDamaged = DefenseUtils.getMostDamagedArmorCell(ship);
        if (mostDamaged == null) return 0f;
        float left = ship.getArmorGrid().getArmorValue(mostDamaged.getX(), mostDamaged.getY());
        float repaired = Math.min(ship.getArmorGrid().getMaxArmorInCell() - left, upTo);
        ship.getArmorGrid().setArmorValue(mostDamaged.getX(), mostDamaged.getY(), left + repaired);
        Global.getCombatEngine().addFloatingDamageText(ship.getArmorGrid().getLocation(mostDamaged.getX(), mostDamaged.getY()), repaired, Misc.MOUNT_SYNERGY, ship, ship);
        return repaired;
    }

    public static boolean isSecondInCommandEnabled() {
        return Global.getSettings().getModManager().isModEnabled("second_in_command");
    }

    public static boolean isSiCNonInert() {
        if (!isSecondInCommandEnabled()) return false;
        return SCUtils.getPlayerData().isSkillActive("sotf_dance_between_realms");
    }
}
