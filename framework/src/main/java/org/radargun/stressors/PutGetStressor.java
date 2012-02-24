package org.radargun.stressors;

//import autonomic.SelfTuner;
//import autonomic.SelfTunerFactory;
import com.sun.corba.se.spi.orbutil.threadpool.ThreadPoolChooser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.radargun.CacheWrapper;
import org.radargun.CacheWrapperStressor;
import org.radargun.tpcc.ElementNotFoundException;
import org.radargun.tpcc.TPCCPopulation;
import org.radargun.tpcc.TPCCTerminal;
import org.radargun.tpcc.TPCCTools;
import org.radargun.utils.Utils;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.transaction.RollbackException;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * On multiple threads executes put and get operations against the CacheWrapper, and returns the result as an Map.
 *
 * @author Mircea.Markus@jboss.com
 */
public class PutGetStressor implements CacheWrapperStressor {

   private static Log log = LogFactory.getLog(PutGetStressor.class);

   private int opsCountStatusLog = 5000;

   public static final String DEFAULT_BUCKET_PREFIX = "BUCKET_";
   public static final String DEFAULT_KEY_PREFIX = "KEY_";


   /**
    * total number of operation to be made against cache wrapper: reads + writes
    */
   private int numberOfRequests = 5000;

   /**
    * for each there will be created fixed number of keys. All the GETs and PUTs are performed on these keys only.
    */
   private int numberOfKeys = 1000;

   /**
    * Each key will be a byte[] of this size.
    */
   private int sizeOfValue = 1000;

   /**
    * Out of the total number of operations, this defines the frequency of writes (percentage).
    */
   private int writePercentage = 20;


   /**
    * the number of threads that will work on this cache wrapper.
    */
   private int numOfThreads = 1;
   
   private boolean forceGlobalWrite;

   private String bucketPrefix = DEFAULT_BUCKET_PREFIX;



   private CacheWrapper cacheWrapper;
   private static Random r = new Random();
   private long startTime;
   private volatile CountDownLatch startPoint;


   private boolean is_master=false;
   private int total_num_of_slaves;
   private int lower_bound_op;
   private int upper_bound_op;
   private long simulTime;

   //private SelfTuner st;

   private double lambda=0.0;
   private BlockingQueue<RequestType> queue;
   private AtomicLong completedThread;
   private Producer[] producers;
   private AtomicLong countJobs;

   private int slaveIndex;



   public PutGetStressor(boolean b, int slaves, int lb, int ub, long simulTime, double lambda, double paymentWeight, double orderStatusWeight, int cLastMask, int olIdMask, int cIdMask, double overlap){
      TPCCTerminal.PAYMENT_WEIGHT=paymentWeight;
      TPCCTerminal.ORDER_STATUS_WEIGHT=orderStatusWeight;

      TPCCTools.A_C_LAST=cLastMask;
      TPCCTools.A_OL_I_ID=olIdMask;
      TPCCTools.A_C_ID=cIdMask;

      TPCCTools.OVERLAP=overlap;


      this.is_master=b;
      this.total_num_of_slaves=slaves;      //This variable was needed for a previous test
      this.lower_bound_op=lb;
      this.upper_bound_op=ub;
      this.simulTime=simulTime;

      this.lambda=lambda;


      completedThread= new AtomicLong(0L);

      if(lambda!=0.0){     //Open system
             queue=new ArrayBlockingQueue<RequestType>(7000);
             countJobs = new AtomicLong(0L);
             producers =new Producer[3];
             producers[0]=new Producer(TPCCTerminal.NEW_ORDER, false, 100.0-(TPCCTerminal.PAYMENT_WEIGHT+TPCCTerminal.ORDER_STATUS_WEIGHT));
             producers[1]=new Producer(TPCCTerminal.PAYMENT, false, TPCCTerminal.PAYMENT_WEIGHT);
             producers[2]=new Producer(TPCCTerminal.ORDER_STATUS, true, TPCCTerminal.ORDER_STATUS_WEIGHT);
      }

   }

   public boolean isMaster(){
      return this.is_master;
   }


   public Map<String, String> stress(CacheWrapper wrapper) {
      this.cacheWrapper = wrapper;
      startTime = System.currentTimeMillis();
      log.info("Executing: " + this.toString());

      List<Stresser> stressers;
      Sampler sampler;
      ResultBean result;
      try {
         if(lambda!=0){ //Open system
             for(int i=0; i<producers.length; i++){
                 producers[i].start();
             }
         }

         result = executeOperations();
      } catch (Exception e) {
          log.info("***** Eccezione nel metodo PutGetStressor.stress durante la chiamata a  executeOperations()*****");
         throw new RuntimeException(e);
      }
      return processResults(result.stressers, result.sampler);
   }

   public void destroy() throws Exception {
      cacheWrapper.empty();
      cacheWrapper = null;
   }

