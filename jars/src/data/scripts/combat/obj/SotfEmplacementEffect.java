// objective effect: spawns a ship that fights for the objective holder
package data.scripts.combat.obj;

import java.awt.*;
import java.io.IOException;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseBattleObjectiveEffect;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

public class SotfEmplacementEffect extends BaseBattleObjectiveEffect {

	private FleetMemberAPI member;
	private ShipAPI ship;
	private PersonAPI officer;
	private Vector2f loc;
	private String type;
	private float radiusPad;
	private static final String VARIANT_CSV = "data/config/sotf/emplacement_variants.csv";

    private boolean first_spawned = false;
    private float facing = 0f;
	private static float respawn_timer = 60;
	private static String RESPAWN_KEY = "sotf_emplacement_respawn_key";

	public static class SotfEmplacementRespawnTimer {
		IntervalUtil interval = new IntervalUtil(respawn_timer, respawn_timer);
	}

	public void init(CombatEngineAPI engine, BattleObjectiveAPI objective) {
		super.init(engine, objective);

		officer = Global.getFactory().createPerson();
		officer.setAICoreId(Commodities.GAMMA_CORE);
		officer.setName(new FullName(Global.getSettings().getCommoditySpec(Commodities.GAMMA_CORE).getName(), "", FullName.Gender.ANY));
		officer.setPortraitSprite("graphics/portraits/portrait_ai1b.png");
		officer.getStats().setLevel(4);
		officer.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
		officer.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
		officer.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
		officer.getStats().setSkillLevel(Skills.STRIKE_COMMANDER, 2); // deprecate THIS, Alex
		// pick a variant and location
		try {
			pickVariant();
		} catch (IOException | JSONException ex) {
            // oopsy daisy
        }
		facing = (float) Math.random() * 360f;
        loc = MathUtils.getRandomPointOnCircumference(objective.getLocation(), 500 + (150 * (float)Math.random()) + radiusPad);
	}

