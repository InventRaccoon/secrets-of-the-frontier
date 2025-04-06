package data.scripts.campaign.missions.hallowhall;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;

public class SotfHFHSoldNanoforgeFactor extends BaseOneTimeFactor {

	public String commodityId;

	public SotfHFHSoldNanoforgeFactor(String commodityId, int points) {
		super(points);
		this.commodityId = commodityId;
		timestamp = 0; // makes it not expire
	}

	@Override
	public String getDesc(BaseEventIntel intel) {
		return "Sold " + Global.getSettings().getSpecialItemSpec(commodityId).getName().toLowerCase();
	}

	@Override
	public TooltipCreator getMainRowTooltip(BaseEventIntel intel) {
		return new BaseFactorTooltip() {
			@Override
			public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
				tooltip.addPara("Trust gained by selling a " + Global.getSettings().getSpecialItemSpec(commodityId).getName().toLowerCase() + " to Holdout Forgeship.",
						0f);
			}
			
		};
	}
	
}
