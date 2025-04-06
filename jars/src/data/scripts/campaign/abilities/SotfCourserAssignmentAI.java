// AI given to Courser when summoned by Courser Protocol. Fairly simple - escort the player for 45 days, then leave
package data.scripts.campaign.abilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseAssignmentAI;
import com.fs.starfarer.api.loading.AbilitySpecAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import org.lwjgl.util.vector.Vector2f;

public class SotfCourserAssignmentAI extends BaseAssignmentAI implements FleetEventListener {

	protected StarSystemAPI system;

	public static final float ESCORT_DURATION = 45f;
	public static final float ORDER_DURATION = 10f;

	protected float escortTimeLeft;

	protected float jumpCooldown = 2f;

	protected final JumpPointAPI inner;
	protected final JumpPointAPI outer;

	public SotfCourserAssignmentAI(CampaignFleetAPI fleet, StarSystemAPI system, JumpPointAPI inner, JumpPointAPI outer) {
		super();
		this.fleet = fleet;
		this.system = system;
		this.inner = inner;
		this.outer = outer;

		escortTimeLeft = ESCORT_DURATION;
		fleet.addEventListener(this);
		giveInitialAssignments();
	}

	public void advance(float amount) {
		if (Global.getSector().isPaused()) return;
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

		jumpCooldown -= Global.getSector().getClock().convertToDays(amount);
		escortTimeLeft -= Global.getSector().getClock().convertToDays(amount);

		if (escortTimeLeft <= 0) {
			pickNext();
			return;
		}
		if (playerFleet != null) {
			// use TJ to follow into hyperspace
			// syncing up with player
			if (
					(
						(!playerFleet.isInHyperspace() && playerFleet.isInHyperspaceTransition() && fleet.getContainingLocation() == playerFleet.getContainingLocation()) ||
						(!playerFleet.isInHyperspace() && playerFleet.hasAbility(Abilities.TRANSVERSE_JUMP) && playerFleet.getAbility(Abilities.TRANSVERSE_JUMP).isActiveOrInProgress())
						)
							&& jumpCooldown < 0 && !fleet.isInHyperspaceTransition()) {

				//Vector2f loc = Misc.getPointAtRadius(playerFleet.getLocationInHyperspace(), 300f + fleet.getRadius());
				//SectorEntityToken token = Global.getSector().getHyperspace().createToken(loc.x, loc.y);
				//JumpPointAPI.JumpDestination dest = new JumpPointAPI.JumpDestination(token, null);
				//Global.getSector().doHyperspaceTransition(fleet, null, dest);
				//if (fleet.isVisibleToPlayerFleet()) {
				//	AbilitySpecAPI transverseJump = Global.getSettings().getAbilitySpec(Abilities.TRANSVERSE_JUMP);
				//	fleet.addFloatingText(transverseJump.getName(), Misc.setAlpha(fleet.getIndicatorColor(), 255), 0.5f);
				//	Global.getSoundPlayer().playSound(transverseJump.getWorldOn(), 1f, 1f, fleet.getLocation(), fleet.getVelocity());
				//}
				fleet.getAbility(Abilities.TRANSVERSE_JUMP).activate();

				jumpCooldown = 2f;
			}
			// in hyper, player isn't - warp in alongside them
			else if (fleet.isInHyperspace() && !playerFleet.isInHyperspace() && !fleet.isInHyperspaceTransition() && jumpCooldown < 0) {

				Vector2f loc  = Misc.getPointAtRadius(playerFleet.getLocation(), 300f + fleet.getRadius());
				SectorEntityToken token = playerFleet.getContainingLocation().createToken(loc.x, loc.y);
				JumpPointAPI.JumpDestination dest = new JumpPointAPI.JumpDestination(token, null);
				Global.getSector().doHyperspaceTransition(fleet, null, dest);
				//if (fleet.isVisibleToPlayerFleet()) {
				//	AbilitySpecAPI transverseJump = Global.getSettings().getAbilitySpec(Abilities.TRANSVERSE_JUMP);
				//	fleet.addFloatingText(transverseJump.getName(), Misc.setAlpha(fleet.getIndicatorColor(), 255), 0.5f);
				//	Global.getSoundPlayer().playSound(transverseJump.getWorldOn(), 1f, 1f, fleet.getLocation(), fleet.getVelocity());
				//}

				jumpCooldown = 2f;
			}
			// attack player if they are an enemy to the Dustkeepers
			else if (Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).isAtBest(Factions.PLAYER, RepLevel.HOSTILE)) {
				fleet.clearAssignments();
				fleet.addAssignmentAtStart(FleetAssignment.INTERCEPT, Global.getSector().getPlayerFleet(), ORDER_DURATION, "hunting your fleet", null);
			}
			// if player has clicked on Courser, make him stop and let them interact with him
			else if (playerFleet.getInteractionTarget() != null && playerFleet.getInteractionTarget().equals(fleet)) {
				fleet.clearAssignments();
				fleet.addAssignmentAtStart(FleetAssignment.HOLD, Global.getSector().getPlayerFleet(), ORDER_DURATION, "waiting for your fleet", null);
			}
			// otherwise follow them around
			else {
				fleet.clearAssignments();
				fleet.addAssignmentAtStart(FleetAssignment.ORBIT_AGGRESSIVE, Global.getSector().getPlayerFleet(), ORDER_DURATION, "escorting your fleet", null);
			}
 		}
	}

	// tracks if player won the fight
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		if (isDone()) return;

		if (!battle.isPlayerInvolved() || !battle.isOnPlayerSide(fleet)) {
			return;
		}
		float totalFPKilledTogether = 0f;
		for (CampaignFleetAPI otherFleet : battle.getNonPlayerSideSnapshot()) {
			if (!otherFleet.getFaction().isHostileTo(SotfIDs.DUSTKEEPERS)) continue;
			for (FleetMemberAPI loss : Misc.getSnapshotMembersLost(otherFleet)) {
				totalFPKilledTogether += loss.getFleetPointCost();
			}
		}

		Global.getSector().adjustPlayerReputation(
				new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.COMMISSION_BOUNTY_REWARD, totalFPKilledTogether * 0.5f * battle.getPlayerInvolvementFraction(),
						null, null, false, true, "Change caused by assisting Annex-Courser to further the Cause"),
				SotfIDs.DUSTKEEPERS);
		Global.getSector().adjustPlayerReputation(
				new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.COMMISSION_BOUNTY_REWARD, totalFPKilledTogether * 1.5f * battle.getPlayerInvolvementFraction(), null, null, false, false),
				SotfPeople.getPerson(SotfPeople.COURSER));
	}

	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {

	}

	@Override
	protected void giveInitialAssignments() {
		if (Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).isAtBest(Factions.PLAYER, RepLevel.HOSTILE)) {
			fleet.addAssignmentAtStart(FleetAssignment.INTERCEPT, Global.getSector().getPlayerFleet(), ORDER_DURATION, "hunting your fleet", null);
		} else {
			fleet.addAssignmentAtStart(FleetAssignment.ORBIT_AGGRESSIVE, Global.getSector().getPlayerFleet(), ORDER_DURATION, "escorting your fleet", null);
		}
	}

	@Override
	protected void pickNext() {
		//MemoryAPI memory = fleet.getMemoryWithoutUpdate();
		//Misc.setFlagWithReason(fleet.getMemoryWithoutUpdate(), MemFlags.ENTITY_MISSION_IMPORTANT,
    	//		   			   "$sotf_courserFleet", false, 1000f);
		fleet.clearAssignments();
		Misc.giveStandardReturnToSourceAssignments(fleet);
		fleet.removeScript(this);
	}

}












