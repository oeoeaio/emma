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


public class CircuitTable extends JTable{
	private static final long serialVersionUID = -7951483149923685326L;
	ArrayList<Circuit> circuitList = new ArrayList<Circuit>();
	public ListSelectionModel circuitListModel = this.getSelectionModel();
	public DefaultTableModel circuitTableModel = (DefaultTableModel)this.getModel();
	String[] columnHeaders;
	Connection dbConn;
	Site site;
	
	public CircuitTable(int selectionMode,String[] columnHeaders){
		this.columnHeaders = columnHeaders;
		this.setSelectionMode(selectionMode);
		this.setColumnSelectionAllowed(false);
		this.setRowSelectionAllowed(true);
		this.circuitTableModel.setColumnIdentifiers(columnHeaders);
		this.circuitListModel.addListSelectionListener(this);
	}
	
	public void update(Connection dbConn,Site site){
		this.dbConn = dbConn;
		this.site = site;
		//greyTable();
		Thread fetchCircuitList = new Thread(new FetchCircuitList());
		fetchCircuitList.start();
	}
	
	class FetchCircuitList extends Thread{
		
		public void run(){
			try {
				circuitList.clear();
				ResultSet circuitRS = dbConn.createStatement().executeQuery("SELECT circuits.*,sources.* FROM circuits LEFT JOIN sources ON circuits.source_id = sources.source_id WHERE circuits.site_id = '"+site.siteID+"' ORDER BY source_name");
				if (circuitRS.next()){
					circuitRS.beforeFirst(); //reset cursor position
					while (circuitRS.next()){
						circuitList.add(new Circuit(site,circuitRS.getString("source_id"),circuitRS.getString("source_name"),circuitRS.getString("notes")));
					}
					SwingUtilities.invokeLater(new PopulateTable());
				}
				else{
					circuitTableModel.setNumRows(0);
					circuitList.clear();
					//TODO add error message here
					//errorMessage = "No issue events found.";
					//IssueTable.this.repaint();
				}
			} catch (SQLException e) {
				circuitTableModel.setNumRows(0);
				circuitList.clear();
				//TODO add error message here
				//errorMessage = "A problem occured when retrieving issue events.";
				//IssueTable.this.repaint();
				e.printStackTrace();
			}
		}
	}
	
	class PopulateTable extends Thread { //must be run in swing utilities
		public void run() {
			circuitListModel.removeListSelectionListener(CircuitTable.this);
			circuitTableModel.setNumRows(0);
			for (int i=0;i<circuitList.size();i++){
				if (Arrays.equals(columnHeaders,new String[] {"Source ID","Source Name","Notes"})){
					circuitTableModel.addRow(new String[] {circuitList.get(i).sourceID,circuitList.get(i).sourceName,circuitList.get(i).notes});
				}
				else{
					JOptionPane.showMessageDialog(null, "Could not display data. Unexpected number of columns. ","Error Displaying Data",JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
			//ungrey table here
			circuitListModel.addListSelectionListener(CircuitTable.this);
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
