package uk.ac.york.mhe504.models;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.cloudbus.cloudsim.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CloudCostModel {
	
	private String sourceFile;
	
	private double databaseInstanceCost;
	private double databaseStorageCostPerGB;
	private double databaseTransferCostPerMIOPS;
	private double migrationInstanceCost;
	private double vpnInstanceCost;


	public CloudCostModel(String filePath, String dbInstName, String dbInstRegion, String dbInstTeir, String dbInstStorageType,
							String migInstName, String migInstRegion, String MigInstTier,
							String vpnInstName, String vpnInstRegion,
							String dbStorageTier, String dbStorageRegion) {
		
		Log.printLine("Loading cloud cost model from: " + filePath);
		
		sourceFile = filePath;
		databaseInstanceCost = loadDbCost(dbInstName,dbInstRegion,dbInstTeir);
		databaseStorageCostPerGB = loadDbStorageCost(dbInstRegion,dbInstStorageType);
		databaseTransferCostPerMIOPS = loadDbIopsCost(dbStorageRegion, dbStorageTier);
		
	}

	private double loadDbIopsCost(String region, String tier) {
		try{
			
			File fXmlFile = new File(sourceFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			
			doc.getDocumentElement().normalize();
			
			NodeList nList = doc.getElementsByTagName("utilisedService");
			for (int i=0; i< nList.getLength(); i++)
			{
				Node nn = nList.item(i);
				if (nn.getAttributes().getNamedItem("name").getNodeValue().equals("DBaaS"))
				{
					NodeList childNodes = nList.item(i).getChildNodes();
					for (int j=0; j< childNodes.getLength(); j++)
					{
						Node n = childNodes.item(j);
						
						if (n.getAttributes() != null &&
							n.getAttributes().getNamedItem("name").getNodeValue().equals(tier))
						{
							NodeList innerChildNodes = n.getChildNodes();
							n.getFirstChild();
							for (int k=0; k< innerChildNodes.getLength(); k++)
							{
								Node last = innerChildNodes.item(k);
								if (last.getAttributes() != null)
								{
									if (last.getAttributes().getNamedItem("location").getNodeValue().equals(region))
									{
										return Double.parseDouble(last.getAttributes().getNamedItem("inboundCost").getNodeValue());
									}
								}
							}
						}							
						
					}

				}
			}
		}catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return 0.0;
	}

	private double loadDbStorageCost(String region, String type) {

		try{
			
			File fXmlFile = new File(sourceFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			
			doc.getDocumentElement().normalize();
			
			NodeList nList = doc.getElementsByTagName("utilisedService");
			for (int i=0; i< nList.getLength(); i++)
			{
				Node nn = nList.item(i);
				if (nn.getAttributes().getNamedItem("name").getNodeValue().equals("DBaaS"))
				{
					NodeList childNodes = nList.item(i).getChildNodes();
					for (int j=0; j< childNodes.getLength(); j++)
					{
						Node n = childNodes.item(j);
						
						if (n.getAttributes() != null &&
							n.getAttributes().getNamedItem("name").getNodeValue().equals(type))
						{
							NodeList innerChildNodes = n.getChildNodes();
							n.getFirstChild();
							for (int k=0; k< innerChildNodes.getLength(); k++)
							{
								Node last = innerChildNodes.item(k);
								if (last.getAttributes() != null)
								{
									if (last.getAttributes().getNamedItem("location").getNodeValue().equals(region))
									{
										return Double.parseDouble(last.getAttributes().getNamedItem("costPerUnit").getNodeValue());
									}
								}
							}
						}							
						
					}

				}
			}
		}catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return 0.0;
	}

	private double loadDbCost(String dbInstName, String dbInstRegion, String dbInstTeir) {
		
		try{
			
			File fXmlFile = new File(sourceFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			
			doc.getDocumentElement().normalize();
			
			NodeList nList = doc.getElementsByTagName("utilisedService");
			for (int i=0; i< nList.getLength(); i++)
			{
				Node nn = nList.item(i);
				if (nn.getAttributes().getNamedItem("name").getNodeValue().equals("DBaaS"))
				{
					NodeList childNodes = nList.item(i).getChildNodes();
					for (int j=0; j< childNodes.getLength(); j++)
					{
						Node n = childNodes.item(j);
						
						if (n.getAttributes() != null &&
							n.getAttributes().getNamedItem("name").getNodeValue().equals(dbInstName))
						{
							NodeList innerChildNodes = n.getChildNodes();
							n.getFirstChild();
							for (int k=0; k< innerChildNodes.getLength(); k++)
							{
								Node last = innerChildNodes.item(k);
								if (last.getAttributes() != null)
								{
									if (last.getAttributes().getNamedItem("name").getNodeValue().equals(dbInstTeir) &&
										last.getAttributes().getNamedItem("location").getNodeValue().equals(dbInstRegion))
									{
										return Double.parseDouble(last.getAttributes().getNamedItem("costPerUnit").getNodeValue());
									}
								}
							}
						}							
						
					}

				}
			}
		}catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return 0.0;
		
	}

	public double getDatabaseInstanceCost() {
		return databaseInstanceCost;
	}

	public double getMigrationInstanceCost() {
		return migrationInstanceCost;
	}


	public double getVpnInstanceCost() {
		return vpnInstanceCost;
	}

	public double getDatabaseTransferCostPerMIOPS() {
		return databaseTransferCostPerMIOPS;
	}

	public double getDatabaseStorageCostPerGB() {
		return databaseStorageCostPerGB;
	}


}
