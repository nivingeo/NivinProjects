<%--
* Licensed Materials - Property of IBM
* 
* 5724-U18
* 
* (C) COPYRIGHT IBM CORP. 2006,2024 All Rights Reserved.
* 
* US Government Users Restricted Rights - Use, duplication or
* disclosure restricted by GSA ADP Schedule Contract with
* IBM Corp.
--%><%@ include file="../common/componentheader.jsp" %><%
  boolean renderHeader = true;
  wcs.setHideTitlebar(true);
  if(request.getParameterMap().containsKey("hide-titlebar")){
    boolean hideTitlebar = Boolean.valueOf((String)request.getParameter("hide-titlebar"));
    wcs.setHideTitlebar(hideTitlebar);
  }
	if (wcs.getHideTitlebar() || designmode || wcs.getLightningPortalMode())
	{
		renderHeader = false;
	}	
	String apptitle = "";
	String docTitle = "";
	String height = component.getProperty("height");
	height = wcs.attachUOM(height);
	String userFullName = control.getWebClientSession().getUserInfo().getDisplayName();
	try
	{
		docTitle = control.getWebClientSession().getCurrentApp().getAppTitle();
		if(control.getWebClientSession().getCurrentApp().getId().equalsIgnoreCase("startcntr"))
		{
			if (BidiUtils.isBidiEnabled())     //bidi-hcg-SC
				userFullName = BidiUtils.enforceBidiDirection(userFullName, BidiUtils.getMboTextDirection("PERSON", "DISPLAYNAME", true)); //bidi-hcg-SC
			apptitle = control.getWebClientSession().getMessage("login","welcomeusername",new String[]{userFullName});
		}
	}
	catch(Exception e)
	{
		apptitle = "";
	}
//bidi-hcg-AS start
	if(BidiUtils.isBidiEnabled())
	{
		String[] bidiTagAttributes = BidiClientUtils.getTagAttributes(null,null,apptitle,false);
		if(bidiTagAttributes[2] != null && bidiTagAttributes[2].length() > 0)
		{
			apptitle = BidiUtils.enforceBidiDirection(apptitle,bidiTagAttributes[2]);
			docTitle = BidiUtils.enforceBidiDirection(docTitle,bidiTagAttributes[2]);
		}
	}
