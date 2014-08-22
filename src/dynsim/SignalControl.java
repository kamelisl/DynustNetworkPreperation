/**
 *
 * @author Islam
 */

package dynsim;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * java -jar DynusT.jar "DynusT project path" "full address of nodes_to_be_controlled.txt"
 * should write "=======Two Way Stop Signs/Yield Signs Below =======" before 2way stop and yield signs
 * @author Islam
 */
/** Assumes UTF-8 encoding. JDK 7+. */
public class SignalControl {

    /**
     Constructor.
     @param aPathName full name of the DynusT project directory.
     @param aMaxConnectedNodes maximum number of connected nodes to an individual node in the network
    */
    public SignalControl(Network aNetwork){
        network = aNetwork;
        maxConnectedNodes = 4;
        fFilePath = Paths.get(aNetwork.getPath().toString());
        //fFilePath = Paths.get(aPathName);
        cycleLength = 120;
        amber = 6;
        green2 = 54;
        green3 = 34;
        green4 = 24;
        maxGreen2 = 60;
        minGreen2 = 50;
        maxGreen3 = 40;
        minGreen3 = 30;
        maxGreen4 = 30;
        minGreen4 = 20;
        offset = 0;
    }


    public int[] findConnectedNodes(int Node, boolean flag) throws IOException {
        File file_network = new File(fFilePath + "\\network.dat");
        int length = 0;
        int connectedNodes[] = new int[maxConnectedNodes+1];
        try (Scanner scanner =  new Scanner(file_network)){
            scanner.nextInt();
            int NumberOfNodes = scanner.nextInt();
            //skip the node-zone part
            for (int i=0; i<NumberOfNodes; i++) {scanner.nextLine();}
            
            while (scanner.hasNextLine()){
                scanner.nextLine();
                if (!scanner.hasNextInt()) break;
                int firstIntegerInLine = scanner.nextInt();
                if (!scanner.hasNextInt()) break;
                if (flag) {
                    //links to mainNode
                    if (scanner.nextInt() == Node) {
                        if (length==4) {
                            network.log("Warning: Node " + Node + " is connected to more than 4 nodes");
                            break;
                        }
                        connectedNodes[++length] = firstIntegerInLine;
                        scanner.nextInt();
                        scanner.nextInt();
                        scanner.nextInt();
                        scanner.nextInt();
                        scanner.nextInt();
                        scanner.nextInt();
                        scanner.nextInt();
                        scanner.nextInt();
                        scanner.nextInt();
                        if (scanner.nextInt() == 1) {
//                            network.log("Warning: A sign/signal is specified on node "
//                                    + Node + " that is connected to a freeway link");
                            connectedNodes[0] = -1;
                            return connectedNodes;
                        }
                    }
                } else {
                    //links from mainNode
                    if (firstIntegerInLine == Node) {
                        if (length==4) {
                            network.log("Warning: Node " + Node + " is connected to more than 4 nodes");
                            break;
                        }
                        connectedNodes[++length] = scanner.nextInt();
                        scanner.nextInt();
                        scanner.nextInt();
                        scanner.nextInt();
                        scanner.nextInt();
                        scanner.nextInt();
                        scanner.nextInt();
                        scanner.nextInt();
                        scanner.nextInt();
                        scanner.nextInt();
                        if (scanner.nextInt() == 1) {
//                            network.log("Warning: A sign/signal is specified on node "
//                                    + Node + " that is connected to a freeway link");
                            connectedNodes[0] = -1;
                            return connectedNodes;
                        }
                    }
                }
            }
            if (length == 0) network.log("Error: Cannot find node " + Node + " in network.dat");
            connectedNodes[0] = length;
            return connectedNodes;
        }
    }

    public float[] findNodeCoordinates(int Node) throws IOException {
        File file_xy = new File(fFilePath + "\\xy.dat");
        float[] coordinates = new float[2];
        try (Scanner scanner =  new Scanner(file_xy)){
            do {
                if (scanner.nextInt() == Node) {
                    coordinates[0] = scanner.nextFloat();
                    coordinates[1] = scanner.nextFloat();
                    break;
                }
                else if (scanner.hasNextLine())
                    scanner.nextLine();
                else {
                    network.log("Error: Cannot find " + Node + "'s coordinates in xy.dat");
                    break;
                }
            } while (true);
        }
        return coordinates;
    }

    public int findNodeZone(int Node) throws IOException {
        File file_network = new File(fFilePath + "\\network.dat");
        int zone = 0;
        try (Scanner scanner =  new Scanner(file_network)){
            scanner.nextInt();
            int NumberOfNodes = scanner.nextInt();
            //skip the node-zone part
            for (int i=0; i<NumberOfNodes; i++) {
                scanner.nextLine();
                int firstIntegerInLine = scanner.nextInt();
                if (firstIntegerInLine < Node)
                    continue;
                else if (firstIntegerInLine == Node) {
                    zone = scanner.nextInt();
                    break;
                }
                else {
                    //System.err.println("Cannot find node " + Node + " in network.dat");
                    network.log("Error: Cannot find node " + Node + " in network.dat");
                    break;
                }
            }
            if (zone == 0) {
                //System.err.println("Cannot find node " + Node + " in network.dat");
                network.log("Warning: Cannot find zone of node " + Node + "(zone = 0)");
            }
            return zone;
        }
    }
    
