package com.cloudant.todo;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.cloudant.sync.datastore.ConflictException;
import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreManager;
import com.cloudant.sync.replication.ErrorInfo;
import com.cloudant.sync.replication.ReplicationListener;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorFactory;

public class TodoActivity extends ListActivity 
	implements ReplicationListener, OnSharedPreferenceChangeListener {

	private static final int DIALOG_PROGRESS = 2;

	private static final int DIALOG_NEW_TASK = 1;

	static final String LOG_TAG = "TodoActivity";

	static final String SETTINGS_CLOUDANT_USER = "pref_key_username";
	static final String SETTINGS_CLOUDANT_DB = "pref_key_dbname";
	static final String SETTINGS_CLOUDANT_API_KEY = "pref_key_api_key";
	static final String SETTINGS_CLOUDANT_API_SECRET = "pref_key_api_secret";

	static final String DATASTORE_MANGER_DIR = "data";
	static final String TASKS_DATASTORE_NAME = "tasks";

	private Tasks mTasks;
	private TaskAdapter mTaskAdapter;
	private Replicator mPushReplicator;
	private Replicator mPullReplicator;
	private Handler mHandler;
	
	DatastoreManager mManager;
	Datastore mDatastore;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_todo);
		
		// set the default settings
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		// register to listen to the setting changes because replicators
		// uses information managed by shared preference
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		sharedPref.registerOnSharedPreferenceChangeListener(this);
		
		this.initDatastore();
		this.initReplicators();
		
		this.mHandler = new Handler(Looper.getMainLooper());

		List<Task> tasks = this.mTasks.allDocuments();
		this.mTaskAdapter = new TaskAdapter(this, tasks);
		this.setListAdapter(this.mTaskAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.todo, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.action_new:
			this.showDialog(DIALOG_NEW_TASK);
			return true;
		case R.id.action_download:
			download();
			return true;
		case R.id.action_upload:
			upload();
			return true;
		case R.id.action_settings:
			this.showSettings();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	void showSettings() {
		this.startActivity(new Intent().setClass(this, SettingsActivity.class));
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		if (id == DIALOG_NEW_TASK) {
			return createNewTaskDialog();
		} else if (id == DIALOG_PROGRESS) {
			return createProgressDialog();
		} else {
			throw new RuntimeException("No dialog defined for id: " + id);
		}
	}

	public Dialog createProgressDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = this.getLayoutInflater();
		final View v = inflater.inflate(R.layout.dialog_loading, null);
		builder.setView(v).setNegativeButton("Stop",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						stopReplication();
					}
				})
				.setOnKeyListener(new DialogInterface.OnKeyListener() {
					@Override
					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
						if(keyCode == KeyEvent.KEYCODE_BACK) {
							Toast.makeText(getApplicationContext(),
									R.string.replication_running, Toast.LENGTH_LONG).show();
							return true;
						}
						return false;
					}
				});

		return builder.create();
	}

	public Dialog createNewTaskDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = this.getLayoutInflater();

		final View v = inflater.inflate(R.layout.dialog_new_task, null);
		final EditText description = (EditText) v
				.findViewById(R.id.new_task_desc);

		builder.setView(v)
				.setTitle(R.string.new_task)
				.setPositiveButton(R.string.create,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								if (description.getText().length() > 0) {
									createNewTask(description.getText()
											.toString());
									description.getText().clear();
								} else {
									// Tell user the task is not created because
									// description is required
									Toast.makeText(getApplicationContext(),
											R.string.task_not_created,
											Toast.LENGTH_LONG).show();
								}
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
							}
						});

		final AlertDialog d = builder.create();
		d.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				final Button b = d.getButton(DialogInterface.BUTTON_POSITIVE);
				b.setEnabled(description.getText().length() > 0);

				description.addTextChangedListener(new TextWatcher() {
					@Override
					public void onTextChanged(CharSequence s, int start,
							int before, int count) {
						b.setEnabled(description.getText().length() > 0);
					}

					@Override
					public void beforeTextChanged(CharSequence s, int start,
							int count, int after) {
					}

					@Override
					public void afterTextChanged(Editable s) {
					}
				});
			}
		});

		return d;
	}

	public void initDatastore() {
		File path = getApplicationContext().getDir(DATASTORE_MANGER_DIR,
				Context.MODE_PRIVATE);
		this.mManager = new DatastoreManager(path.getAbsolutePath());
		this.mDatastore = mManager.openDatastore(TASKS_DATASTORE_NAME);
		this.mTasks = new Tasks(mDatastore);
	}
	
	public void initReplicators() {
		try {
			URI uri = this.createServerURI();
			Log.d(LOG_TAG, "uri:" + uri.toString());
			mPushReplicator = ReplicatorFactory.oneway(mDatastore, uri);
			mPullReplicator = ReplicatorFactory.oneway(uri, mDatastore);

			mPushReplicator.setListener(this);
			mPullReplicator.setListener(this);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private URI createServerURI() throws URISyntaxException {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String username = sharedPref.getString(SETTINGS_CLOUDANT_USER, "");
		String dbname = sharedPref.getString(SETTINGS_CLOUDANT_DB, "");
		String apiKey = sharedPref.getString(SETTINGS_CLOUDANT_API_KEY, "");
		String apiSecret = sharedPref.getString(SETTINGS_CLOUDANT_API_SECRET, "");
		String host = username + ".cloudant.com";
		
		return new URI("https", apiKey + ":" + apiSecret, host, 443, "/" + dbname, null, null);
	}

	public void createNewTask(String desc) {
		Task t = new Task(desc);
		mTaskAdapter.add(mTasks.createDocument(t));
	}

	@Override
	public void complete(Replicator replicator) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				replicationComplete();
			}
		});
	}

	@Override
	public void error(Replicator replicator, final ErrorInfo error) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				replicationError(error);
			}
		});
	}
	
	public void onCompleteCheckboxClicked(View view) {
		try {
			int position = view.getId();
			Task t = (Task) mTaskAdapter.getItem(position);
			t.setCompleted(!t.isCompleted());
			t = mTasks.updateDocument(t);
			mTaskAdapter.set(position, t);
		} catch (ConflictException e) {
			throw new RuntimeException(e);
		}
	}

	boolean isReplicatorRunning(Replicator replicator) {
		return replicator.getState() == Replicator.State.STARTED
				|| replicator.getState() == Replicator.State.STOPPING;
	}

	void replicationComplete() {
		Toast.makeText(getApplicationContext(), R.string.replication_completed,
				Toast.LENGTH_LONG).show();
		this.dismissDialog(DIALOG_PROGRESS);
		mTaskAdapter.notifyDataSetChanged();
	}

	void replicationError(ErrorInfo error) {
		Log.e(LOG_TAG, "Replication error:", error.getException());
		Toast.makeText(getApplicationContext(),
				R.string.replication_error, Toast.LENGTH_LONG)
				.show();
		this.dismissDialog(DIALOG_PROGRESS);
		mTaskAdapter.notifyDataSetChanged();
	}

	void stopReplication() {
		mPullReplicator.stop();
		mPushReplicator.stop();
		this.dismissDialog(DIALOG_PROGRESS);
		mTaskAdapter.notifyDataSetChanged();
	}

	void download() {
		this.showDialog(DIALOG_PROGRESS);
		mPullReplicator.start();
	}

	void upload() {
		this.showDialog(DIALOG_PROGRESS);
		mPushReplicator.start();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d(LOG_TAG, "onSharedPreferenceChanged()");
		this.initReplicators();
	}
}
