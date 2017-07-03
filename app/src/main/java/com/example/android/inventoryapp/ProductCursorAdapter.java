package com.example.android.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.ProductContract;
import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

import butterknife.BindView;

import static android.R.attr.id;

/**
 * Created by pkhotpanya on 6/29/17.
 */

public class ProductCursorAdapter extends CursorAdapter {

    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        TextView nameTextView = (TextView) view.findViewById(R.id.textview_name);
        TextView priceTextView = (TextView) view.findViewById(R.id.textview_price);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.textview_quantity);
        Button saleButton = (Button) view.findViewById(R.id.button_sale);

        int idColumnIndex = cursor.getColumnIndex(ProductEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);

        int productId = cursor.getInt(idColumnIndex);
        String productName = cursor.getString(nameColumnIndex);
        Float productPrice = cursor.getFloat(priceColumnIndex);
        int productQuantity = cursor.getInt(quantityColumnIndex);

        nameTextView.setText(productName);
        priceTextView.setText("$" + productPrice);
        quantityTextView.setText(String.valueOf(productQuantity));
        saleButton.setTag(R.id.product_database_id, productId);
        saleButton.setTag(R.id.product_database_quantity, productQuantity);
        saleButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                int productDbId = (int) view.getTag(R.id.product_database_id);
                int productDbQuantity = (int) view.getTag(R.id.product_database_quantity);
                makeSale(productDbId, productDbQuantity);
            }

            private void makeSale(int productDatabaseId, int productDatabaseQuantity) {
                if (productDatabaseQuantity <= 0) {
                    return;
                } else {
                    int reducedQuantity = productDatabaseQuantity - 1;
                    quantityTextView.setText(String.valueOf(reducedQuantity));

                    ContentValues values = new ContentValues();
                    values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, reducedQuantity);

                    Uri currentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, productDatabaseId);
                    int rowsAffected = context.getContentResolver().update(currentProductUri, values, null, null);
                    if (rowsAffected == 0) {
                        // If no rows were affected, then there was an error with the update.
                        Log.e("ProductCursorAdapter", context.getString(R.string.failed_to_subtract_quantity));
                    }
                }
            }

        });
    }
}
