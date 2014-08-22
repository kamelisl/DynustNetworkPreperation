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
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;


/**
 *
 * @author Islam
 */
public class Network {
    /**
     Constructor.
     @param aPathName Network's path
    */
    public Network (String aPathName) {
        PathName = Paths.get(aPathName);
        Path file_log = Paths.get(PathName.toString(), "PreparingDynusTNetwork.log");
        Charset charset = Charset.forName("US-ASCII");
        try (BufferedWriter writer = Files.newBufferedWriter(file_log, charset)) {
            String s = "=======" +
                    new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss").format(new Date())
                    + "=======";
            writer.write(s, 0, s.length());
            writer.newLine();
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
        
        File file_network = new File(aPathName + "\\network.dat");
        try (Scanner scanner =  new Scanner(file_network)){
            if (scanner.hasNextInt())
                noOfZones = scanner.nextInt();
            else
                log("Error: Invalid format: " + file_network.toString());
            if (scanner.hasNextInt())
                noOfNodes = scanner.nextInt();
            else
                log("Error: Invalid format: " + file_network.toString());
            //zoneCounter = 0;
            //zones = new ArrayList<Zone>(); 
        } catch (IOException x) {
            log("IOException: " + x);
        }
    }
    /**
     Constructor.
     @param number # zones/nodes
    */
    public Network(int number, char t){
        if (t == 'z') {
            noOfZones = number;
            noOfNodes = 0;
        } else if (t == 'n') {
            noOfNodes = number;
            noOfZones = 0;
        }
        //zoneCounter = 0;
        //zones = new ArrayList<Zone>();
    }
    
    /**
     Constructor.
     @param zones # zones
     @param nodes # nodes
    */
    public Network(int nZones, int nNodes){
        noOfZones = nZones;
        noOfNodes = nNodes;
        //zones = new ArrayList<Zone>();
        //zoneCounter = 0;
    }
    
    public void log(String s){
        //java -jar DynusT.jar [args] > *.log
        Path file_log = Paths.get(PathName.toString(), "PreparingDynusTNetwork.log");
        Charset charset = Charset.forName("US-ASCII");
        try (BufferedWriter writer = Files.newBufferedWriter(file_log, charset, StandardOpenOption.APPEND)) {
            writer.write(s, 0, s.length());
            writer.newLine();
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }
    
    public void setNoOfZones (int number) {
        noOfZones = number;
    }
    
    public int getNoOfZones () {
        return noOfZones;
    }
    
    public void setNoOfNodes (int number) {
        noOfNodes = number;
    }
    
    public int getNoOfNodes () {
        return noOfNodes;
    }
    
    public void setPath (String aPathName) {
        PathName = Paths.get(aPathName);
    }
    
    public Path getPath () {
        return PathName;
    }
    
    public boolean setZones () {
        int NumberOfFeaturePoints = 0;
        File file_zone = new File(PathName + "\\zone.dat");
        try (Scanner scanner =  new Scanner(file_zone)){
            scanner.nextLine();
            scanner.nextLine();
            //Get # feature points
            NumberOfFeaturePoints = scanner.nextInt();
            //Get # zones
            setNoOfZones(scanner.nextInt());
            //Get all feature points with their coordinates
            Node[] FeaturePoints = new Node[NumberOfFeaturePoints];
            scanner.nextLine();
            for (int i=0; i<NumberOfFeaturePoints; i++) {
                scanner.nextLine();
                if (scanner.hasNextInt())
                    FeaturePoints[i] = new Node(scanner.nextInt());
                else {
                    log("Error: Invalid format: " + file_zone.toString());
                    return false;
                }
                
                if (scanner.hasNext()) {
                    String temp = scanner.next();
                    temp = temp.substring(0, temp.length()-1);
                    float[] coord = new float[2];
                    coord[0] = Float.parseFloat(temp);
                    if (scanner.hasNextFloat()) {
                        coord[1] = scanner.nextFloat();
                        FeaturePoints[i].setCoordinates(coord);
                    } else {
                        log("Error: Invalid format: " + file_zone.toString());
                        return false;
                    }
                } else {
                    log("Error: Invalid format: " + file_zone.toString());
                    return false;
                }
            }
            scanner.nextLine();
            //Get & store all zones and their feature points
            zones = new Zone[getNoOfZones()];
            for (int i=0; i<getNoOfZones(); i++) {
                scanner.nextLine();
                int id;
                if (scanner.hasNextInt())
                    id = scanner.nextInt();
                else {
                    log("Error: Invalid format: " + file_zone.toString());
                    return false;
                }
                int counter;
                if (scanner.hasNext()) {
                    String temp = scanner.next();
                    temp = temp.substring(0, temp.length()-1);
                    counter = Integer.parseInt(temp);
                } else {
                    log("Error: Invalid format: " + file_zone.toString());
                    return false;
                }
                int[] points = new int[counter];
                for (int j=0; j<counter; j++) {
                    if (scanner.hasNext()) {
                        String temp = scanner.next();
                        temp = temp.substring(0, temp.length()-1);
                        points[j] = Integer.parseInt(temp);
                    } else {
                        log("Error: Invalid format: " + file_zone.toString());
                        return false;
                    }
                }
                zones[i] = new Zone(id, counter, points);
                //System.out.println("Zone " + zones[i].getID() + " initialized...");
            }
            //----------------------------------------------------------------------
            File file_xy = new File(PathName + "\\xy.dat");
            Node node = new Node();
            try (Scanner scanner_xy =  new Scanner(file_xy)){
                do {
                    //Node's ID
                    if (scanner_xy.hasNextInt())
                        node.setID(scanner_xy.nextInt());
                    else {
                        log("Error: Invalid format: " + file_xy.toString());
                        return false;
                    }
                    //Node's coordinates
                    float[] coord = new float[2];
                    if (scanner_xy.hasNextFloat()) {
                        coord[0] = scanner_xy.nextFloat();
                        if (scanner_xy.hasNextFloat()) {
                            coord[1] = scanner_xy.nextFloat();
                            node.setCoordinates(coord);
                        }
                        else {
                            log("Error: Invalid format: " + file_xy.toString());
                            return false;
                        }
                    } else {
                        log("Error: Invalid format: " + file_xy.toString());
                        return false;
                    }

                    boolean nodeHasZone = false;
                    boolean nodeNearZone = false;

                    for (int i=0; i<getNoOfZones(); i++) {
                        float[] vertx = new float[zones[i].getNoOfFeaturePoints()];
                        float[] verty = new float[zones[i].getNoOfFeaturePoints()];
                        for (int j=0; j<zones[i].getNoOfFeaturePoints(); j++) {
                            //If feature points are not consecutive numbers starting from 1
                            //Find the feature point with id = zones[i].featurePoints[j]
//                            int k;
//                            for (k=0; k<NumberOfFeaturePoints; k++)
//                                if (zones[i].featurePoints[j] == FeaturePoints[k].getID())   break;
//                            vertx[j] = FeaturePoints[k].get_x();
//                            verty[j] = FeaturePoints[k].get_y();
                            //If feature points are consecutive numbers starting from 1
                            vertx[j] = FeaturePoints[zones[i].featurePoints[j]-1].get_x();
                            verty[j] = FeaturePoints[zones[i].featurePoints[j]-1].get_y();
                        }

                        if (!nodeHasZone && PointInZone(zones[i].getNoOfFeaturePoints(),
                                 vertx, verty, node.get_x(), node.get_y())) {
                            //If node lies within the i-th zone
                            node.setZone(zones[i].getID());
                            zones[i].addNode(node.getID());
                            nodeHasZone = true;
                        } else if (i<getNoOfZones()-1) {
                            //If node lies within a small distance near this zone
                            if (PointNearZone(zones[i].getNoOfFeaturePoints(), vertx, verty
                                , node.get_x(), node.get_y())) {
                                zones[i].addNode(node.getID());
                                nodeNearZone = true;
                            }
                        } else if ((i==getNoOfZones()-1) && !nodeHasZone && !nodeNearZone) {
                            //In case of a point near a boundary zone, try to 
                            //find the nearest feature point and 
                            //locate this node in its zone
                            double minDistance = 1000;  /*minDistance defines how
                                                        far should we consider this
                                                        node belongs to that zone*/
                            double temp;
                            int tempID = -1;
                            for (int j=0; j<NumberOfFeaturePoints; j++) {
                                temp = Math.hypot(FeaturePoints[j].get_x()-node.get_x(), 
                                        FeaturePoints[j].get_y()-node.get_y());
                                if (temp<minDistance) {
                                    minDistance = temp;
                                    tempID = j;
                                }
                            }
                            //Find zone of feature point with index tempID and id = FeaturePoints[tempID].getID()
                            for (int j=0; j<getNoOfZones(); j++) {
                                for (int k=0; k<zones[j].noOfFeaturePoints; k++) {
                                    if (zones[j].featurePoints[k] == FeaturePoints[tempID].getID()) {
                                        node.setZone(zones[j].getID());
                                        zones[j].addNode(node.getID());
                                        break;
                                    }
                                }
                            }
                            if (minDistance < 1000)
                                log("Warning: Node " + node.getID() +
                                        " was added to zone " + node.getZone() +
                                        "; however, it lies outside its boundaries by few meters");
                            else
                                log("Error: Cannot find zone of node " + node.getID() +
                                        " (nearest zone is more than " + minDistance + " ft from this node)");
                            //return false;
                        } //End of (case of a point near a boundary zone)
                    }
                    scanner_xy.nextLine();
                    //System.out.println("Node " + node.getID() + " initialized...");
                } while (scanner_xy.hasNextLine());
            } catch (IOException x) {
                log("IOException: " + x);
                return false;
            }
        } catch (IOException x) {
            log("IOException: " + x);
            return false;
        } 
        log("Done (Extracting zones and their feature points & nodes)");
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
    
//    public void addZone(Zone aZone) {
//        if (zoneCounter<noOfZones) {
//            zones.add(new Zone(aZone));
//            zoneCounter++;
//        } else
//            log("Error: Cannot add more zones");
//    }
//    
//    public Boolean getZoneByIndex(int index, Zone aZone) {
//        if (index<zoneCounter) {
//            aZone = new Zone(zones.get(index));
//            return true;
//        } else  return false;
//    }
//    
//    public Boolean getZoneByID(int id, Zone aZone) {
//        for (int i=0; i<zoneCounter; i++) {
//            if (id == zones.get(i).getID()) {
//                aZone = new Zone(zones.get(i));
//                return true;
//            }
//        }
//        return false;
//    }
    
    //Private
    private int noOfZones;
    private int noOfNodes;
    private Path PathName;
    //int zoneCounter;
    Zone[] zones;
    //ArrayList<Zone> zones; 
}

class Zone {
    /**
     Constructor.
     @param aLabel node's ID
    */
    public Zone(){
        label = 0;
        noOfFeaturePoints = 10;
        featurePoints = new int[noOfFeaturePoints];
        noOfNodes = 0;
        maxNoOfNodes = 20;
        //nodes = new int[maxNoOfNodes];
        nodes = new ArrayList<Integer>();
    }
    /**
     Constructor.
     @param aLabel node's ID
    */
    public Zone(Zone aZone){
        this.label = aZone.label;
        this.noOfFeaturePoints = aZone.noOfFeaturePoints;
        this.featurePoints = new int[this.noOfFeaturePoints];
        for (int i=0; i<this.noOfFeaturePoints; i++)
            this.featurePoints[i] = aZone.featurePoints[i];
        this.noOfNodes = aZone.noOfNodes;
        this.maxNoOfNodes = aZone.maxNoOfNodes;
        //this.nodes = new int[this.maxNoOfNodes];
        nodes = new ArrayList<Integer>();
        for (int i=0; i<this.noOfNodes; i++)
            nodes.add(aZone.nodes.get(i));
            //this.nodes[i] = aZone.nodes[i];
    }
    /**
     Constructor.
     @param aLabel node's ID
    */
    public Zone(int aLabel, int aNoOfFeaturePoints, int[] aFeaturePoints
            , int aNoOfNodes, int aMaxNoOfNodes, int[] aNodes){
        this.label = aLabel;
        this.noOfFeaturePoints = aNoOfFeaturePoints;
        this.featurePoints = new int[this.noOfFeaturePoints];
        for (int i=0; i<this.noOfFeaturePoints; i++)
            this.featurePoints[i] = aFeaturePoints[i];
        this.noOfNodes = aNoOfNodes;
        this.maxNoOfNodes = aMaxNoOfNodes;
        //this.nodes = new int[this.maxNoOfNodes];
        nodes = new ArrayList<Integer>();
        for (int i=0; i<this.noOfNodes; i++)
            nodes.add(aNodes[i]);
            //this.nodes[i] = aNodes[i];
        
        
        noOfDestinationNodes = 0;
        destinationNodes = new ArrayList<Integer>();
        
    }
    /**
     Constructor.
     @param aLabel node's ID
    */
    public Zone(int aLabel){
        label = aLabel;
        noOfFeaturePoints = 10;
        featurePoints = new int[noOfFeaturePoints];
        noOfNodes = 0;
        nodes = new ArrayList<Integer>();
        noOfDestinationNodes = 0;
        destinationNodes = new ArrayList<Integer>();
        
        maxNoOfNodes = 20;
        //nodes = new int[maxNoOfNodes];
        
    }
        /**
     Constructor.
     @param aLabel node's ID
     @param aNoOfFeaturePoints
    */
    public Zone(int aLabel, int aNoOfFeaturePoints){
        label = aLabel;
        noOfFeaturePoints = aNoOfFeaturePoints;
        featurePoints = new int[aNoOfFeaturePoints];
        noOfNodes = 0;
        nodes = new ArrayList<Integer>();
        noOfDestinationNodes = 0;
        destinationNodes = new ArrayList<Integer>();
        
        maxNoOfNodes = 20;
        //nodes = new int[maxNoOfNodes];
        
    }
    /**
     Constructor.
     @param aLabel node's ID
     @param aNoOfFeaturePoints
     @param aFeaturePoints
    */
    public Zone(int aLabel, int aNoOfFeaturePoints, int[] aFeaturePoints){
        label = aLabel;
        noOfFeaturePoints = aNoOfFeaturePoints;
        featurePoints = aFeaturePoints;
        noOfNodes = 0;
        nodes = new ArrayList<Integer>();
        noOfDestinationNodes = 0;
        destinationNodes = new ArrayList<Integer>();
        
        maxNoOfNodes = 20;
        //nodes = new int[maxNoOfNodes];
        
    }
    public void setID(int id) {
        label = id;
    }
    public int getID() {
        return label;
    }
    public void setNoOfFeaturePoints(int z) {
        noOfFeaturePoints = z;
    }
    public int getNoOfFeaturePoints() {
        return noOfFeaturePoints;
    }
    public void setFeaturePoints(int[] aFeaturePoints) {
        featurePoints = aFeaturePoints;
    }
    public int[] getFeaturePoints() {
        return featurePoints;
    }
    public void addNode(int id) {
        nodes.add(id);
        noOfNodes++;
    }
    public void addDestinationNode(int id) {
        destinationNodes.add(id);
        noOfDestinationNodes++;
    }    
    
    int label;
    int[] featurePoints;
    int noOfFeaturePoints;
    //int[] nodes;
    ArrayList<Integer> nodes;
    ArrayList<Integer> destinationNodes;
    int noOfNodes;
    int noOfDestinationNodes;
    int maxNoOfNodes;
}

class Node {
    /**
     Constructor.
    */
    public Node(){
        coordinates = new float[2];
        label = 0;
        zone = 0;
    }
    /**
     Constructor.
     @param aLabel node's ID
    */
    public Node(int aLabel){
        label = aLabel;
        coordinates = new float[2];
    }
        /**
     Constructor.
     @param aLabel node's ID
     @param aZone node's zone
    */
    public Node(int aLabel, int aZone){
        label = aLabel;
        zone = aZone;
        coordinates = new float[2];
    }
    /**
     Constructor.
     @param aLabel node's ID
     @param aCoord node's coordinates
    */
    public Node(int aLabel, float[] aCoord){
        label = aLabel;
        coordinates = new float[2];
        coordinates = aCoord;
    }
        /**
     Constructor.
     @param aLabel node's ID
     @param aZone node's zone
     @param aCoord node's coordinates
    */
    public Node(int aLabel, int aZone, float[] aCoord){
        label = aLabel;
        zone = aZone;
        coordinates = new float[2];
        coordinates = aCoord;
    }
    public void setID(int id) {
        label = id;
    }
    public int getID() {
        return label;
    }
    public void setZone(int z) {
        zone = z;
    }
    public int getZone() {
        return zone;
    }
    public void setCoordinates(float[] coord) {
        coordinates = coord;
    }
    public float[] getCoordinates() {
        return coordinates;
    }
    public float get_x() {
        return coordinates[0];
    }
    public float get_y() {
        return coordinates[1];
    }
    
    int label;
    int zone;
    float[] coordinates;
}
