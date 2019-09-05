/**
 * @Author Elliot Lunness (915784)
 */

package ct_viewer;

import javafx.fxml.FXML;

import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;

import javafx.scene.image.ImageView;

import javafx.embed.swing.SwingFXUtils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.*;

public class Controller {
    private static final String CT_IMAGE = "CThead";

    private BufferedImage topImage;
    private BufferedImage frontImage;
    private BufferedImage sideImage;

    private short cthead[][][];
    private short min, max;

    private int[] histogram;
    private float[] mapping;

    @FXML
    private Label top_view;

    @FXML
    private Label front_view;

    @FXML
    private Label side_view;

    @FXML
    private ToggleButton histogram_toggle;

    @FXML
    private ToggleButton MIP_toggle;

    @FXML
    private Slider zslice_slider;

    @FXML
    private Slider yslice_slider;

    @FXML
    private Slider xslice_slider;


    @FXML
    void initialize() {
        try {
            setup();
        } catch (Exception e) {
            System.out.println("ERROR!\n" + e);
        }

        /**
         * Toggles
         */
        histogram_toggle.setOnAction(e -> {
            refreshAll();
        });

        MIP_toggle.setOnAction(e -> {
            if (MIP_toggle.isSelected()) {
                renderAllMIP();
            } else {
                refreshAll();
            }
        });

        /**
         *  Slider - Top Slice
         */
        zslice_slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (MIP_toggle.isSelected()) {
                MIP_toggle.setSelected(false);
                refreshAll();
            } else {
                topView(topImage, newValue.intValue());
            }
        });

        /**
         *  Slider - Front Slice
         */
        yslice_slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (MIP_toggle.isSelected()) {
                MIP_toggle.setSelected(false);
                refreshAll();
            } else {
                frontView(frontImage, newValue.intValue());
            }
        });

        /**
         *  Slider - Side Slice
         */
        xslice_slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (MIP_toggle.isSelected()) {
                MIP_toggle.setSelected(false);
                refreshAll();
            } else {
                sideView(sideImage, newValue.intValue());
            }
        });
    }

    private void setup() throws IOException {
        File file;
        DataInputStream in;

        short read; // Value being read in
        short index; // Stores current pixel data for histogram

        int b1, b2; //

        int data_size = (113*256*256); // Total number of pixels in the data set
        int levels = 3366; //
        int t_i = 0; // Cumulative distribution

        histogram = new int[levels]; // Histogram
        mapping = new float[data_size]; // Histogram mapping function


        topImage = new BufferedImage(256, 256, BufferedImage.TYPE_3BYTE_BGR);
        frontImage = new BufferedImage(256, 113, BufferedImage.TYPE_3BYTE_BGR);
        sideImage = new BufferedImage(256, 113, BufferedImage.TYPE_3BYTE_BGR);

        min = Short.MAX_VALUE;
        max = Short.MIN_VALUE;

        cthead = new short[113][256][256]; //allocate memory for storing corrected dataset.

        try {
            file = new File(CT_IMAGE);
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

            for (int k = 0; k < 113; k++) {
                for (int j = 0; j < 256; j++) {
                    for (int i = 0; i < 256; i++) {
                        b1 = ((int)in.readByte()) & 0xff;
                        b2 = ((int)in.readByte()) & 0xff;
                        read = (short)((b2<<8) | b1);
                        if(read < min) min = read;
                        if(read > max) max = read;
                        cthead[k][j][i] = read;

                        // Creating the histogram
                        index = (short)(cthead[k][j][i] - min);
                        histogram[index]++;
                    }
                }
            }

            // Creating the cumulative distribution function and creating the mapping.
            for(int i = 0; i < levels; i++) {
                t_i += histogram[i];
                mapping[i] = (255.0f*t_i) / data_size;
            }

            updateImage(top_view, topImage);
            updateImage(front_view, frontImage);
            updateImage(side_view, sideImage);

            refreshAll();
        } catch(FileNotFoundException e) {
            System.out.println("FILE " + CT_IMAGE + " NOT FOUND!");
        }
    }

    private void updateImage(Label label, BufferedImage image) {
        label.setGraphic(new ImageView(SwingFXUtils.toFXImage(image, null)));
    }

    private void refreshAll() {
        topView(topImage, (int)zslice_slider.getValue());
        frontView(frontImage, (int)yslice_slider.getValue());
        sideView(sideImage, (int)xslice_slider.getValue());
    }

    private void renderAllMIP() {
        MIPTopView(topImage);
        MIPFrontView(frontImage);
        MIPSideView(sideImage);
    }

    /*
        This function will return a pointer to an array
        of bytes which represent the image data in memory.
        Using such a pointer allows fast access to the image
        data for processing (rather than getting/setting
        individual pixels)
    */
    private static byte[] GetImageData(BufferedImage image) {
        WritableRaster WR = image.getRaster();
        DataBuffer DB = WR.getDataBuffer();
        if (DB.getDataType() != DataBuffer.TYPE_BYTE)
            throw new IllegalStateException("That's not of type byte");
        return ((DataBufferByte) DB).getData();
    }

    private int getImageDepth(int h) {
        int d = 113;
        if(h == 113) { d = 256; }
        return d;
    }

    public void MIPTopView(BufferedImage image) {
        float col, maximum;
        short datum;

        int w = image.getWidth();
        int h = image.getHeight();
        int d = getImageDepth(h);
        int j, i, k;

        byte[] data = GetImageData(image);

        for(j = 0; j < h; j++) {
            for(i = 0; i < w; i++) {
                maximum = -1117;
                for(k=0; k < d; k++){
                    datum = cthead[k][j][i];
                    if ((255.0f*((float)datum-(float)min)/((float)(max-min))) > maximum) {
                        col = (255.0f * ((float)datum - (float)min) / ((float)(max-min)));
                        maximum = col;
                        for (int c=0; c<3; c++) {
                            data[c+3*i+3*j*w]=(byte) col;
                        }
                    }
                }

            }
        }
        updateImage(top_view, image);
    }

    private void topView(BufferedImage image, int slice) {
        float col;
        short datum;

        int w = image.getWidth();
        int h = image.getHeight();
        int d = getImageDepth(h);

        byte[] data = GetImageData(image);

        for (int j = 0; j < h; j++) {
            for (int i = 0; i< w; i++) {
                if(slice < d) {
                    datum = cthead[slice][j][i];
                    if(histogram_toggle.isSelected()) {
                        col = mapping[datum - min]; // Histogram mapping
                    } else {
                        col = (255.0f * ((float)datum - (float)min) / ((float)(max - min)));
                    }
                    for (int c = 0; c < 3; c++) {
                        data[c+3*i+3*j*w]=(byte) col;
                    }
                }
            }
        }

        // Update image
        updateImage(top_view, image);
    }

    public void MIPFrontView(BufferedImage image) {
        float col, maximum;
        short datum;

        int w = image.getWidth();
        int h = image.getHeight();
        int d = getImageDepth(h);
        int j, i, k;

        byte[] data = GetImageData(image);

        for (j = 0; j < h; j++) {
            for (i = 0; i < w; i++) {
                maximum = -1117;
                for(k = 0; k < d; k++) {
                    datum=cthead[j][k][i];
                    if ((255.0f*((float)datum-(float)min)/((float)(max-min))) > maximum) {
                        col = (255.0f*((float)datum-(float)min)/((float)(max-min)));
                        maximum = col;

                        for (int c=0; c<3; c++) {
                            data[c+3*i+3*j*w]=(byte) col;
                        }
                    }
                }
            }
        }
        updateImage(front_view, image);
    }

    public void frontView(BufferedImage image, int slice) {
        float col;
        short datum;

        int w = image.getWidth();
        int h = image.getHeight();
        int d = getImageDepth(h);

        byte[] data = GetImageData(image);

        for (int j = 0; j < h; j++) {
            for (int i = 0; i< w; i++) {
                if(slice < d) {
                    datum = cthead[j][slice][i];
                    if(histogram_toggle.isSelected()) {
                        col = mapping[datum - min]; // Histogram mapping
                    } else {
                        col = (255.0f * ((float)datum - (float)min) / ((float)(max - min)));
                    }
                    for (int c = 0; c < 3; c++) {
                        data[c+3*i+3*j*w]=(byte) col;
                    }
                    for (int c = 0; c < 3; c++) {
                        data[c+3*i+3*j*w]=(byte) col;
                    }
                }
            }
        }
        // Update image
        updateImage(front_view, image);
    }

    public void MIPSideView(BufferedImage image) {
        float col, maximum;
        short datum;

        int w = image.getWidth();
        int h = image.getHeight();
        int d = getImageDepth(h);
        int j, i, k;

        byte[] data = GetImageData(image);

        for (j = 0; j < h; j++) {
            for (i = 0; i < w; i++) {
                maximum = -1117;
                for(k = 0; k < d; k++) {
                    datum=cthead[j][i][k];
                    if ((255.0f*((float)datum-(float)min)/((float)(max-min))) > maximum) {
                        col = (255.0f*((float)datum-(float)min)/((float)(max-min)));
                        maximum = col;
                        for (int c=0; c<3; c++) {
                            data[c+3*i+3*j*w]=(byte) col;
                        }
                    }
                }
            }
        }
        updateImage(side_view, image);
    }

    private void sideView(BufferedImage image, int slice) {
        float col;
        short datum;

        int w = image.getWidth();
        int h = image.getHeight();
        int d = getImageDepth(h);

        byte[] data = GetImageData(image);

        for (int j = 0; j < h; j++) {
            for (int i = 0; i< w; i++) {
                if(slice < d) {
                    datum = cthead[j][i][slice];
                    if(histogram_toggle.isSelected()) {
                        col = mapping[datum - min]; // Histogram mapping
                    } else {
                        col = (255.0f * ((float)datum - (float)min) / ((float)(max - min)));
                    }
                    for (int c = 0; c < 3; c++) {
                        data[c+3*i+3*j*w]=(byte) col;
                    }
                    for (int c = 0; c < 3; c++) {
                        //and now we are looping through the bgr components of the pixel
                        //set the colour component c of pixel (i,j)
                        data[c+3*i+3*j*w]=(byte) col;
                    }
                }
            }
        }
        // Update image
        updateImage(side_view, image);
    }

//    private BufferedImage angleView(BufferedImage image, int slice, int angle) {
//        float col;
//        short datum;
//
//        int w = image.getWidth();
//        int h = image.getHeight();
//
//        byte[] data = GetImageData(image);
//
//        int new_slice;
//
//        double new_angle = Math.toRadians(angle);
//
//        for (int j = 0; j < h; j++) {
//            for (int i = 0; i< w; i++) {
//                new_slice = (int)((i * Math.sin(new_angle)) + (slice * Math.cos(new_angle)));
//                int new_i = (int)(i * Math.cos(new_angle) - (slice *  Math.sin(new_angle)));
//                try {
//                    datum = cthead[j][new_i][new_slice];
//                    if(histogram_toggle.isSelected()) {
//                        col = mapping[datum - min]; // Histogram mapping
//                    } else {
//                        col = (255.0f * ((float)datum - (float)min) / ((float)(max - min)));
//                    }
//                }
//                catch(ArrayIndexOutOfBoundsException exception) {
//                    col = 0.0f;
//                }
//                for (int c = 0; c < 3; c++) {
//                    data[c+3*i+3*j*w]=(byte) col;
//                }
//            }
//        }
//        return image;
//    }
}
