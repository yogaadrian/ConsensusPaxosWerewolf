/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package player;

/**
 *
 * @author yoga
 */
public class Player {
    
    public int player_id;
    public int is_alive;
    public String address;
    public int port;
    public boolean is_ready;
    public String username;
    public String role;
    
    public Player(int id,int alive, String addr, int prt, String name){
        player_id = id;
        is_alive= alive;
        address= addr;
        port=prt;
        username=name;
        is_ready=false;
    }
    
    public int getPlayerId(){
        return player_id;
    }
    public int getIsAlive(){
        return is_alive;
    }
    public boolean getIsReady(){
        return is_ready;
    }
    public String getAddress(){
        return address;
    }
    public int getPort(){
        return port;
    }
    public String getUsername(){
        return username;
    }
    public String getRole(){
        return role;
    }
    
    public void setPlayerID(int id){
        player_id= id;
    }
    public void setIsAlive(int alive){
        is_alive= alive;
    }
    public void setIsReady(boolean ready){
        is_ready= ready;
    }
    public void setAddress(String address){
        address= address;
    }
    public void setPort(int prt){
        port= prt;
    }
    public void setUsername(String name){
        username=name;
    }
    public void setRole(String rle){
        role=rle;
    }
    
}
