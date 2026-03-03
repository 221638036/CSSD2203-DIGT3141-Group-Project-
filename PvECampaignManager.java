package com.legends.campaign;

import com.legends.battle.BattleAction;
import com.legends.battle.BattleEngine;
import com.legends.battle.BattleResult;
import com.legends.db.GameRepository;
import com.legends.enemy.EnemyFactory;
import com.legends.inn.InnManager;
import com.legends.model.*;
import com.legends.profile.ProfileManager;
import com.legends.progression.IProgressionService;

import java.util.Random;

/**
 * Orchestrates the 30-room PvE campaign: room generation, battle/inn dispatch,
 * progress persistence, and campaign scoring.
 *
 * Acts as a façade to other subsystems, exposing a simple interface to
 * GameController while hiding coordination of BattleEngine, InnManager,
 * EnemyFactory, and HeroProgressionSystem.
 *
 * Collaborators are injected as interfaces/abstract types; the manager
 * depends on no concrete implementations.
 *
 * Enemy generation is delegated entirely to EnemyFactory; this class only
 * decides when rooms occur and their type.
 */
public class PvECampaignManager {

    private static final Random RNG               = new Random();
    private static final int    BASE_BATTLE_CHANCE = 60; // percent

    // ── Injected collaborators (all abstractions — DIP) ───────────────────────
    private final GameRepository      repo;
    private final ProfileManager      profileManager;
    private final BattleEngine        battleEngine;
    private final InnManager          innManager;
    private final EnemyFactory        enemyFactory;
    private final IProgressionService progression;

    private CampaignProgress currentProgress;

    /**
     * Full constructor: all dependencies injected (DIP + Low Coupling).
     */
    public PvECampaignManager(GameRepository repo,
                               ProfileManager profileManager,
                               BattleEngine battleEngine,
                               InnManager innManager,
                               EnemyFactory enemyFactory,
                               IProgressionService progression) {
        this.repo           = repo;
        this.profileManager = profileManager;
        this.battleEngine   = battleEngine;
        this.innManager     = innManager;
        this.enemyFactory   = enemyFactory;
        this.progression    = progression;
    }

    // ── Campaign lifecycle ────────────────────────────────────────────────────

    public CampaignProgress startCampaign(UserProfile profile, Hero startingHero) {
        Party party = new Party(profile.getUsername() + "'s Party");
        party.addHero(startingHero);
        currentProgress = new CampaignProgress(profile.getId(), party);
        profile.setCampaignProgress(currentProgress);
        repo.saveCampaignProgress(currentProgress);
        repo.updateProfile(profile);
        return currentProgress;
    }

    public boolean loadCampaign(UserProfile profile) {
        var opt = repo.loadCampaignProgress(profile.getId());
        if (opt.isEmpty()) return false;
        currentProgress = opt.get();
        profile.setCampaignProgress(currentProgress);
        return true;
    }

    // ── Room progression ──────────────────────────────────────────────────────

    public enum RoomType { BATTLE, INN }

    /**
     * Roll room type based on cumulative party level.
     * Battle chance: min(90, 60 + floor(cumLevel/10)*3).
     */
    public RoomType rollNextRoom(Party party) {
        int cumLevel    = party.getCumulativeLevel();
        int tenBracket  = Math.min(cumLevel / 10, 9);
        int battleChance = Math.min(90, BASE_BATTLE_CHANCE + tenBracket * 3);
        return RNG.nextInt(100) < battleChance ? RoomType.BATTLE : RoomType.INN;
    }

    /**
     * Advance to the next room. Delegates to BattleEngine or InnManager.
     */
    public String visitNextRoom(UserProfile profile) {
        if (currentProgress == null) return "No active campaign.";
        currentProgress.advanceRoom();
        Party party = currentProgress.getParty();

        if (currentProgress.isComplete()) return endCampaign(profile);

        RoomType type = rollNextRoom(party);
        currentProgress.setLastRoomWasInn(type == RoomType.INN);
        repo.saveCampaignProgress(currentProgress);

        if (type == RoomType.INN) {
            return "[INN] Room " + currentProgress.getCurrentRoom() + "\n"
                    + innManager.visitInn(party);
        } else {
            // Delegate enemy generation to EnemyFactory (SRP, DIP)
            Party enemyParty = enemyFactory.generateParty(party.getCumulativeLevel());
            battleEngine.startBattle(party, enemyParty);
            return "[BATTLE] Room " + currentProgress.getCurrentRoom()
                    + " — Enemy party:\n" + enemyParty;
        }
    }

    public String executeBattleTurn(BattleAction action, Hero target, int abilityIdx) {
        return battleEngine.executeTurn(action, target, abilityIdx);
    }

    /**
     * Resolve a finished battle: distribute XP via IProgressionService, award gold.
     */
    public String resolveBattle(UserProfile profile) {
        BattleResult result = battleEngine.finaliseBattle(
                battleEngine.getEnemyParty().getMembers());
        StringBuilder sb = new StringBuilder();
        sb.append("=== Battle Result: ").append(result.getOutcome()).append(" ===\n");

        if (result.getOutcome() == BattleResult.Outcome.PLAYER_WIN) {
            // Award XP through IProgressionService (DIP — no direct formula)
            int expShare = result.getExpGained() /
                    Math.max(1, profile.getCampaignProgress().getParty().getLivingMembers().size());
            for (Hero h : profile.getCampaignProgress().getParty().getLivingMembers()) {
                sb.append(progression.awardXp(h, expShare)).append("\n");
            }
            profile.earnGold(result.getGoldGained());
            sb.append("Gained ").append(result.getGoldGained()).append(" gold.\n");
        } else {
            int goldLost = (int)(profile.getGold() * 0.10);
            profile.spendGold(goldLost);
            sb.append("Lost ").append(goldLost).append(" gold.\n");
            currentProgress.getParty().reviveFallen();
            sb.append("Returned to last inn. Heroes revived to 1 HP.\n");
        }
        repo.updateProfile(profile);
        repo.saveCampaignProgress(currentProgress);
        return sb.toString();
    }

    // ── Campaign end ──────────────────────────────────────────────────────────

    private String endCampaign(UserProfile profile) {
        currentProgress.setActive(false);
        int score = profileManager.calculateAndSaveScore(profile);
        repo.saveCampaignProgress(currentProgress);
        return "=== Campaign Complete! ===\nFinal Score: " + score;
    }

    public String saveAndExit(UserProfile profile) {
        if (currentProgress == null) return "No active campaign.";
        repo.saveCampaignProgress(currentProgress);
        repo.updateProfile(profile);
        return "Progress saved. See you next time!";
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public CampaignProgress getCurrentProgress() { return currentProgress; }
    public BattleEngine      getBattleEngine()    { return battleEngine; }
    public InnManager        getInnManager()      { return innManager; }
}
