package custom.app.inventory;

import psdi.mbo.MboSetRemote;
import java.rmi.RemoteException;
import java.lang.String;
import psdi.mbo.MboSet;
import psdi.app.inventory.MatUseTrans;
import psdi.util.MXException;
import psdi.mbo.MboRemote;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;
import psdi.util.MXApplicationException;
import psdi.security.ConnectionKey;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import psdi.server.MXServer;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.*;

public class MatUseTransExt extends MatUseTrans implements MatUseTransExtRemote {

	private MXLogger logger = MXLoggerFactory.getLogger("maximo.service.INVENTORY");
	public MatUseTransExt(MboSet mboSet0) throws MXException, RemoteException {
		super(mboSet0);
	}

	public void init()
	        throws MXException
	    {
	        super.init();
	    }


	public void save()
    throws MXException, RemoteException
{	
    String refwo = getString("refwo");
    String siteID = getString("siteid");
    String projectNum=getProjectNumber(refwo,siteID);
    String taskNum=getTaskNumber(refwo,siteID);
    String expType="";
    String costCenter="";
    journalProcessing(projectNum, taskNum, expType, costCenter,siteID);
    super.save();
}


	private void journalProcessing(String projectNum, String taskNum, String expType, String costCenter, String siteid)
			throws RemoteException, MXException {
		// Journal Processing

		long externalRefID = 0;
		String apiSeq = "";

		String storeLoc = "";
		String sStoreloc = "";
		storeLoc = getString("STORELOC");
		sStoreloc = getString("STORELOC");
		String type = "";
		String storetype = "";
		String externalRefid = "";
		String ponum = "";
		externalRefid = getString("EXTERNALREFID");
		ponum = getString("PONUM");
		MboSetRemote locationSet = getMboSet("PLUSTSTOREROOM");
		String siteID = getString("siteid");
		String sGlDebit = getString("GLDEBITACCT");
		String sSite = getString("siteid");
		String sIssueType = getString("ISSUETYPE");
		String sToSiteID = getString("TOSITEID");
		String sRefWO = getString("REFWO");
		String sApiSeqcheck = getString("APISEQ");
		String sPONum = getString("PONUM");

		if (sApiSeqcheck.length()>0  || sPONum.length()>0 ) {
			return;
		}
		
		if (sSite.equals("KAF") || sSite.equals("COMM") || sSite.equals("LMD")) {
			/*if (!storeLoc.equals(""))
				if (!locationSet.isEmpty()) {
					MboRemote locationMbo = locationSet.getMbo(0);
					//type = locationMbo.getString("TYPE");
					//storetype = locationMbo.getString("STORETYPE");
				}*/

			if (sStoreloc != null) {
				// kafco chnages add KAF site
				if (!((sSite.equals("LMD") && sToSiteID.equals("LMD") && sStoreloc.equals("LMD_REFURB")
						&& (sGlDebit.substring(16, 18)).equals("21") && sIssueType.equals("ISSUE"))
						|| (sSite.equals("LMD") && sToSiteID.equals("LMD") && sStoreloc.equals("LMD_REFURB")
								&& sGlDebit.substring(16, 18).equals("21") && sIssueType.equals("RETURN"))
						|| (sSite.equals("COMM")) || (sSite.equals("KAF")))) {
					return;
				}

				if (sStoreloc.contains("CONSIGN")) {

					return;

				}
			}

			if (getString("GLDEBITACCT").equals(getString("GLCREDITACCT"))) {
				return;
			}
			
			BigDecimal bdZero = new BigDecimal(0);
			BigDecimal bdCost;
			bdCost = new BigDecimal(getDouble("LINECOST") ); 
			if (bdCost.compareTo(bdZero) == 0)
				return;


				if (externalRefid.equals("") && ponum.equals("")) {

					//TransactionHelper tH = new TransactionHelper(this);
					//JournalsHelper jUtil = new JournalsHelper(this);

					try {

						ConnectionKey conKey = new ConnectionKey(getUserInfo());
						MXServer mxserver = MXServer.getMXServer();
						Connection con = mxserver.getDBManager().getConnection(conKey);
						Statement stat = con.createStatement();
						ResultSet rs = stat.executeQuery("select apiseq.nextval as EXTREFID from dual");
						while (rs.next()) {
							externalRefID = rs.getLong("EXTREFID");
						}
						mxserver.getDBManager().freeConnection(conKey);
						mxserver = null;

						apiSeq = String.valueOf(externalRefID);

						//TransactionInfo transaction = tH.createIssueTransaction(this, externalRefID, projectNum,
								//taskNum, expType, costCenter);
						//jUtil.setTransaction(transaction);
						//jUtil.prepareJournal(this,getString("siteid"),getString("orgid"));
						prepareJournal(externalRefID) ;
						/*
						if(sRefWO!=null)// If Work Order Exists then jump to Processing
						populatePA(externalRefID);
						*/
						if (apiSeq != null) {
							setValue("EXTERNALREFID", apiSeq, 11L);
							setValue("ITIN7", apiSeq, 11L);

						}
					} catch (MXException e) {
						MXLoggerFactory.getLogger("maximo.custom").error(e.getMessage(), e);
						Object[] params = { e.getMessage() };
						throw new MXApplicationException("journal", e.getMessage());
					} catch (Exception e) {

						MXLoggerFactory.getLogger("maximo.custom").error(e.getMessage(), e);
						Object[] params = { e.getMessage() };
						throw new MXApplicationException("journal", e.getMessage());
					}
				} else
					logger.debug("********Entering MatusetransExt:> no Journals to process");

		}
	}
	
