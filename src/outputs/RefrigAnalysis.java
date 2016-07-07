package outputs;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import endUseWindow.LogWindow;
import endUseWindow.Source;


public class RefrigAnalysis implements Runnable{
	
	private ArrayList<Long> datesArray;
	private ArrayList<Double> valuesArray;
	private ArrayList<Double> tempsArray;
	private final int basePowerOffset;
	private final double pwrCorr;
	private final double blackoutOffCount;
	private final Connection dbConn;
	private final LogWindow logWindow;
	private final Source source;
	private final Source tempSource;
	private final long startDate;
	private final Long endDate;
	private final Double threshold1;
	private final Double threshold2;
	private final SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private final SimpleDateFormat csvDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	
	
	RefrigAnalysis(Connection dbConn,LogWindow logWindow,Source source,Source tempSource,Long startDate,Long endDate,Double threshold1,Double threshold2,int basePowerOffset,Double pwrCorr, Double blackoutOffCount){
		sqlDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		csvDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
		this.dbConn = dbConn;
		this.logWindow = logWindow;
		this.source = source;
		this.tempSource = tempSource;
		this.startDate = startDate;
		this.endDate = endDate;
		this.threshold1 = threshold1;
		this.threshold2 = threshold2;
		this.basePowerOffset = basePowerOffset;
		this.pwrCorr = pwrCorr;
		this.blackoutOffCount = blackoutOffCount;
	}
	
