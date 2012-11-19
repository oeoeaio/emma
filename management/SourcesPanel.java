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
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class SourcesPanel extends JPanel implements ActionListener,TableModelListener{
	private static final long serialVersionUID = 4821170322264443848L;

	JPanel topPanel = new JPanel(new FlowLayout());
	JLabel siteIDL = new JLabel("Site ID: ");
	JTextField siteIDF = new JTextField(10);
	JLabel sourceIDL = new JLabel("Source ID: ");
	JTextField sourceIDF = new JTextField(10);
	JButton updateB = new JButton("Update");
	JPanel tablePanel = new JPanel();
	JTable sourcesTable = new JTable() {
		private static final long serialVersionUID = 2855254660435667704L;
		public boolean isCellEditable(int rowIndex, int vColIndex) {
			if (vColIndex < 2) {
				return false;
			}
			else{
				return true;
			}
	    }
		public Component prepareRenderer(TableCellRenderer renderer,int rowIndex, int vColIndex) {
			Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
			if (vColIndex < 2) {
				c.setForeground(Color.gray);
			} else {
				c.setForeground(getForeground());
			}
			return c;
		}
	};
	ListSelectionModel sourcesTableListModel = sourcesTable.getSelectionModel();
	DefaultTableModel sourcesTableModel = (DefaultTableModel)sourcesTable.getModel();
	JScrollPane tableScroll = new JScrollPane(sourcesTable);
	Connection dbConn;
	
	Color oldSelectionBackground = sourcesTable.getSelectionBackground();
	Color oldSelectionForeground = sourcesTable.getSelectionForeground();
	
	public SourcesPanel(Connection dbConn){
		this.dbConn = dbConn;
		this.setLayout(new BorderLayout());
		
		topPanel.setAlignmentX(CENTER_ALIGNMENT);
		topPanel.add(siteIDL);
		topPanel.add(siteIDF);
		topPanel.add(sourceIDL);
		topPanel.add(sourceIDF);
		topPanel.add(updateB);
		updateB.addActionListener(this);
		
		sourcesTableModel.setColumnIdentifiers(new String[] {"Site ID","Source ID","Source Name","Source Type","Brand","Model","Serial","Location"});
		sourcesTableModel.addTableModelListener(this);
		sourcesTableListModel.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
		sourcesTable.setAutoCreateRowSorter(true);
		tableScroll.setPreferredSize(new Dimension(940,600));
		tablePanel.add(tableScroll);
		
		updateSources();
		
		sourcesTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(new JComboBox(new String[] {"FZ","RF","TP","VM"})));
		
		this.add(topPanel,BorderLayout.NORTH);
		this.add(tablePanel,BorderLayout.CENTER);
	}
	
	void updateSources(){
		greyOutTable();
		sourcesTableModel.setNumRows(0);
		Thread fetchThread = new Thread(new FetchSourcesData(dbConn,sourcesTableModel,siteIDF.getText().toString(),sourceIDF.getText().toString()));
		fetchThread.start();
		unGreyTable();
		//TODO fix this ^^
	}
	
	void greyOutTable(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				sourcesTable.setEnabled(false);
				oldSelectionBackground = sourcesTable.getSelectionBackground();
				oldSelectionForeground = sourcesTable.getSelectionForeground();
				sourcesTable.setSelectionBackground(Color.GRAY);
				sourcesTable.setSelectionForeground(Color.DARK_GRAY);
				sourcesTable.setForeground(Color.GRAY);
				sourcesTable.setBackground(null);
			}
		});
	}
	
	public void unGreyTable(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				sourcesTable.setSelectionBackground(oldSelectionBackground);
				sourcesTable.setSelectionForeground(Color.WHITE);
				sourcesTable.setForeground(Color.BLACK);
				sourcesTable.setBackground(Color.WHITE);
				sourcesTable.setEnabled(true);
			}
		});
	}
	
	@Override
	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(updateB) && (siteIDF.getText().matches("\\d{1,10}") || siteIDF.getText().equals("")) && (sourceIDF.getText().matches("\\d{1,10}") || sourceIDF.getText().equals(""))){
			updateSources();
		}
		else{
			unGreyTable();
		}
	}

	@Override
	public void tableChanged(TableModelEvent tME) {
		if (tME.getType()==TableModelEvent.UPDATE && tME.getSource().equals(sourcesTableModel)){
			try {
				Statement MySQL_Statement = dbConn.createStatement();
				String updSourceSQL = "UPDATE sources SET source_name="+(sourcesTable.getValueAt(tME.getFirstRow(),2).toString().equals("")?"NULL":"'"+sourcesTable.getValueAt(tME.getFirstRow(),2).toString()+"'")+",source_type='"+sourcesTable.getValueAt(tME.getFirstRow(),3).toString()+"',brand="+(sourcesTable.getValueAt(tME.getFirstRow(),4).toString().equals("")?"NULL":"'"+sourcesTable.getValueAt(tME.getFirstRow(),4).toString()+"'")+",model="+(sourcesTable.getValueAt(tME.getFirstRow(),5).toString().equals("")?"NULL":"'"+sourcesTable.getValueAt(tME.getFirstRow(),5).toString()+"'")+",serial="+(sourcesTable.getValueAt(tME.getFirstRow(),6).toString().equals("")?"NULL":"'"+sourcesTable.getValueAt(tME.getFirstRow(),6).toString()+"'")+",location="+(sourcesTable.getValueAt(tME.getFirstRow(),7).toString().equals("")?"NULL":"'"+sourcesTable.getValueAt(tME.getFirstRow(),7).toString()+"'")+" WHERE site_id = "+sourcesTable.getValueAt(tME.getFirstRow(),0).toString()+" AND source_id = "+sourcesTable.getValueAt(tME.getFirstRow(),1).toString(); //updates specified information into the database
				System.out.println(updSourceSQL);
				MySQL_Statement.executeUpdate(updSourceSQL);

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
