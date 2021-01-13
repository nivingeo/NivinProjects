package custom.app.inventory;

import psdi.app.inventory.MatRecTransRemote;
import psdi.util.MXException;
import java.rmi.RemoteException;
import java.sql.Timestamp;


public interface MatRecTransExtRemote extends MatRecTransRemote{

	public String getTaskNumber(String sRefWO, String sSiteID) throws MXException,RemoteException ;

	public String getProjectNumber(String sRefWO, String sSiteID) throws MXException,RemoteException ;

	public void populatePA(long externalRefID) throws MXException,RemoteException
		;

	public Timestamp getExpEndDate(Timestamp tsEndDate) throws Exception, MXException,RemoteException ;

	public void addMedicaPlus() throws RemoteException, MXException
		;

	public String OFSetup(String sValue) throws MXException,RemoteException
		;

	public String getAccountID(String orgID, String glAccount)
				throws MXException,RemoteException ;

	public long getGroupID() throws MXException,RemoteException ;



}//interface 
                                                                                                                                                                                                                                                                                                                                                     