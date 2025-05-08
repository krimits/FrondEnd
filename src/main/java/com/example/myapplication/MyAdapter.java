package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;

/**
 * Adapter for displaying product data in a ListView
 */
public class MyAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<Product> productList;
    private LayoutInflater inflater;

    /**
     * Constructor
     * @param context Application context
     * @param productList List of products to display
     */
    public MyAdapter(Context context, ArrayList<Product> productList) {
        this.context = context;
        this.productList = productList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return productList.size();
    }

    @Override
    public Object getItem(int position) {
        return productList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            // Inflate the layout for each item
            convertView = inflater.inflate(R.layout.item_layout, parent, false);

            holder = new ViewHolder();
            holder.tvStoreName = convertView.findViewById(R.id.tvStoreName);
            holder.tvQuantity = convertView.findViewById(R.id.tvQuantity);
            holder.tvCategory = convertView.findViewById(R.id.tvCategory);
            holder.tvAmount = convertView.findViewById(R.id.tvAmount); // Νέο TextView για το ποσό

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Get the product at this position
        Product product = productList.get(position);

        // Set store name (προϊόν)
        holder.tvStoreName.setText(product.getName());

        // Set quantity
        holder.tvQuantity.setText(String.valueOf(product.getQuantity()));

        // Set category (if not empty)
        if (product.getCategory() != null && !product.getCategory().isEmpty()) {
            holder.tvCategory.setText(product.getCategory());
            holder.tvCategory.setVisibility(View.VISIBLE);
        } else {
            holder.tvCategory.setVisibility(View.GONE);
        }

        // Set amount (ποσότητα * τιμή)
        double amount = product.getQuantity() * product.getPrice();
        holder.tvAmount.setText(String.format("%.2f €", amount));

        // Set different background for the "Total Sales" row
        if (product.getName().equals("Total Sales")) {
            convertView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
        } else {
            convertView.setBackgroundColor(context.getResources().getColor(android.R.color.white));
        }

        return convertView;
    }

    /**
     * ViewHolder pattern for smoother scrolling
     */
    static class ViewHolder {
        TextView tvStoreName;
        TextView tvQuantity;
        TextView tvCategory;
        TextView tvAmount; // TextView για το ποσό
    }
}