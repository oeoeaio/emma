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


public class GasTable extends JTable{
	private static final long serialVersionUID = -7951483149923685326L;
	ArrayList<Gas> gasList = new ArrayList<Gas>();
	public ListSelectionModel gasListModel = this.getSelectionModel();
	public DefaultTableModel gasTableModel = (DefaultTableModel)this.getModel();
	String[] columnHeaders;
	Connection dbConn;
	Site site;
	
	public GasTable(int selectionMode,String[] columnHeaders){
		this.columnHeaders = columnHeaders;
		this.setSelectionMode(selectionMode);
		this.setColumnSelectionAllowed(false);
		this.setRowSelectionAllowed(true);
		this.gasTableModel.setColumnIdentifiers(columnHeaders);
		this.gasListModel.addListSelectionListener(this);
	}
	
	public void update(Connection dbConn,Site site){
		this.dbConn = dbConn;
		this.site = site;
		//greyTable();
		Thread fetchGasList = new Thread(new FetchGasList());
		fetchGasList.start();
	}
	
	class FetchGasList extends Thread{
		
		public void run(){
			try {
				gasList.clear();
				ResultSet gasRS = dbConn.createStatement().executeQuery("SELECT sources.source_name,gas.* FROM gas LEFT JOIN sources ON sources.source_id = gas.source_id WHERE gas.site_id = '"+site.siteID+"' ORDER BY source_id");
				if (gasRS.next()){
					gasRS.beforeFirst(); //reset cursor position
					while (gasRS.next()){
						gasList.add(new Gas(site,gasRS.getString("source_id"),gasRS.getString("source_name"),gasRS.getString("notes")));
					}
					SwingUtilities.invokeLater(new PopulateTable());
				}
				else{
					gasTableModel.setNumRows(0);
					gasList.clear();
					//TODO add error message here
					//errorMessage = "No issue events found.";
					//IssueTable.this.repaint();
				}
			} catch (SQLException e) {
				gasTableModel.setNumRows(0);
				gasList.clear();
				//TODO add error message here
				//errorMessage = "A problem occured when retrieving issue events.";
				//IssueTable.this.repaint();
				e.printStackTrace();
			}
		}
	}
	
	class PopulateTable extends Thread { //must be run in swing utilities
		public void run() {
			gasListModel.removeListSelectionListener(GasTable.this);
			gasTableModel.setNumRows(0);
			for (int i=0;i<gasList.size();i++){
				if (Arrays.equals(columnHeaders,new String[] {"Source ID","Source Name","Notes"})){
					gasTableModel.addRow(new String[] {gasList.get(i).sourceID,gasList.get(i).sourceName,gasList.get(i).notes});
				}
				else{
					JOptionPane.showMessageDialog(null, "Could not display data. Unexpected number of columns. ","Error Displaying Data",JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
			//ungrey table here
			gasListModel.addListSelectionListener(GasTable.this);
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
