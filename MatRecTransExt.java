package custom.app.inventory;

import psdi.util.MXApplicationException;
import psdi.util.MXException;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.lang.Math;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSet;
import psdi.mbo.MboSetRemote;
import psdi.security.ConnectionKey;
import psdi.server.MXServer;
import psdi.app.inventory.MatRecTrans;

public class MatRecTransExt extends MatRecTrans implements MatRecTransExtRemote {

	final private String APPLOGGER = "maximo.application.INVENTORY";
	private MXLogger log;
	private MXLogger logger = MXLoggerFactory.getLogger("maximo.service.INVENTORY");

	public MatRecTransExt(MboSet mboSet0) throws MXException, RemoteException {
		super(mboSet0);
		log = MXLoggerFactory.getLogger(APPLOGGER);
	}

	public void add() throws MXException, RemoteException {
		//TODO Auto-generated method stub
		super.add();
		log.debug("MatRecTransExt.add");

	}

	public void init() throws MXException {
		//TODO Auto-generated method stub
		super.init();
		log.debug("MatRecTransExt.init");

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
		    /*
		    if(getString("issuetype").equals("INVOICE"))
		    {
		    	journalProcessing();
		    	super.save();
		    	return;
		    	
		    }*/
		    journalProcessing(projectNum, taskNum, expType, costCenter,siteID);
		 // MedicaPlus part - After financial transaction is done. NOT USED
		    /*
		    String sItemnum = getString("ITEMNUM");
		    MboRemote ixItemRecord = getMboSet("ixItemRecord$","ITEMNUM","itemnum = '" + sItemnum + "'").getMbo(0);
			if(ixItemRecord!=null)
			{
				if (ixItemRecord.getString("STOCKTYPE").equals("413304")
						&& ixItemRecord.getString("IN5").equals("MEDICAL")) {
					addMedicaPlus();
				}
			}*/
		    super.save();
		}
	
