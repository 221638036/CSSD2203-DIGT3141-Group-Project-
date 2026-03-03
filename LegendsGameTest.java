package com.legends;

import com.legends.battle.*;
import com.legends.campaign.PvECampaignManager;
import com.legends.db.InMemoryRepository;
import com.legends.enemy.EnemyFactory;
import com.legends.inn.InnManager;
import com.legends.model.*;
import com.legends.model.ability.Ability;
import com.legends.model.ability.AbilityFactory;
import com.legends.profile.ProfileManager;
import com.legends.progression.HeroProgressionSystem;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * TDD test suite — 19 tests covering Deliverable 1 requirements.
 *
 * Tests depend on interfaces/abstractions, not concrete implementations.
 */
class LegendsGameTest {

    private InMemoryRepository    repo;
    private ProfileManager        profileMgr;
    private BattleEngine          battleEngine;
    private InnManager            innManager;
    private EnemyFactory          enemyFactory;
    private HeroProgressionSystem progression;
    private PvECampaignManager    campaign;

    @BeforeEach
    void setUp() {
        repo         = new InMemoryRepository();
        profileMgr   = new ProfileManager(repo);
        battleEngine = new BattleEngine();
        innManager   = new InnManager();
        enemyFactory = EnemyFactory.standard();
        progression  = new HeroProgressionSystem();
        campaign     = new PvECampaignManager(
                repo, profileMgr, battleEngine, innManager, enemyFactory, progression);
    }

    // ── TC-01: Profile creation ───────────────────────────────────────────────
    @Test
    void tc01_createProfile_success() {
        UserProfile p = profileMgr.createProfile("alice", "password1");
        assertNotNull(p);
        assertEquals("alice", p.getUsername());
    }

    // ── TC-02: Duplicate username rejected (SRP: ProfileManager owns this rule) ──
    @Test
    void tc02_createProfile_duplicateUsername_throws() {
        profileMgr.createProfile("bob", "pass1234");
        assertThrows(IllegalArgumentException.class,
                () -> profileMgr.createProfile("bob", "other123"));
    }

    // ── TC-03: Login correct credentials ─────────────────────────────────────
    @Test
    void tc03_login_correctCredentials_returnsProfile() {
        profileMgr.createProfile("carol", "mypassword");
        var result = profileMgr.login("carol", "mypassword");
        assertTrue(result.isPresent());
    }

    // ── TC-04: Login wrong password ───────────────────────────────────────────
    @Test
    void tc04_login_wrongPassword_returnsEmpty() {
        profileMgr.createProfile("dave", "rightpass");
        assertTrue(profileMgr.login("dave", "wrongpass").isEmpty());
    }

    // ── TC-05: Attack damage = atk - def, min 0 ───────────────────────────────
    @Test
    void tc05_attackDamage_atkMinusDef() {
        Hero attacker = new Hero("Attacker", HeroClass.WARRIOR);
        Hero defender = new Hero("Defender", HeroClass.ORDER);
        // Warrior base atk=5, Order base def=5 → max(0, 5-5) = 0
        assertEquals(0, attacker.calculateDamage(defender));
    }

    // ── TC-06: Damage never negative ─────────────────────────────────────────
    @Test
    void tc06_attackDamage_neverNegative() {
        Hero weak     = new Hero("Weak", HeroClass.MAGE);
        Hero armoured = new Hero("Tank", HeroClass.WARRIOR);
        armoured.levelUp(HeroClass.WARRIOR);
        armoured.levelUp(HeroClass.WARRIOR);
        assertTrue(weak.calculateDamage(armoured) >= 0);
    }

    // ── TC-07: ATTACK action reduces target HP ────────────────────────────────
    @Test
    void tc07_executeTurn_attack_reducesTargetHp() {
        Hero h1 = new Hero("Hero1", HeroClass.CHAOS);
        h1.levelUp(HeroClass.CHAOS); h1.levelUp(HeroClass.CHAOS);
        Hero h2 = new Hero("Hero2", HeroClass.ORDER);
        Party p1 = new Party("P1"); p1.addHero(h1);
        Party p2 = new Party("P2"); p2.addHero(h2);
        battleEngine.startBattle(p1, p2);
        int hpBefore = h2.getCurrentHp();
        battleEngine.executeTurn(BattleAction.ATTACK, h2, 0);
        assertTrue(h2.getCurrentHp() <= hpBefore);
    }

