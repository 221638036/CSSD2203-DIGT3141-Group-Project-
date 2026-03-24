package model;

import java.io.Serializable;

public class Hero implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private HeroClass heroClass;
    private int level;
    private int hp, maxHp;
    private int mana, maxMana;
    private int attack, defense;
    private boolean alive;
    private boolean stunned;

    private int orderLevel, chaosLevel, warriorLevel, mageLevel;
    private HeroClass firstSpecialization = null;
    private boolean isHybrid = false;

    // Base stats per TC10: attack=5, defense=5, HP=100, mana=50
    public static final int BASE_ATTACK  = 5;
    public static final int BASE_DEFENSE = 5;
    public static final int BASE_HP      = 100;
    public static final int BASE_MANA    = 50;

    // Base level-up bonus per TC06: +1 atk, +1 def, +5 HP, +2 mana
    public static final int LEVELUP_ATTACK  = 1;
    public static final int LEVELUP_DEFENSE = 1;
    public static final int LEVELUP_HP      = 5;
    public static final int LEVELUP_MANA    = 2;

    private int xp = 0;
    private static final int XP_PER_LEVEL = 100;

    public Hero(String name, HeroClass startingClass) {
        this.name = name;
        this.heroClass = startingClass;
        this.level = 1;
        this.alive = true;
        this.stunned = false;
        this.maxHp = BASE_HP;       this.hp = BASE_HP;
        this.maxMana = BASE_MANA;   this.mana = BASE_MANA;
        this.attack = BASE_ATTACK;  this.defense = BASE_DEFENSE;
        incrementClassLevel(startingClass);
    }

    /** Add XP; returns true if levelled up */
    public boolean addXp(int amount) {
        xp += amount;
        if (xp >= XP_PER_LEVEL) {
            xp -= XP_PER_LEVEL;
            levelUp(heroClass);
            return true;
        }
        return false;
    }

    public int getXp() { return xp; }

    public void levelUp(HeroClass classChoice) {
        // Base bonus applied first (TC06)
        maxHp    += LEVELUP_HP;
        maxMana  += LEVELUP_MANA;
        attack   += LEVELUP_ATTACK;
        defense  += LEVELUP_DEFENSE;

        // Then class-specific bonus
        if (isHybrid) applyHybridGrowth();
        else {
            applyClassGrowth(classChoice);
            incrementClassLevel(classChoice);
            checkForSpecialization(classChoice);
        }
        level++;
        hp = maxHp;
        mana = maxMana;
    }

    private void applyClassGrowth(HeroClass cls) {
        switch (cls) {
            case ORDER:   maxMana += 5; defense += 2; break;
            case CHAOS:   attack  += 3; maxHp   += 5; break;
            case WARRIOR: attack  += 2; defense += 3; break;
            case MAGE:    maxMana += 5; attack  += 1; break;
            default: break;
        }
    }

    private void applyHybridGrowth() {
        switch (heroClass) {
            case PALADIN:  applyClassGrowth(HeroClass.ORDER);   applyClassGrowth(HeroClass.WARRIOR); break;
            case HERETIC:  applyClassGrowth(HeroClass.ORDER);   applyClassGrowth(HeroClass.CHAOS);   break;
            case PROPHET:  applyClassGrowth(HeroClass.ORDER);   applyClassGrowth(HeroClass.MAGE);    break;
            case ROGUE:    applyClassGrowth(HeroClass.CHAOS);   applyClassGrowth(HeroClass.WARRIOR); break;
            case SORCERER: applyClassGrowth(HeroClass.CHAOS);   applyClassGrowth(HeroClass.MAGE);    break;
            case WARLOCK:  applyClassGrowth(HeroClass.WARRIOR); applyClassGrowth(HeroClass.MAGE);    break;
            default:       applyClassGrowth(heroClass); break;
        }
    }

    private void incrementClassLevel(HeroClass cls) {
        switch (cls) {
            case ORDER:   orderLevel++;   break;
            case CHAOS:   chaosLevel++;   break;
            case WARRIOR: warriorLevel++; break;
            case MAGE:    mageLevel++;    break;
            default: break;
        }
    }

    private void checkForSpecialization(HeroClass cls) {
        int clvl = getClassLevel(cls);
        if (clvl >= 5 && firstSpecialization == null) {
            firstSpecialization = cls;
        } else if (clvl >= 5 && firstSpecialization != null && cls != firstSpecialization && !isHybrid) {
            isHybrid = true;
            heroClass = resolveHybridClass(firstSpecialization, cls);
        }
    }

    private HeroClass resolveHybridClass(HeroClass a, HeroClass b) {
        if (matches(a, b, HeroClass.ORDER,   HeroClass.CHAOS))    return HeroClass.HERETIC;
        if (matches(a, b, HeroClass.ORDER,   HeroClass.WARRIOR))  return HeroClass.PALADIN;
        if (matches(a, b, HeroClass.ORDER,   HeroClass.MAGE))     return HeroClass.PROPHET;
        if (matches(a, b, HeroClass.CHAOS,   HeroClass.WARRIOR))  return HeroClass.ROGUE;
        if (matches(a, b, HeroClass.CHAOS,   HeroClass.MAGE))     return HeroClass.SORCERER;
        if (matches(a, b, HeroClass.WARRIOR, HeroClass.MAGE))     return HeroClass.WARLOCK;
        return heroClass;
    }

    private boolean matches(HeroClass a, HeroClass b, HeroClass x, HeroClass y) {
        return (a == x && b == y) || (a == y && b == x);
    }

    private int getClassLevel(HeroClass cls) {
        switch (cls) {
            case ORDER:   return orderLevel;
            case CHAOS:   return chaosLevel;
            case WARRIOR: return warriorLevel;
            case MAGE:    return mageLevel;
            default:      return 0;
        }
    }

    public void takeDamage(int dmg) {
        int effective = Math.max(1, dmg - defense);
        hp = Math.max(0, hp - effective);
        if (hp == 0) alive = false;
    }

    public void heal(int amount)        { hp = Math.min(maxHp, hp + amount); if (hp > 0) alive = true; }
    public void restoreMana(int amount) { mana = Math.min(maxMana, mana + amount); }
    public void spendMana(int amount)   { mana = Math.max(0, mana - amount); }
    public boolean hasMana(int cost)    { return mana >= cost; }
    public void revive()                { hp = maxHp / 2; alive = true; }
    public void fullHeal()              { hp = maxHp; mana = maxMana; alive = true; }

    public String getName()          { return name; }
    public HeroClass getHeroClass()  { return heroClass; }
    public int getLevel()            { return level; }
    public int getHp()               { return hp; }
    public int getMaxHp()            { return maxHp; }
    public int getMana()             { return mana; }
    public int getMaxMana()          { return maxMana; }
    public int getAttack()           { return attack; }
    public int getDefense()          { return defense; }
    public boolean isAlive()         { return alive; }
    public boolean isStunned()       { return stunned; }
    public void setStunned(boolean s){ this.stunned = s; }
    public boolean isHybrid()        { return isHybrid; }

    // ── Protected setters used by TestHero for direct stat control in tests ──
    protected void setAttackDirect(int v)               { this.attack = v; }
    protected void setDefenseDirect(int v)              { this.defense = v; }
    protected void setHpDirect(int hp, int maxHp)       { this.hp = hp; this.maxHp = maxHp; this.alive = hp > 0; }
    protected void setManaDirect(int mana, int maxMana) { this.mana = mana; this.maxMana = maxMana; }
    protected void setAliveDirect(boolean alive)        { this.alive = alive; if (!alive) this.hp = 0; }

    @Override
    public String toString() {
        return String.format("%s [%s] Lv%d  HP:%d/%d  MP:%d/%d  ATK:%d  DEF:%d%s",
            name, heroClass, level, hp, maxHp, mana, maxMana, attack, defense,
            stunned ? " [STUNNED]" : "");
    }
}