	public void  prepareJournal(long externalRefID) throws Exception {
		System.out.println("*************** 1 *********************");
		MboSetRemote ixGLInterfaceRecordSet = getMboSet("ixGLInterfaceRecord$","K_GL_INTERFACE","1=2");
		MboRemote ixGLInterfaceRecord = ixGLInterfaceRecordSet.add(2L);
		ixGLInterfaceRecord.setValue("STATUS", "NEW",11L);
		
		MboSetRemote ixGLInterfaceRecord2Set = getMboSet("ixGLInterfaceRecord2$","K_GL_INTERFACE","1=2");
		MboRemote ixGLInterfaceRecord2 = ixGLInterfaceRecord2Set.add(2L);
		ixGLInterfaceRecord2.setValue("STATUS", "NEW",11L);
		
		System.out.println("*************** 2 *********************");
		String sCreditID;
		String sDebitID;
		String sMaximoTransactionType;
		String sDebitAccount = getString("GLDEBITACCT");
		String sCreditAccount = getString("GLCREDITACCT");
		String sMaximoOrg = getString("ORGID");
		Date tTransactionDate = getDate("TRANSDATE");
		String sCurrency = getString("currencycode");		
		sDebitID = getAccountID(sMaximoOrg, sDebitAccount);
		sCreditID = getAccountID(	sMaximoOrg, sCreditAccount);		
		String sValueList = "ISSUETYP";
	    String sTransactionType = getString("ISSUETYPE");
	    String sIssueType = getString("ISSUETYPE");;
	    String sFromStore = "";
	    Date tActualDate = getDate("TRANSDATE");	    
		sMaximoTransactionType = getMaxValue(sValueList, sTransactionType);
        String sTableName = "MATUSETRANS";
        BigDecimal bdExchangeRate =  new BigDecimal(getDouble("EXCHANGERATE") );
        BigDecimal bdBaseAmount = new BigDecimal(getDouble("LINECOST")) ;
        String sDescription = FormatDescription(getString("DESCRIPTION"),240);
		String sReference2 = getMaximoTransactionType(sIssueType, sFromStore);
		String sReference3 = getString("MATUSETRANSID");
        String sReference4 = "";
        String sReference6 = getString("REFWO");
        String sReference7 = "";
        String sReference8 = getString("SITEID");
        String sReference9="";
        String sContractRefNum = null;
        String sRefWO = getString("REFWO");
        System.out.println("*************** 3 *********************");
		// KFCO changes
				if (sReference8.equalsIgnoreCase("KAF")) {
					ixGLInterfaceRecord.setValue("SET_OF_BOOKS_ID", "83", 11L);
					ixGLInterfaceRecord2.setValue("SET_OF_BOOKS_ID", "83", 11L);
					ixGLInterfaceRecord.setValue("LEDGER_ID", "83" ,11L);
					ixGLInterfaceRecord2.setValue("LEDGER_ID", "83" ,11L);
				} else {
					ixGLInterfaceRecord.setValue("SET_OF_BOOKS_ID","2", 11L); //(sMaximoOrg + "_SOBID")
					ixGLInterfaceRecord2.setValue("SET_OF_BOOKS_ID","2", 11L);
					ixGLInterfaceRecord.setValue("LEDGER_ID","2", 11L);
					ixGLInterfaceRecord2.setValue("LEDGER_ID","2", 11L);
				}

		ixGLInterfaceRecord.setValue("ACCOUNTING_DATE", tTransactionDate, 11L);
		ixGLInterfaceRecord2.setValue("ACCOUNTING_DATE", tTransactionDate, 11L);
		ixGLInterfaceRecord.setValue("CURRENCY_CODE", sCurrency, 11L);	
		ixGLInterfaceRecord2.setValue("CURRENCY_CODE", sCurrency, 11L);	
		ixGLInterfaceRecord.setValue("DATE_CREATED",MXServer.getMXServer().getDate(), 11L);
		ixGLInterfaceRecord2.setValue("DATE_CREATED",MXServer.getMXServer().getDate(), 11L);
		ixGLInterfaceRecord.setValue("CREATED_BY", "1", 11L);
		ixGLInterfaceRecord2.setValue("CREATED_BY", "1", 11L);
		ixGLInterfaceRecord.setValue("ACTUAL_FLAG", "A", 11L);		
		ixGLInterfaceRecord2.setValue("ACTUAL_FLAG", "A", 11L);		
		if (sReference2.equals("Inventory Receipt") 	&& sMaximoTransactionType.equals("TRANSFER")) {
			ixGLInterfaceRecord.setValue("USER_JE_CATEGORY_NAME",	OFSetup(sTableName + "_RECEIPT_CATEGORY"), 11L);
			ixGLInterfaceRecord2.setValue("USER_JE_CATEGORY_NAME",	OFSetup(sTableName + "_RECEIPT_CATEGORY"), 11L);
		} else {
			ixGLInterfaceRecord.setValue("USER_JE_CATEGORY_NAME",  OFSetup(sTableName + "_"	+ sMaximoTransactionType + "_CATEGORY"), 11L);
			ixGLInterfaceRecord2.setValue("USER_JE_CATEGORY_NAME",  OFSetup(sTableName + "_"	+ sMaximoTransactionType + "_CATEGORY"), 11L);
		}		
		System.out.println("*************** 1 *********************");
		ixGLInterfaceRecord.setValue(	"USER_JE_SOURCE_NAME",OFSetup(sTableName + "_" + sMaximoTransactionType+ "_SOURCE"), 11L);
		ixGLInterfaceRecord2.setValue(	"USER_JE_SOURCE_NAME",OFSetup(sTableName + "_" + sMaximoTransactionType+ "_SOURCE"), 11L);
		ixGLInterfaceRecord.setValue("CURRENCY_CONVERSION_DATE",tActualDate, 11L);
		ixGLInterfaceRecord2.setValue("CURRENCY_CONVERSION_DATE",tActualDate, 11L);
		ixGLInterfaceRecord.setValue("USER_CURRENCY_CONVERSION_TYPE","Corporate", 11L);		
		ixGLInterfaceRecord2.setValue("USER_CURRENCY_CONVERSION_TYPE","Corporate", 11L);		
		ixGLInterfaceRecord.setValue("CURRENCY_CONVERSION_RATE",bdExchangeRate.doubleValue(), 11L);	
		ixGLInterfaceRecord2.setValue("CURRENCY_CONVERSION_RATE",bdExchangeRate.doubleValue(), 11L);	
		ixGLInterfaceRecord.setValue("GROUP_ID", getGroupID(), 11L);
		ixGLInterfaceRecord2.setValue("GROUP_ID", getGroupID(), 11L);
		System.out.println("*************** 4 *********************");
		// kafco changes
				if (sReference8.equalsIgnoreCase("KAF")) {
					ixGLInterfaceRecord.setValue("CHART_OF_ACCOUNTS_ID", "50615", 11L);
					ixGLInterfaceRecord2.setValue("CHART_OF_ACCOUNTS_ID", "50615", 11L);
				} else {
					ixGLInterfaceRecord.setValue("CHART_OF_ACCOUNTS_ID","50233", 11L);//(sMaximoOrg + "_COAID")
					ixGLInterfaceRecord2.setValue("CHART_OF_ACCOUNTS_ID","50233", 11L);
				}
		//End		
		ixGLInterfaceRecord.setValue("CODE_COMBINATION_ID", sDebitID, 11L);
		//ixGLInterfaceRecord2.set(ixGLInterfaceRecord);
		ixGLInterfaceRecord2.setValue("CODE_COMBINATION_ID", sCreditID, 11L);
		System.out.println("*************** 5 *********************");
		if (bdBaseAmount.compareTo(new BigDecimal(0)) == 1) {
			BigDecimal entered_dr = bdBaseAmount.divide(bdExchangeRate, BigDecimal.ROUND_HALF_EVEN);
			ixGLInterfaceRecord.setValue("ENTERED_DR", (bdBaseAmount.divide(bdExchangeRate, BigDecimal.ROUND_HALF_EVEN)).doubleValue(), 11L);
			ixGLInterfaceRecord.setValue("ACCOUNTED_DR", bdBaseAmount.doubleValue(), 11L);
			ixGLInterfaceRecord2.setValue("ENTERED_CR",	entered_dr.doubleValue(), 11L);
			ixGLInterfaceRecord2.setValue("ACCOUNTED_CR",bdBaseAmount.doubleValue(), 11L);
		} else {
			BigDecimal entered_cr = (bdBaseAmount.divide(bdExchangeRate, BigDecimal.ROUND_HALF_EVEN)).negate();
			BigDecimal accounted_cr = bdBaseAmount.negate();
			ixGLInterfaceRecord.setValue("ENTERED_CR", ((bdBaseAmount.divide(bdExchangeRate, BigDecimal.ROUND_HALF_EVEN)).negate()).doubleValue(), 11L);
			ixGLInterfaceRecord.setValue("ACCOUNTED_CR",(bdBaseAmount.negate()).doubleValue(), 11L);
			ixGLInterfaceRecord2.setValue("ENTERED_DR",entered_cr.doubleValue(), 11L);
			ixGLInterfaceRecord2.setValue("ACCOUNTED_DR",accounted_cr.doubleValue(), 11L);
		}
		System.out.println("*************** 6 *********************");
		ixGLInterfaceRecord.setValue("REFERENCE10", sDescription, 11L);//Description		
		System.out.println("*************** 6.1 sDescription *********************"+sDescription);
		ixGLInterfaceRecord.setValue("ATTRIBUTE1", sReference2, 11L);// Maximo Transaction Type
		System.out.println("*************** 6.2 sReference2 *********************"+sReference2);
		ixGLInterfaceRecord.setValue("ATTRIBUTE2", sReference3, 11L);// MATUSETRANS ID
		System.out.println("*************** 6.3 sReference3 *********************"+sReference3);
		ixGLInterfaceRecord.setValue("ATTRIBUTE3", sReference4, 11L);//Empty
		System.out.println("*************** 6.4 sReference4 *********************"+sReference4);
		ixGLInterfaceRecord.setValue("ATTRIBUTE4", sReference6, 11L);// REFWO
		System.out.println("*************** 6.5 sReference6 *********************"+sReference6);
		ixGLInterfaceRecord.setValue("ATTRIBUTE5", sReference7, 11L);//Empty
		ixGLInterfaceRecord.setValue("ATTRIBUTE6", sReference8, 11L);// Site ID
		System.out.println("*************** 6.6 sReference8 *********************"+sReference8);
		ixGLInterfaceRecord.setValue("ATTRIBUTE7", sReference9, 11L);//Empty
		System.out.println("*************** 6.7 externalRefID *********************"+externalRefID);
		ixGLInterfaceRecord.setValue("ATTRIBUTE8", String.valueOf(externalRefID), 11L); //External REF ID/API
		System.out.println("*************** 7 *********************");
		
		ixGLInterfaceRecord2.setValue("REFERENCE10", sDescription, 11L);
		ixGLInterfaceRecord2.setValue("ATTRIBUTE1", sReference2, 11L);
		ixGLInterfaceRecord2.setValue("ATTRIBUTE2", sReference3, 11L);
		ixGLInterfaceRecord2.setValue("ATTRIBUTE3", sReference4, 11L);
		ixGLInterfaceRecord2.setValue("ATTRIBUTE4", sReference6, 11L);
		ixGLInterfaceRecord2.setValue("ATTRIBUTE5", sReference7, 11L);
		ixGLInterfaceRecord2.setValue("ATTRIBUTE6", sReference8, 11L);
		ixGLInterfaceRecord2.setValue("ATTRIBUTE7", sReference9, 11L);
		ixGLInterfaceRecord2.setValue("ATTRIBUTE8", String.valueOf(externalRefID), 11L);
		System.out.println("*************** 8 *********************");
		if(sContractRefNum != null){
			ixGLInterfaceRecord.setValue("ATTRIBUTE9", sContractRefNum, 11L);
			ixGLInterfaceRecord2.setValue("ATTRIBUTE9", sContractRefNum, 11L);
		}

		/*
		String fBtDate = null;
		DateFormat readFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
		DateFormat writeFormat = new SimpleDateFormat("dd-MMM-yyyy");
		Date date = null;

		try {
			if (!(sBtDate == null || sBtDate.equals("") || sBtDate.isEmpty())) {
				date = readFormat.parse(sBtDate);
			}

			if (date != null) {
				fBtDate = writeFormat.format(date);
			}

		} catch (ParseException e) {
			e.printStackTrace();
		}

		if (sEmpnum != null)  {
			ixGLInterfaceRecord.setValue("ATTRIBUTE11", sEmpnum, 11L);
			ixGLInterfaceRecord2.setValue("ATTRIBUTE11", sEmpnum, 11L);
			
		}
		System.out.println("*************** 9 *********************");
		if (fBtDate != null)  {
			ixGLInterfaceRecord.setValue("ATTRIBUTE12", fBtDate.toUpperCase(), 11L);
			ixGLInterfaceRecord2.setValue("ATTRIBUTE12", fBtDate.toUpperCase(), 11L);
		}*/

		System.out.println("*************** 10 *********************");
		if(sRefWO!=null)// If Work Order Exists then jump to Processing
			populatePA(externalRefID);
		
		//ixGLInterfaceRecordSet.save();
		//ixGLInterfaceRecord2Set.save();
	}
	
