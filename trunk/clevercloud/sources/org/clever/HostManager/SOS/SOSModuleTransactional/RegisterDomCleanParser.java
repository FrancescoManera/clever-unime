/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clever.HostManager.SOS.SOSModuleTransactional;

import java.util.Vector;
import org.w3c.dom.*;

/**
 *
 * @author user
 */
public class RegisterDomCleanParser {
//all'interno del tag component, troviamo la descrizione di ogni sensore che compone la board con gli stessi campi usati nella escrizione della, procedure, della board complessiva
//allochiamo quindi due oggetti diversi:
//il primo per la descrzione della board comlessiva
    protected RegisterNodeDomParser registerNodeDomParser;
//il secondo è un vettore, di cui ogni elemento contiene la descrizione di ogni componente
    protected Vector<RegisterNodeDomParser> registerComponentDescription;

    public RegisterDomCleanParser() {
        registerNodeDomParser = new RegisterNodeDomParser();
        registerComponentDescription = new Vector<RegisterNodeDomParser>(1);
    }

    public void sensorNodeInfo(Node node) {
        // registerNodeDomParser.printNodeInfo(node);

        Vector<Node> tovisit = new Vector<Node>(1);
        Vector<Node> visited = new Vector<Node>(1);
        tovisit.add(node);

        while (tovisit.isEmpty() == false) {

            Node currentNode = tovisit.firstElement();

            if (visited.contains(currentNode) == false) {

                short sNodeType = currentNode.getNodeType();
                if (sNodeType == Node.ELEMENT_NODE) {
                    String sNodeName = currentNode.getNodeName();
                
                    //per ogni componente 



                    //se si tratta del tag che racchiude la descrizione della totalità del sensore
                    if (sNodeName.equals("sml:System")) {

                        for (int i = 0; i < currentNode.getChildNodes().getLength(); i++) {
                            if (currentNode.getChildNodes().item(i).getNodeName().equals("sml:components")) {

                                componentparser(currentNode.getChildNodes().item(i));
                                Node temp = currentNode;
                                temp.removeChild(currentNode.getChildNodes().item(i));
                                registerNodeDomParser.sensorNodeInfo(temp);
                            }
                        }



                    }
                }



                int iChildNumber = currentNode.getChildNodes().getLength();

                if (currentNode.hasChildNodes()) {
                    NodeList nlChilds = currentNode.getChildNodes();
                    for (int iChild = 0; iChild < iChildNumber; iChild++) {
                        if (nlChilds.item(iChild).getNodeType() == Node.ELEMENT_NODE) {
                            tovisit.add(nlChilds.item(iChild));
                        }
                    }

                }
                visited.add(currentNode);
                tovisit.remove(currentNode);
            }
        }




    }

    private void componentparser(Node node) {
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            if (node.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {


                for (int j = 0; j < node.getChildNodes().item(i).getChildNodes().getLength(); j++) {
                    if (node.getChildNodes().item(i).getChildNodes().item(j).getNodeType() == Node.ELEMENT_NODE) {


                        RegisterNodeDomParser componentTemp = new RegisterNodeDomParser();
                        //si effettua il parsing del singolo componente
                        componentTemp.sensorNodeInfo(node.getChildNodes().item(i).getChildNodes().item(j));
                        NamedNodeMap nnmAttributes = node.getChildNodes().item(i).getChildNodes().item(j).getAttributes();
                        //si imposta il nome del componente
                        componentTemp.sensorDescription.setDescription_type(utils.printAttributes(nnmAttributes).split("=")[1].split(";")[0]);
                        //si aggiunge l'oggetto del componente al vettore dei componenti
                        registerComponentDescription.add(componentTemp);



                    }
                }

            }

        }
    }
}