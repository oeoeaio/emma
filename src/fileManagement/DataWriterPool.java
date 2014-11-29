package fileManagement;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import endUseWindow.LogWindow;
import endUseWindow.MySQLConnection;

public class DataWriterPool extends JFrame implements Runnable{
	private static final long serialVersionUID = -592758520120066690L;
	
	private JPanel mainPanel = new JPanel(new BorderLayout());
	private JScrollPane mainScroll = new JScrollPane(mainPanel);
	private JPanel mainGridPanel = new JPanel(new GridLayout(0,2));
	
	private Queue<DataWriter> writerPool = new LinkedList<DataWriter>();
	private static final int fileQueueSize = 2;
	BlockingQueue<DataFile> fileList = new LinkedBlockingQueue<DataFile>(fileQueueSize);
	private static final int maxWriterPoolSize = 0; //number of writers desired minus one
	private boolean showGUI = false;
	boolean moreFilesComing = true;
	private LogWindow logWindow;
		
	//private long totalWaitTime = 0;
	private long totalCompWaitTime = 0;
	
	DataWriterPool(MySQLConnection mySQLConnection,LogWindow logWindow,boolean showGUI){
		this.showGUI = showGUI;
		this.logWindow = logWindow;
		for(int i=0;i<=maxWriterPoolSize;i++){
			writerPool.add(new DataWriter(mySQLConnection, logWindow, this, showGUI));
		}
		
		if (showGUI){
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					buildGUI();
				}
			});
		}
	}
	
	void buildGUI(){
		this.setTitle("Write Progress");
		this.setLayout(new BorderLayout());
		this.setSize(new Dimension(600,300));
		this.setLocation(300,300);
		
		mainScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		mainScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		mainPanel.add(mainGridPanel,BorderLayout.NORTH);
		
		JPanel newDetailsPanel = new JPanel(new GridLayout(1,3));
		newDetailsPanel.add(new JLabel("Site ID"));
		newDetailsPanel.add(new JLabel("Source ID"));
		newDetailsPanel.add(new JLabel("File Name"));
		mainGridPanel.add(newDetailsPanel);
		
		mainGridPanel.add(new JLabel("Progress"));
		
		//getContentPane().add(new JLabel("Write Progress:"));
		getContentPane().add(mainScroll,BorderLayout.CENTER);
		
		this.validate();
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	}
	
	public void run(){
		while (moreFilesComing || !fileList.isEmpty()){ //if more data coming or data still to process
			//System.out.println("lala1");
			//while(!fileList.isEmpty()){  //if data still to process
			//Date start = new Date();
			final DataFile dataFile;
			if ((dataFile = fileList.poll()) != null){ //something to actually write
				synchronized(fileList){
					fileList.notify();
				}
				DataWriter dataWriter = null;
				synchronized(writerPool){
					while ((dataWriter = writerPool.poll()) == null){
						try {
							writerPool.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							break;
						}
					}
				}
				Date startComponents = new Date();
				//totalWaitTime += startComponents.getTime()-start.getTime();

				dataWriter.setDataFile(dataFile);

				if (showGUI){
					final JProgressBar progressBar = new JProgressBar(0,dataFile.dataList.size());
					progressBar.setStringPainted(true);
					dataWriter.setProgressBar(progressBar);

					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							JPanel newDetailsPanel = new JPanel(new GridLayout(1,3));
							newDetailsPanel.add(new JLabel(dataFile.siteID));
							newDetailsPanel.add(new JLabel(dataFile.sourceID));
							newDetailsPanel.add(new JLabel((dataFile.fileName.length()>16?dataFile.fileName.substring(0, 13)+"...":dataFile.fileName)));
							mainGridPanel.add(newDetailsPanel);
							mainGridPanel.add(progressBar);
							mainGridPanel.revalidate();
							mainScroll.getVerticalScrollBar().setValue(mainScroll.getVerticalScrollBar().getMaximum());

						}
					});
					this.setVisible(true);
				}
				Date end = new Date();
				totalCompWaitTime += end.getTime()-startComponents.getTime();
				new Thread (dataWriter).start();
			}
			//}
			try{
				synchronized(fileList){
					while (moreFilesComing && fileList.isEmpty()){
						fileList.notify(); //notify adder just in case things got stuck
						fileList.wait(); //wait for a file to be added to file List.
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		
		synchronized(writerPool){ //determines whether all files have been written 
			while (writerPool.size() < maxWriterPoolSize + 1){ //at least one writer is still running, and we know all have already been sent off
				try {
					writerPool.wait(); //wait a writer returns
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		logWindow.println("Finished writing all files");
		System.out.println("Finished writing all files");
		//System.out.println("Total feeder time: "+getTimeString(totalFeederTime-totalGetWriterTime)+"\r\n");
		//System.out.println("Total get writer time: "+getTimeString(totalGetWriterTime)+"\r\n");
		//System.out.println("Total get time: "+getTimeString(totalGetTime)+"\r\n");
		//System.out.println("Total start time: "+getTimeString(totalStartTime)+"\r\n");
		//System.out.println("Total wait time: "+getTimeString(totalWaitTime)+"\r\n");
		System.out.println("Total components time: "+getTimeString(totalCompWaitTime)+"\r\n");
		long totalWriteTime = 0;
		long total1Time = 0;
		long total2Time = 0;
		long total3Time = 0;
		long total4Time = 0;
		long total5Time = 0;
		long totalEndTime = 0;
		for (int i=0;i<writerPool.size();i++){
			System.out.println("Writer: "+i);
			DataWriter lala = writerPool.poll();
			System.out.println("Write: "+getTimeString(lala.totalWriteTime));
			System.out.println("1: "+getTimeString(lala.total1Time));
			System.out.println("2: "+getTimeString(lala.total2Time));
			System.out.println("3: "+getTimeString(lala.total3Time));
			System.out.println("4: "+getTimeString(lala.total4Time));
			System.out.println("5: "+getTimeString(lala.total5Time));
			System.out.println("end: "+getTimeString(lala.totalEndTime));

			totalWriteTime += lala.totalWriteTime;
			total1Time += lala.total1Time;
			total2Time += lala.total2Time;
			total3Time += lala.total3Time;
			total4Time += lala.total4Time;
			total5Time += lala.total5Time;
			totalEndTime += lala.totalEndTime;
			writerPool.add(lala);
		}
		System.out.println("Total write time: "+getTimeString(totalWriteTime)+"\r\n");
		System.out.println("Total 1 time: "+getTimeString(total1Time)+"\r\n");
		System.out.println("Total 2 time: "+getTimeString(total2Time)+"\r\n");
		System.out.println("Total 3 time: "+getTimeString(total3Time)+"\r\n");
		System.out.println("Total 4 time: "+getTimeString(total4Time)+"\r\n");
		System.out.println("Total 5 time: "+getTimeString(total5Time)+"\r\n");
		System.out.println("Total End time: "+getTimeString(totalEndTime)+"\r\n");
	}
	
	public void addFile(DataFile dataFile){
		try{
			synchronized(fileList){ //only one thread should ever be here at once, because there is only one feeder thread
				while (!fileList.offer(dataFile)){
					fileList.wait(); //wait for space to become available
				}
				fileList.notify(); //this will notify run() if it is waiting on a file
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	void addWriter(DataWriter dataWriter){
		synchronized(writerPool){
			writerPool.add(dataWriter);
			if (writerPool.size()==1){
				writerPool.notify();
			}
			else if(writerPool.size()>1){
				System.out.println("THIS IS A WORRY?");
				writerPool.notifyAll();
			}
		}
	}
	
	//DataWriter getWriter(final DataFile dataFile){
	//	Date start = new Date();
	//	DataWriter dataWriter = null;
		
		
		
	//	return dataWriter;	
	//}
	
	/*public Runnable finish(){
		return new Runnable(){
			public void run(){
				synchronized(writerPool){ //determines whether all files have been written 
					int sentWritersBefore = sentWriters-1;
					while (sentWritersBefore < sentWriters){ //if be notifying writerPool, another writer is sent, means we can 
						sentWritersBefore = sentWriters;
						writerPool.notify();
						while (writerPool.size() < maxWriterPoolSize + 1){ //only wait while there is a writer running
							try {
								writerPool.wait();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
				//if we get to here, all files should have been written
		
				synchronized(dataWriterPool){ //will not enter this block until fileFeeder is waiting 
					dataWriterPool.notify();
				}

				
			}
		};
	}*/
	
	private String getTimeString(long timeInMillis){
		String timeString = "";
		long remainder = timeInMillis;
		int hours = (int)Math.floor(remainder/(1000*60*60));
		remainder = remainder - hours*(1000*60*60);
		int mins = (int)Math.floor(remainder/(1000*60));
		remainder = remainder - mins*(1000*60);
		double secs = (double)remainder/1000;
		if (hours>0){
			if (hours == 1){
				timeString = timeString.concat("1 hour");
			}
			else{
				timeString = timeString.concat(hours+" hours");
			}
		}
		if (hours>0 && mins>0){
			timeString = timeString.concat(", ");
		}
		if (mins>0){
			if (mins == 1){
				timeString = timeString.concat("1 minute");
			}
			else{
				timeString = timeString.concat(mins+" minutes");
			}
		}
		if (hours>0 || mins>0){
			timeString = timeString.concat(" and ");
		}
		timeString = timeString.concat(secs+" seconds");
		return timeString;
	}
}
