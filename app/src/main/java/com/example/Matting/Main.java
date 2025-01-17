package com.example.Matting;

public class Main {
    private String title;
    private String category;
    private String address;
    private String link;
    private double rating;
    private int map_x;
    private int map_y;

    public Main(String title, String category, String address, String link, double rating, int map_x, int map_y) {
        this.title = title;
        this.category = category;
        this.address = address;
        this.link = link;
        this.rating = rating;
        this.map_x = map_x;
        this.map_y = map_y;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getAddress() {
        return address;
    }

    public String getLink() {
        return link;
    }

    public double getRating() {
        return rating;
    }

    public int getMapX() {
        return map_x;
    }

    public int getMapY() {
        return map_y;
    }
}
