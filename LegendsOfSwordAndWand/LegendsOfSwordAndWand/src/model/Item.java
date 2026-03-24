package model;

import java.io.Serializable;

public class Item implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum ItemType { HEALTH_POTION, MANA_POTION, ELIXIR }

    private String name;
    private ItemType type;
    private int value;
    private int cost;

    public Item(String name, ItemType type, int value, int cost) {
        this.name = name; this.type = type; this.value = value; this.cost = cost;
    }

    public void use(Hero target) {
        switch (type) {
            case HEALTH_POTION: target.heal(value); break;
            case MANA_POTION:   target.restoreMana(value); break;
            case ELIXIR:        target.heal(value); target.restoreMana(value); break;
        }
    }

    public String getName()   { return name; }
    public ItemType getType() { return type; }
    public int getValue()     { return value; }
    public int getCost()      { return cost; }

    @Override
    public String toString() { return name + " (" + cost + "g)"; }
}
