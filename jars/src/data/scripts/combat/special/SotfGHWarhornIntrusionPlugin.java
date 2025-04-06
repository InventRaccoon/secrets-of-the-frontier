// Warhorn's entry into Good Hunting
package data.scripts.combat.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.hullmods.ShardSpawner;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.SotfPhantasmalShip;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.skills.SotfLeviathansBane;
import org.magiclib.util.MagicAnim;
import data.scripts.utils.SotfMisc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;

public class SotfGHWarhornIntrusionPlugin extends BaseCombatLayeredRenderingPlugin {

	private float pulseProgress = 0f;
	private float maxPulseProgress = 3f;
	private float progress = 0f;
	private boolean spawned = false;
	private ShipAPI[] warhorn; // 0 = Harbinger, 1 = Champion
	private Vector2f loc;
	private int chatStage = 0;

	public SotfGHWarhornIntrusionPlugin(Vector2f loc) {
		this.loc = loc;
		//Global.getCombatEngine().addSwirlyNebulaParticle(loc, new Vector2f(0,0), 10f, 20f, 1f, 1f, 3f, SotfMisc.getEidolonColor(), false);
	}

	public void advance(float amount) {
		progress += amount;
		entity.getLocation().set(loc);
		if (progress > (maxPulseProgress * 0.5f) && !spawned) {
			spawned = true;
			spawnWarhorn(entity.getLocation());
		}
		if (progress < maxPulseProgress) {
			pulseProgress += amount;
		} else {
			pulseProgress = 0f;
		}
		if (pulseProgress >= 1f) {
			pulseProgress -= 1f;
		}
		if (progress < maxPulseProgress) {
			return;
		}
		float timeSinceSpawn = progress - maxPulseProgress;
		boolean harbAlive = true;
		boolean champAlive = true;
		ShipAPI ship1 = warhorn[0];
		ShipAPI ship2 = warhorn[1];
		if (warhorn[0] == null || warhorn[1] == null) return;
		if (!warhorn[0].isAlive()) {
			ship1 = warhorn[1];
			harbAlive = false;
		}
		if (!warhorn[1].isAlive()) {
			ship2 = warhorn[0];
			champAlive = false;
		}
		if (!harbAlive && !champAlive) return;

		if (timeSinceSpawn >= 6 && chatStage == 0) {
			Global.getSoundPlayer().playUISound("sotf_ghost_angry", 1.2f, 1.2f);
			Global.getCombatEngine().getCombatUI().addMessage(1, ship1, SotfMisc.getEidolonColor(), "Hello, little fish... care to join the hunt?");
			chatStage = 1;
		}
		if (timeSinceSpawn >= 8 && chatStage == 1) {
			Global.getCombatEngine().getCombatUI().addMessage(1, Misc.getPositiveHighlightColor(), "Received ", Misc.getHighlightColor(), "targeting keys (?)");
			Global.getCombatEngine().getPlayerShip().addListener(new SotfLeviathansBane.SotfLeviathansBaneScript(Global.getCombatEngine().getPlayerShip()));
			chatStage = 2;
		}
		if (timeSinceSpawn >= 10 && chatStage == 2) {
			Global.getSoundPlayer().playUISound("sotf_ghost_angry", 1f, 1.2f);
			Global.getCombatEngine().getCombatUI().addMessage(1, ship2,
					SotfMisc.getEidolonColor(), "Paint the beasties and let ",
					Misc.getHighlightColor(), "it",
					SotfMisc.getEidolonColor(), " loose, keh heh heh...");
			chatStage = 3;
		}
	}

