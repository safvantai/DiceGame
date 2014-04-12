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
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DiceStarsRoomExtensionLeisureGame extends BaseTurnRoomAdaptor {

    private IZone izone;
    private ITurnBasedRoom gameRoom;
    
    private ArrayList<IUser> pausedUserList = new ArrayList<IUser>();
    private ArrayList<DiceClass> DiceArray = new ArrayList<DiceClass>();
    private ArrayList<String> UserArray = new ArrayList<String>();
    private ArrayList<String> starightUser = new ArrayList<String>();
    private List<IUser> joinedUserlist = null;
    private int[][] User_Bid_Array = null;//new int[2][2];
    
    private int attempt = 0;    
    private byte GAME_STATUS;
    private int zhai = 1;
    private boolean isNextRound = false;
    private int bidUserIndex = 0;

    private String chatString = "";
    
   
    public DiceStarsRoomExtensionLeisureGame(IZone izone, ITurnBasedRoom room) {
        this.izone = izone;
        this.gameRoom = room;
        GAME_STATUS = DiceStarsConstants.STOPPED;
    }

    /*
     * A game when room full
     * or we can say max users are equals to joined users
     */
    @Override
    public void handleTimerTick(long time) {


        if (GAME_STATUS == DiceStarsConstants.STOPPED && gameRoom.getJoinedUsers().size() == gameRoom.getMaxUsers()) {

            GAME_STATUS = DiceStarsConstants.RUNNING;

            for (int i = 0; i < gameRoom.getMaxUsers(); i++) {
                
                UserArray.add(gameRoom.getJoinedUsers().get(i).getName());
                
            }
            User_Bid_Array = new int[UserArray.size()][2];
            
            
            //gameRoom.startGame(DiceStarsConstants.SERVER_NAME);

            System.out.println("Room is full and Now game is start...");

        } else if (GAME_STATUS == DiceStarsConstants.RESUMED) {

            GAME_STATUS = DiceStarsConstants.RUNNING;

            gameRoom.startGame(DiceStarsConstants.SERVER_NAME);

        } else if (GAME_STATUS == DiceStarsConstants.PAUSED) {
            // System.out.println("paused game");
            if (attempt == gameRoom.getMaxUsers()) {

                attempt = 0;

                //System.out.println("DiceArray size " + DiceArray.size());

                GAME_STATUS = DiceStarsConstants.RUNNING;

                gameRoom.startGame(DiceStarsConstants.SERVER_NAME);

                System.out.println("Next round start..");
            }
        }
    }

    /*
     * This function is invoked when server receive a chat request.
     */
    @Override
    public void handleChatRequest(IUser sender, String message, HandlingResult result) {

        System.out.println("Room Id " + gameRoom.getId() + ":  " + sender.getName() + " Send Message " + message);
        try {

            if ("Chat".equals(splitChatAndMsg(message.toString()))) {
              
                gameRoom.BroadcastChat(DiceStarsConstants.SERVER_NAME, chatString);
                
            } else if ("open".equals(message.toString())){
                
                for (int i = 0; i < UserArray.size(); i++) {

                    if (sender.getName().equals(UserArray.get(i))) {

                        checkForWin(User_Bid_Array, UserArray.get(i), (i - 1 == -1) ? UserArray.size() - 1 : i - 1);

                        isNextRound = true;
                    }
                }

            } else if ("ForceOpen".equals(message.toString())) {
                System.out.println("Force open");
                for (int i = 0; i < UserArray.size(); i++) {

                    if (sender.getName().equals(UserArray.get(i))) {

                        gameRoom.BroadcastChat(DiceStarsConstants.SERVER_NAME, "ForceOpen");
                        
                        checkForWin(User_Bid_Array, UserArray.get(i), bidUserIndex);

                        isNextRound = true;
                    }
                }
            
            }else if ("Zhai0".equals(message.toString())) {

                zhai = 0;

                gameRoom.BroadcastChat(DiceStarsConstants.SERVER_NAME, "Zhai");

            } else if ("Ready".equals(message.toString())) {

                System.out.println(sender.getName()+" is Ready for next round..." );

                attempt = attempt + 1;
                
                gameRoom.BroadcastChat(DiceStarsConstants.SERVER_NAME, "Play"+sender.getName());
                
                if(attempt == gameRoom.getMaxUsers()){                     
                     attempt = 0;
                     gameRoom.startGame(DiceStarsConstants.SERVER_NAME);
                }

            }  else if ("Straight".equals(message.toString())) {

                //System.out.println("The dices of  " + sender.getName()+" are Straight");

                starightUser.add(sender.getName()); 

            }else {

                JSONArray Dice = new JSONArray(message);

                ArrayList<Integer> PlayersDice = new ArrayList<Integer>();

                for (int i = 0; i < Dice.length(); i++) {

                    PlayersDice.add((Integer) Dice.get(i));

                }

                DiceArray.add(new DiceClass(sender.getName(), PlayersDice));

                //System.out.println("Dice Array lenght" + DiceArray.size());

                if (DiceArray.size() > 1 && !isNextRound) {

                    for (int i = 0; i < DiceArray.size() - 1; i++) {

                        sender.SendChatNotification(DiceStarsConstants.SERVER_NAME, "User" + ((DiceClass) DiceArray.get(i)).userName, gameRoom);
                        System.out.println(sender.getName() + " to " + ((DiceClass) DiceArray.get(i)).userName + " sent");

                    }
                }

            }

        } catch (JSONException e) {            
            result.description = "Error in fetching data";
        }
    }
    
    private String splitChatAndMsg(String str) {
        try {
            String[] spited = str.split("-");
            chatString = spited[1];
            return spited[0];
        } catch (Exception exp) {
            return "";
        }
    }
    
    /*
     * This function is invoked when server receive a move request.
     */
    @Override
    public void handleMoveRequest(IUser sender, String moveData, HandlingResult result) {
        try {

            System.out.println(sender.getName() + " send move turn and bid is" + moveData);

            JSONArray bidData = new JSONArray(moveData);


            for (int i = 0; i < UserArray.size(); i++) {

                if (sender.getName().equals(UserArray.get(i))) {

                    bidUserIndex = i;
                    for (int j = 0; j < bidData.length(); j++) {

                        User_Bid_Array[i][j] = (Integer) bidData.get(j);

                    }
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //To findout winner and looser
    private void checkForWin(int[][] User_Bid_Array, String open_User, int previousIndex) {

        int counter = 0;

        for (int i = 0; i < DiceArray.size(); i++) {

            if ((starightUser.isEmpty()?true:!isStraight(i))) {
                
                for (int j = 0; j < 5; j++) {

                    if (((DiceClass) DiceArray.get(i)).PlayersDice.get(j) == User_Bid_Array[previousIndex][1] || ((DiceClass) DiceArray.get(i)).PlayersDice.get(j) == zhai) {

                        counter = counter + 1;
                    }
                }
            }
        }

        System.out.println("Dice Count "+counter);
        
        if (counter >= User_Bid_Array[previousIndex][0]) {

            handleFinishGame(UserArray.get(previousIndex), open_User);//return UserArray.get(previousIndex);//

        } else {

            handleFinishGame(open_User, UserArray.get(previousIndex));// return open_User;//

        }


    }

    //check straight rule
    private boolean isStraight(int index){
                
        for (int i = 0; i < starightUser.size(); i++) {
            
            if (UserArray.get(index).equals(starightUser.get(i))) {
                
                System.out.println(starightUser.get(i)+" is straight");
                
                return true;
            }
            
        }
        
        return false;    
    }
    
    /*
     * This function stop the game and notify the room players about winning user and their dice.
     */
    private void handleFinishGame(String winner, String looser) {
        try {
            
            System.out.println("Finished current round...");
            
            JSONObject object = new JSONObject();

            for (int i = 0; i < UserArray.size(); i++) {
                
                System.out.println(((DiceClass) DiceArray.get(i)).userName+" dice "+((DiceClass) DiceArray.get(i)).PlayersDice);
                
                object.put(((DiceClass) DiceArray.get(i)).userName, ((DiceClass) DiceArray.get(i)).PlayersDice);
                
                
            }
            
            object.put("winner", winner);
            
            object.put("looser", looser);

//            object.put(user1_name, ((DiceClass) DiceArray.get(0)).PlayersDice);
//            object.put(user2_name, ((DiceClass) DiceArray.get(1)).PlayersDice);
//            object.put("winner", winnerUser);

            GAME_STATUS = DiceStarsConstants.PAUSED;

            gameRoom.BroadcastChat(DiceStarsConstants.SERVER_NAME, "Dice" + object);
            zhai = 1;
            //gameRoom.setAdaptor(null);

            //izone.deleteRoom(gameRoom.getId());
            DiceArray.clear();
            starightUser.clear();
            
            gameRoom.stopGame(DiceStarsConstants.SERVER_NAME);
            
        } catch (Exception e) {
        }
    }

    
    private String checkForWin(int[] BidDice, String openUserName, String bidUserName) {
        try {
            System.out.println("BidDice in chk " + BidDice.length);

            int counter = 0;

            System.out.println("DiceArray in chk " + DiceArray.size());

            for (int i = 0; i < DiceArray.size(); i++) {

                for (int j = 0; j < 5; j++) {

                    if (((DiceClass) DiceArray.get(i)).PlayersDice.get(j) == BidDice[1] || ((DiceClass) DiceArray.get(i)).PlayersDice.get(j) == zhai) {

                        counter = counter + 1;
                    }

                }

            }
            System.out.println("Count " + counter);
            if (counter >= BidDice[0]) {

                return bidUserName;

            } else {

                return openUserName;

            }
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
        }
        return "";
    }

    
    /*
     * This function is invoked when server leave user request. In case of two user 
     * server declare other user as winner.
     */
    @Override
    public void onUserLeavingTurnRoom(IUser user, HandlingResult result) {
        System.out.println(user.getName() + " removed, so game is stop");
        DiceArray.clear();
        //starightUser = null;
//        for (int i = 0; i < UserArray.size(); i++) {
//            if (user.getName().equals(UserArray.get(i))) {
//                DiceArray.remove(((DiceClass) DiceArray.get(i)));
//                //User_Bid_Array[i] = null;
//                UserArray.remove(i);
//            }
//        }

//        if (GAME_STATUS != DiceStarsConstants.RUNNING) {
//            return;
//        }

//        String leaveingUser = null;
//        if (user.getName().equals(user1_name)) {
//            leaveingUser = user1_name;
//        } else if (user.getName().equals(user2_name)) {
//            leaveingUser = user2_name;
//        }
//        String message = "You Win! Enemy " + leaveingUser + " left the room";
//        gameRoom.BroadcastChat(DiceStarsConstants.SERVER_NAME, DiceStarsConstants.RESULT_USER_LEFT + "#" + message);
        gameRoom.setAdaptor(null);
        izone.deleteRoom(gameRoom.getId());
        gameRoom.stopGame(DiceStarsConstants.SERVER_NAME);
    }

    public void onUserPaused(IUser user) {
        if (gameRoom.getJoinedUsers().contains(user)) {

            pausedUserList.add(user);

            GAME_STATUS = DiceStarsConstants.PAUSED;

            gameRoom.stopGame(DiceStarsConstants.SERVER_NAME);

        }
    }

    public void onUserResume(IUser user) {
        if (pausedUserList.indexOf(user) != -1) {

            pausedUserList.remove(user);

        }
        if (pausedUserList.isEmpty()) {

            GAME_STATUS = DiceStarsConstants.RESUMED;

        }
    }

    /**
     * Invoked when a start game request is received from a client when the room
     * is in stopped state.
     *
     * By default a success response will be sent back to the client, the game
     * state will be updated and game started notification is sent to all the
     * subscribers of the room.
     *
     * @param sender the user who has sent the request.
     * @param result use this to override the default behavior
     */
    @Override
    public void handleStartGameRequest(IUser sender, HandlingResult result) {
        System.out.println("Game started..");
    }

    /*
     * This method deal new hand for each user and send
     * chat message having his cards array
     */
    private void dealNewCards() {

//        for (int i = 1; i <= DiceStarsConstants.MAX_CARD; i++) {
//            CARDS_DECK.add(i);
//        }
//        Collections.shuffle(CARDS_DECK);
//        for (int i = 0; i < MAX_NO_OF_CARDS; i++) {
//            //USER_1_Bid.add(CARDS_DECK.remove(0));
//            //USER_2_Bid.add(CARDS_DECK.remove(0));
//        }

        if (joinedUserlist.size() == 2) {
            IUser iuser1 = joinedUserlist.get(0);
            IUser iuser2 = joinedUserlist.get(1);
//            user1_name = iuser1.getName();
//            user2_name = iuser2.getName();
//            System.out.println(user1_name + "\n" + user2_name);
            try {
//                JSONObject dataUser1 = new JSONObject();
//                dataUser1.put(iuser1.getName(), USER_1_Bid);
//
//                JSONObject dataUser2 = new JSONObject();
//                dataUser2.put(iuser2.getName(), USER_2_Bid);
//                BufferedReader br= new BufferedReader (new InputStreamReader(System.in));
//               String msg="";
//                do
//                {
//                System.out.println("Enter text here");
//                    msg=br.readLine();
//                gameRoom.BroadcastChat(DiceStarsConstants.SERVER_NAME, msg);
//                }while(msg.equals("exit"));
//                iuser1.SendChatNotification(DiceStarsConstants.SERVER_NAME, user2_name, gameRoom);//DiceStarsConstants.PLAYER_HAND+"#"+dataUser1, gameRoom);
//                iuser2.SendChatNotification(DiceStarsConstants.SERVER_NAME, user1_name, gameRoom);//DiceStarsConstants.PLAYER_HAND+"#"+dataUser2, gameRoom);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        printAll("dealNewCards", true);
    }

    /*
     * This function validate move send by client.
     */
    private void validateAndHandleMove(IUser sender, int topCard, HandlingResult result) {
//        if (sender.getName().equals(user1_name)) {
//            if (USER_1_Bid.indexOf(topCard) != -1) {
//                USER_1_Bid.remove(new Integer(topCard));
//                if (USER_1_Bid.size() < MAX_NO_OF_CARDS) {
//                    USER_1_Bid.add(TOP_CARD_DISCARD_PILE);
//                }
//                TOP_CARD_DISCARD_PILE = topCard;
//            } else {
//                result.code = DiceStarsConstants.INVALID_MOVE;
//                result.description = "Invalid Move";
//            }
//        } else if (sender.getName().equals(user2_name)) {
//            if (USER_2_Bid.indexOf(topCard) != -1) {
//                USER_2_Bid.remove(new Integer(topCard));
//                if (USER_2_Bid.size() < MAX_NO_OF_CARDS) {
//                    USER_2_Bid.add(TOP_CARD_DISCARD_PILE);
//                }
//                TOP_CARD_DISCARD_PILE = topCard;
//            } else {
//                result.code = DiceStarsConstants.INVALID_MOVE;
//                result.description = "Invalid Move";
//            }
//        }
    }

    /*
     * This method return last element of TOTAL_CARDS
     * In case of empty list again shuffle cards
     */
    private Integer getNewCard() {
        return null;
//        if (CARDS_DECK.isEmpty()) {
//            System.out.println("getNewCard Empty list again shuffle cards");
//            for (int i = 1; i <= DiceStarsConstants.MAX_CARD; i++) {
//                CARDS_DECK.add(i);
//            }
//            Collections.shuffle(CARDS_DECK);
//            for (Integer i : USER_1_Bid) {
//                CARDS_DECK.remove(new Integer(i));
//            }
//            for (Integer j : USER_2_Bid) {
//                CARDS_DECK.remove(new Integer(j));
//            }
//            if (TOP_CARD_DISCARD_PILE != -1) {
//                CARDS_DECK.remove(new Integer(TOP_CARD_DISCARD_PILE));
//            }
//        }
//        return CARDS_DECK.remove(CARDS_DECK.size() - 1);
    }

    /*
     * This is a RPC Method when user request for new Card from the deck
     */
    public int requestNewCard(String username) {
        int newCard = getNewCard();
//        if (username.equals(user1_name)) {
//            USER_1_Bid.add(newCard);
//        } else if (username.equals(user2_name)) {
//            USER_2_Bid.add(newCard);
//        }
        return newCard;
    }

    // for debugging
    private void printAll(String TAG, boolean status) {
        if (status) {
            System.out.println("===================" + TAG + "======================");
//            System.out.println("USER_1:   " + USER_1_Bid);
//            System.out.println("USER_2:   " + USER_2_Bid);
            //System.out.println("TOTAL_CA: " + CARDS_DECK);
//            System.out.println("TOP_CARD: " + TOP_CARD_DISCARD_PILE);
        }
    }
}
