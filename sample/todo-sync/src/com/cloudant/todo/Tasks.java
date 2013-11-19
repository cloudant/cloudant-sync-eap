package com.cloudant.todo;

import java.util.ArrayList;
import java.util.List;

import com.cloudant.sync.datastore.DBObject;
import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreExtended;
import com.cloudant.sync.util.JSONUtils;
import com.cloudant.sync.util.TypedDatastore;

public class Tasks extends TypedDatastore<Task> {
	
	private Datastore datastore;

	public Tasks(Datastore datastore) {
		super(Task.class, (DatastoreExtended)datastore);
		this.datastore = datastore;
	}

	/**
	 * Return all the {@code Task} 
	 */
	public List<Task> allDocuments() {
		List<DBObject> all = this.datastore.getAllDocuments(0, 100, true);
		List<Task> tasks = new ArrayList<Task>();
 		for(DBObject obj : all) {
 			if(obj.asMap().containsKey("type") 
 					&& obj.asMap().get("type").equals(Task.DOC_TYPE)) {
 	            tasks.add(deserializTask(obj));
 			}
 		}
 		return tasks;
	}

	// Should be moved into the Library
	private Task deserializTask(DBObject obj) {
		Task t = JSONUtils.deserialize(obj.asBytes(), Task.class);
		t.setId(obj.getId());
		t.setRevision(obj.getRevision());
		return t;
	}
}
