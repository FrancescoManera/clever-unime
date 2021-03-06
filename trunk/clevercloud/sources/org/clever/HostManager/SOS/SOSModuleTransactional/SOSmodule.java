/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clever.HostManager.SOS.SOSModuleTransactional;

import org.clever.HostManager.SOS.SOSModuleTransactional.Readers.ReaderInterface;
import java.io.ByteArrayInputStream;
import java.io.File;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

import java.util.HashMap;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.clever.Common.Communicator.Notification;
import org.clever.HostManager.SOS.ParameterContainer;
import org.clever.HostManager.SOS.SOSAgent;
import org.xml.sax.SAXException;

/**
 *
 * @author user
 */
public class SOSmodule implements SOSforReaderInterface {

    private Logger logger;
    private String service;
    private ReaderInterface usb_reader,test_reader;
    private HashMap mapReaders;
    private ParameterContainer parameterContainer;

    public SOSmodule() {
        parameterContainer = ParameterContainer.getInstance();
        logger = parameterContainer.getLogger();
    }

    public void init() {
        try {
            this.service = "";
            logger.debug("SOSmodule init");
            String filename = parameterContainer.getConfigurationFile();
            this.mapReaders = new HashMap(1);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            File xmlFilereg = new File(filename);
            Document document = builder.parse(xmlFilereg);
            parseConfiguration(document);
            
        } catch (SAXException ex) {
            logger.error("SOSModuleTransactional: SAXException: " + ex);
        } catch (IOException ex) {
            logger.error("SOSModuleTransactional: IOException: " + ex);
        } catch (ParserConfigurationException ex) {
            logger.error("SOSModuleTransactional: ParserConfigurationException: " + ex);
        }


    }

    public void SOSservice(String request) {
        try {
            SOSserviceSelection(request);
        } catch (ParserConfigurationException ex) {
            logger.error("SOSModuleTransactional: ParserConfigurationException: " + ex);
        } catch (SAXException ex) {
            logger.error("SOSModuleTransactional: SAXException: " + ex);
        } catch (IOException ex) {
            logger.error("SOSModuleTransactional: IOException: " + ex);
        } catch (TransformerConfigurationException ex) {
            logger.error("SOSModuleTransactional: TransformerConfigurationException: " + ex);
        } catch (TransformerException ex) {
            logger.error("SOSModuleTransactional: TransformerException: " + ex);
        } catch (SQLException ex) {
            logger.error("SOSModuleTransactional: SQLException: " + ex);
        } catch (ParseException ex) {
            logger.error("SOSModuleTransactional: ParseException: " + ex);
        }
    }
    /*public void SOSservice(String filename_input,String filename_output) {
    try {
    SOSserviceSelection(filename_input, filename_output);
    } catch (ParserConfigurationException ex) {
    logger.error("ParserConfigurationException: "+ex);
    } catch (SAXException ex) {
    logger.error("SAXException: "+ex);
    } catch (IOException ex) {
    logger.error("IOException: "+ex);            
    } catch (TransformerConfigurationException ex) {
    logger.error("TransformerConfigurationException: "+ex);
    } catch (TransformerException ex) {
    logger.error("TransformerException: "+ex);
    } catch (SQLException ex) {
    logger.error("SQLException: "+ex);
    } catch (ParseException ex) {
    logger.error("ParseException: "+ex);            
    }
    }*/

    public void SOSserviceSelection(String request) throws ParserConfigurationException, SAXException, IOException, TransformerConfigurationException, TransformerException, SQLException, ParseException {

        //logger.debug("SOSserviceSelection");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        //File xmlFilereg = new File(filename_input);
        Document documentreg = builder.parse(new ByteArrayInputStream(request.getBytes()));
        //  controllo il nodo radice del file di input, in cui è decritto il tipo di operazione invocata
        int iChildNumber = documentreg.getChildNodes().getLength();
        //Se non si tratta di una foglia continua l'esplorazione
        if (documentreg.hasChildNodes()) {
            NodeList nlChilds = documentreg.getChildNodes();
            for (int iChild = 0; iChild < iChildNumber; iChild++) {
                this.service = (nlChilds.item(iChild).getNodeName());
            }
        }
        //controllo il servizio e alloco gli oggetti corrispondenti all'operazione desiderata

        if (this.service.contains("RegisterSensor")) {
            SOSAgent sosAgent = this.parameterContainer.getSosAgent();
            sosAgent.registerRequest(request);
            RegisterSensor rg = new RegisterSensor(request);
            rg.insertSensor();
            rg.write_register_xml();

        } else if (this.service.contains("InsertObservation")) {
           // logger.info("InsertObservation");
            InsertObservation obs = new InsertObservation(request);
            obs.insertObsdb();
        } else {
            System.out.println("funzione non riconosciuta");
        }
    }

