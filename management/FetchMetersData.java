package management;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class FetchMetersData extends Thread{
	
	MetersPanel metersPanel;
	private Connection dbConn;
	DefaultTableModel metersTableModel;
	
	FetchMetersData(MetersPanel metersPanel,Connection dbConn,DefaultTableModel metersTableModel,String siteID){
		this.metersPanel = metersPanel;
		this.dbConn = dbConn;
		this.metersTableModel = metersTableModel;
	}
	
	public void run(){
		try {
			Statement MySQL_Statement = dbConn.createStatement();
			ResultSet fetchedData = MySQL_Statement.executeQuery("SELECT meter_sn,meter_type,current_site,UNIX_TIMESTAMP(date_installed) AS date_installed,UNIX_TIMESTAMP(date_full) AS date_full,frequency,UNIX_TIMESTAMP(battery_installed) AS battery_installed,UNIX_TIMESTAMP(battery_replace) AS battery_replace FROM meters");
			SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			
			while (fetchedData.next()){
				metersTableModel.addRow(new String[] {fetchedData.getString("meter_sn"),fetchedData.getString("meter_type"),fetchedData.getString("current_site"),(fetchedData.getLong("date_installed")==0?"":sqlDateFormatter.format(fetchedData.getLong("date_installed")*1000)),(fetchedData.getLong("date_full")==0?"":sqlDateFormatter.format(fetchedData.getLong("date_full")*1000)),fetchedData.getString("frequency"),(fetchedData.getLong("battery_installed")==0?"":sqlDateFormatter.format(fetchedData.getLong("battery_installed")*1000)),(fetchedData.getLong("battery_replace")==0?"":sqlDateFormatter.format(fetchedData.getLong("battery_replace")*1000))});
			}
			metersPanel.unGreyTable();
			
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, "An error occured while retrieving meter data.","Error Retrieving Data",JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
}
