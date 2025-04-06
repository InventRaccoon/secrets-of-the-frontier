package data.scripts.campaign.missions.hallowhall;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.intel.events.*;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.MapParams;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;

import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;

public class SotfHFHColonyHACause extends BaseHostileActivityCause2 {

	public static float MAX_MAG = 0.5f;

	public SotfHFHColonyHACause(HostileActivityEventIntel intel) {
		super(intel);
	}

	@Override
	public TooltipCreator getTooltip() {
		return new BaseFactorTooltip() {
			public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
				MarketAPI holdout = Global.getSector().getEconomy().getMarket("sotf_holdout_market");
				tooltip.addPara("You have a colony in the Mia's Star system, where Holdout Forgeship is " +
								"operating droneship patrols.",
						0f, holdout.getTextColorForFactionOrPlanet(), "Holdout Forgeship");
				if (Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).isAtBest(Factions.PLAYER, RepLevel.HOSTILE)) {
					tooltip.addPara("Your hostility to the Dustkeeper Contingency renders this a " +
									"major security concern, with colony fleets subject to attack.",
							10f);
				} else {
					tooltip.addPara("You can expect Holdout's warfleets to bolster the colony's defenses, dissuading would-be threats " +
									"and attacking any fleets hostile to them.",
							10f);
					tooltip.addPara("Drone armadas will deduct more progress if Holdout's nanoforge is upgraded.",
							10f);
				}
			}
		};
	}

	@Override
	public boolean shouldShow() {
		return getProgress() != 0 && Global.getSector().getEconomy().getMarket("sotf_holdout_market") != null;
	}

	public int getProgress() {
		int points = 0;
		for (StarSystemAPI system : Misc.getPlayerSystems(false)) {
			if (!system.getName().contains("Mia's Star")) continue;
			MarketAPI holdout = Global.getSector().getEconomy().getMarket("sotf_holdout_market");
			if (holdout != null && holdout.getIndustry(Industries.HEAVYINDUSTRY) != null) {
				points -= 5;
				if (holdout.getIndustry(Industries.HEAVYINDUSTRY).getSpecialItem() != null) {
					points -= 3;
					if (holdout.getIndustry(Industries.HEAVYINDUSTRY).getSpecialItem().getId().equals(Items.PRISTINE_NANOFORGE)) {
						points -= 5;
					}
				}
			}
 		}
		if (Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).isAtBest(Factions.PLAYER, RepLevel.HOSTILE)) {
			points *= -1;
		}
		return points;
	}

	public String getDesc() {
		return "Holdout Forgeship defensive measures";
	}

	public float getMagnitudeContribution(StarSystemAPI system) {
		if (!system.getId().equals("sotf_mia")) return 0f;
		return MAX_MAG;
	}
	

}


