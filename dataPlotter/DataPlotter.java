package dataPlotter;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import fileManagement.DataPoint;

public class DataPlotter extends JPanel{
	private static final long serialVersionUID = -2897538432601450933L;
	
	
	ArrayList<DataPoint> dataPoints = new ArrayList<DataPoint>();
	double yMax = 0.0;
	double widthInMins = 0.0;
	int yMaxMag = 0;
	Double yMajUnit = 0.0;
	int yAxisMaxUnitCount = 8;
	DataGrabber dataGrabber = new DataGrabber();
	String siteID;
	String sourceID;
	long startDate;
	long endDate;
	
	public DataPlotter(){
		this.setBorder(BorderFactory.createLineBorder(Color.black,1));
		this.setBackground(Color.white);
		this.setLayout(new BorderLayout());
	}
	
	public void setData(Connection dbConn, String siteID, String sourceID, long startDate, long endDate){
		if (!siteID.equals(this.siteID) || !sourceID.equals(this.sourceID) || startDate!=this.startDate || endDate!=this.endDate){
			//only get new data if different period
			dataPoints = dataGrabber.getData(dbConn,siteID, sourceID, startDate, endDate);
			
			this.siteID = siteID;
			this.sourceID = sourceID;
			this.startDate = startDate;
			this.endDate = endDate;
		}

		yMax = Collections.max(dataPoints, new Comparator<DataPoint>(){
			@Override
			public int compare(DataPoint arg0, DataPoint arg1) {
				return arg0.getValue().compareTo(arg1.getValue());
			}

		}).getValue();
		widthInMins = (dataPoints.get(dataPoints.size()-1).getDateTime()-dataPoints.get(0).getDateTime())/60000;

		yMaxMag = (int)Math.ceil(Math.log10(yMax));
		double divisor = 40/Math.ceil(Math.ceil(yMax/(Math.pow(10,yMaxMag)/40))/yAxisMaxUnitCount);
		yMajUnit = Math.pow(10,yMaxMag)/divisor;
		
		this.repaint();
	}
	
