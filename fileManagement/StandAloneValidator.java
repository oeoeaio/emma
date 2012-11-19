package fileManagement;

import issueManagement.FileIssue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;


import endUseWindow.LogWindow;
import endUseWindow.MySQLConnection;

public class StandAloneValidator implements Runnable{
	
	MySQLConnection mySQLConnection;
	private Connection dbConn;
	private ArrayList<FileIssue> fileIssueList;
	private static final int fileQueueSize = 1;
	BlockingQueue<DataFile> fileList = new LinkedBlockingQueue<DataFile>(fileQueueSize);
	private LogWindow logWindow;
	boolean moreFilesComing = true;
	

	
	StandAloneValidator(MySQLConnection mySQLConnection,LogWindow logWindow){
		this.mySQLConnection = mySQLConnection;
		this.dbConn = mySQLConnection.getCopyConnection();
		//this.fileList = fileList;
		this.logWindow = logWindow;
		//this.siteID = site;
		//this.sourceID = source;
	}
	
	public void addFile(DataFile dataFile){
		try{
			synchronized(fileList){
				while (!fileList.offer(dataFile)){
					fileList.wait();
				}
				fileList.notify();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void run(){
		//Date startTime = new Date();
		FileFeeder fileFeeder = new FileFeeder(mySQLConnection,logWindow, fileIssueList, true);
		Thread feederThread = new Thread(fileFeeder);
		feederThread.start();
		while (moreFilesComing || !fileList.isEmpty()){ //if more data still to process or more data coming
			DataFile dataFile;
			if ((dataFile = fileList.poll()) != null){ //something to actually write
				synchronized(fileList){
					fileList.notify();
				}
				
				logWindow.println("Processing data from file: "+dataFile.fileName+"...");
				
				BufferedReader inputStream = null;
				try {
					inputStream = new BufferedReader(new FileReader(dataFile.file));
	
					String fileDateFormat = getFileInfo(dataFile,inputStream);
					
					if (dataFile.frequency!=0 && !dataFile.meterSerial.equals("")){ //if got valid info
						
						try{
							String fileExistsSQL = "SELECT * FROM files WHERE site_id = "+dataFile.siteID+" AND source_id = "+dataFile.sourceID+" AND file_name = '"+dataFile.fileName+"' AND meter_sn = '"+dataFile.meterSerial+"' AND frequency = "+dataFile.frequency;
							ResultSet fileExistsQuery = dbConn.createStatement().executeQuery(fileExistsSQL);
							if (fileExistsQuery.next()==false){ //if no files with same site,source,filename,meterserial and frequency exist

								getValidData(dataFile,fileDateFormat,inputStream);

								if (dataFile.dataList.size()>0){
									logWindow.println("Processing of data complete.");
									
									/*try{
										String fetchRangeLimitsSQL =  "SELECT min,max FROM ranges WHERE source_id = "+dataFile.sourceID+" AND site_id = "+dataFile.siteID;
										ResultSet fetchRangeLimitsRS = dbConn.createStatement().executeQuery(fetchRangeLimitsSQL);

										if (fetchRangeLimitsRS.next()){
											dataFile.rangeMin = fetchRangeLimitsRS.getDouble("min");
											dataFile.rangeMax = fetchRangeLimitsRS.getDouble("max");
										}
										else{
											logWindow.println("No range limits found for "+dataFile.sourceID+" or site "+dataFile.siteID+". Adding defaults now...");	

											dataFile.rangeMin = Source.getRangeMin(dataFile.sourceType,dataFile.measurementType);
											dataFile.rangeMax = Source.getRangeMax(dataFile.sourceType,dataFile.measurementType,dataFile.frequency);

											try{
												String setRangeLimitsSQL =  "INSERT INTO ranges (site_id,source_id,min,max) VALUES("+dataFile.siteID+","+dataFile.sourceID+","+dataFile.rangeMin+","+dataFile.rangeMax+")";
												dbConn.createStatement().executeUpdate(setRangeLimitsSQL);
											}catch(SQLException sE){ //NON FATAL
												sE.printStackTrace();
												logWindow.println("Warning: could not write range limits to database for file "+dataFile.fileName+".");	
											}
										}

										SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
										dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
										dataFile.startDate = dateFormatter.format(dataFile.dataList.get(0).dateTime);
										dataFile.endDate = dateFormatter.format(dataFile.dataList.get(dataFile.dataList.size()-1).dateTime);*/

										fileFeeder.addFile(dataFile); //Send file to feeder

									/*} catch (SQLException sE){
										sE.printStackTrace();
									}*/
								}
								else{
									logWindow.println("Ignored file "+dataFile.fileName+". No Data found in file.");	
								}
							}
							else{
								logWindow.println("Ignored file "+dataFile.fileName+". This file already exists for source "+dataFile.sourceID+" at site "+dataFile.siteID+".");	
							}

						} catch(SQLException sE){
							//TODO need error here
							sE.printStackTrace();
						}
					}
					else{
						logWindow.println(dataFile.frequency+" "+dataFile.meterSerial);
						logWindow.println("Ignored file "+dataFile.fileName+". Could not determine essential file information.");	
					}
				
				} catch (IOException err){
					dataFile.dataList.clear();
					logWindow.println("Unable to read from file: "+dataFile.fileName+"\r\nNo data will be written from this file.");
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
			try{
				synchronized(fileList){ //waiter
					while (moreFilesComing && fileList.isEmpty()){
						fileList.notify(); //notify adder just in case things got stuck
						fileList.wait(); //wait for a file to be added to file List.
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		synchronized(fileFeeder.fileList){
			fileFeeder.moreFilesComing = false;
			fileFeeder.fileList.notify();
		}
		try {
			feederThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//Date endTime = new Date();
		//logWindow.println("Finished sending file(s) to writer for source: "+sourceID);
		//logWindow.println("Time taken: "+getTimeString(endTime.getTime()-startTime.getTime()));
	}

	void getValidData(DataFile dataFile,String fileDateFormat,BufferedReader inputStream) throws IOException {	
		SimpleDateFormat dateTranslator = new SimpleDateFormat(fileDateFormat+" HH:mm:ss");
		dateTranslator.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		if (fileDateFormat.equals("")==false){

			String line = "";
			while ((line = inputStream.readLine()) != null) {
				line = line.replace("   "," ");
				line = line.replace("  "," ");
				line = line.replace("	"," ");
				line = line.replace(","," ");
				String[] splitline = line.split(" ");
				if (splitline.length==3 
						&& Pattern.matches("^\\d{1,2}/\\d{1,2}/\\d\\d(\\d\\d){0,1}$",splitline[0])
						|| Pattern.matches("^\\d\\d(\\d\\d){0,1}/\\d{1,2}/\\d{1,2}$",splitline[0])){
					if (Pattern.matches("^\\d{1,2}:\\d\\d(:\\d\\d){0,1}$",splitline[1])
							&& Pattern.matches("^-?\\d{1,4}(.\\d{1,2}){0,1}$",splitline[2])){
						
						//ONLY ADDS VALID DATA TO THE ARRAY
						splitline[1] += (Pattern.matches("^\\d{1,2}:\\d\\d$",splitline[1])?":00":""); //add seconds if absent
						try {
							dataFile.addRow(new DataPoint(dateTranslator.parse(splitline[0]+" "+splitline[1]).getTime(),Double.parseDouble(splitline[2])));
						} catch (NumberFormatException e) {
							e.printStackTrace();
						} catch (ParseException e) {
							logWindow.println("Error converting date at point: "+splitline[0]+" "+splitline[1]+" in file "+dataFile.fileName+". This file will not be written.");
							e.printStackTrace();
						}
					}
				}
			}

		}
		else{
			
		}
	}
	
	String getFileInfo(DataFile dataFile,BufferedReader inputStream) throws IOException{
		String dateFormat = "";
		Set<String> dateSet1 = new HashSet<String>();
		Set<String> dateSet2 = new HashSet<String>();
		Set<String> dateSet3 = new HashSet<String>();
		
		ArrayList<String> dateStrings = new ArrayList<String>();
		ArrayList<Double> values = new ArrayList<Double>();

		int rowCounter = 0;

		String line = "";
		boolean incompleteSerial = false;
		while ((line = inputStream.readLine()) != null) {
			if ((rowCounter%1440==0 && (!dateSet1.isEmpty() && !dateSet2.isEmpty() && !dateSet3.isEmpty()) && (dateSet3.size() != dateSet2.size() && dateSet2.size() != dateSet1.size() && dateSet1.size() != dateSet3.size()))){
				break;
			}
			line = line.replace("   "," ");
			line = line.replace("  "," ");
			line = line.replace("	"," ");
			String[] splitline = line.split(" ");
			if (splitline.length==3 
					&& Pattern.matches("^\\d{1,2}/\\d{1,2}/\\d\\d(\\d\\d){0,1}$",splitline[0])
					|| Pattern.matches("^\\d\\d(\\d\\d){0,1}/\\d{1,2}/\\d{1,2}$",splitline[0])){
				if (Pattern.matches("^\\d{1,2}:\\d\\d(:00){0,1}$",splitline[1])
						&& Pattern.matches("^-?\\d{1,4}(.\\d{1,2}){0,1}$",splitline[2])){
					
					splitline[1] += (Pattern.matches("^\\d{1,2}:\\d\\d$",splitline[1])?":00":""); //add seconds if absent
					dateStrings.add(splitline[0]+" "+splitline[1]);
					values.add(Double.parseDouble(splitline[2]));
					
					dateSet1.add(splitline[0].split("/",3)[0]);
					dateSet2.add(splitline[0].split("/",3)[1]);
					dateSet3.add(splitline[0].split("/",3)[2]);
				}
				rowCounter++;
			}
			else if (rowCounter <= 10 && line.matches("^.{0,50} \\d{8}$")){
				dataFile.meterSerial = splitline[splitline.length-1];
				logWindow.println("Meter Serial found for file: "+dataFile.fileName+" (meterSN: "+dataFile.meterSerial+")");
			}
			else if (rowCounter <= 10 && line.matches("^.{0,50} \\d{7}$")){
				incompleteSerial = true;
			}
			if (rowCounter > 10 && dataFile.meterSerial.equals("")){
				if (incompleteSerial){
					logWindow.println("Unable to determine meter serial number for file: "+dataFile.fileName+"\r\nFound block of 7 numbers, which could suggest incorrect format.\r\nPlease ensure meter serial number is in the format XXXXXXXX (ie. 8 digits).");
				}
				else{
					logWindow.println("Unable to determine meter serial number for file: "+dataFile.fileName+"\r\nNo data will be written from this file.");
				}
				values.clear();
				break;
			}
		}
		
		if (!dateSet1.isEmpty() || !dateSet2.isEmpty() || !dateSet3.isEmpty()){ //if date is in a valid format
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
						logWindow.println("File1: "+dataFile.fileName+".\r\nDate is in an incorrect format or there is insufficient data to determine its format.\r\nPlease ensure dates are specified as either dd/mm/yyyy or yyyy/mm/dd.\r\nFile will not be processed at this stage.");
					}
				}
				else{ //invalid because an impossible combination is detected (ie. more months than days, more years that months, etc.)
					logWindow.println("File2: "+dataFile.fileName+".\r\nDate is in an incorrect format or there is insufficient data to determine its format.\r\nPlease ensure dates are specified as either dd/mm/yyyy or yyyy/mm/dd.\r\nFile will not be processed at this stage.");
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
				logWindow.println("File3: "+dataFile.fileName+".\r\nDate is in an incorrect format or there is insufficient data to determine its format.\r\nPlease ensure dates are specified as either dd/mm/yyyy or yyyy/mm/dd.\r\nFile will not be processed at this stage.");
			}
		}
		else{ //date format is completely wrong ie. not xxxx/xxxx/xxxx
			dateFormat = "";
			logWindow.println("File4: "+dataFile.fileName+".\r\nDate is in an incorrect format or there is insufficient data to determine its format.\r\nPlease ensure dates are specified as either dd/mm/yyyy or yyyy/mm/dd.\r\nFile will not be processed at this stage.");
		}
		
		SimpleDateFormat dateTranslator = new SimpleDateFormat(dateFormat+" HH:mm:ss");
		dateTranslator.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		for (int i=0;i<dateStrings.size();i++){
			//ONLY ADDS VALID DATA TO THE ARRAY
			try {
				dataFile.addRow(new DataPoint(dateTranslator.parse(dateStrings.get(i)).getTime(),values.get(i)));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				logWindow.println("Error converting date at point: "+dateStrings.get(i)+" in file "+dataFile.fileName+". This file will not be written.");
				e.printStackTrace();
			}
		}
		
		dataFile.frequency = getInterval(dataFile);
			
		return dateFormat;
	}
	
	int getInterval(DataFile dataFile){
		int interval = 0;
		
		double workingInterval = 0;
				
		for (int i=0;interval == 0 && i<dataFile.dataList.size()-2;i++){
			workingInterval = (i*workingInterval + dataFile.dataList.get(i+1).dateTime - dataFile.dataList.get(i).dateTime)/(i+1);
			if (i>60 && (workingInterval-Math.round(workingInterval))<0.001){
				interval = (int)Math.round(workingInterval)/1000;
				logWindow.println("Interval determined for file: "+dataFile.fileName+" (Interval="+interval+" seconds)");
				break;
			}
			else if (i>60){
				interval = 0;
				break;
			}
		}
		
		if (interval==0){
			logWindow.println("Problem encountered while processing file: "+dataFile.fileName+".\r\nNo interval was able to be determined.\r\nFile will not be processed at this stage.");
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
