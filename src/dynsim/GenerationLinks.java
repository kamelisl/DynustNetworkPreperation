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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.ArrayList;

/**
 *
 * @author Islam
 */
public class GenerationLinks {
    public boolean findGenerationLinks(Network network) throws IOException {
        Path fFilePath = network.getPath();
        int NumberOfZones = 0;
        int NumberOfNodes = 0;
        int NumberOfLinks = 0;
        File file_network = new File(fFilePath + "\\network.dat");
        File file_destination = new File(fFilePath + "\\destination.dat");
        try (Scanner scanner_net =  new Scanner(file_network)){
            //Get # zones
            NumberOfZones = scanner_net.nextInt();
            //Get # nodes
            NumberOfNodes = scanner_net.nextInt();
            //Get # links
            NumberOfLinks = scanner_net.nextInt();
        } catch (IOException x) {
            network.log("IOException: " + x);
            return false;
        }
        
        
            //Opening origin.dat for writing
            Path file_origin = Paths.get(fFilePath.toString(), "origin.dat");
            String s;
            Charset charset = Charset.forName("US-ASCII");  //"UTF-8"
            try (BufferedWriter writer = Files.newBufferedWriter(file_origin, charset)) {
                //for all zones in network
                for (int i=0; i<network.getNoOfZones(); i++) {
                    //Zone ID
                    int zone = network.zones[i].getID();
                    int noOfLinksInZone = 0;
                    ArrayList<Integer> fromNode = new ArrayList<Integer>();
                    ArrayList<Integer> toNode = new ArrayList<Integer>();
                    //for all nodes in this zone
                    for (int j=0; j<network.zones[i].noOfNodes; j++) {
                        int node = network.zones[i].nodes.get(j);
                        //Search for arterial links coming from this node
                        try (Scanner scanner_net =  new Scanner(file_network)){
                            scanner_net.nextLine();
                            //Skip the first section in netwrork.dat
                            for (int k=0; k<NumberOfNodes; k++)
                                scanner_net.nextLine();
                            do {
                                int tempFromNode = scanner_net.nextInt();
                                //Check if current node is "start" of arterial link
                                if (node < tempFromNode) break; //Assuming written in order
                                else if (node == tempFromNode) {
                                    int tempNode = scanner_net.nextInt();
                                    //Check if end node lies within the same zone
                                    if (network.zones[i].nodes.indexOf(tempNode) != -1) {
                                        scanner_net.nextInt();
                                        scanner_net.nextInt();
                                        scanner_net.nextInt();
                                        scanner_net.nextInt();
                                        scanner_net.nextInt();
                                        scanner_net.nextInt();
                                        scanner_net.nextInt();
                                        scanner_net.nextInt();
                                        scanner_net.nextInt();
                                        int type = scanner_net.nextInt();
                                        if (type == 5 || type == 7) {
                                            fromNode.add(node);
                                            toNode.add(tempNode);
                                            noOfLinksInZone++;
                                        }
                                    }
                                }/* else if ((node == scanner_net.nextInt()) && 
                                        (network.zones[i].nodes.indexOf(tempFromNode) == -1)) {
                                    //Check if it is "end" of a link & start of this link is outside the zone
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    if (scanner_net.nextInt() == 5) {
                                        fromNode.add(tempFromNode);
                                        toNode.add(node);
                                        noOfLinksInZone++;
                                    }
                                }*/
                                scanner_net.nextLine();
                            } while (scanner_net.hasNextLine());
                        } catch (IOException x) {
                            network.log("IOException: " + x);
                            return false;
                        }
                    }
                    
                    //Remove dead-end links
                    for (int j=0; j<network.zones[i].noOfNodes; j++) {
                        int node = network.zones[i].nodes.get(j);
                        int temp = isGenerationLink(node, -1, network, fromNode, toNode);
                        if (temp == -1) {
                            network.log("Error: Cannot find generation links of zone " + network.zones[i].getID());
                            return false;
                        }
                        noOfLinksInZone -= temp;
                    }
                    /*----------------------------------------------------*/
                    //For zones with zero generation links, search for an arterial
                    //link going to any node regardless of destination's zone number
                    //In this case: ONLY ONE LINK WILL BE ADDED AS A GENERATION LINK
                    for (int j=0; j<network.zones[i].noOfNodes && noOfLinksInZone==0; j++) {
                        int node = network.zones[i].nodes.get(j);
                        //Search for arterial links coming from this node
                        try (Scanner scanner_net =  new Scanner(file_network)){
                            scanner_net.nextLine();
                            //Skip the first section in netwrork.dat
                            for (int k=0; k<NumberOfNodes; k++)
                                scanner_net.nextLine();
                            do {
                                int tempFromNode = scanner_net.nextInt();
                                //Check if current node is "start" of arterial link
                                if (node == tempFromNode) {
                                    int tempNode = scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    scanner_net.nextInt();
                                    if (scanner_net.nextInt() == 5) {
                                        fromNode.add(node);
                                        toNode.add(tempNode);
                                        noOfLinksInZone++;
                                        break;
                                    }
                                }
                                scanner_net.nextLine();
                            } while (scanner_net.hasNextLine());
                        } catch (IOException x) {
                            network.log("IOException: " + x);
                            return false;
                        }
                    }
                    /*----------------------------------------------------*/
                    //Write generation links in origin.dat
                    if (noOfLinksInZone==0)
                        network.log("Warning: Zone " + zone + " does not have generation links");
                    s = "\t" + zone + "\t" + noOfLinksInZone + "\t0";
                    writer.write(s, 0, s.length());
                    writer.newLine();
                    for (int j=0; j<noOfLinksInZone; j++) {
                        s = "\t" + fromNode.get(j) + "\t" + toNode.get(j) + "\t0.000";
                        writer.write(s, 0, s.length());
                        writer.newLine();
                    }
                }
            } catch (IOException x) {
                network.log("IOException: " + x);
                return false;
            }
        
        network.log("Done (Adding Generation Links)");
        return true;
    }
    
