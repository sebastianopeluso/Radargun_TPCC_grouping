package org.radargun.tpcc;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.infinispan.container.key.ContextAwareKey;
import org.infinispan.distribution.ch.StaticGroupSlice;
import org.infinispan.distribution.group.Group;
import org.radargun.CacheWrapper;

public class TPCCKey implements Externalizable, StaticGroupSlice, ContextAwareKey{
	
	private String key;
	private int group;
	private boolean identifyImmutable;
	
	public TPCCKey(String key, int group, boolean identifyImmutable){
		this.key=key;
		this.group=group-1;
		this.identifyImmutable = identifyImmutable;
	}

	public String getKey() {
		return key;
	}

	
	

    public TPCCKey(){
    	this.key=null;
    	this.group=-1;
    	this.identifyImmutable = false;
    }


	public void setKey(String key) {
		this.key = key;
	}

	

	public int getGroup() {
		return group+1;
	}

	public void setGroup(int group) {
		this.group = group-1;
	}
	
	

	public boolean isIdentifyImmutable() {
		return identifyImmutable;
	}

	public void setIdentifyImmutable(boolean identifyImmutable) {
		this.identifyImmutable = identifyImmutable;
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
		TPCCKey other = (TPCCKey) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TPCCKey [key=" + key + "]";
	}

	@Override
	public int getSlice() {
		
		return this.group;
	}
	
	
	public boolean isLocal(CacheWrapper wrapper){
		return wrapper.isLocal(this, this.group);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
	
		this.key = in.readUTF();
		
		this.group = in.readInt();
		
		this.identifyImmutable = in.readBoolean();
		
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		
		out.writeUTF(this.key);
		out.writeInt(this.group);
		out.writeBoolean(this.identifyImmutable);
		
	}

	@Override
	public boolean identifyImmutableValue() {
		
		return this.identifyImmutable;
	}
	
	
	
	

}
