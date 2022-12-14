package com.thora.client;

public class IntVector {
	
	public int x, y;
	
	public IntVector(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public IntVector() {
		this(0, 0);
	}

	public final int maxRectLength() {
		return Math.max(Math.abs(x), Math.abs(y));
	}
	
	public final boolean isZero() {
		return equals(0,0);
	}
	
	public IntVector clear() {
		x = y = 0;
		return this;
	}

	@Override
	public int hashCode() {
		return 31 * (31 + x) + y;
	}

	@Override
	public boolean equals(Object o) {
		if(o == null) return false;
		if(this == o) return true;
		if(o instanceof IntVector) {
			IntVector v = (IntVector) o;
			return x == v.x && y == v.y;
		}
		return false;
	}
	
	public boolean equals(int x, int y) {
		return this.x == x && this.y == y;
	}
	
	@Override
	protected IntVector clone() throws CloneNotSupportedException {
		return new IntVector(x, y);
	}

	@Override
	public String toString() {
		return "[" + x + ", " + y + "]";
	}
	
}
