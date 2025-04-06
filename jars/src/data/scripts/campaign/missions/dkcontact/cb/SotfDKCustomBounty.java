package data.scripts.campaign.missions.dkcontact.cb;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.cb.*;
import data.scripts.campaign.ids.SotfPeople;

import java.util.ArrayList;
import java.util.List;

/**
 *	DUSTKEEPER FLEET BOUNTIES (by them, and usually not of them)
 */

public class SotfDKCustomBounty extends BaseCustomBounty {

	public static List<CustomBountyCreator> CREATORS = new ArrayList<CustomBountyCreator>();
	static {
		CREATORS.add(new SotfDKCBPirate());
		CREATORS.add(new SotfDKCBPather());
		CREATORS.add(new SotfDKCBBurnout());
		CREATORS.add(new SotfDKCBDerelict());
		CREATORS.add(new SotfDKCBDeserter());
		CREATORS.add(new SotfDKCBRemnant());
		CREATORS.add(new SotfDKCBRemnantPlus());
		CREATORS.add(new SotfDKCBProjectSiren());
	}
	
	@Override
	public List<CustomBountyCreator> getCreators() {
		return CREATORS;
	}

	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		if (barEvent) {
			createBarGiver(createdAt);
		}

		PersonAPI person = getPerson();
		if (person == null) return false;

		String id = getMissionId();
		if (!setPersonMissionRef(person, "$" + id + "_ref")) {
			return false;
		}

		setStartingStage(Stage.BOUNTY);
		setSuccessStage(Stage.COMPLETED);
		setFailureStage(Stage.FAILED);
		addNoPenaltyFailureStages(Stage.FAILED_NO_PENALTY);
		//setNoAbandon();


		connectWithMemoryFlag(Stage.BOUNTY, Stage.COMPLETED, person, "$" + id + "_completed");
		connectWithMemoryFlag(Stage.BOUNTY, Stage.FAILED, person, "$" + id + "_failed");

		addTag(Tags.INTEL_BOUNTY);

		DifficultyChoice lowDifficulty = DifficultyChoice.LOW;

		// Wendigo gives 2 medium bounties and 1 hard bounty
		if (getPerson().getId().equals(SotfPeople.WENDIGO)) {
			lowDifficulty = DifficultyChoice.NORMAL;
		}

		int dLow = pickDifficulty(lowDifficulty);
		creatorLow = pickCreator(dLow, lowDifficulty);
		if (creatorLow != null) {
			dataLow = creatorLow.createBounty(createdAt, this, dLow, Stage.BOUNTY);
		}
		if (dataLow == null || dataLow.fleet == null) return false;

		int dNormal = pickDifficulty(DifficultyChoice.NORMAL);
		creatorNormal = pickCreator(dNormal, DifficultyChoice.NORMAL);
		if (creatorNormal != null) {
			dataNormal = creatorNormal.createBounty(createdAt, this, dNormal, Stage.BOUNTY);
		}
		if (dataNormal == null || dataNormal.fleet == null) return false;

		int dHigh = pickDifficulty(DifficultyChoice.HIGH);
		creatorHigh = pickCreator(dHigh, DifficultyChoice.HIGH);
		if (creatorHigh != null) {
			dataHigh = creatorHigh.createBounty(createdAt, this, dHigh, Stage.BOUNTY);
		}
		//getPerson().getNameString() getPerson().getMarket();
		if (dataHigh == null || dataHigh.fleet == null) return false;


		return true;
	}
	
}











