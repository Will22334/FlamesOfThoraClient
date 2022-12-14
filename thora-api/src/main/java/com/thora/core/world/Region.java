package com.thora.core.world;

/**
 * A queryable set of {@link Location points}. Spatial properties such as continuity, shape or size are undefined.
 * @author Dave
 *
 */
public interface Region extends HasWorld {
	
	/**
	 * Returns {@code true} if the given {@link Locatable} is inside this Region.
	 * @param loc the location in question
	 * @return {@code true} if the Location is inside this {@link Region},
	 * {@code false} if the Location is outside.
	 */
	public boolean contains(Locatable loc);
	
}
