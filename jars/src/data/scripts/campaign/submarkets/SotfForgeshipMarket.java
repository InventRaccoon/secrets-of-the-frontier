// Forgeship's "open market" with Proxy drones for sale, and no crew sales because wtf are they gonna do with em
package data.scripts.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.submarkets.OpenMarketPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.SotfIDs;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class SotfForgeshipMarket extends OpenMarketPlugin {

    public static Set<String> HUMAN_COMMODITIES = new HashSet<String>();
    static {
        HUMAN_COMMODITIES.add(Commodities.CREW);
        HUMAN_COMMODITIES.add(Commodities.MARINES);
        HUMAN_COMMODITIES.add("prisoner"); // Nex high value prisoners
    }

    // very low tariffs
    public float getTariff() {
        return 0.06f;
    }

    public void init(SubmarketAPI submarket) {
        super.init(submarket);
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        float seconds = Global.getSector().getClock().convertToSeconds(sinceLastCargoUpdate);
        addAndRemoveStockpiledResources(seconds, false, true, true);
        sinceLastCargoUpdate = 0f;

        if (okToUpdateShipsAndWeapons()) {
            sinceSWUpdate = 0f;

            pruneWeapons(0f);

            int weapons = 14 + Math.max(0, market.getSize() - 1) * 2;
            int fighters = 4 + Math.max(0, market.getSize() - 3);

            addWeapons(weapons, weapons, 2, SotfIDs.DUSTKEEPERS_PROXIES);
            addFighters(fighters, fighters, 1, SotfIDs.DUSTKEEPERS_PROXIES);

            getCargo().getMothballedShips().clear();

            FactionDoctrineAPI doctrineOverride = Global.getSector().getFaction(SotfIDs.DUSTKEEPERS_PROXIES).getDoctrine().clone();
            doctrineOverride.setShipSize(2);

            addShips(SotfIDs.DUSTKEEPERS_PROXIES,
                    200f, // combat
                    0f, // freighter
                    0f, // tanker
                    0f, // transport
                    0f, // liner
                    0f, // utilityPts
                    null, // qualityOverride
                    0f, // qualityMod
                    null,
                    doctrineOverride);


            addHullMods(2, 3 + itemGenRandom.nextInt(3), SotfIDs.DUSTKEEPERS_PROXIES);
        }

        getCargo().sort();
    }

    @Override
    protected FleetMemberAPI addShip(String variantOrWingId, boolean withDmods, float quality) {
        FleetMemberAPI member = null;
        if (variantOrWingId.endsWith("_wing")) {
            member = Global.getFactory().createFleetMember(FleetMemberType.FIGHTER_WING, variantOrWingId);
        } else {
            member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantOrWingId);
        }

        if (withDmods) {
            float averageDmods = DefaultFleetInflater.getAverageDmodsForQuality(quality);
            int addDmods = DefaultFleetInflater.getNumDModsToAdd(member.getVariant(), averageDmods, itemGenRandom);
            if (addDmods > 0) {
                DModManager.setDHull(member.getVariant());
                DModManager.addDMods(member, true, addDmods, itemGenRandom);
            }
        }

        member.getRepairTracker().setMothballed(true);
        member.getRepairTracker().setCR(0.5f);
//		assignShipName(member, submarket.getFaction().getId());
        getCargo().getMothballedShips().addFleetMember(member);
        member.setShipName(Global.getSector().getFaction(SotfIDs.DUSTKEEPERS_PROXIES).pickRandomShipName());
        return member;
    }

    @Override
    public int getStockpileLimit(CommodityOnMarketAPI com) {
//		int demand = com.getMaxDemand();
//		int available = com.getAvailable();
//
//		float limit = BaseIndustry.getSizeMult(available) - BaseIndustry.getSizeMult(Math.max(0, demand - 2));
//		limit *= com.getCommodity().getEconUnit();

        //limit *= com.getMarket().getStockpileMult().getModifiedValue();

        float limit = OpenMarketPlugin.getBaseStockpileLimit(com);

        Random random = new Random(market.getId().hashCode() + submarket.getSpecId().hashCode() + Global.getSector().getClock().getMonth() * 170000);
        limit *= 0.9f + 0.2f * random.nextFloat();

        float sm = market.getStabilityValue() / 10f;
        limit *= (0.25f + 0.75f * sm);

        // more fuel and supplies
        if (com.getId().equals(Commodities.SUPPLIES)) {
            limit *= 2f;
        } else if (com.isFuel()) {
            limit *= 3f;
        }

        if (limit < 0) limit = 0;

        return (int) limit;
    }

    @Override
    public boolean isIllegalOnSubmarket(String commodityId, TransferAction action) {
//		if (market.hasCondition(Conditions.FREE_PORT)) return false;
//		//return market.isIllegal(commodityId);
//		return submarket.getFaction().isIllegal(commodityId);
        return HUMAN_COMMODITIES.contains(commodityId);
    }

    // can't buy ships if no Automated Ships skill
    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
        return action == TransferAction.PLAYER_BUY && Misc.isAutomated(member) && Misc.isUnboardable(member);
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        if (action == TransferAction.PLAYER_BUY) {
            return "Untrained in Automated Ships";
        } else {
            if (isFreeTransfer()) {
                return "Illegal to store";
            }
            return "Illegal to sell";
        }
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        return "Cannot trade personnel here";
    }

    @Override
    public boolean shouldHaveCommodity(CommodityOnMarketAPI com) {
        return !market.isIllegal(com) && !HUMAN_COMMODITIES.contains(com.getId());
    }
}
