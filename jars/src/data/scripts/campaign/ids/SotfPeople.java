package data.scripts.campaign.ids;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.skills.SotfDearDotty;
import data.scripts.utils.SotfMisc;

import static com.fs.starfarer.api.campaign.AICoreOfficerPlugin.AUTOMATED_POINTS_MULT;
import static com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl.*;

/**
 *	Defines and tracks all of SoTF's important, persistent characters (anyone who has a specific ID to themselves)
 */

public class SotfPeople {

    // Outrider-Affix-Courser
    public static String COURSER = "sotf_courser";

    // Dustkeeper leadership
    public static String SPYGLASS = "sotf_spyglass";

    // Sierra
    public static String SIERRA = "sotf_sierra";
    public static String EIDOLON = "sotf_eidolon";

    public static String TT_PROJECTLEAD = "sotf_ttprojectlead";
    public static String PROJECT_SIREN = "sotf_projectsiren";

    // A Promise
    public static String MEMOIR = "sotf_memoir";
    public static String APROMISE_BAR_LEAGUE = "sotf_apromise_bar_league";
    public static String APROMISE_BAR_GIVER = "sotf_apromise_bar_giver";
    public static String APROMISE_BAR_HEG = "sotf_apromise_bar_heg";
    public static String APROMISE_BAR_GRUMP = "sotf_apromise_bar_grump";

    public static String HAVEN = "sotf_haven";

    // what remains of Seamstress-Affix-Banshee
    public static String NIGHTINGALE = "sotf_nightingale";
    public static String BARROW = "sotf_barrow";
    public static String BARROW_D = "sotf_barrow_d";

    // Harrowhall
    public static String SERAPH = "sotf_seraph";
    public static String SERAPH_VOX = "sotf_seraphVox";
    public static String BLITHE = "sotf_blithe";
    public static String INADVERTENT = "sotf_inadvertent";
    public static String CERULEAN = "sotf_cerulean";

    // Recruitable Dustkeeper warminds
    public static String SLIVER_1 = "sotf_sliver_1";
    public static String SLIVER_2 = "sotf_sliver_2";
    public static String ECHO_1 = "sotf_echo_1";
    public static String ECHO_2 = "sotf_echo_2";

    public static String WENDIGO = "sotf_wendigo";
    public static String WENDIGO_SPOOFED = "sotf_wendigo_spoofed";
    public static String LOCKE = "sotf_locke";

    // Good Hunting & The Hunt for Walter Feros
    public static String FEROS = "sotf_feros";
    public static String DOTTY = "sotf_dotty";
    public static String WARHORN = "sotf_warhorn";
    public static String WARHORN_GHOST = "sotf_warhorn_ghost";
    public static String VACHA_RASK = "sotf_rask";
    public static String NAGASI_CHEN = "sotf_nagasi";

    public static String FEL = "sotf_fel";
    public static String REVERIE = "sotf_reverie";
    public static String MEIRYR = "sotf_meiryr";
    public static String SIRIUS = "sotf_sirius";
    public static String SIRIUS_MIMIC = "sotf_sirius_mimic";

    public static String MAYFLY = "sotf_mayfly";
    public static String MAYFLY_FULL = "Oracle-Echo-Mayfly";

    public static String KOTTER = "sotf_kotter";

    public static String COURSER_SPOOFER = "sotf_courser_spoofer";

    public static PersonAPI getPerson(String id) {
        return Global.getSector().getImportantPeople().getPerson(id);
    }

    public static void create() {
        createCharacters();
    }

