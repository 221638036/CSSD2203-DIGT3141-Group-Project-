package state;

import battle.BattleEngine;
import factory.GameFactory;
import model.*;
import persistence.FileDataManager;
import javax.swing.*;
import java.util.*;

/** DESIGN PATTERN: State — manages all transitions between game screens */
public class GameStateManager {
    private static GameStateManager instance;
    public static GameStateManager getInstance() {
        if (instance == null) instance = new GameStateManager();
        return instance;
    }
    private GameStateManager() {}

    public interface StateListener { void onStateChanged(GameState newState); }

    private GameState currentState = GameState.LOGIN;
    private Profile loggedInProfile;
    private CampaignState activeCampaign;
    private BattleEngine battleEngine;
    private final List<StateListener> listeners = new ArrayList<>();

    public void addListener(StateListener l)    { listeners.add(l); }
    public void removeListener(StateListener l) { listeners.remove(l); }

    private void transition(GameState s) {
        currentState = s;
        for (StateListener l : listeners) l.onStateChanged(s);
    }

    // ── Auth ──────────────────────────────────────────────────────────────────
    public boolean login(String username, String password) {
        Profile p = FileDataManager.getInstance().login(username, password);
        if (p == null) return false;
        loggedInProfile = p;
        transition(GameState.MAIN_MENU);
        return true;
    }

    public boolean register(String username, String password) {
        return FileDataManager.getInstance().registerProfile(username, password);
    }

    public void logout() {
        loggedInProfile = null; activeCampaign = null;
        transition(GameState.LOGIN);
    }

    // ── Campaign ──────────────────────────────────────────────────────────────
    public void startNewCampaign(String heroName, HeroClass heroClass) {
        Hero hero = GameFactory.createHero(heroName, heroClass);
        Party party = new Party(heroName + "'s Party");
        party.addHero(hero);
        activeCampaign = new CampaignState(party);
        loggedInProfile.setActiveCampaign(activeCampaign);
        FileDataManager.getInstance().saveProfile(loggedInProfile);
        transition(GameState.CAMPAIGN_MAP);
    }

    public void resumeCampaign() {
        activeCampaign = loggedInProfile.getActiveCampaign();
        if (activeCampaign == null) return;
        transition(activeCampaign.isAtInn() ? GameState.INN : GameState.CAMPAIGN_MAP);
    }

    public void enterNextRoom() {
        int room = activeCampaign.getCurrentRoom() + 1;
        activeCampaign.setCurrentRoom(room);
        activeCampaign.setAtInn(false);

        if (room >= 30) {
            int score = calculateScore();
            loggedInProfile.addScore(score);
            handleEndOfCampaign();
            return;
        }

        // TC08: base 60% battle chance, +3% per 10 cumulative hero levels
        int cumulativeLevels = activeCampaign.getParty().getMembers()
            .stream().mapToInt(model.Hero::getLevel).sum();
        int battleChance = 60 + (cumulativeLevels / 10) * 3;
        battleChance = Math.min(battleChance, 95); // cap at 95%

        boolean isBattleRoom = (Math.random() * 100) < battleChance;

        if (!isBattleRoom) {
            activeCampaign.setAtInn(true);
            FileDataManager.getInstance().saveProfile(loggedInProfile);
            transition(GameState.INN);
        } else {
            battleEngine = new BattleEngine(activeCampaign.getParty(), GameFactory.createEnemyGroup(room));
            activeCampaign.setInBattle(true);
            FileDataManager.getInstance().saveProfile(loggedInProfile);
            transition(GameState.BATTLE);
        }
    }

    private void handleEndOfCampaign() {
        // Campaign complete — save score, go back to map so player can choose to save party
        FileDataManager.getInstance().saveProfile(loggedInProfile);
        transition(GameState.CAMPAIGN_MAP);
    }

