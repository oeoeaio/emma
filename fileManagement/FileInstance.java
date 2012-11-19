package fileManagement;

public class FileInstance{

	String fileName;
	Long size;
	Long modDate;
	
	FileInstance(String fileName,long size,long modDate){
		this.fileName = fileName;
		this.size = size;
		this.modDate = modDate;
	}
	
	@Override
	public boolean equals(Object object){
		try{
			FileInstance file = (FileInstance)object;
			if (this.fileName.equals(file.fileName)){
				if (this.size.equals(file.size)){
					return (this.modDate.equals(file.modDate));
				}
				else{
					return false;
				}
			}
			else{
				return false;
			}
		}catch(ClassCastException ccE){ //if object is not a file instance
			return false;
		}
	}
}
