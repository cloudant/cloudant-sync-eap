package com.cloudant.todo;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreManager;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorFactory;
import com.google.common.base.Strings;

public class TodoActivity extends ListActivity {
	
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
	
	public void initDatastore()  {
		File path = getApplicationContext().getDir(DATASTORE_MANGER_DIR, Context.MODE_PRIVATE);
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
}
