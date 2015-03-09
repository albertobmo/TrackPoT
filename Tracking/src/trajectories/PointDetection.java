package trajectories;

import javax.xml.parsers.ParserConfigurationException;
import org.opencv.core.*;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author pedro
 *
 * Basic class for image data points. It store point coordinates (x, y) and scale (octave)  
 */
/**
 * @author pedro
 *
 */
public class PointDetection {

	/**
	 * Point coordinates (double)
	 */
	public Point position; 
	/**
	 * Scale of the point (image octave where the point was detected) 
	 */
	public int octave;
	
	/**
	 * Constructor from a Point and its corresponding scale.
	 * @param position
	 * @param octave
	 */
	public PointDetection(final Point position, final int octave){
		this.position = position.clone();
		this.octave = octave;
	}
	
	/**
	 * Constructor from a pair a numbers and its corresponding scale.
	 * @param x
	 * @param y
	 * @param octave
	 */
	public PointDetection(final double x, final double y, final int octave){
		this.position = new Point(x, y);
		this.octave = octave;
	}
	/**
	 * Copy constructor
	 * @param arg
	 */
	public PointDetection(final PointDetection arg){
		this.position = arg.position.clone();
		this.octave = arg.octave;
	}

	/**
	 * Stores point data into XML structure.
	 * @param doc XML doc structure
	 * @param element Parent xml element.
	 * @throws ParserConfigurationException
	 */
	public void writeXML(final Document doc, final Element element) throws ParserConfigurationException{
		//Create position Element
		Element posElement = doc.createElement("pos");
		//Append elements into Document
		element.appendChild(posElement);
		//Create attributes of the position element
		Attr attrX = doc.createAttribute("x");		
		attrX.setValue(Double.toString(position.x));
		Attr attrY = doc.createAttribute("y");		
		attrY.setValue(Double.toString(position.y));			
		//Introduce attributes into position element		
		posElement.setAttributeNode(attrX);
		posElement.setAttributeNode(attrY);		
		
		//Create scale Element
		Element scaleElement = doc.createElement("scale");
		//Append elements into Document
		element.appendChild(scaleElement);
		//Create attributes of the scale element
		Attr attrScale = doc.createAttribute("octave");		
		attrScale.setValue(Integer.toString(octave));
		//Introduce attributes into scale element		
		scaleElement.setAttributeNode(attrScale);	
	}
	
	
	public static PointDetection readXML(Node pointDetection, Node scale){
		if (pointDetection.getNodeType() == Node.ELEMENT_NODE) {
			Element eElement1 = (Element) pointDetection;
			Element eElement2 = (Element) scale;
			//Create a new Point2D:
			if (eElement2 != null){
				PointDetection pDet = new PointDetection (	Integer.parseInt(eElement1.getAttribute("x")),
															Integer.parseInt(eElement1.getAttribute("y")),
															Integer.parseInt(eElement2.getAttribute("octave")));
				System.out.println("POS: ");
				System.out.println("x: " + pDet.position.x);
				System.out.println("y: " + pDet.position.y);
				System.out.println("octave: " + pDet.octave);
				return pDet;
			}
			else
				System.out.println("Octave missing...");
				
	
		}
		return null;
		
				
	}
	
	/**
	 * Computes the squared euclidean distance to the given point.
	 * @param point Second point.
	 * @return Squared distance between points.
	 */
	public double PointDistance2(final PointDetection point) {
		//Compute shift between points.
		Point d = new Point(this.position.x-point.position.x, this.position.y-point.position.y);
		//Compute square euclidean distance.
		return d.x*d.x+d.y*d.y;
	}
	
	public String toString() {
		return position + " - " + octave;
	}
}
