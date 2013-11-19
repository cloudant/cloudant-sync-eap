package com.cloudant.todo;

import com.cloudant.sync.util.Document;

public class Task extends Document {
	
	public Task() {}
	
	static final String DOC_TYPE = "com.cloudant.todo.task";
	private String type = DOC_TYPE;
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	private boolean completed;
	public boolean isCompleted() {
		return this.completed;
	}
	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
	
	private String description;
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String desc) {
		this.description = desc;
	}
	
	@Override
	public String toString() {
		return "{ desc: " + getDescription() + ", completed: " + isCompleted() + "}";
	} 
}
