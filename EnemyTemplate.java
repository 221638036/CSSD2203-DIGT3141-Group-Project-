package com.legends.model;

import java.util.List;

/**
 * Lightweight value object describing a generated enemy.
 *
 * Enemy data is a separate concern from player Hero data.
 * Enemies do not have class progression, abilities, or inventory.
 */
public class EnemyTemplate {

    private final int level;
    private final int scaledAtk;
    private final int scaledDef;
    private final int scaledHp;

    /** Enemies may only ATTACK, DEFEND, or WAIT — never CAST. */
    private static final List<String> ALLOWED_ACTIONS = List.of("ATTACK", "DEFEND", "WAIT");

    public EnemyTemplate(int level, int scaledAtk, int scaledDef, int scaledHp) {
        this.level     = level;
        this.scaledAtk = scaledAtk;
        this.scaledDef = scaledDef;
        this.scaledHp  = scaledHp;
    }

    /** Materialise this template into a fully initialised Hero (enemy). */
    public Hero toHero(String name) {
        return Hero.createEnemy(name, level, scaledAtk, scaledDef, scaledHp);
    }

    public int getLevel()     { return level; }
    public int getScaledAtk() { return scaledAtk; }
    public int getScaledDef() { return scaledDef; }
    public int getScaledHp()  { return scaledHp; }
    public List<String> getAllowedActions() { return ALLOWED_ACTIONS; }

    @Override
    public String toString() {
        return String.format("EnemyTemplate[Lv%d atk=%d def=%d hp=%d]",
                level, scaledAtk, scaledDef, scaledHp);
    }
}
