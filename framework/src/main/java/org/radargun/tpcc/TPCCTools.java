package org.radargun.tpcc;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.radargun.CacheWrapper;

/**
 * Created by IntelliJ IDEA.
 * User: sebastiano
 * Date: 4/26/11
 * Time: 6:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class TPCCTools {

    public static final double WAREHOUSE_YTD = 300000.00;
    public static final int NB_MAX_DISTRICT = 10;
    public static final long NB_MAX_ITEM = 100000;
    public static final int NB_MAX_CUSTOMER = 3000;
    public static final int NB_MAX_ORDER = 3000;
    public static final String CHAINE_5_1 = "11111";
    public final static int MIN_C_LAST = 0;
    public final static int MAX_C_LAST = 999;
    public final static String[] C_LAST = {"BAR", "OUGHT", "ABLE", "PRI", "PRES", "ESE", "ANTI", "CALLY", "ATION", "EING"};
    public final static int LIMIT_ORDER = 2101;
    public static final int NULL_NUMBER = -1;
    public final static int S_DATA_MINN = 26;
    public static int A_C_LAST = 255;
    public static int A_OL_I_ID = 8191;
    public static int A_C_ID = 1023;
    public static double OVERLAP= 0.0;
    private final static int DEFAULT_RADIX = 10;
    private final static int DEFAULT_MINL = 65;
    private final static int DEFAULT_MAXL = 90;
    private final static int DEFAULT_MINN = 48;
    private final static int DEFAULT_MAXN = 57;
    private final static int S_DATA_MAXN = 50;
    public final static String ORIGINAL = "ORIGINAL";
    private final static int unicode[][] = { {65, 126}, {192, 259}};
    private static Random _randUniform = new Random(System.nanoTime());
    private static Random _randNonUniform=new Random(System.nanoTime());
    private static Random _randAlea=new Random(System.nanoTime());

    private static String alea_chaine(int deb, int fin, int min, int max, int radix) {
        if (deb > fin) return null;
        String chaine = new String();
        int lch = fin;

        if (deb != fin) lch = alea_number(deb, fin);

        for (int i=0; i<lch; i++) {
            int random = _randAlea.nextInt(max-min+1)+min;
	        char c = (char) (((byte) random) & 0xff);
	        chaine += c;
        }
        return chaine;
    }


    public static String alea_chainel(int deb, int fin, int radix) {
        return alea_chaine(deb, fin, DEFAULT_MINL, DEFAULT_MAXL, radix);
    }

    public static String alea_chainel(int deb, int fin) {
        return alea_chainel(deb, fin, DEFAULT_RADIX);
    }


    public static String alea_chainec(int deb, int fin, int radix) {
        if (deb > fin) return null;
        String chaine = "";
		String str = null;

        int lch = fin;
        if (deb != fin) lch = alea_number(deb, fin);

        for (int i=0; i<lch; i++) {
            int ref = _randAlea.nextInt(2);
            int min = unicode[ref][0];
            int max = unicode[ref][1];
            int random = _randAlea.nextInt(max-min+1)+min;

	        char c = (char) (((byte) random));
	        chaine += c;
        }
        try {
        	str = new String(chaine.toString().getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
        	System.out.println("----------- Error "+e.getMessage());
        }
        return str;
    }

    public static String alea_chainec(int deb, int fin) {
        return alea_chainec(deb, fin, DEFAULT_RADIX);
    }

    public static String s_data() {
        String alea = alea_chainec(S_DATA_MINN, S_DATA_MAXN);
        if (alea_number(1, 10) == 1) {
        	long number = randomNumber(0, alea.length()-8);
        	alea = alea.substring(0, (int)number)+ORIGINAL+alea.substring((int)number+8, alea.length());
        }
        return alea;
    }


    public static String alea_chainen(int deb, int fin, int radix) {
        return alea_chaine(deb, fin, DEFAULT_MINN, DEFAULT_MAXN, radix);
    }

    public static String alea_chainen(int deb, int fin) {
        return alea_chainen(deb, fin, DEFAULT_RADIX);
    }


    public static int alea_number(int deb, int fin) {
        return _randAlea.nextInt(fin-deb+1)+deb;
    }


    public static long alea_number(long deb, long fin) {
        long random = _randAlea.nextLong()%(fin+1);
        while (random < deb) random += fin-deb;
        return random;
    }

    public static float alea_float(float deb, float fin, int virg) {
        if(deb > fin || virg < 1) return 0;
        long pow = (long)Math.pow(10, virg);
        long amin =  (long) (deb * pow);
        long amax =  (long) (fin * pow);
        long random = (long) (_randAlea.nextDouble() * (amax-amin) + amin);
        return (float) random/pow;
    }

    public static double alea_double(double deb, double fin, int virg) {
        if(deb >= fin || virg < 1) return 0.;
        long pow = (long)Math.pow(10, virg);
        long amin =  (long) (deb * pow);
        long amax =  (long) (fin * pow);
        long random = (long) (_randAlea.nextDouble() * (amax-amin) + amin);
        return (double) random/pow;
    }

    public static long randomNumber(long min, long max)
    {
        return (long)(_randUniform.nextDouble() * (max-min+1) + min);
    }

    public static double doubleRandomNumber(long min, long max){
        return _randUniform.nextDouble() * (max-min+1) + min;
    }

    public static long randomNumberForNonUniform(long min, long max){
        return (long)(_randNonUniform.nextDouble() * (max-min+1) + min);
    }

    public static long nonUniformRandom(long type, long x, long min, long max)
    {
        return (((randomNumberForNonUniform(0, x) | randomNumberForNonUniform(min, max)) + type) % (max-min+1)) + min;
    }

    public static long generateId(long min, long max, boolean isPassiveReplication, int indexNode, int indexThread, int numNodes, int numThreads, double overlap){
        int totalNumberOfThreads= numNodes*numThreads;
        long totalIds=max-min+1;
        long newMin;
        long newMax;

        //I solve:
        //(numOverlap+numNoOverlap)*totalNumberOfThreads=totalIds
        //2*numOverlap=overlap*(numNoOverlap+2*numOverlap)
        long numOverlap=(long)((overlap*totalIds)/(totalNumberOfThreads*(2-overlap)));
        long numNoOverlap=(long)((2*totalIds*(1-overlap))/(totalNumberOfThreads*(2-overlap)));

        int intervalIndex;

        if(isPassiveReplication){
           intervalIndex=indexThread;


        }
        else{
           intervalIndex=indexNode*numThreads+indexThread;



        }

		if(numNoOverlap!=0 && numOverlap!=0){
			newMin=min+intervalIndex*(numOverlap+numNoOverlap);
			newMax=newMin+(2*numOverlap+numNoOverlap)-1;
		}
		else{
			newMin=min;
			newMax=max;
		}	

        long result= randomNumber(newMin, newMax);

        if(numOverlap!=0 && numNoOverlap!=0){//Cyclic interval

            if(isPassiveReplication && (indexThread==(numThreads-1))){
               if(result>=(newMax-numOverlap+1)){
                   result=result%((numOverlap+numNoOverlap)*numThreads);
               }

            }
            else if(!isPassiveReplication && (indexThread==(numThreads-1)) && (indexNode==(numNodes-1))){
               if(result>=(newMax-numOverlap+1)){
                   result=result%((numOverlap+numNoOverlap)*totalNumberOfThreads);
               }
            }
        }
		
        return result;


    }
    
    public static List<Integer> selectLocalWarehouse(CacheWrapper wrapper){
    	
    	
    	if(wrapper.isFullyReplicated()){
    		return null;
    	}
    	else{

    		List<Integer> result = new ArrayList<Integer>();

    		Warehouse w;
    		for (int i = 1; i <= TPCCPopulation.NB_WAREHOUSE; i++) {

    			w=new Warehouse();
    			w.setW_id(i);
    			if(w.isLocal(wrapper)){
    				result.add(i);
    			}
    		}

    		return result;
    	}
    }


}
