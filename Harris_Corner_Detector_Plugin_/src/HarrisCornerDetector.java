import java.util.Collections;
import java.util.Vector;

import ij.plugin.filter.Convolver;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class HarrisCornerDetector {
	public static final float DEFAULT_ALPHA = 0.05f;
	public static final int DEFAULT_THRESHOLD = 20000;
	
	float alpha = DEFAULT_ALPHA;
	int threshold = DEFAULT_THRESHOLD;
	double dmin = 10;
	
	final int border = 20;
	
	float[] pfilt = {0.223755f,0.552490f,0.223755f}; //Hp
	float[] dfilt = {0.453014f,0.0f,-0.453014f}; //Hdx, Hdy
	float[] bfilt = {0.01563f,0.09375f,0.234375f,0.3125f,0.234375f,0.09375f,0.01563f}; //Hb, Gauﬂ-Filter
	
	ImageProcessor ipOrig;
	FloatProcessor A;
	FloatProcessor B;
	FloatProcessor C;
	FloatProcessor Q;
	Vector<Corner> corners;
	
	public HarrisCornerDetector(ImageProcessor ip) {
		this.ipOrig = ip;
	}
	
	public HarrisCornerDetector(ImageProcessor ip,float alpha, int threshold) {
		this.ipOrig = ip;
		this.alpha = alpha;
		this.threshold = threshold;
	}
	
	public void findCorners() {
		makeDerivatives();
		makeCrf();
		corners = collectCornersWith(border);
		corners = cleanupCornersIn(corners);
	}


	private void makeDerivatives() {
		FloatProcessor Ix = (FloatProcessor) ipOrig.convertToFloat();
		FloatProcessor Iy = (FloatProcessor) ipOrig.convertToFloat();
		
		Ix = convolve1h(convolve1h(Ix,pfilt),dfilt);
		Iy = convolve1v(convolve1v(Iy,pfilt),dfilt);
		
		//Erechne Elemente der lokalen Strukturmatrix M = ((A,C),(C,B))
		A= sqr((FloatProcessor) Ix.duplicate());
		B= sqr((FloatProcessor) Iy.duplicate());
		C= mult((FloatProcessor) Ix.duplicate(),Iy);
		
		//Gl‰ttung mit linearem Gauﬂ-Filter (Faltung)
		A = convolve2(A,bfilt);
		B = convolve2(B,bfilt);
		C = convolve2(C,bfilt);
	}
	
	private void makeCrf() {
		int w = ipOrig.getWidth();
		int h = ipOrig.getHeight();
		
		Q = new FloatProcessor(w,h);
		
		float[] Apix = (float[]) A.getPixels();
		float[] Bpix = (float[]) B.getPixels();
		float[] Cpix = (float[]) C.getPixels();
		float[] Qpix = (float[]) Q.getPixels();
		
		for (int v = 0; v < h; v++) {
			for (int u = 0; u < w; u++) {
				int i = v*w+u;
				float a = Apix[i];
				float b=Bpix[i];
				float c=Cpix[i];
				
				float det = a*b-c*c;
				float trace = a+b;
				Qpix[i] = det - alpha * (trace*trace);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private Vector<Corner> collectCornersWith(int inputBorder) {
		Vector<Corner> cornerList = new Vector<Corner>(1000);
		int w = Q.getWidth();
		int h = Q.getHeight();
		float[] Qpix = (float[]) Q.getPixels();
		
		for (int v = inputBorder; v < h-inputBorder; v++) {
			for (int u = inputBorder; u < w-inputBorder; u++) {
				float q = Qpix[v*w+u];
				if(q>threshold && isLocalMax(Q,u,v))
				{
					Corner c = new Corner(u, v, q);
					cornerList.add(c);
				}
			}
		}
		Collections.sort(cornerList);
		return cornerList;
	}
	
	private Vector<Corner> cleanupCornersIn(Vector<Corner> inputCorners) {
		double dmin2 = dmin*dmin;
		Corner[] cornerArray = new Corner[inputCorners.size()];
		cornerArray = inputCorners.toArray(cornerArray);
		Vector<Corner> goodCorners = new Vector<Corner>(inputCorners.size());
		
		for (int i = 0; i < cornerArray.length; i++) {
			if(cornerArray[i] != null)
			{
				Corner c1 = cornerArray[i];
				goodCorners.add(c1);
				
				//lˆsche alle verbleibenden Ecken in der N‰he
				for (int j = i+1; j < cornerArray.length; j++) {
					if(cornerArray[j]!=null)
					{
						Corner c2 = cornerArray[j];
						if(c1.dist2(c2)<dmin2)
						{
							cornerArray[j] = null; //lˆsche Ecke
						}
					}
					
				}
			}
		}
		return goodCorners;
	}
	
	
	public ImageProcessor printCornerPointsOn(ImageProcessor ip)
	{
		ByteProcessor ipResult = (ByteProcessor)ip.duplicate();
		
		
		//Bild heller machen und Kontrast runter drehen damit man besser die Kanten sieht
		int[] lookupTable = new int[256];
		for (int i = 0; i < lookupTable.length; i++) {
			lookupTable[i] = 128+(i/2);
		}
		ipResult.applyTable(lookupTable);
		
		//die Ecken in das Bild zeichnen
		for (Corner each : corners) {
			each.draw(ipResult);
		}
		return ipResult;
	}
		
	//Hilfsfunktionen
	
	static FloatProcessor convolve1h (FloatProcessor p, float[] h)
	{
		Convolver conv = new Convolver();
		conv.setNormalize(false);
		conv.convolve(p, h, 1, h.length);
		return p;
	}
	
	
	static FloatProcessor convolve1v (FloatProcessor p, float[] h)
	{
		Convolver conv = new Convolver();
		conv.setNormalize(false);
		conv.convolve(p, h, h.length, 1);
		return p;
	}
	
	static FloatProcessor convolve2 (FloatProcessor p, float[] h)
	{
		convolve1h(p, h);
		convolve1v(p, h);
		return p;
	}
	
	static FloatProcessor sqr(FloatProcessor fp1)
	{
		fp1.sqr();
		return fp1;
	}
	
	static FloatProcessor mult(FloatProcessor fp1,FloatProcessor fp2)
	{
		int mode = Blitter.MULTIPLY;
		fp1.copyBits(fp2, 0, 0, mode);
		return fp1;
	}
	
	static boolean isLocalMax (FloatProcessor fp, int u, int v)
	{
		
		int w = fp.getWidth();
		int h = fp.getHeight();
		
		if(u<=0 || u>=w-1 || v<=0 || v>= h-1)
		{
			return false;
		}
		else 
		{
			float[] pix = (float[]) fp.getPixels();
			int i0 = (v-1)*w+u; //Spalte oben dran
			int i1 = v*w+u; //Spalte
			int i2 = (v+1)*w+u; //Spalte unten dran
			
			float cp = pix[i1];
			return //Maske
					cp > pix[i0-1] && cp > pix[i0] && cp > pix[i0+1] &&
					cp > pix[i1-1] && 					cp > pix[i1+1] &&
					cp > pix[i2-1] && cp > pix[i2] && cp > pix[i2+1];
					
		}
	}
}