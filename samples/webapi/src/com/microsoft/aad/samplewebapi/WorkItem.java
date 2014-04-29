package com.microsoft.aad.samplewebapi;

import java.util.Date;

/**
 * Represents an item in a ToDo list
 */
public class WorkItem {

	/**
	 * Item title
	 */
	@com.google.gson.annotations.SerializedName("Title")
	private String mTitle;

	/**
	 * Item Id
	 */
	@com.google.gson.annotations.SerializedName("Id")
	private int mId;

	/**
	 * Indicates if the item is completed
	 */
	@com.google.gson.annotations.SerializedName("Complete")
	private boolean mComplete;

	/**
	 * Indicates if the item is completed
	 */
	@com.google.gson.annotations.SerializedName("DueDate")
	private Date mDue;

	/**
	 * WorkItem constructor
	 */
	public WorkItem() {

	}

	/**
	 * Initializes a new WorkItem
	 * 
	 * @param text
	 *            The item text
	 * @param id
	 *            The item id
	 */
	public WorkItem(String text, int id) {
		this.setTitle(text);
		this.setId(id);
	}

	@Override
	public String toString() {
		return getTitle();
	}

	/**
	 * Returns the item text
	 */
	public String getTitle() {
		return mTitle;
	}

	/**
	 * Sets the item text
	 * 
	 * @param text
	 *            text to set
	 */
	public final void setTitle(String text) {
		mTitle = text;
	}

	/**
	 * Returns the item id
	 */
	public int getId() {
		return mId;
	}

	/**
	 * Sets the item id
	 * 
	 * @param id
	 *            id to set
	 */
	public final void setId(int id) {
		mId = id;
	}

	/**
	 * Indicates if the item is marked as completed
	 */
	public boolean isComplete() {
		return mComplete;
	}

	/**
	 * Marks the item as completed or incompleted
	 */
	public void setComplete(boolean complete) {
		mComplete = complete;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof WorkItem && ((WorkItem) o).mId == mId;
	}

	public Date getDueDate() {
		return mDue;
	}

	public void setDueDate(Date mDue) {
		this.mDue = mDue;
	}
}
