package gui;

import model.HeroClass;
import state.GameStateManager;
import javax.swing.*;
import java.awt.*;

public class MainMenuPanel extends JPanel {

    private final JLabel welcomeLabel;

    public MainMenuPanel() {
        setLayout(new GridBagLayout());
        setBackground(UI.BG_DARK);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 30, 10, 30);
        g.fill   = GridBagConstraints.HORIZONTAL;

        welcomeLabel = UI.title("Welcome, Hero!");
        g.gridx = 0; g.gridy = 0;
        add(welcomeLabel, g);

        JLabel sub = UI.centeredLabel("What would you like to do?", UI.TEXT_DIM, Font.ITALIC, 14);
        g.gridy = 1;
        add(sub, g);

        JButton newCampaignBtn = UI.successButton("⚔  New PvE Campaign");
        JButton resumeBtn      = UI.primaryButton("▶  Resume Campaign");
        JButton pvpBtn         = UI.purpleButton("🏆  PvP Battle");
        JButton profileBtn     = UI.goldButton("👤  My Profile");
        JButton logoutBtn      = UI.dangerButton("⬅  Logout");

        newCampaignBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        resumeBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        pvpBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        profileBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        logoutBtn.setFont(new Font("SansSerif", Font.BOLD, 14));

        int row = 2;
        for (JButton btn : new JButton[]{newCampaignBtn, resumeBtn, pvpBtn, profileBtn, logoutBtn}) {
            g.gridy = row++;
            add(btn, g);
        }

        newCampaignBtn.addActionListener(e -> showNewCampaignDialog());
        resumeBtn.addActionListener(e -> {
            if (GameStateManager.getInstance().getLoggedInProfile() != null
                    && GameStateManager.getInstance().getLoggedInProfile().getActiveCampaign() != null) {
                GameStateManager.getInstance().resumeCampaign();
            } else {
                JOptionPane.showMessageDialog(this, "No active campaign to resume.", "Resume", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        pvpBtn.addActionListener(e -> GameStateManager.getInstance().showPvpInvite());
        profileBtn.addActionListener(e -> GameStateManager.getInstance().showProfile());
        logoutBtn.addActionListener(e -> GameStateManager.getInstance().logout());
    }

    private void showNewCampaignDialog() {
        JTextField nameField = UI.textField(14);
        nameField.setText("Hero");
        String[] classNames = {"ORDER", "CHAOS", "WARRIOR", "MAGE"};
        JComboBox<String> classBox = new JComboBox<>(classNames);
        classBox.setBackground(UI.BG_CARD);
        classBox.setForeground(UI.TEXT_MAIN);

        JPanel form = UI.darkPanel(new GridLayout(2, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        form.add(UI.label("Hero Name:", UI.TEXT_DIM));
        form.add(nameField);
        form.add(UI.label("Starting Class:", UI.TEXT_DIM));
        form.add(classBox);

        int result = JOptionPane.showConfirmDialog(this, form,
            "New Campaign", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "Hero";
            HeroClass cls = HeroClass.valueOf((String) classBox.getSelectedItem());
            GameStateManager.getInstance().startNewCampaign(name, cls);
        }
    }

    public void refresh() {
        var profile = GameStateManager.getInstance().getLoggedInProfile();
        if (profile != null) welcomeLabel.setText("Welcome back, " + profile.getUsername() + "!");
    }
}
