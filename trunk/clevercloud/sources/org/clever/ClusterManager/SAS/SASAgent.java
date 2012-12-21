/*
 * The MIT License
 *
 * Copyright 2011 Alessio Di Pietro.
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
package org.clever.ClusterManager.SAS;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.log4j.Logger;
import org.clever.Common.Communicator.CmAgent;
import org.clever.Common.Communicator.MethodInvoker;
import org.clever.Common.Communicator.ModuleCommunicator;
import org.clever.Common.Communicator.Notification;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.Shared.Support;
import org.clever.HostManager.SAS.AdvertiseRequestEntry;
import org.clever.HostManager.SAS.AdvertisementExpirationTask;
import org.clever.HostManager.SAS.SensorAlertMessage;
import org.jdom.CDATA;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.safehaus.uuid.UUIDGenerator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

class PublishDeliveryEntry {

    public String mucName = "";
    public String mucPassword = "";
    public Map<FilterKey, Element> resultFilter = null;
}

class SubscriptionEntry {

    public HashMap<String, Integer> soiIndex;
    public Timer timer;
}

class AssignedSoi {

    public Element soiList;
    public String minExpirationDate;
}

/**
 *
 * @author alessiodipietro
 */
public class SASAgent extends CmAgent {

    private Logger logger = null;
    private Map<String, List<PublishDeliveryEntry>> publishDelivery = new ConcurrentHashMap<String, List<PublishDeliveryEntry>>();
    private Map<String, String> pubSoi = new ConcurrentHashMap<String, String>();
    private Map<String, SubscriptionEntry> subscriptions = new ConcurrentHashMap<String, SubscriptionEntry>();
    private UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
    private XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
    private SAXParserFactory spf = null;
    private SAXParser sp = null;
    private XMLReader xr = null;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private String agentPrefix;
    private ThreadMessageDispatcher threadMessageDispatcher;
    private String agentName;

    public SASAgent(/*String agentName*/) throws CleverException {
        try {
            super.setAgentName("SASAgent");
            agentName="SASAgent";
            this.agentName = agentName;
            logger = Logger.getLogger(agentName);
            logger.debug("SASAgent CM !!!!");
            mc = new ModuleCommunicator(agentName,"CM");
            mc.setMethodInvokerHandler(this);

            spf = SAXParserFactory.newInstance();
            sp = spf.newSAXParser();
            xr = sp.getXMLReader();

            init();
            List params = new ArrayList();
            params.add(agentName);
            params.add("SAS/Publish");
            this.invoke("DispatcherAgent", "subscribeNotification", true, params);

            params.clear();
            params.add(agentName);
            params.add("SAS/Advertise");
            this.invoke("DispatcherAgent", "subscribeNotification", true, params);

            params.clear();
            params.add(agentName);
            params.add("SAS/Presence");
            this.invoke("DispatcherAgent", "subscribeNotification", true, params);

            params.clear();
            params.add(agentName);
            params.add("SAS/CancelAdvertisement");
            this.invoke("DispatcherAgent", "subscribeNotification", true, params);

            params.clear();
            params.add(agentName);
            params.add("SAS/RenewAdvertisement");
            this.invoke("DispatcherAgent", "subscribeNotification", true, params);
            
            
           
            
           
            
            
            

            this.threadMessageDispatcher = new ThreadMessageDispatcher(this, 100, 10); //TODO: retrieve parameters from configuration file
            this.threadMessageDispatcher.start();
            
//            ///CREO UNA FINTA NOTIFICA
//                Notification prova=new Notification();
//                prova.setHostId("csk-laptop");
//                this.handlePresence(prova);
//            
//            /// FINE PROVA
//            
            
            
        } catch (ParserConfigurationException ex) {
            throw new CleverException(ex, "Parser Configuration Exception: " + ex);
        } catch (SAXException ex) {
            throw new CleverException(ex, "SAX Exception: " + ex);
        } catch (InstantiationException ex) {
            throw new CleverException(ex, "Missing logger.properties or configuration not found");
        } catch (IllegalAccessException ex) {
            throw new CleverException(ex, "Access Error");
        } catch (ClassNotFoundException ex) {
            throw new CleverException(ex, "Plugin Class not found");
        } catch (IOException ex) {
            throw new CleverException(ex, "Error on reading logger.properties");
        }

    }

   
    
    
    private void init() {
        initDb();
        initSoiPublications();
        initPublishDelivery();
        //logger.info("getCapabilities return="+getCapabilities("<GetCapabilities><Sections><Section>Contents</Section></Sections></GetCapabilities>"));
    }
    
    public String testMethod(String value){
        logger.info("testMethod value is: "+value);
        return value;
    }

    private void initSoiPublications() {
        try {
            String advertisements = "";
            advertisements = query("/advertisements");
            Document doc = this.stringToDom(advertisements);
            Element xmlAdvertisements = doc.getRootElement();

            List children = xmlAdvertisements.getChildren();
            Iterator iterator = children.iterator();
            String pubId = "";
            String soi = "";
            Element element = null;
            Element subscriptionOfferingId = null;

            while (iterator.hasNext()) {
                element = (Element) iterator.next();
                pubId = element.getAttributeValue("pubId");
                subscriptionOfferingId = element.getChild("SubscriptionOfferingID");
                soi = subscriptionOfferingId.getText();
                this.pubSoi.put(pubId, soi);
            }
        } catch (CleverException ex) {
            logger.error("Init publication-soi hashtable error: " + ex);
        }
    }

