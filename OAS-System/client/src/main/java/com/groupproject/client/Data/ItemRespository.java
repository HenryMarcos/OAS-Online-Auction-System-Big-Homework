package com.groupproject.client.Data;
import java.util.*;

public class ItemRespository {
    private static ArrayList<Item> items = new ArrayList<>();
    public static void save (Item item) {
        items.add(item);
    }
    public static List<Item> getAll() {
        return items;
    }

}

