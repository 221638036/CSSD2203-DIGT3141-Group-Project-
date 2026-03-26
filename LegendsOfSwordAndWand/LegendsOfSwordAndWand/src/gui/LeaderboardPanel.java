package gui;

import model.Profile;
import persistence.FileDataManager;
import state.GameStateManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class LeaderboardPanel extends JPanel {

    private JTable scoresTable;
    private JTable winsTable;
    private JTable recordsTable;

    public LeaderboardPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UI.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(UI.title("🏆  Hall of Fame"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(UI.BG_DARK);
        tabs.setForeground(UI.TEXT_MAIN);
        tabs.setFont(UI.FONT_BOLD);

        tabs.addTab("🥇 Most Wins",        buildWinsPanel());
        tabs.addTab("⭐ Campaign Scores",   buildScoresPanel());
        tabs.addTab("📊 Battle Records",    buildRecordsPanel());

        add(tabs, BorderLayout.CENTER);

        JPanel south = UI.bgPanel(new FlowLayout(FlowLayout.CENTER));
        JButton back = UI.primaryButton("⬅  Back to Menu");
        back.addActionListener(e -> GameStateManager.getInstance().showMainMenu());
        south.add(back);
        add(south, BorderLayout.SOUTH);
    }

    private JPanel buildWinsPanel() {
        JPanel p = UI.darkPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        winsTable = styledTable(new String[]{"Rank", "Player", "PvP Wins", "PvP Losses", "Win Rate"});
        p.add(new JScrollPane(winsTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildScoresPanel() {
        JPanel p = UI.darkPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scoresTable = styledTable(new String[]{"Rank", "Player", "Best Score"});
        p.add(new JScrollPane(scoresTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildRecordsPanel() {
        JPanel p = UI.darkPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        recordsTable = styledTable(new String[]{"Rank", "Player", "Wins", "Losses", "Win Rate"});
        p.add(new JScrollPane(recordsTable), BorderLayout.CENTER);
        return p;
    }

    private JTable styledTable(String[] columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(model);
        t.setBackground(UI.BG_PANEL);
        t.setForeground(UI.TEXT_MAIN);
        t.setSelectionBackground(new Color(60, 60, 100));
        t.setRowHeight(26);
        t.setFont(UI.FONT_BODY);
        t.getTableHeader().setBackground(UI.BG_CARD);
        t.getTableHeader().setForeground(UI.ACCENT_GOLD);
        t.getTableHeader().setFont(UI.FONT_BOLD);
        return t;
    }

    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            List<Profile> profiles = FileDataManager.getInstance().getAllProfiles();

            // ── Most Wins tab ─────────────────────────────────────────────────
            DefaultTableModel winsModel = (DefaultTableModel) winsTable.getModel();
            winsModel.setRowCount(0);
            List<Profile> byWins = new ArrayList<>(profiles);
            byWins.sort((a, b) -> b.getWins() - a.getWins());
            int rank = 1;
            for (Profile p : byWins) {
                int total = p.getWins() + p.getLosses();
                String rate = total == 0 ? "—" : String.format("%.1f%%", 100.0 * p.getWins() / total);
                winsModel.addRow(new Object[]{rank++, p.getUsername(), p.getWins(), p.getLosses(), rate});
            }

            // ── Campaign Scores tab ───────────────────────────────────────────
            DefaultTableModel scoresModel = (DefaultTableModel) scoresTable.getModel();
            scoresModel.setRowCount(0);
            List<Profile> byScore = new ArrayList<>(profiles);
            byScore.sort((a, b) -> b.getBestScore() - a.getBestScore());
            rank = 1;
            for (Profile p : byScore) {
                if (p.getBestScore() > 0)
                    scoresModel.addRow(new Object[]{rank++, p.getUsername(), p.getBestScore()});
            }

            // ── Battle Records tab ────────────────────────────────────────────
            DefaultTableModel recModel = (DefaultTableModel) recordsTable.getModel();
            recModel.setRowCount(0);
            List<Profile> byRate = new ArrayList<>(profiles);
            byRate.sort((a, b) -> {
                double ar = (a.getWins() + a.getLosses()) == 0 ? 0 : (double) a.getWins() / (a.getWins() + a.getLosses());
                double br = (b.getWins() + b.getLosses()) == 0 ? 0 : (double) b.getWins() / (b.getWins() + b.getLosses());
                return Double.compare(br, ar);
            });
            rank = 1;
            for (Profile p : byRate) {
                int total = p.getWins() + p.getLosses();
                String rate = total == 0 ? "—" : String.format("%.1f%%", 100.0 * p.getWins() / total);
                recModel.addRow(new Object[]{rank++, p.getUsername(), p.getWins(), p.getLosses(), rate});
            }
        });
    }
}
