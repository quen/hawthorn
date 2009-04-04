<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://www.leafdigital.com/tld/hawthorn" prefix="hawthorn" %>
<%--
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
--%>
<!DOCTYPE html
	PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>Hawthorn JSP example (minimal)</title>
</head>
<body>
<c:choose>
<c:when test="${param.user!=null}">
<hawthorn:init magicNumber="23d70acbe28943b3548e500e297afb16"
	user="${param.user}" displayName="${param.displayname}" jsUrl="hawthorn.js"
	popupUrl="popup.html" reAcquireUrl="reacquire.jsp">
	<server>http://80.229.13.61:13370/</server>
</hawthorn:init>

<hawthorn:getAuthKey channel="a"/>

<p>Ok, got past init again. Key is ${hawthornKey}, time ${hawthornKeyTime}.</p>

<hawthorn:getRecent channel="a"/>

<hawthorn:linkToChat channel="a" title="Channel A">
Chat now!
</hawthorn:linkToChat>
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
<input type="submit" />
</div>

</form>
</c:otherwise>
</c:choose>

</body>
</html>