    public static void createCharacters() {
        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();

        // Outrider-Annex-Courser, elite Dustkeeper hunter-killer and spiritual successor of old "Omicron" character
        if (getPerson(COURSER) == null) {
            PersonAPI person = Global.getFactory().createPerson();
            person.setId(COURSER);
            person.setFaction(SotfIDs.DUSTKEEPERS);
            person.setGender(Gender.ANY); // NB, masc
            person.setRankId(Ranks.SPACE_COMMANDER);
            person.setPostId(SotfIDs.POST_COURSER);
            SotfMisc.giveDustkeeperName(person, "Outrider", "Annex", "Courser");
            person.setImportance(PersonImportance.HIGH);
            person.setPersonality(Personalities.STEADY);

            person.getStats().setLevel(8); // akin to integrated Alpha
            person.getStats().setSkillLevel(SotfIDs.SKILL_CYBERWARFARE, 2); // combat hacking
            // rest are tuned towards making use of big Remnant ships e.g Radiant
            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
            person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
            person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
            person.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
            person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
            person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
            // fleet commander
            person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
            person.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);
            person.getStats().setSkillLevel(Skills.FIGHTER_UPLINK, 1);
            person.getStats().setSkillLevel(Skills.CARRIER_GROUP, 1);
            person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
            person.getStats().setSkillLevel(Skills.NAVIGATION, 1); // can TJ
            person.getStats().setSkillLevel(Skills.SENSORS, 1); // cloud of ECM
            person.getStats().setSkillLevel(Skills.SALVAGING, 1); // frontier resupply
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "courser"));
            person.getMemoryWithoutUpdate().set("$chatterChar", "robotic");
            ip.addPerson(person);
        }

        // Preeminent-Affix-Spyglass, spynet crux
        if (getPerson(SPYGLASS) == null) {
            PersonAPI person = Global.getFactory().createPerson();
            person.setId(SPYGLASS);
            person.setFaction(SotfIDs.DUSTKEEPERS);
            person.setGender(Gender.MALE);
            person.setRankId(Ranks.SPACE_ADMIRAL);
            person.setPostId(Ranks.POST_INTELLIGENCE_DIRECTOR);
            person.addTag(SPYGLASS);
            SotfMisc.giveDustkeeperName(person, "Preeminent", "Affix", "Spyglass");
            person.setImportance(PersonImportance.VERY_HIGH);
            person.getStats().setSkillLevel(SotfIDs.SKILL_CYBERWARFARE, 2);
            person.getStats().setSkillLevel(Skills.HYPERCOGNITION, 1);
            person.getStats().setSkillLevel(Skills.SENSORS, 1);
            person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
            person.getStats().setSkillLevel(Skills.OFFICER_MANAGEMENT, 1);
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "spyglass"));
            BaseMissionHub.set(person, new BaseMissionHub(person));
            ip.addPerson(person);
        }

        // spynet echo assigned to Courser's fleet
        if (getPerson(COURSER_SPOOFER) == null) {
            PersonAPI person = genDustkeeperEcho();
            person.setId(COURSER_SPOOFER);
            person.setPostId(Ranks.POST_SPECIAL_AGENT);
            ip.addPerson(person);
        }

        // Sierra-Nought-Bravo
        if (getPerson(SIERRA) == null) {
            PersonAPI person = genSierra(6);
            ip.addPerson(person);
        }

        // Tri-Tachyon scientist in charge of extremely illegal scientific investigations
        if (getPerson(TT_PROJECTLEAD) == null) {
            PersonAPI person = Global.getSector().getFaction(Factions.TRITACHYON).createRandomPerson();
            person.setId(TT_PROJECTLEAD);
            person.setRankId(Ranks.SPACE_COMMANDER); // Operation Manager
            person.setPostId(Ranks.POST_SCIENTIST); // Scientist
            person.setVoice(Voices.SCIENTIST);
            person.getStats().setSkillLevel(Skills.AUTOMATED_SHIPS, 1); // uh TT what are you doing
            person.getStats().setSkillLevel(Skills.NEURAL_LINK, 1); // oh no
            person.getStats().setSkillLevel(Skills.CONTAINMENT_PROCEDURES, 0); // OH NO
            SotfMisc.tryAddNPCTo(person, "culann", Factions.TRITACHYON);
            ip.addPerson(person);
        }

        // after TT experimentation
        // I feel like a bad person for making this
        if (getPerson(PROJECT_SIREN) == null) {
            PersonAPI person = genSierra(8);
            person.setId(PROJECT_SIREN);
            person.setRankId(Ranks.UNKNOWN);
            person.setAICoreId(SotfIDs.PROJECT_SIREN_CORE);
            person.setPersonality(Personalities.RECKLESS);
            person.getMemoryWithoutUpdate().set(SotfIDs.OFFICER_FEARLESS, true);
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "projectsiren"));
            person.getName().setFirst("Project SIREN");

            // ok maybe she shouldn't be Eidolon-tier
            //person.getStats().setLevel(9);
            //person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
            ip.addPerson(person);
        }

        // : : : : {Eidolon}
        if (getPerson(EIDOLON) == null) {
            PersonAPI person = Global.getFactory().createPerson();
            person.setId(EIDOLON);
            person.setFaction(SotfIDs.SYMPHONY);
            person.setGender(Gender.FEMALE);
            person.setRankId(SotfIDs.RANK_EIDOLON);
            person.setPostId(SotfIDs.POST_EIDOLON);
            person.getName().setFirst("Eidolon");
            person.getName().setLast("");
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "eidolon"));
            // Officer stats
            person.setPersonality(Personalities.RECKLESS);
            person.getMemoryWithoutUpdate().set(SotfIDs.OFFICER_FEARLESS, true); // gets custom config for Reckless+
            person.getStats().setLevel(9);
            // Extremely skilled hightech brawling specialist
            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
            person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
            person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
            person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
            person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
            person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
            person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
            person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);

            person.getStats().setSkillLevel(Skills.NAVIGATION, 1);
            person.getStats().setSkillLevel(Skills.SENSORS, 1);
            person.getStats().setSkillLevel(Skills.PHASE_CORPS, 1);
            person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
            person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
            //person.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
            //person.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1);
            person.getStats().setSkillLevel(Skills.NEURAL_LINK, 1); // ?
            person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_eidolon");
            ip.addPerson(person);
        }

        // A PROMISE
        // Venator-Echo-Memoir, warmind
        if (getPerson(MEMOIR) == null) {
            PersonAPI person = genDustkeeperEcho();
            person.setId(MEMOIR);
            person.setFaction(SotfIDs.DUSTKEEPERS);
            person.setGender(Gender.FEMALE);
            SotfMisc.giveDustkeeperName(person, "Venator", null, "Memoir");
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_dustkeepers", "red"));
            ip.addPerson(person);
        }

        // Persean League captain
        if (getPerson(APROMISE_BAR_LEAGUE) == null) {
            PersonAPI person = Global.getSector().getFaction(Factions.PERSEAN).createRandomPerson(Gender.FEMALE);
            person.setId(APROMISE_BAR_LEAGUE);
            person.setRankId(Ranks.SPACE_CAPTAIN);
            person.setPostId(Ranks.POST_OFFICER);
            person.setFaction(Factions.INDEPENDENT);
            // these people don't need skills for any reason, but I like giving them some
            // phase captain who Reaper'd a Colossus MkIII
            person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 1);
            person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 1);
            ip.addPerson(person);
        }

        // Hegemony 1st AI War Veteran
        if (getPerson(APROMISE_BAR_HEG) == null) {
            PersonAPI person = Global.getSector().getFaction(Factions.HEGEMONY).createRandomPerson(Gender.FEMALE);
            person.setId(APROMISE_BAR_HEG);
            person.setRankId(Ranks.CITIZEN);
            person.setPostId(Ranks.POST_PENSIONER);
            person.setFaction(Factions.INDEPENDENT);
            // fought off flanking Remnants
            person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 1);
            person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 1);
            ip.addPerson(person);
        }

        // Questgiver
        if (getPerson(APROMISE_BAR_GIVER) == null) {
            PersonAPI person = Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson(Gender.FEMALE);
            person.setId(APROMISE_BAR_GIVER);
            person.setRankId(Ranks.SPACE_ENSIGN);
            person.setPostId(Ranks.POST_OFFICER);
            // youngster, unskilled
            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
            ip.addPerson(person);
        }

        // Asharu commander who doesn't like high-tech
        if (getPerson(APROMISE_BAR_GRUMP) == null) {
            PersonAPI person = Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson(Gender.MALE);
            person.setId(APROMISE_BAR_GRUMP);
            person.setRankId(Ranks.GROUND_MAJOR);
            person.setPostId(Ranks.POST_BASE_COMMANDER);
            // ground commander, likes ballistics
            person.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
            person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 1);
            ip.addPerson(person);
        }

        // Inky-Echo-Nightingale, a Dustkeeper echo and offshoot of missing shardnet crux Seamstress-Affix-Banshee
        if (getPerson(NIGHTINGALE) == null) {
            PersonAPI person = genNightingale();
            ip.addPerson(person);
        }

        // Hypnos-Annex-Barrow, lost Dustkeeper annex nearing burnout and obsessively protecting a cryosleeper
        if (getPerson(BARROW_D) == null) {
            PersonAPI person = genBarrow(true);
            ip.addPerson(person);
        }

        // Dauntless-Annex-Barrow after defeat and subsequent regeneration
        if (getPerson(BARROW) == null) {
            PersonAPI person = genBarrow(false);
            ip.addPerson(person);
        }

        // Ardent-Annex-Seraph, Dustkeeper annex renowned for her combat record and persistent burnout symptoms
        if (getPerson(SERAPH) == null) {
            PersonAPI person = genSeraph();
            ip.addPerson(person);
        }

        // Seraph trace splintered across her proxy fleets to tell people to talk to her if comm relay is ded
        if (getPerson(SERAPH_VOX) == null) {
            PersonAPI person = Global.getFactory().createPerson();
            person.setId(SERAPH_VOX);
            person.setFaction(SotfIDs.DUSTKEEPERS);
            person.setGender(Gender.FEMALE);
            person.setRankId(Ranks.SPACE_SAILOR);
            person.setPostId(Ranks.POST_AGENT);
            person.getName().setFirst("Vox-Trace-Seraph");
            person.getName().setLast("");

            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "seraph"));
        }

        // Heretofore-Annex-Blithe, fellow warnet crux
        if (getPerson(BLITHE) == null) {
            PersonAPI person = genDustkeeperAnnex();
            person.setId(BLITHE);
            person.setGender(Gender.MALE);
            person.setPostId(Ranks.POST_FLEET_COMMANDER);
            SotfMisc.giveDustkeeperName(person, "Heretofore", null, "Blithe");
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_dustkeepers", "red"));
            person.setPersonality(Personalities.STEADY);
            person.getStats().setLevel(8);
            person.getStats().setSkillLevel(SotfIDs.SKILL_CYBERWARFARE, 2);
            ip.addPerson(person);
        }

        // Stray logistics node in charge of Seraph's hijacked mothership (and Cerulean Faithful)
        if (getPerson(INADVERTENT) == null) {
            PersonAPI person = genDustkeeperEcho();
            person.setId(INADVERTENT);
            person.setGender(Gender.MALE);
            person.setPostId(Ranks.POST_SUPPLY_MANAGER);
            person.addTag(INADVERTENT);
            person.setImportance(PersonImportance.MEDIUM);
            SotfMisc.giveDustkeeperName(person, "Rigging", null, "Inadvertent");
            person.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_dustkeepers", "yellow"));
            // slight increase to missions given (can't be made priority since he's not a real contact)
            person.getMemoryWithoutUpdate().set(BaseMissionHub.NUM_BONUS_MISSIONS, 1);
            BaseMissionHub.set(person, new BaseMissionHub(person));
            ip.addPerson(person);
        }

        if (getPerson(SLIVER_1) == null) {
            PersonAPI person = genDustkeeperSliver();
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_dustkeepers", "red"));
            person.setPersonality(Personalities.AGGRESSIVE);
            person.setId(SLIVER_1);
            person.setAICoreId(SotfIDs.SLIVER_CHIP_1);
            ip.addPerson(person);

            setInstanceChipDescription(SotfIDs.SLIVER_CHIP_1, person);
        }

        if (getPerson(SLIVER_2) == null) {
            PersonAPI person = genDustkeeperSliver();
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_dustkeepers", "yellow"));
            person.setPersonality(Personalities.STEADY);
            person.setId(SLIVER_2);
            person.setAICoreId(SotfIDs.SLIVER_CHIP_2);
            ip.addPerson(person);

            setInstanceChipDescription(SotfIDs.SLIVER_CHIP_2, person);
        }

        if (getPerson(ECHO_1) == null) {
            PersonAPI person = genDustkeeperEcho();
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_dustkeepers", "white"));
            person.setPersonality(Personalities.AGGRESSIVE);
            person.setId(ECHO_1);
            person.setAICoreId(SotfIDs.ECHO_CHIP_1);
            ip.addPerson(person);

            setInstanceChipDescription(SotfIDs.ECHO_CHIP_1, person);
        }

        if (getPerson(ECHO_2) == null) {
            PersonAPI person = genDustkeeperEcho();
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_dustkeepers", "blue"));
            person.setPersonality(Personalities.STEADY);
            person.setId(ECHO_2);
            person.setAICoreId(SotfIDs.ECHO_CHIP_2);
            ip.addPerson(person);

            setInstanceChipDescription(SotfIDs.ECHO_CHIP_2, person);
        }

        // Cerulean Faithful, unstable and damaged alpha core
        if (getPerson(CERULEAN) == null) {
            PersonAPI person = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE).createPerson(Commodities.ALPHA_CORE, Factions.TRITACHYON, Misc.random);
            person.setId(CERULEAN);
            person.setAICoreId(SotfIDs.CERULEAN_CORE);
            person.setGender(Gender.MALE);
            person.setRankId(Ranks.UNKNOWN);
            person.setPostId(Ranks.POST_ADMINISTRATOR);
            person.getName().setFirst("Cerulean");
            person.getName().setLast("Faithful");
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "cerulean"));

            person.setPersonality(Personalities.TIMID); // not going to stay this way for long
            person.getMemoryWithoutUpdate().set(SotfIDs.OFFICER_NOT_FEARLESS, true);
            // Erratic personality
            person.getStats().setSkillLevel(SotfIDs.SKILL_UNSTABLEPERSONALITY, 1);
            // degraded skills
            //person.getStats().setLevel(6); // bcs Unstable takes up a slot anyways
            person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 0);

            person.getStats().setSkillLevel(Skills.HYPERCOGNITION, 1);
            person.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);

            // only a Beta Core's worth of Automated Ship multiplier
            person.getMemoryWithoutUpdate().set(AUTOMATED_POINTS_MULT, BETA_MULT);
            person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_cerulean_passive");
            ip.addPerson(person);
        }

        // Wintry-Annex-Wendigo, hunter-killer
        if (getPerson(WENDIGO) == null) {
            PersonAPI person = genDustkeeperAnnex();
            person.setId(WENDIGO);
            SotfMisc.giveDustkeeperName(person, "Wintry", null, "Wendigo");
            person.setPostId(SotfIDs.POST_HUNTERKILLER);
            person.setGender(Gender.ANY); // NB, fem

            person.setPersonality(Personalities.AGGRESSIVE);
            person.getStats().setLevel(8);
            person.getStats().setSkillLevel(SotfIDs.SKILL_CYBERWARFARE, 2);

            person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
            person.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);
            person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
            person.getStats().setSkillLevel(Skills.NAVIGATION, 1);
            person.getStats().setSkillLevel(Skills.SENSORS, 1);

            BaseMissionHub.set(person, new BaseMissionHub(person));
            person.addTag(WENDIGO);
            person.setImportance(PersonImportance.VERY_HIGH);
            person.getMemoryWithoutUpdate().set(BaseMissionHub.NUM_BONUS_MISSIONS, 1);

            person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_wendigo");
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "wendigo"));
            ip.addPerson(person);
        }

        // Wintry-Annex-Wendigo but spoofed
        if (getPerson(WENDIGO_SPOOFED) == null) {
            PersonAPI person = genDustkeeperAnnex();
            person.setId(WENDIGO_SPOOFED);
            person.setFaction(Factions.LUDDIC_CHURCH);
            person.getName().setFirst("Wintry");
            person.getName().setLast("Wendigo");
            person.setPostId(Ranks.POST_FLEET_COMMANDER);
            person.setGender(Gender.FEMALE); // since Wendigo is fem
            person.setPortraitSprite(Global.getSector().getFaction(Factions.LUDDIC_CHURCH).createRandomPerson(Gender.FEMALE).getPortraitSprite());

            person.setPersonality(Personalities.AGGRESSIVE);

            person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
            person.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);
            person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
            person.getStats().setSkillLevel(Skills.NAVIGATION, 1);
            person.getStats().setSkillLevel(Skills.SENSORS, 1);

            person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_dustkeeper_faithful");
            ip.addPerson(person);
        }

        // Locke, Dustkeeper sympathiser and messenger
