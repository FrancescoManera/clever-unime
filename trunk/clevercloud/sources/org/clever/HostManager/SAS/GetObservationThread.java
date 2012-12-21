/*
 * The MIT License
 *
 * Copyright 2012 alessiodipietro.
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
package org.clever.HostManager.SAS;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.clever.HostManager.SOS.Database;
import org.clever.HostManager.SOS.SOSModuleTransactional.StdRandom;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.IllegalAddException;
import org.jdom.Namespace;

/**
 *
 * @author alessiodipietro
 */
public class GetObservationThread extends Thread {
    //private String requestId;

    private Element phenomenonAdvertise;
    private double alertFrequency;
    private SASAgent sasAgent;
    private Namespace om = Namespace.getNamespace("om","http://www.opengis.net/om/1.0");
    private Namespace swe = Namespace.getNamespace("swe","http://www.opengis.net/swe/1.0.1");
    private Namespace sweAdvertise = Namespace.getNamespace("http://www.opengis.net/swe");
    private Namespace gml = Namespace.getNamespace("gml","http://www.opengis.net/gml");
    private Namespace ogc = Namespace.getNamespace("ogc","http://www.opengis.net/ogc");
    private Document getObservationDocument;
    protected SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
    protected SimpleDateFormat sdfObservations = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
    private long milliseconds;
    private int count=0;
    
    private String dbServer;
    private String dbDriver;
    private String dbUsername;
    private String dbPassword;
    private String dbName;
    
    
    private ParameterContainer parameterContainer;

    public GetObservationThread(String phenomenonAdvertise, SASAgent sasAgent) {
        //this.requestId=requestId;
        this.sasAgent = sasAgent;
        this.phenomenonAdvertise = sasAgent.stringToDom(phenomenonAdvertise).detachRootElement();
        this.alertFrequency = Double.parseDouble(this.phenomenonAdvertise.getChildText("AlertFrequency"));
        
        //build getObservationRequest from phenomenonAdvertise
        this.getObservationDocument=this.buildGetObservationRequest(this.phenomenonAdvertise);
        this.milliseconds = (long) (alertFrequency * 1000);
        
        parameterContainer=ParameterContainer.getInstance();
        this.dbServer=this.parameterContainer.getDbDriver();
        this.dbDriver=this.parameterContainer.getDbDriver();
        this.dbUsername=this.parameterContainer.getDbUsername();
        this.dbPassword=this.parameterContainer.getDbPassword();
        
        
    }

    private Document buildGetObservationRequest(Element phenomenonAdvertise) {
        Document getObservationDoc = null;
        try {
            //System.out.println(sasAgent.outputter.outputString(phenomenonAdvertise));
            Element getObservation = new Element("GetObservation");
            //getObservation.addNamespaceDeclaration(Namespace.getNamespace("http://opengis.net/sos/1.0"));
            getObservation.addNamespaceDeclaration(Namespace.getNamespace("gml","http://www.opengis.net/gml"));
            getObservation.addNamespaceDeclaration(Namespace.getNamespace("ogc", "http://www.opengis.net/gml"));
            getObservation.addNamespaceDeclaration(Namespace.getNamespace("om", "http://www.opengis.net/om/1.0"));
            //getObservation.addNamespaceDeclaration(Namespace.getNamespace("http://www.w3.org/2001/XMLSchema-instance"));
            //getObservation.addNamespaceDeclaration(Namespace.getNamespace("http://www.opengis.net/sos/1.0 http://schemas.opengis.net/sos/1.0.0/sosAll.xsd"));
            
            
            Element offering=new Element("offering");
            offering.setText("urn:MyOrg:offering:3");
            getObservation.addContent(offering);
            
            String observedProperty = ((Element) phenomenonAdvertise.getChild("AlertMessageStructure").getChildren().get(0)).getChild("Content").getAttributeValue("definition");
            Element observedPropertyElement = new Element("observedProperty");
            observedPropertyElement.addContent(observedProperty);
            Element featureOfInterest = new Element("featureOfInterest");
            Element bBox = new Element("BBOX",ogc);
            Element propertyNameElement = new Element("PropertyName",ogc);
            String propertyName = ((Element) phenomenonAdvertise.getChild("OperationArea").getChild("GeoLocation", sweAdvertise).getChildren().get(0)).getName();
            propertyNameElement.setText("gml:"+propertyName);
            Content envelope = ((Element)((Element) phenomenonAdvertise.getChild("OperationArea").getChild("GeoLocation", sweAdvertise).getChildren().get(0))).getChild("Envelope", gml).detach();
            
            bBox.addContent(propertyNameElement);
            bBox.addContent(envelope);
            featureOfInterest.addContent(bBox);
            
            Element responseFormat = new Element("responseFormat");
            responseFormat.addContent("text/xml; subtype=&quot;om/1.0.0&quot;");
            Element resultModel = new Element("resultModel");
            resultModel.addContent("om:Observation");
            Element responseMode = new Element("responseMode");
            responseMode.addContent("inline");

            Element eventTime = new Element("eventTime");
            Element tmDuring = new Element("TM_During",ogc);
            
            Element propertyNameTime = new Element("PropertyName",ogc);
            propertyNameTime.addContent("om:samplingTime");

            Element timePeriod = new Element("TimePeriod",gml);
            Element beginPosition = new Element("beginPosition",gml);
            Element endPosition = new Element("endPosition",gml);
            

            tmDuring.addContent(propertyNameTime);
            timePeriod.addContent(beginPosition);
            timePeriod.addContent(endPosition);
            tmDuring.addContent(timePeriod);
            eventTime.addContent(tmDuring);

            getObservation.addContent(eventTime);
            getObservation.addContent(observedPropertyElement);
            getObservation.addContent(featureOfInterest);
            getObservation.addContent(responseFormat);
            getObservation.addContent(resultModel);
            getObservation.addContent(responseMode);
            getObservationDoc = new Document(getObservation);
        } catch (IllegalAddException ex) {
            //System.out.println(ex);
        } catch(NullPointerException ex){
            //System.out.println(ex);
        }
        return getObservationDoc;

    }
    
