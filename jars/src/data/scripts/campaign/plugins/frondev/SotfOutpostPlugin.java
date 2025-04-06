// renders a constructible station's sprite. Changes with what station is built on the associated colony
package data.scripts.campaign.plugins.frondev;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CustomEntitySpecAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.lwjgl.util.vector.Vector2f;

public class SotfOutpostPlugin extends BaseCustomEntityPlugin {

	private SectorEntityToken entity;

	transient private SpriteAPI sprite;
	
	public void init(SectorEntityToken entity, Object pluginParams) {
		super.init(entity, pluginParams);
		this.entity = entity;
	}

	public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
		MarketAPI market = entity.getMarket();
		if (market != null) {
			Industry station = null;

			for (Industry industry : market.getIndustries()) {
				if (industry.getSpec().hasTag(Tags.STATION)) {
					station = industry;
				}
			}

			sprite = Global.getSettings().getSprite("stations", "station_side00");

			if (station != null) {
				switch(station.getId()) {
					case Industries.STARFORTRESS_HIGH:
						sprite = Global.getSettings().getSprite("sotf_stations", "starfortress_high");
						break;
					case Industries.STARFORTRESS_MID:
						sprite = Global.getSettings().getSprite("sotf_stations", "starfortress_mid");
						break;
					case Industries.STARFORTRESS:
						sprite = Global.getSettings().getSprite("sotf_stations", "starfortress");
						break;
					case Industries.BATTLESTATION_HIGH:
						sprite = Global.getSettings().getSprite("sotf_stations", "battlestation_high");
						break;
					case Industries.BATTLESTATION_MID:
						sprite = Global.getSettings().getSprite("sotf_stations", "battlestation_mid");
						break;
					case Industries.BATTLESTATION:
						sprite = Global.getSettings().getSprite("sotf_stations", "battlestation");
						break;
					case Industries.ORBITALSTATION_HIGH:
						sprite = Global.getSettings().getSprite("sotf_stations", "orbitalstation_high");
						break;
					case Industries.ORBITALSTATION_MID:
						sprite = Global.getSettings().getSprite("sotf_stations", "orbitalstation_mid");
						break;
					case Industries.ORBITALSTATION:
						sprite = Global.getSettings().getSprite("sotf_stations", "orbitalstation");
						break;
				}
			}
		}
		float alphaMult = viewport.getAlphaMult();
		Vector2f loc = entity.getLocation();
		CustomEntitySpecAPI spec = entity.getCustomEntitySpec();
		if (spec == null) return;

		float w = spec.getSpriteWidth();
		float h = spec.getSpriteHeight();

		sprite.setAngle(entity.getFacing() - 90f);
		sprite.setSize(w, h);
		sprite.setAlphaMult(alphaMult);
		sprite.setNormalBlend();
		sprite.renderAtCenter(loc.x, loc.y);
	}

}









