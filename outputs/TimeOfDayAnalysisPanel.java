package outputs;

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
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.TimeZone;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import endUseWindow.LogWindow;
import endUseWindow.SiteTable;
import endUseWindow.Source;
import endUseWindow.SourceTable;


public class TimeOfDayAnalysisPanel extends JPanel implements ItemListener,ActionListener,ListSelectionListener {
	private static final long serialVersionUID = -3989983616744319157L;

	//Main Panel
	private final JPanel mainPanel = new JPanel();

	//Source Select Panel
	private final JPanel sourceSelectPanel = new JPanel(new GridLayout(1,2));

	//Site List Panel Components
	private final JPanel sitePanel = new JPanel(new BorderLayout());
	private final JPanel siteTitleP = new JPanel(new FlowLayout());
	private final JLabel siteLabel = new JLabel("Select Site");
	private final JComboBox siteSelectionType = new JComboBox(new String[] {"Single","All"});
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
	
	//Analysis Restriction Panel
	//private final JPanel analysisRestrictionPanel = new JPanel(new FlowLayout());
	//private final JLabel analysisRestrictionL = new JLabel("Days:  ");
	//private final JRadioButton allDaysRadio = new JRadioButton("All Days");
	//private final JRadioButton weekDaysRadio = new JRadioButton("Weekdays");
	//private final JRadioButton weekendsRadio = new JRadioButton("Weekends");
	//private final JRadioButton splitByDayRadio = new JRadioButton("Split By Day");

	//Analysis Type Panel
	private final JPanel analysisTypePanel = new JPanel(new FlowLayout());
	private final JLabel analysisTypeL = new JLabel("Analysis Split:  ");
	private final JRadioButton normalRadio = new JRadioButton("Normal TOD");
	private final JRadioButton circuitKnownRadio = new JRadioButton("Circuit Known TOD");
	
	//Analysis Split Panel
	private final JPanel analysisSplitPanel = new JPanel(new FlowLayout());
	private final JLabel analysisPeriodL = new JLabel("Analysis Split:  ");
	private final JRadioButton monthlyRadio = new JRadioButton("Monthly");
	private final JRadioButton selectionRadio = new JRadioButton("No Split");
	
	//SamplePeriodPanel
	private final JPanel samplePeriodPanel = new JPanel(new FlowLayout());
	private final JLabel samplePeriodL = new JLabel("Sample Period");
	private final JComboBox samplePeriodS = new JComboBox(new String[] {"60"});
	
	//Date Panel
	private final JPanel datePanel = new JPanel(new FlowLayout());
	private final JLabel startDateL = new JLabel("Start Date");
	private final JComboBox startDateS = new JComboBox();
	private final JLabel endDateL = new JLabel("End Date");
	private final JComboBox endDateS = new JComboBox();
	private final JLabel inclusiveL = new JLabel("(inclusive)");
	
	//Start Panel
	private final JPanel startPanel = new JPanel(new FlowLayout());
	private final JButton startButton = new JButton("Begin Extraction");
	
	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
	private final SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private final Connection dbConn;
	
	private final ArrayList<Long> dateRange = new ArrayList<Long>();
	
