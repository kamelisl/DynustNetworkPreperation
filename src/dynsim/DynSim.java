
package dynsim;

/**
 *
 * @author Islam
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

/**
 *
 * @author Islam
 */
public class DynSim {

    public static void main(String[] args) throws IOException {
        //args: -<options: s, n, and/or l> <network.dat path> <signal control file (full address) (optional)>
        Network network = new Network(args[1]);
        initLogFile(network.getPath());
        
        if (args[0].contains("f") || args[0].contains("f")) {
            File input_file = (args.length>2)? new File(args[2]):
                    new File(args[1].concat("\\nodes_to_be_controlled.txt"));
            if (!filterSignals(network,input_file))
                network.log("Error: Failed to filter signals");
        }
        
        if (args[0].contains("s") || args[0].contains("S")) {
            File input_file = (args.length>2)? new File(args[2]):
                    new File(args[1].concat("\\nodes_to_be_controlled.txt"));
            if (!addSignalControl(network,input_file))
                network.log("Error: Failed to add signal controls");
        }
        
        if (args[0].contains("n") || args[0].contains("N")) {
            network.setZones();
            DestinationNodes nodes_zones = new DestinationNodes();
            if (!nodes_zones.findDestinationNodes(network))
                network.log("Error: Failed to find destination nodes");
            
            if (args[0].contains("l") || args[0].contains("L")) {
                GenerationLinks links = new GenerationLinks();
                if (!links.findGenerationLinks(network))
                    network.log("Error: Failed to generate generation links");
            }
        } else if (args[0].contains("l") || args[0].contains("L")) {
            network.setZones();
            GenerationLinks links = new GenerationLinks();
            if (!links.findGenerationLinks(network))
                network.log("Error: Failed to generate generation links");
        }
        
        network.log("Done");
        String s = "=======" +
                    new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss").format(new Date())
                    + "=======";
        network.log(s);
            
        //args: <network.dat path> <signal control file (full address)>
//        Network network = new Network(args[0]);
//        initLogFile(network.getPath());
//        network.setZones();
//        
//        if (args.length>1) {
//            File input_file = new File(args[1]);
//            if (!addSignalControl(network,input_file))
//                network.log("Error: Failed to add signal controls");
//        }
//        
//        DestinationNodes nodes_zones = new DestinationNodes();
//        if (!nodes_zones.findDestinationNodes(network))
//            network.log("Error: Failed to find destination nodes");
//        
//        GenerationLinks links = new GenerationLinks();
//        if (!links.findGenerationLinks(network))
//            network.log("Error: Failed to generate generation links");
//        
//        network.log("Done");
//        String s = "=======" +
//                    new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss").format(new Date())
//                    + "=======";
//        network.log(s);
    }
    