	public void populatePA(long externalRefID) throws MXException,RemoteException
	{
		System.out.println("***************  IN Projects*********************");
		String sMatUseTransID  = getString("MATUSETRANSID");
		String sSiteID = getString("SITEID");
		String sWO5="";
		String sWO7="";
		String sPONum = getString("PONUM");
		String sApiSeq = getString("ITIN7");
		String sRefWO = getString("REFWO");
		String sItemnum = getString("ITEMNUM");
		String sStoreLoc = getString("STORELOC");
		String sIncludeProjects = "Y";
        String sTable = "MATUSETRANS";
        String sSource = "Maximo 2";
        String sOrgName ="";
        String sExpType = "";		
		//Long consignmenLong = ixMatUseTransRecord.getLong("CONSIGNMENT");
		//int consignment = consignmenLong.intValue();

		if (sStoreLoc != null) {
			if (sStoreLoc.contains("CONSIGN")) {
					return;
			}
		}
		
		System.out.println("*************** sSiteID *********************"+sSiteID);
		
		//Filter: not already in Oracle & only for COMM site & no PONUM
		if (sApiSeq != null && (!sSiteID.equals("COMM")) && sPONum != null)
			return;
		System.out.println("*************** sSiteID *********************"+sSiteID);
		if (sIncludeProjects.equals("Y") && sRefWO != null)
		{
			System.out.println("*************** sRefWO *********************"+sRefWO);
			String sToSiteId = getString("TOSITEID");
			MboSetRemote ixPATransRecordSet = getMboSet("ixPATransRecordSet$","K_PA_TRANSACTION_INTER_ALL","1=2");
			MboRemote ixPATransRecord = ixPATransRecordSet.add(2L);
			
			MboRemote ixTasksRecord = null;
			MboRemote ixProjectsRecord = null;
			
			MboRemote getWO = getMboSet("getWO$","WORKORDER","wonum = '"+sRefWO+"' and siteid = '"+sSiteID+"'").getMbo(0);
			if(getWO!=null)
			{
				
				sWO5 = getProjectNumber( sRefWO, sToSiteId);
				sWO7 = getTaskNumber( sRefWO, sToSiteId);
				System.out.println("*************** sWO5 *********************"+sWO5);
				
				 //ixProjectsRecord = getMboSet("ixProjectsRecord$","API_PA_PROJECTS","PROJECT_ID='"+ sWO5	+ "' and PROJ_STATUS_CODE != 'CLOSED' AND proj_status_name = 'Approved'").getMbo(0);
				ixProjectsRecord = getMboSet("ixProjectsRecord$","API_PA_PROJECTS","PROJECT_ID='"+ sWO5	+ "' and PROJ_STATUS_CODE != 'CLOSED' AND proj_status_name = 'Approved'").getMbo(0);
				if (ixProjectsRecord!=null &&sWO7 != null) {
					//ixTasksRecord = getMboSet("ixTasksRecord$","API_PA_TASKS","PROJECT_ID='" + sWO5+ "' and TASK_ID='" + sWO7+"'").getMbo(0);
					ixTasksRecord = getMboSet("ixTasksRecord$","API_PA_TASKS","PROJECT_ID='" + ixProjectsRecord.getInt("PROJECT_ID")+ "' and TASK_NUMBER='" + sWO7+"'").getMbo(0);
					if(ixTasksRecord==null)
						throw new MXApplicationException("populatepa", "TaskNotFound");
				}
				else
				{
					return;
				}
				System.out.println("*************** sWO7 *********************"+sWO7);
				
				/* TO DO
				 * 
				 * ixOFOrgNameSet = getResultSet(sOFCN, "ORGANIZATION_NAME",
						sPAUser,
						rOFMap.getString("PA_ORG_NAME_LOOKUP_SET_NAME"),
						sAccount);
				ixOFExpTypeSet = getResultSet(sOFCN, "EXPENDITURE_TYPE",
						sPAUser,
						rOFMap.getString("PA_EXP_TYPE_LOOKUP_SET_NAME"),
						sAccount);
				sOrgName = ixOFOrgNameSet.getString(0, "ORGANIZATION_NAME");
				sExpType = ixOFExpTypeSet.getString(0, "EXPENDITURE_TYPE");
				 */
				
				
				
				Date tsExpItemDate = getDate("TRANSDATE");
				BigDecimal bdQuantity = new BigDecimal(1);
				if (!sTable.equals("TOOLTRANS"))
					bdQuantity = 	new BigDecimal(getDouble("QUANTITY"));// (BigDecimal) NVL(	new BigDecimal(getDouble("QUANTITY")) , new BigDecimal(1));
				else
					bdQuantity = new BigDecimal(getDouble("TOOLQTY")); //(BigDecimal) NVL(new BigDecimal(getDouble("TOOLQTY")), new BigDecimal(1));
				
				BigDecimal bdLineCost = new BigDecimal(getDouble("LINECOST"));
				if (sTable.equals("MATUSETRANS"))
					bdQuantity = bdQuantity.negate();
			    
				ixPATransRecord.setValue("TASK_NUMBER",ixTasksRecord.getString("TASK_NUMBER"));
				ixPATransRecord.setValue("TRANSACTION_SOURCE", sSource);
				ixPATransRecord.setValue("ATTRIBUTE_CATEGORY", sSource);
				ixPATransRecord.setValue("BATCH_NAME",ixProjectsRecord.getString("SEGMENT1"));
				ixPATransRecord.setValue("EXPENDITURE_ITEM_DATE", tsExpItemDate);
				try{
				ixPATransRecord.setValue("EXPENDITURE_ENDING_DATE",	getExpEndDate(new Timestamp(tsExpItemDate.getTime()))); 
				} catch(Exception e)
				{
					
				}
				//Dummy Value
				sOrgName = "AA";
				sExpType = "BB";
				int Txn_interface_id = -1;
				ixPATransRecord.setValue("ORGANIZATION_NAME", sOrgName); //TO SET Value
				ixPATransRecord.setValue("EXPENDITURE_TYPE", sExpType); //TO Set Value
				ixPATransRecord.setValue("Txn_interface_id", Txn_interface_id);//TO Check is KNPC is setting this value
				ixPATransRecord.setValue("QUANTITY", bdQuantity.doubleValue());

				if (sTable.equals("MATRECTRANS") || sTable.equals("SERVRECTRANS"))
						ixPATransRecord.setValue("DENOM_RAW_COST",getDouble("LOADEDCOST"));
				else
					    ixPATransRecord.setValue("DENOM_RAW_COST", bdLineCost.doubleValue()); 
				
				ixPATransRecord.setValue("TRANSACTION_STATUS_CODE", "P");
				ixPATransRecord.setValue("ORIG_TRANSACTION_REFERENCE", String.valueOf(externalRefID));
				ixPATransRecord.setValue("ORG_ID", ixProjectsRecord.getLong("ORG_ID"));
				ixPATransRecord.setValue("USER_TRANSACTION_SOURCE", sSource);
				ixPATransRecord.setValue("UNMATCHED_NEGATIVE_TXN_FLAG", "Y");
				// FLEX FIELDS
				ixPATransRecord.setValue("ATTRIBUTE1", sRefWO);
				ixPATransRecord.setValue("ATTRIBUTE2", sSiteID);
				ixPATransRecord.setValue("ATTRIBUTE3", sItemnum);
				ixPATransRecord.setValue("ATTRIBUTE4", sStoreLoc);
				ixPATransRecord.setValue("ATTRIBUTE5", sPONum);
				ixPATransRecord.setValue("ATTRIBUTE6", "");
				ixPATransRecord.setValue("ATTRIBUTE9", "");
				ixPATransRecord.setValue("ATTRIBUTE10", getString("GLDEBITACCT").substring(16, 18));
				
				setValue("ITIN7",String.valueOf(externalRefID),11L); //Setting API Sequence for Project
				
				/* THIS IS FOR SERVRECTRANS
				String sEmpNum = getString("EMPNUM");
				String sBtDate = getString("BTDATE");
				
				String fBtDate = null;
				DateFormat readFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
				DateFormat writeFormat = new SimpleDateFormat("dd-MMM-yyyy");
				Date date = null;

				try {

					if (!(sBtDate == null || sBtDate.equals("") || sBtDate.isEmpty())) {
						date = readFormat.parse(sBtDate);
					}
					if (date != null) {
						fBtDate = writeFormat.format(date);
					}

				} catch (ParseException e) {
					e.printStackTrace();
				}

				ixPATransRecord.setValue("ATTRIBUTE7", sEmpNum);
				if (fBtDate != null) {
					ixPATransRecord.setValue("ATTRIBUTE8", fBtDate.toUpperCase());
				}
				*/
			}
				
		}
			
	}
	