//bidi-hcg-AS end
	if(apptitle.equals(""))
		apptitle=docTitle;

	String appimage;
	try
	{
		appimage = control.getWebClientSession().getCurrentApp().getAppImage();
	}
	catch(Exception e)
	{
		appimage = "";
	}

	String informationImage = "information.gif";
	if(appimage == null || appimage.length() == 0)
	{
		if(app.getApp().toLowerCase().equals("startcntr"))
			appimage="appimg_startcntr.gif";
		else
			appimage="appimg_generic.gif";
	}
	String classname = "bgnb";
	String bgClass = "titlebarback";
	boolean applinking = app.inAppLinkMode();
	if(applinking)
	{
		classname = "alnb";
		bgClass+="_applink";
	}
	cssclass = classname +" "+ cssclass;

	boolean thePortalMode = control.getWebClientSession().getPortalMode();
	if (thePortalMode)
	{
		appimage = "blank.gif";
		informationImage = "blank.gif";
		cssclass = "bgnbp";
		if (!applinking)
			apptitle = "";
	}
	boolean useHomeButton = false;
	boolean useGotoButton = false;
	if(component.needsRender())
	{
		String homeButtonProperty = component.getProperty("homebutton");
		if(!homeButtonProperty.equals("false"))
		{
			String homeButtonSysSetting = "1";
			boolean isMobile = currentPage.getAppInstance().isMobile() || wcs.getMobile(); 
			if(!isMobile && homeButtonSysSetting.equals("1") || homeButtonProperty.equals("true"))
			{
				useHomeButton = true;	
			}
		}
		useGotoButton = useHomeButton;
        if(useGotoButton && wcs.showSystemNavBar(currentPage)) {
            useGotoButton = true;
        }

        if(control.getProperty("gotoinheader").equals("false")){
        	useGotoButton=false;
        }
        
        if (applinking) {
            height="16px";
            cssclass="stubmsg";
        }
        if(docTitle==""){
        	//Probably Birt report viewer
        	docTitle="Document";
        } 
        
  if(renderHeader){ %>
	<td width="100%" align="<%=defaultAlign%>" class="<%=bgClass%>">
		<table width="100%" border="0" style="height: <%=height%>" class="<%=cssclass%>" role="presentation">
			<tr>
				<td style="vertical-align:top;white-space: nowrap;">
					<table cellspacing="0" cellpadding="0" role="presentation" style="height: 100%;">
						<tr>
							<%if(!accessibilityMode && !useHomeButton){%>
							<td width="35" class="applogo">
								<a id="appImageAnchor" href="javascript: sendEvent('focusfirst')"><img id="appimage" border="0" src="<%=IMAGE_PATH%><%=appimage%>" align="<%=defaultAlign%>" hspace="0" alt="<%=docTitle%>" /></a>
							</td>
							<%}%>
							<td style="white-space: nowrap">
							<%if(useHomeButton){ 
								String homeText = wcs.getMaxMessage("ui", "homelinktext").getMessage();
								String gotoText = wcs.getMaxMessage("ui", "gotoMenuText").getMessage();	%>
								<button type="button" id="<%=id%>_homeButton" onkeypress="if(hasKeyCode(event, 'KEYCODE_ENTER')){setClickPosition(this);this.click();}" onclick="setClickPosition(this);sendEvent('changeapp','<%=app.getId()%>','startcntr')" title="<%=homeText%>" alt="<%=homeText%>" aria-label="<%=homeText%>" onmouseout="this.className='homebutton';" onmouseover="this.className='homebutton homebuttonhover'" onblur="this.className='homebutton';" onfocus="this.className='homebutton homebuttonhover'" class="homebutton" <%if(accessibilityMode){%>style="background: transparent !important;"<%}%>>
                                    <img id="homebutton_image" src="<%= IMAGE_PATH%>ac_ban_home.png" style="display: inline<%if(accessibilityMode){%> !important<%}%>; margin: 0px -5px 0px -5px;" alt="<%=homeText%>"/>
                                </button>
                                <%if(useGotoButton){
                                %><button type="button" id="<%=id%>_gotoButton" onkeypress="if(hasKeyCode(event, 'KEYCODE_ENTER')){setClickPosition(this);this.click();}" onclick="setClickPosition(this);sendEvent('showmenu','<%=id%>_gotoButton','goto')" class="gotobutton" onmouseout="this.className='gotobutton';" onmouseover="this.className='gotobutton gotobuttonhover'" onblur="this.className='gotobutton';" onfocus="this.className='gotobutton gotobuttonhover'" title="<%=gotoText%>" alt="<%=gotoText%>" aria-label="<%=gotoText%>" <%if(accessibilityMode){%>style="background: transparent !important;"<%}%>>
                                    <img id="gotobutton_image" src="<%= IMAGE_PATH%>ac_ban_goto.png" style="display: inline<%if(accessibilityMode){%> !important<%}%>; margin: 0px -5px 0px -5px;" alt="<%=gotoText%>"/>
                                </button><%} 
                                if(!wcs.getMobile()){%><img aria-hidden="true" style="margin:0px;vertical-align: bottom" src="<%=IMAGE_PATH%>ac_ban_divider.png" alt=""/><%}%>
							<%}%>
							</td>
							<td style="vertical-align:<%if(useHomeButton){%>middle<%}else{%>top<%}%>;white-space: nowrap;">
								<span id="<%=id%>_appname" class="<%if(useHomeButton){%>homeButton<%}%>txtappname">&nbsp;
									<%=HTML.encodeTolerant(apptitle)%>
								</span>
							</td>
						</tr>
					</table>
				</td>
		<% }
      if(!ismobile)
			{	%>
				<td style="vertical-align:bottom;text-align:<%=defaultAlign%>">
					<table id="titlebar_error_table" class="umtable" border="0" cellspacing="0" cellpadding="0" style="text-align:<%=defaultAlign%>" role="presentation">
						<tr>
							<td><img id="titlebar_error_image" src="<%= IMAGE_PATH%><%=informationImage%>"  style="visibility:hidden" alt=""/></td>
							<td id="titlebar_error" role="alert" class="<%=textcss%> um" style="white-space: normal">&nbsp;</td>
						</tr>
					</table>
				</td>
		<%	}
        if(renderHeader){
       %>
			
				<td class="titlebarlinks" align="<%=reverseAlign%>" style="text-align:<%=reverseAlign%>">
					<table role="presentation" align="<%=reverseAlign%>" style="height: 100%" border="0" cellspacing="0" cellpadding="0">
						<tr><% if(!control.getProperty("usernameinheader").equals("false")){ %>
							<td>
								<span id="<%=id%>_username" class="homeButtontxtappname userfullname" style="display:none"><%=HTML.encodeTolerant(userFullName)%></span>
								<% if(psdi.server.MXServer.getMXServer().isAdminModeOn(true)){ 
									String adminMode = wcs.getMaxMessage("system", "EndAdminModeOn").getMessage(); 
									adminMode = adminMode.replaceAll("\\.", ""); %>
									<span id="adminMode" class="homeButtontxtappname userfullname adminMode"><%=adminMode%></span>
								<% } %>
							</td><%}%>
		<% }
	}
	if(!designmode) {
		component.renderChildrenControls(); //It has to be called, independent of needsrender
	}
	String companyName = "";
	if(accessibilityMode){
		companyName = wcs.getMaxMessage("fusion","CompanyName").getMessage();
	}
	if(component.needsRender() && renderHeader)
	{	%>
						</tr>
					</table>
				</td>
				<td class="titlelogo"><img src="ibm_logo_grey.png" alt="<%=companyName%>" style="display:inline"/></td>
			</tr>
			<tr>
				<td class="titlebarbottom" colspan="20"></td> 
			</tr>
        </table>
        <script>
        <%
	    	ControlInstance ssLink = control.getPage().getControlInstance("sslink");
	    	Iterator childIterator = control.getChildren().iterator();
	    	String ssLinkId = "";
	    	String gotoLinkId = "";
	    	while(childIterator.hasNext()){
	    		ControlInstance child = ((ControlInstance)childIterator.next());
	    		String childId = child.getId();
	    		String mxEvent = child.getProperty("mxevent");
	    		if(mxEvent.equals("startcenter")){
	    			ssLinkId = childId;
	    		}
	    		else if(mxEvent.equals("showmenu") && child.getProperty("eventvalue").equals("goto")){
	    			gotoLinkId = childId;
	    		}
	    	}
	       	if(useHomeButton && !"".equals(ssLinkId)){
				String ssHotKey = control.getPage().getControlInstance(ssLinkId).getProperty("accesskey");%>
				addLoadMethod("addHotKey('<%=ssHotKey%>', '<%=id%>_homeButton', 'click');");
        	<%}
        	if(useGotoButton && !"".equals(gotoLinkId)){
        		String gotoHotKey = control.getPage().getControlInstance(gotoLinkId).getProperty("accesskey"); %>
        		addLoadMethod("addHotKey('<%=gotoHotKey%>','<%=id%>_gotoButton', 'click');");
        	<%}%>
       	</script>
	</td>
<%	}
  String serverMessage = (String)app.get("servermessage");
	if(!WebClientRuntime.isNull(serverMessage))
	{
		String serverMessageType = (String)app.get("servermessagetype");
		if(WebClientRuntime.isNull(serverMessageType))
			serverMessageType="0"; 
		if(hiddenFrame)
		{	
			%><component id="<%=id%>_holder" role="alert" <%=compType%>><%="<![CDATA["%><%	
		}	%>
		<script>
	<%	if(!RequestHandler.getWebClientProperty("alertmessages","false").equals("true"))
		{	%>
			// IV37703 - Titlebar message not displaying for full timeout
			//addLoadMethod("showMessage(<%=serverMessageType%>, '<%=serverMessage%>', false);");
			addLoadMethod("showMessage(<%=serverMessageType%>, \"<%=serverMessage%>\", false);");  // RTC defect # 92784
	<% 	}
		else
		{	%>
			alert("<%=serverMessage%>");
	<%	}	%>
		</script>
	<%	if(hiddenFrame)
		{	
			%><%="]]>"%></component><%
		}
	}
	app.put("servermessage", "");
	app.put("servermessagetype", "");
%><%@ include file="../common/componentfooter.jsp" %>
