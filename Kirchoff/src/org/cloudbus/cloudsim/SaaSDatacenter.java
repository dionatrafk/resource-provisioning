package org.cloudbus.cloudsim;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.forecaster.ForecasterRuntime;
import org.cloudbus.cloudsim.util.IFeedbackable;
import org.cloudbus.cloudsim.util.Requests;
import org.cloudbus.cloudsim.workloadanalyzer.WorkloadAnalyzer;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Implements a Datacenter able to make analysis based in queue models.
 * In this version, we are assuming that all the VMs have the same capacity. 
 * @author rodrigo
 *
 */
public class SaaSDatacenter extends Datacenter {
	
	public static final int SET_QOS = 455666;
	public static final int SET_QOS_ACK = 455667;
	public static final int RECONFIGURE = 455668;
	public static final int SERVICE_REQUEST = 455669;
	public static final int SERVICE_REQUEST_ACK = 455670;
	public static final int LOAD_SNAPSHOT = 777777; 	
	
	WorkloadAnalyzer analyzer;
	PerformanceModeler modeler;
	String scheduler;
	int vmQueueLength;
	int cloudletLength;
	int fileSize;
	int minVms;
	int maxVms;
	int initialVms;
	double pesMips;
	boolean reconfigure;
	boolean qosTrigger = false;
	boolean startedSnapshotEventFirenig = false;
	boolean stopedStanpshotEventFiring = false;
	
	int numberOfRequestsBetweenSnapshotPeriod = 0;

	//structures for control of use of VMs
	Hashtable<String,Integer> queueLength;
	Hashtable<String,Double> lastExecutionTime;
	List<Vm> vmList;
	List<Vm> vmToDestroyList;
	int currentVmName;
	int regressionCounter;
	double vmSeconds;
	double lastVmAccounting;
	double vmCreationDelay;
	
	Random generator;
	//new-------------
	double lastRequestAccounting;
	double lastSchedulingAccounting;
	double lastTrendAccounting;
	double serviceTime;
	double rejectionRate;
	long rejectedRequests;
	long acceptedRequests;
	List<Requests> requestList;
	int trendAnalyse;
	int vmBillingTime;
	int trendSamples;
	Hashtable<String, Double> vmCapacity;
	Hashtable<String, Double> vmBilling;
	
	DescriptiveStatistics qosStatus;
	DescriptiveStatistics idleStatus;
	DescriptiveStatistics acceptionStatus;
	DescriptiveStatistics rejectionStatus;
	//SimpleRegression regression;
	
	public SaaSDatacenter(String name, DatacenterCharacteristics characteristics,
								VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList,
								double schedulingInterval, String scheduler, WorkloadAnalyzer analyzer,
								int vmQueueLength, int pesMips, int length, int fileSize, double vmCreationDelay, int trendAnalyse, int vmBillingTime, int trendSamples) throws Exception {
		super(name,characteristics,vmAllocationPolicy,storageList,schedulingInterval);

		if (vmQueueLength<1) throw new Exception(super.getName() + " : Error - Capacity must be positive.");
		
		this.vmQueueLength = vmQueueLength;
		this.analyzer = analyzer;
		this.scheduler = scheduler;
		this.modeler = new PerformanceModeler(this,analyzer);
		this.queueLength = new Hashtable<String,Integer>();
		this.lastExecutionTime = new Hashtable<String,Double>();
		this.vmList = new LinkedList<Vm>();
		this.vmToDestroyList = new LinkedList<Vm>();
		this.currentVmName = 0;
		this.vmSeconds = 0.0;
		this.lastVmAccounting = 0.0;
		this.cloudletLength=length;
		this.fileSize=fileSize;
		this.pesMips = pesMips;
		this.generator = new Random(System.currentTimeMillis());
		this.reconfigure = true;
		this.initialVms=-1;
		this.minVms = 999999999;
		this.maxVms=-1;
		this.vmCreationDelay = vmCreationDelay;
		this.regressionCounter = 0;
		
		this.lastRequestAccounting = 0.0;
		this.lastSchedulingAccounting = 0.0;
		this.lastTrendAccounting = 0.0;
		
		this.vmCapacity = new Hashtable<String,Double>();
		this.vmBilling = new Hashtable<String,Double>();
		this.vmBillingTime = vmBillingTime;
		this.trendAnalyse = trendAnalyse;
		this.trendSamples = trendSamples;
		
		this.requestList = new LinkedList<Requests>();
		
		this.qosStatus = new DescriptiveStatistics();
		this.idleStatus = new DescriptiveStatistics();
		this.acceptionStatus = new DescriptiveStatistics();
		this.rejectionStatus = new DescriptiveStatistics();
		//this.regression = new SimpleRegression();
		
	}

