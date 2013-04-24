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


public class CTValidator implements Runnable{
	
	private final Connection dbConn;
	private final MySQLConnection mySQLConnection;
	private final File ctFile;
	private final LogWindow logWindow;
	private final ArrayList<FileIssue> fileIssueList = new ArrayList<FileIssue>();
	private int frequency = 0;
	private String meterSerial = "";
	

	
	public CTValidator(MySQLConnection mySQLConnection,LogWindow logWindow,File ctFile){
		this.mySQLConnection = mySQLConnection;
		this.dbConn = mySQLConnection.getCopyConnection();
		this.ctFile = ctFile;
		this.logWindow = logWindow;
	}
	
	public void run(){
		BufferedReader inputStream = null;
		try {
			inputStream = new BufferedReader(new FileReader(ctFile));
			
			String line = "";
			if ((line=inputStream.readLine())!=null && Pattern.matches("^\\d{8}((	(Volts)){3}){0,1}(	|(\\d{3,8}))+$",line)){
				meterSerial = line.split("	")[0];
				logWindow.println("Meter Serial found for file: "+ctFile.getName()+" (meterSN: "+meterSerial+")");
				if ((line=inputStream.readLine())!=null && line.matches("^.{1,16}(	(((Ph )\\d)){3}){0,1}(	|(.{1,16}))+$")){
					String[] splitLine = line.split("	");
					for(int i=0;i<splitLine.length;i++){
						splitLine[i] = splitLine[i].trim();
					}
					
					Site site = null;
					if (splitLine.length >= 2){
						try{
							String testSiteName = splitLine[0];
							
							String checkSiteNameSQL = "SELECT * FROM sites WHERE site_name = '"+testSiteName+"'";
							ResultSet checkSiteNameRS = dbConn.createStatement().executeQuery(checkSiteNameSQL);
							
							if (checkSiteNameRS.next()){
								Site testSite = new Site(checkSiteNameRS.getString("site_id"),checkSiteNameRS.getString("site_name"),checkSiteNameRS.getString("concentrator"),checkSiteNameRS.getString("given_name"),checkSiteNameRS.getString("surname"),checkSiteNameRS.getString("suburb"),checkSiteNameRS.getString("state"));
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
					}
					else{
						logWindow.println("Invalid file format, must contain at least a date column and a data column");
					}
					
					
					if (site!=null){ //if found a site ID
						LinkedList<DataFile> fileList = new LinkedList<DataFile>();
						
						for (int i=1;i<splitLine.length;i++){
							String sourceID = null;
							if (splitLine[i].matches("^[\\w\\s\\Q#&()[]:-+=/\\.?\\E]{1,16}$")){
								try{
									ResultSet sourceIDRS = dbConn.createStatement().executeQuery("SELECT source_id FROM sources WHERE site_id = "+site.getSiteID()+" AND source_name = '"+splitLine[i]+"'");
									
									if (sourceIDRS.next()){
										sourceID = sourceIDRS.getString("source_id");
									}
									else{// add new circuit
										if (splitLine[i].matches("^Ph \\d$")){ //phase
											logWindow.printString("Warning: unable to match specified phase '"+splitLine[i]+"'.\r\nAdding phase '"+splitLine[i]+"' to database...");
											String addCircuitSourceSQL = "INSERT INTO sources (site_id,source_name,source_type,measurement_type) VALUES("+site.getSiteID()+",'"+splitLine[i]+"','Phase','Volts')";
											
											dbConn.createStatement().executeUpdate(addCircuitSourceSQL);
											ResultSet new_circuit_source_id = dbConn.createStatement().executeQuery("SELECT LAST_INSERT_ID() AS current_id"); //returns new id
											new_circuit_source_id.next();
											sourceID = new_circuit_source_id.getString("current_id");
											try{
												dbConn.createStatement().executeUpdate("INSERT INTO phases (site_id,source_id) VALUES("+site.getSiteID()+","+sourceID+")");
												logWindow.println("Done.");
											} catch(SQLException sE){
												sourceID = null;
												removeSource(site.getSiteID(),sourceID);
												sE.printStackTrace();
												logWindow.println("Failed.");
											}
										}
										else { //don't know, so ask
											String sourceType = "";
											String[] sourceTypes = new String[] {"Appliance","Circuit","Gas","Humidity","Light","Motion","Temperature","Water"};
											sourceType = (String)JOptionPane.showInputDialog(null, "Please provide a source type for channel: '"+splitLine[i]+".\r\nPlease select the appropriate type from the list:", "Select Source Type", JOptionPane.PLAIN_MESSAGE, null, sourceTypes, "Circuit");
											if(sourceType == null){sourceType = "";}
											String measurementType = "";
											String[] measurementTypes = new String[] {"ActEnergy","AppEnergy","OnTime","Temp","Humidity","Pulse","ActPower","AppPower","LightLevel","Volts","Amps","AvgTemp"};
											measurementType = (String)JOptionPane.showInputDialog(null, "Please provide a measurement type for channel: '"+splitLine[i]+".\r\nPlease select the appropriate type from the list:", "Select Source Type", JOptionPane.PLAIN_MESSAGE, null, measurementTypes, "ActPower");
											if(measurementType == null){measurementType = "";}
											if (!sourceType.equals("") || !measurementType.equals("")){
												logWindow.printString("Warning: unable to match specified source '"+splitLine[i]+"'.\r\nAdding circuit '"+splitLine[i]+"' to database...");
												/*String addCircuitSourceSQL = "INSERT INTO sources (site_id,source_name,source_type,measurement_type) VALUES("+site.getSiteID()+",'"+splitLine[i]+"','Circuit','ActPower')";
												dbConn.createStatement().executeUpdate(addCircuitSourceSQL);
												ResultSet new_circuit_source_id = dbConn.createStatement().executeQuery("SELECT LAST_INSERT_ID() AS current_id"); //returns new id
												new_circuit_source_id.next();
												sourceID = new_circuit_source_id.getString("current_id");
												try{
													dbConn.createStatement().executeUpdate("INSERT INTO circuits (site_id,source_id) VALUES("+site.getSiteID()+","+sourceID+")");
													logWindow.println("Done.");
												} catch(SQLException sE){
													sourceID = null;
													removeSource(site.getSiteID(),sourceID);
													sE.printStackTrace();
													logWindow.println("Failed.");
												}*/
											
												Statement mySQLStatement = dbConn.createStatement();
												Source testSource = new Source(site,null,splitLine[i],sourceType,measurementType);
												sourceID = Source.addSource(logWindow, true, mySQLStatement, site, testSource, ctFile.getName(), true);
									
											}
											else{
												sourceID = null;
												logWindow.println("Error: source type not provided, no data will be written.");
											}
										}
									}
								} catch(SQLException sE){
									sE.printStackTrace();
									sourceID = null;
									logWindow.println("Warning: unable to match specified source '"+splitLine[i]+"'");
								}
							}
							else{
								logWindow.println("ERROR: source name '"+splitLine[i]+"' is invalid, and will not be added to to database at this time.");
							}
							if (sourceID!=null){
								DataFile dataFile = new DataFile();
								dataFile.siteID = site.getSiteID();
								dataFile.sourceID = sourceID;
								dataFile.fileName = ctFile.getName();
								dataFile.file = ctFile;
								fileList.add(dataFile);
							}
						}
						
						if (!fileList.isEmpty()){ //got some files							
							if (fileList.size()==splitLine.length-1){
							
								String fileDateFormat = getFileInfo(ctFile.getName(),fileList.size(),inputStream);
								
								if (frequency!=0 && !meterSerial.equals("") && !fileDateFormat.equals("")){ //if got valid info
									try{
										inputStream.close();
										inputStream = new BufferedReader(new FileReader(ctFile)); //restart file reader
										
										getValidData(fileList,ctFile.getName(),fileList.size(),fileDateFormat,inputStream);
									} catch (IOException err){
										logWindow.println("Unable to read from file: "+ctFile.getName().toString()+"\r\nNo data will be written from this file.");
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
									logWindow.println("Could not obtain vital information about file: "+ctFile.getName());
									fileList.clear();
								}
							}
							else{
								logWindow.println("One or more header names were invalid. File: "+ctFile.getName()+" will not be processed until this is recitifed.");
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
											/*try{
	
												String fetchRangeLimitsSQL =  "SELECT min,max FROM ranges WHERE (source_id = "+dataFile.sourceID+" OR source_id IS NULL) AND (site_id = "+dataFile.siteID+" OR site_id IS NULL) ORDER BY site_id DESC,source_id DESC";
												ResultSet fetchRangeLimitsRS = dbConn.createStatement().executeQuery(fetchRangeLimitsSQL);
	
												if (fetchRangeLimitsRS.next()){
													dataFile.rangeMin = fetchRangeLimitsRS.getDouble("min");
													dataFile.rangeMax = fetchRangeLimitsRS.getDouble("max");
												}
												else{
													dbConn.createStatement().executeUpdate("INSERT INTO ranges (site_id,source_id,min,max) VALUES("+dataFile.siteID+","+dataFile.sourceID+",0,8000)");
													dataFile.rangeMin = 0.0;
													dataFile.rangeMax = 8000.0;
													logWindow.println("No range limits found for source "+dataFile.sourceID+" or site "+dataFile.siteID+". Defaults added.");
												}
												if (dataFile.rangeMin != dataFile.rangeMax && dataFile.rangeMax > 0){
	
													SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
													dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
													dataFile.startDate = dateFormatter.format(dataFile.dataList.get(0).dateTime);
													dataFile.endDate = dateFormatter.format(dataFile.dataList.get(dataFile.dataList.size()-1).dateTime);
											 		*/
													fileFeeder.addFile(dataFile); //Send file to feeder
												/*}
												else{
													logWindow.println("ERROR: file "+dataFile.fileName+". No range limits found for source "+dataFile.sourceID+" or site "+dataFile.siteID+".");	
												}
	
	
											} catch (SQLException sE){
												sE.printStackTrace();
											}*/
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
					logWindow.println("Second line should be: [SiteName]	Ph X	Ph X	Ph X( 	[SourceName])*6n");
				}
			}
			else{
				logWindow.println("Unexpected file format. File will not be processed at this stage.");
				logWindow.println("First line should be: XXXXXXXX	Volts	Volts	Volts( 	XXXXXXXX)*6n");
			}
		} catch (IOException err){
			logWindow.println("Unable to read from file: "+ctFile.getName().toString()+"\r\nNo data will be written from this file.");
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
	
	private void removeSource(String siteID,String sourceID){
		try{
			Statement MySQL_Statement = dbConn.createStatement();
			MySQL_Statement.executeUpdate("DELETE FROM sources WHERE site_id = "+siteID+" AND source_id = "+sourceID); //removes data pertaining to selected source
		} catch (SQLException sE){
			JOptionPane.showMessageDialog(null,"An error occured when removing data for the specified source (Source ID: "+sourceID+").","Retrieval Error",JOptionPane.ERROR_MESSAGE);
			sE.printStackTrace();
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
				
				if (splitline.length==sourceCount+1 && Pattern.matches("^((\\d{1,2}/\\d{1,2}/\\d\\d(\\d\\d){0,1})|(\\d\\d(\\d\\d){0,1}/\\d{1,2}/\\d{1,2})) \\d{1,2}:\\d\\d:00$",splitline[0])){
					for (int i=1;i<splitline.length;i++){
						if (Pattern.matches("^-?\\d{1,4}(.\\d{1,2}){0,1}$",splitline[i])){
							//ONLY ADDS VALID DATA TO THE ARRAY
							try {
								fileList.get(i-1).addRow(new DataPoint(dateTranslator.parse(splitline[0]).getTime(),Double.parseDouble(splitline[i])));
							} catch (NumberFormatException e) {
								e.printStackTrace();
							} catch (ParseException e) {
								logWindow.println("Error converting date at point: "+splitline[0]+" in file "+fileName+". This file will not be written.");
								e.printStackTrace();
							}
						}
						else{
							//invalid data point
							try {
								fileList.get(i-1).addRow(new DataPoint(dateTranslator.parse(splitline[0]).getTime(),-123.456));
							} catch (NumberFormatException e) {
								e.printStackTrace();
							} catch (ParseException e) {
								logWindow.println("Error converting date at point: "+splitline[0]+" in file "+fileName+". This file will not be written.");
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
			
			if (splitline.length==sourceCount+1){
				if (Pattern.matches("^((\\d{1,2}/\\d{1,2}/\\d\\d(\\d\\d){0,1})|(\\d\\d(\\d\\d){0,1}/\\d{1,2}/\\d{1,2})) \\d{1,2}:\\d\\d:00$",splitline[0])){
					dateStrings.add(splitline[0]);
	
					dateSet1.add(splitline[0].split("/",3)[0]);
					dateSet2.add(splitline[0].split("/",3)[1]);
					dateSet3.add(splitline[0].split("/",3)[2].split(" ")[0]); //gets rid of time string
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
			
			if (frequency!=60 && frequency!=600 && frequency!=3600){
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
