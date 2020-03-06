package songbook.viewer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.lamerman.FileDialog;
import com.lamerman.SelectionMode;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import songbook.viewer.data.SQLHelper;
import songbook.viewer.intent.action.SELECT_SONGBOOK;
import songbook.viewer.services.SongbookService;

public class SongbookManageActivity extends Activity {

	private class DeleteTask extends AsyncTask<Long, Void, Long> {

		private final String TAG = DeleteTask.class.getCanonicalName();

		private ProgressDialog progressDialog;

		private String errorMessage = null;

		protected Long doInBackground(Long... params) {

			Log.d(TAG, "doInBackground - starting");

			try {

				mSongbookService.deleteSongbook(params[0]);

			} catch (IllegalArgumentException error) {
				errorMessage = error.getMessage();
			}

			Log.d(TAG, "doInBackground - finished");

			return params[0];
		}

		protected void onPostExecute(Long songBookId) {

			loadSBList();

			progressDialog.dismiss();

			String message = null;

			if (errorMessage != null) {

				Log.e(TAG, errorMessage);

				message = getString(R.string.managesb_task_delete_failure);

			} else {
				message = getString(R.string.managesb_task_delete_succcess);
			}

			// add ID to the delete list RESULT

			resultIntent.putExtra(RESULT_COMMAND, CONTEXT_DELETE);

			long[] deleteSet = resultIntent
					.getLongArrayExtra(RESULT_DELETELIST);

			Set<Long> newSet = new HashSet<Long>();

			if (deleteSet != null) {

				for (long value : deleteSet) {
					newSet.add(value);
				}

			}

			newSet.add(songBookId);

			// Ugly way to Copy

			deleteSet = new long[newSet.size()];

			int i = 0;

			for (Iterator<Long> iterator = newSet.iterator(); iterator
					.hasNext(); i++) {

				deleteSet[i] = iterator.next();

			}

			resultIntent.putExtra(RESULT_DELETELIST, deleteSet);

			// display message

			Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT)
					.show();

			Log.d(TAG, "doInBackground - onPostExecute");

		}