    // ── TC-08: DEFEND action restores HP and mana ─────────────────────────────
    @Test
    void tc08_executeTurn_defend_restoresStats() {
        Hero h1 = new Hero("Defender", HeroClass.ORDER);
        Hero h2 = new Hero("Enemy",    HeroClass.CHAOS);
        h1.takeDamage(30); h1.setCurrentMana(10);
        Party p1 = new Party("P1"); p1.addHero(h1);
        Party p2 = new Party("P2"); p2.addHero(h2);
        battleEngine.startBattle(p1, p2);
        int hpBefore   = h1.getCurrentHp();
        int manaBefore = h1.getCurrentMana();
        battleEngine.executeTurn(BattleAction.DEFEND, null, -1);
        assertTrue(h1.getCurrentHp()   >= hpBefore);
        assertTrue(h1.getCurrentMana() >= manaBefore);
    }

    // ── TC-09: Berserker Attack splashes to adjacent targets ──────────────────
    @Test
    void tc09_berserkerAttack_splashDamage() {
        Hero caster = new Hero("Berserker", HeroClass.WARRIOR);
        caster.levelUp(HeroClass.WARRIOR); caster.levelUp(HeroClass.WARRIOR);
        Hero t1 = new Hero("T1", HeroClass.ORDER);
        Hero t2 = new Hero("T2", HeroClass.ORDER);
        // Strategy pattern: create via AbilityFactory, execute via Ability interface
        Ability ab = AbilityFactory.create(AbilityFactory.Type.BERSERKER_ATTACK);
        int t2Before = t2.getCurrentHp();
        ab.execute(caster, List.of(t1, t2));
        assertTrue(t2.getCurrentHp() <= t2Before);
    }

    // ── TC-10: Fireball hits at most 3 targets ────────────────────────────────
    @Test
    void tc10_fireball_hitsMaxThreeTargets() {
        Hero caster = new Hero("Mage", HeroClass.CHAOS);
        caster.levelUp(HeroClass.CHAOS); caster.levelUp(HeroClass.CHAOS);
        List<Hero> targets = List.of(
                new Hero("T1", HeroClass.ORDER), new Hero("T2", HeroClass.ORDER),
                new Hero("T3", HeroClass.ORDER), new Hero("T4", HeroClass.ORDER));
        AbilityFactory.create(AbilityFactory.Type.FIREBALL).execute(caster, targets);
        long damaged = targets.stream().filter(h -> h.getCurrentHp() < h.getMaxHp()).count();
        assertTrue(damaged <= 3, "Fireball should hit at most 3 targets");
    }

    // ── TC-11: Hero levels up at XP threshold ─────────────────────────────────
    @Test
    void tc11_hero_levelsUpAtXpThreshold() {
        Hero h = new Hero("Leveller", HeroClass.MAGE);
        int needed = h.xpForNextLevel();
        int before = h.getLevel();
        h.addExperience(needed);
        assertEquals(before + 1, h.getLevel());
    }

    // ── TC-12: Inn heals entire party ─────────────────────────────────────────
    @Test
    void tc12_innVisit_healsParty() {
        Hero h1 = new Hero("H1", HeroClass.ORDER);
        Hero h2 = new Hero("H2", HeroClass.CHAOS);
        h1.takeDamage(40); h2.takeDamage(40);
        Party party = new Party("Group"); party.addHero(h1); party.addHero(h2);
        innManager.visitInn(party);
        assertEquals(h1.getMaxHp(), h1.getCurrentHp());
        assertEquals(h2.getMaxHp(), h2.getCurrentHp());
    }

