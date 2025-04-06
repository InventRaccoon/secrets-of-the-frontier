package data.scripts.campaign.missions.hallowhall;

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.util.Misc;

public class SotfHFHInadCoresFactor extends BaseOneTimeFactor {

	public SotfHFHInadCoresFactor(int points) {
		super(points);
	}

	@Override
	public String getDesc(BaseEventIntel intel) {
		return "AI cores handed into Echo-Inadvertent";
	}

	@Override
	public TooltipCreator getMainRowTooltip(BaseEventIntel intel) {
		final SotfHopeForHallowhallEventIntel hh = (SotfHopeForHallowhallEventIntel) intel;
		return new BaseFactorTooltip() {
			@Override
			public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
				tooltip.addPara("Trust gained by giving useful AI cores to Rigging-Echo-Inadvertent at Holdout Forgeship.",
						0f);
				tooltip.addPara("You have gained %s points of trust from giving AI cores, out " +
								"of the maximum of %s.",
						0f, Misc.getHighlightColor(), "" + hh.pointsFromCores, "" + SotfHopeForHallowhallEventIntel.MAX_POINTS_FROM_CORES);
			}
			
		};
	}
	
}
