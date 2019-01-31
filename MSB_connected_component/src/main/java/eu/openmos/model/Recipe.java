package eu.openmos.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.bson.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;

/**
 * Object that describe a recipe of the system.
 * 
 * @author Pedro Lima Monteiro <pedro.monteiro@uninova.pt>
 * @author Luis Ribeiro
 * @author Valerio Gentile <valerio.gentile@we-plus.eu>
 */
@XmlRootElement(name = "recipe")
// @XmlAccessorType(XmlAccessType.FIELD)
public class Recipe extends Base implements Serializable {
	private static final Logger logger = Logger.getLogger(Recipe.class.getName());
	private static final long serialVersionUID = 6529685098267757025L;

	/**
	 * Recipe ID.
	 */
	// @XmlElement(name = "amlId")
	private String uniqueId;
	/**
	 * Recipe name.
	 */
	// @XmlElement(name = "name")
	private String name;
	/**
	 * Recipe description.
	 */
	// @XmlElement(name = "description")
	private String description;
	/**
	 * Whether the recipe is valid or not.
	 */
	// @XmlElement(name = "valid")
	private boolean valid = false;
	/**
	 * Recipe's parameter settings. These must match the skill's parameters.
	 */
	private List<ParameterSetting> parameterSettings;
	/**
	 * The skills necessary to execute this recipe.
	 */
	// @XmlElement(name = "skillRequirements")
	private List<SkillRequirement> skillRequirements;
	/**
	 * Recipe's KPI Settings. These must match the skill's KPIs.
	 */
	private List<KPISetting> kpiSettings;

	/**
	 * Pointer to the skill.
	 */
	// @XmlElement(name = "skill")
	private Skill skill;
	private ControlPort executedBySkillControlPort;

	/**
	 * The agent capable of executing this recipe.
	 * 
	 * MSB alignment. on the msb side they have the adapter id, and on the adapter
	 * class they have the agent id
	 */
	private String uniqueAgentName;
	/**
	 * Recipe's equipment.
	 */
	// private Equipment equipment;
	// private SubSystem equipment;
	// private String equipmentId;
	private List<String> equipmentIds;

	/**
	 * Whether the recipe is optimized or not.
	 */
	private boolean optimized = false;
	/**
	 * Last optimization timestamp.
	 */
	private Date lastOptimizationTime;

	/**
	 * MSB alignment.
	 */
	private String msbProtocolEndpoint;

	private String invokeObjectID;

	private String invokeMethodID;

	private transient String changeRecipeObjectID;

	private transient String changeRecipeMethodID;

	private String state;

	private transient String statePath;

	/**
	 * Default constructor, for reflection
	 */
	public Recipe() {
		super();
	}

	/**
	 * Parameterized constructor.
	 * 
	 * @param description
	 *            - Recipe's description.
	 * @param uniqueId
	 *            - Recipe's ID.
	 * @param kpisSetting
	 *            - Recipe's KPI Settings. These must match the skill's KPIs.
	 * @param name
	 *            - Recipe's name.
	 * @param parameterSettings
	 *            - Recipe's parameter settings. These must match the skill's
	 *            parameters.
	 * @param uniqueAgentName
	 *            - The agent capable of executing this recipe.
	 * @param skillRequirements
	 *            - The skills necessary to execute this recipe.
	 * @param skill
	 * @param equipment
	 * @param msbProtocolEndpoint,
	 * @param registeredTimestamp
	 */
	public Recipe(String description, String uniqueId, List<KPISetting> kpiSettings, String name,
			List<ParameterSetting> parameterSettings, String uniqueAgentName, List<SkillRequirement> skillRequirements,
			Skill skill, ControlPort executedBySkillControlPort,
			// Equipment equipment,
			// SubSystem equipment,
			// String equipmentId,
			List<String> equipmentIds, String msbProtocolEndpoint, boolean valid, Date registeredTimestamp) {
		super(registeredTimestamp);

		this.description = description;
		this.uniqueId = uniqueId;
		this.kpiSettings = kpiSettings;
		this.name = name;
		this.parameterSettings = parameterSettings;
		this.uniqueAgentName = uniqueAgentName;
		this.skillRequirements = skillRequirements;

		this.skill = skill;
		this.executedBySkillControlPort = executedBySkillControlPort;

		// this.equipment = equipment;
		// this.equipmentId = equipmentId;
		this.equipmentIds = equipmentIds;

		this.msbProtocolEndpoint = msbProtocolEndpoint;
		this.valid = valid;
	}

