package com.groupproject.client.Utlis;
import java.util.*;
// sẽ được thay thế bằng database 
public class ItemRespository {
    private static ArrayList<Item> items = new ArrayList<>();
    public static void save (Item item) {
        items.add(item);
    }
    public static List<Item> getAll() {
        return items;
    }

}

