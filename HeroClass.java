package com.legends.model;

/**
 * Enumerates the four base hero classes and their hybrid/specialisation results.
 *
 * Adding a new class requires only adding an enum constant here.
 * The old isHybrid() type-check method has been removed; ClassProgression now
 * owns hybrid resolution via resolveHybrid(), replacing type-checking with
 * polymorphism.
 */
public enum HeroClass {

    // Base classes
    ORDER  ("Order",    0, 2, 0, 5),
    CHAOS  ("Chaos",    3, 0, 5, 0),
    WARRIOR("Warrior",  2, 3, 0, 0),
    MAGE   ("Mage",     1, 0, 0, 5),

    // Level-5 specialisations
    KNIGHT    ("Knight",    4, 6, 0, 0),
    ARCHMAGE  ("Archmage",  2, 0, 2, 10),
    TEMPLAR   ("Templar",   2, 5, 3, 4),
    BERSERKER ("Berserker", 5, 1, 5, 0),

    // Hybrid classes
    CLERIC  ("Cleric",   1, 4, 2, 6),
    PALADIN ("Paladin",  2, 3, 3, 3),
    WARLOCK ("Warlock",  3, 3, 0, 5),
    SORCERER("Sorcerer", 4, 0, 5, 5);

    public final String displayName;
    public final int atkPerLevel;
    public final int defPerLevel;
    public final int hpPerLevel;
    public final int manaPerLevel;

    HeroClass(String displayName, int atk, int def, int hp, int mana) {
        this.displayName  = displayName;
        this.atkPerLevel  = atk;
        this.defPerLevel  = def;
        this.hpPerLevel   = hp;
        this.manaPerLevel = mana;
    }

    /** Returns true if this is one of the four playable base classes. */
    public boolean isBaseClass() { return ordinal() < 4; }
}
