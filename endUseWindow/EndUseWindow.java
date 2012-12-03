package endUseWindow;

import issueManagement.IssuePanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.Connection;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import dataPlotter.DataPlotPanel;
import fileManagement.CTValidator;
import fileManagement.PDCDecodeWindow;
import fileManagement.PDCImportPanel;
import fileManagement.StandAloneBatchImporter;
import fileManagement.StandAloneImportPanel;


import outputs.AverageAnalysisPanel;
import outputs.DiscreteModeAnalysisPanel;
import outputs.RawDataExportPanel;
import outputs.RefrigAnalysisPanel;
import outputs.TimeOfDayAnalysisPanel;
import tools.BatchFileSNDatesPanel;
import tools.MissingSummaryPanel;

import management.SiteManagementPanel;
import management.SourceManagementPanel;
import missingPlotter.MissingPlotPanel;

public class EndUseWindow extends JFrame implements ActionListener{
	private static final long serialVersionUID = -8895907428954506528L;
	
	WaitGlassPane waitGlassPane = new WaitGlassPane();
	
	JMenuBar menuBar = new JMenuBar();
	
	//File Menu
	JMenu fileMenu = new JMenu("File");
	
	JMenu importMenu = new JMenu("Import");
	JMenuItem importBatchFile = new JMenuItem("Batch File (.csv)");
	JMenuItem standAloneImport = new JMenuItem("Standalone File (.txt/.csv)");
	JMenuItem pdcImport = new JMenuItem("PDC File(s) (.pdc)");
	JMenuItem ctImport = new JMenuItem("CT File (.txt)");
	
	JMenu exportMenu = new JMenu("Export");
	JMenuItem exportBatchFile = new JMenuItem("Batch File (.csv)");
	
	JMenu processMenu = new JMenu("Process");
	JMenuItem processPDCFile = new JMenuItem("PDC File(s) (.pdc) to (.csv)");
	
	//Output Menu
	JMenu outputMenu = new JMenu("Outputs");
	//Refrig
	JMenu refrigMenu = new JMenu("Refrigerator");
	JMenuItem refrigAnalysis = new JMenuItem("Refrig Analysis");
	
	//Standard Analyses
	JMenu standardOutputsMenu = new JMenu("Standard Outputs");
	JMenuItem rawDataExport = new JMenuItem("Export Raw Data");
	JMenuItem avgAnalysis = new JMenuItem("Average/Sum Analysis");
	JMenuItem discreteModeAvg = new JMenuItem("Discrete Mode Analysis");
	JMenuItem TODAnalysis = new JMenuItem("Time Of Day Analysis");
	
	JMenu toolsMenu = new JMenu("Tools");
	JMenuItem createMissingSummary = new JMenuItem("Create Missing Summary");
	JMenuItem getDatesAndSerials = new JMenuItem("Fetch Dates and Serials");
	
	Preferences fileSettings = Preferences.userRoot().node("EndUseFileSettings");
	
	MySQLConnection mySQLConnection = new MySQLConnection();
	
	Connection dbConn = null;

