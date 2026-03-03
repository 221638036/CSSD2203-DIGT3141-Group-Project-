package com.legends.model;

/**
 * Tracks a hero's class-level progression history.
 *
 * Progression state is a separate concern from
 * hero identity and combat stats.
 *
 * This is serialised as extra columns on the hero DB row;
 * no new table is required.
 */
public class ClassProgression {

    /** Levels accumulated in each of the 4 base classes (indexed by HeroClass ordinal). */
    private final int[] classLevels = new int[4];

    /** Whether a level-5 specialisation has been unlocked for the current class. */
    private boolean specUnlocked;

    /** Whether this hero has fused two specs into a hybrid class. */
    private boolean isHybrid;

    /** The first base class (always set). */
    private HeroClass primaryClass;

    /** The second base class that contributed to the hybrid (null until hybridised). */
    private HeroClass secondaryClass;

    public ClassProgression(HeroClass startingClass) {
        this.primaryClass = startingClass;
        if (startingClass.ordinal() < 4) {
            classLevels[startingClass.ordinal()] = 1;
        }
    }

    // ── Mutation ──────────────────────────────────────────────────────────────

    /**
     * Record a level gained in the given base class.
     * Returns the resulting class (may change to specialisation or hybrid).
     */
    public HeroClass recordLevelIn(HeroClass cls) {
        if (cls.ordinal() < 4) {
            classLevels[cls.ordinal()]++;
        }

        // Hybrid: two distinct base classes both reach level 5
        if (!isHybrid) {
            long qualified = 0;
            int firstIdx = -1, secondIdx = -1;
            for (int i = 0; i < 4; i++) {
                if (classLevels[i] >= 5) {
                    qualified++;
                    if (firstIdx == -1) firstIdx = i;
                    else secondIdx = i;
                }
            }
            if (qualified >= 2 && secondIdx != -1) {
                isHybrid = true;
                secondaryClass = HeroClass.values()[secondIdx];
                return resolveHybrid(HeroClass.values()[firstIdx],
                                     HeroClass.values()[secondIdx]);
            }
        }

        // Specialisation: primary class reaches level 5
        if (!specUnlocked && cls == primaryClass && classLevels[cls.ordinal()] >= 5) {
            specUnlocked = true;
            return getSpecialisation(primaryClass);
        }

        return cls;
    }

    /**
     * Determine the specialisation for a base class that reached level 5.
     * New specialisations can be added to HeroClass enum without changing this logic.
     */
    public static HeroClass getSpecialisation(HeroClass base) {
        return switch (base) {
            case WARRIOR -> HeroClass.KNIGHT;
            case MAGE    -> HeroClass.ARCHMAGE;
            case ORDER   -> HeroClass.TEMPLAR;
            case CHAOS   -> HeroClass.BERSERKER;
            default      -> base;
        };
    }

    /**
     * Resolve which hybrid class is produced by combining two base-class specs.
     */
    public static HeroClass resolveHybrid(HeroClass a, HeroClass b) {
        // Canonical order: lower ordinal first
        if (a.ordinal() > b.ordinal()) { HeroClass tmp = a; a = b; b = tmp; }
        return switch (a) {
            case ORDER -> switch (b) {
                case CHAOS   -> HeroClass.CLERIC;
                case WARRIOR -> HeroClass.PALADIN;
                case MAGE    -> HeroClass.ARCHMAGE;
                default -> a;
            };
            case CHAOS -> switch (b) {
                case WARRIOR -> HeroClass.BERSERKER;
                case MAGE    -> HeroClass.SORCERER;
                default -> a;
            };
            case WARRIOR -> HeroClass.WARLOCK; // WARRIOR + MAGE
            default -> a;
        };
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    public int getLevelsIn(HeroClass cls) {
        return cls.ordinal() < 4 ? classLevels[cls.ordinal()] : 0;
    }

    public boolean isSpecUnlocked()   { return specUnlocked; }
    public boolean isHybrid()         { return isHybrid; }
    public HeroClass getPrimaryClass()   { return primaryClass; }
    public HeroClass getSecondaryClass() { return secondaryClass; }
    public int[] getClassLevels()     { return classLevels.clone(); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Progression[");
        HeroClass[] base = {HeroClass.ORDER, HeroClass.CHAOS, HeroClass.WARRIOR, HeroClass.MAGE};
        for (HeroClass c : base) {
            if (classLevels[c.ordinal()] > 0)
                sb.append(c.displayName).append(":").append(classLevels[c.ordinal()]).append(" ");
        }
        if (specUnlocked) sb.append("(spec) ");
        if (isHybrid) sb.append("(hybrid) ");
        sb.append("]");
        return sb.toString();
    }
}
