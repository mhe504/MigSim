package uk.ac.york.mhe504.models;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.cloudbus.cloudsim.Log;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class WorkloadModel {
	
	private String sourceFile;
	private double[] counts;
	private double[] reads;
	private double[] writes;
	private int durationHours; //the length of the time period this data covers
	private int averageRowSize; //bytes
	private double totalDatabaseSize; //bytes
	
	public WorkloadModel(String filePath) throws DOMException, IOException, SAXException, ParserConfigurationException, ParseException
	{
		sourceFile = filePath;
		load();
	}
	
	private void load() throws DOMException, IOException, SAXException, ParserConfigurationException, ParseException
	{
		Log.printLine("Loading workload model from: " + sourceFile);		
		
		counts = getEntityCounts(sourceFile).stream().mapToDouble(d -> d).toArray();
		reads = getReadCounts(sourceFile).stream().mapToDouble(d -> d).toArray();
		writes = getWriteCounts(sourceFile).stream().mapToDouble(d -> d).toArray();
		durationHours =  getWorkloadModelDuration(sourceFile);
		
		setTotalDatabaseSize(getLastDatabaseSizeRecord(sourceFile));
		averageRowSize = (int) Math.round(getTotalDatabaseSize() /counts[counts.length-1]);
		
		Log.printLine("\t" + counts.length + " measurements found.");
		Log.printLine("Measurement period (hours):" + durationHours);
		
		double modelGrowthRate = ((counts[counts.length-1]-counts[0])/(double)counts[0])*100;
		Log.printLine("Database Record Count (Growth = " + String.format("%.0f", modelGrowthRate) + "%):");
		for (int i = 0; i<counts.length;i++)
			Log.printLine(counts[i]);
		
		double queryGrowthRate = (((reads[reads.length-1] + writes[writes.length-1])-
									(reads[0] + writes[0]))/
									((double)reads[0] + (double)writes[0]))*100;
		Log.printLine("Database Queries (Growth = " + String.format("%.0f", queryGrowthRate) + "%):");
		for (int i = 0; i<reads.length;i++)
			Log.printLine(reads[i] + writes[i]);
		
		Log.printLine();
	}
	
	private Double getLastDatabaseSizeRecord(String workloadModelPath) throws ParserConfigurationException, SAXException, IOException, DOMException, ParseException {
		File fXmlFile = new File(workloadModelPath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		
		doc.getDocumentElement().normalize();
		
		SortedMap<Date,Double> rowsTimestampMap = new TreeMap<>();
		
		NodeList nList = doc.getElementsByTagName("observations");
		for (int i=0; i< nList.getLength(); i++)
		{
			NamedNodeMap whenObserved = nList.item(i).getAttributes();
			if (whenObserved.getNamedItem("requestedMeasures") != null) //Must be growth observation 
			{
				DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH);
				Date date = format.parse(whenObserved.getNamedItem("whenObserved").getNodeValue());
				
				NodeList childNodes = nList.item(i).getChildNodes();
				for (int j=0; j< childNodes.getLength(); j++)
				{
					Node n = childNodes.item(j);
					NodeList innerChildNodes = childNodes.item(j).getChildNodes();
					if ("observedMeasures".equals(n.getNodeName()))
					{
						NamedNodeMap nameMap = n.getAttributes();
						String name = nameMap.getNamedItem("name").getNodeValue();
						if ("Database Size".equals(name))
						{
							String measure = nameMap.getNamedItem("measure").getNodeValue();
							
							char unit = findUnit(measure,doc);
							if (unit == '0')
								throw new ModelValueException("Workload model contains invalid unit");
							
							for (int k=0; k< innerChildNodes.getLength(); k++)
							{
								Node in = innerChildNodes.item(k);
								if ("measurements".equals(in.getNodeName()))
								{
									NamedNodeMap sizeMap = in.getAttributes();
									double size = Double.valueOf(sizeMap.getNamedItem("value").getNodeValue());
									rowsTimestampMap.put(date,convertToBytes(size, unit));
								}
							}	
						}
					}
				}
			}
		}
		return rowsTimestampMap.get(rowsTimestampMap.lastKey());
	}

	private double convertToBytes(double size, char unit) {
		
		switch (unit)
		{
			case 'k': return size * 1000.0;
			case 'm': return size * 1000000.0;
			case 'g': return size * 1000000000.0;
			case 't': return size * 1000000000000.0;
			default : return size;
		}
		
	}

	private char findUnit(String measure, Document doc) throws DOMException, ParseException {
		String[] parts = measure.split("/@");
		String nodeName = parts[1].split("\\.")[0];
		int nodeNumber = Integer.valueOf(parts[1].split("\\.")[1]);
		
		NodeList measureElements = doc.getElementsByTagName(nodeName).item(0).getChildNodes();
		Node target = measureElements.item(1+nodeNumber);
		String nodeValue = target.getAttributes().getNamedItem("unit").getNodeValue();
		
		//the measurement 'unit' in the SMM is a string and the user can specific any value,
		//however this switch catch commonly used values.
		switch (nodeValue)
		{
			case "B": return 'b';
			case "KB": return 'k';
			case "MB": return 'm';
			case "GB": return 'g';
			case "TB": return 't';
			case "b": return 'b';
			case "kb": return 'k';
			case "mb": return 'm';
			case "gb": return 'g';
			case "tb": return 't';
			case "bytes": return 'b';
			case "kilobytes": return 'k';
			case "megabytes": return 'm';
			case "gigbytes": return 'g';
			case "terabytres": return 't';
			case "Bytes": return 'b';
			case "Kilobytes": return 'k';
			case "Megabytes": return 'm';
			case "Gigbytes": return 'g';
			case "Terabytres": return 't';
		}
		
		return '0';
	}

	public double[] getWrites() {
		return writes;
	}
	public void setWrites(double[] writes) {
		this.writes = writes;
	}
	public double[] getReads() {
		return reads;
	}
	public void setReads(double[] reads) {
		this.reads = reads;
	}
	public String getSourceFile() {
		return sourceFile;
	}
	public void setSourceFile(String sourceFile) {
		this.sourceFile = sourceFile;
	}

	public double[] getCounts() {
		return counts;
	}

	public void setCounts(double[] counts) {
		this.counts = counts;
	}
	
	/**
	 * Get the number of hours the workload model covers
	 * 
	 * @param workloadModelPath 
	 * @return
	 * @throws ParseException 
	 * @throws IOException 
	 */
	private int getWorkloadModelDuration(String workloadModelPath) throws ParseException, IOException {
		
		SortedSet<Date> results = new TreeSet<>();
		List<String> file = FileUtils.readLines(new File(workloadModelPath));
		for (String line : file)
		{
			if (line.contains("whenObserved"))
			{
				String[] parts = line.split("\"");
				
				DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH);
				results.add(format.parse(parts[1]));
			}
		}
		
		long diff = results.last().getTime() - results.first().getTime();
		return (int)(diff / (60 * 60 * 1000));
	}
	
	/**
	 * Gets the number of rows in the DB at the last measurement.
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws DOMException
	 * @throws ParseException
	 */
	private static List<Integer> getEntityCounts(String filePath) throws IOException, SAXException, ParserConfigurationException, DOMException, ParseException
	{
		File fXmlFile = new File(filePath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		
		doc.getDocumentElement().normalize();
		
		List<Integer> counts = new ArrayList<>();
		
		NodeList nList = doc.getElementsByTagName("observations");
		for (int i=0; i< nList.getLength(); i++)
		{
			Node nn = nList.item(i);
			if (nn.getAttributes().getNamedItem("requestedMeasures") != null) //Must be growth observation 
			{
				NodeList childNodes = nList.item(i).getChildNodes();
				for (int j=0; j< childNodes.getLength(); j++)
				{
					Node n = childNodes.item(j);
					NodeList innerChildNodes = childNodes.item(j).getChildNodes();
					if ("observedMeasures".equals(n.getNodeName()))
					{
						NamedNodeMap nameMap = n.getAttributes();
						String name = nameMap.getNamedItem("name").getNodeValue();
						if ("Database Workload".equals(name))
						{//Entity Count
							for (int k=0; k< innerChildNodes.getLength(); k++)
							{
								Node in = innerChildNodes.item(k);
								if ("measurements".equals(in.getNodeName()))
								{
									NamedNodeMap sizeMap = in.getAttributes();
									if ("Entity Count".equals(sizeMap.getNamedItem("name").getNodeValue()))
									{
										int rows = (int) Double.parseDouble(sizeMap.getNamedItem("value").getNodeValue());									
										counts.add(rows);
									}
								}
							}	
						}
					}
						
					
				}

			}
		}

		return counts;
	}


	private List<Double> getReadCounts(String path) throws ParserConfigurationException, SAXException, IOException {
		List<Double> readWriteCounts = new ArrayList<>();
		
		File fXmlFile = new File(path);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		
		doc.getDocumentElement().normalize();
		
		NodeList nList = doc.getElementsByTagName("observations");
		for (int i=0; i< nList.getLength(); i++)
		{
			Node observation = nList.item(i);
			if (observation.getAttributes().getNamedItem("requestedMeasures") != null) //Must be growth observation 
			{
				NodeList childNodes = observation.getChildNodes();
				for (int j = 0; j <childNodes.getLength(); j++)
				{
					NamedNodeMap att = childNodes.item(j).getAttributes();
					if (att != null && 
						"Database Workload".equals(att.getNamedItem("name").getNodeValue()))
					{
						NodeList measurements = childNodes.item(j).getChildNodes();
						j = childNodes.getLength();
						double reads = 0;
						
						
						for (int k = 0; k < measurements.getLength(); k++)
						{
							att = measurements.item(k).getAttributes();
							if (att != null)
							{
								if("Entity Reads".equals(att.getNamedItem("name").getNodeValue()))
									reads = Double.parseDouble(att.getNamedItem("value").getNodeValue());						
							}
						}
						readWriteCounts.add(reads);
					}
				}
			}
		}
		return readWriteCounts;		
	}
	
	private List<Double> getWriteCounts(String path) throws ParserConfigurationException, SAXException, IOException {
		List<Double> readWriteCounts = new ArrayList<>();
		
		File fXmlFile = new File(path);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		
		doc.getDocumentElement().normalize();
		
		NodeList nList = doc.getElementsByTagName("observations");
		for (int i=0; i< nList.getLength(); i++)
		{
			Node observation = nList.item(i);
			if (observation.getAttributes().getNamedItem("requestedMeasures") != null) //Must be growth observation 
			{
				NodeList childNodes = observation.getChildNodes();
				for (int j = 0; j <childNodes.getLength(); j++)
				{
					NamedNodeMap att = childNodes.item(j).getAttributes();
					if (att != null && 
						"Database Workload".equals(att.getNamedItem("name").getNodeValue()))
					{
						NodeList measurements = childNodes.item(j).getChildNodes();
						j = childNodes.getLength();
						double writes = 0;
						
						
						for (int k = 0; k < measurements.getLength(); k++)
						{
							att = measurements.item(k).getAttributes();
							if (att != null)
							{
								if("Entity Writes".equals(att.getNamedItem("name").getNodeValue()))
									writes = Double.parseDouble(att.getNamedItem("value").getNodeValue());							
							}
						}
						readWriteCounts.add(writes);
					}
				}
			}
		}
		return readWriteCounts;		
	}

	public int getDurationHours() {
		return durationHours;
	}

	public void setDurationHours(int durationHours) {
		this.durationHours = durationHours;
	}

	public int getAverageRowSize() {
		return averageRowSize;
	}

	public void setAverageRowSize(int averageRowSize) {
		this.averageRowSize = averageRowSize;
	}

	public double getTotalDatabaseSize() {
		return totalDatabaseSize;
	}

	public void setTotalDatabaseSize(double totalDatabaseSize) {
		this.totalDatabaseSize = totalDatabaseSize;
	}

}
