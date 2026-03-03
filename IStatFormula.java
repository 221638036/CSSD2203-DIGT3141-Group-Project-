package com.legends.progression;

import com.legends.model.HeroClass;

/**
 * Interface for stat growth formulas.
 *
 * EnemyFactory only needs xpNeeded / growth methods;
 * it should not be forced to depend on a fat interface with level-up
 * orchestration. This narrow interface is all it needs.
 *
 * EnemyFactory and HeroProgressionSystem depend on this abstraction,
 * not on the concrete StatFormula class.
 */
public interface IStatFormula {
    int xpNeeded(int level);
    int cumulativeXpForLevel(int targetLevel);
    int atkGrowth(HeroClass cls);
    int defGrowth(HeroClass cls);
    int hpGrowth(HeroClass cls);
    int manaGrowth(HeroClass cls);
}
