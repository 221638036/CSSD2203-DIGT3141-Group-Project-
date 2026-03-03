package com.legends.progression;

import com.legends.model.HeroClass;

/**
 * Single source of truth for all hero stat growth formulas.
 *
 * Growth math is its own concern, separated from Hero and
 * HeroProgressionSystem for reuse by EnemyFactory.
 * Exposed via IStatFormula to restrict dependencies.
 */
public class StatFormula implements IStatFormula {

    /**
     * XP needed to advance from level {@code level} to {@code level+1}.
     * Formula: 500 + 75L + 20L²
     */
    @Override
    public int xpNeeded(int level) {
        return 500 + 75 * level + 20 * level * level;
    }

    /**
     * Cumulative XP threshold to reach a given level.
     */
    @Override
    public int cumulativeXpForLevel(int targetLevel) {
        int total = 0;
        for (int l = 1; l <= targetLevel; l++) total += xpNeeded(l);
        return total;
    }

    /**
     * Attack growth per level for a class.
     * Base +1 plus class bonus — matches the Hero.levelUp() formula.
     */
    @Override
    public int atkGrowth(HeroClass cls) {
        return 1 + cls.atkPerLevel;
    }

    /**
     * Defense growth per level for a class.
     */
    @Override
    public int defGrowth(HeroClass cls) {
        return 1 + cls.defPerLevel;
    }

    /**
     * HP growth per level for a class.
     */
    @Override
    public int hpGrowth(HeroClass cls) {
        return 5 + cls.hpPerLevel;
    }

    /**
     * Mana growth per level for a class.
     */
    @Override
    public int manaGrowth(HeroClass cls) {
        return 2 + cls.manaPerLevel;
    }
}
