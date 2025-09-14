package com.dusk.module.auth.isc;

import com.sgcc.isc.service.*;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.xml.ws.Holder;
import jakarta.xml.ws.RequestWrapper;
import jakarta.xml.ws.ResponseWrapper;
import java.util.List;

/**
 * @author kefuming
 * @date 2021-11-18 15:09
 */
@WebService(name = "isc", targetNamespace = "http://www.sgcc.com/isc/service/")
public interface IscService {

    /**
     *
     * @param result
     * @param time
     * @param isDELETED
     * @param description
     * @param bizrolegroupID
     * @param _extends
     * @param updateDATE
     * @param bizrolegroupNAME
     */
    @WebMethod
    @RequestWrapper(localName = "syncBizRoleGroup", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncBizRoleGroup")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncBizRoleGroup(
            @WebParam(name = "BIZROLEGROUP_ID", targetNamespace = "") String bizrolegroupID,
            @WebParam(name = "BIZROLEGROUP_NAME", targetNamespace = "") String bizrolegroupNAME,
            @WebParam(name = "UPDATE_DATE", targetNamespace = "") String updateDATE,
            @WebParam(name = "IS_DELETED", targetNamespace = "") String isDELETED,
            @WebParam(name = "EXTENDS", targetNamespace = "") List<ExtendType> _extends,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);

    /**
     *
     * @param result
     * @param time
     * @param orgList
     * @param description
     */
    @WebMethod
    @RequestWrapper(localName = "syncBizOrganization", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncBizOrganization")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncBizOrganization(
            @WebParam(name = "OrgList", targetNamespace = "") List<BizOrgType> orgList,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);

    /**
     *
     * @param result
     * @param funcID
     * @param isLEAF
     * @param funcNAME
     * @param sortNO
     * @param parentFUNCID
     * @param _extends
     * @param updateDATE
     * @param funcINFO
     * @param isDELETED
     * @param time
     * @param funcURL
     * @param funcSTATUS
     * @param description
     */
    @WebMethod
    @RequestWrapper(localName = "syncFunction", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncFunction")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncFunction(
            @WebParam(name = "FUNC_ID", targetNamespace = "") String funcID,
            @WebParam(name = "FUNC_NAME", targetNamespace = "") String funcNAME,
            @WebParam(name = "FUNC_URL", targetNamespace = "") String funcURL,
            @WebParam(name = "PARENT_FUNC_ID", targetNamespace = "") String parentFUNCID,
            @WebParam(name = "FUNC_INFO", targetNamespace = "") String funcINFO,
            @WebParam(name = "SORT_NO", targetNamespace = "") String sortNO,
            @WebParam(name = "FUNC_STATUS", targetNamespace = "") String funcSTATUS,
            @WebParam(name = "IS_LEAF", targetNamespace = "") String isLEAF,
            @WebParam(name = "UPDATE_DATE", targetNamespace = "") String updateDATE,
            @WebParam(name = "IS_DELETED", targetNamespace = "") String isDELETED,
            @WebParam(name = "EXTENDS", targetNamespace = "") List<ExtendType> _extends,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);

    /**
     *
     * @param result
     * @param time
     * @param description
     * @param userList
     */
    @WebMethod
    @RequestWrapper(localName = "syncUser", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncUser")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncUser(
            @WebParam(name = "UserList", targetNamespace = "") List<UserType> userList,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);

    /**
     *
     * @param result
     * @param bizroleNAME
     * @param time
     * @param isDELETED
     * @param description
     * @param bizroleID
     * @param bizrolegroupID
     * @param _extends
     * @param updateDATE
     */
    @WebMethod
    @RequestWrapper(localName = "syncBizRole", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncBizRole")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncBizRole(
            @WebParam(name = "BIZROLE_ID", targetNamespace = "") String bizroleID,
            @WebParam(name = "BIZROLE_NAME", targetNamespace = "") String bizroleNAME,
            @WebParam(name = "BIZROLEGROUP_ID", targetNamespace = "") String bizrolegroupID,
            @WebParam(name = "UPDATE_DATE", targetNamespace = "") String updateDATE,
            @WebParam(name = "IS_DELETED", targetNamespace = "") String isDELETED,
            @WebParam(name = "EXTENDS", targetNamespace = "") List<ExtendType> _extends,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);

