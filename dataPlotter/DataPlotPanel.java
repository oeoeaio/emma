package dataPlotter;
import java.awt.BorderLayout;
import java.awt.Dimension;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;


import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import endUseWindow.SiteTable;
import endUseWindow.SourceTable;

public class DataPlotPanel extends JPanel implements ActionListener,ListSelectionListener,ItemListener{
	private static final long serialVersionUID = 1L;
	
	
	//Main Panel
	JPanel mainPanel = new JPanel(new GridLayout(2,1));
	
	//Top Panel
	JPanel topPanel = new JPanel(new BorderLayout());
	//Source Select Panel
	//JPanel sourceSelectPanel = new JPanel(new GridLayout(1,2,20,0));


	//Site List Panel Components
	JPanel sitePanel = new JPanel(new BorderLayout());
	JPanel siteTitleP = new JPanel(new FlowLayout());
	JLabel siteLabel = new JLabel("Select Site");
	SiteTable siteTable = new SiteTable(new String[] {"Site ID","Site Name"});
	JScrollPane siteScroll = new JScrollPane(siteTable);

	//Source Select Panel
	//Refigerator Source List Panel Components
	JPanel sourcePanel = new JPanel(new BorderLayout());
	JPanel sourceTitleP = new JPanel(new FlowLayout());
	JLabel sourceLabel = new JLabel("Select Source");
	SourceTable sourceTable = new SourceTable(DefaultListSelectionModel.SINGLE_SELECTION,new String[] {"Source ID","Source Name","Source Type","Measurement Type"});
	JScrollPane sourceScroll = new JScrollPane(sourceTable);
	