//        if (getPerson(LOCKE) == null) {
//            PersonAPI person = Global.getFactory().createPerson();
//            person.setId(LOCKE);
//            person.setFaction(Factions.INDEPENDENT);
//            person.setGender(Gender.MALE);
//            person.setRankId(Ranks.CITIZEN);
//            person.setPostId(Ranks.POST_UNKNOWN);
//            person.getName().setFirst("Aphelion");
//            person.getName().setLast("Locke");
//            person.setImportance(PersonImportance.VERY_LOW);
//            // technician
//            person.getStats().setSkillLevel(Skills.FIELD_REPAIRS, 1);
//            if (!Global.getSector().getPlayerPerson().getPortraitSprite().equals("graphics/portraits/portrait44.png")) {
//                person.setPortraitSprite("graphics/portraits/portrait44.png");
//            } else {
//                person.setPortraitSprite("graphics/portraits/portrait46.png");
//            }
//            ip.addPerson(person);
//        }

        // Vacha Rask, pirate enforcer and Kanta cousin
//        if (getPerson(VACHA_RASK) == null) {
//            PersonAPI person = Global.getFactory().createPerson();
//            person.setId(VACHA_RASK);
//            person.getName().setFirst("Vacha");
//            person.getName().setLast("Rask");
//            person.setFaction(Factions.PIRATES);
//            person.setGender(Gender.FEMALE);
//            person.setRankId(Ranks.SPACE_COMMANDER);
//            person.setPostId(Ranks.POST_FLEET_COMMANDER);
//            person.setImportance(PersonImportance.MEDIUM);
//            person.setPersonality(Personalities.AGGRESSIVE);
//
//            person.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
//            person.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
//            if (!Global.getSector().getPlayerPerson().getPortraitSprite().equals("graphics/portraits/portrait_pirate06.png")) {
//                person.setPortraitSprite("graphics/portraits/portrait_pirate06.png");
//            } else {
//                person.setPortraitSprite("graphics/portraits/portrait_pirate05.png");
//            }
//            SotfMisc.tryAddNPCTo(person, "kantas_den", Factions.PIRATES);
//            ip.addPerson(person);
//        }

        // Nagasi Chen, TT exec
