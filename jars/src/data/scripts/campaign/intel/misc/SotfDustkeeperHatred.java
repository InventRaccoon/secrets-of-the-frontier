package data.scripts.campaign.intel.misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MonthlyReport;
import com.fs.starfarer.api.campaign.listeners.ColonyPlayerHostileActListener;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.plugins.fel.SotfGuiltTracker;
import data.scripts.utils.SotfMisc;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.Set;

/**
 *	Tracks atrocities for Dustkeeper hatred
 */

public class SotfDustkeeperHatred extends BaseIntelPlugin implements ColonyPlayerHostileActListener, EconomyTickListener {

	public static final Object UPDATE_NEW_HATRED = new Object();

	public SotfDustkeeperHatred() {
		Global.getSector().getListenerManager().addListener(this);
	}

	public boolean runWhilePaused() {
		return false;
	}

	protected void advanceImpl(float amount) {

	}

	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {

	}

	public void reportRaidForValuablesFinishedBeforeCargoShown(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData, CargoAPI cargo) {
		//
	}
	public void reportRaidToDisruptFinished(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData, Industry industry) {
		//
	}

	public void reportTacticalBombardmentFinished(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData) {
		//
	}
	public void reportSaturationBombardmentFinished(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData) {
		if (SotfMisc.getDustkeepersNoSatbombConsequences()) return;
		if (SotfGuiltTracker.GUILTY_FACTIONS.contains(market.getFactionId()) || market.getSize() < 4) {
			return;
		}
		if (Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_DUSTKEEPER_HATRED)) {
			return;
		}
		Global.getSector().getMemoryWithoutUpdate().set("$sotf_dustkeeperHatredCause", market.getName());
		// congrats, you are now a servant of ruin!
		Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_DUSTKEEPER_HATRED, true);
		// your reward: eternal hatred
		Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).setRelationship(Factions.PLAYER, -1f);
		this.sendUpdateIfPlayerHasIntel(UPDATE_NEW_HATRED, null);
		//Global.getSector().getCampaignUI().addMessage("The Dustkeeper Contingency is now permanently Vengeful", Misc.getNegativeHighlightColor(), "Dustkeeper Contingency", "", Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).getColor(), Color.BLACK);
	}

	public void reportEconomyMonthEnd() {
		if (!Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_DUSTKEEPER_HATRED)) {
			return;
		}
		if (Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).getRelationship(Factions.PLAYER) > -0.9f) {
			CoreReputationPlugin.CustomRepImpact impact = new CoreReputationPlugin.CustomRepImpact();
			impact.delta = -1f; // straight back to -100
			impact.limit = RepLevel.VENGEFUL;
			Global.getSector().adjustPlayerReputation(
					new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.CUSTOM, impact,
							null, null, false, true, "Change caused by a previously committed atrocity"),
					SotfIDs.DUSTKEEPERS);
		}
	}

	public void reportEconomyTick(int iterIndex) {
	}

	protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode) {
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		float pad = 3f;
		float opad = 10f;
		
		float initPad = pad;
		if (mode == ListInfoMode.IN_DESC) initPad = opad;
		
		Color tc = getBulletColorForMode(mode);
		
		bullet(info);
		boolean isUpdate = getListInfoParam() != null;
		
		unindent(info);
	}
	
	
	@Override
	public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
		Color c = getTitleColor(mode);
		info.addPara(getName(), c, 0f);
		addBulletPoints(info, mode);
	}
	
	@Override
	public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color tc = Misc.getTextColor();
		FactionAPI faction = Global.getSector().getFaction(SotfIDs.DUSTKEEPERS);
		String marketName = (String) Global.getSector().getMemoryWithoutUpdate().get("$sotf_dustkeeperHatredCause");

		float pad = 3f;
		float opad = 10f;
		
		addBulletPoints(info, ListInfoMode.IN_DESC);

		info.addImage(faction.getLogo(), width, 128, opad);

		if (marketName != null) {
			LabelAPI label = info.addPara("Your recent saturation bombardment of " + marketName + " has severely angered the Dustkeeper Contingency.", opad);
			label.setHighlight(marketName, "Dustkeeper Contingency");
			label.setHighlightColors(Misc.getHighlightColor(), faction.getColor());
		}
		// below text should never display
		else {
			LabelAPI label = info.addPara("Your recent saturation bombardment has severely angered the Dustkeeper Contingency.", opad);
			label.setHighlight("Dustkeeper Contingency");
			label.setHighlightColors(faction.getColor());
		}
		info.addPara("Their ongoing mission to preserve human life in the Persean Sector has resulted in you being designated as an unforgivable enemy. " +
				"The severity of your actions has likely ruled out any hope for reconciliation.", opad);
		info.addSectionHeading("Consequences", getFactionForUIColors().getBaseUIColor(),
				getFactionForUIColors().getDarkUIColor(), Alignment.MID, opad);
		info.addPara("Your reputation with the Contingency will be set to Vengeful at the end of every month.", opad);
		info.addPara("An extreme mutiny risk is projected for all Dustkeeper warminds should they be deployed in combat, and immediate disconnection is advised.", opad);
	}

	public String getIcon() {
		return Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).getCrest();
	}
	
	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add(Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).getId());
		return tags;
	}
	
	public String getName() {
		String str = "Dustkeeper Hatred";
		if (listInfoParam == UPDATE_NEW_HATRED) {
			str += " - now permanently vengeful";
		}
		return str;
	}
	
	@Override
	public FactionAPI getFactionForUIColors() {
		return Global.getSector().getFaction(SotfIDs.DUSTKEEPERS);
	}

	public String getSmallDescriptionTitle() {
		return getName();
	}
	
	@Override
	public boolean shouldRemoveIntel() {
		return false;
	}

	// don't show unless Dustkeepers are perma-Vengeful
	@Override
	public boolean isHidden() {
		return !Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_DUSTKEEPER_HATRED);
	}

	@Override
	public String getCommMessageSound() {
		return getSoundMajorPosting();
	}
}