   private Map<String, String> processResults(List<Stresser> stressers, Sampler sampler) {
      long duration = 0;
      int reads = 0;
      int writes = 0;
      int failures = 0;
      int rdFailures=0;
      int wrFailures =0;
      int newOrderFailures =0;
      int paymentFailures =0;

      long readsDurations = 0;
      long writesDurations = 0;

      long commitDurations=0;
      long successful_writesDurations=0;
      long successful_readsDurations =0;

      long not_found_failures=0;
      long newOrderTransactions=0;
      long paymentTransactions=0;
      long paymentDurations=0;
      long newOrderDurations=0;

      long writeServiceTimes=0;
      long readServiceTimes=0;
      long newOrderServiceTimes=0;
      long paymentServiceTimes=0;

      long writeInQueueTimes=0;
      long readInQueueTimes=0;
      long newOrderInQueueTimes=0;
      long paymentInQueueTimes=0;
      long numWritesDequeued=0;
      long numReadsDequeued=0;
      long numNewOrderDequeued=0;
      long numPaymentDequeued=0;

      long LL=0;
      long LR=0;
      long RL=0;
      long RR=0;

      long skew=0;
      long local_tout=0;
      long remote_tout=0;


      for (Stresser stresser : stressers) {
         duration+=stresser.delta;
	     //duration += stresser.totalDuration();
         readsDurations += stresser.readDuration;
         writesDurations += stresser.writeDuration;
         successful_writesDurations += stresser.successful_writeDuration;
         successful_readsDurations += stresser.successful_readDuration;

         commitDurations+=stresser.commitDuration;

         reads += stresser.reads;
         writes += stresser.writes;
         failures += stresser.nrFailures;
         rdFailures += stresser.nrRdFailures;
         wrFailures += stresser.nrWrFailuers;
         newOrderFailures += stresser.nrNewOrderFailures;
         paymentFailures += stresser.nrPaymentFailures;

         not_found_failures += stresser.notFoundFailures;
         newOrderTransactions +=stresser.newOrder;
         paymentTransactions += stresser.payment;
         newOrderDurations += stresser.newOrderDuration;
         paymentDurations += stresser.paymentDuration;

         writeServiceTimes += stresser.writeServiceTime;
         readServiceTimes += stresser.readServiceTime;
         newOrderServiceTimes += stresser.newOrderServiceTime;
         paymentServiceTimes += stresser.paymentServiceTime;

         writeInQueueTimes += stresser.writeInQueueTime;
         readInQueueTimes += stresser.readInQueueTime;
         newOrderInQueueTimes += stresser.newOrderInQueueTime;
         paymentInQueueTimes +=stresser.paymentInQueueTime;
         numWritesDequeued += stresser.numWriteDequeued;
         numReadsDequeued += stresser.numReadDequeued;
         numNewOrderDequeued += stresser.numNewOrderDequeued;
         numPaymentDequeued += stresser.numPaymentDequeued;

         skew+=stresser.numWriteSkew;
         local_tout+=stresser.numLocalTimeOut;
         remote_tout+=stresser.numRemoteTimeOut;

      }

	  duration=duration/1000000;
      readsDurations=readsDurations/1000; //nanos to micros
      writesDurations=writesDurations/1000; //nanos to micros
      newOrderDurations=newOrderDurations/1000; //nanos to micros
      paymentDurations=paymentDurations/1000;//nanos to micros
      writeServiceTimes=writeServiceTimes/1000;//nanos to micros
      readServiceTimes=readServiceTimes/1000;//nanos to micros
      paymentServiceTimes=paymentServiceTimes/1000;//nanos to micros
      newOrderServiceTimes=newOrderServiceTimes/1000;//nanos to micros
      successful_readsDurations=successful_readsDurations/1000; //nanos to micros
      successful_writesDurations=successful_writesDurations/1000; //nanos to micros
      commitDurations=commitDurations/1000; //nanos to micros

      writeInQueueTimes=writeInQueueTimes/1000;//nanos to micros
      readInQueueTimes=readInQueueTimes/1000;//nanos to micros
      newOrderInQueueTimes=newOrderInQueueTimes/1000;//nanos to micros
      paymentInQueueTimes=paymentInQueueTimes/1000;//nanos to micros

      Map<String, String> results = new LinkedHashMap<String, String>();
      results.put("DURATION (msec)", str(duration/numOfThreads));
      double requestPerSec = (reads + writes)  /((duration/numOfThreads) / 1000.0);
      double wrtPerSec=0;
      double rdPerSec=0;
      double newOrderPerSec=0;
      double paymentPerSec=0;

      results.put("REQ_PER_SEC", str(requestPerSec));

      if(readsDurations+writesDurations==0)
         results.put("READS_PER_SEC",str(0));
      else{
         rdPerSec=reads   / (((readsDurations+writesDurations)/numOfThreads) / 1000000.0);
         results.put("READS_PER_SEC", str(rdPerSec));
      }
      if (writesDurations+readsDurations==0)
         results.put("WRITES_PER_SEC", str(0));
      else{
         wrtPerSec=writes / (((writesDurations+readsDurations)/numOfThreads) / 1000000.0);
         results.put("WRITES_PER_SEC", str(wrtPerSec));
      }
      if (writesDurations+readsDurations==0)
         results.put("NEW_ORDER_PER_SEC", str(0));
      else{
         newOrderPerSec=newOrderTransactions/(((writesDurations+readsDurations)/numOfThreads)/1000000.0);

         results.put("NEW_ORDER_PER_SEC", str(newOrderPerSec));
      }
      if (writesDurations+readsDurations==0)
         results.put("PAYMENT_PER_SEC", str(0));
      else{
         paymentPerSec=paymentTransactions/(((writesDurations+readsDurations)/numOfThreads)/1000000.0);

         results.put("PAYMENT_PER_SEC", str(paymentPerSec));
      }
      results.put("READ_COUNT", str(reads));
      results.put("WRITE_COUNT", str(writes));
      results.put("NEW_ORDER_COUNT", str(newOrderTransactions));
      results.put("PAYMENT_COUNT",str(paymentTransactions));
      results.put("FAILURES", str(failures));
      results.put("NOT_FOUND_FAILURES", str(not_found_failures));
      results.put("WRITE_FAILURES", str(wrFailures));
      results.put("NEW_ORDER_FAILURES", str(newOrderFailures));
      results.put("PAYMENT_FAILURES", str(paymentFailures));
      results.put("READ_FAILURES", str(rdFailures));

      if((reads+writes)!=0)
         results.put("AVG_SUCCESSFUL_TX_DURATION (usec)",str((successful_writesDurations+successful_readsDurations)/(reads+writes)));
      else
         results.put("AVG_SUCCESSFUL_TX_DURATION (usec)",str(0));


      if(reads!=0)
        results.put("AVG_SUCCESSFUL_RD_TX_DURATION (usec)",str(successful_readsDurations/reads));
      else
        results.put("AVG_SUCCESSFUL_RD_TX_DURATION (usec)",str(0));


      if(writes!=0)
        results.put("AVG_SUCCESSFUL_WR_TX_DURATION (usec)",str(successful_writesDurations/writes));
      else
        results.put("AVG_SUCCESSFUL_WR_TX_DURATION (usec)",str(0));


      if(writes!=0)
        results.put("AVG_COMMIT_DURATION (usec)",str((commitDurations/writes)));
      else
        results.put("AVG_COMMIT_DURATION (usec)",str(0));

      if((reads+rdFailures)!=0)
        results.put("AVG_RD_SERVICE_TIME (usec)",str(readServiceTimes/(reads+rdFailures)));
      else
        results.put("AVG_RD_SERVICE_TIME (usec)",str(0));

      if((writes+wrFailures)!=0)
        results.put("AVG_WR_SERVICE_TIME (usec)",str(writeServiceTimes/(writes+wrFailures)));
      else
        results.put("AVG_WR_SERVICE_TIME (usec)",str(0));

      if((newOrderTransactions+newOrderFailures)!=0)
        results.put("AVG_NEW_ORDER_SERVICE_TIME (usec)",str(newOrderServiceTimes/(newOrderTransactions+newOrderFailures)));
      else
        results.put("AVG_NEW_ORDER_SERVICE_TIME (usec)",str(0));

      if((paymentTransactions+paymentFailures)!=0)
        results.put("AVG_PAYMENT_SERVICE_TIME (usec)",str(paymentServiceTimes/(paymentTransactions+paymentFailures)));
      else
        results.put("AVG_PAYMENT_SERVICE_TIME (usec)",str(0));
      if(numWritesDequeued!=0)
        results.put("AVG_WR_INQUEUE_TIME (usec)",str(writeInQueueTimes/numWritesDequeued));
      else
        results.put("AVG_WR_INQUEUE_TIME (usec)",str(0));
      if(numReadsDequeued!=0)
        results.put("AVG_RD_INQUEUE_TIME (usec)",str(readInQueueTimes/numReadsDequeued));
      else
        results.put("AVG_RD_INQUEUE_TIME (usec)",str(0));
      if(numNewOrderDequeued!=0)
        results.put("AVG_NEW_ORDER_INQUEUE_TIME (usec)",str(newOrderInQueueTimes/numNewOrderDequeued));
      else
        results.put("AVG_NEW_ORDER_INQUEUE_TIME (usec)",str(0));
      if(numPaymentDequeued!=0)
        results.put("AVG_PAYMENT_INQUEUE_TIME (usec)",str(paymentInQueueTimes/numPaymentDequeued));
      else
        results.put("AVG_PAYMENT_INQUEUE_TIME (usec)",str(0));

    try{
    	String cacheMode = cacheWrapper.getCacheMode().toLowerCase();

        String commonName = new StringBuilder("org.infinispan:type=Cache")
                .append(",name=").append(ObjectName.quote("x(" + cacheMode + ")"))
                .append(",manager=").append(ObjectName.quote("DefaultCacheManager"))
                .append(",component=").toString();

        MBeanServer threadMBeanServer = ManagementFactory.getPlatformMBeanServer();

        ObjectName txInter = new ObjectName(commonName + "Transactions");
        ObjectName distMan = new ObjectName(commonName + "DistributionManager");
        ObjectName rpcMan = new ObjectName(commonName + "RpcManager");
        
        ObjectName distributionInterceptor = new ObjectName(commonName + "DistributionInterceptor");

        long successRemotePrepareDuration = (Long)threadMBeanServer.getAttribute(txInter, "SuccessPrepareTime");
        long numberOfSuccesRemotePrepare = (Long)threadMBeanServer.getAttribute(txInter, "NrSuccessPrepare");
        if(numberOfSuccesRemotePrepare > 0) {
            results.put("SUCCESSFUL_REMOTE_PREPARE_PHASE_TIME(nsec)", str(successRemotePrepareDuration/numberOfSuccesRemotePrepare));
        } else {
        	results.put("SUCCESSFUL_REMOTE_PREPARE_PHASE_TIME(nsec)", str(0));
        }
        
        
        long failedRemotePrepareDuration = (Long)threadMBeanServer.getAttribute(txInter, "FailedPrepareTime");
        
        long numberOfFailedRemotePrepare = (Long)threadMBeanServer.getAttribute(txInter, "NrFailedPrepare");
        
        
        if(numberOfFailedRemotePrepare > 0) {
            results.put("FAILED_REMOTE_PREPARE_PHASE_TIME(nsec)", str(failedRemotePrepareDuration/numberOfFailedRemotePrepare));
        } else {
        	results.put("FAILED_REMOTE_PREPARE_PHASE_TIME(nsec)", str(0));
        }
        
        
        
        long remoteCommitDuration = (Long)threadMBeanServer.getAttribute(txInter, "RemoteCommitTime");
        long numberOfRemoteCommits = (Long)threadMBeanServer.getAttribute(txInter, "NrRemoteCommit");
        
        if(numberOfRemoteCommits > 0) {
            results.put("REMOTE_COMMIT_PHASE_TIME(nsec)", str(remoteCommitDuration/numberOfRemoteCommits));
        } else {
        	results.put("REMOTE_COMMIT_PHASE_TIME(nsec)", str(0));
        }
        
        long localCommitDuration = (Long)threadMBeanServer.getAttribute(txInter, "LocalCommitTime");
        long numberOfLocalCommits = (Long)threadMBeanServer.getAttribute(txInter, "NrLocalCommit");
        
        if(numberOfLocalCommits > 0) {
            results.put("LOCAL_COMMIT_PHASE_TIME(nsec)", str(localCommitDuration/numberOfLocalCommits));
        } else {
        	results.put("LOCAL_COMMIT_PHASE_TIME(nsec)", str(0));
        }
        
        long RWLocksAcquisitionTime = (Long)threadMBeanServer.getAttribute(txInter, "RWLocksAcquisitionTime");
        long NrRWLocksAcquisitions = (Long)threadMBeanServer.getAttribute(txInter, "NrRWLocksAcquisitions");
        
        if(NrRWLocksAcquisitions > 0) {
            results.put("RW_LOCKS_ACQUISITION_TIME(nsec)", str(RWLocksAcquisitionTime/NrRWLocksAcquisitions));
        } else {
        	results.put("RW_LOCKS_ACQUISITION_TIME(nsec)", str(0));
        }
        
        long remoteRollbackDuration = (Long)threadMBeanServer.getAttribute(txInter, "RollbackTime");
        long numberOfRemoteRollback = (Long)threadMBeanServer.getAttribute(txInter, "NrRollback");
        
        if(numberOfRemoteRollback > 0) {
            results.put("REMOTE_ROLLBACK_PHASE_TIME(nsec)", str(remoteRollbackDuration/numberOfRemoteRollback));
        } else {
        	results.put("REMOTE_ROLLBACK_PHASE_TIME(nsec)", str(0));
        }
        
        long totalNumOfInvolvedNodesPerPrepare = (Long)threadMBeanServer.getAttribute(distributionInterceptor, "TotalNumOfInvolvedNodesPerPrepare");
        long totalPrepareSent = (Long)threadMBeanServer.getAttribute(distributionInterceptor, "TotalPrepareSent");
        
        if(totalPrepareSent > 0) {
            results.put("AVG_NUM_INVOLVED_NODES_PER_PREPARE", str((totalNumOfInvolvedNodesPerPrepare*1.0)/totalPrepareSent));
        } else {
        	results.put("AVG_NUM_INVOLVED_NODES_PER_PREPARE", str(0));
        }
        
        
        long numberOfRollbacksDueToUnableAcquireLocks = (Long)threadMBeanServer.getAttribute(txInter, "RollbacksDueToUnableAcquireLock");
        results.put("ROLLBACKS_FOR_FAILED_AQUIRE_LOCK", str(numberOfRollbacksDueToUnableAcquireLocks));
        
        
        long numberOfRollbacksDueToDeadlocks = (Long)threadMBeanServer.getAttribute(txInter, "RollbacksDueToDeadLock");
        results.put("ROLLBACKS_FOR_DEADLOCKS", str(numberOfRollbacksDueToDeadlocks));
        
        
        long numberOfRollbacksDueToValidation = (Long)threadMBeanServer.getAttribute(txInter, "RollbacksDueToValidation");
        results.put("ROLLBACKS_FOR_FAILED_VALIDATION", str(numberOfRollbacksDueToValidation));
        
        
        long getKeyDuration = (Long)threadMBeanServer.getAttribute(txInter, "ReadTime");
        long numberOfGetKey = (Long)threadMBeanServer.getAttribute(txInter, "NrReadOp");
        
        
        if(numberOfGetKey > 0) {
            results.put("GET_DURATION(nsec)", str(getKeyDuration/numberOfGetKey));
        } else {
        	results.put("GET_DURATION(nsec)", str(0));
        }
        
        long getLocalKeyDuration = (Long)threadMBeanServer.getAttribute(txInter, "LocalReadTime");
        long numberOfLocalGetKey = (Long)threadMBeanServer.getAttribute(txInter, "NrLocalReadOp");
        
        if(numberOfLocalGetKey > 0) {
            results.put("LOCAL_GET_DURATION(nsec)", str(getLocalKeyDuration/numberOfLocalGetKey));
        } else {
        	results.put("LOCAL_GET_DURATION(nsec)", str(0));
        }
        
        
        long remoteGetKeyDuration = (Long)threadMBeanServer.getAttribute(txInter, "RemoteReadTime");
        long numberOfRemoteGetKey = (Long)threadMBeanServer.getAttribute(txInter, "NrRemoteReadOp");
        
        if(numberOfRemoteGetKey > 0) {
            results.put("FROM_REMOTE_GET_DURATION(nsec)", str(remoteGetKeyDuration/numberOfRemoteGetKey));
        } else {
        	results.put("FROM_REMOTE_GET_DURATION(nsec)", str(0));
        	
        }
        
        results.put("LOCAL_GET", str(numberOfLocalGetKey));
        results.put("REMOTE_GET", str(numberOfGetKey-numberOfLocalGetKey));
        

        long rttOfRemoteGetKey = (Long)threadMBeanServer.getAttribute(distMan, "RoundTripTimeClusteredGetKey");
        long numberOfRemoteGetKeyII = (Long)threadMBeanServer.getAttribute(distMan, "NrClusteredGet");
        if(numberOfRemoteGetKeyII > 0) {
            results.put("REMOTE_GET_RESPONSE_LATENCY(nsec)", str(rttOfRemoteGetKey/numberOfRemoteGetKeyII));
        } else {
        	results.put("REMOTE_GET_RESPONSE_LATENCY(nsec)", str(0));
        }

        long prepareCommandBytes = (Long)threadMBeanServer.getAttribute(rpcMan, "PrepareCommandBytes");
        long numberOfPrepareCommand = (Long)threadMBeanServer.getAttribute(rpcMan, "NrPrepareCommand");
        
        if(numberOfPrepareCommand > 0) {
            results.put("SIZE_PREPARE_COMMAND(bytes)", str(prepareCommandBytes/numberOfPrepareCommand));
        } else {
        	results.put("SIZE_PREPARE_COMMAND(bytes)", str(0));
        }
        
        
        long commitCommandBytes = (Long)threadMBeanServer.getAttribute(rpcMan, "CommitCommandBytes");
        long numberOfCommitCommand = (Long)threadMBeanServer.getAttribute(rpcMan, "NrCommitCommand");
        if(numberOfCommitCommand > 0) {
            results.put("SIZE_COMMIT_COMMAND(bytes)", str(commitCommandBytes/numberOfCommitCommand));
        } else {
        	results.put("SIZE_COMMIT_COMMAND(bytes)", str(0));
        }
        
        
        long clusteredGetKeyCommandBytes = (Long)threadMBeanServer.getAttribute(rpcMan, "ClusteredGetKeyCommandBytes");
        long numberOfClusteredGetKeyCommand = (Long)threadMBeanServer.getAttribute(rpcMan, "NrClusteredGetKeyCommand");
        if(numberOfClusteredGetKeyCommand > 0) {
            results.put("SIZE_CLUSTERED_GET_KEY_COMMAND(bytes)", str(clusteredGetKeyCommandBytes/numberOfClusteredGetKeyCommand));
        } else {
        	results.put("SIZE_CLUSTERED_GET_KEY_COMMAND(bytes)", str(0));
        }
        

    }
	catch(Exception e){
		log.warn("CASINO");
		e.printStackTrace();
	}




      if(sampler!=null){
        Queue<Double> samples=sampler.getSamples();
        results.put("SAMPLING_INTERVAL (sec)", str(sampler.intervals_length));
        Double current=samples.poll();
        int i=1;
        while(current!=null){
            results.put("T"+i, str(current.doubleValue()));
            current=samples.poll();
            i++;

        }
      }

      log.info("Finished generating report. Nr of failed operations on this node is: " + failures +
            ". Test duration is: " + Utils.getDurationString(System.currentTimeMillis() - startTime));
      return results;
   }

