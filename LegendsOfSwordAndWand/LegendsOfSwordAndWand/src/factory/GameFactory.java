package factory;

import model.*;
import java.util.*;

/** DESIGN PATTERN: Factory — centralizes creation of Heroes, Enemies, Items */
public class GameFactory {

    public static Hero createHero(String name, HeroClass cls) {
        return new Hero(name, cls);
    }

    public static List<Enemy> createEnemyGroup(int roomNumber) {
        Random rand = new Random();
        // Enemy level scales: room 1-4 = level 1, room 5-9 = level 1-2, increases gradually
        int level = Math.max(1, roomNumber / 5 + 1);
        // Enemy count: 1-5 units as per TC04
        int count = 1 + rand.nextInt(5);
        String[] names = {"Goblin", "Orc", "Skeleton", "Troll", "Dark Mage", "Bandit", "Wolf", "Vampire"};
        List<Enemy> group = new ArrayList<>();
        for (int i = 0; i < count; i++)
            group.add(new Enemy(names[rand.nextInt(names.length)] + " " + (i + 1), level));
        return group;
    }

    public static List<Item> createInnShopItems() {
        List<Item> items = new ArrayList<>();
        items.add(new Item("Health Potion",  Item.ItemType.HEALTH_POTION, 40, 20));
        items.add(new Item("Mana Potion",    Item.ItemType.MANA_POTION,   40, 20));
        items.add(new Item("Elixir",         Item.ItemType.ELIXIR,        25, 35));
        items.add(new Item("Greater Potion", Item.ItemType.HEALTH_POTION, 80, 40));
        return items;
    }

    public static List<Hero> createRecruitableHeroes(int roomNumber) {
        Random rand = new Random();
        int level = Math.max(1, roomNumber / 5 + 1);
        String[] heroNames = {"Aldric", "Seraphina", "Maren", "Theron", "Lyra", "Vex"};
        HeroClass[] baseClasses = {HeroClass.ORDER, HeroClass.CHAOS, HeroClass.WARRIOR, HeroClass.MAGE};
        List<Hero> recruits = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            HeroClass cls = baseClasses[rand.nextInt(4)];
            Hero h = new Hero(heroNames[rand.nextInt(heroNames.length)], cls);
            for (int l = 1; l < level; l++) h.levelUp(cls);
            recruits.add(h);
        }
        return recruits;
    }
}
