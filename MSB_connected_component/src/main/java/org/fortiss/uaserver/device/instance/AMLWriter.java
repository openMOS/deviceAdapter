package org.fortiss.uaserver.device.instance;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class AMLWriter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public AMLWriter(String recipe) {
        parseRecipeFromMsb(recipe);
    }

    public void parseRecipeFromMsb(String recipe) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder
                    .parse(new InputSource(new ByteArrayInputStream(recipe.getBytes("utf-8"))));
            NodeList nodeList = document.getElementsByTagName("recipe");
            for (int i = 0; i < nodeList.getLength(); i++) {
                try {
                    NodeList skillNodes = nodeList.item(i).getChildNodes();
                    for (int j = 0; j < skillNodes.getLength(); j++) {
                        Node e = skillNodes.item(j);
                        if (e.getNodeName().contains("name")) {
                            writeToAML(e.getChildNodes().item(0).getNodeValue());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (ParserConfigurationException | SAXException |
                IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void writeToAML(String recipeName) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse("amls/Glue.aml");
            NodeList nodeList = document.getElementsByTagName("InternalElement");
            if (nodeListContainsString(nodeList, recipeName) != null) {
                List<Node> nodes = nodeListContainsString(nodeList, recipeName);
                if (nodes.size() > 0) {
                    logger.info("found recipe {} in aml. should set new parameters", recipeName);

                    for (Node node : nodes) {
                        if (nodeListContainsString(node.getChildNodes(), "ParameterPort").size() > 0) {
                            for (Node n : nodeListContainsString(node.getChildNodes(), "ParameterPort")) {
                                 logger.info("from parameterport {}", n.getTextContent());
                            }
                        }
                    }
                } else {
                    logger.info("no recipe with name {} in aml. should create a new skill", recipeName);
                }
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private List<Node> nodeListContainsString(NodeList nodeList, String string) {
        List<Node> nodes = new ArrayList<Node>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            try {
                Node node = nodeList.item(i);
                if (node.getAttributes().getNamedItem("Name").toString().contains(string)) {
                    nodes.add(node);
                }
            } catch (NullPointerException e) {

            }
        }
        return nodes;
    }
}
