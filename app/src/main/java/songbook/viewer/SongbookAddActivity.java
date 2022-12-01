package songbook.viewer;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import songbook.viewer.services.SongbookService;

public class SongbookAddActivity extends FragmentActivity {

    private final String TAG = SongbookAddActivity.class.getCanonicalName();

    private SongbookService mSongbookService;

    private final ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {

            Log.i(TAG, "onServiceConnected");

            mSongbookService = ((SongbookService.SongbookBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            // never called
        }

    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.import_book);

        InputStream is = null;

        try {
            is = getContentResolver().openInputStream(getIntent().getData());
        } catch (FileNotFoundException notFound) {
            Log.e(TAG, notFound.getMessage());
        }

        // Initialize Import Button

        final Button importButton = (Button) findViewById(R.id.buttonImportSongbook);
        final InputStream inputStream = is;

        importButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {

                String nameValue = ((EditText) findViewById(R.id.importNameEdit))
                        .getText().toString();

                new ImportTask()
                        .execute(new ImportParameter()
                                .setInputStream(inputStream)
                                .setName(nameValue)
                                .setDescription(
                                        ((EditText) findViewById(R.id.importDescEdit))
                                                .getText().toString()));

            }

        });

        bindService(new Intent(SongbookAddActivity.this,
                SongbookService.class), mConnection, Context.BIND_AUTO_CREATE);

        Log.d(TAG, "onCreate");
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mConnection);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.d(TAG, "onOptionsItemSelected");

        return true;

    }

    protected void onStart() {
        super.onStart();
    }

    static class ImportParameter {

        private InputStream inputStream = null;
        private String name = null;
        private String description = null;
        private boolean defaultFlag = false;

        public String getDescription() {
            return description;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public String getName() {
            return name;
        }

        public boolean isDefaultFlag() {
            return defaultFlag;
        }

        public ImportParameter setDefaultFlag(boolean defaultFlag) {
            this.defaultFlag = defaultFlag;
            return this;
        }

        public ImportParameter setDescription(String description) {
            this.description = description;
            return this;
        }

        public ImportParameter setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public ImportParameter setName(String name) {
            this.name = name;
            return this;
        }

    }

    private class ImportTask extends AsyncTask<SongbookAddActivity.ImportParameter, Void, Void> {

        private final String TAG = ImportTask.class.getCanonicalName();

        private ProgressDialog progressDialog;

        private String errorMessage = null;

        protected Void doInBackground(SongbookAddActivity.ImportParameter... params) {

            Log.d(TAG, "doInBackground - starting");

            try {

                mSongbookService.importSongbook(params[0].getInputStream(), params[0].getName(), params[0].getDescription(), params[0].isDefaultFlag());

            } catch (IllegalArgumentException error) {
                errorMessage = error.getMessage();
                Log.e(TAG, errorMessage);
            }

            Log.d(TAG, "doInBackground - finished");

            return null;
        }

        protected void onPostExecute(Void ignore) {

            progressDialog.dismiss();

            String message = null;

            if (errorMessage != null) {

                Log.e(TAG, errorMessage);

                message = getString(R.string.addsb_task_import_failure);

            } else {
                message = getString(R.string.addsb_task_import_success);
            }

            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG)
                    .show();

            Log.d(TAG, "doInBackground - onPostExecute");

            finish();
        }

        @Override
        protected void onPreExecute() {

            progressDialog = new ProgressDialog(SongbookAddActivity.this);
            progressDialog.setMessage(getString(R.string.importing_message));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();

        }
    }

}