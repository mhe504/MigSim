package uk.ac.york.mhe504;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.network.datacenter.AggregateSwitch;
import org.cloudbus.cloudsim.network.datacenter.EdgeSwitch;
import org.cloudbus.cloudsim.network.datacenter.NetDatacenterBroker;
import org.cloudbus.cloudsim.network.datacenter.NetworkCloudlet;
import org.cloudbus.cloudsim.network.datacenter.NetworkConstants;
import org.cloudbus.cloudsim.network.datacenter.NetworkDatacenter;
import org.cloudbus.cloudsim.network.datacenter.NetworkHost;
import org.cloudbus.cloudsim.network.datacenter.NetworkVmAllocationPolicy;
import org.cloudbus.cloudsim.network.datacenter.RootSwitch;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import uk.ac.york.mhe504.models.CloudCostModel;
import uk.ac.york.mhe504.models.ExtrapolatedModel;
import uk.ac.york.mhe504.models.WorkloadModel;
import uk.ac.york.mhe504.util.DataTimeMap;
import uk.ac.york.mhe504.util.TableBuilder;


/**
 * 
 * This class orchestrates the MigSim simulation. It setups all of the migration 
 * components and outputs the results.
 * 
 * @author mhe504@york.ac.uk
 *
 */
public class MigSim {

	private static final int NUM_CLOUD_USERS = 1;
	private static String PARAM_FILE = "params.config";
	private static String OUT_FILE = "output.txt";
	private static double scalingFactor;
	private static double setupTime;
	private static Double readQueryGrowthAdjustmentPercentage;
	private static int migrateAt = 0;

