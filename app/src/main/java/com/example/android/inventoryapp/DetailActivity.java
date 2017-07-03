package com.example.android.inventoryapp;

import android.app.Activity;
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
import android.os.Build;
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
import android.view.ViewTreeObserver;
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

import butterknife.BindView;
import butterknife.ButterKnife;

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

    @BindView(R.id.edittext_name)
    EditText nameEditText;
    @BindView(R.id.edittext_price)
    EditText priceEditText;
    @BindView(R.id.edittext_quantity)
    EditText quantityEditText;
    @BindView(R.id.button_add)
    Button addButton;
    @BindView(R.id.button_subtract)
    Button subtractButton;
    @BindView(R.id.imageview_product)
    ImageView productImageView;
    @BindView(R.id.textview_emptyimage)
    TextView emptyTextView;
    @BindView(R.id.linearlayout_productimage)
    LinearLayout imageLinearLayout;

    private static final int PICK_IMAGE_REQUEST = 0;
    private static final int DEFAULT_IMAGE_TAG = 100;
    private static final int UPLOADED_IMAGE_TAG = 101;

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

        ButterKnife.bind(this);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addOneQuantity();
            }
        });
        subtractButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                subtractOneQuantity();
            }
        });
        productImageView.setTag(DEFAULT_IMAGE_TAG);
        imageLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImageSelector();
            }
        });

        nameEditText.setOnTouchListener(touchListener);
        priceEditText.setOnTouchListener(touchListener);
        quantityEditText.setOnTouchListener(touchListener);
        addButton.setOnTouchListener(touchListener);
        subtractButton.setOnTouchListener(touchListener);
        productImageView.setOnTouchListener(touchListener);
        imageLinearLayout.setOnTouchListener(touchListener);

        Log.d("DetailActivity", "onCreate");
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

    public void openImageSelector() {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri fullPhotoUri = data.getData();

                productImageView.setImageBitmap(getBitmapFromUri(fullPhotoUri));
                productImageView.setTag(R.id.product_image_uri, fullPhotoUri.toString());
                productImageView.setTag(UPLOADED_IMAGE_TAG);
                emptyTextView.setVisibility(View.GONE);
            }
        }
    }

    public Bitmap getBitmapFromUri(Uri uri) {

        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the View
        int targetW = imageLinearLayout.getWidth();
        int targetH = imageLinearLayout.getHeight();
        Log.d("DetailActivity", "View: Width " + targetW + "height" + targetH);

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;
            Log.d("DetailActivity", "Photo: Width " + photoW + "height" + photoH);

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();
            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e("DetailActivity", "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e("DetailActivity", "Failed to load image.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
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
            String uriString = cursor.getString(imageColumnIndex);

            if (uriString != null) {
                restoreProductImageAfterLayoutPhase(Uri.parse(uriString));
                productImageView.setTag(R.id.product_image_uri, uriString);
                productImageView.setTag(UPLOADED_IMAGE_TAG);
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

        Log.d("DetailActivity", "onLoadFinished()");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        nameEditText.setText("");
        priceEditText.setText("");
        quantityEditText.setText("");
        productImageView.setImageResource(R.drawable.ic_image_black_48dp);
        emptyTextView.setVisibility(View.VISIBLE);
    }

    private void restoreProductImageAfterLayoutPhase(final Uri uri) {
        ViewTreeObserver viewTreeObserver = imageLinearLayout.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    imageLinearLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    imageLinearLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                productImageView.setImageBitmap(getBitmapFromUri(uri));
            }
        });
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
        } else if (TextUtils.isEmpty(nameString)) {
            Toast.makeText(this, "Please provide a name.", Toast.LENGTH_SHORT).show();
            return;
        } else if (TextUtils.isEmpty(priceString)) {
            Toast.makeText(this, "Please put a price.", Toast.LENGTH_SHORT).show();
            return;
        } else if (TextUtils.isEmpty(quantityString)) {
            Toast.makeText(this, "Please enter the quantity", Toast.LENGTH_SHORT).show();
            return;
        } else if ((Integer) productImageView.getTag() == DEFAULT_IMAGE_TAG) {
            Toast.makeText(this, "Please upload an image.", Toast.LENGTH_SHORT).show();
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
        String uriString = (String) productImageView.getTag(R.id.product_image_uri);
        values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, uriString);

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
        builder.setNegativeButton(R.string.cancel, null);

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
