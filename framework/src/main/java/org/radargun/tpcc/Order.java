package org.radargun.tpcc;

import org.radargun.CacheWrapper;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: sebastiano
 * Date: 4/26/11
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class Order implements Serializable, Comparable {

    private long o_id;
    private long o_d_id;
    private long o_w_id;
    private long o_c_id;
    private long o_entry_d;
    private long o_carrier_id;
    private int o_ol_cnt;
    private int o_all_local;

    public Order() {
    }

    public Order(long o_id, long o_d_id, long o_w_id, long o_c_id, Date o_entry_d, long o_carrier_id, int o_ol_cnt, int o_all_local) {
        this.o_id = o_id;
        this.o_d_id = o_d_id;
        this.o_w_id = o_w_id;
        this.o_c_id = o_c_id;
        this.o_entry_d = (o_entry_d==null)?-1:o_entry_d.getTime();
        this.o_carrier_id = o_carrier_id;
        this.o_ol_cnt = o_ol_cnt;
        this.o_all_local = o_all_local;
    }
    
    

    public long getO_id() {
        return o_id;
    }

    public long getO_d_id() {
        return o_d_id;
    }

    public long getO_w_id() {
        return o_w_id;
    }

    public long getO_c_id() {
        return o_c_id;
    }

    public Date getO_entry_d() {
        return o_entry_d==-1?null:new Date(o_entry_d);
    }

    public long getO_carrier_id() {
        return o_carrier_id;
    }

    public int getO_ol_cnt() {
        return o_ol_cnt;
    }

    public int getO_all_local() {
        return o_all_local;
    }

    public void setO_id(long o_id) {
        this.o_id = o_id;
    }

    public void setO_d_id(long o_d_id) {
        this.o_d_id = o_d_id;
    }

    public void setO_w_id(long o_w_id) {
        this.o_w_id = o_w_id;
    }

    public void setO_c_id(long o_c_id) {
        this.o_c_id = o_c_id;
    }

    public void setO_entry_d(Date o_entry_d) {
        this.o_entry_d = (o_entry_d==null)?-1:o_entry_d.getTime();
    }

    public void setO_carrier_id(long o_carrier_id) {
        this.o_carrier_id = o_carrier_id;
    }

    public void setO_ol_cnt(int o_ol_cnt) {
        this.o_ol_cnt = o_ol_cnt;
    }

    public void setO_all_local(int o_all_local) {
        this.o_all_local = o_all_local;
    }
    
    private TPCCKey getKey(){
        return new TPCCKey("ORDER_"+this.o_w_id+"_"+this.o_d_id+"_"+this.o_id, (int)this.o_w_id, false);
    }

    

    public void store(CacheWrapper wrapper)throws Throwable{

        wrapper.put(null,this.getKey(), this);
    }

    public boolean load(CacheWrapper wrapper)throws Throwable{

        Order loaded=(Order)wrapper.get(null,this.getKey());

        if(loaded==null) return false;


        this.o_c_id=loaded.o_c_id;
        this.o_carrier_id=loaded.o_carrier_id;
        this.o_entry_d=loaded.o_entry_d;
        this.o_ol_cnt=loaded.o_ol_cnt;
        this.o_all_local=loaded.o_all_local;


        return true;
    }


    //For a decreasing order in sort operation
    @Override
    public int compareTo(Object o) {
        if(o==null || !(o instanceof Order)) return -1;

        Order other=(Order) o;

        if(this.o_id==other.o_id)return 0;
        else if(this.o_id>other.o_id)return -1;
        else return 1;
    }
}
