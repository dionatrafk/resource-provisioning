package org.cloudbus.cloudsim;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ExperimentProperties {
	
	public static final String PROPERTIES_FILENAME = "experiment.properties";
	public static final String PROP_EXPERIMENT_ROUNDS = "experiment.rounds";
	public static final String PROP_INITIAL_VMS = "experiment.initialvms";
	public static final String PROP_SCHEDULING_INTERVAL = "scheduling.interval";
	public static final String PROP_HOSTS_PERDATACENTER = "datacenter.hosts";
	public static final String PROP_DATACENTER_PROVISIONER = "datacenter.provisioner";
	public static final String PROP_WORKLOAD_ANALYZER = "workload.analyzer";
	public static final String PROP_WORKLOADFILE = "workload.filename";
	public static final String PROP_CORES_PERHOST = "host.cores";
	public static final String PROP_MEMORY_PERHOST = "host.memory";
	public static final String PROP_STORAGE_PERHOST = "host.storage";
	public static final String PROP_BANDWIDTH_PERHOST = "host.bandwidth";
	public static final String PROP_VM_SCHEDULER = "host.vmscheduler";
	public static final String PROP_VM_QUEUE_LENGTH = "vm.queue.length";
	public static final String PROP_VM_CREATION_DELAY = "vm.creation.delay";
	public static final String PROP_MIPS_PERCORE = "core.mips";
	public static final String PROP_REQUEST_GENERATOR = "broker.requestgenerator";
	public static final String PROP_CLOUDLET_LENGTH = "cloudlet.length";
	public static final String PROP_CLOUDLET_SCHEDULER = "vm.cloudletscheduler";
	public static final String PROP_CLOUDLET_FILESIZE = "file.size";
	public static final String PROP_NETWORK_LATENCY = "network.latency";
	public static final String PROP_NETWORK_BANDWIDTH = "network.bandwidth";
	public static final String PROP_QOS_SERVICETIME = "qos.servicetime";
	public static final String PROP_QOS_REJECTIONRATE = "qos.rejection";
	public static final String PROP_REQUESTS_INTERARRIVAL_MEAN = "requests.interarrival";
	public static final String PROP_REQUESTS_PERINTERVAL = "requests.perinterval";
	public static final String PROP_REQUESTS_AMOUNT = "requests.number";
	public static final String PROP_TRAINING_FILE_PATH = "traningfile.path";
	public static final String PROP_PREDICTION_INTERVAL = "prediction.interval";
	public static final String PROP_PREDICTION_DEPTH = "prediction.depth";
	public static final String PROB_PREDICTION_CONFIDENCE = "prediction.confidence";
	public static final String PROP_TREND_ANALYSE = "trend.analyse";
	public static final String PROP_TREND_SAMPLES = "trend.samples";
	public static final String PROP_VM_BILLING_TIME = "vm.billing";
	public static final String PROP_PREDICTION_MODEL = "prediction.model";
	
	
	protected static ExperimentProperties singleInstance = null;
	private Properties currentProperties = System.getProperties();

	private ExperimentProperties(){
	    loadPropertiesFromFile(PROPERTIES_FILENAME);
	}
	
	 public synchronized static final ExperimentProperties getInstance() {
	        if (ExperimentProperties.singleInstance == null) {
	        	ExperimentProperties.singleInstance = new ExperimentProperties();
	        }
	        return ExperimentProperties.singleInstance;
	 }
	 
    public final String getProperty(String key) {
        String prop = (String) currentProperties.get(key.trim());
        return prop;
    }
    
    public final void setProperty(String key, String value) {
        currentProperties.put(key.trim(), value.trim());
    }
    
    private void loadPropertiesFromFile(String file) {
        File propertiesFile = new File(file);
        try {
            currentProperties.load(new FileInputStream(propertiesFile));
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }
}
