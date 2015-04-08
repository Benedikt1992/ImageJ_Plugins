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
	
	float[] pfilt = {0.223755f,0.552490f,0.223755f}; //Hp, entspricht Vorgl‰ttung
	float[] dfilt = {0.453014f,0.0f,-0.453014f}; //Hdx, Hdy, entspricht partieller Ableitung in x bzw. y Richtung
	float[] bfilt = {0.01563f,0.09375f,0.234375f,0.3125f,0.234375f,0.09375f,0.01563f}; //Hb, entspricht Gauﬂ-Filter
	
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
		
		Ix = faltung1horizontal(faltung1horizontal(Ix,pfilt),dfilt);
		Iy = faltung1vertikal(faltung1vertikal(Iy,pfilt),dfilt);
		
		//Erechne Elemente der lokalen Strukturmatrix M = ((A,C),(C,B))
		A= sqr((FloatProcessor) Ix.duplicate());
		B= sqr((FloatProcessor) Iy.duplicate());
		C= mult((FloatProcessor) Ix.duplicate(),Iy);
		
		//Gl‰ttung mit linearem Gauﬂ-Filter (Faltung)
		A = faltung2(A,bfilt);
		B = faltung2(B,bfilt);
		C = faltung2(C,bfilt);
	}
	
	private void makeCrf() {
		int w = ipOrig.getWidth();
		int h = ipOrig.getHeight();
		
		//Corner Response Function: Q(u,v)=(A*B-C^2)-alpha*(A+B)^2 = det(M)-alpha*(trace(m))^2
		Q = new FloatProcessor(w,h);
		
		float[] Apix = (float[]) A.getPixels();
		float[] Bpix = (float[]) B.getPixels();
		float[] Cpix = (float[]) C.getPixels();
		float[] Qpix = (float[]) Q.getPixels();
		
		for (int v = 0; v < h; v++) {
			for (int u = 0; u < w; u++) {
				int i = v*w+u; // entspricht Q(u,v) bzw. M(u,v)
				float a = Apix[i]; //A(u,v)
				float b=Bpix[i]; //B(u,v)
				float c=Cpix[i]; //C(u,v)
				
				float det = a*b-c*c; //det(M(u,v)) f¸r je ein Element (<-u,v)
				float trace = a+b; //trace(M(u,v)) f¸r je ein Element (<-u,v)
				Qpix[i] = det - alpha * (trace*trace);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private Vector<Corner> collectCornersWith(int schwellwert) {
		Vector<Corner> cornerList = new Vector<Corner>(1000);
		int w = Q.getWidth();
		int h = Q.getHeight();
		float[] Qpix = (float[]) Q.getPixels();
		
		for (int v = schwellwert; v < h-schwellwert; v++) {
			for (int u = schwellwert; u < w-schwellwert; u++) {
				float q = Qpix[v*w+u];
				if(q>threshold && isLocalMax(Q,u,v))
				{
					Corner c = new Corner(u, v, q);
					cornerList.add(c);
				}
			}
		}
		
		//Ecken absteigend nach ihrer St‰rke sortieren
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
	
	static FloatProcessor faltung1horizontal (FloatProcessor p, float[] h)
	{
		Convolver conv = new Convolver();
		conv.setNormalize(false);
		conv.convolve(p, h, 1, h.length);
		return p;
	}
	
	
	static FloatProcessor faltung1vertikal (FloatProcessor p, float[] h)
	{
		Convolver conv = new Convolver();
		conv.setNormalize(false);
		conv.convolve(p, h, h.length, 1);
		return p;
	}
	
	static FloatProcessor faltung2 (FloatProcessor p, float[] h)
	{
		faltung1horizontal(p, h);
		faltung1vertikal(p, h);
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
			int i0 = (v-1)*w+u; //(u,v-1)
			int i1 = v*w+u; //(u,v)
			int i2 = (v+1)*w+u; //(u,v+1)
			
			float currentPoint = pix[i1];
			return //currentPoint > 8er-Nachbarschaft?
					currentPoint > pix[i0-1] && currentPoint > pix[i0] && currentPoint > pix[i0+1] &&
					currentPoint > pix[i1-1] && 					currentPoint > pix[i1+1] &&
					currentPoint > pix[i2-1] && currentPoint > pix[i2] && currentPoint > pix[i2+1];
					
		}
	}
}