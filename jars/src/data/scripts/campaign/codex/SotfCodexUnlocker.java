package data.scripts.campaign.codex;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.econ.*;
import com.fs.starfarer.api.campaign.listeners.CargoScreenListener;
import com.fs.starfarer.api.campaign.listeners.CodexEventListener;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.SharedUnlockData;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.rulecmd.SotfHasOfficer;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * For the codex entries that require a listener of some type to unlock.
 *
 */
public class SotfCodexUnlocker implements FleetEventListener, CodexEventListener, CargoScreenListener {

	@Override
	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
	}

	@Override
	public void reportBattleOccurred(CampaignFleetAPI nullHere, CampaignFleetAPI primaryWinner, BattleAPI battle) {

	}

	@Override
	public void reportCargoScreenOpened() {
		unlockStuff();
	}
	
	@Override
	public void reportAboutToOpenCodex() {
		unlockStuff();
	}
	
	public void unlockStuff() {
		if (SharedUnlockData.get().isPlayerAwareOfHullmod(SotfIDs.HULLMOD_CWARSUITE)) {
			SharedUnlockData.get().reportPlayerAwareOfSkill(SotfIDs.SKILL_CYBERWARFARE, true);
		}
		if (SharedUnlockData.get().isPlayerAwareOfWeapon("sotf_barbmount") || SharedUnlockData.get().isPlayerAwareOfWeapon("sotf_barbrail")) {
			SharedUnlockData.get().reportPlayerAwareOfFighter("sotf_barb_wing", true);
		}
		if (SharedUnlockData.get().isPlayerAwareOfWeapon("sotf_nettlerail")) {
			SharedUnlockData.get().reportPlayerAwareOfFighter("sotf_nettle_wing", true);
		}

		if (Global.getCurrentState() != GameState.CAMPAIGN) return;
		
		CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
		if (fleet == null || fleet.getFleetData() == null) return;
		CargoAPI cargo = fleet.getCargo();
		if (cargo == null) return;
		
		boolean save = false;

		if (Global.getSector().getCharacterData().knowsHullMod(SotfIDs.HULLMOD_DAYDREAM_SYNTHESIZER) ||
				Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_COTL_START)) {
			save |= SharedUnlockData.get().reportPlayerAwareOfSkill(SotfIDs.SKILL_ADVANCEDCOUNTERMEASURES, false);
			save |= SharedUnlockData.get().reportPlayerAwareOfSkill(SotfIDs.SKILL_FIELDSRESONANCE, false);
			save |= SharedUnlockData.get().reportPlayerAwareOfSkill(SotfIDs.SKILL_GUNNERYUPLINK, false);
			save |= SharedUnlockData.get().reportPlayerAwareOfSkill(SotfIDs.SKILL_MISSILEREPLICATION, false);
			save |= SharedUnlockData.get().reportPlayerAwareOfSkill(SotfIDs.SKILL_ORDNANCEMASTERY, false);
			save |= SharedUnlockData.get().reportPlayerAwareOfSkill(SotfIDs.SKILL_POLARIZEDNANOREPAIR, false);
			save |= SharedUnlockData.get().reportPlayerAwareOfSkill(SotfIDs.SKILL_SPATIALEXPERTISE, false);
		}

		if (SotfHasOfficer.haveOfficer(SotfPeople.NIGHTINGALE) || SotfHasOfficer.haveOfficer(SotfPeople.SERAPH)) {
			save |= SharedUnlockData.get().reportPlayerAwareOfSkill(SotfIDs.SKILL_CYBERWARFARE, false);
		}

		if (SotfHasOfficer.haveOfficer(SotfPeople.BARROW)) {
			save |= SharedUnlockData.get().reportPlayerAwareOfSkill(SotfIDs.SKILL_DERELICTCONTINGENTP, false);
		}

		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			if (member.getVariant().hasHullMod(SotfIDs.HULLMOD_CWARSUITE)) {
				save |= SharedUnlockData.get().reportPlayerAwareOfSkill(SotfIDs.SKILL_CYBERWARFARE, false);
			}
		}
		
		if (save) SharedUnlockData.get().saveIfNeeded();
	}
	
	
	
	@Override
	public void reportClosedCodex() {
		
	}

	@Override
	public void reportPlayerLeftCargoPods(SectorEntityToken entity) {}
	@Override
	public void reportPlayerNonMarketTransaction(PlayerMarketTransaction transaction, InteractionDialogAPI dialog) {}
	@Override
	public void reportSubmarketOpened(SubmarketAPI submarket) {}
	
	
	
	

}
