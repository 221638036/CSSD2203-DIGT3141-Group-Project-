package state;

import battle.BattleEngine;
import factory.GameFactory;
import model.*;
import persistence.FileDataManager;
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
        // GUI will prompt to save party; transition to profile
        transition(GameState.PROFILE_VIEW);
    }

    public void battleEnded(boolean won) {
        activeCampaign.setInBattle(false);
        FileDataManager.getInstance().saveProfile(loggedInProfile);
        transition(won ? GameState.CAMPAIGN_MAP : GameState.GAME_OVER);
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

        // Represent opponent heroes as enemies
        List<Enemy> oppAsEnemies = new ArrayList<>();
        for (Hero h : oppParty.getMembers())
            oppAsEnemies.add(new Enemy(h.getName() + " (" + opponentUsername + ")", h.getLevel()));

        battleEngine = new BattleEngine(myParty, oppAsEnemies);
        transition(GameState.BATTLE);
        return true;
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    public void showMainMenu()  { transition(GameState.MAIN_MENU); }
    public void showProfile()   { transition(GameState.PROFILE_VIEW); }
    public void showPvpInvite() { transition(GameState.PVP_INVITE); }

    // ── Getters ───────────────────────────────────────────────────────────────
    public GameState getCurrentState()       { return currentState; }
    public Profile getLoggedInProfile()      { return loggedInProfile; }
    public CampaignState getActiveCampaign() { return activeCampaign; }
    public BattleEngine getBattleEngine()    { return battleEngine; }
}
