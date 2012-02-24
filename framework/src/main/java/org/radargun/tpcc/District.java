package org.radargun.tpcc;


import org.radargun.CacheWrapper;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: sebastiano
 * Date: 4/26/11
 * Time: 4:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class District implements Serializable {

    private long d_w_id;
    private long d_id;
    private String  d_name;
    private String  d_street1;
    private String  d_street2;
    private String  d_city;
    private String  d_state;
    private String  d_zip;
    private double d_tax;
    private double d_ytd;
    private long d_next_o_id;


    public District() {
    }

    public District(long d_w_id, long d_id, String d_name, String d_street1, String d_street2, String d_city, String d_state, String d_zip, double d_tax, double d_ytd, long d_next_o_id) {
        this.d_w_id = d_w_id;
        this.d_id = d_id;
        this.d_name = d_name;
        this.d_street1 = d_street1;
        this.d_street2 = d_street2;
        this.d_city = d_city;
        this.d_state = d_state;
        this.d_zip = d_zip;
        this.d_tax = d_tax;
        this.d_ytd = d_ytd;
        this.d_next_o_id = d_next_o_id;
    }
    
    


    public long getD_w_id() {
        return d_w_id;
    }

    public long getD_id() {
        return d_id;
    }

    public String getD_name() {
        return d_name;
    }

    public String getD_street1() {
        return d_street1;
    }

    public String getD_street2() {
        return d_street2;
    }

    public String getD_city() {
        return d_city;
    }

    public String getD_state() {
        return d_state;
    }

    public String getD_zip() {
        return d_zip;
    }

    public double getD_tax() {
        return d_tax;
    }

    public double getD_ytd() {
        return d_ytd;
    }

    public long getD_next_o_id() {
        return d_next_o_id;
    }

    public void setD_w_id(long d_w_id) {
        this.d_w_id = d_w_id;
    }

    public void setD_id(long d_id) {
        this.d_id = d_id;
    }

    public void setD_name(String d_name) {
        this.d_name = d_name;
    }

    public void setD_street1(String d_street1) {
        this.d_street1 = d_street1;
    }

    public void setD_street2(String d_street2) {
        this.d_street2 = d_street2;
    }

    public void setD_city(String d_city) {
        this.d_city = d_city;
    }

    public void setD_state(String d_state) {
        this.d_state = d_state;
    }

    public void setD_zip(String d_zip) {
        this.d_zip = d_zip;
    }

    public void setD_tax(double d_tax) {
        this.d_tax = d_tax;
    }

    public void setD_ytd(double d_ytd) {
        this.d_ytd = d_ytd;
    }

    public void setD_next_o_id(long d_next_o_id) {
        this.d_next_o_id = d_next_o_id;
    }
    
    private TPCCKey getKey(){
        return new TPCCKey("DISTRICT_"+this.d_w_id+"_"+this.d_id,(int )this.d_w_id, false);
    }

    public void store(CacheWrapper wrapper)throws Throwable{

        wrapper.put(null,this.getKey(), this);
    }

    public boolean load(CacheWrapper wrapper)throws Throwable{

        District loaded=(District)wrapper.get(null,this.getKey());

        if(loaded==null) return false;

        this.d_city=loaded.d_city;
        this.d_name=loaded.d_name;
        this.d_next_o_id=loaded.d_next_o_id;
        this.d_state=loaded.d_state;
        this.d_street1=loaded.d_street1;
        this.d_street2=loaded.d_street2;
        this.d_tax=loaded.d_tax;
        this.d_ytd=loaded.d_ytd;
        this.d_zip=loaded.d_zip;


        return true;
    }
}
