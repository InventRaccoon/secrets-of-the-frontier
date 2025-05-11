package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.skills.SotfMissileReplication;
import data.scripts.campaign.skills.SotfPolarizedNanorepair;
import data.scripts.combat.special.SotfFelInvasionPlugin;
import data.scripts.utils.SotfMisc;
import data.subsystems.SotfNaniteDronesSubsystem;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static data.scripts.combat.special.SotfInvokeHerBlessingPlugin.UI_COLOR;
import static org.lwjgl.opengl.GL11.GL_ONE;

public class SotfDaydreamSynthesizer extends BaseHullMod {

	// Extra deployment point and supply cost, as % of base cost
	public static float DEPLOYMENT_COST_BONUS_MULT = 1f;

	// Additional % cost increase added on top of above for capitals
	// nvm just making it cost a lot of OP on capitals
	//public static float CAPITAL_COST_BONUS_MULT = 0.25f;

	// At this DP or lower, two copies are created
	// 8 is safe, 9 is spicy b/c it opens up Enforcer
	public static float TWO_MIMIC_MAXIMUM = 8f;

	public static String ALREADY_SYNTHESIZED_KEY = "sotf_usedDaydreamSynthesizer";

	public void advanceInCombat(ShipAPI ship, float amount) {
		if (!Global.getCurrentState().equals(GameState.COMBAT)) {
			return;
		}

		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.getCustomData().containsKey(ALREADY_SYNTHESIZED_KEY + "_" + ship.getId())) {
			return;
		}
		if (!ship.isAlive()) return;
		if (ship.getFullTimeDeployed() < 2) return;

		spawnMimic(ship);
		// Again, if low-DP
		if (ship.getHullSpec().getSuppliesToRecover() <= TWO_MIMIC_MAXIMUM) {
			spawnMimic(ship);
		}

