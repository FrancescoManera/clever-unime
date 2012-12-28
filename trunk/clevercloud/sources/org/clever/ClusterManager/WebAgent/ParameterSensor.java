/*
 * The MIT License
 *
 * Copyright 2012 Francesco Antonino Manera .
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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.clever.HostManager.SOS.SOSModuleTransactional.utils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;













/**
 *
 * @author Francesco Antonino Manera
 */

class ProprietaSensor{
  private String nome;
  private String data;
  private String descrizione;

    public ProprietaSensor() {
        this.nome ="";
        this.data = "";
        this.descrizione = "";
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
  
    public String getData() {
        return data;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public String getNome() {
        return nome;
    }
  
  
  
}





class fenomeno{
    private String Nome;
    private String Descrizione;
    private String uom;
    private String uom_id;

    public String getDescrizione() {
        return Descrizione;
    }

    public String getNome() {
        return Nome;
    }

    public String getUom() {
        return uom;
    }

    public String getUom_id() {
        return uom_id;
    }

    public void setDescrizione(String Descrizione) {
        this.Descrizione = Descrizione;
    }

    public void setNome(String Nome) {
        this.Nome = Nome;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public void setUom_id(String uom_id) {
        this.uom_id = uom_id;
    }
    
    
}


public class ParameterSensor {
    private Map<String, String> paramSens = new ConcurrentHashMap<String, String>();
    private ArrayList<ProprietaSensor> listprop = new ArrayList<ProprietaSensor>();
    private ArrayList<fenomeno> listfenom =new ArrayList<fenomeno>();
    private static ParameterSensor parameterSensor=null;
    private static String Nome;
    private static String MAC;
    private static String descrizione;
    private static String offering;
    private static String proprietario;
    private static String nodeInfo;
    private static String nodeValue;
    private static String coordLatitude;
    private static String coordLongitude;
    private static String coordAltitude;
    private ProprietaSensor tmp=new ProprietaSensor();
    private fenomeno fentmp=new fenomeno();
    private boolean flag=false;
    private boolean uomfen=false;
    
    
    
    
    
    public static String getProprietario() {
        return proprietario;
    }

    public ArrayList<ProprietaSensor> getListprop() {
        return listprop;
    }

    public ArrayList<fenomeno> getListfenom() {
        return listfenom;
    }

   
 



 public static ParameterSensor getInstance(){
       
            parameterSensor=new ParameterSensor();
        
        return parameterSensor;
    }

 
   public static String getMAC() {
        return MAC;
    }

    public static String getNome() {
        return Nome;
    }

    public static String getDescrizione() {
        return descrizione;
    }

    public static String getOffering() {
        return offering;
    }

    public static ParameterSensor getParameterSensor() {
        nodeInfo="";
        nodeValue="";
        return parameterSensor;
    }

    public static String getCoordAltitude() {
        return coordAltitude;
    }

    public static String getCoordLatitude() {
        return coordLatitude;
    }

    public static String getCoordLongitude() {
        return coordLongitude;
    }

    public static void setCoordAltitude(String coord_altitude) {
        ParameterSensor.coordAltitude = coord_altitude;
    }

    public static void setCoordLatitude(String coord_latitude) {
        ParameterSensor.coordLatitude = coord_latitude;
    }

    public static void setCoordLongitude(String coord_longitude) {
        ParameterSensor.coordLongitude = coord_longitude;
    }
    
    public static void setMAC(String MAC) {
        ParameterSensor.MAC = MAC;
    }

    public static void setNome(String Nome) {
        ParameterSensor.Nome = Nome;
    }

    public static void setDescrizione(String descrizione) {
        ParameterSensor.descrizione = descrizione;
    }

    public static void setOffering(String offering) {
        ParameterSensor.offering = offering;
    }

    public static void setParameterSensor(ParameterSensor parameterSensor) {
        ParameterSensor.parameterSensor = parameterSensor;
    }
   

public void parseinput(Node currentNode) {
        
        short sNodeType = currentNode.getNodeType();
        //Se è di tipo Element ricavo le informazioni e le stampo
        if (sNodeType == Node.ELEMENT_NODE) {
            String sNodeName = currentNode.getNodeName();
            //per ogni componente 
            if (sNodeName.equals("sml:identifier")) {
                NamedNodeMap nnmAttributes = currentNode.getAttributes();
                //System.out.println("Content:"+utils.printAttributes(nnmAttributes));
                if (utils.printAttributes(nnmAttributes).indexOf("name=;") != -1) {
                    
                } else {
                    String attrib = utils.printAttributes(nnmAttributes).split(";")[0].split("=")[1];
                    nodeInfo=attrib;
                }

                
            }
            
            if (sNodeName.equals("sml:value")) {
                nodeValue=currentNode.getTextContent();
                if (!(nodeValue.equalsIgnoreCase("")||nodeInfo.equalsIgnoreCase(""))){
                    
                    if(!(this.paramSens.containsKey(nodeInfo))){
                        this.paramSens.put(nodeInfo,nodeValue);
                        //logger.info("?=) new paramSens: "+nodeInfo+" - "+nodeValue);
                    }
                    nodeInfo="";
                    nodeValue="";    
                    
                }
            }
            
            if (sNodeName.equals("swe:field")||sNodeName.equals("swe:coordinate")) {
                flag=true;
                NamedNodeMap fieldAttributes = currentNode.getAttributes();
                //System.out.println("Content:"+utils.printAttributes(nnmAttributes));
                if (utils.printAttributes(fieldAttributes).indexOf("name=;") != -1) {
                    
                } else {
                    String attrib = utils.printAttributes(fieldAttributes).split(";")[0].split("=")[1];
                    this.tmp.setNome(attrib);
                    this.tmp.setData("");
                    this.tmp.setDescrizione("");
                }
            }
            
            
            
            
            if (sNodeName.equals("swe:uom")) {
                if(!uomfen){
                NamedNodeMap uomAttributes = currentNode.getAttributes();
                //System.out.println("Content:"+utils.printAttributes(nnmAttributes));
                if (utils.printAttributes(uomAttributes).indexOf("code=;") != -1) {
                    
                } else {
                    String attrib = utils.printAttributes(uomAttributes).split(";")[0].split("=")[1];
                    if (flag)
                    this.tmp.setDescrizione(attrib);
                }
                }else{
                   NamedNodeMap uomAttributes = currentNode.getAttributes();
               
                if (utils.printAttributes(uomAttributes).indexOf("code=; xlink:href=;") != -1) {
                    
                } else {
                    String attrib = utils.printAttributes(uomAttributes).split(";")[0].split("=")[1];
                    String attrib2 = utils.printAttributes(uomAttributes).split(";")[1].split("=")[1];
                    String reverse = new StringBuffer(attrib2).reverse().toString();
                    String uom_id= reverse.split(":")[0];
 
                    fenomeno ins=new fenomeno();
                    ins.setNome(this.fentmp.getNome());
                    ins.setDescrizione(this.fentmp.getDescrizione());
                    ins.setUom(attrib);
                    ins.setUom_id(uom_id);
                    listfenom.add(ins);
                    uomfen=false;
                    this.fentmp.setNome("");
                    this.fentmp.setDescrizione("");
                    
                }   
                    
                    
                    
                    
                }
           
            
            
            }
            
             if (sNodeName.equals("swe:value")) {
                if (flag){
                    this.tmp.setData(currentNode.getTextContent());
                    if(this.tmp.getNome().equalsIgnoreCase("latitude"))
                        coordLatitude=this.tmp.getData();
                    else if(this.tmp.getNome().equalsIgnoreCase("longitude"))
                        coordLongitude=this.tmp.getData();
                    else if(this.tmp.getNome().equalsIgnoreCase("altitude"))
                        coordAltitude=this.tmp.getData();
                    else{
                    ProprietaSensor ins=new ProprietaSensor();
                    ins.setNome(this.tmp.getNome());
                    ins.setData(this.tmp.getData());
                    ins.setDescrizione(this.tmp.getDescrizione());
                    listprop.add(ins);
                    }
                    this.tmp.setNome("");
                    this.tmp.setData("");
                    this.tmp.setDescrizione("");
                    flag=false;
                    
                }
                
                
            }
            
           if (sNodeName.equals("sml:output")) {
               uomfen=true;
               NamedNodeMap uomAttributes = currentNode.getAttributes();
                //System.out.println("Content:"+utils.printAttributes(nnmAttributes));
                if (utils.printAttributes(uomAttributes).indexOf("name=;") != -1) {
                    
                } else {
                    String attrib = utils.printAttributes(uomAttributes).split(";")[0].split("=")[1];
                    this.fentmp.setNome(attrib);
                }
                

               
                
                
                
           
         } 
            
         if ((sNodeName.equals("swe:Quantity"))&& uomfen) {
                   NamedNodeMap defAttributes = currentNode.getAttributes();
               
                    if (utils.printAttributes(defAttributes).indexOf("definition=;") != -1) {
                    
                    } else {
                        String attrib = utils.printAttributes(defAttributes).split(";")[0].split("=")[1];
                        this.fentmp.setDescrizione(attrib);
                    } 
                }   
            
            
            
            
        }
        if (!currentNode.getNodeName().equals("sml:components")){ //non mi interessano i components
        int iChildNumber = currentNode.getChildNodes().getLength();
        //Se non si tratta di una foglia continua l'esplorazione
        if (currentNode.hasChildNodes()) {
            NodeList nlChilds = currentNode.getChildNodes();
            for (int iChild = 0; iChild < iChildNumber; iChild++) {
                 parseinput(nlChilds.item(iChild));
            }
            
            
        }
        }
    }  


public void setParameter(){
    
    if (this.paramSens.containsKey("uniqueID"))
        ParameterSensor.MAC=this.paramSens.get("uniqueID");      
    if (this.paramSens.containsKey("operator"))
        ParameterSensor.proprietario=this.paramSens.get("operator");
    else
        ParameterSensor.proprietario="";
    if (this.paramSens.containsKey("productName"))
        ParameterSensor.Nome=this.paramSens.get("productName");
    else
        ParameterSensor.Nome="";
    if (this.paramSens.containsKey("manifacturer"))
        ParameterSensor.descrizione=this.paramSens.get("manifacturer");
    else
        ParameterSensor.descrizione="";
    
    

   







}









}
