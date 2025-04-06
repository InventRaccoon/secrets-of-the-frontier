package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.CampaignObjective;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.misc.CommSnifferIntel;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.missions.hallowhall.SotfHopeForHallowhallEventIntel;
import data.scripts.world.mia.SotfPersonalFleetSeraph;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import static com.fs.starfarer.api.impl.campaign.rulecmd.salvage.Objectives.SALVAGE_FRACTION;

public class SotfDKObj extends BaseCommandPlugin {

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null) return false;
		TextPanelAPI text = dialog.getTextPanel();

		String cmd = null;

		cmd = params.get(0).getString(memoryMap);

		switch (cmd) {
			case "okToTakeObj":
				return okToTakeOverObjective(dialog.getInteractionTarget());
			case "salvageFriendly":
				salvage(dialog.getInteractionTarget().getFaction().getId(), dialog);
				return true;
			case "controlFriendly":
				control(Factions.PLAYER, dialog);
				return true;
			default:
				return true;
		}
	}

	/**
	 * Below: a bunch of code from vanilla Objectives rulecmd that needs to be copied over
	 * for Dustkeepers allowing their stuff to be taken over
	 */

	protected boolean okToTakeOverObjective(SectorEntityToken entity) {
		for (MarketAPI curr : Misc.getMarketsInLocation(entity.getContainingLocation(), Factions.PLAYER)) {
			if (!curr.isHidden() && curr.getSize() > 3) {
				return true;
			}
		}
		return false;
	}

	public void control(String factionId, InteractionDialogAPI dialog) {
		SectorEntityToken entity = dialog.getInteractionTarget();

		FactionAPI prev = entity.getFaction();
		entity.setFaction(factionId);
		FactionAPI faction = entity.getFaction();

		if (!entity.hasTag(Tags.COMM_RELAY) && faction.isPlayerFaction()) {
			unhack(entity);
		}

		entity.getMemoryWithoutUpdate().unset(MemFlags.OBJECTIVE_NON_FUNCTIONAL);

		if (dialog != null) {
			((RuleBasedDialog) dialog.getPlugin()).updateMemory();

			printOwner(entity, dialog);
		}


		ListenerUtil.reportObjectiveChangedHands(entity, prev, faction);
	}

	public void unhack(SectorEntityToken entity) {
		CommSnifferIntel intel = CommSnifferIntel.getExistingSnifferIntelForRelay(entity);
		if (intel != null) {
			intel.uninstall();
		} else {
			CustomCampaignEntityPlugin plugin = entity.getCustomPlugin();
			if (plugin instanceof CampaignObjective) {
				CampaignObjective o = (CampaignObjective) plugin;
				o.setHacked(false);
			}
		}
	}

	public void printOwner(SectorEntityToken entity, InteractionDialogAPI dialog) {
		dialog.getTextPanel().addPara("This " + entity.getCustomEntitySpec().getShortName() + " is under your control.",
				entity.getFaction().getBaseUIColor(), "your");
	}

	public void salvage(final String factionId, final InteractionDialogAPI dialog) {
		final SectorEntityToken entity = dialog.getInteractionTarget();
		CargoAPI salvage = Global.getFactory().createCargo(true);
		String [] r = getResources(entity);
		int [] q = getSalvageQuantities(entity);

		for (int i = 0; i < r.length; i++) {
			salvage.addCommodity(r[i], q[i]);
		}

		dialog.getVisualPanel().showLoot("Salvaged", salvage, false, true, true, new CoreInteractionListener() {
			public void coreUIDismissed() {
				dialog.dismiss();
				dialog.hideTextPanel();
				dialog.hideVisualPanel();

				LocationAPI loc = entity.getContainingLocation();
				SectorEntityToken built = loc.addCustomEntity(null,
						null,
						Entities.STABLE_LOCATION, // type of object, defined in custom_entities.json
						Factions.NEUTRAL); // faction
				if (entity.getOrbit() != null) {
					built.setOrbit(entity.getOrbit().makeCopy());
				}
				loc.removeEntity(entity);
				updateOrbitingEntities(loc, entity, built);

				built.getMemoryWithoutUpdate().set(MemFlags.RECENTLY_SALVAGED, true, 30f);

				ListenerUtil.reportObjectiveDestroyed(entity, built, Global.getSector().getFaction(factionId));
			}
		});
		dialog.getOptionPanel().clearOptions();
		dialog.setPromptText("");

	}

	public void updateOrbitingEntities(LocationAPI loc, SectorEntityToken prev, SectorEntityToken built) {
		if (loc == null) return;
		for (SectorEntityToken other : loc.getAllEntities()) {
			if (other == prev) continue;
			if (other.getOrbit() == null) continue;
			if (other.getOrbitFocus() == prev) {
				other.setOrbitFocus(built);
			}
		}
	}

	public String [] getResources(SectorEntityToken entity) {
		if (entity.hasTag(Tags.MAKESHIFT) || entity.hasTag(Tags.STABLE_LOCATION)) {
			return new String[] {Commodities.HEAVY_MACHINERY, Commodities.METALS, Commodities.RARE_METALS};
		}
		return new String[] {Commodities.HEAVY_MACHINERY, Commodities.METALS, Commodities.RARE_METALS, Commodities.VOLATILES};
	}

	public int [] getQuantities(SectorEntityToken entity) {
		if (entity.hasTag(Tags.MAKESHIFT) || entity.hasTag(Tags.STABLE_LOCATION)) {
			return new int[] {15, 30, 5};
		}
		return new int[] {50, 200, 20, 20};
	}

	public int [] getSalvageQuantities(SectorEntityToken entity) {
		int [] q = getQuantities(entity);
		int [] result = new int [q.length];

		for (int i = 0; i < result.length; i++) {
			result[i] = (int) (q[i] * SALVAGE_FRACTION);
		}
		return result;
	}

}















