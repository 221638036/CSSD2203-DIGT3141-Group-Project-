package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Profile implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int MAX_PARTIES = 5;

    private String username, password;
    private List<Party> savedParties = new ArrayList<>();
    private List<Integer> scores = new ArrayList<>();
    private CampaignState activeCampaign;
    private int wins = 0;
    private int losses = 0;

    public Profile(String username, String password) {
        this.username = username; this.password = password;
    }

    public boolean authenticate(String pw)  { return password.equals(pw); }

    public boolean saveParty(Party party) {
        if (savedParties.size() >= MAX_PARTIES) return false;
        savedParties.add(party); return true;
    }

    public void replaceParty(int index, Party party) { if (index >= 0 && index < savedParties.size()) savedParties.set(index, party); }
    public void removeParty(int index)               { if (index >= 0 && index < savedParties.size()) savedParties.remove(index); }
    public void addScore(int score)                  { scores.add(score); }
    public int getBestScore()                        { return scores.stream().mapToInt(Integer::intValue).max().orElse(0); }
    public boolean hasMaxParties()                   { return savedParties.size() >= MAX_PARTIES; }

    public String getUsername()                         { return username; }
    public List<Party> getSavedParties()                { return savedParties; }
    public List<Integer> getScores()                    { return scores; }
    public CampaignState getActiveCampaign()            { return activeCampaign; }
    public void setActiveCampaign(CampaignState c)      { this.activeCampaign = c; }
    public void incrementWins()                         { this.wins++; }
    public void incrementLosses()                       { this.losses++; }
    public int getWins()                                { return wins; }
    public int getLosses()                              { return losses; }
    public int getTotalBattles()                        { return wins + losses; }
}