	public void render(CombatEngineLayers layer, ViewportAPI viewport) {
		if (progress > maxPulseProgress) {
			return;
		}
		Color color = SotfMisc.getEidolonColor();
		float radius = 150f + (350f * MagicAnim.smooth(pulseProgress));
		int segments = (int) ((radius * 2f * 3.14f) / 2f);
		float alpha = 1f - (pulseProgress * 0.5f);

		if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
			renderRing(entity.getLocation().x,
					entity.getLocation().y,
						radius,
						6f, alpha,
						segments,
						Global.getSettings().getSprite("combat", "corona_soft"),
					color, true);
		}
	}

	public float getRenderRadius() {
		return 500f;
	}

	@Override
	public EnumSet<CombatEngineLayers> getActiveLayers() {
		return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
	}

	public boolean isExpired() {
		return super.isExpired();
	}

	private void renderRing(float x, float y, float radius, float thickness, float alphaMult, int segments, SpriteAPI tex, Color color, boolean additive) {
		float startRad = (float) Math.toRadians(0);
		float endRad = (float) Math.toRadians(360);
		float spanRad = Misc.normalizeAngle(endRad - startRad);
		float anglePerSegment = spanRad / segments;

		GL11.glPushMatrix();
		GL11.glTranslatef(x, y, 0);
		GL11.glRotatef(0, 0, 0, 1);
		GL11.glEnable(GL11.GL_TEXTURE_2D);

		tex.bindTexture();

		GL11.glEnable(GL11.GL_BLEND);
		if (additive) {
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		} else {
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		}

		GL11.glColor4ub((byte)color.getRed(),
				(byte)color.getGreen(),
				(byte)color.getBlue(),
				(byte)((float) color.getAlpha() * alphaMult));
		float texX = 0f;
		float incr = 1f / segments;
		GL11.glBegin(GL11.GL_QUAD_STRIP);
		for (float i = 0; i < segments + 1; i++) {
			boolean last = i == segments;
			if (last) i = 0;
			float theta = anglePerSegment * i;
			float cos = (float) Math.cos(theta);
			float sin = (float) Math.sin(theta);

			float m1 = 1f;
			float m2 = 1f;

			float x1 = cos * radius * m1;
			float y1 = sin * radius * m1;
			float x2 = cos * (radius + thickness * m2);
			float y2 = sin * (radius + thickness * m2);

			GL11.glTexCoord2f(0.5f, 0.05f);
			GL11.glVertex2f(x1, y1);

			GL11.glTexCoord2f(0.5f, 0.95f);
			GL11.glVertex2f(x2, y2);

			texX += incr;
			if (last) break;
		}

		GL11.glEnd();
		GL11.glPopMatrix();
	}

	public void spawnWarhorn(Vector2f loc) {
		FactionAPI eidolonFaction = Global.getSettings().createBaseFaction(SotfIDs.SYMPHONY);
		PersonAPI warhornPerson = SotfPeople.genWarhorn(true);

		//CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(eidolonFaction.getId(), "Warhorn", false);
		//fleet.setCommander(warhornPerson);
		//fleet.getFleetData().addFleetMember(member);

		//member.setFleetCommanderForStats(warhornPerson, fleet.getFleetData());

		CombatFleetManagerAPI fleetManager = Global.getCombatEngine().getFleetManager(FleetSide.PLAYER);
		fleetManager.setSuppressDeploymentMessages(true);

		ShipAPI[] ships = new ShipAPI[2];
		for (int i = 0; i <= 1; i++) {
			String shipName = "Up the Riverbank";
			String variant = "sotf_harbinger_Warhorn";
			if (i == 1) {
				shipName = "Children of Everwinter";
				variant = "sotf_champion_Warhorn";
			}
			ShipAPI ship = fleetManager.spawnShipOrWing(variant, new Vector2f(loc.x - (350f * i), loc.y + (450f * i)), 75f + (15f * i), 0f);
			ship.getFleetMember().getRepairTracker().setCR(1f);
			ship.getFleetMember().setCaptain(warhornPerson);
			ship.getFleetMember().setShipName(shipName);
			ship.setCaptain(warhornPerson);
			ship.setAlly(true);
			ship.addListener(new SotfPhantasmalShip.SotfBanishmentDeathScript(ship));
			if (i == 0) {
				ship.getVariant().addPermaMod(HullMods.ADAPTIVE_COILS, true);
			} else {
				ship.getVariant().addPermaMod(HullMods.HARDENED_SHIELDS, true);
			}
			ship.getVariant().addPermaMod(HullMods.HEAVYARMOR, true);
			ship.getVariant().addPermaMod(HullMods.NEURAL_INTERFACE, true);
			ship.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
			ship.setCRAtDeployment(1f);
			ship.setCurrentCR(1f);
			ship.setName(shipName);
			ship.setInvalidTransferCommandTarget(true);
			ships[i] = ship;
		}
		this.warhorn = ships;
		fleetManager.setSuppressDeploymentMessages(false);
		Global.getSoundPlayer().playUISound("sotf_ghost_angry", 0.9f, 1.2f);
		Global.getCombatEngine().getCombatUI().addMessage(1, ships[0], ships[1]);
		// smoke and fadein
		Global.getCombatEngine().addPlugin(new SotfGHWarhornFadeInPlugin(ships[0], 5f, -15f));
		Global.getCombatEngine().addPlugin(new SotfGHWarhornFadeInPlugin(ships[1], 6f, 15f));
	}

	public static class SotfGHWarhornFadeInPlugin extends BaseEveryFrameCombatPlugin {
		float elapsed = 0f;
		CollisionClass collisionClass;

		ShipAPI ship;
		float fadeInTime;
		float angle;

		IntervalUtil interval = new IntervalUtil(0.075f, 0.125f);

		public SotfGHWarhornFadeInPlugin(ShipAPI ship, float fadeInTime, float angle) {
			this.ship = ship;
			this.fadeInTime = fadeInTime;
			this.angle = angle;
		}


		@Override
		public void advance(float amount, List<InputEventAPI> events) {
			if (Global.getCombatEngine().isPaused()) return;

			elapsed += amount;

			CombatEngineAPI engine = Global.getCombatEngine();

			float progress = (elapsed) / fadeInTime;
			if (progress > 1f) progress = 1f;

			// halved compared to e.g Omega reinforcer fadein, since phantasmal ships have 50% alpha normally
			ship.setAlphaMult(progress / 2);

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


			ship.setCollisionClass(CollisionClass.NONE);
			ship.getMutableStats().getHullDamageTakenMult().modifyMult("ShardSpawnerInvuln", 0f);
			if (progress < 0.5f) {
				ship.getVelocity().set(new Vector2f());
			} else if (progress > 0.75f){
				ship.setCollisionClass(CollisionClass.SHIP);
				ship.getMutableStats().getHullDamageTakenMult().unmodifyMult("ShardSpawnerInvuln");
			}

			float jitterRange = 1f - progress;
			float maxRangeBonus = 50f;
			float jitterRangeBonus = jitterRange * maxRangeBonus;
			Color c = ShardSpawner.JITTER_COLOR;

			ship.setJitter(this, c, progress / 2, 25, 0f, jitterRangeBonus);

			interval.advance(amount);
			if (interval.intervalElapsed() && progress < 0.8f) {
				c = Misc.setAlpha(SotfMisc.getEidolonColor(), 155);
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
					engine.addNebulaParticle(pt, v, nSize * 1f, 2f,
							0.5f / dur, 0f, dur, c);
				}
			}

			if (elapsed > fadeInTime) {
				ship.setAlphaMult(0.5f);
				ship.setHoldFire(false);
				ship.setCollisionClass(CollisionClass.SHIP);
				ship.getMutableStats().getHullDamageTakenMult().unmodifyMult("ShardSpawnerInvuln");
				engine.removePlugin(this);
			}
		}
	}

}
