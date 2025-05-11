// Do they hear me calling?
// Spawns wisp fighters from enemy ships that are disabled within range (and allies, if the Wispersong ship is an enemy)
package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.combat.SotfAuraVisualScript;
import data.scripts.utils.SotfMisc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Iterator;

public class SotfWispersong extends SotfBaseConcordAugment
{
    // how often the game checks for dead ships to spawn wisps from
    public static final float CHECK_INTERVAL = 0.15f;
    // range in which a ship must be to actually spawn wisps. Out-of-range ships still can't be wispered in the future
    public static final float WISPERING_RANGE = 1600f;
    // keys for custom data
    public static final String TIMER_KEY = "sotf_wispersongtimer";
    // if ship has this key, it means someone has already wispered it
    // if a ship has this key + _ + ship ID, it means that ship failed to wisper it at some point
    public static final String WISPERED_KEY = "sotf_wispered";
    public static final String VISUAL_KEY = "sotf_wispersongvisual";

    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!Global.getCurrentState().equals(GameState.COMBAT)) {
            return;
        }

        if (!ship.getCustomData().containsKey(TIMER_KEY)) {
            ship.setCustomData(TIMER_KEY, 0f);
        }

        Color color = new Color(100,75,155,255);

        // create AoE visual
        if (!ship.getCustomData().containsKey(VISUAL_KEY)) {
            SotfAuraVisualScript.AuraParams p = new SotfAuraVisualScript.AuraParams();
            p.color = Misc.setAlpha(color, 125);
            p.ship = ship;
            p.radius = WISPERING_RANGE;
            Global.getCombatEngine().addLayeredRenderingPlugin(new SotfAuraVisualScript(p));
            ship.setCustomData(VISUAL_KEY, true);
        }

        float timer = (float) ship.getCustomData().get(TIMER_KEY);
        float new_timer = timer + amount;

        if (!ship.isAlive()) {
            return;
        }

        if (new_timer < CHECK_INTERVAL) {
            ship.setCustomData(TIMER_KEY, new_timer);
            return;
        }

        ShipAPI target = findTarget(ship, Global.getCombatEngine());
        Vector2f from = ship.getLocation();
        if (target != null) {
            // 1/2/4/6
            int wisps_to_spawn = target.getHullSize().ordinal() - 1;
            if (target.getHullSize().ordinal() >= 4) {
                wisps_to_spawn += 1;
            }
            if (target.getHullSize().ordinal() >= 5) {
                wisps_to_spawn += 1;
            }
            for (int i = 0; i < wisps_to_spawn; i++) {
                Vector2f to = Misc.getPointAtRadius(target.getLocation(), target.getCollisionRadius() + 25);

                CombatEngineAPI engine = Global.getCombatEngine();

                CombatFleetManagerAPI fleetManager = engine.getFleetManager(ship.getOriginalOwner());
                boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
                fleetManager.setSuppressDeploymentMessages(true);
                CombatEntityAPI wisp = engine.getFleetManager(ship.getOriginalOwner()).spawnShipOrWing("sotf_wisp_wing", to, Misc.getAngleInDegrees(target.getLocation(), to), 0f);
                Global.getSoundPlayer().playSound("mote_attractor_launch_mote", 1f, 1f, wisp.getLocation(), new Vector2f(0,0));
                Global.getCombatEngine().spawnEmpArcVisual(Misc.getPointWithinRadius(ship.getLocation(), 50f), ship, wisp.getLocation(), wisp, 30f, new Color(100,25,155,255), Color.white);
                fleetManager.setSuppressDeploymentMessages(wasSuppressed);
            }
            target.setCustomData(WISPERED_KEY, true);
        }
        ship.setCustomData(TIMER_KEY, 0f);
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        ShipHullSpecAPI wisp_spec = Global.getSettings().getHullSpec("sotf_wisp");
        WeaponSpecAPI discharge = Global.getSettings().getWeaponSpec("sotf_wispdischarge");
        if (index == 0) return Integer.toString((int) WISPERING_RANGE);
        if (index == 1) return "1";
        if (index == 2) return "2";
        if (index == 3) return "4";
        if (index == 4) return "6";
        if (index == 5) return Integer.toString((int)wisp_spec.getFluxCapacity());
        if (index == 6) return Integer.toString((int)discharge.getDerivedStats().getDamagePerShot());
        if (index == 7) return Integer.toString((int)discharge.getDerivedStats().getEmpPerShot());
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 3f;
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        Color good = Misc.getPositiveHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();
        Color gray = Misc.getGrayColor();

        if (Global.getSettings().getCurrentState() == GameState.TITLE) return;
        if (isForModSpec || ship == null) return;

        LabelAPI label = tooltip.addPara("\"Life and death are siblings in cycle. Where there is one, the other will follow.\"", SotfMisc.getSierraColor().darker(), opad);
        label.italicize();
        tooltip.addPara("   - Sierra-Nought-Bravo", gray, opad);
        label = tooltip.addPara("\"The Spatial Agitation Resonator intentionally damages the fabric of reality in order to " +
                "generate autonomous and possibly sentient EM-anomalies hell-bent on their own violent proliferation. I " +
                "refuse to refer to it by a designation incorporating a pun, but she can call it whatever she wants as long as she's the one with " +
                "the arcane nightmare machine strapped to her liver.\"", gray, opad);
        label.italicize();
        tooltip.addPara("   - chief engineer's report on the \"Wispersong Invoker\"", gray, opad);
    }

    // find a valid Wispersong hulk for a ship and invalidate far-away ships from future checks by that ship
    public ShipAPI findTarget(ShipAPI ship, CombatEngineAPI engine) {
        float range = WISPERING_RANGE;
        Vector2f from = ship.getLocation();

        Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
                range * 2f, range * 2f);
        int owner = ship.getOwner();
        ShipAPI best = null;
        float minScore = Float.MAX_VALUE;

        while (iter.hasNext()) {
            Object o = iter.next();
            if (!(o instanceof ShipAPI)) continue;
            ShipAPI other = (ShipAPI) o;
            // player concord ships can't summon wisps from allies - Eidolon's can
            if (owner == 0 && other.getOwner() == owner) continue;

            ShipAPI otherShip = (ShipAPI) other;
            if (!otherShip.isHulk()) continue;
            if (otherShip.isPiece()) continue;
            if (otherShip.getCustomData().get(WISPERED_KEY) != null) continue;
            if (otherShip.getCustomData().get(WISPERED_KEY + ship.getId()) != null) continue;
            if (otherShip.isFighter()) continue;

            if (other.getCollisionClass() == CollisionClass.NONE) continue;

            float radius = Misc.getTargetingRadius(from, other, false);
            float dist = Misc.getDistance(from, other.getLocation()) - radius;
            if (dist > range) {
                otherShip.setCustomData(WISPERED_KEY + ship.getId(), true);
                continue;
            }
            float score = dist;

            if (score < minScore) {
                minScore = score;
                best = other;
            }
        }
        return best;
    }
}