// ... here we go.
package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhostManager;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;
import com.fs.starfarer.api.impl.campaign.missions.cb.MilitaryCustomBounty;
import com.fs.starfarer.api.impl.campaign.rulecmd.Nex_TransferMarket;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.impl.codex.CodexUnlocker;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import data.scripts.campaign.SotfHostileActivityFactorManager;
import data.scripts.campaign.SotfPlayerColonyScriptManager;
import data.scripts.campaign.codex.SotfCodexUnlocker;
import data.scripts.campaign.customstart.SotfSkillsChangeAutoshipsEffect;
import data.scripts.campaign.ghosts.SotfFelWhisperGhostCreator;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.intel.misc.SotfSierraConvIntel;
import data.scripts.campaign.intel.misc.SotfDustkeeperHatred;
import data.scripts.campaign.intel.misc.SotfSiriusIntel;
import data.scripts.campaign.intel.quests.SotfWaywardStarIntel;
import data.scripts.campaign.misc.PausedTimeAdvancer;
import data.scripts.campaign.missions.cb.SotfCBDustkeeper;
import data.scripts.campaign.missions.cb.SotfCBDustkeeperBurnout;
import data.scripts.campaign.plugins.SotfAbandonStationPlugin;
import data.scripts.campaign.plugins.SotfCampaignPluginImpl;
import data.scripts.campaign.plugins.SotfSalDefModPlugin;
import data.scripts.campaign.plugins.amemory.SotfAMemoryHintScript;
import data.scripts.campaign.plugins.dustkeepers.*;
import data.scripts.campaign.missions.cb.SotfCBProjectSiren;
import data.scripts.campaign.plugins.SotfOfficerJankHandler;
import data.scripts.campaign.plugins.fel.SotfGuiltTracker;
import data.scripts.campaign.plugins.wendigo.SotfWendigoEncounterManager;
import data.scripts.campaign.skills.SotfCyberwarfare;
import data.scripts.combat.convo.SotfOfficerConvoPlugin.SotfOfficerConvoData;
import data.scripts.combat.obj.SotfReinforcerEffect.SotfFactionRemoteAIData;
import data.scripts.world.SotfGen;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.histidine.chatter.ChatterConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class SotfModPlugin extends BaseModPlugin {

    public static boolean WATCHER;
    public static boolean TACTICAL;
    public static boolean DEVELOPMENT;
    public static boolean EXPLORARIUM;
    public static boolean GLIB;
    public static boolean MOD_WARNINGS;

    public static boolean NEW_SIERRA_MECHANICS = false;

    public static final String OFFICER_CONVO_PATH = "data/config/sotf/sotf_officerConvos.json";
    public static List<SotfOfficerConvoData> CONVERSATIONS = new ArrayList<>();

    public static Logger log = Global.getLogger(SotfModPlugin.class);

    public static final String OBJECTIVE_CSV = "data/config/sotf/objective_spawns.csv";
    public static JSONArray OBJECTIVE_DATA;
    public static final String REMOTE_AI_CSV = "data/config/sotf/remote_ai_factions.csv";
    public static final String REMOTE_AI_JSON = "data/config/sotf/remote_ai_factions.json";
    public static Map<String, SotfFactionRemoteAIData> REMOTE_AI_FACTIONS = new HashMap<String, SotfFactionRemoteAIData>();

    // so we know which ships to toggle Sierra's "dramatic" chatter mode on for
    public static boolean IS_CHATTER;
    // also works on her battlecries so let's stick in some vanilla/SotF bosses
    public static final Set<String> BOSS_SHIPS = new HashSet<>();
    static {
        BOSS_SHIPS.add("guardian");
        BOSS_SHIPS.add("ziggurat");
        BOSS_SHIPS.add("tesseract");
        BOSS_SHIPS.add("sotf_repose");
        BOSS_SHIPS.add("sotf_vow_eidolon");
    }

    public static final Set<String> LIFEDRINKER_HULLS = new HashSet<>();
    public static final Set<String> WITCHBLADE_HULLS = new HashSet<>();

    // sync scripts
    public void syncFrontierSecretsScripts() {
        addScriptsIfNeeded();
    }

    // on loading, run the code above.
    public void onGameLoad(boolean newGame) {
        syncFrontierSecretsScripts();
        SectorAPI sector = Global.getSector();
        MemoryAPI sector_mem = Global.getSector().getMemoryWithoutUpdate();

        // toggle the Explorarium drones if we need to
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            // doesn't affect Dustkeepers/Dustkeeper Proxies because they only use the proxy variants
            if (!EXPLORARIUM && faction.knowsShip("sotf_cavalier") && !faction.isPlayerFaction()) {
                faction.removeKnownShip("sotf_cavalier");
                faction.removeKnownShip("sotf_keeper");
                faction.removeKnownFighter("sotf_brattice_wing");
                faction.removeKnownFighter("sotf_parapet_wing");
                faction.removeKnownFighter("sotf_peon_wing");
            } else if (EXPLORARIUM && faction.knowsShip("rampart") && !faction.knowsShip("sotf_cavalier")) {
                faction.addKnownShip("sotf_cavalier", false);
                faction.addKnownShip("sotf_keeper", false);
                faction.addKnownFighter("sotf_brattice_wing", false);
                faction.addKnownFighter("sotf_parapet_wing", false);
                faction.addKnownFighter("sotf_peon_wing", false);
                // LONG LIVE THE KING
                if (faction.knowsShip("ae_cavalier")) {
                    faction.removeKnownShip("ae_cavalier");
                    faction.removeKnownShip("ae_keeper");
                    faction.removeKnownFighter("brattice_wing");
                    faction.removeKnownFighter("parapet_wing");
                    faction.removeKnownFighter("peon_wing");
                }
            }
        }

        if (DEVELOPMENT) {
            if (!sector.getGenericPlugins().hasPlugin(SotfAbandonStationPlugin.class)) {
                sector.getGenericPlugins().addPlugin(new SotfAbandonStationPlugin(), true);
            }
        }

        if (!WATCHER) {
            return;
        }

        // create any PersonAPIs that don't exist (ignores any that already do)
        SotfPeople.create();

        // set Sierra's current ship as the Pledge
        if (sector_mem.get("$sotf_sierra_var") == null) {
            sector_mem.set("$sotf_sierra_var", "sotf_pledge");
        }

        // Keep a list of all Concord-Lifedrinker compatible hulls
        LIFEDRINKER_HULLS.clear();
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            if (spec.getHullId().endsWith("_lifedrinker") && spec.getTags().contains("sotf_lifedrinker")) {
                LIFEDRINKER_HULLS.add(spec.getHullId());
            }
        }
        WITCHBLADE_HULLS.clear();
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            if (spec.getHullId().endsWith("_witchblade") && spec.getTags().contains("sotf_witchblade")) {
                WITCHBLADE_HULLS.add(spec.getHullId());
            }
        }

        if (sector_mem.get(SotfIDs.MEM_NUM_SIERRA_THOUGHTS) == null) {
            sector_mem.set(SotfIDs.MEM_NUM_SIERRA_THOUGHTS, 0);
        }

        if (sector_mem.get("$sotf_kindred") == null) {
            sector_mem.set("$sotf_kindred", "captain");
            sector_mem.set("$sotf_Kindred", "Captain");
        }

        if (Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().get("$sotf_guilt") == null) {
            Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().set("$sotf_guilt", 0f);
        }

        // make sure Nightingale's chip description matches whether she was rescued midgame, or is from Dustkeeper start
        Description nightingaleDesc = Global.getSettings().getDescription(SotfIDs.NIGHTINGALE_CHIP + "_found", Description.Type.CUSTOM);
        if (Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_BEGAN_WITH_NIGHTINGALE)) {
            nightingaleDesc = Global.getSettings().getDescription(SotfIDs.NIGHTINGALE_CHIP + "_start", Description.Type.CUSTOM);
        }
        Global.getSettings().getDescription(SotfIDs.NIGHTINGALE_CHIP, Description.Type.RESOURCE).setText1(nightingaleDesc.getText1());

        // add Project SIREN & Dustkeeper bounty to the list of potential military bounties
        MilitaryCustomBounty.CREATORS.add(new SotfCBProjectSiren());
        MilitaryCustomBounty.CREATORS.add(new SotfCBDustkeeper());
        MilitaryCustomBounty.CREATORS.add(new SotfCBDustkeeperBurnout());
        // add Fel whisper ghost to list of potential sensor ghosts
        SensorGhostManager.CREATORS.add(new SotfFelWhisperGhostCreator());

        // Dustkeepers automatically learn any auto fighters that mercenaries use (e.g Wasp)
        for (FighterWingSpecAPI wingSpec : Global.getSettings().getAllFighterWingSpecs()) {
            if (wingSpec.hasTag(Tags.AUTOMATED_FIGHTER) && wingSpec.hasTag("merc") && !sector.getFaction(SotfIDs.DUSTKEEPERS).knowsFighter(wingSpec.getId())) {
                sector.getFaction(SotfIDs.DUSTKEEPERS).addKnownFighter(wingSpec.getId(), false);
                sector.getFaction(SotfIDs.DUSTKEEPERS_BURNOUTS).addKnownFighter(wingSpec.getId(), false);
            }
        }

        // remove Tahlan's lifeless ships from the Dustkeepers
        // doesn't really make sense for them to use those
        if (sector.getFaction(SotfIDs.DUSTKEEPERS).knowsShip("tahlan_Timeless")) {
            sector.getFaction(SotfIDs.DUSTKEEPERS).removeKnownShip("tahlan_Timeless");
            sector.getFaction(SotfIDs.DUSTKEEPERS).removeKnownShip("tahlan_Nameless");
            sector.getFaction(SotfIDs.DUSTKEEPERS).removeKnownWeapon("tahlan_disparax");
            sector.getFaction(SotfIDs.DUSTKEEPERS).removeKnownWeapon("tahlan_relparax");
            sector.getFaction(SotfIDs.DUSTKEEPERS).removeKnownWeapon("tahlan_nenparax");
            sector.getFaction(SotfIDs.DUSTKEEPERS_BURNOUTS).removeKnownShip("tahlan_Timeless");
            sector.getFaction(SotfIDs.DUSTKEEPERS_BURNOUTS).removeKnownShip("tahlan_Nameless");
            sector.getFaction(SotfIDs.DUSTKEEPERS_BURNOUTS).removeKnownWeapon("tahlan_disparax");
            sector.getFaction(SotfIDs.DUSTKEEPERS_BURNOUTS).removeKnownWeapon("tahlan_relparax");
            sector.getFaction(SotfIDs.DUSTKEEPERS_BURNOUTS).removeKnownWeapon("tahlan_nenparax");
        }

        // Dustkeeper non-burnouts don't use phase ships because that does Bad Things to them
        // also it doesn't really fit their doctrine
        for (String hullId : new ArrayList<>(sector.getFaction(SotfIDs.DUSTKEEPERS).getKnownShips())) {
            if (Global.getSettings().getHullSpec(hullId).isPhase()) {
                sector.getFaction(SotfIDs.DUSTKEEPERS).removeKnownShip(hullId);
            }
        }

        // set instance chip descriptions to match the actual generated person
        if (SotfPeople.getPerson(SotfPeople.SLIVER_1) != null) {
            SotfPeople.setInstanceChipDescription(SotfIDs.SLIVER_CHIP_1, SotfPeople.getPerson(SotfPeople.SLIVER_1));
        }
        if (SotfPeople.getPerson(SotfPeople.SLIVER_2) != null) {
            SotfPeople.setInstanceChipDescription(SotfIDs.SLIVER_CHIP_2, SotfPeople.getPerson(SotfPeople.SLIVER_2));
        }
        if (SotfPeople.getPerson(SotfPeople.ECHO_1) != null) {
            SotfPeople.setInstanceChipDescription(SotfIDs.ECHO_CHIP_1, SotfPeople.getPerson(SotfPeople.ECHO_1));
        }
        if (SotfPeople.getPerson(SotfPeople.ECHO_2) != null) {
            SotfPeople.setInstanceChipDescription(SotfIDs.ECHO_CHIP_2, SotfPeople.getPerson(SotfPeople.ECHO_2));
        }

        // Midsave Alteration Corner 2: Electric Boogaloo
        // This code only exists to perform fixes on existing saves - these have effectively no effect on new saves
        // TODO: Delete all of this on each savebreaking update

        // End midsave alteration corner

        // spawn the ISS Athena for A Memory
        SotfGen.trySpawnAthena(sector);
        // setup for The Hunt for Walter Feros
