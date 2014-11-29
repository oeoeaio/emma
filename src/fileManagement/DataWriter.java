package fileManagement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;


import endUseWindow.LogWindow;
import endUseWindow.MySQLConnection;

public class DataWriter implements Runnable{
	
	private Connection dbConn;
	private LogWindow logWindow; 
	private DataFile dataFile;
	private DataWriterPool dataWriterPool;
	private JProgressBar progressBar;
	private boolean showGUI = false;
	public long totalWriteTime = 0;
	public long totalEndTime = 0;
	public long total1Time = 0;
	public long total2Time = 0;
	public long total3Time = 0;
	public long total4Time = 0;
	public long total5Time = 0;
	
	
	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	
	DataWriter(MySQLConnection mySQLConnection,LogWindow logWindow,DataWriterPool dataWriterPool, boolean showGUI){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		this.dbConn = mySQLConnection.getCopyConnection();
		this.logWindow = logWindow;
		this.dataWriterPool = dataWriterPool;
		this.showGUI = showGUI;
	}
	
	
	public void setDataFile(DataFile dataFile){
		this.dataFile = dataFile;
	}
	
	public void setProgressBar(JProgressBar progressBar){
		this.progressBar = progressBar;
	}
	
	public void run(){
		try {
			Date start = new Date();
			Statement MySQL_Statement = dbConn.createStatement();
			int rowCount = 0;

			while (!dataFile.dataList.isEmpty()){ //if more data still to process or more data coming
				Date numstart = new Date();
				DataPoint point = dataFile.dataList.poll(); //remove top of queue
				
				if (showGUI){ //if need to update GUI
					final int currProg = rowCount++;
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							progressBar.setValue(currProg);
							progressBar.revalidate();
						}
					});
				}
				
				Date Time1 = new Date();
				total1Time += Time1.getTime() - numstart.getTime();					
					try {	
						MySQL_Statement.executeUpdate("INSERT INTO data_sa (site_id,source_id,file_id,date_time,value) VALUES("+dataFile.siteID+","+dataFile.sourceID+","+dataFile.fileID+",'"+dateFormatter.format(point.dateTime)+"',"+(point.value==-123.456?"NULL":point.value)+")");
						Date Time2 = new Date();
						total2Time += Time2.getTime() - Time1.getTime();
						
						try{
							ResultSet newRecordIDQuery = MySQL_Statement.executeQuery("SELECT LAST_INSERT_ID()");
							if (newRecordIDQuery.next()){
								String recordID = Integer.toString(newRecordIDQuery.getInt(1));
								if (point.value == -123.456 || point.value == null){
									MySQL_Statement.executeUpdate("INSERT INTO issue_points (site_id,source_id,record_id,date_time,value,issue_type) VALUES("+dataFile.siteID+","+dataFile.sourceID+","+recordID+",'"+dateFormatter.format(point.dateTime)+"',"+(point.value==-123.456?"NULL":point.value)+",'MissingValue') ON DUPLICATE KEY UPDATE value="+(point.value==-123.456?"NULL":point.value));
									Date Time3 = new Date();
									total3Time += Time3.getTime() - Time2.getTime();
								}
								else if (point.value < dataFile.rangeMin || point.value > dataFile.rangeMax){
									MySQL_Statement.executeUpdate("INSERT INTO issue_points (site_id,source_id,record_id,date_time,value,issue_type) VALUES("+dataFile.siteID+","+dataFile.sourceID+","+recordID+",'"+dateFormatter.format(point.dateTime)+"',"+(point.value==-123.456?"NULL":point.value)+",'OutOfRange') ON DUPLICATE KEY UPDATE value="+(point.value==-123.456?"NULL":point.value));
									Date Time4 = new Date();
									total4Time += Time4.getTime() - Time2.getTime();
								}
							}
							else{
								logWindow.println("Error occured when writing issue log for point: "+point.dateTime+" "+point.value+". Issue was not recorded.");
							}
							newRecordIDQuery.close();
						} catch (SQLException sE){
							sE.printStackTrace();
							logWindow.println("Error occured when writing issue log for point: "+point.dateTime+" "+point.value+". Issue was not recorded.");
						}
						
						
					} catch (SQLException sE) {

						//TODO remove all this, just write an issue to the issue table. OR if the value in the table is NULL, just write over it, and make a low priority conflict entry in the issues table.

						if (sE.getErrorCode()==1062){ //if duplicate entry
							String valueSQL = "SELECT record_id,value FROM data_sa WHERE site_id = "+dataFile.siteID+" AND source_id = "+dataFile.sourceID+" AND date_time = '"+dateFormatter.format(point.dateTime)+"'";
							ResultSet valueRS = MySQL_Statement.executeQuery(valueSQL);
							if (valueRS.next()){
								double value = valueRS.getDouble("value");
								if(Math.abs(value-point.value)>0.001){//if values are not the same
									if (valueRS.wasNull()){
										MySQL_Statement.executeUpdate("UPDATE data_sa SET value="+(point.value==-123.456?"NULL":point.value)+" WHERE site_id = "+dataFile.siteID+" AND source_id = "+dataFile.sourceID+" AND file_id = "+dataFile.fileID+" AND date_time = '"+dateFormatter.format(point.dateTime)+"'");
									}
									else{
										MySQL_Statement.executeUpdate("INSERT INTO issue_points (site_id,source_id,record_id,date_time,value,issue_type) VALUES("+dataFile.siteID+","+dataFile.sourceID+","+Integer.toString(valueRS.getInt("record_id"))+",'"+dateFormatter.format(point.dateTime)+"',"+(point.value==-123.456?"NULL":point.value)+",'Conflict') ON DUPLICATE KEY UPDATE value="+(point.value==-123.456?"NULL":point.value));
									}
								}
								Date Time5 = new Date();
								total5Time += Time5.getTime() - Time1.getTime();
							}
							valueRS.close();
						}
						else{
							logWindow.println("Error occured when writing data: "+point.dateTime+" "+point.value+". Row will be ignored.");
							sE.printStackTrace();
						}
					}
				
			}
			try {
				if (MySQL_Statement!=null){
					MySQL_Statement.close();
				}
			}
			catch (SQLException sE){
				//Statement already closed
			}
			Date end = new Date();
			totalWriteTime += end.getTime()-start.getTime();
			
			if (showGUI){ //if need to update GUI
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						progressBar.setValue(progressBar.getMaximum());
						progressBar.validate();
					}
				});
				progressBar.revalidate();
			}
			
			dataWriterPool.addWriter(this); //add this writer back to the pool	
			Date fin = new Date();
			totalEndTime += fin.getTime()-end.getTime();
		} catch (SQLException sE) {
			logWindow.println("Error occured when connecting to database.");
			sE.printStackTrace();
		}
	}
}
