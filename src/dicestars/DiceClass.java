/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dicestars;

import java.util.ArrayList;

/**
 *
 * @author Imtiyaz
 */


//This class is for save user name with their Dice in one array as a object.
public class DiceClass {
    
    public String userName = "";
    
    public ArrayList<Integer> PlayersDice = null;
    
    public DiceClass(String userName, ArrayList<Integer> PlayersDice){
        
        this.userName = userName;
        
        this.PlayersDice = PlayersDice;
        
    } 
    
}
