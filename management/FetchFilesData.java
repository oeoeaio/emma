package management;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class FetchFilesData extends Thread{
	
	FilesPanel filesPanel;
	private Connection dbConn;
	DefaultTableModel filesTableModel;
	String siteID;
	String sourceID;
	
	FetchFilesData(FilesPanel filesPanel,Connection dbConn,DefaultTableModel filesTableModel,String siteID,String sourceID){
		this.filesPanel = filesPanel;
		this.dbConn = dbConn;
		this.filesTableModel = filesTableModel;
		this.siteID = siteID;
		this.sourceID = sourceID;
	}
	
	public void run(){
		try {
			Statement MySQL_Statement = dbConn.createStatement();
			ResultSet fetchedData = null;
			if (siteID.matches("\\d{1,10}") && sourceID.matches("\\d{1,10}")){
				fetchedData = MySQL_Statement.executeQuery("SELECT files.*,UNIX_TIMESTAMP(min_tab.min_date) AS min_date,UNIX_TIMESTAMP(max_tab.max_date) AS max_date FROM files LEFT JOIN (SELECT file_id,MAX(date_time) AS max_date FROM data_sa GROUP BY file_id) AS max_tab ON files.file_id = max_tab.file_id LEFT JOIN (SELECT file_id,MIN(date_time) AS min_date FROM data_sa GROUP BY file_id) AS min_tab ON files.file_id = min_tab.file_id WHERE site_id = "+siteID+" AND source_id = "+sourceID);
			}
			else if (siteID.matches("\\d{1,10}") && sourceID.matches("")){
				fetchedData = MySQL_Statement.executeQuery("SELECT files.*,UNIX_TIMESTAMP(min_tab.min_date) AS min_date,UNIX_TIMESTAMP(max_tab.max_date) AS max_date FROM files LEFT JOIN (SELECT file_id,MAX(date_time) AS max_date FROM data_sa GROUP BY file_id) AS max_tab ON files.file_id = max_tab.file_id LEFT JOIN (SELECT file_id,MIN(date_time) AS min_date FROM data_sa GROUP BY file_id) AS min_tab ON files.file_id = min_tab.file_id WHERE site_id = "+siteID);
			}
			else if (siteID.matches("") && sourceID.matches("\\d{1,10}")){
				//fetchedData = MySQL_Statement.executeQuery("SELECT files.*,UNIX_TIMESTAMP(min_tab.min_date) AS min_date,UNIX_TIMESTAMP(max_tab.max_date) AS max_date,(@site_id:=(SELECT site_id FROM sources WHERE source_id = "+sourceID+")) AS site_id_ignore FROM files LEFT JOIN (SELECT file_id,MAX(date_time) AS max_date FROM data_sa WHERE site_id = @site_id AND source_id = "+sourceID+" GROUP BY file_id) AS max_tab ON files.file_id = max_tab.file_id LEFT JOIN (SELECT file_id,MIN(date_time) AS min_date FROM data_sa WHERE site_id = @site_id AND source_id = "+sourceID+" GROUP BY file_id) AS min_tab ON files.file_id = min_tab.file_id WHERE files.site_id = @site_id AND files.source_id = "+sourceID);
				fetchedData = MySQL_Statement.executeQuery("SELECT files.*,UNIX_TIMESTAMP(min_tab.min_date) AS min_date,UNIX_TIMESTAMP(max_tab.max_date) AS max_date FROM files LEFT JOIN (SELECT file_id,MAX(date_time) AS max_date FROM data_sa WHERE source_id = "+sourceID+" GROUP BY file_id) AS max_tab ON files.file_id = max_tab.file_id LEFT JOIN (SELECT file_id,MIN(date_time) AS min_date FROM data_sa WHERE source_id = "+sourceID+" GROUP BY file_id) AS min_tab ON files.file_id = min_tab.file_id WHERE files.source_id = "+sourceID);
			}
			else{
				fetchedData = MySQL_Statement.executeQuery("SELECT files.*,UNIX_TIMESTAMP(min_tab.min_date) AS min_date,UNIX_TIMESTAMP(max_tab.max_date) AS max_date FROM files LEFT JOIN (SELECT file_id,MAX(date_time) AS max_date FROM data_sa GROUP BY file_id) AS max_tab ON files.file_id = max_tab.file_id LEFT JOIN (SELECT file_id,MIN(date_time) AS min_date FROM data_sa GROUP BY file_id) AS min_tab ON files.file_id = min_tab.file_id");
			}
			
			SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			
			filesTableModel.setNumRows(0);
			while (fetchedData.next()){
				filesTableModel.addRow(new String[] {fetchedData.getString("file_id"),fetchedData.getString("site_id"),fetchedData.getString("source_id"),fetchedData.getString("file_name"),fetchedData.getString("meter_sn"),fetchedData.getString("frequency"),sqlDateFormatter.format(fetchedData.getLong("min_date")*1000),sqlDateFormatter.format(fetchedData.getLong("max_date")*1000)});
			}
			filesPanel.unGreyTable();
			
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, "An error occured while retrieving file data.","Error Retrieving Data",JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
}
