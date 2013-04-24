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


public class SourceTable extends JTable{
	private static final long serialVersionUID = -7951483149923685326L;
	public ArrayList<Source> sourceList = new ArrayList<Source>();
	public ListSelectionModel sourceListModel = this.getSelectionModel();
	public DefaultTableModel sourceTableModel = (DefaultTableModel)this.getModel();
	String[] columnHeaders;
	Connection dbConn;
	Site site;
	String sourceType;
	String applianceType;
	//TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(this.getModel());
	
	public SourceTable(int selectionMode,String[] columnHeaders){
		this.columnHeaders = columnHeaders;
		this.setSelectionMode(selectionMode);
		this.setColumnSelectionAllowed(false);
		this.setRowSelectionAllowed(true);
		this.sourceTableModel.setColumnIdentifiers(columnHeaders);
		this.sourceListModel.addListSelectionListener(this);
		/*this.setRowSorter(sorter);
		sorter.setComparator(1, new Comparator<String>() {
		    public int compare(String s1, String s2) {
		    	String pfx1 = s1.replaceAll("(^(^[a-zA-Z]))", "");
			    String pfx2 = s2.replaceAll("[^\\d]", "");
		        int num1 = Integer.parseInt(s1.replaceAll("[^\\d]", ""));
		        int num2 = Integer.parseInt(s2.replaceAll("[^\\d]", ""));
		        if (pfx1.compareTo(pfx2) > 0){
		        	return 1;
		        }
		        else if (pfx1.compareTo(pfx2) < 0){
		        	return -1;
		        }
		        else{
		        	if (num1 > num2){
			        	return 1;
			        }
			        else if (num1 < num2){
			        	return -1;
			        }
			        else{
			        	return 0;
			        }
		        }
		    }
		});*/
	}
	
	public void updateList(Connection dbConn,Site site){
		this.dbConn = dbConn;
		this.site = site;
		this.sourceType = "";
		//greyTable();
		Thread fetchSourceList = new Thread(new FetchSourceList());
		fetchSourceList.start();
	}
	
	public void update(Connection dbConn,Site site,String sourceType){
		this.dbConn = dbConn;
		this.site = site;
		this.sourceType = sourceType;
		//greyTable();
		Thread fetchSourceList = new Thread(new FetchSourceList());
		fetchSourceList.start();
	}
	
	public void update(Connection dbConn,Site site,String sourceType,String applianceType){
		this.dbConn = dbConn;
		this.site = site;
		this.sourceType = sourceType;
		this.applianceType = applianceType;
		//greyTable();
		Thread fetchSourceList = new Thread(new FetchSourceList());
		fetchSourceList.start();
	}
	
	class FetchSourceList extends Thread{
		public void run(){
			try {
				sourceList.clear();
				String restrictString = "";
				if (sourceType.equals("appliances")){
					restrictString = "AND source_id IN (SELECT source_id FROM "+sourceType+" WHERE "+(site==null?"":"site_id = "+site.siteID)+" AND appliance_type = '"+applianceType+"')";
				}else if (!sourceType.equals("")){
					restrictString = "AND source_id IN (SELECT source_id FROM "+sourceType+" "+(site==null?"":"WHERE site_id = "+site.siteID)+")";
				}
				
				String whereString = (site==null && restrictString.equals("")?"":"WHERE "+(site==null?"":"site_id = "+site.siteID+" ")+restrictString);
				String joinString = (site==null?"LEFT JOIN sites USING(site_id)":"");
				
				ResultSet sourceRS = dbConn.createStatement().executeQuery("SELECT "+(site==null?"sites.*,":"")+"sources.* FROM sources "+joinString+" "+whereString+" ORDER BY sources.source_type,sources.source_name");
				if (sourceRS.next()){
					sourceRS.beforeFirst(); //reset cursor position
					while (sourceRS.next()){
						sourceList.add(new Source((site==null?new Site(sourceRS.getString("site_id"),sourceRS.getString("site_name"),sourceRS.getString("concentrator"),sourceRS.getString("given_name"),sourceRS.getString("surname"),sourceRS.getString("suburb"),sourceRS.getString("state")):site),sourceRS.getString("source_id"),sourceRS.getString("source_name"),sourceRS.getString("source_type"),sourceRS.getString("measurement_type")));
					}
					SwingUtilities.invokeLater(new PopulateTable());
				}
				else{
					sourceTableModel.setNumRows(0);
					sourceList.clear();
					//TODO add error message here
					//errorMessage = "No issue events found.";
					//IssueTable.this.repaint();
				}
			} catch (SQLException e) {
				sourceTableModel.setNumRows(0);
				sourceList.clear();
				//TODO add error message here
				//errorMessage = "A problem occured when retrieving issue events.";
				//IssueTable.this.repaint();
				e.printStackTrace();
			}
		}
	}
	