	private void journalProcessing(String projectNum, String taskNum, String expType, String costCenter, String siteid)
			throws RemoteException, MXException {
		// Journal Processing

		long externalRefID = 0;
		String apiSeq = "";
		String externalRefid = getString("EXTERNALREFID");
		String sIssueType = getString("ISSUETYPE");
		//String sApiSeqcheck = getString("EXTERNALREFID");
		String sApiSeqcheck = getString("APISEQ");
		String sStatus = getString("STATUS");
		String sInspectionStatus = "WINSP";
		
		if (!getString("ISSUETYPE").equals("INVOICE")) {
			// checking if inspection status is approved or not.
			if (sApiSeqcheck.length()>0 || (sStatus.length()>0 && sStatus.equals(sInspectionStatus))) {
				return;
			}
		} 

		String sToStore = getString("TOSTORELOC");
		String sTransactionType = getString("ISSUETYPE");
		String sValueList = "ISSUETYP";
		
		// Ignoring transactions on KNPCDC
		if (sToStore != null && (sToStore.equals("KNPCDC") || ((sToStore.contains("CONSIGN")))))
			return;
       
		BigDecimal bdLoadedCost = new BigDecimal(getDouble("LOADEDCOST")); 
		BigDecimal bdCurrencyLineCost = 	new BigDecimal(getDouble("CURRENCYLINECOST"));
		BigDecimal bdZero = new BigDecimal(0);
		// filter zero impact transactions
		if (getString("GLDEBITACCT") == null || getString("GLDEBITACCT").trim().equals(""))
			throw new MXApplicationException("journalprocessing", "GLAccountNull"); //GL DEBIT ACCOUNT is null. Processing aborted until issue is resolved

		if (getString("GLDEBITACCT").equals(getString("GLCREDITACCT")))
			return;
		// filter out RECEIPTs - MAXIMO 6 Customization
		if ("RECEIPT".equals(sIssueType))
			return;
		
		if (bdCurrencyLineCost == null)
			bdCurrencyLineCost = bdZero; // to compensate ISSUETYPE TRANSFER
		// filter Zero values
		if (bdLoadedCost.compareTo(bdZero) == 0&& bdCurrencyLineCost.compareTo(bdZero) == 0)
			return;
		if (sToStore != null) {
			if (sToStore.indexOf("TOOL") > -1)
				return;
		}

		//if (getString("EXTERNALREFID").length()>0 )
		if (getString("APISEQ").length()>0 )	
			return;
		
				if (externalRefid.equals("")) {

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

						if(sTransactionType.equals("INVOICE"))
							populateGL(externalRefID,"MATRECTRANS","KNPC",sTransactionType,sValueList);
						else
						   prepareJournal(externalRefID) ;
						/*
						if(sRefWO!=null)// If Work Order Exists then jump to Processing PA
						populatePA(externalRefID);
						*/
						if (apiSeq != null) {
							setValue("EXTERNALREFID", apiSeq, 11L);
							setValue("APISEQ", apiSeq, 11L);

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
	

	public void populateGL(long externalRefID, String sTable, String sMXOrgID,
			String sTransactionType, String sValueList)
					throws RemoteException, MXException {

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
		System.out.println("*************** 1 *********************");
		String sStatus = getString("STATUS");
		String sApiSeq = getString("ITIN7"); 
		String sIssueType = getString("ISSUETYPE");
		// filter out RECEIPTs - MAXIMO 6 Customization
	    if ("RECEIPT".equals(sIssueType))
					return ;
	 // Filter: not already in Oracle & status APPR insert VARIANCE
	 		if (!(sIssueType.equals("INVOICE"))) {
	 			if (sApiSeq != null
	 					|| (sApiSeq == null && (!sStatus.equals("COMP")))) 
	 				return; 
	 		}
	 		
		MboSetRemote ixGLInterfaceRecordSet = getMboSet("ixGLInterfaceRecord$","K_GL_INTERFACE","1=2");
		MboRemote ixGLInterfaceRecord = ixGLInterfaceRecordSet.add(2L);
		ixGLInterfaceRecord.setValue("STATUS", "NEW",11L);
		
		MboSetRemote ixGLInterfaceRecord2Set = getMboSet("ixGLInterfaceRecord2$","K_GL_INTERFACE","1=2");
		MboRemote ixGLInterfaceRecord2 = ixGLInterfaceRecord2Set.add(2L);
		ixGLInterfaceRecord2.setValue("STATUS", "NEW",11L);
		
		BigDecimal bdZero = new BigDecimal(0);
		sMaximoTransactionType= getMaxValue(sValueList, sTransactionType); 

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

		Timestamp tsCurrent = new Timestamp(System.currentTimeMillis());
		BigDecimal bdAmount = new BigDecimal(getDouble("CURRENCYLINECOST") );
		BigDecimal bdExchangeRate = new BigDecimal(getDouble("EXCHANGERATE") );
        BigDecimal bdBaseAmount = new BigDecimal(getDouble("LINECOST")) ;
				// KAFCO changes
				if (getString("SITEID").equalsIgnoreCase("KAF")) {
					ixGLInterfaceRecord.setValue("SET_OF_BOOKS_ID", "83");
					ixGLInterfaceRecord2.setValue("SET_OF_BOOKS_ID", "83");
				} else {
					ixGLInterfaceRecord.setValue("SET_OF_BOOKS_ID", "2");//Check setOfBooksID from KNPC to set the value
					ixGLInterfaceRecord2.setValue("SET_OF_BOOKS_ID", "2");//Check setOfBooksID from KNPC to set the value
				}
		ixGLInterfaceRecord.setValue("ACCOUNTING_DATE",getDate("TRANSDATE"));
		ixGLInterfaceRecord2.setValue("ACCOUNTING_DATE",getDate("TRANSDATE"));
		ixGLInterfaceRecord.setValue("CURRENCY_CODE",sCurrency);
		ixGLInterfaceRecord2.setValue("CURRENCY_CODE",sCurrency);
		ixGLInterfaceRecord.setValue("DATE_CREATED", tsCurrent);
		ixGLInterfaceRecord2.setValue("DATE_CREATED", tsCurrent);
		ixGLInterfaceRecord.setValue("CREATED_BY", "1");
		ixGLInterfaceRecord2.setValue("CREATED_BY", "1");
		ixGLInterfaceRecord.setValue("ACTUAL_FLAG", "A");
		ixGLInterfaceRecord2.setValue("ACTUAL_FLAG", "A");
		ixGLInterfaceRecord.setValue("USER_JE_CATEGORY_NAME",OFSetup(sTable + "_" + sMaximoTransactionType + "_CATEGORY"));
		ixGLInterfaceRecord2.setValue("USER_JE_CATEGORY_NAME",OFSetup(sTable + "_" + sMaximoTransactionType + "_CATEGORY"));
		
		ixGLInterfaceRecord.setValue("USER_JE_SOURCE_NAME",OFSetup(sTable + "_" + sMaximoTransactionType	+ "_SOURCE"));
		ixGLInterfaceRecord2.setValue("USER_JE_SOURCE_NAME",OFSetup(sTable + "_" + sMaximoTransactionType	+ "_SOURCE"));
		ixGLInterfaceRecord.setValue("CURRENCY_CONVERSION_DATE",tTransactionDate);
		ixGLInterfaceRecord2.setValue("CURRENCY_CONVERSION_DATE",tTransactionDate);
		ixGLInterfaceRecord.setValue("USER_CURRENCY_CONVERSION_TYPE","Corporate");
		ixGLInterfaceRecord2.setValue("USER_CURRENCY_CONVERSION_TYPE","Corporate");
		ixGLInterfaceRecord.setValue("CURRENCY_CONVERSION_RATE",getDouble("EXCHANGERATE"));
		ixGLInterfaceRecord2.setValue("CURRENCY_CONVERSION_RATE",getDouble("EXCHANGERATE"));
		
		// KAFCO changes
				if (getString("SITEID").equalsIgnoreCase("KAF")) {
					ixGLInterfaceRecord.setValue("CHART_OF_ACCOUNTS_ID", "50615");
					ixGLInterfaceRecord2.setValue("CHART_OF_ACCOUNTS_ID", "50615");
				} else {
					ixGLInterfaceRecord.setValue("CHART_OF_ACCOUNTS_ID","50233");//sChartOfAccountsID
					ixGLInterfaceRecord2.setValue("CHART_OF_ACCOUNTS_ID","50233");
				}
		ixGLInterfaceRecord.setValue(	"CODE_COMBINATION_ID",sDebitID);
		ixGLInterfaceRecord2.setValue(	"CODE_COMBINATION_ID",sCreditID);
		ixGLInterfaceRecord.setValue("ATTRIBUTE8",String.valueOf(externalRefID));
		ixGLInterfaceRecord2.setValue("ATTRIBUTE8",String.valueOf(externalRefID));
		ixGLInterfaceRecord.setValue("REFERENCE10",	"Variance Maximo Invoice:" + getString("INVOICENUM")+ " SITE:" + getString("SITEID"));
		ixGLInterfaceRecord2.setValue("REFERENCE10",	"Variance Maximo Invoice:" + getString("INVOICENUM")+ " SITE:" + getString("SITEID"));
		ixGLInterfaceRecord.setValue("ATTRIBUTE3", "");
		ixGLInterfaceRecord2.setValue("ATTRIBUTE3", "");
		ixGLInterfaceRecord.setValue("ATTRIBUTE4", getString("REFWO"));
		ixGLInterfaceRecord2.setValue("ATTRIBUTE4", getString("REFWO"));
		ixGLInterfaceRecord.setValue("ATTRIBUTE7", getString("SITEID"));
		ixGLInterfaceRecord2.setValue("ATTRIBUTE7", getString("SITEID"));
		if (sTable.equals("MATRECTRANS")) {
			ixGLInterfaceRecord.setValue("ATTRIBUTE1", "Inventory Receipt");
			ixGLInterfaceRecord.setValue("ATTRIBUTE2",getString("MATRECTRANSID"));
			ixGLInterfaceRecord2.setValue("ATTRIBUTE1", "Inventory Receipt");
			ixGLInterfaceRecord2.setValue("ATTRIBUTE2",getString("MATRECTRANSID"));
		}
		if (sTable.equals("SERVRECTRANS")) {
			ixGLInterfaceRecord.setValue("ATTRIBUTE1", "Service Receipt");
			ixGLInterfaceRecord.setValue("ATTRIBUTE2",getString("SERVRECTRANSID"));
			ixGLInterfaceRecord2.setValue("ATTRIBUTE1", "Service Receipt");
			ixGLInterfaceRecord2.setValue("ATTRIBUTE2",getString("SERVRECTRANSID"));

			ixGLInterfaceRecord.setValue("ATTRIBUTE11", sEmpNum);
			ixGLInterfaceRecord2.setValue("ATTRIBUTE11", sEmpNum);
			if (fBtDate != null) {
				ixGLInterfaceRecord.setValue("ATTRIBUTE12",fBtDate.toUpperCase());
				ixGLInterfaceRecord2.setValue("ATTRIBUTE12",fBtDate.toUpperCase());
			}
		}
		ixGLInterfaceRecord.setValue("GROUP_ID", getGroupID(), 11L);
		ixGLInterfaceRecord2.setValue("GROUP_ID", getGroupID(), 11L);
		ixGLInterfaceRecord.setValue("ENTERED_DR", bdAmount.doubleValue()); // variance
		ixGLInterfaceRecord.setValue("ACCOUNTED_DR", bdBaseAmount.doubleValue()); // ZERO
		ixGLInterfaceRecord2.setValue("ENTERED_CR",bdAmount.doubleValue()); // variance
		ixGLInterfaceRecord2.setValue("ACCOUNTED_CR",	bdBaseAmount.doubleValue()); // Zero
		if (bdAmount.compareTo(bdZero) == -1) // if negative swap values
		{
			BigDecimal entered_cr = (bdBaseAmount.divide(bdExchangeRate, BigDecimal.ROUND_HALF_EVEN)).negate();
			BigDecimal accounted_cr = bdBaseAmount.negate();
			ixGLInterfaceRecord.setValue("ENTERED_CR", bdAmount.negate().doubleValue()); // variance
			ixGLInterfaceRecord.setValue("ACCOUNTED_CR", (bdBaseAmount.negate()).doubleValue()); 
			ixGLInterfaceRecord2.setValue("ENTERED_DR",entered_cr.doubleValue()); // variance
			ixGLInterfaceRecord2.setValue("ACCOUNTED_DR",accounted_cr.doubleValue()); 
			//ixGLInterfaceRecord.setValue("ENTERED_DR", null);
			//ixGLInterfaceRecord.setValue("ACCOUNTED_DR", null);
			//ixGLInterfaceRecord2.setValue("ENTERED_CR", null);
			//ixGLInterfaceRecord2.setValue("ACCOUNTED_CR", null);
		}
	}
	
	public void addMedicaPlus() throws RemoteException, MXException
	{
		String sItemnum = getString("ITEMNUM");
		String sDescription = getString("DESCRIPTION");
		String sPONum = getString("PONUM");
		String sPORevisionNum = getString("POREVISIONUM");
		String sSiteID = getString("SITEID");
		BigDecimal bdLoadedCost = new BigDecimal(getDouble("LOADEDCOST")); 
		
		MboRemote ixItemRecord = getMboSet("ixItemRecord$","ITEMNUM","itemnum = '" + sItemnum + "'").getMbo(0);
		if(ixItemRecord!=null)
		{
			if (ixItemRecord.getString("STOCKTYPE").equals("413304")
					&& ixItemRecord.getString("IN5").equals("MEDICAL")) {
				//IXDatabaseRecord ixMPTransactionRecord = getDatabaseRecord(sMaximoConnectionName, "MEDICAPLUS", "TRANSACTION");
				MboSetRemote ixMPTransactionRecordSet = getMboSet("ixMPTransactionRecordSet$","TRANSACTION","1=2");
				MboRemote ixMPTransactionRecord = ixMPTransactionRecordSet.add(2L);
				ixMPTransactionRecord.setValue("ITEM_CODE", sItemnum);
				ixMPTransactionRecord.setValue("ITEM_NAME", sDescription);
				
				MboRemote ixPORecord = getMboSet("ixPORecord$","PO","ponum = '" + sPONum	+ "' and revisionnum = '" + sPORevisionNum + "'").getMbo(0);
				if(ixPORecord!=null)
				{
					ixMPTransactionRecord.setValue("SUPPLIER_CODE",	ixPORecord.getString("VENDOR"));
				}

				ixMPTransactionRecord.setValue("BATCH_NO",getString("TOLOT"));
				MboRemote ixInvlotRecord = getMboSet("ixInvlotRecord$","INVLOT","where lotnum = '"+ getString("TOLOT")+ "' and siteid = '" + sSiteID + "'").getMbo(0);
				if(ixInvlotRecord!=null)
				{
					ixMPTransactionRecord.setValue("EXPIRY_DATE",ixInvlotRecord.getDate("USEBY"));
				}
				ixMPTransactionRecord.setValue("TRANSACTION_DATE",getDate("ACTUALDATE"));
				ixMPTransactionRecord.setValue("PURCHASE_PRICE",	bdLoadedCost.doubleValue());
				if (getDouble("QUANTITY") >= 0.0D) {
					ixMPTransactionRecord.setValue("ISTRANSFEROUT", "1");
					ixMPTransactionRecord.setValue("QUANTITY",getDouble("QUANTITY"));
				} else {
					ixMPTransactionRecord.setValue("ISRETURN", "1");
					ixMPTransactionRecord.setValue("QUANTITY",Math.abs(getDouble("QUANTITY"))) ;
				}
				ixMPTransactionRecord.setValue("ITEM_UNIT_CODE",getString("RECEIVEDUNIT"));
			}
		}		
	}
	
	public void  prepareJournal(long externalRefID) throws RemoteException, MXException {
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
        String sTableName = "MATRECTRANS";
        BigDecimal bdExchangeRate =  new BigDecimal(getDouble("EXCHANGERATE") );
        BigDecimal bdBaseAmount = new BigDecimal(getDouble("LINECOST")) ;

		String sReference2 = getMaximoTransactionType(sIssueType, sFromStore);
		String sReference3 = getString("MATRECTRANSID");
        String sReference4 = getString("INVOICENUM");
        String sReference6 = getString("REFWO");
        String sReference7 = "";
        String sReference8 = getString("SITEID");
        String sReference9="";
        String sBtDate = null;
        String sEmpnum = null;
        String sRefWO = getString("REFWO");
        String sBigDescription="";
		String sPONum = getString("PONUM");
		String sPORevisionNum = getString("POREVISIONNUM");
		String sWonum = getString("REFWO");
		String sItemnum = getString("ITEMNUM");
		String sDescription = getString("DESCRIPTION");
		String sToStore = getString("TOSTORELOC");
		
		if (sWonum != null)
			sBigDescription = "WONUM: " + sWonum + " PONUM: " + sPONum
					+ " ITEMNUM: " + sItemnum + " " + sDescription;
		else
			sBigDescription = "FROM: " + sFromStore + " TO :"
					+ sToStore + " PONUM: " + sPONum + " ITEMNUM: "
					+ sItemnum + " " + sDescription;
		sBigDescription = FormatDescription(sBigDescription, 240);

		String sContractNum = "";
		if (sPONum != null) {
			MboRemote ixCPORecord = getMboSet("ixCPORecord$","PO","ponum = '" + sPONum+ "' and revisionnum = '" + sPORevisionNum+ "' and orgid='KNPC'").getMbo(0);
			if(ixCPORecord!=null)
			{
				sContractNum = ixCPORecord.getString("CONTRACTNUM");
			    sReference9 = sContractNum; //Setting for ATTRIBUTE7
			}
		}
		
		String sContractRefNum = "";
		if (sPONum != null) {
			MboRemote ixPORecord = getMboSet("ixPORecord$","PO","ponum = '" + sPONum	+ "' and revisionnum = '" + sPORevisionNum+ "' and orgid='KNPC' and apprest = 'P'").getMbo(0);
			sContractRefNum = ixPORecord.getString("CONTRACTREFNUM");
		}
		
        System.out.println("*************** 3 *********************");
		// KFCO changes
				if (sReference8.equalsIgnoreCase("KAF")) {
					ixGLInterfaceRecord.setValue("SET_OF_BOOKS_ID", "83", 11L);
					ixGLInterfaceRecord2.setValue("SET_OF_BOOKS_ID", "83", 11L);
					ixGLInterfaceRecord.setValue("LEDGER_ID", "83" ,11L);
					ixGLInterfaceRecord2.setValue("LEDGER_ID", "83" ,11L);
				} else {
					ixGLInterfaceRecord.setValue("SET_OF_BOOKS_ID",OFSetup(sMaximoOrg + "_SOBID"), 11L);
					ixGLInterfaceRecord2.setValue("SET_OF_BOOKS_ID",OFSetup(sMaximoOrg + "_SOBID"), 11L);
					ixGLInterfaceRecord.setValue("LEDGER_ID",OFSetup(sMaximoOrg + "_SOBID"), 11L);
					ixGLInterfaceRecord2.setValue("LEDGER_ID",OFSetup(sMaximoOrg + "_SOBID"), 11L);
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
		// KAFCO changes
				if (sReference8.equalsIgnoreCase("KAF")) {
					ixGLInterfaceRecord.setValue("CHART_OF_ACCOUNTS_ID", "50615", 11L);
					ixGLInterfaceRecord2.setValue("CHART_OF_ACCOUNTS_ID", "50615", 11L);
				} else {
					ixGLInterfaceRecord.setValue("CHART_OF_ACCOUNTS_ID",OFSetup(sMaximoOrg + "_COAID"), 11L);
					ixGLInterfaceRecord2.setValue("CHART_OF_ACCOUNTS_ID",OFSetup(sMaximoOrg + "_COAID"), 11L);
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
		ixGLInterfaceRecord.setValue("REFERENCE10", sBigDescription, 11L);//Description		
		System.out.println("*************** 6.1 sDescription *********************"+sBigDescription);
		ixGLInterfaceRecord.setValue("ATTRIBUTE1", sReference2, 11L);// Maximo Transaction Type
		System.out.println("*************** 6.2 sReference2 *********************"+sReference2);
		ixGLInterfaceRecord.setValue("ATTRIBUTE2", sReference3, 11L);// MATRECTRANS ID
		System.out.println("*************** 6.3 sReference3 *********************"+sReference3);
		ixGLInterfaceRecord.setValue("ATTRIBUTE3", sReference4, 11L);//Empty
		System.out.println("*************** 6.4 sReference4 *********************"+sReference4);
		ixGLInterfaceRecord.setValue("ATTRIBUTE4", sReference6, 11L);// REFWO
		System.out.println("*************** 6.5 sReference6 *********************"+sReference6);
		ixGLInterfaceRecord.setValue("ATTRIBUTE5", sReference7, 11L);//Empty
		ixGLInterfaceRecord.setValue("ATTRIBUTE6", sReference8, 11L);// Site ID
		System.out.println("*************** 6.6 sReference8 *********************"+sReference8);
		ixGLInterfaceRecord.setValue("ATTRIBUTE7", sReference9, 11L);//ContractNum
		System.out.println("*************** 6.7 externalRefID *********************"+externalRefID);
		ixGLInterfaceRecord.setValue("ATTRIBUTE8", String.valueOf(externalRefID), 11L); //External REF ID/API
		System.out.println("*************** 7 *********************");
		
		ixGLInterfaceRecord2.setValue("REFERENCE10", sBigDescription, 11L);
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
			if (fBtDate != null)  {
				ixGLInterfaceRecord.setValue("ATTRIBUTE12", fBtDate.toUpperCase(), 11L);
				ixGLInterfaceRecord2.setValue("ATTRIBUTE12", fBtDate.toUpperCase(), 11L);
			}

		} catch (ParseException e) {
			e.printStackTrace();
		}

		if (sEmpnum != null)  {
			ixGLInterfaceRecord.setValue("ATTRIBUTE11", sEmpnum, 11L);
			ixGLInterfaceRecord2.setValue("ATTRIBUTE11", sEmpnum, 11L);
			
		}*/
		
		if(sRefWO!=null)// If Work Order Exists then jump to Processing
			populatePA(externalRefID);
		
		//ixGLInterfaceRecordSet.save();
		//ixGLInterfaceRecord2Set.save();
	}
	
	public void populatePA(long externalRefID) throws MXException,RemoteException
	{
		System.out.println("***************  IN Projects*********************");
		String sSiteID = getString("SITEID");
		String sWO5="";
		String sWO7="";
		String sPONum = getString("PONUM");
		String sApiSeq = getString("ITIN7");
		String sRefWO = getString("REFWO");
		String sItemnum = getString("ITEMNUM");
		String sStoreLoc = getString("STORELOC");
		String sIncludeProjects = "Y";
        String sTable = "MATRECTRANS";
        String sSource = "Maximo 2";
        String sOrgName ="";
        String sExpType = "";		
		String sStatus = getString("STATUS");
		String sIssueType = getString("ISSUETYPE");
		if (sApiSeq != null)
			return;
		
		if ("RECEIPT".equals(sIssueType))
			return;

		if (sStatus == null)
			sStatus = "";
		sApiSeq = null;
		
		// Filter: not already in Oracle & status APPR insert VARIANCE
				if (!(sIssueType.equals("INVOICE") && toBeAdded())) {
					if (sApiSeq != null
							|| (sApiSeq == null && (!sStatus.equals("COMP")))) 
						return;
				}
		
				sApiSeq = getString("ITIN7");
				// sSiteID = ixMatRecTransRecord.getString("SITEID");
				if (sApiSeq != null)
					return;
				// end by srd

				String sToStoreLoc = getString("TOSTORELOC");
				String sFromStoreLoc = getString("FROMSTORELOC");
				
				if (sToStoreLoc != null && sFromStoreLoc != null) {

					if (sToStoreLoc.contains("CONSIGN")
							|| sFromStoreLoc.contains("CONSIGN"))
					{
							return;
					}
					}
				BigDecimal bdZero = new BigDecimal(0);
				BigDecimal bdLoadedCost = new BigDecimal(getDouble("LOADEDCOST") );
				//bdApiSeq = new BigDecimal(IXUtil.getNextSequence(ixMXconn, "APISEQ"));
				// filter Zero values #MUST BE AFTER VARIANCE!!!
				if (bdLoadedCost.compareTo(bdZero) == 0)
					return; // stop
					
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
				if (ixProjectsRecord!=null && sWO7 != null) {
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
				//BigDecimal bdZero = new BigDecimal(0);
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
				 if(sTable.equals("SERVRECTRANS")){
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
		if (sIssueType.equals("RECEIPT"))
			return "Inventory Receipt";
		if (sIssueType.equals("TRANSFER") && fromStoreLoc.equals("RECEIVING"))
			return "Inventory Receipt";
		if (sIssueType.equals("TRANSFER") && !(fromStoreLoc.equals("RECEIVING")))
			return "Inventory Transfer";
		if (sIssueType.equals("WRITEON"))
			return "Inventory Write-On";
		if (sIssueType.equals("INVOICE"))
			return "Inventory Receipt";
		return sIssueType;
	}

}//class 