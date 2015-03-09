package trajectories;

import javax.xml.parsers.ParserConfigurationException;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TrajectoryNode {

	public PointDetection point;
	public int time;
	
	public TrajectoryNode(final PointDetection point, final int time) {
		this.point = new PointDetection(point);
		this.time = time;
	}
	
	public TrajectoryNode(final TrajectoryNode node) {
		this.point = new PointDetection(node.point);
		this.time = node.time;
	}

	public void writeXML(final Document doc, final Element element) throws ParserConfigurationException{
		
		Element nodeElement = doc.createElement("Node");
		element.appendChild(nodeElement);

		//Create and set attribute time
		Attr attrTime = doc.createAttribute("time");		
		attrTime.setValue(Integer.toString(time));
		//Introduce attribute time into Node element		
		nodeElement.setAttributeNode(attrTime);

		//Create and set attribute type
		Attr attrType = doc.createAttribute("type");		
		attrType.setValue("P");
		//Introduce attribute time into Node element		
		nodeElement.setAttributeNode(attrType);
		
		point.writeXML(doc, nodeElement);
	}
	
	
	public static TrajectoryNode ReadXMLTrajectoryNode(final Node node){		
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element eElement3 = (Element) node;
			NodeList listPointDetection = eElement3.getElementsByTagName("pos");
			NodeList listScales = eElement3.getElementsByTagName("scale");
			Node pointDetection = listPointDetection.item(0);
			Node scale = listScales.item(0);
			System.out.println("");
			TrajectoryNode tNode = new TrajectoryNode (	PointDetection.readXML(pointDetection, scale),
														Integer.parseInt(eElement3.getAttribute("time")));
			System.out.println("Node time: " + tNode.time);
			return tNode;
			
		}
		return null;					
	}	

	public String toString() {
		String value = " D: " + this.point + " T: " + this.time;
		return value;
	}
	public void DrawNode(final Mat img, final Scalar color) {
		//Draw the point in the image with radius = 1 and thickness = 1.
		Core.circle(img, point.position, 1, color, 1);
	}
	
	public void DrawPath(final Mat img, final TrajectoryNode node, final Scalar color) {
		//Draw a line joining both nodes with thickness = 1.		
		Core.line(img, node.point.position, point.position, color, 4);
	}
}
