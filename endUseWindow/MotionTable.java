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


public class MotionTable extends JTable{
	private static final long serialVersionUID = -7951483149923685326L;
	ArrayList<Motion> motionList = new ArrayList<Motion>();
	public ListSelectionModel motionListModel = this.getSelectionModel();
	public DefaultTableModel motionTableModel = (DefaultTableModel)this.getModel();
	String[] columnHeaders;
	Connection dbConn;
	Site site;
	
	public MotionTable(int selectionMode,String[] columnHeaders){
		this.columnHeaders = columnHeaders;
		this.setSelectionMode(selectionMode);
		this.setColumnSelectionAllowed(false);
		this.setRowSelectionAllowed(true);
		this.motionTableModel.setColumnIdentifiers(columnHeaders);
		this.motionListModel.addListSelectionListener(this);
	}
	
	public void update(Connection dbConn,Site site){
		this.dbConn = dbConn;
		this.site = site;
		//greyTable();
		Thread fetchMotionList = new Thread(new FetchMotionList());
		fetchMotionList.start();
	}
	
	class FetchMotionList extends Thread{
		
		public void run(){
			try {
				motionList.clear();
				ResultSet motionRS = dbConn.createStatement().executeQuery("SELECT sources.source_name,motion.* FROM motion LEFT JOIN sources ON sources.source_id = motion.source_id WHERE motion.site_id = '"+site.siteID+"' ORDER BY source_id");
				if (motionRS.next()){
					motionRS.beforeFirst(); //reset cursor position
					while (motionRS.next()){
						motionList.add(new Motion(site,motionRS.getString("source_id"),motionRS.getString("source_name"),motionRS.getString("room_id"),motionRS.getString("notes")));
					}
					SwingUtilities.invokeLater(new PopulateTable());
				}
				else{
					motionTableModel.setNumRows(0);
					motionList.clear();
					//TODO add error message here
					//errorMessage = "No issue events found.";
					//IssueTable.this.repaint();
				}
			} catch (SQLException e) {
				motionTableModel.setNumRows(0);
				motionList.clear();
				//TODO add error message here
				//errorMessage = "A problem occured when retrieving issue events.";
				//IssueTable.this.repaint();
				e.printStackTrace();
			}
		}
	}
	
	class PopulateTable extends Thread { //must be run in swing utilities
		public void run() {
			motionListModel.removeListSelectionListener(MotionTable.this);
			motionTableModel.setNumRows(0);
			for (int i=0;i<motionList.size();i++){
				if (Arrays.equals(columnHeaders,new String[] {"Source ID","Source Name","Room ID","Notes"})){
					motionTableModel.addRow(new String[] {motionList.get(i).sourceID,motionList.get(i).sourceName,motionList.get(i).roomID,motionList.get(i).notes});
				}
				else{
					JOptionPane.showMessageDialog(null, "Could not display data. Unexpected number of columns. ","Error Displaying Data",JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
			//ungrey table here
			motionListModel.addListSelectionListener(MotionTable.this);
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
