package data.scripts.campaign.plugins.dustkeepers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetInflater;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater.*;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.plugins.AutofitPlugin;
import com.fs.starfarer.api.plugins.AutofitPlugin.AvailableWeapon;
import com.fs.starfarer.api.plugins.impl.CoreAutofitPlugin;
import com.fs.starfarer.api.util.ListMap;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.plugins.AutofitPlugin.AvailableFighter;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.utils.SotfMisc;

import java.util.*;

import static com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater.*;

/**
 *	Modified fleet inflater used by Dustkeeper fleets
 * 	Inflates different ships differently: Regulars with higher quality, auxiliaries with lower
 */

public class SotfDustkeeperFleetInflater implements FleetInflater, AutofitPlugin.AutofitPluginDelegate {

	// Quality penalty applied to auxiliary drones, to weapon/fighter selection and dmod count
	//private static final float AUXILIARY_QUALITY_PENALTY = 0.25f;

	public static class SortedWeapons {
		protected Map<Integer, DefaultFleetInflater.WeaponsForTier> tierMap = new LinkedHashMap<Integer, DefaultFleetInflater.WeaponsForTier>();

		public DefaultFleetInflater.WeaponsForTier getWeapons(int tier) {
			DefaultFleetInflater.WeaponsForTier data = tierMap.get(tier);
			if (data == null) {
				data = new DefaultFleetInflater.WeaponsForTier();
				tierMap.put(tier, data);
			}
			return data;
		}
	}

	public static class WeaponsForTier {
		//		protected Map<String, List<AvailableWeapon>> catMap = new LinkedHashMap<String, List<AvailableWeapon>>();
//
//		public List<AvailableWeapon> getWeapons(String cat) {
//			List<AvailableWeapon> list = catMap.get(cat);
//			if (list == null) {
//				list = new ArrayList<AvailableWeapon>();
//				catMap.put(cat, list);
//			}
//			return list;
//		}
		protected Map<String, DefaultFleetInflater.WeaponsForSize> catMap = new LinkedHashMap<String, DefaultFleetInflater.WeaponsForSize>();

		public DefaultFleetInflater.WeaponsForSize getWeapons(String cat) {
			DefaultFleetInflater.WeaponsForSize size = catMap.get(cat);
			if (size == null) {
				size = new DefaultFleetInflater.WeaponsForSize();
				catMap.put(cat, size);
			}
			return size;
		}
	}

	public static class WeaponsForSize {
		protected Map<WeaponSize, List<AvailableWeapon>> sizeMap = new LinkedHashMap<WeaponAPI.WeaponSize, List<AvailableWeapon>>();
		public List<AvailableWeapon> getWeapons(WeaponSize size) {
			List<AvailableWeapon> list = sizeMap.get(size);
			if (list == null) {
				list = new ArrayList<AvailableWeapon>();
				sizeMap.put(size, list);
			}
			return list;
		}
	}

	public static class AvailableFighterImpl implements AvailableFighter {
		protected FighterWingSpecAPI spec;
		protected int quantity = 0;

		public AvailableFighterImpl(FighterWingSpecAPI spec, int quantity) {
			this.spec = spec;
			this.quantity = quantity;
		}

		public AvailableFighterImpl(String wingId, int quantity) {
			spec = Global.getSettings().getFighterWingSpec(wingId);
			this.quantity = quantity;
		}

