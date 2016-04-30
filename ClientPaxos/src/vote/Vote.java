/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vote;

/**
 *
 * @author asus
 */
public class Vote {
    public int player_id;
    public int countVote;
    
    public Vote(int id, int count) {
        player_id = id;
        countVote = count;
    }
    
    public int getPlayerId() {
        return player_id;
    }
    
    public int getCountVote() {
        return countVote;
    }
    
    public void setPlayerID(int id) {
        player_id  = id;
    }
    
    public void setCountVote(int count) {
        countVote  = count;
    }
    
}