	/**
	 * Returns the capacity of a VM identified by its ID
	 * @param vmId
	 * @return VM Capacity (in this case, it is equal for all VMs)
	 */
	public int getVmCapacity(String vmId) {
		return vmQueueLength;
	}
	
	@Override
	protected void processOtherEvent(SimEvent ev) {		
		if (ev == null){
			Log.printLine(getName() + ".processOtherEvent(): Error - an event is null.");
		} else {
			int tag = ev.getTag();
			switch(tag){
				case SET_QOS: setQos(ev); break;
				case SERVICE_REQUEST: processServiceRequest(ev); break;
				case RECONFIGURE: try {
					processReconfigure();
					
				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (REXPMismatchException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} break;
				case CloudSimTags.END_OF_SIMULATION: shutdownEntity(); break;
				case LOAD_SNAPSHOT: 
				{
					//TODO: The eventhandler for timing should be called here.                                            <==============================
					processSnapshotEvent();
				}
				default: Log.printLine(this.getName()+": Unknown event: tag "+tag);
			}
		}
	}
	
	@Override
	protected void processVmCreate(SimEvent ev, boolean ack) {
		int numberOfVms = (Integer) ev.getData();
		if (this.initialVms==-1){
			this.initialVms=numberOfVms;
			send(this.getId(),super.getSchedulingInterval(),RECONFIGURE, 1);
			    
		}
		createVms(numberOfVms);
		if(ack) sendNow(ev.getSource(),CloudSimTags.VM_CREATE_ACK, true);

	}
	
	@Override
	protected void processVmDestroy(SimEvent ev, boolean ack){
		Log.printLine(CloudSim.clock()+":"+this.getName()+": Broker requested destruction of VMs.");
		reconfigure=false;
		
		ArrayList<String> toDestroy = new ArrayList<String>();
		List<? extends Host> list = getVmAllocationPolicy().getHostList();
		for (int i = 0; i < list.size(); i++) {
			Host host = list.get(i);
			for (Vm vm : host.getVmList()) {
				toDestroy.add(vm.getUid());
			}
		}
		for(String uid:toDestroy){
			destroyVm(uid);
		}
	}
	
	protected void processSnapshotEvent()
	{
		Log.printLine(CloudSim.clock() + ":" + this.getName() + ": Processing snapshot event: the current load is: " + numberOfRequestsBetweenSnapshotPeriod);
		
		if(analyzer instanceof IFeedbackable)
		{
			Log.printLine(CloudSim.clock() + ":" + this.getName() + ": Sending feedback to analyzer with value of: " +  numberOfRequestsBetweenSnapshotPeriod);
			((IFeedbackable)analyzer).AddKnownDataLoadEntry(numberOfRequestsBetweenSnapshotPeriod);
		}
		
		numberOfRequestsBetweenSnapshotPeriod = 0;
		
		if(!stopedStanpshotEventFiring)
			send(this.getId(), super.getSchedulingInterval(), SaaSDatacenter.LOAD_SNAPSHOT);
			//send(this.getId(), 600, SaaSDatacenter.LOAD_SNAPSHOT);
	}
	
	protected void processServiceRequest(SimEvent ev) {

		
		numberOfRequestsBetweenSnapshotPeriod++;
//		
		if(!startedSnapshotEventFirenig)
		{
			Log.printLine(CloudSim.clock() + ":" + this.getName() + ": Starting auto event firing of LoadSnapshot");
			startedSnapshotEventFirenig = true; 
//			//send(this.getId(), 600, SaaSDatacenter.LOAD_SNAPSHOT);
			send(this.getId(), super.getSchedulingInterval(), SaaSDatacenter.LOAD_SNAPSHOT);
//			
		}
		
		
		//creates a cloudlet representing the request
		Service service = (Service) ev.getData();
		int serviceId = service.getId();
		//data by minute
		if (CloudSim.clock() > this.lastRequestAccounting ) {
        	
			double vmListCapacity = (vmList.size() * (60 / this.serviceTime));
			double requestsReceived = acceptedRequests + rejectedRequests;
			double qos = 1-(vmListCapacity/requestsReceived); //percentage of refused requests]
			if (qos < 0) qos = 0.0;
			double idle = vmListCapacity - acceptedRequests; // if positive vm idle id negative vm faulth
			if (idle < 0) idle = 0.0;
			DecimalFormat percent = new DecimalFormat("##0.00");
			System.out.println(CloudSim.clock()+": Process Request: accepted: "+acceptedRequests+" rejected: "+rejectedRequests + " Idle: "+idle +  " Qos: "+percent.format(qos));
        	
			Requests requests = new Requests(CloudSim.clock(), acceptedRequests,rejectedRequests);
			this.requestList.add(requests);
						
			this.lastRequestAccounting = CloudSim.clock();
        				
			acceptionStatus.addValue(acceptedRequests);
			rejectionStatus.addValue(rejectedRequests);
			        	
        	qosStatus.addValue(qos);
        	idleStatus.addValue(idle);
        	System.out.println(CloudSim.clock()+": REQUEST MIN: "+requestList.size()); 
        
        	// reset forecast model
        	if (requestList.size() % 1000 == 0) {
        	//just for mlp model
        		ForecasterRuntime  reset = new ForecasterRuntime();
        		//reset.resetModel();
        		System.out.println(CloudSim.clock()+" Reset Model: " + reset.resetModel() );
        	}
        	

        	//Trigger the Qos parameter to reconfigure the infrastructure
        	if(qosStatus.getSum() >= rejectionRate ) {
        		
        		qosTrigger = true;
        		System.out.println(CloudSim.clock()+": Qos Trigger "); 
        		send(this.getId(),0,RECONFIGURE);
        		
        	}
        
        	//---------------------------------------------------------

			// reset values for new minute
			rejectedRequests = 0;
        	acceptedRequests = 0;
        	
        	for (int i=0; i < this.vmList.size();i++) {
        		String vmuid = vmList.get(i).getUid();
        		vmCapacity.put(vmuid,0.0);
        	}	      	

        }
        
		int[] data = new int[3];
        data[0] = this.getId();
        data[1] = serviceId;
        int tag = SERVICE_REQUEST_ACK;
        
               
   
        //we always assign to the first element of the list, and put it in the end
        Vm vm = vmList.remove(0);
        vmList.add(vm);
        
		String vmuid = vm.getUid();
		
		double processingBuffer  = vmCapacity.get(vmuid);
		processingBuffer +=  this.serviceTime; 
				
		// buffer length      
		if(processingBuffer > 60){//queue is full. Drop
			data[2] = CloudSimTags.FALSE;
			rejectedRequests++;
		} else {//submit request to vm
			
			vmCapacity.put(vmuid,processingBuffer);
			data[2] = CloudSimTags.TRUE;
			acceptedRequests++;
		}
			
        //sendNow(cl.getUserId(), tag, data);
		sendNow(ev.getSource(), tag, data);
	}

	protected void processReconfigure() throws RserveException, REXPMismatchException{
		
		//update accounting of use of vms
		if (reconfigure) {
			updateVmUsage();
			
			double rejections = 0;
			double acceptions = 0;
			double minutes=0;
		
			acceptions = acceptionStatus.getSum();
			rejections = rejectionStatus.getSum();
			minutes = acceptionStatus.getN();
			
			
			//prepare data for prediction
						
			acceptionStatus.clear();
			rejectionStatus.clear();
			
			double interval = minutes;
	       	//double interval = super.getSchedulingInterval()/60;
			double requestsReceived = acceptions + rejections;
			//double vmCapacity = interval / this.serviceTime;			
			//the VM capacity  by 1 minute.
			double vmCapacity = 60 / this.serviceTime;
			//double vmListCapacity = (vmList.size() * (interval / this.serviceTime));
			double vmListCapacity = (vmList.size() * vmCapacity);
			
			double idle = vmListCapacity - (acceptions/interval);	
			//percentage of refused requests
			double qos = ((rejections * 100 )/requestsReceived); 

			double futureRequestsReceived = analyzer.getEstimatedArrivalRate(CloudSim.clock());
			
			DecimalFormat percent = new DecimalFormat("##0.00");
			DecimalFormat dft = new DecimalFormat("#####.00");
			double vmhours = vmSeconds/3600;
				
			double requestPerMinute = requestEstimate(futureRequestsReceived);
			//double requestPerMinute  = futureRequestsReceived / predictionInterval;
			// whether trend analyzes do not start the reconfiguring scheduling.
			//no trend analyze and dont activate when the process was called by idleness
			if((this.trendAnalyse == 0) && (qosTrigger == false)){
				send(this.getId(),super.getSchedulingInterval(),RECONFIGURE);
			}
			qosTrigger = false;

			int requiredVms = estimateVms(requestPerMinute, vmCapacity);
			
			int vmsDestroy = 0;
			Log.printLine(CloudSim.clock()+":Process Reconfigure: " +"Total Request: "+ requestsReceived+" Future Request: "+ futureRequestsReceived+ " accepted: "+ acceptions+ " rejected: "+ rejections+
					" idle: "+ dft.format(idle)+" Qos: "+ percent.format(qos)+ " Vms_capacity: "+ vmList.size()+" VMs_hours: "+ 
					dft.format(vmhours));
			Log.printLine(CloudSim.clock()+":Request per min: " +requestPerMinute);		
			
			//double requiredVms = 1;
			if ((qos >= rejectionRate) || (requiredVms >= vmList.size())){
				
				if (qos >= rejectionRate) qosStatus.clear();
				//qosStatus.clear();
			      if ((requiredVms-vmList.size()) >=1.0) {
					send(this.getId(),this.vmCreationDelay ,CloudSimTags.VM_CREATE, requiredVms - vmList.size());
					Log.printLine(CloudSim.clock()+":Reconfigure VM Create request: " +(requiredVms - vmList.size()));
				  }
				
			} else {	
				// idle
				if ((idleStatus.getMean() > (vmCapacity)) && (idleStatus.getN()> 10)) {
					
					
					vmsDestroy = vmList.size() - requiredVms;
					idleStatus.clear();
					
					Log.printLine(CloudSim.clock()+":Reconfigure: Idle CLEAR");
//					if (vmsDestroy < 1)
//						vmsDestroy =1;
//				
//					if (vmsDestroy >=1) {
//						for (int i=0; i < vmsDestroy;i++) {
//							destroyVm(vmList.get(0).getUid());
//						}
//					}
						//billing 1 hour
						for (int i=0; i < vmList.size();i++) {
							
							if (vmsDestroy >=1) {
								if (this.vmBillingTime >0) {
									if ((CloudSim.clock() - vmBilling.get(vmList.get(i).getUid()) >= this.vmBillingTime)){
										try {							
											destroyVm(vmList.get(i).getUid());  //com billing
											vmsDestroy--;
										} catch (Exception e) {
											// TODO: handle exception
										}	
									}
								}
								else {
									destroyVm(vmList.get(0).getUid());	
									vmsDestroy--;
								}
								
								
							}
						}
						
				}
			}
			
		}		
	}	
	
	protected double requestEstimate(double futureRequestsReceived) {
		//ArrayList<Long> requests = new ArrayList<Long>();
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		//didi
				
		Double  requestMin= 0.0;
		
		for (int i=0; i < requestList.size();i++) {
			
			if (requestList.get(i).getCurrentTime()>= (CloudSim.clock() - super.getSchedulingInterval())){
				
				stats.addValue(requestList.get(i).getAccepted() + requestList.get(i).getRejected());
				
			}	
		}
		
		Double Q1 = stats.getPercentile(25);
		Double Q2 = stats.getPercentile(50);	
		Double Q3 = stats.getPercentile(75);
		Double Q4 = stats.getPercentile(100);
		Double sum = stats.getSum();
		Double std = stats.getStandardDeviation();
		Double difference =  Math.abs(futureRequestsReceived - sum);
		
		if (std > 10.0) {
			
			if (futureRequestsReceived > sum) { 
				Double percent = ((difference*100)/ futureRequestsReceived);
				requestMin = Q3 + (Q4 *(percent / 100));
				System.out.println(CloudSim.clock()+": r 1");
			}else {
				Double percent = ((difference*100)/ sum);
				requestMin = Q4 - (Q4*(percent / 100));
				System.out.println(CloudSim.clock()+": r 2");
			}
			
		} else {
			
			if (futureRequestsReceived > sum) { 
				Double percent = ((difference*100)/ futureRequestsReceived);
				//requestMin = Q2 + (Q2*(percent / 100));
				requestMin = Q3 + (Q3*(percent / 100));
				System.out.println(CloudSim.clock()+": r 3");
			}else {
				Double percent = ((difference*100)/ sum);
				if (percent > 45.0)
					requestMin = Q4 - (Q4*(percent / 100));
				else
					requestMin = Q2 - (Q2*(percent / 100));
				
				System.out.println(CloudSim.clock()+": r 4");
			}
			
		}
		
		return requestMin;
	}
	

	@Override
	protected void updateCloudletProcessing() {
		//if some time passed since last processing
		if (CloudSim.clock() > this.getLastProcessTime()+0.5) {
			List<? extends Host> list = getVmAllocationPolicy().getHostList();
			double smallerTime = Double.MAX_VALUE;
			//for each host...
			for (int i = 0; i < list.size(); i++) {
				Host host = list.get(i);
				double time = host.updateVmsProcessing(CloudSim.clock());//inform VMs to update processing
				//what time do we expect that the next cloudlet will finish?
				if (time < smallerTime) {
					smallerTime = time;
				}
			}
			//schedules an event to the next time, if valid
			if (smallerTime<=CloudSim.clock()+1.0) smallerTime=CloudSim.clock()+1.0;
			if (smallerTime != Double.MAX_VALUE) {
				schedule(getId(), (smallerTime - CloudSim.clock()), CloudSimTags.VM_DATACENTER_EVENT);
			} 
			setLastProcessTime(CloudSim.clock());
		}
	}

	@Override
	protected void checkCloudletCompletion() {
		List<? extends Host> list = getVmAllocationPolicy().getHostList();
		ArrayList<String> toDestroy = new ArrayList<String>();//avoid concurrent modification to hostlist 
		for (int i = 0; i < list.size(); i++) {
			Host host = list.get(i);
			for (Vm vm : host.getVmList()) {
				while (vm.getCloudletScheduler().isFinishedCloudlets()){
					Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
					if (cl != null) {
						//one less cloudlet in the VM: update information
						int length = queueLength.remove(vm.getUid());
						length--;
						queueLength.put(vm.getUid(), length);
						if (length==0&&vmIsInList(vm.getUid(),this.vmToDestroyList)){
							toDestroy.add(vm.getUid());
						} else {
							lastExecutionTime.remove(vm.getUid());
							lastExecutionTime.put(vm.getUid(),cl.getFinishTime()-cl.getExecStartTime());
						}
						sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
					}
				}
			}
		}
		for(String uid:toDestroy){
			destroyVm(uid);
		}
	}
	
	private boolean vmIsInList(String uid,List<Vm> list) {		
		for (Vm vm:list){
			if (vm.getUid().equals(uid)) return true;
		}	
		return false;
	}

	protected void destroyVm(String uid){
		//we only destroy the VM if it is idle. Otherwise, we mark for destruction and
		//stop sending jobs to it. But it is only destroyed when current jobs finish.
		vmBilling.remove(uid);
		
		updateVmUsage();
		Vm removedVm = null;
			
		if(!vmIsInList(uid,vmToDestroyList)){
			//it is an active vm. Remove it from the list
			for(int i=0;i<vmList.size();i++){
				if (uid.equals(vmList.get(i).getUid())){
					removedVm = vmList.remove(i);
					break;
				}
			}
			lastExecutionTime.remove(uid);
		}
		
		int length = queueLength.remove(uid);
		if(length==0){//vm was idle
			Log.printLine(CloudSim.clock()+":"+this.getName()+": Destroying VM "+uid);
			modeler.removeVm(uid);
			if (removedVm==null){//vm was in destroy list and now is idle. Remove from list
				for(int i=0;i<vmToDestroyList.size();i++){
					if (uid.equals(vmToDestroyList.get(i).getUid())){
						removedVm = vmToDestroyList.remove(i);
						break;
					}
				}
			}
			
			//destroy Vm in the host
			getVmAllocationPolicy().deallocateHostForVm(removedVm);
			
			//remove vm from parent datacenter list
			int size = getVmList().size();
			for(int i=0;i<size;i++){
				if (getVmList().get(i).equals(uid)){
					getVmList().remove(i);
					break;
				}
			}
		} else {//vm still have some jobs to process
			queueLength.put(uid,length);
			Log.printLine(CloudSim.clock()+":"+this.getName()+": Sentencing VM "+uid);
			if(removedVm!=null) {//vm was in the vmlist. Put it in destroy list
				vmToDestroyList.add(removedVm);
			}
		}
		
		if (reconfigure && minVms>(vmList.size()+vmToDestroyList.size())) minVms=vmList.size()+vmToDestroyList.size();
		
		if (vmList.isEmpty())
		{
			stopedStanpshotEventFiring = true;
			Log.printLine(CloudSim.clock() + ":" + this.getName() + ": Stopping auto event firing of LoadSnapshot");
		}
	}
	
	protected void createVms(int requiredVms){
		//first, we check if there are VMs marked for destruction that still on
		//if there are, we remove it from destruction list and restart sending jobs
		//to it. If there are not, we create new VMs.
		while (!vmToDestroyList.isEmpty()&&requiredVms>0){
			Vm vm = vmToDestroyList.remove(0);
			vmList.add(0,vm);
			Log.printLine(CloudSim.clock()+":"+this.getName()+": Sparing VM "+vm.getUid());
			lastExecutionTime.put(vm.getUid(),modeler.getCurrentExecutionTime());
			requiredVms--;
		}
		
		while(requiredVms>0){
			//create a new VM
			int pesNumber = 1;
			int ram = 2048;
			long bw = 1;
			long size = 10000;
			String vmm = "Xen";

			Vm vm = null;
			if (scheduler.equalsIgnoreCase("spaceshared")) vm = new Vm(currentVmName,this.getId(),pesMips,pesNumber,ram,bw,size,vmm,new CloudletSchedulerSpaceShared());
			else if (scheduler.equalsIgnoreCase("timeshared")) vm = new Vm(currentVmName,this.getId(),pesMips,pesNumber,ram,bw,size,vmm,new CloudletSchedulerTimeShared());
						
			currentVmName++;
			String uid = vm.getUid();
			
			if (getVmAllocationPolicy().allocateHostForVm(vm)){
				Log.printLine(CloudSim.clock()+":"+this.getName()+": Creating VM "+uid);
				queueLength.put(uid,0);
				vmList.add(0,vm);
				lastExecutionTime.put(uid,0.0);
				getVmList().add(vm);
				vm.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(vm).getVmScheduler().getAllocatedMipsForVm(vm));
				
				vmBilling.put(uid,CloudSim.clock());	
			
			}
			requiredVms--;
		}
		
		if (maxVms<(vmList.size()+vmToDestroyList.size())) maxVms=vmList.size()+vmToDestroyList.size();
	}
	
