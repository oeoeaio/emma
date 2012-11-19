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


public class TemperatureTable extends JTable{
	private static final long serialVersionUID = -7951483149923685326L;
	ArrayList<Temperature> temperatureList = new ArrayList<Temperature>();
	public ListSelectionModel temperatureListModel = this.getSelectionModel();
	public DefaultTableModel temperatureTableModel = (DefaultTableModel)this.getModel();
	String[] columnHeaders;
	Connection dbConn;
	Site site;
	
	public TemperatureTable(int selectionMode,String[] columnHeaders){
		this.columnHeaders = columnHeaders;
		this.setSelectionMode(selectionMode);
		this.setColumnSelectionAllowed(false);
		this.setRowSelectionAllowed(true);
		this.temperatureTableModel.setColumnIdentifiers(columnHeaders);
		this.temperatureListModel.addListSelectionListener(this);
	}
	
	public void update(Connection dbConn,Site site){
		this.dbConn = dbConn;
		this.site = site;
		//greyTable();
		Thread fetchTemperatureList = new Thread(new FetchTemperatureList());
		fetchTemperatureList.start();
	}
	
	class FetchTemperatureList extends Thread{
		
		public void run(){
			try {
				temperatureList.clear();
				ResultSet temperatureRS = dbConn.createStatement().executeQuery("SELECT sources.source_name,temperatures.* FROM temperatures LEFT JOIN sources ON sources.source_id = temperatures.source_id WHERE temperatures.site_id = '"+site.siteID+"' ORDER BY source_id");
				if (temperatureRS.next()){
					temperatureRS.beforeFirst(); //reset cursor position
					while (temperatureRS.next()){
						temperatureList.add(new Temperature(site,temperatureRS.getString("source_id"),temperatureRS.getString("source_name"),temperatureRS.getString("room_id"),temperatureRS.getString("notes")));
					}
					SwingUtilities.invokeLater(new PopulateTable());
				}
				else{
					temperatureTableModel.setNumRows(0);
					temperatureList.clear();
					//TODO add error message here
					//errorMessage = "No issue events found.";
					//IssueTable.this.repaint();
				}
			} catch (SQLException e) {
				temperatureTableModel.setNumRows(0);
				temperatureList.clear();
				//TODO add error message here
				//errorMessage = "A problem occured when retrieving issue events.";
				//IssueTable.this.repaint();
				e.printStackTrace();
			}
		}
	}
	
	class PopulateTable extends Thread { //must be run in swing utilities
		public void run() {
			temperatureListModel.removeListSelectionListener(TemperatureTable.this);
			temperatureTableModel.setNumRows(0);
			for (int i=0;i<temperatureList.size();i++){
				if (Arrays.equals(columnHeaders,new String[] {"Source ID","Source Name","Room ID","Notes"})){
					temperatureTableModel.addRow(new String[] {temperatureList.get(i).sourceID,temperatureList.get(i).sourceName,temperatureList.get(i).roomID,temperatureList.get(i).notes});
				}
				else{
					JOptionPane.showMessageDialog(null, "Could not display data. Unexpected number of columns. ","Error Displaying Data",JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
			//ungrey table here
			temperatureListModel.addListSelectionListener(TemperatureTable.this);
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