	public void advance(float amount) {
		if (engine.isPaused()) {return;}
		boolean render_blueprint = true;
		float blueprint_alpha = 200f;
		CombatEngineAPI engine = Global.getCombatEngine();
		ShipHullSpecAPI hullspec = Global.getSettings().getVariant(type).getHullSpec();
		SpriteAPI hullsprite = Global.getSettings().getSprite(hullspec.getSpriteName());
		Color blueprintcolor = getBlueprintColor();

		String key = RESPAWN_KEY + "_" + objective.toString();
		SotfEmplacementRespawnTimer data = (SotfEmplacementRespawnTimer) engine.getCustomData().get(key);
		if (data == null) {
			data = new SotfEmplacementRespawnTimer();
			engine.getCustomData().put(key, data);
		}

		if (ship != null) {
			if ((objective.getOwner() == 0 || objective.getOwner() == 1) && !ship.isAlive() && !engine.getFleetManager(objective.getOwner()).getTaskManager(false).isInFullRetreat()) {
				data.interval.advance(amount);
				blueprint_alpha = 200f * (data.interval.getElapsed() / respawn_timer);
				if (data.interval.intervalElapsed()) {
					replaceTurret();
					Color color = Misc.getPositiveHighlightColor();
					if (objective.getOwner() == 1) {
						color = Misc.getNegativeHighlightColor();
					}
					Global.getCombatEngine().getCombatUI().addMessage(1, ship, color, objective.getDisplayName(), Misc.getTextColor(), " has completed repairs");
				}
			} else {
				data.interval.setElapsed(0);
			}

			// change to the objective's side
			if (ship.isAlive()) {
				render_blueprint = false;
				boolean player_full_retreat = engine.getFleetManager(0).getTaskManager(false).isInFullRetreat();
				boolean enemy_full_retreat = engine.getFleetManager(1).getTaskManager(true).isInFullRetreat();
				boolean no_player_ships = true;
				boolean no_enemy_ships = true;

				for (ShipAPI ship : engine.getShips()) {
					if (ship.getOwner() == 0 && !ship.getHullSpec().hasTag("sotf_reinforcementship") && !ship.getHullSpec().hasTag("sotf_empl") && !ship.isHulk() && !ship.isFighter()) {
						no_player_ships = false;
					}
					if (ship.getOwner() == 1 && !ship.getHullSpec().hasTag("sotf_reinforcementship") && !ship.getHullSpec().hasTag("sotf_empl") && !ship.isHulk() && !ship.isFighter()) {
						no_enemy_ships = false;
					}
				}

				if (no_enemy_ships || player_full_retreat || enemy_full_retreat) {
					engine.applyDamage(ship, ship.getLocation(), 1000000f, DamageType.HIGH_EXPLOSIVE, 0f, true, false, ship);
					for (ShipAPI module : ship.getChildModulesCopy()) {
						engine.applyDamage(module, module.getLocation(), 1000000f, DamageType.HIGH_EXPLOSIVE, 0f, true, false, module);
					}
					return;
				}

				facing = ship.getFacing();
				if (objective.getOwner() == 0 && ship.getOwner() == 1) {
					//setSide(0);
					engine.removeEntity(ship);
					replaceTurret();
				}
				if (objective.getOwner() == 1 && ship.getOwner() == 0) {
					//setSide(1);
//					for (CombatFleetManagerAPI.AssignmentInfo assignment : engine.getFleetManager(0).getTaskManager(false).getAllAssignments().) {
//						if (assignment.getTarget().equals(ship)) {
//							engine.getFleetManager(0).getTaskManager(false).removeAssignment(assignment);
//						}
//					}
					engine.removeEntity(ship);
					replaceTurret();
				}
			}
		} else if ((objective.getOwner() == 0 || objective.getOwner() == 1) && !first_spawned) {
			first_spawned = true;
			replaceTurret();
		}

		blueprintcolor = Misc.setAlpha(blueprintcolor, (int) blueprint_alpha);
		if (render_blueprint) {
			MagicRender.singleframe(
					hullsprite,
					loc,
					new Vector2f(hullsprite.getWidth(), hullsprite.getHeight()),
					facing - 90f,
					blueprintcolor,
					true
			);
		}
	}


	public String getLongDescription() {
		float min = Global.getSettings().getFloat("minFractionOfBattleSizeForSmallerSide");
		int total = Global.getSettings().getBattleSize();
		int maxPoints = (int)Math.round(total * (1f - min));
		String variant_string = null;
		if (type != null) {
			ShipVariantAPI variant = Global.getSettings().getVariant(type);
			variant_string = "type: " + variant.getHullSpec().getDesignation() + ", " + variant.getDisplayName() + " variant";
		}
		return String.format(
				variant_string + "\n" +
						"attacks enemies of objective holder\n" +
				"reconstructed " + (int) respawn_timer + " seconds after destruction\n\n" +
				"+%d bonus deployment points\n" +
				"up to a maximum of " + maxPoints + " points",
				getBonusDeploymentPoints());
	}

	public List<ShipStatusItem> getStatusItemsFor(ShipAPI ship) {
		return null;
	}

	private void pickVariant() throws IOException, JSONException {
		WeightedRandomPicker<JSONObject> post = new WeightedRandomPicker<JSONObject>();

		JSONArray variants = Global.getSettings().getMergedSpreadsheetDataForMod("id", VARIANT_CSV, "frontiersecrets");
        for(int i = 0; i < variants.length(); i++) {
            JSONObject row = variants.getJSONObject(i);

            if (row.getString("obj_id").equals(objective.getType())) {
				post.add(row, (float) row.getDouble("weight"));
            }
        }
		JSONObject pickedRow = post.pick();
		type = pickedRow.getString("id");
		radiusPad = (float) pickedRow.getDouble("extra_distance");
	}

