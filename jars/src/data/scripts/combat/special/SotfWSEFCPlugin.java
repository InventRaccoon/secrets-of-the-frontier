// every frame plugin for the Wayward Star fight
package data.scripts.combat.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.utils.SotfMisc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class SotfWSEFCPlugin extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI engine;
    private BattleCreationContext context;
    // tracks # of seconds passed in combat
    private float counter = 0;
    // is this the first time the player has fought this battle?
    private boolean repeat = true;
    // have we added the dialogue plugin?
    private boolean plugin = false;
    // various bits of dialogue
    private boolean sierraChatted = true;
    private boolean eidolonChatted = false;
    // the player's Concord ship
    private ShipAPI sierra = null;
    // ISS Athena
    private ShipAPI eidolon = null;
    private boolean reallyStarted = false;
    private boolean started = false;

    public SotfWSEFCPlugin(BattleCreationContext context) {
        this.context = context;
    }
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        engine.setDoNotEndCombat(true);
        if (!Global.getSector().getMemoryWithoutUpdate().contains("$sotf_WSCombatStarted")) {
            Global.getSector().getMemoryWithoutUpdate().set("$sotf_WSCombatStarted", null);
            repeat = false;
        }
        engine.getCustomData().put(SotfIDs.WAYWARDSTAR_COMBAT_KEY, true);
        engine.getCustomData().put(SotfIDs.ALWAYS_CONVO_KEY, true);
        engine.getCustomData().put("$sotf_noSierraChatter", true);
        engine.getCustomData().put(SotfIDs.INVASION_NEVER_KEY, true);
    }

    public void advance(float amount, List<InputEventAPI> events) {
        for (ShipAPI ship : engine.getShips()) {
                if (ship.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD) && ship.getOwner() == 0) {
                    sierra = ship;
                } else if (ship.getVariant().hasHullMod(SotfIDs.EIDOLONS_CONCORD) && ship.getOwner() == 1) {
                    eidolon = ship;
            }
        }
        if (!started) {
            started = true;
            return;
        }
        if (!reallyStarted) {
            reallyStarted = true;
            Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true);
            Global.getSoundPlayer().playCustomMusic(0, 0, "sotf_weightlessthoughts", true);
        }
        if (!engine.isPaused()) {
            counter += amount;
        }
        if (counter > 3) {
            engine.setDoNotEndCombat(false);
            // NOTE: sierraChatted is always true, this line was replaced by the officer convo system
            if (sierra != null && !sierraChatted) {
                Color sc = Global.getSector().getFaction(SotfIDs.SIERRA_FACTION).getBaseUIColor();
                Global.getCombatEngine().getCombatUI().addMessage(1, sierra, sc, sierra.getName() + " (" + sierra.getHullSpec().getHullNameWithDashClass() + "): \"... what...?\"");
                engine.addFloatingText(new Vector2f(sierra.getLocation().x, sierra.getLocation().y + 100),
                        "\"... what...?\"",
                        40f, sc, sierra, 1f, 0f);
                sierraChatted = true;
            }
        }
        if (counter > 3 && engine.getFleetManager(1).getDeployedCopyDFM().isEmpty()) {
            if (eidolon != null && !eidolonChatted) {
                Global.getCombatEngine().getCombatUI().addMessage(1, eidolon, SotfMisc.getEidolonColor(), "We must convene again");
                Global.getCombatEngine().getCombatUI().addMessage(2, eidolon, SotfMisc.getEidolonColor(), "This eidolon is impressed, you did beautifully");
                eidolonChatted = true;
            }
        }
        if (engine.isCombatOver()) {
            Global.getSoundPlayer().pauseCustomMusic();
            Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false);
            Global.getSoundPlayer().restartCurrentMusic();
        }
    }
}