package com.ONEMicroscopy.gui;

/**
 * -------------------------------------------------------------------------------------------
 * This class is used to import, check and sort data.
 * This class uses Bio-Formats to read images Metadata. 
 * 
 * Bio-Formats citation DOI: 10.1083/jcb.201004104 PMID: 20513764 PMCID: PMC2878938
 * 
 * @author Abed Chouaib
 * @version 1.0.0
 * -------------------------------------------------------------------------------------------
 */
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import ij.IJ;
import ij.ImagePlus;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import ome.units.quantity.Length;
import ome.units.quantity.Time;

public class InputData {

	boolean Cancel = false;
	JFrame frame = new JFrame();
	JButton CancelButton;
	JLabel progressStatusLabel = new JLabel();
	JLabel loaderImg = new JLabel("");
	private ArrayList<String> FilePath;
	private double ResolutionX;
	private double ResolutionY;
	private double ResolutionZ;
	private int nChannels;
	private int zSlices;
	private int nFrames;
	private String Unit;
	private double time;
	private int imNum;

	public static enum ImportType {
		Vi, MVi, ImgS, ets
	}

	public InputData(int imnum) {
		this.imNum = imnum;
	}

	public InputData() {

	}

	public InputData(String path, ImportType DataType, int imnum) throws Exception {
		this.imNum = imnum;
		switch (DataType) {
		case Vi:
			FilePath = CheckVideo(path);
			break;
		case MVi:
			FilePath = CheckForVideos(path);
			break;
		default:
			break;
		}
	}

	public ArrayList<String> GetListofFiles() {
		return FilePath;
	}

	public double[] GetResolutionsXYZ() {
		double[] args = { ResolutionX, ResolutionY, ResolutionZ };
		return args;
	}

	public int GetnChannels() {
		return nChannels;
	}

	public int GetzSlices() {
		return zSlices;
	}

	public int GetnFrames() {
		return nFrames;
	}



	public double GetTime() {
		return time;
	}

	public ArrayList<String> CheckVideo(String Path) throws Exception {
		ArrayList<String> FilePath = new ArrayList<String>();
		initialiseReader IR = new initialiseReader(Path);
		ImageReader reader = IR.getReader();
		nFrames = reader.getSizeT();
		zSlices = reader.getSizeZ();
		nChannels = reader.getSizeC();
		if (nFrames == 1) // switch zSlice if it was inverted.
		{
			nFrames = zSlices;
			zSlices = 1;
		}
		if (nFrames >= imNum) {
			IMetadata omeMeta = IR.getIMetadata();
			GetImportantValues(omeMeta);
			FilePath.add(Path);
		}
		return FilePath;
	}

