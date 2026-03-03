package com.legends.progression;

import com.legends.model.ClassProgression;
import com.legends.model.Hero;
import com.legends.model.HeroClass;
import com.legends.model.ability.Ability;
import com.legends.model.ability.AbilityFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Façade over all hero progression concerns.
 *
 * Hides the internal complexity of:
 *   - LevelUpManager  (stat application + XP threshold checking)
 *   - ClassTree       (specialisation and hybrid resolution)
 *   - AbilityRegistry (ability assignment on class change)
 *   - StatFormula     (growth math)
 *
 * Each inner class has a single responsibility and callers depend only
 * on this façade interface.
 */
public class HeroProgressionSystem implements IProgressionService {

    private final IStatFormula   formula;
    private final ClassTree      classTree;
    private final AbilityRegistry abilityRegistry;

    public HeroProgressionSystem() {
        this.formula         = new StatFormula();
        this.classTree       = new ClassTree();
        this.abilityRegistry = new AbilityRegistry();
    }

    // ── IProgressionService ───────────────────────────────────────────────────

    /**
     * Award XP to a hero and trigger level-up if the threshold is crossed.
     *
     * @return A log string describing what happened (level-up or not).
     */
    @Override
    public String awardXp(Hero hero, int xp) {
        boolean levelled = hero.addExperience(xp);
        if (levelled) {
            String classChange = checkClassChange(hero);
            return hero.getName() + " gained " + xp + " XP and reached level "
                    + hero.getLevel() + "!" + classChange;
        }
        return hero.getName() + " gained " + xp + " XP.";
    }

    /**
     * Preview the stat gains a hero would receive from levelling up in a given class.
     * Used by LevelUpView to let the player see the numbers before committing.
     */
    @Override
    public StatPreview previewLevelUp(Hero hero, HeroClass cls) {
        return new StatPreview(
                formula.atkGrowth(cls),
                formula.defGrowth(cls),
                formula.hpGrowth(cls),
                formula.manaGrowth(cls),
                classTree.getSpecialisation(cls),
                classTree.willHybridise(hero.getClassProgression(), cls)
        );
    }

    /**
     * XP still needed before this hero levels up.
     */
    @Override
    public int xpToNextLevel(Hero hero) {
        return Math.max(0, formula.cumulativeXpForLevel(hero.getLevel()) - hero.getExperience());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Check whether the hero's class changed after a level-up and apply ability updates.
     * Returns a description of any class change.
     */
    private String checkClassChange(Hero hero) {
        ClassProgression prog = hero.getClassProgression();
        HeroClass current = hero.getHeroClass();
        // ClassProgression.recordLevelIn() is already called inside Hero.levelUp()
        // so we just check if the class stored there now differs
        if (prog.isHybrid() && current != classTree.resolveHybrid(
                prog.getPrimaryClass(), prog.getSecondaryClass())) {
            List<Ability> newAbilities = abilityRegistry.getAbilities(hero.getHeroClass());
            return " Hybrid class unlocked: " + hero.getHeroClass().displayName + "!";
        }
        if (prog.isSpecUnlocked()) {
            return " Specialisation unlocked: " + hero.getHeroClass().displayName + "!";
        }
        return "";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Inner classes: LevelUpManager, ClassTree, AbilityRegistry
    // Each has SRP — one reason to change.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Manages class specialisation and hybrid resolution.
     *
     * SRP: Only cares about class tree logic.
     * OCP: New specialisations are added by extending ClassProgression.getSpecialisation()
     *      and this ClassTree — nothing else changes.
     */
    public static class ClassTree {

        public HeroClass getSpecialisation(HeroClass base) {
            return ClassProgression.getSpecialisation(base);
        }

        public HeroClass resolveHybrid(HeroClass a, HeroClass b) {
            return ClassProgression.resolveHybrid(a, b);
        }

        /**
         * Returns true if levelling in {@code cls} would trigger hybridisation
         * given the hero's current progression.
         */
        public boolean willHybridise(ClassProgression prog, HeroClass cls) {
            if (prog.isHybrid()) return false;
            int[] levels = prog.getClassLevels();
            // Would adding a level in cls cause TWO base classes to both have >= 5?
            int future = (cls.isBaseClass() ? levels[cls.ordinal()] + 1 : 0);
            int qualified = 0;
            for (int i = 0; i < 4; i++) {
                int effective = (HeroClass.values()[i] == cls) ? future : levels[i];
                if (effective >= 5) qualified++;
            }
            return qualified >= 2;
        }
    }

    /**
     * Maps hero classes to their ability sets.
     *
     * SRP: Only cares about which abilities belong to which class.
     * OCP: New classes map to new abilities by adding a case — no existing code changes.
     */
    public static class AbilityRegistry {

        public List<Ability> getAbilities(HeroClass cls) {
            List<Ability> result = new ArrayList<>();
            switch (cls) {
                case ORDER   -> { result.add(AbilityFactory.create(AbilityFactory.Type.PROTECT));
                                  result.add(AbilityFactory.create(AbilityFactory.Type.HEAL)); }
                case CHAOS   -> { result.add(AbilityFactory.create(AbilityFactory.Type.FIREBALL));
                                  result.add(AbilityFactory.create(AbilityFactory.Type.CHAIN_LIGHTNING)); }
                case WARRIOR -> result.add(AbilityFactory.create(AbilityFactory.Type.BERSERKER_ATTACK));
                case MAGE    -> result.add(AbilityFactory.create(AbilityFactory.Type.REPLENISH));
                // Hybrid / spec: inherits from primary class (could be extended here)
                default      -> {}
            }
            return result;
        }
    }

    // ── Value object returned by previewLevelUp ───────────────────────────────

    /**
     * Immutable preview of what a level-up in a given class would yield.
     * Displayed by LevelUpView before the player commits.
     */
    public record StatPreview(
            int atkGain,
            int defGain,
            int hpGain,
            int manaGain,
            HeroClass possibleSpec,
            boolean willHybridise
    ) {}
}
