package tests;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.opencv.core.*;
import org.opencv.highgui.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import trajectories.Trajectories;

public class TrajectoriesTest {

	public static void main(String args[]) throws ParserConfigurationException, TransformerException, SAXException, IOException{

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		VideoCapture video = new VideoCapture();
		
		//Open settings XML
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse("settings.xml");
		document.getDocumentElement().normalize();
		
		NodeList settings = document.getElementsByTagName("video");
		Element setVideo = (Element) settings.item(0);
		String path = setVideo.getAttribute("path");
		String file = setVideo.getAttribute("file");
		
		video.open(path+file);
		
		if (!video.isOpened()) {
			System.out.println("Error. Can not be open video");
			return;
		}
		Mat image = new Mat();
		video.read(image);
		Trajectories trajectories = new Trajectories(image);
		
		int i = 0;
		while (image.dims()>0) {
			trajectories.update(image);
//			System.out.println(i);
//			i++;
//			PrintWriter writer;
//			try {
//				writer = new PrintWriter("file");
//				writer.print(trajectories);
//				writer.close();
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			video.read(image);
			i++;
			if (i>=1000) break;
		}
			
		trajectories.close("video.xml");
	}
}
