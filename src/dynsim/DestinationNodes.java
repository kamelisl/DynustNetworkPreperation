/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dynsim;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Scanner;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;


/**
 *
 * @author Islam
 */
public class DestinationNodes {
    public boolean findDestinationNodes(Network network) throws IOException {
        Path fFilePath = network.getPath();
        /*----------------------------------------------------------------*/
        //Writing destination nodes in destination.dat
        //File file_destination = new File(fFilePath + "\\destination.dat");
        //changeNoOfZones(network.getNoOfZones(), network);
        Path file_destination = Paths.get(fFilePath.toString(), "destination.dat");
        String s;
        Charset charset = Charset.forName("US-ASCII");  //"UTF-8"
        try (BufferedWriter writer = Files.newBufferedWriter(file_destination, charset)) {
            for (int i=0; i<network.getNoOfZones(); i++) {
                for (int j=0; j<network.zones[i].noOfNodes; j++) {
                    if (isDestinationNode(network.zones[i].nodes.get(j), network)) {
                        network.zones[i].addDestinationNode(network.zones[i].nodes.get(j));
                    }
                }
                
                if (network.zones[i].noOfDestinationNodes==0)
                    network.log("Warning: Zone " + network.zones[i].getID() + " does not have destination nodes.");
                
                s = "\t" +  network.zones[i].getID() + "\t"
                        + network.zones[i].noOfDestinationNodes;
                for (int j=0; j<network.zones[i].noOfDestinationNodes; j++) {
                    //s = s + "\t" + zones[i].nodes[j];
                    s = s + "\t" + network.zones[i].destinationNodes.get(j);
                    //changeZone(zones[i].nodes.get(j), zones[i].getID(), network);
                }
                writer.write(s, 0, s.length());
                writer.newLine();
            }
        } catch (IOException x) {
            network.log("IOException: " + x);
            return false;
        }

        network.log("Done (Adding Destination Nodes)");
        return true;
    }
    //http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html#The C Code
    private boolean PointInZone(int nvert, float[] vertx, float[] verty, float testx, float testy)
    {
        boolean c = false;
        int i, j= 0;
        for (i = 0, j = nvert-1; i < nvert; j = i++) {
            if ( ((verty[i]>testy) != (verty[j]>testy)) && 
                    (testx < (vertx[j]-vertx[i]) * (testy-verty[i]) / (verty[j]-verty[i]) + vertx[i]) )
                c = !c;
        }
        return c;
    }
    
    private boolean PointNearZone(int nvert, float[] vertx, float[] verty, float testx, float testy)
    {
        float maxDistance = 20;
        if (distToSegment(testx, testy, vertx[0], verty[0], vertx[nvert-1], verty[nvert-1])<maxDistance)
            return true;
        for (int i=0; i<nvert-1; i++) {
            if (distToSegment(testx, testy, vertx[i], verty[i], vertx[i+1], verty[i+1])<maxDistance)
                return true;
        }
        return false;
        
//        float xDiff = vertx[nvert-1]-vertx[0];
//        float yDiff = verty[nvert-1]-verty[0];
//        double a, b, c;
//        a = Math.hypot(xDiff, yDiff);
//        b = Math.hypot(testx-vertx[0], testy-verty[0]);
//        c = Math.hypot(testx-vertx[nvert-1], testy-verty[nvert-1]);
//        double distance = Math.abs(xDiff*(testy-verty[0])-yDiff*(testx-vertx[0]))/a;
//        if (distance==0) {
//            if (a == (b+c))
//                return true;
//        } else if (distance<maxDistance)
//            if (Math.acos((a*a+b*b-c*c)/(2*a*b)) <= Math.PI/2)
//                if (Math.acos((a*a+c*c-b*b)/(2*a*c)) <= Math.PI/2)
//                    return true;
//        
//        for (int i=0; i<nvert-1; i++) {
//            xDiff = vertx[i+1]-vertx[i];
//            yDiff = verty[i+1]-verty[i];
//            a = Math.hypot(xDiff, yDiff);
//            b = Math.hypot(testx-vertx[i], testy-verty[i]);
//            c = Math.hypot(testx-vertx[i+1], testy-verty[i+1]);
//            distance = Math.abs(xDiff*(testy-verty[i])-yDiff*(testx-vertx[i]))/a;
//            if (distance==0) {
//                if (a == (b+c))
//                    return true;
//            } else if (distance<maxDistance) {
//                if (Math.acos((a*a+b*b-c*c)/(2*a*b)) > Math.PI/2)   continue;
//                if (Math.acos((a*a+c*c-b*b)/(2*a*c)) > Math.PI/2)   continue;
//                return true;
//            }
//        }
//        
//        return false;
    }
    private double dist(double vx, double vy, double wx, double wy) {
        /*Distance between two points*/
        return Math.hypot(vx-wx, vy-wy);
    }
    private double distToSegment(float px, float py, float vx, float vy,  float wx, float wy) {
        /*Distance between point and segment*/
        double l = dist(vx,vy,wx,wy);
        if (l == 0) return dist(vx,vy,px,py);   /*v & w are coincident points*/
        double t = ((px-vx)*(wx-vx)+(py-vy)*(wy-vy))/(l*l);  /*Projection of pv on 
                                                        vw normalized wrt vw length*/
        if (t < 0) return dist(px,py,vx,vy);    /*Shortest distance is measured to v*/
        if (t > 1) return dist(px,py,wx,wy);    /*Shortest distance is measured to w*/
        return dist(px, py, (vx+t*(wx-vx)), (vy+t*(wy-vy)));    /*The normal distance is the shortest one*/
    }


