package endUseWindow;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class SiteTable extends JTable{
	private static final long serialVersionUID = 233850392634466700L;
	public ArrayList<Site> siteList = new ArrayList<Site>();
	public ListSelectionModel siteListModel = this.getSelectionModel();
	public DefaultTableModel siteTableModel = (DefaultTableModel)this.getModel();
	String[] columnHeaders;
	Connection dbConn;
	
	public SiteTable(String[] columnHeaders){
		this.columnHeaders = columnHeaders;
		this.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
		this.setColumnSelectionAllowed(false);
		this.setRowSelectionAllowed(true);
		this.siteTableModel.setColumnIdentifiers(columnHeaders);
		this.siteListModel.addListSelectionListener(this);
	}
	
	public void updateList(Connection dbConn){
		this.dbConn = dbConn;
		//greyTable();
		Thread fetchSiteList = new Thread(new FetchSiteList());
		fetchSiteList.start();
	}
	
	class FetchSiteList extends Thread{
		public void run(){
			try {
				siteList.clear();
				ResultSet siteRS = dbConn.createStatement().executeQuery("SELECT * FROM sites ORDER BY site_name");				
				if (siteRS.next()){
					siteRS.beforeFirst(); //reset cursor position
					while (siteRS.next()){
						siteList.add(new Site(siteRS.getString("site_id"),siteRS.getString("site_name"),siteRS.getString("concentrator"),siteRS.getString("start_date"),siteRS.getString("end_date"),siteRS.getString("given_name"),siteRS.getString("surname"),siteRS.getString("suburb"),siteRS.getString("state")));
					}
					SwingUtilities.invokeLater(new PopulateTable());
				}
				else{
					siteTableModel.setNumRows(0);
					siteList.clear();
					//TODO add error message here
					//errorMessage = "No issue events found.";
					//IssueTable.this.repaint();
				}
			} catch (SQLException e) {
				siteTableModel.setNumRows(0);
				siteList.clear();
				//TODO add error message here
				//errorMessage = "A problem occured when retrieving issue events.";
				//IssueTable.this.repaint();
				e.printStackTrace();
			}
		}
	}
	
	class PopulateTable extends Thread { //must be run in swing utilities
		public void run() {
			siteListModel.removeListSelectionListener(SiteTable.this);
			siteTableModel.setNumRows(0);
			for (int i=0;i<siteList.size();i++){
				
				if (Arrays.equals(columnHeaders,new String[] {"Site ID","Site Name","Given Name","Surname","Suburb","State"})){
					siteTableModel.addRow(new String[] {siteList.get(i).getSiteID(),siteList.get(i).getSiteName(),siteList.get(i).getGivenName(),siteList.get(i).getSurname(),siteList.get(i).getSuburb(),siteList.get(i).getState()});
				}
				else if (Arrays.equals(columnHeaders,new String[] {"Site ID","Site Name","Given Name","Surname"})){
					siteTableModel.addRow(new String[] {siteList.get(i).getSiteID(),siteList.get(i).getSiteName(),siteList.get(i).getGivenName(),siteList.get(i).getSurname()});
				}
				else if (Arrays.equals(columnHeaders,new String[] {"Site ID","Site Name"})){
					siteTableModel.addRow(new String[] {siteList.get(i).getSiteID(),siteList.get(i).getSiteName()});
				}
				else{
					JOptionPane.showMessageDialog(null, "Could not display data. Unexpected number of columns. ","Error Displaying Data",JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
			//ungrey table here
			siteListModel.addListSelectionListener(SiteTable.this);
		}
	}
	
	
	
	/*
	
	public void updateSites(final Connection dbConn,ListSelectionListener lsListener) throws {
		siteListModel.removeListSelectionListener(lsListener);
		//grey table here
		ResultSet siteRS = dbConn.createStatement().executeQuery("SELECT * FROM sites ORDER BY site_id");		
		populateList(siteRS, lsListener);
	}
	
	void updateSites(Connection dbConn,String siteID) throws SQLException{
		siteListModel.removeListSelectionListener(this);
		//grey table here
		ResultSet siteRS =  dbConn.createStatement().executeQuery("SELECT * FROM sites WHERE CAST(site_id AS TEXT) LIKE '"+siteID+"%'");		
		populateList(siteRS, this);
	}

	private void populateList(final ResultSet fetchedData,final ListSelectionListener lsListener) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				siteTableModel.setNumRows(0);
				try {
					while (fetchedData.next()){
						if (siteTableModel.getColumnCount()==6){
							siteTableModel.addRow(new String[] {fetchedData.getString("site_id"),(fetchedData.getString("site_name")==null?"":fetchedData.getString("site_name")),(fetchedData.getString("given_name")==null?"":fetchedData.getString("given_name")),(fetchedData.getString("surname")==null?"":fetchedData.getString("surname")),(fetchedData.getString("suburb")==null?"":fetchedData.getString("suburb")),(fetchedData.getString("state")==null?"":fetchedData.getString("state"))});
						}
						else{
							siteTableModel.addRow(new String[] {fetchedData.getString("site_id"),(fetchedData.getString("site_name")==null?"":fetchedData.getString("site_name")),(fetchedData.getString("given_name")==null?"":fetchedData.getString("given_name")),(fetchedData.getString("surname")==null?"":fetchedData.getString("surname"))});
						}
					}
				} catch (SQLException sE) {
					sE.printStackTrace();
					JOptionPane.showMessageDialog(null, "An error occured while retrieving site data.","Error Retrieving Data",JOptionPane.ERROR_MESSAGE);
				}
				//ungrey table here
				siteListModel.addListSelectionListener(lsListener);
			}
		});
	}*/
	
	public boolean isCellEditable(int rowIndex, int vColIndex) {
		return false;
    }
}