package agent;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Main class for an Agent. Runs the gui. Establishes a connection with the bank on start up. The hostname and port for
 * the bank are input as commandline arguments.
 */
public class Agent extends Application{

    private static VBox auctionsBox;
    private static VBox itemsBox;
    private static Label currAuctionLbl;
    private static Label currItemLbl;
    private static Label bidStatusLbl;
    private static Label balLbl;
    private static Label heldBalLbl;
    private static Label availableBalLbl;
    private static VBox yourItemsBox;
    private static String currAuction;
    private static Item currItem;
    private static Slider bidSldr;
    private static Label signInLbl;
    private static Label signUpLbl;
    private static TextField bidFld;

    public static void main(String[] args) throws Exception{
        String hostName = "localhost";
        int portNumber = 1235;
        try {
            hostName = args[0];
            portNumber = Integer.parseInt(args[1]);
        }catch (ArrayIndexOutOfBoundsException aiob){}

        Server server = new Server(hostName,portNumber);
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //Auctions: left pane
        Label auctionLbl = new Label("   Auctions:");
        auctionLbl.setMinWidth(100);
        auctionsBox = new VBox(auctionLbl);
        auctionsBox.setAlignment(Pos.TOP_LEFT);
        auctionsBox.setSpacing(10);
        VBox auctionsNamesBox = new VBox(auctionLbl,auctionsBox);
        auctionsNamesBox.setSpacing(20);

        //Auction items, bid stuff middle pane
        VBox auctionItemBox = new VBox();
        itemsBox = new VBox();
        itemsBox.setAlignment(Pos.CENTER);
        itemsBox.setMinHeight(75);
        currAuctionLbl = new Label("Select an Auction");
        currItemLbl = new Label("Select an Item");
        bidStatusLbl = new Label();
        Label bidLbl = new Label("Place a bid below");
        bidLbl.setTextFill(Color.GRAY);
        bidLbl.setMinWidth(200);
        bidLbl.setAlignment(Pos.CENTER);
        bidSldr = new Slider(0,100,50);
        bidFld = new TextField();
        bidFld.setText(String.valueOf((int)bidSldr.getValue()));
        bidFld.setMaxWidth(60);
        Button submitBtn = new Button("Submit Bid");
        submitBtn.setOnMouseClicked(e -> {
            if(currAuction != null && currItem != null) {
                currItem.setBidStatus("Bid Submitted");
                bidStatusLbl.setText("Bid submitted");
                Server.submitBid(currAuction, currItem.getName(), Integer.parseInt(bidFld.getText()));
            }
        });
        auctionItemBox.getChildren().addAll(currAuctionLbl, itemsBox, currItemLbl,bidStatusLbl,
                bidLbl,bidSldr,bidFld,submitBtn);
        auctionItemBox.setAlignment(Pos.CENTER);
        auctionItemBox.setSpacing(10);
        bidSldr.setMaxWidth(150);
        bidSldr.setMajorTickUnit(10);
        bidSldr.setMinorTickCount(1);
        bidSldr.setSnapToTicks(true);
        bidSldr.setShowTickMarks(true);
        bidSldr.setShowTickLabels(true);
        bidSldr.setOnMouseReleased(e -> bidFld.setText(String.valueOf((int)bidSldr.getValue())));
        bidFld.setOnKeyTyped(e -> {
            try {
                int bid = Integer.parseInt(bidFld.getText());
                if(bid < 0) throw new NumberFormatException();
                bidSldr.setValue(bid);
            } catch (NumberFormatException nfe){
                bidFld.setText("");
            }
        });
        Label deposit = new Label("Deposit: ");
        TextField depositAmt = new TextField();
        Button depositBtn = new Button("Deposit");
        Label withdraw = new Label("Withdraw: ");
        TextField withdrawAmt = new TextField();
        Button withdrawBtn = new Button("Withdraw");
        HBox depositHBox = new HBox(deposit, depositAmt, depositBtn);
        depositHBox.setAlignment(Pos.CENTER);
        depositHBox.setSpacing(10);
        HBox withdrawHBox = new HBox(withdraw, withdrawAmt, withdrawBtn);
        withdrawHBox.setAlignment(Pos.CENTER);
        withdrawHBox.setSpacing(10);
        VBox balance = new VBox(depositHBox, withdrawHBox);
        balance.setAlignment(Pos.CENTER);
        balance.setSpacing(20);


        depositBtn.setOnAction(e -> {
            double amount = 0;
            boolean work = true;
            try {
                amount = Double.parseDouble(depositAmt.getText());
            } catch (Exception exception){
                work = false;
            }
            if (work){
                Server.getBankConnection().sendMessage("deposit " + Server.getClientId() + " " + amount);

            } else {

            }
            depositAmt.setText("");

        });

        withdrawBtn.setOnAction(e -> {
            double amount = 0;
            boolean work = true;
            try {
                amount = Double.parseDouble(withdrawAmt.getText());
            } catch (Exception exception){
                work = false;
            }
            if (work){
                Server.getBankConnection().sendMessage("withdraw " + Server.getClientId() + " " + amount);
            } else {

            }
            withdrawAmt.setText("");

        });

        //Agent information: right pane
        VBox agentBox = new VBox();
        Label agentNumLbl = new Label("Agent Number: ");
        agentNumLbl.setMinWidth(150);
        balLbl = new Label("Balance: ");
        heldBalLbl = new Label("Withheld Funds: ");
        availableBalLbl = new Label("Available Funds: ");
        Label yourItemsLbl = new Label("Your Items:");
        yourItemsBox = new VBox();
        agentBox.getChildren().addAll(agentNumLbl,balLbl,heldBalLbl,availableBalLbl,yourItemsLbl,yourItemsBox);
        agentBox.setAlignment(Pos.TOP_LEFT);
        agentBox.setSpacing(10);

        //Registration Pane
        signInLbl = new Label("Please Sign In");
        TextField signInUser = new TextField();
        Label signInUserLab = new Label("Username: ");
        signInUserLab.setAlignment(Pos.CENTER);
        PasswordField signInPassword = new PasswordField();
        Label signInPasswordLab = new Label("Password: ");
        signInPasswordLab.setAlignment(Pos.CENTER);
        Button signIn = new Button("Submit");
        HBox signInUpper = new HBox(signInUserLab,signInUser);
        HBox signInLower = new HBox(signInPasswordLab, signInPassword);
        VBox signInVbox =new VBox(signInLbl,signInUpper,signInLower,signIn);
        signInUpper.setAlignment(Pos.CENTER);
        signInLower.setAlignment(Pos.CENTER);
        signInVbox.setAlignment(Pos.CENTER);
        signInVbox.setSpacing(10);


        signUpLbl = new Label("Register New User");
        TextField signUpUser = new TextField();
        Label signUpUserLab = new Label("Username: ");
        signUpUserLab.setAlignment(Pos.CENTER);
        TextField signUpPassword = new TextField();
        Label signUpPasswordLab = new Label("Password: ");
        signUpPasswordLab.setAlignment(Pos.CENTER);
        Button signUp = new Button("Submit");
        HBox signUpUpper = new HBox(signUpUserLab,signUpUser);
        HBox signUpLower = new HBox(signUpPasswordLab, signUpPassword);
        VBox signUpVbox =new VBox(signUpLbl,signUpUpper,signUpLower,signUp);
        signUpUpper.setAlignment(Pos.CENTER);
        signUpLower.setAlignment(Pos.CENTER);
        signUpVbox.setAlignment(Pos.CENTER);
        signUpVbox.setSpacing(10);

        VBox openVBox = new VBox(signInVbox, signUpVbox);
        openVBox.setAlignment(Pos.CENTER);
        openVBox.setSpacing(20);

        StackPane loginPane = new StackPane(openVBox);
        loginPane.setAlignment(Pos.CENTER);
        HBox mainHbox = new HBox(auctionsNamesBox,auctionItemBox,agentBox);
        VBox mainVBox = new VBox(mainHbox, balance);
        mainVBox.setOpacity(0);
        StackPane root = new StackPane(mainVBox,loginPane);
        mainHbox.setSpacing(20);
        mainVBox.setSpacing(20);

        /**
         * Sends the bank an existing users information
         */
        signIn.setOnAction(e -> {
            Server.getBankConnection().sendMessage("ReturningUser " + signInUser.getText() +
                    " " + signInPassword.getText());
            agentNumLbl.setText("Agent Number: " + signInPassword.getText());
            Server.setClientId(Integer.parseInt(signInPassword.getText()));
            signInUser.setText("");
            signInPassword.setText("");
            try {
                Thread.sleep(100);
            }catch (InterruptedException ei){}

            if(BankConnection.isRegistered()){
                root.getChildren().remove(loginPane);
                mainVBox.setOpacity(1);
                Server.getBankConnection().sendMessage("Auctions ");
                Server.getBankConnection().sendMessage("CheckBalance");
            }
        });

        /**
         * Sends the Bank a new user's registration information
         */
        signUp.setOnAction(e -> {
            Server.getBankConnection().sendMessage("registerAgent " + signUpUser.getText() +
                    " " + signUpPassword.getText());
            agentNumLbl.setText("Agent Number: " + signUpPassword.getText());
            Server.setClientId(Integer.parseInt(signUpPassword.getText()));
            signUpUser.setText("");
            signUpPassword.setText("");
            try {
                Thread.sleep(100);
            }catch (InterruptedException ei){}

            if(BankConnection.isRegistered()){
                root.getChildren().remove(loginPane);
                mainVBox.setOpacity(1);
                Server.getBankConnection().sendMessage("Auctions ");
                Server.getBankConnection().sendMessage("CheckBalance");
            }
        });

        primaryStage.setOnCloseRequest(e -> System.exit(0));
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * updates the gui's list of auctions
     * @param auctionsUpdate the list of new auctions
     */
    protected static void updateAuctions(ArrayList<Auction> auctionsUpdate){
        for (Auction auction :
                auctionsUpdate) {
            auctionsBox.getChildren().addAll(auction.getDisplayBox());
        }
    }

    /**
     * Updates the gui's list of items. Updates the selected item and bidstatus if needed
     * @param auctionItemsUpdate the new list of items
     * @param auctionName the auction the items go to
     */
    protected static void updateItemsBox(HashMap<Integer,Item> auctionItemsUpdate, String auctionName){
        if(!currAuction.contentEquals(auctionName)) return;
        itemsBox.getChildren().clear();
        for (Item item : auctionItemsUpdate.values()) {
            itemsBox.getChildren().add(item.getDisplayBox());
        }
        if(currItem != null){
            bidStatusLbl.setText(currItem.getBidStatus());
            if(auctionItemsUpdate.containsKey(currItem.getItemId())){
                setCurrItem(auctionItemsUpdate.get(currItem.getItemId()));
            }
            else {
                currItem = null;
                currItemLbl.setText("Select an Item");
                bidStatusLbl.setText("Item no longer available");
            }
        }
    }

    /**
     * Updates the gui's balance information
     * @param balance the total balance
     * @param withHeldFunds withheld funds
     */
    protected static void updateBalance(double balance, double withHeldFunds){
        balLbl.setText("Balance: " + balance);
        heldBalLbl.setText("Withheld Funds: " + withHeldFunds);
        availableBalLbl.setText("Available Balance: " + (balance + withHeldFunds));
    }

    /**
     * Updates the bid slide and bid field to the selected items bid. The slider gets set from 0 to 2 times the bid
     * @param bid the current bid
     */
    protected static void updateBidSlrVals(int bid){
        bidSldr.setMax(bid*2);
        int majTickUnit = ((int) Math.floor(bid/50)) * 10;
        if(majTickUnit == 0) majTickUnit = 5;
        bidSldr.setMajorTickUnit(majTickUnit);
        bidSldr.setValue(bid);
        bidFld.setText(String.valueOf(bid));
    }

    /**
     * Sets the selected item
     * @param item the selected item
     */
    protected static void setCurrItem(Item item){
        currItem = item;
        currItemLbl.setText("Selected: " + item.getName() + "   $" + item.getBid());
        bidStatusLbl.setText(item.getBidStatus());
        updateBidSlrVals(item.getBid());
    }

    /**
     * Adds an item that has been one to the your items list
     * @param itemName the item you won
     */
    protected static void addToYourItems(String itemName){
        currItemLbl.setText("");
        currItem = null;
        yourItemsBox.getChildren().add(new Label(itemName));
    }

    /**
     * Sets the selected auction and updates the list of items. Also clears the selected item
     * @param auction
     */
    protected static void setCurrAuction(Auction auction){
        currItem = null;
        currItemLbl.setText("Select an Item");
        currAuctionLbl.setText(auction.getName() + "'s Items: ");
        currAuction = auction.getName();
        itemsBox.getChildren().clear();
        updateItemsBox(auction.getItems(),auction.getName());
    }

    protected static void failedLogin(){
        signInLbl.setText("Failed to connect please make sure you login is correct");
        signInLbl.setTextFill(Color.RED);
    }

    protected static void failedReg(){
        signUpLbl.setText("A user already has that password or username");
        signUpLbl.setTextFill(Color.RED);
    }

}
