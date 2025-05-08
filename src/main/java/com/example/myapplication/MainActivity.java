package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

/**
 * Main activity class for the application
 * Handles UI interactions and displays data from the server
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ListView purchaseListView;
    private Button btnConnect;
    private Button btnViewCategory;
    private Button btnViewCustomerPurchases;
    private EditText etCustomerEmail;
    private EditText etProductCategory;
    private EditText etCustomerName;
    private EditText etStoreName;
    private TextView tvPurchaseInfo;
    private LinearLayout listViewHeader;
    private MyAdapter adapter;
    private ArrayList<Product> productList;
    private Handler handler;
    private ProgressBar progressBar;

    // Server connection details
    private static final String SERVER_IP = "192.168.56.1"; // Master server IP address
    private static final int SERVER_PORT = 4321;            // Master server port

    // Message types
    public static final int MSG_ERROR = 0;
    public static final int MSG_PRODUCT_CATEGORY = 1;
    public static final int MSG_PURCHASE = 2;
    public static final int MSG_CONNECTION_ERROR = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate called");

        // Initialize UI components
        initializeUIComponents();

        // Initialize product list and adapter
        productList = new ArrayList<>();
        adapter = new MyAdapter(this, productList);
        purchaseListView.setAdapter(adapter);

        // Initialize handler to update UI from background thread
        initializeHandler();

        // Set click listeners for buttons
        setupButtonListeners();

        // Restore state if available
        if (savedInstanceState != null) {
            Log.d(TAG, "Restoring state from savedInstanceState");

            if (savedInstanceState.containsKey("purchaseInfo")) {
                tvPurchaseInfo.setText(savedInstanceState.getString("purchaseInfo"));
                tvPurchaseInfo.setVisibility(View.VISIBLE);
                purchaseListView.setVisibility(View.GONE);
                listViewHeader.setVisibility(View.GONE);
            }
            if (savedInstanceState.containsKey("productCategory")) {
                etProductCategory.setText(savedInstanceState.getString("productCategory"));
            }
            if (savedInstanceState.containsKey("customerEmail")) {
                etCustomerEmail.setText(savedInstanceState.getString("customerEmail"));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState called");

        // Save purchase info if available
        if (tvPurchaseInfo.getVisibility() == View.VISIBLE) {
            outState.putString("purchaseInfo", tvPurchaseInfo.getText().toString());
        }
        // Save product category if entered
        if (!etProductCategory.getText().toString().isEmpty()) {
            outState.putString("productCategory", etProductCategory.getText().toString());
        }
        // Save customer email if entered
        if (!etCustomerEmail.getText().toString().isEmpty()) {
            outState.putString("customerEmail", etCustomerEmail.getText().toString());
        }
    }

    @Override
    public void onBackPressed() {
        // If purchase info is displayed, hide it and show the list
        if (tvPurchaseInfo.getVisibility() == View.VISIBLE) {
            Log.d(TAG, "onBackPressed: hiding purchase info and showing list view");
            tvPurchaseInfo.setVisibility(View.GONE);
            purchaseListView.setVisibility(View.VISIBLE);
            listViewHeader.setVisibility(View.VISIBLE);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Initialize all UI components
     */
    private void initializeUIComponents() {
        Log.d(TAG, "Initializing UI components");

        purchaseListView = findViewById(R.id.purchaseListView);
        btnConnect = findViewById(R.id.btnConnect);
        btnViewCategory = findViewById(R.id.btnViewPurchase); // Αναφορά στο υπάρχον ID, θα αλλάξουμε τη λειτουργία
        etProductCategory = findViewById(R.id.etProductCategory);
        etCustomerEmail = findViewById(R.id.etCustomerEmail);
        etCustomerName = findViewById(R.id.etCustomerName);
        etStoreName = findViewById(R.id.etStoreName);
        btnViewCustomerPurchases = findViewById(R.id.btnViewCustomerPurchases);
        tvPurchaseInfo = findViewById(R.id.tvPurchaseInfo);
        listViewHeader = findViewById(R.id.listViewHeader);
        progressBar = findViewById(R.id.progressBar);

        // Αλλαγή κειμένων κουμπιών ώστε να αντανακλούν τη νέα λειτουργικότητα
        btnConnect.setText("Προβολή Τελευταίας Αγοράς");
        btnViewCategory.setText("Προβολή Κατηγορίας Προϊόντος");
    }

    /**
     * Initialize handler for communication with background threads
     */
    private void initializeHandler() {
        Log.d(TAG, "Initializing handler");

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "Handler received message, type: " + msg.what);
                hideProgressBar();

                switch (msg.what) {
                    case MSG_PRODUCT_CATEGORY: // Product category data
                        Log.d(TAG, "Handler processing product category message");
                        handleProductCategoryResponse(msg);
                        break;

                    case MSG_PURCHASE: // Purchase data
                        Log.d(TAG, "Handler processing purchase message");
                        handlePurchaseResponse(msg);
                        break;

                    case MSG_ERROR: // Error
                        Log.d(TAG, "Handler processing error message");
                        handleErrorResponse(msg);
                        break;

                    case MSG_CONNECTION_ERROR: // Connection error
                        Log.d(TAG, "Handler processing connection error message");
                        handleConnectionError(msg);
                        break;

                    case MyThread.MSG_CUSTOMER_PURCHASES:
                        handleCustomerPurchasesResponse(msg);
                        break;

                    default:
                        Log.d(TAG, "Unknown message type: " + msg.what);
                        break;
                }
            }
        };
    }

    /**
     * Set up click listeners for buttons
     */
    private void setupButtonListeners() {
        Log.d(TAG, "Setting up button listeners");

        // Connect button now shows customer's last purchase
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String customerEmail = etCustomerEmail.getText().toString().trim();
                if (!customerEmail.isEmpty()) {
                    Log.d(TAG, "Requesting last purchase for email: " + customerEmail);
                    showProgressBar();

                    // Start thread to connect to server for the last purchase data
                    MyThread thread = new MyThread(handler, SERVER_IP, SERVER_PORT,
                            MyThread.REQUEST_LAST_PURCHASE, customerEmail);
                    thread.start();
                    Toast.makeText(MainActivity.this, "Λήψη τελευταίας αγοράς...", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "Customer email field is empty");
                    Toast.makeText(MainActivity.this, "Παρακαλώ εισάγετε email πελάτη", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // View category button now shows product category data
        btnViewCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String productCategory = etProductCategory.getText().toString().trim();
                if (!productCategory.isEmpty()) {
                    Log.d(TAG, "Requesting product category data for: " + productCategory);
                    showProgressBar();

                    // Start thread to connect to server for product category data
                    MyThread thread = new MyThread(handler, SERVER_IP, SERVER_PORT,
                            MyThread.REQUEST_PRODUCT_CATEGORY, productCategory);
                    thread.start();
                    Toast.makeText(MainActivity.this, "Σύνδεση με διακομιστή...", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "Product category field is empty");
                    Toast.makeText(MainActivity.this, "Παρακαλώ εισάγετε κατηγορία προϊόντος", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnViewCustomerPurchases.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String customerName = etCustomerName.getText().toString().trim();
                String storeName = etStoreName.getText().toString().trim();
                if (!customerName.isEmpty() && !storeName.isEmpty()) {
                    showProgressBar();
                    String param = customerName + ";" + storeName;
                    MyThread thread = new MyThread(handler, SERVER_IP, SERVER_PORT,
                            MyThread.REQUEST_CUSTOMER_PURCHASES_BY_STORE, param);
                    thread.start();
                    Toast.makeText(MainActivity.this, "Ανάκτηση αγορών πελάτη...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Συμπλήρωσε όνομα πελάτη και κατάστημα", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Handle product category response
     * @param msg Message containing product category data
     */
    private void handleProductCategoryResponse(Message msg) {
        Log.d(TAG, "Handling product category response");

        try {
            // Update UI with received products
            @SuppressWarnings("unchecked")
            ArrayList<Product> receivedProducts = (ArrayList<Product>) msg.obj;

            if (receivedProducts == null) {
                Log.e(TAG, "Received products list is null");
                Toast.makeText(this, "Δεν ελήφθησαν δεδομένα", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Received " + receivedProducts.size() + " products");

            // Clear existing list and add new products
            productList.clear();
            productList.addAll(receivedProducts);
            adapter.notifyDataSetChanged();

            // Update UI visibility
            tvPurchaseInfo.setVisibility(View.GONE);
            purchaseListView.setVisibility(View.VISIBLE);
            listViewHeader.setVisibility(View.VISIBLE);

            Toast.makeText(MainActivity.this,
                    "Ελήφθησαν " + (receivedProducts.size()-1) + " καταστήματα",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error in handleProductCategoryResponse: " + e.getMessage(), e);
            Toast.makeText(this, "Σφάλμα επεξεργασίας δεδομένων: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle purchase response
     * @param msg Message containing purchase data
     */
    private void handlePurchaseResponse(Message msg) {
        Log.d(TAG, "Handling purchase response");

        try {
            // Get the purchase from the message
            Purchase purchase = (Purchase) msg.obj;
            if (purchase == null) {
                Log.e(TAG, "Purchase object is null!");
                Toast.makeText(this, "Δεν βρέθηκαν στοιχεία αγοράς", Toast.LENGTH_SHORT).show();
                return;
            }

            // Log purchase details for debugging
            Log.d(TAG, "Purchase customer: " + purchase.getCustomerName());
            Log.d(TAG, "Purchase email: " + purchase.getCustomerEmail());
            Log.d(TAG, "Purchase total price: " + purchase.getTotalPrice());
            Log.d(TAG, "Purchase time: " + purchase.getPurchaseTime());

            ArrayList<Product> products = purchase.getPurchasedProducts();
            if (products == null) {
                Log.e(TAG, "Products list is null!");
            } else {
                Log.d(TAG, "Products count: " + products.size());

                // Log each product for debugging
                for (Product p : products) {
                    Log.d(TAG, "Product: " + p.getName() +
                            ", Category: " + p.getCategory() +
                            ", Price: " + p.getPrice() +
                            ", Quantity: " + p.getQuantity());
                }
            }

            // Format purchase info for display
            StringBuilder sb = new StringBuilder();
            sb.append("Τελευταία παραγγελία:\n\n");
            sb.append("Πελάτης: ").append(purchase.getCustomerName()).append("\n");
            sb.append("Email: ").append(purchase.getCustomerEmail()).append("\n\n");
            sb.append("Προϊόντα:\n");

            if (products != null && !products.isEmpty()) {
                for (Product product : products) {
                    if (product == null) continue;

                    sb.append("- ").append(product.getName());

                    if (product.getCategory() != null && !product.getCategory().isEmpty()) {
                        sb.append(" (").append(product.getCategory()).append(")");
                    }

                    sb.append("\n");
                    sb.append("  Τιμή: ").append(String.format("%.2f", product.getPrice())).append(" €\n");
                    sb.append("  Ποσότητα: ").append(product.getQuantity()).append("\n\n");
                }
            } else {
                sb.append("Δεν βρέθηκαν προϊόντα\n\n");
            }

            sb.append("Συνολικό Κόστος: ").append(String.format("%.2f", purchase.getTotalPrice())).append(" €");

            // Update UI
            tvPurchaseInfo.setText(sb.toString());
            tvPurchaseInfo.setVisibility(View.VISIBLE);
            purchaseListView.setVisibility(View.GONE);
            listViewHeader.setVisibility(View.GONE);

            Log.d(TAG, "Purchase info displayed successfully");
            Toast.makeText(MainActivity.this, "Στοιχεία αγοράς ελήφθησαν", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error in handlePurchaseResponse: " + e.getMessage(), e);
            Toast.makeText(this, "Σφάλμα επεξεργασίας αγοράς: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle customer purchases response
     * @param msg Message containing customer purchases data
     */
    private void handleCustomerPurchasesResponse(Message msg) {
        @SuppressWarnings("unchecked")
        ArrayList<Product> purchases = (ArrayList<Product>) msg.obj;
        productList.clear();
        if (purchases != null && !purchases.isEmpty()) {
            productList.addAll(purchases);
            adapter.notifyDataSetChanged();
            purchaseListView.setVisibility(View.VISIBLE);
            listViewHeader.setVisibility(View.VISIBLE);
            tvPurchaseInfo.setVisibility(View.GONE);
        } else {
            tvPurchaseInfo.setText("Δεν βρέθηκαν αγορές για αυτόν τον πελάτη στο κατάστημα.");
            tvPurchaseInfo.setVisibility(View.VISIBLE);
            purchaseListView.setVisibility(View.GONE);
            listViewHeader.setVisibility(View.GONE);
        }
    }

    /**
     * Handle error response
     * @param msg Message containing error information
     */
    private void handleErrorResponse(Message msg) {
        Log.d(TAG, "Handling error response");

        String errorMessage = (String) msg.obj;
        Log.e(TAG, "Error received: " + errorMessage);
        Toast.makeText(MainActivity.this, "Σφάλμα: " + errorMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Handle connection error
     * @param msg Message containing connection error information
     */
    private void handleConnectionError(Message msg) {
        Log.d(TAG, "Handling connection error");

        String errorMessage = (String) msg.obj;
        Log.e(TAG, "Connection error: " + errorMessage);
        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();

        // Display detailed error message
        tvPurchaseInfo.setText("Σφάλμα Σύνδεσης\n\n" + errorMessage +
                "\n\nΠαρακαλώ ελέγξτε τη σύνδεση και τις ρυθμίσεις του διακομιστή." +
                "\n\nΔιακομιστής: " + SERVER_IP + ":" + SERVER_PORT);
        tvPurchaseInfo.setVisibility(View.VISIBLE);
        purchaseListView.setVisibility(View.GONE);
        listViewHeader.setVisibility(View.GONE);
    }

    /**
     * Show progress bar and disable buttons
     */
    private void showProgressBar() {
        Log.d(TAG, "Showing progress bar");

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Disable buttons during processing
        btnConnect.setEnabled(false);
        btnViewCategory.setEnabled(false);
        btnViewCustomerPurchases.setEnabled(false);
    }

    /**
     * Hide progress bar and enable buttons
     */
    private void hideProgressBar() {
        Log.d(TAG, "Hiding progress bar");

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        // Re-enable buttons
        btnConnect.setEnabled(true);
        btnViewCategory.setEnabled(true);
        btnViewCustomerPurchases.setEnabled(true);
    }
}