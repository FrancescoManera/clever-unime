/*
 * The MIT License
 *
 * Copyright 2012 Francesco Antonino Manera.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.clever.ClusterManager.WebAgent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.clever.Common.Communicator.CmAgent;
import org.clever.Common.Communicator.ModuleCommunicator;
import org.clever.Common.Communicator.Notification;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMLTools.FileStreamer;
import org.clever.Common.XMLTools.ParserXML;
import org.clever.HostManager.SAS.ParameterContainer;
import org.clever.HostManager.SOS.SOSModuleTransactional.utils;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class WebAgent extends CmAgent {
     private FileStreamer fs = new FileStreamer();
     private Database db;
     private ParameterDbContainer parameterContainer;
     private Document doc;
     private ParameterSensor parameterSensor;
     private String sednaPrefix;
     
    
    public WebAgent(){
     try {
        super.setAgentName("WebAgent");
        logger=logger.getLogger(this.getAgentName());
        logger.info("WebAgent Avviato");
        
        mc=new ModuleCommunicator(this.getAgentName(),"CM");
        mc.setMethodInvokerHandler(this);
        
        InputStream inxml = getClass().getResourceAsStream( "/org/clever/ClusterManager/WebAgent/configuration_web.xml" );
        ParserXML pXML = new ParserXML( fs.xmlToString( inxml ) );
        
        
            parameterContainer=ParameterDbContainer.getInstance();
            Element dbParams=pXML.getRootElement().getChild("dbParams");
            parameterContainer.setDbDriver(dbParams.getChildText("driver"));
            parameterContainer.setDbServer(dbParams.getChildText("server"));
            parameterContainer.setDbPassword(dbParams.getChildText("password"));
            parameterContainer.setDbUsername(dbParams.getChildText("username"));
            parameterContainer.setDbName(dbParams.getChildText("name"));
           
            //get agent db prefix
            ArrayList params = new ArrayList();
            params.add("SASAgent");
            try {
            this.sednaPrefix = (String) this.invoke("DatabaseManagerAgent", "getAgentPrefix", true, params);
            } catch (CleverException ex) {
            logger.error("Init error: " + ex);
            }

            logger.debug("?=) sednaPrefix is: "+this.sednaPrefix);
            
           
        
     
     
     
     
     } 
//        catch (CleverException ex) {
//            java.util.logging.Logger.getLogger(WebAgent.class.getName()).log(Level.SEVERE, null, ex);
//        } 
        catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(WebAgent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(WebAgent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(WebAgent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WebAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
       
   
    
    
    }
    
    
    public void checkCapabilities(){
        String SoId = null;
        String Alertfreq= null;
       
        
        try {
            String subscr = "/capabilities/Contents/SubscriptionOfferingList";
            String qresult=this.query(subscr);
            logger.info("?=) query result:\n"+qresult);
            db=Database.getNewInstance(this.parameterContainer);
            logger.info("DB: "+this.parameterContainer.getDbName());
            org.jdom.Document docSol = this.stringToDom(qresult);
            Element getCapabilitiesElement = docSol.detachRootElement();
            
            
            List so = getCapabilitiesElement.getChildren();
            Iterator soiterator = so.iterator();
            while (soiterator.hasNext()) {
                
            
            Element sectionsElement = (Element) soiterator.next();//getCapabilitiesElement.getChild("SubscriptionOffering");
            if (sectionsElement != null) {
               List children = sectionsElement.getChildren();
               Iterator iterator = children.iterator();
               Element element = null; 
               String parameterName = "";
               String section = "";
               String definition=null;
               String uom=null;
               String name=null;
               String desc=null;
               int idprop = -1;
               int idalert=-1;
            while (iterator.hasNext()) {
//                try {
                
                    element = (Element) iterator.next();
                    
                        
                    if(element.getName().equalsIgnoreCase("AlertMessageStructure")){
                     definition = element.getChild("QuantityProperty").getChild("Content").getAttributeValue("definition");
                     uom=element.getChild("QuantityProperty").getChild("Content").getAttributeValue("uom");    
                     
                     //inserisco nel db questa proprietà
                     //prima verifico se già esiste
                     String verifyprop="SELECT `ID` FROM `ProprietaSottoscrizione` WHERE `Nome`='AlertMessageStructure' AND `Descrizione`='"+definition+"' AND `Uom`='"+uom+"'";
                     ResultSet rprop = db.exQuery(verifyprop);
                     if (rprop.next()){
                         idprop=rprop.getInt(1);
                     }else{
                     
                     String prop="INSERT INTO `CleverResources`.`ProprietaSottoscrizione` (`ID`, `Nome`, `Tipo`, `Valore`, `Uom`, `Descrizione`) VALUES (NULL, 'AlertMessageStructure', 'Capabilities',NULL, '"+uom+"','"+definition+"');";
                     logger.info("?=) query: "+prop);
                     idprop = db.exInsert(prop);
                     //db.exUpdate(prop);
                     }
                     logger.info("prop id: "+idprop);
                     
                     
                     
                    }else if (element.getName().equalsIgnoreCase("FeatureOfInterest")){
                         name=element.getChild("Name").getText();
                        desc=element.getChild("Description").getText();
                        logger.info("name: "+name+" description: "+desc);
                    }
//                    else if(element.getName().equalsIgnoreCase("OperationArea")){
//                       // Element child = element.getChild("GeoLocation");
//                                logger.info("?=) Operation Area ");
//                                if (element.getChild("swe:GeoLocation").getChild("gml:boundedBy")!=null){
//                                    logger.info("?=) boundedBy");
//                                }
//                                else if (element.getChild("swe:GeoLocation").getChild("swe:longitude")!=null){
//                                    logger.debug("?=) coord");
//                                    String longit= element.getChild("swe:GeoLocation").getChild("swe:longitude").getChildText("Quantity");
//                                    String latit=element.getChild("swe:GeoLocation").getChild("swe:latitude").getChildText("Quantity");
//                                    String altitu=element.getChild("swe:GeoLocation").getChild("swe:altitude").getChildText("Quantity");
//                                    logger.info("?=) long= "+longit+" latitu= "+latit+" altitu= "+altitu);
//                                    
//                                    
//                                }
//                                else{
//                                    logger.debug("?=) else ");
//                                    
//                                    
//                                }
                    
//                    }
                    
                    
                    
                    else{
                    
                        
                    
                        String elementName=element.getName();
                        parameterName = element.getText();
                        
                        if (elementName.equalsIgnoreCase("SubscriptionOfferingID"))
                                SoId=parameterName;
                            
                        if (elementName.equalsIgnoreCase("AlertFrequency")){
                                Alertfreq=parameterName;
                                //prima verifico se già esiste
                                String verifyal="SELECT `ID` FROM `ProprietaSottoscrizione` WHERE `Nome`='AlertFrequency' AND `Tipo`='Capabilities' AND `Valore`='"+Alertfreq+"' AND `Uom`='Hz'";
                                ResultSet ral = db.exQuery(verifyal);
                                if (ral.next()){
                                    idalert=ral.getInt(1);
                                }else{
                                String alertquery="INSERT INTO `CleverResources`.`ProprietaSottoscrizione` (`ID`, `Nome`, `Tipo`, `Valore`, `Uom`, `Descrizione`) VALUES (NULL, 'AlertFrequency', 'Capabilities', '"+Alertfreq+"', 'Hz', '');";
                                idalert = db.exInsert(alertquery);
                                }
                        }
                        //logger.info("?=) element: "+element.getName()+" parameterName: "+parameterName);
                        
                    }
                    
                    
                    
                    
                    
                    
                    //section = this.query("/capabilities/" + parameterName);
                    
                    
//                } 
//                catch (CleverException ex) {
//                    logger.error("Error getting capabilities: " + ex);
//                }


            }
             //inserisco nel db queste info
            //verifico se è gia presente 
            String verysoi="SELECT * FROM `SottoscrizioneOfferta` WHERE `SoId`='"+SoId+"'";
            ResultSet rvsoi = db.exQuery(verysoi);
            if(rvsoi.next()){
                logger.info("?=) SubscriptionOfferingId presente: "+rvsoi.getInt(1));
            }else{
            
            String querySoi="INSERT INTO `CleverResources`.`SottoscrizioneOfferta` (`ID`, `SoId`, `Nome`, `Descrizione`) VALUES (NULL, '"+SoId+"', '"+name+"', '"+desc+"');";
            int idSoi=db.exInsert(querySoi);
            logger.info("?=) return id: "+idSoi);
            
            String querycar="INSERT INTO `CleverResources`.`CaratteristicheSottoscrizione` (`SottoscrizioneOff`, `ProprietaSottoscrizione`) VALUES ('"+idSoi+"', '"+idprop+"');";
            db.exUpdate(querycar);
            
            String querycar2="INSERT INTO `CleverResources`.`CaratteristicheSottoscrizione` (`SottoscrizioneOff`, `ProprietaSottoscrizione`) VALUES ('"+idSoi+"', '"+idalert+"');";
            db.exUpdate(querycar2);
                
            }
            
            
            
            
            
            
            
            
            }
            
            } 
            
            
            
            
            
            
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(WebAgent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (CleverException ex) {
            logger.error(ex);
        }
       
       
        
        
    }
    
    private void checkAdvertisement(){
        try {
           
            String checkAd = "/advertisements";
            String rAd=this.query(checkAd);
            
            logger.info("?=) Advertisements result:\n"+rAd);
        
            db=Database.getNewInstance(this.parameterContainer);
            logger.info("DB: "+this.parameterContainer.getDbName());
            org.jdom.Document docAdv = this.stringToDom(rAd);
            Element getCapabilitiesElement = docAdv.detachRootElement();
            
            List adv = getCapabilitiesElement.getChildren();
            Iterator adviterator = adv.iterator();
            String idAdv=null;
            String nAdv=null;
            String sAdv=null;
            String dAdv=null;
            String vAdv=null;
            while (adviterator.hasNext()) {
            
            Element sectionsElement = (Element) adviterator.next();
            if (sectionsElement != null) {
                idAdv=sectionsElement.getAttributeValue("pubId");
                dAdv=sectionsElement.getAttributeValue("hm");
                sAdv=sectionsElement.getChildText("SubscriptionOfferingID");
                nAdv=sectionsElement.getChildText("RequestID");
                vAdv=sectionsElement.getChildText("DesiredPublicationExpiration");
                
                logger.info("Advertisements of so "+sAdv+" :\n"+idAdv+" "+dAdv+" "+nAdv+" "+vAdv);
                 
                 int idsub=0;
                //ottengo l'id della sottoscrizione offerta
                 String idsoi="SELECT `ID` FROM `SottoscrizioneOfferta` WHERE `SoId`='"+sAdv+"'";
                 ResultSet rs = db.exQuery(idsoi);
                 if (rs.next()){
                 idsub=rs.getInt(1);
                 
                 }
                
                
                
                //verifico se la prop esiste già
                String verprop="SELECT ID FROM `ProprietaSottoscrizione` WHERE `Nome`='Publication' AND `Tipo`='Advertise' AND `Valore`='"+idAdv+"' AND `Descrizione`='"+dAdv+"'";
                ResultSet rprop = db.exQuery(verprop);
                if (rprop.next()){
                    logger.info("?=) Advertise gia esistente id: "+rprop.getInt(1));
                    
                }else{
                 //inserisco la proprietà   
                 String insp ="INSERT INTO `CleverResources`.`ProprietaSottoscrizione` (`ID`, `Nome`, `Tipo`, `Valore`, `Uom`, `Descrizione`) VALUES (NULL, 'Publication', 'Advertise', '"+idAdv+"', NULL, '"+dAdv+"');";
                 int idprop = db.exInsert(insp);
                   
                 
                 if (idsub!=0){
                 
                 //collego la proprieta con la sottoscrizione offerta
                 String carp="INSERT INTO `CleverResources`.`CaratteristicheSottoscrizione` (`SottoscrizioneOff`, `ProprietaSottoscrizione`) VALUES ('"+idsub+"', '"+idprop+"');";
                 db.exUpdate(carp);  
                 }
                }
                 //seconda prop
                 
                 //verifico se la prop esiste già
                String verprop2="SELECT ID FROM `ProprietaSottoscrizione` WHERE `Nome`='Expiration' AND `Tipo`='Advertise' AND `Valore`='"+vAdv+"' AND `Descrizione`='"+nAdv+"'";
                ResultSet rprop2 = db.exQuery(verprop2);
                if (rprop2.next()){
                    logger.info("?=) Advertise gia esistente id: "+rprop2.getInt(1));
                    
                }else{
                 //inserisco la proprietà   
                 String insp2 ="INSERT INTO `CleverResources`.`ProprietaSottoscrizione` (`ID`, `Nome`, `Tipo`, `Valore`, `Uom`, `Descrizione`) VALUES (NULL, 'Expiration', 'Advertise', '"+vAdv+"', NULL, '"+nAdv+"');";
                 int idprop2 = db.exInsert(insp2);
                  
                 if (idsub!=0){
                 
                 //collego la proprieta con la sottoscrizione offerta
                 String carp2="INSERT INTO `CleverResources`.`CaratteristicheSottoscrizione` (`SottoscrizioneOff`, `ProprietaSottoscrizione`) VALUES ('"+idsub+"', '"+idprop2+"');";
                 db.exUpdate(carp2);  
                 }
                 
                 
                 
                
                 }
                 
                 
                 
                 
                 
                 
                 
                 
                 
                 
                 
                 
                 logger.info("inserito un nuovo advertise");
                 
                                
                
                
                
            }  
                         
                
                
                
            }
            
            
            
            
            
        
        
        
        
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(WebAgent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (CleverException ex) {
            logger.error("CleverException"+ex);
        }
        
        
        
    }
    
    
    private void checksub(){
        try {
            String checksb = "/subscriptions";
            String rsb=this.query(checksb);
                   
            logger.info("?=) Subscriptions result:\n"+rsb);
        
        
        
        
        
        
        
        
        
        } catch (CleverException ex) {
            java.util.logging.Logger.getLogger(WebAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
             
    }
    
    
    
    
    private String query(String location) throws CleverException {
        String result = "";
        try {
            //init hashtable
            List<String> params = new ArrayList();
            logger.debug("Agent Name= WebAgent");
            params.add("SASAgent");
            params.add(location);
            result = (String) this.invoke("DatabaseManagerAgent", "query", true, params);
        } catch (CleverException ex) {
            logger.error("Query error: " + ex);
            throw new CleverException("Query error " + ex);
        }
        return result;
    }
    
    
    @Override
    public void initialization() throws Exception {
        checkCapabilities();
        checkAdvertisement();
        checksub();
        logger.info("intialization done");
    }

    @Override
    public Class getPluginClass() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getPlugin() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void shutDown() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void handleNotification(Notification notification) throws CleverException {
        String notificationType = notification.getId();
        logger.debug("received notification "+notificationType);
//        if (notificationType.equals("Web/RegisterSensor")) {
//            this.handleRegisterSensor(notification);
//        }
//        if (notificationType.equals("Web/RegisterClient")) {
//            this.handleRegisterClient(notification);
//        }
        
    }
    
    private org.jdom.Document stringToDom(String xmlSource) {
        StringReader stringReader = new StringReader(xmlSource);

        SAXBuilder builder = new SAXBuilder();
        org.jdom.Document doc = null;
        try {
            doc = builder.build(stringReader);
        } catch (JDOMException ex) {
            logger.error("JDOM exception: " + ex);
        } catch (IOException ex) {
            logger.error("IOException: " + ex);
        }
        return doc;

    }
    
   
    
    
    

//    private void handleRegisterSensor(Notification notification){
//        try {
//            String request=(String)notification.getBody();
//            String user=notification.getHostId()+"/"+notification.getAgentId();
//            
//                        
//            logger.info("received resisterSensor:\n"+request);
//             
//             // controllare che il sensore non sia gia presente nel database
//            // inserire il nuovo sensore in CleverResource
//            
//            DocumentBuilderFactory f =DocumentBuilderFactory.newInstance ();
//           
//            f.setNamespaceAware (true);
//            DocumentBuilder d;
//            try {
//            d = f.newDocumentBuilder ();
//            
//            doc = d.parse(new ByteArrayInputStream(request.getBytes()));
//            
//            }           
//            catch (SAXException ex) {
//                logger.error("SAXException"+ex);
//            }           
//            catch (ParserConfigurationException ex) {
//                logger.error("ParserConfigurationException"+ex);
//            } 
//            catch (IOException ioe) { logger.error("IOException"+ioe); }
//             // parsing del doc per ottenere tutti i parametri
//             parameterSensor=ParameterSensor.getInstance();
//             parameterSensor.parseinput(doc);
//             parameterSensor.setParameter();
//             
//             
//             //inserisco nel db il sensore
//             db=Database.getNewInstance();
//             
//             
//             //cerco l'id del client
//            int idclient=-1;
//            String verifyclient="SELECT `ID` FROM `Client` WHERE `Username` ='"+user+"'";
//            ResultSet rsclient = db.exQuery(verifyclient);
//            if (rsclient.next()){
//                idclient = rsclient.getInt(1);
//                logger.info("?=) client gia presente nel db id:"+idclient);
//            }
//             
//             
//             
//             
//             //verifico la presenza del sensore nel db
//             String verify="SELECT * FROM `Sensore` WHERE `MAC`='"+ParameterSensor.getMAC()+"'";
//             ResultSet rsVerify = db.exQuery(verify);
//             if(!rsVerify.next()){
//             logger.info("?=) new Sensor!");
//             
//             //inserisco le coordinate
//             String querycoord ="INSERT INTO `CleverResources`.`Coordinata` (`ID`, `Latitudine`, `Longitudine`, `Altitudine`, `Descrizione`) VALUES (NULL, '"+ParameterSensor.getCoordLatitude()+"', '"+ParameterSensor.getCoordLongitude()+"', '"+ParameterSensor.getCoordAltitude()+"', '');";
//             int idCoord = db.exInsert(querycoord);
//             logger.info("?=) return id: "+idCoord);
//             
//             //inserisco il sensore STABILIRE IL PROPRIETARIO
//             String querysensor="INSERT INTO `CleverResources`.`Sensore` (`ID`, `Nome`, `MAC`, `Descrizione`, `Coordinata`, `Proprietario`) VALUES (NULL, '"+ParameterSensor.getNome()+"', '"+ParameterSensor.getMAC()+"', '"+ParameterSensor.getDescrizione()+"' , '"+idCoord+"', '"+idclient+"');";
//             int idSensor=db.exInsert(querysensor);
//             logger.info("?=) return id: "+idSensor);
//             
//             
//                        
//             
//             //proprietà del sensore
//             ArrayList<ProprietaSensor> listprop = parameterSensor.getListprop();
//             logger.info("?=) PARAMETER SENSOR:\n"+ParameterSensor.getMAC()+"\n"+ParameterSensor.getNome()+"\n"+ParameterSensor.getDescrizione()+"\n"+ParameterSensor.getProprietario()+"\n"+ParameterSensor.getCoordLatitude()+"\n"+ParameterSensor.getCoordLongitude()+"\n"+ParameterSensor.getCoordAltitude()+"\n");
//             Iterator<ProprietaSensor> iterator = listprop.iterator();
//             while(iterator.hasNext()){
//                ProprietaSensor next = iterator.next();
//                logger.info("new proprietà: "+next.getNome()+" "+next.getData()+" "+next.getDescrizione());
//                
//                //verifico se la proprietà è gia presente 
//                int idprop;
//                String verifyprop="SELECT * FROM `ProprietaSensore` WHERE `Nome`='"+next.getNome()+"' AND `Data`='"+next.getData()+"' AND `Descrizione`='"+next.getDescrizione()+"'";
//                ResultSet rsprop=db.exQuery(verifyprop);
//                if (rsprop.next()){
//                    //prendo l'idprop
//                    idprop=rsprop.getInt(1);
//                    logger.info("proprietà gia presente id: "+idprop);
//                }else{
//                //inserisco la proprietà
//                String insProp="INSERT INTO `CleverResources`.`ProprietaSensore` (`ID`, `Nome`, `Data`, `Descrizione`) VALUES (NULL, '"+next.getNome()+"', '"+next.getData()+"', '"+next.getDescrizione()+"');";
//                idprop=db.exInsert(insProp);
//                logger.info("proprietà non presente id: "+idprop);
//                }
//                
//                //assegno la proprieta al sensore
//                String inscarat="INSERT INTO `CleverResources`.`CaratteristicheSensore` (`Sensore`, `Proprieta`) VALUES ('"+idSensor+"', '"+idprop+"');";
//                db.exUpdate(inscarat);
//                
//                
//             }
//             
//            //fenomeni misurati dal sensore 
//            ArrayList<fenomeno> listfenom = parameterSensor.getListfenom();
//            Iterator<fenomeno> iterator1 = listfenom.iterator();
//             while(iterator1.hasNext()){
//                fenomeno nextf = iterator1.next();
//                logger.info("new fenomeno: "+nextf.getNome()+" "+nextf.getDescrizione()+" "+nextf.getUom()+" "+nextf.getUom_id());
//                
//                //verifico se il fenomeno gia esiste
//                int idfen;
//                String verifyfen="SELECT * FROM `Fenomeno` WHERE `Nome`='"+nextf.getNome()+"' AND `Descrizione`='"+nextf.getDescrizione()+"'";
//                ResultSet rsfen = db.exQuery(verifyfen);
//                if (rsfen.next()){
//                    idfen=rsfen.getInt(1);
//                    logger.info("?=) fenomeno gia presente id: "+idfen);
//                }else{
//                    //inserisci il fenomeno
//                    String insfen="INSERT INTO `CleverResources`.`Fenomeno` (`ID`, `Nome`, `Descrizione`) VALUES (NULL, '"+nextf.getNome()+"', '"+nextf.getDescrizione()+"');";
//                    idfen = db.exInsert(insfen);
//                    logger.info("?=) fenomeno non presente id: "+idfen);
//                }
//                
//                //verifico se esiste la stessa unita di misura
//                int iduom;
//                String verifyuom="SELECT * FROM `UnitaDiMisura` WHERE `Nome`='"+nextf.getUom()+"' AND `Descrizione`='' AND `Simbolo`='"+nextf.getUom_id()+"'";
//                ResultSet rsuom=db.exQuery(verifyuom);
//                if (rsuom.next()){
//                    iduom=rsuom.getInt(1);
//                    logger.info("?=) unita di misura gia presente id: "+iduom);
//                    
//                }else{
//                    String insuom="INSERT INTO `CleverResources`.`UnitaDiMisura` (`ID`, `Nome`, `Descrizione`, `Simbolo`) VALUES (NULL, '"+nextf.getUom()+"', '', '"+nextf.getUom_id()+"');";
//                    iduom=db.exInsert(insuom);
//                    logger.info("?=) unita di misura non presente id: "+iduom);
//                }
//                
//                
//                
//                //collego il fenomeno con uom
//             
//                String insdefinito="INSERT INTO `CleverResources`.`Definito` (`Fenomeno`, `UdM`) VALUES ('"+idfen+"', '"+iduom+"');";
//                db.exUpdate(insdefinito);
//             
//             
//             
//             
//             }
//             
//             
//             }else{
//                 logger.info("?=) sensor previously recorded!");
//             }
//             
//             
//             //
////             String phen = "SELECT * FROM `Sensore`";
////             ResultSet rs = db.exQuery(phen);
////             logger.debug("?=) prova select:\n");
////             while (rs.next()) {
////
////                     logger.info(rs.getString(1)+"\n");
////             }
//        } catch (SQLException ex) {
//            logger.error("SQLException: "+ex);
//        }
//        
//        
//        
//    }

//    private void handleRegisterClient(Notification notification){
//        try {
//            logger.info("?=) register Client:\n"+notification);
//            String password = (String)notification.getBody();
//            String user=notification.getHostId()+"/"+notification.getAgentId();
//            String Nome="VirtualClient";
//             //logger.info("?=) inserisco nel db il client: "+Nome+" con user: "+user+" and pass: "+password);
//            db=Database.getNewInstance();
//            //verifico la presenza sul db
//            int idclient;
//            String verifyclient="SELECT `ID` FROM `Client` WHERE `Username` ='"+user+"'";
//            ResultSet rsclient = db.exQuery(verifyclient);
//            if (rsclient.next()){
//                idclient = rsclient.getInt(1);
//                logger.info("?=) client gia presente nel db id:"+idclient);
//            }else{
//              String insclient="INSERT INTO `CleverResources`.`Client` (`ID`, `Nome`, `Cognome`, `Email`, `Indirizzo`, `Username`, `Password`) VALUES (NULL, '"+Nome+"', NULL, NULL, NULL, '"+user+"', SHA1('"+password+"'));";
//              idclient=db.exInsert(insclient);
//              logger.info("?=) client non presente nel db id:"+idclient);
//            }
//        } catch (SQLException ex) {
//            java.util.logging.Logger.getLogger(WebAgent.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//    
//    }
    
    
   
    
    
    
    
}
