package data.scripts.campaign.plugins.frondev;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.BaseCampaignObjectivePlugin;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.econ.SotfHabitatCondition;
import data.scripts.campaign.ids.SotfIDs;

public class SotfHabitatEntityPlugin extends BaseCampaignObjectivePlugin {

	
	public void init(SectorEntityToken entity, Object pluginParams) {
		super.init(entity, pluginParams);
		readResolve();
	}
	
	Object readResolve() {
		return this;
	}
	
	public void advance(float amount) {
		if (entity.getContainingLocation() == null || entity.isInHyperspace()) return;
		
		if (entity.getMemoryWithoutUpdate().getBoolean(MemFlags.OBJECTIVE_NON_FUNCTIONAL)) return;
		
		// everything else is handled by the habitat condition - it picks what habitat to use and when to remove itself
		for (MarketAPI market : Misc.getMarketsInLocation(entity.getContainingLocation())) {
            SotfHabitatCondition mc = SotfHabitatCondition.get(market);
			if (mc == null) {
				market.addCondition(SotfIDs.CONDITION_HABITAT);
				mc = SotfHabitatCondition.get(market);
			}
			if (mc != null) {
				mc.getHabitats().add(entity);
			}
		}
	}

	public void printNonFunctionalAndHackDescription(TextPanelAPI text) {
	}
	
	
	public void printEffect(TooltipMakerAPI text, float pad) {
		text.addPara(BaseIntelPlugin.INDENT + "%s hazard rating for same-faction colonies in system",
				pad, Misc.getHighlightColor(), "-8%");
	}

}