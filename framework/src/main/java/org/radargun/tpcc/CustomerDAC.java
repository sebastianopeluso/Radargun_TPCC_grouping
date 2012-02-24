package org.radargun.tpcc;

import org.radargun.CacheWrapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: sebastiano
 * Date: 4/27/11
 * Time: 6:00 PM
 * To change this template use File | Settings | File Templates.
 */
public final class CustomerDAC {

    private CustomerDAC(){}

    public static List<Customer> loadByCLast(CacheWrapper cacheWrapper, long c_w_id, long c_d_id, String c_last) throws Throwable{

        List<Customer> result=new ArrayList<Customer>();

        Customer current=null;
        boolean found=false;

        for (int i = 1; i <= TPCCTools.NB_MAX_CUSTOMER; i++) {

            current=new Customer();

            current.setC_id(i);
            current.setC_d_id(c_d_id);
            current.setC_w_id(c_w_id);

            found=current.load(cacheWrapper);
            if(found && current.getC_last() !=null && current.getC_last().equals(c_last)){

                result.add(current);

            }


        }

        return result;


    }
    
    public static List<Customer> loadByCLast2(CacheWrapper cacheWrapper, long c_w_id, long c_d_id, String c_last) throws Throwable{

        List<Customer> result=new ArrayList<Customer>();
        
        CustomerLookup customerLookup = new CustomerLookup(c_last, c_w_id, c_d_id);
        
        customerLookup.load(cacheWrapper);
        
        Customer current=null;
        boolean found = false;
        if(customerLookup.getIds() != null){
        	
        	Iterator<Long> itr = customerLookup.getIds().iterator();
        	
        	while(itr.hasNext()){
        		
        		long c_id = itr.next();
        		
        		current=new Customer();

                current.setC_id(c_id);
                current.setC_d_id(c_d_id);
                current.setC_w_id(c_w_id);
                
                found = current.load(cacheWrapper);
                
                if(found){
                	result.add(current);
                }
        		
        	}
        }

        return result;


    }
}