    private void setObservationSent(String observation){
        //database.openDB(this.parameterContainer.getDbServer(),this.parameterContainer.getDbDriver(),this.parameterContainer.getDbName(),
        //         this.parameterContainer.getDbUsername(),this.parameterContainer.getDbPassword());
        Database database=Database.getTestInstance(this.dbServer,this.dbDriver, this.dbName,this.dbUsername,this.dbPassword);
        Date sendDate=new Date();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        
        database.exUpdate( "UPDATE `test_reader_observation` "
                                                 + "SET `sent`='1' , `timestamp`='"+sdf.format(sendDate) +"' "
                                                 + "WHERE `observation`="+observation);
       
        //database.closeDB();
        
    }

    private Date sendObservations(Element getObservationResponse) {
        //System.out.println(this.sasAgent.outputter.outputString(getObservationResponse));
        Date lastObservationTime=null;
        Element observationElement=null;
        Element memberElement=null;
        Element dataArray=null;
        Element simpleDataRecord=null;
        Element textBlock=null;
        String blockSeparator="";
        String tokenSeparator="";
        int timeIndex = -1;
        int longitudeIndex = -1;
        int latitudeIndex = -1;
        int dataIndex = -1;
        String values="";
        String[] rawObservations=null;
        String[] splittedObservation=null;
        
        
        List memberElementList=getObservationResponse.getChildren("member", om);
        Iterator memberIterator=memberElementList.iterator();
        if (memberIterator.hasNext()) {
            memberElement = (Element)memberIterator.next();
            observationElement = memberElement.getChild("Observation", om);
            dataArray = observationElement.getChild("result", om).getChild("DataArray", swe);


            simpleDataRecord = dataArray.getChild("elementType", swe).getChild("SimpleDataRecord", swe);

            //get fields indexes
            List fieldList = simpleDataRecord.getChildren("field", swe);
            Iterator iterator = fieldList.iterator();
            Element field = null;
            int i = 0;
            timeIndex = -1;
            longitudeIndex = -1;
            latitudeIndex = -1;
            dataIndex = -1;
            String definition = "";
            String uom = "";
            while (iterator.hasNext()) {
                field = (Element) iterator.next();
                String nameAttribute = field.getAttributeValue("name");
                if (nameAttribute.equals("longitude")) {
                    longitudeIndex = i;

                } else if (nameAttribute.equals("time")) {
                    timeIndex = i;
                } else if (nameAttribute.equals("latitude")) {
                    latitudeIndex = i;
                } else {
                    dataIndex = i;
                    definition = field.getChild("Quantity", swe).getAttributeValue("definition");
                    uom = field.getChild("Quantity", swe).getChild("uom", swe).getAttributeValue("code");
                }
                i++;
            }

            //build AlertMessageStructure
            Element alertMessageStructure = new Element("AlertMessageStructure");
            Element quantityProperty = new Element("QuantityProperty");
            Element content = new Element("Content");
            content.setAttribute("definition", definition);
            content.setAttribute("uom", uom);
            quantityProperty.addContent(content);
            alertMessageStructure.addContent(quantityProperty);
            String alertMessageStructureString = this.sasAgent.outputter.outputString(alertMessageStructure);

            //get values string
            textBlock = dataArray.getChild("encoding", swe).getChild("TextBlock", swe);
            blockSeparator = textBlock.getAttributeValue("blockSeparator");
            tokenSeparator = textBlock.getAttributeValue("tokenSeparator");

            values = dataArray.getChildText("values", swe);
            
            rawObservations = values.split(Pattern.quote(blockSeparator));
            
            for (int j = 0; j < rawObservations.length; j++) {
                splittedObservation = rawObservations[j].split(Pattern.quote(tokenSeparator));
                
                
                this.sasAgent.sendSensorAlertMessage(splittedObservation[timeIndex],
                        Double.parseDouble(splittedObservation[latitudeIndex]),
                        0.0,
                        Double.valueOf(splittedObservation[longitudeIndex]),
                        splittedObservation[dataIndex],
                        alertMessageStructureString);
                
                //this.setObservationSent(splittedObservation[dataIndex]);
                
                //get max observation
                try {
                Date parsedTimestamp=sdfObservations.parse(splittedObservation[timeIndex]);
                
                if(j==0){
                   lastObservationTime=parsedTimestamp;                    
                }else if(parsedTimestamp.after(lastObservationTime)){
                   lastObservationTime=parsedTimestamp;
                }
                
                } catch (ParseException ex) {
                    
                }
                
                //System.out.println("GetObservationThread: sendObservation "+count+++" TIMESTAMP:"+splittedObservation[timeIndex]);
 
            }
            
            
           //analyzes other <member> element (multiple sensors for the same phenomenon)
           while(memberIterator.hasNext()){
               Element otherMemberElement=(Element)memberIterator.next();
               observationElement = otherMemberElement.getChild("Observation", om);
               dataArray = observationElement.getChild("result", om).getChild("DataArray", swe);
               simpleDataRecord = dataArray.getChild("elementType", swe).getChild("SimpleDataRecord", swe);
               //get values string
               textBlock = dataArray.getChild("encoding", swe).getChild("TextBlock", swe);
               values = dataArray.getChildText("values", swe);
               rawObservations = values.split(Pattern.quote(blockSeparator));
               
               for (int j = 0; j < rawObservations.length; j++) {
                splittedObservation = rawObservations[j].split(Pattern.quote(tokenSeparator));
                
                this.sasAgent.sendSensorAlertMessage(splittedObservation[timeIndex],
                        Double.parseDouble(splittedObservation[latitudeIndex]),
                        0.0,
                        Double.valueOf(splittedObservation[longitudeIndex]),
                        splittedObservation[dataIndex],
                        alertMessageStructureString);
                //this.setObservationSent(splittedObservation[dataIndex]);
                
                try {
                Date parsedTimestamp=sdfObservations.parse(splittedObservation[timeIndex]);
                
                if(parsedTimestamp.after(lastObservationTime)){
                   lastObservationTime=parsedTimestamp;
                }
                
                } catch (ParseException ex) {
                        Logger.getLogger(GetObservationThread.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                //System.out.println("GetObservationThread: sendObservation "+count+++" TIMESTAMP:"+splittedObservation[timeIndex]);
 
            }
               
           }

        }
        
        return lastObservationTime;

    }

    @Override
    public void run() {
        
        boolean holdBeginPosition=false;
        long interval=0;
        Date startTime=new Date();
        
        Date beginPosition=startTime;
        while (true) {
            try {
                //sleep alertFrequency seconds
                //sleep(milliseconds);
                 sleep(milliseconds);
                 
                Date endPosition=new Date(); 
                if(getObservationDocument==null){
                    break;
                }
  
                 //update begin end end time position
                 Element getObservationElement=this.getObservationDocument.getRootElement();
                 Element timePeriod=getObservationElement.getChild("eventTime").getChild("TM_During",ogc).getChild("TimePeriod",gml);
                 
                 
                 
                 
                 if(holdBeginPosition){
                    interval+=(long)(alertFrequency*1000);
                 }else{
                    interval=(long)(alertFrequency*1000); 
                 }
                 
               
                 //endPosition.setTime(beginPosition.getTime()+interval);
                 Date now=new Date();
                 endPosition.setTime(beginPosition.getTime()+interval<=now.getTime()?beginPosition.getTime()+interval:now.getTime());
                 
                 timePeriod.getChild("endPosition",gml).setText(this.sdf.format(endPosition));
                 //Long interval=now.getTime()-(long)alertFrequency*1000;
                 //now.setTime(interval);
                 timePeriod.getChild("beginPosition",gml).setText(this.sdf.format(beginPosition));
                                  
                 //System.out.println(sasAgent.outputter.outputString(getObservationDocument));
                //invoke SOS getObservation
                 //System.out.println("NOW: "+sdf.format(new Date()));
                Element getObservationResponse = sasAgent.getObservation(sasAgent.outputter.outputString(getObservationDocument)).detachRootElement();
                
                //beginPosition.setTime(endPosition.getTime()+1000);
                
                //System.out.println(sasAgent.outputter.outputString(getObservationResponse));
                //parse Observations from getObservationResponse end send to SASAgent on Cluster Manager
                
                
                Date lastObservationDate=this.sendObservations(getObservationResponse);
                
                if(lastObservationDate!=null){
                    //beginPosition.setTime(lastObservationDate.getTime()/*+1000*/);
                    holdBeginPosition=false;
                }else{
                    //no observations hold begin position and increase interval
                    holdBeginPosition=true;
                    //System.out.println("no observations");
                    
                } 
                    
                if(!holdBeginPosition){
                    beginPosition.setTime(endPosition.getTime()+1000);
                } 
                
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