        public void parseConfiguration(Node currentNode) {
        short sNodeType = currentNode.getNodeType();
        
        //Se è di tipo Element ricavo le informazioni e le stampo
        if (sNodeType == Node.ELEMENT_NODE) {
            String sNodeName = currentNode.getNodeName();
            String sNodeValue = utils.searchTextInElement(currentNode);
            //per ogni componente 
          //  logger.info("SOS NodeName="+sNodeName);
            if (sNodeName.equals("reader")) {
                parseReader(currentNode);
            }
        }
        int iChildNumber = currentNode.getChildNodes().getLength();
        //Se non si tratta di una foglia continua l'esplorazione
        if (currentNode.hasChildNodes()) {
            NodeList nlChilds = currentNode.getChildNodes();
            for (int iChild = 0; iChild < iChildNumber; iChild++) {
                parseConfiguration(nlChilds.item(iChild));
            }
        }
    }

    public void parseReader(Node currentNode) {
        short sNodeType = currentNode.getNodeType();
       // logger.info("parseReader sNodetype="+sNodeType+" and Node.Elem="+Node.ELEMENT_NODE);
        if (sNodeType == Node.ELEMENT_NODE) {
            String sNodeName = currentNode.getNodeName();
            String sNodeValue = utils.searchTextInElement(currentNode);
          //  logger.info("sNodeName="+sNodeName+" and sNodeValue="+sNodeValue);
            if (sNodeName.equals("className")) {
                //if(sNodeValue.contains("Usb_reader")){
                try {
                //    logger.info("Reader="+sNodeValue);
                    Class cl = Class.forName(sNodeValue);

                    usb_reader = (ReaderInterface) cl.newInstance();
                    while (true) {
                        currentNode = currentNode.getNextSibling();
                        if (currentNode.getNodeName().equals("moduleName")) {
                            mapReaders.put(usb_reader, sNodeValue);
                           // logger.info("mapReaders put done!");
                        }
                        if (currentNode.getNodeName().equals("pluginParams")) {
                            usb_reader.init(currentNode, this);
                          //  logger.info("init reader done!");
                            break;
                        }
                    }
                } catch (InstantiationException ex) {
                    logger.error("SOSModuleTransactional: InstantiationException: " + ex);
                } catch (IllegalAccessException ex) {
                    logger.error("SOSModuleTransactional: IllegalAccessException: " + ex);
                } catch (ClassNotFoundException ex) {
                    logger.error("SOSModuleTransactional: ClassNotFoundException: " + ex);
                }
//                }else if(sNodeValue.contains("Test_Reader")){
//                    logger.info("SOSmodule Test_reader ->"+sNodeValue);
//                    try {
//                    Class cl = Class.forName(sNodeValue);
//
//                    test_reader = (ReaderInterface) cl.newInstance();
//                    while (true) {
//                        currentNode = currentNode.getNextSibling();
//                        if (currentNode.getNodeName().equals("moduleName")) {
//                            mapReaders.put(test_reader, sNodeValue);
//                        }
//                        if (currentNode.getNodeName().equals("pluginParams")) {
//                            test_reader.init(currentNode, this);
//                            break;
//                        }
//                    }
//                } catch (InstantiationException ex) {
//                    logger.error("SOSModuleTransactional: InstantiationException: " + ex);
//                } catch (IllegalAccessException ex) {
//                    logger.error("SOSModuleTransactional: IllegalAccessException: " + ex);
//                } catch (ClassNotFoundException ex) {
//                    logger.error("SOSModuleTransactional: ClassNotFoundException: " + ex);
//                }
                    
                    
                //}
            }
        }
        int iChildNumber = currentNode.getChildNodes().getLength();
        //Se non si tratta di una foglia continua l'esplorazione
        if (currentNode.hasChildNodes()) {
            NodeList nlChilds = currentNode.getChildNodes();
            for (int iChild = 0; iChild < iChildNumber; iChild++) {
                parseReader(nlChilds.item(iChild));
            }
        }

    }
}