	/**
	 * Run the MigSim simulation according to the 
	 * params.config file.
	 * 
	 */
	public static void main(String[] args) {
		
		long startTime = System.currentTimeMillis();
		System.out.println("Simulation running...");
		System.out.println("This may take an hour or more depnding on the model --- see " + OUT_FILE + " for progress.");
		
		try {
			
			Log.setOutput(new FileOutputStream(new File(OUT_FILE)));
			Log.printLine("Starting MigSim...");
			
			Map<String,String> params = loadParams(PARAM_FILE);
			
			setupTime=toDouble(params.get("setup-time"));
			scalingFactor=toDouble(params.get("scaling-factor"));
			readQueryGrowthAdjustmentPercentage=toDouble(params.get("query-growth"));
			
			printParams();
			
			List<String> tableNames = loadStrcutureModel(params.get("structure-model"));
			WorkloadModel workload = new WorkloadModel(params.get("workload-model"));
			CloudCostModel costs = new CloudCostModel(params.get("cost-model"),
													  params.get("db-instance-type"),
													  params.get("db-instance-region"),
													  params.get("db-instance-tier"),
													  params.get("db-instance-storage-type"),
													  params.get("migration-instance-type"),
													  params.get("migration-instance-region"),
													  params.get("migration-instance-tier"),
													  params.get("vpn-instance-type"),
													  params.get("vpn-instance-region"),
													  params.get("db-storage-tier"),
													  params.get("db-storage-region"));
			
			
			ExtrapolatedModel extrapolation= new ExtrapolatedModel(workload,readQueryGrowthAdjustmentPercentage);

			Map<String, Long> migrationTableRowMap = getTableRowMap(tableNames,	
																	Math.round(extrapolation.getMigrationCounts().get(migrateAt)/tableNames.size()));
			Map<String, Long> runtimeTableRowMap = getTableRowMap(tableNames,
																	Math.round(extrapolation.getRuntimeCounts().stream().reduce(0.0, Double::sum)/tableNames.size()));
					
			NetDatacenterBroker broker = runCloudSim(migrationTableRowMap, 
													 runtimeTableRowMap,
													 workload.getAverageRowSize(),
													 costs,
													 Integer.parseInt(params.get("db-instance-iops")),
													 Integer.parseInt(params.get("migration-instance-iops")),
													 Integer.parseInt(params.get("vpn-instance-iops")));
						
			
			System.out.println("Simulation Complete!");
			System.out.println();
			
			final long MILLISECS_IN_MONTH = 2628000000l;
			long monthlyCapacity = Math.round(MILLISECS_IN_MONTH / broker.getQueryExectuionTime_cloudDB(28));
			int numberOfMonths = extrapolation.getRuntimeCounts().size();
			long totalCapacity = monthlyCapacity * numberOfMonths;
		
			List<Long> cumlativeLoad = getCumlativeLoad(runtimeTableRowMap,extrapolation);
			int scaleAt = -1;

			for (int i =0; i<cumlativeLoad.size();i++)
			{
				long load = cumlativeLoad.get(i);
				if (load >  totalCapacity)
				{
					scaleAt = i;
					i = cumlativeLoad.size();
				}
			}
			Log.printLine("Total Capacity: " +totalCapacity);
			Log.printLine("Scale At: " +scaleAt);
			Log.printLine("Cumulative Load:");
			cumlativeLoad.forEach(item->Log.printLine(item));
			
			printResults(workload, broker.getCloudletReceivedList(),scaleAt,costs);

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		long exeTime=System.currentTimeMillis() - startTime;
		Log.printLine("Simulation finished!");
		Log.printLine("Executed in " + exeTime + "ms");
		
		System.out.println();
		System.out.println("Simulation execution time=" +exeTime + "ms");
	}

	
	/**
	 * This method provides the same functionality as Double.valueOf(),
	 * expect it returns null values rather than throwing a
	 * NullPointerExcpetion.
	 * 
	 * @param s The string to be parsed
	 * @return The value as a Double object or null if the input is null.
	 */
	private static Double toDouble (String s)
	{
		if (s==null)
			return null;
		else
			return Double.valueOf(s);
		
	}
	
	private static Map<String, String> loadParams(String filePath) {
		
		Map<String, String> results = new HashMap<>();
		try {
			List<String> lines = FileUtils.readLines(new File(filePath));
			
			for (String line : lines)
			{
				String[] parts = line.split("=");
				if (parts.length == 2)
					results.put(parts[0], parts[1]);
				else if (! "".equals(parts[0]))
					results.put(parts[0], null);
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return results;
	}

	private static List<Long> getCumlativeLoad(Map<String, Long> runtimeTableRowMap, ExtrapolatedModel extrapolation) {
		
		long totalQueries = runtimeTableRowMap.values().stream().mapToLong(l -> l).sum();
		double numerOfMonths = extrapolation.getRuntimeCounts().size();
		
		List<Long> results = new ArrayList<>();
		
		for (double d = 1.0; d<=numerOfMonths;d++)
			results.add(Math.round((totalQueries*(d/numerOfMonths)/scalingFactor)));
		
		return results;
		
	}

	private static void printResults(WorkloadModel workload, List<NetworkCloudlet> cloudletResults, int scaleAt, CloudCostModel costs) throws IOException {
		
		printCloudletList(cloudletResults);
		Log.printLine("Number of Cloudlets: " + cloudletResults.size());
		Log.printLine("Cost of IOPS: " + calcIOPSCost(cloudletResults));
		printComputeCost(cloudletResults,workload,scaleAt,costs);
		Log.printLine("Storage Cost: " + calcStorageCost(cloudletResults));
		
	}

	private static NetDatacenterBroker runCloudSim(Map<String, Long> migrationTableRowMap,
			Map<String, Long> runtimeTableRowMap, int averageRowSize, CloudCostModel costs,
			 int cloudDbIops, int mwIops, int gtwIops) throws Exception {
		
		CloudSim.init(NUM_CLOUD_USERS, Calendar.getInstance(), false);	
		
		NetworkDatacenter datacentre = createDatacenter("MockDatacenter");
		NetDatacenterBroker broker = new NetDatacenterBroker(migrationTableRowMap,
															 runtimeTableRowMap,
															 datacentre,
															 averageRowSize,
															 costs,
															 cloudDbIops, mwIops, gtwIops);
		
		createNetwork(datacentre);
		
		CloudSim.startSimulation();
		CloudSim.stopSimulation();
		
		return broker;
	}

	private static Map<String, Long> getTableRowMap(List<String> tableNames, long averageRowsPerTable) {
		averageRowsPerTable=(long) (averageRowsPerTable*scalingFactor);

		Map<String,Long> tableRowMap = new HashMap<>();
		for (String name : tableNames)
		{
			tableRowMap.put(name,averageRowsPerTable) ;
		}
		return tableRowMap;
	}

	private static void printParams() {
		Log.print("Paramters: ");
		if (readQueryGrowthAdjustmentPercentage != null)
			Log.print("Query/Load Growth = " + readQueryGrowthAdjustmentPercentage+ ", ");
		Log.print("Migration Period = " + migrateAt + ", ");
		Log.print("Scaling Factor = " + scalingFactor);
		Log.printLine();
		Log.printLine();
	}


	private static String calcStorageCost(List<NetworkCloudlet> newList) {
		
		Map<Double,List<DataTimeMap>> costGroups = new HashMap<>();
		for (NetworkCloudlet nc : newList)
		{
			if(costGroups.containsKey(nc.getStorageCost()))
			{
				List<DataTimeMap> additonal = nc.getDataTimeMap();
				List<DataTimeMap> existing = costGroups.get(nc.getStorageCost());
				existing.addAll(additonal);	
				costGroups.put(nc.getStorageCost(), existing);
			}
			else
			{
				costGroups.put(nc.getStorageCost(), nc.getDataTimeMap());
			}
		}
		double cost = 0.0;
		
		double gbMonths = 0.0;
		for (Entry<Double, List<DataTimeMap>>  entry: costGroups.entrySet()) {
		    
		    Double price = entry.getKey();
		    List<DataTimeMap> list = entry.getValue();
		    //merge into 3600 intervals
		    int interval = (int)Math.ceil(list.get(list.size()-1).getTime()/3600000.0);
		    int[] intervalList = new int[interval];
		    
		    for (int j =0; j<interval;j++)
		    	intervalList[j] = 3600000 * (1+j);
		    
		    List<DataTimeMap> byteHoursList = new LinkedList<>();
		    int intervalIndex = 0;
		    long bytes = 0l;
		    for (int i =0;i<=list.size()-1;i++)
		    {
		    	DataTimeMap map = list.get(i);
		    	if (map.getTime() < intervalList[intervalIndex])
		    	{
		    		bytes = bytes + map.getData();		    		
		    	}
		    	else
		    	{
		    		byteHoursList.add(new DataTimeMap(bytes, 1+intervalIndex));
		    		bytes = 0l;
		    		intervalIndex++;
		    	}
		    	
		    	if (intervalIndex > intervalList.length-1)
		    	{
		    		i = list.size();
		    	}
		    }
		    
		    if (byteHoursList.size() == 0)
		    	byteHoursList.add(new DataTimeMap(bytes, 1+intervalIndex));
		    
		    long byteHours =0;
		    
		    for (DataTimeMap dataTimeMap: byteHoursList)
		    {
		    	byteHours = byteHours + Math.round(dataTimeMap.getData() * dataTimeMap.getTime());
		    }
		    
		    double bytesPerGB = 1073741824.0;
		    double hoursPerMonth = 720.0;
		    gbMonths = byteHours / bytesPerGB / hoursPerMonth;
		    gbMonths = gbMonths / scalingFactor;
		    cost = cost + (price * Math.ceil(gbMonths));

		}
		
		return cost + " (" + Math.ceil(gbMonths) + " GBMonths)";
	}

	private static void printComputeCost(List<NetworkCloudlet> newList, WorkloadModel workload, int scaleAt, CloudCostModel costs) {
		Log.printLine("Compute Cost Results:");
		Log.printLine();
		
		Log.printLine("\t Data Migrated = " + (workload.getTotalDatabaseSize()/1000000000) + " GB");
		
		Map<Double,Double> costGroups = new HashMap<>();
		for (NetworkCloudlet nc : newList)
		{			
			if(costGroups.containsKey(nc.getComputeCost()))
			{
				Double total = costGroups.get(nc.getComputeCost());
				total = total + (nc.getActualCPUTime());
				costGroups.put(nc.getComputeCost(),  total);
			}
			else
			{
				costGroups.put(nc.getComputeCost(),  nc.getActualCPUTime());
			}
		
		}
		
		double maxDuration = 0;
		for (Map.Entry<Double,Double> entry : costGroups.entrySet()) {
		    double currentPrice = entry.getKey();
		    double milliseconds = entry.getValue();
		    
		    if (currentPrice == costs.getDatabaseInstanceCost() &&
		    	milliseconds <= 65000000)
		    {
		    	//Correction factor for small scale experiments
		    	milliseconds = milliseconds * 0.94;
		    }
		    int hours = (int) Math.ceil(	milliseconds/3600000);
		    
		    Log.printLine(currentPrice + ", " + milliseconds + "s (" + hours + " hours)");
		    
		    if (hours > maxDuration)
		    {
		    	maxDuration = hours;
		    }
		    
		}
		
		System.out.println("Migration Results:");
		
		//Only if all components are required for all the time
		Log.printLine("Migration Duration (hours) = " + maxDuration);
		//maxDuration = maxDuration + modelHours;
		Log.printLine("Runtime Duration (hours) = " + workload.getDurationHours());
		Log.printLine("Setup & Testing Duration (hours) = " + setupTime);
		maxDuration = maxDuration + setupTime;
		Log.printLine("Total Duration (hours) = " + maxDuration);
		
		System.out.println("\t duration=" +maxDuration);
		
		double totalCost = 0.0;	
		for (Map.Entry<Double,Double> entry : costGroups.entrySet()) {
		    double currentPrice = entry.getKey();
		    totalCost = totalCost + (currentPrice * maxDuration);
		}
		totalCost=totalCost*0.8025;
		
		DecimalFormat currency = new DecimalFormat("##.00");
		
		double numIntervals = workload.getCounts().length;
		long interval = Math.round(workload.getDurationHours() / numIntervals);
		
		Log.printLine("\t Migration Cost:" + currency.format(totalCost));
		System.out.println("\t cost=" + currency.format(totalCost));
		
		System.out.println("Running Results:");
		Log.printLine("\t Cumlative Running Costs:");
		double cost = 0.0;
		for (int i=0; i<numIntervals;i++)
		{
			if (i <scaleAt)
			{
				cost = cost + (costs.getDatabaseInstanceCost() * (interval));				
			}
			else if (i >= scaleAt)
			{
				cost = cost + ((costs.getDatabaseInstanceCost()*2) * (interval));
			}
			System.out.println("\tp" + i + "=" + cost);
			Log.printLine("\t\t" + cost);
		}
		
		totalCost = totalCost + cost;
		
		Log.printLine("Total Compute Cost = " + totalCost);
		
	    
	}

	private static double calcIOPSCost(List<NetworkCloudlet> newList) {
		
		Map<Double,Double> costGroups = new HashMap<>();
		for (NetworkCloudlet nc : newList)
		{
			if(costGroups.containsKey(nc.getIoCost()))
			{
				Double total = costGroups.get(nc.getIoCost());
				total += nc.getDataSentRecieved();
				costGroups.put(nc.getIoCost(),total);
			}
			else
			{
				costGroups.put(nc.getIoCost(),  nc.getDataSentRecieved());
			}
		}		
		double cost = 0.0;
		for (Map.Entry<Double,Double> entry : costGroups.entrySet()) {
		    double price = entry.getKey();
		    double bytes = entry.getValue();
		    double iops = Math.ceil(bytes / 1000000.0);
		    
		    cost = cost + (price * iops);
		}
		return cost;
	}
	
	/**
	 * Creates the datacenter.
	 * 
	 * @param name
	 *            the name
	 * 
	 * @return the datacenter
	 */
	private static NetworkDatacenter createDatacenter(String name) {

		List<NetworkHost> hostList = new ArrayList<NetworkHost>();

		int mips = 100;
		int ram = 16000; // host memory (MB)
		long storage = 1000000; // host storage
		int bw = 10000;
		
		for (int i = 0; i < NetworkConstants.EdgeSwitchPort * NetworkConstants.AggSwitchPort
				* NetworkConstants.RootSwitchPort; i++) {

			//Add 32 Cores
			List<Pe> peList = new ArrayList<Pe>();
			for (int n = 0; n<16; n++)
				peList.add(new Pe(n, new PeProvisionerSimple(mips))); 

			hostList.add(new NetworkHost(
					i,
					new RamProvisionerSimple(ram),
					new BwProvisionerSimple(bw),
					storage,
					peList,
					new VmSchedulerTimeShared(peList)));
		}


		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
		double costPerBw = 0.0; // the cost of using bw in this resource

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				"x86",
				"Linux",
				"Xen",
				hostList,
				10.0,
				cost,
				costPerMem,
				costPerStorage,
				costPerBw);

		NetworkDatacenter datacenter = null;
		try {
			datacenter = new NetworkDatacenter(
					name,
					characteristics,
					new NetworkVmAllocationPolicy(hostList),
					new ArrayList<Storage>(),
					0);

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return datacenter;
	}

	/**
	 * Prints the Cloudlet objects.
	 * 
	 * @param list
	 *            list of Cloudlets
	 * @throws IOException
	 */
	private static void printCloudletList(List<NetworkCloudlet> list) throws IOException {
		TableBuilder tb = new TableBuilder();
		int size = list.size();
		NetworkCloudlet cloudlet;
		Log.printLine();
		Log.printLine("OUTPUT:");
		Log.printLine("---------------------------------------------------------------------------------------------------------------");
		tb.addRow("Cloudlet ID","Cloudlet Name","VM ID","Compute Hours","ActualCPUTime", "Data Stored", "Data Sent/Recieved");
		tb.addRow("--------------","------------------","--------------","--------------","--------------","------------","-------------------");

		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
				tb.addRow(String.valueOf(cloudlet.getCloudletId()),
						  String.valueOf(cloudlet.getName()),
						  String.valueOf(cloudlet.getVmId()),	
						  String.valueOf(cloudlet.getRequestedComputeHours()),
						  String.valueOf(cloudlet.getActualCPUTime()),
						  String.valueOf(cloudlet.getData()),
						  String.valueOf(cloudlet.getDataSentRecieved()));
			}
		}
		Log.printLine(tb.toString());

	}
	


	private static void createNetwork(NetworkDatacenter dc) {

		EdgeSwitch edge = new EdgeSwitch("EdgeSwitch", NetworkConstants.EDGE_LEVEL, dc);
		edge.setUplinkbandwidth(1000.0);
		edge.setDownlinkbandwidth(1000.0);
		edge.setLatency(0.0001);
		
		for (NetworkHost h : dc.getHostList()){
			edge.hostlist.put(h.getId(), (NetworkHost)h);
			h.sw = edge;
			h.bandwidth = 100 * 1024 * 1024 * 100; //100gbps
			dc.HostToSwitchid.put(h.getId(), edge.getId());
		}
		
		AggregateSwitch agg = new AggregateSwitch("AggSwitch", NetworkConstants.Agg_LEVEL, dc);
		agg.setUplinkbandwidth(1000.0);
		agg.setDownlinkbandwidth(1000.0);
		agg.setLatency(0.0001);
		
		RootSwitch root = new RootSwitch("RootSwitch", NetworkConstants.ROOT_LEVEL, dc);
		root.setUplinkbandwidth(1000.0);
		root.setDownlinkbandwidth(1000.0);
		root.setLatency(0.0001);

		//Connect together switches
		edge.getUplinkswitches().add(agg);
		agg.getDownlinkswitches().add(edge);
		agg.getUplinkswitches().add(root);
		root.getDownlinkswitches().add(agg);
		
		
	}
	
	private static List<String> loadStrcutureModel(String filePath) throws IOException {
		Log.printLine("Loading structure model from: " + filePath);
		List<String> file = FileUtils.readLines(new File(filePath));
		List<String> tableNames = new ArrayList<>();
		
		for (String line: file)
			if (line.contains("<dataElement xsi:type=\"data:RelationalTable\""))
				tableNames.add(line.split("name=")[1].replace("\"", "").replace(">", ""));
		
		Log.printLine("\t" + tableNames.size() + " tables found.\n");
		return tableNames;
	}
	
}