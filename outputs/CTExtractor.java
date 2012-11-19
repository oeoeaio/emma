package outputs;

import java.awt.FlowLayout;
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
import java.util.TimeZone;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;

import endUseWindow.LogWindow;


public class CTExtractor extends JFrame implements Runnable,ItemListener,ActionListener {
	private static final long serialVersionUID = 1753874715599117347L;

	//Main Panel
	JPanel mainPanel = new JPanel();

	//Installation Panel
	JPanel instPanel = new JPanel(new FlowLayout());
	JLabel installation_id_selectL = new JLabel("Installation ID");
	JComboBox installation_id_select = new JComboBox();
	
	//Radio Button Panel
	JPanel radPanel = new JPanel(new FlowLayout());
	//Radio Label
	JLabel radioLabel = new JLabel("Channels");
	//Radio Buttons	
	JRadioButton allChannels = new JRadioButton("All");
	JRadioButton selectedChannels = new JRadioButton("Selection");
	
	//Checkbox Panel
	JPanel chPanel = new JPanel();
	ArrayList<JPanel> chPanels = new ArrayList<JPanel>();
	ArrayList<JLabel> chPanelLs = new ArrayList<JLabel>();
	ArrayList<JCheckBox> module_checks = new ArrayList<JCheckBox>();
	ArrayList<JCheckBox[]> chPanelChs = new ArrayList<JCheckBox[]>();

	
	//Date Panel
	JPanel datePanel = new JPanel(new FlowLayout());
	JLabel startDateL = new JLabel("Start Date");
	JComboBox startDateS = new JComboBox();
	JLabel endDateL = new JLabel("End Date");
	JComboBox endDateS = new JComboBox();
	JLabel exclusiveL = new JLabel("(exclusive)");
	
	//Start Panel
	JPanel startPanel = new JPanel(new FlowLayout());
	JButton startButton = new JButton("Begin Extraction");
	
	SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
	SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	Connection dbConn;
	
	LogWindow logWindow;
	
	ArrayList<int[]> moduleList = new ArrayList<int[]>();
	ArrayList<long[]> dateRange = new ArrayList<long[]>();
	
