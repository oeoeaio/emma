package endUseWindow;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;


public class ApplianceTable extends JTable{
	private static final long serialVersionUID = -7951483149923685326L;
	ArrayList<Appliance> applianceList = new ArrayList<Appliance>();
	public ListSelectionModel applianceListModel = this.getSelectionModel();
	public DefaultTableModel applianceTableModel = (DefaultTableModel)this.getModel();
	String[] columnHeaders;
	Connection dbConn;
	Site site;
	
	public ApplianceTable(int selectionMode,String[] columnHeaders){
		this.columnHeaders = columnHeaders;
		this.setSelectionMode(selectionMode);
		this.setColumnSelectionAllowed(false);
		this.setRowSelectionAllowed(true);
		this.applianceTableModel.setColumnIdentifiers(columnHeaders);
		this.applianceListModel.addListSelectionListener(this);
	}
	
	public void update(Connection dbConn,Site site){
		this.dbConn = dbConn;
		this.site = site;
		//greyTable();
		Thread fetchApplianceList = new Thread(new FetchApplianceList());
		fetchApplianceList.start();
	}
	
	class FetchApplianceList extends Thread{
		
		public void run(){
			try {
				applianceList.clear();
				ResultSet applianceRS = dbConn.createStatement().executeQuery("SELECT sources.source_name,appliances.* FROM appliances LEFT JOIN sources ON sources.source_id = appliances.source_id WHERE appliances.site_id = '"+site.siteID+"' ORDER BY source_id");
				if (applianceRS.next()){
					applianceRS.beforeFirst(); //reset cursor position
					while (applianceRS.next()){
						applianceList.add(new Appliance(site,applianceRS.getString("source_id"),applianceRS.getString("source_name"),applianceRS.getString("circuit_id"),applianceRS.getString("room_id"),applianceRS.getString("appliance_group"),applianceRS.getString("appliance_type"),applianceRS.getString("brand"),applianceRS.getString("model"),applianceRS.getString("serial_no"),applianceRS.getString("connection_type"),applianceRS.getString("control"),applianceRS.getString("switch_type"),applianceRS.getString("display"),applianceRS.getString("eps"),applianceRS.getString("delay_start"),applianceRS.getString("on_w"),applianceRS.getString("as_w"),applianceRS.getString("ps_w"),applianceRS.getString("off_w"),applianceRS.getString("ds_w"),applianceRS.getString("year_of_purchase"),applianceRS.getString("usage_amount"),applianceRS.getString("usage_units"),applianceRS.getString("feature1"),applianceRS.getString("feature2"),applianceRS.getString("feature3"),applianceRS.getString("feature4"),applianceRS.getString("feature5"),applianceRS.getString("notes")));
					}
					SwingUtilities.invokeLater(new PopulateTable());
				}
				else{
					applianceTableModel.setNumRows(0);
					applianceList.clear();
					//TODO add error message here
					//errorMessage = "No issue events found.";
					//IssueTable.this.repaint();
				}
			} catch (SQLException e) {
				applianceTableModel.setNumRows(0);
				applianceList.clear();
				//TODO add error message here
				//errorMessage = "A problem occured when retrieving issue events.";
				//IssueTable.this.repaint();
				e.printStackTrace();
			}
		}
	}
	
	class PopulateTable extends Thread { //must be run in swing utilities
		public void run() {
			applianceListModel.removeListSelectionListener(ApplianceTable.this);
			applianceTableModel.setNumRows(0);
			for (int i=0;i<applianceList.size();i++){
				if (Arrays.equals(columnHeaders,new String[] {"Source ID","Source Name","Circuit ID","Room ID","Appliance Group","Appliance Type","Brand","Model"})){
					applianceTableModel.addRow(new String[] {applianceList.get(i).sourceID,applianceList.get(i).sourceName,applianceList.get(i).circuitID,applianceList.get(i).roomID,applianceList.get(i).applianceGroup,applianceList.get(i).applianceType,applianceList.get(i).brand,applianceList.get(i).model});
				}
				else{
					JOptionPane.showMessageDialog(null, "Could not display data. Unexpected number of columns. ","Error Displaying Data",JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
			//ungrey table here
			applianceListModel.addListSelectionListener(ApplianceTable.this);
		}
	}
	
	void greyTable(){
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				//TODO GREY TABLE greyTable();
			}
		});
	}
	
	void unGreyTable(){
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				//TODO GREY TABLE greyTable();
			}
		});
	}
	
	public boolean isCellEditable(int rowIndex, int vColIndex) {
		return false;
    }
}
