package trajectories;

import java.util.ArrayList;

import java.util.Iterator;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.w3c.dom.*;
import javax.xml.parsers.*;

/**
 * @author pedro
 *
 *Class to store all the nodes of the point trajectory
 */
public class Trajectory {

	/**
	 * List of nodes which composes the trajectory path of a given point
	 */
	private ArrayList<TrajectoryNode> nodes;
	/**
	 * Number of times the point of the trajectory has not been detected.
	 * When a trajectory reaches a given number of occlusions, the trajectory 
	 * finishes.
	 */
	int occlusions;
	
	/**
	 * Dynamic filter used to predict projected values. 
	 */
	DynamicFilter filter;
	
	/**
	 * Unique ID for the trajectory
	 */
	private int ID;
	
	/**
	 * Static variable to ensure a unique ID for each trajectory. 
	 */
	private static int TrajectoryID = 0; 
	/**
	 * Constructor: Build a trajectory with the first node.
	 * @param point First position of the trajectory.
	 * @param time Current time for the first node.
	 */
	public Trajectory(final PointDetection point, final int time) {
		//Add first node of the trajectory
		TrajectoryNode node = new TrajectoryNode(point, time);
		nodes = new ArrayList<TrajectoryNode>();
		nodes.add(node);
		//Assign an unique ID for each trajectory
		this.ID = TrajectoryID;
		TrajectoryID++;
		//Create dynamic filter.
		filter = new DynamicFilter(point.position, time);
	}
	
	public void writeXML(final Document doc, final Element element) throws ParserConfigurationException{
		//Create trajectory element
		Element trajectoryElement = doc.createElement("Trajectory");
		element.appendChild(trajectoryElement);
		//Include attribute ID into trajectory element		
		Attr attrID = doc.createAttribute("ID");		
		attrID.setValue(Integer.toString(ID));
		trajectoryElement.setAttributeNode(attrID);
		//Create element TrajectoryNodes
		Element trajectoryNodeElement = doc.createElement("TrajectoryNodes");
		trajectoryElement.appendChild(trajectoryNodeElement);
		
		//Write all the nodes of the trajectory
		for (TrajectoryNode node : nodes) {
			//Create Node element and call next function
			node.writeXML(doc, trajectoryNodeElement);			
		}
	}

