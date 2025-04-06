// adds a little portrait icon to Dustkeeper chips
package data.scripts.campaign.plugins.dustkeepers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.CommodityIconProvider;
import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker;
import com.fs.starfarer.api.impl.campaign.econ.impl.InstallableItemEffect;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.abilities.SotfCourserFIDPluginImpl;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.plugins.amemory.SotfAMemoryBCPImpl;
import data.scripts.campaign.plugins.amemory.SotfAMemoryFIDPluginImpl;
import data.scripts.campaign.plugins.sierra.SotfSierraOfficerPlugin;
import data.scripts.campaign.plugins.waywardstar.SotfWSBCPImpl;
import data.scripts.plugins.SotfBattleCreationPluginImpl;
import data.scripts.utils.SotfMisc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SotfDustkeeperChipIconProvider implements CommodityIconProvider {

	public static final Map<String, String> PORTRAIT_MINIS = new HashMap<>();
	static {
		PORTRAIT_MINIS.put(SotfPeople.NIGHTINGALE, Global.getSettings().getSpriteName("sotf_characters", "nightingale_mini"));
		PORTRAIT_MINIS.put(SotfPeople.BARROW, Global.getSettings().getSpriteName("sotf_characters", "barrow_mini"));
		PORTRAIT_MINIS.put(SotfPeople.SERAPH, Global.getSettings().getSpriteName("sotf_characters", "seraph_mini"));
		PORTRAIT_MINIS.put(SotfPeople.SLIVER_1, Global.getSettings().getSpriteName("sotf_dustkeepers", "red_mini"));
		PORTRAIT_MINIS.put(SotfPeople.SLIVER_2, Global.getSettings().getSpriteName("sotf_dustkeepers", "yellow_mini"));
		PORTRAIT_MINIS.put(SotfPeople.ECHO_1, Global.getSettings().getSpriteName("sotf_dustkeepers", "white_mini"));
		PORTRAIT_MINIS.put(SotfPeople.ECHO_2, Global.getSettings().getSpriteName("sotf_dustkeepers", "blue_mini"));
	}

	public SotfDustkeeperChipIconProvider() {
		super();
		Global.getSector().getListenerManager().addListener(this);
	}

	public int getHandlingPriority(Object params) {
		if (params instanceof PlayerFleetPersonnelTracker.CommodityIconProviderWrapper) {
			CargoStackAPI stack = ((PlayerFleetPersonnelTracker.CommodityIconProviderWrapper) params).stack;
			if (stack.getCommodityId().contains("sotf_ichip")) {
				return GenericPluginManagerAPI.MOD_SPECIFIC;
			}
		}
		return -1;
	}

	public String getRankIconName(CargoStackAPI stack) {
		if (!SotfModPlugin.WATCHER) return null;
		switch (stack.getCommodityId()) {
			case SotfIDs.NIGHTINGALE_CHIP:
				return PORTRAIT_MINIS.get(SotfPeople.NIGHTINGALE);
			case SotfIDs.BARROW_CHIP_D:
			case SotfIDs.BARROW_CHIP:
				return PORTRAIT_MINIS.get(SotfPeople.BARROW);
			case SotfIDs.SERAPH_CHIP:
				return PORTRAIT_MINIS.get(SotfPeople.SERAPH);
			case SotfIDs.SLIVER_CHIP_1:
				return PORTRAIT_MINIS.get(SotfPeople.SLIVER_1);
			case SotfIDs.SLIVER_CHIP_2:
				return PORTRAIT_MINIS.get(SotfPeople.SLIVER_2);
			case SotfIDs.ECHO_CHIP_1:
				return PORTRAIT_MINIS.get(SotfPeople.ECHO_1);
			case SotfIDs.ECHO_CHIP_2:
				return PORTRAIT_MINIS.get(SotfPeople.ECHO_2);
		}
		return null;
	}

	public String getIconName(CargoStackAPI stack) {
		return null;
	}

}








