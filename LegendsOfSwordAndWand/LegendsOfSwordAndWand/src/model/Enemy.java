package model;

public class Enemy {
    private String name;
    private int hp, maxHp, attack, defense, level, goldReward, xpReward;
    private boolean stunned;
    private Hero linkedHero;

    public Enemy(String name, int level) {
        this.name = name;
        this.level = level;
        this.maxHp = 40 + level * 10; this.hp = maxHp;
        this.attack = 8 + level * 2;
        this.defense = 2 + level;
        this.goldReward = 10 + level * 5;
        this.xpReward = 20 + level * 10;
        this.stunned = false;
    }

    /** Constructor for tests: full control over stats */
    public Enemy(String name, int hp, int attack, int defense, int goldReward, int xpReward) {
        this.name = name; this.level = 1;
        this.maxHp = hp; this.hp = hp;
        this.attack = attack; this.defense = defense;
        this.goldReward = goldReward; this.xpReward = xpReward;
        this.stunned = false;
    }

    /** Damage formula per TC01: max(0, incomingDamage - defense). Minimum damage is 0. */
    public void takeDamage(int dmg) {
        if (linkedHero != null) {
            linkedHero.takeDamage(dmg);
            hp = linkedHero.getHp();
        } else {
            hp = Math.max(0, hp - Math.max(0, dmg - defense));
        }
    }

    public boolean isAlive()   { return linkedHero != null ? linkedHero.isAlive() : hp > 0; }
    public String getName()    { return name; }
    public int getHp()         { return hp; }
    public int getMaxHp()      { return maxHp; }
    public int getAttack()     { return attack; }
    public int getDefense()    { return defense; }
    public int getLevel()      { return level; }
    public int getGoldReward() { return goldReward; }
    public int getXpReward()   { return xpReward; }
    public boolean isStunned() { return stunned; }
    public void setStunned(boolean s) { this.stunned = s; }
    public Hero getLinkedHero() { return linkedHero; }
    public void setLinkedHero(Hero linkedHero) {
        this.linkedHero = linkedHero;
        if (linkedHero != null) {
            this.hp = linkedHero.getHp();
            this.maxHp = linkedHero.getMaxHp();
            this.attack = linkedHero.getAttack();
            this.defense = linkedHero.getDefense();
        }
    }
    @Override
    public String toString() {
        return String.format("%s [Lv%d] HP:%d/%d ATK:%d DEF:%d", name, level, hp, maxHp, attack, defense);
    }
}