    public void arrangeConnectedNodes(MainNode mainNode, boolean  flag) throws IOException {
        /*----------------------------------------------------------------------
        Arrange the connected nodes to be:
                   (North)
                      3
                  2       0
                      1
        If connected nodes=3: find the two nodes that are most likely to
        be aligned (regardless of their orientation)
        ----------------------------------------------------------------------*/
        //if (mainNode.numberOfConnectedNodes<3)  return;
        double D = 1000000;
        double Dij, Dim, Dmj, x, y;
        int temp1=0, temp2=1;
        Node[] connectedNodes = flag ? mainNode.to : mainNode.from;
        int numberOfConnectedNodes = flag ? mainNode.numberOfConnectedNodes_to :
                mainNode.numberOfConnectedNodes_from;
        
        if (numberOfConnectedNodes == 0)    return;
        if (numberOfConnectedNodes <3) {
            double x_diff, y_diff, angle1, angle2;
            int quarter1, quarter2;
            x_diff = connectedNodes[0].get_x() - mainNode.mainNode.get_x();
            y_diff = connectedNodes[0].get_y() - mainNode.mainNode.get_y();
            angle1 = Math.acos(x_diff/Math.hypot(x_diff, y_diff));
            if (y_diff>0)   angle1 = 2*Math.PI-angle1;
            angle1 = (angle1+Math.PI/4)%(2*Math.PI);
            if (angle1<0.5*Math.PI) {
                quarter1 = 0;
            } else if (angle1<Math.PI) {
                quarter1 = 1;
            } else if (angle1<1.5*Math.PI) {
                quarter1 = 2;
            } else {
                quarter1 =3;
            }
            
            if (numberOfConnectedNodes == 2) {
                x_diff = connectedNodes[1].get_x() - mainNode.mainNode.get_x();
                y_diff = connectedNodes[1].get_y() - mainNode.mainNode.get_y();
                angle2 = Math.acos(x_diff/Math.hypot(x_diff, y_diff));
                if (y_diff>0)   angle2 = 2*Math.PI-angle2;
                angle2 = (angle2+Math.PI/4)%(2*Math.PI);
                if (angle2<0.5*Math.PI) {
                    quarter2 = 0;
                } else if (angle2<Math.PI) {
                    quarter2 = 1;
                } else if (angle2<1.5*Math.PI) {
                    quarter2 = 2;
                } else {
                    quarter2 =3;
                }
            
                if (mainNode.mainNode.getID() == 26830) {
                    network.log("2 nodes: " + connectedNodes[1].getID() + "," + angle2);
                    network.log("2 nodes: " + quarter2);
                }
                
                if (quarter1==quarter2) {
                    //if the 2 nodes lie in the same quarter, move one of them to another quarter
                    if (Math.abs(angle1-(quarter1*Math.PI/2+Math.PI/4)) < 
                            Math.abs(angle2-(quarter1*Math.PI/2+Math.PI/4))) {
                        quarter2 += Math.signum(angle2-(quarter1*Math.PI/2+Math.PI/4));
                        quarter2 = (quarter2+4)%4;
                    } else {
                        quarter1 += Math.signum(angle1-(quarter2*Math.PI/2+Math.PI/4));
                        quarter1 = (quarter1+4)%4;
                    }
                }
                Node temp_node = connectedNodes[0];
                connectedNodes[quarter2] = connectedNodes[1];
                connectedNodes[quarter1] = temp_node;
                if (flag) {
                    mainNode.to_flag[0] = false;
                    mainNode.to_flag[1] = false;
                    mainNode.to_flag[quarter1] = true;
                    mainNode.to_flag[quarter2] = true;
                } else {
                    mainNode.from_flag[0] = false;
                    mainNode.from_flag[1] = false;
                    mainNode.from_flag[quarter1] = true;
                    mainNode.from_flag[quarter2] = true;
                }
            } else {
                connectedNodes[quarter1] = connectedNodes[0];
                if (flag) {
                    mainNode.to_flag[0] = false;
                    mainNode.to_flag[quarter1] = true;
                } else {
                    mainNode.from_flag[0] = false;
                    mainNode.from_flag[quarter1] = true;
                }
            }
            
            if (flag)
                mainNode.to = connectedNodes;
            else
                mainNode.from = connectedNodes;
            
            return;
        }
        
        Node[] tempNode = new Node[4];
        int[] pair = new int[numberOfConnectedNodes];
        
        boolean EW_3_node_intersection = false;
        
        for (int i=0; i<numberOfConnectedNodes; i++) pair[i] = i;
        //Find aligned nodes
        for (int i=0; i<numberOfConnectedNodes; i++) {
            for (int j=i+1; j<numberOfConnectedNodes; j++) {
                x = connectedNodes[i].coordinates[0] -
                        connectedNodes[j].coordinates[0];
                y = connectedNodes[i].coordinates[1] -
                        connectedNodes[j].coordinates[1];
                Dij = Math.hypot(x, y); //Distance between nodes i & j
                x = connectedNodes[i].coordinates[0] -
                        mainNode.mainNode.coordinates[0];
                y = connectedNodes[i].coordinates[1] -
                        mainNode.mainNode.coordinates[1];
                Dim = Math.hypot(x, y); //Distance between nodes i & MN
                x = mainNode.mainNode.coordinates[0] -
                        connectedNodes[j].coordinates[0];
                y = mainNode.mainNode.coordinates[1] -
                        connectedNodes[j].coordinates[1];
                Dmj = Math.hypot(x, y); //Distance between nodes MN & j
                if (D > Math.abs(Dij - Dim - Dmj)) {
                    //Find largest angle between i,MN,j, so thta we may claim that 
                    //i & j are (semi-)alligned
                    D = Math.abs(Dij - Dim - Dmj);
                    temp1= i; temp2 = j;
                }
            }
        }
        if (D == 1000000) {
            //To Do: to verify the distance condition with the other two nodes
            //No need to verify!
            network.log("Error: Cannot identify reasonable alignment of the connected nodes to node "
                + mainNode.mainNode.label);
            return;
        }
        //Order the pairs:  node[pair[0]] & node[pair[2]] are on the same alignment
        //                  node[pair[1]] & node[pair[3]] are aligned
        pair[temp1] = pair[0];
        pair[0] = temp1;
        pair[temp2] = pair[2];
        pair[2] = temp2;
        
        if (numberOfConnectedNodes == 4) {
        //Case of 4-way intersection
            //Determine which node is on East and which on West
            x = Math.abs(connectedNodes[pair[0]].coordinates[1]
                    - connectedNodes[pair[2]].coordinates[1]);
            y = Math.abs(connectedNodes[pair[1]].coordinates[1]
                    - connectedNodes[pair[3]].coordinates[1]);
            if (x<=y) {
            //Case: 0 & 2 are East-West
                if (connectedNodes[pair[0]].coordinates[0] <
                        connectedNodes[pair[2]].coordinates[0]) {
                //Case: 2 is East
                    temp1 = pair[0];
                    pair[0] = pair[2];
                    pair[2] = temp1;
                }
            } else {
            //Case: 1 & 3 are East-West
                if (connectedNodes[pair[1]].coordinates[0] <
                        connectedNodes[pair[3]].coordinates[0]) {
                //Case: 3 is East
                    temp1 = pair[0];
                    pair[0] = pair[3];
                    pair[3] = temp1;
                    temp1 = pair[2];
                    pair[2] = pair[1];
                    pair[1] = temp1;
                } else {
                //Case: 1 is East    
                    temp1 = pair[0];
                    pair[0] = pair[1];
                    pair[1] = temp1;
                    temp1 = pair[2];
                    pair[2] = pair[3];
                    pair[3] = temp1;
                }
            }
            //Determine which node is on South and which on North
            if (connectedNodes[pair[1]].coordinates[1] >
                        connectedNodes[pair[3]].coordinates[1]) {
            //Case: 1 is North    
                temp1 = pair[3];
                pair[3] = pair[1];
                pair[1] = temp1;
            }
        } else if (numberOfConnectedNodes == 3) {
        //Case of 3-way intersection
            mainNode.singleNodeLabel = 1;
            boolean NW_to_SE = false;
            
            //Determine the alignment of pair 0-2: EW or NS
            x = Math.abs(connectedNodes[pair[0]].coordinates[0]
                    - connectedNodes[pair[2]].coordinates[0]);
            y = Math.abs(connectedNodes[pair[0]].coordinates[1]
                    - connectedNodes[pair[2]].coordinates[1]);
            if (x>y) {
            //Case: 0 & 2 are East-West
                EW_3_node_intersection =true;
                //Determine which node is on East and which on West
                if (connectedNodes[pair[0]].coordinates[0]
                        < connectedNodes[pair[2]].coordinates[0]) {
                    //pair[0]: East
                    temp1 = pair[0];
                    pair[0] = pair[2];
                    pair[2] = temp1;
                }
            } else {
                EW_3_node_intersection =false;
                //Determine which node is on North and which on South
                if (connectedNodes[pair[0]].coordinates[1]
                        < connectedNodes[pair[2]].coordinates[1]) {
                    //pair[0]: North
                    NW_to_SE = true;
                    temp1 = pair[0];
                    pair[0] = pair[2];
                    pair[2] = temp1;
                }
            }
            
            if (connectedNodes[pair[0]].coordinates[0]
                    == connectedNodes[pair[2]].coordinates[0]) {
                //Case: 0 & 2 form a verticel line
                if (connectedNodes[pair[0]].coordinates[0]
                        < connectedNodes[pair[1]].coordinates[0]) {
                    temp1 = pair[1];
                    pair[1] = pair[2];
                    pair[2] = temp1;
                    mainNode.singleNodeLabel = 2;
                }
            } else {
                //Find the third node's location
                //s = A xt + B yt + C
                //If s < 0, t lies in the clockwise halfplane of L;
                //if s > 0, t lies on the counter-clockwise halfplane;
                //if s = 0, t lies on L

                //x: slope of line between 0 & 2
                x = (connectedNodes[pair[0]].coordinates[1]
                        - connectedNodes[pair[2]].coordinates[1])/
                        (connectedNodes[pair[0]].coordinates[0]
                        - connectedNodes[pair[2]].coordinates[0]);
                //y: s = yt - m xt + (m x1 - y1)
                y = connectedNodes[pair[1]].coordinates[1]
                        - x * connectedNodes[pair[1]].coordinates[0]
                        + (x * connectedNodes[pair[0]].coordinates[0]
                        - connectedNodes[pair[0]].coordinates[1]);
                //no thing to do for y<0
                if ((!NW_to_SE && (y>0)) || (NW_to_SE && (y<0))) {
                    temp1 = pair[1];
                    pair[1] = pair[2];
                    pair[2] = temp1;
                    mainNode.singleNodeLabel = 2;
                } else if (y==0) {
                    //Case: the three connected nodes are on the same alignment
                    //Find location of the main node wrt the connected nodes
                    y = mainNode.mainNode.coordinates[1]
                            - x * mainNode.mainNode.coordinates[0]
                            + (x * connectedNodes[0].coordinates[0]
                            - connectedNodes[0].coordinates[1]);
                    //no thing to do for y>0
                    if(y<0) {
                        temp1 = pair[1];
                        pair[1] = pair[2];
                        pair[2] = temp1;
                        mainNode.singleNodeLabel = 2;
                    } else if (y==0)
                        network.log("Error: Node " + mainNode.mainNode.label
                                + " and their connected nodes ("
                                + connectedNodes[0].label + ", "
                                + connectedNodes[1].label + ", "
                                + connectedNodes[2].label + ") are aligned");
                }
            }
        }
        //Store the connected nodes with the final arrangement
        for (int i=0; i<numberOfConnectedNodes; i++) {
            tempNode[i] = connectedNodes[pair[i]];
        }
        connectedNodes = tempNode;
        
        if(mainNode.singleNodeLabel==1) {
            if(EW_3_node_intersection) {
                if (flag)
                    mainNode.to_flag[3] = false;
                else
                    mainNode.from_flag[3] = false;
                //Dummy node won't be used
                connectedNodes[3] = connectedNodes[1];
                //numberOfConnectedNodes++;
            } else {
                if (flag) {
                    mainNode.to_flag[2] = false;
                    mainNode.to_flag[3] = true;
                } else {
                    mainNode.from_flag[2] = false;
                    mainNode.from_flag[3] = true;
                }
                
                connectedNodes[3] = connectedNodes[0];
                connectedNodes[0] = connectedNodes[1];
                connectedNodes[1] = connectedNodes[2];
                //Dummy node won't be used
                connectedNodes[2] = connectedNodes[1];
                //numberOfConnectedNodes++;
            }
        } else if (mainNode.singleNodeLabel==2) {
            if(EW_3_node_intersection) {
                if (flag) {
                    mainNode.to_flag[1] = false;
                    mainNode.to_flag[3] = true;
                } else {
                    mainNode.from_flag[1] = false;
                    mainNode.from_flag[3] = true;
                }
                
                connectedNodes[3] = connectedNodes[2];
                connectedNodes[2] = connectedNodes[1];
                //Dummy node won't be used
                connectedNodes[1] = connectedNodes[3];
                //numberOfConnectedNodes++;
            } else {
                if (flag) {
                    mainNode.to_flag[0] = false;
                    mainNode.to_flag[3] = true;
                } else {
                    mainNode.from_flag[0] = false;
                    mainNode.from_flag[3] = true;
                }
                
                connectedNodes[3] = connectedNodes[0];
                //Dummy node won't be used
                connectedNodes[0] = connectedNodes[2];
                //numberOfConnectedNodes++;
            }
        }
        
        if (flag)
            mainNode.to = connectedNodes;
        else
            mainNode.from = connectedNodes;
    }
    