	public Timestamp getExpEndDate(Timestamp tsEndDate) throws Exception, MXException,RemoteException {
		// we specify Locale.US since months are in english we want to parse a
		// TimeStamp
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// fix timezone in the SimpleDateFormat bug in JDK1.1
		sdf.setCalendar(Calendar.getInstance());
		// create a Date (no choice, parse returns a Date object)
		Date d = sdf.parse(tsEndDate.toString());
		// create a GregorianCalendar from a Date object
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(d);
		if (gc.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY)
			return tsEndDate;
		if (gc.get(Calendar.DAY_OF_WEEK) < Calendar.FRIDAY) {
			gc.add(Calendar.DATE,
					Calendar.FRIDAY - gc.get(Calendar.DAY_OF_WEEK));
		} else if (gc.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
			gc.add(Calendar.DATE, 6);
		}
		return new java.sql.Timestamp(gc.getTime().getTime());
	}
	
	public String getProjectNumber(String sRefWO, String sSiteID) throws MXException,RemoteException {
		MboRemote ixWORecord = getMboSet("ixWORecord$", "WORKORDER",
				"wonum = '" + sRefWO + "' and siteid = '" + sSiteID + "'").getMbo(0);
		if (ixWORecord != null) {
			String sWO5 = null;
			String sParentWO = null;
			sWO5 = ixWORecord.getString("WO5");
			sParentWO = ixWORecord.getString("PARENT");

			do {
				if (ixWORecord.getString("WO5") != null) {
					sWO5 = ixWORecord.getString("WO5");
					return sWO5;
				} else {
					MboRemote ixWORecord1 = getMboSet("ixWORecord1$", "WORKORDER",
							"wonum = '" + sParentWO + "' and siteid = '" + sSiteID + "'").getMbo(0);
					if (ixWORecord1 != null) {
						sWO5 = ixWORecord.getString("WO5");
						sParentWO = ixWORecord.getString("PARENT");

						if (sWO5 != null) {
							return sWO5;
						}
					}
				}
			} while (sParentWO != null);
			return sWO5;
		} else
			return null;

	}

