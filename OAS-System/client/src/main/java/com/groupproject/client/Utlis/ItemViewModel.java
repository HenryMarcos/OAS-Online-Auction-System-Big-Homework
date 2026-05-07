package com.groupproject.client.Utlis;
// ve sau se thay the toan bo item bang lenh import com.groupproject.client.shared.package.Auction;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class ItemViewModel {
    private final DoubleProperty currentprice = new SimpleDoubleProperty();
    private final Item item;
    public ItemViewModel(Item item) {
        this.item= item;
        this.currentprice.set(item.getCurrentPrice());
    }
    public DoubleProperty currentPriceProperty() {
        return currentprice;
    }
    public void updatePrice(double newPrice) {
        this.currentprice.set(newPrice);
        this.item.setCurrentPrice(newPrice);
    }
    public Item getItem() {
        return item;
    }
}