    public <T> void swap (T arg1, T arg2) {
        System.out.println(arg1 + "\n" + arg2);
        T temp = arg1;
        arg1 = arg2;
        arg2 = temp;
        System.out.println(arg1 + "\n" + arg2);
    }
    
    public void initControlFile() throws IOException {
        Path file_control = Paths.get(fFilePath.toString(), "control.dat");
        Charset charset = Charset.forName("UTF-8");
        int controlType = 1;
        int noOfApproaches = 0;
        int cycleLength = 0;
        File file_network = new File(fFilePath + "\\network.dat");
        try (Scanner scanner =  new Scanner(file_network)){
            //Get nodes' IDs
            scanner.nextInt();
            int NumberOfNodes = scanner.nextInt();
            int[] nodes = new int[NumberOfNodes];
            for (int i=0; i<NumberOfNodes; i++) {
                scanner.nextLine();
                if (!scanner.hasNextInt()) {
                    network.log("Error: Cannot find " + NumberOfNodes + " nodes in network.dat");
                    break;
                }
                nodes[i] = scanner.nextInt();
            }
            //Write in control.dat
            try (BufferedWriter writer = Files.newBufferedWriter(file_control, charset)) {
                writer.write(Integer.toString(1));
                writer.newLine();
                writer.write("\t0.00");
                writer.newLine();
                for(int i=0; i<NumberOfNodes; i++) {
                    String s = "\t" + Integer.toString(nodes[i]) + "\t" +
                            Integer.toString(controlType) + "\t" +
                            Integer.toString(noOfApproaches) + "\t" +
                            Integer.toString(cycleLength) + "\t\t\t\t\t\t";
                    writer.write(s);
                    writer.newLine();
                }
            } catch (IOException x) {
                network.log("Error: IOException: " + x);
            }
        } catch (IOException x) {
            network.log("Error: IOException: " + x);
        }
    }
    public void addPretimedControlHeader(MainNode mainNode, File file_control
            , int noOfApproaches, int cycleLength) throws IOException {
//        Path file_control = Paths.get(fFilePath.toString(), "islam.txt");
//        try (RandomAccessFile file = new RandomAccessFile(file_control.toFile().toString(), "rw")) {
        try (RandomAccessFile file = new RandomAccessFile(file_control, "rw")) {    
            file.seek(0);
            file.readLine();
            file.readLine();
            do {
                long pointer = file.getFilePointer();
                Scanner scanner = new Scanner(file.readLine()).useDelimiter("\t");
                //To do: Check by number of nodes in network
                if (!scanner.hasNext()) {
                    network.log("Error: Cannot find node " + mainNode.mainNode.label
                            + " in control.dat");
                    break;
                }
                if (Integer.parseInt(scanner.next()) == mainNode.mainNode.label) {
                    file.seek(pointer);
                    String s = "\t" + mainNode.mainNode.label + "\t4\t" + 
                            noOfApproaches + "\t" + cycleLength;
                    file.write(s.getBytes());
                    break;
                }
            } while (true);            
            file.close();
        } catch (IOException x) {
            network.log("Error: IOException: " + x);
        }
    }
 
