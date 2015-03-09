package trajectories;

import org.opencv.core.Point;

/**
 * @author pedro
 * 
 *         Dynamic filter, to predict trajectory nodes coordinates from previous
 *         positions.
 */
public class DynamicFilter {

	/**
	 * Parameters that keep the state of the dynamic filter
	 */
	Point position;
	Point velocity;
	/**
	 * Time corresponding to the last update of the filter. It need not be the
	 * previous frame, due to occlusions or optical flow errors.
	 */
	int time;
	/**
	 * Filter gain. TODO: Variable filter gain
	 */
	double gain;

	/**
	 * Correction factor for filter gain (see update method).
	 */
	double correction = 0.5;
	/**
	 * Constructor: Initialize the filter with the first measure.
	 * 
	 * @param start
	 *            First position (coordinates) of the filter, for the given time
	 */
	public DynamicFilter(final Point start, final int time) {
		position = start.clone();
		velocity = new Point(0, 0);
		this.time = time;
		gain = 1.0;
	}
 
	/**
	 * update dynamic filter status with a new measure.
	 * 
	 * @param value
	 *            new measure
	 * @param currentTime
	 *            time for the new measure (always greater than filter time).
	 */
	public void update(final Point value, final int currentTime) {
		// Estimate the new position, without correction
		float timeDiff = currentTime - time;
		Point projection = predict(currentTime);
		// Estimate velocity
		Point newVelocity = new Point((value.x - position.x) / timeDiff,
				(value.y - position.y) / timeDiff);
		// Correct position using the new measure
		position.x = projection.x + gain * (value.x - projection.x);
		position.y = projection.y + gain * (value.y - projection.y);
		// Correct velocity using the new measure
		velocity.x += gain * (newVelocity.x - velocity.x);
		velocity.y += gain * (newVelocity.y - velocity.y);
		// Correct filter gain:
		gain -= correction;
		correction/=2;
		time = currentTime;
	}

	/**
	 * Predict the position for the given time. The required time must always be
	 * greater than the actual filter time, but this is not checked by the
	 * function.
	 * 
	 * @param currentTime
	 *            time where the position is to be estimated.
	 * @return Predicted coordinates
	 */
	public Point predict(final float currentTime) {
		// Compute point shift according to its current velocity.
		float timeDiff = currentTime - time;
		Point shift = new Point(velocity.x * timeDiff, velocity.y * timeDiff);
		// Estimate new position.
		Point estimation = new Point(position.x + shift.x, position.y + shift.y);
		return estimation;
	}
}