    public void battleEnded(boolean won) {
        activeCampaign.setInBattle(false);
        
        if (!won) {
            // TC02: Apply loss penalties
            // Lose 10% gold
            Party party = activeCampaign.getParty();
            int goldLoss = Math.max(1, party.getGold() / 10);
            party.spendGold(goldLoss);
            
            // Lose 30% XP from each hero (but not levels)
            for (Hero h : party.getMembers()) {
                int xpLoss = Math.max(1, h.getXp() / 3);  // 30% ≈ 1/3
                h.removeXp(xpLoss);
            }
            
            // Return to inn (previous room which should be an inn)
            int currentRoom = activeCampaign.getCurrentRoom();
            activeCampaign.setCurrentRoom(Math.max(0, currentRoom - 1));
            activeCampaign.setAtInn(true);
            
            // Track loss in profile
            loggedInProfile.incrementLosses();
        }
        
        // After win, process pending level-ups for heroes (not in PvP)
        if (won && battleEngine != null && !battleEngine.isPvpMode()) {
            for (Hero h : activeCampaign.getParty().getAliveMembers()) {
                while (h.getPendingLevelUps() > 0 && h.getLevel() < 20) {
                    HeroClass[] options = h.getAvailableLevelUpClasses();
                    HeroClass defaultChoice = options.length > 0 ? options[0] : h.getHeroClass();
                    HeroClass choice = (HeroClass) JOptionPane.showInputDialog(null,
                        h.getName() + " has leveled up! Choose a class path for this level (current " + h.getHeroClass() + "):",
                        "Level Up Class Choice", JOptionPane.PLAIN_MESSAGE, null, options, defaultChoice);
                    if (choice == null) {
                        choice = defaultChoice;
                    }
                    h.processLevelUp(choice);
                }
                if (h.getLevel() >= 20) {
                    h.clearPendingLevelUps();
                }
            }
        }

        if (battleEngine != null && battleEngine.isPvpMode()) {
            // PvP: update win/loss for BOTH players
            String opponentUsername = battleEngine.getOpponentUsername();
            if (won) {
                loggedInProfile.incrementWins();
            } else {
                loggedInProfile.incrementLosses();
            }
            // Update opponent's record
            if (opponentUsername != null) {
                Profile opponentProfile = FileDataManager.getInstance().getAllProfiles()
                    .stream().filter(p -> p.getUsername().equals(opponentUsername))
                    .findFirst().orElse(null);
                if (opponentProfile != null) {
                    if (won) opponentProfile.incrementLosses();
                    else     opponentProfile.incrementWins();
                    FileDataManager.getInstance().saveProfile(opponentProfile);
                }
            }
            FileDataManager.getInstance().saveProfile(loggedInProfile);
            transition(GameState.MAIN_MENU);
        } else {
            // PvE: go back to campaign map on win, inn on loss
            FileDataManager.getInstance().saveProfile(loggedInProfile);
            transition(won ? GameState.CAMPAIGN_MAP : GameState.INN);
        }
    }

    public void leaveInn() {
        activeCampaign.setAtInn(false);
        transition(GameState.CAMPAIGN_MAP);
    }

    public void exitCampaign() {
        FileDataManager.getInstance().saveProfile(loggedInProfile);
        transition(GameState.MAIN_MENU);
    }

    private int calculateScore() {
        int score = 0;
        for (Hero h : activeCampaign.getParty().getMembers()) score += h.getLevel() * 100;
        score += activeCampaign.getParty().getGold();
        return score;
    }

    // ── PvP ───────────────────────────────────────────────────────────────────
    public boolean initiatePvp(String opponentUsername, int myPartyIndex) {
        Profile opponent = FileDataManager.getInstance().getAllProfiles()
            .stream().filter(p -> p.getUsername().equals(opponentUsername)).findFirst().orElse(null);
        if (opponent == null || opponent.getSavedParties().isEmpty()) return false;
        if (loggedInProfile.getSavedParties().isEmpty()) return false;

        Party myParty  = loggedInProfile.getSavedParties().get(myPartyIndex);
        Party oppParty = opponent.getSavedParties().get(0);

        // Challenger (myParty) always goes first (isOpponentTurn = false by default)
        battleEngine = new BattleEngine(myParty, oppParty, true, opponentUsername);
        transition(GameState.BATTLE);
        return true;
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    public void showMainMenu()  { transition(GameState.MAIN_MENU); }
    public void showProfile()   { transition(GameState.PROFILE_VIEW); }
    public void showPvpInvite() { transition(GameState.PVP_INVITE); }
    public void showLeaderboard() { transition(GameState.LEADERBOARD); }

    // ── Getters ───────────────────────────────────────────────────────────────
    public GameState getCurrentState()       { return currentState; }
    public Profile getLoggedInProfile()      { return loggedInProfile; }
    public CampaignState getActiveCampaign() { return activeCampaign; }
    public BattleEngine getBattleEngine()    { return battleEngine; }
}
