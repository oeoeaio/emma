package management;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class FetchSourcesData extends Thread{
	
	private Connection dbConn;
	DefaultTableModel filesTableModel;
	String siteID;
	String sourceID;
	
	public FetchSourcesData(Connection dbConn,DefaultTableModel filesTableModel,String siteID,String sourceID){
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
				fetchedData = MySQL_Statement.executeQuery("SELECT * FROM sources WHERE site_id = "+siteID+" AND source_id = "+sourceID);
			}
			else if (siteID.matches("\\d{1,10}") && sourceID.matches("")){
				fetchedData = MySQL_Statement.executeQuery("SELECT * FROM sources WHERE site_id = "+siteID);
			}
			else if (siteID.matches("") && sourceID.matches("\\d{1,10}")){
				fetchedData = MySQL_Statement.executeQuery("SELECT * FROM sources WHERE source_id = "+sourceID);
			}
			else{
				fetchedData = MySQL_Statement.executeQuery("SELECT * FROM sources");
			}
			
			while (fetchedData.next()){
				filesTableModel.addRow(new String[] {fetchedData.getString("site_id"),fetchedData.getString("source_id"),(fetchedData.getString("source_name")==null?"":fetchedData.getString("source_name")),fetchedData.getString("source_type"),(fetchedData.getString("brand")==null?"":fetchedData.getString("brand")),(fetchedData.getString("model")==null?"":fetchedData.getString("model")),(fetchedData.getString("serial")==null?"":fetchedData.getString("serial")),(fetchedData.getString("location")==null?"":fetchedData.getString("location"))});
			}
			
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, "An error occured while retrieving source data.","Error Retrieving Data",JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
}
