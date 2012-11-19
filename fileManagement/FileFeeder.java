package fileManagement;

import issueManagement.FileIssue;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import endUseWindow.LogWindow;
import endUseWindow.MySQLConnection;
import endUseWindow.Source;

public class FileFeeder implements Runnable{

	MySQLConnection mySQLConnection;
	private Connection dbConn;
	private LogWindow logWindow;
	private ArrayList<FileIssue> fileIssueList;
	private boolean showGUI = false;
	private static final int fileQueueSize = 1;
	BlockingQueue<DataFile> fileList = new LinkedBlockingQueue<DataFile>(fileQueueSize);
	boolean moreFilesComing = true;
	private long totalFeederTime;
	private long totalGetWriterTime;
	private long totalGetTime;
	private long totalStartTime;
	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	FileFeeder(MySQLConnection mySQLConnection,LogWindow logWindow,ArrayList<FileIssue> fileIssueList,boolean showGUI){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		this.mySQLConnection = mySQLConnection;
		this.dbConn = mySQLConnection.getCopyConnection();
		this.logWindow = logWindow;
		this.fileIssueList = fileIssueList;
		this.showGUI = showGUI;
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
	
	public void run() {
		DataWriterPool dataWriterPool = new DataWriterPool(mySQLConnection,logWindow,showGUI);
		Thread dataWriterPoolThread = new Thread(dataWriterPool);
		dataWriterPoolThread.start();
		logWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		try{
			Statement MySQL_Statement = dbConn.createStatement();
			//DataFactorConverter dataFactorConverter = new DataFactorConverter();
			while (moreFilesComing || !fileList.isEmpty()){ //if more data still to process or more data coming
				Date start = new Date();
				DataFile dataFile;
				if ((dataFile = fileList.poll()) != null){ //something to actually write
					synchronized(fileList){
						fileList.notify();
					}
					//TODO check if needs filling
					
					//TODO check if counter
					
					//Double factor = 1.0;
					//if ((factor = dataFactorConverter.getConversionFactor(MySQL_Statement,dataFile)) != null){//determine if data needs a factor applied
					//	dataFactorConverter.applyFactor(dataFile,factor);
					//}		
					
					
					try{
						String fetchRangeLimitsSQL =  "SELECT min,max FROM ranges WHERE source_id = "+dataFile.sourceID+" AND site_id = "+dataFile.siteID;
						ResultSet fetchRangeLimitsRS = MySQL_Statement.executeQuery(fetchRangeLimitsSQL);

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
								MySQL_Statement.executeUpdate(setRangeLimitsSQL);
							}catch(SQLException sE){ //NON FATAL
								sE.printStackTrace();
								logWindow.println("Warning: could not write range limits to database for file "+dataFile.file.getName()+".");	
							}
						}
						fetchRangeLimitsRS.close();

						SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
						dataFile.startDate = dateFormatter.format(dataFile.dataList.get(0).dateTime);
						dataFile.endDate = dateFormatter.format(dataFile.dataList.get(dataFile.dataList.size()-1).dateTime);

						//fileFeeder.addFile(dataFile); //Send file to feeder

						try{
							String fileExistsSQL = "SELECT * FROM files WHERE site_id = "+dataFile.siteID+" AND source_id = "+dataFile.sourceID+" AND file_name = '"+dataFile.fileName+"' AND meter_sn = '"+dataFile.meterSerial+"' AND frequency = "+dataFile.frequency;
							ResultSet fileExistsQuery = dbConn.createStatement().executeQuery(fileExistsSQL);
							if (fileExistsQuery.next()==false){ //if no files with same site,source,filename,meterserial and frequency exist
								String newFileSQL = "INSERT INTO files (site_id,source_id,file_name,meter_sn,frequency,start_date,end_date,folder_name,file_size,date_modified) VALUES("+dataFile.siteID+","+dataFile.sourceID+",'"+dataFile.fileName+"','"+dataFile.meterSerial+"',"+dataFile.frequency+","+(dataFile.startDate==null?"NULL":"'"+dataFile.startDate+"'")+","+(dataFile.endDate==null?"NULL":"'"+dataFile.endDate+"'")+",'"+new File(dataFile.file.getParent()).getName()+"',"+dataFile.file.length()+",'"+dateFormatter.format(dataFile.file.lastModified())+"')";
								MySQL_Statement.executeUpdate(newFileSQL);
								ResultSet newFileIDQuery = MySQL_Statement.executeQuery("SELECT LAST_INSERT_ID()");
								if (newFileIDQuery.next()){
									//logWindow.println("Attempting to write data from file "+dataFile.fileName+" to database...");
									Date fetchStart = new Date();									
									
									dataFile.fileID = Integer.toString(newFileIDQuery.getInt(1));
									dataWriterPool.addFile(dataFile);
									Date get = new Date();
									totalGetTime += get.getTime()-fetchStart.getTime();
									Date fetchEnd = new Date();
									totalStartTime += fetchEnd.getTime()-get.getTime();
									totalGetWriterTime += fetchEnd.getTime()-fetchStart.getTime();
								}
								else{
									fileIssueList.add(new FileIssue(dataFile.file,"FileWriteError","Could not create a new file entry in Database."));
									logWindow.println("Ignored file "+dataFile.fileName+". Could not create a new source with name '"+dataFile.fileName+"'");
									if (showGUI){JOptionPane.showMessageDialog(null,"FATAL ERROR: could not add information about file "+dataFile.fileName+" to database.\r\nFile will not be written.","Error",JOptionPane.ERROR_MESSAGE);}
								}
								newFileIDQuery.close();
							}
							else{
								logWindow.println("Ignored file "+dataFile.fileName+". This file already exists for source "+dataFile.sourceID+" at site "+dataFile.siteID+".");	
							}
							fileExistsQuery.close();
						}catch(SQLException sE){
							JOptionPane.showMessageDialog(null,"FATAL ERROR: could not add information about file "+dataFile.fileName+" to database.\r\nFile will not be written.","Error",JOptionPane.ERROR_MESSAGE);
							logWindow.println("Error occured when adding information about file "+dataFile.fileName+" to database.\r\nData in this file will not be written to database.");	
							sE.printStackTrace();
						}
					}catch(SQLException sE){ //NON FATAL
						JOptionPane.showMessageDialog(null,"FATAL ERROR: could not collect/add range limits for file "+dataFile.fileName+" to database.\r\nFile will not be written.","Error",JOptionPane.ERROR_MESSAGE);
						logWindow.println("Warning: could not collect range limits from database for source '"+dataFile.sourceID+"'.");	
						sE.printStackTrace();
					}
					
				}
				Date end = new Date();
				totalFeederTime += end.getTime()-start.getTime();

				try{
					synchronized(fileList){
						while (moreFilesComing && fileList.isEmpty()){
							fileList.notify(); //notify adder just in case things got stuck
							fileList.wait(); //wait for a file to be added to file List.
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}catch(SQLException sE){
			logWindow.println("Error occured when connecting to database. Writing Failed.");	
		}
		
		
		synchronized(dataWriterPool.fileList){
			dataWriterPool.moreFilesComing = false;
			dataWriterPool.fileList.notify();
		}
		try {
			dataWriterPoolThread.join(); //wait for all writers to finish before finishing
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		dataWriterPool.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		logWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
}