		engine.getCustomData().put(ALREADY_SYNTHESIZED_KEY + "_" + ship.getId(), true);
	}

	public void spawnMimic(ShipAPI ship) {
		learnAllWeaponsAndHullmodsFromShip(ship.getVariant());

		ShipVariantAPI variant = ship.getVariant().clone();
		// don't do this: we need to keep the hullmod so it eats up the OP on the mimic
		//variant.removeMod(SotfIDs.HULLMOD_DAYDREAM_SYNTHESIZER);
		// clear all dmods, perfect replication
		for (String dmod : new ArrayList<String>(variant.getHullMods())) {
			if (Global.getSettings().getHullModSpec(dmod).hasTag(Tags.HULLMOD_DMOD)) {
				DModManager.removeDMod(variant, dmod);
			}
		}
		FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
		member.setShipName(Global.getSettings().createBaseFaction(SotfIDs.DREAMING_GESTALT).pickRandomShipName());
		member.setCaptain(SotfPeople.genReverie());
		member.updateStats();
		member.getRepairTracker().setCR(ship.getCurrentCR());
		member.getVariant().addPermaMod(HullMods.AUTOMATED);

		CampaignFleetAPI emptyFleet = Global.getFactory().createEmptyFleet(SotfIDs.DREAMING_GESTALT, "Nanite-Synthesized Ship", true);
		emptyFleet.getFleetData().addFleetMember(member);
		emptyFleet.setInflater(new DefaultFleetInflater(new DefaultFleetInflaterParams()));
		emptyFleet.getInflater().setQuality(1f);
		if (emptyFleet.getInflater() instanceof DefaultFleetInflater) {
			DefaultFleetInflater dfi = (DefaultFleetInflater) emptyFleet.getInflater();
			((DefaultFleetInflaterParams) dfi.getParams()).allWeapons = true;
			// try to replicate ship variant as closely as possible
			((DefaultFleetInflaterParams) dfi.getParams()).rProb = 0f;
		}
		emptyFleet.inflateIfNeeded();
		member.getVariant().addPermaMod(SotfIDs.HULLMOD_NANITE_SYNTHESIZED);

		Vector2f spawnLoc = Misc.getPointAtRadius(ship.getLocation(),
				ship.getCollisionRadius() + 400f + 200f * (float) Math.random());

		Global.getCombatEngine().getFleetManager(ship.getOriginalOwner()).spawnFleetMember(member, spawnLoc, 90f, 0f);
		ShipAPI newShip = Global.getCombatEngine().getFleetManager(ship.getOwner()).getShipFor(member);
		Global.getCombatEngine().addPlugin(new SotfDaydreamFadeinPlugin(newShip, 3f, ship.getFacing() - 15f + (30f * Misc.random.nextFloat())));
	}

	/**
	 * Dreaming Gestalt attempts to learn all of a ship's weapons, fighters and hullmods
	 * @param variant
	 */
	public static void learnAllWeaponsAndHullmodsFromShip(ShipVariantAPI variant) {
		if (Global.getSector() == null) return;
		if (Global.getSector().getFaction(SotfIDs.DREAMING_GESTALT) == null) return;
		for (String slot : variant.getNonBuiltInWeaponSlots()) {
			WeaponSpecAPI weapon = Global.getSettings().getWeaponSpec(variant.getWeaponId(slot));
			if (weapon.hasTag(Tags.RESTRICTED)) return;
			if (weapon.getTier() == 0) return;
			Global.getSector().getFaction(SotfIDs.DREAMING_GESTALT).addKnownWeapon(weapon.getWeaponId(), true);
		}
		for (String wing : variant.getNonBuiltInWings()) {
			FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(wing);
			if (spec.hasTag(Tags.NO_DROP)) return;
			Global.getSector().getFaction(SotfIDs.DREAMING_GESTALT).addKnownFighter(wing, true);
		}
		for (String hmod : variant.getNonBuiltInHullmods()) {
			HullModSpecAPI spec = Global.getSettings().getHullModSpec(hmod);
			if (spec.isHiddenEverywhere()) return;
			// no Dweller or Threat hullmods pls
			if (spec.getEffect().getRequiredItem() != null) return;
			Global.getSector().getFaction(SotfIDs.DREAMING_GESTALT).addKnownHullMod(hmod);
		}
	}

	public static class SotfDaydreamFadeinPlugin extends BaseEveryFrameCombatPlugin {
		float elapsed = 0f;
		CollisionClass collisionClass;

		ShipAPI ship;
		float fadeInTime;
		float angle;

		public String spawnText = "DEPLOYING COUNTERMEASURE";

		IntervalUtil interval = new IntervalUtil(0.075f, 0.125f);

		private static LazyFont.DrawableString TODRAW14;
		private static LazyFont.DrawableString TODRAW10;

		public SotfDaydreamFadeinPlugin(ShipAPI ship, float fadeInTime, float angle) {
			this.ship = ship;
			this.fadeInTime = fadeInTime;
			this.angle = angle;

			try {
				LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/victor14.fnt");
				TODRAW14 = fontdraw.createText();
				TODRAW14.setBlendSrc(GL_ONE);

				fontdraw = LazyFont.loadFont("graphics/fonts/victor10.fnt");
				TODRAW10 = fontdraw.createText();
				TODRAW10.setBlendSrc(GL_ONE);

			} catch (FontException ignored) {
			}
		}

		@Override
		public void advance(float amount, List<InputEventAPI> events) {
			if (Global.getCombatEngine().isPaused()) return;

			elapsed += amount;

			CombatEngineAPI engine = Global.getCombatEngine();

			float progress = Math.max(0f, (elapsed) / fadeInTime);
			if (progress > 1f) progress = 1f;

			if (progress < 0.5f) {
				ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
				ship.blockCommandForOneFrame(ShipCommand.TURN_LEFT);
				ship.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
				ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
				ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
			}

			ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
			ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
			ship.blockCommandForOneFrame(ShipCommand.FIRE);
			ship.blockCommandForOneFrame(ShipCommand.PULL_BACK_FIGHTERS);
			ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
			ship.setHoldFireOneFrame(true);
			ship.setHoldFire(true);

			List<ShipAPI> shipAndModules = ship.getChildModulesCopy();
			shipAndModules.add(ship);
			for (ShipAPI toFade : shipAndModules) {
				toFade.setExtraAlphaMult2(progress);

				toFade.setCollisionClass(CollisionClass.NONE);
				toFade.getMutableStats().getHullDamageTakenMult().modifyMult("ShardSpawnerInvuln", 0f);
				if (progress < 0.5f) {
					toFade.getVelocity().set(new Vector2f());
				} else if (progress > 0.75f) {
					toFade.setCollisionClass(CollisionClass.SHIP);
					toFade.getMutableStats().getHullDamageTakenMult().unmodifyMult("ShardSpawnerInvuln");
				}
			}

			float jitterRange = 1f - progress;
			float maxRangeBonus = 50f;
			float jitterRangeBonus = jitterRange * maxRangeBonus;
			Color c = SotfFelInvasionPlugin.JITTER_COLOR;

			//ship.setJitter(this, c, progress, 25, 0f, jitterRangeBonus);

			interval.advance(amount);
			if (interval.intervalElapsed() && progress < 0.8f) {
				c = RiftLanceEffect.getColorForDarkening(SotfNaniteSynthesized.SMOKE_COLOR);
				c = Misc.setAlpha(c, 55);
				float baseDuration = 2f;
				Vector2f vel = new Vector2f(ship.getVelocity());
				float size = ship.getCollisionRadius() * 0.35f;
				for (int i = 0; i < 3; i++) {
					Vector2f point = new Vector2f(ship.getLocation());
					point = Misc.getPointWithinRadiusUniform(point, ship.getCollisionRadius() * 0.5f, Misc.random);
					float dur = baseDuration + baseDuration * (float) Math.random();
					float nSize = size;
					Vector2f pt = Misc.getPointWithinRadius(point, nSize * 0.5f);
					Vector2f v = Misc.getUnitVectorAtDegreeAngle((float) Math.random() * 360f);
					v.scale(nSize + nSize * (float) Math.random() * 0.5f);
					v.scale(0.2f);
					Vector2f.add(vel, v, v);

					float maxSpeed = nSize * 1.5f * 0.2f;
					float minSpeed = nSize * 1f * 0.2f;
					float overMin = v.length() - minSpeed;
					if (overMin > 0) {
						float durMult = 1f - overMin / (maxSpeed - minSpeed);
						if (durMult < 0.1f) durMult = 0.1f;
						dur *= 0.5f + 0.5f * durMult;
					}
					engine.addNegativeNebulaParticle(pt, v, nSize * 1f, 2f,
							0.5f / dur, 0f, dur, c);
				}
			}

			if (elapsed > fadeInTime) {
				for (ShipAPI toFade : shipAndModules) {
					toFade.setAlphaMult(1f);
					toFade.setHoldFire(false);
					toFade.setCollisionClass(CollisionClass.SHIP);
					toFade.getMutableStats().getHullDamageTakenMult().unmodifyMult("ShardSpawnerInvuln");
				}
				engine.removePlugin(this);
			}
		}

		@Override
		public void renderInUICoords(ViewportAPI viewport) {
			super.renderInUICoords(viewport);

			float progress = Math.max(0f, elapsed - 2f);
			if (progress > 1f) progress = 1f;

			LazyFont.DrawableString toUse = TODRAW14;
			if (toUse != null) {
				int alpha = Math.round(255 - (255 * progress));

				String text = spawnText;

				//TODRAW14.setFontSize(28);
				toUse.setText(text);
				toUse.setAnchor(LazyFont.TextAnchor.BOTTOM_CENTER);
				toUse.setAlignment(LazyFont.TextAlignment.CENTER);
				Vector2f pos = new Vector2f(viewport.convertWorldXtoScreenX(ship.getShieldCenterEvenIfNoShield().x),
				viewport.convertWorldYtoScreenY(ship.getShieldCenterEvenIfNoShield().y + ship.getShieldRadiusEvenIfNoShield() + 50f));
				toUse.setBaseColor(Misc.setBrightness(Color.BLACK, alpha));
				toUse.draw(pos.x + 1, pos.y - 1);
				toUse.setBaseColor(Misc.setBrightness(UI_COLOR, alpha));
				toUse.draw(pos);
			}
		}
	}

	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		float dpMult = DEPLOYMENT_COST_BONUS_MULT;
		if (stats.getVariant().getHullSpec().getSuppliesToRecover() <= TWO_MIMIC_MAXIMUM) {
			dpMult *= 2f;
		}
		//if (hullSize == ShipAPI.HullSize.CAPITAL_SHIP) {
		//	dpMult += CAPITAL_COST_BONUS_MULT;
		//}
		stats.getSuppliesToRecover().modifyMult(id, 1f + dpMult);
		//stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, 1f + dpMult);
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return reverieCanSynthesize(ship.getHullSpec());
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (!reverieCanSynthesize(ship.getHullSpec())) {
			return "Hull design is too unusual or excessively modified to replicate";
		}
		return super.getUnapplicableReason(ship);
	}

	public static boolean reverieCanSynthesize(ShipHullSpecAPI spec) {
		if (spec.isDefaultDHull()) spec = spec.getDParentHull();
		return SotfMisc.shipHasBlueprint(spec) || spec.hasTag(Tags.SHIP_REMNANTS) ||
				spec.hasTag("derelict") ||
				spec.hasTag(SotfIDs.TAG_DUSTKEEPER_AUXILIARY) ||
				spec.hasTag("sotf_dustkeeper");
	}

	@Override
	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 3f;
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		Color good = Misc.getPositiveHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();
		Color lgray = Misc.getGrayColor().brighter();
		Color gray = Misc.getGrayColor();

		tooltip.addPara("Marks the ship for analysis and replication by a nanomechanical subswarm of the Dreaming Gestalt.", opad);

		tooltip.addPara("Upon deployment, the nanite swarm activates and synthesizes a temporary reflection of the ship, fighting " +
				"under the captaincy of %s. Ships with a %s of %s or less will spawn an additional reflection.", opad, h, "Reverie, the Voice", "base deployment point cost", "" + Math.round(TWO_MIMIC_MAXIMUM));

		//tooltip.addPara("Mimics have no d-mods, and attempt to replicate the original ship's loadout as closely as possible. " +
		//		"The subswarm attempts to learn any unknown weapons or fighters, but may be unable to replicate extremely rare and exotic designs.", opad);

		//tooltip.addPara("The ship's deployment point cost and supplies-to-recover are both increased by %s " +
		//		"for each mimic it would spawn on deployment. Capital ships have an additional %s increase.", opad, h,
		//		"" + Math.round(DEPLOYMENT_COST_BONUS_MULT * 100) + "%",
		//		"" + Math.round(CAPITAL_COST_BONUS_MULT * 100) + "%");

		tooltip.addPara("The ship's supplies-to-recover is increased by %s " +
						"for each reflection it would spawn on deployment. Each reflection uses deployment points equal to the original ship.", opad, h,
				"" + Math.round(DEPLOYMENT_COST_BONUS_MULT * 100) + "%");

