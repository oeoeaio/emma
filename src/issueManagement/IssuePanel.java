package issueManagement;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import dataPlotter.DataPlotter;

public class IssuePanel extends JPanel implements ActionListener,ListSelectionListener{
	private static final long serialVersionUID = 6096908470416764028L;

	//Main Panel
	JPanel mainPanel = new JPanel(new GridLayout(2,1));
	
	//Top Panel
	JPanel topPanel = new JPanel(new BorderLayout());

	//Issue List Panel Components
	JPanel issuePanel = new JPanel(new BorderLayout());
	JPanel issueTitleP = new JPanel(new FlowLayout());
	JLabel issueLabel = new JLabel("Select Site");
	IssueTable issueTable = new IssueTable();
	JScrollPane issueScroll = new JScrollPane(issueTable);
	
	//Middle Panel
	JPanel middlePanel = new JPanel(new BorderLayout());
	
	
	//Plot Button
	JPanel plotButtonPanel = new JPanel();
	JButton plotButton = new JButton("Plot Data");
	
	//Data Plotter
	DataPlotter dataPlotter = new DataPlotter();
	
	//Database Connection
	Connection dbConn;
	
	//Date parser
	SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	public IssuePanel(Connection dbConn){
		this.dbConn = dbConn;
		dateParser.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
	}
	
	void buildGUI(){
		this.setLayout(new BorderLayout());
		
		//issues
		issuePanel.add(issueTitleP,BorderLayout.NORTH);
		issueTitleP.add(issueLabel);
		issuePanel.add(issueScroll,BorderLayout.CENTER);
		
		plotButtonPanel.setLayout(new FlowLayout());
		plotButtonPanel.add(plotButton);
		plotButton.addActionListener(this);
		plotButton.setEnabled(false);
		
		topPanel.add(issuePanel,BorderLayout.CENTER);
		topPanel.add(plotButtonPanel,BorderLayout.SOUTH);
		
		mainPanel.add(topPanel);
		mainPanel.add(dataPlotter);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(0,50,30,50));
		
		issueTable.updateIssues(dbConn);
		this.add(mainPanel,BorderLayout.CENTER);
	}
	
	public void actionPerformed(ActionEvent aE) {
		System.out.println("Issue Plot Button Thread: "+Thread.currentThread().getName());
		if (aE.getSource()==plotButton){
			if (issueTable.issueListModel.isSelectionEmpty()==false){
				int sRow = issueTable.getSelectedRow();
				if (issueTable.issueList.get(sRow).siteID != null && issueTable.issueList.get(sRow).sourceID != null){
					
					long issueLength = (issueTable.issueList.get(sRow).endDate-issueTable.issueList.get(sRow).startDate)/60000;
					
					int ext = 10; //extension beyond actual issue
					if (issueLength<30){
						ext = 15;
					}
					else if (issueLength<60){
						ext = 30;
					}
					else if (issueLength<180){
						ext = 60;
					}
					else if (issueLength<360){
						ext = 180;
					}
					else {
						ext = 360;
					}
					
					long plotStartDate = issueTable.issueList.get(sRow).startDate-60000*ext;
					long plotEndDate = issueTable.issueList.get(sRow).endDate+60000*ext;
					
					dataPlotter.setData(dbConn, issueTable.issueList.get(sRow).siteID, issueTable.issueList.get(sRow).sourceID, plotStartDate, plotEndDate);
				}
				else{
		        	JOptionPane.showMessageDialog(null, "Error\r\nMissing SiteID or sourceID. Data Cannot be plotted.", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
			else{
	        	JOptionPane.showMessageDialog(null, "Error\r\nNo issue event selected.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent lSE) {
		if (lSE.getSource().equals(issueTable.issueListModel) && lSE.getValueIsAdjusting()==false){
			if(issueTable.issueListModel.isSelectionEmpty()==false){
				plotButton.setEnabled(true);
			}
			else{
				plotButton.setEnabled(false);
			}
		}
	}
}
