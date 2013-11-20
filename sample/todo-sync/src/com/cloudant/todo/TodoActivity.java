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
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreManager;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorFactory;
import com.google.common.base.Strings;

public class TodoActivity extends ListActivity {
	
	private static final int DIALOG_NEW_TASK = 1;

	static final String LOG_TAG = "TodoActivity";
	
    static final String CLOUDANT_HOST = "demomobile2012.cloudant.com";
    static final String CLOUDANT_DB = "example_app_todo";
    static final String CLOUDANT_API_KEY = "demomobile2012";
    public static final String CLOUDANT_API_SECRET = "b5GP6soyxkCw";
    
    static final String DATASTORE_MANGER_DIR = "data";
    static final String TASKS_DATASTORE_NAME = "tasks";
	
    private Tasks mTasks; 
    private TaskAdapter mTaskAdapter;
    private Replicator mPushReplicator;
    private Replicator mPullReplicator;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_todo);
		this.initDatastore();
		
		List<Task> tasks = this.mTasks.allDocuments();
		Log.d(LOG_TAG, tasks.toString());
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
	        case R.id.action_settings:
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		if(id == DIALOG_NEW_TASK) {
		    return createNewTaskDialog();
		} else {
			throw new RuntimeException("No dialog defined for id: " + id);
		}
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
									Toast toast = Toast
											.makeText(getApplicationContext(),
													R.string.task_not_created,
													Toast.LENGTH_LONG);
									toast.show();
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
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						b.setEnabled(description.getText().length() > 0);
					}

					@Override
					public void beforeTextChanged(CharSequence s, int start, int count,
							int after) {
					}

					@Override
					public void afterTextChanged(Editable s) {
					}
				});
			}
		});
		
		return d;
	}

	public void initDatastore()  {
		File path = getApplicationContext().getDir(DATASTORE_MANGER_DIR, 
				Context.MODE_PRIVATE);
		DatastoreManager manager = new DatastoreManager(path.getAbsolutePath());
		Datastore ds = manager.openDatastore(TASKS_DATASTORE_NAME);
		
		try {
			URI uri = this.createServerURI();
			mPushReplicator = ReplicatorFactory.oneway(ds, uri);
			mPullReplicator = ReplicatorFactory.oneway(uri, ds);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		} 
		
		mTasks = new Tasks(ds);
		
		List<Task> allTasks = mTasks.allDocuments();
		Log.d(LOG_TAG, allTasks.toString());
        
        mPushReplicator.start();
        mPullReplicator.start();
	}

	private URI createServerURI() throws URISyntaxException {
		return new URI("https", 
				CLOUDANT_API_KEY + ":" + CLOUDANT_API_SECRET, 
				CLOUDANT_HOST, 
				443,
				"/" + CLOUDANT_DB, 
				null, 
				null);
	}
	
	public void createNewTask(String desc) {
		Task t = new Task(desc);
		mTaskAdapter.add(mTasks.createDocument(t));
	}
}
