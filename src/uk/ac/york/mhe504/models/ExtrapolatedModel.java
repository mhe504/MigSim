package uk.ac.york.mhe504.models;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.cloudbus.cloudsim.Log;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

public class ExtrapolatedModel {
	
	WorkloadModel orginalModel;
	
	private List<Double> migrationCounts;
	private List<Double> migrationWrites;
	private List<Double> migrationReads;
	private List<Double> runtimeQueries;
	private Double readQueryGrowthAdjustmentPercentage;
	
	
	public ExtrapolatedModel(WorkloadModel model, Double queryGrowth) throws DOMException, ParserConfigurationException, SAXException, IOException, ParseException
	{
		orginalModel = model;
		readQueryGrowthAdjustmentPercentage=queryGrowth;
		
		extrapolate();
		
	}
	
	public ExtrapolatedModel(WorkloadModel model) throws DOMException, ParserConfigurationException, SAXException, IOException, ParseException
	{
		orginalModel = model;
		readQueryGrowthAdjustmentPercentage=null;
		
		extrapolate();
	}
	
	private void extrapolate() throws ParserConfigurationException, SAXException, IOException, DOMException, ParseException
	{
		Log.printLine("Extrapolated Migration Values:");
		
		migrationWrites = extrapolateWrites(orginalModel.getWrites());

		migrationCounts = extrapolateCounts(orginalModel.getCounts(),migrationWrites);
		
		
		double growthRate = ((migrationCounts.get(migrationCounts.size()-1)-migrationCounts.get(0))/(double)migrationCounts.get(0))*100;
		Log.printLine("Database Record Count (Growth = " + String.format("%.0f", growthRate) + "%):");
		migrationCounts.forEach(Log::printLine);
		
		migrationReads = extrapolateReads(orginalModel.getReads());
		Log.printLine("Read Queries:");
		migrationReads.forEach(Log::printLine);
		Log.printLine("Write Queries:");
		migrationWrites.forEach(Log::printLine);
		Log.printLine();
		
		Log.printLine("Extrapolated Runtime Values:");
		runtimeQueries = calculateRuntimeQueries();
		Log.printLine("Queries/DB Load:");
		runtimeQueries.forEach(Log::printLine);
		Log.printLine();
	}
	
	private List<Double> extrapolateReads(double[] reads) throws ParserConfigurationException, SAXException, IOException {
		
		double[] time = IntStream.rangeClosed(1, reads.length).asDoubleStream().toArray();
		
		if (readQueryGrowthAdjustmentPercentage == null)
		{
			return extrapolateFromModel(reads, time);
		}
		else if (readQueryGrowthAdjustmentPercentage == 0)
		{
			List <Double> results = new ArrayList<>();
			for (int i = 0; i < reads.length; i++)
			{
				results.add(reads[0]);
			}
			return results;
		}
		else
		{
			return findExrapolatedResults(readQueryGrowthAdjustmentPercentage,reads, time);
		}
	}

	private static List<Double> extrapolateFromModel(double[] values, double[] time) {
		List <Double> results = new ArrayList<>();
		SimpleRegression simpleRegression = new SimpleRegression();
		for (int i=0 ; i<values.length; i++)
		{
			simpleRegression.addData(time[i],values[i]);
		}
		double[] newTime = IntStream.rangeClosed(values.length+1, (values.length+1)*2).asDoubleStream().toArray();
		for (int i=0 ; i<values.length; i++)
		{
			results.add((double) Math.round(simpleRegression.predict(newTime[i])));
		}
		return results;
	}
	
	private List<Double> calculateRuntimeQueries () throws ParserConfigurationException, SAXException, IOException, DOMException, ParseException
	{
		
		List<Double> runtimeReads = extrapolateReads(migrationReads.stream().mapToDouble(d -> d).toArray());
		List<Double> runtimeWrites = extrapolateWrites(migrationWrites.stream().mapToDouble(d -> d).toArray());
		
		List<Double> results = new ArrayList<>();
		
		for (int i=0;i<runtimeReads.size(); i++)
		{
			results.add(runtimeReads.get(i) + runtimeWrites.get(i));
		}
		
		return results;
	
	}
	
	private List<Double> extrapolateWrites(double[] writes) throws ParserConfigurationException, SAXException, IOException {
		
		double[] time = IntStream.rangeClosed(1, writes.length).asDoubleStream().toArray();

		if (readQueryGrowthAdjustmentPercentage == null)
		{
			return extrapolateFromModel(writes, time);
		}
		else if (readQueryGrowthAdjustmentPercentage == 0)
		{
			List <Double> results = new ArrayList<>();
			for (int i = 0; i < writes.length; i++)
			{
				results.add(writes[0]);
			}
			return results;
		}
		else
		{
			return findExrapolatedResults(readQueryGrowthAdjustmentPercentage,writes, time);
		}
	}

	
	private List<Double> extrapolateCounts(double[] counts, List<Double> writes) throws ParserConfigurationException, SAXException, IOException, DOMException, ParseException {
		
		List<Double> results = new ArrayList<>();
		for (int i=0; i<counts.length;i++)
		{
			results.add(counts[i] + writes.get(i));
		}

		return results;		
	}

	/**
	 * Search function which returns a set of extrapolated results using linear regression.
	 * 
	 * @param targetPercentage The desired percentage growth within the results
	 * @param values y-axis for regression
	 * @param time x-axis for regression
	 * @return Extrapolated values
	 */
	private List <Double> findExrapolatedResults(double targetPercentage, double[] values, double[] time) {

		double[] newTime = IntStream.rangeClosed(values.length+1, (values.length*2)).asDoubleStream().toArray();
		
		double[] ylimits = new double[2];
		ylimits[0] = values[values.length-1];
		ylimits[1] = ylimits[0] * (1+targetPercentage);
		
		double[] xlimits = new double[2];
		xlimits[0] = time[time.length-1];
		xlimits[1] = newTime[newTime.length-1]+1;
		
		List <Double> results = new ArrayList<>();
		
		LinearInterpolator interp = new LinearInterpolator();
	    PolynomialSplineFunction f = interp.interpolate(xlimits, ylimits);
	     
		for (int i=0 ; i<values.length; i++)
		{
			results.add((double) Math.round(f.value(newTime[i])));
		}

	
		return results;
	}

	
	public List<Double> getMigrationCounts() {
		return migrationCounts;
	}
	public void setMigrationCounts(List<Double> migrationCounts) {
		this.migrationCounts = migrationCounts;
	}
	public List<Double> getMigrationWrites() {
		return migrationWrites;
	}
	public void setMigrationWrites(List<Double> migrationWrites) {
		this.migrationWrites = migrationWrites;
	}
	public List<Double> getMigrationReads() {
		return migrationReads;
	}
	public void setMigrationReads(List<Double> migrationReads) {
		this.migrationReads = migrationReads;
	}
	public List<Double> getRuntimeCounts() {
		return runtimeQueries;
	}
	public void setRuntimeCounts(List<Double> runtimeCounts) {
		this.runtimeQueries = runtimeCounts;
	}

}
