package data.scripts.campaign.missions.hallowhall;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.HAShipsDestroyedFactor;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;

import java.awt.*;

public class SotfHFHShipsDestroyedFactorHint extends BaseOneTimeFactor {

	public SotfHFHShipsDestroyedFactorHint() {
		super(0);
		timestamp = 0; // makes it not expire
	}

	@Override
	public boolean shouldShow(BaseEventIntel intel) {
		return true;
//		for (EventFactor factor : intel.getFactors()) {
//			if (factor != this && factor instanceof HAShipsDestroyedFactor) {
//				return false;
//			}
//		}
//		return true;
	}

	@Override
	public String getDesc(BaseEventIntel intel) {
		return "Dustkeepers' sworn foes destroyed";
	}
	
	@Override
	public TooltipCreator getMainRowTooltip(BaseEventIntel intel) {
		return new BaseFactorTooltip() {
			@Override
			public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
				// bcs of PAGSM lol
				String diktatName = Global.getSector().getFaction(Factions.DIKTAT).getDisplayName();
				LabelAPI label = tooltip.addPara("Destroy the sworn enemies of the Dustkeeper Contingency anywhere in the sector. This includes:" +
								"\n- " + diktatName + " fleets (Lion's Guard count for double)" +
								"\n- Luddic Path fleets, and stations supporting Pather cells",
						0f);
				label.setHighlight(diktatName, "Lion's Guard", "Luddic Path", "Pather");
				Color sd = Global.getSector().getFaction(Factions.DIKTAT).getBaseUIColor();
				Color lp = Global.getSector().getFaction(Factions.LUDDIC_PATH).getBaseUIColor();
				label.setHighlightColors(sd, sd, lp, lp);
			}
		};
	}

}
