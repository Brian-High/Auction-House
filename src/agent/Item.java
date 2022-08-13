package agent;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * Stores the information for an Item. Also stores the gui representation and handles corresponding the
 * mouse events
 * bidStatus - any information/import message about an item or a bid on the item
 * displayBox - what users clicks on
 */
public class Item{
    private String name;
    private int itemId;
    private int bid;
    private String bidStatus = "Place A Bid";
    private final HBox displayBox;

    Item(String name, int itemId, int bid) {
        this.name = name;
        this.itemId = itemId;
        this.bid = bid;
        Label nameLbl = new Label(name + "   $" + bid);
        displayBox = new HBox(nameLbl);
        displayBox.setOnMouseEntered(event -> nameLbl.setOpacity(.5));
        displayBox.setOnMouseExited(event -> nameLbl.setOpacity(1));
        displayBox.setOnMouseClicked(event -> {
            Agent.setCurrItem(this);
        });
    }

    public HBox getDisplayBox() {
        return displayBox;
    }

    public String getName() {
        return name;
    }

    public int getItemId() {
        return itemId;
    }

    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
        displayBox.getChildren().clear();
        displayBox.getChildren().add(new Label(name + " " + bid));
    }

    public void setBidStatus(String bidStatus) {
        this.bidStatus = bidStatus;
    }

    public String getBidStatus() {
        return bidStatus;
    }
}
