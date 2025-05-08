package com.example.myapplication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Request class for server communication, containing filter parameters
 */
public class MapReduceRequest implements Serializable {

    private double clientLatitude;
    private double clientLongitude;
    private ArrayList<String> foodCategories;
    private double minStars;
    private String priceCategory;
    private double radius;
    private String requestId;

    /**
     * Default constructor
     */
    public MapReduceRequest() {
        // Default constructor
        this.foodCategories = new ArrayList<>();
    }

    /**
     * Full constructor with all parameters
     */
    public MapReduceRequest(double clientLatitude, double clientLongitude,
                            ArrayList<String> foodCategories, double minStars,
                            String priceCategory, double radius, String requestId) {
        this.clientLatitude = clientLatitude;
        this.clientLongitude = clientLongitude;
        this.foodCategories = foodCategories;
        this.minStars = minStars;
        this.priceCategory = priceCategory;
        this.radius = radius;
        this.requestId = requestId;
    }

    // Getters and Setters
    public double getClientLatitude() {
        return clientLatitude;
    }

    public void setClientLatitude(double clientLatitude) {
        this.clientLatitude = clientLatitude;
    }

    public double getClientLongitude() {
        return clientLongitude;
    }

    public void setClientLongitude(double clientLongitude) {
        this.clientLongitude = clientLongitude;
    }

    public List<String> getFoodCategories() {
        return foodCategories;
    }

    public void setFoodCategories(ArrayList<String> foodCategories) {
        this.foodCategories = foodCategories;
    }

    public double getMinStars() {
        return minStars;
    }

    public void setMinStars(double minStars) {
        this.minStars = minStars;
    }

    public String getPriceCategory() {
        return priceCategory;
    }

    public void setPriceCategory(String priceCategory) {
        this.priceCategory = priceCategory;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return "MapReduceRequest{" +
                "clientLatitude=" + clientLatitude +
                ", clientLongitude=" + clientLongitude +
                ", foodCategories=" + foodCategories +
                ", minStars=" + minStars +
                ", priceCategory='" + priceCategory + '\'' +
                ", radius=" + radius +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}