	public List<SkillRequirement> getSkillRequirements() {
		return skillRequirements;
	}

	public void setSkillRequirements(List<SkillRequirement> skillRequirements) {
		this.skillRequirements = skillRequirements;
	}

	public String getUniqueAgentName() {
		return uniqueAgentName;
	}

	public void setUniqueAgentName(String uniqueAgentName) {
		this.uniqueAgentName = uniqueAgentName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public List<KPISetting> getKpiSettings() {
		return kpiSettings;
	}

	public void setKpiSettings(List<KPISetting> kpiSettings) {
		this.kpiSettings = kpiSettings;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getChangeRecipeObjectID() {
		return changeRecipeObjectID;
	}

	public void setChangeRecipeObjectID(String changeRecipeObjectID) {
		this.changeRecipeObjectID = changeRecipeObjectID;
	}

	public String getChangeRecipeMethodID() {
		return changeRecipeMethodID;
	}

	public void setChangeRecipeMethodID(String changeRecipeMethodID) {
		this.changeRecipeMethodID = changeRecipeMethodID;
	}

	public List<ParameterSetting> getParameterSettings() {
		return parameterSettings;
	}

	public void setParameterSettings(List<ParameterSetting> parameterSettings) {
		this.parameterSettings = parameterSettings;
	}

	public Skill getSkill() {
		return skill;
	}

	public void setSkill(Skill skill) {
		this.skill = skill;
	}

	public List<String> getEquipmentIds() {
		return equipmentIds;
	}

	/*
	 * public Equipment getEquipment() { return equipment; } public void
	 * setEquipment(Equipment equipment) { this.equipment = equipment; }
	 */
	/*
	 * public SubSystem getEquipment() { return equipment; } public void
	 * setEquipment(SubSystem equipment) { this.equipment = equipment; }
	 */
	// public String getEquipmentId() {
	// return equipmentId;
	// }
	//
	// public void setEquipmentId(String equipmentId) {
	// this.equipmentId = equipmentId;
	// }
	public void setEquipmentIds(List<String> equipmentIds) {
		this.equipmentIds = equipmentIds;
	}

	public boolean isOptimized() {
		return optimized;
	}

	public void setOptimized(boolean optimized) {
		this.optimized = optimized;
	}

	public Date getLastOptimizationTime() {
		return lastOptimizationTime;
	}

	public void setLastOptimizationTime(Date lastOptimizationTimestamp) {
		this.lastOptimizationTime = lastOptimizationTimestamp;
	}

	public String getMsbProtocolEndpoint() {
		return msbProtocolEndpoint;
	}

	public void setMsbProtocolEndpoint(String msbProtocolEndpoint) {
		this.msbProtocolEndpoint = msbProtocolEndpoint;
	}

	public String getInvokeObjectID() {
		return invokeObjectID;
	}

	public void setInvokeObjectID(String invokeObjectID) {
		this.invokeObjectID = invokeObjectID;
	}

	public String getInvokeMethodID() {
		return invokeMethodID;
	}

	public void setInvokeMethodID(String invokeMethodID) {
		this.invokeMethodID = invokeMethodID;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public ControlPort getExecutedBySkillControlPort() {
		return executedBySkillControlPort;
	}

	public void setExecutedBySkillControlPort(ControlPort executedBySkillControlPort) {
		this.executedBySkillControlPort = executedBySkillControlPort;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getStatePath() {
		return statePath;
	}

	public void setStatePath(String statePath) {
		this.statePath = statePath;
	}

	/**
	 * Method that serializes the object into a BSON document.
	 * 
	 * @return BSON form of the object.
	 */
	public Document toBSON() {
		Document doc = new Document();

		List<String> kpiSettingIds = null;
		if (kpiSettings != null)
			kpiSettingIds = kpiSettings.stream().map(kpiSetting -> kpiSetting.getUniqueId())
					.collect(Collectors.toList());

		List<String> parameterSettingIds = null;
		if (parameterSettings != null)
			parameterSettingIds = parameterSettings.stream().map(parameterSetting -> parameterSetting.getUniqueId())
					.collect(Collectors.toList());

		List<String> skillRequirementIds = null;
		if (skillRequirements != null)
			skillRequirementIds = skillRequirements.stream().map(skillRequirement -> skillRequirement.getName())
					.collect(Collectors.toList());

		String skillId = null;
		if (skill != null)
			skillId = skill.getUniqueId();

		String controlPortId = null;
		if (executedBySkillControlPort != null)
			controlPortId = executedBySkillControlPort.getUniqueId();

		SimpleDateFormat sdf = new SimpleDateFormat(SerializationConstants.DATE_REPRESENTATION);

		doc.append(DatabaseConstants.DESCRIPTION, description);
		doc.append(DatabaseConstants.UNIQUE_ID, uniqueId);
		doc.append(DatabaseConstants.KPI_SETTING_IDS, kpiSettingIds);
		doc.append(DatabaseConstants.NAME, name);
		doc.append(DatabaseConstants.PARAMETER_SETTING_IDS, parameterSettingIds);
		doc.append(DatabaseConstants.UNIQUE_AGENT_NAME, uniqueAgentName);
		doc.append(DatabaseConstants.SKILL_REQUIREMENT_IDS, skillRequirementIds);
		doc.append(DatabaseConstants.SKILL_ID, skillId);
		doc.append(DatabaseConstants.EXECUTED_BY_SKILL_CONTROL_PORT_ID, controlPortId);
		// doc.append(DatabaseConstants.EQUIPMENT_ID, equipmentId);
		doc.append(DatabaseConstants.EQUIPMENT_IDS, equipmentIds);
		doc.append(DatabaseConstants.OPTIMIZED, optimized);
		doc.append(DatabaseConstants.OPTIMIZED_TIME,
				this.lastOptimizationTime == null ? "null" : sdf.format(this.lastOptimizationTime));
		doc.append(DatabaseConstants.MSB_PROTOCOL_ENDPOINT, msbProtocolEndpoint);

		doc.append(DatabaseConstants.VALID, valid);

		doc.append(DatabaseConstants.REGISTERED, this.registered == null ? "null" : sdf.format(this.registered));

		return doc;
	}

	private static Object StringToClass(String stringClass, Object classToParse) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;

		try {
			db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(stringClass));

			org.w3c.dom.Document doc = db.parse(is);
			JAXBContext jc = JAXBContext.newInstance(classToParse.getClass());
			Unmarshaller unmar = jc.createUnmarshaller();
			classToParse = (Object) unmar.unmarshal(doc);

			return classToParse;
		} catch (IOException | JAXBException | ParserConfigurationException | SAXException ex) {
			// handle ParserConfigurationException
			System.out.println("Error parsing stringToClass: " + ex);
		}
		return null;
	}

	// EXAMPLE string creation
	private static String createString() throws JAXBException {
		SkillRequirement SR = new SkillRequirement();
		SR.setUniqueId("SR_ID");
		SR.setName("SR_NAME");
		List<SkillRequirement> SR_List = new ArrayList<>();
		SR_List.add(SR);
		Skill skill = new Skill();
		skill.setUniqueId("skill_ID");
		skill.setName("skill_NAME");
		List<Parameter> pars = new ArrayList();
		pars.add(new Parameter("default value", "description", "uid", "lowerBound", "upperBound", "name", "seconds", "type", "valueType", new Date()));
		skill.setParameters(pars);
		Recipe recipe = new Recipe();
		recipe.setUniqueId("recipe_ID");
		recipe.setName("recipe_NAME");
		recipe.setSkill(skill);
		recipe.setSkillRequirements(SR_List);

		StringWriter sw = new StringWriter();
		javax.xml.bind.JAXBContext jc = JAXBContext.newInstance(Recipe.class);
		Marshaller jaxbMArshaller = jc.createMarshaller();
		jaxbMArshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		jaxbMArshaller.marshal(recipe, sw); // print to String
		// String excTablesString = XMLtoString("updateExecTables.xml"); //TODO: use
		// outputStream instead of file!
		String recipeString = sw.toString();
		return recipeString;
	}

	public String toString() {
		String s = "";
		try {
			s = createString();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return s;
		//Recipe recipe = (Recipe) StringToClass(marsh, new Recipe());
	}

	
}
