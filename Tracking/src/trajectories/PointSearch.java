package trajectories;
import java.util.ArrayList;

/**
 * 
 * @author alberto
 *
 * Auxiliar class for fast sparse point search, over a matrix with the size of the image.
 * The class, for a given point, find on an initial list, the one that is closer.
 * This class prevent the calling function to look for a point on a large set of points,
 * by looking only on those points which are close enough to a given point. 
 */
public class PointSearch {

	/**
	 * 	DIST_MAX: it's a constant that stores the maximum distance allowed in pixels
	 * for a pair of points.
	 *  DIST_MAX_2: is another constant that stores the squared previous value.
	 *  step: Instance variable of the class that stores the ceil function of the 
	 * DIST_MAX variable.
	 *  rows, cols: Those variables stores the number of rows and columns of the
	 * image after the division in equal squares.   
	 *  pointsList: Is the most important variable of the class. It's a 3-dimensional
	 * array. The vector of vectors represents each square of the image. For each position
	 * the variable contains a List of points, thus we store all the points in each quadrant.
	 *  
	 */
	
	
	/**
	 * When looking for points, only points which are nearer than this distance are considered
	 * valid. If there are any point closer to this distance, the function will return null.
	 */
	private final static byte DIST_MAX = 4;
	/**
	 * Squared value of the previous constant.
	 */
	private final static byte DIST_MAX_2 = 16;
	/**
	 * Size (width and height) of each object cell.
	 */
	private int step;
	/**
	 * Number of rows cells in which the image is divided.
	 */
	private int rows;
	/**
	 * Number of columns cells in which the image is divided.
	 */
	private int cols;
	
	/**
	 * Main structure of the object. The image is divided into rows x cols cells, and each
	 * cell stores all the points which lies within its boundaries. Thus, each cell must
	 * be an array, since more than one point could lie in the cell. 
	 */
	ArrayList<ArrayList<ArrayList<PointData>>> pointsList; 
	/**
	 * Constructor. Create the cell structure to hold the points, but all the cells are uninitialiced
	 * (set to null).
	 * @param height image height in pixels
	 * @param width image width in pixels
	 * in the correct block.
	 */
	public PointSearch(final int height, final int width) {
		//The size of the cell must be fixed to the maximum distance allowed for points.
		//In case DIST_MAX is not an integer, ceil it, to prevent errors.
		step = (int) (DIST_MAX+0.9999F);
		//The number of rows of the structure equals to the height of the surface divided by
		//the width of each cell. We sum 1 to prevent the last row to disappear. The same
		//  for columns.
		//We have to reduce actual dimension by 1 so that when the image size is multiple exact of
		//the step, it doesn't add a cell that is not going to be used. For instance:
		//  step = 5.
		//	height = 21 -> rows = 1+20/5 = 5
		//  height = 22 -> rows = 1+21/5 = 5
		//  height = 25 -> rows = 1+24/5 = 5
		//  height = 26 -> rows = 1+25/5=6 (In this case there is one cell for one row, 
		//    but it is inevitable).
		rows = 1+(height-1)/step;
		cols = 1+(width-1)/step;
		//First of all we have to generate the search structure, i.e., the 3-dimensional array.
		pointsList = new ArrayList<ArrayList<ArrayList<PointData>>>(cols);
		
		//Fill all the cells the structure with null arrays. 
		for (int i=0; i<cols; i++){
			ArrayList<ArrayList<PointData>> t = new ArrayList<ArrayList<PointData>>(rows);
			pointsList.add(t);
			for(int j = 0; j < rows; j++){
				t.add(new ArrayList<PointData>());
			}
		}
	}

	/**
	 * Fill the cells of the structure with the points given.
	 *  @param points list of detected points to be allocated in the main structure
	 */
	public void Init(final ArrayList<PointData> points) {
		//Delete previous point data, if any.
		for (ArrayList<ArrayList<PointData>> tx : pointsList)
			for (ArrayList<PointData> ty : tx)
				ty.clear();
			
		int row, col;
		for (PointData p : points){
			//Compute the cell index (row and col) for the point.
			row = (int) (p.point.position.y/step);
			col = (int) (p.point.position.x/step);
			//Now we check if the row and column are out of the limits. It is possible
			//That the algorithms returns points slightly outside of the image limits.
			if (row>=rows || row<0)
				continue;
			if (col>=cols || col<0)
				continue;
			//We add the point to the List in the corresponding block
			ArrayList<ArrayList<PointData>> tx = pointsList.get(col);
			ArrayList<PointData> ty = tx.get(row); 
			ty.add(p);
		}
	}