	public static Trajectory ReadXMLTrajectory(final Node trayectory){
		
			if (trayectory.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) trayectory;
				System.out.println("Trajectory ID : " + eElement.getAttribute("ID"));
				
				//Get all TrajectoryNodes
				NodeList listTrajectoryNodes = eElement.getElementsByTagName("TrajectoryNodes");
				System.out.println("=======");
				//Inside each TrayectoryNode
					//Getting the "j" TrajectoryNode of the 'i' Trajectory
					Node trajectoryNode = listTrajectoryNodes.item(0);
					System.out.println("");
	
					if (trajectoryNode.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement2 = (Element) trajectoryNode;
						Trajectory trj = null;
						NodeList listNodes = eElement2.getElementsByTagName("Node");
						System.out.println("===");
						//Inside each Node
						for (int k = 0; k < listNodes.getLength(); k++){
							//Getting the 'k' node
							Node node = listNodes.item(k);
							System.out.println("");
							//Create a new trajectory with the first point
							TrajectoryNode aux = new TrajectoryNode(TrajectoryNode.ReadXMLTrajectoryNode(node));
							if (trj == null){
								trj = new Trajectory (aux.point, aux.time);
								trj.ID = Integer.parseInt(eElement.getAttribute("ID"));
							}
							else
								trj.update(aux.point, aux.time);							
						}
						return trj;
					}		
				}
			return null;
			}


	/**
	 * Update trajectory with a new node. The function checks if the node is static.
	 * In this case, the last node is replaced with the new one, so that, the
	 * object memory does not increase.
	 * @param point New position of the trajectory.
	 * @param time Current time for the new node.
	 */
	public boolean update(final PointDetection point, final int time) {
		if (!this.CheckGeometricConstrains(point.position, time))
			return false;
		
		TrajectoryNode node = new TrajectoryNode(point, time);
		TrajectoryNode end = nodes.get(nodes.size()-1);
		
		filter.update(point.position, time);
		//If the trajectory only has one node, always add int.
		if (nodes.size()==1) {
			nodes.add(node);
			return true;
		}

		//Check if the point is static. In this case, do not increment the size of 
		//  the list of nodes
		//Get last node of the trajectory.
		//TODO: Pedro: Check if it is enough changing end values (if end is a reference to the element)
		if (end.point.PointDistance2(point)<2)
			//In this case, replace new node for the last one.
			nodes.set(nodes.size()-1, node);
		else
			//Otherwise, add the new node to the trajectory.
			nodes.add(node);
		
		return true;
	}
	
	/**
	 * Generates a point correspondence, where start is the last known
	 * position of the trajectory, and end is the prediction for the trajectory
	 * position at the time given.
	 * @param time Prediction is computed for the given time.
	 * @return Point correspondence.
	 */
	public PointCorrespondence predict(final int time) {
		//Get last node of the trajectory.
		TrajectoryNode start = nodes.get(nodes.size()-1);
		//Predict new position for the trajectory.
		PointDetection point = new PointDetection(filter.predict(time), start.point.octave); 
		TrajectoryNode end = new TrajectoryNode(point, time);
		PointCorrespondence prediction = new PointCorrespondence(start, end, this);
		return prediction;
	}
	

	/**
	 * Starting time for the trajectory.
	 * @return staring time
	 */
	public int start() {
		TrajectoryNode start = nodes.get(0);
		return start.time;
	}
	
	/**
	 * Ending time for the trajectory.
	 * @return ending time
	 */
	public int end() {
		TrajectoryNode end = nodes.get(nodes.size()-1);
		return end.time;
	}

	public String toString () {
		String value = "R:"; 

		for (TrajectoryNode node : nodes) {
			value += node.toString();
		}
		return value;
	}
	
	/**
	 * Check whether a new node with the given point coordinates and time will
	 * violates the geometric restrictions of a trajectory to be valid.
	 * @param point Point coordinates for the new node.
	 * @param time Time for the new node.
	 * @return true if the new node does not violate geometric restrictions.
	 */
	private boolean CheckGeometricConstrains(final Point point, final int time) {
		//TODO: Revisar esto con el proyecto en C
		return true;
	}
	
	public boolean trajectoryExists(final int time) {
		return (nodes.get(0).time>=time && nodes.get(nodes.size()-1).time<=time);
	}
	public void drawPath(Mat image, Scalar color) {
		Iterator<TrajectoryNode> it = nodes.iterator();
		TrajectoryNode node1 = it.next();
		TrajectoryNode node2;
		while (it.hasNext()) {
			node2 = it.next();
			node1.DrawPath(image, node2, color);
		}
	}
	
	public void drawNode(final int time) {
		//TODO:
	}
}


