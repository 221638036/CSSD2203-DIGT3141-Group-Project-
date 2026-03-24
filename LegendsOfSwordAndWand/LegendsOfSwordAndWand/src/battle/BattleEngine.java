package battle;

import model.*;
import java.util.*;

public class BattleEngine {
    private Party playerParty;
    private List<Enemy> enemies;
    private List<BattleObserver> observers = new ArrayList<>();
    private int currentHeroIndex = 0;
    private boolean battleOver = false;
    private boolean playerWon = false;
    private Set<String> defendingHeroes = new HashSet<>();

    public BattleEngine(Party playerParty, List<Enemy> enemies) {
        this.playerParty = playerParty;
        this.enemies = enemies;
    }

    public void addObserver(BattleObserver obs)    { observers.add(obs); }
    public void removeObserver(BattleObserver obs) { observers.remove(obs); }

    private void notifyEvent(String msg)        { for (BattleObserver o : observers) o.onBattleEvent(msg); }
    private void notifyBattleEnd(boolean won)   { for (BattleObserver o : observers) o.onBattleEnd(won); }
    private void notifyTurnChanged(int idx)     { for (BattleObserver o : observers) o.onTurnChanged(idx); }

    public Hero getCurrentHero() {
        List<Hero> alive = playerParty.getAliveMembers();
        if (alive.isEmpty()) return null;
        return alive.get(currentHeroIndex % alive.size());
    }

    public List<Enemy> getAliveEnemies() {
        List<Enemy> alive = new ArrayList<>();
        for (Enemy e : enemies) if (e.isAlive()) alive.add(e);
        return alive;
    }

    public void playerAction(BattleAction action, int targetIndex) {
        if (battleOver) return;
        Hero hero = getCurrentHero();
        if (hero == null || !hero.isAlive()) { advanceTurn(); return; }

        if (hero.isStunned()) {
            notifyEvent(hero.getName() + " is stunned and skips their turn!");
            hero.setStunned(false);
            enemyTurn();
            advanceTurn();
            return;
        }

        // Build target list: chosen target first, then rest
        List<Enemy> aliveEnemies = getAliveEnemies();
        List<Enemy> targetList = new ArrayList<>();
        if (!aliveEnemies.isEmpty()) {
            int idx = Math.min(targetIndex, aliveEnemies.size() - 1);
            targetList.add(aliveEnemies.get(idx));
            for (int i = 0; i < aliveEnemies.size(); i++) if (i != idx) targetList.add(aliveEnemies.get(i));
        }

        if (action instanceof BattleActions.Defend) defendingHeroes.add(hero.getName());

        String result = action.execute(hero, targetList, playerParty);
        notifyEvent(result);
        checkBattleEnd();
        if (!battleOver) { enemyTurn(); advanceTurn(); }
    }

    private void enemyTurn() {
        List<Hero> alive = playerParty.getAliveMembers();
        if (alive.isEmpty()) return;
        Random rand = new Random();
        for (Enemy enemy : getAliveEnemies()) {
            Hero target = alive.get(rand.nextInt(alive.size()));
            int dmg = enemy.getAttack();
            if (defendingHeroes.contains(target.getName())) dmg /= 2;
            target.takeDamage(dmg);
            notifyEvent(enemy.getName() + " attacks " + target.getName() + " for " + dmg + " damage!"
                + (!target.isAlive() ? " " + target.getName() + " is defeated!" : ""));
        }
        defendingHeroes.clear();
        checkBattleEnd();
    }

    private void advanceTurn() {
        if (battleOver) return;
        List<Hero> alive = playerParty.getAliveMembers();
        if (alive.isEmpty()) return;
        currentHeroIndex = (currentHeroIndex + 1) % alive.size();
        notifyTurnChanged(currentHeroIndex);
    }

    private void checkBattleEnd() {
        if (getAliveEnemies().isEmpty()) {
            battleOver = true; playerWon = true;

            // TC07: gold = goldReward * level * count, XP = xpReward * level * count
            // then XP split among SURVIVING heroes only
            int totalGold = 0, totalXp = 0;
            for (Enemy e : enemies) {
                totalGold += e.getGoldReward() * e.getLevel();
                totalXp  += e.getXpReward()   * e.getLevel();
            }
            playerParty.earnGold(totalGold);

            List<Hero> survivors = playerParty.getAliveMembers();
            if (!survivors.isEmpty()) {
                int xpEach = totalXp / survivors.size();
                for (Hero h : survivors) h.addXp(xpEach);
            }

            notifyEvent("Victory! Earned " + totalGold + " gold and " + totalXp + " XP split among " + survivors.size() + " survivors!");
            notifyBattleEnd(true);
        } else if (playerParty.isDefeated()) {
            battleOver = true; playerWon = false;
            notifyEvent("Your party has been defeated...");
            notifyBattleEnd(false);
        }
    }

    public boolean isBattleOver()   { return battleOver; }
    public boolean isPlayerWon()    { return playerWon; }
    public Party getPlayerParty()   { return playerParty; }
    public List<Enemy> getEnemies() { return enemies; }
}