	/**
	 * Find the closest point in the structure to the point given. Optimization
	 * is performed by searching only in the same cell than the one corresponding
	 * to the point given, plus the 8 cells surrounding that cell, since for other
	 * cells, an hypothetical point will be further than the minimum allowed.
	 * @param point coordinates of the point to search its closest in the structure.
	 * @return Reference of the closest point, or null if no point is nearer than
	 * the minimum.
	 */
	public PointData FindPoint(final PointDetection point) {
		
		//Igual que en el constructor, localizamos la celda en la que cae este punto.
		//As in the constructor, we locate the cell where the point is. 
		int row=(int)(point.position.y/step);
		int col=(int)(point.position.x/step);
		int minCol, maxCol, minRow, maxRow;
		//We adjust the cells where the point could be found. Those cells are his own 
		//and the eight adjacent ones. Any point in other different cell the distance 
		//will be greater than the maximum, so it won't be necessary to check them.
		//Also we have to check the cell it's not an end cell that makes the loop gets 
		//out of the reserved memory
		if (row<=0)
			minRow=0;
		else
			minRow=row-1;
		if (col<=0)
			minCol=0;
		else
			minCol=col-1;

		maxRow=row+2;
		if (maxRow>rows)
			maxRow=rows;
		maxCol=col+2;
		if (maxCol>cols)
			maxCol=cols;
		//Once limits are fixed, we calculate the distance to all points in the 9 cells.
		double dAux, dist;
			
		//We set distance to a sufficiently large value. It would be enough a value slightly greater
		// to the margin because if any distance is less than it, no valid points would be found.
		dist = 1e10;
		PointData selected = null;
		for (ArrayList<ArrayList<PointData>> tx : pointsList.subList(minCol, maxCol)) {
			for (ArrayList<PointData> ty : tx.subList(minRow, maxRow)) {
				try {
					for (PointData p : ty) {
						if (p.used) 
							continue;
						if (point.octave != p.point.octave) continue;
						dAux=point.PointDistance2(p.point);
						if (dAux<dist) {
							dist = dAux;
							selected = p;
						}
					}
				}
				catch (java.lang.NullPointerException e) { continue; }
			}
		}
		//Once the loop is finished we need to check if the distance is less than the indicated margin.
		if (dist>DIST_MAX_2)
			return null;
		
		return selected;
	}
	
	
	
	
	
///////////////////////////////////////////////////////////////////////////////////////////////////	
///////////////////////////////////////////////////////////////////////////////////////////////////	
//////////////END OF FILE//////////////////////////////////////////////////////////////////////////	
///////////////////////////////////////////////////////////////////////////////////////////////////	
///////////////////////////////////////////////////////////////////////////////////////////////////	
	
	/**
	 * This method simply returns a list of the unused points in the pointsList
	 * array, i.e., the unassigned points
	 * 
	 */
	/*
	public ArrayList<PointDetection> UnusedPoints() { 
		//The list to be returned. {UnusedPointList}
		ArrayList<PointDetection> unused = new ArrayList<PointDetection>();
		
		for (ArrayList<ArrayList<PointData>> tx : pointsList) {
			for (ArrayList<PointData> ty : tx) {
				try {
					for (PointData p : ty) {
						if (p.used) continue;
						unused.add(new PointDetection(p.point));
					}
				}
				catch (java.lang.NullPointerException e) { continue; }
			}
		}
		return unused;
	}
	*/

	/*
	public boolean FindPoint(PointDetection point) {
		
		//Igual que en el constructor, localizamos la celda en la que cae este punto.
		//As in the constructor, we locate the cell where the point is. 
		int row=(int)(point.position.y/step);
		int col=(int)(point.position.x/step);
		int xMin, xMax, yMin, yMax;
		//We adjust the cells where the point could be found. Those cells are his own 
		//and the eight adjacent ones. Any point in other different cell the distance 
		//will be greater than the maximum, so it won't be necessary to check them.
		//Also we have to check the cell it's not an end cell that makes the loop gets 
		//out of the reserved memory
		if (row==0)
			yMin=0;
		else
			yMin=row-1;
		if (col==0)
			xMin=0;
		else
			xMin=col-1;

		yMax=row+1;
		if (yMax>=rows)
			yMax=rows-1;
		xMax=col+1;
		if (xMax>=cols)
			xMax=cols-1;
		//Once limits are fixed, we calculate the distance to all points in the 9 cells.
		double dAux, dist;
			
		//We set dist to a sufficiently large value. It would be enough a value slightly greater
		// to the margin because if any distance is less than it, no valid points would be found.
		dist=(float) 1e10;
		int pt = 0;
		int prow = 0;
		int pcol = 0;
		for (col=xMin; col<=xMax; col++)
			for (row=yMin; row<=yMax; row++)
			{
				//We extract all the points stored in each List
				for(int i = 0; i < pointsList.elementAt(col).elementAt(row).size(); i++){
					//We cross the list
					//First we check the scales.
					if (point.octave !=pointsList.elementAt(col).elementAt(row).get(i).point.octave)
						continue;
					//Calculate the distance between the argument point with all the list points.
					dAux = point.PointDistance2(pointsList.elementAt(col).elementAt(row).get(i).point);
					if (dAux<dist)
					{
						//We refresh the minimum distance.
						dist=dAux;
						//We save the point position.
						pt=i; prow = row; pcol = col;
					}
				}
			}
		//Once the loop is finished we need to check if the distance is less than the indicated margin.
		if (dist>DIST_MAX_2)
			return false;
		//We change the input parameter "point" with the coordinates of the closest found point of the point list.
		//Actually we just clone the point.
		point.position = pointsList.elementAt(pcol).elementAt(prow).get(pt).point.position.clone();
		//And finally we mark that point as used. 
		pointsList.elementAt(pcol).elementAt(prow).get(pt).used = true;
		return true;
	}

 */

/*
	public ArrayList<PointDetection> UnusedPoints() { 
		//The list to be returned. {UnusedPointList}
		ArrayList<PointDetection> UPList = new ArrayList<PointDetection>();
		Iterator<PointData> it;
		//We cross all the cells of the searcher
		for (int col=0; col<cols; col++)
			for (int row=0; row<rows; row++)
			{
				//For each cell we extract all the stored points in its corresponding list.
				it=pointsList.elementAt(col).elementAt(row).iterator();
				PointData pAux = null; 
				while (it.hasNext()){
					pAux = it.next();
					//If the current point flag "used" is false, we add it to the new list.
					if (!pAux.used){						
						PointDetection pDetAux = new PointDetection(pAux.point.position, pAux.point.octave);
						UPList.add(pDetAux);
					}
				}
			}		
		return UPList;
	}
	

 */
}