    public void addPretimedControl(MainNode mainNode, boolean advLeft) throws IOException {
        if (mainNode.numberOfConnectedNodes == 2) {
            network.log("Warning: Attempt to add pre-timed control to node "
                    + mainNode.mainNode.label + " that connects only two nodes");
            return;
        } else if (mainNode.numberOfConnectedNodes == 1) {
            network.log("Warning: Attempt to add pre-timed control to node "
                    + mainNode.mainNode.label + " that is connected only to one node");
            return;
        } else if (mainNode.numberOfConnectedNodes == 0) {
            network.log("Warning: Attempt to add pre-timed control to a floating node "
                    + mainNode.mainNode.label);
            return;
        }
        Path file_control = Paths.get(fFilePath.toString(), "control.dat");
        String s;
        Charset charset = Charset.forName("US-ASCII");  //"UTF-8"
        int phaseNo = 1;
        int green = advLeft ? green4 : green2;
        try (BufferedWriter writer = Files.newBufferedWriter(file_control, charset, StandardOpenOption.APPEND)) {
            
            for (int N=0; N<2; N++) {
                if (advLeft && (mainNode.maxNoOfConnectedNodes!=phaseNo) &&
                        ((mainNode.to_flag[N] && mainNode.from_flag[(N+1)%4]) || 
                        (mainNode.to_flag[(N+2)%4] && mainNode.from_flag[(N+3)%4]))) {
                    //Phase1 & 3: Advanced Left (East-West & North-South)
                    s = "\t" +  mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + offset + "\t" + green
                            + "\t" + amber + "\t";
                    //the two LT's allowed or only one
                    if ((mainNode.to_flag[N] && mainNode.from_flag[(N+1)%4]) && 
                        (mainNode.to_flag[(N+2)%4] && mainNode.from_flag[(N+3)%4])) {
                        s = s + "2\t" + mainNode.to[N].label + "\t"
                                + mainNode.to[N+2].label + "\t0\t0";
                        writer.write(s, 0, s.length());
                        writer.newLine();
                        s = "\t" + mainNode.to[N].label + "\t"
                                + mainNode.mainNode.label + "\t" + phaseNo + "\t1\t"
                                + mainNode.from[(N+1)%4].label;
                        writer.write(s, 0, s.length());
                        writer.newLine();
                        s = "\t" + mainNode.to[N+2].label + "\t"
                                + mainNode.mainNode.label + "\t" + phaseNo + "\t1\t"
                                + mainNode.from[(N+3)%4].label;
                        writer.write(s, 0, s.length());
                        writer.newLine();
                    } else {
                        s = s + "1\t";
                        if ((mainNode.to_flag[N] && mainNode.from_flag[(N+1)%4])) {        
                            s = s + mainNode.to[N].label + "\t0\t0\t0";
                            writer.write(s, 0, s.length());
                            writer.newLine();
                            s = "\t" + mainNode.to[N].label + "\t"
                                    + mainNode.mainNode.label + "\t" + phaseNo + "\t1\t"
                                    + mainNode.from[(N+1)%4].label;
                            writer.write(s, 0, s.length());
                            writer.newLine();
                        } else if (mainNode.to_flag[(N+2)%4] && mainNode.from_flag[(N+3)%4]) {
                            s = s + mainNode.to[N+2].label + "\t0\t0\t0";
                            writer.write(s, 0, s.length());
                            writer.newLine();
                            s = "\t" + mainNode.to[N+2].label + "\t"
                                    + mainNode.mainNode.label + "\t" + phaseNo + "\t1\t"
                                    + mainNode.from[(N+3)%4].label;
                            writer.write(s, 0, s.length());
                            writer.newLine();
                        }
                    }
                    phaseNo++;
                }
                //Phase2 & 4: East-West & North-South
                if ((mainNode.to_flag[N] && (mainNode.from_flag[(N+1)%4] || mainNode.from_flag[(N+2)%4] || mainNode.from_flag[(N+3)%4]))
                        && (mainNode.to_flag[N+2] && (mainNode.from_flag[(N+3)%4] || mainNode.from_flag[(N+4)%4] || mainNode.from_flag[(N+5)%4]))) {
                    s = "\t" +  mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + offset + "\t"
                            + green + "\t" + amber + "\t2\t"
                            + mainNode.to[N].label + "\t"
                            + mainNode.to[N+2].label + "\t0\t0";
                    writer.write(s, 0, s.length());
                    writer.newLine();
                    int noOfConnNodes = (mainNode.from_flag[(N+1)%4] ? 1:0)
                            + (mainNode.from_flag[(N+2)%4] ? 1:0)
                            + (mainNode.from_flag[(N+3)%4] ? 1:0);
                    s = "\t" + mainNode.to[N].label + "\t"
                            + mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + noOfConnNodes + "\t";
                    if (mainNode.from_flag[(N+1)%4])
                        s = s + mainNode.from[(N+1)%4].label + "\t";
                    if (mainNode.from_flag[(N+2)%4])
                        s = s + mainNode.from[(N+2)%4].label + "\t";
                    if (mainNode.from_flag[(N+3)%4])
                        s = s + mainNode.from[(N+3)%4].label;
                    writer.write(s, 0, s.length());
                    writer.newLine();
                    
                    noOfConnNodes = (mainNode.from_flag[(N+3)%4] ? 1:0)
                            + (mainNode.from_flag[(N+4)%4] ? 1:0)
                            + (mainNode.from_flag[(N+5)%4] ? 1:0);
                    s = "\t" + mainNode.to[N+2].label + "\t"
                            + mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + noOfConnNodes + "\t";
                    if (mainNode.from_flag[(N+3)%4])
                        s = s + mainNode.from[(N+3)%4].label + "\t";
                    if (mainNode.from_flag[(N+4)%4])
                        s = s + mainNode.from[(N+4)%4].label + "\t";
                    if (mainNode.from_flag[(N+5)%4])
                        s = s + mainNode.from[(N+5)%4].label;
                    writer.write(s, 0, s.length());
                    writer.newLine();
                    
                    phaseNo++;
                } else if ((mainNode.to_flag[N] && (mainNode.from_flag[(N+1)%4] || mainNode.from_flag[(N+2)%4] || mainNode.from_flag[(N+3)%4]))) {
                    s = "\t" +  mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + offset + "\t"
                            + green + "\t" + amber + "\t1\t"
                            + mainNode.to[N].label + "\t0\t0\t0";
                    writer.write(s, 0, s.length());
                    writer.newLine();
                    int noOfConnNodes = (mainNode.from_flag[(N+1)%4] ? 1:0)
                            + (mainNode.from_flag[(N+2)%4] ? 1:0)
                            + (mainNode.from_flag[(N+3)%4] ? 1:0);
                    s = "\t" + mainNode.to[N].label + "\t"
                            + mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + noOfConnNodes + "\t";
                    if (mainNode.from_flag[(N+1)%4])
                        s = s + mainNode.from[(N+1)%4].label + "\t";
                    if (mainNode.from_flag[(N+2)%4])
                        s = s + mainNode.from[(N+2)%4].label + "\t";
                    if (mainNode.from_flag[(N+3)%4])
                        s = s + mainNode.from[(N+3)%4].label;
                    writer.write(s, 0, s.length());
                    writer.newLine();
                    
                    phaseNo++;
                } else if ((mainNode.to_flag[N+2] && (mainNode.from_flag[(N+3)%4] || mainNode.from_flag[(N+4)%4] || mainNode.from_flag[(N+5)%4]))) {
                    s = "\t" +  mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + offset + "\t"
                            + green + "\t" + amber + "\t1\t"
                            + mainNode.to[N+2].label + "\t0\t0\t0";
                    writer.write(s, 0, s.length());
                    writer.newLine();
                    int noOfConnNodes = (mainNode.from_flag[(N+3)%4] ? 1:0)
                            + (mainNode.from_flag[(N+4)%4] ? 1:0)
                            + (mainNode.from_flag[(N+5)%4] ? 1:0);
                    s = "\t" + mainNode.to[N+2].label + "\t"
                            + mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + noOfConnNodes + "\t";
                    if (mainNode.from_flag[(N+3)%4])
                        s = s + mainNode.from[(N+3)%4].label + "\t";
                    if (mainNode.from_flag[(N+4)%4])
                        s = s + mainNode.from[(N+4)%4].label + "\t";
                    if (mainNode.from_flag[(N+5)%4])
                        s = s + mainNode.from[(N+5)%4].label;
                    writer.write(s, 0, s.length());
                    writer.newLine();
                    
                    phaseNo++;
                }
            }
            
        } catch (IOException x) {
            network.log("Error: IOException: " + x);
        }
        addPretimedControlHeader(mainNode, file_control.toFile(), --phaseNo
                , (green+amber)*phaseNo);
    }
    