    public int isGenerationLink(int node, int last_node, Network network,
            ArrayList<Integer> fromNode, ArrayList<Integer> toNode) throws IOException {
        int returned_value = 0;
        int from_index = fromNode.indexOf(node);
        int to_index = toNode.indexOf(node);
                        if (last_node == 500149)
                            network.log("Last Node 500149: " + to_index);
                        if (node == 420253)
                            network.log("Node 420253: " + from_index);
        if ((last_node != -1) && (to_index!=-1)) {
            int size = toNode.size();
            for (int i=to_index; i!=-1; i+= (toNode.subList(i+1, size).indexOf(node)+1)) {
                        if (last_node == 500149)
                            network.log("Last Node 500149: i=" + i);
                int temp = fromNode.get(i);
                        if (node == 420253)
                            network.log("Node 420253: temp=" + temp);
                if (temp==last_node) {
                            network.log("Inside IF: temp=" + temp + ", node=" + node);
                    toNode.set(i, -node);
                    to_index = toNode.indexOf(node);
                    break;
                }
                if (i==size-1)  break;
                if (toNode.subList(i+1, size).indexOf(node)==-1)    break;
            }
        }
        

        //Case of one or zero generation-link from this node
        if (from_index==fromNode.lastIndexOf(node)) {
            /*----------------------------------------------------*/
            //If the node has other outgoing links rather than
            // the generation links that lie in the same zone
            int counter = 0;    /*total number of all outgoing
                                links from this node*/
            File file_network = new File(network.getPath() + "\\network.dat");
            try (Scanner scanner_net =  new Scanner(file_network)){
                scanner_net.nextLine();
                //Skip the first section in netwrork.dat
                for (int k=0; k<network.getNoOfNodes(); k++)
                    scanner_net.nextLine();
                do {
                    if ((node == scanner_net.nextInt()) && (last_node != scanner_net.nextInt())) {
                        //Don't count the link to "last_node" as it is a dead-end link
                        counter++;
                        if (counter > 1)    break;
                    }
                    scanner_net.nextLine();
                } while (scanner_net.hasNextLine());
            } catch (IOException x) {
                network.log("IOException: " + x);
                return -1;
            }

            /*----------------------------------------------------*/
            //Case of exactly one (not zero) link to this node
            if ((to_index==toNode.lastIndexOf(node)) && (to_index!=-1) && counter < 2) {
                /*Remove the dead end link if this node has only
                one bidirectional link or has only one coming link
                without outgoing one*/
                if (from_index==-1) {
                    if (counter==0) {
                        //one coming link
                        int from_temp = fromNode.get(to_index);
                        fromNode.remove(to_index);
                        toNode.remove(to_index);
                        //Check for previous node and link
                        int temp = isGenerationLink(from_temp, node, network, fromNode, toNode);
                        if (temp == -1)
                            return -1;
                        returned_value += temp;
                        returned_value++;
                    }
                } else { 
                    int from_temp = fromNode.get(to_index);
                    int to_temp = toNode.get(from_index);
                    if (from_temp==to_temp) {
                        //one bidirectional link
                        fromNode.remove(to_index);
                        toNode.remove(to_index);
                        //Check for previous node and link
                        int temp = isGenerationLink(from_temp, node, network, fromNode, toNode);
                        if (temp == -1)
                            return -1;
                        returned_value += temp; 
                        returned_value++;
                    }
                }
            } else if ((to_index!=-1) && counter < 2) {
                //Case of exactly two links to this node
                /*Remove the dead end link if this node has only
                one bidirectional link + one coming link or has only two coming links
                without outgoing one*/
                if (to_index<toNode.size()-1) {
                    if (toNode.subList(to_index+1, toNode.size()).indexOf(node)==toNode.lastIndexOf(node)) {
                        if (from_index==-1) {
                            if (counter==0) {
                                //two coming links
                                int from_temp = fromNode.get(to_index);
                                fromNode.remove(to_index);
                                toNode.remove(to_index);
                                //Check for previous node and link
                                int temp = isGenerationLink(from_temp, node, network, fromNode, toNode);
                                if (temp == -1)
                                    return -1;
                                returned_value += temp;
                                returned_value++;
                                
                                from_temp = fromNode.get(toNode.lastIndexOf(node));
                                fromNode.remove(to_index);
                                toNode.remove(to_index);
                                //Check for previous node and link
                                temp = isGenerationLink(from_temp, node, network, fromNode, toNode);
                                if (temp == -1)
                                    return -1;
                                returned_value += temp;
                                returned_value++;
                            }
                        } else { 
                            int from_temp = fromNode.get(to_index);
                            int from_temp_last = fromNode.get(toNode.lastIndexOf(node));
                            int to_temp = toNode.get(from_index);
                            if (from_temp==to_temp) {
                                //one bidirectional link
                                fromNode.remove(to_index);
                                toNode.remove(to_index);
                                //Check for previous node and link
                                int temp = isGenerationLink(from_temp, node, network, fromNode, toNode);
                                if (temp == -1)
                                    return -1;
                                returned_value += temp; 
                                returned_value++;
                                
                                //one outgoing link
                                fromNode.remove(toNode.lastIndexOf(node));
                                toNode.remove(toNode.lastIndexOf(node));
                                //Check for previous node and link
                                temp = isGenerationLink(from_temp, node, network, fromNode, toNode);
                                if (temp == -1)
                                    return -1;
                                returned_value += temp; 
                                returned_value++;
                            } else if (from_temp_last==to_temp) {
                                //one outgoing link
                                fromNode.remove(to_index);
                                toNode.remove(to_index);
                                //Check for previous node and link
                                int temp = isGenerationLink(from_temp, node, network, fromNode, toNode);
                                if (temp == -1)
                                    return -1;
                                returned_value += temp; 
                                returned_value++;
                                
                                //one bidirectional link
                                fromNode.remove(toNode.lastIndexOf(node));
                                toNode.remove(toNode.lastIndexOf(node));
                                //Check for previous node and link
                                temp = isGenerationLink(from_temp, node, network, fromNode, toNode);
                                if (temp == -1)
                                    return -1;
                                returned_value += temp; 
                                returned_value++;
                            }
                        }
                    }
                }
            }
        }
        
        int last_node_index = toNode.indexOf(-node);
        if (last_node_index != -1)
            toNode.set(last_node_index, node);
        return returned_value;
    }
}
