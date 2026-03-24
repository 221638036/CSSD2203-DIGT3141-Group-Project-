package model;

public class Enemy {
    private String name;
    private int hp, maxHp, attack, defense, level, goldReward, xpReward;

    public Enemy(String name, int level) {
        this.name = name;
        this.level = level;
        this.maxHp = 40 + level * 10; this.hp = maxHp;
        this.attack = 8 + level * 2;
        this.defense = 2 + level;
        this.goldReward = 10 + level * 5;
        this.xpReward = 20 + level * 10;
    }

    /** Constructor for tests: full control over stats */
    public Enemy(String name, int hp, int attack, int defense, int goldReward, int xpReward) {
        this.name = name; this.level = 1;
        this.maxHp = hp; this.hp = hp;
        this.attack = attack; this.defense = defense;
        this.goldReward = goldReward; this.xpReward = xpReward;
    }

    /** Damage formula per TC01: max(0, incomingDamage - defense). Minimum damage is 0. */
    public void takeDamage(int dmg) { hp = Math.max(0, hp - Math.max(0, dmg - defense)); }

    public boolean isAlive()   { return hp > 0; }
    public String getName()    { return name; }
    public int getHp()         { return hp; }
    public int getMaxHp()      { return maxHp; }
    public int getAttack()     { return attack; }
    public int getDefense()    { return defense; }
    public int getLevel()      { return level; }
    public int getGoldReward() { return goldReward; }
    public int getXpReward()   { return xpReward; }

    @Override
    public String toString() {
        return String.format("%s [Lv%d] HP:%d/%d ATK:%d DEF:%d", name, level, hp, maxHp, attack, defense);
    }
}
