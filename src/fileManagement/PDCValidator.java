package fileManagement;


import issueManagement.FileIssue;

import java.awt.HeadlessException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;

import javax.swing.JOptionPane;

import endUseWindow.LogWindow;
import endUseWindow.MySQLConnection;
import endUseWindow.Site;
import endUseWindow.Source;

public class PDCValidator implements Runnable{
	

	public static class FileData {
		public final String[] currConcHeaders;
		public final ArrayList<String[]> currCTModHeaders;
		public final ArrayList<String[]> currWModHeaders;
		public final ArrayList<Long> currConcDateData;
		public final double[][] currConcPhaseData;
		public final double[][] currCTModData;
		public final double[][] currWModData;
		public final String currConcSN;
		public final boolean validDataExtracted;

		public FileData(String[] currConcHeaders,ArrayList<String[]> currCTModHeaders,ArrayList<String[]> currWModHeaders,ArrayList<Long> currConcDateData,double[][] currConcPhaseData,double[][] currCTModData,double[][] currWModData,String currConcSN,boolean validDataExtracted) {
			this.currConcHeaders = currConcHeaders;
			this.currCTModHeaders = currCTModHeaders;
			this.currWModHeaders = currWModHeaders;
			this.currConcDateData = currConcDateData;
			this.currConcPhaseData = currConcPhaseData;
			this.currCTModData = currCTModData;
			this.currWModData = currWModData;
			this.currConcSN = currConcSN;
			this.validDataExtracted = validDataExtracted;
		}
		
	}

	//variables
	private LogWindow logWindow;
	private Connection dbConn = null;
	private MySQLConnection mySQLConnection = null;
	private ArrayList<File> filesToProcess;
	private final boolean writeToFile;
	private final boolean writeToDatabase;
	private final boolean showGUI;
	private final boolean needMiniClampFix;
	private final boolean validateWChNames;
	
	private ArrayList<FileIssue> fileIssueList = new ArrayList<FileIssue>();
	private ArrayList<String[]> sourceIssueList = new ArrayList<String[]>();
	//private ArrayList<String> brokenSiteNameList = new ArrayList<String>(); // list of siteID associations for each file.
	private ArrayList<File> fileList = new ArrayList<File>(); //pointer object for the file selected by the user
	//private ArrayList<String[]> brokenFileList = new ArrayList<String[]>(); //pointer object for holding broken file info
	
	//private ArrayList<String[]> ALL_CONC_HEADERS = new ArrayList<String[]>();
	//private ArrayList<ArrayList<String[]>> ALL_W_MOD_HEADERS = new ArrayList<ArrayList<String[]>>();
	//private ArrayList<ArrayList<String[]>> ALL_CT_MOD_HEADERS = new ArrayList<ArrayList<String[]>>();
	//private ArrayList<ArrayList<Long>> ALL_CONC_DATE_DATA = new ArrayList<ArrayList<Long>>();
	//private ArrayList<double[][]> ALL_CONC_PHASE_DATA = new ArrayList<double[][]>();
	//private ArrayList<double[][]> ALL_CT_MOD_DATA = new ArrayList<double[][]>();
	//private ArrayList<double[][]> ALL_W_MOD_DATA = new ArrayList<double[][]>();
	
	
	private int[] concHeaderDelimiters = new int[2]; // Concentrator Header starts and lengths
	private ArrayList<int[]> ctModHeaderDelimiters = new ArrayList<int[]>(); // CT Module Header starts and lengths
	private ArrayList<int[]> wModHeaderDelimiters = new ArrayList<int[]>(); // Wireless Module Header starts and lengths
	
	private int[] concDataBlockDelimiters = new int[2]; // Concentrator Data Block starts and lengths
	private ArrayList<int[]> ctModDataBlockDelimiters = new ArrayList<int[]>(); // CT Module Data Block starts and lengths
	private ArrayList<int[]> wModDataBlockDelimiters = new ArrayList<int[]>(); // Wireless Module Data Block starts and lengths
	
	private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	//BUG FIX - MAY BE ABLE TO REMOVE IN FUTURE
	//required when concentrator date/times jump backward and then forward within the same file
	private boolean fixing = false;
	private int lastOKRow = 0;
	private int affectedRowCount = 0;
	
	//BUG FIX - MAY BE ABLE TO REMOVE IN FUTURE
	//required when concentrator makes several records within a single minute
	private int repeatRecordCount = 0;
	
	//DO NOT REMOVE - HELPFUL REFERENCE
	//String[] concTitles = {"Name","SN","NbData","Period","OverFlow","Number of Modules","Version","Wintertime"};
	//String[] ctModTitles = {"SN","Type of Module","Version","Channel Names","Multiplicators","Divisors","Phases"};
	//String[] wModTitles = {"SN","Type of Module","Version","Channel Names","Sensor SNs","Sensor Chs","Line","TotalLines"};
	//String[] dataTitles = {"SN","Current Address","Type of Module","Line Radio"};
	
	PDCValidator(ArrayList<File> filesToProcess,MySQLConnection mySQLConnection,LogWindow logWindow,boolean writeToFile,boolean writeToDatabase,boolean showGUI,boolean needMiniClampFix,boolean validateWChNames) {
		this.mySQLConnection = mySQLConnection;
		this.filesToProcess = filesToProcess;
		if (mySQLConnection != null){
			this.dbConn = mySQLConnection.getCopyConnection();
		}
		this.logWindow = logWindow;
		this.writeToFile = writeToFile;
		this.writeToDatabase = writeToDatabase;
		this.showGUI = showGUI;
		this.needMiniClampFix = needMiniClampFix;
		this.validateWChNames = validateWChNames;
	}
	