    /**
     *
     * @param result
     * @param time
     * @param isDELETED
     * @param datatypeID
     * @param datasetNAME
     * @param datasetCODE
     * @param datasetID
     * @param description
     * @param _extends
     * @param updateDATE
     */
    @WebMethod
    @RequestWrapper(localName = "syncDataSet", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncDataSet")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncDataSet(
            @WebParam(name = "DATASET_ID", targetNamespace = "") String datasetID,
            @WebParam(name = "DATASET_NAME", targetNamespace = "") String datasetNAME,
            @WebParam(name = "DATASET_CODE", targetNamespace = "") String datasetCODE,
            @WebParam(name = "DATATYPE_ID", targetNamespace = "") String datatypeID,
            @WebParam(name = "UPDATE_DATE", targetNamespace = "") String updateDATE,
            @WebParam(name = "IS_DELETED", targetNamespace = "") String isDELETED,
            @WebParam(name = "EXTENDS", targetNamespace = "") List<ExtendType> _extends,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);

    /**
     *
     * @param result
     * @param time
     * @param description
     * @param bizroleID
     * @param resID
     * @param _extends
     * @param updateDATE
     */
    @WebMethod
    @RequestWrapper(localName = "syncResOfBizRole", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResOfBizRole")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncResOfBizRole(
            @WebParam(name = "BIZROLE_ID", targetNamespace = "") String bizroleID,
            @WebParam(name = "RES_ID", targetNamespace = "") List<String> resID,
            @WebParam(name = "UPDATE_DATE", targetNamespace = "") String updateDATE,
            @WebParam(name = "EXTENDS", targetNamespace = "") List<ExtendType> _extends,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);

    /**
     *
     * @param result
     * @param time
     * @param isDELETED
     * @param funcID
     * @param description
     * @param sortNO
     * @param resCODE
     * @param resID
     * @param _extends
     * @param resINFO
     * @param resNAME
     * @param updateDATE
     */
    @WebMethod
    @RequestWrapper(localName = "syncResource", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResource")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncResource(
            @WebParam(name = "RES_ID", targetNamespace = "") String resID,
            @WebParam(name = "RES_NAME", targetNamespace = "") String resNAME,
            @WebParam(name = "RES_CODE", targetNamespace = "") String resCODE,
            @WebParam(name = "FUNC_ID", targetNamespace = "") String funcID,
            @WebParam(name = "RES_INFO", targetNamespace = "") String resINFO,
            @WebParam(name = "SORT_NO", targetNamespace = "") String sortNO,
            @WebParam(name = "UPDATE_DATE", targetNamespace = "") String updateDATE,
            @WebParam(name = "IS_DELETED", targetNamespace = "") String isDELETED,
            @WebParam(name = "EXTENDS", targetNamespace = "") List<ExtendType> _extends,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);

    /**
     *
     * @param result
     * @param time
     * @param isDELETED
     * @param bizorgroleNAME
     * @param description
     * @param bizroleID
     * @param bizorgroleID
     * @param _extends
     * @param updateDATE
     * @param bizorgID
     */
    @WebMethod
    @RequestWrapper(localName = "syncBizOrgRole", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncBizOrgRole")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncBizOrgRole(
            @WebParam(name = "BIZORGROLE_ID", targetNamespace = "") String bizorgroleID,
            @WebParam(name = "BIZORGROLE_NAME", targetNamespace = "") String bizorgroleNAME,
            @WebParam(name = "BIZROLE_ID", targetNamespace = "") String bizroleID,
            @WebParam(name = "BIZORG_ID", targetNamespace = "") String bizorgID,
            @WebParam(name = "UPDATE_DATE", targetNamespace = "") String updateDATE,
            @WebParam(name = "IS_DELETED", targetNamespace = "") String isDELETED,
            @WebParam(name = "EXTENDS", targetNamespace = "") List<ExtendType> _extends,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);

    /**
     *
     * @param result
     * @param userID
     * @param time
     * @param description
     * @param _extends
     * @param updateDATE
     * @param orgRoleOfUserList
     */
    @WebMethod
    @RequestWrapper(localName = "syncBizOrgRoleOfUser", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncBizOrgRoleOfUser")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncBizOrgRoleOfUser(
            @WebParam(name = "OrgRoleOfUserList", targetNamespace = "") List<BizOrgRoleOfUserType> orgRoleOfUserList,
            @WebParam(name = "USER_ID", targetNamespace = "") String userID,
            @WebParam(name = "UPDATE_DATE", targetNamespace = "") String updateDATE,
            @WebParam(name = "EXTENDS", targetNamespace = "") List<ExtendType> _extends,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);

