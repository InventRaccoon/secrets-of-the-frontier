// used to remove all salvage options other than recovering player ships
package data.scripts.campaign.plugins.amemory;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;

import java.util.*;

public class SotfAMemoryFEContext extends FleetEncounterContext {

    protected void generatePlayerLoot(List<FleetMemberAPI> recoveredShips, boolean withCredits) {
        DataForEncounterSide winner = getWinnerData();
        for (FleetMemberData data : winner.getOwnCasualties()) {
            if (data.getMember().isAlly()) continue;

            if (data.getStatus() == Status.CAPTURED || data.getStatus() == Status.REPAIRED) {
                continue;
            }
            float mult = getSalvageMult(data.getStatus());
            lootWeapons(data.getMember(), data.getMember().getVariant(), true, mult, false);
            lootWings(data.getMember(), data.getMember().getVariant(), true, mult);
        }
        handleCargoLooting(recoveredShips, false);
    }
}
