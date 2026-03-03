package com.legends.progression;

import com.legends.model.Hero;
import com.legends.model.HeroClass;

/**
 * Client-facing interface for hero progression.
 *
 * Only the methods callers actually need are exposed.
 * BattleEngine needs awardXp(); LevelUpView needs previewLevelUp() and xpToNextLevel().
 * Neither needs the internal formula or class tree details.
 *
 * BattleEngine, InnManager, and PvECampaignManager depend on THIS
 * interface — not on the concrete HeroProgressionSystem class.
 */
public interface IProgressionService {

    /** Award XP to a hero; trigger and report any level-up. */
    String awardXp(Hero hero, int xp);

    /** Preview the stat gains from levelling up in a given class (for UI). */
    HeroProgressionSystem.StatPreview previewLevelUp(Hero hero, HeroClass cls);

    /** XP remaining before the hero's next level-up. */
    int xpToNextLevel(Hero hero);
}