   private ResultBean executeOperations() throws Exception {
      List<Stresser> stressers = new ArrayList<Stresser>();
      final AtomicInteger requestsLeft = new AtomicInteger(numberOfRequests);       //now it is number of txs
      startPoint = new CountDownLatch(1);


      long c_run=TPCCTools.randomNumber(0, TPCCTools.A_C_LAST);
      long c_id=TPCCTools.randomNumber(0, TPCCTools.A_C_ID);
      long ol_i_id=TPCCTools.randomNumber(0, TPCCTools.A_OL_I_ID);
      
      List<Integer> listLocalWarehouses = TPCCTools.selectLocalWarehouse(cacheWrapper);
      
      int[] localWarehouses = null;
      int numLocalWarehouses = -1;;
      if(listLocalWarehouses != null && !listLocalWarehouses.isEmpty()){
    	  Iterator<Integer> itr = listLocalWarehouses.iterator();
    	  numLocalWarehouses = listLocalWarehouses.size();

    	  localWarehouses = new int[numLocalWarehouses];

    	  String debug = "";

    	  for(int i = 0; i< numLocalWarehouses; i++){

    		  localWarehouses[i] = itr.next();

    		  debug+=" "+localWarehouses[i];
    	  }

    	  log.debug("Local warehouses: "+debug);
      }

      for (int threadIndex = 0; threadIndex < numOfThreads; threadIndex++) {
    	 
         Stresser stresser = new Stresser((localWarehouses!=null)?localWarehouses[(threadIndex % numLocalWarehouses)]:-1, threadIndex, requestsLeft,this.is_master,this.lower_bound_op,this.upper_bound_op,this.simulTime, c_run, c_id, ol_i_id, this.forceGlobalWrite);
         stresser.initializeKeys();
         stressers.add(stresser);

         try{
            stresser.start();
         }
         catch (Throwable t){

             log.info("***Eccezione nella START "+t);
         }
      }

      Sampler sampler= new Sampler(stressers, 10);       //COMMENT OUT
       //Sampler sampler=null;//COMMENT IN
      Timer timer= new Timer();               //COMMENT OUT

      //st= SelfTunerFactory.createDefaultSelfTuner(30000L, this.cacheWrapper.getCache()); COOMMENT OUT


      log.info("Cache private class Stresser extends Thread { wrapper info is: " + cacheWrapper.getInfo());
      startPoint.countDown();

      timer.scheduleAtFixedRate(sampler, 10000L, 10000L); //COMMENT OUT

	  //st.start();            COMMENT OUT
      //st.setHighContention();       COMMENT OUT

      for (Stresser stresser : stressers) {
         stresser.join();
      }

      timer.cancel();    //COMMENT OUT
       //st.stop();  COMMENT OUT

      /*
      Queue<Double> samples=sampler.getSamples();
      Double current=samples.poll();
      int i=0;
      while(current!=null){
         log.info("T"+i+" - "+current.doubleValue());
         current=samples.poll();
         i++;

      }
      */


      log.info("****BARRIER JOIN PASSED****");

      return new ResultBean(stressers, sampler);
   }

