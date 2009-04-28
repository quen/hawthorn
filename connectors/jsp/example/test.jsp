<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://www.leafdigital.com/tld/hawthorn" prefix="hawthorn" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html
	PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<!--
Copyright 2009 Samuel Marshall

This file is part of Hawthorn.
http://www.leafdigital.com/software/hawthorn/

Hawthorn is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Hawthorn is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Hawthorn.  If not, see <http://www.gnu.org/licenses/>.
-->
<% request.setCharacterEncoding("UTF-8"); %>
<!--
If using the Tomcat application server, you must be aware that YOU
NEED TO CHANGE A SETTING TO MAKE CHARACTER ENCODING WORK. Otherwise
anyone who has a display name containing a non-ASCII character will
not get valid keys.

The setting is in your server.xml - the <Connector> tag must include
the attribute:

URIEncoding="UTF-8"

(This affects all web applications, so make sure it doesn't break
anything else you run.)
-->
<html>
<head>
<title>Hawthorn JSP example (minimal)</title>
</head>
<body>

<p><strong>This script must not be deployed on a live server.</strong> It
allows anyone to obtain any permissions on the Hawthorn server.</p>

<c:choose>
<c:when test="${param.user!=null}">

<!--
In a real system you would fill the values in here from:
1) Magic number from your system's configuration, which would be set to
   match the Hawthorn server's magic number. (Do not use this example
   magic number on any live system!)
2) User and display name from your system's user database, based on
   the current authenticated user.
3) Permissions from your system's user database, based on permission
   information your system stores ("rw" for normal users).
4) Hawthorn server URL(s) from your system's configuration.
-->
<hawthorn:init magicNumber="23d70acbe28943b3548e500e297afb16"
	user="${param.user}" displayName="${param.displayname}"
	permissions="${param.permissions}" jsUrl="hawthorn.js" popupUrl="popup.html"
	reAcquireUrl="reacquire.jsp">
	<server>http://192.168.0.100:13370/</server>
</hawthorn:init>

<!-- This uses a load test channel in case you want to watch a load test. -->
<hawthorn:getAuthKey channel="loadtestchan3"/>

<p>Ok, got past init again. Key is ${hawthornKey}, time ${hawthornKeyTime}.</p>

<hawthorn:recent channel="loadtestchan3"/>

<hawthorn:linkToChat channel="loadtestchan3" title="Load test channel 3">
Chat now!
</hawthorn:linkToChat>

<%
boolean isAdmin = request.getParameter("permissions").indexOf('a') != -1;
%>
<!-- Only admins can see statistics -->
<c:if test="${isAdmin}">
<hawthorn:linkToStatistics/>
</c:if>

</c:when>
<c:otherwise>
<form method="get" action="test.jsp">
<div>
Username (lowercase letters/numbers only)
<input type="text" name="user" />
</div>
<div>
Display name (any text)
<input type="text" name="displayname" />
</div>
<div>
Permissions
<input type="text" name="permissions" value="rw" />
</div>
<div>
<input type="submit" />
</div>

</form>
</c:otherwise>
</c:choose>


</body>
</html>

