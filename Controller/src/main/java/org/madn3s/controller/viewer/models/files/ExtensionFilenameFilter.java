package org.madn3s.controller.viewer.models.files;

import java.io.File;
import java.io.FilenameFilter;

public class ExtensionFilenameFilter implements FilenameFilter {
	private String[] mExtensions;
	
	public ExtensionFilenameFilter(String[] extensions) {
		super();
		mExtensions = extensions;
	}
	
	@Override
	public boolean accept(File dir, String filename) {
		if(new File(dir, filename).isDirectory()) {
			// Accept all directory names
			return true;
		}
		if(mExtensions != null && mExtensions.length > 0) {
			for(int i = 0; i < mExtensions.length; i++) {
				if(filename.endsWith(mExtensions[i])) {
					// The filename ends with the extension
					return true;
				}
			}
			// The filename did not match any of the extensions
			return false;
		}
		// No extensions has been set. Accept all file extensions.
		return true;
	}
}
