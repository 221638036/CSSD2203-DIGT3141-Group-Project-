package gui;

import state.GameState;
import state.GameStateManager;
import javax.swing.*;
import java.awt.*;

/**
 * Main application window. Uses CardLayout to switch between all panels.
 * Listens to GameStateManager and swaps panels automatically on state change.
 */
public class MainWindow extends JFrame implements GameStateManager.StateListener {

    private CardLayout cardLayout;
    private JPanel cardPanel;

    private LoginPanel       loginPanel;
    private MainMenuPanel    mainMenuPanel;
    private CampaignMapPanel campaignMapPanel;
    private BattlePanel      battlePanel;
    private InnPanel         innPanel;
    private ProfilePanel     profilePanel;
    private PvpPanel         pvpPanel;
    private GameOverPanel    gameOverPanel;
    private LeaderboardPanel leaderboardPanel;

    public MainWindow() {
        super("Legends of Sword and Wand");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 840);
        setLocationRelativeTo(null);
        setResizable(true);

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);

        loginPanel       = new LoginPanel();
        mainMenuPanel    = new MainMenuPanel();
        campaignMapPanel = new CampaignMapPanel();
        battlePanel      = new BattlePanel();
        innPanel         = new InnPanel();
        profilePanel     = new ProfilePanel();
        pvpPanel         = new PvpPanel();
        gameOverPanel    = new GameOverPanel();
        leaderboardPanel = new LeaderboardPanel();

        cardPanel.add(loginPanel,       GameState.LOGIN.name());
        cardPanel.add(mainMenuPanel,    GameState.MAIN_MENU.name());
        cardPanel.add(campaignMapPanel, GameState.CAMPAIGN_MAP.name());
        cardPanel.add(battlePanel,      GameState.BATTLE.name());
        cardPanel.add(innPanel,         GameState.INN.name());
        cardPanel.add(profilePanel,     GameState.PROFILE_VIEW.name());
        cardPanel.add(pvpPanel,         GameState.PVP_INVITE.name());
        cardPanel.add(gameOverPanel,    GameState.GAME_OVER.name());
        cardPanel.add(leaderboardPanel, GameState.LEADERBOARD.name());

        add(cardPanel);
        GameStateManager.getInstance().addListener(this);
        showPanel(GameState.LOGIN);
    }

    @Override
    public void onStateChanged(GameState newState) {
        SwingUtilities.invokeLater(() -> {
            switch (newState) {
                case MAIN_MENU:    mainMenuPanel.refresh();    break;
                case CAMPAIGN_MAP: campaignMapPanel.refresh(); break;
                case BATTLE:       battlePanel.refresh();      break;
                case INN:          innPanel.refresh();         break;
                case PROFILE_VIEW: profilePanel.refresh();     break;
                case PVP_INVITE:   pvpPanel.refresh();         break;
                case LEADERBOARD:  leaderboardPanel.refresh(); break;
                default: break;
            }
            showPanel(newState);
        });
    }

    private void showPanel(GameState state) {
        cardLayout.show(cardPanel, state.name());
    }
}
