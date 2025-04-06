/*
    By Tartiflette
 */

/**
 *	Raccoon's Note: modified version provided by Selkie with higher refresh rate than default MagicVectorThruster
 *  Also: reduced engine wobble
 *  Used for Respite/Anamnesis vector thrusters
 */
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.awt.Color;
import java.util.HashMap;

import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
//import data.scripts.plugins.SpriteRenderManager;

public class SotfMagicVectorThrusterSelkie implements EveryFrameWeaponEffectPlugin {
    
    private boolean runOnce=false, accel=false, turn=false;
    private ShipAPI SHIP;
    private HashMap<Integer, ThrusterData> thrusters;
    private ShipEngineControllerAPI ENGINES;
    private float previousThrust = 0;

    private float MAX_THRUST_CHANGE_PER_SECOND = 0;
    private float MAX_ANGLE_CHANGE_PER_SECOND = 0;
    private float TURN_RIGHT_ANGLE = 0, THRUST_TO_TURN = 0, NEUTRAL_ANGLE = 0, OFFSET = 0;
    private float glowCompensation = 1f;
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        
        if(!runOnce){
            runOnce=true;
            
            SHIP = weapon.getShip();
            ENGINES = SHIP.getEngineController();
            ENGINES.forceShowAccelerating();
            thrusters = new HashMap<>();
            //find the ship engine associated with the deco thruster
            for (ShipEngineAPI e : SHIP.getEngineController().getShipEngines()) {
                if (MathUtils.isWithinRange(e.getLocation(), weapon.getLocation(), 2)) {
                    ThrusterData t = new ThrusterData(e);
                    thrusters.put(MathUtils.getRandom().nextInt(),t);
                }
            }

            //desync the engines wobble
            OFFSET = (float) (Math.random() * MathUtils.FPI);

            //"rest" angle when not in use
            NEUTRAL_ANGLE = weapon.getSlot().getAngle();
            //ideal aim angle to rotate the ship (allows free-form placement on the hull)
            TURN_RIGHT_ANGLE = MathUtils.clampAngle(VectorUtils.getAngle(SHIP.getLocation(), weapon.getLocation()));
            TURN_RIGHT_ANGLE = MathUtils.getShortestRotation(SHIP.getFacing(), TURN_RIGHT_ANGLE) + 90;
            //is the thruster performant at turning the ship? Engines closer to the center of mass will concentrate more on dealing with changes of velocity.
            THRUST_TO_TURN = smooth(MathUtils.getDistance(SHIP.getLocation(), weapon.getLocation()) / SHIP.getCollisionRadius());

            MAX_ANGLE_CHANGE_PER_SECOND = weapon.getTurnRate();
            MAX_THRUST_CHANGE_PER_SECOND = weapon.getDerivedStats().getDps()/100f;
        }

        if(engine.isPaused() || SHIP.getOriginalOwner() == -1){
            return;
        }

        //check for death/engine disabled
        boolean dead = true;
        for (ThrusterData e : thrusters.values()) {
            if (e.engine.isActive()) dead = false;
        }
        if (!SHIP.isAlive() || dead) {
            previousThrust = 0;
            return;
        }

        //check what the ship is doing
        float accelerateAngle = NEUTRAL_ANGLE;
        float turnAngle = NEUTRAL_ANGLE;
        float thrust = 0;

        if (!ENGINES.isAccelerating() && (ENGINES.isStrafingLeft() || ENGINES.isStrafingRight() || ENGINES.isAcceleratingBackwards())) {
            glowCompensation -= amount * 1.5f;
            if (glowCompensation < 0.5) glowCompensation = 0.5f;
        }else if (ENGINES.isAccelerating()) {
            glowCompensation += amount*1.5f;
            if (glowCompensation > 1) glowCompensation = 1;
        } else{
            glowCompensation += amount * 1.5f;
            if (glowCompensation > 1.5) glowCompensation = 1.5f;
        }

        if (ENGINES.isAccelerating()) {
            accelerateAngle = 180;
            thrust = 1.5f;
            accel = true;
        } else if (ENGINES.isAcceleratingBackwards()) {
            accelerateAngle = 0;
            thrust = 1.5f;
            accel = true;
        } else if (ENGINES.isDecelerating()) {
            accelerateAngle = NEUTRAL_ANGLE;
            thrust = 0.5f;
            accel = true;
        } else {
            accel = false;
        }

