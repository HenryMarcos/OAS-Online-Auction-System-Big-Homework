package com.groupproject.shared.model.item;

public class Art extends Item {
    private static final long serialVersionUID = 1L;

    private String artist; // Tên nghệ sĩ tạo ra tác phẩm nghệ thuật

    public Art() {
        super();
    }

    public Art(String name, String description, String artist) {
        super(name, description);
        this.artist = artist;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}
