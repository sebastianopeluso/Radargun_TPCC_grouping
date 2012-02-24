package org.radargun.stages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.radargun.reporting.ClusterReport;
import org.radargun.reporting.LineClusterReport;
import org.radargun.utils.Utils;

import java.io.*;
import java.security.PublicKey;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Stage that generates a chart from a set of csv files.
 * <pre>
 * - fnPrefix: the prefix of the generated chart file (png). No value by default, optional parameter.
 * - reportDirectory - where are the csv files located. Defaults to 'reports'
 * - outputDir - where to output the generated graphical reports. Defaults to 'reports'
 * </pre>
 *
 * @author Mircea.Markus@jboss.com
 */
public class GenerateChartStage extends AbstractMasterStage {

   private static Log log = LogFactory.getLog(GenerateChartStage.class);

   public static final String X_LABEL = "Cluster size (number of cache instances)";

   private String reportDirectory = "reports";
   private String csvFilesDirectory = "reports";
   private String fnPrefix;
   private Map<String, List<String>> filter = new HashMap<String, List<String>>();
   protected Map<String, List<Pattern>> compiledFilters = null;
   ClusterReport putReport = new LineClusterReport();
   ClusterReport getReport = new LineClusterReport();
   ClusterReport throughputReport = new LineClusterReport();

   public boolean execute() throws Exception {
      putReport.setReportFile(reportDirectory, fnPrefix + "_WRITE_TX");
      putReport.init(X_LABEL, "WRITE_ONLY tx/sec ", "Throughput WRITE_ONLY tx ", getSubtitle());
      getReport.setReportFile(reportDirectory, fnPrefix + "_READ_TX");
      getReport.init(X_LABEL, "READ_ONLY tx/sec ", "Throughput READ_ONLY tx ", getSubtitle());


      throughputReport.setReportFile(reportDirectory, fnPrefix + "_WRITE_Throughput");
      throughputReport.init("Elapsed time (sec)", "WRITE tx/sec ", "Throughput WRITE tx", getSubtitle());


      File[] files = getFilteredFiles(new File(csvFilesDirectory));
      for (File f : files) {
         readData(f);
      }

      putReport.generate();
      getReport.generate();

      throughputReport.generate();

      return true;
   }

   private void readData(File f) throws IOException {
      log.debug("Processing file " + f);
      //expected file format is: <product>_<config>_<size>.csv
      String productName = null;
      String configName = null;
      int clusterSize = 0;
      try {
         StringTokenizer tokenizer = new StringTokenizer(Utils.fileName2Config(f.getName()), "_");
         productName = tokenizer.nextToken();
         configName = tokenizer.nextToken();
         clusterSize = Integer.parseInt(tokenizer.nextToken());
      } catch (Throwable e) {
         String fileName = f == null ? null : f.getAbsolutePath();
         log.error("unexpected exception while parsing filename: " + fileName, e);
      }

      //now the contents of this file:
      String line;
      BufferedReader br = new BufferedReader(new FileReader(f));
      long avgPutPerSec = 0, avgGetsPerSec = 0;
      Stats s = null;

      PeriodicWriteThroughput pwt=null;
      List<PeriodicWriteThroughput> lpwt=new ArrayList<PeriodicWriteThroughput>();
      int min_num_throughput=-1;

      br.readLine(); //this is the header
      while ((line = br.readLine()) != null) {
         s = getAveragePutAndGet(line);
         log.debug("Read stats " + s);
         if (s != null) {
            avgPutPerSec += s.putsPerSec;
            avgGetsPerSec += s.getsPerSec;
         }

         pwt=getPeriodicWriteThroughput(line);


         if(min_num_throughput==-1 || pwt.getNumValues() < min_num_throughput){
            min_num_throughput=pwt.getNumValues();
         }

         lpwt.add(pwt);

      }
      br.close();

      //avgGetsPerSec = avgGetsPerSec / clusterSize;
      //avgPutPerSec = avgPutPerSec  / clusterSize;

      String name = productName + "(" + configName + ")";
      putReport.addCategory(name, clusterSize, avgPutPerSec);
      getReport.addCategory(name, clusterSize, avgGetsPerSec);

      PeriodicWriteThroughput pwt_total=new PeriodicWriteThroughput();
      Iterator<PeriodicWriteThroughput> itr=null;
      double total_in_period;

      for(int i=0; i<min_num_throughput; i++){
         total_in_period=0;
         itr=lpwt.iterator();
         PeriodicWriteThroughput current_pwt=null;
         while(itr.hasNext()){
            current_pwt=itr.next();
            total_in_period+=current_pwt.dequeueWriteThroughput();
            pwt_total.setSamplingInterval(current_pwt.getSamplingInterval());
         }

         pwt_total.enqueueWriteThroughput(new Double(total_in_period));

      }

      File f_throughputs=new File (f.getParent(), f.getName()+"_WriteThroughput_"+clusterSize+".txt");
      PrintStream f_out=new PrintStream(f_throughputs);
      f_out.println("Elapsed Time (sec)"+'\t'+"Throughput (Write_Tx/sec)");
      //log.info("Total Throughputs");
      Double current_value=pwt_total.dequeueWriteThroughput();
      int i=0;
      while(current_value!=null){
          i+=pwt_total.getSamplingInterval();
          //log.info(current_value);
          throughputReport.addCategory(name +" Cluster size="+clusterSize, i, current_value);
          f_out.println(String.valueOf(i)+'\t'+String.valueOf(current_value));
          current_value=pwt_total.dequeueWriteThroughput();


      }

      f_out.close();

   }