	private void setSide(int side) {
		ship.setOwner(side);
		ship.setOriginalOwner(side);
		ship.getFleetMember().setOwner(side);
		ship.setShipTarget(null);
		ship.getShipAI().cancelCurrentManeuver();
		ship.getShipAI().forceCircumstanceEvaluation();
		for (FighterWingAPI wing : ship.getAllWings()) {
			wing.setWingOwner(side);
			for (ShipAPI fighter : wing.getWingMembers()) {
				fighter.setOwner(side);
			}
		}
		if (side == 0) {
			ship.setAlly(true);
			ship.getFleetMember().setAlly(true);
		} else {
			ship.setAlly(false);
			ship.getFleetMember().setAlly(false);
		}
		for (ShipAPI module : ship.getChildModulesCopy()) {
			module.setOwner(side);
			module.setOriginalOwner(side);
			module.setAlly(false);
			module.setShipTarget(null);
			if (side == 0) {
				module.setAlly(true);
			} else {
				module.setAlly(false);
			}
			for (FighterWingAPI wing : module.getAllWings()) {
				wing.setWingOwner(side);
				for (ShipAPI fighter : wing.getWingMembers()) {
					fighter.setOwner(side);
				}
			}
		}
	}

	private void replaceTurret() {
		engine.getFleetManager(objective.getOwner()).setSuppressDeploymentMessages(true);
		// kill old turret's fighters
		if (ship != null) {
			for (FighterWingAPI wing : ship.getAllWings()) {
				for (ShipAPI fighter : wing.getWingMembers()) {
					engine.applyDamage(fighter, fighter.getLocation(),
							1000000, DamageType.HIGH_EXPLOSIVE, 0f,
							true, false,
							null);
				}
			}
		}
		member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, type);
		member.setShipName("");
		member.setCaptain(officer);
		member.setOwner(objective.getOwner());
		ship = engine.getFleetManager(objective.getOwner()).spawnFleetMember(member, loc, facing, 0f);
		ship.getHullSpec().addTag("sotf_empl");
		ship.getHullSpec().addTag("no_combat_chatter");
		ship.setStation(true);
		ship.setFixedLocation(ship.getLocation());
		ship.setMediumDHullOverlay();
		ship.getMutableStats().getBreakProb().setBaseValue(1f);
		ship.getMutableStats().getHullCombatRepairRatePercentPerSecond().modifyFlat("sotf_empl", 0.5f);
		ship.getMutableStats().getMaxCombatHullRepairFraction().modifyFlat("sotf_empl", 1f);
		ship.getMutableStats().getDynamic().getMod(Stats.SHIP_OBJECTIVE_CAP_RANGE_MOD).modifyFlat("sotf_empl", -5000f);
		ship.getVariant().addTag(Tags.VARIANT_DO_NOT_DROP_AI_CORE_FROM_CAPTAIN);
		if (objective.getOwner() == 0) {
			ship.setAlly(true);
			member.setAlly(true);
		}
		for (WeaponAPI weapon : ship.getAllWeapons()) {
			weapon.disable(false);
		}
		for (ShipAPI module : ship.getChildModulesCopy()) {
			for (WeaponAPI weapon : module.getAllWeapons()) {
				weapon.disable(false);
			}
		}
		engine.getFleetManager(objective.getOwner()).setSuppressDeploymentMessages(false);
		// Delete the emplacement from the owner's deployed ship list
		//com.fs.starfarer.combat.CombatFleetManager realMan = (com.fs.starfarer.combat.CombatFleetManager) engine.getFleetManager(objective.getOwner());
	}

	private Color getBlueprintColor() {
		Color color = Color.CYAN;
		if (objective.getOwner() == 0) {
			color = Misc.getHighlightColor();
		} else if (objective.getOwner() == 1) {
			color = Misc.getNegativeHighlightColor();
		}
		return color;
	}
}