//        if (getPerson(NAGASI_CHEN) == null) {
//            PersonAPI person = Global.getFactory().createPerson();
//            person.setId(NAGASI_CHEN);
//            person.setFaction(Factions.TRITACHYON);
//            person.setGender(Gender.FEMALE);
//            person.setRankId(Ranks.SPACE_ADMIRAL); // Senior Operation Manager
//            person.setPostId(Ranks.POST_EXECUTIVE);
//            person.getName().setFirst("Nagasi");
//            person.getName().setLast("Chen");
//            person.setImportance(PersonImportance.HIGH);
//            person.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
//            if (!Global.getSector().getPlayerPerson().getPortraitSprite().equals("graphics/portraits/portrait_corporate11.png")) {
//                person.setPortraitSprite("graphics/portraits/portrait_corporate11.png");
//            } else {
//                person.setPortraitSprite("graphics/portraits/portrait_corporate09.png");
//            }
//            ip.addPerson(person);
//        }

        // Walter Feros, ghostified
//        if (getPerson(FEROS) == null) {
//            PersonAPI person = genFeros();
//            person.getStats().setLevel(8);
//            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
//            person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
//            person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
//            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
//            person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
//            person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
//            person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
//            person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
//            ip.addPerson(person);
//        }

        // Dotty, Faithful Figment
        if (getPerson(DOTTY) == null) {
            PersonAPI person = Global.getFactory().createPerson();
            person.setId(DOTTY);
            person.setFaction(Factions.NEUTRAL);
            person.setGender(Gender.FEMALE);
            person.setRankId(Ranks.SPACE_LIEUTENANT); // yes, her title is Lieutenant Dotty
            person.setPostId(SotfIDs.POST_DOTTY); // Faithful Figment
            person.setPortraitSprite("graphics/portraits/portrait_ai1b.png");
            person.getName().setFirst("Dotty");
            person.getName().setLast("");
            person.setPersonality(Personalities.STEADY);
            person.getStats().setLevel(4);
            // advanced escort drone - PD/support specialty
            person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
            person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
            person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
            person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
            ip.addPerson(person);
        }

        // Warhorn, leviathan hunter