   private class Stresser extends Thread {

      private ArrayList<String> pooledKeys = new ArrayList<String>(numberOfKeys);

      private int threadIndex;
      private String bucketId;
      private int nrFailures=0;
      private int nrWrFailuers=0;
      private int nrRdFailures=0;
      private int nrNewOrderFailures=0;
      private int nrPaymentFailures=0;
      private long readDuration = 0;
      private long writeDuration = 0;
      private long successful_writeDuration=0;
      private long successful_readDuration=0;
      private long newOrderDuration=0;
      private long paymentDuration=0;
      private long writeServiceTime=0;
      private long paymentServiceTime=0;
      private long newOrderServiceTime=0;
      private long readServiceTime=0;
      private long writeInQueueTime=0;
      private long readInQueueTime=0;
      private long newOrderInQueueTime=0;
      private long paymentInQueueTime=0;
      private long numWriteDequeued=0;
      private long numReadDequeued=0;
      private long numNewOrderDequeued=0;
      private long numPaymentDequeued=0;

      private long reads=0;
      private long writes=0;
      private long startTime;
      private AtomicInteger requestsLeft;
      private long delta=0;

      private long notFoundFailures=0;
      private long newOrder=0;
      private long payment=0;



      //Added for the new kind of test
      private int lower_bound_tx_op;
      private int upper_bound_tx_op;
      private boolean is_master_thread;

