package fileManagement;

import issueManagement.FileIssue;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import endUseWindow.LogWindow;
import endUseWindow.MySQLConnection;
import endUseWindow.Site;
import endUseWindow.Source;


public class FlexiValidator implements Runnable{
	
	private final Connection dbConn;
	private final MySQLConnection mySQLConnection;
	private final File[] flexiFiles;
	private final LogWindow logWindow;
	private final ArrayList<FileIssue> fileIssueList = new ArrayList<FileIssue>();
	private int frequency = 0;
	private String meterSerial = "";
	

	
	public FlexiValidator(MySQLConnection mySQLConnection,LogWindow logWindow,File[] flexiFiles){
		this.mySQLConnection = mySQLConnection;
		this.dbConn = mySQLConnection.getCopyConnection();
		this.flexiFiles = flexiFiles;
		this.logWindow = logWindow;
	}
	
	public void run(){
		for(int b=0;b<flexiFiles.length;b++){
			File flexiFile = flexiFiles[b];
			BufferedReader inputStream = null;
			try {
				inputStream = new BufferedReader(new FileReader(flexiFile));
				
				String line = "";
				meterSerial = "00000000";
				String testSiteName = flexiFile.getName().substring(0,flexiFile.getName().indexOf("-"));
				logWindow.println("Processing file: "+flexiFile.getName()+", using site name: "+testSiteName+")");
				line = inputStream.readLine();
				for(int i=0;line!=null && i<5;i++,line=inputStream.readLine()){
					if (line.matches("^Date	Time(	(.{1,20}))+$")) break;
					else if (line.matches("^Station-number	[^\\s]{1,16}$")){
						meterSerial = line.substring(line.lastIndexOf("	"), line.length());
					}
					else logWindow.println("Ignoring line: '"+ line +"'");
				}
				if(line.matches("^Date	Time(	(.{1,20}))+$")){
					logWindow.println("Found column headers: '"+ line +"'");
					Site site = null;
					try{
						String checkSiteNameSQL = "SELECT * FROM sites WHERE site_name = '"+testSiteName+"'";
						ResultSet checkSiteNameRS = dbConn.createStatement().executeQuery(checkSiteNameSQL);

						if (checkSiteNameRS.next()){
							Site testSite = new Site(checkSiteNameRS.getString("site_id"),checkSiteNameRS.getString("site_name"),checkSiteNameRS.getString("concentrator"),checkSiteNameRS.getString("start_date"),checkSiteNameRS.getString("end_date"),checkSiteNameRS.getString("given_name"),checkSiteNameRS.getString("surname"),checkSiteNameRS.getString("suburb"),checkSiteNameRS.getString("state"));
							if (testSite.isValid()){
								site = testSite;
							}
							else{
								site = null;
							}
						}
						else{
							site = new SiteNotFoundWindow(dbConn, logWindow).getNewSite(testSiteName);	
						}
					}catch(SQLException sE){
						site = null;
					}



					if (site!=null){ //if found a site ID
						LinkedList<DataFile> fileList = new LinkedList<DataFile>();
						String[] sources = line.replace("Date	Time	", "").split("	");
						for (int i=0;i<sources.length;i++){
							String sourceID = null;

							try{
								ResultSet sourceIDRS = dbConn.createStatement().executeQuery("SELECT source_id FROM sources WHERE site_id = "+site.getSiteID()+" AND source_name = '"+sources[i]+"'");

								if (sourceIDRS.next()){
									sourceID = sourceIDRS.getString("source_id");
								}
								else{// add new source
									String sourceType = "";
									String[] sourceTypes = new String[] {"Appliance","Circuit","Gas","Humidity","Light","Motion","Temperature","Water"};
									sourceType = (String)JOptionPane.showInputDialog(null, "Please provide a source type for channel: '"+sources[i]+".\r\nPlease select the appropriate type from the list:", "Select Source Type", JOptionPane.PLAIN_MESSAGE, null, sourceTypes, "Circuit");
									if(sourceType == null){sourceType = "";}
									String measurementType = "";
									String[] measurementTypes = new String[] {"ActEnergy","AppEnergy","OnTime","Temp","Humidity","Pulse","ActPower","AppPower","LightLevel","Volts","Amps","AvgTemp"};
									measurementType = (String)JOptionPane.showInputDialog(null, "Please provide a measurement type for channel: '"+sources[i]+".\r\nPlease select the appropriate type from the list:", "Select Source Type", JOptionPane.PLAIN_MESSAGE, null, measurementTypes, "ActPower");
									if(measurementType == null){measurementType = "";}
									if (!sourceType.equals("") && !measurementType.equals("")){
										logWindow.printString("Adding source '"+sources[i]+"' to database, as type: " + sourceType + ".");
										Statement mySQLStatement = dbConn.createStatement();
										Source testSource = new Source(site,null,sources[i],sourceType,measurementType);
										sourceID = Source.addSource(logWindow, true, mySQLStatement, site, testSource, flexiFile.getName(), true);

									}
									else{
										sourceID = null;
										logWindow.println("Error: source type not provided, no data will be written.");
									}
								}
							} catch(SQLException sE){
								sE.printStackTrace();
								sourceID = null;
								logWindow.println("Warning: unable to match specified source '"+sources[i]+"'");
							}
							if (sourceID!=null){
								DataFile dataFile = new DataFile();
								dataFile.siteID = site.getSiteID();
								dataFile.sourceID = sourceID;
								dataFile.fileName = flexiFile.getName();
								dataFile.file = flexiFile;
								fileList.add(dataFile);
							}
						}

						if (!fileList.isEmpty()){ //got some files	
							if (fileList.size()==sources.length){
								System.out.println("Added File");

								String fileDateFormat = getFileInfo(flexiFile.getName(),fileList.size(),inputStream);

								if (frequency!=0 && !meterSerial.equals("") && !fileDateFormat.equals("")){ //if got valid info
									try{
										inputStream.close();
										inputStream = new BufferedReader(new FileReader(flexiFile)); //restart file reader

										getValidData(fileList,flexiFile.getName(),fileList.size(),fileDateFormat,inputStream);
									} catch (IOException err){
										logWindow.println("Unable to read from file: "+flexiFile.getName().toString()+"\r\nNo data will be written from this file.");
										System.err.println("IOException: "+err.getMessage());
									} finally {
										try {
											inputStream.close();
										} catch (IOException err) {
											System.err.println("IOException: "+err.getMessage());
										}
									}
								}
								else{
									logWindow.println("Could not obtain vital information about file: "+flexiFile.getName());
									fileList.clear();
								}
							}
							else{
								logWindow.println("One or more header names were invalid. File: "+flexiFile.getName()+" will not be processed until this is recitifed.");
								fileList.clear();
							}
						}


						if (!fileList.isEmpty()){ //while still dataFiles left to process
							FileFeeder fileFeeder = new FileFeeder(mySQLConnection,logWindow, fileIssueList,true);
							Thread feederThread = new Thread(fileFeeder);
							feederThread.start();
							Date startTime = new Date();

							while(!fileList.isEmpty()){
								DataFile dataFile = fileList.poll();
								dataFile.frequency = frequency;
								dataFile.meterSerial = meterSerial;
								logWindow.println("Processing data from file: "+dataFile.fileName+"...");

								if (dataFile.dataList.size()>0){

									try{
										String fileExistsSQL = "SELECT * FROM files WHERE site_id = "+dataFile.siteID+" AND source_id = "+dataFile.sourceID+" AND file_name = '"+dataFile.fileName+"' AND meter_sn = '"+dataFile.meterSerial+"' AND frequency = "+dataFile.frequency;
										ResultSet fileExistsQuery = dbConn.createStatement().executeQuery(fileExistsSQL);
										if (fileExistsQuery.next()==false){ //if no files with same site,source,filename,meterserial and frequency exist
											fileFeeder.addFile(dataFile); //Send file to feeder
										}
										else{
											logWindow.println("This file already exists for source "+dataFile.sourceID+" at site "+dataFile.siteID+". File will be ignored.");	
										}


									} catch(SQLException sE){
										//TODO need error here
									}
								}
								else{
									logWindow.println("No data found for source "+dataFile.sourceID+" at site "+dataFile.siteID+".");	
								}
							}

							//TODO write fileErrors here somewhere
							synchronized(fileFeeder.fileList){
								fileFeeder.moreFilesComing = false;
								fileFeeder.fileList.notify();
							}
							try {
								feederThread.join();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							Date endTime = new Date();
							System.out.println("Time taken: "+getTimeString(endTime.getTime()-startTime.getTime()));
						}
						else{
							//no files to process
						}
					}
					else{
						logWindow.println("Could not find matching siteID in the database. File will not be processed at this stage.");
					}
				}
				else{
					logWindow.println("Unexpected file format. File will not be processed at this stage.");
					logWindow.println("Unable to find valid column header row within the first five lines of file.");
					logWindow.println("Expected column header line format is: Date	Time	xxxx........");
				}
			} catch (IOException err){
				logWindow.println("Unable to read from file: "+flexiFile.getName().toString()+"\r\nNo data will be written from this file.");
				System.err.println("IOException: "+err.getMessage());
			} finally {
				if (inputStream != null){
					try {
						inputStream.close();
					} catch (IOException err) {
						System.err.println("IOException: "+err.getMessage());
					}
				}
			}
		}
	}

	void getValidData(LinkedList<DataFile> fileList,String fileName,int sourceCount,String fileDateFormat,BufferedReader inputStream) throws IOException {	

		SimpleDateFormat dateTranslator = new SimpleDateFormat(fileDateFormat+" HH:mm:ss");
		dateTranslator.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		if (fileDateFormat.equals("")==false){
			String line = "";
			while ((line = inputStream.readLine()) != null) {
				String[] splitline = line.split("	"); //;split by tabs

				for (int i=0;i<splitline.length;i++){
					splitline[i] = splitline[i].trim();
				}
				
				String dateTime = splitline[0]+" "+splitline[1];
				
				if (splitline.length==sourceCount+2 && Pattern.matches("^((\\d{1,2}/\\d{1,2}/\\d\\d(\\d\\d){0,1})|(\\d\\d(\\d\\d){0,1}/\\d{1,2}/\\d{1,2})) \\d{1,2}:\\d\\d:00$",dateTime)){
					for (int i=2;i<splitline.length;i++){
						int fileIndex = i-2;
						if (Pattern.matches("^-?\\d{1,4}(.\\d{1,6})?$",splitline[i])){
							//ONLY ADDS VALID DATA TO THE ARRAY
							try {
								fileList.get(fileIndex).addRow(new DataPoint(dateTranslator.parse(dateTime).getTime(),Double.parseDouble(splitline[i])));
							} catch (NumberFormatException e) {
								e.printStackTrace();
							} catch (ParseException e) {
								logWindow.println("Error converting date at point: "+dateTime+" in file "+fileName+". This file will not be written.");
								e.printStackTrace();
							}
						}
						else{
							//invalid data point
							try {
								fileList.get(fileIndex).addRow(new DataPoint(dateTranslator.parse(dateTime).getTime(),-123.456));
							} catch (NumberFormatException e) {
								e.printStackTrace();
							} catch (ParseException e) {
								logWindow.println("Error converting date at point: "+dateTime+" in file "+fileName+". This file will not be written.");
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		else{

		}
	}
	
	String getFileInfo(String fileName,int sourceCount,BufferedReader inputStream) throws IOException{
		String dateFormat = "";
		Set<String> dateSet1 = new HashSet<String>();
		Set<String> dateSet2 = new HashSet<String>();
		Set<String> dateSet3 = new HashSet<String>();
		
		ArrayList<String> dateStrings = new ArrayList<String>();

		int rowCounter = 0;
		
		

		String line = "";
		int wrongColumnCount = 0;
		while ((line = inputStream.readLine()) != null) {
			if ((rowCounter%1440==0 && (!dateSet1.isEmpty() && !dateSet2.isEmpty() && !dateSet3.isEmpty()) && (dateSet3.size() != dateSet2.size() && dateSet2.size() != dateSet1.size() && dateSet1.size() != dateSet3.size()))){
				break;
			}
			
			String[] splitline = line.split("	"); //;split by tabs
			for (int i=0;i<splitline.length;i++){
				splitline[i] = splitline[i].trim();
			}
			
			if (splitline.length==sourceCount+2){
				if (Pattern.matches("^((\\d{1,2}/\\d{1,2}/\\d\\d(\\d\\d){0,1})|(\\d\\d(\\d\\d){0,1}/\\d{1,2}/\\d{1,2})) \\d{1,2}:\\d\\d:00$",splitline[0]+" "+splitline[1])){
					dateStrings.add(splitline[0]+" "+splitline[1]);
	
					dateSet1.add(splitline[0].split("/",3)[0]);
					dateSet2.add(splitline[0].split("/",3)[1]);
					dateSet3.add(splitline[0].split("/",3)[2]);
					rowCounter++;
				}
			}
			else{
				wrongColumnCount++;
				if (wrongColumnCount>10 && wrongColumnCount>rowCounter){ //if have more than 10 rows and most rows are the wrong length
					logWindow.println("File: "+fileName+".\r\nData is in an unexpected format. Please ensure data is provided in a [tab] delimited file, and that the number of column headers matches the number of data columns.");
					break;
				}
			}
		}
		
		if (!dateSet1.isEmpty() && !dateSet2.isEmpty() && !dateSet3.isEmpty()){ //if date is in a valid format
			if (dateSet1.size()>dateSet2.size() && dateSet2.size()>dateSet3.size()){
				dateFormat = "dd/MM/yy";
			}
			else if (dateSet1.size()>dateSet2.size() && dateSet2.size()==dateSet3.size()){
				dateFormat = "dd/MM/yy";
			}
			else if (dateSet2.size()>dateSet1.size() && dateSet1.size()>dateSet3.size()){
				dateFormat = "MM/dd/yy";
			}
			else if (dateSet2.size()>dateSet1.size() && dateSet1.size()==dateSet3.size()){
				dateFormat = "MM/dd/yy";
			}
			else if (dateSet3.size()>dateSet2.size() && dateSet2.size()>dateSet1.size()){
				dateFormat = "yy/MM/dd";
			}
			else if (dateSet3.size()>dateSet2.size() && dateSet2.size()==dateSet1.size()){
				dateFormat = "yy/MM/dd";
			}
			else{ //if all three Sets have an equal size (can only be 1)
				if (dateSet1.size()==1 && dateSet2.size()==1 && dateSet3.size()==1){
					if (getYearFormat(dateSet1)<=2 && getYearFormat(dateSet3)==4){
						dateFormat = "dd/MM/yyyy";
					}
					else if (getYearFormat(dateSet1)==4 && getYearFormat(dateSet3)<=2){
						dateFormat = "yyyy/MM/dd";
					}
					else{
						dateFormat = "";
						logWindow.println("File: "+fileName+".\r\nDate is in an incorrect format or there is insufficient data to determine its format.\r\nPlease ensure dates are specified as either dd/mm/yyyy or yyyy/mm/dd.\r\nFile will not be processed at this stage.");
					}
				}
				else{ //invalid because an impossible combination is detected (ie. more months than days, more years that months, etc.)
					logWindow.println("File: "+fileName+".\r\nDate is in an incorrect format or there is insufficient data to determine its format.\r\nPlease ensure dates are specified as either dd/mm/yyyy or yyyy/mm/dd.\r\nFile will not be processed at this stage.");
				}
			}
			if (dateFormat == "yy/MM/dd" && getYearFormat(dateSet1)==4 && getYearFormat(dateSet3)<=2){
				dateFormat = "yyyy/MM/dd";
			}
			else if (dateFormat == "dd/MM/yy" && getYearFormat(dateSet1)<=2 && getYearFormat(dateSet3)==4){
				dateFormat = "dd/MM/yyyy";
			}
			else if (getYearFormat(dateSet1)<=2 && getYearFormat(dateSet2)<=2 && getYearFormat(dateSet3)<=2){
				
			}
			else{ //if date is not: dd/mm/yy or yy/mm/dd or yyyy/mm/dd or dd/mm/yyyy
				dateFormat = "";
				logWindow.println("File: "+fileName+".\r\nDate is in an incorrect format or there is insufficient data to determine its format.\r\nPlease ensure dates are specified as either dd/mm/yyyy or yyyy/mm/dd.\r\nFile will not be processed at this stage.");
			}
		}
		else{ //date format is completely wrong ie. not xxxx/xxxx/xxxx
			dateFormat = "";
			logWindow.println("File: "+fileName+".\r\nDate is in an incorrect format or there is insufficient data to determine its format.\r\nPlease ensure dates are specified as either dd/mm/yyyy or yyyy/mm/dd.\r\nFile will not be processed at this stage.");
		}
		
		if (!dateFormat.equals("")){
			SimpleDateFormat dateTranslator = new SimpleDateFormat(dateFormat+" HH:mm:ss");
			dateTranslator.setTimeZone(TimeZone.getTimeZone("GMT+10"));
			long[] dates = new long[dateStrings.size()];
			for (int i=0;i<dateStrings.size();i++){
				try {
					dates[i] = dateTranslator.parse(dateStrings.get(i)).getTime();
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					logWindow.println("Error converting date at point: "+dateStrings.get(i)+" in file "+fileName+". This file will not be written.");
					e.printStackTrace();
				}
			}
			
			frequency = getInterval(dates,fileName);
			
			if (frequency!=60 && frequency!=600 && frequency!=1800 && frequency!=3600){
				JPanel errorPanel = new JPanel(new GridLayout(2,1));
				JPanel p1 = new JPanel(new FlowLayout());
				JPanel p2 = new JPanel();
				JLabel message1 = new JLabel("ERROR: the automatically determined interval for this file '"+frequency+"' is invalid.");
				JLabel message2 = new JLabel("Please provide the interval between data points for this file in seconds.");
				JTextField input = new JTextField();
				p1.add(message1);
				p2.add(message2);
				p2.add(input);
				errorPanel.add(p1);
				errorPanel.add(p2);
				int response = JOptionPane.OK_OPTION;
				while(response == JOptionPane.OK_OPTION && !input.getText().matches("\\d{1,5}")){
					if (!input.getText().matches("\\d{1,5}") && !input.getText().equals("")){
						message1.setText("The interval provided '"+input.getText()+"' is invalid.");
					}
					response = JOptionPane.showConfirmDialog(null,errorPanel,"ERROR",JOptionPane.OK_CANCEL_OPTION,JOptionPane.ERROR_MESSAGE);
				}
				if (response == JOptionPane.OK_OPTION && input.getText().matches("\\d{1,5}")){
					frequency = Integer.parseInt(input.getText());
					logWindow.println("Interval set for file: "+fileName+" (Interval="+input.getText()+" seconds)");
				}
				else{
					frequency = 0;
					logWindow.println("No interval set for file: "+fileName);
				}
			}
		}
			
		return dateFormat;
	}
	
	int getInterval(long[] dates,String fileName){ //finds a block of 50 consistent consecutive date intervals and sets interval
		int interval = 0;
		
		double workingInterval = 0;
		int consecutive = 0;
		
		for (int i=0;interval == 0 && i<dates.length-2;i++){
			workingInterval = (i*workingInterval + dates[i+1] - dates[i])/(i+1);
			if ((dates[i+1] - dates[i])==workingInterval){
				consecutive++;
			}
			else{
				consecutive = 1;
				workingInterval = (dates[i+1] - dates[i]);
			}
			if (consecutive >= 50){
				interval = (int)Math.round(workingInterval)/1000;
				logWindow.println("Interval determined for file: "+fileName+" (Interval="+interval+" seconds)");
			}
			else if (i>500 && interval == 0){ //if gone through 55 and still no consensus
				logWindow.println("Problem encountered while processing file: "+fileName+".\r\nNo interval was able to be determined.\r\nFile will not be processed at this stage.");
				interval = 0;
				break;
			}
		}
		
		return interval;
	}
	
	
	
	int getYearFormat(Set<String> dateSet){
		int yearFormat = 0;
		int counter = 0;
		int totalCount = 0;
		Iterator<String> it = dateSet.iterator();
		while (it.hasNext() && counter <=10){
			totalCount+=it.next().length();
			counter++;
		}
		yearFormat = totalCount/counter;
		return yearFormat;
	}
	
	String getTimeString(long timeInMillis){
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
}
