/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dicestars;

/**
 *
 * @author Imtiyaz
 */

import com.shephertz.app42.server.idomain.BaseTurnRoomAdaptor;
import com.shephertz.app42.server.idomain.HandlingResult;
import com.shephertz.app42.server.idomain.ITurnBasedRoom;
import com.shephertz.app42.server.idomain.IUser;
import com.shephertz.app42.server.idomain.IZone;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DiceStarsRoomExtensionTournament extends BaseTurnRoomAdaptor {

    private ITurnBasedRoom gameRoom;
    private IZone izone;
    ArrayList<IUser> pausedUserList = new ArrayList<IUser>();
    
    // GameData
    private ArrayList<Integer> CARDS_DECK = new ArrayList<Integer>();
    
    private ArrayList<Integer> USER_1_HAND = new ArrayList<Integer>();
    private ArrayList<Integer> USER_2_HAND = new ArrayList<Integer>();
    private ArrayList<Integer> USER_3_HAND = new ArrayList<Integer>();
    
    private int TOP_CARD_DISCARD_PILE = -1;
    
    private final int MAX_NO_OF_CARDS = 6;// for each user
    
    private String user1_name = null;
    private String user2_name = null;
    private String user3_name = null;
    
    public byte GAME_STATUS;
    
    public DiceStarsRoomExtensionTournament(IZone izone, ITurnBasedRoom room){
        this.gameRoom = room;
        this.izone = izone;
        GAME_STATUS = DiceStarsConstants.STOPPED;
    }
    
    /*
     * This is a RPC Method when user request for new Card from the deck
     */
    public int requestNewCard(String username){
        int newCard = getNewCard();
        if(username.equals(user1_name)){
            USER_1_HAND.add(newCard);
        }else if(username.equals(user2_name)){
            USER_2_HAND.add(newCard);
        }else if(username.equals(user3_name)){
            USER_3_HAND.add(newCard);
        }
        return newCard;
    }
    
    /*
     * This function is invoked when server receive a move request.
     */
    @Override
    public void handleMoveRequest(IUser sender, String moveData, HandlingResult result){
        try{
            int top_card =-1;
            JSONObject data = new JSONObject(moveData);
            top_card = data.getInt("top");
            validateAndHandleMove(sender, top_card, result);
            // replace card array on server
            JSONArray cards = data.getJSONArray("cards");
            if(sender.getName().equals(user1_name)){
                for(int i=0;i<cards.length();i++){
                    USER_1_HAND.set(i, cards.getInt(i));
                }
            }else if(sender.getName().equals(user2_name)){
                for(int i=0;i<cards.length();i++){
                    USER_2_HAND.set(i, cards.getInt(i));
                }
            }else if(sender.getName().equals(user3_name)){
                for(int i=0;i<cards.length();i++){
                    USER_3_HAND.set(i, cards.getInt(i));
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        printAll("handleMoveRequest", true);
    }
    
    
    /*
     * This function is invoked when server receive a chat request.
     */
    @Override
    public void handleChatRequest(IUser sender, String message, HandlingResult result){
        result.code = DiceStarsConstants.SUBMIT_CARD;
        try{
            JSONArray cards = new JSONArray(message);
            ArrayList cardList = new ArrayList();
            for(int i=0;i<cards.length();i++){
                cardList.add(cards.get(i));
            }
           boolean status = Utils.checkForWin(cardList);
            if(status){// for winning condition
                if(sender.getName().equals(user1_name)){
                    handleFinishGame(user1_name, cardList);
                }else if(sender.getName().equals(user2_name)){
                    handleFinishGame(user2_name, cardList);
                }else if(sender.getName().equals(user3_name)){
                    handleFinishGame(user3_name, cardList);
                }
            }else{
                String desc = DiceStarsConstants.SUBMIT_CARD+"#"+"You don't have winning cards";
                sender.SendChatNotification(DiceStarsConstants.SERVER_NAME, desc, gameRoom);
            }
        }catch(JSONException e){
            e.printStackTrace();
            result.description = "Error in fetching data";
        }
    }
    
    /*
     * This function is invoked when server leave user request. In case of three user 
     * server continue game with remaining two users and add the cards of third user in total cards.
     */
    @Override
    public void onUserLeavingTurnRoom(IUser user, HandlingResult result){
        if(GAME_STATUS!=DiceStarsConstants.RUNNING){
            return;
        }
        if(gameRoom.getJoinedUsers().size()==2){// if three users are playing and one of them left room
            if(user.getName().equals(user1_name)){
                CARDS_DECK.addAll(0, USER_1_HAND);
            }else if(user.getName().equals(user2_name)){
                CARDS_DECK.addAll(0, USER_2_HAND);
            }else if(user.getName().equals(user3_name)){
                CARDS_DECK.addAll(0, USER_3_HAND);
            }
        }else if(gameRoom.getJoinedUsers().size()==1){// if two users are playing and one of them left room
            String leaveingUser = null;
            if(user.getName().equals(user1_name)){
                leaveingUser = user1_name;
            }else if(user.getName().equals(user2_name)){
                leaveingUser = user2_name;
            }else if(user.getName().equals(user3_name)){
                leaveingUser = user3_name;
            }
            String message = "You Win! Enemy "+leaveingUser+" left the room";
            gameRoom.BroadcastChat(DiceStarsConstants.SERVER_NAME, DiceStarsConstants.RESULT_USER_LEFT+"#"+message);
            gameRoom.setAdaptor(null);
            izone.deleteRoom(gameRoom.getId());
            gameRoom.stopGame(DiceStarsConstants.SERVER_NAME);
        }
    }
    
    public void onUserPaused(IUser user){
        if(gameRoom.getJoinedUsers().contains(user)){
            pausedUserList.add(user);
            GAME_STATUS = DiceStarsConstants.PAUSED;
            gameRoom.stopGame(DiceStarsConstants.SERVER_NAME);
        }
    }
    
    public void onUserResume(IUser user){
        if(pausedUserList.indexOf(user)!=-1){
            pausedUserList.remove(user);
        }
        if(pausedUserList.isEmpty()){
            GAME_STATUS = DiceStarsConstants.RESUMED;
        }
    }
    
    /*
     * This method deal new hand for each user and send
     * chat message having his cards array
     */
    private void dealNewCards(){
        for(int i=1;i<=DiceStarsConstants.MAX_CARD;i++){
            CARDS_DECK.add(i);
        }
        Collections.shuffle(CARDS_DECK);
        for(int i=0;i<MAX_NO_OF_CARDS;i++){
            USER_1_HAND.add(CARDS_DECK.remove(0));
            USER_2_HAND.add(CARDS_DECK.remove(0));
            USER_3_HAND.add(CARDS_DECK.remove(0));
        }
        List<IUser>list = gameRoom.getJoinedUsers();
        if(list.size()==3){
            IUser iuser1 = list.get(0);
            IUser iuser2 = list.get(1);
            IUser iuser3 = list.get(2);
            user1_name = iuser1.getName();
            user2_name = iuser2.getName();
            user3_name = iuser3.getName();
            try{
                JSONObject dataUser1 = new JSONObject();
                dataUser1.put(iuser1.getName(), USER_1_HAND);
                JSONObject dataUser2 = new JSONObject();
                dataUser2.put(iuser2.getName(), USER_2_HAND);
                JSONObject dataUser3 = new JSONObject();
                dataUser3.put(iuser3.getName(), USER_3_HAND);
                iuser1.SendChatNotification(DiceStarsConstants.SERVER_NAME, DiceStarsConstants.PLAYER_HAND+"#"+dataUser1, gameRoom);
                iuser2.SendChatNotification(DiceStarsConstants.SERVER_NAME, DiceStarsConstants.PLAYER_HAND+"#"+dataUser2, gameRoom);
                iuser3.SendChatNotification(DiceStarsConstants.SERVER_NAME, DiceStarsConstants.PLAYER_HAND+"#"+dataUser3, gameRoom);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        printAll("dealNewCards", true);
    }
    
    @Override
    public void handleTimerTick(long time){
        /*
         * A game when room full
         * or we can say max users are equals to joined users
         */
        
        if(GAME_STATUS==DiceStarsConstants.STOPPED && gameRoom.getJoinedUsers().size()==gameRoom.getMaxUsers()){
            GAME_STATUS=DiceStarsConstants.RUNNING;
            dealNewCards();
            gameRoom.startGame(DiceStarsConstants.SERVER_NAME);
        }else if(GAME_STATUS==DiceStarsConstants.RESUMED){
            GAME_STATUS=DiceStarsConstants.RUNNING;
            gameRoom.startGame(DiceStarsConstants.SERVER_NAME);
        }
       
        
    }
    /*
     * This method return last element of TOTAL_CARDS
     * In case of empty list again shuffle cards
     */
    private Integer getNewCard(){
        if(CARDS_DECK.isEmpty()){
            for(int i=1;i<=DiceStarsConstants.MAX_CARD;i++){
                CARDS_DECK.add(i);
            }
            Collections.shuffle(CARDS_DECK);
            for(Integer i:USER_1_HAND){
                CARDS_DECK.remove(new Integer(i));
            }
            for(Integer j:USER_2_HAND){
                CARDS_DECK.remove(new Integer(j));
            }
            for(Integer k:USER_3_HAND){
                CARDS_DECK.remove(new Integer(k));
            }
            if(TOP_CARD_DISCARD_PILE!=-1){
                CARDS_DECK.remove(new Integer(TOP_CARD_DISCARD_PILE));
            }
        }
        return CARDS_DECK.remove(CARDS_DECK.size()-1);
     }
            
    /*
     * This function validate move send by client.
     */
    private void validateAndHandleMove(IUser sender, int topCard, HandlingResult result){
        if(sender.getName().equals(user1_name)){
            if(USER_1_HAND.indexOf(topCard)!=-1){
                USER_1_HAND.remove(new Integer(topCard));
                if(USER_1_HAND.size()<MAX_NO_OF_CARDS){
                    USER_1_HAND.add(TOP_CARD_DISCARD_PILE);
                }
                TOP_CARD_DISCARD_PILE = topCard;
            }else{
                result.code = DiceStarsConstants.INVALID_MOVE;
                result.description = "Invalid Move";
            }
        }else if(sender.getName().equals(user2_name)){
            if(USER_2_HAND.indexOf(topCard)!=-1){
                USER_2_HAND.remove(new Integer(topCard));
                if(USER_2_HAND.size()<MAX_NO_OF_CARDS){
                    USER_2_HAND.add(TOP_CARD_DISCARD_PILE);
                }
                TOP_CARD_DISCARD_PILE = topCard;
            }else{
                result.code = DiceStarsConstants.INVALID_MOVE;
                result.description = "Invalid Move";
            }
        }else if(sender.getName().equals(user3_name)){
            if(USER_3_HAND.indexOf(topCard)!=-1){
                USER_3_HAND.remove(new Integer(topCard));
                if(USER_3_HAND.size()<MAX_NO_OF_CARDS){
                    USER_3_HAND.add(TOP_CARD_DISCARD_PILE);
                }
                TOP_CARD_DISCARD_PILE = topCard;
            }else{
                result.code = DiceStarsConstants.INVALID_MOVE;
                result.description = "Invalid Move";
            }
        }
    }
    
    /*
     * This function stop the game and notify the room players about winning user and his cards.
     */
    private void handleFinishGame(String winningUser, ArrayList<Integer> cards){
        try{
            JSONObject object = new JSONObject();
            object.put("win", winningUser);
            object.put("cards", cards);
            GAME_STATUS = DiceStarsConstants.FINISHED;
            gameRoom.BroadcastChat(DiceStarsConstants.SERVER_NAME, DiceStarsConstants.RESULT_GAME_OVER+"#"+object);
            gameRoom.setAdaptor(null);
            izone.deleteRoom(gameRoom.getId());
            gameRoom.stopGame(DiceStarsConstants.SERVER_NAME);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    // for debugging 
    
    private void printAll(String TAG, boolean status){
        if(status){
            System.out.println("==================="+TAG+"======================");
            System.out.println("USER_1:   "+USER_1_HAND);
            System.out.println("USER_2:   "+USER_2_HAND);
            System.out.println("USER_3:   "+USER_3_HAND);
            System.out.println("TOTAL_CA: "+CARDS_DECK);
            System.out.println("TOP_CARD: "+TOP_CARD_DISCARD_PILE);
        }
    }
}