      private long commitDuration=0;
      private long simulTime;

      private int numWriteSkew=0;
      private int numLocalTimeOut=0;
      private int numRemoteTimeOut=0;

      private long c_run;
      private long c_id;
      private long ol_i_id;
      
      private int localWarehouse;
      
      private boolean forceGlobalWrite;

      public Stresser(int localWarehouse, int threadIndex, AtomicInteger requestsLeft, boolean is_master_thread, int lb, int ub, long time, long c_run, long c_id, long ol_i_id, boolean forceGlobalWrite) {

         super("Stresser-" + threadIndex);
         this.threadIndex = threadIndex;
         bucketId = getBucketId(threadIndex);
         this.requestsLeft = requestsLeft;

         this.is_master_thread=is_master_thread;
         this.lower_bound_tx_op=lb;
         this.upper_bound_tx_op=ub;
         this.simulTime=time;


         this.c_run=c_run;
         this.c_id=c_id;
         this.ol_i_id=ol_i_id;
         
         this.localWarehouse=localWarehouse;
         
         this.forceGlobalWrite = forceGlobalWrite;

      }

      @Override
      public void run() {


         int randomKeyInt;
         try {
            startPoint.await();
            log.info("Starting thread: " + getName());
         } catch (InterruptedException e) {
            log.warn(e);
         }

         int i = 0;
         int op_left;
         boolean force_ro;
         boolean successful;
         long start, end, startService, endInQueueTime;



         TPCCTerminal terminal= new TPCCTerminal(this.localWarehouse, c_run, c_id, ol_i_id, this.forceGlobalWrite);


	     long init_time=System.nanoTime();
         long commit_start=0;
         int transaction_type=0;


         while(delta<this.simulTime){

            successful=true;


            start = System.nanoTime();
            if(lambda!=0.0){  //Open system
               try{
                  RequestType request=queue.take();
                  transaction_type=request.transactionType;
                  endInQueueTime=System.nanoTime();
                  if(transaction_type==TPCCTerminal.NEW_ORDER){
                       numWriteDequeued++;
                       numNewOrderDequeued++;
                       writeInQueueTime += endInQueueTime - request.timestamp;
                       newOrderInQueueTime += endInQueueTime - request.timestamp;
                  }
                  else if(transaction_type==TPCCTerminal.PAYMENT){
                       numWriteDequeued++;
                       numPaymentDequeued++;
                       writeInQueueTime += endInQueueTime - request.timestamp;
                       paymentInQueueTime += endInQueueTime - request.timestamp;

                  }
                  else if(transaction_type==TPCCTerminal.ORDER_STATUS){
                       numReadDequeued++;
                       readInQueueTime += endInQueueTime - request.timestamp;

                  }

                  //log.info("New Transaction: "+transaction_type);
               }
               catch(InterruptedException ir){
                  log.error("»»»»»»»THREAD INTERRUPTED WHILE TRYING GETTING AN OBJECT FROM THE QUEUE«««««««");
               }
            }
            else{ //Closed system without think time
                transaction_type=terminal.choiceTransaction(cacheWrapper.isPassiveReplication(), is_master);
            }

            force_ro=transaction_type==TPCCTerminal.ORDER_STATUS;

            startService = System.nanoTime();

            cacheWrapper.startTransaction();


            try{
                terminal.executeTransaction(cacheWrapper, transaction_type,cacheWrapper.isPassiveReplication(), slaveIndex, this.threadIndex, total_num_of_slaves, numOfThreads);
            }
            catch (Throwable e) {
               successful=false;
               log.warn(e);
               if(e instanceof ElementNotFoundException){
                   this.notFoundFailures++;
               }
               if(e instanceof NullPointerException){
                   log.error("Problem!!!!", e);
               }

               if(e.getClass().getName()=="org.infinispan.CacheException")
                   this.numWriteSkew++;
               else if(e.getClass().getName()=="org.infinispan.util.concurrent.TimeoutException")
                   this.numLocalTimeOut++;

            }

            //here we try to finalize the transaction
            //if any read/write has failed we abort
            try{
                /* In our tests we are interested in the commit time spent for write txs*/
               if(successful && !force_ro){
                  commit_start=System.nanoTime();
               }
               cacheWrapper.endTransaction(successful);
               if(!successful){
                  nrFailures++;
                  if(!force_ro){
                     nrWrFailuers++;
                     if(transaction_type==TPCCTerminal.NEW_ORDER){
                        nrNewOrderFailures++;
                     }
                     else if(transaction_type== TPCCTerminal.PAYMENT){
                        nrPaymentFailures++;
                     }

                  }
                  else{
                     nrRdFailures++;
                  }

               }
            }
            catch(Throwable rb){
               log.info(this.threadIndex+"Error while committing");
               nrFailures++;
               if(!force_ro){
                  nrWrFailuers++;
                  if(transaction_type==TPCCTerminal.NEW_ORDER){
                     nrNewOrderFailures++;
                  }
                  else if(transaction_type== TPCCTerminal.PAYMENT){
                     nrPaymentFailures++;
                  }
               }
               else{
                  nrRdFailures++;
               }
               successful=false;
               log.warn(rb);
               if(rb instanceof NullPointerException){
                   log.error("Problem!!!!",rb);
               }

               this.numRemoteTimeOut++;

            }

            end=System.nanoTime();

            if(lambda==0.0){  //Closed system
                start=startService;
            }

            if(!force_ro){
               writeDuration += end - start;
               writeServiceTime += end - startService;
               if(transaction_type==TPCCTerminal.NEW_ORDER){
                  newOrderDuration += end - start;
                  newOrderServiceTime += end - startService;
               }
               else if(transaction_type== TPCCTerminal.PAYMENT){
                  paymentDuration += end - start;
                  paymentServiceTime += end - startService;
               }
               if(successful){
                  successful_writeDuration += end - startService;
		          writes++;
                  if(transaction_type==TPCCTerminal.PAYMENT){
                      payment++;
                  }
                  else if(transaction_type==TPCCTerminal.NEW_ORDER){
                      newOrder++;
                  }
               }
            }
            else{    //ro transaction
               readDuration += end - start;
               readServiceTime += end -startService;
               if(successful){
		          reads++;
                  successful_readDuration += end - startService;
               }
            }
            //We are interested only in the successful commits for write transactions
            if(successful && !force_ro){
               this.commitDuration+=end-commit_start;
            }


            this.delta=end-init_time;
         }

         completedThread.incrementAndGet();
      }


