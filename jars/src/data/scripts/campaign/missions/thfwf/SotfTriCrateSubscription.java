package data.scripts.campaign.missions.thfwf;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MonthlyReport;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.Set;

/**
 *	Weapons subscription player can obtain during THFWF party scene
 */

public class SotfTriCrateSubscription extends BaseIntelPlugin implements EconomyTickListener {

	protected MarketAPI market;
	protected CargoAPI lastDelivery = null;

	public static final Object UPDATE_DELIVERED = new Object();
	public static final Object UPDATE_SHUT_DOWN = new Object();

	protected boolean wasShutDown = false;

	public SotfTriCrateSubscription(MarketAPI market) {
		this.market = market;
		Global.getSector().getListenerManager().addListener(this);
	}

	public boolean runWhilePaused() {
		return false;
	}

	public void reportEconomyMonthEnd() {
		if (isEnding() || isEnded()) return;

		float shutDownRoll = Misc.random.nextFloat();
		if (shutDownRoll < 0.01f) {
			wasShutDown = true;
			endImmediately();
			this.sendUpdateIfPlayerHasIntel(UPDATE_SHUT_DOWN, null);
			return;
		}

		//CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		MonthlyReport report = SharedData.getData().getCurrentReport();

		MonthlyReport.FDNode fleetNode = report.getNode(MonthlyReport.FLEET);
		float monthly = 1000f;
		MonthlyReport.FDNode costNode = report.getNode(fleetNode, "sotf_node_tricrate");

		costNode.upkeep += monthly;

		costNode.name = getName();
		costNode.icon = Global.getSector().getFaction(Factions.TRITACHYON).getLogo();
		costNode.tooltipCreator = new TooltipMakerAPI.TooltipCreator() {
			public boolean isTooltipExpandable(Object tooltipParam) {
				return true;
			}
			public float getTooltipWidth(Object tooltipParam) {
				return 450;
			}
			public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
				tooltip.addPara("Subscription fees paid for the TriCrate service for a monthly delivery of starship weapons.", 10f);
			}
		};

		CargoAPI delivery = Global.getFactory().createCargo(true);

		float random = Misc.random.nextFloat();
		if (random < 0.33f) {
			delivery.addWeapons("lightmg", 1);
			delivery.addWeapons("lightmortar", 1);
			delivery.addWeapons("lightac", 1);
		} else if (random < 0.66f) {
			delivery.addWeapons("sabot_single", 1);
			delivery.addWeapons("harpoon_single", 1);
			delivery.addWeapons("atropos_single", 1);
		} else if (random < 0.99f) {
			delivery.addWeapons("mininglaser", 3);
		} else {
			WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
			for (String weapon : Global.getSector().getFaction(Factions.TRITACHYON).getKnownWeapons()) {
				if (Global.getSettings().getWeaponSpec(weapon).getTier() > 1) {
					post.add(weapon);
				}
			}

			String actuallyDecentWeapon = post.pick();
			int numToGive = 3;
			if (Global.getSettings().getWeaponSpec(actuallyDecentWeapon).getTier() == 2) {
				numToGive = 5;
			}
			delivery.addWeapons(actuallyDecentWeapon, numToGive);
		}

		lastDelivery = delivery;

		Misc.getStorage(market).addAllCargo(delivery);

		this.sendUpdateIfPlayerHasIntel(UPDATE_DELIVERED, null);
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
		FactionAPI faction = Global.getSector().getFaction(Factions.TRITACHYON);

		float pad = 3f;
		float opad = 10f;
		
		addBulletPoints(info, ListInfoMode.IN_DESC);

		info.addImage(faction.getLogo(), width, 128, opad);

		info.addPara("You have signed up for an exciting venture: TriCrate, a service whereby an algorithmically-determined " +
				"set of weapons manufactured by Tri-Tachyon will be delivered to " + market.getName() + " for your retrieval.", opad);

		if (wasShutDown) {
			info.addPara("Unfortunately, TriCrate's project team was reassigned to another, more profitable venture and the service has been " +
					"permanently discontinued.", opad);
		}

		if (lastDelivery != null) {
			info.addPara("Last delivery: ", opad);
			info.showCargo(lastDelivery, 20, true, opad);
		}

		if (!isEnding() && !isEnded()) {
			ButtonAPI button = info.addButton("Cancel subscription", "sotf_endSubscription",
					getFactionForUIColors().getBaseUIColor(), getFactionForUIColors().getDarkUIColor(),
					(int)(width), 20f, opad * 2f);
			button.setShortcut(Keyboard.KEY_U, true);
		}
	}

	@Override
	public void createConfirmationPrompt(Object buttonId, TooltipMakerAPI prompt) {
		FactionAPI faction = getFactionForUIColors();

		if (buttonId == "sotf_endSubscription") {
			prompt.addPara("Really cancel your TriCrate subscription, and forever end your monthly deliveries of " +
							"high-quality Tri-Tachyon ship equipment? This cannot be undone!", 0f,
					Misc.getTextColor(), faction.getBaseUIColor(), faction.getDisplayNameWithArticleWithoutArticle());
		} else {
			super.createConfirmationPrompt(buttonId, prompt);
		}
	}

	@Override
	public boolean doesButtonHaveConfirmDialog(Object buttonId) {
		if (buttonId == "sotf_endSubscription") {
			return true;
		}
		return super.doesButtonHaveConfirmDialog(buttonId);
	}

	public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
		if (buttonId == "sotf_endSubscription") {
			endImmediately();
			super.buttonPressConfirmed(buttonId, ui);
		}
	}

	public String getIcon() {
		return Global.getSector().getFaction(Factions.TRITACHYON).getCrest();
	}

	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add(Global.getSector().getFaction(Factions.TRITACHYON).getId());
		return tags;
	}

	public String getBaseName() {
		return "TriCrate Subscription";
	}

	public String getName() {
		String postfix = "";
		if (listInfoParam == UPDATE_DELIVERED) {
			postfix = " - new delivery";
		}
		if (listInfoParam == UPDATE_SHUT_DOWN) {
			postfix = " - service shut down";
		}
		return getBaseName() + postfix;
	}
	
	@Override
	public FactionAPI getFactionForUIColors() {
		return Global.getSector().getFaction(Factions.TRITACHYON);
	}

	public String getSmallDescriptionTitle() {
		return getName();
	}

	@Override
	public String getCommMessageSound() {
		return getSoundMinorMessage();
	}
}







