// wait 1 day then show a dialogue
package data.scripts.dialog;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;

public class SotfWSDialogScript implements EveryFrameScript, FleetEventListener {

	private float counter = 0;

	public void advance(float amount) {
		if (Global.getSector().isPaused()) {return;}
		counter += amount;

		if (counter < 1) {return;}

		if (Global.getSector().getMemoryWithoutUpdate().contains("$sotf_WSCombatWon")) {
			Global.getSector().getCampaignUI().showInteractionDialog(new SotfWSWinPlugin(), null);
		} else if (Global.getSector().getMemoryWithoutUpdate().contains("$sotf_WSCombatStarted")) {
			Global.getSector().getCampaignUI().showInteractionDialog(new SotfWSLosePlugin(), null);
		}
		Global.getSector().removeScript(this);
	}

	// tracks if player won the fight
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		if (isDone()) return;

		if (!battle.isPlayerInvolved() || primaryWinner != fleet) {
			return;
		}

		if (primaryWinner == Global.getSector().getPlayerFleet()) {
			Global.getSector().getMemoryWithoutUpdate().set("$sotf_WSCombatWon", true);
		}
		// stop the music
		Global.getSoundPlayer().pauseCustomMusic();
		Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false);
	}

	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {

	}

	public boolean isDone() {
		return false;
	}

	public boolean runWhilePaused() {
		return false;
	}

}