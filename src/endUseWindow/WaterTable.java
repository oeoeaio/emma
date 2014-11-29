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


public class WaterTable extends JTable{
	private static final long serialVersionUID = -7951483149923685326L;
	ArrayList<Water> waterList = new ArrayList<Water>();
	public ListSelectionModel waterListModel = this.getSelectionModel();
	public DefaultTableModel waterTableModel = (DefaultTableModel)this.getModel();
	String[] columnHeaders;
	Connection dbConn;
	Site site;
	
	public WaterTable(int selectionMode,String[] columnHeaders){
		this.columnHeaders = columnHeaders;
		this.setSelectionMode(selectionMode);
		this.setColumnSelectionAllowed(false);
		this.setRowSelectionAllowed(true);
		this.waterTableModel.setColumnIdentifiers(columnHeaders);
		this.waterListModel.addListSelectionListener(this);
	}
	
	public void update(Connection dbConn,Site site){
		this.dbConn = dbConn;
		this.site = site;
		//greyTable();
		Thread fetchWaterList = new Thread(new FetchWaterList());
		fetchWaterList.start();
	}
	
	class FetchWaterList extends Thread{
		
		public void run(){
			try {
				waterList.clear();
				ResultSet waterRS = dbConn.createStatement().executeQuery("SELECT sources.source_name,water.* FROM water LEFT JOIN sources ON sources.source_id = water.source_id WHERE water.site_id = '"+site.siteID+"' ORDER BY source_id");
				if (waterRS.next()){
					waterRS.beforeFirst(); //reset cursor position
					while (waterRS.next()){
						waterList.add(new Water(site,waterRS.getString("source_id"),waterRS.getString("source_name"),waterRS.getString("notes")));
					}
					SwingUtilities.invokeLater(new PopulateTable());
				}
				else{
					waterTableModel.setNumRows(0);
					waterList.clear();
					//TODO add error message here
					//errorMessage = "No issue events found.";
					//IssueTable.this.repaint();
				}
			} catch (SQLException e) {
				waterTableModel.setNumRows(0);
				waterList.clear();
				//TODO add error message here
				//errorMessage = "A problem occured when retrieving issue events.";
				//IssueTable.this.repaint();
				e.printStackTrace();
			}
		}
	}
	
	class PopulateTable extends Thread { //must be run in swing utilities
		public void run() {
			waterListModel.removeListSelectionListener(WaterTable.this);
			waterTableModel.setNumRows(0);
			for (int i=0;i<waterList.size();i++){
				if (Arrays.equals(columnHeaders,new String[] {"Source ID","Source Name","Notes"})){
					waterTableModel.addRow(new String[] {waterList.get(i).sourceID,waterList.get(i).sourceName,waterList.get(i).notes});
				}
				else{
					JOptionPane.showMessageDialog(null, "Could not display data. Unexpected number of columns. ","Error Displaying Data",JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
			//ungrey table here
			waterListModel.addListSelectionListener(WaterTable.this);
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
