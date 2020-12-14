package org.cloudbus.cloudsim;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.util.Requests;
import org.cloudbus.cloudsim.workloadanalyzer.WorkloadAnalyzer;


public class ReactiveSaaSDatacenter extends Datacenter {
	
	/**
	 * TODO define policies for reactiveness:
	 * --> when new vms are created?
	 * 1. When total accumulated rejections > QoS
	 * 2. When rejection in last X minutes > QoS
	 * 2. When rejection in last measurement interval > QoS
	 * --> when VMs are destroyed?
	 * 1. When utilization in last X minutes < threshold
	 * 1. When utilization in last measurement interval < threshold
	 * 
	 * when system decides to create vms, send a message to itself:
	 * tag=VM_CREATE delay=vmCreationDelay message=numVms ack=false
	 */

	public static final int SET_QOS = 455666;
	public static final int SET_QOS_ACK = 455667;
	public static final int SERVICE_REQUEST = 455669;
	public static final int SERVICE_REQUEST_ACK = 455670;
	public static final int RECONFIGURE = 455668;
	
	String scheduler;
	int vmQueueLength;
	int cloudletLength;
	int fileSize;
	int minVms;
	int maxVms;
	int initialVms;
	double vmCreationDelay;
	double pesMips;
	//double processingBuffer;
	double serviceTime;
	double rejectionRate;
	double lastRequestAccounting;
	//structures for control Vms use
	Hashtable<String,Integer> queueLength;
	Hashtable<String,Double> lastExecutionTime;
	
	Hashtable<String, Double> vmCapacity;
	
	List<Vm> vmList;
	List<Vm> vmToDestroyList;
	int currentVmName;
	double vmSeconds;
	double lastVmAccounting;
	boolean reconfigure;
	long rejectedRequests;
	long acceptedRequests;
	int vmBillingTime;
	
	Hashtable<String, Double> vmBilling;
	DescriptiveStatistics qosStatus;
	DescriptiveStatistics idleStatus;
	DescriptiveStatistics acceptionStatus;
	DescriptiveStatistics rejectionStatus;
	
	List<Requests> requestList;
	
	Random generator;

