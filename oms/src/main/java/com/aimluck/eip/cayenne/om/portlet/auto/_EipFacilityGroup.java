package com.aimluck.eip.cayenne.om.portlet.auto;

/** Class _EipFacilityGroup was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _EipFacilityGroup extends org.apache.cayenne.CayenneDataObject {

    public static final String EIP_MFACILITY_PROPERTY = "eipMFacility";
    public static final String TURBINE_GROUP_PROPERTY = "turbineGroup";

    public static final String ID_PK_COLUMN = "ID";

    public void setEipMFacility(com.aimluck.eip.cayenne.om.portlet.EipMFacility eipMFacility) {
        setToOneTarget("eipMFacility", eipMFacility, true);
    }

    public com.aimluck.eip.cayenne.om.portlet.EipMFacility getEipMFacility() {
        return (com.aimluck.eip.cayenne.om.portlet.EipMFacility)readProperty("eipMFacility");
    } 
    
    
    public void setTurbineGroup(com.aimluck.eip.cayenne.om.security.TurbineGroup turbineGroup) {
        setToOneTarget("turbineGroup", turbineGroup, true);
    }

    public com.aimluck.eip.cayenne.om.security.TurbineGroup getTurbineGroup() {
        return (com.aimluck.eip.cayenne.om.security.TurbineGroup)readProperty("turbineGroup");
    } 
    
    
}
