package data.scripts.combat.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandListener;

import java.util.Locale;

public class SotfFelCommandListener implements CommandListener {

    @Override
    public boolean onPreExecute(@NotNull String command, @NotNull String args, @NotNull BaseCommand.CommandContext context, boolean alreadyIntercepted) {
        return false;
    }

    @Override
    public BaseCommand.CommandResult execute(@NotNull String command, @NotNull String args, @NotNull BaseCommand.CommandContext context) {
        return null;
    }

    @Override
    public void onPostExecute(@NotNull String command, @NotNull String args, @NotNull BaseCommand.CommandResult result, @NotNull BaseCommand.CommandContext context, @Nullable CommandListener interceptedBy) {
        if (Global.getCombatEngine() == null) return;
        if ((command.toLowerCase(Locale.ROOT).equals("nuke") || command.toLowerCase(Locale.ROOT).equals("god")) && Global.getCombatEngine().getCustomData().containsKey("sotf_fel_fight")) {
            Global.getCombatEngine().getCombatUI().addMessage(0, Misc.getNegativeHighlightColor(), "Coward.");
        }
    }
}
