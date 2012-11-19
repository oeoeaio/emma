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


public class HumidityTable extends JTable{
	private static final long serialVersionUID = -7951483149923685326L;
	ArrayList<Humidity> humidityList = new ArrayList<Humidity>();
	public ListSelectionModel humidityListModel = this.getSelectionModel();
	public DefaultTableModel humidityTableModel = (DefaultTableModel)this.getModel();
	String[] columnHeaders;
	Connection dbConn;
	Site site;
	
	public HumidityTable(int selectionMode,String[] columnHeaders){
		this.columnHeaders = columnHeaders;
		this.setSelectionMode(selectionMode);
		this.setColumnSelectionAllowed(false);
		this.setRowSelectionAllowed(true);
		this.humidityTableModel.setColumnIdentifiers(columnHeaders);
		this.humidityListModel.addListSelectionListener(this);
	}
	
	public void update(Connection dbConn,Site site){
		this.dbConn = dbConn;
		this.site = site;
		//greyTable();
		Thread fetchHumidityList = new Thread(new FetchHumidityList());
		fetchHumidityList.start();
	}
	
	class FetchHumidityList extends Thread{
		
		public void run(){
			try {
				humidityList.clear();
				ResultSet humidityRS = dbConn.createStatement().executeQuery("SELECT sources.source_name,humidities.* FROM humidities LEFT JOIN sources ON sources.source_id = humidities.source_id WHERE humidities.site_id = '"+site.siteID+"' ORDER BY source_id");
				if (humidityRS.next()){
					humidityRS.beforeFirst(); //reset cursor position
					while (humidityRS.next()){
						humidityList.add(new Humidity(site,humidityRS.getString("source_id"),humidityRS.getString("source_name"),humidityRS.getString("room_id"),humidityRS.getString("notes")));
					}
					SwingUtilities.invokeLater(new PopulateTable());
				}
				else{
					humidityTableModel.setNumRows(0);
					humidityList.clear();
					//TODO add error message here
					//errorMessage = "No issue events found.";
					//IssueTable.this.repaint();
				}
			} catch (SQLException e) {
				humidityTableModel.setNumRows(0);
				humidityList.clear();
				//TODO add error message here
				//errorMessage = "A problem occured when retrieving issue events.";
				//IssueTable.this.repaint();
				e.printStackTrace();
			}
		}
	}
	
	class PopulateTable extends Thread { //must be run in swing utilities
		public void run() {
			humidityListModel.removeListSelectionListener(HumidityTable.this);
			humidityTableModel.setNumRows(0);
			for (int i=0;i<humidityList.size();i++){
				if (Arrays.equals(columnHeaders,new String[] {"Source ID","Source Name","Room ID","Notes"})){
					humidityTableModel.addRow(new String[] {humidityList.get(i).sourceID,humidityList.get(i).sourceName,humidityList.get(i).roomID,humidityList.get(i).notes});
				}
				else{
					JOptionPane.showMessageDialog(null, "Could not display data. Unexpected number of columns. ","Error Displaying Data",JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
			//ungrey table here
			humidityListModel.addListSelectionListener(HumidityTable.this);
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
