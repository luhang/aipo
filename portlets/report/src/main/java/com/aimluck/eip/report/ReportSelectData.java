/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2011 Aimluck,Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aimluck.eip.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.jar.Attributes;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.EipTReport;
import com.aimluck.eip.cayenne.om.portlet.EipTReportFile;
import com.aimluck.eip.cayenne.om.portlet.EipTReportMap;
import com.aimluck.eip.cayenne.om.portlet.EipTReportMemberMap;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALData;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.fileupload.beans.FileuploadBean;
import com.aimluck.eip.fileupload.util.FileuploadUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.report.util.ReportUtils;
import com.aimluck.eip.util.ALEipUtils;

/**
 * 報告書検索データを管理するクラスです。 <BR>
 * 
 */
public class ReportSelectData extends
    ALAbstractSelectData<EipTReport, EipTReport> implements ALData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ReportSelectData.class.getName());

  /** サブメニュー（送信） */
  public static final String SUBMENU_CREATED = "created";

  /** サブメニュー（受信） */
  public static final String SUBMENU_REQUESTED = "requested";

  /** サブメニュー（全て） */
  public static final String SUBMENU_ALL = "all";

  /** 現在選択されているサブメニュー */
  private String currentSubMenu;

  private ALEipUser login_user;

  /** 他ユーザーの報告書の閲覧権限 */
  private boolean hasAuthorityOther;

  /**
   * 
   * @param action
   * @param rundata
   * @param context
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {

    // if (ReportUtils.hasResetFlag(rundata, context)) {
    // ReportUtils.clearReportSession(rundata, context);
    // }
    login_user = ALEipUtils.getALEipUser(rundata);

    String subMenuParam = rundata.getParameters().getString("submenu");
    currentSubMenu = ALEipUtils.getTemp(rundata, context, "submenu");
    if (subMenuParam == null && currentSubMenu == null) {
      ALEipUtils.setTemp(rundata, context, "submenu", SUBMENU_REQUESTED);
      currentSubMenu = SUBMENU_REQUESTED;
    } else if (subMenuParam != null) {
      ALEipUtils.setTemp(rundata, context, "submenu", subMenuParam);
      currentSubMenu = subMenuParam;
    }

    String sort = ALEipUtils.getTemp(rundata, context, LIST_SORT_STR);
    String sorttype = ALEipUtils.getTemp(rundata, context, LIST_SORT_TYPE_STR);

    if (sort == null || sort.equals("")) {
      ALEipUtils.setTemp(rundata, context, LIST_SORT_STR, "create_date");
    }

    if ("create_date".equals(ALEipUtils
      .getTemp(rundata, context, LIST_SORT_STR))
      && (sorttype == null || "".equals(sorttype))) {
      ALEipUtils.setTemp(
        rundata,
        context,
        LIST_SORT_TYPE_STR,
        ALEipConstants.LIST_SORT_TYPE_DESC);
    }

    // アクセス権限
    /*
     * ALAccessControlFactoryService aclservice =
     * (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
     * .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
     * ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();
     * hasAuthorityOther = aclhandler.hasAuthority(
     * ALEipUtils.getUserId(rundata),
     * ALAccessControlConstants.POERTLET_FEATURE_REPORT_OTHER,
     * ALAccessControlConstants.VALUE_ACL_LIST);
     */
    hasAuthorityOther = true;

    super.init(action, rundata, context);

  }

  /**
   * 一覧データを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  @Override
  public ResultList<EipTReport> selectList(RunData rundata, Context context) {
    try {

      SelectQuery<EipTReport> query = getSelectQuery(rundata, context);
      buildSelectQueryForFilter(query, rundata, context);
      buildSelectQueryForListView(query);
      buildSelectQueryForListViewSort(query, rundata, context);

      ResultList<EipTReport> list = query.getResultList();
      return list;
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * 検索条件を設定した SelectQuery を返します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  private SelectQuery<EipTReport> getSelectQuery(RunData rundata,
      Context context) {

    SelectQuery<EipTReport> query = Database.query(EipTReport.class);

    Integer login_user_id =
      Integer.valueOf((int) login_user.getUserId().getValue());

    if (ALEipUtils.getTemp(rundata, context, "Report_Maximize") == "false") {
      // 通常画面
      // 受信したもので未読
      SelectQuery<EipTReportMap> q = Database.query(EipTReportMap.class);
      Expression exp1 =
        ExpressionFactory.matchExp(
          EipTReportMap.USER_ID_PROPERTY,
          login_user_id);
      q.andQualifier(exp1);
      Expression exp2 =
        ExpressionFactory.matchExp(
          EipTReportMap.STATUS_PROPERTY,
          ReportUtils.DB_STATUS_UNREAD);
      q.andQualifier(exp2);
      List<EipTReportMap> queryList = q.fetchList();

      List<Integer> resultid = new ArrayList<Integer>();
      for (EipTReportMap item : queryList) {
        if (item.getReportId() != 0 && !resultid.contains(item.getReportId())) {
          resultid.add(item.getReportId());
        } else if (!resultid.contains(item.getReportId())) {
          resultid.add(item.getReportId());
        }
      }
      if (resultid.size() == 0) {
        // 検索結果がないことを示すために-1を代入
        resultid.add(-1);
      }
      Expression ex =
        ExpressionFactory.inDbExp(EipTReport.REPORT_ID_PK_COLUMN, resultid);
      query.andQualifier(ex);

    } else if (SUBMENU_CREATED.equals(currentSubMenu)) {
      // 送信
      Expression exp1 =
        ExpressionFactory.matchExp(EipTReport.USER_ID_PROPERTY, login_user_id);
      query.setQualifier(exp1);
    } else if (SUBMENU_REQUESTED.equals(currentSubMenu)) {
      // 受信
      SelectQuery<EipTReportMap> q = Database.query(EipTReportMap.class);
      Expression exp1 =
        ExpressionFactory.matchExp(
          EipTReportMap.USER_ID_PROPERTY,
          login_user_id);
      q.andQualifier(exp1);
      List<EipTReportMap> queryList = q.fetchList();

      List<Integer> resultid = new ArrayList<Integer>();
      for (EipTReportMap item : queryList) {
        if (item.getReportId() != 0 && !resultid.contains(item.getReportId())) {
          resultid.add(item.getReportId());
        } else if (!resultid.contains(item.getReportId())) {
          resultid.add(item.getReportId());
        }
      }
      if (resultid.size() == 0) {
        // 検索結果がないことを示すために-1を代入
        resultid.add(-1);
      }
      Expression ex =
        ExpressionFactory.inDbExp(EipTReport.REPORT_ID_PK_COLUMN, resultid);
      query.andQualifier(ex);
    } else if (SUBMENU_ALL.equals(currentSubMenu)) {
      // 全て
    }

    return query;
  }

  /**
   * ResultData に値を格納して返します。（一覧データ） <BR>
   * 
   * @param obj
   * @return
   */
  @Override
  protected Object getResultData(EipTReport record) {
    try {
      ReportResultData rd = new ReportResultData();
      rd.initField();
      rd.setReportId(record.getReportId().intValue());
      rd.setReportName(record.getReportName());
      rd.setUpdateDateTime(record.getUpdateDate());

      rd.setCreateDateTime(record.getCreateDate());
      ALEipUser client = ALEipUtils.getALEipUser(record.getUserId().intValue());
      rd.setClientName(client.getAliasName().getValue());
      // 自身の報告書かを設定する
      Integer login_user_id =
        Integer.valueOf((int) login_user.getUserId().getValue());
      rd.setIsSelfReport(record.getUserId() == login_user_id);
      return rd;
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * 詳細データを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  @Override
  public EipTReport selectDetail(RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    EipTReport request = ReportUtils.getEipTReport(rundata, context);

    return request;
  }

  /**
   * ResultData に値を格納して返します。（詳細データ） <BR>
   * 
   * @param obj
   * @return
   */
  @Override
  protected Object getResultDataDetail(EipTReport obj)
      throws ALPageNotFoundException, ALDBErrorException {

    try {

      EipTReport record = obj;
      ReportDetailResultData rd = new ReportDetailResultData();
      rd.initField();
      rd.setUserId(record.getUserId().longValue());
      rd.setReportName(record.getReportName());
      rd.setReportId(record.getReportId().longValue());
      rd.setNote(record.getNote());
      ALEipUser client = ALEipUtils.getALEipUser(record.getUserId().intValue());
      rd.setClientName(client.getAliasName().getValue());
      // 自身の報告書かを設定する
      Integer login_user_id =
        Integer.valueOf((int) login_user.getUserId().getValue());
      rd.setIsSelfReport(record.getUserId().intValue() == login_user_id);

      List<Integer> users = new ArrayList<Integer>();
      EipTReportMap map = null;
      List<EipTReportMap> tmp_maps = ReportUtils.getEipTReportMap(record);
      HashMap<Integer, String> statusList = new HashMap<Integer, String>();
      int size = tmp_maps.size();
      for (int i = 0; i < size; i++) {
        map = tmp_maps.get(i);
        users.add(map.getUserId());
        if (map.getUserId().intValue() == login_user_id) {
          // 既読に変更する
          map.setStatus(ReportUtils.DB_STATUS_READ);
          Database.commit();
        }
        statusList.put(map.getUserId(), map.getStatus());
      }
      rd.setStatusList(statusList);
      SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
      Expression exp =
        ExpressionFactory.inDbExp(TurbineUser.USER_ID_PK_COLUMN, users);
      query.setQualifier(exp);
      rd.setMapList(ALEipUtils.getUsersFromSelectQuery(query));

      List<Integer> users1 = new ArrayList<Integer>();
      EipTReportMemberMap map1 = null;
      List<EipTReportMemberMap> tmp_maps1 =
        ReportUtils.getEipTReportMemberMap(record);
      int size1 = tmp_maps1.size();
      for (int i = 0; i < size1; i++) {
        map1 = tmp_maps1.get(i);
        users1.add(map1.getUserId());
      }
      SelectQuery<TurbineUser> query1 = Database.query(TurbineUser.class);
      Expression exp1 =
        ExpressionFactory.inDbExp(TurbineUser.USER_ID_PK_COLUMN, users1);
      query1.setQualifier(exp1);
      rd.setMemberList(ALEipUtils.getUsersFromSelectQuery(query1));

      // ファイルリスト
      List<EipTReportFile> list =
        ReportUtils
          .getSelectQueryForFiles(record.getReportId().intValue())
          .fetchList();
      if (list != null && list.size() > 0) {
        List<FileuploadBean> attachmentFileList =
          new ArrayList<FileuploadBean>();
        FileuploadBean filebean = null;
        for (EipTReportFile file : list) {
          String realname = file.getFileName();
          javax.activation.DataHandler hData =
            new javax.activation.DataHandler(
              new javax.activation.FileDataSource(realname));

          filebean = new FileuploadBean();
          filebean.setFileId(file.getFileId().intValue());
          filebean.setFileName(realname);
          if (hData != null) {
            filebean.setContentType(hData.getContentType());
          }
          filebean.setIsImage(FileuploadUtils.isImage(realname));
          attachmentFileList.add(filebean);
        }
        rd.setAttachmentFiles(attachmentFileList);
      }

      rd.setCreateDate(ReportUtils.translateDate(
        record.getCreateDate(),
        "yyyy年M月d日H時m分"));
      rd.setUpdateDate(ReportUtils.translateDate(
        record.getUpdateDate(),
        "yyyy年M月d日H時m分"));

      return rd;
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * @return
   * 
   */
  @Override
  protected Attributes getColumnMap() {
    Attributes map = new Attributes();
    map.putValue("report_name", EipTReport.REPORT_NAME_PROPERTY);
    map.putValue("create_date", EipTReport.CREATE_DATE_PROPERTY);
    map.putValue("user_id", EipTReport.USER_ID_PROPERTY);
    return map;
  }

  /**
   * 現在選択されているサブメニューを取得します。 <BR>
   * 
   * @return
   */
  public String getCurrentSubMenu() {
    return this.currentSubMenu;
  }

  public ALEipUser getLoginUser() {
    return login_user;
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   * 
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    // return ALAccessControlConstants.POERTLET_FEATURE_REPORT_SELF;
    return null;
  }

  public boolean hasAuthorityOther() {
    return hasAuthorityOther;
  }
}