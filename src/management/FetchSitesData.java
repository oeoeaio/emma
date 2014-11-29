package management;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class FetchSitesData extends Thread{
	
	SitesPanel sitesPanel;
	private Connection dbConn;
	DefaultTableModel sitesTableModel;
	String siteID;
	
	FetchSitesData(SitesPanel sitesPanel,Connection dbConn,DefaultTableModel sitesTableModel,String siteID){
		this.sitesPanel = sitesPanel;
		this.dbConn = dbConn;
		this.sitesTableModel = sitesTableModel;
		this.siteID = siteID;
	}
	
	public void run(){
		try {
			Statement MySQL_Statement = dbConn.createStatement();
			ResultSet fetchedData = null;
			if (siteID.matches("\\d{1,10}")){
				fetchedData = MySQL_Statement.executeQuery("SELECT * FROM sites WHERE site_id = "+siteID);
			}
			else{
				fetchedData = MySQL_Statement.executeQuery("SELECT * FROM sites");
			}
			
			populateModel(fetchedData);
			sitesPanel.unGreyTable();
			
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, "An error occured while retrieving site data.","Error Retrieving Data",JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	private void populateModel(final ResultSet fetchedData) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				try {
					while (fetchedData.next()){
						sitesTableModel.addRow(new String[] {fetchedData.getString("site_id"),(fetchedData.getString("given_name")==null?"":fetchedData.getString("given_name")),(fetchedData.getString("surname")==null?"":fetchedData.getString("surname")),(fetchedData.getString("suburb")==null?"":fetchedData.getString("suburb")),(fetchedData.getString("state")==null?"":fetchedData.getString("state"))});
					}
				} catch (SQLException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, "An error occured while retrieving site data.","Error Retrieving Data",JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}
}