	class PopulateTable extends Thread { //must be run in swing utilities
		public void run() {
			sourceListModel.removeListSelectionListener(SourceTable.this);
			sourceTableModel.setNumRows(0);
			for (int i=0;i<sourceList.size();i++){
				if (Arrays.equals(columnHeaders,new String[] {"Source ID","Source Name","Source Type","Measurement Type"})){
					sourceTableModel.addRow(new String[] {sourceList.get(i).getSourceID(),sourceList.get(i).getSourceName(),sourceList.get(i).getSourceType(),sourceList.get(i).getMeasurementType()});
				}
				else if (Arrays.equals(columnHeaders,new String[] {"Source ID","Source Name","Source Type"})){
					sourceTableModel.addRow(new String[] {sourceList.get(i).getSourceID(),sourceList.get(i).getSourceName(),sourceList.get(i).getSourceType()});
				}
				else if (Arrays.equals(columnHeaders,new String[] {"Source ID","Source Name"})){
					sourceTableModel.addRow(new String[] {sourceList.get(i).getSourceID(),sourceList.get(i).getSourceName()});
				}
				else{
					JOptionPane.showMessageDialog(null, "Could not display data. Unexpected number of columns. ","Error Displaying Data",JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
			//ungrey table here
			sourceListModel.addListSelectionListener(SourceTable.this);
			//sorter.sort();
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
	
	
	
	
	/*
	
	public void updateSources(Connection dbConn,SiteTable siteList) throws SQLException{
		sourceListModel.removeListSelectionListener(this);
		if (siteList.siteListModel.isSelectionEmpty()==false && siteList.getValueAt(siteList.getSelectedRow(),0).toString()!=null){
			ResultSet sourceRS = dbConn.createStatement().executeQuery("SELECT * FROM sources WHERE site_id = '"+siteList.getValueAt(siteList.getSelectedRow(),0)+"' ORDER BY source_id");
			System.out.println("SELECT * FROM sources WHERE site_id = '"+siteList.getValueAt(siteList.getSelectedRow(),0)+"' ORDER BY source_id");
			populateList(sourceRS);
		}
		else{
			//TODO write a thing to place a message in the sources table to say that insufficient data from sites was found
			System.out.println("Selected site is invalid, no source data collected.");
		}
	}
	
	public void updateSources(Connection dbConn,SiteTable siteList,String sourceType) throws SQLException{
		sourceListModel.removeListSelectionListener(this);
		if (siteList.siteListModel.isSelectionEmpty()==false && siteList.getValueAt(siteList.getSelectedRow(),0).toString()!=null){
			ResultSet sourceRS = dbConn.createStatement().executeQuery("SELECT * FROM sources WHERE site_id = '"+siteList.getValueAt(siteList.getSelectedRow(),0)+"' AND source_type = '"+sourceType+"' ORDER BY source_id");
			System.out.println("SELECT * FROM sources WHERE site_id = '"+siteList.getValueAt(siteList.getSelectedRow(),0)+"' AND source_type = '"+sourceType+"' ORDER BY source_id");
			populateList(sourceRS);
		}
		else{
			//TODO write a thing to place a message in the sources table to say that insufficient data from sites was found
			System.out.println("Selected site is invalid, no source data collected.");
		}
	}
	
	private void populateList(final ResultSet fetchedData) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				sourceTableModel.setNumRows(0);
				try {
					while (fetchedData.next()){
						if (Arrays.equals(columnHeaders,new String[] {"Source ID","Source Name","Source Type","Brand","Model","Serial","Location"})){
							sourceTableModel.addRow(new String[] {fetchedData.getString("source_id"),(fetchedData.getString("source_name")==null?"":fetchedData.getString("source_name")),fetchedData.getString("source_type"),(fetchedData.getString("brand")==null?"":fetchedData.getString("brand")),(fetchedData.getString("model")==null?"":fetchedData.getString("model")),(fetchedData.getString("serial")==null?"":fetchedData.getString("serial")),(fetchedData.getString("location")==null?"":fetchedData.getString("location"))});
						}
						else if (Arrays.equals(columnHeaders,new String[] {"Source ID","Source Name","Brand","Model","Serial","Location"})){
							sourceTableModel.addRow(new String[] {fetchedData.getString("source_id"),(fetchedData.getString("source_name")==null?"":fetchedData.getString("source_name")),(fetchedData.getString("brand")==null?"":fetchedData.getString("brand")),(fetchedData.getString("model")==null?"":fetchedData.getString("model")),(fetchedData.getString("serial")==null?"":fetchedData.getString("serial")),(fetchedData.getString("location")==null?"":fetchedData.getString("location"))});
						}
						else if (Arrays.equals(columnHeaders,new String[] {"Source ID","Source Name","Location"})){
							sourceTableModel.addRow(new String[] {fetchedData.getString("source_id"),(fetchedData.getString("source_name")==null?"":fetchedData.getString("source_name")),(fetchedData.getString("location")==null?"":fetchedData.getString("location"))});
						}
						else{
							JOptionPane.showMessageDialog(null, "Could not display data. Unexpected number of columns. ","Error Displaying Data",JOptionPane.ERROR_MESSAGE);
							break;
						}
						
					}
				} catch (SQLException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, "An error occured while retrieving site data.","Error Retrieving Data",JOptionPane.ERROR_MESSAGE);
				}
				sourceListModel.addListSelectionListener(SourceTable.this);
			}
		});
	}
	*/
	
	
	public boolean isCellEditable(int rowIndex, int vColIndex) {
		return false;
    }
}
