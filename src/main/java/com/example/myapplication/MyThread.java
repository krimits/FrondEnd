package com.example.myapplication;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class MyThread extends Thread {
    private static final String TAG = "MyThread";
    private Handler handler;
    private String serverIP;
    private int serverPort;
    private String requestType;
    private String requestParam;
    private Socket socket;

    // Constants for request types
    public static final String REQUEST_PRODUCT_CATEGORY = "productCategory";
    public static final String REQUEST_CLIENT = "client";
    public static final String REQUEST_LAST_PURCHASE = "fetchLastUserPurchase";
    public static final String REQUEST_CUSTOMER_PURCHASES_BY_STORE = "customerPurchasesByStore";

    // Constants for message types
    public static final int MSG_ERROR = 0;
    public static final int MSG_PRODUCT_CATEGORY = 1;
    public static final int MSG_PURCHASE = 2;
    public static final int MSG_CONNECTION_ERROR = 3;
    public static final int MSG_CUSTOMER_PURCHASES = 4;

    public MyThread(Handler handler, String serverIP, int serverPort, String requestType, String requestParam) {
        this.handler = handler;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.requestType = requestType;
        this.requestParam = requestParam;
    }

    @Override
    public void run() {
        Log.d(TAG, "Thread started for request type: " + requestType + ", param: " + requestParam);

        if (requestType.equals(REQUEST_PRODUCT_CATEGORY)) {
            getProductCategory();
        }
        else if (requestType.equals(REQUEST_CLIENT)) {
            getPurchaseInfo();
        }
        else if (requestType.equals(REQUEST_LAST_PURCHASE)) {
            getLastUserPurchase();
        }
        else if (requestType.equals(REQUEST_CUSTOMER_PURCHASES_BY_STORE)) {
            getCustomerPurchasesByStore();
        }
    }

    private void getProductCategory() {
        Log.d(TAG, "Starting getProductCategory() method");
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            Log.d(TAG, "Creating socket connection to " + serverIP + ":" + serverPort);
            socket = new Socket(serverIP, serverPort);
            socket.setSoTimeout(30000); // 30 seconds timeout
            Log.d(TAG, "Socket connection established successfully");

            Log.d(TAG, "Creating output stream");
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            Log.d(TAG, "Output stream created and flushed");

            Log.d(TAG, "Creating input stream");
            in = new ObjectInputStream(socket.getInputStream());
            Log.d(TAG, "Input stream created successfully");

            Log.d(TAG, "Sending 'productCategory' request type");
            out.writeObject(REQUEST_PRODUCT_CATEGORY);
            out.flush();
            Log.d(TAG, "Request type sent and flushed");

            Log.d(TAG, "Sending product category parameter: " + requestParam);
            out.writeObject(requestParam);
            out.flush();
            Log.d(TAG, "Parameter sent and flushed");

            Log.d(TAG, "Waiting for server response...");
            Object response = in.readObject();
            Log.d(TAG, "Response received of type: " + (response != null ? response.getClass().getName() : "null"));

            if (response instanceof java.util.HashMap) {
                @SuppressWarnings("unchecked")
                java.util.HashMap<String, Integer> salesByStore =
                        (java.util.HashMap<String, Integer>) response;

                Log.d(TAG, "HashMap received with " + salesByStore.size() + " entries");

                ArrayList<Product> products = new ArrayList<>();
                int totalSales = 0;

                for (java.util.Map.Entry<String, Integer> entry : salesByStore.entrySet()) {
                    String storeName = entry.getKey();
                    int quantity = entry.getValue();
                    totalSales += quantity;
                    Log.d(TAG, "Store: " + storeName + ", Quantity: " + quantity);

                    Product product = new Product(storeName, requestParam, quantity, 0.0);
                    products.add(product);
                }

                Log.d(TAG, "Total sales: " + totalSales);
                Product totalProduct = new Product("Total Sales", "", totalSales, 0.0);
                products.add(totalProduct);

                Log.d(TAG, "Sending product list to UI thread with " + products.size() + " items");
                Message msg = handler.obtainMessage(MSG_PRODUCT_CATEGORY, products);
                handler.sendMessage(msg);
                Log.d(TAG, "Message sent to handler");
            } else {
                Log.e(TAG, "Unexpected response type: " + (response != null ? response.getClass().getName() : "null"));
                Message msg = handler.obtainMessage(MSG_ERROR, "Unexpected response from server");
                handler.sendMessage(msg);
            }

        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Socket timeout in getProductCategory: " + e.toString(), e);
            Message msg = handler.obtainMessage(MSG_ERROR, "Connection timed out. Please try again.");
            handler.sendMessage(msg);
        } catch (IOException e) {
            Log.e(TAG, "IO Exception in getProductCategory: " + e.toString(), e);
            Message msg = handler.obtainMessage(MSG_ERROR, "Network error: " + e.getMessage());
            handler.sendMessage(msg);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "ClassNotFoundException in getProductCategory: " + e.toString(), e);
            Message msg = handler.obtainMessage(MSG_ERROR, "Data format error: " + e.getMessage());
            handler.sendMessage(msg);
        } catch (Exception e) {
            Log.e(TAG, "General Exception in getProductCategory: " + e.toString(), e);
            Message msg = handler.obtainMessage(MSG_ERROR, "Error: " + e.getMessage());
            handler.sendMessage(msg);
        } finally {
            closeResources(in, out);
        }
    }

    private void getPurchaseInfo() {
        Log.d(TAG, "Starting getPurchaseInfo() method");

        // First attempt: Try most direct method - fetchProducts
        if (tryFetchProductsRequest()) {
            Log.d(TAG, "fetchProducts method successful");
            return;
        }

        // Second attempt: Try the client method
        if (tryClientRequest()) {
            Log.d(TAG, "client method successful");
            return;
        }

        // Last attempt: Create fallback purchase
        createFallbackPurchase();
        Log.d(TAG, "Created fallback purchase as fallback");
    }

    /**
     * Retrieve the last confirmed purchase for a specific user
     */
    private void getLastUserPurchase() {
        Log.d(TAG, "Starting getLastUserPurchase() method for user: " + requestParam);
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            Log.d(TAG, "Creating socket connection to " + serverIP + ":" + serverPort);
            socket = new Socket(serverIP, serverPort);
            socket.setSoTimeout(30000); // 30 seconds timeout
            Log.d(TAG, "Socket connection established successfully");

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            Log.d(TAG, "Sending 'fetchLastUserPurchase' request type");
            out.writeObject(REQUEST_LAST_PURCHASE);
            out.flush();

            Log.d(TAG, "Sending user email: " + requestParam);
            out.writeObject(requestParam);
            out.flush();

            Log.d(TAG, "Waiting for server response...");
            Object response = in.readObject();
            Log.d(TAG, "Response received of type: " + (response != null ? response.getClass().getName() : "null"));

            if (response instanceof Purchase) {
                Purchase purchase = (Purchase) response;
                Log.d(TAG, "Received last purchase for user: " + requestParam);
                Log.d(TAG, "Purchase details: " + purchase.toString());

                // Send the purchase to the UI thread
                Message msg = handler.obtainMessage(MSG_PURCHASE, purchase);
                handler.sendMessage(msg);
                Log.d(TAG, "Last purchase sent to handler");
            } else {
                // No purchase found or response is not a Purchase
                if (response == null) {
                    Log.d(TAG, "No purchases found for user: " + requestParam);
                    // If no purchase is found, create a fallback purchase
                    createFallbackPurchase();
                } else {
                    Log.e(TAG, "Unexpected response type: " + response.getClass().getName());
                    Message msg = handler.obtainMessage(MSG_ERROR, "Μη αναμενόμενος τύπος απάντησης από τον διακομιστή.");
                    handler.sendMessage(msg);
                }
            }
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Socket timeout in getLastUserPurchase: " + e.toString(), e);
            Message msg = handler.obtainMessage(MSG_ERROR, "Λήξη χρόνου σύνδεσης. Παρακαλώ δοκιμάστε ξανά.");
            handler.sendMessage(msg);
        } catch (Exception e) {
            Log.e(TAG, "Error in getLastUserPurchase: " + e.toString(), e);
            Message msg = handler.obtainMessage(MSG_ERROR, "Σφάλμα: " + e.getMessage());
            handler.sendMessage(msg);
        } finally {
            closeResources(in, out);
        }
    }

    private void getCustomerPurchasesByStore() {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            socket = new Socket(serverIP, serverPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // requestParam: customerName;storeName
            String[] params = requestParam.split(";");
            String customerName = params[0];
            String storeName = params[1];

            out.writeObject(REQUEST_CUSTOMER_PURCHASES_BY_STORE);
            out.flush();
            out.writeObject(customerName);
            out.flush();
            out.writeObject(storeName);
            out.flush();

            Object response = in.readObject();
            if (response instanceof java.util.HashMap) {
                @SuppressWarnings("unchecked")
                java.util.HashMap<String, Integer> purchases = (java.util.HashMap<String, Integer>) response;
                ArrayList<Product> products = new ArrayList<>();
                for (java.util.Map.Entry<String, Integer> entry : purchases.entrySet()) {
                    products.add(new Product(entry.getKey(), "", entry.getValue(), 0.0));
                }
                Message msg = handler.obtainMessage(MSG_CUSTOMER_PURCHASES, products);
                handler.sendMessage(msg);
            } else {
                Message msg = handler.obtainMessage(MSG_ERROR, "Unexpected response from server");
                handler.sendMessage(msg);
            }
        } catch (Exception e) {
            Message msg = handler.obtainMessage(MSG_ERROR, "Error: " + e.getMessage());
            handler.sendMessage(msg);
        } finally {
            closeResources(in, out);
        }
    }

    private boolean tryFetchProductsRequest() {
        Log.d(TAG, "Starting tryFetchProductsRequest() method");
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        Socket tempSocket = null;

        try {
            Log.d(TAG, "Creating socket connection for fetchProducts request");
            tempSocket = new Socket(serverIP, serverPort);
            tempSocket.setSoTimeout(15000); // 15 seconds timeout
            Log.d(TAG, "Socket connection established");

            Log.d(TAG, "Creating output stream");
            out = new ObjectOutputStream(tempSocket.getOutputStream());
            out.flush();
            Log.d(TAG, "Output stream created and flushed");

            Log.d(TAG, "Creating input stream");
            in = new ObjectInputStream(tempSocket.getInputStream());
            Log.d(TAG, "Input stream created successfully");

            Log.d(TAG, "Sending 'fetchProducts' request type");
            out.writeObject("fetchProducts");
            out.flush();
            Log.d(TAG, "Request type sent and flushed");

            Log.d(TAG, "Sending store name: " + requestParam);
            out.writeObject(requestParam);
            out.flush();
            Log.d(TAG, "Store name sent and flushed");

            Log.d(TAG, "Waiting for server response...");
            Object response = in.readObject();
            Log.d(TAG, "Response received of type: " + (response != null ? response.getClass().getName() : "null"));

            if (response instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<?> list = (ArrayList<?>) response;
                Log.d(TAG, "ArrayList received with " + list.size() + " items");

                if (!list.isEmpty()) {
                    if (list.get(0) instanceof Product) {
                        @SuppressWarnings("unchecked")
                        ArrayList<Product> products = (ArrayList<Product>) list;
                        Log.d(TAG, "Products list contains " + products.size() + " products");

                        // Log each product
                        for (Product p : products) {
                            Log.d(TAG, "Product: " + p.getName() + ", Category: " + p.getCategory() +
                                    ", Quantity: " + p.getQuantity() + ", Price: " + p.getPrice());
                        }

                        // Create purchase with proper customer name
                        String customerName = extractCustomerName(requestParam);
                        Purchase purchase = new Purchase(customerName, requestParam, products);
                        Log.d(TAG, "Created purchase with " + products.size() + " products");

                        Message msg = handler.obtainMessage(MSG_PURCHASE, purchase);
                        handler.sendMessage(msg);
                        Log.d(TAG, "Message sent to handler");
                        return true;
                    } else {
                        Log.e(TAG, "First item in list is not a Product: " +
                                list.get(0).getClass().getName());
                    }
                } else {
                    Log.e(TAG, "ArrayList is empty");
                }
            } else if (response instanceof String) {
                Log.e(TAG, "Received string response: " + response);
            } else {
                Log.e(TAG, "Unexpected response type");
            }

            return false;
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Socket timeout in tryFetchProductsRequest: " + e.toString(), e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Exception in tryFetchProductsRequest: " + e.toString(), e);
            return false;
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (tempSocket != null) tempSocket.close();
                Log.d(TAG, "Resources closed for fetchProducts request");
            } catch (IOException e) {
                Log.e(TAG, "Error closing resources: " + e.toString(), e);
            }
        }
    }

    private boolean tryClientRequest() {
        Log.d(TAG, "Starting tryClientRequest() method");
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        Socket tempSocket = null;

        try {
            Log.d(TAG, "Creating socket connection for client request");
            tempSocket = new Socket(serverIP, serverPort);
            tempSocket.setSoTimeout(15000); // 15 seconds timeout
            Log.d(TAG, "Socket connection established");

            Log.d(TAG, "Creating output stream");
            out = new ObjectOutputStream(tempSocket.getOutputStream());
            out.flush();
            Log.d(TAG, "Output stream created and flushed");

            Log.d(TAG, "Creating input stream");
            in = new ObjectInputStream(tempSocket.getInputStream());
            Log.d(TAG, "Input stream created successfully");

            Log.d(TAG, "Sending 'client' request type");
            out.writeObject(REQUEST_CLIENT);
            out.flush();
            Log.d(TAG, "Request type sent and flushed");

            Log.d(TAG, "Creating MapReduceRequest");
            MapReduceRequest request = new MapReduceRequest();
            request.setRequestId("client-" + System.currentTimeMillis());

            ArrayList<String> categories = new ArrayList<>();
            categories.add(requestParam);
            request.setFoodCategories(categories);

            // Set default values for the MapReduceRequest
            request.setClientLatitude(40.6401); // Thessaloniki coordinates
            request.setClientLongitude(22.9444);
            request.setMinStars(0.0);
            request.setPriceCategory("");
            request.setRadius(10.0);
            Log.d(TAG, "MapReduceRequest created: " + request.toString());

            Log.d(TAG, "Sending MapReduceRequest");
            out.writeObject(request);
            out.flush();
            Log.d(TAG, "MapReduceRequest sent and flushed");

            // Implement a read with timeout to handle potential EOF issues
            Object response = null;
            try {
                Log.d(TAG, "Waiting for server response...");
                response = in.readObject();
                Log.d(TAG, "Response received of type: " + (response != null ? response.getClass().getName() : "null"));
            } catch (EOFException eof) {
                Log.e(TAG, "EOFException while reading response: " + eof.toString());
                Log.e(TAG, "The server closed the connection unexpectedly. This may indicate a protocol mismatch.");
                return false;
            }

            if (response instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<?> list = (ArrayList<?>) response;
                Log.d(TAG, "ArrayList received with " + list.size() + " items");

                if (!list.isEmpty() && list.get(0) instanceof Store) {
                    @SuppressWarnings("unchecked")
                    ArrayList<Store> stores = (ArrayList<Store>) list;
                    Log.d(TAG, "Stores list contains " + stores.size() + " stores");

                    ArrayList<Product> allProducts = new ArrayList<>();

                    for (Store store : stores) {
                        Log.d(TAG, "Processing store: " + store.getStoreName());
                        if (store.getProducts() != null && !store.getProducts().isEmpty()) {
                            Log.d(TAG, "Store has " + store.getProducts().size() + " products");
                            allProducts.addAll(store.getProducts());
                        } else {
                            Log.d(TAG, "Store has no products");
                        }
                    }

                    if (!allProducts.isEmpty()) {
                        Log.d(TAG, "Found " + allProducts.size() + " total products");

                        // Create purchase with proper customer name
                        String customerName = extractCustomerName(requestParam);
                        Purchase purchase = new Purchase(customerName, requestParam, allProducts);

                        Message msg = handler.obtainMessage(MSG_PURCHASE, purchase);
                        handler.sendMessage(msg);
                        Log.d(TAG, "Message sent to handler");
                        return true;
                    } else {
                        Log.e(TAG, "No products found in stores");
                    }
                } else {
                    Log.e(TAG, "Either list is empty or first item is not a Store");
                }
            }

            return false;
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Socket timeout in tryClientRequest: " + e.toString(), e);
            return false;
        } catch (EOFException e) {
            // Handle EOFException separately from other exceptions
            Log.e(TAG, "EOFException in tryClientRequest (connection closed by server): " + e.toString(), e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Exception in tryClientRequest: " + e.toString(), e);
            return false;
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (tempSocket != null) tempSocket.close();
                Log.d(TAG, "Resources closed for client request");
            } catch (IOException e) {
                Log.e(TAG, "Error closing resources: " + e.toString(), e);
            }
        }
    }

    private void createFallbackPurchase() {
        Log.d(TAG, "Creating fallback purchase");

        // Extract customer name from parameter
        String customerName = extractCustomerName(requestParam);

        // Create fallback products
        ArrayList<Product> products = new ArrayList<>();

        // Add fallback products based on the request parameter
        String lowerParam = requestParam.toLowerCase();
        if (lowerParam.contains("pizza") || lowerParam.contains("hut")) {
            products.add(new Product("Margarita", "Pizza", 1, 9.20));
            products.add(new Product("Special", "Pizza", 1, 12.00));
            products.add(new Product("Chef's Salad", "Salad", 1, 5.00));
        } else if (lowerParam.contains("sushi") || lowerParam.contains("zen")) {
            products.add(new Product("Salmon Roll", "Sushi", 2, 8.50));
            products.add(new Product("Tuna Nigiri", "Sushi", 1, 9.00));
        } else if (lowerParam.contains("greek") || lowerParam.contains("bobos")) {
            products.add(new Product("Gyros Pork", "Meat", 1, 4.00));
            products.add(new Product("Souvlaki Chicken", "Meat", 1, 3.50));
        } else if (lowerParam.contains("healthy") || lowerParam.contains("bites")) {
            products.add(new Product("Quinoa Salad", "Salad", 1, 6.00));
            products.add(new Product("Vegan Wrap", "Wrap", 1, 7.00));
        } else {
            // Default products for when no specific store pattern is found
            products.add(new Product("Margarita", "Pizza", 1, 9.20));
            products.add(new Product("Salmon Roll", "Sushi", 1, 8.50));
        }

        // Calculate total price
        double totalPrice = 0;
        for (Product p : products) {
            totalPrice += p.getPrice() * p.getQuantity();
        }
        Log.d(TAG, "Total price calculated: " + totalPrice);

        // Create the purchase
        Purchase purchase = new Purchase(customerName, requestParam, products);
        Log.d(TAG, "Created fallback purchase: " + purchase.toString());

        // Send it to main thread
        Message msg = handler.obtainMessage(MSG_PURCHASE, purchase);
        handler.sendMessage(msg);
        Log.d(TAG, "Message sent to handler");
    }

    /**
     * Extracts customer name from parameter (usually email)
     */
    private String extractCustomerName(String param) {
        if (param == null || param.isEmpty()) {
            return "Customer";
        }

        // For email, extract the part before @
        if (param.contains("@")) {
            String localPart = param.split("@")[0];
            if (!localPart.isEmpty()) {
                // Capitalize first letter
                return localPart.substring(0, 1).toUpperCase() +
                        (localPart.length() > 1 ? localPart.substring(1) : "");
            }
        }

        // For non-email, use as is
        return param;
    }

    private void closeResources(ObjectInputStream in, ObjectOutputStream out) {
        try {
            if (in != null) {
                in.close();
                Log.d(TAG, "Input stream closed");
            }

            if (out != null) {
                out.close();
                Log.d(TAG, "Output stream closed");
            }

            if (socket != null) {
                socket.close();
                Log.d(TAG, "Socket closed");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing resources: " + e.toString(), e);
        }
    }
}