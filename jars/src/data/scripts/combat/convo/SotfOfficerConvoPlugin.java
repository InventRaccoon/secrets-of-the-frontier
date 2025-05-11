package data.scripts.combat.convo;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.combat.convo.SotfShipConvoPlugin.SotfShipConvoParams;
import data.scripts.utils.SotfMisc;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.List;

import static data.scripts.SotfModPlugin.OFFICER_CONVO_PATH;
import static data.scripts.SotfModPlugin.log;

/**
 *	Handles custom battle openers between special SotF officers - e.g quips between Sierra and Barrow when both are deployed
 */

public class SotfOfficerConvoPlugin extends BaseEveryFrameCombatPlugin {

    public static class SotfOfficerConvoData {

        public String id;
        public List<String> officers = new ArrayList<>();
        public List<String> strings = new ArrayList<>();
        public List<Float> delays = new ArrayList<>();

        public List<String> combatKeys = new ArrayList<>();
        public List<String> requiredFlags = new ArrayList<>();
        public List<String> disallowedFlags = new ArrayList<>();
        public List<String> setFlags = new ArrayList<>();

        public Map<String, String> requiredHullmods = new HashMap<>();
        public boolean oneShot = false;

        public float weight = 1f;

        public SotfOfficerConvoData() {
        }

    }

    //public static List<SotfOfficerConvoData> CONVERSATIONS = new ArrayList<>();

    private CombatEngineAPI engine;
    // tracks # of seconds passed in combat
    private float counter = 0;
    private boolean done = false;

    public void init(CombatEngineAPI engine) {
        this.engine = engine;
//        try {
//            loadData();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } catch (JSONException e) {
//            throw new RuntimeException(e);
//        }
    }

    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) return;
        if (done) return;
        if (engine.isPaused()) return;
        //if (!engine.isInCampaign()) {
        //    done = true;
        //    return;
        //}

        float timeUntilChat = 10f;
        float chanceToTrigger = Global.getSettings().getFloat("sotf_officerCombatConvoChance");
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            chanceToTrigger = LunaSettings.getFloat("secretsofthefrontier", "sotf_convoChance");
        }

        if (engine.getCustomData().containsKey(SotfIDs.ALWAYS_CONVO_KEY)) {
            timeUntilChat = 6f;
            chanceToTrigger = 1f;
        }

        counter += amount * engine.getTimeMult().getModifiedValue();

        if (counter < timeUntilChat) return;

        done = true;

        if (Math.random() > chanceToTrigger) return;

        ArrayList<PersonAPI> officers = new ArrayList<PersonAPI>();
        ArrayList<String> officerIds = new ArrayList<String>();

        for (FleetMemberAPI member : engine.getFleetManager(0).getDeployedCopy()) {
            if (member.getCaptain() != null) {
                if (!member.getCaptain().isDefault() && !officers.contains(member.getCaptain())) {
                    officers.add(member.getCaptain());
                    officerIds.add(member.getCaptain().getId());
                }
            }
        }
        WeightedRandomPicker<SotfOfficerConvoData> picker = new WeightedRandomPicker<SotfOfficerConvoData>();
        for (SotfOfficerConvoData data : SotfModPlugin.CONVERSATIONS) {
            boolean canAdd = true;
            for (String id : data.officers) {
                // specialcase for Sirius
                if (id.equals(SotfPeople.SIRIUS) && engine.isInCampaign()) {
                    MemoryAPI sector_mem = Global.getSector().getMemoryWithoutUpdate();
                    if (sector_mem.contains(SotfIDs.MEM_COTL_START)) {
                        continue;
                    }
                }
                if (!officerIds.contains(id)) {
                    canAdd = false;
                    break;
                }
            }
            for (String key : data.combatKeys) {
                if (!engine.getCustomData().containsKey("$" + key)) {
                    canAdd = false;
                    break;
                }
            }
            if (engine.isInCampaign()) {
                MemoryAPI sector_mem = Global.getSector().getMemoryWithoutUpdate();
                if (data.oneShot && Global.getSector().getMemoryWithoutUpdate().contains("$sotf_officerConvoWasPicked_" + data.id)) {
                    continue;
                }
                for (String flag : data.disallowedFlags) {
                    if (sector_mem.getBoolean("$" + flag)) {
                        canAdd = false;
                        break;
                    }
                }
                for (String flag : data.requiredFlags) {
                    if (!sector_mem.getBoolean("$" + flag)) {
                        canAdd = false;
                        break;
                    }
                }
            }
            for (String hullmodOfficer : data.requiredHullmods.values()) {
                PersonAPI officer = null;
                for (PersonAPI iter : officers) {
                    if (hullmodOfficer.equals(iter.getId())) {
                        officer = iter;
                    }
                }
                if (officer == null) {
                    canAdd = false;
                    break;
                }
                if (Global.getCombatEngine().getFleetManager(0).getShipFor(officer) != null) {
                    ShipAPI ship = Global.getCombatEngine().getFleetManager(0).getShipFor(officer);
                    if (!ship.getVariant().hasHullMod(data.requiredHullmods.get(hullmodOfficer))) {
                        canAdd = false;
                    }
                }
            }
            if (canAdd) picker.add(data, data.weight);
        }
        SotfOfficerConvoData data = picker.pick();
        if (data == null) return;
        Map<String, ShipAPI> map = new HashMap<String, ShipAPI>();
        List<ShipAPI> ships = new ArrayList<ShipAPI>();
        for (String id : data.officers) {
            if (!map.containsKey(id)) {
                for (FleetMemberAPI member : engine.getFleetManager(0).getDeployedCopy()) {
                    // specialcase for Sirius: doesn't chat with a ship
                    if (id.equals(SotfPeople.SIRIUS)) {
                        map.put(id, null);
                        continue;
                    }
                    if (member.getCaptain() != null) {
                        if (!member.getCaptain().isDefault() && member.getCaptain().getId().equals(id)) {
                            map.put(id, engine.getFleetManager(0).getShipFor(member));
                        }
                    }
                }
            }
        }
        for (String id : data.officers) {
            ships.add(map.get(id));
        }
        SotfShipConvoParams params = new SotfShipConvoParams(ships, data.strings, data.delays);
        Integer colorsPos = 0;
        for (String id : data.officers) {
            if (id.equals(SotfPeople.SIERRA)) {
                params.colors.put(colorsPos, SotfMisc.getSierraColor());
                params.classColorOverride.put(colorsPos, true);
                params.classColorOverrides.put(colorsPos, SotfMisc.getSierraColor());
            }
            if (id.equals(SotfPeople.SIRIUS)) {
                params.colors.put(colorsPos, Global.getSettings().getFactionSpec(SotfIDs.DREAMING_GESTALT).getColor());
                // specialcase for Sirius: no ship used
                params.siriusOverride.put(colorsPos, true);
                params.classColorOverride.put(colorsPos, true);
                params.classColorOverrides.put(colorsPos, Global.getSettings().getFactionSpec(SotfIDs.DREAMING_GESTALT).getColor());
            }
            colorsPos++;
        }

        if (engine.isInCampaign()) {
            MemoryAPI sector_mem = Global.getSector().getMemoryWithoutUpdate();
            sector_mem.set("$sotf_officerConvoWasPicked_" + data.id, true);
            for (String flag : data.setFlags) {
                sector_mem.set("$" + flag, true);
            }
        }

        engine.addPlugin(new SotfShipConvoPlugin(params));
    }