	public String getTaskNumber(String sRefWO, String sSiteID) throws MXException,RemoteException {
		MboRemote ixWORecord = getMboSet("ixWORecord$", "WORKORDER",
				"wonum = '" + sRefWO + "' and siteid = '" + sSiteID + "'").getMbo(0);
		if (ixWORecord != null) {
			String sWO5 = null;
			String sParentWO = null;
			sWO5 = ixWORecord.getString("WO7");
			sParentWO = ixWORecord.getString("PARENT");

			do {
				if (ixWORecord.getString("WO7") != null) {
					sWO5 = ixWORecord.getString("WO7");
					return sWO5;
				} else {
					MboRemote ixWORecord1 = getMboSet("ixWORecord1$", "WORKORDER",
							"wonum = '" + sParentWO + "' and siteid = '" + sSiteID + "'").getMbo(0);
					if (ixWORecord1 != null) {
						sWO5 = ixWORecord.getString("WO7");
						sParentWO = ixWORecord.getString("PARENT");

						if (sWO5 != null) {
							return sWO5;
						}
					}
				}
			} while (sParentWO != null);
			return sWO5;
		} else
			return null;

	}
	
	 public String FormatDescription(String sDesc,int j) throws MXException,RemoteException {
		if(sDesc==null) return null;
		int i, iLen;
		String sSingle="",sNewDesc=new String(sDesc);
		
		if (sDesc.indexOf("'")!=-1) 
		{
			iLen=sDesc.length();
			for (i=0;i<iLen;i++) {
				sSingle=sDesc.substring(i,i+ 1);
				if(sDesc.substring(i,i+ 1).equals("'"))sSingle="''";
				sNewDesc+=sSingle;
			}
		}

		if(sNewDesc.length()>j) sNewDesc=sNewDesc.substring(0,j-3)+"...";
		
		return sNewDesc;
	}
	