		public String getId() {
			return spec.getId();
		}
		public float getPrice() {
			return 0;
		}
		public int getQuantity() {
			return quantity;
		}
		public CargoAPI getSource() {
			return null;
		}
		public SubmarketAPI getSubmarket() {
			return null;
		}
		public FighterWingSpecAPI getWingSpec() {
			return spec;
		}
		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}
	}

	public static class AvailableWeaponImpl implements AvailableWeapon {
		protected WeaponSpecAPI spec;
		protected int quantity = 0;
		public AvailableWeaponImpl(WeaponSpecAPI spec, int quantity) {
			this.spec = spec;
			this.quantity = quantity;
		}

		public String getId() {
			return spec.getWeaponId();
		}
		public float getPrice() {
			return 0;
		}
		public int getQuantity() {
			return quantity;
		}
		public CargoAPI getSource() {
			return null;
		}
		public SubmarketAPI getSubmarket() {
			return null;
		}
		public WeaponSpecAPI getSpec() {
			return spec;
		}
		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}

		protected MutableShipStatsAPI savedCostStats = null;
		protected float cachedOPCost = -1;
		public float getOPCost(MutableCharacterStatsAPI stats, MutableShipStatsAPI shipStats) {
			if (savedCostStats == shipStats && cachedOPCost >= 0) return cachedOPCost;

			cachedOPCost = spec.getOrdnancePointCost(stats, shipStats);
			savedCostStats = shipStats;
			return cachedOPCost;
		}
	}

	protected DefaultFleetInflaterParams p;

	protected transient FleetMemberAPI currMember = null;
	protected transient ShipVariantAPI currVariant = null;
	protected transient List<AvailableFighter> fighters;
	protected transient List<AvailableWeapon> weapons;
	protected transient List<String> hullmods;
	protected transient CampaignFleetAPI fleet;
	protected transient FactionAPI faction;

	public static float getTierProbability(int tier, float quality) {
		// since whether to upgrade or not is now randomized, higher probability of
		// better tier weapons being available (as they may still not end up being used)
		if (tier == 1) return Math.min(0.9f, 0.75f + quality);
		if (tier == 2) return Math.min(0.9f, 0.5f + quality * 0.5f);
		if (tier == 3) return Math.min(0.9f, 0.25f + quality * 0.25f);

		return 1f;
	}

	public SotfDustkeeperFleetInflater(DefaultFleetInflaterParams p) {
		this.p = p;
	}

	public void inflate(CampaignFleetAPI fleet) {
		Random random = new Random();
		//p.seed = null;
		if (p.seed != null) random = new Random(p.seed);

		//p.quality = 2f;

		//random = new Random();


		Random dmodRandom = new Random();
		if (p.seed != null) dmodRandom = Misc.getRandom(p.seed, 5);

		boolean usesAuxiliaries = false;
		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			if (SotfMisc.isDustkeeperAuxiliary(member)) {
				usesAuxiliaries = true;
			}
		}

		// autofit for regulars
		// see for details: tldr they use CH, ECM more often
		CoreAutofitPlugin auto = new SotfDustkeeperAutofitPlugin(fleet.getCommander(), false);
		// anyone who uses Cyberwarfare (typically just the flagship) WILL ensure their loadout has ECM to beef it up
		CoreAutofitPlugin cyberAuto = new SotfDustkeeperAutofitPlugin(fleet.getCommander(), true);
		// autofit for auxiliary drones
		CoreAutofitPlugin auxAuto = new CoreAutofitPlugin(fleet.getCommander());
		auto.setRandom(random);
		cyberAuto.setRandom(random);
		auxAuto.setRandom(random);

		boolean upgrade = random.nextFloat() < Math.min(0.1f + p.quality * 0.5f, 0.5f);
		auto.setChecked(CoreAutofitPlugin.UPGRADE, upgrade);
		cyberAuto.setChecked(CoreAutofitPlugin.UPGRADE, true);
		// auxiliaries don't have good weapons unless required by their fit
		// e.g Close Support warden will still try to have a Hvel Driver, but no railguns on a Picket
		auxAuto.setChecked(CoreAutofitPlugin.UPGRADE, false);

		this.fleet = fleet;
		this.faction = fleet.getFaction();
		if (p.factionId != null) {
			this.faction = Global.getSector().getFaction(p.factionId);
		}

		hullmods = new ArrayList<String>(faction.getKnownHullMods());

		SortedWeapons nonPriorityWeapons = new SortedWeapons();
		SortedWeapons priorityWeapons = new SortedWeapons();


		Set<String> weaponCategories = new LinkedHashSet<String>();
		for (String weaponId : faction.getKnownWeapons()) {
			if (!faction.isWeaponKnownAt(weaponId, p.timestamp)) continue;

			WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(weaponId);

			if (spec == null) {
				throw new RuntimeException("Weapon with spec id [" + weaponId + "] not found");
			}

			int tier = spec.getTier();
			String cat = spec.getAutofitCategory();

			if (isPriority(spec)) {
				List<AvailableWeapon> list = priorityWeapons.getWeapons(tier).getWeapons(cat).getWeapons(spec.getSize());
				list.add(new AvailableWeaponImpl(spec, 1000));
			} else {
				List<AvailableWeapon> list = nonPriorityWeapons.getWeapons(tier).getWeapons(cat).getWeapons(spec.getSize());
				list.add(new AvailableWeaponImpl(spec, 1000));
			}
			weaponCategories.add(cat);
		}

		ListMap<AvailableFighter> nonPriorityFighters = new ListMap<AvailableFighter>();
		ListMap<AvailableFighter> priorityFighters = new ListMap<AvailableFighter>();
		Set<String> fighterCategories = new LinkedHashSet<String>();
		for (String wingId : faction.getKnownFighters()) {
			if (!faction.isFighterKnownAt(wingId, p.timestamp)) continue;

			FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(wingId);
			if (spec == null) {
				throw new RuntimeException("Fighter wing with spec id [" + wingId + "] not found");
			}

			// Remnant-only fleets don't use Derelict fighters ever
			if (!usesAuxiliaries && spec.hasTag(SotfIDs.TAG_DUSTKEEPER_AUXILIARY)) {
				continue;
			}

			String cat = spec.getAutofitCategory();

			if (isPriority(spec)) {
				priorityFighters.add(cat, new AvailableFighterImpl(spec, 1000));
			} else {
				nonPriorityFighters.add(cat, new AvailableFighterImpl(spec, 1000));
			}
			fighterCategories.add(cat);
		}


		//float averageDmods = (1f - quality) / Global.getSettings().getFloat("qualityPerDMod");
		float averageDmods = getAverageDmodsForQuality(p.quality);

		//System.out.println("Quality: " + quality + ", Average: " + averageDmods);

		boolean forceAutofit = fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_FORCE_AUTOFIT_ON_NO_AUTOFIT_SHIPS);
		int memberIndex = 0;
		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {

			if (!forceAutofit && member.getHullSpec().hasTag(Tags.TAG_NO_AUTOFIT)) {
				continue;
			}

			if (!forceAutofit && member.getVariant() != null && member.getVariant().hasTag(Tags.TAG_NO_AUTOFIT)) {
				continue;
			}

			if (!faction.isPlayerFaction()) {
				if (!forceAutofit && member.getHullSpec().hasTag(Tags.TAG_NO_AUTOFIT_UNLESS_PLAYER)) {
					continue;
				}
				if (!forceAutofit && member.getVariant() != null && member.getVariant().hasTag(Tags.TAG_NO_AUTOFIT_UNLESS_PLAYER)) {
					continue;
				}
			}

			boolean isAuxiliary = SotfMisc.isDustkeeperAuxiliary(member);

			// need this so that when reinflating a fleet that lost members, the members reinflate consistently
			if (p.seed != null) {
				int extra = member.getShipName().hashCode();
				random = new Random(p.seed * extra);
				auto.setRandom(random);
				cyberAuto.setRandom(random);
				auxAuto.setRandom(random);
				dmodRandom = Misc.getRandom(p.seed * extra, 5);
			}

			List<WeaponSize> sizes = new ArrayList<WeaponAPI.WeaponSize>();
			sizes.add(WeaponSize.SMALL);
			sizes.add(WeaponSize.MEDIUM);
			sizes.add(WeaponSize.LARGE);

			weapons = new ArrayList<AvailableWeapon>();
			for (String cat : weaponCategories) {
				for (WeaponSize size : sizes) {
					boolean foundSome = false;
					for (int tier = 0; tier < 4; tier++) {
						float p = getTierProbability(tier, this.p.quality);
						if (this.p.allWeapons != null && this.p.allWeapons) {
							p = 1f;
						}

						List<AvailableWeapon> priority = priorityWeapons.getWeapons(tier).getWeapons(cat).getWeapons(size);
						List<AvailableWeapon> nonPriority = nonPriorityWeapons.getWeapons(tier).getWeapons(cat).getWeapons(size);

						if (!foundSome) {
							p = 1f;
						}

						boolean tierAvailable = random.nextFloat() < p;
						if (!tierAvailable && foundSome) continue;
						//if (random.nextFloat() >= p) continue;

						int num = 2;

						// get a better selection of smaller weapons compared to normal autofit
						switch (size) {
							case MEDIUM:
								num += 1;
								break;
							case SMALL:
								num += 2;
								break;
						}
//						if (!tierAvailable) {
//							num = 1;
//						}

						if (this.p.allWeapons != null && this.p.allWeapons) {
							num = 500;
						}

						Set<Integer> picks = makePicks(num, priority.size(), random);
						for (Integer index : picks) {
							AvailableWeapon w = priority.get(index);
							weapons.add(w);
							foundSome = true;
						}

						num -= picks.size();
						if (num > 0) {
							picks = makePicks(num, nonPriority.size(), random);
							for (Integer index : picks) {
								AvailableWeapon w = nonPriority.get(index);
								weapons.add(w);
								foundSome = true;
							}
						}
					}
				}
			}

			fighters = new ArrayList<AvailableFighter>();
			for (String cat : fighterCategories) {
				List<AvailableFighter> priority = priorityFighters.get(cat);

				boolean madePriorityPicks = false;
				if (priority != null) {
					int num = random.nextInt(2) + 1;
					if (this.p.allWeapons != null && this.p.allWeapons) {
						num = 100;
					}

					// Remnant ships get better availability of their own fighters
					if (!isAuxiliary) {
						num += 2;
					}

					Set<Integer> picks = makePicks(num, priority.size(), random);
					for (Integer index : picks) {
						AvailableFighter f = priority.get(index);
						fighters.add(f);
						madePriorityPicks = true;
					}
				}

				if (!madePriorityPicks || isAuxiliary) {
					int num = random.nextInt(2) + 1;
					if (this.p.allWeapons != null && this.p.allWeapons) {
						num = 100;
					}
					// Auxiliaries ALWAYS have access to their own fighters
					if (isAuxiliary) {
						num = 100;
					}

					List<AvailableFighter> nonPriority = nonPriorityFighters.get(cat);
					Set<Integer> picks = makePicks(num, nonPriority.size(), random);
					for (Integer index : picks) {
						AvailableFighter f = nonPriority.get(index);
						fighters.add(f);
					}
				}
			}


			ShipVariantAPI target = member.getVariant();
			if (target.getOriginalVariant() != null) {
				// needed if inflating the same fleet repeatedly to pick up weapon availability changes etc
				target = Global.getSettings().getVariant(target.getOriginalVariant());
			}

			currVariant = Global.getSettings().createEmptyVariant(fleet.getId() + "_" + memberIndex, target.getHullSpec());
			currMember = member;

			if (target.isStockVariant()) {
				currVariant.setOriginalVariant(target.getHullVariantId());
			}

			float rProb = faction.getDoctrine().getAutofitRandomizeProbability();
			if (p.rProb != null) rProb = p.rProb;
			boolean randomize = random.nextFloat() < rProb;
			if (member.isStation()) randomize = false;
			auto.setChecked(CoreAutofitPlugin.RANDOMIZE, randomize);
			// more stringent loadouts for flagships
			cyberAuto.setChecked(CoreAutofitPlugin.RANDOMIZE, false);
			// do what you're told
			auxAuto.setChecked(CoreAutofitPlugin.RANDOMIZE, false);

			memberIndex++;

			int maxSmods = 0;
			if (p.averageSMods != null && !member.isCivilian()) {
				maxSmods = getMaxSMods(currVariant, p.averageSMods, dmodRandom) - currVariant.getSMods().size();
			}
			// Annexes with Cyberwarfare get +1 smod
			if (member.getCaptain() != null && member.getCaptain().getStats().hasSkill(SotfIDs.SKILL_CYBERWARFARE)) {
				cyberAuto.doFit(currVariant, target, Math.min(maxSmods + 1, 3), this);
			} else if (!isAuxiliary) {
				auto.doFit(currVariant, target, maxSmods, this);
			} else {
				// no smods on auxiliaries, even if the regulars have them
				auxAuto.doFit(currVariant, target, 0, this);
			}
			currVariant.setSource(VariantSource.REFIT);
			member.setVariant(currVariant, false, false);

			if (!currMember.isStation()) {
				float averageDmodsToAdd = averageDmods;
				// flagship is generally a bit more pristine
				if (member.isFlagship()) {
					averageDmodsToAdd -= 1f;
				}
				// auxiliaries are in worse states
				if (isAuxiliary) {
					averageDmodsToAdd += 2f;
				}
				int addDmods = getNumDModsToAdd(currVariant, averageDmodsToAdd, dmodRandom);
				// if captain has Derelict Contingent, add LOTS OF DMODS
				if (member.getCaptain() != null && member.getCaptain().getStats().hasSkill(SotfIDs.SKILL_DERELICTCONTINGENTP)) {
					addDmods = 5;
				}
				if (addDmods > 0) {
					DModManager.setDHull(currVariant);
					DModManager.addDMods(member, true, addDmods, dmodRandom);
				}
			}
		}


		fleet.getFleetData().setSyncNeeded();
		fleet.getFleetData().syncIfNeeded();

	}

	public static int getNumDModsToAdd(ShipVariantAPI variant, float averageDMods, Random random) {
		int dmods = (int) Math.round(averageDMods + random.nextDouble() * 3f - 2f);
		if (dmods > 5) dmods = 5;
		int dmodsAlready = DModManager.getNumDMods(variant);
		dmods -= dmodsAlready;

		return Math.max(0, dmods);
	}

	public static int getMaxSMods(ShipVariantAPI variant, int averageSMods, Random random) {
		float f = random.nextFloat();
		int sMods = averageSMods;
		if (f < 0.25f) {
			sMods = averageSMods - 1;
		} else if (f < 0.5f) {
			sMods = averageSMods + 1;
		}
		if (sMods > 3) sMods = 3;
		if (sMods < 0) sMods = 0;
		return sMods;
	}

	public static float getAverageDmodsForQuality(float quality) {
		float averageDmods = (1f - quality) / Global.getSettings().getFloat("qualityPerDMod");
		return averageDmods;
	}

	public static Set<Integer> makePicks(int num, int max, Random random) {
		if (num > max) num = max;
		Set<Integer> result = new LinkedHashSet<Integer>();
		if (num == 0) return result;

		if (num == max) {
			for (int i = 0; i < max; i++) {
				result.add(i);
			}
			return result;
		}

		while (result.size() < num) {
			int add = random.nextInt(max);
			result.add(add);
		}

		return result;
	}


	public boolean removeAfterInflating() {
		return p.persistent == null || !p.persistent;
	}

	public void setRemoveAfterInflating(boolean removeAfterInflating) {
		p.persistent = !removeAfterInflating;
		if (!p.persistent) p.persistent = null;
	}

	public void clearFighterSlot(int index, ShipVariantAPI variant) {
		variant.setWingId(index, null);
		for (AvailableFighter curr : fighters) {
			if (curr.getId().equals(curr.getId())) {
				curr.setQuantity(curr.getQuantity() + 1);
				break;
			}
		}
	}

	public void clearWeaponSlot(WeaponSlotAPI slot, ShipVariantAPI variant) {
		variant.clearSlot(slot.getId());
		for (AvailableWeapon curr : weapons) {
			if (curr.getId().equals(curr.getId())) {
				curr.setQuantity(curr.getQuantity() + 1);
				break;
			}
		}
	}

	public void fitFighterInSlot(int index, AvailableFighter fighter, ShipVariantAPI variant) {
		fighter.setQuantity(fighter.getQuantity() - 1);
		variant.setWingId(index, fighter.getId());
	}

	public void fitWeaponInSlot(WeaponSlotAPI slot, AvailableWeapon weapon, ShipVariantAPI variant) {
		weapon.setQuantity(weapon.getQuantity() - 1);
		variant.addWeapon(slot.getId(), weapon.getId());
	}

	public List<AutofitPlugin.AvailableFighter> getAvailableFighters() {
		return fighters;
	}

	public List<AvailableWeapon> getAvailableWeapons() {
		return weapons;
	}

	public List<String> getAvailableHullmods() {
		return hullmods;
	}

	public ShipAPI getShip() {
		return null;
	}

	public void syncUIWithVariant(ShipVariantAPI variant) {

	}

	public boolean isPriority(WeaponSpecAPI weapon) {
		return faction.isWeaponPriority(weapon.getWeaponId());
	}

	public boolean isPriority(FighterWingSpecAPI wing) {
		return faction.isFighterPriority(wing.getId());
	}

	public FleetMemberAPI getMember() {
		return currMember;
	}


	public static void main(String[] args) {

		Random random = new Random();


		float total = 0f;
		float num = 1000f;
		int []counts = new int[10];
		for (int i = 0; i < num; i++) {
			int dmods = 1;
			total += dmods;
			counts[dmods]++;
		}

		System.out.println("Average dmods: " + total / num);
		for (int i = 0; i <= 5; i++) {
			System.out.println(i + ":" + counts[i]);
		}
	}
	public FactionAPI getFaction() {
		return faction;
	}
	public Long getSeed() {
		return p.seed;
	}
	public void setSeed(Long seed) {
		this.p.seed = seed;
	}
	public Boolean getPersistent() {
		return p.persistent;
	}
	public void setPersistent(Boolean persistent) {
		this.p.persistent = persistent;
	}
	public float getQuality() {
		return p.quality;
	}
	public int getAverageNumSMods() {
		return p.averageSMods == null ? 0 : p.averageSMods;
	}
	public void setQuality(float quality) {
		this.p.quality = quality;
	}
	public Long getTimestamp() {
		return p.timestamp;
	}
	public void setTimestamp(Long timestamp) {
		this.p.timestamp = timestamp;
	}
	public Object getParams() {
		return p;
	}


	public boolean canAddRemoveHullmodInPlayerCampaignRefit(String modId) {
		return true;
	}

	public boolean isPlayerCampaignRefit() {
		return false;
	}


	public boolean isAllowSlightRandomization() {
		return true;
	}

	@Override
	public MarketAPI getMarket() {
		return null;
	}

	@Override
	public FleetMemberAPI getFleetMember() {
		return null;
	}

}