	public ReactiveSaaSDatacenter(String name, DatacenterCharacteristics characteristics,
								VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList,
								double schedulingInterval, String scheduler, WorkloadAnalyzer analyzer,
								int vmQueueLength, int pesMips, int length, int fileSize, double vmCreationDelay, int vmBillingTime) throws Exception {
		super(name,characteristics,vmAllocationPolicy,storageList,schedulingInterval);

		if (vmQueueLength<1) throw new Exception(super.getName() + " : Error - Capacity must be positive.");
		
		this.vmQueueLength = vmQueueLength;
		this.scheduler = scheduler;
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
		this.reconfigure = true;
		this.initialVms=-1;
		this.minVms = 999999999;
		this.maxVms=-1;
		this.vmCreationDelay = vmCreationDelay;
		this.generator = new Random(System.currentTimeMillis());
		
		//new---------------
		this.lastRequestAccounting = 0.0;
		this.vmCapacity = new Hashtable<String,Double>();
		this.vmBilling = new Hashtable<String,Double>();
		this.requestList = new LinkedList<Requests>();
		this.qosStatus = new DescriptiveStatistics();
		this.idleStatus = new DescriptiveStatistics();
		this.acceptionStatus = new DescriptiveStatistics();
		this.rejectionStatus = new DescriptiveStatistics();
		this.vmBillingTime = vmBillingTime;
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
				case RECONFIGURE: processReconfigure(); break;
				case CloudSimTags.END_OF_SIMULATION: shutdownEntity(); break;
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
		if(ack) sendNow(ev.getSource(),CloudSimTags.VM_CREATE_ACK,true); //send(ev.getSource(),this.vmCreationDelay,CloudSimTags.VM_CREATE_ACK); // 
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
	
	protected void processReconfigure() {
		//update accounting of use of vms
		
		if (reconfigure) {
			updateVmUsage();
			
			double rejections = 0;
			double acceptions = 0;
			double minutes=0;
					
			acceptions = acceptionStatus.getSum();
			rejections = rejectionStatus.getSum();
			minutes = acceptionStatus.getN();
			
			acceptionStatus.clear();
			rejectionStatus.clear();
			
			double interval = minutes;
	       	//double interval = super.getSchedulingInterval()/60;
			double requestsReceived = acceptions + rejections;
			double vmCapacity = 60 / this.serviceTime;			
			double vmListCapacity = (vmList.size() * vmCapacity);

			double idle = vmListCapacity - (acceptions/interval);	
			
			double qos = ((rejections * 100 )/requestsReceived); //percentage of refused requests
			if (Double.isNaN(idle)) idle = 0.0;
			if (Double.isNaN(qos)) qos = 0.0;
			
			DecimalFormat percent = new DecimalFormat("##0.00");
			DecimalFormat dft = new DecimalFormat("#####.00");
			double vmhours = vmSeconds/3600;
						
			
			Log.printLine(CloudSim.clock()+":Process Reconfigure: " +"Total Request: "+ requestsReceived+ " accepted: "+ acceptions+ " rejected: "+ rejections+
					" idle: "+ dft.format(idle)+" Qos: "+ percent.format(qos)+ " Vms_capacity: "+ vmList.size()+" VMs_hours: "+ 
					dft.format(vmhours));
						
			double requestPerMinute = requestEstimate(); //this one
			
			
			int requiredVms = estimateVms(requestPerMinute, vmCapacity);
			double vmsDestroy = 0;
			
			if (qos >= rejectionRate) {
				qosStatus.clear();
				//if ((requiredVms-vmList.size()) >=1.0) {
					send(this.getId(),this.vmCreationDelay ,CloudSimTags.VM_CREATE, requiredVms);
					Log.printLine(CloudSim.clock()+":Reconfigure VM Create request: " +(requiredVms));
				//}
				
			}else {
				
				if ((idleStatus.getMean() > (vmCapacity)) && (idleStatus.getN()> 10)) {
					vmsDestroy = vmList.size() - requiredVms;
					idleStatus.clear();
					//billing of 1 hour
					for (int i=0; i < vmList.size();i++) {
						
						if (vmsDestroy >=1) {
							if (this.vmBillingTime > 0) {
								if ((CloudSim.clock() - vmBilling.get(vmList.get(i).getUid()) >= this.vmBillingTime)){
									try {
									destroyVm(vmList.get(i).getUid());  //with billing
									vmsDestroy--;
									}catch (Exception e) {
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
	
	
	// this method take the values from 1 hour ago and take the average as result
	protected double requestEstimate() {
		
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
				
		Double  requestMin= 0.0;
		
		for (int i=0; i < requestList.size();i++) {
			
			if (requestList.get(i).getCurrentTime()>= (CloudSim.clock() - 3600)){
				
				stats.addValue(requestList.get(i).getAccepted() + requestList.get(i).getRejected());
				
			}	
		} 
		
		Double sum = stats.getSum();
		requestMin = sum/60;
		System.out.println(CloudSim.clock()+": request min: "+requestMin);	
		
		return requestMin;
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

	protected void processServiceRequest(SimEvent ev) {
		//updateCloudletProcessing();
		 
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
			if(idle < 0) idle = 0.0;
			DecimalFormat percent = new DecimalFormat("##0.00");
			System.out.println(CloudSim.clock()+": Process Request: accepted: "+acceptedRequests+" rejected: "+rejectedRequests + " Idle: "+idle +  " Qos: "+percent.format(qos));
        	
			Requests requests = new Requests(CloudSim.clock(), acceptedRequests,rejectedRequests);
			this.requestList.add(requests);
						
			this.lastRequestAccounting = CloudSim.clock();
        	
			
			acceptionStatus.addValue(acceptedRequests);
			rejectionStatus.addValue(rejectedRequests);
			        	
        	qosStatus.addValue(qos);
        	idleStatus.addValue(idle);
        	
        	//Trigger the Qos parameter to reconfigure the infrastructure
        	if(qosStatus.getSum() >= rejectionRate ) {
        		send(this.getId(),0,RECONFIGURE);

        	}
    	 
        	if (idleStatus.getN()> 10) {
    	 
        		if (idleStatus.getMean()> (60/this.serviceTime)) {
    			send(this.getId(),0,RECONFIGURE);
    			
        		}
        	}
        	//---------------------------------------------------------
        	
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
	

	@Override
	protected void updateCloudletProcessing() {
//		We are not using cloudlets in this simulation
		//if some time passed since last processing
//		if (CloudSim.clock() > this.getLastProcessTime()+0.5) {
//			List<? extends Host> list = getVmAllocationPolicy().getHostList();
//			double smallerTime = Double.MAX_VALUE;
//			//for each host...
//			for (int i = 0; i < list.size(); i++) {
//				Host host = list.get(i);
//				double time = host.updateVmsProcessing(CloudSim.clock());//inform VMs to update processing
//				//what time do we expect that the next cloudlet will finish?
//				if (time < smallerTime) {
//					smallerTime = time;
//				}
//			}
//			//schedules an event to the next time, if valid
//			if (smallerTime<=CloudSim.clock()+1.0) smallerTime=CloudSim.clock()+1.0;
//			if (smallerTime != Double.MAX_VALUE) {
//				schedule(getId(), (smallerTime - CloudSim.clock()), CloudSimTags.VM_DATACENTER_EVENT);
//			} 
//			setLastProcessTime(CloudSim.clock());
//		}
	}

	@Override
	protected void checkCloudletCompletion() {
//		We are not using cloudlets in this simulation		
//		List<? extends Host> list = getVmAllocationPolicy().getHostList();
//		ArrayList<String> toDestroy = new ArrayList<String>();//avoid concurrent modification to hostlist 
//		for (int i = 0; i < list.size(); i++) {
//			Host host = list.get(i);
//			for (Vm vm : host.getVmList()) {
//				while (vm.getCloudletScheduler().isFinishedCloudlets()){
//					Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
//					if (cl != null) {
//						//one less cloudlet in the VM: update information
//						int length = queueLength.remove(vm.getUid());
//						length--;
//						queueLength.put(vm.getUid(), length);
//						if (length==0&&vmIsInList(vm.getUid(),this.vmToDestroyList)){
//							toDestroy.add(vm.getUid());
//						} else {
//							lastExecutionTime.remove(vm.getUid());
//							lastExecutionTime.put(vm.getUid(),cl.getFinishTime()-cl.getExecStartTime());
//						}
//						sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
//					}
//				}
//			}
//		}
//		for(String uid:toDestroy){
//			destroyVm(uid);
//		}
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
	}
	
	protected void createVms(int requiredVms){	
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
	}
	
	@SuppressWarnings("deprecation")
	protected void setQos(SimEvent ev) {
		double[] params = (double[]) ev.getData();
		this.serviceTime = params[0];
		this.rejectionRate = params[1];
		Log.printLine("QoS parameters set: Service time="+this.serviceTime+", Rejection="+this.rejectionRate);
		sendNow(ev.getSource(),StaticSaaSDatacenter.SET_QOS_ACK,new Boolean(true));
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
		Log.printLine(" Name: "+getName());
		DecimalFormat dft = new DecimalFormat("#####.00");
		double vmhours = vmSeconds/3600;
		Log.printLine(" VM hours: "+dft.format(vmhours));
		Log.printLine(" Min VMs: "+minVms);
		Log.printLine(" Max VMs: "+maxVms);
				
		Log.printLine(" QoS violations: "+percent.format(qos));
		Log.printLine(" Accepted: "+acceptions);
		Log.printLine(" Rejected: "+rejections);
		Log.printLine(" Total: "+(acceptions+rejections));
		Log.printLine("========== END OF SUMMARY =========");
	}
}
