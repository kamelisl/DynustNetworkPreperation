
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
                MainNode tempNode = new MainNode(new Node(nodes_control_adv[i][0],
                        signal_control.findNodeZone(nodes_control_adv[i][0]),
                        signal_control.findNodeCoordinates(nodes_control_adv[i][0])), network);
                Nodes[i] = tempNode;
                //Find connected nodes (to) main node---------------------------
                connectedNodes = signal_control.findConnectedNodes(Nodes[i].mainNode.label,true);
                //Don't add signals to nodes connected to freeways
                //Don't add signals to nodes connected to start of ramps
                if (connectedNodes[0] == -1)    continue;
                //Nodes[i].maxNoOfConnectedNodes = connectedNodes[0];
                //Find coordinates of connected nodes and add them to the MainNode
                for (int j=1; j <= connectedNodes[0]; j++) {
                    Nodes[i].AddConnectedNode(new Node(connectedNodes[j],
                            signal_control.findNodeZone(connectedNodes[j]),
                            signal_control.findNodeCoordinates(connectedNodes[j])),true);
                }
                //Find connected nodes (from) main node
                connectedNodes = signal_control.findConnectedNodes(Nodes[i].mainNode.label,false);
                if (connectedNodes[0] == -1)    continue;   //Don't add signals to nodes connected to freeways
                //Nodes[i].maxNoOfConnectedNodes += connectedNodes[0];
                //Find coordinates of connected nodes and add them to the MainNode
                for (int j=1; j <= connectedNodes[0]; j++) {
                    Nodes[i].AddConnectedNode(new Node(connectedNodes[j],
                            signal_control.findNodeZone(connectedNodes[j]),
                            signal_control.findNodeCoordinates(connectedNodes[j])),false);
                }
                //--------------------------------------------------------------
                Nodes[i].maxNoOfConnectedNodes = Nodes[i].numberOfConnectedNodes;
                signal_control.arrangeConnectedNodes(tempNode, true);
                signal_control.arrangeConnectedNodes(tempNode, false);
                                
                Nodes[i].numberOfConnectedNodes = ((Nodes[i].from_flag[0] || Nodes[i].to_flag[0]) ? 1:0)
                            + ((Nodes[i].from_flag[1] || Nodes[i].to_flag[1]) ? 1:0)
                            + ((Nodes[i].from_flag[2] || Nodes[i].to_flag[2]) ? 1:0)
                            + ((Nodes[i].from_flag[3] || Nodes[i].to_flag[3]) ? 1:0);
                Nodes[i].maxNoOfConnectedNodes = Nodes[i].numberOfConnectedNodes;
                
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
                    case 4: signal_control.addPretimedControl(tempNode, advance);
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
        network.log("Done (Adding Signal Control)");
        return true;
    }
    
    public static boolean filterSignals (Network network, File input_file) throws IOException {
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