    public void addActuatedControlHeader(MainNode mainNode, File file_control
            , int noOfApproaches, int cycleLength) throws IOException {
//        Path file_control = Paths.get(fFilePath.toString(), "islam.txt");
//        try (RandomAccessFile file = new RandomAccessFile(file_control.toFile().toString(), "rw")) {
        try (RandomAccessFile file = new RandomAccessFile(file_control, "rw")) {    
            file.seek(0);
            file.readLine();
            file.readLine();
            do {
                long pointer = file.getFilePointer();
                Scanner scanner = new Scanner(file.readLine()).useDelimiter("\t");
                //To do: Check by number of nodes in network
                if (!scanner.hasNext()) {
                    network.log("Error: Cannot find node " + mainNode.mainNode.label
                            + " in control.dat");
                    break;
                }
                if (Integer.parseInt(scanner.next()) == mainNode.mainNode.label) {
                    file.seek(pointer);
                    String s = "\t" + mainNode.mainNode.label + "\t5\t" + 
                            noOfApproaches + "\t" + cycleLength;
                    file.write(s.getBytes());
                    break;
                }
            } while (true);            
            file.close();
        } catch (IOException x) {
            network.log("Error: IOException: " + x);
        }
    }
 
    public void addActuatedControl(MainNode mainNode, boolean advLeft) throws IOException {
        if (mainNode.numberOfConnectedNodes == 2) {
            network.log("Warning: Attempt to add actuated control to node "
                    + mainNode.mainNode.label + " that connects only two nodes");
            return;
        } else if (mainNode.numberOfConnectedNodes == 1) {
            network.log("Warning: Attempt to add actuated control to node "
                    + mainNode.mainNode.label + " that is connected only to one node");
            return;
        } else if (mainNode.numberOfConnectedNodes == 0) {
            network.log("Warning: Attempt to add actuated control to a floating node "
                    + mainNode.mainNode.label);
            return;
        }
        Path file_control = Paths.get(fFilePath.toString(), "control.dat");
        String s;
        Charset charset = Charset.forName("US-ASCII");  //"UTF-8"
        //int noOfApproaches = advLeft ? mainNode.numberOfConnectedNodes : 2;
        int phaseNo = 1;
        //addActuatedControlHeader(mainNode, file_control.toFile(), noOfApproaches);
        int maxGreen = advLeft ? maxGreen4 : maxGreen2;
        int minGreen = advLeft ? minGreen4 : minGreen2;
        /*network.log("Debug: MN: " + mainNode.mainNode.getID());
        network.log("Debug: Nodes:" + mainNode.connectedNodes[0].getID() + ","
                + mainNode.connectedNodes[1].getID() + "," 
                + mainNode.connectedNodes[2].getID() + ","
                + mainNode.connectedNodes[3].getID());
        network.log("Debug: AL:" + advLeft + "," + mainNode.to[0] + ","
                + mainNode.to[1] + "," + mainNode.to[2] + "," + mainNode.to[3]);
        network.log("Debug: From:" + mainNode.from[0] + ","
                + mainNode.from[1] + "," + mainNode.from[2] + "," + mainNode.from[3]);
        network.log("Debug: maxNoOfConnectedNodes" + mainNode.mainNode.getID() 
                + "," + mainNode.maxNoOfConnectedNodes);
        */
        try (BufferedWriter writer = Files.newBufferedWriter(file_control, charset, StandardOpenOption.APPEND)) {
            
            for (int N=0; N<2; N++) {
                if (advLeft && (mainNode.maxNoOfConnectedNodes!=phaseNo) &&
                        ((mainNode.to_flag[N] && mainNode.from_flag[(N+1)%4]) || 
                        (mainNode.to_flag[(N+2)%4] && mainNode.from_flag[(N+3)%4]))) {
                    //Phase1 & 3: Advanced Left (East-West & North-South)
                    s = "\t" +  mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + maxGreen + "\t" + minGreen
                            + "\t" + amber + "\t";
                    //(if) the two LT's allowed or (else) only one of them
                    if ((mainNode.to_flag[N] && mainNode.from_flag[(N+1)%4]) && 
                        (mainNode.to_flag[(N+2)%4] && mainNode.from_flag[(N+3)%4])) {
                        s = s + "2\t" + mainNode.to[N].label + "\t"
                                + mainNode.to[N+2].label + "\t0\t0";
                        writer.write(s, 0, s.length());
                        writer.newLine();
                        s = "\t" + mainNode.to[N].label + "\t"
                                + mainNode.mainNode.label + "\t" + phaseNo + "\t1\t"
                                + mainNode.from[(N+1)%4].label;
                        writer.write(s, 0, s.length());
                        writer.newLine();
                        s = "\t" + mainNode.to[N+2].label + "\t"
                                + mainNode.mainNode.label + "\t" + phaseNo + "\t1\t"
                                + mainNode.from[(N+3)%4].label;
                        writer.write(s, 0, s.length());
                        writer.newLine();
                    } else {
                        s = s + "1\t";
                        if ((mainNode.to_flag[N] && mainNode.from_flag[(N+1)%4])) {        
                            s = s + mainNode.to[N].label + "\t0\t0\t0";
                            writer.write(s, 0, s.length());
                            writer.newLine();
                            s = "\t" + mainNode.to[N].label + "\t"
                                    + mainNode.mainNode.label + "\t" + phaseNo + "\t1\t"
                                    + mainNode.from[(N+1)%4].label;
                            writer.write(s, 0, s.length());
                            writer.newLine();
                        } else if (mainNode.to_flag[(N+2)%4] && mainNode.from_flag[(N+3)%4]) {
                            s = s + mainNode.to[N+2].label + "\t0\t0\t0";
                            writer.write(s, 0, s.length());
                            writer.newLine();
                            s = "\t" + mainNode.to[N+2].label + "\t"
                                    + mainNode.mainNode.label + "\t" + phaseNo + "\t1\t"
                                    + mainNode.from[(N+3)%4].label;
                            writer.write(s, 0, s.length());
                            writer.newLine();
                        }
                    }
                    phaseNo++;
                }
                //Phase2 & 4: East-West & North-South
                if ((mainNode.to_flag[N] && (mainNode.from_flag[(N+1)%4] || mainNode.from_flag[(N+2)%4] || mainNode.from_flag[(N+3)%4]))
                        && (mainNode.to_flag[N+2] && (mainNode.from_flag[(N+3)%4] || mainNode.from_flag[(N+4)%4] || mainNode.from_flag[(N+5)%4]))) {
                    s = "\t" +  mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + maxGreen + "\t"
                            + minGreen + "\t" + amber + "\t2\t"
                            + mainNode.to[N].label + "\t"
                            + mainNode.to[N+2].label + "\t0\t0";
                    writer.write(s, 0, s.length());
                    writer.newLine();
                    int noOfConnNodes = (mainNode.from_flag[(N+1)%4] ? 1:0)
                            + (mainNode.from_flag[(N+2)%4] ? 1:0)
                            + (mainNode.from_flag[(N+3)%4] ? 1:0);
                    s = "\t" + mainNode.to[N].label + "\t"
                            + mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + noOfConnNodes + "\t";
                    if (mainNode.from_flag[(N+1)%4])
                            s = s + mainNode.from[(N+1)%4].label + "\t";
                    if (mainNode.from_flag[(N+2)%4])
                        s = s + mainNode.from[(N+2)%4].label + "\t";
                    if (mainNode.from_flag[(N+3)%4])
                        s = s + mainNode.from[(N+3)%4].label;
                    writer.write(s, 0, s.length());
                    writer.newLine();
                    
                    noOfConnNodes = (mainNode.from_flag[(N+3)%4] ? 1:0)
                            + (mainNode.from_flag[(N+4)%4] ? 1:0)
                            + (mainNode.from_flag[(N+5)%4] ? 1:0);
                    s = "\t" + mainNode.to[N+2].label + "\t"
                            + mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + noOfConnNodes + "\t";
                    if (mainNode.from_flag[(N+3)%4])
                        s = s + mainNode.from[(N+3)%4].label + "\t";
                    if (mainNode.from_flag[(N+4)%4])
                        s = s + mainNode.from[(N+4)%4].label + "\t";
                    if (mainNode.from_flag[(N+5)%4])
                        s = s + mainNode.from[(N+5)%4].label;
                    writer.write(s, 0, s.length());
                    writer.newLine();
                    
                    phaseNo++;
                } else if ((mainNode.to_flag[N] && (mainNode.from_flag[(N+1)%4] || mainNode.from_flag[(N+2)%4] || mainNode.from_flag[(N+3)%4]))) {
                    s = "\t" +  mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + maxGreen + "\t"
                            + minGreen + "\t" + amber + "\t1\t"
                            + mainNode.to[N].label + "\t0\t0\t0";
                    writer.write(s, 0, s.length());
                    writer.newLine();
                    int noOfConnNodes = (mainNode.from_flag[(N+1)%4] ? 1:0)
                            + (mainNode.from_flag[(N+2)%4] ? 1:0)
                            + (mainNode.from_flag[(N+3)%4] ? 1:0);
                    s = "\t" + mainNode.to[N].label + "\t"
                            + mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + noOfConnNodes + "\t";
                    if (mainNode.from_flag[(N+1)%4])
                        s = s + mainNode.from[(N+1)%4].label + "\t";
                    if (mainNode.from_flag[(N+2)%4])
                        s = s + mainNode.from[(N+2)%4].label + "\t";
                    if (mainNode.from_flag[(N+3)%4])
                        s = s + mainNode.from[(N+3)%4].label;
                    writer.write(s, 0, s.length());
                    writer.newLine();
                    
                    phaseNo++;
                } else if ((mainNode.to_flag[N+2] && (mainNode.from_flag[(N+3)%4] || mainNode.from_flag[(N+4)%4] || mainNode.from_flag[(N+5)%4]))) {
                    s = "\t" +  mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + maxGreen + "\t"
                            + minGreen + "\t" + amber + "\t1\t"
                            + mainNode.to[N+2].label + "\t0\t0\t0";
                    writer.write(s, 0, s.length());
                    writer.newLine();
                    int noOfConnNodes = (mainNode.from_flag[(N+3)%4] ? 1:0)
                            + (mainNode.from_flag[(N+4)%4] ? 1:0)
                            + (mainNode.from_flag[(N+5)%4] ? 1:0);
                    s = "\t" + mainNode.to[N+2].label + "\t"
                            + mainNode.mainNode.label + "\t"
                            + phaseNo + "\t" + noOfConnNodes + "\t";
                    if (mainNode.from_flag[(N+3)%4])
                        s = s + mainNode.from[(N+3)%4].label + "\t";
                    if (mainNode.from_flag[(N+4)%4])
                        s = s + mainNode.from[(N+4)%4].label + "\t";
                    if (mainNode.from_flag[(N+5)%4])
                        s = s + mainNode.from[(N+5)%4].label;
                    writer.write(s, 0, s.length());
                    writer.newLine();
                    
                    phaseNo++;
                }
            }
            
        } catch (IOException x) {
            network.log("Error: IOException: " + x);
        }
        addActuatedControlHeader(mainNode, file_control.toFile(), --phaseNo
                , (maxGreen+amber)*phaseNo);
    }
    
