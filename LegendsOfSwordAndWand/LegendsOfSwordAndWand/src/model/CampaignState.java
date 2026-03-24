package model;

import java.io.Serializable;

public class CampaignState implements Serializable {
    private static final long serialVersionUID = 1L;

    private Party party;
    private int currentRoom;
    private boolean inBattle;
    private boolean atInn;

    public CampaignState(Party party) {
        this.party = party; this.currentRoom = 0;
    }

    public Party getParty()               { return party; }
    public int getCurrentRoom()           { return currentRoom; }
    public void setCurrentRoom(int r)     { this.currentRoom = r; }
    public boolean isInBattle()           { return inBattle; }
    public void setInBattle(boolean b)    { this.inBattle = b; }
    public boolean isAtInn()              { return atInn; }
    public void setAtInn(boolean a)       { this.atInn = a; }
    public boolean isComplete()           { return currentRoom >= 30; }
}
