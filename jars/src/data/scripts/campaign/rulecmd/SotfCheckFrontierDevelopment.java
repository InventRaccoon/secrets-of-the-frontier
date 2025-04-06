// checks for the Frontier Development module active
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;

import java.util.List;
import java.util.Map;

public class SotfCheckFrontierDevelopment extends BaseCommandPlugin
{
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap)
    {
        return SotfModPlugin.DEVELOPMENT;
    }
}