	public ArrayList<String> CheckForVideos(String Path) throws Exception {
		ArrayList<String> FilesPath = new ArrayList<String>();
		ArrayList<File> files = new ArrayList<File>();
		GetFiles(Path, files);
		int count = 0;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ShowProgressBarWindow();
			}
		});
		for (int i = 0; i < files.size(); i++) {
			if (Cancel) {
				FilesPath.clear();
				break;
			}
			if (files.get(i).length() > 10000) {
				initialiseReader IR = new initialiseReader(files.get(i).getPath());
				ImageReader reader = IR.getReader();
				int NumFrames = reader.getImageCount();
				int NumSeries = reader.getSeriesCount();

				if (NumFrames >= imNum || NumSeries > 1) {
					if (count == 0) {
						IMetadata omeMeta = IR.getIMetadata();
						nFrames = NumFrames;
						zSlices = reader.getSizeZ();
						nChannels = reader.getSizeC();
						if (nFrames == 1) // switch zSlice if it was inverted.
						{
							nFrames = zSlices;
							zSlices = 1;
						}
						GetImportantValues(omeMeta);
					}
					FilesPath.add(files.get(i).getPath());
					count++;
				}
			}
			if (files.size() > 1 && i == files.size() - 1) {
				progressStatusLabel.setText("Done!");
				loaderImg.setVisible(false);
				CancelButton.setText("Close");
			}
		}
		Thread.sleep(200);
		frame.dispose();
		return FilesPath;
	}

	// ======================= Get Metadata =======================
	private void GetImportantValues(IMetadata omeMeta) {
		Length dpiX = omeMeta.getPixelsPhysicalSizeX(0);
		Length dpiY = omeMeta.getPixelsPhysicalSizeY(0);
		Length dpiZ = omeMeta.getPixelsPhysicalSizeZ(0);
		Time tempx = null;
		try {
			tempx = omeMeta.getPlaneDeltaT(0, nChannels);
		} catch (Exception e) {
			System.out.println("no timestamp detected!");
		}

		ResolutionX = dpiX == null ? 1 : (double) dpiX.value();
		ResolutionY = dpiY == null ? 1 : (double) dpiY.value();
		ResolutionZ = dpiZ == null ? 1 : (double) dpiZ.value();
		time = tempx == null ? 1 : (double) tempx.value();
	}

	// ======================= end =======================

	public void GetFiles(String Path, ArrayList<File> files) {
		File directory = new File(Path);
		File[] fList = directory.listFiles();
		if (fList != null) {
			for (File file : fList) {
				if (file.isFile()) {
					files.add(file);
				} else if (file.isDirectory()) {
					GetFiles(file.getAbsolutePath(), files);
				}
			}
		}
	}



	private void ShowProgressBarWindow() {
		frame.setVisible(true);
		frame.setResizable(false);
		frame.setBounds(100, 100, 350, 100);
		frame.getContentPane().setLayout(null);
		frame.setTitle("Importing videos please wait...");


		CancelButton = new JButton("Cancel");
		CancelButton.setBounds(50, 30, 90, 25);
		frame.getContentPane().add(CancelButton);

		Icon imgIcon = new ImageIcon(this.getClass().getResource("/loading_32.gif"));
		JLabel ImGif = new JLabel(imgIcon);
		ImGif.setBounds(180, 20, 45, 45);
		frame.getContentPane().add(ImGif);

		CancelButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				Cancel = true;
				frame.dispose();
			}
		});
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				Cancel = true;
				frame.dispose();
			}
		});
	}



	// ======================= #Region Classes =======================
	final class initialiseReader {
		private final String fileName;
		private final ImageReader reader = new ImageReader();
		private IMetadata omeMeta;

		public initialiseReader(String Filename) throws FormatException, IOException {
			this.fileName = Filename;
			omeMeta = MetadataTools.createOMEXMLMetadata();
			reader.setMetadataStore(omeMeta);
			try {
				reader.setId(fileName);
				reader.setSeries(imNum);
			} catch (Exception e) {
//				IJ.log("Bio-Formats, reading parent file path error, please change file path");
			}
			reader.setSeries(0);
		}

		public ImageReader getReader() {
			return reader;
		}

		public IMetadata getIMetadata() {
			return omeMeta;
		}
	}

	public ImagePlus[] getImageBF(String Path) {
		ImagePlus[] impBF;
		try {
			initialiseReader IR = new initialiseReader(Path);
			ImageReader reader = IR.getReader();
			int NumSeries = reader.getSeriesCount();
			if (NumSeries > 1) {
				int countSeries = 0;
				for (int i = 0; i < NumSeries; i++) {
					ImporterOptions options = new ImporterOptions();
					options.setId(Path);
					options.setSeriesOn(i, true);
					impBF = BF.openImagePlus(options);
					int[] Dim = impBF[0].getDimensions();
					if (Dim[3] > 1 || Dim[4] > 1) {
						countSeries++;
					}
				}
				if (countSeries == 1) {
					return BF.openImagePlus(Path);
				} else {
					ImagePlus[] Imgx = new ImagePlus[countSeries];
					ImagePlus[] Img = new ImagePlus[1];
					String Opt = "";
					int ndx = 0;
					for (int i = 0; i < NumSeries; i++) {
						ImporterOptions options = new ImporterOptions();
						options.setId(Path);
						options.setSeriesOn(i, true);
						impBF = BF.openImagePlus(options);
						int[] Dim = impBF[0].getDimensions();
						if (Dim[3] > 1 || Dim[4] > 1) {
							Imgx[ndx] = impBF[0].duplicate();
							Imgx[ndx].show();
							Imgx[ndx].setTitle("I-" + ndx);
							Opt = Opt + "c" + (ndx + 1) + "=[" + "I-" + ndx + "] ";
							ndx++;
						}
					}
					IJ.run(Img[0], "Merge Channels...", Opt + "create");
					return Img;
				}
			} else {
				return BF.openImagePlus(Path);
			}
		} catch (FormatException | IOException e) {
			return null;
		}
	}
}