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


public class RoomTable extends JTable{
	private static final long serialVersionUID = -7951483149923685326L;
	ArrayList<Room> roomList = new ArrayList<Room>();
	public ListSelectionModel roomListModel = this.getSelectionModel();
	public DefaultTableModel roomTableModel = (DefaultTableModel)this.getModel();
	String[] columnHeaders;
	Connection dbConn;
	String siteID;
	
	public RoomTable(int selectionMode,String[] columnHeaders){
		this.columnHeaders = columnHeaders;
		this.setSelectionMode(selectionMode);
		this.setColumnSelectionAllowed(false);
		this.setRowSelectionAllowed(true);
		this.roomTableModel.setColumnIdentifiers(columnHeaders);
		this.roomListModel.addListSelectionListener(this);
	}
	
	public void update(Connection dbConn,String siteID){
		this.dbConn = dbConn;
		this.siteID = siteID;
		//greyTable();
		Thread fetchRoomList = new Thread(new FetchRoomList());
		fetchRoomList.start();
	}
	
	class FetchRoomList extends Thread{
		
		public void run(){
			try {
				roomList.clear();
				ResultSet roomRS = dbConn.createStatement().executeQuery("SELECT * FROM rooms WHERE site_id = '"+siteID+"' ORDER BY room_id");
				if (roomRS.next()){
					roomRS.beforeFirst(); //reset cursor position
					while (roomRS.next()){
						roomList.add(new Room(roomRS.getString("room_id"),roomRS.getString("room_number"),roomRS.getString("room_type"),roomRS.getString("area"),roomRS.getString("notes")));
					}
					SwingUtilities.invokeLater(new PopulateTable());
				}
				else{
					roomTableModel.setNumRows(0);
					roomList.clear();
					//TODO add error message here
					//errorMessage = "No issue events found.";
					//IssueTable.this.repaint();
				}
			} catch (SQLException e) {
				roomTableModel.setNumRows(0);
				roomList.clear();
				//TODO add error message here
				//errorMessage = "A problem occured when retrieving issue events.";
				//IssueTable.this.repaint();
				e.printStackTrace();
			}
		}
	}
	
	class PopulateTable extends Thread { //must be run in swing utilities
		public void run() {
			roomListModel.removeListSelectionListener(RoomTable.this);
			roomTableModel.setNumRows(0);
			for (int i=0;i<roomList.size();i++){
				if (Arrays.equals(columnHeaders,new String[] {"Room ID","Room Number","Room Type","Area","Notes"})){
					roomTableModel.addRow(new String[] {roomList.get(i).roomID,roomList.get(i).roomNumber,roomList.get(i).roomType,roomList.get(i).area,roomList.get(i).notes});
				}
				else{
					JOptionPane.showMessageDialog(null, "Could not display data. Unexpected number of columns. ","Error Displaying Data",JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
			//ungrey table here
			roomListModel.addListSelectionListener(RoomTable.this);
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
