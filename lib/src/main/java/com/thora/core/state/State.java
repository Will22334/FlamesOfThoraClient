package com.thora.core.state;

public abstract class State {
	
	private int id;
	
	private String stateName;
	
	public State(String name, int id) {
		
		stateName = name;
		this.id = id;
	}
	
	public String getStateName() {
		return stateName;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	@Override
	public String toString() {
		return getStateName();
	}
	
}