        if (ENGINES.isStrafingLeft()) {
            if (thrust == 0) {
                accelerateAngle = -90;
            } else {
                accelerateAngle = MathUtils.getShortestRotation(accelerateAngle, -90) / 2 + accelerateAngle;
            }
            thrust = Math.max(1, thrust);
            accel = true;
        } else if (ENGINES.isStrafingRight()) {
            if (thrust == 0) {
                accelerateAngle = 90;
            } else {
                accelerateAngle = MathUtils.getShortestRotation(accelerateAngle, 90) / 2 + accelerateAngle;
            }
            thrust = Math.max(1, thrust);
            accel = true;
        }

        if (ENGINES.isTurningRight()) {
            turnAngle = TURN_RIGHT_ANGLE;
            thrust = Math.max(1, thrust);
            turn = true;
        } else if (ENGINES.isTurningLeft()) {
            turnAngle = MathUtils.clampAngle(180 + TURN_RIGHT_ANGLE);
            thrust = Math.max(1, thrust);
            turn = true;
        } else {
            turn = false;
        }
            
            //calculate the corresponding vector thrusting            
            if(thrust>0){
                SHIP.getEngineController().forceShowAccelerating();

                //DEBUG
                Vector2f offset = new Vector2f(weapon.getLocation().x-SHIP.getLocation().x,weapon.getLocation().y-SHIP.getLocation().y);
                VectorUtils.rotate(offset, -SHIP.getFacing(), offset);

                float thrustChangeModifier = (SHIP.getMutableStats().getTurnAcceleration().computeMultMod() + SHIP.getMutableStats().getAcceleration().computeMultMod()) / 2;

                if(!turn){
                    thrust(weapon, accelerateAngle, thrustChangeModifier, amount);
                } else {
                    if (!accel) {
                        //turn only, easy too.
                        thrust(weapon, turnAngle, thrustChangeModifier, amount);

                    } else {
                        //start from the neutral angle
                        float combinedAngle = NEUTRAL_ANGLE;

                        //adds both thrust and turn angle at their respective thrust-to-turn ratio. Gives a "middleground" angle
                        combinedAngle = MathUtils.clampAngle(combinedAngle + MathUtils.getShortestRotation(NEUTRAL_ANGLE, accelerateAngle));
                        combinedAngle = MathUtils.clampAngle(combinedAngle + THRUST_TO_TURN * MathUtils.getShortestRotation(accelerateAngle, turnAngle));

                        thrust(weapon, combinedAngle, thrustChangeModifier, amount);
                    }
                }
            } else {
                thrust(weapon, NEUTRAL_ANGLE, 0, amount);
            }
    }

    public boolean isAngleWithinArc(float startAngle, float endAngle, float testAngle) {
        startAngle = MathUtils.clampAngle(startAngle);
        endAngle = MathUtils.clampAngle(endAngle);
        testAngle = MathUtils.clampAngle(testAngle);

        float diff_ccw;
        if (startAngle <= endAngle)
            diff_ccw = endAngle - startAngle;
        else
            diff_ccw = (360 - startAngle) + endAngle;

        if (diff_ccw > 180) {
            if (startAngle >= endAngle)
                return testAngle <= startAngle && testAngle >= endAngle;
            else
                return testAngle <= startAngle || testAngle >= endAngle;
        }
        else {
            if (startAngle <= endAngle)
                return testAngle >= startAngle && testAngle <= endAngle;
            else
                return testAngle >= startAngle || testAngle <= endAngle;
        }
    }
    
    private void thrust(WeaponAPI weapon, float angle, float thrustChangeModifier, float amount){
        //target angle
        float optimalAim = angle + SHIP.getFacing();

        //how far from the target angle the engine is aimed at

        float aimDirection = MathUtils.getShortestRotation(weapon.getCurrAngle(), optimalAim);

        //Global.getCombatEngine().addSmoothParticle(MathUtils.getPointOnCircumference(weapon.getLocation(), 100, weapon.getCurrAngle()), SHIP.getVelocity(), 20f, 100f,0.1f, Color.red);
        //Global.getCombatEngine().addSmoothParticle(MathUtils.getPointOnCircumference(weapon.getLocation(), 30, weapon.getArcFacing() + SHIP.getFacing()+ 180f), SHIP.getVelocity(), 20f, 100f,0.1f, Color.magenta);
        //Global.getCombatEngine().addSmoothParticle(MathUtils.getPointOnCircumference(weapon.getLocation(), 80, angle + SHIP.getFacing()), SHIP.getVelocity(), 20f, 100f,0.1f, Color.green);

        //thrust is reduced while the engine isn't facing the target angle

        float targetThrust = MathUtils.clamp((1 - (Math.abs(aimDirection) / 90)),0, 1);
        if (thrustChangeModifier == 0) targetThrust = 0;

        float currentThrust;
        if (previousThrust < targetThrust){
            currentThrust = previousThrust + amount * MAX_THRUST_CHANGE_PER_SECOND;
            if (currentThrust > targetThrust)
                currentThrust = targetThrust;
        }
        else {
            currentThrust = previousThrust - amount * MAX_THRUST_CHANGE_PER_SECOND * 2.5f;
            if (currentThrust < targetThrust)
                currentThrust = targetThrust;
        }
        currentThrust = MathUtils.clamp(currentThrust, 0, 1);
        previousThrust = currentThrust;

        //engine wobble
        //float targetAim = optimalAim + ((Math.abs(aimDirection) < 10f) ? (float) (2 * FastTrig.cos(SHIP.getFullTimeDeployed() * 2 + OFFSET)) : 0f);
        aimDirection = MathUtils.getShortestRotation(weapon.getCurrAngle(), optimalAim);
        if (isAngleWithinArc(weapon.getCurrAngle(), angle + SHIP.getFacing(), weapon.getArcFacing() + SHIP.getFacing() + 180))
            aimDirection = -aimDirection;

        float turnBonus = Misc.interpolate(1, 2, Math.abs(aimDirection)/180);
        if(aimDirection > 0){
            weapon.setCurrAngle(MathUtils.clampAngle(weapon.getCurrAngle() + amount * MAX_ANGLE_CHANGE_PER_SECOND * turnBonus));
        } else{
            weapon.setCurrAngle(MathUtils.clampAngle(weapon.getCurrAngle() - amount * MAX_ANGLE_CHANGE_PER_SECOND * turnBonus));
        }


        float offset = weapon.getSprite().getHeight() - weapon.getSprite().getCenterY();
        for (ThrusterData thruster : thrusters.values()) {
            EngineSlotAPI engineSlot = thruster.engine.getEngineSlot();
            float flameLevel = 0f;
            if(currentThrust > 0) flameLevel = Misc.interpolate(0.8f, 1f, currentThrust);
            SHIP.getEngineController().setFlameLevel(engineSlot, flameLevel);
            ((com.fs.starfarer.loading.specs.EngineSlot) engineSlot).setGlowParams(thruster.width * glowCompensation, thruster.length + offset,1f,1f); // no clue what v2 and v3 do
            engineSlot.setAngle(weapon.getCurrAngle() - weapon.getShip().getFacing());
            engineSlot.setGlowSizeMult(0f);

            /*
            weapon.setForceFireOneFrame(true);
            weapon.setRenderOffsetForDecorativeBeamWeaponsOnly(new Vector2f(0f,100f));
            weapon.getGlowSpriteAPI().setColor(new Color(engineSlot.getColor().getRed(), engineSlot.getColor().getGreen(),engineSlot.getColor().getBlue(), (int) Misc.interpolate(0, 255, currentThrust)));

            Color glowColor = new Color(engineSlot.getColor().getRed(), engineSlot.getColor().getGreen(),engineSlot.getColor().getBlue(), (int) Misc.interpolate(0, 255, currentThrust));

            SpriteAPI thrusterSprite = Global.getSettings().getSprite(weapon.getSpec().getTurretSpriteName());
            thrusterSprite.setAngle(weapon.getCurrAngle() - 90f);
            weapon.getSprite().setColor(new Color(0,0,0,0));
            MagicRenderPlugin.addSingleframe(thrusterSprite, weapon.getLocation(), CombatEngineLayers.CAPITAL_SHIPS_LAYER);
            //MagicRender.singleframe(Global.getSettings().getSprite(weapon.getSpec().getTurretSpriteName()), weapon.getLocation(), new Vector2f(weapon.getSprite().getWidth(), weapon.getSprite().getWidth()), weapon.getCurrAngle()-90f, glowColor, true);
            */
        }
    }

    static class ThrusterData {
        private final Float length;
        private final Float width;
        private final ShipEngineAPI engine;

        ThrusterData(ShipEngineAPI engine) {
            length = engine.getEngineSlot().getLength();
            width = engine.getEngineSlot().getWidth();
            this.engine = engine;
        }

    }
        
    //////////////////////////////////////////
    //                                      //
    //           SMOOTH DAT MOVE            //
    //                                      //
    //////////////////////////////////////////
    
    public float smooth (float x){
        return 0.5f - ((float)(Math.cos(x*MathUtils.FPI) /2 ));
    }  
}