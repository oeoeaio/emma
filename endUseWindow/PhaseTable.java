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


public class PhaseTable extends JTable{
	private static final long serialVersionUID = -7951483149923685326L;
	ArrayList<Phase> phaseList = new ArrayList<Phase>();
	public ListSelectionModel phaseListModel = this.getSelectionModel();
	public DefaultTableModel phaseTableModel = (DefaultTableModel)this.getModel();
	String[] columnHeaders;
	Connection dbConn;
	Site site;
	
	public PhaseTable(int selectionMode,String[] columnHeaders){
		this.columnHeaders = columnHeaders;
		this.setSelectionMode(selectionMode);
		this.setColumnSelectionAllowed(false);
		this.setRowSelectionAllowed(true);
		this.phaseTableModel.setColumnIdentifiers(columnHeaders);
		this.phaseListModel.addListSelectionListener(this);
	}
	
	public void update(Connection dbConn,Site site){
		this.dbConn = dbConn;
		this.site = site;
		//greyTable();
		Thread fetchPhaseList = new Thread(new FetchPhaseList());
		fetchPhaseList.start();
	}
	
	class FetchPhaseList extends Thread{
		
		public void run(){
			try {
				phaseList.clear();
				ResultSet phaseRS = dbConn.createStatement().executeQuery("SELECT sources.source_name,phases.* FROM phases LEFT JOIN sources ON sources.source_id = phases.source_id WHERE phases.site_id = '"+site.siteID+"' ORDER BY source_id");
				if (phaseRS.next()){
					phaseRS.beforeFirst(); //reset cursor position
					while (phaseRS.next()){
						phaseList.add(new Phase(site,phaseRS.getString("source_id"),phaseRS.getString("source_name"),phaseRS.getString("notes")));
					}
					SwingUtilities.invokeLater(new PopulateTable());
				}
				else{
					phaseTableModel.setNumRows(0);
					phaseList.clear();
					//TODO add error message here
					//errorMessage = "No issue events found.";
					//IssueTable.this.repaint();
				}
			} catch (SQLException e) {
				phaseTableModel.setNumRows(0);
				phaseList.clear();
				//TODO add error message here
				//errorMessage = "A problem occured when retrieving issue events.";
				//IssueTable.this.repaint();
				e.printStackTrace();
			}
		}
	}
	
	class PopulateTable extends Thread { //must be run in swing utilities
		public void run() {
			phaseListModel.removeListSelectionListener(PhaseTable.this);
			phaseTableModel.setNumRows(0);
			for (int i=0;i<phaseList.size();i++){
				if (Arrays.equals(columnHeaders,new String[] {"Source ID","Source Name","Notes"})){
					phaseTableModel.addRow(new String[] {phaseList.get(i).sourceID,phaseList.get(i).sourceName,phaseList.get(i).notes});
				}
				else{
					JOptionPane.showMessageDialog(null, "Could not display data. Unexpected number of columns. ","Error Displaying Data",JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
			//ungrey table here
			phaseListModel.addListSelectionListener(PhaseTable.this);
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
