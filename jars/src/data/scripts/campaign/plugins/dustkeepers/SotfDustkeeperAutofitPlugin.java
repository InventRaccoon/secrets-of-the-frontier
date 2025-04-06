package data.scripts.campaign.plugins.dustkeepers;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.plugins.impl.CoreAutofitPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 *	Autofit plugin used by Dustkeeper regulars. Is not used by their auxiliary drones - those use the core plugin!
 * 	Gives regulars more of a fondness for ECM and Converted Hangar than they usually would
 * 	Also causes Cyberwarfare users to hard-prioritize ECM package to boost their hacking
 */

public class SotfDustkeeperAutofitPlugin extends CoreAutofitPlugin {

	public static boolean CYBERWARFARE = false;

	public SotfDustkeeperAutofitPlugin(PersonAPI fleetCommander, boolean hasCyberwarfare) {
		super(fleetCommander);
		CYBERWARFARE = hasCyberwarfare;
	}

	public void doFit(ShipVariantAPI current, ShipVariantAPI target, int maxSMods, AutofitPluginDelegate delegate) {

		boolean player = fleetCommander != null && fleetCommander.isPlayer();

		if (!fittingModule) {
			fittedWeapons.clear();
			fittedFighters.clear();

			randomize = isChecked(RANDOMIZE);

			availableMods = new LinkedHashSet<String>(delegate.getAvailableHullmods());
		}

		current.getStationModules().putAll(target.getStationModules());

		int index = 0;
		for (String slotId : current.getStationModules().keySet()) {
			ShipVariantAPI moduleCurrent = current.getModuleVariant(slotId);
			boolean forceClone = false;
			if (moduleCurrent == null) {
				// when the target variant is not stock and has custom variants for the modules, grab them
				forceClone = true;
				moduleCurrent = target.getModuleVariant(slotId);
				//continue;
			}
			if (moduleCurrent == null) {
				String variantId = current.getHullVariantId();
				throw new RuntimeException("Module variant for slotId [" + slotId + "] not found for " +
						"variantId [" + variantId + "] of hull [" + current.getHullSpec().getHullId() + "]");
				//continue;
			}
			if (moduleCurrent.isStockVariant() || forceClone) {
				moduleCurrent = moduleCurrent.clone();
				moduleCurrent.setSource(VariantSource.REFIT);
				if (!forceClone) {
					moduleCurrent.setHullVariantId(moduleCurrent.getHullVariantId() + "_" + index);
				}
			}
			index++;

			ShipVariantAPI moduleTarget = target.getModuleVariant(slotId);
			if (moduleTarget == null) continue;

			fittingModule = true;
			doFit(moduleCurrent, moduleTarget, 0, delegate);
			fittingModule = false;

			current.setModuleVariant(slotId, moduleCurrent);
		}
		current.setSource(VariantSource.REFIT);

		weaponFilterSeed = random.nextLong();

		emptyWingTarget = null;
		if (delegate.getAvailableFighters().size() > 0) {
			emptyWingTarget = delegate.getAvailableFighters().get(random.nextInt(delegate.getAvailableFighters().size())).getId();
		}

		altWeaponCats.clear();
		altFighterCats.clear();

		slotsToSkip.clear();
		baysToSkip.clear();

		missilesWithAmmoOnCurrent = 0;

		boolean strip = isChecked(STRIP);
		if (strip) {
			stripWeapons(current, delegate);
			stripFighters(current, delegate);

			current.setNumFluxCapacitors(0);
			current.setNumFluxVents(0);
			if (delegate.isPlayerCampaignRefit()) {
				for (String modId : current.getNonBuiltInHullmods()) {
					boolean canRemove = delegate.canAddRemoveHullmodInPlayerCampaignRefit(modId);
					if (canRemove) {
						current.removeMod(modId);
					}
				}
			} else {
				current.clearHullMods();
			}
		} else {
			slotsToSkip.addAll(current.getFittedWeaponSlots());
			for (int i = 0; i < 20; i++) {
				String wingId = current.getWingId(i);
				if (wingId != null && !wingId.isEmpty()) {
					baysToSkip.add(i);
				}
			}
		}

		boolean reinforcedHull = isChecked(ALWAYS_REINFORCED_HULL);
		//boolean blastDoors = isChecked(ALWAYS_BLAST_DOORS); // NO

		//
		// This is the main change here
		// Cyberwarfare users will ALWAYS install ECM to boost their Cyberwarfare ability
		//

		if (CYBERWARFARE) {
			addHullmods(current, delegate, HullMods.ECM);
		}
		if (reinforcedHull) {
			addHullmods(current, delegate, HullMods.REINFORCEDHULL);
		}

		// WE ARE ROBOTS DO NOT INSTALL THE CREW CASUALTY REDUCTION
		//if (blastDoors) {
		//	addHullmods(current, delegate, HullMods.BLAST_DOORS);
		//}

		addHullmods(current, delegate, target.getNonBuiltInHullmods().toArray(new String[0]));

		int addedRandomHullmodPts = 0;
		if (randomize) {
			addedRandomHullmodPts = addRandomizedHullmodsPre(current, delegate);
		}


		fitFighters(current, target, false, delegate);
		fitWeapons(current, target, false, delegate);

		if (current.hasHullMod(HullMods.FRAGILE_SUBSYSTEMS) &&
				(current.getHullSize() == ShipAPI.HullSize.FRIGATE || current.getHullSize() == ShipAPI.HullSize.DESTROYER)) {
			addHullmods(current, delegate, HullMods.HARDENED_SUBSYSTEMS);
		}


		float addedMax = current.getHullSpec().getOrdnancePoints(stats) * 0.1f;
		if (randomize && addedRandomHullmodPts <= addedMax) {
			addRandomizedHullmodsPost(current, delegate);
		}

		float ventsCapsFraction = 1f;
		boolean upgrade = isChecked(UPGRADE);
		if (upgrade) {
			ventsCapsFraction = 0.5f;
		}

		addVentsAndCaps(current, target, ventsCapsFraction);


		// now that we're at the target level of vents and caps
		// see if we can upgrade some weapons
		if (upgrade) {
			fitFighters(current, target, true, delegate);
			fitWeapons(current, target, true, delegate);
			addVentsAndCaps(current, target, 1f - ventsCapsFraction);
		}

		addExtraVentsAndCaps(current, target);
		//addHullmods(current, delegate, HullMods.REINFORCEDHULL, HullMods.BLAST_DOORS, HullMods.HARDENED_SUBSYSTEMS);

		// NO BLAST DOORS. Frigates prioritise hsub, others reinfhull.
		if (current.getHullSize() == ShipAPI.HullSize.FRIGATE) {
			addHullmods(current, delegate, HullMods.HARDENED_SUBSYSTEMS, HullMods.REINFORCEDHULL);
		} else {
			addHullmods(current, delegate, HullMods.REINFORCEDHULL, HullMods.HARDENED_SUBSYSTEMS);
		}
		addModsWithSpareOPIfAny(current, target, false, delegate);

		//maxSMods = 2;
		if (maxSMods > 0) {
			int added = convertToSMods(current, maxSMods);
			addExtraVents(current);
			addExtraCaps(current);
			if (!current.hasHullMod(HullMods.FLUX_DISTRIBUTOR)) {
				addDistributor(current, delegate);
			}
			if (!current.hasHullMod(HullMods.FLUX_COIL)) {
				addCoil(current, delegate);
			}
			if (current.getHullSize() == ShipAPI.HullSize.FRIGATE || current.hasHullMod(HullMods.SAFETYOVERRIDES)) {
				addHullmods(current, delegate, HullMods.HARDENED_SUBSYSTEMS, HullMods.REINFORCEDHULL, HullMods.BLAST_DOORS);
			} else {
				addHullmods(current, delegate, HullMods.REINFORCEDHULL, HullMods.BLAST_DOORS, HullMods.HARDENED_SUBSYSTEMS);
			}
			int remaining = maxSMods - added;
			if (remaining > 0) {
				List<String> mods = new ArrayList<String>();
				mods.add(HullMods.FLUX_DISTRIBUTOR);
				mods.add(HullMods.FLUX_COIL);
				if (current.getHullSize() == ShipAPI.HullSize.FRIGATE || current.hasHullMod(HullMods.SAFETYOVERRIDES)) {
					mods.add(HullMods.HARDENED_SUBSYSTEMS);
					mods.add(HullMods.REINFORCEDHULL);
				} else {
					mods.add(HullMods.REINFORCEDHULL);
					mods.add(HullMods.HARDENED_SUBSYSTEMS);
				}
				mods.add(HullMods.BLAST_DOORS);
				Iterator<String> iter = mods.iterator();
				while (iter.hasNext()) {
					String modId = iter.next();
					if (current.getPermaMods().contains(modId)) {
						iter.remove();
					}
				}
				for (int i = 0; i < remaining && !mods.isEmpty(); i++) {
					current.setNumFluxCapacitors(0);
					current.setNumFluxVents(0);
					String modId = mods.get(Math.min(i, mods.size() - 1));
					addHullmods(current, delegate, modId);
					convertToSMods(current, 1);
				}
			}
		}
		addExtraVents(current);
		addExtraCaps(current);

		current.setVariantDisplayName(target.getDisplayName());

		current.getWeaponGroups().clear();
		for (WeaponGroupSpec group : target.getWeaponGroups()) {
			WeaponGroupSpec copy = new WeaponGroupSpec(group.getType());
			copy.setAutofireOnByDefault(group.isAutofireOnByDefault());
			for (String slotId : group.getSlots()) {
				if (current.getWeaponId(slotId) != null) {
					copy.addSlot(slotId);
				}
			}
			if (!copy.getSlots().isEmpty()) {
				current.addWeaponGroup(copy);
			}
		}

		if (player) {
			if (current.getWeaponGroups().isEmpty() || randomize || current.hasUnassignedWeapons()) {
				current.autoGenerateWeaponGroups();
			}
		} else {
			current.getWeaponGroups().clear(); // will get auto-assigned when deployed in combat; until then don't care
		}

		if (!fittingModule) {
			delegate.syncUIWithVariant(current);
		}
	}

