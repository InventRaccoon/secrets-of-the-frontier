package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.CustomRepImpact;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.missions.hallowhall.SotfHFHInadCoresFactor;
import data.scripts.campaign.missions.hallowhall.SotfHFHInadMissionCompletedFactor;
import data.scripts.campaign.missions.hallowhall.SotfHopeForHallowhallEventIntel;

import java.util.List;
import java.util.Map;

/**
 * NotifyEvent $eventHandle <params> 
 * 
 */
public class SotfDKAICores extends BaseCommandPlugin {
	
	protected CampaignFleetAPI playerFleet;
	protected SectorEntityToken entity;
	protected FactionAPI playerFaction;
	protected FactionAPI entityFaction;
	protected TextPanelAPI text;
	protected OptionPanelAPI options;
	protected CargoAPI playerCargo;
	protected MemoryAPI memory;
	protected InteractionDialogAPI dialog;
	protected Map<String, MemoryAPI> memoryMap;
	protected PersonAPI person;
	protected FactionAPI faction;

	protected boolean buysAICores;
	protected float valueMult;
	protected float repMult;
	
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		
		this.dialog = dialog;
		this.memoryMap = memoryMap;
		
		String command = params.get(0).getString(memoryMap);
		if (command == null) return false;
		
		memory = getEntityMemory(memoryMap);
		
		entity = dialog.getInteractionTarget();
		text = dialog.getTextPanel();
		options = dialog.getOptionPanel();
		
		playerFleet = Global.getSector().getPlayerFleet();
		playerCargo = playerFleet.getCargo();
		
		playerFaction = Global.getSector().getPlayerFaction();
		entityFaction = entity.getFaction();
		
		person = dialog.getInteractionTarget().getActivePerson();
		faction = person.getFaction();
		
		buysAICores = faction.getCustomBoolean("buysAICores");
		valueMult = faction.getCustomFloat("AICoreValueMult");
		repMult = faction.getCustomFloat("AICoreRepMult");
		
		if (command.equals("selectCores")) {
			selectCores();
		} else if (command.equals("playerHasCores")) {
			return playerHasCores();
		}
		