    public static boolean addSignalControl (Network network, File input_file) throws IOException {
        SignalControl signal_control = new SignalControl(network);
        int numberOfNodes = 0;
        boolean advance;
        int number_of_added_signals = 0;
        //Get number of nodes needed to be controlled
        try (Scanner scanner =  new Scanner(input_file).useDelimiter("\t")){
            while (scanner.hasNextLine()){
                if (!scanner.hasNextInt()) break;
                numberOfNodes++;
                scanner.nextLine();
            }
            scanner.close();
        } catch (NumberFormatException e) {
            network.log("Error: Cannot open " + input_file);
            return false;
        }
        if (numberOfNodes == 0) {
            network.log("Error: Cannot exclude data from " + input_file);
            return false;
        }
        
        try (Scanner scanner =  new Scanner(input_file)){
            //To do: use dynamic array to store inputs to scan the input file once
            int[][] nodes_control_adv = new int[numberOfNodes][3];
            for (int i=0; i<numberOfNodes; i++) {
                if (!scanner.hasNextInt()) {
                    network.log("Error: Invalid format: " + input_file +
                            ": line " + Integer.toString(i+1));
                    return false;
                }
                nodes_control_adv[i][0] = scanner.nextInt();
                if (!scanner.hasNextInt()) {
                    network.log("Error: Invalid control type for node " +
                            nodes_control_adv[i][0]);
                    return false;
                }
                nodes_control_adv[i][1] = scanner.nextInt();
                if (!scanner.hasNextInt()) {
                    network.log("Error: Advanced Left option should be 0 or 1: node " +
                            nodes_control_adv[i][0]);
                    return false;
                }
                nodes_control_adv[i][2] = scanner.nextInt();
                if (scanner.hasNextLine()) scanner.nextLine();
                else break;
            }
            
            //Initialize array of nodes
            MainNode[] Nodes = new MainNode[numberOfNodes];
            int[] connectedNodes = new int[signal_control.maxConnectedNodes+1];
            signal_control.initControlFile();
            for (int i=0; i<Nodes.length; i++) {
                //To do: don't need to get and store zone of each node
//                MainNode tempNode = new MainNode(new Node(nodes_control_adv[i][0],
//                        signal_control.findNodeZone(nodes_control_adv[i][0]),
//                        signal_control.findNodeCoordinates(nodes_control_adv[i][0])), network);
                MainNode tempNode = new MainNode(new Node(nodes_control_adv[i][0], 1,
                        signal_control.findNodeCoordinates(nodes_control_adv[i][0])), network);
                Nodes[i] = tempNode;
                //Find connected nodes (to) main node---------------------------
                connectedNodes = signal_control.findConnectedNodes(Nodes[i].mainNode.label,true);
                //Don't add signals to nodes connected to freeways
                //Don't add signals to nodes that have only one incoming approach
                if ((connectedNodes[0] == -1) || (connectedNodes[0] == 1))    continue;
                //Find coordinates of connected nodes and add them to the MainNode
                for (int j=1; j <= connectedNodes[0]; j++) {
//                    Nodes[i].AddConnectedNode(new Node(connectedNodes[j],
//                            signal_control.findNodeZone(connectedNodes[j]),
//                            signal_control.findNodeCoordinates(connectedNodes[j])),true);
                    Nodes[i].AddConnectedNode(new Node(connectedNodes[j], 1,
                            signal_control.findNodeCoordinates(connectedNodes[j])),true);
                }
                //Find connected nodes (from) main node
                connectedNodes = signal_control.findConnectedNodes(Nodes[i].mainNode.label,false);
                //Don't add signals to nodes connected to freeways
                //Don't add signals to nodes connected to start of ramps
                if (connectedNodes[0] == -1)    continue;
                //Find coordinates of connected nodes and add them to the MainNode
                for (int j=1; j <= connectedNodes[0]; j++) {
//                    Nodes[i].AddConnectedNode(new Node(connectedNodes[j],
//                            signal_control.findNodeZone(connectedNodes[j]),
//                            signal_control.findNodeCoordinates(connectedNodes[j])),false);
                    Nodes[i].AddConnectedNode(new Node(connectedNodes[j], 1,
                            signal_control.findNodeCoordinates(connectedNodes[j])),false);
                }
                //--------------------------------------------------------------
                if (signal_control.isEndOfRamp(Nodes[i].mainNode.label)) {
                    //To do: if end of ramp of length less than 150m, don't add signal
                }
                //Don't add signals to a node connected to 3 nodes and at least
                //one of them is signalized
                else if ((Nodes[i].numberOfConnectedNodes_from == 1) && (Nodes[i].numberOfConnectedNodes_to == 2)) {
                    boolean near_signals = false;
                    for (int j=0; j<numberOfNodes; j++) {
                        if ((nodes_control_adv[j][0]==Nodes[i].from[0].getID()) ||
                                (nodes_control_adv[j][0]==Nodes[i].to[0].getID()) ||
                                (nodes_control_adv[j][0]==Nodes[i].to[1].getID())) {
                            near_signals = true;
                            break;
                        }
                    }
                    if (near_signals) {
                        network.log("Warning: A sign/signal is specified on node "
                                + Nodes[i].mainNode.label + " which is "
                                + "directly connected to another signalized node");
                        continue;
                    }
                }
                
                //Nodes[i].maxNoOfConnectedNodes = Nodes[i].numberOfConnectedNodes;
                int NoOfCommonNodes = 0;
                for (int j=0; j<Nodes[i].numberOfConnectedNodes_to; j++) {
                    for (int k=0; k<Nodes[i].numberOfConnectedNodes_from; k++) {
                        if (Nodes[i].to[j].getID()==Nodes[i].from[k].getID()) {
                            NoOfCommonNodes++;
                            break;
                        }
                    }
                }
                
                if ((NoOfCommonNodes==Nodes[i].numberOfConnectedNodes_to) &&
                        (NoOfCommonNodes==Nodes[i].numberOfConnectedNodes_from)) {
                    signal_control.arrangeConnectedNodes(tempNode, true);
                    Nodes[i].from = Nodes[i].to;
                    Nodes[i].from_flag = Nodes[i].to_flag;
                } else {
                    //signal_control.arrangeConnectedNodes(tempNode, true);
                    //signal_control.arrangeConnectedNodes(tempNode, false);
                    if (!signal_control.arrangeConnectedNodes2(tempNode)) {
                        network.log("Error: cannot arrange connected nodes to node: "
                                + Nodes[i].mainNode.label);
                        continue;
                    }
                }
//                //Just for DEBUG
//                for (int j=0; j<4; j++) {
//                    network.log("Node: " + Nodes[i].mainNode.getID());
//                    network.log("to:" + Nodes[i].to_flag[j]);
//                    if (Nodes[i].to_flag[j]) network.log(";" + Nodes[i].to[j].getID());
//                    network.log("from:" + Nodes[i].from_flag[j]);
//                    if (Nodes[i].from_flag[j]) network.log(";" + Nodes[i].from[j].getID());
//                }//End of DEBUG
                                
                //Number of approaches to/from the signalized node
                Nodes[i].numberOfConnectedNodes = ((Nodes[i].from_flag[0] || Nodes[i].to_flag[0]) ? 1:0)
                            + ((Nodes[i].from_flag[1] || Nodes[i].to_flag[1]) ? 1:0)
                            + ((Nodes[i].from_flag[2] || Nodes[i].to_flag[2]) ? 1:0)
                            + ((Nodes[i].from_flag[3] || Nodes[i].to_flag[3]) ? 1:0);
                Nodes[i].maxNoOfConnectedNodes = Nodes[i].numberOfConnectedNodes;
                
                if ((Nodes[i].numberOfConnectedNodes == 3)) {
                    boolean near_signals = false;
                    for (int j=0; j<numberOfNodes; j++) {
                        if (Nodes[i].from_flag[0] && (nodes_control_adv[j][0]==Nodes[i].from[0].getID())) {
                            near_signals = true;
                            break;
                        } else if (Nodes[i].from_flag[1] && (nodes_control_adv[j][0]==Nodes[i].from[1].getID())) {
                            near_signals = true;
                            break;
                        } else if (Nodes[i].from_flag[2] && (nodes_control_adv[j][0]==Nodes[i].from[2].getID())) {
                            near_signals = true;
                            break;
                        } else if (Nodes[i].from_flag[3] && (nodes_control_adv[j][0]==Nodes[i].from[3].getID())) {
                            near_signals = true;
                            break;
                        } else if (Nodes[i].to_flag[0] && (nodes_control_adv[j][0]==Nodes[i].to[0].getID())) {
                            near_signals = true;
                            break;
                        } else if (Nodes[i].to_flag[1] && (nodes_control_adv[j][0]==Nodes[i].to[1].getID())) {
                            near_signals = true;
                            break;
                        } else if (Nodes[i].to_flag[2] && (nodes_control_adv[j][0]==Nodes[i].to[2].getID())) {
                            near_signals = true;
                            break;
                        } else if (Nodes[i].to_flag[3] && (nodes_control_adv[j][0]==Nodes[i].to[3].getID())) {
                            near_signals = true;
                            break;
                        }
                    }
                    if (near_signals) {
                        network.log("Warning: A sign/signal is specified on node "
                                + Nodes[i].mainNode.label + " which is "
                                + "directly connected to another signalized node");
                        continue;
                    }
                }
                
                if (nodes_control_adv[i][2]==1) advance = true;
                else    advance = false;
                switch (nodes_control_adv[i][1]) {
                    //None
                    case 1: break;
                    //Yield Sign
                    case 2: signal_control.addYieldSign(tempNode);
                            break;
                    //4-way Stop Sign
                    case 3: signal_control.add4WayStop(tempNode);
                            break;
                    //Pre-timed Control
                    case 4: if (signal_control.addPretimedControl(tempNode, advance))
                                number_of_added_signals++;
                            break;
                    //Actuated Control
                    case 5: signal_control.addActuatedControl(tempNode, advance);
                            break;
                    //2-way Stop Sign
                    case 6: signal_control.add2wayStop(tempNode);
                            break;
                    default: network.log("Error: Invalid control type for node "
                            + nodes_control_adv[i][0]);
                            return false;
                }
            }
        } catch (NumberFormatException e) {
            network.log("Error: Cannot open " + input_file);
            return false;
        }
        network.log(number_of_added_signals + " signals were added out of " + numberOfNodes);
        network.log("Done (Adding Signal Control)");
        return true;
    }
    
