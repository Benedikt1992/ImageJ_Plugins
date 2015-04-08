import ij.process.ByteProcessor;

public class Corner implements Comparable {
	int u;
	int v;
	float q;
	
	public Corner(int u, int v, float q) 
	{
		this.u = u;
		this.v = v;
		this.q = q;
	}
	
	public int compareTo(Object obj) 
	{
		Corner c2 = (Corner) obj;
		if (this.q >c2.q) return -1;
		if (this.q <c2.q) return 1;
		else return 0;
	}
	
	public double dist2(Corner c2) {
		int dx = this.u -c2.u;
		int dy = this.v -c2.v;
		return (dx*dx)+(dy*dy);
	}
	
	public void draw(ByteProcessor ip) {
		//zeichne Ecke als schwarzes Kreuz
		
		int paintValue = 0;
		int size = 2;
		ip.setValue(paintValue);
		ip.drawLine(this.u-size,this.v,this.u+size,v);
		ip.drawLine(this.u,this.v-size,this.u,v+size);
	}
}