    private boolean changeZone(int node, int zone, Network network)
    {
        int noOfNodesInNetwork = network.getNoOfNodes();
        Path fFilePath = network.getPath();
        File file_network = new File(fFilePath + "\\network.dat");
        ArrayList<String> text = new ArrayList<String>();
        String s;
        try (Scanner scanner =  new Scanner(file_network)){
            text.add(scanner.nextLine());
            while (scanner.hasNextLine()) {
                s = scanner.nextLine();
                if (text.size() <= noOfNodesInNetwork+1) {
                    try (Scanner line =  new Scanner(s)){
                        if (line.hasNextInt()) {
                            if (line.nextInt()==node) {
                                s = s.substring(0, s.length()-2);
                                s = s + String.valueOf(zone);
                                //s = s.replaceAll(s.substring(s.length()-1), String.valueOf(zone));
                            }
                        }
                    }
                }
                text.add(s);
            }
        } catch (IOException x) {
            network.log("IOException: " + x);
            return false;
        }
        Path file_net = Paths.get(fFilePath.toString(), "network.dat");
        Charset charset = Charset.forName("US-ASCII");  //"UTF-8"
        try (BufferedWriter writer = Files.newBufferedWriter(file_net, charset)) {
            for (int i=0; i<text.size(); i++) {
                writer.write(text.get(i), 0, text.get(i).length());
                writer.newLine();
            }
        } catch (IOException x) {
            network.log("IOException: " + x);
            return false;
        }
        return true;
    }
    
    private boolean changeNoOfZones(int noOfZonesInNetwork, Network network)
    {
        Path fFilePath = network.getPath();
        File file_network = new File(fFilePath + "\\network.dat");
        ArrayList<String> text = new ArrayList<String>();
        String s;
        try (Scanner scanner =  new Scanner(file_network)){
            s = scanner.nextLine();
            text.add(s.replaceFirst("0",String.valueOf(noOfZonesInNetwork)));
            while (scanner.hasNextLine()) {
                text.add(scanner.nextLine());
            }
        } catch (IOException x) {
            //network.log("IOException: " + x);
            return false;
        }
        Path file_net = Paths.get(fFilePath.toString(), "network.dat");
        Charset charset = Charset.forName("US-ASCII");  //"UTF-8"
        try (BufferedWriter writer = Files.newBufferedWriter(file_net, charset)) {
            for (int i=0; i<text.size(); i++) {
                writer.write(text.get(i), 0, text.get(i).length());
                writer.newLine();
            }
        } catch (IOException x) {
            //network.log("IOException: " + x);
            return false;
        }
        return true;
    }
    
    private boolean isDestinationNode(int node, Network network)
    {
        boolean returned_value = false;
        Path fFilePath = network.getPath();
        //Search for arterial links coming to this node
        File file_network = new File(fFilePath + "\\network.dat");
        try (Scanner scanner_net =  new Scanner(file_network)){
            scanner_net.nextLine();
            for (int k=0; k<network.getNoOfNodes(); k++)
                scanner_net.nextLine();
            do {
                if (node == scanner_net.nextInt()) {
                    //Check if it is "start" of a link
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    int linkType = scanner_net.nextInt();
                    if ((linkType != 5) && (linkType!=7)) {
                        /*It is not a destination node if it is connected to 
                        other types of links rather than arterial link (5)*/
                        return false;
                    }
                } else if (node == scanner_net.nextInt()) {
                    //Check if it is "end" of a link
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    scanner_net.nextInt();
                    int linkType = scanner_net.nextInt();
                    if ((linkType != 5) && (linkType!=7)) {
                        /*It is not a destination node if it is connected to 
                        other types of links rather than arterial link (5)*/
                        return false;
                    } else
                        returned_value = true;
                }
                scanner_net.nextLine();
            } while (scanner_net.hasNextLine());
        } catch (IOException x) {
            network.log("IOException: " + x);
            return false;
        }
        return returned_value;
    }
}
