package issueManagement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class IssueTable extends JTable{
	private static final long serialVersionUID = 233850392634466700L;
	ArrayList<Issue> issueList = new ArrayList<Issue>();
	public ListSelectionModel issueListModel = this.getSelectionModel();
	public DefaultTableModel issueTableModel = (DefaultTableModel)this.getModel();
	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	Connection dbConn;
	
	public boolean isCellEditable(int rowIndex, int vColIndex) {
		return false;
	}

	IssueTable(){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		this.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
		this.setColumnSelectionAllowed(false);
		this.setRowSelectionAllowed(true);
		this.issueTableModel.setColumnIdentifiers(new String[] {"Start Date","End Date","Site ID","Source ID","Site Name","Source Name","Issue Type","Urgency"});
		this.issueListModel.addListSelectionListener(this);
	}

	void updateIssues(Connection dbConn){
		this.dbConn = dbConn;
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				//TODO GREY TABLE greyTable();
			}
		});
		
		Thread fetchIssues = new Thread(new FetchIssueList());
		fetchIssues.start();
	}
	
	class FetchIssueList extends Thread{
		public void run(){
			try {
				issueList.clear();
				ResultSet issueRS = IssueTable.this.dbConn.createStatement().executeQuery("SELECT issues.issue_id,UNIX_TIMESTAMP(issues.start_date) AS start_date,UNIX_TIMESTAMP(issues.end_date) AS end_date,issues.site_id,sites.site_name,issues.source_id,sources.source_name,issues.issue_type,issues.urgency,issues.notes FROM issues LEFT JOIN sites ON sites.site_id = issues.site_id LEFT JOIN sources ON sources.source_id = issues.source_id ORDER BY end_date DESC");
				if (issueRS.next()){
					issueRS.beforeFirst(); //reset cursor position
					while (issueRS.next()){
						issueList.add(new Issue(issueRS.getString("issue_id"),issueRS.getLong("start_date")*1000,issueRS.getLong("end_date")*1000,issueRS.getString("site_id"),issueRS.getString("source_id"),issueRS.getString("site_name"),issueRS.getString("source_name"),issueRS.getString("issue_type"),issueRS.getString("urgency"),issueRS.getString("notes")));
					}
					SwingUtilities.invokeLater(new PopulateTable());
				}
				else{
					issueTableModel.setNumRows(0);
					issueList.clear();
					//errorMessage = "No issue events found.";
					//IssueTable.this.repaint();
				}
			} catch (SQLException e) {
				issueTableModel.setNumRows(0);
				issueList.clear();
				//errorMessage = "A problem occured when retrieving issue events.";
				//IssueTable.this.repaint();
				e.printStackTrace();
			}
		}
	}
	
	class PopulateTable extends Thread { //must be run in swing utilities
		public void run() {
			issueTableModel.setNumRows(0);
			for (int i=0;i<issueList.size();i++){
				issueTableModel.addRow(new String[] {dateFormatter.format(issueList.get(i).startDate), dateFormatter.format(issueList.get(i).endDate), issueList.get(i).siteID, issueList.get(i).sourceID, issueList.get(i).siteName, issueList.get(i).sourceName, issueList.get(i).issueType, issueList.get(i).urgency});
			}
			//ungrey table here
		}
	}
	
	/*@Override
	public void paint(Graphics g){
	    super.paint(g);
	    Graphics2D g2 = (Graphics2D)g;
	    System.out.println("lalala34");
	    if(!errorMessage.equals("")){
	    	System.out.println("lalala");
	    	double w = g2.getFontMetrics().getStringBounds(errorMessage, g2).getWidth();
	    	double h = g2.getFontMetrics().getStringBounds(errorMessage, g2).getHeight();
	    	g2.drawString(errorMessage,(int)Math.round(this.getWidth()-w)/2, (int)Math.round(this.getHeight()-h)/2);
	    	errorMessage = "";
	    }
	}*/

}