    public void addYieldSignHeader(MainNode mainNode, File file_control) throws IOException {
        int noOfApproaches = 0;
        long pointer;
        try (RandomAccessFile file = new RandomAccessFile(file_control, "rw")) {    
            file.seek(0);
            file.readLine();
            file.readLine();
            do {
                pointer = file.getFilePointer();
                Scanner scanner = new Scanner(file.readLine()).useDelimiter("\t");
                //To do: Check by number of nodes in network
                if (!scanner.hasNext()) {
                    network.log("Error: Cannot find node " + mainNode.mainNode.label
                            + " in control.dat");
                    break;
                }
                if (Integer.parseInt(scanner.next()) == mainNode.mainNode.label) {
                    file.seek(pointer);
                    String s = "\t" + mainNode.mainNode.label + "\t2\t" + 
                            noOfApproaches + "\t0";
                    file.write(s.getBytes());
                    break;
                }
            } while (true);            
            file.close();
        } catch (IOException x) {
            network.log("Error: IOException: " + x);
        }
    }
    
    public void addYieldSign(MainNode mainNode) throws IOException {
        Path file_control = Paths.get(fFilePath.toString(), "control.dat");
        String s;
        Charset charset = Charset.forName("US-ASCII");
        addYieldSignHeader(mainNode, file_control.toFile());
        try (BufferedWriter writer = Files.newBufferedWriter(file_control, charset, StandardOpenOption.APPEND)) {
            s = "\t" +  mainNode.mainNode.label + "\t"
                    + mainNode.numberOfConnectedNodes_to + "\t0";
            writer.write(s, 0, s.length());
            writer.newLine();
            //Major approaches
            s = "";
            for (int i=0; i<mainNode.numberOfConnectedNodes_to; i++)
                s = s + "\t" +  mainNode.to[i].label + "\t" + mainNode.mainNode.label;
            //Consider the dummy node that added inside arrangeConnectedNodes
            writer.write(s, 0, s.length());
            writer.newLine();
            //Minor approaches
            writer.newLine();
        }
    }
    