    private void initPublishDelivery() {
        try {
            String subscriptions = "";
            subscriptions = query("/subscriptions");
            Document doc = this.stringToDom(subscriptions);
            Element subscriptionsElement = doc.detachRootElement();

            List children = subscriptionsElement.getChildren();
            Iterator iterator = children.iterator();
            List resultFilter = null;
            Element element = null;
            Element xmppUri = null;
            PublishDeliveryEntry publishDeliveryEntry = null;
            List assignedSoiList = null;
            String subscriptionId = "";
            while (iterator.hasNext()) {
                try {
                    element = (Element) iterator.next();

                    //get actual expiration date
                    String actualExpiration = element.getAttributeValue("expires");
                    Date actualExpirationDate = sdf.parse(actualExpiration);
                    //get current date
                    Date currentDate = new Date();

                    //check actual expiration date
                    if (actualExpirationDate.after(currentDate)) {

                        subscriptionId = element.getAttributeValue("subId");
                        xmppUri = element.getChild("XMPPURI");
                        publishDeliveryEntry = new PublishDeliveryEntry();
                        publishDeliveryEntry.mucName = xmppUri.getText();
                        publishDeliveryEntry.mucPassword = xmppUri.getAttributeValue("password");
                        publishDeliveryEntry.resultFilter = this.createFilterHashMap(element.getChildren("ResultFilter"));
                        assignedSoiList = element.getChild("AssignedSoiList").getChildren();




                        //join the room
                        try {
                            List<String> joinParameters = new ArrayList();
                            joinParameters.add(agentName);
                            joinParameters.add(publishDeliveryEntry.mucName);
                            joinParameters.add(publishDeliveryEntry.mucPassword);
                            this.invoke("DispatcherAgent", "joinAgentRoom", true, joinParameters);
                        } catch (CleverException ex) {
                            logger.error("Error joining rooms: " + ex);
                        }
                        String expirationDate = getSubscriptionExpiration(subscriptionId);
                        this.updatePublishDelivery(assignedSoiList, publishDeliveryEntry, subscriptionId, expirationDate);
                    }
                } catch (ParseException ex) {
                    logger.error("Parse exception: " + ex);
                }

            }
        } catch (CleverException ex) {
            logger.error("Init subscription hashtables error: " + ex);
        }
    }

    private String getSubscriptionExpiration(String subId) {
        String expirationDate = "";
        try {
            String soiListQuery = "/subscriptions/subscription[@subId=\"" + subId + "\"]"
                    + "/AssignedSoiList/AssignedSoi/text()";

            String query = "min("
                    + this.agentPrefix + "/advertisements/advertise[SubscriptionOfferingID="
                    + "../.." + soiListQuery + "]/DesiredPublicationExpiration/xs:dateTime(.))";

            expirationDate = this.rawQuery(query);
            expirationDate = expirationDate.replaceAll("\n", "");
        } catch (CleverException ex) {
            logger.error("Error getting subscription expiration date: " + ex);
        }
        return expirationDate;
    }

    private String query(String location) throws CleverException {
        String result = "";
        try {
            //init hashtable
            List<String> params = new ArrayList();
            logger.debug("***Agent Name= "+this.agentName);
            params.add(this.agentName);
            params.add(location);
            result = (String) this.invoke("DatabaseManagerAgent", "query", true, params);
        } catch (CleverException ex) {
            logger.error("Query error: " + ex);
            throw new CleverException("Query error " + ex);
        }
        return result;
    }

    private String rawQuery(String query) throws CleverException {
        String result = "";
        try {
            //init hashtable
            List<String> params = new ArrayList();
            params.add(query);
            result = (String) this.invoke("DatabaseManagerAgent", "rawQuery", true, params);
        } catch (CleverException ex) {
            logger.error("Query error: " + ex);
            throw new CleverException("Query error " + ex);
        }
        return result;

    }

    private String queryJoin(String location1, String location2) throws CleverException {
        String result = "";
        try {
            //init hashtable
            List<String> params = new ArrayList();
            params.add(this.agentName);
            params.add(location1);
            params.add(location2);
            result = (String) this.invoke("DatabaseManagerAgent", "queryJoin", true, params);
        } catch (CleverException ex) {
            logger.error("Query error: " + ex);
            throw new CleverException("Query error " + ex);
        }
        return result;
    }

    private Boolean checkAgent() {
        List<String> params = new ArrayList();
        Boolean existsAgent = false;
        params.add(this.agentName);
        try {
            existsAgent = (Boolean) this.invoke("DatabaseManagerAgent", "checkAgent", true, params);
        } catch (CleverException ex) {
            logger.error("Init error: " + ex);
        }
        return existsAgent;

    }

    private Boolean checkAgentNode(String location) {
        List<String> params = new ArrayList();
        Boolean existsNode = false;
        params.add(this.agentName);
        params.add(location);
        try {
            existsNode = (Boolean) this.invoke("DatabaseManagerAgent", "checkAgentNode", true, params);
        } catch (CleverException ex) {
            logger.error("Init error: " + ex);
        }
        return existsNode;
    }

    private void insertNode(String node, String where, String location) {
        List<String> params = new ArrayList();
        params.add(this.agentName);
        params.add(node);
        params.add(where);
        params.add(location);

        try {
            this.invoke("DatabaseManagerAgent", "insertNode", true, params);
        } catch (CleverException ex) {
            logger.error("Init error: " + ex);
        }

    }

    private void deleteNode(String location) throws CleverException {
        List<String> params = new ArrayList();
        params.add(this.agentName);
        params.add(location);
        try {
            this.invoke("DatabaseManagerAgent", "deleteNode", true, params);
        } catch (CleverException ex) {
            logger.error("Init error: " + ex);
            throw new CleverException("Error deleting node " + ex);
        }
    }
    
    private void replaceNode(String location, String node) throws CleverException {
        List<String> params = new ArrayList();
        params.add(this.agentName);
        params.add(location);
        params.add(node);
        try {
            this.invoke("DatabaseManagerAgent", "replaceNode", true, params);
        } catch (CleverException ex) {
            logger.error("Init error: " + ex);
            throw new CleverException("Error replacing node " + ex);
        }
        
        
    }

    private void initDb() {
        //logger.debug("Francesco initDb");
        List<String> params = new ArrayList();
        MethodInvoker mi = null;
        Boolean existsAgent = false;
        Boolean existsAdv = false;
        if (!checkAgent()) {
            //insert agent
            params.add(agentName);
            try {
                this.invoke("DatabaseManagerAgent", "addAgent", true, params);
            } catch (CleverException ex) {
                logger.error("Init error: " + ex);
            }

        }

        //check advertisements
        if (!checkAgentNode("/advertisements")) {
            insertNode("<advertisements></advertisements>", "into", "");
        }

        //check capabilities
        if (!checkAgentNode("/capabilities")) {
            insertNode("<capabilities>"
                    + "<Contents>"
                    + "<AcceptAdvertisements>true</AcceptAdvertisements>"
                    + "<SubscriptionOfferingList></SubscriptionOfferingList>"
                    + "</Contents>"
                    + "<ServiceIdentification></ServiceIdentification>"
                    + "<ServiceProvider></ServiceProvider>"
                    + "<OperationsMetadata></OperationsMetadata></capabilities>", "into", "");
        }
        //check subscriptions
        if (!checkAgentNode("/subscriptions")) {
            insertNode("<subscriptions></subscriptions>", "into", "");
        }

        //get agent db prefix
        params = new ArrayList();
        params.add(agentName);
        try {
            this.agentPrefix = (String) this.invoke("DatabaseManagerAgent", "getAgentPrefix", true, params);
        } catch (CleverException ex) {
            logger.error("Init error: " + ex);
        }

        logger.debug(" initDb Done");
    }