//    public void loadData() throws IOException, JSONException {
//        JSONObject convoJson = Global.getSettings().getMergedJSONForMod(OFFICER_CONVO_PATH, SotfIDs.SOTF);
//        Iterator iter = convoJson.keys();
//        while (iter.hasNext()) {
//            String id = (String)iter.next();
//            JSONObject entryJson = convoJson.getJSONObject(id);
//
//            SotfOfficerConvoData data = new SotfOfficerConvoData();
//            data.id = id;
//            JSONArray linesArray = entryJson.getJSONArray("lines");
//            for (int i=0; i < linesArray.length(); i++) {
//                data.officers.add((String) linesArray.getJSONArray(i).get(0));
//                String lineString = (String) linesArray.getJSONArray(i).get(1);
//                data.strings.add(lineString);
//                data.delays.add((float) linesArray.getJSONArray(i).optDouble(2, lineString.length() / 20f));
//            }
//            if (entryJson.optJSONArray("disallowedFlags") != null) {
//                for (int i = 0; i < entryJson.optJSONArray("disallowedFlags").length(); i++) {
//                    data.disallowedFlags.add(entryJson.getJSONArray("disallowedFlags").getString(i));
//                }
//            }
//                if (entryJson.optJSONArray("requiredFlags") != null) {
//                    for (int i = 0; i < entryJson.optJSONArray("requiredFlags").length(); i++) {
//                        data.requiredFlags.add(entryJson.getJSONArray("requiredFlags").getString(i));
//                    }
//                }
//                if (entryJson.optJSONArray("setFlags") != null) {
//                    for (int i = 0; i < entryJson.optJSONArray("setFlags").length(); i++) {
//                        data.setFlags.add(entryJson.getJSONArray("setFlags").getString(i));
//                    }
//                }
//                data.oneShot = entryJson.optBoolean("oneShot", false);
//                data.weight = (float) entryJson.optDouble("weight", 1);
//                CONVERSATIONS.add(data);
//            }
//    }
}