package com.example.myapplication;

import java.io.Serializable;
import java.util.*;

public class Purchase implements Serializable {
    private String customerName;
    private String customerEmail;
    private ArrayList<Product> purchasedProducts;
    private double totalPrice;
    private long purchaseTime;

    public Purchase(String customerName, String customerEmail, ArrayList<Product> purchasedProducts) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.purchasedProducts = purchasedProducts;
        this.totalPrice = calculateTotalPrice();
    }

    // Getters and Setters
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public ArrayList<Product> getPurchasedProducts() {
        return purchasedProducts;
    }

    public void setPurchasedProducts(ArrayList<Product> purchasedProducts) {
        this.purchasedProducts = purchasedProducts;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public long getPurchaseTime() {
        return purchaseTime;
    }

    public void setPurchaseTime(long purchaseTime) {
        this.purchaseTime = purchaseTime;
    }

    // Calculate total price of the purchase
    private double calculateTotalPrice() {
        double total = 0.0;
        for (Product product : purchasedProducts) {
            total += product.getPrice() * product.getQuantity();
        }
        return total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Customer Name: ").append(customerName).append("\n");
        sb.append("Customer Email: ").append(customerEmail).append("\n");
        sb.append("Products:\n");
        for (Product product : purchasedProducts) {
            sb.append(product.toString()).append("\n");
        }
        sb.append("Total Price: ").append(totalPrice).append(" €");
        return sb.toString();
    }
}