   private Stats getAveragePutAndGet(String line) {
      // To be a valid line, the line should be comma delimited
      StringTokenizer strTokenizer = new StringTokenizer(line, ",");
      if (strTokenizer.countTokens() < 7) return null;

      strTokenizer.nextToken();//skip index
      strTokenizer.nextToken();//skip duration
      strTokenizer.nextToken();//skip request per sec

      String getStr = strTokenizer.nextToken(); //this is get/sec
      String putStr = strTokenizer.nextToken(); //this is put/sec

      Stats s = new Stats();
      try {
         s.putsPerSec = Double.parseDouble(putStr);
         s.getsPerSec = Double.parseDouble(getStr);
      }
      catch (NumberFormatException nfe) {
         log.error("Unable to parse file properly!", nfe);
         return null;
      }
      return s;
   }

   private PeriodicWriteThroughput getPeriodicWriteThroughput(String line){
      StringTokenizer strTokenizer = new StringTokenizer(line, ",");
      if (strTokenizer.countTokens() < 7) return null;
      PeriodicWriteThroughput pwt=new PeriodicWriteThroughput();

      for(int i=0; i<48; i++){
         strTokenizer.nextToken();//skip other statistics
      }

      pwt.setSamplingInterval(Integer.parseInt(strTokenizer.nextToken()));

      String nextThroughput=null;

      while(strTokenizer.hasMoreTokens()){

         nextThroughput=strTokenizer.nextToken();

         if(nextThroughput.equals(String.valueOf(-1)))   //This node doesn't have that Throughput sample
            break;

         pwt.enqueueWriteThroughput(Double.parseDouble(nextThroughput));

      }



      return pwt;

   }

   public void setCsvFilesDirectory(String csvFilesDirectory) {
      this.csvFilesDirectory = csvFilesDirectory;
   }

   public void setFnPrefix(String fnPrefix) {
      this.fnPrefix = fnPrefix;
   }

   public void setReportDirectory(String reportDirectory) {
      this.reportDirectory = reportDirectory;
   }

