package com.example.android.inventoryapp;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v4.graphics.BitmapCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.ProductContract;
import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.R.attr.bitmap;
import static android.R.attr.name;
import static android.R.attr.width;
import static android.R.id.message;
import static java.lang.Float.parseFloat;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private Uri currentProductUri;
    private boolean productDetailHasChanged = false;
    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            productDetailHasChanged = true;
            return false;
        }
    };

    private EditText nameEditText;
    private EditText priceEditText;
    private EditText quantityEditText;
    private Button addButton;
    private Button subtractButton;
    private ImageView productImageView;
    private TextView emptyTextView;
    private LinearLayout imageLinearLayout;

    static final int REQUEST_IMAGE_GET = 1;
    static final int DEFAULT_IMAGE_TAG = 100;
    static final int UPLOADED_IMAGE_TAG = 101;
    static final int MEGABYTE = 1000000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent intent = getIntent();
        currentProductUri = intent.getData();
        if (currentProductUri == null) {
            setTitle(getString(R.string.add_a_product));
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.edit_product_details));
            getLoaderManager().initLoader(0, null, this);
        }

        nameEditText = (EditText) findViewById(R.id.edittext_name);
        priceEditText = (EditText) findViewById(R.id.edittext_price);
        quantityEditText = (EditText) findViewById(R.id.edittext_quantity);
        addButton = (Button) findViewById(R.id.button_add);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addOneQuantity();
            }
        });
        subtractButton = (Button) findViewById(R.id.button_subtract);
        subtractButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                subtractOneQuantity();
            }
        });
        productImageView = (ImageView) findViewById(R.id.imageview_product);
        productImageView.setTag(DEFAULT_IMAGE_TAG);
        emptyTextView = (TextView) findViewById(R.id.textview_emptyimage);
        imageLinearLayout = (LinearLayout) findViewById(R.id.linearlayout_productimage);
        imageLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });

        nameEditText.setOnTouchListener(touchListener);
        priceEditText.setOnTouchListener(touchListener);
        quantityEditText.setOnTouchListener(touchListener);
        addButton.setOnTouchListener(touchListener);
        subtractButton.setOnTouchListener(touchListener);
        productImageView.setOnTouchListener(touchListener);
        imageLinearLayout.setOnTouchListener(touchListener);
    }

    private void addOneQuantity() {
        String quantityString = quantityEditText.getText().toString();
        int quantity = 0;
        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
        }
        quantity += 1;
        quantityEditText.setText(String.valueOf(quantity));
    }

    private void subtractOneQuantity() {
        String quantityString = quantityEditText.getText().toString();
        int quantity = 0;
        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
        }

        if (quantity >= 1) {
            quantity -= 1;
        } else {
            quantity = 0;
        }
        quantityEditText.setText(String.valueOf(quantity));
    }

    public void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_GET);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_GET && resultCode == RESULT_OK) {
            Uri fullPhotoUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fullPhotoUri);

                productImageView.setImageBitmap(bitmap);
                productImageView.setTag(UPLOADED_IMAGE_TAG);
                productImageView.setTag(R.id.product_image_length, getFileLength(fullPhotoUri));
                emptyTextView.setVisibility(View.GONE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private long getFileLength(Uri uri) {
        InputStream inputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("temp_", null);
            tempFile.deleteOnExit();
            FileOutputStream out = new FileOutputStream(tempFile);
            IOUtils.copy(inputStream, out);
            long length = tempFile.length(); // Size in bytes
            Log.d("DetailActivity", "file length " + length);

            return length;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_IMAGE
        };

        return new CursorLoader(this,
                currentProductUri,
                projection,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);

            String name = cursor.getString(nameColumnIndex);
            Float price = cursor.getFloat(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            byte[] image = cursor.getBlob(imageColumnIndex);

            if (image != null) {
                Bitmap bitmap = getImage(image);
                productImageView.setImageBitmap(bitmap);
                productImageView.setTag(UPLOADED_IMAGE_TAG);
                productImageView.setTag(R.id.product_image_length, 0);
                emptyTextView.setVisibility(View.GONE);
            } else {
                productImageView.setImageResource(R.drawable.ic_image_black_48dp);
                productImageView.setTag(DEFAULT_IMAGE_TAG);
                emptyTextView.setVisibility(View.VISIBLE);
            }

            nameEditText.setText(name);
            priceEditText.setText(String.valueOf(price));
            quantityEditText.setText(String.valueOf(quantity));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        nameEditText.setText("");
        priceEditText.setText("");
        quantityEditText.setText("");
        productImageView.setImageResource(R.drawable.ic_image_black_48dp);
        emptyTextView.setVisibility(View.VISIBLE);
    }

    public static byte[] getBitmapAsByteArray(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
        return outputStream.toByteArray();
    }

    public static Bitmap getImage(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (currentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveProduct();
                return true;
            case R.id.action_order:
                orderProduct();
                return true;
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                if (!productDetailHasChanged) {
                    NavUtils.navigateUpFromSameTask(DetailActivity.this);
                    return true;
                }

                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                NavUtils.navigateUpFromSameTask(DetailActivity.this);
                            }
                        };

                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveProduct() {

        String nameString = nameEditText.getText().toString().trim();
        String priceString = priceEditText.getText().toString().trim();
        String quantityString = quantityEditText.getText().toString().trim();

        if (currentProductUri == null
                && TextUtils.isEmpty(nameString)
                && TextUtils.isEmpty(priceString)
                && TextUtils.isEmpty(quantityString)) {
            Toast.makeText(this, "Please enter product information.", Toast.LENGTH_SHORT).show();
            return;
        } else if (TextUtils.isEmpty(nameString)){
            Toast.makeText(this, "Please provide a name.", Toast.LENGTH_SHORT).show();
            return;
        } else if (TextUtils.isEmpty(priceString)){
            Toast.makeText(this, "Please put a price.", Toast.LENGTH_SHORT).show();
            return;
        } else if (TextUtils.isEmpty(quantityString)){
            Toast.makeText(this, "Please enter the quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        Float price = 0.0f;
        if (!TextUtils.isEmpty(priceString)) {
            price = parseFloat(priceString);
        }
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
        int quantity = 0;
        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
        }
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);

        //We don't want to save the default image.
        if ((Integer) productImageView.getTag() != DEFAULT_IMAGE_TAG) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) productImageView.getDrawable();
            Bitmap bitmap = bitmapDrawable.getBitmap();

            long imageByteLength = (long) productImageView.getTag(R.id.product_image_length);
            Log.d("DetailActivity", "image byte count " + bitmap.getByteCount());
            if (imageByteLength != 0 && imageByteLength > MEGABYTE) {

                int bitmapWidth = bitmap.getWidth();
                int bitmapHeight = bitmap.getHeight();
                Bitmap scaledBitmap;
                if (bitmapWidth > bitmapHeight) {
                    scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int) 400, (int) 300, false);
                } else {
                    scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int) 300, (int) 400, false);
                }

                byte[] imageByteArray = getBitmapAsByteArray(scaledBitmap);
                scaledBitmap.recycle();
                values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, imageByteArray);
            } else {
                byte[] imageByteArray = getBitmapAsByteArray(bitmap);
                values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, imageByteArray);
            }
        }

        if (currentProductUri == null) {
            Uri newRowUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);
            Toast.makeText(this, "Product saved with uri: " + newRowUri, Toast.LENGTH_SHORT).show();
        } else {
            int rowsAffected = getContentResolver().update(currentProductUri, values, null, null);
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show();
            }
        }

        finish();
    }


    //The ‘order more’ button sends an intent to either a phone app or an email app to contact the supplier using the information stored in the database.
    private void orderProduct() {
        String nameString = nameEditText.getText().toString().trim();
        String priceString = priceEditText.getText().toString().trim();
        String quantityString = quantityEditText.getText().toString().trim();

        if (TextUtils.isEmpty(nameString) && TextUtils.isEmpty(priceString) &&
                TextUtils.isEmpty(quantityString)) {
            return;
        }

        String body = "Product name:" + nameString + "/n"
                + "Price: " + priceString + "/n"
                + "Current quantity: " + quantityString + "/n";

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_SUBJECT, "Order request for " + nameEditText.getText().toString());
        intent.putExtra(Intent.EXTRA_TEXT, body);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }

        finish();
    }

    private void deleteProduct() {
        if (currentProductUri != null) {
            int rowsDeleted = getContentResolver().delete(currentProductUri, null, null);

            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.unable_to_delete_product),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.product_deleted),
                        Toast.LENGTH_SHORT).show();
            }

            finish();
        }

    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.discard_your_changes);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.resume_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_this_product);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        if (!productDetailHasChanged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                };

        showUnsavedChangesDialog(discardButtonClickListener);
    }
    
}
