package tests;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.opencv.core.*;
import org.opencv.highgui.*;

import trajectories.Trajectories;

public class TrajectoriesTest {

	public static void main(String args[]) throws ParserConfigurationException, TransformerException{

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		VideoCapture video = new VideoCapture();
		video.open("/home/pedro/Videos/GTT/gtt-v1.avi");
		
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