      private void logProgress(int i, Object result) {
         if ((i + 1) % opsCountStatusLog == 0) {
            double elapsedTime = System.currentTimeMillis() - startTime;
            double estimatedTotal = ((double) (numberOfRequests / numOfThreads) / (double) i) * elapsedTime;
            double estimatedRemaining = estimatedTotal - elapsedTime;
            if (log.isTraceEnabled()) {
               log.trace("i=" + i + ", elapsedTime=" + elapsedTime);
            }
            log.info("Thread index '" + threadIndex + "' executed " + (i + 1) + " operations. Elapsed time: " +
                  Utils.getDurationString((long) elapsedTime) + ". Estimated remaining: " + Utils.getDurationString((long) estimatedRemaining) +
                  ". Estimated total: " + Utils.getDurationString((long) estimatedTotal));
            System.out.println("Last result" + result);//this is printed here just to make sure JIT doesn't skip the call to cacheWrapper.get
         }
      }

      public long totalDuration() {
         return readDuration + writeDuration;
      }


      public void initializeKeys() {

         for (int keyIndex = 0; keyIndex < numberOfKeys; keyIndex++) {

            String key="key:"+keyIndex;
            pooledKeys.add(key);

            //cacheWrapper.put(this.bucketId, key, generateRandomString(sizeOfValue));
         }
      }

