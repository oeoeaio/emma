package management;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public class MetersPanel extends JPanel implements ActionListener{
	private static final long serialVersionUID = 4821170322264443848L;

	JPanel topPanel = new JPanel(new FlowLayout());
	JLabel siteIDL = new JLabel("Site ID: ");
	JTextField siteIDF = new JTextField(10);
	JButton updateB = new JButton("Update");
	JPanel tablePanel = new JPanel();
	JTable metersTable = new JTable() {
		private static final long serialVersionUID = 2855254660435667704L;
		public boolean isCellEditable(int rowIndex, int vColIndex) {
	        return false;
	    }
	};
	ListSelectionModel metersTableListModel = metersTable.getSelectionModel();
	DefaultTableModel metersTableModel = (DefaultTableModel)metersTable.getModel();
	JScrollPane tableScroll = new JScrollPane(metersTable);
	Connection dbConn;
	
	Color oldSelectionBackground = metersTable.getSelectionBackground();;
	Color oldSelectionForeground = metersTable.getSelectionForeground();;
	
	public MetersPanel(Connection dbConn){
		this.dbConn = dbConn;
		this.setLayout(new BorderLayout());
		
		topPanel.setAlignmentX(CENTER_ALIGNMENT);
		topPanel.add(siteIDL);
		topPanel.add(siteIDF);
		topPanel.add(updateB);
		updateB.addActionListener(this);
		
		metersTableModel.setColumnIdentifiers(new String[] {"Meter Serial","Meter Type","Current Site ID","Date Installed","Date Full","Frequency","Battery Installed","Battery Replace"});
		metersTableListModel.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
		metersTable.setAutoCreateRowSorter(true);
		tableScroll.setPreferredSize(new Dimension(940,600));
		tablePanel.add(tableScroll);
		
		updateMeters();
		
		this.add(topPanel,BorderLayout.NORTH);
		this.add(tablePanel,BorderLayout.CENTER);
	}
	
	void updateMeters(){
		greyOutTable();
		metersTableModel.setNumRows(0);
		Thread fetchThread = new Thread(new FetchMetersData(this,dbConn,metersTableModel,siteIDF.getText().toString()));
		fetchThread.start();
	}
	
	void greyOutTable(){
		metersTable.setEnabled(false);
		oldSelectionBackground = metersTable.getSelectionBackground();
		oldSelectionForeground = metersTable.getSelectionForeground();
		metersTable.setSelectionBackground(Color.GRAY);
		metersTable.setSelectionForeground(Color.DARK_GRAY);
		metersTable.setForeground(Color.GRAY);
		metersTable.setBackground(null);
	}
	
	public void unGreyTable(){
		metersTable.setSelectionBackground(oldSelectionBackground);
		metersTable.setSelectionForeground(oldSelectionForeground);
		metersTable.setForeground(Color.BLACK);
		metersTable.setBackground(Color.WHITE);
		metersTable.setEnabled(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(updateB) && (siteIDF.getText().matches("\\d{1,10}") || siteIDF.getText().equals(""))){
			updateMeters();
		}
		else{
			unGreyTable();
		}
	}
}
