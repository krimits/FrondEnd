package com.example.myapplication;



import java.io.Serializable;
import java.util.List;
import java.util.*;

public class Store implements Serializable {

    private String storeName;
    private double latitude;
    private double longitude;
    private String category;
    private double stars;
    private int noOfReviews;
    private ArrayList<Product> products;
    private ArrayList<Purchase> purchases;

    public Store(String storeName, double latitude, double longitude, String category, double stars, int noOfReviews, String storeLogoPath, ArrayList<Product> products) {
        this.storeName = storeName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
        this.stars = stars;
        this.noOfReviews = noOfReviews;
        this.products = products;
        this.purchases = new  ArrayList<>();
    }


    public String getStoreName() {
        return storeName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getCategory() {
        return category;
    }

    public double getStars() {
        return stars;
    }

    public int getNoOfReviews() {
        return noOfReviews;
    }

    public void setStars(double stars) {
        this.stars = stars;
    }

    public void setNoOfReviews(int noOfReviews) {
        this.noOfReviews = noOfReviews;
    }

    public ArrayList<Product> getProducts() {
        return products;
    }

    public ArrayList<Purchase> getPurchases() {
        return purchases;
    }

    public String calculatePriceCategory() {
        double totalPrice = 0;
        for (Product product : products) {
            totalPrice += product.getPrice();
        }
        double avgPrice = totalPrice / products.size();
        if (avgPrice <= 5) return "$";
        if (avgPrice <= 15) return "$$";
        return "$$$";
    }

    @Override
    public String toString() {
        return "Store Name: " + storeName + "\nCategory: " + category + "\nStars: " + String.format("%.2f", stars) + "\nReviews: " + noOfReviews;
    }
}
