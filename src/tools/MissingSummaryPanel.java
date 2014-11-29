package tools;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import javax.swing.BoxLayout;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import endUseWindow.LogWindow;
import endUseWindow.SiteTable;
import endUseWindow.SourceTable;


public class MissingSummaryPanel extends JPanel implements ItemListener,ActionListener,ListSelectionListener {
	private static final long serialVersionUID = -3989983616744319157L;

	//Main Panel
	private final JPanel mainPanel = new JPanel();

	//Source Select Panel
	private final JPanel sourceSelectPanel = new JPanel(new GridLayout(1,2));

	//Site List Panel Components
	private final JPanel sitePanel = new JPanel(new BorderLayout());
	private final JPanel siteTitleP = new JPanel(new FlowLayout());
	private final JLabel siteLabel = new JLabel("Select Site");
	private final SiteTable siteTable = new SiteTable(new String[] {"Site ID","Site Name","Given Name","Surname"});
	private final JScrollPane siteScroll = new JScrollPane(siteTable);

	//Source Select Panel
	//Refigerator Source List Panel Components
	private final JPanel sourcePanel = new JPanel(new BorderLayout());
	private final JPanel sourceTitleP = new JPanel(new FlowLayout());
	private final JLabel sourceLabel = new JLabel("Select Source");
	private final SourceTable sourceTable = new SourceTable(DefaultListSelectionModel.MULTIPLE_INTERVAL_SELECTION,new String[] {"Source ID","Source Name","Source Type","Measurement Type"});
	private final JScrollPane sourceScroll = new JScrollPane(sourceTable);
	
	//Button Panel
	private final JPanel buttonPanel = new JPanel(new FlowLayout());
	//Buttons	
	private final JButton allSources = new JButton("Select All Sources");
	private final JButton noSources = new JButton("Select No Sources");
	
	//SamplePeriodPanel
	private final JPanel samplePeriodPanel = new JPanel(new FlowLayout());
	private final JLabel samplePeriodL = new JLabel("Sample Period");
	private final JComboBox<String> samplePeriodS = new JComboBox<String>();
	
	//Date Panel
	private final JPanel datePanel = new JPanel(new FlowLayout());
	private final JLabel startDateL = new JLabel("Start Date");
	private final JComboBox<String> startDateS = new JComboBox<String>();
	private final JLabel endDateL = new JLabel("End Date");
	private final JComboBox<String> endDateS = new JComboBox<String>();
	private final JLabel inclusiveL = new JLabel("(inclusive)");
	
	//Start Panel
	private final JPanel startPanel = new JPanel(new FlowLayout());
	private final JButton startButton = new JButton("Begin Extraction");
	
	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
	private final SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private final Connection dbConn;
	
	private final ArrayList<Long> dateRange = new ArrayList<Long>();
	
	public MissingSummaryPanel(final Connection dbConn){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		this.dbConn = dbConn;
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
		
	}
	
