package trajectories;

/**
 * 
 * @author alberto
 * 
 * Auxiliar class, for the point search class. It extend PointDetection class with a boolean
 * check to mark the point when it has already been used.
 *
 */
public class PointData {
	
	
	/**
	 * Actual point detection data.
	 */
	public PointDetection point;
	/**
	 * Used / unused check 
	 */
	public boolean used;
	
	/**
	 * Constructor with the two basic parameters.
	 * @param point
	 * @param used
	 */
	public PointData(final PointDetection point, final boolean used) {
		this.point = new PointDetection(point);
		this.used = used;		
	}
	/**
	 * Copy constructor.
	 * @param point The object to be copied.
	 */
	public PointData(final PointData point) {
		this.point = new PointDetection(point.point);
		this.used = point.used;
	}	
}
