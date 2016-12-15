package com.gardner.dashboardcleaner;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DashboardProcessor
{
    private File m_oDashboardFileOrDir;
    //private Document oDoc;
    private boolean m_bProcessingDirectory = false;
    //private List<Document> oDocList = new ArrayList<Document>();
    private HashMap<Document, File> oDocMap = new HashMap<Document, File>();
    
    public DashboardProcessor(File oDashboard)
    {
        m_oDashboardFileOrDir = oDashboard;
        
        // Set flag if we're processing a directory
        if (m_oDashboardFileOrDir.isDirectory()) m_bProcessingDirectory = true;
       
        try
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            if (m_oDashboardFileOrDir.isDirectory())
            {
            	//FileNameExtensionFilter filter = new FileNameExtensionFilter("Dashboard XML Files (.xml)","xml");
            	FilenameFilter filter = new FilenameFilter() {
            		
					@Override
					public boolean accept(File dir, String name) {
						if (name.endsWith(".dashboard.xml")) return true;
						return false;
					}
				};
				
            	File[] a_Dashboards = m_oDashboardFileOrDir.listFiles(filter);
            	
            	for (File oFile : a_Dashboards)
            	{
            		oDocMap.put(dBuilder.parse(oFile), oFile);
            	}
            	
            }
            else
            {
            	oDocMap.put(dBuilder.parse(m_oDashboardFileOrDir), m_oDashboardFileOrDir);
            }
			
            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            for (Document oDoc : oDocMap.keySet()) oDoc.getDocumentElement().normalize();
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
    public void setSystemProfileName(String strSysProfName,Document oDoc)
    {    
        List<String> oTagList = new ArrayList<String>();
        oTagList.add("dashboardconfig");
        oTagList.add("portletconfig");
        
        for (String strTag : oTagList)
        {
            NodeList nList = oDoc.getElementsByTagName(strTag); 
        
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
            NodeList nList = oDoc.getElementsByTagName(strTag); 
        
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
    public void setAuthorModifiedByName(String strAuthorName, String strModifiedByName, Document oDoc)
    {
        NodeList nList = oDoc.getElementsByTagName("dashboardconfig"); 
        
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
    private void setLocationAsSource(Document oDoc)
    { 
        List<String> oTagList = new ArrayList<String>();
        oTagList.add("dashboardconfig");
        oTagList.add("portletconfig");
        
        for (String strTag : oTagList)
        {
            NodeList nList = oDoc.getElementsByTagName(strTag); 
        
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
    private void setChartSourcePositionEntries(Document oDoc)
    {
         NodeList nList = oDoc.getElementsByTagName("chartsource"); 
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
    public void setAutoRefresh(boolean bEnable, String strTime, String strUnit, Document oDoc)
    {
        // Translate 1 hour to HOURS
        if (strUnit.equalsIgnoreCase(IConstants.HOUR))
        {
            strUnit = IConstants.HOURS;
        }
        // Translate 24 hours to 1 day
        if (strTime.equalsIgnoreCase("24"))
        {
            strTime = "1";
            strUnit = IConstants.DAYS;
        }
        
        NodeList nList = oDoc.getElementsByTagName("autorefreshconfig");    

        for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
        {
            Node oNode = nList.item(iTemp);

            Element eElement = (Element) oNode;
            
            if (!bEnable) eElement.setAttribute("enabled","false");
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
    public void setTimeframe(String strDashboardTimeframe, Document oDoc)
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
        * Last 7d		Last Week
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
        
        NodeList nList = oDoc.getElementsByTagName("timeframe");    

        for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
        {
            Node oNode = nList.item(iTemp);

            Element eElement = (Element) oNode;
            eElement.setAttribute("type", strDashboardTimeframe);
        }

    }
    
    // Rule 8
    private void setChartSourceVariables(Document oDoc)
    {
        
        String strChartSource = "chartsource";
        
        // dynatrace, version
        // dashboarconfig, memento.version
        Map<String,String> oAttributeMap = new HashMap<String,String>();
        oAttributeMap.put("colorinheritancemode","parent");
        oAttributeMap.put("fetchmeasurecolor","false");
        
        NodeList nList = oDoc.getElementsByTagName(strChartSource); 
        
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
    private void setLocalization(Document oDoc)
    {       
        String strDashboardConfig = "localizationenabled";
        
        NodeList nList = oDoc.getElementsByTagName("dashboardconfig");    

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
    public void setTargetVersion(String strTargetVersion, Document oDoc)
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
            NodeList nList = oDoc.getElementsByTagName(strKey);    
            
            for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
            {
                Node oNode = nList.item(iTemp);

                Element eElement = (Element) oNode;

                if(strTargetVersion.equals(IConstants.DT_VERSION_56)) eElement.setAttribute(oVersionNodes.get(strKey), IConstants.DT_VERSION_56);
                else if (strTargetVersion.equals(IConstants.DT_VERSION_60)) eElement.setAttribute(oVersionNodes.get(strKey), IConstants.DT_VERSION_60);
                else if (strTargetVersion.equals(IConstants.DT_VERSION_61)) eElement.setAttribute(oVersionNodes.get(strKey), IConstants.DT_VERSION_61);
                else if (strTargetVersion.equals(IConstants.DT_VERSION_62)) eElement.setAttribute(oVersionNodes.get(strKey), IConstants.DT_VERSION_62);
                else if (strTargetVersion.equals(IConstants.DT_VERSION_63)) eElement.setAttribute(oVersionNodes.get(strKey), IConstants.DT_VERSION_63);
                else if (strTargetVersion.equals(IConstants.DT_VERSION_65)) eElement.setAttribute(oVersionNodes.get(strKey), IConstants.DT_VERSION_65);
                // Future versions possible here.
            }
        }

    }
    
    /* Rule 12
    * <dashboardconfig opendrilldowninnewdashboard="true".....
    */
    public void drilldownsNewDashboard(String strDrilldownsNewDashboard, Document oDoc)
    {
        String strDashboardConfig = "opendrilldowninnewdashboard";
        
        NodeList nList = oDoc.getElementsByTagName("dashboardconfig");    

        for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
        {
            Node oNode = nList.item(iTemp);

            Element eElement = (Element) oNode;
            
            if (strDrilldownsNewDashboard.equalsIgnoreCase(IConstants.YES)) eElement.setAttribute(strDashboardConfig,"true");
            else eElement.setAttribute(strDashboardConfig,"false");
        }
    }
    
    /* Rule 13
    */
    public void setDayColourScheme(String strDayNightSelection, Document oDoc)
    {
        /* Original string comes from UI as either:
         *    - Day (Black Text and White Background)
         *    - Night (White Text and Black Background)
         *
         * Substring at first space to leave "Day" or "Night".
         */
        strDayNightSelection = strDayNightSelection.substring(0, strDayNightSelection.indexOf(" "));
        
        NodeList nList = oDoc.getElementsByTagName("designconfig");

        for (int iTemp = 0; iTemp < nList.getLength(); iTemp++)
        {
            Node oNode = nList.item(iTemp);

            Element eElement = (Element) oNode;
            if (strDayNightSelection.equals(IConstants.DAY)) eElement.setAttribute("clienttheme","DEFAULT");
            else eElement.setAttribute("clienttheme","NIGHT");
        }
    }
    
    // Helper method: determine whether we're processing a directory or not
    public boolean processingDirectory()
    {
    	return m_bProcessingDirectory;
    }
    
    // Get list of all Document objects - these correspond to a dashboard per element
    public List<Document> getDashboardList()
    {
    	return new ArrayList<Document>(oDocMap.keySet());
    }
    
    private void doFinalProcessing(Document oDoc)
    {
        setLocationAsSource(oDoc);
        setChartSourcePositionEntries(oDoc);
        setChartSourceVariables(oDoc);
        setLocalization(oDoc);
    }
    
    /*
    * Save the XML to disk
    * Note: This messes with line formatting making comparisons
    * with tools like BeyondCompare difficult.
    * No idea why, but it's an enhancement possibility.
    */
    public boolean save(Document oDoc)
    {
       doFinalProcessing(oDoc); // Non-GUI-configurable options.
        
       // Get File corresponding to oDoc
       File oFile = oDocMap.get(oDoc);
       
       try {
 		// Save the new file in the old directory with a _clean.dashboard.xml ending
    	   	String strPath = oFile.getParent()+"/"; // Represents directory. Add the trailing slash in anticipation of the save.
    	   	String strOldName = oFile.getName();
		// Strip off old .dashboard.xml then rename ending.
		String strNewName = strOldName.substring(0,strOldName.indexOf(".dashboard.xml")) + "_clean.dashboard.xml";

		Transformer transformer = TransformerFactory.newInstance().newTransformer();

		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		Result output = new StreamResult(new File(strPath+strNewName));
		oDoc.setXmlStandalone(true);
		Source input = new DOMSource(oDoc);
		transformer.transform(input, output);
        } catch (TransformerException ex) {
            System.out.println("Save Exception");
            return false;
        }
       return true;
    }
}
