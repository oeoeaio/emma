package fileManagement;

import java.io.File;
import java.io.FileFilter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import endUseWindow.LogWindow;
import endUseWindow.MySQLConnection;



public class FileFetcher {
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length==5){
			File testFolder = new File(args[0]);
			String dbName = args[1];
			String username = args[2];
			String pass = args[3];
			boolean needMiniClampFix = (args[4].equals("true"));
			if (testFolder.isDirectory()){
				new FileFetcher(testFolder,"localhost","3306",dbName,username,pass.toCharArray(),needMiniClampFix);
			}
			else{
				System.out.println("Invalid folder location, please enter another");
			}
		}
		else{
			System.out.println("Invalid agruments");
			System.out.println("Please specify the full path of the folder to be checked, the name of the database, the username and password");
			System.out.println("ie. java Auto_PDC_Processor.jar [folder path] [dbname] [username] [password]");
		}
	}
	
	FileFetcher(File folderToProcess,String address,String port, String dbName, String user,char[] pass,boolean needMiniClampFix){
		try{			
			MySQLConnection mySQLConnection = new MySQLConnection();
			mySQLConnection.ipAddress = address;
			mySQLConnection.portNumber = port;
			mySQLConnection.dbName = dbName;
			mySQLConnection.userName = user;
			mySQLConnection.pWord = pass;
			
			mySQLConnection.dbConn = MySQLConnection.getConnection(address, port, dbName, user, pass);

			Connection dbConn = mySQLConnection.dbConn;
			
			ResultSet brokenFileRS = dbConn.createStatement().executeQuery("SELECT file_name,file_size,UNIX_TIMESTAMP(date_modified) AS date_modified_ts FROM issues_files WHERE folder_name = '"+folderToProcess.getName()+"' AND attempts > 3 ORDER BY file_name ASC,file_size ASC,date_modified_ts ASC");

			final ArrayList<FileInstance> brokenFiles = new ArrayList<FileInstance>();
			while(brokenFileRS.next()){
				brokenFiles.add(new FileInstance(brokenFileRS.getString("file_name"),brokenFileRS.getLong("file_size"),brokenFileRS.getLong("date_modified_ts")*1000));
			}
			brokenFileRS.close();
			
			
			ResultSet filesInDatabaseRS = dbConn.createStatement().executeQuery("SELECT file_name,file_size,UNIX_TIMESTAMP(date_modified) AS date_modified_ts FROM files WHERE folder_name = '"+folderToProcess.getName()+"' ORDER BY file_name ASC,file_size ASC,date_modified_ts ASC");
			
			final ArrayList<FileInstance> filesInDatabase = new ArrayList<FileInstance>();
			while(filesInDatabaseRS.next()){
				filesInDatabase.add(new FileInstance(filesInDatabaseRS.getString("file_name"),filesInDatabaseRS.getLong("file_size"),filesInDatabaseRS.getLong("date_modified_ts")*1000));
			}	
			filesInDatabaseRS.close();
						
			FileFilter fileFilter = new FileFilter() {
				@Override
				public boolean accept(File arg0) {
					FileInstance testFile = new FileInstance(arg0.getName(),arg0.length(),(long) (Math.floor(arg0.lastModified()/1000)*1000));
					if (filesInDatabase.contains(testFile)){
						return false;
					}
					else{
						if (brokenFiles.contains(testFile)){ //files which have been attempted more than three times but have a fatal file error
							return false;
						}
						else{
							return true;
						}
					}
				}
			};
			
			File[] filesInFolder = folderToProcess.listFiles(fileFilter);
			ArrayList<File> fileList = new ArrayList<File>();
			
			for (int i=0;i<filesInFolder.length;i++){
				fileList.add(filesInFolder[i]);
			}
			
			System.out.println("Found "+fileList.size()+" valid files.");
			
			File logFolder = new File("./PDCWriteLogs");
			if (logFolder.isDirectory() || logFolder.mkdir()){//if no PDCWriteLogs directory in the current folder, attempt to create one. Continue if successful.
				//SEND FILES TO VALIDATOR
				SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
				String now = dateFormatter.format(new Date().getTime());
				PDCValidator pdcValidator = new PDCValidator(fileList,mySQLConnection,new LogWindow(new File("./PDCWriteLogs/"+folderToProcess.getName()+"-"+now+".txt")),false,true,false,needMiniClampFix);
				new Thread(pdcValidator).start();
			}
			else{
				System.out.println("Failed to locate/create log file directory, aborting...\r\n");
			}

		} catch(SQLException sE){
			System.out.println("Error connecting to DB using specified information.\r\n");
			sE.printStackTrace();
		}
	}

}
