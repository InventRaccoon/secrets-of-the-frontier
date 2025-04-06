// Fighters gain the Wispblossom hullmod, which causes them to spawn a lesser wisp if they become a hulk
package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class SotfWisptender extends BaseHullMod {
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        fighter.getVariant().addMod("sotf_wispblossom");
    }
}
