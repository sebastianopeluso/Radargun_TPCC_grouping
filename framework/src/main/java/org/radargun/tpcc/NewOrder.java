package org.radargun.tpcc;

import org.radargun.CacheWrapper;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: sebastiano
 * Date: 4/26/11
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class NewOrder implements Serializable {


    private long no_o_id;
    private long no_d_id;
    private long no_w_id;

    public NewOrder() {
    }

    public NewOrder(long no_o_id, long no_d_id, long no_w_id) {
        this.no_o_id = no_o_id;
        this.no_d_id = no_d_id;
        this.no_w_id = no_w_id;
    }
    
    


    public long getNo_o_id() {
        return no_o_id;
    }

    public long getNo_d_id() {
        return no_d_id;
    }

    public long getNo_w_id() {
        return no_w_id;
    }

    public void setNo_o_id(long no_o_id) {
        this.no_o_id = no_o_id;
    }

    public void setNo_d_id(long no_d_id) {
        this.no_d_id = no_d_id;
    }

    public void setNo_w_id(long no_w_id) {
        this.no_w_id = no_w_id;
    }
    
    private TPCCKey getKey(){
        return new TPCCKey("NEWORDER_"+this.no_w_id+"_"+this.no_d_id+"_"+this.no_o_id, (int)this.no_w_id, false);
    }

    

    public void store(CacheWrapper wrapper)throws Throwable{

        wrapper.put(null,this.getKey(), this);
    }

    public void insert(CacheWrapper wrapper)throws Throwable{

        wrapper.put(null,this.getKey(), this);
    }

}
