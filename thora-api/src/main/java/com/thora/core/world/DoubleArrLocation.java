package com.thora.core.world;

import java.lang.ref.WeakReference;

import com.thora.core.math.DoubleArrVector;

public abstract class DoubleArrLocation extends Location {
	
	public static class DoubleArrRefLocation<W extends AbstractWorld> extends Location implements Cloneable {
		
		private WeakReference<W> worldRef;
		private final DoubleArrVector v;
		
		public DoubleArrRefLocation(W world, double x, double y) {
			this(world, new DoubleArrVector(x, y));
		}
		
		public DoubleArrRefLocation(W world, double[] comps) {
			this(world, new DoubleArrVector(comps));
		}
		
		public DoubleArrRefLocation(W world, DoubleArrVector v) {
			this.worldRef = new WeakReference<>(world);
			this.v = v;
		}
		
		@Override
		public W world() {
			return worldRef.get();
		}

		@Override
		public DoubleArrRefLocation<W> setAs(int x, int y) {
			this.v.setAs(x, y);
			return this;
		}

		@Override
		public DoubleArrVector asVector() {
			return v;
		}

		@Override
		public double[] comps() {
			return asVector().comps();
		}

		@Override
		public DoubleArrRefLocation<W> clone() {
			return new DoubleArrRefLocation<>(world(), asVector().clone());
		}
		
	}
	
	private final double[] comps;

	DoubleArrLocation(double[] comps) {
		this.comps = comps;
	}

	public DoubleArrLocation(double x, double y) {
		this(new double[] {x, y});
	}

	public double getRX() {
		return comps[0];
	}

	public double getRY() {
		return comps[1];
	}

	@Override
	public DoubleArrLocation setAs(int x, int y) {
		this.comps[0] = x;
		this.comps[1] = y;
		return this;
	}

	public DoubleArrLocation setAs(double x, double y) {
		this.comps[0] = x;
		this.comps[1] = y;
		return this;
	}
	
	public DoubleArrLocation comps(double[] arr, int index) {
		arr[index] = getRX();
		arr[index] = getRY();
		return this;
	}
	
	public DoubleArrLocation comps(double[] arr) {
		comps(arr, 0);
		return this;
	}
	
	@Override
	public abstract DoubleArrLocation clone();

}
