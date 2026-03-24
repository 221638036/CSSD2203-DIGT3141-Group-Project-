package gui;

import state.GameStateManager;
import javax.swing.*;
import java.awt.*;

public class GameOverPanel extends JPanel {

    public GameOverPanel() {
        setLayout(new GridBagLayout());
        setBackground(new Color(8, 4, 4));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(14, 30, 14, 30);
        g.gridx  = 0;

        JLabel skull = new JLabel("☠", SwingConstants.CENTER);
        skull.setFont(new Font("Serif", Font.PLAIN, 64));
        skull.setForeground(new Color(160, 30, 30));
        g.gridy = 0; add(skull, g);

        JLabel title = new JLabel("GAME OVER", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 44));
        title.setForeground(new Color(200, 40, 40));
        g.gridy = 1; add(title, g);

        JLabel sub = new JLabel("Your party has fallen into darkness...", SwingConstants.CENTER);
        sub.setFont(new Font("Serif", Font.ITALIC, 16));
        sub.setForeground(new Color(120, 80, 80));
        g.gridy = 2; add(sub, g);

        JButton menuBtn = UI.dangerButton("Return to Main Menu");
        menuBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        menuBtn.addActionListener(e -> GameStateManager.getInstance().showMainMenu());
        g.gridy = 3; add(menuBtn, g);
    }
}