	public long getGroupID() throws MXException,RemoteException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
		Date date = new Date();
		String formattedDate = sdf.format(date);
		return Integer.valueOf(formattedDate);
	}
	/* From KNPC
	public String getGroupID() throws Exception {
		Timestamp tsCurrent = new Timestamp(System.currentTimeMillis());
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
		Date d = sdf.parse(tsCurrent.toString());
		String s = d.toString();
		s = tsCurrent.toString();
		s = s.substring(0, 10);
		s = s.replaceAll("-", "");
		return s;
	}*/
	
	public String getMaxValue(String sValueList, String sTransactionType) throws MXException,RemoteException
	{
		MboSetRemote issueTypeSet = getMboSet("issueTypeSet$","SYNONYMDOMAIN","domainid = '"+sValueList+"' and value = '"+sTransactionType+"'");
		MboRemote issueType;
		if(issueTypeSet!=null && !issueTypeSet.isEmpty())
		{
			issueType = issueTypeSet.getMbo(0);
			return issueType.getString("MAXVALUE");
		}
		else
			throw new MXApplicationException("IssueTypeNotFound", sTransactionType);
	}
	
	public String getAccountID(String orgID, String glAccount)
			throws MXException,RemoteException {

		
		MboSetRemote coaSet = getMboSet("coaSet$","CHARTOFACCOUNTS","glaccount = '" + glAccount + "' and orgid = '"+ orgID+"' and active = 1");
		MboRemote glAccountMbo;

		if (coaSet != null && !coaSet.isEmpty()) {
			glAccountMbo = coaSet.getMbo(0);
			return glAccountMbo.getString("EXTERNALREFID");
		}

		else
			throw new MXApplicationException("GLCodeCombNotFound", glAccount);

	}
	
	public String OFSetup(String sValue) throws MXException,RemoteException
	{
		MboSetRemote OFSetupSet = getMboSet("OFSetupSet$","ALNDOMAIN","domainid = 'OFSETUP' and value = '"+sValue+"'");
		if(!OFSetupSet.isEmpty())
		{
			MboRemote OFSetup = OFSetupSet.getMbo(0);
			return OFSetup.getString("DESCRIPTION");
		}else
		{
			throw new MXApplicationException("OFSetupNotFound", sValue);
		}
	}
	
	private String getMaximoTransactionType(String sIssueType, String fromStoreLoc) throws MXException,RemoteException {
		if (sIssueType.equals("ISSUE"))
			return "Inventory Issue/Return";
		if (sIssueType.equals("RETURN"))
			return "Inventory Issue/Return";
		if (sIssueType.equals("WRITEOFF"))
			return "Inventory Write-Off";
		if (sIssueType.equals("INVOICE"))
			return "Inventory Issue/Return";
		if (sIssueType.equals("SHIPCANCEL"))
			return "Inventory Issue/Return";
		return sIssueType;
	}


}//class 