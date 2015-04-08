import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Harris_Corner_Detector_Plugin_ implements PlugInFilter {
	ImagePlus imp;
	static float alpha = HarrisCornerDetector.DEFAULT_ALPHA;
	static int threshold = HarrisCornerDetector.DEFAULT_THRESHOLD;

	public int setup(String arg, ImagePlus imp) {
		IJ.register(Harris_Corner_Detector_Plugin_.class);
		this.imp = imp;

		return DOES_8G + NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		if (!showDialog())
			return;
		HarrisCornerDetector hcd = new HarrisCornerDetector(ip, alpha,
				threshold);
		hcd.findCorners();
		ImageProcessor result = hcd.printCornerPointsOn(ip);
		ImagePlus win = new ImagePlus("Corners from " + imp.getTitle(), result);
		win.show();
	}

	private boolean showDialog() {
		// display dialog , return false if cancelled or on error.
		GenericDialog dlg = new GenericDialog("Harris Corner Detector",
				IJ.getInstance());
		float def_alpha = HarrisCornerDetector.DEFAULT_ALPHA;
		dlg.addNumericField("Alpha (default: " + def_alpha + ")", alpha, 3);
		int def_threshold = HarrisCornerDetector.DEFAULT_THRESHOLD;
		dlg.addNumericField("Threshold (default: " + def_threshold + ")",
				threshold, 0);
		dlg.showDialog();
		if (dlg.wasCanceled())
			return false;
		if (dlg.invalidNumber()) {
			IJ.showMessage("Error", "Invalid input number");
			return false;
		}
		alpha = (float) dlg.getNextNumber();
		threshold = (int) dlg.getNextNumber();
		return true;
	}

}
