package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Party implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int MAX_SIZE = 4;

    private String name;
    private List<Hero> members;
    private List<Item> inventory;
    private int gold;

    public Party(String name) {
        this.name = name;
        this.members = new ArrayList<>();
        this.inventory = new ArrayList<>();
        this.gold = 50;
    }

    public boolean addHero(Hero hero)  { if (members.size() >= MAX_SIZE) return false; members.add(hero); return true; }
    public void removeHero(Hero hero)  { members.remove(hero); }

    public List<Hero> getAliveMembers() {
        List<Hero> alive = new ArrayList<>();
        for (Hero h : members) if (h.isAlive()) alive.add(h);
        return alive;
    }

    public boolean isDefeated() { return getAliveMembers().isEmpty(); }

    public Hero getHeroWithLowestHp() {
        Hero lowest = null;
        for (Hero h : getAliveMembers()) {
            if (lowest == null || h.getHp() < lowest.getHp()) lowest = h;
        }
        return lowest;
    }

    public void addItem(Item item)      { inventory.add(item); }
    public void removeItem(Item item)   { inventory.remove(item); }
    public boolean canAfford(int cost)  { return gold >= cost; }
    public void spendGold(int amount)   { gold = Math.max(0, gold - amount); }
    public void earnGold(int amount)    { gold += amount; }

    public String getName()             { return name; }
    public void setName(String name)    { this.name = name; }
    public List<Hero> getMembers()      { return members; }
    public List<Item> getInventory()    { return inventory; }
    public int getGold()                { return gold; }
    public int getSize()                { return members.size(); }
    public boolean isFull()             { return members.size() >= MAX_SIZE; }

    @Override
    public String toString() { return name + " (" + members.size() + " heroes, " + gold + "g)"; }
}
