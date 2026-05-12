package com.groupproject.client.Data;
import java.util.ArrayList;
import java.util.List;
import com.groupproject.shared.model.transaction.Auction;

public class ItemRespository {
    private static ArrayList<Auction> items = new ArrayList<>();
    public static void save (Auction item) {
        items.add(item);
    }
    public static List<Auction> getAll() {
        return items;
    }

}
