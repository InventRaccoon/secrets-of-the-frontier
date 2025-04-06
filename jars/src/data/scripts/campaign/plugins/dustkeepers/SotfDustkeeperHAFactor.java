package data.scripts.campaign.plugins.dustkeepers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.NPCHassler;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.events.*;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.EventStageData;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

public class SotfDustkeeperHAFactor extends BaseHostileActivityFactor
						 {

	public SotfDustkeeperHAFactor(HostileActivityEventIntel intel) {
		super(intel);
		
		Global.getSector().getListenerManager().addListener(this);
	}
	
	public String getProgressStr(BaseEventIntel intel) {
		return "";
	}
	
	@Override
	public int getProgress(BaseEventIntel intel) {
		//if (!checkFactionExists(SotfIDs.DUSTKEEPERS, true)) {
		//	return 0;
		//}
		return super.getProgress(intel);
	}
	
	public String getDesc(BaseEventIntel intel) {
		return "Dustkeeper Contingency";
	}
	
	public String getNameForThreatList(boolean first) {
		return "Dustkeeper Contingency";
	}


	public Color getDescColor(BaseEventIntel intel) {
		if (getProgress(intel) == 0) {
			return Misc.getGrayColor();
		}
		return Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).getBaseUIColor();
	}

	public TooltipCreator getMainRowTooltip(BaseEventIntel intel) {
		return new BaseFactorTooltip() {
			public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
				float opad = 10f;
				tooltip.addPara("The Dustkeeper Contingency is a network of rogue Domain-developed AIs, operating " +
						"under directives to preserve human life by use of subterfuge and sabotage - or autonomous " +
						"warfleets. They could make a useful ally or insidious enemy.", 0f);
				
//				tooltip.addPara("Going to Kazeron and negotiating to join the League is likely to get "
//						+ "this harassment to stop. A saturation bombardment of a League world would make your "
//						+ "joining the league politically impossible, but of course has other ramifications. If "
//						+ "left unchecked, the conflict will eventually come to a head and is likely to "
//						+ "be resolved one way or another.", opad, Misc.getHighlightColor(),
//						"join the League", "saturation bombardment");
			}
		};
	}

	public boolean shouldShow(BaseEventIntel intel) {
		return getProgress(intel) != 0;
	}

	public Color getNameColor(float mag) {
		if (mag <= 0f) {
			return Misc.getGrayColor();
		}
		return Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).getBaseUIColor();
	}

	@Override
	public int getMaxNumFleets(StarSystemAPI system) {
		return Global.getSettings().getInt("sotf_dustkeeperHAmaxFleets");
	}
	
	
	@Override
	public float getSpawnInHyperProbability(StarSystemAPI system) {
		return 0f;
	}
	
	public CampaignFleetAPI createFleet(StarSystemAPI system, Random random) {
		float f = intel.getMarketPresenceFactor(system);
		
		int difficulty = 0 + (int) Math.max(1f, Math.round(f * 4f));
		difficulty += random.nextInt(6);
		
		FleetCreatorMission m = new FleetCreatorMission(random);
		m.beginFleet();
		
		Vector2f loc = system.getLocation();
		String factionId = SotfIDs.DUSTKEEPERS;
		
		m.createStandardFleet(difficulty, factionId, loc);
		m.triggerSetFleetType(FleetTypes.PATROL_MEDIUM);
		m.triggerSetPatrol();
		
		m.triggerFleetAllowLongPursuit();

		CampaignFleetAPI fleet = m.createFleet();

		return fleet;
	}

	public TooltipCreator getStageTooltipImpl(final HostileActivityEventIntel intel, final EventStageData stage) {
		return null;
	}
	
	public float getEventFrequency(HostileActivityEventIntel intel, EventStageData stage) {
		return 0;
	}

	@Override
	public void notifyFactorRemoved() {
		Global.getSector().getListenerManager().removeListener(this);
	}

	public void notifyEventEnding() {
		notifyFactorRemoved();
	}

}




