package battle;

import model.*;
import java.util.*;

public class BattleActions {

    public static class BasicAttack implements BattleAction {
        @Override public String execute(Hero actor, List<Enemy> targets, Party allies) {
            if (targets.isEmpty()) return actor.getName() + " has no target!";
            Enemy t = targets.get(0);
            t.takeDamage(actor.getAttack());
            return actor.getName() + " attacks " + t.getName() + " for " + actor.getAttack() + " damage!";
        }
        @Override public String getName()         { return "Basic Attack"; }
        @Override public int getManaCost()        { return 0; }
        @Override public boolean canUse(Hero h)   { return true; }
    }

    public static class Defend implements BattleAction {
        public static final int HP_BONUS   = 10;
        public static final int MANA_BONUS = 5;

        @Override public String execute(Hero actor, List<Enemy> targets, Party allies) {
            actor.heal(HP_BONUS);
            actor.restoreMana(MANA_BONUS);
            return actor.getName() + " defends! +" + HP_BONUS + " HP, +" + MANA_BONUS + " MP. Turn skipped.";
        }
        @Override public String getName()         { return "Defend"; }
        @Override public int getManaCost()        { return 0; }
        @Override public boolean canUse(Hero h)   { return true; }
    }

    public static class Protect implements BattleAction {
        @Override public String execute(Hero actor, List<Enemy> targets, Party allies) {
            if (!actor.hasMana(25)) return actor.getName() + " doesn't have enough mana!";
            actor.spendMana(25);
            StringBuilder sb = new StringBuilder(actor.getName() + " casts Protect!\n");
            for (Hero ally : allies.getAliveMembers()) {
                int shield = (int)(ally.getMaxHp() * 0.10);
                ally.heal(shield);
                sb.append("  ").append(ally.getName()).append(" gains ").append(shield).append(" HP.\n");
            }
            return sb.toString().trim();
        }
        @Override public String getName()         { return "Protect (25 MP)"; }
        @Override public int getManaCost()        { return 25; }
        @Override public boolean canUse(Hero h)   { return h.hasMana(25); }
    }

    public static class Heal implements BattleAction {
        @Override public String execute(Hero actor, List<Enemy> targets, Party allies) {
            if (!actor.hasMana(35)) return actor.getName() + " doesn't have enough mana!";
            actor.spendMana(35);
            Hero target = allies.getHeroWithLowestHp();
            if (target == null) return "No one to heal!";
            int amt = (int)(target.getMaxHp() * 0.25);
            target.heal(amt);
            return actor.getName() + " heals " + target.getName() + " for " + amt + " HP!";
        }
        @Override public String getName()         { return "Heal (35 MP)"; }
        @Override public int getManaCost()        { return 35; }
        @Override public boolean canUse(Hero h)   { return h.hasMana(35); }
    }

    public static class Fireball implements BattleAction {
        @Override public String execute(Hero actor, List<Enemy> targets, Party allies) {
            if (!actor.hasMana(30)) return actor.getName() + " doesn't have enough mana!";
            actor.spendMana(30);
            StringBuilder sb = new StringBuilder(actor.getName() + " casts Fireball!\n");
            for (int i = 0; i < Math.min(3, targets.size()); i++) {
                int dmg = actor.getAttack() * 2;
                targets.get(i).takeDamage(dmg);
                sb.append("  ").append(targets.get(i).getName()).append(" takes ").append(dmg).append(" fire damage!\n");
            }
            return sb.toString().trim();
        }
        @Override public String getName()         { return "Fireball (30 MP)"; }
        @Override public int getManaCost()        { return 30; }
        @Override public boolean canUse(Hero h)   { return h.hasMana(30); }
    }

    public static class ChainLightning implements BattleAction {
        @Override public String execute(Hero actor, List<Enemy> targets, Party allies) {
            if (!actor.hasMana(40)) return actor.getName() + " doesn't have enough mana!";
            actor.spendMana(40);
            List<Enemy> shuffled = new ArrayList<>(targets);
            Collections.shuffle(shuffled);
            StringBuilder sb = new StringBuilder(actor.getName() + " casts Chain Lightning!\n");
            double dmg = actor.getAttack();
            for (Enemy e : shuffled) {
                if (!e.isAlive() || dmg < 1) continue;
                e.takeDamage((int) dmg);
                sb.append("  ").append(e.getName()).append(" takes ").append((int)dmg).append(" damage!\n");
                dmg *= 0.25;
            }
            return sb.toString().trim();
        }
        @Override public String getName()         { return "Chain Lightning (40 MP)"; }
        @Override public int getManaCost()        { return 40; }
        @Override public boolean canUse(Hero h)   { return h.hasMana(40); }
    }

    public static class BerserkerAttack implements BattleAction {
        @Override public String execute(Hero actor, List<Enemy> targets, Party allies) {
            if (!actor.hasMana(60)) return actor.getName() + " doesn't have enough mana!";
            actor.spendMana(60);
            StringBuilder sb = new StringBuilder(actor.getName() + " goes berserk!\n");
            if (!targets.isEmpty()) {
                targets.get(0).takeDamage(actor.getAttack());
                sb.append("  ").append(targets.get(0).getName()).append(" takes ").append(actor.getAttack()).append(" damage!\n");
                int splash = (int)(actor.getAttack() * 0.25);
                for (int i = 1; i < Math.min(3, targets.size()); i++) {
                    targets.get(i).takeDamage(splash);
                    sb.append("  ").append(targets.get(i).getName()).append(" takes ").append(splash).append(" splash!\n");
                }
            }
            return sb.toString().trim();
        }
        @Override public String getName()         { return "Berserker Attack (60 MP)"; }
        @Override public int getManaCost()        { return 60; }
        @Override public boolean canUse(Hero h)   { return h.hasMana(60); }
    }

    public static class Replenish implements BattleAction {
        @Override public String execute(Hero actor, List<Enemy> targets, Party allies) {
            if (!actor.hasMana(80)) return actor.getName() + " doesn't have enough mana!";
            actor.spendMana(80);
            StringBuilder sb = new StringBuilder(actor.getName() + " casts Replenish!\n");
            for (Hero ally : allies.getAliveMembers()) {
                int restore = ally == actor ? 60 : 30;
                ally.restoreMana(restore);
                sb.append("  ").append(ally.getName()).append(" restores ").append(restore).append(" MP.\n");
            }
            return sb.toString().trim();
        }
        @Override public String getName()         { return "Replenish (80 MP)"; }
        @Override public int getManaCost()        { return 80; }
        @Override public boolean canUse(Hero h)   { return h.hasMana(80); }
    }

    /** Returns the list of actions available to a hero based on their class */
    public static List<BattleAction> getActionsForHero(Hero hero) {
        List<BattleAction> actions = new ArrayList<>();
        actions.add(new BasicAttack());
        actions.add(new Defend());
        switch (hero.getHeroClass()) {
            case ORDER: case PRIEST: case PROPHET: case PALADIN:
                actions.add(new Protect()); actions.add(new Heal()); break;
            case CHAOS: case INVOKER: case HERETIC: case ROGUE: case SORCERER:
                actions.add(new Fireball()); actions.add(new ChainLightning()); break;
            case WARRIOR: case KNIGHT:
                actions.add(new BerserkerAttack()); break;
            case MAGE: case WIZARD: case WARLOCK:
                actions.add(new Replenish()); break;
            default: break;
        }
        return actions;
    }
}
