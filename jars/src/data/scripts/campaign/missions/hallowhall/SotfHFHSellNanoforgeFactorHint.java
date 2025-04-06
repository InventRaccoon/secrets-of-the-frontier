package data.scripts.campaign.missions.hallowhall;

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.HAShipsDestroyedFactor;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.util.Misc;

public class SotfHFHSellNanoforgeFactorHint extends BaseOneTimeFactor {

	public SotfHFHSellNanoforgeFactorHint() {
		super(0);
		timestamp = 0; // makes it not expire
	}

	@Override
	public boolean shouldShow(BaseEventIntel intel) {
		return !hasOtherFactorsOfClass(intel, SotfHFHSoldNanoforgeFactor.class);
	}

	@Override
	public String getDesc(BaseEventIntel intel) {
		return "Nanoforge sold";
	}
	
	@Override
	public TooltipCreator getMainRowTooltip(BaseEventIntel intel) {
		return new BaseFactorTooltip() {
			@Override
			public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
				tooltip.addPara("Sell an upgraded nanoforge to Holdout Forgeship's market. " +
								"Gain %s points of trust for a corrupted nanoforge, and %s points for a pristine nanoforge.",
						0f, Misc.getHighlightColor(), "" + Math.round(SotfHopeForHallowhallEventIntel.POINTS_FOR_NANOFORGE * 0.5f), "" + SotfHopeForHallowhallEventIntel.POINTS_FOR_NANOFORGE);
			}
		};
	}

}