	@Override
	public void run() {
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		
		fileList = filesToProcess;
		
		Date startTime = new Date();
		
		logWindow.println("Extracting data from "+fileList.size()+" selected file(s)...\r\n");
		if (writeToDatabase){
			FileFeeder fileFeeder = new FileFeeder(mySQLConnection, logWindow, fileIssueList, false);
			Thread feederThread = new Thread(fileFeeder);
			feederThread.start();
			
			if (dbConn!=null){
		
				for (int i=0;i<fileList.size();i++){
					if (fileList.get(i).isFile()){
						FileData fileData = extractData(fileList.get(i),i);
						if (fileData.validDataExtracted){
							validateForDatabase(fileFeeder,fileList.get(i),fileData);
						}
						else{
							i--;
						}
					}
				}
				
				synchronized(fileFeeder.fileList){
					fileFeeder.moreFilesComing = false;
					fileFeeder.fileList.notify();
				}
				Date joinStart = new Date();
				logWindow.println("Total process and send time: "+getTimeString(joinStart.getTime()-startTime.getTime()));
				try {
					feederThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Date joinEnd = new Date();
				logWindow.println("Total wait for feeder time: "+getTimeString(joinEnd.getTime()-joinStart.getTime()));
				
				try{
					//write broken file info to database and logfile, write here because outside brackets means dbConn error
					Statement mySQLStatement = dbConn.createStatement();
					for (int i=0;i<fileIssueList.size();i++){
						File file = fileIssueList.get(i).getFile();
						String brokenFileSQL = "INSERT INTO issues_files (file_name,folder_name,issue_type,file_size,date_modified,attempts,notes) VALUES('"+file.getName()+"','"+new File(file.getParent()).getName()+"','"+fileIssueList.get(i).getIssueType()+"','"+file.length()+"','"+dateFormatter.format(file.lastModified())+"',1,'"+fileIssueList.get(i).getNotes()+"') ON DUPLICATE KEY UPDATE attempts=attempts+1,file_size='"+file.length()+"',date_modified='"+dateFormatter.format(file.lastModified())+"'";
						mySQLStatement.executeUpdate(brokenFileSQL);
					}
					if (fileIssueList.size()>0){
						logWindow.println("Wrote "+fileIssueList.size()+" file issues to the database.");
					}
					for (int i=0;i<sourceIssueList.size();i++){
						String brokenSourceSQL = "INSERT INTO issues_sources (file_name,folder_name,issue_type,source_name,attempts,notes) VALUES('"+sourceIssueList.get(i)[0]+"','"+sourceIssueList.get(i)[1]+"','"+sourceIssueList.get(i)[2]+"','"+sourceIssueList.get(i)[3]+"',1,'"+sourceIssueList.get(i)[4]+"') ON DUPLICATE KEY UPDATE attempts=attempts+1";
						mySQLStatement.executeUpdate(brokenSourceSQL);
					}
					if (sourceIssueList.size()>0){
						logWindow.println("Wrote "+sourceIssueList.size()+" source issues to the database.");
					}
					
					dbConn.close();
				}
				catch(SQLException sE){
					logWindow.println("WARNING: Error occurred while writing file processing logs (errors, etc) to database.\r\n");
				}
				logWindow.println("\r\nFinished writing "+fileList.size()+" file(s) to database.\r\n");
				Date endDataBaseTime = new Date();
				logWindow.println("Total log write time: "+getTimeString(endDataBaseTime.getTime()-joinEnd.getTime()));
				logWindow.println("Total write time: "+getTimeString(endDataBaseTime.getTime()-startTime.getTime()));
			}
			else{
				if (showGUI){JOptionPane.showMessageDialog(logWindow,"The selected database was is invalid, no data written.","Error",JOptionPane.ERROR_MESSAGE);}
			}
		}
		else if (writeToFile){
			validateForFileWrite();
			logWindow.println("\r\nFinished writing "+fileList.size()+" file(s) to file.\r\n");
		}
		Date endTime = new Date();
		logWindow.println("\r\nTotal time taken: "+getTimeString(endTime.getTime()-startTime.getTime()));
		
		logWindow.println("Done writing, found "+fileList.size()+" valid file(s).");
		
		Date endProcessTime = new Date();
		logWindow.println("Total extraction time: "+getTimeString(endProcessTime.getTime()-startTime.getTime())+"\r\n");
	}

	private void validateForFileWrite() {
		PDCTextFileWriter fWriter = new PDCTextFileWriter();
		File currFile = fileList.get(0);
		FileData fileData = extractData(fileList.get(0),0);
		FileData nextFileData = extractData(fileList.get(1),1);
		for (int i=1;i<fileList.size();i++){ //i is index of nextFile
			try {
				logWindow.println("Attempting to write "+ currFile.getName()+" to file...");
				File outputFile = new File(currFile.getName()+"(extracted).csv");
				BufferedWriter outputStream = new BufferedWriter(new FileWriter(outputFile));
				if (outputFile.canWrite()){
					logWindow.println("Processing file: "+currFile.getName());
					fWriter.writeHeaderToFile(outputStream,fileData.currConcHeaders,fileData.currCTModHeaders,fileData.currWModHeaders); //Write dataTable to File
					fWriter.writeDataToFile(outputStream,fileData.currConcDateData,fileData.currConcPhaseData,fileData.currCTModData,fileData.currWModData); //Write data to file
					// (((Integer.parseInt(ALL_CONC_HEADERS.get(i+1)[2])) - (Integer.parseInt(ALL_CONC_HEADERS.get(i)[2]))) % 720 == 0)
					// ALL_W_MOD_HEADERS.get(i)==ALL_W_MOD_HEADERS.get(i+1)
					while(i<fileList.size() && (((Integer.parseInt(nextFileData.currConcHeaders[2])) - (Integer.parseInt(fileData.currConcHeaders[2]))) % 720 == 0) && nextFileData.currConcDateData.get(0)-fileData.currConcDateData.get(fileData.currConcDateData.size()-1)==60000){
						currFile = fileList.get(i);
						fileData = nextFileData;
						if(i<fileList.size()-1){nextFileData = extractData(fileList.get(i+1),i+1);}
						logWindow.println("Processing file: "+currFile.getName());
						fWriter.writeDataToFile(outputStream,fileData.currConcDateData,fileData.currConcPhaseData,fileData.currCTModData,fileData.currWModData); //Write data to file
						i++;
					}
					if (i<fileList.size()-1){
						if (((Integer.parseInt(nextFileData.currConcHeaders[2])) - (Integer.parseInt(fileData.currConcHeaders[2]))) % 720 != 0){
							logWindow.println("Gap of "+(((Integer.parseInt(nextFileData.currConcHeaders[2])) - (Integer.parseInt(fileData.currConcHeaders[2]))) % 720)+" between: "+currFile.getName()+" and "+fileList.get(i).getName());
						}
						if (nextFileData.currConcDateData.get(0)-fileData.currConcDateData.get(fileData.currConcDateData.size()-1)!=60000){
							logWindow.println("Gap of "+((nextFileData.currConcDateData.get(0)-fileData.currConcDateData.get(fileData.currConcDateData.size()-1))/60/1000)+" mins between: "+currFile.getName()+" and "+fileList.get(i).getName());
						}
						if (fileData.currWModHeaders!=nextFileData.currWModHeaders){
							logWindow.println("Header Mismatch beteen files: "+currFile.getName()+" and "+fileList.get(i+1).getName());
						}
					}
					else{ //final file
						//TODO this should check whether to actually write the final data file to the current file or not...s
						logWindow.println("Processing file: "+currFile.getName());
						fWriter.writeDataToFile(outputStream,fileData.currConcDateData,fileData.currConcPhaseData,fileData.currCTModData,fileData.currWModData); //Write data to file
					}
					outputStream.flush();
					outputStream.close();
					outputFile = new File("");
				}
				else{
					logWindow.println("Cannot write to output file: "+outputFile.getName()+". Skipping...");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (i<fileList.size()){//will go past on final file
				currFile = fileList.get(i);
				fileData = nextFileData;
				if (i<fileList.size()-1){nextFileData = extractData(fileList.get(i+1),i+1);}
			}
		}
	}

	private void validateForDatabase(FileFeeder fileFeeder,File currFile,FileData fileData) {
		try {
			if (dbConn!=null){
				Site site = null;
				try{
					String fetchSiteIDSQL = "SELECT * FROM sites WHERE concentrator = '"+fileData.currConcSN+"' AND FROM_UNIXTIME("+(fileData.currConcDateData.get(0)/1000)+") BETWEEN start_date AND end_date";
					Statement findSiteStatement = dbConn.createStatement();
					ResultSet siteRS = findSiteStatement.executeQuery(fetchSiteIDSQL);
					if (siteRS.next()){
						site = new Site(siteRS.getString("site_id"),siteRS.getString("site_name"),siteRS.getString("concentrator"),siteRS.getString("start_date"),siteRS.getString("end_date"),siteRS.getString("given_name"),siteRS.getString("surname"),siteRS.getString("suburb"),siteRS.getString("state"));
					}
					else{
						site = null;
					}
					siteRS.close();
					findSiteStatement.close();
				} catch(SQLException sE){
					sE.printStackTrace();
					String errorMsg = "DB error while matching concentrator number to site.";
					if (showGUI){JOptionPane.showMessageDialog(null,errorMsg,"Error",JOptionPane.ERROR_MESSAGE);}
					logWindow.println(errorMsg+" Exiting...\r\n");
					throw new SQLException(errorMsg); //throws to higher try/catch block, which will add issue to database
				}


				String siteID = (site==null?null:site.getSiteID());

				if(siteID!=null){

					Date fileStartTime = new Date();

					int frequency = getInterval(fileData.currConcDateData,currFile.getName()); //get interval for file
					if(frequency!=0){
						System.out.println("Processing file: "+currFile.getName());
						LinkedList<DataFile> dataFiles = new LinkedList<DataFile>();
						Statement mySQLStatement = dbConn.createStatement();
						try{
							//check phase data

							if (fileData.currConcPhaseData[0].length==fileData.currConcDateData.size()){ //check same number of rows
								String[] phNames = new String [] {"Ph 1","Ph 2","Ph 3"};

								if (fileData.currConcPhaseData.length==phNames.length){ //check same number of columns
									for (int j=0;j<fileData.currConcPhaseData.length;j++){
										String sourceID = null;
										String sourceType = "";
										String measurementType = "";
										//try{
										String fetchSourceIDSQL =  "SELECT source_id,source_type,measurement_type FROM sources WHERE source_name = '"+phNames[j]+"' AND site_id = "+siteID;
										ResultSet fetchSourceIDRS = mySQLStatement.executeQuery(fetchSourceIDSQL);


										if (fetchSourceIDRS.next()){
											sourceID = fetchSourceIDRS.getString("source_id");
											sourceType = fetchSourceIDRS.getString("source_type");
											measurementType = fetchSourceIDRS.getString("measurement_type");
											if (!sourceType.equals("Phase")){
												logWindow.println("Warning: Unexpected Source Type '"+sourceType+"' for source '"+phNames[j]+"'. Expected 'Phase'. Site: "+siteID+".");
											}
											if (!measurementType.equals("Volts")){
												logWindow.println("Warning: Measurement Type '"+measurementType+"' for Phase '"+phNames[j]+"' does not match data in file '"+currFile.getName()+"' at site: "+siteID+".");
												//TODO write to issues file.
											}
										}
										else{
											logWindow.println("No matching Source ID found for source '"+phNames[j]+"' at site "+siteID+". Adding now...");

											sourceType = "Phase";
											measurementType = "Volts";
											try{

												Source testSource = new Source(site,null,phNames[j],sourceType,measurementType);
												sourceID = Source.addSource(logWindow, showGUI, mySQLStatement, site, testSource, currFile.getName(), true);

											}catch(SQLException sE){
												sE.printStackTrace();
												String errorMsg = "DB error occured while adding a new source: "+phNames[j];
												if (showGUI){JOptionPane.showMessageDialog(null,errorMsg,"Error",JOptionPane.ERROR_MESSAGE);}
												logWindow.println(errorMsg);
												throw new SQLException(); //throws to higher try/catch block, which will add issue to database
											}
										}
										fetchSourceIDRS.close();
										/*} catch (SQLException sE){
												sE.printStackTrace();
												String errorMsg = "DB error occured while matching source: '"+phNames[j]+"'.";
												if (showGUI){JOptionPane.showMessageDialog(null,errorMsg,"Error",JOptionPane.ERROR_MESSAGE);}
												logWindow.println(errorMsg);
												fileIssueList.add(new String[] {currFile.getName(),"DBError",errorMsg});
												throw new SQLException(); //throws to higher try/catch block, which will add issue to database
											}
											try{*/
										if (sourceID!=null){
											String fileExistsSQL = "SELECT * FROM files WHERE site_id = "+siteID+" AND source_id = "+sourceID+" AND file_name = '"+currFile.getName()+"' AND meter_sn = '"+fileData.currConcSN+"' AND frequency = "+frequency;
											ResultSet fileExistsQuery = mySQLStatement.executeQuery(fileExistsSQL);
											if (fileExistsQuery.next()==false){

												DataFile dataFile = new DataFile();
												dataFile.siteID = siteID;
												dataFile.sourceID = sourceID;
												dataFile.fileName = currFile.getName().toString();
												dataFile.file = currFile;
												dataFile.frequency = frequency;
												dataFile.meterSerial = fileData.currConcSN;
												dataFile.sourceType = sourceType;
												dataFile.measurementType = measurementType;

												for (int k=0;k<fileData.currConcPhaseData[j].length;k++){
													dataFile.dataList.add(new DataPoint(fileData.currConcDateData.get(k),fileData.currConcPhaseData[j][k]));
												}

												dataFiles.add(dataFile);

											}
											else{
												//fileIssueList.add(new String[] {currFile.getName(),"FileAlreadyExists",""});
												logWindow.println("Ignored file "+currFile.getName()+". This file already exists for source "+sourceID+" at site "+siteID+".");	
											}
											fileExistsQuery.close();
										}
										else{
											sourceIssueList.add(new String[] {currFile.getName(),new File(currFile.getParent()).getName(),"SourceWriteError",phNames[j],""});
											throw new FatalIssueException("Ignored file "+currFile.getName()+". Could not match or create new source with name '"+phNames[j]+"'");
										}
									}
								}
								else{ //column count mismatch
									fileIssueList.add(new FileIssue(currFile,"PHColMismatch",""));
									throw new FatalIssueException("Ignored file "+currFile.getName()+". Column count mismatch between wHeader and phData");
								}
							}
							else{ // row count mismatch
								fileIssueList.add(new FileIssue(currFile,"PHRowMismatch",""));
								throw new FatalIssueException("Ignored file "+currFile.getName()+". Row count mismatch between concDates and phData");
							}

							boolean legitimateHeaders = true;
							//check circuit data
							if (fileData.currCTModData[0].length==fileData.currConcDateData.size()){ //check same number of rows
								//Do CT channels
								//build Channel Names Array
								String[] ctModuleSNs = new String[fileData.currCTModHeaders.size()*6];
								String[] ctNames = new String[fileData.currCTModHeaders.size()*6];
								for (int c=0;c<fileData.currCTModHeaders.size();c++){ //CYCLES ONCE FOR EACH CT BLOCK (GROUP OF 6)
									String[] ctNameParts= fileData.currCTModHeaders.get(c)[3].split(",");
									for (int d=0;d<6;d++){
										String tempCTChName = ctNameParts[d];
										//sorts out dodgy headers and blank columns etc.
										if (tempCTChName.matches("^[\\w\\s\\-\\?\\+\\&\\(\\)\\/]{1,16}$")==false){ //could be error or legitimately blank or corrupted header
											//search for a legitimate header
											String headerQuerySQL = "SELECT ct_ch_names FROM header_log WHERE date_time = (SELECT MAX(date_time) FROM header_log WHERE date_time <= '"+dateFormatter.format(fileData.currConcDateData.get(0))+"' AND site_id = "+siteID+") AND site_id = "+siteID;
											ResultSet lastHeaderRS = mySQLStatement.executeQuery(headerQuerySQL);
											if (lastHeaderRS.next() && lastHeaderRS.getString("ct_ch_names").split(",").length == (fileData.currCTModHeaders.size()*6)){ // if found something and same number of channels
												String previousName = lastHeaderRS.getString("ct_ch_names").split(",")[c*6+d];
												if(previousName.matches("^[\\w\\s\\-\\?\\+\\&\\(\\)\\/]{1,16}$")){
													tempCTChName = previousName;
													legitimateHeaders = false;
												}
												else{ //if name is still bad
													sourceIssueList.add(new String[] {currFile.getName(),new File(currFile.getParent()).getName(),"InvalidSourceName",tempCTChName,""});
													throw new FatalIssueException("Ignored file "+currFile.getName()+". Source name '"+tempCTChName+"' is invalid.\r\n");
												}
											}
											else{//if nothing relevant in database
												sourceIssueList.add(new String[] {currFile.getName(),new File(currFile.getParent()).getName(),"InvalidSourceName",tempCTChName,""});
												throw new FatalIssueException("Ignored file "+currFile.getName()+". Source name '"+tempCTChName+"' is invalid.\r\n");
											}
											lastHeaderRS.close();
										}


										//test that name does not already exist in Wireless Channel Name List
										String fixedCTChName = tempCTChName;
										int duplicateCounter = 0;
										while (tempCTChName.matches("^[\\w\\s\\-\\?\\+\\&\\(\\)\\/]{1,16}$") && Arrays.asList(ctNames).contains(tempCTChName)){ //if ctChNames list already contains ctChName
											duplicateCounter++;
											tempCTChName = fixedCTChName + "("+duplicateCounter+")";
										}

										if (duplicateCounter>0){ //if current channel is a duplicate
											sourceIssueList.add(new String[] {currFile.getName(),new File(currFile.getParent()).getName(),"DuplicateCTChName",ctNameParts[d],""});
											//non fatal, so no exception thrown
										}


										ctNames[c*6+d] = tempCTChName;
										ctModuleSNs[c*6+d] = fileData.currCTModHeaders.get(c)[0];
									}
								}

								if (fileData.currCTModData.length==ctNames.length){ //check same number of columns
									for (int j=0;j<fileData.currCTModData.length;j++){
										String fetchSourceIDSQL =  "SELECT source_id,source_type,measurement_type FROM sources WHERE source_name = '"+ctNames[j]+"' AND site_id = "+siteID;
										ResultSet fetchSourceIDRS = mySQLStatement.executeQuery(fetchSourceIDSQL);

										String sourceID = null;
										String sourceType = "";
										String measurementType = "";
										if (fetchSourceIDRS.next()){
											sourceID = fetchSourceIDRS.getString("source_id");
											sourceType = fetchSourceIDRS.getString("source_type");
											measurementType = fetchSourceIDRS.getString("measurement_type");
											if (!sourceType.equals("Circuit")){
												logWindow.println("Warning: Unexpected Source Type '"+sourceType+"' for source '"+ctNames[j]+"'. Expected 'Circuit'. Site: "+siteID+".");
											}
											if (!measurementType.equals("ActPower")){
												logWindow.println("Warning: Measurement Type '"+measurementType+"' for Circuit '"+ctNames[j]+"' does not match data in file '"+currFile.getName()+"' at site: "+siteID+".");
												//TODO write to issues file.
											}
										}
										else{
											logWindow.println("No matching Source ID found for source '"+ctNames[j]+"' at site "+siteID+". Adding now...");

											sourceType = "Circuit";
											measurementType = "ActPower";
											try{
												Source testSource = new Source(site,null,ctNames[j],sourceType,measurementType);
												sourceID = Source.addSource(logWindow,showGUI,mySQLStatement, site, testSource,currFile.getName(), true);
											}catch(SQLException sE){
												sE.printStackTrace();
												if (showGUI){JOptionPane.showMessageDialog(null,"Error occured while adding a new source: '"+ctNames[j]+"' for file '"+currFile.getName()+"'.\r\nSource will not be added at this point. File will be ignored.","Error",JOptionPane.ERROR_MESSAGE);}
												logWindow.println("Error occured while adding a new source: '"+ctNames[j]+"' for file '"+currFile.getName()+"'.\r\nSource will not be added at this point. File will be ignored.\r\n");
											}
										}
										fetchSourceIDRS.close();
										if (sourceID!=null){
											String fileExistsSQL = "SELECT * FROM files WHERE site_id = "+siteID+" AND source_id = "+sourceID+" AND file_name = '"+currFile.getName()+"' AND meter_sn = '"+ctModuleSNs[j]+"' AND frequency = "+frequency;
											ResultSet fileExistsQuery = mySQLStatement.executeQuery(fileExistsSQL);
											if (fileExistsQuery.next()==false){

												DataFile dataFile = new DataFile();
												dataFile.siteID = siteID;
												dataFile.sourceID = sourceID;
												dataFile.fileName = currFile.getName().toString();
												dataFile.file = currFile;
												dataFile.frequency = frequency;
												dataFile.meterSerial = ctModuleSNs[j];
												dataFile.sourceType = sourceType;
												dataFile.measurementType = measurementType;

												for (int k=0;k<fileData.currCTModData[j].length;k++){
													dataFile.dataList.add(new DataPoint(fileData.currConcDateData.get(k),fileData.currCTModData[j][k]));
												}

												dataFiles.add(dataFile);

											}
											else{
												//fileIssueList.add(new String[] {currFile.getName(),"FileAlreadyExists",""});
												logWindow.println("Ignored file "+currFile.getName()+". This file already exists for source "+sourceID+" at site "+siteID+".");	
											}
											fileExistsQuery.close();
										}
										else{
											sourceIssueList.add(new String[] {currFile.getName(),new File(currFile.getParent()).getName(),"SourceWriteError",ctNames[j],""});
											throw new FatalIssueException("Ignored file "+currFile.getName()+". Could not match or create new source with name '"+ctNames[j]+"'");
										}
									}
								}
								else{ //column count mismatch
									fileIssueList.add(new FileIssue(currFile,"CTColMismatch",""));
									throw new FatalIssueException("Ignored file "+currFile.getName()+". Column count mismatch between wHeader and ctData");
								}
							}
							else{ // row count mismatch
								fileIssueList.add(new FileIssue(currFile,"CTRowMismatch",""));
								throw new FatalIssueException("Ignored file "+currFile.getName()+". Row count mismatch between concDates and ctData");
							}

							//check wireless data
							if (fileData.currWModData[0].length==fileData.currConcDateData.size()){ //check same number of rows
								//Do wireless channels
								//build Channel Names Array
								String[] wChNames = new String[fileData.currWModHeaders.size()*6];
								String[] wSensorSNs = new String[fileData.currWModHeaders.size()*6];
								String[] wSensorChs = new String[fileData.currWModHeaders.size()*6];
								for (int c=0;c<fileData.currWModHeaders.size();c++){ //c cycles through channels
									String[] wChNameParts = fileData.currWModHeaders.get(c)[3].split(",");
									String[] wSensorSNParts = fileData.currWModHeaders.get(c)[4].split(",");
									String[] wSensorChParts = fileData.currWModHeaders.get(c)[5].split(",");
									for (int d=0;d<6;d++){
										String tempWChName = wChNameParts[d];
										if (validateWChNames && (tempWChName.equals("[LEFT BLANK]") || tempWChName.matches("^[AGHLMTW][\\w\\s\\-\\(\\)\\/]{1,15}$")==false)){ //could be error or legitimately blank or corrupted header
											//search for a legitimate header
											String headerQuerySQL = "SELECT wl_ch_names FROM header_log WHERE date_time = (SELECT MAX(date_time) FROM header_log WHERE date_time <= '"+dateFormatter.format(fileData.currConcDateData.get(0))+"' AND site_id = "+siteID+") AND site_id = "+siteID;
											ResultSet lastHeaderRS = mySQLStatement.executeQuery(headerQuerySQL);
											if (lastHeaderRS.next() && lastHeaderRS.getString("wl_ch_names").split(",").length == (fileData.currWModHeaders.size()*6)){ // if found something and same number of channels
												String previousName = lastHeaderRS.getString("wl_ch_names").split(",")[c*6+d];
												if(previousName.equals("[LEFT BLANK]")==false && previousName.matches("^[\\w\\s\\-\\(\\)\\/]{1,16}$")){
													tempWChName = previousName;
													legitimateHeaders = false;
												}
												else{ //if name is still bad
													if (previousName.equals("[LEFT BLANK]")){
														if (tempWChName.equals("[LEFT BLANK]")==false){ //if title had an error, but header log was ok
															tempWChName = previousName;
															legitimateHeaders = false;
														}
														for (int k=0;k<fileData.currWModData[c*6+d].length;k++){ //search for any data in this column
															if ((fileData.currWModData[c*6+d][k] == -123.456) == false){ //if any data other than blanks is found
																sourceIssueList.add(new String[] {currFile.getName(),new File(currFile.getParent()).getName(),"InvalidSourceName",tempWChName,""});
																throw new FatalIssueException("Ignored file "+currFile.getName()+". Source name '"+tempWChName+"' is invalid.\r\n");
															}
														}
														//if get to here, legitimately empty, but column will be ignored
													}
													else{
														sourceIssueList.add(new String[] {currFile.getName(),new File(currFile.getParent()).getName(),"InvalidSourceName",tempWChName,""});
														throw new FatalIssueException("Ignored file "+currFile.getName()+". Source name '"+tempWChName+"' is invalid.\r\n");
													}
												}
											}
											else{//if nothing relevant in database
												if (tempWChName.equals("[LEFT BLANK]")){
													for (int k=0;k<fileData.currWModData[c*6+d].length;k++){ //search for any data in this column
														if ((fileData.currWModData[c*6+d][k] == -123.456) == false){ //if any data other than blanks is found
															sourceIssueList.add(new String[] {currFile.getName(),new File(currFile.getParent()).getName(),"LeftBlankWithData",tempWChName,""});
															//suggests corruption of some kind, so throw
															throw new FatalIssueException("Ignored file "+currFile.getName()+". Source name '"+tempWChName+"' is invalid.\r\n");
														}
													}
													//if get to here, legitimately empty, but column will be ignored
												}
												else{ //has name but name is invalid, stop all writing so that error can be dealt with
													sourceIssueList.add(new String[] {currFile.getName(),new File(currFile.getParent()).getName(),"InvalidSourceName",tempWChName,""});
													throw new FatalIssueException("Ignored file "+currFile.getName()+". Source name '"+tempWChName+"' is invalid.\r\n");
												}
											}
											lastHeaderRS.close();
										}

										//test that name does not already exist in Wireless Channel Name List
										String fixedWChName = tempWChName;
										int duplicateCounter = 0;
										while (tempWChName.equals("[LEFT BLANK]")==false && Arrays.asList(wChNames).contains(tempWChName)){ //if wchNames list already contains wChName (ignoring [LEFT BLANK]s)
											duplicateCounter++;
											tempWChName = fixedWChName + "("+duplicateCounter+")";

										}

										if (duplicateCounter>0){ //if current channel is a duplicate
											sourceIssueList.add(new String[] {currFile.getName(),new File(currFile.getParent()).getName(),"DuplicateWChName",wChNameParts[d],""});
											//non fatal, so no exception thrown
										}

										wChNames[c*6+d] = tempWChName;
										wSensorSNs[c*6+d] = wSensorSNParts[d];
										wSensorChs[c*6+d] = wSensorChParts[d];
									}
								}


								if (fileData.currWModData.length==wChNames.length){ //check same number of columns
									for (int j=0;j<fileData.currWModData.length;j++){
										if (wChNames[j].equals("[LEFT BLANK]")==false){ //ignore [LEFT BLANK]s
											String fetchSourceIDSQL =  "SELECT source_id,source_type,measurement_type FROM sources WHERE source_name = '"+wChNames[j]+"' AND site_id = "+siteID;
											ResultSet fetchSourceIDRS = mySQLStatement.executeQuery(fetchSourceIDSQL);

											String sourceID = null;
											String sourceType = "";
											String measurementType = "";
											if (fetchSourceIDRS.next()){
												sourceID = fetchSourceIDRS.getString("source_id");
												sourceType = fetchSourceIDRS.getString("source_type");
												measurementType = fetchSourceIDRS.getString("measurement_type");
												if (!sourceType.equals(getSourceType(wChNames[j],wSensorChs[j]))){
													logWindow.println("Warning: Source Type '"+sourceType+"' for source '"+wChNames[j]+"' contradicts naming convention. Site: "+siteID+".");
												}
												if (!measurementType.equals(getMeasurementType(wSensorChs[j]))){
													logWindow.println("Warning: Measurement Type '"+measurementType+"' for source '"+wChNames[j]+"' does not match data in file '"+currFile.getName()+"' at site: "+siteID+".");
													//TODO write to issues file.
												}
											}
											else{
												logWindow.println("No matching Source ID found for source '"+wChNames[j]+"' at site "+siteID+". Adding now...");

												sourceType = getSourceType(wChNames[j],wSensorChs[j]);
												measurementType = getMeasurementType(wSensorChs[j]);

												if (sourceType.equals("") && showGUI){
													String[] sourceTypes = new String[] {"Appliance","Gas","Humidity","Light","Motion","Temperature","Water"};
													sourceType = (String)JOptionPane.showInputDialog(null, "Could not determine Source Type using available information: '"+wChNames[j]+"' and '"+wSensorChs[j]+"' in file '"+currFile.getName()+"'.\r\nPlease select the appropriate type from the list:", "Select Source Type", JOptionPane.PLAIN_MESSAGE, null, sourceTypes, "Appliance");
													if(sourceType == null){sourceType = "";}
												}
												if (!sourceType.equals("") || !measurementType.equals("")){
													Source testSource = new Source(site,null,wChNames[j],sourceType,measurementType);
													sourceID = Source.addSource(logWindow,showGUI,mySQLStatement, site, testSource,currFile.getName(), true);
												}
												else{
													if (showGUI){JOptionPane.showMessageDialog(null,"Could not determine Source Type using available information: '"+wChNames[j]+"' and '"+wSensorChs[j]+"' in file '"+currFile.getName()+"'.\r\nSource will not be added at this point. File will be ignored.","Error",JOptionPane.ERROR_MESSAGE);}
													//determine appliance type
													logWindow.println("Could not determine Source Type using available information: '"+wChNames[j]+"' and '"+wSensorChs[j]+"' in file '"+currFile.getName()+"'.\r\nSource will not be added at this point. File will be ignored.");
												}
											}
											fetchSourceIDRS.close();
											if (sourceID!=null){
												String fileExistsSQL = "SELECT * FROM files WHERE site_id = "+siteID+" AND source_id = "+sourceID+" AND file_name = '"+currFile.getName()+"' AND meter_sn = '"+wSensorSNs[j]+"' AND frequency = "+frequency;
												ResultSet fileExistsQuery = mySQLStatement.executeQuery(fileExistsSQL);
												if (fileExistsQuery.next()==false){

													DataFile dataFile = new DataFile();
													dataFile.siteID = siteID;
													dataFile.sourceID = sourceID;
													dataFile.fileName = currFile.getName().toString();
													dataFile.file = currFile;
													dataFile.frequency = frequency;
													dataFile.meterSerial = wSensorSNs[j];
													dataFile.sourceType = sourceType;
													dataFile.measurementType = measurementType;

													for (int k=0;k<fileData.currWModData[j].length;k++){
														dataFile.dataList.add(new DataPoint(fileData.currConcDateData.get(k),fileData.currWModData[j][k]));
													}

													dataFiles.add(dataFile);

												}
												else{
													//fileIssueList.add(new String[] {currFile.getName(),"FileAlreadyExists",""});
													logWindow.println("Ignored file "+currFile.getName()+". This file already exists for source "+sourceID+" at site "+siteID+".");	
												}
												fileExistsQuery.close();
											}
											else{
												sourceIssueList.add(new String[] {currFile.getName(),new File(currFile.getParent()).getName(),"SourceWriteError",wChNames[j],""});
												throw new FatalIssueException("Ignored file "+currFile.getName()+". Could not match or create new source with name '"+wChNames[j]+"'\r\n");
											}
										}
										else{
											// no need to do anything, legitimately empty data set
										}
									}
								}
								else{ //column count mismatch
									fileIssueList.add(new FileIssue(currFile,"WColMismatch",""));
									throw new FatalIssueException("Ignored file "+currFile.getName()+". Column count mismatch between wHeader and wData\r\n");
								}
							}
							else{ // row count mismatch
								fileIssueList.add(new FileIssue(currFile,"WRowMismatch",""));
								throw new FatalIssueException("Ignored file "+currFile.getName()+". Row count mismatch between concDates and wData\r\n");
							}

							//send all files to file feeder
							while( dataFiles.peek() != null ){
								DataFile dataFile = dataFiles.poll();

								fileFeeder.addFile(dataFile); //Send file to feeder
							}

							//if get to here file will have been written
							//check to see that no file or source errors were thrown
							if (legitimateHeaders){//if no fatal errors (these throw exceptions) and haven't messed around with the headers
								//write header log to file
								try{
									writeHeaderLog(siteID,dateFormatter.format(fileData.currConcDateData.get(0)),fileData.currConcHeaders,fileData.currCTModHeaders,fileData.currWModHeaders);
								}catch(SQLException sE){
									//NON FATAL
									sE.printStackTrace();
									logWindow.println("Database error occurred while logging headers for file "+currFile.getName());
								}
							}
						} catch(FatalIssueException fIE){
							logWindow.println(fIE.getMessage());
						}
					}
					else{
						fileIssueList.add(new FileIssue(currFile,"NoFileFreq",""));
						logWindow.println("Error: no file frequency found for file "+currFile.getName()+"\r\n");
					}


					//logWindow.println("Attempting to write "+ currFile.getName()+" to database...");
					//dbWriter.writeDataToDatabase(logWindow,currFile.getName(),dbConn,installation_id,verbose,ALL_CONC_HEADERS.get(i),ALL_CT_MOD_HEADERS.get(i),ALL_W_MOD_HEADERS.get(i),ALL_CONC_DATE_DATA.get(i),ALL_CONC_PHASE_DATA.get(i),ALL_CT_MOD_DATA.get(i),ALL_W_MOD_DATA.get(i));
					//pdcAdapter.adaptWireless(currFile.getName(),logWindow,mySQLConnection,siteID,ALL_CONC_DATE_DATA.get(i),ALL_W_MOD_HEADERS.get(i),ALL_W_MOD_DATA.get(i));

					Date fileEndTime = new Date();
					logWindow.println("Time to write "+currFile.getName()+" to database: "+getTimeString(fileEndTime.getTime()-fileStartTime.getTime())+"\r\n");
				}
				else{
					fileIssueList.add(new FileIssue(currFile,"NoSiteMatch","Could not match concentrator number "+fileData.currConcSN+" to a Site"));
					logWindow.println("Error occured while processing "+currFile.getName()+" to database: unable to determine a Site ID for this file.\r\n");
				}
			}

		}
		catch (SQLException sE) {
			sE.printStackTrace();
			logWindow.println("FATAL DB ERROR OCCURRED, ISSUE WILL NOT BE RECORDED IN DB. EXITING...\r\n");
			logWindow.println(" Exiting...\r\n");
		}
	}	
	
	private FileData extractData(File fileToProcess,int filePointer) {
		byte[] fileArray = null;
		try {
			fileArray = readBytesIntoArray(fileToProcess);
		} catch (HeadlessException e) {
			e.printStackTrace();
		} catch (IOException e) {
			
			logWindow.println("Reading file "+fileToProcess.getName().toString()+" failed.");
			e.printStackTrace();
		}
		if (fileArray != null && fileArray.length!=0){
			
			concHeaderDelimiters = new int[2]; //reset values
			ctModHeaderDelimiters.clear(); //reset values
			wModHeaderDelimiters.clear(); //reset values
			
			concDataBlockDelimiters = new int[2]; //reset values
			ctModDataBlockDelimiters.clear(); //reset values
			wModDataBlockDelimiters.clear(); //reset values
			
			String[] blocksOK = getBlockEnds(fileArray,filePointer); //starts and ends of each datablock placed into ctModHeaders, wModHeaders and dataBlocks		
			
			if (blocksOK[0].equals("OK") && blocksOK[1].equals("")){
				if (Arrays.equals(concHeaderDelimiters, new int[2])==false && ctModHeaderDelimiters.size()!=0 && wModHeaderDelimiters.size()!=0){
					
					String[] concHeaderValues = readConcHeader(fileArray); //Read Concentrator Header
		
					ArrayList<String[]> ctModHeaderValues = readCTModuleHeaders(fileArray); //Read CT Module Headers

					ArrayList<String[]> wModHeaderValues = readWirelessModuleHeaders(fileArray); //Read Wireless Module Headers
		
					//String[] concDataBlockHeaderValues = readConcDataBlockHeaders(fileArray); //Read Concentrator Data Block Headers
					
					ArrayList<String[]> ctModDataBlockHeaderValues = readCTModDataBlockHeaders(fileArray); //Read CT Module Data Block Headers
					
					ArrayList<String[]> wModDataBlockHeaderValues = readWModDataBlockHeaders(fileArray); //Read Wireless Module Data Block Headers
					
					ArrayList<Long> concDatesDataBlock = readConcDatesDataBlock(fileArray,filePointer); // Read Concentrator Data Block Dates
					
					if (concDatesDataBlock.isEmpty()==false){
						
						double[][] concPhasesDataBlock = readConcPhasesDataBlock(fileArray,filePointer,concDatesDataBlock.size()); // Read Concentrator Data Block Dates
						
						if ((Integer.parseInt(concHeaderValues[5])-1) == ctModDataBlockHeaderValues.size() && ctModDataBlockHeaderValues.size() == ctModHeaderValues.size() && wModDataBlockHeaderValues.size() == wModHeaderValues.size() && wModDataBlockHeaderValues.size() >= 1 && wModDataBlockHeaderValues.size() <= 8){
	 						
	 						
	 						
	 						double[][] ctModDataBlocks = makeCTModDataBlocks(fileArray,ctModHeaderValues,concDatesDataBlock.size()); //make CT Mod Data Table by reading data Blocks into it
							
							double[][] wModDataBlocks = makeWModDataBlocks(fileArray,wModHeaderValues,concDatesDataBlock.size()); //make W Mod Data Table by reading data Blocks into it
							
							
							FileData fileData = new FileData(concHeaderValues,ctModHeaderValues,wModHeaderValues,concDatesDataBlock,concPhasesDataBlock,ctModDataBlocks,wModDataBlocks,concHeaderValues[1],true);
							
							return fileData;
						}
						else{
							logWindow.println("Missing module data blocks or headers from file: "+fileList.get(filePointer).getName()+". This file will be ignored.\r\n");
							logWindow.println((Integer.parseInt(concHeaderValues[5])-1)+" "+ctModDataBlockHeaderValues.size());
							fileIssueList.add(new FileIssue(fileList.get(filePointer),"ModData",""));
							fileList.remove(filePointer);
							return new FileData(null,null,null,null,null,null,null,null,false);
						}
					}
					else{
						logWindow.println("Corrupted or missing concentrator data blocks in file: "+fileList.get(filePointer).getName()+". This file will be ignored.\r\n");
						fileIssueList.add(new FileIssue(fileList.get(filePointer),"ConcData",""));
						fileList.remove(filePointer);
						return new FileData(null,null,null,null,null,null,null,null,false);
					}
				}
				else {
					logWindow.println("File: "+fileList.get(filePointer).getName()+" is missing Headers. This file will be ignored.\r\n");
					fileIssueList.add(new FileIssue(fileList.get(filePointer),"MissHead",""));
					fileList.remove(filePointer);
					return new FileData(null,null,null,null,null,null,null,null,false);
				}
            }
			else {
				logWindow.println(blocksOK[1]);
				logWindow.println("Ignored corrupted file: "+fileList.get(filePointer).getName()+"\r\n");
				fileIssueList.add(new FileIssue(fileList.get(filePointer),"Corrupt",""));
				fileList.remove(filePointer);
				return new FileData(null,null,null,null,null,null,null,null,false);
			}
		}   
		else{
			logWindow.println("Ignored empty file "+fileList.get(filePointer).getName()+"\r\n");
			fileIssueList.add(new FileIssue(fileList.get(filePointer),"Empty",""));
			fileList.remove(filePointer);
			return new FileData(null,null,null,null,null,null,null,null,false);
		}
	}
	
	private String[] getBlockEnds(byte[] fileArray, int filePointer){
		String[] blocksOK = new String[] {"ERR","ERROR: Full set of valid Data Blocks could not be found. File may be corrupted."};
		for (int i=0; i<fileArray.length; i++) {
        	if (byteToHex(fileArray[i]).equals("80") && byteToHex(fileArray[i+1]).equals("01")){
        		boolean blockDelimOk = false;
        		if (i>1){
        			if (byteToHex(fileArray[i-2]).equals("7f") && byteToHex(fileArray[i-1]).equals("fe")){
        				blockDelimOk = true;
        			}
        		} else{
        			if (i==0){
        				blockDelimOk = true;
        			}
        		}	
        		if (blockDelimOk==true){
        			if (i+6+bytesToInt(fileArray[i+2],fileArray[i+3])<fileArray.length){
	            		if (byteToHex(fileArray[i+6+bytesToInt(fileArray[i+2],fileArray[i+3])]).equals("7f") && byteToHex(fileArray[i+7+bytesToInt(fileArray[i+2],fileArray[i+3])]).equals("fe")){
		            		if (byteToHex(fileArray[i+4]).equals("00") && byteToHex(fileArray[i+5]).equals("01")){
		            			if (Arrays.equals(concHeaderDelimiters, new int[2])){
		            				concHeaderDelimiters[0] = i;
		            				concHeaderDelimiters[1] = bytesToInt(fileArray[i+2],fileArray[i+3])+8;		            			}
		            			else{
		            				blocksOK = new String[] {"ERR","ERROR: Multiple Concentrator Headers found.\nFile cannot be processed."};
		            				break;
		            			}
		            		}
		            		else if (byteToHex(fileArray[i+4]).equals("00") && byteToHex(fileArray[i+5]).equals("02")){
		            			int[] modHeaderInfo = {i,bytesToInt(fileArray[i+2],fileArray[i+3])+8};
		            			if (byteToHex(fileArray[i+10]).equals("fa")){
		            				wModHeaderDelimiters.add(modHeaderInfo);  
		            			}
		            			//else if (byteToHex(fileArray[i+10]).equals("ff")){ //OLD
		            			else if (byteToHex(fileArray[i+10]).equals("ff") || byteToHex(fileArray[i+10]).equals("00")){
		            				ctModHeaderDelimiters.add(modHeaderInfo);
		            			}
		            		}
		            		else if (byteToHex(fileArray[i+4]).equals("00") && byteToHex(fileArray[i+5]).equals("10")){
		            			int[] blockInfo = {i,bytesToInt(fileArray[i+2],fileArray[i+3])+8};
		            			//if (byteToHex((byte)(makeUnsigned(fileArray[i+14]) | makeUnsigned(fileArray[i+14]))).equals("00")){ //OLD
		            			if ((byteToHex((byte)(makeUnsigned(fileArray[i+14]) | makeUnsigned(fileArray[i+14]))).equals("00")) && Arrays.equals(new int[2],concDataBlockDelimiters)){
		            				concDataBlockDelimiters = blockInfo;
		            			}
		            			//else if (byteToHex((byte)(makeUnsigned(fileArray[i+14]) | makeUnsigned(fileArray[i+14]))).equals("ff")){ //OLD
		            			else if ((byteToHex((byte)(makeUnsigned(fileArray[i+14]) | makeUnsigned(fileArray[i+14]))).equals("ff")) || (byteToHex((byte)(makeUnsigned(fileArray[i+14]) | makeUnsigned(fileArray[i+14]))).equals("00"))){
		            				ctModDataBlockDelimiters.add(blockInfo);
		            			}
		            			else if (byteToHex((byte)(makeUnsigned(fileArray[i+14]) | makeUnsigned(fileArray[i+14]))).equals("fa")){
		            				wModDataBlockDelimiters.add(blockInfo);
		            			}
		            			else {
		            				blocksOK = new String[] {"ERR","ERROR: Data Blocks detected an unknown Data Type, this file will be ignored."};
		            				break;
		            			}
		            		}
		            		else {
		            			blocksOK = new String[] {"ERR","ERROR: Data Blocks detected without Data Type, this file will be ignored."};
		            			break;
		            		}
	            		}
        			}
        			else {
        				blocksOK = new String[] {"ERR","ERROR: Data block with length exceeding that of the file. File Will be ignored."};
        				break;
        			}
        		}
        	}
        }
		if (blocksOK[1] == "ERROR: Full set of valid Data Blocks could not be found. File may be corrupted."){
			if (concHeaderDelimiters[1]!=38){
				blocksOK = new String[] {"ERR","ERROR: Concentrator header length is incorrect. ie. not 38 : "+concHeaderDelimiters[1]};
			}
			else {
				blocksOK = new String[] {"OK",""};
			}
		}
		return blocksOK;
	}
	
	private String[] readConcHeader(byte[] fileArray){ //Read Concentrator Header
		String[] concHeader = new String[8]; //Array holding 8 values relating to the concentrator - as specified in concTitles
		concHeader[0] = "";
    	for (int i=concHeaderDelimiters[0]+6;i<22;i++){ //Name
    		if (byteToHex(fileArray[i]).equals("00")){
				break;
			}
			else {
				concHeader[0] += (char)fileArray[i]; //add 16 characters to each space in wModValues then move to next channel 
			}
    	} 
    	concHeader[1] = byteToHex(fileArray[concHeaderDelimiters[0]+25]) + "" + byteToHex(fileArray[concHeaderDelimiters[0]+24]) + "" + byteToHex(fileArray[concHeaderDelimiters[0]+23]) + "" + byteToHex(fileArray[concHeaderDelimiters[0]+22]); //SN
    	concHeader[2] = Integer.toString((((makeUnsigned(fileArray[concHeaderDelimiters[0]+29]) << 8) << 8) << 8) | ((makeUnsigned(fileArray[concHeaderDelimiters[0]+28]) << 8) << 8) | (makeUnsigned(fileArray[concHeaderDelimiters[0]+27]) << 8) | makeUnsigned(fileArray[concHeaderDelimiters[0]+26])); //NbData
    	concHeader[3] = getPeriod(Integer.parseInt(byteToHex(fileArray[concHeaderDelimiters[0]+30]))); //Period
    	concHeader[4] = byteToHex(fileArray[concHeaderDelimiters[0]+31]); //Overflow
    	concHeader[5] = byteToHex(fileArray[concHeaderDelimiters[0]+32]); //Number of Modules
    	concHeader[6] = byteToHex(fileArray[concHeaderDelimiters[0]+33]); //GPRS Version
    	concHeader[7] = byteToHex(fileArray[concHeaderDelimiters[0]+34]); //Wintertime
    	
    	//System.out.println("Concentrator SN: "+concHeader[1]);
    	//System.out.println("GPRS Version: "+concHeader[6]);

    	return concHeader;
	}
	
	
	private ArrayList<String[]> readCTModuleHeaders(byte[] fileArray){  //Read CT Module Headers
		ArrayList<String[]> ctModHeaderValues = new ArrayList<String[]>(); //CT Module Values
		
    	for (int i=0;i<ctModHeaderDelimiters.size();i++){
    		String[] ctModHeaderValues_TEMP = new String[7];
    		String modType = byteToHex(fileArray[ctModHeaderDelimiters.get(i)[0]+10]);
    		if (modType.equals("fc") || modType.equals("fd") || modType.equals("fe")|| modType.equals("ff") || modType.equals("00")){
        		ctModHeaderValues_TEMP[0] = byteToHex(fileArray[ctModHeaderDelimiters.get(i)[0]+9]) + "" + byteToHex(fileArray[ctModHeaderDelimiters.get(i)[0]+8]) + "" + byteToHex(fileArray[ctModHeaderDelimiters.get(i)[0]+7]) + "" + byteToHex(fileArray[ctModHeaderDelimiters.get(i)[0]+6]);
        		ctModHeaderValues_TEMP[1] = modType; //Type of Module ("ff" means CT Module)
        		ctModHeaderValues_TEMP[2] = byteToHex(fileArray[ctModHeaderDelimiters.get(i)[0]+11]);
        		ctModHeaderValues_TEMP[3] = "";
            	for (int j=0;j<6;j++){ //6 channel names in each module
            		String tempChNm = ""; //temporary channel name
            		for (int k=0;k<16;k++){ //16 characters in each name
            			byte tempByte = fileArray[ctModHeaderDelimiters.get(i)[0]+12+(j*16)+k];
            			if (byteToHex(tempByte).equals("00")){
            				break;
            			}
	            		tempChNm += (char)tempByte; //add 16 characters to each space in ctModValues then move to next channel     		
            		}
            		ctModHeaderValues_TEMP[3] += tempChNm.trim();
            		if (j!=5){ctModHeaderValues_TEMP[3] += ",";} //separate each name by a comma to easily split later
            	}
            	ctModHeaderValues_TEMP[4] = "";
            	for (int j=0;j<6;j++){ // Muls for 6 Channels
            		ctModHeaderValues_TEMP[4] += byteToHex(fileArray[ctModHeaderDelimiters.get(i)[0]+108+j*2]); 
            		if (j!=5){ctModHeaderValues_TEMP[4] += ",";} //separate each Mul by a comma
            	}
            	ctModHeaderValues_TEMP[5] = "";
            	for (int j=0;j<6;j++){ //Divs for 6 Channels
            		ctModHeaderValues_TEMP[5] += byteToHex(fileArray[ctModHeaderDelimiters.get(i)[0]+109+j*2]); 
            		if (j!=5){ctModHeaderValues_TEMP[5] += ",";} //separate each Div by a comma
            	}
            	ctModHeaderValues_TEMP[6] = "";
            	for (int j=0;j<6;j++){ //Phases for 6 Channels
            		ctModHeaderValues_TEMP[6] += byteToHex(fileArray[ctModHeaderDelimiters.get(i)[0]+120+j]); 
            		if (j!=5){ctModHeaderValues_TEMP[6] += ",";} //separate each Phase by a comma
            	}
            	
            	ctModHeaderValues.add(i,ctModHeaderValues_TEMP); //write values to the CT Module Header Array
        	}
    		else {
    			//TODO FUCKUP 
    		}
    	}
    	return ctModHeaderValues;
	}
	
	private ArrayList<String[]> readWirelessModuleHeaders(byte[] fileArray){ //Read Wireless Module Headers
		ArrayList<String[]> wModHeaderValues = new ArrayList<String[]>(); //Wireless Module values
		
    	for (int i=0;i<wModHeaderDelimiters.size();i++){
    		String[] wModHeaderValues_TEMP = new String[8];
    		String modType = byteToHex(fileArray[wModHeaderDelimiters.get(i)[0]+10]);
    		if (modType.equals("fa")){
            	wModHeaderValues_TEMP[0] = byteToHex(fileArray[wModHeaderDelimiters.get(i)[0]+9]) + "" + byteToHex(fileArray[wModHeaderDelimiters.get(i)[0]+8]) + "" + byteToHex(fileArray[wModHeaderDelimiters.get(i)[0]+7]) + "" + byteToHex(fileArray[wModHeaderDelimiters.get(i)[0]+6]); //SN
            	wModHeaderValues_TEMP[1] = modType; //Type of Module ("fa" means wireless)
            	wModHeaderValues_TEMP[2] = byteToHex(fileArray[wModHeaderDelimiters.get(i)[0]+11]); //Version
            	wModHeaderValues_TEMP[3] = ""; //Channel Names
            	for (int j=0;j<6;j++){ //6 channel names in each module
            		String tempChNm = ""; //temporary channel name
            		for (int k=0;k<16;k++){ //16 characters in each name
            			byte tempByte = fileArray[wModHeaderDelimiters.get(i)[0]+12+(j*16)+k];
            			if (byteToHex(tempByte).equals("00")){
            				break;
            			}
            			else {
            				if (byteToHex(tempByte).equals("ff")){
            					tempChNm += "?";
            				}
            				else {
            					tempChNm += (char)tempByte; //add 16 characters to each space in wModValues then move to next channel 
            				}
            			}
            		}
            		if (tempChNm.equals("????????????????") || tempChNm.trim().equals("")){ tempChNm = "[LEFT BLANK]";}
            		wModHeaderValues_TEMP[3] += tempChNm.trim();
            		if (j!=5){wModHeaderValues_TEMP[3] += ",";} //separate each name by a comma to easily split later
            	}
            	wModHeaderValues_TEMP[4] = ""; //Sensor SNs
            	for (int j=0;j<12;j=j+2){
            		wModHeaderValues_TEMP[4] += byteToHex(fileArray[wModHeaderDelimiters.get(i)[0]+108+j])+""+byteToHex(fileArray[wModHeaderDelimiters.get(i)[0]+109+j]);
            		if (j!=10){ wModHeaderValues_TEMP[4] += ",";}
            	}
            	wModHeaderValues_TEMP[5] = ""; //Sensors Chs
            	for (int j=0;j<6;j++){
            			wModHeaderValues_TEMP[5] += byteToHex(fileArray[wModHeaderDelimiters.get(i)[0]+120+j]);
            		if (j!=5){ wModHeaderValues_TEMP[5] += ",";}
            	}
            	wModHeaderValues_TEMP[6] = byteToHex(fileArray[wModHeaderDelimiters.get(i)[0]+126]); //Line
            	wModHeaderValues_TEMP[7] = byteToHex(fileArray[wModHeaderDelimiters.get(i)[0]+127]); //Total Lines  
            	
            	wModHeaderValues.add(i,wModHeaderValues_TEMP); //write values to the Wireless Module Header Array
    		}
    		else{
    			//TODO FUCKUP
    		}
    	}
    	return wModHeaderValues;
	}

	/*private String[] readConcDataBlockHeaders(byte[] fileArray){ //Read Concentrator Data Block Headers
		String[] concDataBlockHeaderValues = new String[4]; //Data Block Header values
		String[] concDataBlockHeaderValues_TEMP = new String[4];
	    
		//Read Data Block Header
		concDataBlockHeaderValues_TEMP[0] = byteToHex(fileArray[concDataBlockDelimiters[0]+9]) + "" + byteToHex(fileArray[concDataBlockDelimiters[0]+8]) + "" + byteToHex(fileArray[concDataBlockDelimiters[0]+7]) + "" + byteToHex(fileArray[concDataBlockDelimiters[0]+6]); //SN
		concDataBlockHeaderValues_TEMP[1] = Integer.toString((((makeUnsigned(fileArray[concDataBlockDelimiters[0]+13]) << 8) << 8) << 8) | ((makeUnsigned(fileArray[concDataBlockDelimiters[0]+12]) << 8) << 8) | (makeUnsigned(fileArray[concDataBlockDelimiters[0]+11]) << 8) | makeUnsigned(fileArray[concDataBlockDelimiters[0]+10]));
		concDataBlockHeaderValues_TEMP[2] = byteToHex((byte)(makeUnsigned(fileArray[concDataBlockDelimiters[0]+14]) | makeUnsigned(fileArray[concDataBlockDelimiters[0]+15]))); //Type Of Module
		concDataBlockHeaderValues_TEMP[3] = byteToHex((byte)(makeUnsigned(fileArray[concDataBlockDelimiters[0]+16]) | makeUnsigned(fileArray[concDataBlockDelimiters[0]+17]))); //Line Radio
	
		concDataBlockHeaderValues = concDataBlockHeaderValues_TEMP;
		
		return concDataBlockHeaderValues;
	}*/
	
	private ArrayList<String[]> readCTModDataBlockHeaders(byte[] fileArray){ ////Read CT Module Data Block Headers
		ArrayList<String[]> ctModDataBlockHeaderValues = new ArrayList<String[]>(); //Data Block Header values

		for (int i=0;i<ctModDataBlockDelimiters.size();i++){
			String[] ctModDataBlockHeaderValues_TEMP = new String[4];
	    	//Read Data Block Header
			ctModDataBlockHeaderValues_TEMP[0] = byteToHex(fileArray[ctModDataBlockDelimiters.get(i)[0]+9]) + "" + byteToHex(fileArray[ctModDataBlockDelimiters.get(i)[0]+8]) + "" + byteToHex(fileArray[ctModDataBlockDelimiters.get(i)[0]+7]) + "" + byteToHex(fileArray[ctModDataBlockDelimiters.get(i)[0]+6]); //SN
			ctModDataBlockHeaderValues_TEMP[1] = Integer.toString((((makeUnsigned(fileArray[ctModDataBlockDelimiters.get(i)[0]+13]) << 8) << 8) << 8) | ((makeUnsigned(fileArray[ctModDataBlockDelimiters.get(i)[0]+12]) << 8) << 8) | (makeUnsigned(fileArray[ctModDataBlockDelimiters.get(i)[0]+11]) << 8) | makeUnsigned(fileArray[ctModDataBlockDelimiters.get(i)[0]+10]));
			ctModDataBlockHeaderValues_TEMP[2] = byteToHex((byte)(makeUnsigned(fileArray[ctModDataBlockDelimiters.get(i)[0]+14]) | makeUnsigned(fileArray[ctModDataBlockDelimiters.get(i)[0]+15]))); //Type Of Module
			ctModDataBlockHeaderValues_TEMP[3] = byteToHex((byte)(makeUnsigned(fileArray[ctModDataBlockDelimiters.get(i)[0]+16]) | makeUnsigned(fileArray[ctModDataBlockDelimiters.get(i)[0]+17]))); //Line Radio
			
			ctModDataBlockHeaderValues.add(i,ctModDataBlockHeaderValues_TEMP);
		}
		return ctModDataBlockHeaderValues;
	}
	
	private ArrayList<String[]> readWModDataBlockHeaders(byte[] fileArray){ //Read Wireless Module Data Block Headers
		ArrayList<String[]> wModDataBlockHeaderValues = new ArrayList<String[]>(); //Data Block Header values
		
		for (int i=0;i<wModDataBlockDelimiters.size();i++){
			String[] wModDataBlockHeaderValues_TEMP = new String[4];
	    	//Read Data Block Header
			wModDataBlockHeaderValues_TEMP[0] = byteToHex(fileArray[wModDataBlockDelimiters.get(i)[0]+9]) + "" + byteToHex(fileArray[wModDataBlockDelimiters.get(i)[0]+8]) + "" + byteToHex(fileArray[wModDataBlockDelimiters.get(i)[0]+7]) + "" + byteToHex(fileArray[wModDataBlockDelimiters.get(i)[0]+6]); //SN
			wModDataBlockHeaderValues_TEMP[1] = Integer.toString((((makeUnsigned(fileArray[wModDataBlockDelimiters.get(i)[0]+13]) << 8) << 8) << 8) | ((makeUnsigned(fileArray[wModDataBlockDelimiters.get(i)[0]+12]) << 8) << 8) | (makeUnsigned(fileArray[wModDataBlockDelimiters.get(i)[0]+11]) << 8) | makeUnsigned(fileArray[wModDataBlockDelimiters.get(i)[0]+10]));
			wModDataBlockHeaderValues_TEMP[2] = byteToHex((byte)(makeUnsigned(fileArray[wModDataBlockDelimiters.get(i)[0]+14]) | makeUnsigned(fileArray[wModDataBlockDelimiters.get(i)[0]+15]))); //Type Of Module
			wModDataBlockHeaderValues_TEMP[3] = byteToHex((byte)(makeUnsigned(fileArray[wModDataBlockDelimiters.get(i)[0]+16]) | makeUnsigned(fileArray[wModDataBlockDelimiters.get(i)[0]+17]))); //Line Radio
			
			wModDataBlockHeaderValues.add(i,wModDataBlockHeaderValues_TEMP);
		}
		return wModDataBlockHeaderValues;
	}
			
	private ArrayList<Long> readConcDatesDataBlock(byte[] fileArray,int filePointer){ //Read Concentrator Data Block (Dates and Times)
		ArrayList<Long> concDatesDataBlock = new ArrayList<Long>();
		boolean Ok = true;
		
		int currentAddress = Integer.parseInt(byteToHex(fileArray[concDataBlockDelimiters[0]+13])+""+byteToHex(fileArray[concDataBlockDelimiters[0]+12])+""+byteToHex(fileArray[concDataBlockDelimiters[0]+11])+""+byteToHex(fileArray[concDataBlockDelimiters[0]+10]),16);
		
		
		
		int rowPointer = 0;
		for (int j=concDataBlockDelimiters[0]+18;j<concDataBlockDelimiters[0]+concDataBlockDelimiters[1]-12;j=j+12){
			//int rowPointer = (j-18-concDataBlockDelimiters[0])/12; //operations on j to make it cycle through 0,1,2,3,4,5,6 as per datablock
			
			if (byteToHex(fileArray[j+0]).matches("\\d\\d") && byteToHex(fileArray[j+1]).matches("\\d\\d") && byteToHex(fileArray[j+2]).matches("\\d\\d") && byteToHex(fileArray[j+3]).matches("\\d\\d") && byteToHex(fileArray[j+4]).matches("\\d\\d") && byteToHex(fileArray[j+5]).matches("\\d\\d")){ //check that all date components are numbers		
				if (rowPointer!=0 || (rowPointer == 0 && ((writeToDatabase && byteToHex(fileArray[j+5]).equals("00")) || !writeToDatabase))){ //file must start with a valid time ie. xx:xx:00 if writing to database
					//if (rowPointer == 0 && byteToHex(fileArray[j+5]).equals("00")==false){
					//	System.out.println(fileArray[j+5]);
					//}

					int rowYear = Integer.parseInt(byteToHex(fileArray[j+0]));
					int rowMon = Integer.parseInt(byteToHex(fileArray[j+1]));
					int rowDay = Integer.parseInt(byteToHex(fileArray[j+2]));
					int rowHour = Integer.parseInt(byteToHex(fileArray[j+3]));
					int rowMin = Integer.parseInt(byteToHex(fileArray[j+4]));
					int rowSec = Integer.parseInt(byteToHex(fileArray[j+5]));
					//if (rowSec!=0){rowSec = 0;} //shoudln't need this, only for testing...
					if (rowYear>70 && rowYear<99){rowYear=+1900;} else if (rowYear<=70){rowYear+=2000;}


					try{
						SimpleDateFormat concDatesFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						concDatesFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
						String tString = rowYear+"-"+rowMon+"-"+rowDay+" "+rowHour+":"+rowMin+":"+rowSec;
						long tDate = concDatesFormatter.parse(tString).getTime();
						concDatesDataBlock.add(rowPointer,tDate);


						if (rowPointer>0 && writeToDatabase){ //ignore first row of file
							long dateDiff = concDatesDataBlock.get(rowPointer)-concDatesDataBlock.get(rowPointer-1);
							if (dateDiff!=60000 && fixing == false){
								if (dateDiff > 0 && dateDiff < 60000 && concDatesDataBlock.get(rowPointer)%60000!=0 && concDatesDataBlock.get(rowPointer)-concDatesDataBlock.get(rowPointer-(repeatRecordCount+1))<60000){
									long offendingRow =  concDatesDataBlock.get(rowPointer);
									String gapTime = dateFormatter.format(offendingRow);
									logWindow.println("WARNING: found gap of < 1 min at "+gapTime+" in file: "+fileList.get(filePointer).getName()+". Row will not be processed into database.");
									repeatRecordCount++;
									Ok = false;
								}
								else if (dateDiff > 0 && dateDiff < 60000 && concDatesDataBlock.get(rowPointer)%60000!=0 && concDatesDataBlock.get(rowPointer)-concDatesDataBlock.get(rowPointer-(repeatRecordCount+1))==60000){
									long offendingRow = concDatesDataBlock.get(rowPointer);
									String gapTime = dateFormatter.format(offendingRow);
									logWindow.println("INFO: data returns to normal at "+gapTime+" in file: "+fileList.get(filePointer).getName()+".");
									repeatRecordCount = 0;
									Ok = true;
								}
								else {//fix for bug where concentrator date/times jumps back and then forward again before the end of the file 
									lastOKRow = rowPointer-1;
									long offendingRow = concDatesDataBlock.get(lastOKRow);
									String gapTime = dateFormatter.format(offendingRow);
									logWindow.printString("WARNING: found gap of "+(dateDiff/60/1000)+" mins at "+gapTime+" in file: "+fileList.get(filePointer).getName()+". Attempting to fix gap...");
									long newDateTime = offendingRow+60000;
									concDatesDataBlock.set(rowPointer,newDateTime);
									fixing = true;
									Ok = false;
									affectedRowCount=1;
								}
							}
							else if (fixing == true){ //fix for bug where concentrator date/times jumps back and then forward again before the end of the file 
								affectedRowCount++;
								if (concDatesDataBlock.get(rowPointer)-concDatesDataBlock.get(lastOKRow) == 60000*(affectedRowCount)){
									logWindow.println("Fixed");
									Ok = true;
									fixing = false;
									lastOKRow = 0;
									affectedRowCount = 0;
								}
								else{
									long lastOKTime = concDatesDataBlock.get(lastOKRow);
									long newDateTime = lastOKTime+(60000*affectedRowCount);
									concDatesDataBlock.set(rowPointer,newDateTime);
									if (j>=concDataBlockDelimiters[0]+concDataBlockDelimiters[1]-24){
										fixing = false;
										lastOKRow = 0;
										affectedRowCount = 0;
										logWindow.println("Failed");
									}
								}
							}
						}
					}catch(ParseException pE){
						pE.printStackTrace();
						logWindow.println("ERROR: Invalid start time \""+byteToHex(fileArray[j+3])+":"+byteToHex(fileArray[j+4])+":"+byteToHex(fileArray[j+5])+"\" for file: "+fileList.get(filePointer).getName()+". File will be ignored.");
						Ok=false;
						break;
					}
				}
				else{
					logWindow.println("ERROR: Invalid start time \""+byteToHex(fileArray[j+3])+":"+byteToHex(fileArray[j+4])+":"+byteToHex(fileArray[j+5])+"\" for file: "+fileList.get(filePointer).getName()+". File will be ignored.");
					Ok=false;
					break;
				}
			}
			else{
				logWindow.println("ERROR: Unexpected data type in: "+fileList.get(filePointer).getName()+" at byte "+j+". File will be ignored.");
				Ok=false;
				break;
			}
			
			currentAddress = currentAddress + 12;
			if (currentAddress == 0x3FFF0){ //this means entering second segment of extended memory mode
				j=j+16; //chew through 16 bytes 
			}
			rowPointer++;
		}
		if (Ok==true){
			return concDatesDataBlock;
		}
		else {
			return new ArrayList<Long>();
		}
		
		
	}
	
	/*private ArrayList<GregorianCalendar> readConcDatesDataBlock(byte[] fileArray,int filePointer){ //Read Concentrator Data Block (Dates and Times)
		ArrayList<GregorianCalendar> concDatesDataBlock = new ArrayList<GregorianCalendar>();
		boolean Ok = true;
		
		for (int j=concDataBlockDelimiters[0]+18;j<concDataBlockDelimiters[0]+concDataBlockDelimiters[1]-12;j=j+12){
			int rowPointer = (j-18-concDataBlockDelimiters[0])/12; //operations on j to make it cycle through 0,1,2,3,4,5,6 as per datablock

			if (byteToHex(fileArray[j+0]).matches("\\d\\d") && byteToHex(fileArray[j+1]).matches("\\d\\d") && byteToHex(fileArray[j+2]).matches("\\d\\d") && byteToHex(fileArray[j+3]).matches("\\d\\d") && byteToHex(fileArray[j+4]).matches("\\d\\d") && byteToHex(fileArray[j+5]).matches("\\d\\d")){ //check that all date components are numbers		
				if (rowPointer!=0 || (rowPointer == 0 && ((writeToDatabase && byteToHex(fileArray[j+5]).equals("00")) || !writeToDatabase))){ //file must start with a valid time ie. xx:xx:00 if writing to database
					//if (rowPointer == 0 && byteToHex(fileArray[j+5]).equals("00")==false){
					//	System.out.println(fileArray[j+5]);
					//}
					
					int rowYear = Integer.parseInt(byteToHex(fileArray[j+0]));
					int rowMon = Integer.parseInt(byteToHex(fileArray[j+1]));
					int rowDay = Integer.parseInt(byteToHex(fileArray[j+2]));
					int rowHour = Integer.parseInt(byteToHex(fileArray[j+3]));
					int rowMin = Integer.parseInt(byteToHex(fileArray[j+4]));
					int rowSec = Integer.parseInt(byteToHex(fileArray[j+5]));
					//if (rowSec!=0){rowSec = 0;} //shoudln't need this, only for testing...
					if (rowYear>70 && rowYear<99){rowYear=+1900;} else if (rowYear<=70){rowYear+=2000;}
				    
					
					
					GregorianCalendar concDatesDataBlock_TEMP = new GregorianCalendar();
					concDatesDataBlock_TEMP.setTimeZone(TimeZone.getTimeZone("GMT+10"));
					concDatesDataBlock_TEMP.set(rowYear,rowMon-1,rowDay,rowHour,rowMin,rowSec);
					concDatesDataBlock_TEMP.set(Calendar.MILLISECOND, 0);
					concDatesDataBlock.add(rowPointer,concDatesDataBlock_TEMP);
					
					if (rowPointer>0 && writeToDatabase){ //ignore first row of file
						long dateDiff = concDatesDataBlock.get(rowPointer).getTimeInMillis()-concDatesDataBlock.get(rowPointer-1).getTimeInMillis();
						if (dateDiff!=60000 && fixing == false){
							if (dateDiff > 0 && dateDiff < 60000 && concDatesDataBlock.get(rowPointer).get(Calendar.SECOND)!=0 && concDatesDataBlock.get(rowPointer).getTimeInMillis()-concDatesDataBlock.get(rowPointer-(repeatRecordCount+1)).getTimeInMillis()<60000){
								GregorianCalendar offendingRow =  concDatesDataBlock.get(rowPointer);
								offendingRow.setTimeZone(TimeZone.getTimeZone("GMT+10"));
								String gapTime = dateFormatter.format(offendingRow.getTimeInMillis());
								logWindow.println("WARNING: found gap of < 1 min at "+gapTime+" in file: "+fileList.get(filePointer).getName()+". Row will not be processed into database.");
								repeatRecordCount++;
								Ok = false;
							}
							else if (dateDiff > 0 && dateDiff < 60000 && concDatesDataBlock.get(rowPointer).get(Calendar.SECOND)==0 && concDatesDataBlock.get(rowPointer).getTimeInMillis()-concDatesDataBlock.get(rowPointer-(repeatRecordCount+1)).getTimeInMillis()==60000){
								GregorianCalendar offendingRow = concDatesDataBlock.get(rowPointer);
								offendingRow.setTimeZone(TimeZone.getTimeZone("GMT+10"));
								String gapTime = dateFormatter.format(offendingRow.getTimeInMillis());
								logWindow.println("INFO: data returns to normal at "+gapTime+" in file: "+fileList.get(filePointer).getName()+".");
								repeatRecordCount = 0;
								Ok = true;
							}
							else {//fix for bug where concentrator date/times jumps back and then forward again before the end of the file 
								lastOKRow = rowPointer-1;
								GregorianCalendar offendingRow = concDatesDataBlock.get(lastOKRow);
								offendingRow.setTimeZone(TimeZone.getTimeZone("GMT+10"));
								String gapTime = dateFormatter.format(offendingRow.getTimeInMillis());
								logWindow.printString("WARNING: found gap of "+(dateDiff/60/1000)+" mins at "+gapTime+" in file: "+fileList.get(filePointer).getName()+". Attempting to fix gap...");
								GregorianCalendar newDateTime = new GregorianCalendar();
								newDateTime.setTimeZone(TimeZone.getTimeZone("GMT+10"));
								newDateTime.setTimeInMillis(offendingRow.getTimeInMillis()+60000);
								concDatesDataBlock.set(rowPointer,newDateTime);
								fixing = true;
								Ok = false;
								affectedRowCount=1;
							}
						}
						else if (fixing == true){ //fix for bug where concentrator date/times jumps back and then forward again before the end of the file 
							affectedRowCount++;
							if (concDatesDataBlock.get(rowPointer).getTimeInMillis()-concDatesDataBlock.get(lastOKRow).getTimeInMillis() == 60000*(affectedRowCount)){
								logWindow.println("Fixed");
								Ok = true;
								fixing = false;
								lastOKRow = 0;
								affectedRowCount = 0;
							}
							else{
								GregorianCalendar lastRow = concDatesDataBlock.get(lastOKRow);
								long lastOKTime = lastRow.getTimeInMillis();
								GregorianCalendar newDateTime = new GregorianCalendar();
								newDateTime.setTimeZone(TimeZone.getTimeZone("GMT+10"));
								newDateTime.setTimeInMillis(lastOKTime+(60000*affectedRowCount));
								concDatesDataBlock.set(rowPointer,newDateTime);
								if (j>=concDataBlockDelimiters[0]+concDataBlockDelimiters[1]-24){
									fixing = false;
									lastOKRow = 0;
									affectedRowCount = 0;
									logWindow.println("Failed");
								}
							}
						}
					}
				}
				else{
					logWindow.println("ERROR: Invalid start time \""+byteToHex(fileArray[j+3])+":"+byteToHex(fileArray[j+4])+":"+byteToHex(fileArray[j+5])+"\" for file: "+fileList.get(filePointer).getName()+". File will be ignored.");
					Ok=false;
					break;
				}
			}
			else{
				logWindow.println("ERROR: Unexpected data type in: "+fileList.get(filePointer).getName()+" at byte "+j+". File will be ignored.");
				Ok=false;
				break;
			}
		}
		if (Ok==true){
			return concDatesDataBlock;
		}
		else {
			return new ArrayList<GregorianCalendar>();
		}
	}*/
	
	private double[][] readConcPhasesDataBlock(byte[] fileArray,int filePointer,int concDataBlockRows){ //Read Concentrator Data Block (Phases)
		
		double[][] concPhasesDataBlock = new double[3][concDataBlockRows];
		
		int currentAddress = Integer.parseInt(byteToHex(fileArray[concDataBlockDelimiters[0]+13])+""+byteToHex(fileArray[concDataBlockDelimiters[0]+12])+""+byteToHex(fileArray[concDataBlockDelimiters[0]+11])+""+byteToHex(fileArray[concDataBlockDelimiters[0]+10]),16);
		
		int rowPointer = 0;
		for (int j=concDataBlockDelimiters[0]+18;j<concDataBlockDelimiters[0]+concDataBlockDelimiters[1]-12;j=j+12){	
			//int rowPointer = (j-18-concDataBlockDelimiters[0])/12; //operations on j to make it cycle through 0,1,2,3,4,5,6 as per datablock

			byte U11 = (byte) (makeUnsigned(fileArray[j+6]) - ((makeUnsigned(fileArray[j+6]) >> 2) << 2));
			byte U21 = (byte) (makeUnsigned(fileArray[j+8]) - ((makeUnsigned(fileArray[j+8]) >> 2) << 2));
			byte U31 = (byte) (makeUnsigned(fileArray[j+10]) - ((makeUnsigned(fileArray[j+10]) >> 2) << 2));
			int NrSamp = U11 | (U21 << 2) | (U31 << 4);
			int U1 = makeUnsigned(fileArray[j+6]) | (makeUnsigned(fileArray[j+7]) << 8);
			int U2 = makeUnsigned(fileArray[j+8]) | (makeUnsigned(fileArray[j+9]) << 8);
			int U3 = makeUnsigned(fileArray[j+10]) | (makeUnsigned(fileArray[j+11]) << 8);
			double V1  =  (double) Math.round(Math.sqrt((44.74 / NrSamp * (U1  &  0xFFFC)))*10)/(double)10;
			double V2  =  (double) Math.round(Math.sqrt((44.74 / NrSamp * (U2  &  0xFFFC)))*10)/(double)10;
			double V3  =  (double) Math.round(Math.sqrt((44.74 / NrSamp * (U3  &  0xFFFC)))*10)/(double)10;

			//double[] concPhasesDataBlock_TEMP = {V1,V2,V3};
			concPhasesDataBlock[0][rowPointer] = V1;
			concPhasesDataBlock[1][rowPointer] = V2;
			concPhasesDataBlock[2][rowPointer] = V3;
			//concPhasesDataBlock.add(rowPointer,concPhasesDataBlock_TEMP);

			currentAddress = currentAddress + 12;
			if (currentAddress == 0x3FFF0){ //this means entering second segment of extended memory mode
				j=j+16; //chew through 16 bytes 
			}
			rowPointer++;
		}

		return concPhasesDataBlock;
	}
	
	private double[][] makeCTModDataBlocks(byte[] fileArray,ArrayList<String[]> ctModHeaderValues,int concDataBlockRows){
		double[][] ctModDataBlocks = new double[6*ctModDataBlockDelimiters.size()][concDataBlockRows];
	
		for (int i=0;i<ctModDataBlockDelimiters.size();i++){ //cycle through CT modules
			
			int currentAddress = Integer.parseInt(byteToHex(fileArray[ctModDataBlockDelimiters.get(i)[0]+13])+""+byteToHex(fileArray[ctModDataBlockDelimiters.get(i)[0]+12])+""+byteToHex(fileArray[ctModDataBlockDelimiters.get(i)[0]+11])+""+byteToHex(fileArray[ctModDataBlockDelimiters.get(i)[0]+10]),16);
			//TODO ONLY NEED THESE FOR MINI CLAMP FIX, MAY BE ABLE TO DELETE IN FUTURE
			int nbData = (((makeUnsigned(fileArray[concHeaderDelimiters[0]+29]) << 8) << 8) << 8) | ((makeUnsigned(fileArray[concHeaderDelimiters[0]+28]) << 8) << 8) | (makeUnsigned(fileArray[concHeaderDelimiters[0]+27]) << 8) | makeUnsigned(fileArray[concHeaderDelimiters[0]+26]); //NbData
			int multiples = (int) Math.floor((nbData & 0xFFFFFF00)/12);
			int tripPoint = 12*multiples;
			
			int rowPointer = 0;
			for (int j=ctModDataBlockDelimiters.get(i)[0]+18;j<ctModDataBlockDelimiters.get(i)[0]+ctModDataBlockDelimiters.get(i)[1]-12;j=j+12){
				
				if (!needMiniClampFix || (needMiniClampFix && currentAddress < tripPoint)){
				
					//int rowPointer = (j-18-ctModDataBlockDelimiters.get(i)[0])/12; //operations on j to make it cycle through 0,1,2,3,4,5,6 as per datablock
					for (int k=0;k<6;k++){
						String[] MULs = ctModHeaderValues.get(i)[4].split(",");
						String[] DIVs = ctModHeaderValues.get(i)[5].split(",");
						Float MUL = Float.parseFloat(MULs[k]);
						Float DIV = Float.parseFloat(DIVs[k]);
						byte[] PL = {fileArray[j+0],fileArray[j+2],fileArray[j+4],fileArray[j+6],fileArray[j+8],fileArray[j+10]};		           				
						byte[] PM = {fileArray[j+1],fileArray[j+3],fileArray[j+5],fileArray[j+7],fileArray[j+9],fileArray[j+11]};
						double Pk = (makeUnsigned(PL[k]) | (PM[k] << 8));
						//if (Pk>-5 && Pk<5){
						//	Pk=0;
						//}
						double actualMeasurement = (double)Math.round((Pk*MUL/DIV)*10)/(double)10;
						ctModDataBlocks[(i*6)+k][rowPointer] = actualMeasurement;
					}
				}
				else{
					for (int k=0;k<6;k++){
						ctModDataBlocks[(i*6)+k][rowPointer] = -123.456;
					}
				}
				
				currentAddress = currentAddress + 12;
				if (currentAddress == 0x3FFF0){ //this means entering second segment of extended memory mode
					j=j+16; //chew through 16 bytes 
				}
				rowPointer++;
			}
		}
		return ctModDataBlocks;
	}
	
	private double[][] makeWModDataBlocks(byte[] fileArray,ArrayList<String[]> wModHeaderValues,int concDataBlockRows){
		double[][] wModDataBlocks = new double[6*wModHeaderValues.size()][concDataBlockRows];
	
		for (int i=0;i<wModDataBlockDelimiters.size();i++){ //cycle through Wireless modules
			int currentAddress = Integer.parseInt(byteToHex(fileArray[wModDataBlockDelimiters.get(i)[0]+13])+""+byteToHex(fileArray[wModDataBlockDelimiters.get(i)[0]+12])+""+byteToHex(fileArray[wModDataBlockDelimiters.get(i)[0]+11])+""+byteToHex(fileArray[wModDataBlockDelimiters.get(i)[0]+10]),16);

			int rowPointer = 0;
			for (int j=wModDataBlockDelimiters.get(i)[0]+18;j<wModDataBlockDelimiters.get(i)[0]+wModDataBlockDelimiters.get(i)[1]-12;j=j+12){
				//int rowPointer = (j-18-wModDataBlockDelimiters.get(i)[0])/12; //operations on j to make it cycle through 0,1,2,3,4,5,6 as per datablock
				String[] chs = wModHeaderValues.get(i)[5].split(",");
				int DIV = 1;
				byte[] DL = {fileArray[j+0],fileArray[j+2],fileArray[j+4],fileArray[j+6],fileArray[j+8],fileArray[j+10]};		           				
				byte[] DM = {fileArray[j+1],fileArray[j+3],fileArray[j+5],fileArray[j+7],fileArray[j+9],fileArray[j+11]};
				for (int k=0;k<6;k++){
   					
   					int chNo = Integer.parseInt(chs[k]);
   					switch (chNo){
	   					case 0: DIV=100; break;
	   					case 1: DIV=100; break;
	   					case 2: DIV=100; break;
	   					case 3: DIV=100; break;
	   					case 4: DIV=10; break;
	   					case 5: DIV=10; break;
	   					case 7: DIV=100; break;
	   					case 90: DIV=10; break;
	   					case 91: DIV=10; break;
	   					default: DIV=1;
					}
   					
   					double measuredValue;
   					
   					if ((byteToHex(DM[k])+""+byteToHex(DL[k])).equals("8000") || (byteToHex(DM[k])+""+byteToHex(DL[k])).equals("7ffe")){
   						measuredValue = (double) -123.456; //ERROR CODE
   						if (rowPointer > 0 && wModDataBlocks[(6*i)+k][rowPointer-1]*DIV >= 65535){ //if previous block contains an error
   							int fixCount = 1;
   							while (rowPointer-fixCount >= 0 && wModDataBlocks[(6*i)+k][rowPointer-fixCount]*DIV >= 65535){
   								wModDataBlocks[(6*i)+k][rowPointer-fixCount] = (double) -123.456; //ERROR CODE
   								fixCount++;
   							}
   						}
   					}
   					else if ((byteToHex(DM[k])+""+byteToHex(DL[k])).equals("ffff")==false && rowPointer > 0 && wModDataBlocks[(6*i)+k][rowPointer-1]*DIV >= 65535){
   						double Dk = (makeUnsigned(DL[k]) | makeUnsigned(DM[k]) <<8);
       					measuredValue = Dk/DIV;
       					measuredValue = (double)Math.round(measuredValue*10)/(double)10;
       					int fixCount = 1;
						while (rowPointer-fixCount >= 0 && wModDataBlocks[(6*i)+k][rowPointer-fixCount]*DIV >= 65535){ 
							fixCount++;
						}
						if (rowPointer-fixCount >=0){ //if haven't reached the start of file
							if (wModDataBlocks[(6*i)+k][rowPointer-fixCount] == -123.456 || (measuredValue*DIV <= 65500 && wModDataBlocks[(6*i)+k][rowPointer-fixCount]*DIV <= 65500) || (measuredValue*DIV > 65500 && wModDataBlocks[(6*i)+k][rowPointer-fixCount]*DIV > 65500) || (measuredValue*DIV > 65500 && wModDataBlocks[(6*i)+k][rowPointer-fixCount]*DIV <= 65500)){
								for (int m=1;m<fixCount;m++){
									wModDataBlocks[(6*i)+k][rowPointer-m] = (double) -123.456; //ERROR CODE
								}
							}
							else{
								//assume legitimate
								//TODO more robust testing?
							}
						}
						else{ //backfill to start of file
							for (int m=1;rowPointer-m>=0 && wModDataBlocks[(6*i)+k][rowPointer-m]*DIV >= 65535;m++){
								wModDataBlocks[(6*i)+k][rowPointer-m] = (double) -123.456; //ERROR CODE
							}
						}
   					}
   					else if ((byteToHex(DM[k])+""+byteToHex(DL[k])).equals("ffff") && j>=wModDataBlockDelimiters.get(i)[0]+wModDataBlockDelimiters.get(i)[1]-24){ //if final value in file is a 65535
   						measuredValue = (double) -123.456; //ERROR CODE
   						int fixCount = 1;
						while (rowPointer-fixCount >= 0 && wModDataBlocks[(6*i)+k][rowPointer-fixCount]*DIV >= 65535){
							wModDataBlocks[(6*i)+k][rowPointer-fixCount] = (double) -123.456; //ERROR CODE
							fixCount++;
						}
   					}
   					else{
       					double Dk = (makeUnsigned(DL[k]) | makeUnsigned(DM[k]) <<8);
       					measuredValue = Dk/DIV;
       					measuredValue = (double)Math.round(measuredValue*10)/(double)10;
   					}
   					
   					wModDataBlocks[(6*i)+k][rowPointer] = measuredValue;
       				
       				//(j-18-dataBlocks.get(i)[0])/12 = 0 to total number of data rows in file ie. 60 for 1 hour
        			//9+(6*(Integer.parseInt(concValues[5])-1))+(6*Integer.parseInt(dataValues[i][3]))+k = horizontal position in Array accounting for dates, voltages, modules and wireless modules
        			//fileArray[j+k] = position in file
    			}
				
				currentAddress = currentAddress + 12;
				if (currentAddress == 0x3FFF0){ //this means entering second segment of extended memory mode
					j=j+16; //chew through 16 bytes 
				}
				rowPointer++;
			}
		}
		return wModDataBlocks;
	}

	
	private static int makeUnsigned(byte signedByte){
		int unsignedByte;
		if ((int)signedByte < 0){
			unsignedByte = (int) (signedByte & 0xff);
		}
		else{
			unsignedByte = signedByte;
		}
		return unsignedByte;
	}
	
	
	private static int bytesToInt(byte lsb, byte msb){
		int newInt = (int)(makeUnsigned(msb) << 8) | makeUnsigned(lsb);
		return newInt;
	}
		
		
	//method to determine period length of concentrator in seconds
	private static String getPeriod(int pPointer){
		String[] periodArray = {"1","2","5","10","15","20","30","60","600","3600","120","300"};
		String periodLength = periodArray[pPointer];
		return periodLength;
	}
	
	
	
	//method to convert a byte to a hex string.	
	private static String byteToHex(byte data) {
	
		StringBuffer buf = new StringBuffer();
		buf.append(toHexChar((data >>> 4) & 0x0F));
		buf.append(toHexChar(data & 0x0F));
		
		return buf.toString();
	}
	
	
	//Convenience method to convert an int to a hex char.
	private static char toHexChar(int i) {
		if ((0 <= i) && (i <= 9)) {
			return (char) ('0' + i);
		} else {
			return (char) ('a' + (i - 10));
		}
	}
	
	private int getInterval(ArrayList<Long> dates,String fileName){ //finds a block of 50 consistent consecutive date intervals and sets interval
		int interval = 0;
		
		double workingInterval = 0;
		int consecutive = 0;
		
		for (int i=0;interval == 0 && i<dates.size()-2;i++){
			workingInterval = (i*workingInterval + dates.get(i+1) - dates.get(i))/(i+1);
			if ((dates.get(i+1) - dates.get(i))==workingInterval){
				consecutive++;
			}
			else{
				consecutive = 1;
				workingInterval = (dates.get(i+1) - dates.get(i));
			}
			if (consecutive >= 10 || consecutive >= dates.size()/2){
				interval = (int)Math.round(workingInterval)/1000;
				logWindow.println("Interval determined for file: "+fileName+" (Interval="+interval+" seconds)");
			}
			else if (i>500 && interval == 0){ //if gone through 500 and still no consensus
				logWindow.println("Problem encountered while processing file: "+fileName+".\r\nNo interval was able to be determined.\r\nFile will not be processed at this stage.");
				interval = 0;
				break;
			}
		}
		
		return interval;
	}
	
	private String getTimeString(long timeInMillis){
		String timeString = "";
		long remainder = timeInMillis;
		int hours = (int)Math.floor(remainder/(1000*60*60));
		remainder = remainder - hours*(1000*60*60);
		int mins = (int)Math.floor(remainder/(1000*60));
		remainder = remainder - mins*(1000*60);
		double secs = (double)remainder/1000;
		if (hours>0){
			if (hours == 1){
				timeString = timeString.concat("1 hour");
			}
			else{
				timeString = timeString.concat(hours+" hours");
			}
		}
		if (hours>0 && mins>0){
			timeString = timeString.concat(", ");
		}
		if (mins>0){
			if (mins == 1){
				timeString = timeString.concat("1 minute");
			}
			else{
				timeString = timeString.concat(mins+" minutes");
			}
		}
		if (hours>0 || mins>0){
			timeString = timeString.concat(" and ");
		}
		timeString = timeString.concat(secs+" seconds");
		return timeString;
	}
	
	private String getSourceType(String chName, String sensorCh){
		String sourceType = "";
		//determine appliance type
		if (chName.startsWith("A")){sourceType = "Appliance";}
		else if (chName.startsWith("G")){sourceType = "Gas";}
		else if (chName.startsWith("H") || sensorCh.equals("01")){sourceType = "Humidity";}
		else if (chName.startsWith("L")){sourceType = "Light";}
		else if (chName.startsWith("M")){sourceType = "Motion";}
		else if (chName.startsWith("T") || sensorCh.equals("00")){sourceType = "Temperature";}
		else if (chName.startsWith("W")){sourceType = "Water";}
		return sourceType;
	}
	
	private String getMeasurementType(String sensorCh){
		String measurementType = "";
		//determine metering type
		if (sensorCh.equals("00")){measurementType = "Temp";} //0 : temperature
		else if (sensorCh.equals("01")){measurementType = "Humidity";} //1 : humidity
		else if (sensorCh.equals("02")){measurementType = "Volts";} //2 : volts
		else if (sensorCh.equals("03")){measurementType = "Amps";} //3 : amps
		else if (sensorCh.equals("04")){measurementType = "ActPower";} //4 : active power
		else if (sensorCh.equals("05")){measurementType = "AppPower";} //5 : apparent power
		else if (sensorCh.equals("06")){measurementType = "LightLevel";} //6 : light level
		else if (sensorCh.equals("07")){measurementType = "AvgTemp";} //7 : average temperature
		else if (sensorCh.equals("80")){measurementType = "Pulse";} //80 : pulse
		else if (sensorCh.equals("81")){measurementType = "OnTime";} //81 : on time
		else if (sensorCh.equals("82")){measurementType = "";} //82 : RSSI to (for test purposes)
		else if (sensorCh.equals("83")){measurementType = "";} //83 : RSSI to (for test purposes)
		else if (sensorCh.equals("84")){measurementType = "";} //84 : Battery  (for test purposes)
		else if (sensorCh.equals("90")){measurementType = "ActEnergy";} //90 : Active Energy
		else if (sensorCh.equals("91")){measurementType = "AppEnergy";} //91 : Apparent Energy
		return measurementType;

	}
	
	private byte[] readBytesIntoArray(File fileToProcess) throws IOException{
		InputStream inputStream = new FileInputStream(fileToProcess);
		//logWindow.println("Processing File:" + fileToProcess.getName());
		
		// Get the size of the file
		long length = fileToProcess.length();
		//logWindow.println("Length of " + fileToProcess.getName() + " is " + length);
		
		/*
		 * You cannot create an array using a long type. It needs to be an int
		 * type. Before converting to an int type, check to ensure that file is
		 * not larger than Integer.MAX_VALUE;
		 */
		if (length > Integer.MAX_VALUE) {
			logWindow.println("Cannot process file "+fileToProcess.getName().toString()+" as it is too large.");
			inputStream.close();
			return null;
		}
		
		// Create the byte array to hold the data
		byte[] bytes = new byte[(int)length];
		
		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while ((offset < bytes.length) && ((numRead=inputStream.read(bytes, offset, bytes.length-offset)) >= 0)) {
			offset += numRead;
		}
		
		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			logWindow.println("Could not completely read file " + fileToProcess.getName()+". File will be ignored.");
			inputStream.close();
			throw new IOException("Could not completely read file " + fileToProcess.getName());
		}
		
		inputStream.close();
		return bytes;
	
	}
	
	private void writeHeaderLog(String siteID,String formStartDateTime,String[] concHeaderValues,ArrayList<String[]> ctModHeaderValues,ArrayList<String[]> wModHeaderValues) throws SQLException{		
		String[] headerArray = new String[17];
		
		//Concentrator Module headers
		headerArray[0] = concHeaderValues[0]; //Conc. Name
		headerArray[1] = concHeaderValues[1]; //Conc. SN
		headerArray[2] = concHeaderValues[3]; //Conc. Period
		headerArray[3] = concHeaderValues[5]; //Conc. Number Of Modules
		headerArray[4] = concHeaderValues[6]; //Conc. Version
		headerArray[5] = concHeaderValues[7]; //Conc. WinterTime
		
		//CT Module Headers
		String ctSNs = "";
		String ctVers = "";
		String ctChNames = "";
		String ctMuls = "";
		String ctDivs = "";
		String ctPhases = "";
		for (int i=0;i<ctModHeaderValues.size();i++){
			ctSNs += ctModHeaderValues.get(i)[0];
			ctVers += ctModHeaderValues.get(i)[2];
			ctChNames += ctModHeaderValues.get(i)[3];
			ctMuls += ctModHeaderValues.get(i)[4];
			ctDivs += ctModHeaderValues.get(i)[5];
			ctPhases += ctModHeaderValues.get(i)[6];
			if (i<ctModHeaderValues.size()-1){
				ctSNs += ",";
				ctVers += ",";
				ctChNames += ",";
				ctMuls += ",";
				ctDivs += ",";
				ctPhases += ",";
			}
		}
		headerArray[6] = ctSNs; //CT SNs
		headerArray[7] = ctVers; //CT Versions
		headerArray[8] = ctChNames; //CT Channel Names
		headerArray[9] = ctMuls; //CT Muls
		headerArray[10] = ctDivs; //CT Divs
		headerArray[11] = ctPhases; //CT Phases
		
		
		//Wireless Module Headers
		String wSN = "";
		String wVer = "";
		String wChNames = "";
		String wSensorSNs = "";
		String wSensorChs = "";
		wSN += wModHeaderValues.get(0)[0];
		wVer += wModHeaderValues.get(0)[2];
		for (int i=0;i<wModHeaderValues.size();i++){
			wChNames += wModHeaderValues.get(i)[3];
			wSensorSNs += wModHeaderValues.get(i)[4];
			wSensorChs += wModHeaderValues.get(i)[5];
			if (i<wModHeaderValues.size()-1){
				wChNames += ",";
				wSensorSNs += ",";
				wSensorChs += ",";
			}
		}
		headerArray[12] = wSN; //WL SN
		headerArray[13] = wVer; //WL Version
		headerArray[14] = wChNames; //WL Channel Names
		headerArray[15] = wSensorSNs; //WL Sensor SNs
		headerArray[16] = wSensorChs; //WL Sensor Chs

		Statement headerStatement = dbConn.createStatement();
		String headerCheckSQL =  "SELECT * FROM header_log WHERE date_time = (SELECT MAX(date_time) FROM header_log WHERE date_time <= '"+formStartDateTime+"' AND site_id = "+siteID+") AND site_id = "+siteID;
		ResultSet headerCheckRS = headerStatement.executeQuery(headerCheckSQL);
		//System.out.println(headerCheckSQL);
		if (headerCheckRS.next()){ //if a header with a date preceding the start of the current file in in the database
			String[] headerArray_CHECK = new String[] {headerCheckRS.getString("conc_name"),headerCheckRS.getString("conc_sn"),headerCheckRS.getString("conc_period"),headerCheckRS.getString("conc_nbmod"),headerCheckRS.getString("conc_version"),headerCheckRS.getString("conc_winter"),headerCheckRS.getString("ct_sns"),headerCheckRS.getString("ct_versions"),headerCheckRS.getString("ct_ch_names"),headerCheckRS.getString("ct_muls"),headerCheckRS.getString("ct_divs"),headerCheckRS.getString("ct_phases"),headerCheckRS.getString("wl_sn"),headerCheckRS.getString("wl_version"),headerCheckRS.getString("wl_ch_names"),headerCheckRS.getString("wl_sensor_sns"),headerCheckRS.getString("wl_sensor_chs")};
			if (Arrays.equals(headerArray, headerArray_CHECK)){ //if current header is equal to most recent header for this site_id in the database 
				//Do nothing because no change has occurred since the last header logged
				//System.out.println("Do Nothing");
			}
			else { //if current header differs from most recent header
				//TODO CHECK IF DATES ARE EQUAL

				// check to see if subsequent header is the same as this one, if so shift date, if not, add new record
				headerCheckSQL =  "SELECT * FROM header_log WHERE date_time = (SELECT MIN(date_time) FROM header_log WHERE date_time > '"+formStartDateTime+"' AND site_id = "+siteID+") AND site_id = "+siteID;
				ResultSet headerCheckAfterRS = headerStatement.executeQuery(headerCheckSQL);
				//System.out.println(headerCheckSQL);

				if (headerCheckAfterRS.next()){ //if a header with a date_time after the start of the current file in in the database
					headerArray_CHECK = new String[] {headerCheckAfterRS.getString("conc_name"),headerCheckAfterRS.getString("conc_sn"),headerCheckAfterRS.getString("conc_period"),headerCheckAfterRS.getString("conc_nbmod"),headerCheckAfterRS.getString("conc_version"),headerCheckAfterRS.getString("conc_winter"),headerCheckAfterRS.getString("ct_sns"),headerCheckAfterRS.getString("ct_versions"),headerCheckAfterRS.getString("ct_ch_names"),headerCheckAfterRS.getString("ct_muls"),headerCheckAfterRS.getString("ct_divs"),headerCheckAfterRS.getString("ct_phases"),headerCheckAfterRS.getString("wl_sn"),headerCheckAfterRS.getString("wl_version"),headerCheckAfterRS.getString("wl_ch_names"),headerCheckAfterRS.getString("wl_sensor_sns"),headerCheckAfterRS.getString("wl_sensor_chs")};
					int subs_sub_period_id = headerCheckAfterRS.getInt("sub_period_id");
					if (Arrays.equals(headerArray, headerArray_CHECK)){ //
						//shift date of header already in database so that is covers the data within the current file as well 
						String subs_date_time = headerCheckAfterRS.getTimestamp("date_time").toString().split("[.]")[0];
						String updateHeaderSQL = "UPDATE header_log SET date_time = '"+formStartDateTime+"' WHERE site_id = "+siteID+" AND sub_period_id = "+subs_sub_period_id+" AND date_time = '"+subs_date_time+"'";
						//System.out.println(updateHeaderSQL);
						headerStatement.executeUpdate(updateHeaderSQL);
					}
					else { // current header is different to subsequent header
						//shift the sub_period_id of all subsequent headers
						String shiftSubPeriodsSQL = "UPDATE header_log SET sub_period_id = sub_period_id + 1 WHERE site_id = "+siteID+" AND sub_period_id >= "+subs_sub_period_id+"";
						//System.out.println(shiftSubPeriodsSQL);					
						headerStatement.executeUpdate(shiftSubPeriodsSQL);

						String insertHeaderSQL = "INSERT INTO header_log (site_id,sub_period_id,date_time,conc_name,conc_sn,conc_period,conc_nbmod,conc_version,conc_winter,ct_sns,ct_versions,ct_ch_names,ct_muls,ct_divs,ct_phases,wl_sn,wl_version,wl_ch_names,wl_sensor_sns,wl_sensor_chs) VALUES("+siteID+","+subs_sub_period_id+",'"+formStartDateTime+"','"+headerArray[0]+"','"+headerArray[1]+"','"+headerArray[2]+"','"+headerArray[3]+"','"+headerArray[4]+"','"+headerArray[5]+"','"+headerArray[6]+"','"+headerArray[7]+"','"+headerArray[8]+"','"+headerArray[9]+"','"+headerArray[10]+"','"+headerArray[11]+"','"+headerArray[12]+"','"+headerArray[13]+"','"+headerArray[14]+"','"+headerArray[15]+"','"+headerArray[16]+"')";
						//System.out.println(insertHeaderSQL);	
						headerStatement.executeUpdate(insertHeaderSQL);
					}
				}
				else{ //if no subsequent records exist for this installation
					String insertHeaderSQL = "INSERT INTO header_log (site_id,sub_period_id,date_time,conc_name,conc_sn,conc_period,conc_nbmod,conc_version,conc_winter,ct_sns,ct_versions,ct_ch_names,ct_muls,ct_divs,ct_phases,wl_sn,wl_version,wl_ch_names,wl_sensor_sns,wl_sensor_chs) VALUES("+siteID+",(SELECT MAX(sub_period_id)+1 FROM (SELECT * FROM header_log) AS self WHERE site_id = "+siteID+"),'"+formStartDateTime+"','"+headerArray[0]+"','"+headerArray[1]+"','"+headerArray[2]+"','"+headerArray[3]+"','"+headerArray[4]+"','"+headerArray[5]+"','"+headerArray[6]+"','"+headerArray[7]+"','"+headerArray[8]+"','"+headerArray[9]+"','"+headerArray[10]+"','"+headerArray[11]+"','"+headerArray[12]+"','"+headerArray[13]+"','"+headerArray[14]+"','"+headerArray[15]+"','"+headerArray[16]+"')";
					//System.out.println(insertHeaderSQL);
					headerStatement.executeUpdate(insertHeaderSQL);
				}
				headerCheckAfterRS.close();
			}
		}
		else { // if no records dated prior or equal to the start of the current file in in the database
			headerCheckSQL =  "SELECT * FROM header_log WHERE date_time = (SELECT MIN(date_time) FROM header_log WHERE date_time > '"+formStartDateTime+"' AND site_id = "+siteID+") AND site_id = "+siteID;
			ResultSet headerCheckAfterRS = headerStatement.executeQuery(headerCheckSQL);
			if (headerCheckAfterRS.next()){ //if a header with a date_time after the start of the current file in in the database
				String[] headerArray_CHECK = new String[] {headerCheckAfterRS.getString("conc_name"),headerCheckAfterRS.getString("conc_sn"),headerCheckAfterRS.getString("conc_period"),headerCheckAfterRS.getString("conc_nbmod"),headerCheckAfterRS.getString("conc_version"),headerCheckAfterRS.getString("conc_winter"),headerCheckAfterRS.getString("ct_sns"),headerCheckAfterRS.getString("ct_versions"),headerCheckAfterRS.getString("ct_ch_names"),headerCheckAfterRS.getString("ct_muls"),headerCheckAfterRS.getString("ct_divs"),headerCheckAfterRS.getString("ct_phases"),headerCheckAfterRS.getString("wl_sn"),headerCheckAfterRS.getString("wl_version"),headerCheckAfterRS.getString("wl_ch_names"),headerCheckAfterRS.getString("wl_sensor_sns"),headerCheckAfterRS.getString("wl_sensor_chs")};
				int subs_sub_period_id = headerCheckAfterRS.getInt("sub_period_id");
				if (Arrays.equals(headerArray, headerArray_CHECK)){ //
					//shift date of header already in database so that is covers the data within the current file as well 
					String subs_date_time = headerCheckAfterRS.getTimestamp("date_time").toString().split("[.]")[0];
					String updateHeaderSQL = "UPDATE header_log SET date_time = '"+formStartDateTime+"' WHERE site_id = "+siteID+" AND sub_period_id = "+subs_sub_period_id+" AND date_time = '"+subs_date_time;
					//System.out.println(updateHeaderSQL);	
					headerStatement.executeUpdate(updateHeaderSQL);
				}
				else { // current header is different to subsequent header
					//shift the sub_period_id of all subsequent headers
					String shiftSubPeriodsSQL = "UPDATE header_log SET sub_period_id = sub_period_id + 1 WHERE site_id = "+siteID+" AND sub_period_id >= "+subs_sub_period_id+"";
					//System.out.println(shiftSubPeriodsSQL);					
					headerStatement.executeUpdate(shiftSubPeriodsSQL);

					String insertHeaderSQL = "INSERT INTO header_log (site_id,sub_period_id,date_time,conc_name,conc_sn,conc_period,conc_nbmod,conc_version,conc_winter,ct_sns,ct_versions,ct_ch_names,ct_muls,ct_divs,ct_phases,wl_sn,wl_version,wl_ch_names,wl_sensor_sns,wl_sensor_chs) VALUES("+siteID+","+subs_sub_period_id+",'"+formStartDateTime+"','"+headerArray[0]+"','"+headerArray[1]+"','"+headerArray[2]+"','"+headerArray[3]+"','"+headerArray[4]+"','"+headerArray[5]+"','"+headerArray[6]+"','"+headerArray[7]+"','"+headerArray[8]+"','"+headerArray[9]+"','"+headerArray[10]+"','"+headerArray[11]+"','"+headerArray[12]+"','"+headerArray[13]+"','"+headerArray[14]+"','"+headerArray[15]+"','"+headerArray[16]+"')";
					//System.out.println(insertHeaderSQL);	
					headerStatement.executeUpdate(insertHeaderSQL);
				}
			}
			else{ //if no records exist for this installation
				String insertHeaderSQL = "INSERT INTO header_log (site_id,sub_period_id,date_time,conc_name,conc_sn,conc_period,conc_nbmod,conc_version,conc_winter,ct_sns,ct_versions,ct_ch_names,ct_muls,ct_divs,ct_phases,wl_sn,wl_version,wl_ch_names,wl_sensor_sns,wl_sensor_chs) VALUES("+siteID+",1,'"+formStartDateTime+"','"+headerArray[0]+"','"+headerArray[1]+"','"+headerArray[2]+"','"+headerArray[3]+"','"+headerArray[4]+"','"+headerArray[5]+"','"+headerArray[6]+"','"+headerArray[7]+"','"+headerArray[8]+"','"+headerArray[9]+"','"+headerArray[10]+"','"+headerArray[11]+"','"+headerArray[12]+"','"+headerArray[13]+"','"+headerArray[14]+"','"+headerArray[15]+"','"+headerArray[16]+"')";
				//System.out.println(insertHeaderSQL);
				headerStatement.executeUpdate(insertHeaderSQL);
			}
			headerCheckAfterRS.close();
		}
		headerCheckRS.close();
		headerStatement.close();
	}
	
	private class FatalIssueException extends Exception{
		private static final long serialVersionUID = -2455314237037953535L;
		
		String message = "";
		FatalIssueException(String message){
			this.message = message;
		}
		
		public String getMessage(){
			return message;
		}
	}
}