		return true;
	}

	protected void selectCores() {
		CargoAPI copy = Global.getFactory().createCargo(false);
		//copy.addAll(cargo);
		//copy.setOrigSource(playerCargo);
		for (CargoStackAPI stack : playerCargo.getStacksCopy()) {
			CommoditySpecAPI spec = stack.getResourceIfResource();
			if (spec != null && spec.getDemandClass().equals(Commodities.AI_CORES) && !spec.getId().contains("rat_")) {
				copy.addFromStack(stack);
			}
			if (stack.isSpecialStack()) {
				SpecialItemSpecAPI data = stack.getSpecialItemSpecIfSpecial();
				if (data != null && data.getId().equals("rat_ai_core_special")) {
					copy.addFromStack(stack);
				}
			}
		}
		copy.sort();
		
		final float width = 310f;
		dialog.showCargoPickerDialog("Select AI cores to turn in", "Confirm", "Cancel", true, width, copy, new CargoPickerListener() {
			public void pickedCargo(CargoAPI cargo) {
				if (cargo.isEmpty()) {
					cancelledCargoSelection();
					return;
				}
				
				cargo.sort();
				for (CargoStackAPI stack : cargo.getStacksCopy()) {
					playerCargo.removeItems(stack.getType(), stack.getData(), stack.getSize());
					if (stack.isCommodityStack()) { // should be always, but just in case
						int num = (int) stack.getSize();

						// put beta cores into industries as per Dustkeeper protocol
						if (stack.getCommodityId().equals(Commodities.BETA_CORE) && person.getMarket() != null) {
							int numCores = num;
							for (Industry ind : person.getMarket().getIndustries()) {
								if (!ind.canInstallAICores()) continue;
								if (ind.getAICoreId() != null) {
									if (ind.getAICoreId().equals(Commodities.BETA_CORE)) continue;
								}
								ind.setAICoreId(Commodities.BETA_CORE);
								numCores--;
								if (numCores == 0) break;
							}
						}

						AddRemoveCommodity.addCommodityLossText(stack.getCommodityId(), num, text);
						
						String key = "$turnedIn_" + stack.getCommodityId();
						int turnedIn = faction.getMemoryWithoutUpdate().getInt(key);
						faction.getMemoryWithoutUpdate().set(key, turnedIn + num);
						
						// Also, total of all cores! -dgb
						String key2 = "$turnedIn_allCores";
						int turnedIn2 = faction.getMemoryWithoutUpdate().getInt(key2);
						faction.getMemoryWithoutUpdate().set(key2, turnedIn2 + num);
					} else if (stack.isSpecialStack()) {
						SpecialItemData data = stack.getSpecialDataIfSpecial();
						int num = (int) stack.getSize();

						AddRemoveCommodity.addCommodityLossText(data.getData(), num, text);

						String key = "$turnedIn_" + data.getData();
						int turnedIn = faction.getMemoryWithoutUpdate().getInt(key);
						faction.getMemoryWithoutUpdate().set(key, turnedIn + num);

						// Also, total of all cores! -dgb
						String key2 = "$turnedIn_allCores";
						int turnedIn2 = faction.getMemoryWithoutUpdate().getInt(key2);
						faction.getMemoryWithoutUpdate().set(key2, turnedIn2 + num);
					}
				}
				
				float bounty = computeCoreCreditValue(cargo);
				float repChange = computeCoreReputationValue(cargo);
				int trust = computeCoreTrustValue(cargo);

				if (person.getId().equals(SotfPeople.INADVERTENT) && trust > 0) {
					SotfHopeForHallowhallEventIntel hh = SotfHopeForHallowhallEventIntel.get();
					if (hh != null) {
						int maxAddablePoints = SotfHopeForHallowhallEventIntel.MAX_POINTS_FROM_CORES - hh.pointsFromCores;
						if (trust > maxAddablePoints) {
							trust = maxAddablePoints;
						}
						if (trust > 0) {
							SotfHFHInadCoresFactor factor = new SotfHFHInadCoresFactor(trust);
							SotfHopeForHallowhallEventIntel.addFactorIfAvailable(factor, dialog);
							hh.pointsFromCores += trust;
						}
					}
				}

				if (bounty > 0) {
					playerCargo.getCredits().add(bounty);
					AddRemoveCommodity.addCreditsGainText((int)bounty, text);
				}
				
				if (repChange >= 1f) {
					CustomRepImpact impact = new CustomRepImpact();
					impact.delta = repChange * 0.01f;
					Global.getSector().adjustPlayerReputation(
							new RepActionEnvelope(RepActions.CUSTOM, impact,
												  null, text, true), 
												  faction.getId());
					
					impact.delta *= 0.5f;
					if (impact.delta >= 0.01f) {
						Global.getSector().adjustPlayerReputation(
								new RepActionEnvelope(RepActions.CUSTOM, impact,
													  null, text, true), 
													  person);
					}
				}
				
				FireBest.fire(null, dialog, memoryMap, "AICoresTurnedIn");
			}
			public void cancelledCargoSelection() {
			}
			public void recreateTextPanel(TooltipMakerAPI panel, CargoAPI cargo, CargoStackAPI pickedUp, boolean pickedUpFromSource, CargoAPI combined) {
				float bounty = computeCoreCreditValue(combined);
				float repChange = computeCoreReputationValue(combined);
				int trust = computeCoreTrustValue(combined);
				
				float pad = 3f;
				float small = 5f;
				float opad = 10f;

				panel.setParaFontOrbitron();
				panel.addPara(Misc.ucFirst(faction.getDisplayName()), faction.getBaseUIColor(), 1f);

				panel.setParaFontDefault();
				
				panel.addImage(faction.getLogo(), width * 1f, 3f);
				
				
				//panel.setParaFontColor(Misc.getGrayColor());
				//panel.setParaSmallInsignia();
				//panel.setParaInsigniaLarge();
				panel.addPara("Compared to dealing with other factions, turning AI cores in to " + 
						faction.getDisplayNameLongWithArticle() + " " +
						"will result in:", opad);
				panel.beginGridFlipped(width, 1, 40f, 10f);
				//panel.beginGrid(150f, 1);
				panel.addToGrid(0, 0, "Bounty value", "" + (int)(valueMult * 100f) + "%");
				panel.addToGrid(0, 1, "Reputation gain", "" + (int)(repMult * 100f) + "%");
				panel.addGrid(pad);
				
				panel.addPara("If you turn in the selected AI cores, you will receive a %s bounty " +
						"and your standing with " + faction.getDisplayNameWithArticle() + " will improve by %s points.",
						opad, Misc.getHighlightColor(),
						Misc.getWithDGS(bounty) + Strings.C,
						"" + (int) repChange);

				if (person.getId().equals(SotfPeople.INADVERTENT) && SotfHopeForHallowhallEventIntel.get() != null) {
					boolean capped = false;
					SotfHopeForHallowhallEventIntel hh = SotfHopeForHallowhallEventIntel.get();
					int maxAddablePoints = SotfHopeForHallowhallEventIntel.MAX_POINTS_FROM_CORES - hh.pointsFromCores;
					if (trust > maxAddablePoints) {
						trust = maxAddablePoints;
						capped = true;
					}
					if (trust <= 0 && capped) {
						panel.addPara("You've reached the maximum trust attainable by handing in AI cores, and will have to earn more by other means.",
								opad, Misc.getNegativeHighlightColor(),
								"maximum trust");
					} else if (capped) {
						panel.addPara("Additionally, you'll build %s trust with the Dustkeepers at Mia's Star. This brings you to the maximum trust you can build by handing in AI cores.",
								opad, Misc.getNegativeHighlightColor(),
								"" + trust, "maximum trust");
					} else {
						panel.addPara("Additionally, you'll build %s trust with the Dustkeepers at Mia's Star. You can gain %s more trust by handing in AI cores.",
								opad, Misc.getHighlightColor(),
								"" + trust, "" + maxAddablePoints);
					}
				}

				
				//panel.addPara("Bounty: %s", opad, Misc.getHighlightColor(), Misc.getWithDGS(bounty) + Strings.C);
				//panel.addPara("Reputation: %s", pad, Misc.getHighlightColor(), "+12");
			}
		});
	}

	protected float computeCoreCreditValue(CargoAPI cargo) {
		float bounty = 0;
		for (CargoStackAPI stack : cargo.getStacksCopy()) {
			CommoditySpecAPI spec = stack.getResourceIfResource();
			if (spec != null && spec.getDemandClass().equals(Commodities.AI_CORES)) {
				bounty += spec.getBasePrice() * stack.getSize();
			} else if (stack.isSpecialStack()) {
				SpecialItemData data = stack.getSpecialDataIfSpecial();
				bounty += Global.getSettings().getCommoditySpec(data.getData()).getBasePrice() * stack.getSize();
			}
		}
		bounty *= valueMult;
		return bounty;
	}

	protected int computeCoreTrustValue(CargoAPI cargo) {
		int hallowPoints = 0;
		for (CargoStackAPI stack : cargo.getStacksCopy()) {
			if (stack.isCommodityStack()) {
				switch (stack.getCommodityId()) {
					case Commodities.GAMMA_CORE:
						hallowPoints += SotfHopeForHallowhallEventIntel.GAMMA_POINTS * (int) stack.getSize();
						break;
					case Commodities.BETA_CORE:
						hallowPoints += SotfHopeForHallowhallEventIntel.BETA_POINTS * (int) stack.getSize();
						break;
					case Commodities.ALPHA_CORE:
						hallowPoints += SotfHopeForHallowhallEventIntel.ALPHA_POINTS * (int) stack.getSize();
						break;
					case "tahlan_daemoncore":
						hallowPoints += SotfHopeForHallowhallEventIntel.TAHLAN_DAEMON_POINTS * (int) stack.getSize();
						break;
					case "tahlan_archdaemoncore":
						hallowPoints += SotfHopeForHallowhallEventIntel.TAHLAN_ARCHDAEMON_POINTS * (int) stack.getSize();
						break;
					case "AL_graven_ai":
						hallowPoints += SotfHopeForHallowhallEventIntel.AL_GRAVEN_POINTS * (int) stack.getSize();
						break;
				}
			} else {
				SpecialItemData data = stack.getSpecialDataIfSpecial();
				String id = data.getData();
				switch (id) {
					case "rat_chronos_core":
						hallowPoints += SotfHopeForHallowhallEventIntel.RAT_CHRONOS_POINTS * (int) stack.getSize();
						break;
					case "rat_cosmos_core":
						hallowPoints += SotfHopeForHallowhallEventIntel.RAT_COSMOS_POINTS * (int) stack.getSize();
						break;
					case "rat_seraph_core":
						hallowPoints += SotfHopeForHallowhallEventIntel.RAT_SERAPH_POINTS * (int) stack.getSize();
						break;
					case "rat_primordial_core":
						hallowPoints += SotfHopeForHallowhallEventIntel.RAT_PRIMORDIAL_POINTS * (int) stack.getSize();
						break;
					case "rat_neuro_core":
						hallowPoints += SotfHopeForHallowhallEventIntel.RAT_NEURO_POINTS * (int) stack.getSize();
						break;
					case "rat_exo_processor":
						hallowPoints += SotfHopeForHallowhallEventIntel.RAT_EXO_POINTS * (int) stack.getSize();
						break;
				}
			}
		}
		return hallowPoints;
	}
	
	protected float computeCoreReputationValue(CargoAPI cargo) {
		float rep = 0;
		for (CargoStackAPI stack : cargo.getStacksCopy()) {
			CommoditySpecAPI spec = stack.getResourceIfResource();
			if (stack.isCommodityStack()) {
				rep += getBaseRepValue(spec.getId()) * stack.getSize();
				switch (spec.getId()) {
					case Commodities.GAMMA_CORE:
					case Commodities.BETA_CORE:
					case Commodities.ALPHA_CORE:
						rep += getBaseRepValue(spec.getId()) * (int) stack.getSize();
					case "tahlan_daemoncore":
						rep += getBaseRepValue(Commodities.BETA_CORE) * (int) stack.getSize();
						break;
					case "tahlan_archdaemoncore":
						rep += getBaseRepValue(Commodities.ALPHA_CORE) * (int) stack.getSize();
						break;
					case "AL_graven_ai":
						rep += getBaseRepValue(Commodities.ALPHA_CORE) * (int) stack.getSize();
						break;
				}
			} else if (stack.isSpecialStack()) {
				SpecialItemData data = stack.getSpecialDataIfSpecial();
				String id = data.getData();
				switch (id) {
					case "rat_chronos_core":
					case "rat_cosmos_core":
					case "rat_seraph_core":
					case "rat_neuro_core":
					case "rat_exo_processor":
						rep += getBaseRepValue(Commodities.ALPHA_CORE) * (int) stack.getSize();
						break;
					case "rat_primordial_core":
						rep += getBaseRepValue(Commodities.OMEGA_CORE) * (int) stack.getSize();
						break;
				}
			}
		}
		rep *= repMult;
		return rep;
	}
	
	public static float getBaseRepValue(String coreType) {
		if (Commodities.OMEGA_CORE.equals(coreType)) {
			return 15f;
		}
		if (Commodities.ALPHA_CORE.equals(coreType)) {
			return 5f;
		}
		if (Commodities.BETA_CORE.equals(coreType)) {
			return 2f;
		}
		if (Commodities.GAMMA_CORE.equals(coreType)) {
			return 1f;
		}
		return 1f;
	}

	protected boolean playerHasCores() {
		for (CargoStackAPI stack : playerCargo.getStacksCopy()) {
			CommoditySpecAPI spec = stack.getResourceIfResource();
			if (spec != null && spec.getDemandClass().equals(Commodities.AI_CORES)) {
				return true;
			}
			SpecialItemSpecAPI data = stack.getSpecialItemSpecIfSpecial();
			if (data != null && data.getId().equals("rat_ai_core_special")) {
				return true;
			}
		}
		return false;
	}
}















