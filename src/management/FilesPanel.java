package management;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import javax.swing.table.TableCellRenderer;

public class FilesPanel extends JPanel implements ActionListener{
	private static final long serialVersionUID = 4821170322264443848L;

	JPanel topPanel = new JPanel(new FlowLayout());
	JLabel siteIDL = new JLabel("Site ID: ");
	JTextField siteIDF = new JTextField(10);
	JLabel sourceIDL = new JLabel("Source ID: ");
	JTextField sourceIDF = new JTextField(10);
	JButton updateB = new JButton("Update");
	JPanel tablePanel = new JPanel();
	JTable filesTable = new JTable() {
		private static final long serialVersionUID = 2855254660435667704L;
		public boolean isCellEditable(int rowIndex, int vColIndex) {
			return false;
	    }
		public Component prepareRenderer(TableCellRenderer renderer,int rowIndex, int vColIndex) {
			Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
			c.setForeground(Color.gray);
			return c;
		}
	};
	ListSelectionModel filesTableListModel = filesTable.getSelectionModel();
	DefaultTableModel filesTableModel = (DefaultTableModel)filesTable.getModel();
	JScrollPane tableScroll = new JScrollPane(filesTable);
	Connection dbConn;
	
	Color oldSelectionBackground = filesTable.getSelectionBackground();;
	Color oldSelectionForeground = filesTable.getSelectionForeground();;
	
	public FilesPanel(Connection dbConn){
		this.dbConn = dbConn;
		this.setLayout(new BorderLayout());
		
		topPanel.setAlignmentX(CENTER_ALIGNMENT);
		topPanel.add(siteIDL);
		topPanel.add(siteIDF);
		topPanel.add(sourceIDL);
		topPanel.add(sourceIDF);
		topPanel.add(updateB);
		updateB.addActionListener(this);
		
		filesTableModel.setColumnIdentifiers(new String[] {"File ID","Site ID","Source ID","File Name","Meter Serial","Frequency","Start Time","End Time"});
		filesTableListModel.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
		filesTable.setAutoCreateRowSorter(true);
		tableScroll.setPreferredSize(new Dimension(940,600));
		tablePanel.add(tableScroll);
		
		updateFiles();
		
		this.add(topPanel,BorderLayout.NORTH);
		this.add(tablePanel,BorderLayout.CENTER);
	}
	
	void updateFiles(){
		//greyOutTable();
		Thread fetchThread = new Thread(new FetchFilesData(this,dbConn,filesTableModel,siteIDF.getText().toString(),sourceIDF.getText().toString()));
		fetchThread.start();
	}
	
	void greyOutTable(){
		oldSelectionBackground = filesTable.getSelectionBackground();
		oldSelectionForeground = filesTable.getSelectionForeground();
		filesTable.setSelectionBackground(Color.GRAY);
		filesTable.setSelectionForeground(Color.DARK_GRAY);
		filesTable.setForeground(Color.GRAY);
		filesTable.setBackground(null);
		filesTable.setEnabled(false);
	}
	
	public void unGreyTable(){
		filesTable.setSelectionBackground(oldSelectionBackground);
		filesTable.setSelectionForeground(oldSelectionForeground);
		filesTable.setForeground(Color.BLACK);
		filesTable.setBackground(Color.WHITE);
		filesTable.setEnabled(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(updateB) && (siteIDF.getText().matches("\\d{1,10}") || siteIDF.getText().equals("")) && (sourceIDF.getText().matches("\\d{1,10}") || sourceIDF.getText().equals(""))){
			updateFiles();
		}
		else{
			unGreyTable();
		}
	}
}
