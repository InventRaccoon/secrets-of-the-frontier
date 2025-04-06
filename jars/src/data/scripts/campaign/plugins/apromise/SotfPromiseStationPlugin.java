// this one makes the anomalous station let off pings and glow
package data.scripts.campaign.plugins.apromise;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomEntitySpecAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import data.scripts.campaign.ids.SotfIDs;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SotfPromiseStationPlugin extends BaseCustomEntityPlugin {

	public static String GLOW_COLOR_KEY = "$core_beaconGlowColor";
	public static String PING_COLOR_KEY = "$core_beaconPingColor";
	
	public static float GLOW_FREQUENCY = 1.2f; // on/off cycles per second

	//private SectorEntityToken entity;

	transient private SpriteAPI sprite;
	transient private SpriteAPI glow;
	
	public void init(SectorEntityToken entity, Object pluginParams) {
		super.init(entity, pluginParams);
		//this.entity = entity;
		//entity.setDetectionRangeDetailsOverrideMult(0.75f);
		//CargoAPI extraPromiseSalvage = Global.getFactory().createCargo(true);
		//extraPromiseSalvage.addItems(CargoAPI.CargoItemType.SPECIAL, new SpecialItemData(SotfIDs.SIERRA_CORE, null), 1);
		//BaseSalvageSpecial.addExtraSalvage(entity, extraPromiseSalvage);
		readResolve();
	}
	
	Object readResolve() {
		sprite = Global.getSettings().getSprite("campaignEntities", "warning_beacon");
		glow = Global.getSettings().getSprite("campaignEntities", "warning_beacon_glow");
		return this;
	}
	
	private float phase = 0f;
	private float freqMult = 1f;
	private float sincePing = 10f;
	public void advance(float amount) {
		phase += amount * GLOW_FREQUENCY * freqMult;
		while (phase > 1) phase --;
		
		if (entity.isInCurrentLocation()) {
			sincePing += amount;
			if (sincePing >= 6f && phase > 0.1f && phase < 0.2f) {
				sincePing = 0f;
				CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
				if (playerFleet != null && 
					entity.getVisibilityLevelTo(playerFleet) == VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS) {
					
					String pingId = SotfIDs.PING_SIERRA;
					freqMult = 1f;
					
					//Global.getSector().addPing(entity, pingId);
					
					//Color pingColor = entity.getFaction().getBrightUIColor();
					Color pingColor = null;
					if (entity.getMemoryWithoutUpdate().contains(PING_COLOR_KEY)) {
						pingColor = (Color) entity.getMemoryWithoutUpdate().get(PING_COLOR_KEY);
					}
					
					Global.getSector().addPing(entity, pingId, pingColor);
				}
			}
		}
	}

	public float getRenderRange() {
		return entity.getRadius() + 100f;
	}

	public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
		float alphaMult = viewport.getAlphaMult();
		if (alphaMult <= 0f) return;
		
		CustomEntitySpecAPI spec = entity.getCustomEntitySpec();
		if (spec == null) return;
		
		float w = spec.getSpriteWidth();
		float h = spec.getSpriteHeight();
		
		Vector2f loc = entity.getLocation();
		
		sprite.setAngle(entity.getFacing() - 90f);
		sprite.setSize(w, h);
		sprite.setAlphaMult(alphaMult);
		sprite.setNormalBlend();
		sprite.renderAtCenter(loc.x, loc.y);
		
		
		float glowAlpha = 0f;
		if (phase < 0.5f) glowAlpha = phase * 2f;
		if (phase >= 0.5f) glowAlpha = (1f - (phase - 0.5f) * 2f);
		
		float glowAngle1 = (((phase * 1.3f) % 1) - 0.5f) * 12f;
		float glowAngle2 = (((phase * 1.9f) % 1) - 0.5f) * 12f;
//		glowAngle1 = 0f;
//		glowAngle2 = 0f;
		
		boolean glowAsLayer = true;
		if (glowAsLayer) {
			//glow.setAngle(entity.getFacing() - 90f);
			Color glowColor = new Color(205,155,255,255);
			//Color glowColor = entity.getFaction().getBrightUIColor();
			if (entity.getMemoryWithoutUpdate().contains(GLOW_COLOR_KEY)) {
				glowColor = (Color) entity.getMemoryWithoutUpdate().get(GLOW_COLOR_KEY);
			}

			//glow.setColor(Color.white);
			glow.setColor(glowColor);
			
			glow.setSize(w, h);
			glow.setAlphaMult(alphaMult * glowAlpha);
			glow.setAdditiveBlend();
			
			glow.setAngle(entity.getFacing() - 90f + glowAngle1);
			glow.renderAtCenter(loc.x, loc.y);
			
			glow.setAngle(entity.getFacing() - 90f + glowAngle2);
			glow.setAlphaMult(alphaMult * glowAlpha * 0.5f);
			glow.renderAtCenter(loc.x, loc.y);
		} else {
			glow.setAngle(entity.getFacing() - 90f);
			glow.setColor(new Color(205,155,255));
			float gs = w * 3;
			glow.setSize(gs, gs);
			glow.setAdditiveBlend();
			
			float spacing = 10;
			glow.setAlphaMult(alphaMult * glowAlpha * 0.5f);
			glow.renderAtCenter(loc.x - spacing, loc.y);
			glow.renderAtCenter(loc.x + spacing, loc.y);
			
			glow.setAlphaMult(alphaMult * glowAlpha);
			glow.setSize(gs * 0.25f, gs * 0.25f);
			glow.renderAtCenter(loc.x - spacing, loc.y);
			glow.renderAtCenter(loc.x + spacing, loc.y);
		}
	}
	

}