   protected File[] getFilteredFiles(File file) {
      return file.listFiles(new FilenameFilter() {
         //accepted file names are <product-name>_<config-name>_<cluster-size>.csv
         public boolean accept(File dir, String name) {
            compileFilters();
            if (!name.toUpperCase().endsWith(".CSV")) {
               return false;
            }
            if (!isUsingFilters()) {
               return true;
            }
            StringTokenizer tokenizer = new StringTokenizer(name, "_");
            String productName = tokenizer.nextToken();
            String configName = tokenizer.nextToken();
            if (!filter.containsKey(productName)) return false;

            //first check that this works as compilation issues might have occurred during parsing the patterns
            if (filter.get(productName).contains(configName)) {
               return true;
            } else {
               if (configName.equals("*")) {
                  return true;
               } else {
                  List<Pattern> patternList = compiledFilters.get(productName);
                  for (Pattern pattern : patternList) {
                     if (pattern.matcher(configName).find()) {
                        log.trace("Pattern '" + pattern + "' matched config: " + configName);
                        return true;
                     }
                  }
               }
            }
            return false;
         }
      });
   }

   private void compileFilters() {
      if (compiledFilters == null) {
         compiledFilters = new HashMap<String, List<Pattern>>();
         for (String product : filter.keySet()) {
            List<Pattern> compiled = new ArrayList<Pattern>();
            for (String aFilter : filter.get(product)) {
               if (aFilter.equals("*") || aFilter.equals("?") || aFilter.equals("+")) {
                  String oldPatter = aFilter;
                  aFilter = "[" + aFilter + "]";
                  log.info("Converting the pattern from '" + oldPatter + "' to '" + aFilter +"'. " +
                        "See: http://arunma.com/2007/08/23/javautilregexpatternsyntaxexception-dangling-meta-character-near-index-0");
               }
               try {
                  Pattern pattern = Pattern.compile(aFilter);
                  compiled.add(pattern);
               } catch (Exception e) {
                  String message = "Exception while compiling the pattern: '" + aFilter + "'";
                  if (e.getMessage().indexOf("Dangling meta character ") >= 0) {
                     message += "If your regexp is like '*' or '+' (or other methachars), add square brackets to it, " +
                           "e.g. '[*]'. See: http://arunma.com/2007/08/23/javautilregexpatternsyntaxexception-dangling-meta-character-near-index-0/";
                  } else {
                  }
                  log.warn(message, e);

               }
            }
            compiledFilters.put(product, compiled);
         }
      }
   }

   public boolean isUsingFilters() {
      return !filter.isEmpty();
   }


   public void addReportFilter(String prodName, String config) {
      List<String> stringList = filter.get(prodName);
      if (stringList == null) {
         stringList = new ArrayList<String>();
         filter.put(prodName, stringList);
      }
      stringList.add(Utils.fileName2Config(config));
   }

   public static String getSubtitle() {
      return "Generated on " + new Date() + " by RadarGun\nJDK: " +
            System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.version") + ", " +
            System.getProperty("java.vm.vendor") + ") OS: " + System.getProperty("os.name") + " (" +
            System.getProperty("os.version") + ", " + System.getProperty("os.arch") + ")";
   }

   private static class Stats {
      private double putsPerSec, getsPerSec;

      public String toString() {
         return "Stats{" +
               "avgPut=" + putsPerSec +
               ", avgGet=" + getsPerSec +
               '}';
      }
   }

   private static class PeriodicWriteThroughput{

      private Queue<Double> values;
      private int num_values;
      private int samplingInterval;

      public PeriodicWriteThroughput(){
         this.values=new LinkedList<Double>();
         this.num_values=0;
         this.samplingInterval=0;
      }

      public void enqueueWriteThroughput(Double value){
         this.values.add(value);
         this.num_values++;
      }
      public Double dequeueWriteThroughput(){

         Double value=this.values.poll();
         if(value!=null) this.num_values--;

         return value;
      }

      public int getNumValues(){

         return this.num_values;
      }

      public void setSamplingInterval(int value){
         this.samplingInterval=value;
      }

      public int getSamplingInterval(){
         return this.samplingInterval;
      }
   }
}