      public String getKey(int keyIndex) {
         return pooledKeys.get(keyIndex);
      }
   }

   private class Sampler extends TimerTask{

       private Queue<Double> samples;

       private int intervals_length;
       private List<Stresser> stressers;
       private long current_value=0;
       private int num_samples=0;

       private long old_countJobs=0;

       private MemoryMXBean memoryBean;

       /**
        *
        * @param stressers    list of Stresser
        * @param intervals_length length in seconds
        */
       public Sampler(List<Stresser> stressers, int intervals_length){
          this.intervals_length=intervals_length;
          this.stressers=stressers;
          this.samples=new LinkedList<Double>();

          this.memoryBean=ManagementFactory.getMemoryMXBean();
       }

       @Override
       public void run(){
          if(lambda!=0.0){
                long current_countJobs= countJobs.get();

                log.info("Queued jobs in period "+num_samples+" :"+(current_countJobs-old_countJobs));

                old_countJobs=current_countJobs;
          }

          MemoryUsage u1=this.memoryBean.getHeapMemoryUsage();
          log.info("Memory Statistics (Heap) - used="+u1.getUsed()+" bytes; committed="+u1.getCommitted()+" bytes");
          MemoryUsage u2=this.memoryBean.getNonHeapMemoryUsage();
          log.info("Memory Statistics (NonHeap) - used="+u2.getUsed()+" bytes; committed="+u2.getCommitted()+" bytes");

          Iterator<Stresser> itr=this.stressers.iterator();
          Stresser current_stresser=null;
          long temp_value=0;
          while (itr.hasNext()){

             current_stresser=itr.next();

             temp_value+=current_stresser.writes;
             //temp_value+=current_stresser.reads;

          }
          Double newSample=new Double(((temp_value - current_value)*1.0)/intervals_length);
          samples.add(newSample);
          num_samples++;

          this.current_value=temp_value;

          //log.info("Sample "+num_samples+": "+newSample.doubleValue()+" wrt_TX/sec");
       }

