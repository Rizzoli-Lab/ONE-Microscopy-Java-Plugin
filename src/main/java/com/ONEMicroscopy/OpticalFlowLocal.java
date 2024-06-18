package com.ONEMicroscopy;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class OpticalFlowLocal {
	public static void calculateCoefficients1(ImageProcessor ip1, ImageProcessor ip2) {
		// Convert images to float
		ip1 = ip1.convertToFloat();
		ip2 = ip2.convertToFloat();

		// Calculate optical flow using the Lucas-Kanade method
		float[] u = new float[ip1.getWidth() * ip1.getHeight()];
		float[] v = new float[ip1.getWidth() * ip1.getHeight()];

		// Calculate the mean of the optical flow vectors
		double sumU = 0;
		double sumV = 0;
		for (int i = 0; i < u.length; i++) {
			sumU += u[i];
			sumV += v[i];
		}
		double meanU = sumU / u.length;
		double meanV = sumV / v.length;

		// Calculate the standard deviation of the optical flow vectors
		double varU = 0;
		double varV = 0;
		for (int i = 0; i < u.length; i++) {
			varU += Math.pow(u[i] - meanU, 2);
			varV += Math.pow(v[i] - meanV, 2);
		}
		double stdU = Math.sqrt(varU / u.length);
		double stdV = Math.sqrt(varV / v.length);
		// Print the results
		System.out.println("Optical flow coefficients:");
		System.out.println("Mean U: " + meanU);
		System.out.println("Mean V: " + meanV);
		System.out.println("Std U: " + stdU);
		System.out.println("Std V: " + stdV);
	}

	public static void main(String[] args) {
		ImagePlus imp1 = IJ.openImage("path/to/image1");
		ImageProcessor ip1 = imp1.getProcessor();
		ImagePlus imp2 = IJ.openImage("path/to/image2");
		ImageProcessor ip2 = imp2.getProcessor();
		// Then, you can call the calculate Coefficients method and pass in the two
		// ImageProcessor objects:

		OpticalFlowLocal.calculateCoefficients1(ip1, ip2);
		// print the optical flow coefficients to the console. You can modify the
		// parameters of the OpticalFlow.run method to adjust the sensitivity and
		// accuracy of the optical flow calculation.
	}
}
