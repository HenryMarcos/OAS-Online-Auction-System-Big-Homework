package com.groupproject.shared.model.item;

public class Art extends Item {
    private static final long serialVersionUID = 1L;

    private String artist; // Tên nghệ sĩ tạo ra tác phẩm nghệ thuật

    public Art() {
        super();
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}
