package com.legends.model;

import com.legends.model.ability.Ability;
import com.legends.model.ability.AbilityFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Core domain class representing a hero's identity, combat stats and abilities.
 *
 * SRP: Hero is responsible ONLY for:
 *   - Storing identity (name, class, level)
 *   - Storing and mutating combat stats (HP, mana, atk, def, shield)
 *   - Performing combat actions (takeDamage, heal, defend, cast)
 *
 * Class progression logic lives in ClassProgression.
 * Enemy creation lives in EnemyFactory.
 * Score calculation lives in ProfileManager.
 *
 * High Cohesion (GRASP): every method here uses Hero's own fields.
 * Low Coupling (GRASP): Hero has no dependency on services or repositories.
 */
public class Hero {

    // ── Identity ──────────────────────────────────────────────────────────────
    private String    name;
    private HeroClass heroClass;
    private int       level;

    // ── Base stat constants ───────────────────────────────────────────────────
    private static final int BASE_ATK  = 5;
    private static final int BASE_DEF  = 5;
    private static final int BASE_HP   = 100;
    private static final int BASE_MANA = 50;

    // ── Stats ─────────────────────────────────────────────────────────────────
    private int attack;
    private int defense;
    private int maxHp;
    private int currentHp;
    private int maxMana;
    private int currentMana;
    private int shield;
    private int experience;
    private boolean stunned;

    // ── Abilities (strategy pattern) ─────────────────────────────────
    private List<Ability> abilities;

    // ── Class progression ─────────────────────────────────────────────────────
    /** Separated into its own object (SRP). */
    private ClassProgression classProgression;

    // ─────────────────────────────────────────────────────────────────────────

    /** Construct a brand-new level-1 hero. */
    public Hero(String name, HeroClass heroClass) {
        this.name     = name;
        this.heroClass = heroClass;
        this.level    = 1;
        this.attack   = BASE_ATK;
        this.defense  = BASE_DEF;
        this.maxHp    = BASE_HP;
        this.currentHp = BASE_HP;
        this.maxMana   = BASE_MANA;
        this.currentMana = BASE_MANA;
        this.shield      = 0;
        this.experience  = 0;
        this.stunned     = false;
        this.abilities   = new ArrayList<>();
        this.classProgression = new ClassProgression(heroClass);
        assignAbilities(heroClass);
    }

    /** Reconstruct a hero from persisted data (e.g., loaded from DB). */
    public Hero(String name, HeroClass heroClass, int level,
                int attack, int defense, int maxHp, int currentHp,
                int maxMana, int currentMana, int experience) {
        this(name, heroClass);
        this.level       = level;
        this.attack      = attack;
        this.defense     = defense;
        this.maxHp       = maxHp;
        this.currentHp   = currentHp;
        this.maxMana     = maxMana;
        this.currentMana = currentMana;
        this.experience  = experience;
    }

    // ── Level-up ──────────────────────────────────────────────────────────────

    /**
     * Level up in the specified class.
     * Applies base growth (+1 atk, +1 def, +5 hp, +2 mana) plus class bonus.
     * Delegates progression tracking to ClassProgression (SRP).
     *
     * @param cls The class being levelled — may differ from current heroClass
     *            if the player is multi-classing.
     * @return The resulting HeroClass after any spec/hybrid resolution.
     */
    public HeroClass levelUp(HeroClass cls) {
        level++;
        attack      += 1 + cls.atkPerLevel;
        defense     += 1 + cls.defPerLevel;
        maxHp       += 5 + cls.hpPerLevel;
        currentHp    = Math.min(currentHp + 5 + cls.hpPerLevel, maxHp);
        maxMana     += 2 + cls.manaPerLevel;
        currentMana  = Math.min(currentMana + 2 + cls.manaPerLevel, maxMana);

        HeroClass resolved = classProgression.recordLevelIn(cls);
        if (resolved != heroClass) {
            heroClass = resolved;
            assignAbilities(resolved);
        }
        return resolved;
    }

    /**
     * XP threshold for the next level: Exp(L) = sum_{l=1}^{L} (500 + 75l + 20l²).
     */
    public int xpForNextLevel() {
        int total = 0;
        for (int l = 1; l <= level; l++) total += 500 + 75 * l + 20 * l * l;
        return total;
    }

    /**
     * Add experience and auto-level up if threshold crossed.
     *
     * @return true if a level-up occurred.
     */
    public boolean addExperience(int xp) {
        experience += xp;
        if (level < 20 && experience >= xpForNextLevel()) {
            levelUp(heroClass);
            return true;
        }
        return false;
    }

    // ── Combat ────────────────────────────────────────────────────────────────