    public void add2wayStopHeader(MainNode mainNode, File file_control) throws IOException {
        int noOfApproaches = 0;
        try (RandomAccessFile file = new RandomAccessFile(file_control, "rw")) {    
            file.seek(0);
            file.readLine();
            file.readLine();
            do {
                long pointer = file.getFilePointer();
                Scanner scanner = new Scanner(file.readLine()).useDelimiter("\t");
                //To do: Check by number of nodes in network
                if (!scanner.hasNext()) {
                    network.log("Error: Cannot find node " + mainNode.mainNode.label
                            + " in control.dat");
                    break;
                }
                if (Integer.parseInt(scanner.next()) == mainNode.mainNode.label) {
                    file.seek(pointer);
                    String s = "\t" + mainNode.mainNode.label + "\t6\t" + 
                            noOfApproaches + "\t0";
                    file.write(s.getBytes());
                    break;
                }
            } while (true);            
            file.close();
        } catch (IOException x) {
            network.log("Error: IOException: " + x);
        }
    }
    
    public void add2wayStop(MainNode mainNode) throws IOException {
        Path file_control = Paths.get(fFilePath.toString(), "control.dat");
        String s;
        Charset charset = Charset.forName("US-ASCII");
        add2wayStopHeader(mainNode, file_control.toFile());
        try (BufferedWriter writer = Files.newBufferedWriter(file_control, charset, StandardOpenOption.APPEND)) {
            s = "\t" +  mainNode.mainNode.label + "\t"
                    + mainNode.numberOfConnectedNodes + "\t0";
            writer.write(s, 0, s.length());
            writer.newLine();
            //Major approaches
            s = "";
            for (int i=0; i<mainNode.numberOfConnectedNodes; i++)
                s = s + "\t" +  mainNode.connectedNodes[i].label
                        + "\t" + mainNode.mainNode.label;
            writer.write(s, 0, s.length());
            writer.newLine();
            //Minor approaches
            writer.newLine();
        }
    }
    
