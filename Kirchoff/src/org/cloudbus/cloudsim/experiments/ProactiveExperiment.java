package org.cloudbus.cloudsim.experiments;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.ExperimentProperties;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Broker;
import org.cloudbus.cloudsim.SaaSDatacenter;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.NetworkTopology;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.requestsgenerator.BotRequestsGenerator;
import org.cloudbus.cloudsim.requestsgenerator.Grid5000RequestsGenerator;
import org.cloudbus.cloudsim.requestsgenerator.LoadRequestsGenerator;
import org.cloudbus.cloudsim.requestsgenerator.RequestsGenerator;
import org.cloudbus.cloudsim.requestsgenerator.SimpleRequestsGenerator;
import org.cloudbus.cloudsim.requestsgenerator.WikiRequestsGenerator;
import org.cloudbus.cloudsim.workloadanalyzer.BotWorkloadAnalyzer;
import org.cloudbus.cloudsim.workloadanalyzer.Grid5000WorkloadAnalyzer;
import org.cloudbus.cloudsim.workloadanalyzer.ProactiveWorkloadAnalyzer;
import org.cloudbus.cloudsim.workloadanalyzer.SimpleWorkloadAnalyzer;
import org.cloudbus.cloudsim.workloadanalyzer.WikiWorkloadAnalyzer;
import org.cloudbus.cloudsim.workloadanalyzer.WorkloadAnalyzer;

public class ProactiveExperiment {