    @Override
    public Class getPluginClass() {
        return SASAgent.class;
    }

    @Override
    public Object getPlugin() {
        return this;
    }

    @Override
    public void handleNotification(Notification notification) throws CleverException {
        String notificationType = notification.getId();

        
        if (notificationType.equals("SAS/Advertise")) {
            this.handleAdvertiseRequest(notification);
        } else if (notificationType.equals("SAS/Publish")) {
            this.handlePublish(notification);
        } else if (notificationType.equals("SAS/Presence")) {
            this.handlePresence(notification);
        /*} else if (notificationType.equals("SAS/CancelAdvertisement")) {
            this.handleCancelAdvertisementRequest(notification);*/
        } else if (notificationType.endsWith("SAS/RenewAdvertisement")) {
            this.handleRenewAdvertisement(notification);
        }


    }

    public void handleRenewAdvertisement(Notification notification) {
        String renewAdvertisement = (String) notification.getBody();
        Document doc = stringToDom(renewAdvertisement); //TODO: validate document
        logger.debug("Received renew advertise request:\n" + renewAdvertisement);
        Element renewalStatus = new Element("renewalStatus");
        renewalStatus.setText("confirmed");
        Element renewAdvertisementElement = doc.detachRootElement();

        String requestId = renewAdvertisementElement.getChildText("RequestID");
        Element publicationIdElement = renewAdvertisementElement.getChild("PublicationID");
        String publicationId = publicationIdElement.getText();
        Element desiredPublicationExpirationElement = renewAdvertisementElement.getChild("DesiredPublicationExpiration");
        String desiredPublicationExpiration = desiredPublicationExpirationElement.getText();
        try {
            
            Date expirationDate = sdf.parse(desiredPublicationExpiration);

            //get advertisement from db
            String advertisement = this.query("/advertisements/advertise[@pubId=\"" + publicationId + "\"]");
            if (!advertisement.equals("")) {
                Document advertisementDoc = this.stringToDom(advertisement);
                Element advertiseElement = advertisementDoc.detachRootElement();
                String currentPublicationExpiration = advertiseElement.getChildText("DesiredPublicationExpiration");
                Date currentExpiration = sdf.parse(currentPublicationExpiration);
                if (expirationDate.before(currentExpiration)) {
                    renewalStatus.setText("rejected");
                } else {
                    //update expiration date
                    this.replaceNode("/advertisements/advertise[@pubId=\"" + publicationId + "\"]/DesiredPublicationExpiration", outputter.outputString(desiredPublicationExpirationElement));

                }
            } else {
                renewalStatus.setText("rejected");
            }

            //check if date is after current
        } catch (CleverException ex) {
            logger.error("Error updating expiration date: " + ex);
            renewalStatus.setText("rejected");
        } catch (ParseException ex) {
            logger.error("ParseException: " + ex);
            renewalStatus.setText("rejected");
        }
        //build renew advertise response
        Element renewAdvertisementResponse = new Element("RenewAdvertisementResponse");
        renewAdvertisementResponse.addContent(publicationIdElement.detach());
        renewAdvertisementResponse.addContent(renewalStatus);
        renewAdvertisementResponse.setAttribute("expires", desiredPublicationExpiration);
        Document renewAdvertisementResponseDoc = new Document(renewAdvertisementResponse);
        //send response to hm
        List params = new ArrayList();
        params.add(requestId);
        params.add(outputter.outputString(renewAdvertisementResponseDoc));
        try {
            this.remoteInvocation(notification.getHostId(), "SASAgentHm", "handleRenewAdvertisementResponse", true, params);
        } catch (CleverException ex) {
            logger.error("Error sending renew adv response on hm " + notification.getHostId());
        }

    }

    public void handlePresence(Notification notification) {
        try {
            String queryResult = this.queryJoin("/advertisements/advertise[@hm='" + notification.getHostId() + "']",
                    "/capabilities//SubscriptionOfferingList"
                    + "/SubscriptionOffering[./SubscriptionOfferingID=../../../..//advertise[@hm='" + notification.getHostId() + "']/SubscriptionOfferingID/text()]");
            
                List params = new ArrayList();
                params.add(queryResult);

                this.remoteInvocation(notification.getHostId(), "SASAgentHm", "publicationsRecovery", true, params);

            
        } catch (CleverException ex) {
            logger.error("Error recovering publications on hm " + notification.getHostId());
        }
    }

    public void handlePublish(Notification notification) {
        SensorAlertMessage alertMessage = (SensorAlertMessage) notification.getBody();
        logger.debug("Received alert from " + notification.getHostId() + ": " + alertMessage.toString());


        //get soi
        String soi = this.pubSoi.get(alertMessage.getPublicationId());


        //get muc and filter list
        List<PublishDeliveryEntry> publishDeliveryEntryList = this.publishDelivery.get(soi);
        if (publishDeliveryEntryList != null) {
            Iterator iterator = publishDeliveryEntryList.iterator();
            PublishDeliveryEntry publishDeliveryEntry = null;
            while (iterator.hasNext()) {
                publishDeliveryEntry = (PublishDeliveryEntry) iterator.next();
                //deliver alert message to muc, applying filter
                MucSensorAlertMessage message = new MucSensorAlertMessage();
                message.setPublishDeliveryEntry(publishDeliveryEntry);
                message.setSensorAlertMessage(alertMessage);
                this.threadMessageDispatcher.pushMessage(message);
                //deliverPublish(subscriptionEntry, alertMessage);
            }
        }


    }

