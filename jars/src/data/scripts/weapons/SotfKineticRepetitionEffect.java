package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;

public class SotfKineticRepetitionEffect implements OnFireEffectPlugin {

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        engine.addPlugin(new SotfKineticRepetitionGuidanceScript(projectile, null));
    }
}
