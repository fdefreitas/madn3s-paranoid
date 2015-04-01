package org.madn3s.controller.viewer.models.files;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import org.madn3s.controller.MADN3SController;
import org.madn3s.controller.R;
import org.madn3s.controller.viewer.opengl.ModelDisplayActivity;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

@SuppressWarnings("unused")
public class ModelPickerActivity extends ListActivity {
	
	public static final String tag = "ModelPickerFragment";
	/**
	 * The file path
	 */
	public final static String EXTRA_FILE_PATH = "file_path";
	
	/**
	 * Sets whether hidden files should be visible in the list or not
	 */
	public final static String EXTRA_SHOW_HIDDEN_FILES = "show_hidden_files";

	/**
	 * The allowed file extensions in an ArrayList of Strings
	 */
	public final static String EXTRA_ACCEPTED_FILE_EXTENSIONS = "accepted_file_extensions";
	
	/**
	 * The initial directory which will be used if no directory has been sent with the intent 
	 */
	private final static String DEFAULT_INITIAL_DIRECTORY = "/";
	
	protected File mDirectory;
	protected ArrayList<File> mFiles;
	protected FilePickerListAdapter mAdapter;
	protected boolean mShowHiddenFiles = false;
	protected String[] acceptedFileExtensions;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Set the view to be shown if the list is empty
		LayoutInflater inflator = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View emptyView = inflator.inflate(R.layout.file_picker_empty_view, null);
		((ViewGroup)getListView().getParent()).addView(emptyView);
		getListView().setEmptyView(emptyView);
		
		// Set initial directory
		mDirectory = new File(DEFAULT_INITIAL_DIRECTORY);
		
		// Initialize the ArrayList
		mFiles = new ArrayList<File>();
		
		// Set the ListAdapter
		mAdapter = new FilePickerListAdapter(this, mFiles);
		setListAdapter(mAdapter);
		
		// Initialize the extensions array to allow any file extensions
		acceptedFileExtensions = new String[] {};
		
		// Get intent extras
		if(getIntent().hasExtra(EXTRA_FILE_PATH)) {
			mDirectory = new File(getIntent().getStringExtra(EXTRA_FILE_PATH));
		}
		if(getIntent().hasExtra(EXTRA_SHOW_HIDDEN_FILES)) {
			mShowHiddenFiles = getIntent().getBooleanExtra(EXTRA_SHOW_HIDDEN_FILES, false);
		}
		if(getIntent().hasExtra(EXTRA_ACCEPTED_FILE_EXTENSIONS)) {
			ArrayList<String> collection = getIntent().getStringArrayListExtra(EXTRA_ACCEPTED_FILE_EXTENSIONS);
			acceptedFileExtensions = (String[]) collection.toArray(new String[collection.size()]);
		}
	}
	
	@Override
	protected void onResume() {
		refreshFilesList();
		super.onResume();
	}
	
	/**
	 * Updates the list view to the current directory
	 */
	protected void refreshFilesList() {
		// Clear the files ArrayList
		mFiles.clear();
		
		// Set the extension file filter
		ExtensionFilenameFilter filter = new ExtensionFilenameFilter(acceptedFileExtensions);
		
		// Get the files in the directory
		File[] files = mDirectory.listFiles(filter);
		if(files != null && files.length > 0) {
			for(File f : files) {
				if(f.isHidden() && !mShowHiddenFiles) {
					// Don't add the file
					continue;
				}
				
				// Add the file the ArrayAdapter
				mFiles.add(f);
			}
			
			Collections.sort(mFiles, new FileComparator());
		}
		mAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void onBackPressed() {
		if(mDirectory.getParentFile() != null) {
			// Go to parent directory
			mDirectory = mDirectory.getParentFile();
			refreshFilesList();
			return;
		}
		
		super.onBackPressed();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		File newFile = (File)l.getItemAtPosition(position);
		
		if(newFile.isFile()) {
			// Set result
			Intent extra = new Intent();
			extra.putExtra(EXTRA_FILE_PATH, newFile.getAbsolutePath());
			setResult(RESULT_OK, extra);
			// Finish the activity
			finish();
			Intent intent = new Intent(getBaseContext(), ModelDisplayActivity.class);
			intent.putExtra(MADN3SController.MODEL_MESSAGE, newFile.getAbsolutePath());
			startActivity(intent);
		} else {
			mDirectory = newFile;
			// Update the files list
			refreshFilesList();
		}
		
		super.onListItemClick(l, v, position, id);
	}

}