    /** Apply damage: shield absorbs first, then HP. */
    public void takeDamage(int dmg) {
        if (dmg <= 0) return;
        if (shield > 0) {
            if (shield >= dmg) { shield -= dmg; return; }
            dmg   -= shield;
            shield = 0;
        }
        currentHp = Math.max(0, currentHp - dmg);
    }

    /** Heal, capped at maxHp. */
    public void heal(int amount) {
        currentHp = Math.min(maxHp, currentHp + amount);
    }

    /** Revive to 1 HP (used after a lost battle). */
    public void revive() {
        if (currentHp <= 0) currentHp = 1;
    }

    /** Fully restore HP and mana (inn visit). */
    public void fullyRestore() {
        currentHp   = maxHp;
        currentMana = maxMana;
        shield      = 0;
    }

    /** Add a temporary shield value. */
    public void addShield(int amount) { shield += amount; }

    /** Restore mana, capped at maxMana. */
    public void restoreMana(int amount) {
        currentMana = Math.min(maxMana, currentMana + amount);
    }

    /** Defend action: +10 HP, +5 mana (GRASP Information Expert). */
    public void defend() {
        heal(10);
        restoreMana(5);
    }

    /** Raw attack damage against a defender (atk − def, min 0). */
    public int calculateDamage(Hero defender) {
        return Math.max(0, this.attack - defender.defense);
    }

    public boolean isAlive()   { return currentHp > 0; }
    public boolean isStunned() { return stunned; }
    public void setStunned(boolean v) { this.stunned = v; }
    public void clearStun()    { this.stunned = false; }

    // ── Abilities (Strategy pattern) ──────────────────────────────────────────

    private void assignAbilities(HeroClass cls) {
        abilities.clear();
        // OCP: switch on enum; adding a new class enum constant only requires
        // adding a new case here.
        switch (cls) {
            case ORDER    -> { abilities.add(AbilityFactory.create(AbilityFactory.Type.PROTECT));
                               abilities.add(AbilityFactory.create(AbilityFactory.Type.HEAL)); }
            case CHAOS    -> { abilities.add(AbilityFactory.create(AbilityFactory.Type.FIREBALL));
                               abilities.add(AbilityFactory.create(AbilityFactory.Type.CHAIN_LIGHTNING)); }
            case WARRIOR  -> abilities.add(AbilityFactory.create(AbilityFactory.Type.BERSERKER_ATTACK));
            case MAGE     -> abilities.add(AbilityFactory.create(AbilityFactory.Type.REPLENISH));
            // Hybrids & specs inherit from their base class by retaining existing list
            default       -> {}
        }
    }

    public List<Ability> getAbilities() { return abilities; }

    /**
     * Attempt to use an ability. Returns a result string or an error message.
     */
    public String useAbility(int abilityIndex, List<Hero> targets) {
        if (abilityIndex < 0 || abilityIndex >= abilities.size())
            return "Invalid ability index.";
        Ability ab = abilities.get(abilityIndex);
        if (currentMana < ab.getManaCost()) return "Not enough mana!";
        currentMana -= ab.getManaCost();
        return ab.execute(this, targets);
    }

    // ── Package-level factory used ONLY by EnemyFactory ──────────────────────

    /**
     * Creates an enemy hero with pre-scaled stats.
     * Intentionally package-private: only EnemyFactory should call this,
     * enforcing SRP and Low Coupling.
     */
    static Hero createEnemy(String name, int level, int atk, int def, int hp) {
        Hero e = new Hero(name, HeroClass.WARRIOR);
        e.level      = level;
        e.attack     = atk;
        e.defense    = def;
        e.maxHp      = hp;
        e.currentHp  = hp;
        e.abilities.clear(); // enemies never cast
        return e;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String    getName()             { return name; }
    public HeroClass getHeroClass()        { return heroClass; }
    public int       getLevel()            { return level; }
    public int       getAttack()           { return attack; }
    public int       getDefense()          { return defense; }
    public int       getMaxHp()            { return maxHp; }
    public int       getCurrentHp()        { return currentHp; }
    public int       getMaxMana()          { return maxMana; }
    public int       getCurrentMana()      { return currentMana; }
    public int       getShield()           { return shield; }
    public int       getExperience()       { return experience; }
    public ClassProgression getClassProgression() { return classProgression; }

    public void setCurrentHp(int hp)     { this.currentHp   = Math.max(0, Math.min(maxHp, hp)); }
    public void setCurrentMana(int mana) { this.currentMana = Math.max(0, Math.min(maxMana, mana)); }
    public void setExperience(int xp)    { this.experience  = xp; }

    @Override
    public String toString() {
        return String.format("[%s | Lv%d %s | HP:%d/%d | Mana:%d/%d | ATK:%d DEF:%d%s]",
                name, level, heroClass.displayName,
                currentHp, maxHp, currentMana, maxMana,
                attack, defense, shield > 0 ? " SHD:" + shield : "");
    }
}
