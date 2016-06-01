package com.deleidos.dp.profiler;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.deleidos.dp.enums.GroupingBehavior;

public class VectorOrderedProfilerKey {
	public final static Integer UNORDERED = -1;
	public final static Integer ABSENT = 0;
	private String name;
	private Vector<Integer> orderVector;

	public VectorOrderedProfilerKey(String name, Vector<Integer> orderVector) {
		this.name = name;
		this.orderVector = orderVector;
	}

	public VectorOrderedProfilerKey(String name, Integer ... orderIntegers) {
		orderVector = new Vector<Integer>();
		if(orderIntegers.length == 0) {
			orderVector.add(-1);
		} else {
			for(Integer i : orderIntegers) {
				orderVector.add(i);
			}
		}
		this.name = name;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Vector<Integer> getOrderVector() {
		return orderVector;
	}
	public void setOrderVector(Vector<Integer> orderVector) {
		this.orderVector = orderVector;
	}

	@Override
	public boolean equals(Object object) {
		if(!(object instanceof VectorOrderedProfilerKey)) {
			return false;
		} else {
			VectorOrderedProfilerKey otherRecord = (VectorOrderedProfilerKey) object;
			if(otherRecord.getName().equals(this.getName()) && otherRecord.getOrderVector().equals(this.getOrderVector())) {
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public int hashCode() {
		return getName().hashCode() + getOrderVector().hashCode();
	}
	
	@Override
	public String toString() {
		StringBuilder vectorOutput = new StringBuilder("[");
		orderVector.forEach((x)->vectorOutput.append(x.toString() + ","));
		return getName() + vectorOutput.toString().substring(0, vectorOutput.length()-1) + "]";
	}

}

