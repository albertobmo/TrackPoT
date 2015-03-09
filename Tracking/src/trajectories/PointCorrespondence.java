package trajectories;


/**
 * @author pedro
 *
 * Correspondence between the last known node of a trajectory, and its predicted position for the
 * current time. The class also stores the own trajectory, for fast access to trajectory updates.
 */
public class PointCorrespondence {
	
	/**
	 * Last known position of the trajectory.
	 */
	public TrajectoryNode actual;
	/**
	 * Predicted position for the trajectory for the current time
	 */
	public TrajectoryNode predicted;
	/**
	 * Reference to the own trajectory, for fast access to trajectory updates.
	 */
	public Trajectory trajectory;
	/**
	 * Status of the point correspondence (optical flow usage)
	 */
	public byte status;
	
	public PointCorrespondence(final TrajectoryNode lastNode, final TrajectoryNode prediction, final Trajectory trajectory) {
		this.actual = new TrajectoryNode(lastNode);
		this.predicted = new TrajectoryNode(prediction);
		//Store a reference to the trajectory, for fast access when updating.
		this.trajectory = trajectory;
		this.status = 0;
	}
	
	public String toString() {
		String str = "S: "+actual+"-E:"+predicted+" T:"+status;
		return str;
		
	}
	
}
