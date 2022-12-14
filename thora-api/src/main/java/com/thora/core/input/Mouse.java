package com.thora.core.input;

import com.thora.core.world.Location;
import com.thora.core.world.WeakVectorLocation;

public class Mouse {
	
	private Location loc;
	private int scrollDelta;
	
	public void setLocation(int x, int y) {
		
		loc = new WeakVectorLocation<>(null, x,y);
		
	}
	
	public Location getMouseLocation() {
		
		return loc;
		
	}
	
	public int getScrollDelta() {
		return scrollDelta;
	}
	
	public int popScrollDelta() {
		int scroll = scrollDelta;
		scrollDelta = 0;
		return scroll;
	}
	
	public Mouse setSCrollDelta(int scroll) {
		this.scrollDelta = scroll;
		return this;
	}
	
}