	@Override
	public void paint(Graphics g){
		Graphics2D g2 = (Graphics2D)g;
		g2.setBackground(Color.white);
		int w = this.getWidth();
		int h = this.getHeight();
		int tBuf = 30;
		int bBuf = 40;
		int lBuf = 30;
		int rBuf = 30;
		
		g2.clearRect(0, 0, w, h);
		
		if (dataPoints.size()==0){
			FontMetrics fm = getFontMetrics(g.getFont());
			Rectangle2D fontSize = fm.getStringBounds("No data to plot.",g);
			g2.setPaint(Color.gray);
			g2.drawString("No data to plot.", Math.round((w-fontSize.getWidth())/2), Math.round((h-fontSize.getHeight())/2));
		}
		else{
			
			//g2.rotate(-Math.PI/2);
			//g2.drawString("Helloee!",-h/2-10,12);
			//g2.rotate(Math.PI/2);

			//determine y-axis unit width
			FontMetrics fm = getFontMetrics(g.getFont());
			//int maxYLabelWidth = 0;
			String[] yVals = new String[(int)(Math.ceil(yMax/yMajUnit)+1)];
			for(int i=0;i<=Math.ceil(yMax/yMajUnit);i++){
				yVals[i]= Double.toString((double)Math.round(i*yMajUnit*4)/4);
				Rectangle2D fontSize = fm.getStringBounds(yVals[i],g);
				if (fontSize.getWidth()+10>lBuf){lBuf = (int) Math.round(fontSize.getWidth()+10);}
			}
			
			//Calculate gaps size between x-axis markers
			int minimumGap = 10080;
			int[] allowableGaps = new int[] {5,15,30,60,180,360,720,1440,2880,5760,10080,20160,40320,80640,161280}; 
			SimpleDateFormat dateDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat timeDateFormatter = new SimpleDateFormat("HH:mm:ss");
			dateDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
			timeDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+10"));
			if (widthInMins<=2880){ //less than two days, just use times
				bBuf = (int) fm.getStringBounds("99:99",g).getHeight()+10;
				minimumGap = (int)(widthInMins/((w-(rBuf+lBuf))/(fm.getStringBounds("99:99",g).getWidth()+30)));
			}
			else{
				bBuf = (int) fm.getStringBounds("99:99",g).getHeight()*2+7;
				minimumGap = (int)(widthInMins/((w-(rBuf+lBuf))/(fm.getStringBounds("9999-99-99",g).getWidth()+30)));
			}
			int gapSize = 0;
			if (Arrays.binarySearch(allowableGaps,minimumGap)>=0){
				gapSize = allowableGaps[Arrays.binarySearch(allowableGaps,minimumGap)];
			}
			else{
				gapSize = allowableGaps[Math.min(allowableGaps.length-1,-(Arrays.binarySearch(allowableGaps,minimumGap)+1))];
			}
			if (gapSize>=1440){
				bBuf = (int) fm.getStringBounds("9999-99-99",g).getHeight()+10;
			}
			
			
			//draw y-axis units and markers
			for(int i=0;i<=Math.ceil(yMax/yMajUnit);i++){
				double Y = h - (bBuf + ((i*yMajUnit)/(Math.ceil(yMax/yMajUnit)*yMajUnit))*(h-(tBuf+bBuf)));
				g2.drawString(yVals[i],5,Math.round(Y+5));
				g2.draw(new Line2D.Double(new Point2D.Double(lBuf+5, Y),new Point2D.Double(lBuf-2, Y)));
			}
			
			
			//draw x-axis units and markers
			GregorianCalendar xAxisLabel = new GregorianCalendar();
			xAxisLabel.setTimeZone(TimeZone.getTimeZone("GMT+10"));
			xAxisLabel.setTimeInMillis(startDate);
			long startDateExclTime = (xAxisLabel.getTimeInMillis()/60000)-xAxisLabel.get(GregorianCalendar.HOUR_OF_DAY)*60-xAxisLabel.get(GregorianCalendar.MINUTE);
			while (((xAxisLabel.getTimeInMillis()/60000)-(startDateExclTime))%gapSize!=0){
				xAxisLabel.add(GregorianCalendar.MINUTE, 1);
			}
			while (xAxisLabel.getTimeInMillis()<=endDate){
				double X = lBuf + (((xAxisLabel.getTimeInMillis()-dataPoints.get(0).getDateTime())/60000)/widthInMins)*(w-(lBuf+rBuf));
				if (gapSize<1440){
					String xTimeLabel = timeDateFormatter.format(xAxisLabel.getTimeInMillis());
					g2.drawString(xTimeLabel,(int)(X-fm.getStringBounds(xTimeLabel,g).getWidth()/2),h-(bBuf-15));
				}
				if (widthInMins>2880){
					String xDateLabel = dateDateFormatter.format(xAxisLabel.getTimeInMillis());
					g2.drawString(xDateLabel,(int)(X-fm.getStringBounds(xDateLabel,g).getWidth()/2),h-5);
				}
				g2.draw(new Line2D.Double(new Point2D.Double(X, h-(bBuf+5)),new Point2D.Double(X, h-(bBuf-2))));
				xAxisLabel.add(GregorianCalendar.MINUTE, gapSize);
			}
			
			//Draw x axis
			g2.draw(new Line2D.Double(new Point2D.Double(lBuf, h-bBuf),new Point2D.Double(w-rBuf, h-bBuf)));
			//Draw y axis
			g2.draw(new Line2D.Double(new Point2D.Double(lBuf, tBuf),new Point2D.Double(lBuf, h-bBuf)));
			
			g2.setPaint(Color.blue);
			g2.setStroke(new BasicStroke(1));
						
			//System.out.println(widthInMins+" "+dataPoints.get(dataPoints.size()-1).getDateTime().getTimeInMillis()+" "+dataPoints.get(0).getDateTime().getTimeInMillis());
			
			for (int i=0;i<dataPoints.size()-2;i++){
				
				double X1 = lBuf + (((dataPoints.get(i).getDateTime()-dataPoints.get(0).getDateTime())/60000)/widthInMins)*(w-(lBuf+rBuf));
				double Y1 = h - (bBuf + ((dataPoints.get(i).getValue())/(Math.ceil(yMax/yMajUnit)*yMajUnit))*(h-(bBuf+tBuf)));
				double X2 = lBuf + (((dataPoints.get(i+1).getDateTime()-dataPoints.get(0).getDateTime())/60000)/widthInMins)*(w-(lBuf+rBuf));
				double Y2 = h - (bBuf + ((dataPoints.get(i+1).getValue())/(Math.ceil(yMax/yMajUnit)*yMajUnit))*(h-(bBuf+tBuf)));
				
				//System.out.println(X1+" "+Y1+" "+X2+" "+Y2);
				
				Point2D.Double point1 = new Point2D.Double(X1, Y1);
				Point2D.Double point2 = new Point2D.Double(X2, Y2);
				g2.draw(new Line2D.Double(point1,point2));
			}
		}

	}
}
