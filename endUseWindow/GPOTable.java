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


public class GPOTable extends JTable{
	private static final long serialVersionUID = -7951483149923685326L;
	ArrayList<GPO> gpoList = new ArrayList<GPO>();
	public ListSelectionModel gpoListModel = this.getSelectionModel();
	public DefaultTableModel gpoTableModel = (DefaultTableModel)this.getModel();
	String[] columnHeaders;
	Connection dbConn;
	String siteID;
	
	public GPOTable(int selectionMode,String[] columnHeaders){
		this.columnHeaders = columnHeaders;
		this.setSelectionMode(selectionMode);
		this.setColumnSelectionAllowed(false);
		this.setRowSelectionAllowed(true);
		this.gpoTableModel.setColumnIdentifiers(columnHeaders);
		this.gpoListModel.addListSelectionListener(this);
	}
	
	public void update(Connection dbConn,String siteID){
		this.dbConn = dbConn;
		this.siteID = siteID;
		//greyTable();
		Thread fetchGPOList = new Thread(new FetchGPOList());
		fetchGPOList.start();
	}
	
	class FetchGPOList extends Thread{
		
		public void run(){
			try {
				gpoList.clear();
				ResultSet gpoRS = dbConn.createStatement().executeQuery("SELECT * FROM gpos WHERE gpos.site_id = '"+siteID+"' ORDER BY gpo_id");
				if (gpoRS.next()){
					gpoRS.beforeFirst(); //reset cursor position
					while (gpoRS.next()){
						gpoList.add(new GPO(gpoRS.getString("gpo_id"),gpoRS.getString("gpo_name"),gpoRS.getString("circuit_id"),gpoRS.getString("room_id"),gpoRS.getString("notes")));
					}
					SwingUtilities.invokeLater(new PopulateTable());
				}
				else{
					gpoTableModel.setNumRows(0);
					gpoList.clear();
					//TODO add error message here
					//errorMessage = "No issue events found.";
					//IssueTable.this.repaint();
				}
			} catch (SQLException e) {
				gpoTableModel.setNumRows(0);
				gpoList.clear();
				//TODO add error message here
				//errorMessage = "A problem occured when retrieving issue events.";
				//IssueTable.this.repaint();
				e.printStackTrace();
			}
		}
	}
	
	class PopulateTable extends Thread { //must be run in swing utilities
		public void run() {
			gpoListModel.removeListSelectionListener(GPOTable.this);
			gpoTableModel.setNumRows(0);
			for (int i=0;i<gpoList.size();i++){
				if (Arrays.equals(columnHeaders,new String[] {"GPO ID","GPO Name","Circuit ID","Room ID","Notes"})){
					gpoTableModel.addRow(new String[] {gpoList.get(i).gpoID,gpoList.get(i).gpoName,gpoList.get(i).circuitID,gpoList.get(i).roomID,gpoList.get(i).notes});
				}
				else{
					JOptionPane.showMessageDialog(null, "Could not display data. Unexpected number of columns. ","Error Displaying Data",JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
			//ungrey table here
			gpoListModel.addListSelectionListener(GPOTable.this);
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