		@Override
		protected void onPreExecute() {

			progressDialog = new ProgressDialog(SongbookManageActivity.this);
			progressDialog.setMessage(getString(R.string.deleting_message));
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.show();

		}
	}

	class ImportParameter {

		private String filePath = null;
		private String name = null;
		private String description = null;
		private boolean defaultFlag = false;

		public String getDescription() {
			return description;
		}

		public String getFilePath() {
			return filePath;
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

		public ImportParameter setFilePath(String filePath) {
			this.filePath = filePath;
			return this;
		}

		public ImportParameter setName(String name) {
			this.name = name;
			return this;
		}

	}

	private class ImportTask extends AsyncTask<ImportParameter, Void, Void> {

		private final String TAG = ImportTask.class.getCanonicalName();

		private ProgressDialog progressDialog;

		private String errorMessage = null;

		protected Void doInBackground(ImportParameter... params) {

			Log.d(TAG, "doInBackground - starting");

			try {

				mSongbookService.importSongbook(
						new File(params[0].getFilePath()), params[0].getName(),
						params[0].getDescription(), params[0].isDefaultFlag());

			} catch (IllegalArgumentException error) {
				errorMessage = error.getMessage();
			}

			Log.d(TAG, "doInBackground - finished");

			return null;
		}

		protected void onPostExecute(Void ignore) {

			loadSBList();

			progressDialog.dismiss();

			if (errorMessage != null) {

				Log.e(TAG, errorMessage);

			}

			viewFlipper = (ViewFlipper) findViewById(R.id.manageSBViewFlipper);
			viewFlipper.showPrevious();
			filePath = null;

			Log.d(TAG, "doInBackground - onPostExecute");

		}

		@Override
		protected void onPreExecute() {

			progressDialog = new ProgressDialog(SongbookManageActivity.this);
			progressDialog.setMessage(getString(R.string.importing_message));
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.show();

		}
	}

	private class SetDefaultTask extends AsyncTask<Long, Void, Void> {

		private final String TAG = SetDefaultTask.class.getCanonicalName();

		private ProgressDialog progressDialog;

		private String errorMessage = null;

		protected Void doInBackground(Long... params) {

			Log.d(TAG, "doInBackground - starting");

			try {
				mSongbookService.setAsDefault(params[0]);
			} catch (IllegalArgumentException error) {
				errorMessage = error.getMessage();
			}

			Log.d(TAG, "doInBackground - finished");

			return null;
		}

		protected void onPostExecute(Void ignore) {

			progressDialog.dismiss();

			String message = null;

			if (errorMessage != null) {

				Log.e(TAG, errorMessage);

				message = getString(R.string.managesb_task_setdefault_failure);

			} else {
				message = getString(R.string.managesb_task_setdefault_succcess);
			}

			Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT)
					.show();

			Log.d(TAG, "doInBackground - onPostExecute");

		}

		@Override
		protected void onPreExecute() {

			progressDialog = new ProgressDialog(SongbookManageActivity.this);
			progressDialog.setMessage(getString(R.string.processing_message));
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.show();

		}
	}

	public final static int CONTEXT_DELETE = 1;

	public final static int CONTEXT_SET_DEFAULT = 2;

	public final static int CONTEXT_LOAD = 3;

	public final static String RESULT_COMMAND = "resultCommand";

	public final static String RESULT_DELETELIST = "resultDeleteList";

	public final static String RESULT_LOADSBID = "resultLoadSBId";

	private final String TAG = SongbookManageActivity.class.getCanonicalName();

	private ViewFlipper viewFlipper;

	private String filePath;

	private SongbookService mSongbookService;

	private ListView listView;

	private final Intent resultIntent = new Intent("");

	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			mSongbookService = ((SongbookService.SongbookBinder) service)
					.getService();

			loadSBList();

		}

		public void onServiceDisconnected(ComponentName className) {
			// never called
		}

	};

	private void loadSBList() {

		// Initialize ListView

		SimpleCursorAdapter cursorAdapter = null;

		if (listView.getAdapter() == null) {

			cursorAdapter = new SimpleCursorAdapter(this,
					R.layout.songbook_list_item, null, new String[] {
							SQLHelper.tblSongbook_ID,
							SQLHelper.tblSongbook_NAME,
							SQLHelper.tblSongbook_DESC }, new int[] {
							R.id.sbListItem_id, R.id.sbListItem_name,
							R.id.sbListItem_desc }, 0);

			listView.setAdapter(cursorAdapter);

		}

		cursorAdapter = (SimpleCursorAdapter) listView.getAdapter();

		cursorAdapter
				.changeCursor(mSongbookService.retrieveSongBooksByCursor());

		cursorAdapter.notifyDataSetChanged();

	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		Log.d(TAG, "onActivityResult");

		if (data != null) {

			filePath = data.getStringExtra(FileDialog.RESULT_PATH);

		}

	}

	public boolean onContextItemSelected(MenuItem item) {

		Log.d(TAG, "onContextItemSelected");

		AdapterView.AdapterContextMenuInfo menuInfo = null;

		switch (item.getItemId()) {

		case CONTEXT_DELETE:

			menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

			Log.d(TAG, "list pos:" + menuInfo.position + " id:" + menuInfo.id);

			new DeleteTask().execute(menuInfo.id);

			break;

		case CONTEXT_SET_DEFAULT:

			menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

			Log.d(TAG, "list pos:" + menuInfo.position + " id:" + menuInfo.id);

			new SetDefaultTask().execute(menuInfo.id);

			break;

		case CONTEXT_LOAD:

			menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

			Log.d(TAG, "list pos:" + menuInfo.position + " id:" + menuInfo.id);

			resultIntent.putExtra(RESULT_COMMAND, CONTEXT_LOAD);
			resultIntent.putExtra(RESULT_LOADSBID, menuInfo.id);

			finish();

			break;

		default:

			Log.d(TAG, "Default Context chosen");

			return super.onContextItemSelected(item);

		}

		return true;

	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.manage_songbook);

		viewFlipper = (ViewFlipper) findViewById(R.id.manageSBViewFlipper);

		listView = (ListView) findViewById(R.id.listViewSongbooks);

		// Initialize Add Button

		final Button addButton = (Button) findViewById(R.id.buttonAddSongbook);

		addButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				viewFlipper.showNext();
			}
		});

		// Initialize OK Button

		final Button okButton = (Button) findViewById(R.id.buttonOKSongbook);

		okButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				finish();
			}
		});

		// Initialize Cancel Button

		final Button cancelButton = (Button) findViewById(R.id.buttonCancelAddSongbook);

		cancelButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				viewFlipper.showPrevious();
			}
		});

		// Initialize Import Button

		final Button importButton = (Button) findViewById(R.id.buttonImportSongbook);

		importButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {

				String nameValue = ((EditText) findViewById(R.id.importNameText))
						.getText().toString();

				if (filePath != null && nameValue != null
						&& nameValue.trim().length() > 0) {

					new ImportTask()
							.execute(new ImportParameter()
									.setFilePath(filePath)
									.setName(nameValue)
									.setDescription(
											((EditText) findViewById(R.id.importDescText))
													.getText().toString()));
				} else {
					Toast.makeText(getApplicationContext(),
							getString(R.string.managesb_import_required),
							Toast.LENGTH_SHORT).show();
				}
			}

		});

		// Initialize Choose Button

		final Button chooseButton = (Button) findViewById(R.id.buttonSelectFile);

		chooseButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {

				Intent intent = new SELECT_SONGBOOK();

				intent.putExtra(FileDialog.START_PATH, Environment
						.getExternalStorageDirectory().getAbsolutePath());

				intent.putExtra(FileDialog.SELECTION_MODE,
						SelectionMode.MODE_OPEN);

				startActivityForResult(intent, 1);

			}
		});

		filePath = null;

		registerForContextMenu(listView);

		bindService(new Intent(SongbookManageActivity.this,
				SongbookService.class), mConnection, Context.BIND_AUTO_CREATE);

		Log.d(TAG, "onCreate");

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

		menu.setHeaderTitle(R.string.managesb_cmenu_header);
		menu.add(0, CONTEXT_DELETE, 0, R.string.managesb_cmenu_delete);
		menu.add(0, CONTEXT_SET_DEFAULT, 0, R.string.managesb_cmenu_setdefault);
		menu.add(0, CONTEXT_LOAD, 0, R.string.managesb_cmenu_load);
	}

	@Override
	public void finish() {

		Log.d(TAG, "onFinish");

		setResult(Activity.RESULT_OK, resultIntent);

		super.finish();

	}

	@Override
	protected void onDestroy() {

		Log.d(TAG, "onDestroy");

		super.onDestroy();

		unbindService(mConnection);
	}

}