	@SuppressWarnings("deprecation")
	protected void setQos(SimEvent ev) {
		double[] params = (double[]) ev.getData();
		this.serviceTime = params[0];
		this.rejectionRate = params[1];
		Log.printLine("QoS parameters set: Service time="+this.serviceTime+", Rejection="+this.rejectionRate);
		modeler.setQos(this.serviceTime,this.rejectionRate);
		sendNow(ev.getSource(),SaaSDatacenter.SET_QOS_ACK,new Boolean(true));
	}
	
	
	
	protected int estimateVms(double requestRate, double vmCapacity) {
		int vms = 0;
		double totalCapacity = 0;
		do {
			vms++;
			totalCapacity+= vmCapacity;
			//requests = requestRate / totalCapacity;
			
		
		}while(totalCapacity <= requestRate );
		
		return vms;
		
		
	}
	protected void processReconfigure_old() throws RserveException, REXPMismatchException{
		if(reconfigure){
			//update accounting of use of vms
			updateVmUsage();

			//update solver with VMs performance
			for (Vm vm:vmList){
				modeler.setVmQueueSize(vm.getUid(),queueLength.get(vm.getUid()));
				modeler.setVmExecutionTime(vm.getUid(),lastExecutionTime.get(vm.getUid()));
			}

			//run the solver
			int requiredVms =  modeler.solve(analyzer.getEstimatedArrivalRate(CloudSim.clock()));
			Log.printLine(CloudSim.clock()+": processReconfigure(): solver required "+requiredVms+" virtual machines.");
			if (requiredVms!=vmList.size()){//number of VMs has to change...
				if (requiredVms<vmList.size()){
					do {
						//look for least used vm
						int leastUsed = 9999999;
						String leastUsedId = null;
						Enumeration<String> ids = queueLength.keys();
						//send(this.getId(),this.vmCreationDelay,CloudSimTags.VM_CREATE_ACK, 1);
						while(ids.hasMoreElements()){
							String uid = ids.nextElement();
							if (vmIsInList(uid,vmList)&&queueLength.get(uid)<leastUsed){
								leastUsed=queueLength.get(uid);
								leastUsedId=uid;
							}
						}
						destroyVm(leastUsedId);
					} while(requiredVms<vmList.size());
				} else {
												
					send(this.getId(),this.vmCreationDelay ,CloudSimTags.VM_CREATE, requiredVms - vmList.size());
			
					Log.printLine(CloudSim.clock()+": " +String.valueOf(requiredVms-vmList.size())+ " VM creation requested");

				}
			}
			double nextEvent=analyzer.delayToNextChangeInModel(CloudSim.clock());
			
			send(this.getId(),Math.max(nextEvent,super.getSchedulingInterval()),RECONFIGURE);
		}
	}

