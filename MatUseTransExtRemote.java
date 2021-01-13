package custom.app.inventory;

import psdi.app.inventory.MatUseTransRemote;
import psdi.util.MXException;
import java.rmi.RemoteException;
import java.lang.String;
import java.sql.Timestamp;


public interface MatUseTransExtRemote extends MatUseTransRemote{

	public String getTaskNumber(String sRefWO, String sSiteID) throws MXException,RemoteException ;

	public void populatePA(long externalRefID) throws MXException,RemoteException
		;

	public String getProjectNumber(String sRefWO, String sSiteID) throws MXException,RemoteException ;

	public Timestamp getExpEndDate(Timestamp tsEndDate) throws Exception, MXException,RemoteException ;

	public String getAccountID(String orgID, String glAccount)
			throws MXException,RemoteException ;

	public String OFSetup(String sValue) throws MXException,RemoteException
	;

	public long getGroupID() throws MXException,RemoteException ;



}//interface 
                                                                                                                                                                                                                                                                                                                                                     