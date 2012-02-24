package org.radargun.tpcc;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.infinispan.container.key.ContextAwareKey;

public class ItemKey implements Externalizable, ContextAwareKey{
	
	private String key;
	
	
	public ItemKey(String key){
		this.key=key;
		
	}

    public ItemKey(){
    	this.key=null;
    	
    }
    
    
    public String getKey() {
		return key;
	}


	public void setKey(String key) {
		this.key = key;
	}
	
	
	
	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
	
		this.key = in.readUTF();
		
		
		
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		
		out.writeUTF(this.key);
		
		
	}
	
	/**
	 * Objects of this class identify values which are not modified in a transactional context
	 */
	@Override
	public boolean identifyImmutableValue(){
		
		return true;
	}
	
	
	
	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ItemKey other = (ItemKey) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ItemKey [key=" + key + "]";
	}

}
