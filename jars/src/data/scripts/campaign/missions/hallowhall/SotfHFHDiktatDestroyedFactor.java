package data.scripts.campaign.missions.hallowhall;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;

public class SotfHFHDiktatDestroyedFactor extends BaseOneTimeFactor {

	public SotfHFHDiktatDestroyedFactor(int points) {
		super(points);
	}

	@Override
	public String getDesc(BaseEventIntel intel) {
		// bcs of PAGSM lol
		String diktatName = Global.getSector().getFaction(Factions.DIKTAT).getDisplayName();
		return diktatName + " ships destroyed";
	}

	@Override
	public TooltipCreator getMainRowTooltip(BaseEventIntel intel) {
		return new BaseFactorTooltip() {
			@Override
			public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
				// bcs of PAGSM lol
				String diktatName = Global.getSector().getFaction(Factions.DIKTAT).getDisplayName();
				tooltip.addPara("Ship belonging to the " + diktatName + " destroyed by your fleet. Lion's Guard build twice as much trust.",
						0f);
			}
			
		};
	}
	
}
