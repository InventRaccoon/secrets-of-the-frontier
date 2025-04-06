package data.scripts.campaign.missions.hallowhall;

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;

public class SotfHFHRestoreAstropolisFactor extends BaseOneTimeFactor {

	public SotfHFHRestoreAstropolisFactor(int points) {
		super(points);
		timestamp = 0; // makes it not expire
	}

	@Override
	public String getDesc(BaseEventIntel intel) {
		return "Restored astropolis";
	}

	@Override
	public TooltipCreator getMainRowTooltip(BaseEventIntel intel) {
		return new BaseFactorTooltip() {
			@Override
			public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
				tooltip.addPara("Trust gained by restoring Hallowhall's astropolis",
						0f);
			}
			
		};
	}
	
}