	public static void main(String[] args) {
		try {
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if ("Nimbus".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		            break;
		        }
		    }
		} catch (Exception e) {
		    // If Nimbus is not available, you can set the GUI to another look and feel.
			
			// Get the native look and feel class name
			String nativeLF = UIManager.getSystemLookAndFeelClassName();

			// Install the look and feel
			try {
			    UIManager.setLookAndFeel(nativeLF);
			} catch (InstantiationException iE) {
			} catch (ClassNotFoundException cNFE) {
			} catch (UnsupportedLookAndFeelException uLAFE) {
			} catch (IllegalAccessException iAE) {
			}
		}
		
		
		new EndUseWindow();
	}	
	
	EndUseWindow(){		
		dbConn = mySQLConnection.retrieveDB(); //returns when valid dbConn has been selected
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI(); 
			}
		});
	}


	private void buildGUI() {
		this.setLocation(150,150);
		this.setPreferredSize(new Dimension(800,600));
		this.setVisible(false);

		final JTabbedPane mainPane = new JTabbedPane();

		if (dbConn!=null){
			SiteManagementPanel managementPanel = new SiteManagementPanel(dbConn);
			SourceManagementPanel sourceManagementPanel = new SourceManagementPanel(dbConn);
			DataPlotPanel dataPlotPanel = new DataPlotPanel(dbConn);
			MissingPlotPanel missingPlotPanel = new MissingPlotPanel(dbConn);
			IssuePanel errorPanel = new IssuePanel(dbConn);
			
			//TODO Panels To Add
			//SitesPanel sitesPanel = new management.SitesPanel(dbConn);
			//SourcesPanel sourcesPanel = new management.SourcesPanel(dbConn);
			//FilesPanel filesPanel = new management.FilesPanel(dbConn);
			//BatchFileSNDatesPanel batchFileSNDatesPanel = new other.BatchFileSNDatesPanel(dbConn);
			//PDCImportPanel pdcImportPanel = new management.PDCImportPanel(dbConn);
			//MetersPanel metersPanel = new management.MetersPanel(dbConn);
		
			
			this.add(menuBar, BorderLayout.PAGE_START);
			menuBar.add(fileMenu);
			menuBar.add(outputMenu);
			
			fileMenu.add(importMenu);
			importMenu.add(importBatchFile);
			importMenu.add(standAloneImport);
			importMenu.add(pdcImport);
			importMenu.add(ctImport);
			importBatchFile.addActionListener(this);
			standAloneImport.addActionListener(this);
			pdcImport.addActionListener(this);
			ctImport.addActionListener(this);
			
			fileMenu.add(exportMenu);
			exportMenu.add(exportBatchFile);
			exportBatchFile.addActionListener(this);
			
			fileMenu.add(processMenu);
			processMenu.add(processPDCFile);
			processPDCFile.addActionListener(this);
			
			outputMenu.add(refrigMenu);
			refrigMenu.add(refrigAnalysis);
			refrigAnalysis.addActionListener(this);
			
			outputMenu.add(standardOutputsMenu);
			standardOutputsMenu.add(rawDataExport);
			standardOutputsMenu.add(avgAnalysis);
			standardOutputsMenu.add(discreteModeAvg);
			standardOutputsMenu.add(TODAnalysis);
			rawDataExport.addActionListener(this);
			avgAnalysis.addActionListener(this);
			discreteModeAvg.addActionListener(this);
			TODAnalysis.addActionListener(this);
			
			outputMenu.add(toolsMenu);
			toolsMenu.add(createMissingSummary);
			toolsMenu.add(getDatesAndSerials);
			createMissingSummary.addActionListener(this);
			getDatesAndSerials.addActionListener(this);
			
			mainPane.add(managementPanel,"Site Management");
			mainPane.add(sourceManagementPanel,"Source Management");
			mainPane.add(dataPlotPanel,"Data Visualisation");
			mainPane.add(missingPlotPanel,"Missing Visualisation");
			mainPane.add(errorPanel,"Issue Monitoring");

			getContentPane().add(mainPane, BorderLayout.CENTER);
			setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			this.pack();
			//this.setExtendedState(this.getExtendedState() | Frame.MAXIMIZED_BOTH);

			this.setVisible(true);

			/*endUseWindow.setGlassPane(waitGlassPane);
							waitGlassPane.setVisible(true);
							System.out.println(waitGlassPane.getWidth()+" "+waitGlassPane.getHeight());
							//waitGlassPane.setBackground(Color.black);
							//waitGlassPane.setOpaque(true);
							waitGlassPane.setLayout(new BorderLayout());
							JLabel waitLabel = new JLabel("Please Wait...");
							waitLabel.setFont(new Font("Times New Roman",Font.PLAIN,24));
							waitGlassPane.add(waitLabel,BorderLayout.CENTER);*/


		}
		else{
			setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			this.dispose();
		}

	}

	@Override
	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(exportBatchFile)){
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setSelectedFile(new File("EndUseBatchFile.csv"));
			if (fileChooser.showSaveDialog(this)==JFileChooser.APPROVE_OPTION){
				Thread exporterThread = new Thread(new fileManagement.StandAloneBatchExporter(fileChooser.getSelectedFile(),dbConn,new LogWindow("Export Batch File Log")));
				exporterThread.start();
			}
		}
		else if (aE.getSource().equals(importBatchFile)){
			JFileChooser fileChooser = new JFileChooser();
			File lastDir = new File(fileSettings.get("LastBatchOpen", fileChooser.getCurrentDirectory().getAbsolutePath()));
			if (lastDir.isDirectory()){fileChooser.setCurrentDirectory(lastDir);}
			if (fileChooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){
				fileSettings.put("LastBatchOpen", fileChooser.getSelectedFile().getParent());
				Thread processBatch = new Thread(new StandAloneBatchImporter(mySQLConnection,new LogWindow("Data Import Log"),fileChooser.getSelectedFile()));
				processBatch.start();
			}
		}
		else if (aE.getSource().equals(ctImport)){
			JFileChooser fileChooser = new JFileChooser();
			File lastDir = new File(fileSettings.get("LastCTOpen", fileChooser.getCurrentDirectory().getAbsolutePath()));
			if (lastDir.isDirectory()){fileChooser.setCurrentDirectory(lastDir);}
			if (fileChooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){
				fileSettings.put("LastCTOpen", fileChooser.getSelectedFile().getParent());
				Thread processBatch = new Thread(new CTValidator(mySQLConnection,new LogWindow("CT Data Import Log"),fileChooser.getSelectedFile()));
				processBatch.start();
			}
		}
		else if (aE.getSource().equals(processPDCFile)){
			JFrame newFrame = new PDCDecodeWindow();
			newFrame.setLocation(200, 200);
			newFrame.setPreferredSize(new Dimension(600,400));
			newFrame.pack();
			newFrame.setVisible(true);
		}
		else if (aE.getSource().equals(refrigAnalysis)){
			JFrame newFrame = new JFrame("Refrigerator Analysis");
			newFrame.getContentPane().add(new RefrigAnalysisPanel(dbConn));
			newFrame.setLocation(200, 200);
			newFrame.setPreferredSize(new Dimension(600,400));
			newFrame.pack();
			newFrame.setVisible(true);
		}
		else if (aE.getSource().equals(rawDataExport)){			
			JFrame newFrame = new JFrame("Export Raw data");
			newFrame.getContentPane().add(new RawDataExportPanel(dbConn));
			newFrame.setLocation(200, 200);
			newFrame.setPreferredSize(new Dimension(600,400));
			newFrame.pack();
			newFrame.setVisible(true);
		}
		else if (aE.getSource().equals(avgAnalysis)){			
			JFrame newFrame = new JFrame("Average Analysis");
			newFrame.getContentPane().add(new AverageAnalysisPanel(dbConn));
			newFrame.setLocation(200, 200);
			newFrame.setPreferredSize(new Dimension(600,400));
			newFrame.pack();
			newFrame.setVisible(true);
		}
		else if (aE.getSource().equals(discreteModeAvg)){
			JFrame newFrame = new JFrame("Discrete Mode Average Analysis");
			newFrame.getContentPane().add(new DiscreteModeAnalysisPanel(dbConn));
			newFrame.setLocation(200, 200);
			newFrame.setPreferredSize(new Dimension(600,400));
			newFrame.pack();
			newFrame.setVisible(true);
			
		}
		else if (aE.getSource().equals(TODAnalysis)){
			JFrame newFrame = new JFrame("Time Of Day Analysis");
			newFrame.getContentPane().add(new TimeOfDayAnalysisPanel(dbConn));
			newFrame.setLocation(200, 200);
			newFrame.setPreferredSize(new Dimension(600,400));
			newFrame.pack();
			newFrame.setVisible(true);
			
		}
		else if (aE.getSource().equals(standAloneImport)){
			JFrame newFrame = new JFrame("Standalone File Importer");
			newFrame.getContentPane().add(new StandAloneImportPanel(mySQLConnection));
			newFrame.setLocation(200, 200);
			newFrame.setPreferredSize(new Dimension(1000,600));
			newFrame.pack();
			newFrame.setVisible(true);
		}
		else if (aE.getSource().equals(pdcImport)){
			JFrame newFrame = new JFrame("PDC File Importer");
			newFrame.getContentPane().add(new PDCImportPanel(mySQLConnection));
			newFrame.setLocation(200, 200);
			newFrame.setPreferredSize(new Dimension(600,400));
			newFrame.pack();
			newFrame.setVisible(true);
		}
		else if (aE.getSource().equals(createMissingSummary)){			
			JFrame newFrame = new JFrame("Missing Summary Analysis");
			newFrame.getContentPane().add(new MissingSummaryPanel(dbConn));
			newFrame.setLocation(200, 200);
			newFrame.setPreferredSize(new Dimension(600,400));
			newFrame.pack();
			newFrame.setVisible(true);
			
		}
		else if (aE.getSource().equals(getDatesAndSerials)){
			JFrame newFrame = new JFrame("Fetch Dates and Meter Serials");
			newFrame.getContentPane().add(new BatchFileSNDatesPanel());
			newFrame.setLocation(200, 200);
			newFrame.setPreferredSize(new Dimension(600,400));
			newFrame.pack();
			newFrame.setVisible(true);
		}
	}

}
