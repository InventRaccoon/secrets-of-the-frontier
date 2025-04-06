// prevents outpost stations from growing beyond size 4
package data.scripts.campaign.econ;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;


public class SotfCrampedCondition extends BaseMarketConditionPlugin implements MarketImmigrationModifier {

	public void apply(String id) {
		super.apply(id);

		market.addTransientImmigrationModifier(this);
	}

	public void unapply(String id) {
		super.unapply(id);

		market.removeTransientImmigrationModifier(this);
	}

	public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
		incoming.getWeight().modifyFlat(getModId(), getImmigrationBonus(), Misc.ucFirst(condition.getName().toLowerCase()));
	}

	protected float getImmigrationBonus() {
		if (market.getSize() >= 6) {
			return -40f;
		}
		else if (market.getSize() >= 5) {
			return -20f;
		}
		else if (market.getSize() >= 4) {
			return -10f;
		}
		else return 0f;
	}

	protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
		super.createTooltipAfterDescription(tooltip, expanded);

		tooltip.addPara("Population growth is reduced by 10 at size 4, with the penalty doubling at each colony size thereafter.",
				10f, Misc.getNegativeHighlightColor(),
				"10");
	}
}