///////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
//////////END OF FILE//////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////
/**
	public static void main(String args[]) throws ParserConfigurationException, SAXException, IOException, TransformerException{
				ArrayList<Trajectory> trajectories = new ArrayList<Trajectory>();
		//Create a DocumentBuilder
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				//Create a document from a file
				Document document = builder.parse(new File( "/home/alberto/Descargas/video.xml" ));
				//Important
				document.getDocumentElement().normalize();
				//Extract the root element
				Element root = document.getDocumentElement();
				System.out.println(root.getNodeName());
				
				//Get all Trajectories<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
				NodeList ListTrajectory = document.getElementsByTagName("Trajectory");
				System.out.println("====================");
				
				//Inside each trajectory
				for (int i = 0; i < ListTrajectory.getLength(); i++) {
					//Getting the "i" Trajectory
					Node node = ListTrajectory.item(i);
					System.out.println(""); 
					//I suppose trajectories is an ArrayList of trajectory in the upper class.
					trajectories.add(Trajectory.ReadXMLTrajectory(node));
				}
				//Check!!
				System.out.println(trajectories);	
	}



public void WriteXMLTrajectories(Trajectories trajectories, String filePath){				
	
	
//Create the document XML
DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
Document doc= docBuilder.newDocument();

//Example trajectory
Point p1 = new Point(10, 20);
PointDetection d1 = new PointDetection(p1, 1);
Trajectory t = new Trajectory(d1, 1);
System.out.println(t);
PointCorrespondence c2 = t.predict(2);
System.out.println("Prediction: "+c2.end);

Point p2 = new Point(15, 20);
PointDetection d2 = new PointDetection(p2, 1);
t.update(d2, 2);
System.out.println(t);
PointCorrespondence c3 = t.predict(3);
System.out.println("Prediction: "+c3.end);

Point p3 = new Point(20, 20);
PointDetection d3 = new PointDetection(p3, 1);
t.update(d3, 3);
System.out.println(t);
PointCorrespondence c4 = t.predict(4);
System.out.println("Prediction: "+c4.end);

Point p4 = new Point(30, 20);
PointDetection d4 = new PointDetection(p4, 1);
t.update(d4, 5);
System.out.println(t);
PointCorrespondence c5 = t.predict(6);
System.out.println("Prediction: "+c5.end);			
//End of example trajectory


//Create upperElement VideoAnnotation and set date and time of video.
Element upperElement = doc.createElement("VideoAnnotation");
doc.appendChild(upperElement);

Attr attrDate = doc.createAttribute("date");		
Date date = new Date();
attrDate.setValue( date.toString());
//Introduce attribute date into Videoannotation element		
upperElement.setAttributeNode(attrDate);

//MODIFICAR ESTO EN LA CLASE SUPERIOR.
int size = trajectories.trajectoryArrayList.size();
//For each Node
for (int i = 0; i < size; i++){
	//Create Node element and call next function
	Element trajectoryElement = doc.createElement("Trajectory");
	upperElement.appendChild(trajectoryElement);
	TrajectoryNode.WriteXMLTrajectoryNode(trajectories.trajectoryArrayList.get(i), doc, trajectoryElement);			
}

//Call next function
Trajectory.WriteXMLTrajectory(t, doc, upperElement);

//Create file
TransformerFactory transformerFactory = TransformerFactory.newInstance();
Transformer transformer = transformerFactory.newTransformer();
DOMSource source = new DOMSource(doc);
StreamResult result = new StreamResult(new File(filePath));
transformer.transform(source, result);

}

*/
/*
public static Trajectories ReadXMLTrajectories(String filePath, Node trajectories){
	//Open the document XML
	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();
	Document doc= dbBuilder.parse(filePath);
	
	Element docElement = (Element) doc;
	NodeList listVideoAnnotations = docElement.getElementsByTagName("VideoAnnotation");	
	Node videoAnnotation = listVideoAnnotations.item(0);
	
	if (videoAnnotation.getNodeType() == Node.ELEMENT_NODE) {
		Element videoAnnotationElement  = (Element) videoAnnotation;
		System.out.println("VideoAnnotation : " + videoAnnotationElement.getAttribute("date"));
		
		//Get all Trajectories
		NodeList listTrajectories = videoAnnotationElement.getElementsByTagName("Trajectory");
		System.out.println("=======");
		//Inside each Trayectory
		//Getting the "j" Trajectory of the 'i' Trajectory
		System.out.println("");
				for (int k = 0; k < listTrajectories.getLength(); k++){
					//Getting the 'k' trajectory
					Node node = listTrajectories.item(k);
					System.out.println("");
					//Add the trajectory to the ArrayList
					//POR CORREGIR
					trajectories.trajectoryArrayList.add(Trajectory.ReadXMLTrajectory(node));
					//POR CORREGIR
					
				}
				return algo;
			}	
	return null;
	}

*/