    public void add4WayStop(MainNode mainNode) throws IOException {
        Path file_control = Paths.get(fFilePath.toString(), "control.dat");
        try (RandomAccessFile file = new RandomAccessFile(file_control.toFile(), "rw")) {
            file.seek(0);
            file.readLine();
            file.readLine();
            do {
                long pointer = file.getFilePointer();
                Scanner scanner = new Scanner(file.readLine()).useDelimiter("\t");
                if (Integer.parseInt(scanner.next()) == mainNode.mainNode.label) {
                    file.seek(pointer);
                    String s = "\t" + mainNode.mainNode.label + "\t3\t0\t0";
                    file.write(s.getBytes());
                    break;
                }
            } while (file.readLine() != null);            
            file.close();
        } catch (IOException x) {
            network.log("Error: IOException: " + x);
        }
    }
    
//    public void log(String s){
//        //java -jar DynusT.jar [args] > control-log.txt
//        Path file_control = Paths.get(fFilePath.toString(), "control-log.txt");
//        Charset charset = Charset.forName("US-ASCII");
//        try (BufferedWriter writer = Files.newBufferedWriter(file_control, charset, StandardOpenOption.APPEND)) {
//            writer.write(s, 0, s.length());
//            writer.newLine();
//        } catch (IOException x) {
//            System.err.format("IOException: %s%n", x);
//        }
//    }
    
    int maxConnectedNodes;
    // PRIVATE 
    
    private final Path fFilePath;
    
    private final int cycleLength;
    private final int green2;
    private final int maxGreen2;
    private final int minGreen2;
    private final int green3;
    private final int maxGreen3;
    private final int minGreen3;
    private final int green4;
    private final int maxGreen4;
    private final int minGreen4;
    private final int amber;
    private final int offset;
    private final Network network;
}


class MainNode {
    /**
     Constructor.
     @param aNode the main node
    */
    public MainNode(Node aNode, Network aNetwork){
        mainNode = aNode;
        numberOfConnectedNodes = 0;
        numberOfConnectedNodes_to = 0;
        numberOfConnectedNodes_from = 0;
        maxNoOfConnectedNodes = 4;
        connectedNodes = new Node[maxNoOfConnectedNodes];
        singleNodeLabel = -1;
        to = new Node[maxNoOfConnectedNodes];
        from = new Node[maxNoOfConnectedNodes];
        to_flag = new boolean[maxNoOfConnectedNodes];
        from_flag = new boolean[maxNoOfConnectedNodes];
        network = aNetwork;
    }
    /**
     Constructor.
     @param aNode the main node
    */
    public MainNode(Node aNode, Network aNetwork, int aNoConnNodes){
        mainNode = aNode;
        numberOfConnectedNodes = 0;
        numberOfConnectedNodes_to = 0;
        numberOfConnectedNodes_from = 0;
        maxNoOfConnectedNodes = aNoConnNodes;
        connectedNodes = new Node[maxNoOfConnectedNodes];
        singleNodeLabel = -1;
        to = new Node[maxNoOfConnectedNodes];
        from = new Node[maxNoOfConnectedNodes];
        to_flag = new boolean[maxNoOfConnectedNodes];
        from_flag = new boolean[maxNoOfConnectedNodes];
        network = aNetwork;
    }
    public void AddConnectedNode (Node aNode, boolean flag) {
        /*----------------------------------------------------------------------
        flag=true:  aNode->mainNode (to)
        flag=false: aNode<-mainNode (from)
        ----------------------------------------------------------------------*/
        if (flag) {
            if (numberOfConnectedNodes_to == 4) {
                network.log("Warning: Node " + mainNode.getID() + " has more than 4 incoming links" +
                        "\n\t\t Cannot connect node " + aNode.label);
                return;
            }
            to[numberOfConnectedNodes_to] = aNode;
            to_flag[numberOfConnectedNodes_to++] = true;
        } else {
            if (numberOfConnectedNodes_from == 4) {
                network.log("Warning: Node " + mainNode.getID() + " has more than 4 outgoing links" +
                        "\n\t\t Cannot connect node " + aNode.label);
                return;
            }
            from[numberOfConnectedNodes_from] = aNode;
            from_flag[numberOfConnectedNodes_from++] = true;
        }
    }
    
    Node mainNode;
    Node[] connectedNodes;
    int numberOfConnectedNodes;
    int numberOfConnectedNodes_to;
    int numberOfConnectedNodes_from;
    int maxNoOfConnectedNodes;
    int singleNodeLabel;
    Node[] to;
    Node[] from;
    boolean[] to_flag;
    boolean[] from_flag;
    Network network;
}