	public TimeOfDayAnalysisPanel(final Connection dbConn){
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
		mainPanel.add(analysisTypePanel);
		mainPanel.add(analysisSplitPanel);
		mainPanel.add(samplePeriodPanel);
		mainPanel.add(datePanel);
		mainPanel.add(startPanel);
		
		//sites
		sitePanel.add(siteTitleP,BorderLayout.NORTH);
		siteTitleP.add(siteLabel);
		siteTitleP.add(siteSelectionType);
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
		
		analysisTypePanel.add(analysisTypeL);
		analysisTypePanel.add(normalRadio);
		analysisTypePanel.add(circuitKnownRadio);
		normalRadio.setSelected(true);
		
		analysisSplitPanel.add(analysisPeriodL);
		analysisSplitPanel.add(monthlyRadio);
		analysisSplitPanel.add(selectionRadio);
		
		datePanel.add(startDateL);
		datePanel.add(startDateS);
		datePanel.add(endDateL);
		datePanel.add(endDateS);
		datePanel.add(inclusiveL);
		startPanel.add(startButton);
		startButton.addActionListener(this);
		
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(allSources);
		buttonGroup.add(noSources);
		
		ButtonGroup periodButtonGroup = new ButtonGroup();
		periodButtonGroup.add(normalRadio);
		periodButtonGroup.add(circuitKnownRadio);
		
		ButtonGroup splitButtonGroup = new ButtonGroup();
		splitButtonGroup.add(monthlyRadio);
		splitButtonGroup.add(selectionRadio);
		
		siteSelectionType.addItemListener(this);
		allSources.addActionListener(this);
		noSources.addActionListener(this);
		//monthlyRadio.addActionListener(this);
		//selectionRadio.addActionListener(this);
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
			String getMinMaxSQL = "SELECT UNIX_TIMESTAMP(DATE_ADD(DATE(MIN(start_date)),INTERVAL 1 DAY)) AS min_date,UNIX_TIMESTAMP(DATE(MAX(end_date))) AS max_date FROM files WHERE"+(siteSelectionType.getSelectedItem().equals("All")?"":" site_id = "+siteTable.getValueAt(siteTable.getSelectedRow(),0)+" AND ")+" source_id IN "+sourceList;
			//System.out.println(getMinMaxSQL);
			ResultSet minMaxResults = MySQL_Statement.executeQuery(getMinMaxSQL);
			//System.out.println(new Date().getTime()-before.getTime());
			if (minMaxResults.next()){
				Calendar minCal = Calendar.getInstance();
				minCal.setTimeZone(TimeZone.getTimeZone("GMT+10"));
				minCal.setTimeInMillis(minMaxResults.getLong("min_date")*1000);
				long maxCal = minMaxResults.getLong("max_date")*1000;
			
				for (int i=0;minCal.getTimeInMillis() <= maxCal;i++){
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
	}
	
	void populateEndDate(){
		endDateS.removeAllItems();
		for (int i=Math.max(0,startDateS.getSelectedIndex());i<dateRange.size();i++){
			endDateS.addItem(dateFormatter.format(dateRange.get(i)));
		}
		endDateS.setEnabled(true);
	}

	@Override
	public void itemStateChanged(ItemEvent iE) {
		if (iE.getSource().equals(startDateS) && iE.getStateChange()==ItemEvent.SELECTED){
			populateEndDate();
		}
		else if (iE.getSource().equals(siteSelectionType) && iE.getStateChange()==ItemEvent.SELECTED){
			if (siteSelectionType.getSelectedItem().toString().equals("All")){
				siteTable.setEnabled(false);
				sourceTable.updateList(dbConn,null);
				
				if (sourceTable.sourceListModel.isSelectionEmpty()){
					startButton.setEnabled(false);
					startDateS.removeAllItems();
					endDateS.removeAllItems();
				}
			}
			else{
				siteTable.setEnabled(true);
				siteTable.updateList(dbConn);
			}
		}
	}

	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(startButton)){
			
			int[] selectedRows = sourceTable.getSelectedRows();
			
			
			LinkedList<Source> sourceList = new LinkedList<Source>();
			//String[] sourceNames = new String[sourceTable.getSelectedRows().length];
			
			for (int i=0;i<selectedRows.length;i++){
				sourceList.add(i,sourceTable.sourceList.get(selectedRows[i]));
				//sourceNames[i] = sourceTable.getValueAt(sourceTable.getSelectedRows()[i], 1).toString();
			}
			
			long startDate = dateRange.get(startDateS.getSelectedIndex());
			long endDate = dateRange.get(startDateS.getSelectedIndex()+endDateS.getSelectedIndex());
			int samplePeriod = Integer.parseInt(samplePeriodS.getSelectedItem().toString());
			String analysisType = (normalRadio.isSelected()?"normal":"circuitKnown");
			String splitType = (monthlyRadio.isSelected()?"monthly":"none");
			
			LinkedList<long[]> customStartAndEndDates = new LinkedList<long[]>();
			LinkedList<String> rangeTitles = new LinkedList<String>();
			SimpleDateFormat monthFormatter = new SimpleDateFormat("yyyy-MM");
			for (int i=0;i<sourceList.size();i++){
				customStartAndEndDates.add(i,new long[] {startDate,endDate});
				rangeTitles.add("All");
				if (monthlyRadio.isSelected()){
					long previousDate = startDate;
					GregorianCalendar monthCal = new GregorianCalendar();
					monthCal.setTimeZone(TimeZone.getTimeZone("GMT+10"));
					monthCal.setTimeInMillis(startDate);
					monthCal.set(GregorianCalendar.DAY_OF_MONTH, 1);
					monthCal.set(GregorianCalendar.HOUR_OF_DAY, 0);
					monthCal.set(GregorianCalendar.MINUTE, 0);
					monthCal.set(GregorianCalendar.SECOND, 0);
					monthCal.add(GregorianCalendar.MONTH,1);
					while(monthCal.getTimeInMillis()<=endDate){
						sourceList.add(i+1,sourceList.get(i)); //replicate current source
						long newDate = monthCal.getTimeInMillis();
						customStartAndEndDates.set(i, new long[] {previousDate+60000,newDate});
						rangeTitles.set(i, monthFormatter.format(previousDate));
						previousDate = newDate;

						//move to new source
						i++;
						customStartAndEndDates.add(i,new long[] {previousDate+6000,endDate});
						rangeTitles.add(i, monthFormatter.format(previousDate));
						monthCal.add(GregorianCalendar.MONTH,1);
					}
				}
			}
			
			if (sourceList.size() > 0){
				Thread averageAnalysisProcess = new Thread(new TimeOfDayAnalysis(new LogWindow("Time Of Day Analysis Log"),dbConn,sourceList,customStartAndEndDates,rangeTitles,startDate,endDate,samplePeriod,analysisType,splitType,siteSelectionType.getSelectedItem().equals("All")));
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
