/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.gardner.dashboardcleaner;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class DashboardProcessor
{
    private File m_oDashboard;
    private Document m_oDoc;
    
    public DashboardProcessor(File oDashboard)
    {
        m_oDashboard = oDashboard;
        
        try
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            m_oDoc = dBuilder.parse(m_oDashboard);
			
            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            m_oDoc.getDocumentElement().normalize();
        }
        catch (Exception e)
        {
            System.out.println("Exception caught parsing dashboard.");
        }
    }
    
    /*
     * Rule 1
     * Set System Profile name on various fields.
    */
    public void setSystemProfileName(String strSysProfName)
    {    
        List<String> oTagList = new ArrayList<String>();
        oTagList.add("dashboardconfig");
        oTagList.add("portletconfig");
        
        for (String strTag : oTagList)
        {
            NodeList nList = m_oDoc.getElementsByTagName(strTag); 
        
            // For each chart source node, set each variable.
            for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
            {
                Node oNode = nList.item(iTemp);

                Element eElement = (Element) oNode;
                eElement.setAttribute("sessionid", strSysProfName);
            }
        }
        
        oTagList.clear();
        oTagList.add("chartsource");
        oTagList.add("lightweightuniquemeasureidentifier");
        
        for (String strTag : oTagList)
        {
            NodeList nList = m_oDoc.getElementsByTagName(strTag); 
        
            // For each chart source node, set each variable.
            for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
            {
                Node oNode = nList.item(iTemp);

                Element eElement = (Element) oNode;
                eElement.setAttribute("systemid", strSysProfName);
            }
        }
    }
    
    
    /* Rule 2 & 3
    * reset author and modifiedby name to admin
    in <dashboardconfig node
    */
    public void setAuthorModifiedByName(String strAuthorName, String strModifiedByName)
    {
        NodeList nList = m_oDoc.getElementsByTagName("dashboardconfig"); 
        
        // For each chart source node, set each variable.
        for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
        {
            Node oNode = nList.item(iTemp);

            Element eElement = (Element) oNode;
            eElement.setAttribute("authorname", strAuthorName);
            eElement.setAttribute("modifiedbyname", strModifiedByName);
        }
    }
    
    /* Rule 4
     set locationassource to false
     Occurs in 2 places: dashboardconfig and portletconfig tags
    */
    private void setLocationAsSource()
    { 
        List<String> oTagList = new ArrayList<String>();
        oTagList.add("dashboardconfig");
        oTagList.add("portletconfig");
        
        for (String strTag : oTagList)
        {
            NodeList nList = m_oDoc.getElementsByTagName(strTag); 
        
            // For each chart source node, set each variable.
            for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
            {
                Node oNode = nList.item(iTemp);
                
                Element eElement = (Element) oNode;
                eElement.setAttribute("locationassource", "false");
            }
        }
    }
    
    /*
    * Rule 5
    */
    private void setChartSourcePositionEntries()
    {
        //Document m_oDoc = doc;
         NodeList nList = m_oDoc.getElementsByTagName("chartsource"); 
          // For each chart source node, set each variable.
            for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
            {
                Node oNode = nList.item(iTemp);

                Element eElement = (Element) oNode;
                
                if (eElement.getAttribute("position").equals("-1")) eElement.setAttribute("position", "1");
            }
    }
    
    /* Rule 6
    * constants are SECONDS, HOURS (1hr is represented as 1 HOURS) and DAYS
    * 24hr is represented as 1 DAYS
    */
    public void setAutoRefresh(boolean bEnable, String strTime, String strUnit)
    {
        // Translate 1 hour to HOURS
        if (strUnit.equalsIgnoreCase(IConstants.HOUR))
        {
            System.out.println("1 hour to 1 HOURS");
            strUnit = IConstants.HOURS;
        }
        // Translate 24 hours to 1 day
        if (strTime.equalsIgnoreCase("24"))
        {
            System.out.println("24hrs to 1 DAYS");
            strTime = "1";
            strUnit = IConstants.DAYS;
        }
        
        NodeList nList = m_oDoc.getElementsByTagName("autorefreshconfig");    

        for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
        {
            Node oNode = nList.item(iTemp);

            Element eElement = (Element) oNode;
            
            if (!bEnable)
            {
                eElement.setAttribute("enabled","false");
            }
            else
            {   
                eElement.setAttribute("enabled","true");
                eElement.setAttribute("time",strTime);
                eElement.setAttribute("unit",strUnit);
            }
        }

    }
    
    /* Rule 7
     * <criterion type="timeframe">
     *   <timeframe end="9223372036854775807" start="0" type="Auto"/>
     * </criterion>
     * Sets the type attribute to Auto
     */
    public void setTimeframe(String strDashboardTimeframe)
    {
        /* Translate GUI Options into XML-valid options
        * XML Options		GUI Options
        * Last 5min		Last 5 minutes
        * Last 15min		Last 15 minutes
        * Last 30min		Last 30 minutes
        * Last 1h		Last 1 hour
        * Last 6h		Last 6 hours
        * Last 24h		Last 24 hours
        * Last 72h		Last 72 hours
        * Last 7d			Last Week
        * Last 30d		Last Month
        * Last 365d		Last Year
        */
        if (strDashboardTimeframe.equals("Last 5 minutes")) strDashboardTimeframe = "Last 5min";
        if (strDashboardTimeframe.equals("Last 15 minutes")) strDashboardTimeframe = "Last 15min";
        if (strDashboardTimeframe.equals("Last 30 minutes")) strDashboardTimeframe = "Last 30min";
        if (strDashboardTimeframe.equals("Last 1 hour")) strDashboardTimeframe = "Last 1h";
        if (strDashboardTimeframe.equals("Last 6 hours")) strDashboardTimeframe = "Last 6h";
        if (strDashboardTimeframe.equals("Last 24 hours")) strDashboardTimeframe = "Last 24h";
        if (strDashboardTimeframe.equals("Last 72 hours")) strDashboardTimeframe = "Last 72h";
        if (strDashboardTimeframe.equals("Last Week")) strDashboardTimeframe = "Last 7d";
        if (strDashboardTimeframe.equals("Last Month")) strDashboardTimeframe = "Last 30d";
        if (strDashboardTimeframe.equals("Last Year")) strDashboardTimeframe = "Last 365d";
        
        NodeList nList = m_oDoc.getElementsByTagName("timeframe");    

        for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
        {
            Node oNode = nList.item(iTemp);

            Element eElement = (Element) oNode;
            eElement.setAttribute("type", strDashboardTimeframe);
        }

    }
    
    // Rule 8
    private void setChartSourceVariables()
    {
        
        String strChartSource = "chartsource";
        
        // dynatrace, version
        // dashboarconfig, memento.version
        Map<String,String> oAttributeMap = new HashMap<String,String>();
        oAttributeMap.put("colorinheritancemode","parent");
        oAttributeMap.put("fetchmeasurecolor","false");
        
        NodeList nList = m_oDoc.getElementsByTagName(strChartSource); 
        
        // For each chart source node, set each variable.
        for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
        {
            Node oNode = nList.item(iTemp);

            Element eElement = (Element) oNode;

            for (String strKey : oAttributeMap.keySet()) eElement.setAttribute(strKey, oAttributeMap.get(strKey));
        }
    }
    
    // TODO - Rule #9
    
    /* Rule 10
    * <dashboardconfig localizationenabled="false"
    * Sets the localizationenabled flag to false.
    */
    private void setLocalization()
    {       
        String strDashboardConfig = "localizationenabled";
        
        NodeList nList = m_oDoc.getElementsByTagName("dashboardconfig");    

        for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
        {
            Node oNode = nList.item(iTemp);

            Element eElement = (Element) oNode;
            eElement.setAttribute(strDashboardConfig,"false");
        }
    }
    
    /* Rule 11
    * This alters the following lines:
    *      <dynatrace version="6.1.0.8154" date="10/8/15 2:06 PM">
    *      <dashboardconfig memento.version="6.1.0.8154"
    * to the relevant versions
    */
    public void setTargetVersion(String strTargetVersion)
    {
     
        // GUI input is user friendly 5.6 - we need it in the format 5.6.0.0000 so let's transform.
        strTargetVersion += ".0.0000";
        
        // dynatrace, version
        // dashboarconfig, memento.version
        Map<String,String> oVersionNodes = new HashMap<String,String>();
        oVersionNodes.put("dynatrace","version");
        oVersionNodes.put("dashboardconfig","memento.version");
        
        for (String strKey : oVersionNodes.keySet())
        {
            NodeList nList = m_oDoc.getElementsByTagName(strKey);    
            
            for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
            {
                Node oNode = nList.item(iTemp);

                Element eElement = (Element) oNode;

                if(strTargetVersion.equals(IConstants.DT_VERSION_56)) eElement.setAttribute(oVersionNodes.get(strKey), IConstants.DT_VERSION_56);
                else if (strTargetVersion.equals(IConstants.DT_VERSION_60)) eElement.setAttribute(oVersionNodes.get(strKey), IConstants.DT_VERSION_60);
                else if (strTargetVersion.equals(IConstants.DT_VERSION_61)) eElement.setAttribute(oVersionNodes.get(strKey), IConstants.DT_VERSION_61);
                else if (strTargetVersion.equals(IConstants.DT_VERSION_62)) eElement.setAttribute(oVersionNodes.get(strKey), IConstants.DT_VERSION_62);
                else if (strTargetVersion.equals(IConstants.DT_VERSION_63)) eElement.setAttribute(oVersionNodes.get(strKey), IConstants.DT_VERSION_63);
                // Future versions possible here.
            }
        }

    }
    
    /* Rule 12
    * <dashboardconfig opendrilldowninnewdashboard="true".....
    */
    public void drilldownsNewDashboard(String strDrilldownsNewDashboard)
    {
        String strDashboardConfig = "opendrilldowninnewdashboard";
        
        NodeList nList = m_oDoc.getElementsByTagName("dashboardconfig");    

        for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
        {
            Node oNode = nList.item(iTemp);

            Element eElement = (Element) oNode;
            
            if (strDrilldownsNewDashboard.equalsIgnoreCase(IConstants.YES))
            {
                eElement.setAttribute(strDashboardConfig,"true");
            }
            else
            {
                eElement.setAttribute(strDashboardConfig,"false");
            }
        }
    }
    
    /* Rule 13
    */
    public void setDayColourScheme(String strDayNightSelection)
    {
        /* Original string comes from UI as either:
         *    - Day (Black Text and White Background)
         *    - Night (White Text and Black Background)
         *
         * Substring at first space to leave "Day" or "Night".
         */
        strDayNightSelection = strDayNightSelection.substring(0, strDayNightSelection.indexOf(" "));
        
        NodeList nList = m_oDoc.getElementsByTagName("designconfig");

        for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
        {
            Node oNode = nList.item(iTemp);

            Element eElement = (Element) oNode;
            if (strDayNightSelection.equals(IConstants.DAY)) eElement.setAttribute("clienttheme","DEFAULT");
            else eElement.setAttribute("clienttheme","NIGHT");
        }
    }
    
    
    private void doFinalProcessing()
    {
        setLocationAsSource();
        setChartSourcePositionEntries();
        setChartSourceVariables();
        setLocalization();
    }
    
    /*
    * Save the XML to disk
    * Note: This messes with line formatting making comparisons
    * with tools like BeyondCompare difficult.
    * No idea why, but it's an enhancement possibility.
    */
    public boolean save()
    {
       doFinalProcessing(); // Non-GUI-configurable options.
        
       try {
           // Save the new file in the old directory with a _clean.dashboard.xml ending
           String strPath = m_oDashboard.getParent()+"/"; // Represents directory. Add the trailing slash in anticipation of the save.

           String strOldName = m_oDashboard.getName();
           
           // Strip off old .dashboard.xml then rename ending.
           String strNewName = strOldName.substring(0,strOldName.indexOf(".dashboard.xml")) + "_clean.dashboard.xml";
        
        Transformer transformer = TransformerFactory.newInstance().newTransformer();

        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        Result output = new StreamResult(new File(strPath+strNewName));
        m_oDoc.setXmlStandalone(true);
        Source input = new DOMSource(m_oDoc);
        transformer.transform(input, output);
        } catch (TransformerException ex) {
            System.out.println("Save Exception");
            return false;
        }
       return true;
    }
}