package management;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class SitesPanel extends JPanel implements ActionListener,TableModelListener{
	private static final long serialVersionUID = 4821170322264443848L;

	JPanel topPanel = new JPanel(new FlowLayout());
	JLabel siteIDL = new JLabel("Site ID: ");
	JTextField siteIDF = new JTextField(10);
	JButton updateB = new JButton("Update");
	JPanel tablePanel = new JPanel();
	JTable sitesTable = new JTable() {
		private static final long serialVersionUID = 8782310988232381585L;
		public boolean isCellEditable(int rowIndex, int vColIndex) {
			if (vColIndex < 1) {
				return false;
			}
			else{
				return true;
			}
	    }
		public Component prepareRenderer(TableCellRenderer renderer,int rowIndex, int vColIndex) {
			Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
			if (vColIndex < 1) {
				c.setForeground(Color.gray);
			} else {
				c.setForeground(getForeground());
			}
			return c;
		}
	};
	ListSelectionModel sitesTableListModel = sitesTable.getSelectionModel();
	DefaultTableModel sitesTableModel = (DefaultTableModel)sitesTable.getModel();
	JScrollPane tableScroll = new JScrollPane(sitesTable);
	Connection dbConn;
	
	Color oldSelectionBackground = sitesTable.getSelectionBackground();;
	Color oldSelectionForeground = sitesTable.getSelectionForeground();;
	
	public SitesPanel(Connection dbConn){
		this.dbConn = dbConn;
		this.setLayout(new BorderLayout());
		
		topPanel.setAlignmentX(CENTER_ALIGNMENT);
		topPanel.add(siteIDL);
		topPanel.add(siteIDF);
		topPanel.add(updateB);
		updateB.addActionListener(this);
		
		sitesTableModel.setColumnIdentifiers(new String[] {"Site ID","Given Name","Surname","Suburb","State"});
		sitesTableModel.addTableModelListener(this);
		sitesTableListModel.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
		sitesTable.setAutoCreateRowSorter(true);
		tableScroll.setPreferredSize(new Dimension(940,600));
		tablePanel.add(tableScroll);
				
		updateSites();
		
		sitesTable.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(new JComboBox(new String[] {"","NSW","VIC","QLD","ACT","WA","SA","TAS","NT"})));
		
		this.add(topPanel,BorderLayout.NORTH);
		this.add(tablePanel,BorderLayout.CENTER);
	}
	
	void updateSites(){
		greyOutTable();
		sitesTableModel.setNumRows(0);
		Thread fetchThread = new Thread(new FetchSitesData(this,dbConn,sitesTableModel,siteIDF.getText().toString()));
		fetchThread.start();
	}
	
	void greyOutTable(){
		sitesTable.setEnabled(false);
		oldSelectionBackground = sitesTable.getSelectionBackground();
		oldSelectionForeground = sitesTable.getSelectionForeground();
		sitesTable.setSelectionBackground(Color.GRAY);
		sitesTable.setSelectionForeground(Color.DARK_GRAY);
		sitesTable.setForeground(Color.GRAY);
		sitesTable.setBackground(null);
	}
	
	public void unGreyTable(){
		sitesTable.setSelectionBackground(oldSelectionBackground);
		sitesTable.setSelectionForeground(oldSelectionForeground);
		sitesTable.setForeground(Color.BLACK);
		sitesTable.setBackground(Color.WHITE);
		sitesTable.setEnabled(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(updateB) && (siteIDF.getText().matches("\\d{1,10}") || siteIDF.getText().equals(""))){
			updateSites();
		}
		else{
			unGreyTable();
		}
	}
	
	@Override
	public void tableChanged(TableModelEvent tME) {
		if (tME.getType()==TableModelEvent.UPDATE && tME.getSource().equals(sitesTableModel)){
			try {
				Statement MySQL_Statement = dbConn.createStatement();
				String updSourceSQL = "UPDATE sites SET given_name="+(sitesTable.getValueAt(tME.getFirstRow(),1).toString().equals("")?"NULL":"'"+sitesTable.getValueAt(tME.getFirstRow(),1).toString()+"'")+",surname="+(sitesTable.getValueAt(tME.getFirstRow(),2).toString().equals("")?"NULL":"'"+sitesTable.getValueAt(tME.getFirstRow(),2).toString()+"'")+",suburb="+(sitesTable.getValueAt(tME.getFirstRow(),3).toString().equals("")?"NULL":"'"+sitesTable.getValueAt(tME.getFirstRow(),3).toString()+"'")+",state="+(sitesTable.getValueAt(tME.getFirstRow(),4).toString().equals("")?"NULL":"'"+sitesTable.getValueAt(tME.getFirstRow(),4).toString()+"'")+" WHERE site_id = "+sitesTable.getValueAt(tME.getFirstRow(),0).toString(); //updates specified information into the database
				System.out.println(updSourceSQL);
				MySQL_Statement.executeUpdate(updSourceSQL);

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
