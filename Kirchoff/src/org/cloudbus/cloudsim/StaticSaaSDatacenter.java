package org.cloudbus.cloudsim;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.workloadanalyzer.WorkloadAnalyzer;

/**
 * Implements a Datacenter able to make analysis based in queue models.
 * In this version, we are assuming that all the VMs have the same capacity. 
 * @author rodrigo
 *
 */
public class StaticSaaSDatacenter extends Datacenter {
	
	public static final int SET_QOS = 455666;
	public static final int SET_QOS_ACK = 455667;
	public static final int SERVICE_REQUEST = 455669;
	public static final int SERVICE_REQUEST_ACK = 455670;
	
	String scheduler;
	int vmQueueLength;
	int cloudletLength;
	int fileSize;
	int initialVms;
	double pesMips;

	//structures for control of use of VMs
	Hashtable<String,Integer> queueLength;
	Hashtable<String,Double> lastExecutionTime;
	List<Vm> vmList;
	List<Vm> vmToDestroyList;
	int currentVmName;
	double vmSeconds;
	double lastVmAccounting;
	
	Random generator;

	public StaticSaaSDatacenter(String name, DatacenterCharacteristics characteristics,
								VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList,
								double schedulingInterval, String scheduler, WorkloadAnalyzer analyzer,
								int vmQueueLength, int pesMips, int length, int fileSize) throws Exception {
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
		this.generator = new Random(System.currentTimeMillis());
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
				case CloudSimTags.END_OF_SIMULATION: shutdownEntity(); break;
				default: Log.printLine(this.getName()+": Unknown event: tag "+tag);
			}
		}
	}
	
	@Override
	protected void processVmCreate(SimEvent ev, boolean ack) {
		int numberOfVms = (Integer) ev.getData();
		this.initialVms=numberOfVms;
		createVms(numberOfVms);
		if(ack) sendNow(ev.getSource(),CloudSimTags.VM_CREATE_ACK,true);
	}
	
	@Override
	protected void processVmDestroy(SimEvent ev, boolean ack){
		Log.printLine(CloudSim.clock()+":"+this.getName()+": Broker requested destruction of VMs.");
		
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
	
	protected void processServiceRequest(SimEvent ev) {
		updateCloudletProcessing();
		
		//creates a cloudlet representing the request
		Service service = (Service) ev.getData();
		int serviceId = service.getId();
		int serviceClass = service.getServiceClass();
		
		System.out.println("* "+CloudSim.clock()+":"+this.getName()+": request received.");
		
		int pes=1;
		UtilizationModel utilizationModel = new UtilizationModelFull();
		
		long length = cloudletLength+ Math.round(cloudletLength*generator.nextDouble()*0.1);
		if (serviceClass==Service.SERVICE_CLASS_BIG) length*=2;
		else if (serviceClass==Service.SERVICE_CLASS_SMALL) length/=2;

		int size = fileSize;
		Cloudlet cl = new Cloudlet(serviceId,length,pes,0,size,utilizationModel,utilizationModel,utilizationModel);
		cl.setUserId(ev.getSource());
		int[] data = new int[3];
        data[0] = this.getId();
        data[1] = serviceId;
        int tag = SERVICE_REQUEST_ACK;
        
		//markovian-based submission of cloudlets to vms at any time,
		//each VM has the same chance of receiving the cloudlet
		//int index = generator.nextInt(vmList.size());	
		//Vm vm = vmList.get(index);
        
        //round-robin
        //this.currentVm = (this.currentVm+1)%vmList.size();
        //Vm vm = vmList.get(this.currentVm);
        
        //we always assign to the first element of the list, and put it in the end
        Vm vm = vmList.remove(0);
        vmList.add(vm);
        
		int vmId = vm.getId();
		cl.setVmId(vmId);
		cl.setResourceParameter(getId(), getCharacteristics().getCostPerSecond(), getCharacteristics().getCostPerBw());
		cl.setSubmissionTime(CloudSim.clock());
		
		String vmuid = vm.getUid();
        int currentqueueLength = queueLength.get(vmuid);
		if(currentqueueLength==this.vmQueueLength){//queue is full. Drop
			data[2] = CloudSimTags.FALSE;
		} else {//submit cloudlet to vm
			CloudletScheduler scheduler = vm.getCloudletScheduler();
			double estimatedFinishTime = scheduler.cloudletSubmit(cl,0);
			if(estimatedFinishTime>0.1)	send(this.getId(),estimatedFinishTime,CloudSimTags.VM_DATACENTER_EVENT);
        
			//update hashtable
			int vmQueueLength = queueLength.remove(vmuid);
			vmQueueLength++;
			queueLength.put(vmuid,vmQueueLength);
			data[2] = CloudSimTags.TRUE;
		}
        sendNow(cl.getUserId(), tag, data);
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
			}
			requiredVms--;
		}
	}
	
	protected void setQos(SimEvent ev) {
		double[] params = (double[]) ev.getData();
		double serviceTime = params[0];
		double rejection = params[1];
		Log.printLine("QoS parameters set: Service time="+serviceTime+", Rejection="+rejection);
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
		Log.printLine();
		Log.printLine("======== DATACENTER SUMMARY ========");
		Log.printLine("= Name: "+getName());
		DecimalFormat dft = new DecimalFormat("#####.00");
		double vmhours = vmSeconds/3600;
		Log.printLine("= VM hours: "+dft.format(vmhours));
		Log.printLine("= Min VMs: "+this.initialVms);
		Log.printLine("= Max VMs: "+this.initialVms);
		Log.printLine("========== END OF SUMMARY =========");
	}
}
