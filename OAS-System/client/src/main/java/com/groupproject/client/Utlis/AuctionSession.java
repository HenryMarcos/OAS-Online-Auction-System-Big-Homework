package com.groupproject.client.Utlis;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
// item , bidder, pricepoint (ve do thi);
public class AuctionSession {
    private final ObservableList<PricePoint> priceHistory = FXCollections.observableArrayList();
    public ObservableList<PricePoint> getPriceHistory() { return priceHistory; }
    public static class PricePoint {
        private final String label;
        private final double price;
        public PricePoint(String label, double price) {
            this.price= price;
            this.label= label;
        }
        // getter
        public String getLabel() {
            return label;
        }
        public Double getPrice() {
            return price;
        }
    }

}