    /**
     *
     * @param result
     * @param time
     * @param funcID
     * @param description
     * @param bizorgroleID
     * @param _extends
     * @param updateDATE
     */
    @WebMethod
    @RequestWrapper(localName = "syncFunctionOfOrgRole", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncFunctionOfOrgRole")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncFunctionOfOrgRole(
            @WebParam(name = "BIZORGROLE_ID", targetNamespace = "") String bizorgroleID,
            @WebParam(name = "FUNC_ID", targetNamespace = "") List<String> funcID,
            @WebParam(name = "UPDATE_DATE", targetNamespace = "") String updateDATE,
            @WebParam(name = "EXTENDS", targetNamespace = "") List<ExtendType> _extends,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);

    /**
     *
     * @param result
     * @param time
     * @param datasetID
     * @param description
     * @param bizorgroleID
     * @param _extends
     * @param updateDATE
     */
    @WebMethod
    @RequestWrapper(localName = "syncDataSetOfOrgRole", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncDataSetOfOrgRole")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncDataSetOfOrgRole(
            @WebParam(name = "BIZORGROLE_ID", targetNamespace = "") String bizorgroleID,
            @WebParam(name = "DATASET_ID", targetNamespace = "") List<String> datasetID,
            @WebParam(name = "UPDATE_DATE", targetNamespace = "") String updateDATE,
            @WebParam(name = "EXTENDS", targetNamespace = "") List<ExtendType> _extends,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);

    /**
     *
     * @param result
     * @param time
     * @param description
     * @param bizorgroleID
     * @param resID
     * @param _extends
     * @param updateDATE
     */
    @WebMethod
    @RequestWrapper(localName = "syncResOfOrgRole", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResOfOrgRole")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncResOfOrgRole(
            @WebParam(name = "BIZORGROLE_ID", targetNamespace = "") String bizorgroleID,
            @WebParam(name = "RES_ID", targetNamespace = "") List<String> resID,
            @WebParam(name = "UPDATE_DATE", targetNamespace = "") String updateDATE,
            @WebParam(name = "EXTENDS", targetNamespace = "") List<ExtendType> _extends,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);

    /**
     *
     * @param result
     * @param datatypeCODE
     * @param datatypeNAME
     * @param time
     * @param isDELETED
     * @param datatypeID
     * @param description
     * @param _extends
     * @param updateDATE
     */
    @WebMethod
    @RequestWrapper(localName = "syncDataType", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncDataType")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncDataType(
            @WebParam(name = "DATATYPE_ID", targetNamespace = "") String datatypeID,
            @WebParam(name = "DATATYPE_NAME", targetNamespace = "") String datatypeNAME,
            @WebParam(name = "DATATYPE_CODE", targetNamespace = "") String datatypeCODE,
            @WebParam(name = "UPDATE_DATE", targetNamespace = "") String updateDATE,
            @WebParam(name = "IS_DELETED", targetNamespace = "") String isDELETED,
            @WebParam(name = "EXTENDS", targetNamespace = "") List<ExtendType> _extends,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);

    /**
     *
     * @param result
     * @param time
     * @param funcID
     * @param description
     * @param bizroleID
     * @param _extends
     * @param updateDATE
     */
    @WebMethod
    @RequestWrapper(localName = "syncFunctionOfBizRole", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncFunctionOfBizRole")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncFunctionOfBizRole(
            @WebParam(name = "BIZROLE_ID", targetNamespace = "") String bizroleID,
            @WebParam(name = "FUNC_ID", targetNamespace = "") List<String> funcID,
            @WebParam(name = "UPDATE_DATE", targetNamespace = "") String updateDATE,
            @WebParam(name = "EXTENDS", targetNamespace = "") List<ExtendType> _extends,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);

    /**
     *
     * @param result
     * @param userID
     * @param time
     * @param description
     * @param bizroleID
     * @param _extends
     * @param updateDATE
     */
    @WebMethod
    @RequestWrapper(localName = "syncBizRoleOfUser", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncBizRoleOfUser")
    @ResponseWrapper(localName = "syncResponse", targetNamespace = "http://www.sgcc.com/isc/service/", className = "com.sgcc.isc.service.SyncResponse")
    void syncBizRoleOfUser(
            @WebParam(name = "USER_ID", targetNamespace = "") String userID,
            @WebParam(name = "BIZROLE_ID", targetNamespace = "") List<String> bizroleID,
            @WebParam(name = "UPDATE_DATE", targetNamespace = "") String updateDATE,
            @WebParam(name = "EXTENDS", targetNamespace = "") List<ExtendType> _extends,
            @WebParam(name = "result", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> result,
            @WebParam(name = "time", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> time,
            @WebParam(name = "description", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<String> description);
}