	//Create a split pane with the two scroll panes in it.
	JSplitPane sourceSelectSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
	                           sitePanel, sourcePanel);
	
	//Top Panel
	JPanel middlePanel = new JPanel(new GridLayout(2,1));
	
	//Date Panel
	JPanel datePanel = new JPanel(new FlowLayout());
	JLabel startDateL = new JLabel("Start Date");
	JComboBox<String> startDateS = new JComboBox<String>();
	JLabel startTimeL = new JLabel("Start Time");
	JComboBox<String> startHourS = new JComboBox<String>(new String[] {"00","01","02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22","23"});
	JComboBox<String> startMinS = new JComboBox<String>(new String[] {"00","05","10","15","20","25","30","35","40","45","50","55"});
	JLabel endDateL = new JLabel("               End Date");
	JComboBox<String> endDateS = new JComboBox<String>();
	JLabel endTimeL = new JLabel("End Time");
	JComboBox<String> endHourS = new JComboBox<String>(new String[] {"00","01","02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22","23"});
	JComboBox<String> endMinS = new JComboBox<String>(new String[] {"00","05","10","15","20","25","30","35","40","45","50","55"});
	JLabel colonL = new JLabel(" : ");
	JLabel inclusiveL = new JLabel("(inclusive)");
	
	//Plot Button
	JPanel plotButtonPanel = new JPanel();
	JButton plotButton = new JButton("Plot Data");
	
	//Data Plotter
	DataPlotter dataPlotter = new DataPlotter();
	
	
	//Variables
	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	Connection dbConn;
	ArrayList<Long> dateRange = new ArrayList<Long>();

	public DataPlotPanel(Connection dbConn){
		//version 0.0.10
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		dateParser.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		this.dbConn = dbConn;
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
	}
	
	void buildGUI(){
		this.setLayout(new BorderLayout());
		
		//sites
		sitePanel.add(siteTitleP,BorderLayout.NORTH);
		siteTitleP.add(siteLabel);
		sitePanel.add(siteScroll,BorderLayout.CENTER);
		siteTable.siteListModel.addListSelectionListener(this);

		// sources
		sourcePanel.add(sourceTitleP,BorderLayout.NORTH);
		sourceTitleP.add(sourceLabel);
		sourcePanel.add(sourceScroll,BorderLayout.CENTER);
		sourceTable.sourceListModel.addListSelectionListener(this);
		
		//sourceSelectPanel.add(sitePanel);
		//sourceSelectPanel.add(sourcePanel);
		
		sourceSelectSplitPane.setOneTouchExpandable(true);
		sourceSelectSplitPane.setDividerLocation(150);

		//Provide minimum sizes for the two components in the split pane
		Dimension minimumSize = new Dimension(100, 50);
		sitePanel.setMinimumSize(minimumSize);
		sourcePanel.setMinimumSize(minimumSize);
		
		//Date Panel
		datePanel.add(startDateL);
		datePanel.add(startDateS);
		datePanel.add(startTimeL);
		datePanel.add(startHourS);
		datePanel.add(new JLabel(" : "));
		datePanel.add(startMinS);
		datePanel.add(endDateL);
		datePanel.add(endDateS);
		datePanel.add(endTimeL);
		datePanel.add(endHourS);
		datePanel.add(new JLabel(" : "));
		datePanel.add(endMinS);
		datePanel.add(inclusiveL);
		startDateS.addItemListener(this);
		
		plotButtonPanel.setLayout(new FlowLayout());
		plotButtonPanel.add(plotButton);
		plotButton.addActionListener(this);
		plotButton.setEnabled(false);
		
		middlePanel.add(datePanel);
		middlePanel.add(plotButtonPanel);
		
		topPanel.add(sourceSelectSplitPane,BorderLayout.CENTER);
		topPanel.add(middlePanel,BorderLayout.SOUTH);
		
		mainPanel.add(topPanel);
		mainPanel.add(dataPlotter);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(0,50,30,50));
		
		siteTable.updateList(dbConn);
		this.add(mainPanel,BorderLayout.CENTER);
	}

	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource()==plotButton){
			if (sourceTable.sourceListModel.isSelectionEmpty()==false && sourceTable.getValueAt(sourceTable.getSelectedRow(),0) != null){
				//Build dates
				GregorianCalendar startDate = new GregorianCalendar();
				GregorianCalendar endDate = new GregorianCalendar();
				startDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
				endDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
				try {
					startDate.setTime(dateParser.parse(startDateS.getSelectedItem().toString()+" "+startHourS.getSelectedItem().toString()+":"+startMinS.getSelectedItem().toString()));
					endDate.setTime(dateParser.parse(endDateS.getSelectedItem().toString()+" "+endHourS.getSelectedItem().toString()+":"+endMinS.getSelectedItem().toString()));
					
					if (startDate.before(endDate)){
						dataPlotter.setData(dbConn, siteTable.getValueAt(siteTable.getSelectedRow(),0).toString(), sourceTable.getValueAt(sourceTable.getSelectedRow(),0).toString(), startDate.getTimeInMillis(), endDate.getTimeInMillis());
					}
					else{
						JOptionPane.showMessageDialog(null, "Error: Start date must be before End Date.");
					}
				} catch (ParseException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, "Error: could not build valid dates from the selection made.");
				}
			}
			else{
	        	JOptionPane.showMessageDialog(null, "Error Code 002\r\nPlease select a file to Analyse.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	int getYearFormat(Set<String> dateSet){
		int yearFormat = 0;
		int counter = 0;
		int totalCount = 0;
		Iterator<String> it = dateSet.iterator();
		while (it.hasNext()){
			totalCount+=it.next().length();
			counter++;
		}
		yearFormat = totalCount/counter;
		return yearFormat;
	}
	
	void populateStartDate() throws SQLException,Exception{
		startDateS.removeActionListener(this);
		startDateS.removeAllItems();
		dateRange.clear();
		if (sourceTable.sourceListModel.isSelectionEmpty()==false && sourceTable.getValueAt(sourceTable.getSelectedRow(),0).toString()!=null){
			Statement MySQL_Statement = dbConn.createStatement();
			String getMinMaxSQL = "SELECT UNIX_TIMESTAMP(DATE_ADD(DATE(MIN(date_time)),INTERVAL 1 DAY)) AS min_date,UNIX_TIMESTAMP(DATE(MAX(date_time))) AS max_date FROM data_sa WHERE site_id = "+siteTable.getValueAt(siteTable.getSelectedRow(),0)+" AND source_id = "+sourceTable.getValueAt(sourceTable.getSelectedRow(),0);
			ResultSet minMaxResults = MySQL_Statement.executeQuery(getMinMaxSQL);
			if (minMaxResults.next()){
				Calendar minCal = Calendar.getInstance();
				minCal.setTimeZone(TimeZone.getTimeZone("GMT+10"));
				minCal.setTimeInMillis(minMaxResults.getLong("min_date")*1000);
				long maxCal = minMaxResults.getLong("max_date")*1000;
			
				while (minCal.getTimeInMillis() <= maxCal){
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
		}
		else{ //This should never be bale to happen
			throw new Exception("NoSource");
		}
		startDateS.addActionListener(this);
	}
	
	void populateEndDate(){
		endDateS.removeAllItems();
		for (int i=Math.max(0,startDateS.getSelectedIndex());i<dateRange.size();i++){
			endDateS.addItem(dateFormatter.format(dateRange.get(i)));
		}
		endDateS.setEnabled(true);
	}

	@Override
	public void valueChanged(ListSelectionEvent lSE) {
		if (lSE.getSource().equals(siteTable.siteListModel) && lSE.getValueIsAdjusting()==false){
			sourceTable.updateList(dbConn,siteTable.siteList.get(siteTable.getSelectedRow()));
			
			if (sourceTable.sourceListModel.isSelectionEmpty()){
				plotButton.setEnabled(false);
				startDateS.removeAllItems();
				endDateS.removeAllItems();
			}
		}
		else if (lSE.getSource().equals(sourceTable.sourceListModel) && lSE.getValueIsAdjusting()==false){
			
			if (sourceTable.sourceListModel.isSelectionEmpty()==false){
				try {
					populateStartDate();
					if (startDateS.getItemCount()>0 && endDateS.getItemCount()>0){
						plotButton.setEnabled(true);
					}
				} catch (SQLException e) {
					plotButton.setEnabled(false);
					startDateS.removeAllItems();
					endDateS.removeAllItems();
					startDateS.addItem("ERR:NoData");
					endDateS.addItem("ERR:NoData");
					e.printStackTrace();
				}catch (Exception e){
					plotButton.setEnabled(false);
					startDateS.removeAllItems();
					endDateS.removeAllItems();
					startDateS.addItem(e.getMessage());
					endDateS.addItem(e.getMessage());
				}
			}
			else{
				plotButton.setEnabled(false);
				startDateS.removeAllItems();
				endDateS.removeAllItems();
			}
		}
		
	}

	@Override
	public void itemStateChanged(ItemEvent iE) {
		if (iE.getSource().equals(startDateS) && iE.getStateChange()==ItemEvent.SELECTED){
			populateEndDate();
		}
	}
}