       public Queue<Double> getSamples(){

           return this.samples;
       }


   }

   private class ResultBean{

      private List<Stresser> stressers;
      private Sampler sampler;

      public ResultBean(List<Stresser> stressers, Sampler sampler){
         this.stressers=stressers;
         this.sampler=sampler;
      }

      public List<Stresser> getStressers(){

         return this.stressers;
      }

      public Sampler getSampler(){

         return this.sampler;
      }

   }
   private String str(Object o) {
      return String.valueOf(o);
   }

   public void setNumberOfRequests(int numberOfRequests) {
      this.numberOfRequests = numberOfRequests;
   }

   public void setNumberOfAttributes(int numberOfKeys) {
      this.numberOfKeys = numberOfKeys;
   }

   public void setSizeOfAnAttribute(int sizeOfValue) {
      this.sizeOfValue = sizeOfValue;
   }

   public void setNumOfThreads(int numOfThreads) {
      this.numOfThreads = numOfThreads;
   }
   
   public void setForceGlobalWrite(boolean forceGlobalWrite){
	   this.forceGlobalWrite = forceGlobalWrite;
   }

   public void setWritePercentage(int writePercentage) {
      this.writePercentage = writePercentage;
   }

   public void setOpsCountStatusLog(int opsCountStatusLog) {
      this.opsCountStatusLog = opsCountStatusLog;
   }

   /**
    * This will make sure that each session runs in its own thread and no collisition will take place. See
    * https://sourceforge.net/apps/trac/cachebenchfwk/ticket/14
    */
   private String getBucketId(int threadIndex) {
      return bucketPrefix + "_" + threadIndex;
   }

   private static String generateRandomString(int size) {
      // each char is 2 bytes
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < size / 2; i++) sb.append((char) (64 + r.nextInt(26)));
      return sb.toString();
   }

   public String getBucketPrefix() {
      return bucketPrefix;
   }

   public void setBucketPrefix(String bucketPrefix) {
      this.bucketPrefix = bucketPrefix;


   }

   public void setSlaveIndex(int index){
       this.slaveIndex=index;
   }

   @Override
   public String toString() {
      return "PutGetStressor{" +
            "opsCountStatusLog=" + opsCountStatusLog +
            ", numberOfRequests=" + numberOfRequests +
            ", numberOfKeys=" + numberOfKeys +
            ", sizeOfValue=" + sizeOfValue +
            ", writePercentage=" + writePercentage +
            ", numOfThreads=" + numOfThreads +
            ", bucketPrefix=" + bucketPrefix +
            ", cacheWrapper=" + cacheWrapper +
            "}";
   }

    /*
      Obtain a (pseudo)random number between lower_bound and upper bound
     */

   private int opPerTx(int lower_bound, int upper_bound,Random ran){
      if(lower_bound==upper_bound)
         return lower_bound;
      return(ran.nextInt(upper_bound-lower_bound)+lower_bound);


   }

    private class Producer extends Thread{


           private double transaction_weight;    //an integer in [0,100]
           private int transaction_type;
           private boolean read_only;
           private double mean_lambda;
           private Random random;

           public Producer(int transaction_type, boolean read_only, double transaction_weight){

               this.transaction_weight=transaction_weight;
               this.transaction_type=transaction_type;
               this.read_only=read_only;
               this.mean_lambda=(lambda/1000.0)*(this.transaction_weight/100.0);
               this.random=new Random(System.currentTimeMillis());


           }
            //think time = numero di tx medie al secondo
           public void run(){

              long time;
              double new_mean_lambda;



              while(completedThread.get()!=numOfThreads){

                   try{
                       new_mean_lambda=mean_lambda;
                       if(cacheWrapper.isPassiveReplication()){

                            if(!is_master){
                                new_mean_lambda=new_mean_lambda/(total_num_of_slaves-1);
                            }
                       }
                       else{
                            new_mean_lambda=new_mean_lambda/total_num_of_slaves;
                       }


                       if(!cacheWrapper.isPassiveReplication() || ((read_only && !is_master) || (!read_only && is_master))){
                          //log.info("Producer "+this.transaction_type+" adds an new transaction.");
                          queue.add(new RequestType(System.nanoTime(),this.transaction_type));
                          countJobs.incrementAndGet();
                       }

                       time =(long) (exp(new_mean_lambda));
                       //log.info("Producer "+this.transaction_type+" sleeps for "+time+" millisec" );
                       Thread.sleep(time);
                   }
                   catch(InterruptedException i){
                        log.error("»»»»»»INTERRUPTED_EXCEPTION«««««««");
                   }
                   catch(IllegalStateException il){
                       log.error("»»»»»»»CODA PIENA«««««««««");

                   }
              }

           }




           private double exp(double lambda) {

              return -Math.log(1.0 - random.nextDouble()) / lambda;
           }


       }

     private class RequestType{

         private long timestamp;
         private int transactionType;

         public RequestType(long timestamp, int transactionType){
              this.timestamp=timestamp;
             this.transactionType=transactionType;
         }

     }
}

