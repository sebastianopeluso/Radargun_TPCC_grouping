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
public class Customer implements Serializable, Comparable {

    private long c_w_id;
    private long c_d_id;
    private long c_id;
    private String c_first;
    private String c_middle;
    private String c_last;
    private String c_street1;
    private String c_street2;
    private String c_city;
    private String c_state;
    private String c_zip;
    private String c_phone;
    private long c_since;
    private String c_credit;
    private double c_credit_lim;
    private double c_discount;
    private double c_balance;
    private double c_ytd_payment;
    private int c_payment_cnt;
    private int c_delivery_cnt;
    private String c_data;

    public Customer() {

    }

    public Customer(long c_w_id, long c_d_id, long c_id, String c_first, String c_middle, String c_last, String c_street1, String c_street2, String c_city, String c_state, String c_zip, String c_phone, Date c_since, String c_credit, double c_credit_lim, double c_discount, double c_balance, double c_ytd_payment, int c_payment_cnt, int c_delivery_cnt, String c_data) {
        this.c_w_id = c_w_id;
        this.c_d_id = c_d_id;
        this.c_id = c_id;
        this.c_first = c_first;
        this.c_middle = c_middle;
        this.c_last = c_last;
        this.c_street1 = c_street1;
        this.c_street2 = c_street2;
        this.c_city = c_city;
        this.c_state = c_state;
        this.c_zip = c_zip;
        this.c_phone = c_phone;
        this.c_since = (c_since==null)?-1:c_since.getTime();
        this.c_credit = c_credit;
        this.c_credit_lim = c_credit_lim;
        this.c_discount = c_discount;
        this.c_balance = c_balance;
        this.c_ytd_payment = c_ytd_payment;
        this.c_payment_cnt = c_payment_cnt;
        this.c_delivery_cnt = c_delivery_cnt;
        this.c_data=c_data;
    }
    
    

    public long getC_w_id() {
        return c_w_id;
    }

    public long getC_d_id() {
        return c_d_id;
    }

    public long getC_id() {
        return c_id;
    }

    public String getC_first() {
        return c_first;
    }

    public String getC_middle() {
        return c_middle;
    }

    public String getC_last() {
        return c_last;
    }

    public String getC_street1() {
        return c_street1;
    }

    public String getC_street2() {
        return c_street2;
    }

    public String getC_city() {
        return c_city;
    }

    public String getC_state() {
        return c_state;
    }

    public String getC_zip() {
        return c_zip;
    }

    public String getC_phone() {
        return c_phone;
    }

    public Date getC_since() {
        return (c_since==-1)?null:new Date(c_since);
    }

    public String getC_credit() {
        return c_credit;
    }

    public double getC_credit_lim() {
        return c_credit_lim;
    }

    public double getC_discount() {
        return c_discount;
    }

    public double getC_balance() {
        return c_balance;
    }

    public double getC_ytd_payment() {
        return c_ytd_payment;
    }

    public int getC_payment_cnt() {
        return c_payment_cnt;
    }

    public int getC_delivery_cnt() {
        return c_delivery_cnt;
    }

    public String getC_data() {
        return c_data;
    }

    public void setC_w_id(long c_w_id) {
        this.c_w_id = c_w_id;
    }

    public void setC_d_id(long c_d_id) {
        this.c_d_id = c_d_id;
    }

    public void setC_id(long c_id) {
        this.c_id = c_id;
    }

    public void setC_first(String c_first) {
        this.c_first = c_first;
    }

    public void setC_middle(String c_middle) {
        this.c_middle = c_middle;
    }

    public void setC_last(String c_last) {
        this.c_last = c_last;
    }

    public void setC_street1(String c_street1) {
        this.c_street1 = c_street1;
    }

    public void setC_street2(String c_street2) {
        this.c_street2 = c_street2;
    }

    public void setC_city(String c_city) {
        this.c_city = c_city;
    }

    public void setC_state(String c_state) {
        this.c_state = c_state;
    }

    public void setC_zip(String c_zip) {
        this.c_zip = c_zip;
    }

    public void setC_phone(String c_phone) {
        this.c_phone = c_phone;
    }

    public void setC_since(Date c_since) {
        this.c_since = (c_since==null)?-1:c_since.getTime();
    }

    public void setC_credit(String c_credit) {
        this.c_credit = c_credit;
    }

    public void setC_credit_lim(double c_credit_lim) {
        this.c_credit_lim = c_credit_lim;
    }

    public void setC_discount(double c_discount) {
        this.c_discount = c_discount;
    }

    public void setC_balance(double c_balance) {
        this.c_balance = c_balance;
    }

    public void setC_ytd_payment(double c_ytd_payment) {
        this.c_ytd_payment = c_ytd_payment;
    }

    public void setC_payment_cnt(int c_payment_cnt) {
        this.c_payment_cnt = c_payment_cnt;
    }

    public void setC_delivery_cnt(int c_delivery_cnt) {
        this.c_delivery_cnt = c_delivery_cnt;
    }

    public void setC_data(String c_data) {
        this.c_data = c_data;
    }
    
    private TPCCKey getKey(){
        return new TPCCKey("CUSTOMER_"+this.c_w_id+"_"+this.c_d_id+"_"+this.c_id, (int) this.c_w_id, false);
    }

    

    public void store(CacheWrapper wrapper)throws Throwable{

        wrapper.put(null,this.getKey(), this);
    }

    public boolean load(CacheWrapper wrapper)throws Throwable{

        Customer loaded=(Customer)wrapper.get(null,this.getKey());

        if(loaded==null) return false;

        this.c_balance=loaded.c_balance;
        this.c_city=loaded.c_city;
        this.c_credit=loaded.c_credit;
        this.c_credit_lim=loaded.c_credit_lim;
        this.c_data=loaded.c_data;
        this.c_delivery_cnt=loaded.c_delivery_cnt;
        this.c_discount=loaded.c_discount;
        this.c_first=loaded.c_first;
        this.c_last=loaded.c_last;
        this.c_middle=loaded.c_middle;
        this.c_payment_cnt=loaded.c_payment_cnt;
        this.c_phone=loaded.c_phone;
        this.c_since=loaded.c_since;
        this.c_state=loaded.c_state;
        this.c_street1=loaded.c_street1;
        this.c_street2=loaded.c_street2;
        this.c_ytd_payment=loaded.c_ytd_payment;
        this.c_zip=loaded.c_zip;


        return true;
    }

    @Override
    public int compareTo(Object o) {
        if(o==null || !(o instanceof Customer)) return -1;

        Customer other=(Customer) o;
        if(this.c_first!=null)
            return this.c_first.compareTo(other.c_first);
        else if(other.c_first!=null)
            return 1;
        else
            return 0;
    }
}