	void buildGUI(){
		this.setLayout(new BorderLayout());
		
		mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.PAGE_AXIS));
		sourcePanel.setLayout(new BoxLayout(sourcePanel,BoxLayout.PAGE_AXIS));
		
		mainPanel.add(sourceSelectPanel);
		mainPanel.add(buttonPanel);
		mainPanel.add(samplePeriodPanel);
		mainPanel.add(datePanel);
		mainPanel.add(startPanel);
		
		//sites
		sitePanel.add(siteTitleP,BorderLayout.NORTH);
		siteTitleP.add(siteLabel);
		sitePanel.add(siteScroll,BorderLayout.CENTER);

		// sources
		sourcePanel.add(sourceTitleP,BorderLayout.NORTH);
		sourceTitleP.add(sourceLabel);
		sourcePanel.add(sourceScroll,BorderLayout.CENTER);
		
		sourceSelectPanel.add(sitePanel);
		sourceSelectPanel.add(sourcePanel);
		
		//buttons
		buttonPanel.add(allSources);
		buttonPanel.add(noSources);
		allSources.addActionListener(this);
		noSources.addActionListener(this);
				
		samplePeriodPanel.add(samplePeriodL);
		samplePeriodPanel.add(samplePeriodS);
		
		samplePeriodS.addItem("60");
		samplePeriodS.addItem("1440");
		samplePeriodS.setSelectedItem("1440");
		
		datePanel.add(startDateL);
		datePanel.add(startDateS);
		datePanel.add(endDateL);
		datePanel.add(endDateS);
		datePanel.add(inclusiveL);
		startPanel.add(startButton);
		startButton.addActionListener(this);
		
		startDateS.addItemListener(this);
		siteTable.siteListModel.addListSelectionListener(this);
		sourceTable.sourceListModel.addListSelectionListener(this);
		
		siteTable.updateList(dbConn);
		this.add(mainPanel,BorderLayout.CENTER);
	}
	
	void populateStartDate() throws SQLException,Exception{
		startDateS.removeActionListener(this);
		startDateS.removeAllItems();
		dateRange.clear();
		if (sourceTable.sourceListModel.isSelectionEmpty()==false && sourceTable.getValueAt(sourceTable.getSelectedRow(),0).toString()!=null){
			
			String sourceList = "(";
			for (int i=0;i<sourceTable.getSelectedRows().length;i++){
				sourceList = sourceList + sourceTable.getValueAt(sourceTable.getSelectedRows()[i],0) + (i<sourceTable.getSelectedRows().length-1?",":"");
			}
			sourceList = sourceList + ")";
			
			Statement MySQL_Statement = dbConn.createStatement();
			//Date before = new Date();
			String getMinMaxSQL = "SELECT UNIX_TIMESTAMP(DATE_ADD(DATE(MIN(start_date)),INTERVAL 1 DAY)) AS min_date,UNIX_TIMESTAMP(DATE(MAX(end_date))) AS max_date FROM files WHERE site_id = "+siteTable.getValueAt(siteTable.getSelectedRow(),0)+" AND source_id IN "+sourceList;
			//System.out.println(getMinMaxSQL);
			ResultSet minMaxResults = MySQL_Statement.executeQuery(getMinMaxSQL);
			//System.out.println(new Date().getTime()-before.getTime());
			if (minMaxResults.next()){
				Calendar minCal = Calendar.getInstance();
				minCal.setTimeZone(TimeZone.getTimeZone("GMT+10"));
				minCal.setTimeInMillis(minMaxResults.getLong("min_date")*1000);
				long maxCal = minMaxResults.getLong("max_date")*1000;
			
				while( minCal.getTimeInMillis() <= maxCal ){
					dateRange.add(minCal.getTimeInMillis());
					startDateS.addItem(dateFormatter.format(minCal.getTimeInMillis()));
					minCal.add(Calendar.DATE, 1);
				}
				startDateS.setEnabled(true);
				populateEndDate();
			}
			else{
				throw new Exception("ERR:NoData");
			}
			//System.out.println(new Date().getTime()-before.getTime());
		}
		else{ //This should never be able to happen
			throw new Exception("NoSource");
		}
		startDateS.addActionListener(this);
		if (startDateS.getItemCount()>0){startDateS.setSelectedIndex(Math.max(0,startDateS.getItemCount()-8));}
	}
	
	void populateEndDate(){
		endDateS.removeAllItems();
		for (int i=Math.max(0,startDateS.getSelectedIndex());i<dateRange.size();i++){
			endDateS.addItem(dateFormatter.format(dateRange.get(i)));
		}
		endDateS.setEnabled(true);
		if (endDateS.getItemCount()>0){endDateS.setSelectedIndex(Math.max(0, endDateS.getItemCount()-1));}
	}

	@Override
	public void itemStateChanged(ItemEvent iE) {
		if (iE.getSource().equals(startDateS) && iE.getStateChange()==ItemEvent.SELECTED){
			populateEndDate();
		}
	}

	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(startButton)){
			
			int[] selectedSources = new int[sourceTable.getSelectedRows().length];
			String[] sourceNames = new String[sourceTable.getSelectedRows().length];
			
			for (int i=0;i<selectedSources.length;i++){
				selectedSources[i] = Integer.parseInt(sourceTable.getValueAt(sourceTable.getSelectedRows()[i], 0).toString());
				sourceNames[i] = sourceTable.getValueAt(sourceTable.getSelectedRows()[i], 1).toString();
			}
			
			long startDate = dateRange.get(startDateS.getSelectedIndex());
			long endDate = dateRange.get(startDateS.getSelectedIndex()+endDateS.getSelectedIndex());
			int samplePeriod = Integer.parseInt(samplePeriodS.getSelectedItem().toString());
			
			if (selectedSources.length > 0){
				Thread averageAnalysisProcess = new Thread(new MissingSummaryAnalysis(new LogWindow("Average analysis process log"),dbConn,selectedSources,sourceNames,Integer.parseInt(siteTable.getValueAt(siteTable.getSelectedRow(),0).toString()),siteTable.getValueAt(siteTable.getSelectedRow(),1).toString(),startDate,endDate,samplePeriod));
				averageAnalysisProcess.start();
			}
			else{
				JOptionPane.showMessageDialog(this, "No sources selected for processing.\r\nPlease ensure at least one source is selected.", "No Channels Selected", JOptionPane.WARNING_MESSAGE);
			}
		}
		else if (aE.getSource().equals(allSources)){
			sourceTable.selectAll();
		}
		else if (aE.getSource().equals(noSources)){
			sourceTable.clearSelection();
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent lSE) {
		if (lSE.getSource().equals(siteTable.siteListModel) && lSE.getValueIsAdjusting()==false){
			sourceTable.updateList(dbConn,siteTable.siteList.get(siteTable.getSelectedRow()));
			
			if (sourceTable.sourceListModel.isSelectionEmpty()){
				startButton.setEnabled(false);
				startDateS.removeAllItems();
				endDateS.removeAllItems();
			}
		}
		else if (lSE.getSource().equals(sourceTable.sourceListModel) && lSE.getValueIsAdjusting()==false){
			
			if (sourceTable.sourceListModel.isSelectionEmpty()==false){
				try {
					populateStartDate();
					if (startDateS.getItemCount()>0 && endDateS.getItemCount()>0){
						startButton.setEnabled(true);
					}
				} catch (SQLException e) {
					startButton.setEnabled(false);
					startDateS.removeAllItems();
					endDateS.removeAllItems();
					startDateS.addItem("ERR:NoData");
					endDateS.addItem("ERR:NoData");
					e.printStackTrace();
				}catch (Exception e){
					startButton.setEnabled(false);
					startDateS.removeAllItems();
					endDateS.removeAllItems();
					startDateS.addItem(e.getMessage());
					endDateS.addItem(e.getMessage());
				}
			}
			else{
				startButton.setEnabled(false);
				startDateS.removeAllItems();
				endDateS.removeAllItems();
			}
		}
	}
}
