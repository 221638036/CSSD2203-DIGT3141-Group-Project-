package gui;

import battle.*;
import model.*;
import state.GameStateManager;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BattlePanel extends JPanel implements BattleObserver {

    private JLabel    turnLabel;
    private JPanel    enemyPanel;
    private JPanel    heroPanel;
    private JPanel    actionPanel;
    private JTextArea battleLog;
    private BattleEngine engine;

    public BattlePanel() {
        setLayout(new BorderLayout(6, 6));
        setBackground(UI.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── North: turn label ─────────────────────────────────────────────────
        turnLabel = UI.centeredLabel("Battle!", UI.ACCENT_RED, Font.BOLD, 20);
        add(turnLabel, BorderLayout.NORTH);

        // ── Centre: enemy row / hero row ──────────────────────────────────────
        JPanel centre = UI.bgPanel(new GridLayout(2, 1, 4, 4));

        enemyPanel = UI.bgPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        enemyPanel.setBackground(new Color(35, 10, 10));
        enemyPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UI.ACCENT_RED), "Enemies",
            0, 0, UI.FONT_SMALL, UI.ACCENT_RED));

        heroPanel = UI.bgPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        heroPanel.setBackground(new Color(10, 18, 40));
        heroPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UI.ACCENT_BLUE), "Your Party",
            0, 0, UI.FONT_SMALL, UI.ACCENT_BLUE));

        centre.add(enemyPanel);
        centre.add(heroPanel);
        add(centre, BorderLayout.CENTER);

        // ── East: battle log ──────────────────────────────────────────────────
        battleLog = UI.logArea();
        JScrollPane logScroll = UI.scrollPane(battleLog, "Battle Log", Color.GRAY);
        logScroll.setPreferredSize(new Dimension(240, 0));
        add(logScroll, BorderLayout.EAST);

        // ── South: action buttons ─────────────────────────────────────────────
        actionPanel = UI.bgPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        actionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Actions",
            0, 0, UI.FONT_SMALL, Color.GRAY));
        add(actionPanel, BorderLayout.SOUTH);
    }

    /** Called by MainWindow every time BATTLE state is entered */
    public void refresh() {
        engine = GameStateManager.getInstance().getBattleEngine();
        if (engine == null) return;
        engine.addObserver(this);
        battleLog.setText("⚔  Battle begins!\n\n");
        updateDisplay();
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    private void updateDisplay() {
        if (engine == null) return;
        rebuildEnemyPanel();
        rebuildHeroPanel();
        rebuildActionPanel();
        revalidate();
        repaint();
    }

    private void rebuildEnemyPanel() {
        enemyPanel.removeAll();
        for (Enemy e : engine.getAliveEnemies()) {
            enemyPanel.add(makeEnemyCard(e));
        }
        // Show defeated enemies greyed out
        for (Enemy e : engine.getEnemies()) {
            if (!e.isAlive()) enemyPanel.add(makeDefeatedEnemyCard(e));
        }
    }

    private void rebuildHeroPanel() {
        heroPanel.removeAll();
        Hero current = engine.getCurrentHero();
        for (Hero h : engine.getPlayerParty().getMembers()) {
            heroPanel.add(makeHeroCard(h, h == current));
        }
    }

    private void rebuildActionPanel() {
        actionPanel.removeAll();
        Hero current = engine.getCurrentHero();
        if (current == null || !current.isAlive() || engine.isBattleOver()) return;

        turnLabel.setText("⚔  " + current.getName() + "'s turn  [" + current.getHeroClass() + "]");

        List<BattleAction> actions = BattleActions.getActionsForHero(current);
        List<Enemy> alive = engine.getAliveEnemies();

        for (BattleAction action : actions) {
            JButton btn = UI.button(action.getName(),
                action.canUse(current) ? new Color(50, 65, 130) : new Color(35, 35, 55));
            btn.setEnabled(action.canUse(current) && !alive.isEmpty());
            btn.addActionListener(e -> {
                engine.playerAction(action, 0);
                if (!engine.isBattleOver()) updateDisplay();
            });
            actionPanel.add(btn);
        }
    }

    // ── Card builders ─────────────────────────────────────────────────────────

    private JPanel makeEnemyCard(Enemy e) {
        JPanel card = new JPanel(new GridLayout(4, 1, 2, 2));
        card.setBackground(new Color(55, 15, 15));
        card.setBorder(BorderFactory.createLineBorder(UI.ACCENT_RED));
        card.setPreferredSize(new Dimension(145, 105));

        card.add(UI.centeredLabel(e.getName(), new Color(255, 140, 140), Font.BOLD, 12));
        card.add(UI.centeredLabel("Lv " + e.getLevel(), UI.TEXT_DIM, Font.PLAIN, 11));
        card.add(UI.enemyHpBar(e.getHp(), e.getMaxHp()));
        card.add(UI.centeredLabel("ATK " + e.getAttack() + "  DEF " + e.getDefense(), UI.TEXT_DIM, Font.PLAIN, 10));
        return card;
    }

    private JPanel makeDefeatedEnemyCard(Enemy e) {
        JPanel card = new JPanel(new GridLayout(2, 1, 2, 2));
        card.setBackground(new Color(25, 10, 10));
        card.setBorder(BorderFactory.createLineBorder(new Color(60, 30, 30)));
        card.setPreferredSize(new Dimension(145, 60));
        card.add(UI.centeredLabel(e.getName(), new Color(90, 60, 60), Font.BOLD, 12));
        card.add(UI.centeredLabel("☠ Defeated", new Color(90, 60, 60), Font.ITALIC, 11));
        return card;
    }

    private JPanel makeHeroCard(Hero h, boolean active) {
        JPanel card = new JPanel(new GridLayout(5, 1, 2, 2));
        Color bg     = active ? new Color(22, 42, 85) : new Color(18, 25, 52);
        Color border = active ? UI.ACCENT_BLUE        : new Color(45, 65, 110);
        card.setBackground(bg);
        card.setBorder(BorderFactory.createLineBorder(border, active ? 2 : 1));
        card.setPreferredSize(new Dimension(155, 125));

        Color nameColor = !h.isAlive() ? UI.TEXT_DIM : active ? new Color(140, 195, 255) : UI.TEXT_MAIN;
        card.add(UI.centeredLabel(h.getName(), nameColor, Font.BOLD, 13));
        card.add(UI.centeredLabel(h.getHeroClass() + "  Lv" + h.getLevel(), UI.TEXT_DIM, Font.PLAIN, 11));

        if (h.isAlive()) {
            card.add(UI.hpBar(h.getHp(), h.getMaxHp()));
            card.add(UI.mpBar(h.getMana(), h.getMaxMana()));
            String status = h.isStunned() ? "⚡ STUNNED" : active ? "▶ Acting" : "";
            card.add(UI.centeredLabel(status, h.isStunned() ? UI.ACCENT_GOLD : UI.ACCENT_BLUE, Font.PLAIN, 10));
        } else {
            card.add(UI.centeredLabel("☠ Defeated", UI.ACCENT_RED, Font.ITALIC, 12));
            card.add(new JLabel());
            card.add(new JLabel());
        }
        return card;
    }

    // ── BattleObserver ────────────────────────────────────────────────────────

    @Override
    public void onBattleEvent(String message) {
        SwingUtilities.invokeLater(() -> {
            battleLog.append(message + "\n");
            battleLog.setCaretPosition(battleLog.getDocument().getLength());
            updateDisplay();
        });
    }

    @Override
    public void onBattleEnd(boolean playerWon) {
        SwingUtilities.invokeLater(() -> {
            turnLabel.setText(playerWon ? "🏆  Victory!" : "💀  Defeat...");
            actionPanel.removeAll();
            JButton continueBtn = playerWon ? UI.successButton("Continue →") : UI.dangerButton("Continue →");
            continueBtn.addActionListener(e -> GameStateManager.getInstance().battleEnded(playerWon));
            actionPanel.add(continueBtn);
            revalidate();
            repaint();
        });
    }

    @Override
    public void onTurnChanged(int heroIndex) {
        SwingUtilities.invokeLater(this::updateDisplay);
    }
}