	protected int addRandomizedHullmodsPre(ShipVariantAPI current, AutofitPluginDelegate delegate) {
		// Vanilla is 0-2 random hullmods - let's bump the floor to 1 to get more unique hullmod doctrine
		int num = 1;
		// lesser chance for 2, give the actual fit a bit of breathing room
		//if (random.nextFloat() > 0.8f){
		//	num++;
		//}

		// ok don't stack TOO many hullmods
		if (current.getNonBuiltInHullmods().size() > 3) {
			num = 0;
		}

		ShipHullSpecAPI hull = current.getHullSpec();
		boolean omni = hull.getShieldType() == ShieldAPI.ShieldType.OMNI;
		boolean front = hull.getShieldType() == ShieldAPI.ShieldType.FRONT;
		boolean shield = omni || front;
		boolean phase = hull.getShieldType() == ShieldAPI.ShieldType.PHASE;
		int bays = hull.getFighterBays();
		float shieldArc = hull.getShieldSpec().getArc();

		WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(random);

		// Dustkeepers are much more likely to use Converted Hangar and ECM Package

		if (availableMods.contains(HullMods.CONVERTED_HANGAR) && hull.getHullSize() != ShipAPI.HullSize.FRIGATE) {
			if (bays <= 0) {
				picker.add(HullMods.CONVERTED_HANGAR, 8f);
			}
		}

		if (availableMods.contains(HullMods.ECM)) {
			if (hull.getHullSize() != ShipAPI.HullSize.FRIGATE || hull.getHullId().contains("lumen")) {
				picker.add(HullMods.ECM, 6f);
			} else {
				// don't waste OP on decent frigates (i.e Glimmers) for only 1% ECM
				picker.add(HullMods.ECM, 1f);
			}
		}

		// Dustkeeper frigates go ZOOM
		if (availableMods.contains(HullMods.UNSTABLE_INJECTOR)) {
			if (hull.getHullSize().equals(ShipAPI.HullSize.FRIGATE)) {
				picker.add(HullMods.UNSTABLE_INJECTOR, 4f);
			} else {
				picker.add(HullMods.UNSTABLE_INJECTOR, 1f);
			}
		}

		// Vanilla REALLY likes putting on ITU - let's tone that down for smaller ships to get more CH/ECM
		// Obv keep slapping it on cruisers/capitals since they actually need it
		if (availableMods.contains(HullMods.INTEGRATED_TARGETING_UNIT)) {
			if (hull.getHullSize().ordinal() >= ShipAPI.HullSize.CRUISER.ordinal()) {
				picker.add(HullMods.INTEGRATED_TARGETING_UNIT, 100f);
			} else if (hull.getHullSize().equals(ShipAPI.HullSize.DESTROYER)) {
				picker.add(HullMods.INTEGRATED_TARGETING_UNIT, 4f);
			} else {
				picker.add(HullMods.INTEGRATED_TARGETING_UNIT, 2f);
			}
		} else if (availableMods.contains(HullMods.DEDICATED_TARGETING_CORE)) {
			if (hull.getHullSize().ordinal() >= ShipAPI.HullSize.CRUISER.ordinal()) {
				picker.add(HullMods.DEDICATED_TARGETING_CORE, 100f);
			}
		}

		// FINALLY THE LUMEN CAN BE USEFUL, THANK YOU ROIDER VERY COOL
		if (availableMods.contains("roider_fighterClamps") && hull.getHullSize() == ShipAPI.HullSize.FRIGATE) {
			if (bays <= 0) {
				if (hull.getHullId().contains("lumen")) {
					picker.add("roider_fighterClamps", 12f);
				} else {
					picker.add("roider_fighterClamps", 4f);
				}
			}
		}

		// no
		//if (availableMods.contains(HullMods.HEAVYARMOR)) {
		//	picker.add(HullMods.HEAVYARMOR, 1f);
		//}

		// also no
		//if (availableMods.contains(HullMods.SAFETYOVERRIDES)) {
		//	if (hull.getHullSize().ordinal() <= HullSize.CRUISER.ordinal()) {
		//		picker.add(HullMods.SAFETYOVERRIDES, 1f);
		//	}
		//}


		// vanilla below


		if (availableMods.contains(HullMods.FRONT_SHIELD_CONVERSION)) {
			if (omni && shieldArc < 270) {
				picker.add(HullMods.FRONT_SHIELD_CONVERSION, 1f);
			}
		}

		if (availableMods.contains(HullMods.EXTENDED_SHIELDS)) {
			if (shield && shieldArc <= 300) {
				picker.add(HullMods.EXTENDED_SHIELDS, 1f);
			}
		}

		if (availableMods.contains(HullMods.MAKESHIFT_GENERATOR)) {
			if (!shield && !phase) {
				picker.add(HullMods.MAKESHIFT_GENERATOR, 1f);
			}
		}

		if (availableMods.contains(HullMods.EXPANDED_DECK_CREW)) {
			if (bays >= 2) {
				picker.add(HullMods.EXPANDED_DECK_CREW, 1f);
			}
		}

		if (availableMods.contains(HullMods.HARDENED_SHIELDS)) {
			if (shield) {
				picker.add(HullMods.HARDENED_SHIELDS, 1.5f);
			}
		}

		if (availableMods.contains(HullMods.STABILIZEDSHIELDEMITTER)) {
			if (shield) {
				picker.add(HullMods.STABILIZEDSHIELDEMITTER, 1f);
			}
		}

		if (availableMods.contains(HullMods.INSULATEDENGINE)) {
			if (!omni) {
				picker.add(HullMods.INSULATEDENGINE, 1f);
			}
		}

		if (availableMods.contains(HullMods.FLUXBREAKERS)) {
			if (shield) {
				picker.add(HullMods.FLUXBREAKERS, 1f);
			} else {
				picker.add(HullMods.FLUXBREAKERS, 10f);
			}
		}

		float addedTotal = 0;
		float addedMax = current.getHullSpec().getOrdnancePoints(stats) * 0.2f;
		for (int i = 0; i < num; i++) {
			String modId = picker.pickAndRemove();
			if (modId == null) break;
			if (current.hasHullMod(modId)) {
				i--;
				continue;
			}

			if (modId.equals(HullMods.EXTENDED_SHIELDS)) {
				picker.remove(HullMods.FRONT_SHIELD_CONVERSION);
			} else if (modId.equals(HullMods.FRONT_SHIELD_CONVERSION) && shieldArc >= 180) {
				picker.remove(HullMods.EXTENDED_SHIELDS);
			}
			addedTotal = addHullmods(current, delegate, modId);
			if (addedTotal >= addedMax) break;
		}

		return (int) addedTotal;
	}
}
