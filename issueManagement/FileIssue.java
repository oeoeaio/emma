package issueManagement;

import java.io.File;

public class FileIssue {

	private final File file;
	private final String issueType;
	private final String notes;
	
	
	public FileIssue(File file,String issueType,String notes){
		this.file = file;
		this.issueType = issueType;
		this.notes = notes;
	}
	
	public File getFile(){
		return file;
	}
	
	public String getIssueType(){
		return issueType;
	}
	
	public String getNotes(){
		return notes;
	}
}
