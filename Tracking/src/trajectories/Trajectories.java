package trajectories;

import java.util.*;

import org.opencv.core.*;
import org.opencv.imgproc.*;
import org.opencv.video.*;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

import java.io.*;


public class Trajectories {

	/**
	 * Maximum number of occlusions allowed to trajectories, before the trajectory finishes.
	 */
	public static final int MAX_OCCLUSIONS = 5;
	/**
	 * List of active trajectories. Trajectories are created and destroyed as they appear and
	 * disappear from the video.
	 */
	LinkedList<Trajectory> trajectories;
	/**
	 * Current time for the video. Time starts at 0 for the first frame.
	 */
	int time;
	/**
	 * Buffer to store the last frames of the video. The number of images to store coincides with
	 * the maximum number of occlusions allowed to the trajectory, for the optical flow algorithm to
	 * compute it from the proper image.
	 */
	Mat[] buffer=new Mat[MAX_OCCLUSIONS];
	
	/**
	 * Auxiliary structure, to optimize searching of points
	 */
	PointSearch search;
	
	/**
	 * Point correspondences (actual position - predicted position) for each trajectory.
	 */
	ArrayList<ArrayList<PointCorrespondence>> positions;
	
	Document doc;
	Element xmlElement;
	/**
	 * Constructor. Initialize all the parameters
	 * @throws ParserConfigurationException 
	 */
	public Trajectories(final Mat image) throws ParserConfigurationException {
		//Initialize object members:
		//List of trajectories.
		trajectories = new LinkedList<Trajectory>();
		//Initial time for the video. Start at 0.
		time = 0;
		//Creating structure for the point search. For each frame, it will be populated with incoming points.
		search = new PointSearch(image.height(), image.width());
		//initialize list of correspondences.
		positions = new ArrayList<ArrayList<PointCorrespondence>>();
		for (int i=0; i<MAX_OCCLUSIONS; i++) {
			positions.add(new ArrayList<PointCorrespondence>());
		}
		//Create the document XML
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		doc = docBuilder.newDocument();
		
		//Create parent element VideoAnnotation and set date and time of video.
		xmlElement = doc.createElement("VideoAnnotation");
		doc.appendChild(xmlElement);
		Attr attrDate = doc.createAttribute("date");		
		Date date = new Date();
		attrDate.setValue( date.toString());
		//Include attribute date into Videoannotation element		
		xmlElement.setAttributeNode(attrDate);
		
		correct(image, positions);
		time++;
	}
	
