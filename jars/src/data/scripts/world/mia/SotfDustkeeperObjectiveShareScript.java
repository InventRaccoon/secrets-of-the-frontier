package data.scripts.world.mia;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.econ.CommRelayCondition;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.events.TriTachyonHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.group.TTMercenaryAttack;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.Nex_SpecialForcesCommands;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;

import java.util.List;

import static com.fs.starfarer.api.impl.campaign.econ.CommRelayCondition.*;

/**
 * Causes Dustkeepers to share in-system comm relay stability bonus with player colonies
 */

public class SotfDustkeeperObjectiveShareScript implements EveryFrameScript {

	public StarSystemAPI system;

	private IntervalUtil interval = new IntervalUtil(0.25f,0.35f);

	public SotfDustkeeperObjectiveShareScript(StarSystemAPI system) {
		this.system = system;
	}

	@Override
	public void advance(float amount) {
		interval.advance(amount);
		if (!interval.intervalElapsed()) return;
		List<CampaignFleetAPI> fleets = system.getFleets();
		for (CampaignFleetAPI fleet : Global.getSector().getHyperspace().getFleets()) {
			if (fleet.isInOrNearSystem(system)) {
				fleets.add(fleet);
			}
		}
		for (CampaignFleetAPI fleet : fleets) {
			boolean makeNonHostile = false;
			if (fleet.getMemoryWithoutUpdate().contains(MemFlags.MEMORY_KEY_TRADE_FLEET)) {
				makeNonHostile = true;
			} else if (fleet.getMemoryWithoutUpdate().contains(MemFlags.SHRINE_PILGRIM_FLEET)) {
				makeNonHostile = true;
			} else if (fleet.getMemoryWithoutUpdate().contains(MemFlags.MEMORY_KEY_SMUGGLER)) {
				makeNonHostile = true;
			} else if (fleet.getMemoryWithoutUpdate().contains("$nex_specialforces_npc") && !fleet.getFaction().isHostileTo(Factions.PLAYER)) {
				makeNonHostile = true;
			}
			if (!makeNonHostile) continue;
			fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_FACTION, SotfIDs.DUSTKEEPERS, 10f);
			fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_FACTION, SotfIDs.DUSTKEEPERS, 10f);
			Misc.makeNonHostileToFaction(fleet, SotfIDs.DUSTKEEPERS, 10f);
		}
		for (CampaignFleetAPI fleet : fleets) {
			boolean makeHostile = false;
			if (fleet.getMemoryWithoutUpdate().contains(TTMercenaryAttack.TTMA_FLEET)) {
				makeHostile = true;
			} else if (fleet.getMemoryWithoutUpdate().contains(TriTachyonHostileActivityFactor.COMMERCE_RAIDER_FLEET)) {
				makeHostile = true;
			}
			if (makeHostile) {
				Misc.makeHostileToFaction(fleet, SotfIDs.DUSTKEEPERS, 10f);
			}
		}
		// require Suspicious at least
		if (Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).isAtBest(Factions.PLAYER, RepLevel.INHOSPITABLE)) {
			// bcs if player breaks the beacon, Dustkeepers become hostile and the bonus is never unapplied
			for (MarketAPI market : Misc.getMarketsInLocation(system)) {
				market.getStability().unmodify("sotf_" + COMM_RELAY_MOD_ID + "_shared");
			}
			return;
		}
		// iterate through all markets in the system
		for (MarketAPI market : Misc.getMarketsInLocation(system)) {
			if (!market.getFactionId().equals(Factions.PLAYER)) return;
			//SharedData.getData().getMarketsWithoutTradeFleetSpawn().add(market.getId());
			CommRelayCondition mc = CommRelayCondition.get(market);
			if (mc == null) {
				market.addCondition(Conditions.COMM_RELAY);
				mc = CommRelayCondition.get(market);
			}
			if (mc != null) {
				// check what bonus we're currently getting from comm relay
				// in case e.g we have a makeshift somehow while Dustkeepers have a domain-era
				MutableStat.StatMod mod = market.getStability().getFlatStatMod(COMM_RELAY_MOD_ID);
				float playerBonus = 0f;
				if (mod != null) {
					playerBonus = mod.getValue();
				}
				boolean anyRelay = false;
				boolean domainEra = false;
				// figure out what stability bonus Dustkeepers are getting
				for (SectorEntityToken relay : mc.getRelays()) {
					if (relay.getMemoryWithoutUpdate().getBoolean(MemFlags.OBJECTIVE_NON_FUNCTIONAL)) {
						continue;
					}
					if (relay.getFaction().getId().equals(SotfIDs.DUSTKEEPERS)) {
						anyRelay = true;
						if (!relay.hasTag(Tags.MAKESHIFT)) {
							domainEra = true;
						}
					}
				}
				// player does not have Domain-era, Dustkeepers do, only +1 if player has makeshift already
				if (domainEra && playerBonus < 2f) {
					market.getStability().modifyFlat("sotf_" + COMM_RELAY_MOD_ID + "_shared", COMM_RELAY_BONUS - playerBonus, "Comm relay (shared)");
				}
				// Dustkeepers have makeshift, player has nothing
				else if (anyRelay && playerBonus < 1f) {
					market.getStability().modifyFlat("sotf_" + COMM_RELAY_MOD_ID + "_shared", MAKESHIFT_COMM_RELAY_BONUS, "Makeshift comm relay (shared)");
				}
				// DKs have no relay, purge bonus
				else {
					market.getStability().unmodify("sotf_" + COMM_RELAY_MOD_ID + "_shared");
				}
			}
		}
	}

	@Override
	public boolean isDone() {
		return system == null;
	}

	@Override
	public boolean runWhilePaused() {
		return true;
	}
}




