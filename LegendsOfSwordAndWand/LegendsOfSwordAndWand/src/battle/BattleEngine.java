package battle;

import model.*;
import java.util.*;
import java.util.stream.Collectors;

public class BattleEngine {
    private Party playerParty;
    private List<Enemy> enemies;
    private Party opponentParty;
    private List<Enemy> pvpPlayerEnemies = new ArrayList<>();
    private List<Enemy> pvpOpponentEnemies = new ArrayList<>();
    private List<BattleObserver> observers = new ArrayList<>();
    private int currentHeroIndex = 0;
    private boolean battleOver = false;
    private boolean playerWon = false;
    private boolean pvpMode = false;
    private boolean isOpponentTurn = false;
    private Set<String> defendingHeroes = new HashSet<>();

    public BattleEngine(Party playerParty, List<Enemy> enemies) {
        this(playerParty, enemies, false);
    }

    public BattleEngine(Party playerParty, List<Enemy> enemies, boolean pvpMode) {
        this.playerParty = playerParty;
        this.enemies = enemies;
        this.pvpMode = pvpMode;
        this.opponentParty = null;
        this.isOpponentTurn = false;
    }

    public BattleEngine(Party playerParty, Party opponentParty, boolean pvpMode) {
        this.playerParty = playerParty;
        this.opponentParty = opponentParty;
        this.enemies = new ArrayList<>();
        this.pvpMode = pvpMode;
        this.isOpponentTurn = false;
        initializePvpEnemies();
    }

    private void initializePvpEnemies() {
        pvpPlayerEnemies.clear();
        pvpOpponentEnemies.clear();

        for (Hero h : playerParty.getMembers()) {
            Enemy e = new Enemy(h.getName(), h.getHp(), h.getAttack(), h.getDefense(), 0, 0);
            e.setLinkedHero(h);
            pvpPlayerEnemies.add(e);
        }
        for (Hero h : opponentParty.getMembers()) {
            Enemy e = new Enemy(h.getName(), h.getHp(), h.getAttack(), h.getDefense(), 0, 0);
            e.setLinkedHero(h);
            pvpOpponentEnemies.add(e);
        }
    }

    public void addObserver(BattleObserver obs)    { observers.add(obs); }
    public void removeObserver(BattleObserver obs) { observers.remove(obs); }

    private void notifyEvent(String msg)        { for (BattleObserver o : observers) o.onBattleEvent(msg); }
    private void notifyBattleEnd(boolean won)   { for (BattleObserver o : observers) o.onBattleEnd(won); }
    private void notifyTurnChanged(int idx)     { for (BattleObserver o : observers) o.onTurnChanged(idx); }

    public Party getCurrentParty() { return isOpponentTurn ? opponentParty : playerParty; }
    public Party getOtherParty() { return isOpponentTurn ? playerParty : opponentParty; }
    public List<Hero> getOpponentHeroes() { return pvpMode ? getOtherParty().getAliveMembers() : null; }
    public boolean isOpponentTurn() { return isOpponentTurn; }
    public void setOpponentTurn(boolean isOpponentTurn) { this.isOpponentTurn = isOpponentTurn; }

    public Hero getCurrentHero() {
        List<Hero> alive = getCurrentParty().getAliveMembers();
        if (alive.isEmpty()) return null;
        return alive.get(currentHeroIndex % alive.size());
    }

    public List<Enemy> getAliveEnemies() {
        if (pvpMode) {
            List<Enemy> others = isOpponentTurn ? pvpPlayerEnemies : pvpOpponentEnemies;
            return others.stream().filter(Enemy::isAlive).collect(Collectors.toList());
        } else {
            return enemies.stream().filter(e -> e.isAlive()).collect(Collectors.toList());
        }
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

        String result = action.execute(hero, targetList, getCurrentParty());
        notifyEvent(result);
        checkBattleEnd();
        if (!battleOver) { 
            if (!pvpMode) enemyTurn(); 
            advanceTurn(); 
        }
    }

    private void enemyTurn() {
        if (pvpMode) return;
        List<Hero> alive = playerParty.getAliveMembers();
        if (alive.isEmpty()) return;
        Random rand = new Random();
        for (Enemy enemy : getAliveEnemies()) {
            if (enemy.isStunned()) {
                notifyEvent(enemy.getName() + " is stunned and skips their turn!");
                enemy.setStunned(false);
                continue;
            }
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
        List<Hero> alive = getCurrentParty().getAliveMembers();
        if (alive.isEmpty()) return;
        currentHeroIndex = (currentHeroIndex + 1) % alive.size();
        if (currentHeroIndex == 0) {
            if (pvpMode) {
                isOpponentTurn = !isOpponentTurn;
            }
        }
        notifyTurnChanged(currentHeroIndex);
    }

    private void checkBattleEnd() {
        if (pvpMode) {
            if (getOtherParty().isDefeated()) {
                battleOver = true;
                playerWon = !isOpponentTurn; // if opponent is defeated, the current player wins
                notifyEvent("Victory! The " + (isOpponentTurn ? "opponent" : "your") + " party has been defeated!");
                notifyBattleEnd(playerWon);
            }
        } else {
            if (getAliveEnemies().isEmpty()) {
                battleOver = true; playerWon = true;

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
                notifyEvent("Victory! Earned " + totalGold + " gold and " + totalXp + " XP split among " + playerParty.getAliveMembers().size() + " survivors!");
                notifyBattleEnd(true);
            } else if (playerParty.isDefeated()) {
                battleOver = true; playerWon = false;
                notifyEvent("Your party has been defeated...");
                notifyBattleEnd(false);
            }
        }
    }

    public void useItem(Hero user, Item item, Hero target, Party party) {
        if (battleOver) return;
        if (!party.getInventory().contains(item)) return;

        item.use(target);
        party.removeItem(item);
        notifyEvent(user.getName() + " used " + item.getName() + " on " + target.getName() + "!");
        checkBattleEnd();
        if (!battleOver) { 
            if (!pvpMode) enemyTurn(); 
            advanceTurn(); 
        }
    }

    public boolean isBattleOver()   { return battleOver; }
    public boolean isPlayerWon()    { return playerWon; }
    public Party getPlayerParty()   { return playerParty; }
    public Party getOpponentParty() { return opponentParty; }
    public List<Enemy> getEnemies() { return enemies; }
    public boolean isPvpMode()      { return pvpMode; }
}
