package gui;

import model.*;
import persistence.FileDataManager;
import state.GameStateManager;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ProfilePanel extends JPanel {

    private final JLabel    userLabel;
    private final JLabel    bestScoreLabel;
    private final JPanel    partiesPanel;
    private final JTextArea scoresArea;

    public ProfilePanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UI.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header
        JPanel header = UI.bgPanel(new GridLayout(2, 1, 0, 2));
        userLabel      = UI.title("Profile");
        bestScoreLabel = UI.centeredLabel("Best Score: 0", UI.TEXT_DIM, Font.PLAIN, 14);
        header.add(userLabel);
        header.add(bestScoreLabel);
        add(header, BorderLayout.NORTH);

        // Centre
        JPanel centre = UI.bgPanel(new GridLayout(1, 2, 12, 0));

        partiesPanel = UI.darkPanel(null);
        partiesPanel.setLayout(new BoxLayout(partiesPanel, BoxLayout.Y_AXIS));
        centre.add(UI.scrollPane(partiesPanel, "Saved Parties  (max 5)", Color.GRAY));

        scoresArea = UI.logArea();
        scoresArea.setBackground(UI.BG_PANEL);
        centre.add(UI.scrollPane(scoresArea, "Campaign Scores", Color.GRAY));

        add(centre, BorderLayout.CENTER);

        // Back
        JPanel south = UI.bgPanel(new FlowLayout(FlowLayout.CENTER));
        JButton back = UI.primaryButton("← Back to Menu");
        back.addActionListener(e -> GameStateManager.getInstance().showMainMenu());
        south.add(back);
        add(south, BorderLayout.SOUTH);
    }

    public void refresh() {
        Profile profile = GameStateManager.getInstance().getLoggedInProfile();
        if (profile == null) return;

        userLabel.setText("⚔  " + profile.getUsername() + "'s Profile");
        bestScoreLabel.setText("Best Score:  " + profile.getBestScore());

        // Parties
        partiesPanel.removeAll();
        List<Party> parties = profile.getSavedParties();
        if (parties.isEmpty()) {
            JLabel none = UI.label("  No saved parties yet.", UI.TEXT_DIM);
            none.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            partiesPanel.add(none);
        } else {
            for (int i = 0; i < parties.size(); i++) {
                partiesPanel.add(makePartyRow(profile, parties.get(i), i));
                partiesPanel.add(Box.createVerticalStrut(4));
            }
        }

        // Scores
        List<Integer> scores = profile.getScores();
        if (scores.isEmpty()) {
            scoresArea.setText("No completed campaigns yet.\n\nFinish a 30-room campaign to earn a score!");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < scores.size(); i++)
                sb.append("Run ").append(i + 1).append(":  ").append(scores.get(i)).append(" pts\n");
            scoresArea.setText(sb.toString());
        }

        revalidate();
        repaint();
    }

    private JPanel makePartyRow(Profile profile, Party party, int index) {
        JPanel row = UI.darkPanel(new BorderLayout(6, 0));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(55, 55, 90)),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        StringBuilder html = new StringBuilder("<html><b>" + party.getName() + "</b>  (" + party.getGold() + "g)<br>");
        for (Hero h : party.getMembers())
            html.append("&nbsp;&nbsp;").append(h.getName()).append("  ").append(h.getHeroClass())
                .append("  Lv").append(h.getLevel()).append("<br>");
        html.append("</html>");

        JLabel lbl = new JLabel(html.toString());
        lbl.setForeground(UI.TEXT_MAIN);
        row.add(lbl, BorderLayout.CENTER);

        JButton del = UI.dangerButton("✕");
        del.setFont(new Font("SansSerif", Font.BOLD, 11));
        del.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        del.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this,
                "Permanently delete \"" + party.getName() + "\"?",
                "Delete Party", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c == JOptionPane.YES_OPTION) {
                profile.removeParty(index);
                FileDataManager.getInstance().saveProfile(profile);
                refresh();
            }
        });
        row.add(del, BorderLayout.EAST);
        return row;
    }
}