	private void updateVmUsage() {
		double currentTime = CloudSim.clock();
		double interval = currentTime-lastVmAccounting;
		if(interval>1.0){
			int machines = vmList.size()+vmToDestroyList.size();
			this.vmSeconds += (machines*interval); 
			this.lastVmAccounting=currentTime;
		}
	}
	
	public void printSummary(){
		
		double acceptions=0;
		double rejections=0;
		for (int i=0; i < requestList.size();i++) {
			acceptions += requestList.get(i).getAccepted();
			rejections += requestList.get(i).getRejected();
		}
		double generatedRequest = acceptions+rejections;
		
		double qos = ((rejections * 100 )/generatedRequest); //percentage of refused requests
		DecimalFormat percent = new DecimalFormat("##0.00");
		
		if(minVms==999999999) minVms=this.initialVms;
		Log.printLine();
		Log.printLine("======== DATACENTER SUMMARY ========");
		Log.printLine("Name: "+getName());
		DecimalFormat dft = new DecimalFormat("#####.00");
		double vmhours = vmSeconds/3600;
		Log.printLine("VM hours: "+dft.format(vmhours));
		Log.printLine("Min VMs: "+minVms);
		Log.printLine("Max VMs: "+maxVms);
				
		Log.printLine("QoS violations: "+percent.format(qos));
		Log.printLine("Accepted: "+acceptions);
		Log.printLine("Rejected: "+rejections);
		Log.printLine("Total: "+(acceptions+rejections));
		Log.printLine("========== END OF SUMMARY =========");
	}
}