	public void run(){
		Preferences fileSettings = Preferences.userRoot().node("EndUseFileSettings");
		JFileChooser fileChooser = new JFileChooser();
		File lastDir = new File(fileSettings.get("LastRefrigAnalysisSave", fileChooser.getCurrentDirectory().getAbsolutePath()));
		String fileName = "./Refrig_"+"Site"+source.getSite().getSiteName()+"_"+source.getSourceName()+ "_"+csvDateFormatter.format(startDate)+"_"+csvDateFormatter.format(endDate)+".csv";
		fileChooser.setCurrentDirectory(lastDir);
		fileChooser.setSelectedFile(new File(fileName));
		int fChooserOption = fileChooser.showSaveDialog(fileChooser);
		while (fileChooser.getSelectedFile().exists()==true && fChooserOption==JFileChooser.APPROVE_OPTION && JOptionPane.showConfirmDialog(logWindow, "Selected file already exists, ok to overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)==JOptionPane.NO_OPTION){
			JOptionPane.showMessageDialog(null,"Could not write to selected file.\r\nPlease select another filename.","Write Error",JOptionPane.ERROR_MESSAGE);
			fChooserOption = fileChooser.showSaveDialog(fileChooser);
		}
		if (fChooserOption==JFileChooser.APPROVE_OPTION){
			fileSettings.put("LastRefrigAnalysisSave", fileChooser.getSelectedFile().getParent());
			File outFile = fileChooser.getSelectedFile();
		
			try {
				Statement MySQL_Statement = dbConn.createStatement();
				//String getFreqSQL = "SELECT DISTINCT frequency AS frequency FROM files WHERE site_id = "+source.getSite().getSiteID()+" AND source_id = "+sourceID+" AND (start_date <= '"+sqlDateFormatter.format(endDate)+"' AND end_date >= '"+sqlDateFormatter.format(startDate)+"')";
				String getFreqSQL = "SELECT file_id,UNIX_TIMESTAMP(start_date) AS start_date,UNIX_TIMESTAMP(end_date) AS end_date,frequency FROM files WHERE site_id = "+source.getSite().getSiteID()+" AND source_id = "+source.getSourceID()+" AND end_date >= '"+sqlDateFormatter.format(startDate)+"' AND start_date <= '"+sqlDateFormatter.format(endDate)+"' ORDER BY start_date ASC";
				ResultSet frequencyResults = MySQL_Statement.executeQuery(getFreqSQL);
					
				//if (frequencyResults.next()){
					/*int file_count=0;
					LinkedList<long[]> fileStartAndEndDates = new LinkedList<long[]>();
					LinkedList<Integer> frequencies = new LinkedList<Integer>();
					fileStartAndEndDates.add(file_count,new long[] {startDate+60000,endDate});
					frequencies.add(file_count,frequencyResults.getInt("frequency"));
					long previousDate = startDate;
					int previousFreq = frequencyResults.getInt("frequency");
					while(frequencyResults.next()){
						long newDate = frequencyResults.getLong("start_date")*1000;
						fileStartAndEndDates.set(file_count, new long[] {previousDate,newDate-(1000*frequencyResults.getInt("frequency"))});
						frequencies.set(file_count,previousFreq);
						previousFreq = frequencyResults.getInt("frequency");
						previousDate = newDate;
						
						//move to new file
						file_count++;
						fileStartAndEndDates.add(file_count,new long[] {previousDate,endDate});
						frequencies.add(file_count,previousFreq);
					}*/
	
				LinkedList<Long> fileIDs = new LinkedList<Long>();
				LinkedList<long[]> fileStartAndEndDates = new LinkedList<long[]>();
				LinkedList<Integer> frequencies = new LinkedList<Integer>();
				while(frequencyResults.next()){
					fileIDs.add(frequencyResults.getLong("file_id"));
					fileStartAndEndDates.add(new long[] {Math.max(frequencyResults.getLong("start_date")*1000,startDate),Math.min(frequencyResults.getLong("end_date")*1000,endDate)});
					frequencies.add(frequencyResults.getInt("frequency"));
				}
				frequencyResults.close();
				MySQL_Statement.close();
					
				if (fileStartAndEndDates.size()>0){
					//build frequency string
					String freqString = "";
					HashSet<Integer> freqSet = new HashSet<Integer>();
					freqSet.addAll(frequencies);
					LinkedList<Integer> freqList = new LinkedList<Integer>();
					freqList.addAll(freqSet);
					for (int i=0;i<freqList.size();i++){
						freqString += freqList.get(i)+(i<freqList.size()-1?";":"");
					}
					//write headers to file
					BufferedWriter outputStream = null;
					try{
						outputStream = new BufferedWriter(new FileWriter(outFile));
						logWindow.println("Writing to file: "+outFile.getName());
						outputStream.write("Date Processed: "+sqlDateFormatter.format(GregorianCalendar.getInstance().getTime())+"\r\n");
						outputStream.write("Site: "+source.getSite().getSiteName()+"(id:"+source.getSite().getSiteID()+")\r\n");
						outputStream.write("Source: "+source.getSourceName()+"(id:"+source.getSourceID()+"), "+tempSource.getSourceName()+"(id:"+tempSource.getSourceID()+")\r\n");
						outputStream.write("Start Date: "+sqlDateFormatter.format(startDate)+"\r\n");
						outputStream.write("End Date: "+sqlDateFormatter.format(endDate)+"\r\n");
						outputStream.write((freqList.size()>1?"Frequencies: ":"Frequency: ")+freqString+" seconds\r\n");
						outputStream.write("Threshold 1: "+threshold1+"\r\n");
						outputStream.write("Threshold 2: "+threshold2+"\r\n");
						outputStream.write("Base Power Offset: "+basePowerOffset+"\r\n");
						outputStream.write("Power Correction: "+pwrCorr+", Blackout Off: "+blackoutOffCount+"\r\n");
						outputStream.write("Start Date,Start Time,Average Power (W),Av ON Power (W),Av OFF Power (W),Base Power,Trigger Threshold,Cycle Length (mins),% t ON,% t OFF,Energy,Frequency"+(tempSource==null?"":",Temperature")+"\r\n");
	
	
						for (int i=0;i<fileStartAndEndDates.size();i++){
							int frequency = frequencies.get(i);
							makeDataArrays(fileIDs.get(i),fileStartAndEndDates.get(i)[0],fileStartAndEndDates.get(i)[1],frequency);
							if (datesArray.isEmpty()==false && valuesArray.isEmpty()==false && datesArray.size()==valuesArray.size()){
								ArrayList<int[]> crossoverArray = getCrossoverPoints(threshold1,threshold2,frequency);
								if (crossoverArray.isEmpty()==false){
									double[] boundedAverages = getBoundedAverages(crossoverArray);
									if (i>0){
										long gapSize = fileStartAndEndDates.get(i)[0] - fileStartAndEndDates.get(i-1)[1];
										if (gapSize > frequency * 1000){
											String[] date_time = sqlDateFormatter.format(fileStartAndEndDates.get(i-1)[1]+60000).split(" ");
											outputStream.write(date_time[0]+","+date_time[1]+",0,0,0,null,EOF,"+(gapSize/60000)+",null,null"+(tempSource==null?"":",null")+"\r\n");
										}
									}
									writeToFile(outputStream,crossoverArray,boundedAverages,frequency);
								}
								else{
									logWindow.println("Error 001: No power shifts detected. Please try another theshold value.");
								}
							}
							else{
								logWindow.println("Error 002: No data found for source "+source.getSourceID()+" at site "+source.getSite().getSiteID()+".\r\n");
							}
						}
					} catch (FileNotFoundException fNFE){
						logWindow.println("Error 006:Could not write to output file "+outFile.getName()+"\r\nPlease check that you have permission to write to this location.");
					} catch (IOException iOE){
						iOE.printStackTrace();
					} finally{
						if (outputStream!=null){
							try{
								outputStream.close();
							}
							catch(IOException iO){
								//do nothing
							}
						}
						logWindow.println("Finished Writing.");
					}
				}
				else{
					logWindow.println("Error 003: No files found for source "+source.getSourceID()+" at site "+source.getSite().getSiteID()+" in the date range specified.\r\n");
				}
				//}
				//else{
				//	logWindow.println("Error 004: Data frequency could not be determined for source "+source.getSourceID()+" at site "+source.getSite().getSiteID()+".\r\n");
				//}
			} catch (SQLException e) {
				logWindow.println("Error 005: Data frequency could not be determined for source "+source.getSourceID()+" at site "+source.getSite().getSiteID()+".\r\n");
				e.printStackTrace();
			}
		}
	}
	
	void makeDataArrays(long fileID, long fileStartDate, long fileEndDate, int frequency){
		datesArray = new ArrayList<Long>();
		valuesArray = new ArrayList<Double>();
		tempsArray = new ArrayList<Double>();
		logWindow.println("Collecting data for source "+source.getSourceName()+" between '"+sqlDateFormatter.format(fileStartDate)+"' and '"+sqlDateFormatter.format(fileEndDate)+"'.");
		Statement MySQL_Statement;
		try {
			MySQL_Statement = dbConn.createStatement();
			if (tempSource==null){ //if no temperatures required
				String getDataSQL = "SELECT UNIX_TIMESTAMP(date_time) AS unix_ts,value FROM data_sa WHERE site_id = "+source.getSite().getSiteID()+" AND source_id = "+source.getSourceID()+" AND date_time BETWEEN '"+(sqlDateFormatter.format(fileStartDate+(frequency*1000)))+"' AND '"+(sqlDateFormatter.format(fileEndDate+(frequency*1000)))+"'";
				ResultSet dataResultSet = MySQL_Statement.executeQuery(getDataSQL);
				while (dataResultSet.next()){
					datesArray.add(dataResultSet.getLong("unix_ts")*1000);
					valuesArray.add(dataResultSet.getDouble("value"));
				}
				dataResultSet.close();
			}
			else{ //if need temperatures
				String getDataSQL = "CALL getRefrigAnalysisData("+source.getSite().getSiteID()+","+source.getSourceID()+","+fileID+","+tempSource.getSourceID()+",'"+sqlDateFormatter.format(fileStartDate)+"','"+sqlDateFormatter.format(fileEndDate)+"')";
				ResultSet dataResultSet = MySQL_Statement.executeQuery(getDataSQL);
				
				while (dataResultSet.next()){
					datesArray.add(dataResultSet.getLong("unix_ts")*1000);
					valuesArray.add(dataResultSet.getDouble("value"));
					tempsArray.add(dataResultSet.getDouble("temp"));
					if (dataResultSet.wasNull()){tempsArray.set(tempsArray.size()-1, null);}
				}
				dataResultSet.close();
			}
			MySQL_Statement.close();
		} catch (SQLException e) {
			datesArray.clear();
			valuesArray.clear();
			logWindow.println("ERROR: data could not be retrived from the database. Analysis will be aborted...");
			e.printStackTrace();
		} //catch (Exception e){
			//e.printStackTrace();
		//}
	}
	
	//ArrayList<int[]> getCrossoverPoints(Double threshold1,Double threshold2){
	//	return getCrossoverPoints(valuesArray, threshold1, threshold2);
	//}
	
	ArrayList<int[]> getCrossoverPoints(Double threshold1,Double threshold2, int frequency){ //Get points where graph crosses a threshold (1 or 2)
		ArrayList<int[]> crossoverArray = new ArrayList<int[]>();
		Double prevPower = valuesArray.get(0);
		long prevDate = datesArray.get(0);
		crossoverArray.add(new int[] {0,0});
		for (int i=1;i<valuesArray.size();i++){
			Double currPower = valuesArray.get(i);
			Double nextPower = 0.0;
			long currDate = datesArray.get(i);

			if (currDate == prevDate + frequency*1000){
				if (i<valuesArray.size()-1){
					nextPower = valuesArray.get(i+1);

					if (threshold1!=threshold2 && threshold2!=0){
						if (prevPower<threshold1 && (currPower>=threshold1 && currPower<threshold2)){ // standby to compressor
							//if (nextPower>=threshold1 && nextPower<threshold2){ //ensures that no intermediate points are caught
							if (nextPower<threshold2){ //ensures that no intermediate points are caught
								if (crossoverArray.get(crossoverArray.size()-1)[1] != 1){
									crossoverArray.add(new int[] {i,1});
								}
								else{
									System.out.println("11 " + sqlDateFormatter.format(datesArray.get(i)) + " " + crossoverArray.get(crossoverArray.size()-1)[1]);
								}
							}
						}
						if (prevPower>=threshold2 && (currPower>=threshold1 && currPower<threshold2)){ //defrost to compressor
							if (nextPower>=threshold1 && nextPower<threshold2){ //ensures that no intermediate points are caught
								if (crossoverArray.get(crossoverArray.size()-1)[1] != 1){
									crossoverArray.add(new int[] {i,1});
								}
								else{
									System.out.println("12 " + sqlDateFormatter.format(datesArray.get(i)) + " " + crossoverArray.get(crossoverArray.size()-1)[1]);
								}
							}
						}
						if (prevPower<threshold1 && currPower>=threshold2){ //standby to defrost
							if (nextPower>=threshold2){ //ensures that no false defrosts are caught
								if (crossoverArray.get(crossoverArray.size()-1)[1] != 3){
									crossoverArray.add(new int[] {i,3});
								}
								else{
									System.out.println("31 " + sqlDateFormatter.format(datesArray.get(i)) + " " + crossoverArray.get(crossoverArray.size()-1)[1]);
								}
							}
						}
						if ((prevPower>=threshold1 && prevPower<threshold2) && currPower>=threshold2){ //compressor to defrost
							if (nextPower>=threshold2){ //ensures that no false defrosts are caught
								if (crossoverArray.get(crossoverArray.size()-1)[1] != 3){
									crossoverArray.add(new int[] {i,3});
								}
								else{
									System.out.println("32 " + sqlDateFormatter.format(datesArray.get(i)) + " " + crossoverArray.get(crossoverArray.size()-1)[1]);
								}
							}
						}
						if ((prevPower>=threshold1 && prevPower<threshold2) && currPower<threshold1){ //compressor to off
							if (crossoverArray.get(crossoverArray.size()-1)[1] != 5){
								crossoverArray.add(new int[] {i,5});
							}
							else{
								System.out.println("51 " + sqlDateFormatter.format(datesArray.get(i)) + " " + crossoverArray.get(crossoverArray.size()-1)[1]);
							}
						}
						if (prevPower>threshold2 && currPower<threshold1){ //defrost to off
							if (crossoverArray.get(crossoverArray.size()-1)[1] != 5){
								crossoverArray.add(new int[] {i,5});
							}
							else{
								System.out.println("52 " + sqlDateFormatter.format(datesArray.get(i)) + " " + crossoverArray.get(crossoverArray.size()-1)[1]);
							}
						}
					}
					else if (threshold1==threshold2 || threshold2==0){
						if (prevPower<threshold1 && currPower>=threshold1){
							if (nextPower>=threshold1){ //ensures no false compressor starts are caught
								crossoverArray.add(new int[] {i,1});
							}
						}
					}
				}
			}
			else{
				if (crossoverArray.get(crossoverArray.size()-1)[1]==5 && crossoverArray.get(crossoverArray.size()-2)[1]!=5){
					crossoverArray.set(crossoverArray.size()-2, new int[] {crossoverArray.get(crossoverArray.size()-2)[0],-1});
				}
				else{
					crossoverArray.set(crossoverArray.size()-1, new int[] {crossoverArray.get(crossoverArray.size()-1)[0],-1});
				}
				crossoverArray.add(new int[] {i,0});
			}
			prevPower = currPower;
			prevDate = currDate;
		}
		return crossoverArray;
	}
	
	double[] getBoundedAverages(ArrayList<int[]> crossoverArray){
		double[] boundedAverages = new double[crossoverArray.size()];
		int periodStartRow;
		int nextPeriodStartRow;
		double median;
		double total = 0;
		double count = 0;
		
		for (int i=0;i<crossoverArray.size()-1;i++){
			total = 0;
			count = 0;
			periodStartRow = crossoverArray.get(i)[0];
			nextPeriodStartRow = crossoverArray.get(i+1)[0]; 
			
			if (crossoverArray.get(i)[1]!=5){ //ON Periods
				median = getMedian(valuesArray.subList(periodStartRow, nextPeriodStartRow),true);
				for (int j=periodStartRow;j<nextPeriodStartRow;j++){
					if (valuesArray.get(j)>=median-(median*0.2)){
						total += valuesArray.get(j);
						count++;
					}
				}
			}
			else{ //OFF periods
				median = getMedian(valuesArray.subList(periodStartRow, nextPeriodStartRow),false);
				for (int j=periodStartRow;j<nextPeriodStartRow;j++){
					if (valuesArray.get(j)<=median+(median*1.0)){
						total += valuesArray.get(j);
						count++;
					}
				}
			}
			
			boundedAverages[i] = total/count;
		}
		
		return boundedAverages;
	}
	
	
	
	
	
	
	void writeToFile(BufferedWriter outputStream,ArrayList<int[]> crossoverArray, double[] boundedAverages,int frequency) throws IOException{
		int periodStartRow;
		int offPeriodStartRow;
		int nextPeriodStartRow;
		Double periodTotal = 0.0;
		Double onTotal;
		Double onCount;
		Double offTotal;
		Double offCount;
		Double offset = 0.0;
		Double timeOffset;

		for (int i=1;i<crossoverArray.size();i++){
			if (crossoverArray.get(i-1)[1]!=5){ //do not process OFF periods
				periodTotal = 0.0;
				onTotal = 0.0;
				onCount = 0.0;
				offTotal = 0.0;
				offCount = 0.0;
				timeOffset = offset;

				periodStartRow = crossoverArray.get(i-1)[0];
				offPeriodStartRow = crossoverArray.get(i)[0]; //if the next period is not an OFF period, this represents the beginning of the next ON period (eg. defrost to compressor OR compressor to defrost)
				nextPeriodStartRow = crossoverArray.get(i)[0]; //set to the start of the next period until we have checked that it is an OFF period and that another block exists after that
				
				if (crossoverArray.get(i)[1]==5){ //if the next period is an OFF period, alter nextPeriodStartRow, otherwise leave it the same
					if (i<crossoverArray.size()-1){
						nextPeriodStartRow = crossoverArray.get(i+1)[0]; //if next OFF period is not the final period, set nextPeriodStartRow to the start of the next ON period
					}
					else{
						nextPeriodStartRow = valuesArray.size()-1; //next period is OFF and final period so set nextPeriodStartRow to be the end of the data set
					}
				}

				for (int j=periodStartRow;j<offPeriodStartRow;j++){
					periodTotal += valuesArray.get(j);
					if (j!=periodStartRow && j!=offPeriodStartRow-1){ //exclude start and end points
						onTotal += valuesArray.get(j);
						onCount++;
					}
				}

				for (int j=offPeriodStartRow;j<nextPeriodStartRow;j++){ //will not run if no OFF period
					periodTotal += valuesArray.get(j);
					if (j!=offPeriodStartRow && j!=nextPeriodStartRow-1){ //exclude start and end points
						offTotal += valuesArray.get(j);
						offCount++;
					}
				}

				if (offset >  0){
					onCount = onCount + 1 - offset;
					onTotal = onTotal + (1 - offset) * valuesArray.get(periodStartRow+1);
				}
				else { //offset < 0 OR offset == 0
					onCount = onCount + 1 - offset;
					onTotal = onTotal + (1 - offset) * valuesArray.get(periodStartRow);
				}
				
				if (offPeriodStartRow!=nextPeriodStartRow){ //if there is an off period
					//if (offPeriodStartRow != nextPeriodStartRow-1){
						double endOnPropTest = 1-(((valuesArray.get(offPeriodStartRow-1)-boundedAverages[i])/(boundedAverages[i-1]-boundedAverages[i])));
						double startOffPropTest = ((valuesArray.get(offPeriodStartRow)-boundedAverages[i])/(boundedAverages[i-1]-boundedAverages[i]));
	
						if (endOnPropTest<0){endOnPropTest = 0.0;} //important for splitting equally
						if (startOffPropTest<0){startOffPropTest = 0.0;} //important for splitting equally
						if (endOnPropTest>startOffPropTest){
							offset = -(1-(((valuesArray.get(offPeriodStartRow-1)-valuesArray.get(offPeriodStartRow))/(valuesArray.get(offPeriodStartRow-2)-valuesArray.get(offPeriodStartRow)))));
							if (offset>0){offset = 0.0;}
							if (startOffPropTest>0.1){
								System.out.println("PROBLEM at 1: "+endOnPropTest+" "+startOffPropTest);
							}
						}
						else if (startOffPropTest>endOnPropTest){
							offset = ((valuesArray.get(offPeriodStartRow)-valuesArray.get(offPeriodStartRow+1))/(valuesArray.get(offPeriodStartRow-1)-valuesArray.get(offPeriodStartRow+1)));
							if (offset<0){offset = 0.0;}
							if (endOnPropTest>0.1){
								System.out.println("PROBLEM at 2: "+endOnPropTest+" "+startOffPropTest);
							}
						}
						else{ //must be equal, assume that this means both are 1 
							offset = 0.0;
						}
					//}
					//else{ //only one off point
					//	offset = 0.0;
					//}

					if (offset >  0){
						onCount = onCount + 1 + offset;
						onTotal = onTotal + (1 + offset) * valuesArray.get(offPeriodStartRow-1);
						offCount = offCount + 1 - offset;
						offTotal = offTotal + (1 - offset) * valuesArray.get(offPeriodStartRow+1);
					}
					else if (offset <0){
						onCount = onCount + 1 + offset;
						onTotal = onTotal + (1 + offset) * valuesArray.get(offPeriodStartRow-2);
						offCount = offCount + (1 - offset);
						offTotal = offTotal + (1 - offset) * valuesArray.get(offPeriodStartRow);
					}
					else{
						onCount = onCount + 1;
						offCount = offCount + 1;
						onTotal = onTotal + valuesArray.get(offPeriodStartRow-1);
						offTotal = offTotal + valuesArray.get(offPeriodStartRow);
					}
				}

				//System.out.println(sqlDateFormatter.format(datesArray.get(nextPeriodStartRow)));
				if (crossoverArray.get(i)[1]==0){
					offset = 0.0;
				}
				else if (crossoverArray.get(i)[1]==5){ //if there is an OFF period
					double endCurrPropTest = 0;
					double startNextPropTest = 0;
					if (i<crossoverArray.size()-1 && crossoverArray.get(i+1)[1]!=0){
						endCurrPropTest = ((valuesArray.get(nextPeriodStartRow-1)-boundedAverages[i])/(boundedAverages[i+1]-boundedAverages[i]));
						startNextPropTest = 1-((valuesArray.get(nextPeriodStartRow)-boundedAverages[i])/(boundedAverages[i+1]-boundedAverages[i]));

						//If endCurrProp or startNextProp are negative, means that they are outside the range between offAverage and onAverage, so set to zero
						if (endCurrPropTest<0){endCurrPropTest = 0.0;} //important for splitting equally
						if (startNextPropTest<0){startNextPropTest = 0.0;} //important for splitting equally
						if (endCurrPropTest>startNextPropTest){
							offset = -((valuesArray.get(nextPeriodStartRow-1)-valuesArray.get(nextPeriodStartRow-2))/(valuesArray.get(nextPeriodStartRow)-valuesArray.get(nextPeriodStartRow-2)));
							if (offset>0){offset = 0.0;}
							if (startNextPropTest>0.1){
								System.out.println("PROBLEM at 7: "+endCurrPropTest+" "+startNextPropTest);
							}
						}
						else if (startNextPropTest>endCurrPropTest){
							offset = 1-((valuesArray.get(nextPeriodStartRow)-valuesArray.get(nextPeriodStartRow-1))/(valuesArray.get(nextPeriodStartRow+1)-valuesArray.get(nextPeriodStartRow-1)));
							if (offset<0){offset = 0.0;}
							if (endCurrPropTest>0.1){
								System.out.println("PROBLEM at 8: "+endCurrPropTest+" "+startNextPropTest);
							}
						}
						else{ //must be equal, assume that this means both are 1 
							offset = 0.0;
						}
					}
					else{ //if final period
						offset = 0.0;
					}
					
					//NOT SURE WHY I NEED THIS YET BUT I DO
					if (offPeriodStartRow == nextPeriodStartRow-1){ //only one off point
						offset = offset - 1;
					}

					if (offset < 0){
						offCount = offCount + 1 + offset;
						offTotal = offTotal + (1 + offset) * valuesArray.get(nextPeriodStartRow-2);
					}
					else { //offset > 0 OR offset == 0
						offCount = offCount + 1 + offset;
						offTotal = offTotal + (1 + offset) * valuesArray.get(nextPeriodStartRow-1);
					}
					
					//NOT SURE WHY I NEED THIS YET BUT I DO
					if (offPeriodStartRow == nextPeriodStartRow-1){ //only one off point
						offset = offset + 1;
					}
				}
				else if (crossoverArray.get(i)[1]!=5){ //if no OFF period
					double endCurrPropTest = 0;
					double startNextPropTest = 0;
					if (i<crossoverArray.size()-1){ //if not final period
						if (boundedAverages[i-1] > boundedAverages[i]){//if dropping down
							endCurrPropTest = 1-((valuesArray.get(nextPeriodStartRow-1)-boundedAverages[i])/(boundedAverages[i-1]-boundedAverages[i]));
							startNextPropTest = ((valuesArray.get(nextPeriodStartRow)-boundedAverages[i])/(boundedAverages[i-1]-boundedAverages[i]));

							//If endCurrProp or startNextProp are negative, means that they are outside the range between offAverage and onAverage, so set to zero
							if (endCurrPropTest<0){endCurrPropTest = 0.0;} //important for splitting equally
							if (startNextPropTest<0){startNextPropTest = 0.0;} //important for splitting equally
							if (endCurrPropTest>startNextPropTest){
								offset = -(1-((valuesArray.get(nextPeriodStartRow-1)-valuesArray.get(nextPeriodStartRow))/(valuesArray.get(nextPeriodStartRow-2)-valuesArray.get(nextPeriodStartRow))));
								if (offset>0){offset = 0.0;}
								if (startNextPropTest>0.1){
									System.out.println("PROBLEM at 3: "+endCurrPropTest+" "+startNextPropTest);
								}
							}
							else if (startNextPropTest>endCurrPropTest){
								offset = ((valuesArray.get(nextPeriodStartRow)-valuesArray.get(nextPeriodStartRow+1))/(valuesArray.get(nextPeriodStartRow-1)-valuesArray.get(nextPeriodStartRow+1)));
								if (offset<0){offset = 0.0;}
								if (endCurrPropTest>0.1){
									System.out.println("PROBLEM at 4: "+endCurrPropTest+" "+startNextPropTest);
								}
							}
							else{ //must be equal, assume that this means both are 1 
								offset = 0.0;
							}
						}
						else if (boundedAverages[i-1] < boundedAverages[i]){//if stepping up
							endCurrPropTest = ((valuesArray.get(nextPeriodStartRow-1)-boundedAverages[i-1])/(boundedAverages[i]-boundedAverages[i-1]));
							startNextPropTest = 1-((valuesArray.get(nextPeriodStartRow)-boundedAverages[i-1])/(boundedAverages[i]-boundedAverages[i-1]));

							//If endCurrProp or startNextProp are negative, means that they are outside the range between offAverage and onAverage, so set to zero
							if (endCurrPropTest<0){endCurrPropTest = 0.0;} //important for splitting equally
							if (startNextPropTest<0){startNextPropTest = 0.0;} //important for splitting equally
							if (endCurrPropTest>startNextPropTest){
								offset = -((valuesArray.get(nextPeriodStartRow-1)-valuesArray.get(nextPeriodStartRow-2))/(valuesArray.get(nextPeriodStartRow)-valuesArray.get(nextPeriodStartRow-2)));
								if (offset>0){offset = 0.0;}
								if (startNextPropTest>0.1){
									System.out.println("PROBLEM at 5: "+endCurrPropTest+" "+startNextPropTest);
								}
							}
							else if (startNextPropTest>endCurrPropTest){
								offset = 1-((valuesArray.get(nextPeriodStartRow)-valuesArray.get(nextPeriodStartRow-1))/(valuesArray.get(nextPeriodStartRow+1)-valuesArray.get(nextPeriodStartRow-1)));
								if (offset<0){offset = 0.0;}
								if (endCurrPropTest>0.1){
									System.out.println("PROBLEM at 6: "+endCurrPropTest+" "+startNextPropTest);
								}
							}
							else{ //must be equal, assume that this means both are 1 
								offset = 0.0;
							}
						}
						else{
							//should never happen, but just in case
							offset = 0.0;
						}
					}
					else{ //if final period
						offset = 0.0;
					}

					if (offset <  0){
						onCount = onCount + 1 + offset;
						onTotal = onTotal + (1 + offset) * valuesArray.get(nextPeriodStartRow-2);
					}
					else { //offset > 0 OR offset == 0
						onCount = onCount + 1 + offset;
						onTotal = onTotal + (1 + offset) * valuesArray.get(nextPeriodStartRow-1);
					}
				}

				//diffInMins = (int)Math.round((datesArray.get(nextPeriodStartRow)-datesArray.get(periodStartRow))/(1000*60));
				//periodAverage = (periodTotal)/(nextPeriodStartRow-periodStartRow);
				Double actualPeriodAverage = (onTotal+offTotal)/(onCount+offCount);
				Double energy = (onTotal + offTotal)*(frequency/60)/60;
				Double avTemp = 0.0;
				int threshold = crossoverArray.get(i-1)[1];
				if (Math.abs((int)Math.round((datesArray.get(nextPeriodStartRow)-datesArray.get(periodStartRow))/(1000*60))-((onTotal+offTotal)*(frequency/60)))<(2*frequency/60) && crossoverArray.get(i-1)[1] != -3){
					System.out.println(Math.abs((int)Math.round((datesArray.get(nextPeriodStartRow)-datesArray.get(periodStartRow))/(1000*60))-((onTotal+offTotal)*(frequency/60))));
					System.out.println((int)Math.round((datesArray.get(nextPeriodStartRow)-datesArray.get(periodStartRow))/(1000*60))+","+((onTotal+offTotal)*(frequency/60)));
					threshold = -2;
				}
				else if (i==crossoverArray.size()-1 || (i==crossoverArray.size()-2 && crossoverArray.get(i)[1] == 5)){
					threshold = -1;
				}
				else if (i<crossoverArray.size()-1 && (offCount * (frequency/60)) > blackoutOffCount){
					//if more than blackoutOffCount mins in off
					threshold = 8;
					if (i<crossoverArray.size()-2 && crossoverArray.get(i)[1] != 5){
						crossoverArray.get(i)[1] = 9;
					}
					else if (i<crossoverArray.size()-3 && crossoverArray.get(i+1)[1] != 5){
						crossoverArray.get(i+1)[1] = 9;
					}
				}
				else if (crossoverArray.get(i)[1] == 3){
					if (threshold == 1){threshold = 2;}
				}
				else if (i<crossoverArray.size()-1 && crossoverArray.get(i)[1] == 5 && crossoverArray.get(i+1)[1] == 3){
					if (threshold == 1){threshold = 2;}
				}
				else if (i>1 && crossoverArray.get(i-2)[1] == 3){
					if (threshold == 1){threshold = 4;}
				}
				else if (i>2 && crossoverArray.get(i-2)[1] == 5 && crossoverArray.get(i-3)[1] == 3){
					if (threshold == 1){threshold = 4;}
				}
				
				if (tempSource!=null){avTemp = getAverage(tempsArray.subList(periodStartRow, nextPeriodStartRow-1));}
				String[] date_time = sqlDateFormatter.format(datesArray.get(periodStartRow)-(frequency*1000)+Math.round(frequency*1000*timeOffset)).split(" ");
				String newLine = date_time[0]+","+date_time[1]+","+(actualPeriodAverage-pwrCorr)+","+(onTotal/onCount)+","+(offCount!=0?(offTotal/offCount):"0")+","+(valuesArray.get(nextPeriodStartRow-(nextPeriodStartRow<basePowerOffset?0:basePowerOffset))-pwrCorr)+","+threshold+","+((onCount+offCount)*(frequency/60))+","+((onCount/(onCount+offCount))*100)+"%,"+((offCount/(onCount+offCount))*100)+"%,"+energy+","+(frequency/60)+(tempSource==null?"":","+avTemp);
				outputStream.write(newLine+"\r\n");
			}
		}
		outputStream.flush();
	}
	

	double getAverage(List<Double> temps){
		Double total = 0.0;
		int count = 0;
		for (int i=0;i<temps.size();i++){
			if (temps.get(i)!=null){
				total += temps.get(i);
				count += 1;
			}
		}
		return total/count;
	}
	
	public double getMedian(List<Double> points, boolean on){
		ArrayList<Double> relevantPoints = new ArrayList<Double>();
		if (points.size() > 0){
			if (on){
				for (int i=0;i<points.size();i++){
					if (points.get(i)>=threshold1){
						relevantPoints.add(points.get(i));
					}
				}
			}
			else{
				for (int i=0;i<points.size();i++){
					if (points.get(i)<threshold1){
						relevantPoints.add(points.get(i));
					}
				}
			}
			if (relevantPoints.size() > 0){ //if still points left
			    Collections.sort(relevantPoints);
			 
			    if (relevantPoints.size() % 2 == 1){
			    	return relevantPoints.get((relevantPoints.size()+1)/2-1);
			    }
			    else {
					double lower = relevantPoints.get(relevantPoints.size()/2-1);
					double upper = relevantPoints.get(relevantPoints.size()/2);
					return (lower + upper) / 2.0;
			    }
			}
			else{
				return 0.0;
			}
		}
		else{
			return 0.0;
		}
	}
	
	class crossoverPoint {
		int index = 0;
		String code = "";
		
		crossoverPoint(int index, String code){
			this.index = index;
			this.code = code;
		}
	}
	
	
}