    private boolean applyFilterRule(String type, String operator, String observedValue, String filterParameter) {
        boolean send = false;
        if (type.equals("QuantityProperty")) {
            if (operator.equals("isSmallerThan")) {
                send = Double.parseDouble(observedValue) < Double.parseDouble(filterParameter);
            } else if (operator.equals("isGreaterThan")) {
                send = Double.parseDouble(observedValue) > Double.parseDouble(filterParameter);
            } else if (operator.equals("equals")) {
                send = Double.parseDouble(observedValue) == Double.parseDouble(filterParameter);
            } else if (operator.equals("isNotEqualTo")) {
                send = Double.parseDouble(observedValue) != Double.parseDouble(filterParameter);
            }

        } else if (type.equals("CategoryProperty")) {
            if (operator.equals("equals")) {
                send = observedValue.equals(filterParameter);
            } else if (operator.equals("isNotEqualTo")) {
                send = !observedValue.equals(filterParameter);
            }
        } else if (type.equals("CountProperty")) {
            if (operator.equals("isSmallerThan")) {
                send = Integer.parseInt(observedValue) < Integer.parseInt(filterParameter);
            } else if (operator.equals("isGreaterThan")) {
                send = Integer.parseInt(observedValue) > Integer.parseInt(filterParameter);
            } else if (operator.equals("equals")) {
                send = Integer.parseInt(observedValue) == Integer.parseInt(filterParameter);
            } else if (operator.equals("isNotEqualTo")) {
                send = Integer.parseInt(observedValue) != Integer.parseInt(filterParameter);
            }


        } else if (type.equals("BooleanProperty")) {
            if (operator.equals("equals")) {
                send = Boolean.parseBoolean(observedValue) == Boolean.parseBoolean(filterParameter);
            } else if (operator.equals("isNotEqualTo")) {
                send = Boolean.parseBoolean(observedValue) != Boolean.parseBoolean(filterParameter);
            }
        }

        return send;
    }

    protected void deliverPublish(MucSensorAlertMessage message) {

        SensorAlertMessage sensorAlertMessage = message.getSensorAlertMessage();
        PublishDeliveryEntry publishDeliveryEntry = message.getPublishDeliveryEntry();
        Document alertMessageStructureDocument = this.stringToDom(sensorAlertMessage.getAlertMessageStructure());
        Element alertMessageStructure = alertMessageStructureDocument.detachRootElement();

        //get message type
        Element observedProperty = (Element) alertMessageStructure.getChildren().get(0);
        String type = observedProperty.getName();

        //get definition and uom of alert message structures
        Element content = observedProperty.getChild("Content");
        String definition = content.getAttributeValue("definition");
        String uom = content.getAttributeValue("uom");

        //get the correct filter from hashmap
        FilterKey filterKey = new FilterKey();
        filterKey.setDefinition(definition);
        filterKey.setUom(uom);

        Element operator = publishDeliveryEntry.resultFilter.get(filterKey);
        Boolean send=true;
        if(operator!=null){
            String value = operator.getText();

            //apply filter rule depending on message type
            send = applyFilterRule(type, operator.getName(), sensorAlertMessage.getValue(), value);
        }
        //build sas alert message and send it into muc
        if (send) {
            SASAlertMessage sasAlertMessage = new SASAlertMessage();
            sasAlertMessage.setBodyText(sensorAlertMessage.toString());
            sasAlertMessage.setHeader(sensorAlertMessage.getAlertMessageStructure());
            logger.debug("Prepared SASAlertMessage:" + "\n" + sasAlertMessage.toXml());
            sendSASAlertMessage(publishDeliveryEntry.mucName, sasAlertMessage);
        }

    }

    private void sendSASAlertMessage(String mucName, SASAlertMessage sasAlertMessage) {
        List<String> params = new ArrayList();
        params.add(mucName);
        params.add(sasAlertMessage.toXml());
        try {
            this.invoke("DispatcherAgent", "sendMessageAgentRoom", true, params);
        } catch (CleverException ex) {
            logger.error("Error invoking sendMessage on dispatcher " + ex);
        }
    }