	public static void main(String[] args) {
		printSimulationProperties();
		
		int rounds = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_EXPERIMENT_ROUNDS));
		
		for (int round=1; round<=rounds; round++) {
			runSimulationRound(round);
		}
	}
	
	private static void printSimulationProperties() {
		Log.printLine("=========== Experiment properties =============");
		Log.printLine("= Number of rounds:       " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_EXPERIMENT_ROUNDS));
		Log.printLine("= Initial number of VMs:  " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_INITIAL_VMS));
		Log.printLine("= Scheduling interval:    " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_SCHEDULING_INTERVAL));
		Log.printLine("= Hosts per datacenter:   " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_HOSTS_PERDATACENTER));
		Log.printLine("= Resource provisioner:   " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_DATACENTER_PROVISIONER));
		Log.printLine("= Workload analyzer:      " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_WORKLOAD_ANALYZER));
		Log.printLine("= Workload file:          " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_WORKLOADFILE));
		Log.printLine("= Cores per host:         " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_CORES_PERHOST));
		Log.printLine("= Memory per host:        " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_MEMORY_PERHOST));
		Log.printLine("= Storage per host:       " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_STORAGE_PERHOST));
		Log.printLine("= Bandwidth per host:     " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_BANDWIDTH_PERHOST));
		Log.printLine("= VMM scheduler:          " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_VM_SCHEDULER));
		Log.printLine("= VM queue length:        " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_VM_QUEUE_LENGTH));
		Log.printLine("= VM scheduler:           " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_CLOUDLET_SCHEDULER));
		Log.printLine("= VM creation delay:      " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_VM_CREATION_DELAY));
		Log.printLine("= MIPS per core:          " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_MIPS_PERCORE));
		Log.printLine("= Requests generator:     " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_REQUEST_GENERATOR));
		Log.printLine("= Cloudlet length:        " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_CLOUDLET_LENGTH));
		Log.printLine("= Cloudlet file size:     " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_CLOUDLET_FILESIZE));
		Log.printLine("= Network latency:        " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_NETWORK_LATENCY));
		Log.printLine("= Network bandwidth:      " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_NETWORK_BANDWIDTH));
		Log.printLine("= Service time:           " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_QOS_SERVICETIME));
		Log.printLine("= Rejection rate:         " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_QOS_REJECTIONRATE));
		//Log.printLine("= Mean Req. Interarrival: " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_REQUESTS_INTERARRIVAL_MEAN));
		//Log.printLine("= Requests per interval:  " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_REQUESTS_PERINTERVAL));
		Log.printLine("= Number of days:         " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_REQUESTS_AMOUNT));
		Log.printLine("= Training file Path:     " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_TRAINING_FILE_PATH));
		Log.printLine("= Trend interval Analyse: " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_TREND_ANALYSE));
		Log.printLine("= Trend samples to Analyse: " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_TREND_SAMPLES));
		Log.printLine("= Prediction Model: " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_PREDICTION_MODEL));
		Log.printLine("= Billing Time:           " + ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_VM_BILLING_TIME));
			
		Log.printLine("===============================================");
		Log.printLine("");
	}
		
	private static void runSimulationRound(int round) {
		Log.printLine("Starting InterCloud experiment round " + round + "...");
		
		try {
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			//calendar = new GregorianCalendar(2013,1,28,15,24,31);	
			//testar calendar
			boolean trace_flag = false;

			CloudSim.init(num_user, calendar, trace_flag);

			SaaSDatacenter datacenter = createDatacenter("Datacenter");
			Broker broker = createBroker("Broker");
			
			//NETWORK PROPERTIES
			double latency = Double.parseDouble(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_NETWORK_LATENCY));
			double bw = Double.parseDouble(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_NETWORK_BANDWIDTH));
			NetworkTopology.addLink(datacenter.getId(),broker.getId(),bw,latency);

			CloudSim.startSimulation();
			broker.printExecutionSummary();
			datacenter.printSummary();
			
			Log.printLine("InterCloud experiment finished!");
			Log.printLine("");
			Log.printLine("");
		} catch (Exception e) {
			Log.printLine("Unwanted errors happen.");
			e.printStackTrace();
		}
	}

	private static SaaSDatacenter createDatacenter(String name) throws Exception{	
		//DATACENTER PROPERTIES
		int hosts = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_HOSTS_PERDATACENTER));
		double schedulingInterval = Double.parseDouble(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_SCHEDULING_INTERVAL));
		String provisioner = ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_DATACENTER_PROVISIONER);
		String workAnalyzer = ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_WORKLOAD_ANALYZER);
		
		//HOST PROPERTIES
		int ram = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_MEMORY_PERHOST));
		int cores = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_CORES_PERHOST));
		int mips = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_MIPS_PERCORE));
		long storage = Long.parseLong(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_STORAGE_PERHOST));
		int bw = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_BANDWIDTH_PERHOST));
		String sched = ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_VM_SCHEDULER);
		
		//VM PROPERTIES
		int vmQueueLength = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_VM_QUEUE_LENGTH));
		String clSched = ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_CLOUDLET_SCHEDULER);
		
		//WORKLOAD PROPERTIES
		int cloudletLength = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_CLOUDLET_LENGTH));
		int fileSize = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_CLOUDLET_FILESIZE));
		
		List<Host> hostList = new ArrayList<Host>();
		for(int i=0;i<hosts;i++){
			List<Pe> peList = new ArrayList<Pe>();
			for(int j=0;j<cores;j++) peList.add(new Pe(j, new PeProvisionerSimple(mips)));
			
			VmScheduler vmScheduler = null;
			if(sched.equalsIgnoreCase("timeshared")) vmScheduler = new VmSchedulerTimeShared(peList);
			else if(sched.equalsIgnoreCase("spaceshared")) vmScheduler = new VmSchedulerSpaceShared(peList);
			
			hostList.add(new Host(i,new RamProvisionerSimple(ram),new BwProvisionerSimple(bw),storage,peList,vmScheduler));
		}

		String arch = "Xeon";
		String os = "Linux";
		String vmm = "Xen";
		double time_zone = 10.0;
		double cost = 0.0;
		double costPerMem = 0.00;
		double costPerStorage = 0.0;
		double costPerBw = 0.0;
		LinkedList<Storage> storageList = new LinkedList<Storage>();

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

		VmAllocationPolicy vmPolicy = null;	
		if(provisioner.equalsIgnoreCase("simple")) vmPolicy = new VmAllocationPolicySimple(hostList);
		
		WorkloadAnalyzer analyzer = null;
		if(workAnalyzer.equalsIgnoreCase("simple")) {
			double reqPerInter = Double.parseDouble(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_REQUESTS_PERINTERVAL));
			double interval = Double.parseDouble(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_REQUESTS_INTERARRIVAL_MEAN));
			analyzer = new SimpleWorkloadAnalyzer(reqPerInter/interval);
		} else if(workAnalyzer.equalsIgnoreCase("grid5000")) {
			String fileName =  ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_WORKLOADFILE);
			int numberOfRequests = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_REQUESTS_AMOUNT));
			analyzer = new Grid5000WorkloadAnalyzer(fileName,numberOfRequests);
		} else if(workAnalyzer.equalsIgnoreCase("wiki")) {
			analyzer = new WikiWorkloadAnalyzer();
		}  else if(workAnalyzer.equalsIgnoreCase("bot")) {
			analyzer = new BotWorkloadAnalyzer();	
		} else if(workAnalyzer.equalsIgnoreCase("proactive")) {
			String trainingFilePath = ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_TRAINING_FILE_PATH);
			int predictionInterval  = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_PREDICTION_INTERVAL));
			int predictionDepth = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_PREDICTION_DEPTH));		
			String predictionModel = ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_PREDICTION_MODEL);
			analyzer = new ProactiveWorkloadAnalyzer(trainingFilePath, predictionInterval, predictionDepth, predictionModel);
		}
		
		int vmcreationDelay=Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_VM_CREATION_DELAY));
		int trendAnalyse = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_TREND_ANALYSE));
		int vmBillingTime = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_VM_BILLING_TIME));
		int trendSamples = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_TREND_SAMPLES));
		
		return new SaaSDatacenter(name,characteristics,vmPolicy,storageList,schedulingInterval,clSched,analyzer,vmQueueLength,mips,cloudletLength,fileSize,vmcreationDelay, trendAnalyse, vmBillingTime, trendSamples);
	}

	private static Broker createBroker(String name) throws FileNotFoundException{
		double serviceTime = Double.parseDouble(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_QOS_SERVICETIME));
		double rejection = Double.parseDouble(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_QOS_REJECTIONRATE));
		int initialVms = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_INITIAL_VMS));
		String reqGen = ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_REQUEST_GENERATOR);

		RequestsGenerator generator = null;
		if(reqGen.equalsIgnoreCase("simple")) {
			double meanInterArrival = Double.parseDouble(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_REQUESTS_INTERARRIVAL_MEAN));
			int requests = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_REQUESTS_PERINTERVAL));
			int numberOfRequests = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_REQUESTS_AMOUNT));
			generator = new SimpleRequestsGenerator(meanInterArrival,requests,numberOfRequests);
		} else if (reqGen.equalsIgnoreCase("grid5000")) {
			String fileName =  ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_WORKLOADFILE);
			int numberOfRequests = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_REQUESTS_AMOUNT));
			generator = new Grid5000RequestsGenerator(fileName,numberOfRequests);
		} else if (reqGen.equalsIgnoreCase("wiki")) {
			int simulatedDays = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_REQUESTS_AMOUNT));
			generator = new WikiRequestsGenerator(simulatedDays);
		} else if (reqGen.equalsIgnoreCase("bot")) {
			int simulatedDays = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_REQUESTS_AMOUNT));
			generator = new BotRequestsGenerator(simulatedDays);
		
		} else if (reqGen.equalsIgnoreCase("load")) {
			String fileName =  ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_WORKLOADFILE);
			int numberOfRequests = Integer.parseInt(ExperimentProperties.getInstance().getProperty(ExperimentProperties.PROP_REQUESTS_AMOUNT));
			generator = new LoadRequestsGenerator(fileName,numberOfRequests);
		}

		return new Broker(name,generator,initialVms,serviceTime,rejection);
	}
}
