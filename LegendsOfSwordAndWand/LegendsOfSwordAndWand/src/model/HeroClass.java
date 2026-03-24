package model;

public enum HeroClass {
    ORDER, CHAOS, WARRIOR, MAGE,
    PRIEST,   // Order specialization
    INVOKER,  // Chaos specialization
    KNIGHT,   // Warrior specialization
    WIZARD,   // Mage specialization
    HERETIC,  // Order + Chaos hybrid
    PALADIN,  // Order + Warrior hybrid
    PROPHET,  // Order + Mage hybrid
    ROGUE,    // Chaos + Warrior hybrid
    SORCERER, // Chaos + Mage hybrid
    WARLOCK   // Warrior + Mage hybrid
}
