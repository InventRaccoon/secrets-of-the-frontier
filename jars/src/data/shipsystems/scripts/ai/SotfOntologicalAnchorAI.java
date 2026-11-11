package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.shipsystems.SotfOntologicalAnchorScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;


public class SotfOntologicalAnchorAI implements ShipSystemAIScript {

	protected ShipAPI ship;
	protected CombatEngineAPI engine;
	protected ShipwideAIFlags flags;
	protected ShipSystemAPI system;
	protected SotfOntologicalAnchorScript script;

	private float desire = 0f; // slowly rising desire to use system: faster depending on circumstance
	protected IntervalUtil tracker = new IntervalUtil(0.5f, 1f);
	
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.flags = flags;
		this.engine = engine;
		this.system = system;
		
		script = (SotfOntologicalAnchorScript) system.getScript();
	}
	
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		tracker.advance(amount);
		
		if (tracker.intervalElapsed()) {
			if (!AIUtils.canUseSystemThisFrame(ship)) return;

			ShipAPI pick = getWeightedTargets(target).getItemWithHighestWeight();
			if (pick != null) {
				ship.getAIFlags().setFlag(AIFlags.CUSTOM1, 1.5f, pick);
				ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
			}
		}
	}
	
	public List<ShipAPI> getPossibleTargets() {
		List<ShipAPI> result = new ArrayList<>();
		CombatEngineAPI engine = Global.getCombatEngine();
		
		List<ShipAPI> ships = engine.getShips();
		for (ShipAPI other : ships) {
			if (other == ship) continue;
			if (!script.isValidLashTarget(ship, other, false)) continue;
			if (!script.isInRange(ship, other)) continue;
			result.add(other);
		}
		return result;
	}
	
	public WeightedRandomPicker<ShipAPI> getWeightedTargets(ShipAPI shipTarget) {
		WeightedRandomPicker<ShipAPI> picker = new WeightedRandomPicker<>();
		
		for (ShipAPI other : getPossibleTargets()) {
			float w = 0f;
			if (ship.getOwner() == other.getOwner()) {
				continue;
			}
			float dist = Misc.getDistance(ship.getLocation(), other.getLocation());
			dist -= ship.getCollisionRadius() + other.getCollisionRadius();
			if (dist < 0) dist = 0;
			if (other == shipTarget) {
				w += 0.25f;
			}
			if (dist < 1000f) {
				w += (1f - (dist / 1000f)) * 0.5f;
			}
			// prio dephasing
			if (other.isPhased()) {
				w += 0.5f;
			}
			if (other.isFighter()) {
				w *= 0.25f;
			}
			w += 0.01f;
			picker.add(other, w);
		}
		return picker;
	}

}






