	public void close(final String videoFile) throws TransformerException, ParserConfigurationException {
		//Save the remaining trajectories.
		for (Trajectory tr : trajectories) {
			if (tr.end()-tr.start()>MAX_OCCLUSIONS)
				tr.writeXML(doc, xmlElement);
		}
		//Create XML file before deleting the object.
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(videoFile));
		transformer.transform(source, result);
	}
	/**
	 * Main function of the class. This function takes the current frame of the video, and extract
	 * the point trajectories. The functión also create new trajectories, and delete finished ones.
	 * @param image new image of the video sequence.
	 * @throws ParserConfigurationException 
	 */
	public void update(final Mat image) throws ParserConfigurationException {

		//Predict positions for all active trajectories (and remove finished trajectories).
		positions = predict();
		//Measure actual trajectory positions in the new image, through opticl flow computation.
		measure(image, positions);
		//Correct trajectory positions with interest point coordinates (and create new trajectories).
		correct(image, positions);
		//Update the object time
		time++;
	}
	
	/**
	 * Predict position for all the trajectories, for the current time. Also, for the trajectories
	 * that have reached the maximum number of occlusions, remove then from the list.
	 * @return: Vector with correspondences for all active trajectories for the current time, organized
	 * by number of occlusions.
	 * @throws ParserConfigurationException 
	 */
	private ArrayList<ArrayList<PointCorrespondence>> predict() throws ParserConfigurationException {
		//Return array. A list of MAX_OCCLUSIONS lists need to be created, each one for all the trajectories
		//  with a given number of occlusions for the current time. For instance, the index 0 is for
		//  trajectories which do not have any occlusions for this time.
		
		//Remove all the correspondences for the previous frame.
		for (ArrayList<PointCorrespondence> p : positions)
			p.clear();
	
		//Create a list iterator, since removal from the trajectories linked list need to be performed,
		//  that is, the finished trajectories need to be deleted.
		ListIterator<Trajectory> it = trajectories.listIterator();
		while (it.hasNext()) {
			Trajectory tr = it.next();
			//Get last known point of the trajectory, and the prediction for it, for the current time.
			PointCorrespondence point = tr.predict(time);
			//Check if the trajectory is finished.
			int index = time-point.actual.time-1;
			if (index>=MAX_OCCLUSIONS) {
				//Check if it is a valid trajectory, that is, is duration in larger than the minimum required.
				if (tr.end()-tr.start()>MAX_OCCLUSIONS)
					//If it is a valid trajectory, write it in the output file before remove it.
					tr.writeXML(doc, xmlElement);
				it.remove();
			}
			else
				//Add the point correspondence to the corresponding list.
				positions.get(index).add(point);
		}
		return positions;
	}
	
	/**
	 * Get actual position for each trajectory from the optical flow computation between current
	 * image, and previous images stored in the image buffer. The function update end coordinates for
	 * each point, and remove the points for which the optical flow do not find correspondence.
	 * @param image: Current image for optical flow computation.
	 * @param positions: Initial guess for optical flow point correspondences. For each element, actual
	 * is the actual interest point detected in previous image, and predicted is the prediction for that
	 * point in the current image.
	 */
	private void measure(final Mat image, final ArrayList<ArrayList<PointCorrespondence>> positions) {
		for (int i = 0; i < MAX_OCCLUSIONS; i++) {
			//For each image in the buffer (o correspondingly, for all the trajectories with a given 
			//  number of occlusions), compute the optical flow.
			ArrayList<PointCorrespondence> points = positions.get(i);
			//If there is not any point, it is not needed to run the optical flow for this index.
			if (points.size()==0) continue;
			ArrayList<Point> st = new ArrayList<Point>(); 
			ArrayList<Point> ed = new ArrayList<Point>(); 
			for (PointCorrespondence p : points) {
				st.add(p.actual.point.position);
				ed.add(p.predicted.point.position);
			}
			//Parameter conversion for Optical Flow function.
			MatOfPoint2f points1 = new MatOfPoint2f();
			points1.fromList(st);
			MatOfPoint2f points2 = new MatOfPoint2f();
			points2.fromList(ed);
			
			MatOfByte status = new MatOfByte();
			MatOfFloat err = new MatOfFloat();
			
			TermCriteria term = new TermCriteria(TermCriteria.MAX_ITER|TermCriteria.EPS, 10, 0.1);
			Size winSize= new Size(9, 9);
			//Optical flow computation.
			Video.calcOpticalFlowPyrLK(getBufferImage(i+1), image, points1, points2, status, err, 
					winSize, 3, term, Video.OPTFLOW_USE_INITIAL_FLOW, 0.1);

			//Update points prediction with the values computed with the optical flow.
			int n = 0;
			byte[] val = status.toArray();
			Point[] pt2 = points2.toArray();
			for (PointCorrespondence p : points) {
				//Update coordinates
				p.predicted.point.position = pt2[n];
				//Update status (0: point not found).
				p.status = val[n];
				n++;
			}
		}
//		/////////////////////////////////////////////////////////////////
//		Mat img = image.clone();
//		Scalar color = new Scalar(0xFF, 0x00, 0x00);
//		for (ArrayList<PointCorrespondence> pc : positions) {
//			for (PointCorrespondence p : pc) {
//				Core.line(img, p.start.point.position, p.end.point.position, color, 2);
//			}
//		}
//		Highgui.imwrite("flow.png", img);
//		/////////////////////////////////////////////////////////////////

	}
	
	/**
	 * Correct coordinates for end positions with actual interest point coordinates, and update successful
	 * trajectories, adding the new node.
	 * @param image: Current image for interest point computation.
	 * @param positions: Point correspondences obtained with optical flow algorithm.
	 */
	private void correct(final Mat image, final ArrayList<ArrayList<PointCorrespondence>> positions) {
		//Detect interest points in the image.
		MatOfPoint crn = new MatOfPoint();
		Mat gray = new Mat();
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		Imgproc.goodFeaturesToTrack(gray, crn, 200, 0.01, 4);
		Point[] corners = crn.toArray();
		
//		/////////////////////////////////////////////////////////////////
//		Mat img = image.clone();
//		Scalar color = new Scalar(0xFF, 0x00, 0x00);
//		for (Point p : corners) {
//			Core.circle(img, p, 2, color, 1);
//		}
//		Highgui.imwrite("points.png", img);
//		/////////////////////////////////////////////////////////////////
		
		
		//Create the list of detection points for the point search object.
		int size = crn.rows();
		ArrayList<PointData> points = new ArrayList<PointData>(size);
		for (Point p : corners) {
			points.add(new PointData(new PointDetection( p, 1), false));
		}
		//Create the point search object, to optimize point correspondence search.
		search.Init(points);
		
		//For each trajectory prediction, find its corresponding interest point in the current
		//  image (if it exists).
		for (ArrayList<PointCorrespondence> pt : positions) {
			for (PointCorrespondence p : pt) {
				//If the point has not found its corresponding point in the optical flow step, do not
				//  update its trajectory.
				if (p.status==0) continue;
				PointData newPoint = search.FindPoint(p.predicted.point); 
				if (newPoint != null) {
					//Update the trajectory last known point with the coordinates of the closest
					//  interest point.
					//NOTE: It is preferable to change optical flow coordinates by interest point
					//  coordinates, since the last are better tracked.
					if (p.trajectory.update(newPoint.point, time))
						newPoint.used = true;
				}
			}
		}
		
		//For the points not used, create new trajectories.
		for (PointData p : points){
			if (!p.used) {
				trajectories.add(new Trajectory(p.point, time));
			}
		}
		//Store the new image in the image buffer.
		updateBuffer(image);

	}
	/**
	 * The buffer is a circular buffer, and when a new image comes to the object, this image
	 * substitute the oldest one. This way, the buffer keeps a copy of the last five images of the
	 * video.
	 * @param image: new image
	 */
	private void updateBuffer(final Mat image) {
		//Substitute the oldest image with the new one.
		buffer[time % MAX_OCCLUSIONS] = image.clone();
	}
	
	private Mat getBufferImage(final int index) {
		int diff = (time-index) % MAX_OCCLUSIONS;
		return buffer[diff];
	}
	
	public String toString() {
		String str="";
		for (Trajectory tr : trajectories) {
			str = str + tr + "\n";
		}
		return str;
	}
	
	public void DrawTrajectories(final Mat image) {
		Scalar color = new Scalar(0, 0, 0);
		for (Trajectory tr : trajectories) {
			tr.drawPath(image, color);
		}
	}
	
}
