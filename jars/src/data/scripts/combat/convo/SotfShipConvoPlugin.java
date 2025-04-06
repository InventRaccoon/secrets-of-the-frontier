package data.scripts.combat.convo;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *	Allows for two ships to have a conversation in combat
 */

public class SotfShipConvoPlugin extends BaseEveryFrameCombatPlugin {

    public int stage = 0;
    public float progress = 0f;
    public float currDelay = 0f;

    public static class SotfShipConvoParams implements Cloneable {

        public List<ShipAPI> ships;
        public List<String> strings;
        public List<Float> delays;
        public Map<Integer, Color> colors = new HashMap<>();
        public Map<Integer, Boolean> shipClass = new HashMap<>();;
        public Map<Integer, Boolean> classColorOverride = new HashMap<>();;
        public Map<Integer, Color> classColorOverrides = new HashMap<>();;

        public SotfShipConvoParams() {
        }

        public SotfShipConvoParams(List<ShipAPI> ships, List<String> strings, List<Float> delays) {
            super();
            this.ships = ships;
            this.strings = strings;
            this.delays = delays;
        }

        @Override
        protected SotfShipConvoParams clone() {
            try {
                return (SotfShipConvoParams) super.clone();
            } catch (CloneNotSupportedException e) {
                return null; // should never happen
            }
        }

    }

    public SotfShipConvoParams p;

    public SotfShipConvoPlugin(SotfShipConvoParams p) {
        this.p = p;
    }

    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) return;
        progress += amount * engine.getTimeMult().getModifiedValue();

        if (progress < currDelay) return;

        ShipAPI ship = p.ships.get(stage);
        if (ship == null || !ship.isAlive()) {
            engine.removePlugin(this);
            return;
        }
        Color baseNameColor = Misc.getPositiveHighlightColor();
        if (ship.getOwner() == 1) {
            baseNameColor = Misc.getNegativeHighlightColor();
        } else if (ship.isAlly()) {
            baseNameColor = Misc.getHighlightColor();
        }
        Color shipNameColor = baseNameColor;
        if (p.classColorOverride.get(stage) != null && p.classColorOverride.get(stage)) {
            shipNameColor = p.classColorOverrides.get(stage);
        }
        Color textColor = p.colors.get(stage);
        if (textColor == null) textColor = Misc.getTextColor();
        if (p.shipClass.get(stage) != null && !p.shipClass.get(stage)) {
            engine.getCombatUI().addMessage(0, p.ships.get(stage), textColor, p.strings.get(stage));
        } else {
            engine.getCombatUI().addMessage(0, p.ships.get(stage), shipNameColor, ship.getName() + " (" + ship.getHullSpec().getHullNameWithDashClass() + "): ", textColor, "\"" + p.strings.get(stage) + "\"");
        }
        engine.addFloatingText(new Vector2f(ship.getLocation().x, ship.getLocation().y + 100),
                "\"" + p.strings.get(stage) + "\"",
                40f, textColor, ship, 1f, 0f);

        currDelay = p.delays.get(stage);
        progress = 0;
        stage++;
        if ((stage + 1) > p.ships.size()) {
            progress = -99999f;
            engine.removePlugin(this);
        }
    }

}