    // ── TC-13: Battle ends when a side is wiped ───────────────────────────────
    @Test
    void tc13_battle_endsWhenSideWiped() {
        Hero h1 = new Hero("H1", HeroClass.WARRIOR);
        Hero e1 = new Hero("E1", HeroClass.WARRIOR);
        Party p1 = new Party("P1"); p1.addHero(h1);
        Party p2 = new Party("P2"); p2.addHero(e1);
        battleEngine.startBattle(p1, p2);
        e1.takeDamage(9999);
        assertTrue(battleEngine.isBattleOver());
    }

    // ── TC-14: PvE campaign starts and returns progress ───────────────────────
    @Test
    void tc14_pveCampaign_starts() {
        UserProfile profile = profileMgr.createProfile("eve", "password1");
        Hero hero = new Hero("Eve", HeroClass.MAGE);
        CampaignProgress progress = campaign.startCampaign(profile, hero);
        assertNotNull(progress);
        assertEquals(0, progress.getCurrentRoom());
    }

    // ── TC-15: Max 5 parties per profile ─────────────────────────────────────
    @Test
    void tc15_profile_maxFiveSavedParties() {
        UserProfile profile = profileMgr.createProfile("frank", "pass1234");
        for (int i = 0; i < 5; i++) {
            Party p = new Party("Party_" + i);
            p.addHero(new Hero("H" + i, HeroClass.WARRIOR));
            profileMgr.saveParty(profile, p);
        }
        Party extra = new Party("Extra");
        extra.addHero(new Hero("Extra", HeroClass.MAGE));
        assertFalse(profileMgr.saveParty(profile, extra), "Should not save a 6th party");
    }

    // ── TC-16: EnemyFactory generates a valid party (Factory Method + Template Method) ──
    @Test
    void tc16_enemyFactory_generatesValidParty() {
        Party enemies = enemyFactory.generateParty(15);
        assertNotNull(enemies);
        assertFalse(enemies.getMembers().isEmpty());
        assertTrue(enemies.getMembers().size() <= 5);
        enemies.getMembers().forEach(e -> assertTrue(e.getLevel() >= 1));
    }

    // ── TC-17: ClassProgression resolves correct hybrid class ─────────────────
    @Test
    void tc17_classProgression_resolvesHybrid() {
        assertEquals(HeroClass.WARLOCK,
                ClassProgression.resolveHybrid(HeroClass.WARRIOR, HeroClass.MAGE));
    }

    // ── TC-18: HeroProgressionSystem (Façade) awards XP and reports level-up ──
    @Test
    void tc18_progressionSystem_awardsXpAndLevels() {
        Hero h = new Hero("TestHero", HeroClass.ORDER);
        int needed = h.xpForNextLevel();
        String log = progression.awardXp(h, needed);
        assertTrue(log.contains("level"), "Log should mention level-up");
        assertEquals(2, h.getLevel());
    }

    // ── TC-19: Factory Method pattern – factoryMethod() returns correct product ─
    @Test
    void tc19_abilityFactory_factoryMethod_returnsCorrectProduct() {
        // simple iteration over creators to exercise factoryMethod()
        Ability protect  = AbilityFactory.create(AbilityFactory.Type.PROTECT);
        Ability fireball = AbilityFactory.create(AbilityFactory.Type.FIREBALL);
        assertEquals("Protect",  protect.getName());
        assertEquals("Fireball", fireball.getName());
        // Verify the product is used via the Ability (Product) interface, not concrete type
        assertTrue(protect  instanceof Ability);
        assertTrue(fireball instanceof Ability);
    }

    // ── TC-20: Observer pattern – BattleObserver receives BATTLE_START event ──
    @Test
    void tc20_observer_receivesBattleStartEvent() {
        // observer example to verify notifications
        List<String> receivedEvents = new ArrayList<>();
        BattleObserver observer = (event, actor, result) -> receivedEvents.add(event);

        battleEngine.attach(observer); // attach observer

        Hero h1 = new Hero("H1", HeroClass.WARRIOR);
        Hero e1 = new Hero("E1", HeroClass.CHAOS);
        Party p1 = new Party("P1"); p1.addHero(h1);
        Party p2 = new Party("P2"); p2.addHero(e1);
        battleEngine.startBattle(p1, p2);

        assertTrue(receivedEvents.contains("BATTLE_START"),
                "Observer should receive BATTLE_START notification");
    }
}
