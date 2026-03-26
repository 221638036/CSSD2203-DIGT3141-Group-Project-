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

    private JTable leaderboardTable;
    private JTabbedPane tabbedPane;

    public LeaderboardPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UI.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(UI.title("🏆  Hall of Fame"), BorderLayout.NORTH);

        // Tabbed pane for different leaderboards
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(UI.BG_DARK);
        tabbedPane.setForeground(UI.TEXT_MAIN);

        // Tab 1: Campaign High Scores
        JPanel scoresPanel = createScoresPanel();
        tabbedPane.addTab("Campaign Scores", scoresPanel);

        // Tab 2: Win/Loss Records
        JPanel recordsPanel = createRecordsPanel();
        tabbedPane.addTab("Battle Records", recordsPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Back button
        JPanel south = UI.bgPanel(new FlowLayout(FlowLayout.CENTER));
        JButton backBtn = UI.primaryButton("⬅  Back to Menu");
        backBtn.addActionListener(e -> GameStateManager.getInstance().showMainMenu());
        south.add(backBtn);
        add(south, BorderLayout.SOUTH);
    }

    private JPanel createScoresPanel() {
        JPanel panel = UI.darkPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Rank", "Username", "Best Score", "Total Wins"}, 0
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        leaderboardTable = new JTable(model);
        leaderboardTable.setBackground(UI.BG_PANEL);
        leaderboardTable.setForeground(UI.TEXT_MAIN);
        leaderboardTable.setRowHeight(25);
        leaderboardTable.getTableHeader().setBackground(UI.BG_CARD);
        leaderboardTable.getTableHeader().setForeground(UI.ACCENT_GOLD);

        JScrollPane scroll = new JScrollPane(leaderboardTable);
        scroll.setBackground(UI.BG_DARK);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRecordsPanel() {
        JPanel panel = UI.darkPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Rank", "Username", "Wins", "Losses", "Win Rate"}, 0
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable recordsTable = new JTable(model);
        recordsTable.setBackground(UI.BG_PANEL);
        recordsTable.setForeground(UI.TEXT_MAIN);
        recordsTable.setRowHeight(25);
        recordsTable.getTableHeader().setBackground(UI.BG_CARD);
        recordsTable.getTableHeader().setForeground(UI.ACCENT_GOLD);

        JScrollPane scroll = new JScrollPane(recordsTable);
        scroll.setBackground(UI.BG_DARK);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            // Update scores leaderboard
            DefaultTableModel scoresModel = (DefaultTableModel) leaderboardTable.getModel();
            scoresModel.setRowCount(0);

            List<Profile> profiles = FileDataManager.getInstance().getAllProfiles();
            List<ProfileScore> scoreList = new ArrayList<>();
            for (Profile p : profiles) {
                int bestScore = p.getBestScore();
                if (bestScore > 0) {
                    scoreList.add(new ProfileScore(p.getUsername(), bestScore, p.getWins()));
                }
            }
            scoreList.sort((a, b) -> b.score - a.score);

            int rank = 1;
            for (ProfileScore ps : scoreList) {
                scoresModel.addRow(new Object[]{rank++, ps.username, ps.score, ps.wins});
            }

            // Update records leaderboard
            JTable recordsTable = (JTable) ((JScrollPane) tabbedPane.getComponentAt(1)).getViewport().getView();
            DefaultTableModel recordsModel = (DefaultTableModel) recordsTable.getModel();
            recordsModel.setRowCount(0);

            List<ProfileRecord> recordList = new ArrayList<>();
            for (Profile p : profiles) {
                int total = p.getTotalBattles();
                if (total > 0) {
                    recordList.add(new ProfileRecord(p.getUsername(), p.getWins(), p.getLosses()));
                }
            }
            recordList.sort((a, b) -> {
                double aRate = (double) a.wins / (a.wins + a.losses);
                double bRate = (double) b.wins / (b.wins + b.losses);
                return Double.compare(bRate, aRate);
            });

            rank = 1;
            for (ProfileRecord pr : recordList) {
                double winRate = (double) pr.wins / (pr.wins + pr.losses) * 100;
                recordsModel.addRow(new Object[]{
                    rank++, pr.username, pr.wins, pr.losses,
                    String.format("%.1f%%", winRate)
                });
            }
        });
    }

    private static class ProfileScore {
        String username;
        int score;
        int wins;

        ProfileScore(String username, int score, int wins) {
            this.username = username;
            this.score = score;
            this.wins = wins;
        }
    }

    private static class ProfileRecord {
        String username;
        int wins;
        int losses;

        ProfileRecord(String username, int wins, int losses) {
            this.username = username;
            this.wins = wins;
            this.losses = losses;
        }
    }
}