	CTExtractor(Connection dbConn,LogWindow logWindow){
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		this.dbConn = dbConn;
		this.logWindow = logWindow;
		this.setSize(600,300);
		this.setLocation(300,200);
		this.setTitle("Extract CT Channels from Database");
		this.setVisible(true);
		
		mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.PAGE_AXIS));
		chPanel.setLayout(new BoxLayout(chPanel,BoxLayout.PAGE_AXIS));
		
		mainPanel.add(instPanel);
		mainPanel.add(radPanel);
		mainPanel.add(chPanel);	
		mainPanel.add(datePanel);
		mainPanel.add(startPanel);
		
		instPanel.add(installation_id_selectL);
		instPanel.add(installation_id_select);
		radPanel.add(radioLabel);
		radPanel.add(allChannels);
		radPanel.add(selectedChannels);
		
		for (int i=0;i<3;i++){ //build Channel Panels
			chPanels.add(new JPanel(new FlowLayout()));
			chPanel.add(chPanels.get(i));
			
			chPanelLs.add(new JLabel());
			chPanels.get(i).add(chPanelLs.get(i));
			module_checks.add(new JCheckBox());
			chPanels.get(i).add(module_checks.get(i));
			chPanels.get(i).add(new JLabel("   "));
			chPanelChs.add(new JCheckBox[6]);
			for (int j=0;j<6;j++){
				chPanelChs.get(i)[j] = new JCheckBox(Integer.toString(j+1));
				chPanels.get(i).add(chPanelChs.get(i)[j]);
				chPanelChs.get(i)[j].addItemListener(this);
			}
		}
		
		datePanel.add(startDateL);
		datePanel.add(startDateS);
		datePanel.add(endDateL);
		datePanel.add(endDateS);
		datePanel.add(exclusiveL);
		startPanel.add(startButton);
		startButton.addActionListener(this);
		
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(allChannels);
		buttonGroup.add(selectedChannels);
		allChannels.addItemListener(this);
		selectedChannels.addItemListener(this);
		allChannels.setSelected(true);

		getContentPane().add(mainPanel);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.validate();
	}
	
	public void run(){
		try {
			if (dbConn!=null){
				Statement getInstallationsStatement = dbConn.createStatement();
				String getInstallationsSQL = "SELECT DISTINCT installation_id FROM data_all ORDER BY installation_id";
				ResultSet getInstallationsRS = getInstallationsStatement.executeQuery(getInstallationsSQL);
				while (getInstallationsRS.next()){
					installation_id_select.addItem(getInstallationsRS.getInt("installation_id"));
					
					Statement getMaxStatement = dbConn.createStatement();
					String getMaxSQL = "SELECT UNIX_TIMESTAMP(DATE_ADD(DATE(MIN(date_time)),INTERVAL 1 DAY)) AS minDate,UNIX_TIMESTAMP(DATE(MAX(date_time))) AS maxDate FROM data_all WHERE installation_id = "+getInstallationsRS.getInt("installation_id");
					ResultSet getMaxRS = getMaxStatement.executeQuery(getMaxSQL);
					if (getMaxRS.next()){
						Calendar minCal = Calendar.getInstance();
						minCal.setTimeZone(TimeZone.getTimeZone("GMT+10"));
						minCal.setTimeInMillis(getMaxRS.getLong("minDate")*1000);
						Calendar maxCal = Calendar.getInstance();
						maxCal.setTimeZone(TimeZone.getTimeZone("GMT+10"));
						maxCal.setTimeInMillis(getMaxRS.getLong("maxDate")*1000);
						
						int rowsRequired = (int)((maxCal.getTimeInMillis()-minCal.getTimeInMillis())/(60000*60*24) + 1);
	
						long[] dateRange_TEMP = new long[rowsRequired];
						for (int i=0;minCal.getTimeInMillis() <= maxCal.getTimeInMillis();i++){
							dateRange_TEMP[i] = minCal.getTimeInMillis();
							minCal.add(Calendar.DATE, 1);
						}
						dateRange.add(dateRange_TEMP);
					}
					
					Statement getModulesStatement = dbConn.createStatement();
					String getModulesSQL = "SELECT ct_sns FROM header_log WHERE installation_id = "+getInstallationsRS.getInt("installation_id")+" ORDER BY date_time DESC";
					ResultSet getModulesRS = getModulesStatement.executeQuery(getModulesSQL);
					if (getModulesRS.next()){
						String[] tempStrings = getModulesRS.getString("ct_sns").split(",");
						int[] tempInts = new int[tempStrings.length];
						for (int i=0;i<tempInts.length;i++){
							tempInts[i] = Integer.parseInt(tempStrings[i]);
						}
						moduleList.add(tempInts);
					}
				}
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		for (int i=0;i<module_checks.size();i++){
			module_checks.get(i).addItemListener(this);
		}
		populateStartDate();
		populateEndDate();
		updateChannelPanels();
		installation_id_select.setSelectedIndex(0);
		installation_id_select.addItemListener(this);
		startDateS.addItemListener(this);
		

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
	
	void populateStartDate(){
		startDateS.removeAllItems();
		for (int i=0;i<dateRange.get(installation_id_select.getSelectedIndex()).length;i++){
			startDateS.addItem(dateFormatter.format(dateRange.get(installation_id_select.getSelectedIndex())[i]));
		}
		startDateS.setEnabled(true);
	}
	
	void populateEndDate(){
		endDateS.removeAllItems();
		for (int i=Math.max(0,startDateS.getSelectedIndex());i<dateRange.get(installation_id_select.getSelectedIndex()).length;i++){
			endDateS.addItem(dateFormatter.format(dateRange.get(installation_id_select.getSelectedIndex())[i]));
		}
		endDateS.setEnabled(true);
	}
	
	void updateChannelPanels(){
		for (int i=0;i<chPanels.size();i++){
			if(i<moduleList.get(installation_id_select.getSelectedIndex()).length){
				chPanels.get(i).setVisible(true);
				chPanelLs.get(i).setText("Module "+moduleList.get(installation_id_select.getSelectedIndex())[i]);
				if (selectedChannels.isSelected()){
					module_checks.get(i).setEnabled(true);
					module_checks.get(i).setSelected(true);
					for (int j=0;j<chPanelChs.get(i).length;j++){
						chPanelChs.get(i)[j].setEnabled(true);
						chPanelChs.get(i)[j].setSelected(true);
					}
				}
				else if (allChannels.isSelected()){
					module_checks.get(i).setEnabled(false);
					module_checks.get(i).setSelected(true);
					for (int j=0;j<chPanelChs.get(i).length;j++){
						chPanelChs.get(i)[j].setEnabled(false);
						chPanelChs.get(i)[j].setSelected(true);
					}
				}
			}
			else{
				chPanels.get(i).setVisible(false);
				chPanelLs.get(i).setText("");
				module_checks.get(i).setEnabled(false);
				module_checks.get(i).setSelected(false);
				for (int j=0;j<chPanelChs.get(i).length;j++){
					chPanelChs.get(i)[j].setEnabled(false);
					chPanelChs.get(i)[j].setSelected(false);
				}
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent iE) {
		if (iE.getSource().equals(installation_id_select) && iE.getStateChange()==ItemEvent.SELECTED){
			populateStartDate();
			populateEndDate();
			updateChannelPanels();
		}
		else if (iE.getSource().equals(startDateS) && iE.getStateChange()==ItemEvent.SELECTED){
			populateEndDate();
		}
		for (int i=0;i<module_checks.size();i++){
			for (int j=0;j<chPanelChs.get(i).length;j++){
				if(iE.getSource().equals(chPanelChs.get(i)[j]) && chPanelChs.get(i)[j].isSelected()==false && module_checks.get(i).isSelected()){
					if(((chPanelChs.get(i)[0].isSelected()?1:0)+(chPanelChs.get(i)[1].isSelected()?1:0)+(chPanelChs.get(i)[2].isSelected()?1:0)+(chPanelChs.get(i)[3].isSelected()?1:0)+(chPanelChs.get(i)[4].isSelected()?1:0)+(chPanelChs.get(i)[5].isSelected()?1:0)) == 0){
						module_checks.get(i).setSelected(false);
					}
				}
				/*else if (iE.getSource().equals(chPanelChs.get(i)[j]) && chPanelChs.get(i)[j].isSelected()==true && module_checks.get(i).isSelected()==false){
					if(((chPanelChs.get(i)[0].isSelected()?1:0)+(chPanelChs.get(i)[1].isSelected()?1:0)+(chPanelChs.get(i)[2].isSelected()?1:0)+(chPanelChs.get(i)[3].isSelected()?1:0)+(chPanelChs.get(i)[4].isSelected()?1:0)+(chPanelChs.get(i)[5].isSelected()?1:0)) == 1){
						module_checks.get(i).setSelected(true);
					}
				}*/
			}
		}
		for (int i=0;i<module_checks.size();i++){
			if(iE.getSource().equals(module_checks.get(i)) && module_checks.get(i).isSelected()==false && module_checks.get(i).isEnabled()==true){
				for (int j=0;j<chPanelChs.get(i).length;j++){
					chPanelChs.get(i)[j].setEnabled(false);
					chPanelChs.get(i)[j].setSelected(false);
				}
			}
		}
		for (int i=0;i<module_checks.size();i++){
			if(iE.getSource().equals(module_checks.get(i)) && module_checks.get(i).isSelected()==true && module_checks.get(i).isEnabled()==true){
				for (int j=0;j<chPanelChs.get(i).length;j++){
					chPanelChs.get(i)[j].setEnabled(true);
					chPanelChs.get(i)[j].setSelected(true);
				}
			}
		}
		if (iE.getSource().equals(allChannels) && allChannels.isSelected()){
			for (int i=0;i<module_checks.size();i++){
				if (chPanels.get(i).isVisible()){
					module_checks.get(i).setEnabled(false);
					module_checks.get(i).setSelected(true);
					for (int j=0;j<chPanelChs.get(i).length;j++){
						chPanelChs.get(i)[j].setEnabled(false);
						chPanelChs.get(i)[j].setSelected(true);
					}
				}
			}
		}
		else if (iE.getSource().equals(selectedChannels) && selectedChannels.isSelected()){
			for (int i=0;i<module_checks.size();i++){
				if (chPanels.get(i).isVisible()){
					module_checks.get(i).setEnabled(true);
					module_checks.get(i).setSelected(true);
					for (int j=0;j<chPanelChs.get(i).length;j++){
						chPanelChs.get(i)[j].setEnabled(true);
						chPanelChs.get(i)[j].setSelected(true);
					}
				}
			}
		}
	}

	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(startButton)){
			
			
			boolean[][] mod_ch_states = new boolean[module_checks.size()][6];
			boolean[] module_states = new boolean[module_checks.size()];
			ArrayList<Integer> modules_to_write = new ArrayList<Integer>();
			int[] mod_ch_count = new int[module_checks.size()];
			int total_ch_count = 0;	
			
			Calendar startDate = new GregorianCalendar();
			startDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
			startDate.setTimeInMillis(dateRange.get(installation_id_select.getSelectedIndex())[startDateS.getSelectedIndex()]);
			Calendar endDate = new GregorianCalendar();
			endDate.setTimeZone(TimeZone.getTimeZone("GMT+10"));
			endDate.setTimeInMillis(dateRange.get(installation_id_select.getSelectedIndex())[startDateS.getSelectedIndex()+endDateS.getSelectedIndex()]);
			
			for (int i=0;i<module_checks.size();i++){
				mod_ch_states[i] = new boolean[] {chPanelChs.get(i)[0].isSelected(),chPanelChs.get(i)[1].isSelected(),chPanelChs.get(i)[2].isSelected(),chPanelChs.get(i)[3].isSelected(),chPanelChs.get(i)[4].isSelected(),chPanelChs.get(i)[5].isSelected()};
				mod_ch_count[i] = (module_checks.get(i).isSelected()?((chPanelChs.get(i)[0].isSelected()?1:0)+(chPanelChs.get(i)[1].isSelected()?1:0)+(chPanelChs.get(i)[2].isSelected()?1:0)+(chPanelChs.get(i)[3].isSelected()?1:0)+(chPanelChs.get(i)[4].isSelected()?1:0)+(chPanelChs.get(i)[5].isSelected()?1:0)):0);
				module_states[i] = module_checks.get(i).isSelected();
				if (module_states[i] && mod_ch_count[i] == 0){module_states[i] = false;}  //check that channels are actually selected for selected modules
				if (module_states[i]){modules_to_write.add(i);} //add modules with channels selected to the modules to write group
				total_ch_count = total_ch_count + mod_ch_count[i];
			}
			
			if (total_ch_count > 0){
				this.setVisible(false);
				Thread ctExtractorProcess = new Thread(new CTExtractorProcess(logWindow,dbConn,modules_to_write,mod_ch_count,mod_ch_states,total_ch_count,Integer.parseInt(installation_id_select.getSelectedItem().toString()),installation_id_select.getSelectedIndex(),moduleList,startDate,endDate));
				ctExtractorProcess.start();
				this.dispose();
			}
			else{
				JOptionPane.showMessageDialog(this, "No channels selected for processing.\r\nPlease ensure at least one channel is selected.", "No Channels Selected", JOptionPane.WARNING_MESSAGE);
			}
		}
	}
}