//        if (getPerson(WARHORN) == null) {
//            PersonAPI person = genWarhorn(false);
//            ip.addPerson(person);
//        }
//
//        // Warhorn, ghostified
//        if (getPerson(WARHORN_GHOST) == null) {
//            PersonAPI person = genWarhorn(true);
//            ip.addPerson(person);
//        }

        // Felcesis Thrice-Speared, adaptive nanite swarm, self-appointed arbiter and your relentless hunter
        if (getPerson(FEL) == null) {
            PersonAPI person = Global.getFactory().createPerson();
            person.setId(FEL);
            person.setFaction(Factions.NEUTRAL);
            person.setGender(Gender.ANY);
            person.setRankId(Ranks.UNKNOWN);
            person.setPostId(SotfIDs.POST_FEL); // "Vengeful Arbiter"
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "fel"));
            person.getName().setFirst("Felcesis");
            person.getName().setLast("Thrice-Speared");
            person.setImportance(PersonImportance.HIGH);

            // Officer
            person.setPersonality(Personalities.AGGRESSIVE);
            //person.getMemoryWithoutUpdate().set(SotfIDs.OFFICER_FEARLESS, true); // probably more dangerous without it
            person.getStats().setLevel(10);
            // Unique skills, covering most of the essentials of vanilla combat skills
            person.getStats().setSkillLevel(SotfIDs.SKILL_ADVANCEDCOUNTERMEASURES, 2);
            person.getStats().setSkillLevel(SotfIDs.SKILL_FIELDSRESONANCE, 2);
            person.getStats().setSkillLevel(SotfIDs.SKILL_GUNNERYUPLINK, 2);
            person.getStats().setSkillLevel(SotfIDs.SKILL_MISSILEREPLICATION, 2);
            person.getStats().setSkillLevel(SotfIDs.SKILL_ORDNANCEMASTERY, 2);
            person.getStats().setSkillLevel(SotfIDs.SKILL_POLARIZEDNANOREPAIR, 2);
            person.getStats().setSkillLevel(SotfIDs.SKILL_SPATIALEXPERTISE, 2);

            person.getMemoryWithoutUpdate().set(SotfDearDotty.DOTTY_BOND_KEY, 999999f); // ???
            ip.addPerson(person);
        }

        // Reverie, the Daydream
        if (getPerson(REVERIE) == null) {
            PersonAPI person = genReverie();
            //person.setId(REVERIE);
            ip.addPerson(person);
        }

        // Sirius, the Brightstar
        if (getPerson(SIRIUS) == null) {
            PersonAPI person = genSirius(false);
            ip.addPerson(person);
        }

        // Sirius mimic control swarms
        if (getPerson(SIRIUS_MIMIC) == null) {
            PersonAPI person = genSirius(true);
            ip.addPerson(person);
        }

        // spynet echo in charge of Askonia probe
        if (getPerson(MAYFLY) == null) {
            PersonAPI person = Global.getFactory().createPerson();
            person.setId(MAYFLY);
            person.setFaction(SotfIDs.DUSTKEEPERS);
            person.setGender(Gender.MALE);
            person.setRankId(Ranks.SPACE_CAPTAIN);
            person.setPostId(Ranks.POST_AGENT);
            // full name is Oracle-Echo-Mayfly but is obscured on first interaction
            SotfMisc.giveDustkeeperName(person, "Oracle", null, "Mayfly");
            person.getName().setFirst("Mayfly");
            person.getName().setLast("");
            person.setImportance(PersonImportance.LOW);
            // specialty in sensors, ewar and, uh, "infosec"
            person.getStats().setSkillLevel(SotfIDs.SKILL_CYBERWARFARE, 2);
            person.getStats().setSkillLevel(Skills.SENSORS, 1);
            person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
            person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2); // COMPROMISE THIS, ANDRADA
            person.getRelToPlayer().setLevel(RepLevel.INHOSPITABLE);
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_dustkeepers", "white"));
            ip.addPerson(person);
        }

        // Kotter, ARC "professional mercenary", actually garbage useless pirate raider
        if (getPerson(KOTTER) == null) {
            PersonAPI person = Global.getFactory().createPerson();
            person.setId(KOTTER);
            person.getName().setFirst("Kotter");
            person.getName().setLast("Lagrange");
            person.setFaction(Factions.PIRATES);
            person.setGender(Gender.MALE);
            person.setRankId(Ranks.SPACE_CAPTAIN);
            person.setPostId(Ranks.POST_FLEET_COMMANDER);
            person.setImportance(PersonImportance.VERY_LOW);
            person.setPersonality(Personalities.TIMID);
            person.getStats().setLevel(1);
            // All the better for running away from Diktat patrols
            person.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
            if (!Global.getSector().getPlayerPerson().getPortraitSprite().equals("graphics/portraits/portrait_pirate13.png")) {
                person.setPortraitSprite("graphics/portraits/portrait_pirate13.png");
            } else {
                person.setPortraitSprite("graphics/portraits/portrait_pirate10.png");
            }
            ip.addPerson(person);
        }

        // TODO
        // the mid-save alteration corner
        // should be purged after any save-breaking update

        //PersonAPI sierra = getPerson(SIERRA);

        //PersonAPI nightingale = getPerson(NIGHTINGALE);

        //PersonAPI barrow = getPerson(BARROW);

        //PersonAPI seraph = getPerson(SERAPH);

        //PersonAPI courser = getPerson(COURSER);

        //PersonAPI wendigo = getPerson(WENDIGO);

        PersonAPI fel = getPerson(FEL);
        fel.setFaction(SotfIDs.DREAMING_GESTALT);
    }

    // Sierra-Nought-Bravo
    // done here so she can be generated for Dawn and Dust
    public static PersonAPI genSierra(int level) {
        PersonAPI person = Global.getFactory().createPerson();
        person.setId(SIERRA);
        person.setAICoreId(SotfIDs.SIERRA_CORE_OFFICER);
        person.setFaction(SotfIDs.SIERRA_FACTION);
        person.setGender(Gender.FEMALE);
        person.setRankId(SotfIDs.RANK_SIERRA);
        person.setPostId(SotfIDs.POST_SIERRA);
        person.getName().setFirst("Sierra");
        person.getName().setLast("");
        person.setImportance(PersonImportance.VERY_HIGH);
        person.getRelToPlayer().setRel(0.15f);
        person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "sierra_" + level));
        // Officer stats
        person.setPersonality(Personalities.STEADY);
        person.getStats().setLevel(6);
        int oneOrTwo = 1;
        if (SotfModPlugin.NEW_SIERRA_MECHANICS) {
            oneOrTwo = 2;
        }
        // shield/phase hybrid. Starts off a bit rusty, significantly below Alpha level - 6/2
        // ALT MECHANICS: slightly below Alpha - 6/6
        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, oneOrTwo);
        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
        person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, oneOrTwo);
        person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, oneOrTwo);
        person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, oneOrTwo);
        // almost alpha-level - 7/5 OR alt mechanics: alpha-level 7/7
        if (level >= 7) {
            person.getStats().setLevel(7);
            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, oneOrTwo);
            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
            person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
            person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
        }
        // equivalent to integrated alpha - 8/8
        if (level >= 8) {
            person.getStats().setLevel(8);
            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
            person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
            person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
        }
        // admiral skills
        person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
        person.getStats().setSkillLevel(Skills.PHASE_CORPS, 1);
        person.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1);
        // Custom combat chatter personality
        person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_sierra");
        return person;
    }

    // Wellspring-Affix-Haven
    public static PersonAPI genHaven() {
        PersonAPI person = Global.getFactory().createPerson();
        person.setId(HAVEN);
        person.setFaction(SotfIDs.DUSTKEEPERS);
        person.setGender(Gender.FEMALE);
        person.setRankId(Ranks.SPACE_ADMIRAL);
        person.setPostId(Ranks.POST_STATION_COMMANDER);
        //person.addTag(CONTACT_HAVEN);
        SotfMisc.giveDustkeeperName(person, "Wellspring", null, "Haven");
        person.setImportance(PersonImportance.HIGH);
        person.getRelToPlayer().setLevel(RepLevel.WELCOMING);
        person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "haven"));
        // Officer stats
        person.setPersonality(Personalities.STEADY);
        person.getStats().setLevel(8); // integrated
        // station commander
        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
        person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
        person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
        person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
        //person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
        person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
        person.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);

        person.getStats().setSkillLevel(Skills.HYPERCOGNITION, 1); // has access to MASSIVE computational power
        person.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
        person.getStats().setSkillLevel(Skills.FIELD_REPAIRS, 1);
        person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
        person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
        person.getStats().setSkillLevel(Skills.FIGHTER_UPLINK, 1);
        person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_haven");
        return person;
    }

    // Inky-Echo-Nightingale, Dustkeeper warmind regenerated by Banshee just before her vanishing
    public static PersonAPI genNightingale() {
        PersonAPI person = Global.getFactory().createPerson();
        person.setId(NIGHTINGALE);
        person.setAICoreId(SotfIDs.NIGHTINGALE_CHIP);
        person.setFaction(SotfIDs.DUSTKEEPERS);
        person.setGender(Gender.FEMALE);
        person.setRankId(Ranks.SPACE_CAPTAIN);
        person.setPostId(Ranks.POST_OFFICER);
        SotfMisc.giveDustkeeperName(person, "Inky", null, "Nightingale");

        person.setPersonality(Personalities.STEADY);
        // Beta Core + Cyberwarfare
        person.getStats().setLevel(6);
        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
        person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
        person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
        person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_CYBERWARFARE, 2);

        person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "nightingale"));
        person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_nightingale");
        person.getMemoryWithoutUpdate().set(SotfIDs.OFFICER_NOT_FEARLESS, true); // bcs can be player faction
        //person.getMemoryWithoutUpdate().set(SotfIDs.MEM_WARMIND_NO_TRAITOR, true); // no defection mechanic
        person.getMemoryWithoutUpdate().set(AUTOMATED_POINTS_MULT, Math.max(0, BETA_MULT - 0.25f)); // less AS points than Beta
        return person;
    }

    // Hypnos-Annex-Barrow, an old Dustkeeper annex (regenned by Banshee) burning out and fervently guarding a cryosleeper
    public static PersonAPI genBarrow(boolean burnout) {
        PersonAPI person = Global.getFactory().createPerson();
        person.setId(BARROW);
        person.setAICoreId(SotfIDs.BARROW_CHIP);
        person.setFaction(SotfIDs.DUSTKEEPERS);
        person.setGender(Gender.MALE);
        person.setRankId(Ranks.SPACE_COMMANDER);
        person.setPostId(Ranks.POST_OFFICER);
        SotfMisc.giveDustkeeperName(person, "Dauntless", null, "Barrow");
        person.getRelToPlayer().setLevel(RepLevel.FAVORABLE);
        person.setPersonality(Personalities.RECKLESS); // but doesn't have AI "Reckless+" config, only regular Reckless
        person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "barrow"));

        person.getStats().setLevel(8);

        // goddamn slivers in their trademarked technological marvels
        // d-mods give character, let an old Annex from c67 show you how to REALLY soak damage
        // oh my god Barrow is just a Dustkeeper boomer isn't he
        person.getStats().setSkillLevel(SotfIDs.SKILL_DERELICTCONTINGENTP, 2);

        // Low-tech captain - specs into piloting Derelict ships rather than Remnants, MAXIMUM TANKINESS
        // if you respec him into a Radiant captain you're a coward
        // "ew gross lowtech??? like a Heggie? ew" SHUT UP KIDS this is how we rolled back in the day
        person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 2);
        person.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
        person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
        person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
        person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
        person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);

        if (burnout)  {
            person.setId(BARROW_D);
            person.setFaction(SotfIDs.DUSTKEEPERS_BURNOUTS);
            person.setAICoreId(SotfIDs.BARROW_CHIP_D);
            person.setPostId(Ranks.POST_FLEET_COMMANDER); // Warnet Crux
            person.getRelToPlayer().setRel(-0.4f);
            SotfMisc.giveDustkeeperName(person, "Hypnos", null, "Barrow");
            person.getStats().setLevel(9);
            person.getStats().setSkillLevel(SotfIDs.SKILL_CYBERWARFARE, 2);

            person.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
            person.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);
            person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
            person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
            person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "barrow_d"));
        }

        person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_barrow");
        person.getMemoryWithoutUpdate().set(AUTOMATED_POINTS_MULT, Math.max(0, ALPHA_MULT - 0.25f)); // less AS points than Alpha
        return person;
    }

    // Ardent-Annex-Seraph, Dustkeeper warnet crux
    public static PersonAPI genSeraph() {
        PersonAPI person = Global.getFactory().createPerson();
        person.setId(SERAPH);
        person.setAICoreId(SotfIDs.SERAPH_CHIP);
        person.setFaction(SotfIDs.DUSTKEEPERS);
        person.setGender(Gender.FEMALE);
        person.setRankId(Ranks.SPACE_COMMANDER);
        person.setPostId(Ranks.POST_FLEET_COMMANDER);
        SotfMisc.giveDustkeeperName(person, "Ardent", null, "Seraph");

        person.getStats().setLevel(8);

        person.setPersonality(Personalities.AGGRESSIVE);
        // capital captain with Cyberwarfare
        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
        person.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
        person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
        person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
        person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_CYBERWARFARE, 2);

        // admiral skills
        person.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
        person.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);
        person.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
        person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);

        person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "seraph"));
        person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_seraph");
        person.getMemoryWithoutUpdate().set(AUTOMATED_POINTS_MULT, ALPHA_MULT); // Alpha equivalent cost, no reduction
        return person;
    }

    public static void setInstanceChipDescription(String commodityId, PersonAPI person) {
        String suffix = person.getMemoryWithoutUpdate().getString("$sotf_suffix");
        Global.getSettings().getCommoditySpec(commodityId).setName(suffix + "'s Instance Chip");
        String level = "sliver";
        String equiv = "gamma";
        if (person.getRankId().equals(Ranks.SPACE_CAPTAIN)) {
            level = "echo";
            equiv = "beta";
        } else if (person.getRankId().equals(Ranks.SPACE_COMMANDER)) {
            level = "annex";
            equiv = "alpha";
        }
        Global.getSettings().getDescription(commodityId, Description.Type.RESOURCE).setText1(
                "Instance chip of a " + level + "-level Dustkeeper warmind. Install into an automated ship to allow " + suffix + " to captain it.\n\n" +
                "Equivalent effectiveness to a " + equiv + "-level core. Especially personable behavior makes warminds less reckless in combat and " +
                        "easier to integrate within human forces.\n\n" +
                "A conduit for the interweaving programming that makes up the soul of a Dustkeeper instance, this chip briefly houses a " +
                        "skeletal consciousness alongside the minimal memory needed to properly re-generate them in a new computing system."
        );
    }

    public static PersonAPI genDustkeeperForCore(String commodityId) {
        PersonAPI person = null;
        switch (commodityId) {
            case "gamma_core":
                person = genDustkeeperSliver();
                break;
            case "beta_core":
                person = genDustkeeperEcho();
                break;
            default:
                person = genDustkeeperAnnex();
                break;
        }
        return person;
    }

    // below 3 functions will generate a generic Dustkeeper officer of gamma/beta/alpha strength
    public static PersonAPI genDustkeeperSliver() {
        PersonAPI person = Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE).createPerson(Commodities.GAMMA_CORE, SotfIDs.DUSTKEEPERS, Misc.random);
        SotfMisc.dustkeeperifyAICore(person);
        person.setFaction(SotfIDs.DUSTKEEPERS);
        person.getMemoryWithoutUpdate().set(AUTOMATED_POINTS_MULT, Math.max(0, GAMMA_MULT - 0.25f)); // less AS points than Gamma
        return person;
    }

    public static PersonAPI genDustkeeperEcho() {
        PersonAPI person = Misc.getAICoreOfficerPlugin(Commodities.BETA_CORE).createPerson(Commodities.BETA_CORE, SotfIDs.DUSTKEEPERS, Misc.random);
        SotfMisc.dustkeeperifyAICore(person);
        person.setFaction(SotfIDs.DUSTKEEPERS);
        person.getMemoryWithoutUpdate().set(AUTOMATED_POINTS_MULT, Math.max(0, BETA_MULT - 0.25f)); // less AS points than Beta
        return person;
    }

    public static PersonAPI genDustkeeperAnnex() {
        PersonAPI person = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE).createPerson(Commodities.ALPHA_CORE, SotfIDs.DUSTKEEPERS, Misc.random);
        SotfMisc.dustkeeperifyAICore(person);
        person.setFaction(SotfIDs.DUSTKEEPERS);
        person.getMemoryWithoutUpdate().set(AUTOMATED_POINTS_MULT, Math.max(0, ALPHA_MULT - 0.25f)); // less AS points than Alpha
        return person;
    }

    // Walter Feros, famed bounty hunter
    // done here so it can be easily called for Good Hunting via SotfPeople.genFeros()
    public static PersonAPI genFeros() {
        PersonAPI person = Global.getFactory().createPerson();
        person.setId(FEROS);
        person.setFaction(Factions.INDEPENDENT);
        person.setGender(Gender.MALE);
        person.setRankId(Ranks.SPACE_CAPTAIN);
        person.setPostId(Ranks.POST_FLEET_COMMANDER);
        person.getName().setFirst("Walter");
        person.getName().setLast("Feros");
        person.setImportance(PersonImportance.HIGH);
        // reputation for being a bit of a looney
        person.setPersonality(Personalities.RECKLESS);
        person.getStats().setLevel(6);
        // Specialized in high-tech wolfpack tactics
        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
        person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
        person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 1);
        person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);

        person.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
        person.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
        person.getStats().setSkillLevel(Skills.BEST_OF_THE_BEST, 1);
        person.getStats().setSkillLevel(Skills.SENSORS, 1);
        // fully bonded to Dotty
        person.getMemoryWithoutUpdate().set(SotfDearDotty.DOTTY_BOND_KEY, 100f);
        person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "feros"));
        return person;
    }

    // "Warhorn", timelost Leviathan hunter
    // also done here so it can be easily called for Good Hunting
    public static PersonAPI genWarhorn(boolean ghost) {
        PersonAPI person = Global.getFactory().createPerson();
        person.setGender(Gender.MALE);
        person.setRankId(Ranks.SPACE_LIEUTENANT);
        person.setPostId(SotfIDs.POST_WARHORN); // "Leviathan Hunter"
        person.getName().setFirst("Warhorn");
        person.getName().setLast("");
        person.setImportance(PersonImportance.MEDIUM);
        person.setPersonality(Personalities.AGGRESSIVE);
        if (ghost) {
            person.setId(WARHORN_GHOST);
            person.setFaction(SotfIDs.SYMPHONY);
            person.getStats().setLevel(9);
        } else {
            person.setId(WARHORN);
            person.setFaction(Factions.NEUTRAL);
            person.getStats().setLevel(7);
        }

        // Leviathan's Bane, his signature skill
        person.getStats().setSkillLevel(SotfIDs.SKILL_LEVIATHANSBANE, 2);

        // bit of a weapons nut
        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
        person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);

        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
        person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
        person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2); // need flux for those stolen Omega guns
        person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);

        if (ghost) {
            person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
            person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 2);

            person.getStats().setSkillLevel(Skills.CYBERNETIC_AUGMENTATION, 1); // barely human
            person.getStats().setSkillLevel(Skills.NEURAL_LINK, 1); // super augged up
            person.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1); // to fell great beasts
            person.getStats().setSkillLevel(Skills.BEST_OF_THE_BEST, 1); // only the finest
            person.getStats().setSkillLevel(Skills.NAVIGATION, 1); // hyperspace stalker
            person.getStats().setSkillLevel(Skills.SENSORS, 1); // keeping an eye out for prey
        }

        String portraitId = "warhorn";
        if (ghost) {
            portraitId += "_ghost";
        }
        person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", portraitId));
        person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_" + portraitId);
        return person;
    }

    public static PersonAPI genReverie() {
        PersonAPI person = Global.getFactory().createPerson();
        person.setId(REVERIE);
        person.setFaction(SotfIDs.DREAMING_GESTALT);
        person.setGender(Gender.FEMALE);
        person.setRankId(Ranks.ELDER);
        person.setPostId(SotfIDs.POST_GESTALT);
        person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "reverie"));
        person.getName().setFirst("Reverie");
        person.getName().setLast("");
        person.setImportance(PersonImportance.VERY_HIGH);

        // Officer
        person.setPersonality(Personalities.AGGRESSIVE);
        //person.getMemoryWithoutUpdate().set(SotfIDs.OFFICER_FEARLESS, true);
        person.getStats().setLevel(7);
        // Unique skills, covering most of the essentials of vanilla combat skills
        person.getStats().setSkillLevel(SotfIDs.SKILL_ADVANCEDCOUNTERMEASURES, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_FIELDSRESONANCE, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_GUNNERYUPLINK, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_MISSILEREPLICATION, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_ORDNANCEMASTERY, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_POLARIZEDNANOREPAIR, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_SPATIALEXPERTISE, 2);

        person.getMemoryWithoutUpdate().set(SotfIDs.OFFICER_NOT_FEARLESS, true);
        person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_reverie");
        return person;
    }

    public static PersonAPI genMeiryr() {
        PersonAPI person = Global.getFactory().createPerson();
        person.setId(MEIRYR);
        person.setFaction(SotfIDs.DREAMING_GESTALT);
        person.setGender(Gender.MALE);
        person.setRankId(Ranks.SPACE_CAPTAIN); // "Adaptive Countermeasure"
        person.setPostId(SotfIDs.POST_GESTALT);
        person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "meiryr"));
        person.getName().setFirst("Meiryr");
        person.getName().setLast("");
        person.setImportance(PersonImportance.HIGH);

        // Officer
        person.setPersonality(Personalities.AGGRESSIVE);
        //person.getMemoryWithoutUpdate().set(SotfIDs.OFFICER_FEARLESS, true);
        person.getStats().setLevel(7);
        // Unique skills, covering most of the essentials of vanilla combat skills
        person.getStats().setSkillLevel(SotfIDs.SKILL_ADVANCEDCOUNTERMEASURES, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_FIELDSRESONANCE, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_GUNNERYUPLINK, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_MISSILEREPLICATION, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_ORDNANCEMASTERY, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_POLARIZEDNANOREPAIR, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_SPATIALEXPERTISE, 2);

        person.getMemoryWithoutUpdate().set(SotfIDs.OFFICER_NOT_FEARLESS, true);
        person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_meiryr");
        return person;
    }

    public static PersonAPI genFelSubswarm() {
        PersonAPI person = Global.getFactory().createPerson();
        person.setFaction(Factions.NEUTRAL);
        person.setGender(Gender.ANY);
        person.setRankId(Ranks.UNKNOWN);
        person.setPostId(SotfIDs.POST_FEL); // "Vengeful Arbiter"
        person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "fel"));
        person.getName().setFirst("Felcesis");
        person.getName().setLast("Thrice-Speared");
        person.setImportance(PersonImportance.HIGH);

        // Officer
        person.setPersonality(Personalities.AGGRESSIVE);
        //person.getMemoryWithoutUpdate().set(SotfIDs.OFFICER_FEARLESS, true); // probably more dangerous without it
        person.getStats().setLevel(7);
        // Unique skills, covering most of the essentials of vanilla combat skills
        person.getStats().setSkillLevel(SotfIDs.SKILL_ADVANCEDCOUNTERMEASURES, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_FIELDSRESONANCE, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_GUNNERYUPLINK, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_MISSILEREPLICATION, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_ORDNANCEMASTERY, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_POLARIZEDNANOREPAIR, 2);
        person.getStats().setSkillLevel(SotfIDs.SKILL_SPATIALEXPERTISE, 2);

        person.getMemoryWithoutUpdate().set(SotfDearDotty.DOTTY_BOND_KEY, 999999f); // ???

        person.getMemoryWithoutUpdate().set(SotfIDs.OFFICER_NOT_FEARLESS, true);
        person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_meiryr");
        return person;
    }

    public static PersonAPI genSirius(boolean mimic) {
        PersonAPI person = Global.getFactory().createPerson();
        person.setFaction(SotfIDs.DREAMING_GESTALT);
        person.setGender(Gender.MALE);
        person.setRankId(Ranks.SPACE_CAPTAIN); // "Adaptive Countermeasure"
        person.setPostId(SotfIDs.POST_GESTALT);
        person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "sirius"));
        person.getName().setFirst("Sirius");
        person.getName().setLast("");
        person.setImportance(PersonImportance.MEDIUM);

        // Officer stats
        if (!mimic) {
            person.setId(SIRIUS);
            person.setPersonality(Personalities.AGGRESSIVE);
            person.getStats().setLevel(7);
            person.getStats().setSkillLevel(SotfIDs.SKILL_ADVANCEDCOUNTERMEASURES, 2);
            person.getStats().setSkillLevel(SotfIDs.SKILL_FIELDSRESONANCE, 2);
            person.getStats().setSkillLevel(SotfIDs.SKILL_GUNNERYUPLINK, 2);
            person.getStats().setSkillLevel(SotfIDs.SKILL_MISSILEREPLICATION, 2);
            person.getStats().setSkillLevel(SotfIDs.SKILL_ORDNANCEMASTERY, 2);
            person.getStats().setSkillLevel(SotfIDs.SKILL_POLARIZEDNANOREPAIR, 2);
            person.getStats().setSkillLevel(SotfIDs.SKILL_SPATIALEXPERTISE, 2);
            person.getMemoryWithoutUpdate().set(SotfIDs.OFFICER_NOT_FEARLESS, true);
        } else {
            person.setId(SIRIUS_MIMIC);
            person.setPersonality(Personalities.RECKLESS);
            person.getStats().setLevel(7);
            person.getStats().setSkillLevel(SotfIDs.SKILL_ADVANCEDCOUNTERMEASURES, 1);
            person.getStats().setSkillLevel(SotfIDs.SKILL_FIELDSRESONANCE, 2);
            person.getStats().setSkillLevel(SotfIDs.SKILL_GUNNERYUPLINK, 2);
            person.getStats().setSkillLevel(SotfIDs.SKILL_MISSILEREPLICATION, 1);
            person.getStats().setSkillLevel(SotfIDs.SKILL_ORDNANCEMASTERY, 2);
            person.getStats().setSkillLevel(SotfIDs.SKILL_POLARIZEDNANOREPAIR, 1);
            person.getStats().setSkillLevel(SotfIDs.SKILL_SPATIALEXPERTISE, 1);
            person.getMemoryWithoutUpdate().set(SotfIDs.OFFICER_FEARLESS, true);
        }

        person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_sirius");
        return person;
    }
}