//		tooltip.addPara("Once a ship activates this hullmod, it can't activate for any other ship in the fleet " +
//				"until the next engagement.", opad);

		tooltip.addSectionHeading("Ship Synthesis", Alignment.MID, opad);
		tooltip.addPara("- Only ships with a readily-available blueprint can be replicated, including anything one could reasonably find in Domain-era ruins. Common Domain, " +
				"Tri-Tachyon and Dustkeeper drone designs can also be mimicked.", opad);
		tooltip.addPara("- Reflections have no d-mods.", opad);
		tooltip.addPara("- Subswarm attempts to scan and learn all weapon and fighter designs to replicate the " +
				"original ship's loadout as closely as possible. Unusually exotic weapon designs may be " +
				"exchanged for more common alternatives.", opad);

		tooltip.addSectionHeading("Daydream Skills", Alignment.MID, opad);
		tooltip.addPara("Reverie's skillset is roughly equivalent in power to an alpha core, but takes the form of " +
				"7 unique skills that provide unique bonuses such as periodic missile reloads, gradual armor repair and a " +
				"defensive swarm . See the hullmod's related entries in the codex for a full list.", opad, h,
				"alpha core", "7 unique skills", "periodic missile reloads", "gradual armor repair", "defensive swarm", "related entries");
//		tooltip.addPara("Reverie's skillset is roughly equivalent in power to an alpha core, but taking the " +
//				"form of 6 unique skills that include the following bonuses: ", opad, new Color[]{h, bad}, "Capital ships", "cannot be dragged");
//		tooltip.addPara("- Regeneration of small missile weapon ammo every %s seconds equal to their " +
//				"base ammmo", pad,  h, "" + Misc.getRoundedValueMaxOneAfterDecimal(SotfMissileReplication.RELOAD_TIMER));
//		tooltip.addPara("- Regeneration of medium missile weapon ammo every %s seconds equal to half of " +
//				"their base ammmo", pad, h, "" + Misc.getRoundedValueMaxOneAfterDecimal(SotfMissileReplication.RELOAD_TIMER));
//		tooltip.addPara("- Repair of %s of max armor per second, up to %s armor or %s of max armor, whichever is higher", pad,  h,
//				Misc.getRoundedValueMaxOneAfterDecimal(SotfPolarizedNanorepair.ARMOR_REGEN_RATE * 100f) + "%",
//				"" + Misc.getRoundedValue(SotfPolarizedNanorepair.TOTAL_ARMOR_REGEN_MAX_POINTS),
//				Misc.getRoundedValueMaxOneAfterDecimal(SotfPolarizedNanorepair.TOTAL_ARMOR_REGEN_MAX_FRACTION * 100f) + "%");
//		tooltip.addPara("- An escort of %s/%s/%s/%s point-defense drones, depending on hull size", pad,  h,
//				"" + SotfNaniteDronesSubsystem.DRONES_FRIGATE,
//				"" + SotfNaniteDronesSubsystem.DRONES_DESTROYER,
//				"" + SotfNaniteDronesSubsystem.DRONES_CRUISER,
//				"" + SotfNaniteDronesSubsystem.DRONES_CAPITAL);

		if (isForModSpec || ship == null) return;

		// try for specific skin quote
		String quote = Global.getSettings().getDescription(ship.getHullSpec().getHullId() + "_sotfdds", Description.Type.CUSTOM).getText1();
		// else try for base hull's quote
		if (quote.equals("No description... yet")) {
			quote = Global.getSettings().getDescription(ship.getHullSpec().getBaseHullId() + "_sotfdds", Description.Type.CUSTOM).getText1();
			if (quote.equals("No description... yet")) {
				quote = "Look into the Lake and see your reflection.";
			}
		}
		tooltip.addSpacer(5f);
		LabelAPI label = tooltip.addPara("\"" + quote + "\"", lgray, opad);
		label.italicize();
		tooltip.addPara("   - Reverie, the Voice", gray, pad);

		// TODO: list of all compatible ships on F1

		//if (isForModSpec || ship == null) return;
	}

	@Override
	public CargoStackAPI getRequiredItem() {
		return Global.getSettings().createCargoStack(CargoAPI.CargoItemType.SPECIAL,
				new SpecialItemData(SotfIDs.DAYDREAM_ANALYZER, null), null);
	}
}