//        SotfGen.trySetupHuntingFeros(sector);
        // spawn Nightingale's encounter
        SotfGen.trySpawnNightingale(sector);
        // spawn the Hypnos cryosleeper and Hypnos-Annex-Barrow
        SotfGen.trySpawnBarrow(sector);
        // spawn the Askonia Probe for Mayfly's interaction
        SotfGen.trySpawnMayfly(sector);
        SotfGen.tryAddKottersCutthroats(sector);
        // spawn Mia's Star system for Hope for Hallowhall
        SotfGen.trySpawnMia(sector);
        SotfGen.trySpawnLOTL(sector);

        Global.getSector().addTransientScript(new PausedTimeAdvancer());
    }

    public void onEnabled(boolean wasEnabledBefore) {
        SectorAPI sector = Global.getSector();
        if (!wasEnabledBefore) {
            SotfGen.initFactionRelationships(sector);
        }
    }
    protected void addScriptsIfNeeded() {
        SectorAPI sector = Global.getSector();
        ListenerManagerAPI listeners = sector.getListenerManager();

        sector.registerPlugin(new SotfCampaignPluginImpl());

        if (!sector.getGenericPlugins().hasPlugin(SotfDustkeeperOfficerPlugin.class)) {
            sector.getGenericPlugins().addPlugin(new SotfDustkeeperOfficerPlugin(), true);
        }
        if (!sector.getGenericPlugins().hasPlugin(SotfDustkeeperFleetCreator.class)) {
            sector.getGenericPlugins().addPlugin(new SotfDustkeeperFleetCreator(), true);
        }
        if (!sector.getGenericPlugins().hasPlugin(SotfDustkeeperChipIconProvider.class)) {
            sector.getGenericPlugins().addPlugin(new SotfDustkeeperChipIconProvider(), true);
        }

        if (!sector.getGenericPlugins().hasPlugin(SotfSalDefModPlugin.class)) {
            sector.getGenericPlugins().addPlugin(new SotfSalDefModPlugin(), true);
        }

        if (WATCHER) {
            if (!listeners.hasListenerOfClass(SotfCodexUnlocker.class)) {
                listeners.addListener(new SotfCodexUnlocker(), true);
            }

            // sierra conversation intel
            if (!sector.getIntelManager().hasIntelOfClass(SotfSierraConvIntel.class)) {
                Global.getSector().getIntelManager().addIntel(new SotfSierraConvIntel(), false);
            } else if (!Global.getSector().getListenerManager().hasListenerOfClass(SotfSierraConvIntel.class)) {
                Global.getSector().getListenerManager().addListener(Global.getSector().getIntelManager().getFirstIntel(SotfSierraConvIntel.class));
            }

            // Dustkeeper hatred on atrocity
            if (!sector.getIntelManager().hasIntelOfClass(SotfDustkeeperHatred.class)) {
                Global.getSector().getIntelManager().addIntel(new SotfDustkeeperHatred(), false);
            }

            // A Memory hint ghosts
            if (!sector.hasScript(SotfAMemoryHintScript.class) && !sector.getMemoryWithoutUpdate().contains("$sotf_AMemoryCombatStarted")) {
                sector.addScript(new SotfAMemoryHintScript());
            }

            // Dustkeeper generic officer manager
            if (!sector.hasScript(SotfDustkeeperOfficerManagerEvent.class)) {
                sector.addScript(new SotfDustkeeperOfficerManagerEvent());
            }

            // Sierra/Derelict Contingent handler
            if (!sector.hasScript(SotfOfficerJankHandler.class)) {
                sector.addScript(new SotfOfficerJankHandler());
            }

            // Guilt gain on atrocity, and Haunted dreams
            if (!sector.hasScript(SotfGuiltTracker.class)) {
                sector.addScript(new SotfGuiltTracker());
            }

            // Dustkeeper bonuses and etc
            if (!sector.hasScript(SotfPlayerColonyScriptManager.class)) {
                sector.addScript(new SotfPlayerColonyScriptManager());
            }

            if (sector.hasScript(SotfHostileActivityFactorManager.class)) {
                sector.removeScriptsOfClass(SotfHostileActivityFactorManager.class);

                HostileActivityEventIntel ha = HostileActivityEventIntel.get();
                if (ha != null) {
                    for (EventFactor factor : ha.getFactors()) {
                        if (factor.getClass() == SotfDustkeeperHAFactor.class) {
                            ha.removeFactor(factor);
                        }
                    }
                }
            }
            if (!sector.hasTransientScript(SotfHostileActivityFactorManager.class)) {
                sector.addTransientScript(new SotfHostileActivityFactorManager());
            }

            if (!sector.hasScript(SotfWendigoEncounterManager.class)) {
                sector.addScript(new SotfWendigoEncounterManager());
            }

            if (!sector.getIntelManager().hasIntelOfClass(SotfSiriusIntel.class) && sector.getMemoryWithoutUpdate().contains(SotfIDs.MEM_COTL_START)) {
                Global.getSector().getIntelManager().addIntel(new SotfSiriusIntel(), false);
            }
        }

        if (sector.getMemoryWithoutUpdate().contains(SotfIDs.MEM_BEGAN_WITH_NIGHTINGALE)) {
            listeners.addListener(new SotfSkillsChangeAutoshipsEffect(), true);
        }
    }

    private static void loadSettings() throws IOException, JSONException {
        JSONObject setting = Global.getSettings().loadJSON("sotf_settings.ini");
        WATCHER = setting.getBoolean("enableWatcherBeyondTheWalls");
        TACTICAL = setting.getBoolean("enableTacticalExpansion");
        DEVELOPMENT = setting.getBoolean("enableFrontierDevelopment");
        EXPLORARIUM = setting.getBoolean("enableExplorariumDrones");

        MOD_WARNINGS = setting.getBoolean("enableModWarnings");


        if (Global.getSettings().getModManager().isModEnabled("nexerelin")) {
            Nex_TransferMarket.NO_TRANSFER_FACTIONS.add(SotfIDs.DUSTKEEPERS);
        }

        // get Combat Chatter's boss list, so we know when to swap Sierra to dramatic mode
        if (Global.getSettings().getModManager().isModEnabled("chatter")) {
            IS_CHATTER = true;

            // also remove Symphony from no-chatter factions so Eidolon has special chatter
            ChatterConfig.noEnemyChatterFactions.remove("sotf_symphony");

            JSONArray bosses = Global.getSettings().getMergedSpreadsheetDataForMod("hull id", "data/config/chatter/boss_ships.csv", "chatter");
            for (int x = 0; x < bosses.length(); x++) {
                try {
                    JSONObject row = bosses.getJSONObject(x);
                    String hullId = row.getString("hull id");
                    if (hullId.isEmpty()) continue;
                    if (BOSS_SHIPS.contains(hullId)) continue;
                    BOSS_SHIPS.add(hullId);
                } catch (JSONException ex) {
                    log.error("SotF failed to load Combat Chatter boss ships", ex);
                }
            }
        }
        if (WATCHER) {
            JSONObject convoJson = Global.getSettings().getMergedJSONForMod(OFFICER_CONVO_PATH, SotfIDs.SOTF);
            Iterator iter = convoJson.keys();
            while (iter.hasNext()) {
                String id = (String)iter.next();
                try {
                    JSONObject entryJson = convoJson.getJSONObject(id);

                    SotfOfficerConvoData data = new SotfOfficerConvoData();
                    data.id = id;
                    JSONArray linesArray = entryJson.getJSONArray("lines");
                    for (int i = 0; i < linesArray.length(); i++) {
                        data.officers.add((String) linesArray.getJSONArray(i).get(0));
                        String lineString = (String) linesArray.getJSONArray(i).get(1);
                        data.strings.add(lineString);
                        data.delays.add((float) linesArray.getJSONArray(i).optDouble(2, 1 + (lineString.length() / 20f)));
                    }
                    if (entryJson.optJSONArray("combatKeys") != null) {
                        for (int i = 0; i < entryJson.optJSONArray("combatKeys").length(); i++) {
                            data.combatKeys.add(entryJson.getJSONArray("combatKeys").getString(i));
                        }
                    }
                    if (entryJson.optJSONArray("disallowedFlags") != null) {
                        for (int i = 0; i < entryJson.optJSONArray("disallowedFlags").length(); i++) {
                            data.disallowedFlags.add(entryJson.getJSONArray("disallowedFlags").getString(i));
                        }
                    }
                    if (entryJson.optJSONArray("requiredFlags") != null) {
                        for (int i = 0; i < entryJson.optJSONArray("requiredFlags").length(); i++) {
                            data.requiredFlags.add(entryJson.getJSONArray("requiredFlags").getString(i));
                        }
                    }
                    if (entryJson.optJSONArray("setFlags") != null) {
                        for (int i = 0; i < entryJson.optJSONArray("setFlags").length(); i++) {
                            data.setFlags.add(entryJson.getJSONArray("setFlags").getString(i));
                        }
                    }
                    if (entryJson.optJSONArray("requiredHullmods") != null) {
                        JSONArray hullmodsArray = entryJson.getJSONArray("requiredHullmods");
                        for (int i = 0; i < hullmodsArray.length(); i++) {
                            data.requiredHullmods.put((String) hullmodsArray.getJSONArray(i).get(0), (String) hullmodsArray.getJSONArray(i).get(1));
                        }
                    }
                    data.oneShot = entryJson.optBoolean("oneShot", false);
                    data.weight = (float) entryJson.optDouble("weight", 1);
                    CONVERSATIONS.add(data);
                } catch (JSONException ex) {
                    log.error("SotF failed loading officer conversation " + id, ex);
                }
            }
        }

        if (TACTICAL) {
            OBJECTIVE_DATA = Global.getSettings().getMergedSpreadsheetDataForMod("id", OBJECTIVE_CSV, SotfIDs.SOTF);

            if (OBJECTIVE_DATA == null) {
                throw new JSONException("SotF failed to load dynamic objective spawn data!");
            }
//            JSONArray remoteAiCSV = Global.getSettings().getMergedSpreadsheetDataForMod("id", REMOTE_AI_CSV, SotfIDs.SOTF);
//            for (int i = 0; i < remoteAiCSV.length(); i++) {
//                REMOTE_AI_FACTIONS.add(remoteAiCSV.getJSONObject(i).getString("id"));
//            }
        }

        // Some objective stuff might need to be on because of either Watcher or Tactical
        // e.g remote AI is primarily a Tactical thing, but Hyperwave Transmitters also appear in Wayward Star
        if (WATCHER || TACTICAL) {
            JSONObject remoteAIJson = Global.getSettings().getMergedJSONForMod(REMOTE_AI_JSON, SotfIDs.SOTF);
            Iterator iter = remoteAIJson.keys();
            while (iter.hasNext()) {
                String id = (String) iter.next();
                JSONObject entryJson = remoteAIJson.getJSONObject(id);

                SotfFactionRemoteAIData data = new SotfFactionRemoteAIData();
                data.alphas = entryJson.optInt("alphas", 0);
                data.betas = entryJson.optInt("betas", 1);
                data.gammas = entryJson.optInt("gammas", 3);
                REMOTE_AI_FACTIONS.put(id, data);
            }
        }
    }

    // load the settings ini
     public void onApplicationLoad() {
         GLIB = Global.getSettings().getModManager().isModEnabled("shaderLib");
         if (GLIB) {
             ShaderLib.init();
             LightData.readLightDataCSV("data/lights/sotf_light_data.csv");
         }
         try {
             loadSettings();
         } catch (IOException | JSONException e) {
             Global.getLogger(SotfModPlugin.class).log(Level.ERROR, "Failed to load sotf_settings.ini!" + e.getMessage());
         }
         // no longer required as of Starpocalypse Revengeance 3.0.1
//         if (Global.getSettings().getModManager().isModEnabled("starpocalypse") && MOD_WARNINGS && WATCHER) {
//             try {
//                 JSONObject starpocalypse = Global.getSettings().loadJSON("starpocalypse.json");
//                 if (starpocalypse.getBoolean("stingyRecoveries")) {
//                     throw new JSONException("Secrets of the Frontier's Watcher Beyond the Walls module is incompatible " +
//                             "with Starpocalypse's Stingy Recovery setting. Disable that setting in starpocalypse.json, or disable " +
//                             "mod compatibility warnings in SotF's configuration and accept the consequences (it WILL break a SotF quest!)");
//                 }
//             } catch (IOException | JSONException e) {
//                 Global.getLogger(SotfModPlugin.class).log(Level.ERROR, "Secrets of the Frontier failed to load starpocalypse.json! " +
//                         "This was done to attempt to try and prevent a quest-breaking incompatibility. Disable its Stingy Recovery setting and disable mod warnings " +
//                         "in SotF's configuration." + e.getMessage());
//             }
//         }
     }

    @Override
    public void onAboutToLinkCodexEntries() {
        // Daydream Synthesizer w daydream skills
        CodexDataV2.makeRelated(
                CodexDataV2.getHullmodEntryId(SotfIDs.HULLMOD_DAYDREAM_SYNTHESIZER),
                CodexDataV2.getSkillEntryId(SotfIDs.SKILL_ADVANCEDCOUNTERMEASURES),
                CodexDataV2.getSkillEntryId(SotfIDs.SKILL_FIELDSRESONANCE),
                CodexDataV2.getSkillEntryId(SotfIDs.SKILL_GUNNERYUPLINK),
                CodexDataV2.getSkillEntryId(SotfIDs.SKILL_MISSILEREPLICATION),
                CodexDataV2.getSkillEntryId(SotfIDs.SKILL_ORDNANCEMASTERY),
                CodexDataV2.getSkillEntryId(SotfIDs.SKILL_POLARIZEDNANOREPAIR),
                CodexDataV2.getSkillEntryId(SotfIDs.SKILL_SPATIALEXPERTISE)
        );
        // CWAR
        CodexDataV2.makeRelated(
                CodexDataV2.getHullmodEntryId(SotfIDs.HULLMOD_CWARSUITE),
                CodexDataV2.getSkillEntryId(SotfIDs.SKILL_CYBERWARFARE)
        );
        CodexDataV2.makeRelated(
                CodexDataV2.getHullmodEntryId(HullMods.ECM),
                CodexDataV2.getSkillEntryId(Skills.ELECTRONIC_WARFARE),
                CodexDataV2.getSkillEntryId(SotfIDs.SKILL_CYBERWARFARE)
        );
        CodexDataV2.makeRelated(
                CodexDataV2.getHullmodEntryId(HullMods.ECCM),
                CodexDataV2.getSkillEntryId(SotfIDs.SKILL_CYBERWARFARE)
        );
        for (String dmod : SotfCyberwarfare.VULNERABLE_DMODS) {
            CodexDataV2.makeRelated(
                    CodexDataV2.getHullmodEntryId(dmod),
                    CodexDataV2.getSkillEntryId(SotfIDs.SKILL_CYBERWARFARE)
            );
        }
        // Proxy ships with original versions
        CodexDataV2.makeRelated(
                CodexDataV2.getShipEntryId("sotf_picket_prox"),
                CodexDataV2.getSkillEntryId("picket")
        );
        CodexDataV2.makeRelated(
                CodexDataV2.getShipEntryId("sotf_warden_prox"),
                CodexDataV2.getSkillEntryId("warden")
        );
        CodexDataV2.makeRelated(
                CodexDataV2.getShipEntryId("sotf_sentry_prox"),
                CodexDataV2.getSkillEntryId("sentry")
        );
        CodexDataV2.makeRelated(
                CodexDataV2.getShipEntryId("sotf_defender_prox"),
                CodexDataV2.getSkillEntryId("defender")
        );
        CodexDataV2.makeRelated(
                CodexDataV2.getShipEntryId("sotf_bastillon_prox"),
                CodexDataV2.getSkillEntryId("bastillon")
        );CodexDataV2.makeRelated(
                CodexDataV2.getShipEntryId("sotf_berserker_prox"),
                CodexDataV2.getSkillEntryId("berserker")
        );
        CodexDataV2.makeRelated(
                CodexDataV2.getShipEntryId("sotf_keeper_prox"),
                CodexDataV2.getSkillEntryId("sotf_keeper")
        );
        CodexDataV2.makeRelated(
                CodexDataV2.getShipEntryId("sotf_rampart_prox"),
                CodexDataV2.getSkillEntryId("rampart")
        );
        CodexDataV2.makeRelated(
                CodexDataV2.getShipEntryId("sotf_cavalier_prox"),
                CodexDataV2.getSkillEntryId("sotf_cavalier")
        );

    }

    @Override
    public void afterGameSave()
    {
        if (Global.getSector().getListenerManager().hasListenerOfClass(SotfGuiltTracker.class)) {
            for (SotfGuiltTracker listener : Global.getSector().getListenerManager().getListeners(SotfGuiltTracker.class)) {
                listener.timeSinceSave = 0;
            }
        }
    }

    @Override
    public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
        if (ship.isFighter()) return null;

        // AI config for the "Fearless" behavior displayed by vanilla's automated ships, aka Reckless+, aka "unbound S key"
        ShipAIConfig fearlessConfig = new ShipAIConfig();
        fearlessConfig.alwaysStrafeOffensively = true;
        fearlessConfig.backingOffWhileNotVentingAllowed = false;
        fearlessConfig.turnToFaceWithUndamagedArmor = false;
        fearlessConfig.burnDriveIgnoreEnemies = true;

        boolean carrier = false;
        if (ship != null && ship.getVariant() != null) {
            carrier = ship.getVariant().isCarrier() && !ship.getVariant().isCombat();
        }
        if (carrier) {
            fearlessConfig.personalityOverride = Personalities.AGGRESSIVE;
            fearlessConfig.backingOffWhileNotVentingAllowed = true;
        } else {
            fearlessConfig.personalityOverride = Personalities.RECKLESS;
        }

        String hullId = ship.getHullSpec().getHullId();

        // Dustkeeper escort auxiliaries have hacked programming, so they're more passive
        if (ship.getVariant().hasHullMod(SotfIDs.HULLMOD_AUX_ESCORT)) {
            ShipAIConfig auxEscortConfig = new ShipAIConfig();
            auxEscortConfig.personalityOverride = Personalities.STEADY;
            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, auxEscortConfig), CampaignPlugin.PickPriority.MOD_SET);
        }
        if (ship.getCaptain() == null) return null;
        if (ship.getCaptain().isDefault()) return null;

        // Any officer with this memory flag gains Fearless AI (e.g Eidolon)
        if (ship.getCaptain().getMemoryWithoutUpdate().contains(SotfIDs.OFFICER_FEARLESS)) {
            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, fearlessConfig), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }

        // Dustkeeper warminds are calm and don't pilot their ships recklessly
        // Also add a memory flag that can be assigned to any officer to prevent their automated ships from being fearless
        if (ship.getCaptain().getFaction().getId().equals(SotfIDs.DUSTKEEPERS) || ship.getCaptain().getFaction().getId().equals(SotfIDs.DUSTKEEPERS_BURNOUTS)
                || ship.getCaptain().getMemoryWithoutUpdate().contains(SotfIDs.OFFICER_NOT_FEARLESS)) {
            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, new ShipAIConfig()), CampaignPlugin.PickPriority.MOD_SET);
        }

        return null;
    }
}