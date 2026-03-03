package com.legends.battle;

import com.legends.model.Hero;
import com.legends.model.Party;

import java.util.*;

/**
 * Core turn-based battle engine.
 *
 * Coordinates turn ordering, action resolution, wait-queue management,
 * and XP distribution. Observers can register to receive battle events.
 * Action execution is delegated to hero instances.
 */
public class BattleEngine {

    private Party playerParty;
    private Party enemyParty;

    private final Deque<Hero> waitQueue = new ArrayDeque<>();
    private List<Hero> turnOrder;
    private int turnIndex;
    private boolean battleOver;
    private BattleResult.Outcome lastOutcome;

    // ── Observer fields ────────────────────────────────────────────────────
    private final List<BattleObserver> observers = new ArrayList<>();

    /** Attach an observer. */
    public void attach(BattleObserver observer) { observers.add(observer); }

    /** Detach an observer. */
    public void detach(BattleObserver observer) { observers.remove(observer); }

    /** Notify all registered observers. */
    private void notifyObservers(String event, Hero actor, String result) {
        for (BattleObserver o : observers) o.onBattleEvent(event, actor, result);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    public void startBattle(Party player, Party enemy) {
        this.playerParty = player;
        this.enemyParty  = enemy;
        this.waitQueue.clear();
        this.battleOver  = false;
        buildTurnOrder();
        notifyObservers("BATTLE_START", null, "Battle started");
    }

    private void buildTurnOrder() {
        List<Hero> playerSorted = playerParty.getLivingMembers().stream()
                .sorted(Comparator.comparingInt(Hero::getLevel)
                        .thenComparingInt(Hero::getAttack).reversed()).toList();
        List<Hero> enemySorted = enemyParty.getLivingMembers().stream()
                .sorted(Comparator.comparingInt(Hero::getLevel)
                        .thenComparingInt(Hero::getAttack).reversed()).toList();

        List<Hero> all = new ArrayList<>();
        all.addAll(playerSorted);
        all.addAll(enemySorted);
        all.sort(Comparator.comparingInt(Hero::getLevel)
                           .thenComparingInt(Hero::getAttack).reversed());

        boolean playerFirst = !all.isEmpty() && playerSorted.contains(all.get(0));
        List<Hero> primary   = playerFirst ? playerSorted : enemySorted;
        List<Hero> secondary = playerFirst ? enemySorted  : playerSorted;

        turnOrder = new ArrayList<>();
        int i = 0, j = 0;
        while (i < primary.size() || j < secondary.size()) {
            if (i < primary.size())   turnOrder.add(primary.get(i++));
            if (j < secondary.size()) turnOrder.add(secondary.get(j++));
        }
        turnIndex = 0;
    }

    // ── Turn execution ────────────────────────────────────────────────────────

    public Hero getCurrentUnit() {
        while (turnIndex < turnOrder.size()) {
            Hero h = turnOrder.get(turnIndex);
            if (!h.isAlive() || h.isStunned()) { h.clearStun(); turnIndex++; continue; }
            return h;
        }
        return null;
    }

    public String executeTurn(BattleAction action, Hero target, int abilityIdx) {
        Hero actor = getCurrentUnit();
        if (actor == null) return "No unit to act.";

        String log;
        switch (action) {
            case ATTACK -> {
                if (target == null || !target.isAlive()) return "Invalid target.";
                int dmg = actor.calculateDamage(target);
                target.takeDamage(dmg);
                log = actor.getName() + " attacks " + target.getName()
                        + " for " + dmg + " damage. (HP left: " + target.getCurrentHp() + ")";
            }
            case DEFEND -> {
                actor.defend();
                log = actor.getName() + " defends. (+10 HP, +5 Mana)";
            }
            case WAIT -> {
                waitQueue.addLast(actor);
                turnIndex++;
                log = actor.getName() + " waits.";
                notifyObservers("TURN_END", actor, log);
                return log;
            }
            case CAST -> {
                // Strategy pattern: BattleEngine delegates to Ability.execute() via hero
                List<Hero> targets = resolveTargets(actor, target);
                log = actor.getName() + " casts: " + actor.useAbility(abilityIdx, targets);
            }
            default -> log = "Unknown action.";
        }

        turnIndex++;
        checkRoundEnd();
        notifyObservers("TURN_END", actor, log); // Observer: notify after every action
        return log;
    }

    private void checkRoundEnd() {
        if (turnIndex >= turnOrder.size()) {
            while (!waitQueue.isEmpty()) {
                Hero h = waitQueue.poll();
                if (h.isAlive()) turnOrder.add(h);
            }
            if (turnIndex >= turnOrder.size()) {
                if (!battleOver) buildTurnOrder();
            }
        }
    }

    // ── Battle state ──────────────────────────────────────────────────────────

    public boolean isBattleOver() {
        if (!playerParty.isAlive()) { battleOver = true; lastOutcome = BattleResult.Outcome.PLAYER_LOSE; }
        if (!enemyParty.isAlive())  { battleOver = true; lastOutcome = BattleResult.Outcome.PLAYER_WIN; }
        if (battleOver) {
            notifyObservers("BATTLE_OVER", null, lastOutcome.name()); // Observer notification
        }
        return battleOver;
    }

    public BattleResult finaliseBattle(List<Hero> enemies) {
        int totalExp  = enemies.stream().mapToInt(h -> 50 * h.getLevel()).sum();
        int totalGold = enemies.stream().mapToInt(h -> 75 * h.getLevel()).sum();

        if (lastOutcome == BattleResult.Outcome.PLAYER_WIN) {
            List<Hero> survivors = playerParty.getLivingMembers();
            if (!survivors.isEmpty()) {
                int expShare = totalExp / survivors.size();
                survivors.forEach(h -> h.addExperience(expShare));
            }
            return new BattleResult(lastOutcome, playerParty, enemyParty, totalExp, totalGold);
        }
        return new BattleResult(lastOutcome, playerParty, enemyParty, 0, 0);
    }

    private List<Hero> resolveTargets(Hero actor, Hero primaryTarget) {
        List<Hero> targets = new ArrayList<>();
        boolean actorIsPlayer = playerParty.getMembers().contains(actor);
        if (primaryTarget != null) targets.add(primaryTarget);
        List<Hero> all = actorIsPlayer ? enemyParty.getLivingMembers()
                                       : playerParty.getLivingMembers();
        for (Hero h : all) if (h != primaryTarget) targets.add(h);
        return targets;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Party getPlayerParty()               { return playerParty; }
    public Party getEnemyParty()                { return enemyParty; }
    public List<Hero> getTurnOrder()            { return turnOrder; }
    public BattleResult.Outcome getLastOutcome() { return lastOutcome; }
}
