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


public class LightTable extends JTable{
	private static final long serialVersionUID = -7951483149923685326L;
	ArrayList<Light> lightList = new ArrayList<Light>();
	public ListSelectionModel lightListModel = this.getSelectionModel();
	public DefaultTableModel lightTableModel = (DefaultTableModel)this.getModel();
	String[] columnHeaders;
	Connection dbConn;
	Site site;
	
	public LightTable(int selectionMode,String[] columnHeaders){
		this.columnHeaders = columnHeaders;
		this.setSelectionMode(selectionMode);
		this.setColumnSelectionAllowed(false);
		this.setRowSelectionAllowed(true);
		this.lightTableModel.setColumnIdentifiers(columnHeaders);
		this.lightListModel.addListSelectionListener(this);
	}
	
	public void update(Connection dbConn,Site site){
		this.dbConn = dbConn;
		this.site = site;
		//greyTable();
		Thread fetchLightList = new Thread(new FetchLightList());
		fetchLightList.start();
	}
	
	class FetchLightList extends Thread{
		
		public void run(){
			try {
				lightList.clear();
				ResultSet lightRS = dbConn.createStatement().executeQuery("SELECT sources.source_name,lights.* FROM lights LEFT JOIN sources ON sources.source_id = lights.source_id WHERE lights.site_id = '"+site.siteID+"' ORDER BY source_id");
				if (lightRS.next()){
					lightRS.beforeFirst(); //reset cursor position
					while (lightRS.next()){
						lightList.add(new Light(site,lightRS.getString("source_id"),lightRS.getString("source_name"),lightRS.getString("circuit_id"),lightRS.getString("room_id"),lightRS.getString("wattage"),lightRS.getString("notes")));
					}
					SwingUtilities.invokeLater(new PopulateTable());
				}
				else{
					lightTableModel.setNumRows(0);
					lightList.clear();
					//TODO add error message here
					//errorMessage = "No issue events found.";
					//IssueTable.this.repaint();
				}
			} catch (SQLException e) {
				lightTableModel.setNumRows(0);
				lightList.clear();
				//TODO add error message here
				//errorMessage = "A problem occured when retrieving issue events.";
				//IssueTable.this.repaint();
				e.printStackTrace();
			}
		}
	}
	
	class PopulateTable extends Thread { //must be run in swing utilities
		public void run() {
			lightListModel.removeListSelectionListener(LightTable.this);
			lightTableModel.setNumRows(0);
			for (int i=0;i<lightList.size();i++){
				if (Arrays.equals(columnHeaders,new String[] {"Source ID","Source Name","Circuit ID","Room ID","Wattage"})){
					lightTableModel.addRow(new String[] {lightList.get(i).sourceID,lightList.get(i).sourceName,lightList.get(i).circuitID,lightList.get(i).roomID,lightList.get(i).wattage});
				}
				else{
					JOptionPane.showMessageDialog(null, "Could not display data. Unexpected number of columns. ","Error Displaying Data",JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
			//ungrey table here
			lightListModel.addListSelectionListener(LightTable.this);
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