    private Document stringToDom(String xmlSource) {
        StringReader stringReader = new StringReader(xmlSource);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(stringReader);
        } catch (JDOMException ex) {
            logger.error("JDOM exception: " + ex);
        } catch (IOException ex) {
            logger.error("IOException: " + ex);
        }
        return doc;

    }

    public void handleAdvertiseRequest(Notification notification) {
        try {
            String advertiseRequest = (String) notification.getBody();
            Document doc = stringToDom(advertiseRequest); //TODO: validate document
            logger.debug("Received advertise request:\n" + advertiseRequest);

            Element advertiseElement = doc.detachRootElement();
            String requestId = ((Element) advertiseElement.getChild("AlertMessageStructure").getChildren().get(0)).getChild("Content").getAttributeValue("definition");
            String advertiseResponse = "";
            String advertiseQuery;
            requestId = requestId.replaceAll(Pattern.quote(":"), "_");


            Element desiredPublicationExpirationElement = advertiseElement.getChild("DesiredPublicationExpiration");
            String desiredPublicationExpiration = desiredPublicationExpirationElement.getText();

            //check desiredPublicationExpiration>currentDate
            Date currentDate = new Date();
            Date desiredExpirationDate = sdf.parse(desiredPublicationExpiration);
            Element advertiseResponseElement = new Element("AdvertiseResponse");
            Element publicationIdElement = new Element("PublicationID");
            if (desiredExpirationDate.after(currentDate)) {
                //check if advertise already exists
                advertiseQuery = this.query("/advertisements/advertise[RequestID=\"" + requestId + "\" and @hm=\""+notification.getHostId()+"\"]");

                if (advertiseQuery.equals("")) {//advertise doesn't exists

                    //generate pubId and soi
                    String publicationId = "Pub-" + Math.abs(uuidGenerator.generateTimeBasedUUID().hashCode());
                    String subscriptionOfferingId = "Soi-" + Math.abs(uuidGenerator.generateTimeBasedUUID().hashCode());




                    //update hashtable
                    this.pubSoi.put(publicationId, subscriptionOfferingId);


                    //insert new advertise in db
                    this.insertNode("<advertise pubId='" + publicationId /*+ "' expires='" + desiredPublicationExpiration*/ + "' hm='" + notification.getHostId() + "'>"
                            + "<SubscriptionOfferingID>" + subscriptionOfferingId + "</SubscriptionOfferingID>"
                            + "<DesiredPublicationExpiration>" + desiredPublicationExpiration + "</DesiredPublicationExpiration>"
                            + "<RequestID>" + requestId + "</RequestID>"
                            + "</advertise>", "into", "/advertisements");

                    //update capabilities document
                    Element root = new Element("SubscriptionOffering");
                    Element subscriptionOfferingIdElement = new Element("SubscriptionOfferingID");
                    subscriptionOfferingIdElement.addContent(subscriptionOfferingId);
                    root.addContent(subscriptionOfferingIdElement);
                    root.addContent(advertiseElement.getChild("AlertMessageStructure").detach());
                    root.addContent(advertiseElement.getChild("FeatureOfInterest").detach());
                    root.addContent(advertiseElement.getChild("OperationArea").detach());
                    root.addContent(advertiseElement.getChild("AlertFrequency").detach());
                    String capabilities = outputter.outputString(root);
                    this.insertNode(capabilities, "into", "/capabilities/Contents/SubscriptionOfferingList");


                    //build advertise response
                    advertiseResponseElement.setAttribute("expires", desiredPublicationExpiration); //TODO: manage expiration
                    publicationIdElement.setText(publicationId);
                    advertiseResponseElement.addContent(publicationIdElement);

                } else {//advertise exists
                    //update advertisement
                    
                    
                    //update expiration
                    this.replaceNode("/advertisements/advertise[./RequestID=\"" + requestId + "\"]/DesiredPublicationExpiration", outputter.outputString(desiredPublicationExpirationElement));
                    
                    //update subscriptionOffering
                    String getSoiQuery = "../../../../advertisements/advertise[./RequestID=\"" + requestId + "\"]/SubscriptionOfferingID/text()";
                    String subscriptionOfferingQuery = "/capabilities/Contents/SubscriptionOfferingList"
                            + "/SubscriptionOffering[./SubscriptionOfferingID=" + getSoiQuery + "]";

                    this.replaceNode(subscriptionOfferingQuery + "/FeatureOfInterest", outputter.outputString(advertiseElement.getChild("FeatureOfInterest")));
                    this.replaceNode(subscriptionOfferingQuery + "/OperationArea", outputter.outputString(advertiseElement.getChild("OperationArea")));
                    this.replaceNode(subscriptionOfferingQuery + "/AlertFrequency", outputter.outputString(advertiseElement.getChild("AlertFrequency")));
                    



                    //build advertise response
                    Document advertiseResponseDocument = this.stringToDom(advertiseQuery);
                    Element advertiseQueryElement = advertiseResponseDocument.detachRootElement();
                    publicationIdElement.setText(advertiseQueryElement.getAttributeValue("pubId"));
                    advertiseResponseElement.addContent(publicationIdElement);
                    advertiseResponseElement.setAttribute("expires", advertiseQueryElement.getChildText("DesiredPublicationExpiration"));
                }
            } else {
                logger.debug("Desired expiration date is after current date");
                advertiseResponseElement.addContent(publicationIdElement);
                advertiseResponseElement.setAttribute("expires", "");
            }
            advertiseResponse = outputter.outputString(advertiseResponseElement);
            //send response to hm agent
            List params = new ArrayList();
            params.add(requestId);
            params.add(advertiseResponse);
            try {
                this.remoteInvocation(notification.getHostId(), "SASAgentHm", "handleAdvertiseResponse", true, params);
            } catch (CleverException ex) {
                logger.error("Error sending advertisement response " + ex);
            }


        } catch (ParseException ex) {
            logger.error("Error checking expiration date: " + ex);
        } catch (CleverException ex) {
            logger.error("Error handling advertise request: " + ex);
        }

    }

    public String getCapabilities(String getCapabilitiesRequest) {
        
        Document doc = stringToDom(getCapabilitiesRequest); //TODO: validate document
        Element getCapabilitiesElement = doc.detachRootElement();
        String prova="";
        Element sectionsElement = getCapabilitiesElement.getChild("Sections");
        Document getCapabilitiesResponseDoc = new Document();
        String getCapabilitiesResponse = "";
        Element capabilitiesElement = new Element("Capabilities");
        prova=prova+"sono prima dell'if \n";
        if (sectionsElement != null) {
            List children = sectionsElement.getChildren();
            Iterator iterator = children.iterator();
            Element element = null;
            String parameterName = "";
            String section = "";
            prova=prova+"sono prima del while \n";
            while (iterator.hasNext()) {
                try {
                    element = (Element) iterator.next();
                    parameterName = element.getText();
                    prova=prova+"prima della query \n";
                    section = this.query("/capabilities/" + parameterName);
                    prova=prova+"ho fatto la query \n";
                    capabilitiesElement.addContent(this.stringToDom(section).detachRootElement());
                } catch (CleverException ex) {
                    logger.error("Error getting capabilities: " + ex);
                }


            }
            getCapabilitiesResponseDoc.addContent(capabilitiesElement);
            getCapabilitiesResponse = outputter.outputString(getCapabilitiesResponseDoc);

        }

        return getCapabilitiesResponse;
            //return prova;//prova francesco
    }

    public String renewSubscription(String renewSubscriptionRequest) {
        String renewSubscriptionResponseString = "";
        try {
            Document renewSubscriptionRequestDoc = this.stringToDom(renewSubscriptionRequest);
            Element renewSubscriptionElement = renewSubscriptionRequestDoc.detachRootElement();

            String subscriptionId = renewSubscriptionElement.getChildText("SubscriptionID");
            Element status = new Element("Status");
            status.setText("OK");
            String soiExpiration = "";
            String subscription = this.query("/subscriptions/subscription[@subId=\"" + subscriptionId + "\"]");

            String newExpiration = "";
            if (!subscription.equals("")) {
                try {

                    soiExpiration = this.getSubscriptionExpiration(subscriptionId);
                    Element subscriptionElement = this.stringToDom(subscription).detachRootElement();
                    String actualSubscriptionExpiration = subscriptionElement.getAttributeValue("expires");
                    newExpiration = actualSubscriptionExpiration;
                    //System.out.println("renew");
                    //System.out.println(actualSubscriptionExpiration);

                    //System.out.println(soiExpiration);
                    Date actualSubscriptionExpirationDate = sdf.parse(actualSubscriptionExpiration);
                    Date soiExpirationDate = sdf.parse(soiExpiration);
                    //renew it's possible
                    if (actualSubscriptionExpirationDate.before(soiExpirationDate)) {
                        //update db
                        subscriptionElement.setAttribute("expires", soiExpiration);
                        newExpiration = soiExpiration;
                        
                        this.replaceNode("/subscriptions/subscription[@subId=\"" + subscriptionId + "\"]", outputter.outputString(subscriptionElement));

                        List soiList = subscriptionElement.getChild("AssignedSoiList").getChildren();
                        //insert into hashtables
                        PublishDeliveryEntry publishDeliveryEntry = new PublishDeliveryEntry();
                        publishDeliveryEntry.mucName = subscriptionElement.getChildText("XMPPURI");
                        publishDeliveryEntry.mucPassword = subscriptionElement.getChild("XMPPURI").getAttributeValue("password");
                        publishDeliveryEntry.resultFilter = createFilterHashMap(subscriptionElement.getChildren("ResultFilter"));
                        updatePublishDelivery(soiList, publishDeliveryEntry, subscriptionId, soiExpiration);
                        //join the room
                        try {
                            List<String> joinParameters = new ArrayList();
                            joinParameters.add(agentName);
                            joinParameters.add(publishDeliveryEntry.mucName);
                            joinParameters.add(publishDeliveryEntry.mucPassword);
                            this.invoke("DispatcherAgent", "joinAgentRoom", true, joinParameters);
                        } catch (CleverException ex) {
                            logger.error("Error joining rooms: " + ex);
                        }

                    } else {
                        status.setText("Error");
                    }
                } catch (CleverException ex) {
                    logger.error("Error updating db: " + ex);
                    status.setText("Error");
                } catch (ParseException ex) {
                    logger.error("Parse date error: " + ex);
                    status.setText("Error");

                }
            } else {
                status.setText("Error");
            }

            Element renewSubscriptionResponse = new Element("RenewSubscriptionResponse");
            renewSubscriptionResponse.setAttribute("expires", newExpiration);
            Element subscriptionIdElement = new Element("SubscriptionID");
            subscriptionIdElement.setText(subscriptionId);
            renewSubscriptionResponse.addContent(subscriptionIdElement);
            renewSubscriptionResponse.addContent(status);
            renewSubscriptionResponseString = outputter.outputString(renewSubscriptionResponse);
        } catch (CleverException ex) {
            logger.error("Error renewing advertisement: " + ex);
        }
        return renewSubscriptionResponseString;
    }

    public String subscribe(String subscribeRequest) {
        Document subscribeRequestDocument = this.stringToDom(subscribeRequest);
        Element subscribeRequestElement = subscribeRequestDocument.detachRootElement();

        //generate subId
        String subscriptionId = "Sub-" + Math.abs(uuidGenerator.generateTimeBasedUUID().hashCode());

        //build subscription
        Element subscriptionElement = new Element("subscription");
        subscriptionElement.setAttribute("subId", subscriptionId);


        List subscribeRequestList = subscribeRequestElement.getChildren();
        Iterator subscribeRequestIterator = subscribeRequestList.iterator();
        Element element = null;
        while (subscribeRequestIterator.hasNext()) {
            element = (Element) subscribeRequestIterator.next();
            Element elementClone = (Element) element.clone();
            subscriptionElement.addContent(elementClone.detach());
        }

        //assign the soi
        AssignedSoi assignedSoi = assignSoi(subscribeRequestElement);
        //List<String> soiList=assignedSoi.soiList;
        List soiList = assignedSoi.soiList.getChildren();
        
        //assign expiration date (min expiration date of assigned soi)
        String expirationDate = assignedSoi.minExpirationDate;
        subscriptionElement.setAttribute("expires", expirationDate);


        Element xmppUri = new Element("XMPPURI");
        xmppUri.setAttribute("password", "");
        if (soiList.size() > 0) {
            //assign the muc
            List<String> joinParameters = new ArrayList();
            joinParameters.add(agentName);
            String mucPassword = Support.generatePassword(7);
            joinParameters.add(mucPassword);
            String mucName = "";
            try {
                mucName = (String) this.invoke("DispatcherAgent", "joinAgentRoom", true, joinParameters);
            } catch (CleverException ex) {
                logger.error("Error joining muc: " + ex);
            }

            xmppUri.setAttribute("password", mucPassword);
            xmppUri.setText(mucName);



            //retreive filter list
            List resultFilterList = subscribeRequestElement.getChildren("ResultFilter");

            //update hashtable
            PublishDeliveryEntry publishDeliveryEntry = new PublishDeliveryEntry();
            publishDeliveryEntry.mucName = mucName;
            publishDeliveryEntry.mucPassword = mucPassword;
            publishDeliveryEntry.resultFilter = createFilterHashMap(resultFilterList);
            updatePublishDelivery(soiList, publishDeliveryEntry, subscriptionId, expirationDate);

            //insert subscription into db
            subscriptionElement.addContent(xmppUri);
            subscriptionElement.addContent(assignedSoi.soiList);
            this.insertNode(outputter.outputString(subscriptionElement), "into", "/subscriptions");
        }
        //build subscribe response
        Element subscribeResponse = new Element("SubscribeResponse");

        subscribeResponse.setAttribute("SubscriptionID", subscriptionId);
        subscribeResponse.setAttribute("expires", expirationDate);
        Element xmppResponse = new Element("XMPPResponse");
        xmppResponse.addContent(xmppUri.detach());
        subscribeResponse.addContent(xmppResponse);
        Document subscribeResponseDocument = new Document(subscribeResponse);
        return outputter.outputString(subscribeResponseDocument);
    }

    private void updatePublishDelivery(List soiList, PublishDeliveryEntry publishDeliveryEntry,
            String subscriptionId, String expirationDate) {
        try {
            Iterator iterator = soiList.iterator();
            Element assignedSoi = null;
            List<PublishDeliveryEntry> se = null;
            SubscriptionEntry subscriptionEntry = null;
            //HashMap<String, Integer> publishDeliveryEntryMap = null;
            while (iterator.hasNext()) {
                assignedSoi = (Element) iterator.next();
                se = this.publishDelivery.get(assignedSoi.getText());
                if (se == null) {
                    se = new ArrayList<PublishDeliveryEntry>();
                }
                se.add(publishDeliveryEntry);
                this.publishDelivery.put(assignedSoi.getText(), se);
                //update subscriptions
                subscriptionEntry = this.subscriptions.get(subscriptionId);
                if (subscriptionEntry == null) {
                    subscriptionEntry = new SubscriptionEntry();
                    subscriptionEntry.soiIndex = new HashMap<String, Integer>();
                }
                subscriptionEntry.soiIndex.put(assignedSoi.getText(), se.size() - 1);


            }
            if (subscriptionEntry.timer != null) {
                subscriptionEntry.timer.cancel();
                subscriptionEntry.timer.purge();
            }
            subscriptionEntry.timer = new Timer();
            SubscriptionExpirationTask subscriptionExpirationTask = new SubscriptionExpirationTask(this, subscriptionId);
            Date expiration = sdf.parse(expirationDate);
            subscriptionEntry.timer.schedule(subscriptionExpirationTask, expiration);
            this.subscriptions.put(subscriptionId, subscriptionEntry);
        } catch (ParseException ex) {
            logger.error("Error updating subscriptions hashtable: " + ex);
        }

    }

    private HashMap<FilterKey, Element> createFilterHashMap(List resultFilterList) {
        HashMap<FilterKey, Element> resultFilter = new HashMap<FilterKey, Element>();

        Iterator iterator = resultFilterList.iterator();
        Element element = null;
        while (iterator.hasNext()) {
            element = (Element) iterator.next();
            FilterKey filterKey = new FilterKey();
            filterKey.setDefinition(element.getAttributeValue("ObservedPropertyDefinition"));
            filterKey.setUom(element.getAttributeValue("uom"));
            resultFilter.put(filterKey, (Element) (element.getChildren().get(0)));

        }
        return resultFilter;
    }

   /* public void handleCancelAdvertisementRequest(Notification notification) {
        String cancelAdvertisementRequest = (String) notification.getBody();
        Document doc = stringToDom(cancelAdvertisementRequest); //TODO: validate document
        logger.debug("Received cancel advertise request:\n" + cancelAdvertisementRequest);
        Element cancelAdvertisementElement = doc.detachRootElement();
        Element cancellationStatusElement = new Element("CancellationStatus");
        cancellationStatusElement.setText("confirmed");

        //get pubId from request
        String publicationId = cancelAdvertisementElement.getChildText("PublicationID");

        //get soi from pub-Soi hashtable
        String soi = this.pubSoi.get(publicationId);
        if (soi == null) {
            cancellationStatusElement.setText("invalid_PublicationID");
        } else {
            //delete soi from soi-SubscribeEntryList hashtable
            this.publishDelivery.remove(soi);
            try {
                //delete advertisement from db
                this.deleteNode("/advertisements/advertise[@pubId=\"" + publicationId + "\"]");
                //delete soi from capabilities document
                this.deleteNode("/capabilities/Contents/SubscriptionOfferingList/SubscriptionOffering[SubscriptionOfferingID=\"" + soi + "\"]");
                //delete soi from hashtable pub-soi
                this.pubSoi.remove(publicationId);

                //get all subscriptions assigned to this soi
                String affectedSubscriptions = this.query("/subscriptions[subscription[//AssignedSoi=\"" + soi + "\"]]");

                //iterate affected subId
                Document affectedSubscriptionsDoc = this.stringToDom(affectedSubscriptions);
                Element affectedSubscriptionsElement = affectedSubscriptionsDoc.detachRootElement();
                List affectedSubscriptionsList = affectedSubscriptionsElement.getChildren();
                Iterator iterator = affectedSubscriptionsList.iterator();
                Element affectedSubscription = null;

                while (iterator.hasNext()) {
                    affectedSubscription = (Element) iterator.next();
                    String affectedSubId = affectedSubscription.getAttributeValue("subId");

                    //update subscription hashtable
                    SubscriptionEntry subscriptionEntry = this.subscriptions.get(affectedSubId);
                    HashMap<String, Integer> publishDeliveryMap = subscriptionEntry.soiIndex;
                    publishDeliveryMap.remove(soi);
                    if (publishDeliveryMap.isEmpty()) { //it was the only soi assigned
                        //remove subId entry
                        this.subscriptions.remove(affectedSubId);
                        //delete also subscription from db
                        this.deleteNode("/subscriptions/subscription[@subId=\"" + affectedSubId + "\"]");

                    }

                }

                //delete soi assignation from residual subscriptions
                this.deleteNode("/subscriptions//AssignedSoi[text()=\"" + soi + "\"]");

            } catch (CleverException ex) {
                cancellationStatusElement.setText("error");
                logger.error("Error deleting advertisement from db: " + ex);
            }


        }

        //build cancelAdvertisement Response
        Element cancelAdvertisementResponseElement = new Element("CancelAdvertisementResponse");
        Element publicationIdElement = new Element("PublicationID");
        publicationIdElement.setText(publicationId);

        cancelAdvertisementResponseElement.addContent(publicationIdElement);
        cancelAdvertisementResponseElement.addContent(cancellationStatusElement);
        Document cancelAdvertisementResponseDoc = new Document(cancelAdvertisementResponseElement);
        String cancelAdvertisementResponse = outputter.outputString(cancelAdvertisementResponseDoc);

        //invoke handleCancelAdvertiseResponse on hm agent
        String requestId = cancelAdvertisementElement.getChildText("RequestID");
        List params = new ArrayList();
        params.add(requestId);
        params.add(cancelAdvertisementResponse);
        try {
            this.remoteInvocation(notification.getHostId(), "SASAgentHm", "handleCancelAdvertisementResponse", true, params);
        } catch (CleverException ex) {
            logger.error("Error sending cancelAdvertisement response " + ex);
        }


    }*/

    public String removeSubscriptionFromTables(String subscriptionId) {
        String message = "OK";
        //remove from hashtable
        SubscriptionEntry subscriptionEntry = this.subscriptions.get(subscriptionId);
        if (subscriptionEntry != null) {
            HashMap subscriptionEntryMap = subscriptionEntry.soiIndex;
            Iterator iterator = subscriptionEntryMap.entrySet().iterator();
            subscriptionEntry.timer.cancel();
            subscriptionEntry.timer.purge();
            subscriptions.remove(subscriptionId);

            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                List<PublishDeliveryEntry> publishDeliveryEntryList = this.publishDelivery.get((String) entry.getKey());

                //leave muc
                List<String> joinParameters = new ArrayList();
                joinParameters.add(publishDeliveryEntryList.get((Integer) entry.getValue()).mucName);
                try {
                    this.invoke("DispatcherAgent", "leaveAgentRoom", true, joinParameters);
                } catch (CleverException ex) {
                    logger.error("Error leaving muc: " + ex);
                    message = "ERROR";
                }

                publishDeliveryEntryList.remove(((Integer) entry.getValue()).intValue());
                this.publishDelivery.put((String) entry.getKey(), publishDeliveryEntryList);


            }
        }

        return message;

    }

    public String cancelSubscription(String cancelSubscriptionRequest) {
        String cancelSubscriptionResponseString = "";
        try {
            Document cancelSubscriptionRequestDocument = this.stringToDom(cancelSubscriptionRequest);
            Element cancelSubscriptionRequestElement = cancelSubscriptionRequestDocument.detachRootElement();

            //get subscription id
            String subscriptionId = cancelSubscriptionRequestElement.getChild("SubscriptionID").getText();
            Element statusElement = new Element("Status");
            statusElement.setText("OK");
            //remove subscription from db
            String subscription = query("/subscriptions/subscription[@subId=\"" + subscriptionId + "\"]");
            if (!subscription.equals("")) {
                try {
                    this.deleteNode("/subscriptions/subscription[@subId=\"" + subscriptionId + "\"]");

                    statusElement.setText(this.removeSubscriptionFromTables(subscriptionId));
                } catch (CleverException ex) {
                    statusElement.setText("ERROR");
                }

            } else {//status=error
                statusElement.setText("ERROR");
            }

            //build cancel subscription response
            Element cancelSubscriptionResponse = new Element("CancelSubscriptionResponse");
            cancelSubscriptionResponse.addContent(cancelSubscriptionRequestElement.getChild("SubscriptionID").detach());
            cancelSubscriptionResponse.addContent(statusElement);
            Document cancelSubscriptionResponseDoc = new Document(cancelSubscriptionResponse);
            cancelSubscriptionResponseString = outputter.outputString(cancelSubscriptionResponseDoc);
        } catch (CleverException ex) {
            logger.error("Error deleting subscription: " + ex);
        }

        return cancelSubscriptionResponseString;
    }

    private AssignedSoi assignSoi(Element subscribeRequestElement) {        
        Element assignedSoiList=new Element("AssignedSoiList");
        String expirationDate = "";
        //check soi
        Element subscriptionOfferingIdElement = subscribeRequestElement.getChild("SubscriptionOfferingId");
        if (subscriptionOfferingIdElement != null) {
            try {
                //there is a soi, don't check other parameters
                String desiredSoi = subscriptionOfferingIdElement.getText();
                String queryResult = this.query("/capabilities/Contents/SubscriptionOfferingList/SubscriptionOffering[SubscriptionOfferingID=\"" + desiredSoi + "\"]");
                if (!queryResult.equals("")) {
                    expirationDate = this.query("/advertisements/advertise[./SubscriptionOfferingID=\"" + desiredSoi + "\"]/DesiredPublicationExpiration/text()");
                    expirationDate=expirationDate.replaceAll("\n", "");
                    Element assignedSoi=new Element("AssignedSoi");
                    assignedSoi.setText(desiredSoi);
                    assignedSoiList.addContent(assignedSoi);
                    
                }
            } catch (CleverException ex) {
                logger.error("Error assigning soi: " + ex);
            }
        } else {
            try {
                //get SubscriptionOfferingList from Capabilities Document
                //get SubscriptionOfferingList from Capabilities Document


                Element subscribeRequestElementLocation = subscribeRequestElement.getChild("Location");
                Element featureOfInterestName = subscribeRequestElement.getChild("FeatureOfInterestName");
                List resultFilterList = subscribeRequestElement.getChildren("ResultFilter");

                StringBuffer xpathConditions = new StringBuffer();


                //get feature of interest xpath
                xpathConditions.append("true() and ");
                String featureOfInterestXpath = "";
                if (featureOfInterestName != null) {
                    featureOfInterestXpath = "./FeatureOfInterest"
                            + "/Name[text()=\"" + featureOfInterestName.getText() + "\"] and ";
                }
                xpathConditions.append(featureOfInterestXpath);

                //get location xpaths
                xpathConditions.append("true() and ");
                String locationXpaths = "";
                if (subscribeRequestElementLocation != null) {
                    locationXpaths = this.getNodeExistsXPathQuery((Element) subscribeRequestElementLocation.getChildren().get(0)) + " and ";
                }
                xpathConditions.append(locationXpaths);


                //get filter xpaths
                xpathConditions.append("true()");
                String filterXpaths = "";
                if (resultFilterList.size() > 0) {
                    filterXpaths = " and " + this.getFilterExistsXPathQuery(resultFilterList);
                }
                xpathConditions.append(filterXpaths);

                //build query
                String query = "/capabilities"
                        + "/Contents"
                        + "/SubscriptionOfferingList"
                        + "/SubscriptionOffering[" + xpathConditions.substring(0) + "]"
                        + "/SubscriptionOfferingID";

                //execute query
                String soiListString = "";
                soiListString = this.query(query);

                //get expiration date
                String expirationDateQuery = "min("
                        + this.agentPrefix + "/advertisements/advertise[SubscriptionOfferingID="
                        + "../.." + query + "/text()]/DesiredPublicationExpiration/xs:dateTime(.))";


                expirationDate = this.rawQuery(expirationDateQuery);
                expirationDate = expirationDate.replaceAll("\n", "");



                //build soi List
                if (!soiListString.equals("")) {
                    Document soiListDocument = this.stringToDom("<soiList>" + soiListString + "</soiList>");
                    Element soiListElement = soiListDocument.detachRootElement();

                    List soiListList = soiListElement.getChildren();
                    Iterator iterator = soiListList.iterator();

                    while (iterator.hasNext()) {
                        Element soi = (Element) iterator.next();
                        Element soiListElem = new Element("AssignedSoi");
                        soiListElem.setText(soi.getText());
                        assignedSoiList.addContent(soiListElem);

                    }


                }
            } catch (CleverException ex) {
                logger.error("Error assigning soi: " + ex);
            }


        }
        AssignedSoi assignedSoi = new AssignedSoi();
        assignedSoi.soiList = assignedSoiList;
        assignedSoi.minExpirationDate = expirationDate;
        return assignedSoi;
    }

    private String getNodeExistsXPathQuery(Element node) {
        StringBuffer buffer = new StringBuffer();
        XPathContentHandler fragmentContentHandler = new XPathContentHandler(xr, buffer);
        xr.setContentHandler(fragmentContentHandler);
        try {
            xr.parse(new InputSource(new StringReader(outputter.outputString(node))));
        } catch (IOException ex) {
            logger.error("IOException: " + ex);
        } catch (SAXException ex) {
            logger.error("SAXException: " + ex);
        }

        return buffer.substring(0);

    }

    private String getFilterExistsXPathQuery(List resultFilterList) {
        StringBuffer xpathQuery = new StringBuffer();
        Iterator iterator = resultFilterList.iterator();
        Element element = null;
        String definition = "";
        String uom = "";
        xpathQuery.append("(");
        int i = 0;
        while (iterator.hasNext()) {
            element = (Element) iterator.next();
            definition = element.getAttributeValue("ObservedPropertyDefinition");
            uom = element.getAttributeValue("uom");
            xpathQuery.append("./AlertMessageStructure//Content[@definition=\"" + definition + "\" and @uom=\"" + uom + "\"]");

            if (iterator.hasNext() && resultFilterList.size() > 1) {
                xpathQuery.append(" or ");
            } else {
                xpathQuery.append(")");
            }

        }

        return xpathQuery.substring(0);

    }

    @Override
    public void initialization() throws Exception {
        
        
    }

    @Override
    public void shutDown() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