    public static boolean filterSignals (Network network, File input_file) throws IOException {
        //This function finds any cascaded signalized nodes and display a warning
        //with those two nodes
        SignalControl signal_control = new SignalControl(network);
        int numberOfNodes = 0;
        //Get number of nodes needed to be controlled
        try (Scanner scanner =  new Scanner(input_file).useDelimiter("\t")){
            while (scanner.hasNextLine()){
                if (!scanner.hasNextInt()) break;
                numberOfNodes++;
                scanner.nextLine();
            }
            scanner.close();
        } catch (NumberFormatException e) {
            network.log("Error: Cannot open " + input_file);
            return false;
        }
        if (numberOfNodes == 0) {
            network.log("Error: Cannot exclude data from " + input_file);
            return false;
        }
        
        int[] nodes = new int[numberOfNodes];
        
        try (Scanner scanner =  new Scanner(input_file)){
            for (int i=0; i<numberOfNodes; i++) {
                if (!scanner.hasNextInt()) {
                    network.log("Error: Invalid format: " + input_file +
                            ": line " + Integer.toString(i+1));
                    return false;
                }
                nodes[i] = scanner.nextInt();
                if (scanner.hasNextLine()) scanner.nextLine();
                else break;
            }
        } catch (NumberFormatException e) {
            network.log("Error: Cannot open " + input_file);
            return false;
        }
        
        ArrayList<Integer> connectedNodes = new ArrayList<Integer>();
        int[] connectedNodes1 = new int[signal_control.maxConnectedNodes+1];
        int[] connectedNodes2 = new int[signal_control.maxConnectedNodes+1];
        for (int i=0; i<nodes.length; i++) {
            //Find connected nodes (to) main node---------------------------
            connectedNodes1 = signal_control.findConnectedNodes(nodes[i],true);
            if (connectedNodes1[0] != -1) {
                for (int j=1; j <= connectedNodes2[0]; j++) {
                    connectedNodes.add(connectedNodes1[j]);
                }
            }
            connectedNodes2 = signal_control.findConnectedNodes(nodes[i],false);
            if (connectedNodes2[0] != -1) {
                for (int j=1; j <= connectedNodes2[0]; j++) {
                    if (connectedNodes.indexOf(connectedNodes2[j]) == -1)
                        connectedNodes.add(connectedNodes2[j]);
                }
            }
            
            for (int k=i+1; k<nodes.length; k++) {
                if (connectedNodes.indexOf(nodes[k]) != -1)
                    network.log("Two cascaded signals at nodes: "
                            + nodes[k] + "," + nodes[i]);
            }
            
            connectedNodes.clear();
        }
        network.log("Done (Filtering Signals)");    
        return true;
    }
    
    public static void initLogFile(Path fFilePath) throws IOException {
        Path file_control = Paths.get(fFilePath.toString(), "PreparingDynusTNetwork.log");
        Charset charset = Charset.forName("US-ASCII");
        try (BufferedWriter writer = Files.newBufferedWriter(file_control, charset)) {
            String s = "=======" +
                    new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss").format(new Date())
                    + "=======";
            writer.write(s, 0, s.length());
            writer.newLine();
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }
        
}
