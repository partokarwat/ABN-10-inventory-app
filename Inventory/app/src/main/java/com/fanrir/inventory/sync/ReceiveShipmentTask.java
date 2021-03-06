package com.fanrir.inventory.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.Toast;

import com.fanrir.inventory.R;
import com.fanrir.inventory.data.InventoryContract;
import com.fanrir.inventory.data.InventoryDbHelper;
import com.fanrir.inventory.ui.DetailActivity;
import com.fanrir.inventory.ui.DetailFragment;
import com.fanrir.inventory.ui.InventoryFragment;
import com.fanrir.inventory.ui.MainActivity;

/**
 * Created by Eisdrachl on 04.07.2016.
 */
public class ReceiveShipmentTask extends AsyncTask<String, Void, Void> {
    public final String LOG_TAG = ReceiveShipmentTask.class.getSimpleName();

    private static final int DETAIL_LOADER = 0;

    private final Context mContext;
    private String mName;
    private int mQuantitySold;
    private boolean isNotAvailable;

    public ReceiveShipmentTask(Context context, String name, int quantitySold) {
        mContext = context;
        this.mName = name;
        mQuantitySold = quantitySold;
    }

    /**
     * Helper method to handle update of a product in the inventory database.
     */
    void updateProduct(String name, int quantity) {
        InventoryDbHelper mDbHelper = new InventoryDbHelper(mContext);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                InventoryContract.InventoryEntry._ID,
                InventoryContract.InventoryEntry.COLUMN_NAME,
                InventoryContract.InventoryEntry.COLUMN_QUANTITY,
                InventoryContract.InventoryEntry.COLUMN_AVAILABLE
        };

        Cursor productCursor = db.query(
                InventoryContract.InventoryEntry.TABLE_NAME,                // The table to query
                projection,                               // The columns to return
                InventoryContract.InventoryEntry.COLUMN_NAME + " = ?",      // The columns for the WHERE clause
                new String[]{name},                       // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        ContentValues productValues = new ContentValues();

        productCursor.moveToFirst();

        // update quantity
        int oldQuantity = productCursor.getInt(productCursor.getColumnIndexOrThrow(InventoryContract.InventoryEntry.COLUMN_QUANTITY));
        int newQuantity = oldQuantity + quantity;
        productValues.put(InventoryContract.InventoryEntry.COLUMN_QUANTITY, newQuantity);

        // refresh available
        if (newQuantity > 0) {
            productValues.put(InventoryContract.InventoryEntry.COLUMN_AVAILABLE, 1);
        }

        String selection = InventoryContract.InventoryEntry.COLUMN_NAME + " LIKE ?";
        String[] selectionArgs = {name};

        // Finally, insert inventory data into the database.
        db.update(
                InventoryContract.InventoryEntry.TABLE_NAME,
                productValues,
                selection,
                selectionArgs
        );
    }

    @Override
    protected Void doInBackground(String... params) {
        updateProduct(mName, mQuantitySold);
        Log.i(LOG_TAG, mContext.getString(R.string.product_selling_done));
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (isNotAvailable) {
            Toast.makeText(mContext, R.string.bad_sell_quantity_error, Toast.LENGTH_SHORT).show();
        } else {
            try {
                // Reload current fragment
                DetailFragment df = (DetailFragment) getSupportFragmentManager().findFragmentByTag(DetailFragment.class.getSimpleName());
                df.getLoaderManager().restartLoader(DETAIL_LOADER, null, df);
            } catch (Exception e) {
                Log.v(LOG_TAG, "DetailFragment not reloaded");
            }
            try {
                // Reload current fragment
                InventoryFragment inventoryFragment = (InventoryFragment) getSupportFragmentManager().findFragmentByTag(InventoryFragment.class.getSimpleName());
                inventoryFragment.getLoaderManager().restartLoader(DETAIL_LOADER, null, inventoryFragment);
            } catch (Exception e) {
                Log.v(LOG_TAG, "InventoryFragment not reloaded");
            }
            Toast.makeText(mContext, mName + mContext.getString(R.string.received), Toast.LENGTH_SHORT).show();
        }
    }

    private FragmentManager getSupportFragmentManager() {
        try {
            final MainActivity activity = (MainActivity) mContext;

            // Return the fragment manager
            return activity.getSupportFragmentManager();

        } catch (ClassCastException e) {
            Log.d(LOG_TAG, mContext.getString(R.string.error_cant_get_fragment));
        }
        try {
            final DetailActivity activity = (DetailActivity) mContext;

            // Return the fragment manager
            return activity.getSupportFragmentManager();

        } catch (ClassCastException e) {
            Log.d(LOG_TAG, mContext.getString(R.string.error_cant_get_fragment));
        }
        return null;
    }
}
