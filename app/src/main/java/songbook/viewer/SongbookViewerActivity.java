package songbook.viewer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SimpleCursorAdapter;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.ListFragment;

import java.io.InputStream;
import java.text.MessageFormat;

import songbook.viewer.data.Songbook;
import songbook.viewer.intent.action.MANAGE_SONGBOOK;
import songbook.viewer.interfaces.OnIndexSelectedListener;
import songbook.viewer.services.SongbookService;

public class SongbookViewerActivity extends FragmentActivity implements OnIndexSelectedListener, OnQueryTextListener {

    private final String TAG = SongbookViewerActivity.class.getCanonicalName();
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

    private long currentCatalogId = SongbookService.PARAM_INVALID_SBID;

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    if (result.getResultCode() == Activity.RESULT_OK) {

                        Intent data = result.getData();

                        if (data != null) {

                            int command = data.getIntExtra(SongbookManageActivity.RESULT_COMMAND, Integer.MIN_VALUE);

                            Log.d(TAG, "command = " + command);

                            switch (command) {

                                case SongbookManageActivity.CONTEXT_DELETE:

                                    long[] deleteList = data.getLongArrayExtra(SongbookManageActivity.RESULT_DELETELIST);

                                    if (deleteList != null) {

                                        for (long songBookId : deleteList) {

                                            if (currentCatalogId == songBookId) {
                                                new InitTask(false).execute(SongbookService.PARAM_INVALID_SBID);
                                                break;
                                            }

                                        }

                                    }

                                    break;

                                case SongbookManageActivity.CONTEXT_LOAD:

                                    long songBookId = data.getLongExtra(SongbookManageActivity.RESULT_LOADSBID, SongbookService.PARAM_INVALID_SBID);

                                    if (songBookId != SongbookService.PARAM_INVALID_SBID && songBookId != currentCatalogId) {
                                        new InitTask(true).execute(songBookId);
                                    }

                                    break;

                            }

                        }

                    }

                }
            });

    void doBindService() {

        bindService(new Intent(SongbookViewerActivity.this, SongbookService.class), mConnection, Context.BIND_AUTO_CREATE);

    }

    void doUnbindService() {

        if (mSongbookService != null) {
            unbindService(mConnection);
        }

    }

    private boolean isArtistChecked() {

        final CheckBox checkBoxArtist = findViewById(R.id.checkBoxArtist);

        return checkBoxArtist.isChecked();

    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final CheckBox checkBoxArtist = findViewById(R.id.bookListFragment).findViewById(R.id.checkBoxArtist);

        checkBoxArtist.setOnClickListener((View v) -> {
            new ByArtistToggleTask().execute(null, null, null);
        });

        Log.d(TAG, "onCreate");
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putLong("currentCatalogId", currentCatalogId);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentCatalogId = savedInstanceState.getLong("currentCatalogId");
        new InitTask(false).execute(currentCatalogId, null, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setIconifiedByDefault(true);
        searchView.setQueryHint(getString(R.string.query_hint));
        searchView.setSubmitButtonEnabled(false);

        searchView.setOnQueryTextListener(this);

        searchView.setOnQueryTextFocusChangeListener((View v, boolean hasFocus) -> {

            if (!hasFocus) {
                searchView.setQuery("", false);
                searchView.setIconified(true);
                searchItem.collapseActionView();
            }

        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    public void onIndexSelected(String letter) {

        Log.d(TAG, "onIndexSelected");

        new SelectIndexTask().execute(letter, null, null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.d(TAG, "onOptionsItemSelected");

        Intent intent = new MANAGE_SONGBOOK();

        activityResultLauncher.launch(intent);

        return true;

    }

    public boolean onQueryTextChange(String text) {
        return false;
    }

    public boolean onQueryTextSubmit(String query) {

        Log.d(TAG, "onQueryTextSubmit = " + query);

        new SearchTask().execute(query);

        return false;
    }

    protected void onStart() {
        super.onStart();
        new InitTask(false).execute(currentCatalogId, null, null);
    }

    private String getDisplayedTitle() {

        String title = null;

        if (currentCatalogId != SongbookService.PARAM_INVALID_SBID) {

            Songbook songBook = mSongbookService.retrieveSongbookById(currentCatalogId, false);

            String description = songBook.getDescription();

            if (description != null && description.trim().length() > 0) {
                title = MessageFormat.format("{0} ({1})", songBook.getName(), description);
            } else {
                title = songBook.getName();
            }

        }

        return title;
    }

    class InitTask extends AsyncTask<Long, Void, Void> {

        private final String TAG = InitTask.class.getCanonicalName();
        private final boolean displayLoadingMessage;
        private ProgressDialog progressDialog;

        private ArrayAdapter<String> arrAdapter;

        private Cursor resultCursor;

        private String[] indeces = new String[0];

        public InitTask(boolean displayLoadingMessage) {
            super();
            this.displayLoadingMessage = displayLoadingMessage;
        }

        protected Void doInBackground(Long... songBookId) {

            Log.d(TAG, "doInBackground - starting");

            while (mSongbookService == null) {
                try {
                    Log.w(TAG, "waiting");
                    Thread.sleep(50);
                } catch (InterruptedException ignore) {

                }
            }

            if (songBookId[0] == SongbookService.PARAM_INVALID_SBID) {

                currentCatalogId = mSongbookService.retrieveDefaultSongbookId();

                if (currentCatalogId == SongbookService.PARAM_INVALID_SBID) {

                    InputStream input = getResources().openRawResource(R.raw.juke);

                    mSongbookService.importSongbook(input, "DEFAULT", "DEFAULT SONGBOOK", true);

                    currentCatalogId = mSongbookService.retrieveDefaultSongbookId();
                }

            } else {
                currentCatalogId = songBookId[0];
            }

            if (currentCatalogId != SongbookService.PARAM_INVALID_SBID) {

                indeces = mSongbookService.retrieveIndeces(currentCatalogId, isArtistChecked());

                if (indeces.length > 0) {
                    resultCursor = mSongbookService.findSongsByIndex(currentCatalogId, indeces[0], isArtistChecked());
                }

            }
            Log.d(TAG, "doInBackground - finished");

            return null;
        }

        protected void onPostExecute(Void ignore) {

            arrAdapter.clear();

            if (indeces.length > 0) {
                arrAdapter.addAll(indeces);
            }

            arrAdapter.notifyDataSetChanged();

            ResultFragment resultFragment = (ResultFragment) getSupportFragmentManager().findFragmentById(R.id.contentFragment).getChildFragmentManager().findFragmentById(R.id.resultFragment);

            SimpleCursorAdapter cursorAdapter = (SimpleCursorAdapter) resultFragment.getListAdapter();

            cursorAdapter.changeCursor(resultCursor);
            cursorAdapter.notifyDataSetChanged();

            if (currentCatalogId != SongbookService.PARAM_INVALID_SBID) {
                getActionBar().setTitle(getDisplayedTitle());
            }

            progressDialog.dismiss();

            Log.d(TAG, "doInBackground - onPostExecute");

        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPreExecute() {

            // setFragmentVisibility(false);

            progressDialog = new ProgressDialog(SongbookViewerActivity.this);
            progressDialog.setMessage(displayLoadingMessage ? getString(R.string.loading_message) : getString(R.string.initializing_message));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();

            doBindService();

            Intent serviceIntent = new Intent(getApplicationContext(), SongbookService.class);

            getApplicationContext().startService(serviceIntent);

            ListFragment listFrag = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.bookListFragment);

            if (listFrag.getListAdapter() == null) {
                arrAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.one_line_list_item, R.id.text1);
                listFrag.setListAdapter(arrAdapter);
            } else {
                arrAdapter = (ArrayAdapter<String>) listFrag.getListAdapter();
            }

        }

    }

    class ByArtistToggleTask extends AsyncTask<Void, Void, Void> {

        private final String TAG = ByArtistToggleTask.class.getCanonicalName();

        private ProgressDialog progressDialog;

        private ArrayAdapter<String> arrAdapter;

        private String[] indeces = new String[0];

        protected Void doInBackground(Void... ignoreParam) {

            Log.d(TAG, "doInBackground - starting");

            if (currentCatalogId != SongbookService.PARAM_INVALID_SBID) {
                indeces = mSongbookService.retrieveIndeces(currentCatalogId, isArtistChecked());
            }

            Log.d(TAG, "doInBackground - finished");

            return null;
        }

        protected void onPostExecute(Void ignore) {

            ListFragment listFrag = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.bookListFragment);

            arrAdapter.clear();

            if (indeces.length > 0) {
                arrAdapter.addAll(indeces);
            }

            arrAdapter.notifyDataSetChanged();

            listFrag.getListView().smoothScrollToPosition(0);

            progressDialog.dismiss();

            Log.d(TAG, "doInBackground - onPostExecute");

        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPreExecute() {

            progressDialog = new ProgressDialog(SongbookViewerActivity.this);
            progressDialog.setMessage(getString(R.string.loading_message));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();

            ListFragment listFrag = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.bookListFragment);

            if (listFrag.getListAdapter() == null) {
                arrAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.one_line_list_item, R.id.text1);
                listFrag.setListAdapter(arrAdapter);
            } else {
                arrAdapter = (ArrayAdapter<String>) listFrag.getListAdapter();
            }

        }

    }

    class SelectIndexTask extends AsyncTask<String, Void, Void> {

        private final String TAG = SelectIndexTask.class.getCanonicalName();

        private ProgressDialog progressDialog;

        private Cursor resultCursor;

        protected Void doInBackground(String... param) {

            Log.d(TAG, "doInBackground - starting");

            resultCursor = mSongbookService.findSongsByIndex(currentCatalogId, param[0], isArtistChecked());

            Log.d(TAG, "doInBackground - finished");

            return null;
        }

        protected void onPostExecute(Void ignore) {

            ResultFragment resultFragment = (ResultFragment) getSupportFragmentManager().findFragmentById(R.id.contentFragment).getChildFragmentManager().findFragmentById(R.id.resultFragment);

            SimpleCursorAdapter cursorAdapter = (SimpleCursorAdapter) resultFragment.getListAdapter();

            cursorAdapter.changeCursor(resultCursor);
            cursorAdapter.notifyDataSetChanged();

            progressDialog.dismiss();

            Log.d(TAG, "doInBackground - onPostExecute");

        }

        @Override
        protected void onPreExecute() {

            progressDialog = new ProgressDialog(SongbookViewerActivity.this);
            progressDialog.setMessage(getString(R.string.loading_message));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();

        }

    }

    class SearchTask extends AsyncTask<String, Void, Void> {

        private final String TAG = SearchTask.class.getCanonicalName();

        private ProgressDialog progressDialog;

        private Cursor resultCursor;

        protected Void doInBackground(String... param) {

            Log.d(TAG, "doInBackground - starting");

            resultCursor = mSongbookService.findSongsByKeyWord(currentCatalogId, param[0], isArtistChecked());

            Log.d(TAG, "doInBackground - finished");

            return null;
        }

        protected void onPostExecute(Void ignore) {

            ResultFragment resultFragment = (ResultFragment) getSupportFragmentManager().findFragmentById(R.id.contentFragment).getChildFragmentManager().findFragmentById(R.id.resultFragment);

            SimpleCursorAdapter cursorAdapter = (SimpleCursorAdapter) resultFragment.getListAdapter();

            cursorAdapter.changeCursor(resultCursor);
            cursorAdapter.notifyDataSetChanged();

            progressDialog.dismiss();

            Log.d(TAG, "doInBackground - onPostExecute");

        }

        @Override
        protected void onPreExecute() {

            progressDialog = new ProgressDialog(SongbookViewerActivity.this);
            progressDialog.setMessage(getString(R.string.searching_message));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();

        }

    }
}