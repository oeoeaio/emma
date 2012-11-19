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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
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
import endUseWindow.Source;
import endUseWindow.SourceTable;


public class RawDataExportPanel extends JPanel implements ItemListener,ActionListener,ListSelectionListener {
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
	
	public RawDataExportPanel(final Connection dbConn){
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
		
		datePanel.add(startDateL);
		datePanel.add(startDateS);
		datePanel.add(endDateL);
		datePanel.add(endDateS);
		datePanel.add(inclusiveL);
		startPanel.add(startButton);
		startButton.addActionListener(this);
		
		siteSelectionType.addItemListener(this);
		startDateS.addItemListener(this);
		siteTable.siteListModel.addListSelectionListener(this);
		sourceTable.sourceListModel.addListSelectionListener(this);
		
		siteTable.updateList(dbConn);
		this.add(mainPanel,BorderLayout.CENTER);
	}
	
	void populateStartDate() throws SQLException,Exception{
		startDateS.removeItemListener(this);
		long startDate = 0;
		if (dateRange.size()>0 && startDateS.getSelectedIndex()!=-1 && endDateS.getSelectedIndex()!=-1){
			startDate = dateRange.get(startDateS.getSelectedIndex());
		}

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
			String getMinMaxSQL = "SELECT UNIX_TIMESTAMP(DATE(MIN(start_date))) AS min_date,UNIX_TIMESTAMP(DATE_ADD(DATE(MAX(end_date)),INTERVAL 1 DAY)) AS max_date FROM files WHERE"+(siteSelectionType.getSelectedItem().equals("All")?"":" site_id = "+siteTable.getValueAt(siteTable.getSelectedRow(),0)+" AND ")+" source_id IN "+sourceList;
			//System.out.println(getMinMaxSQL);
			ResultSet minMaxResults = MySQL_Statement.executeQuery(getMinMaxSQL);
			//System.out.println(new Date().getTime()-before.getTime());
			if (minMaxResults.next()){
				Calendar minCal = Calendar.getInstance();
				minCal.setTimeZone(TimeZone.getTimeZone("GMT+10"));
				minCal.setTimeInMillis(minMaxResults.getLong("min_date")*1000);
				long maxCal = minMaxResults.getLong("max_date")*1000;
			
				for (int i=0;minCal.getTimeInMillis() < maxCal;i++){
					dateRange.add(minCal.getTimeInMillis());
					startDateS.addItem(dateFormatter.format(minCal.getTimeInMillis()));
					minCal.add(Calendar.DATE, 1);
				}
				dateRange.add(minCal.getTimeInMillis()); //add one additional date (so "inclusive" works)
				startDateS.setEnabled(true);
				
				if (startDate > 0 && dateRange.indexOf(startDate) >= 0){
					int startIndex = dateRange.indexOf(startDate);
					startDateS.setSelectedIndex(startIndex);
				}
				
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
		
		startDateS.addItemListener(this);
	}
	
	void populateEndDate(){
		long endDate = 0;
		SimpleDateFormat dateParser = new SimpleDateFormat("dd/MM/yyyy");
		dateParser.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		
		if (endDateS.getSelectedIndex()!=-1){
			try{
				endDate = dateParser.parse(endDateS.getSelectedItem().toString()).getTime();
			}
			catch (ParseException pE){
				//do nothing
			}
		}
		endDateS.removeAllItems();
		for (int i=Math.max(0,startDateS.getSelectedIndex());i<dateRange.size()-1;i++){
			endDateS.addItem(dateFormatter.format(dateRange.get(i)));
		}
		endDateS.setEnabled(true);
		if (endDateS.getItemCount() > 0 && endDate > 0 && dateRange.indexOf(endDate) >= 0){
			int endIndex = Math.max(0, Math.min(dateRange.size()-1, dateRange.indexOf(endDate))-startDateS.getSelectedIndex());
			endDateS.setSelectedIndex(endIndex);
		}
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
			long endDate = dateRange.get(startDateS.getSelectedIndex()+endDateS.getSelectedIndex()+1);			
			
			LinkedList<long[]> customStartAndEndDates = new LinkedList<long[]>();
			LinkedList<Integer> frequencies = new LinkedList<Integer>();
			try{
				//Check for multiple frequencies
				ResultSet getFreqCountResults;
				ResultSet getFrequenciesResults;
				for (int i=0;i<sourceList.size();i++){
					String getFreqCountSQL = "SELECT COUNT(DISTINCT frequency) AS freqCount FROM files WHERE "+(siteSelectionType.getSelectedItem().equals("All")?"":"site_id = "+siteTable.getValueAt(siteTable.getSelectedRow(),0)+" AND ")+"source_id = "+sourceList.get(i).getSourceID()+" AND end_date >= '"+sqlDateFormatter.format(startDate)+"' AND start_date <= '"+sqlDateFormatter.format(endDate)+"'";
					String getFrequenciesSQL = "SELECT UNIX_TIMESTAMP(start_date) AS start_date,frequency FROM files WHERE "+(siteSelectionType.getSelectedItem().equals("All")?"":"site_id = "+siteTable.getValueAt(siteTable.getSelectedRow(),0)+" AND ")+"source_id = "+sourceList.get(i).getSourceID()+" AND end_date >= '"+sqlDateFormatter.format(startDate)+"' AND start_date <= '"+sqlDateFormatter.format(endDate)+"' ORDER BY start_date ASC";
					getFreqCountResults = dbConn.createStatement().executeQuery(getFreqCountSQL);
					getFrequenciesResults = dbConn.createStatement().executeQuery(getFrequenciesSQL);
					//System.out.println(getFreqCountSQL);
					//System.out.println(getFrequenciesSQL);
					if (getFreqCountResults.next() && getFrequenciesResults.next()){
						customStartAndEndDates.add(i,new long[] {startDate+60000,endDate});
						frequencies.add(i,0);
						if (getFreqCountResults.getInt("freqCount")!=1){
							//String errorMsg = "<html>Multiple recording intervals found for source: "+selectedSources[i].getSourceName()+" (id: "+selectedSources[i].getSourceID()+")";
							//errorMsg += "<br /><br /><table cellspacing=10><tr><td><b>Start Date</b></td><td>&nbsp;&nbsp;&nbsp;</td><td><b>Interval (seconds)</b></td></tr>";
							//errorMsg += "<tr><td>"+sqlDateFormatter.format(getFrequenciesResults.getLong("start_date")*1000)+"</td><td>&nbsp;&nbsp;&nbsp;</td><td>"+getFrequenciesResults.getInt("frequency")+"</td></tr>";
							long previousDate = startDate;
							int previousFreq = getFrequenciesResults.getInt("frequency");
							while(getFrequenciesResults.next()){
								if (getFrequenciesResults.getInt("frequency")!=previousFreq){
									sourceList.add(i+1,sourceList.get(i)); //replicate current source
									//errorMsg += "<tr><td>"+sqlDateFormatter.format(getFrequenciesResults.getLong("start_date")*1000)+"</td><td>&nbsp;&nbsp;&nbsp;</td><td>"+getFrequenciesResults.getInt("frequency")+"</td></tr>";
									long newDate = getFrequenciesResults.getLong("start_date")*1000;
									customStartAndEndDates.set(i, new long[] {previousDate,newDate-(1000*getFrequenciesResults.getInt("frequency"))});
									frequencies.set(i,previousFreq);
									previousFreq = getFrequenciesResults.getInt("frequency");
									previousDate = newDate;
									
									//create a new source
									i++;
									customStartAndEndDates.add(i,new long[] {previousDate,endDate});
									frequencies.add(i,previousFreq);
								}
							}
							//errorMsg += "</table></html>";
							//throw new Exception(errorMsg);
						}
						else{
							frequencies.set(i,getFrequenciesResults.getInt("frequency"));
						}
					}
					else{
						throw new SourceException("Error: no relevant data found for source: "+sourceList.get(i).getSourceName()+"(id: "+sourceList.get(i).getSourceID()+")");
					}
				}
				
				if (sourceList.size() > 0){
					//System.out.println(sqlDateFormatter.format(startDate)+" "+sqlDateFormatter.format(endDate));
					Thread rawDataExportProcess = new Thread(new RawDataExport(new LogWindow("Average analysis process log"),dbConn,sourceList,customStartAndEndDates,frequencies,siteSelectionType.getSelectedItem().equals("All"),startDate,endDate));
					rawDataExportProcess.start();
				}
				else{
					JOptionPane.showMessageDialog(this, "No sources selected for processing.\r\nPlease ensure at least one source is selected.", "No Channels Selected", JOptionPane.WARNING_MESSAGE);
				}
			}catch(SQLException sE){
				sE.printStackTrace();
				JOptionPane.showMessageDialog(this, "Unable to access database to verify that analysis request is valid.", "Database Error", JOptionPane.ERROR_MESSAGE);
			}catch(SourceException sE){
				JOptionPane.showMessageDialog(this, sE.msg, "Error", JOptionPane.ERROR_MESSAGE);
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
	
	private class SourceException extends Exception{
		private static final long serialVersionUID = 1L;
		private final String msg;
		
		SourceException(String msg){
			this.msg = msg;
		}
	}
}
