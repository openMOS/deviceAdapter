package eu.openmos.model;

import java.io.Serializable;
import java.util.Date;
import org.apache.log4j.Logger;
import org.bson.Document;

/**
 * A product can be made of parts.
 * 
 * @author Valerio Gentile <valerio.gentile@we-plus.eu>
 */
public class Part extends Base implements Serializable {
    private static final Logger logger = Logger.getLogger(Part.class.getName());
    private static final long serialVersionUID = 6529685098267757018L;

    /**
     * Part unique identifier.
     */
    protected String uniqueId;
    /**
     * Part name.
     */
    protected String name;
    /**
     * Part description.
     */
    protected String description;

    /**
     * Default constructor.
     */    
    public Part() {super();}

    /**
     * Parameterized constructor.
     * 
     * @param uniqueId
     * @param name
     * @param description
     * @param registeredTimestamp 
     */
    public Part(String uniqueId, String name, String description, Date registeredTimestamp) {
        super(registeredTimestamp);
        
        this.uniqueId = uniqueId;
        this.name = name;
        this.description = description;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

     /**
     * Method that serializes the object into a BSON document.
     * 
     * @return BSON form of the object. 
     */
    public Document toBSON() {
        return toBSON2();        
    }    
}
