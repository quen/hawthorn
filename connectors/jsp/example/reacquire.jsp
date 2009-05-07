<%@ page contentType="text/javascript; charset=UTF-8" %>
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
<% request.setCharacterEncoding("UTF-8"); %>

<%--
This script should now check that the user is authorised to chat and has
the specified permissions. (Or ignore permissions/user etc in the request
and generate them afresh.) In this test version, we assume that the user
is authorised.
--%>

<hawthorn:init magicNumber="23d70acbe28943b3548e500e297afb16"
	user="${param.user}" displayName="${param.displayname}" extra="${param.extra}"
	permissions="${param.permissions}" jsUrl="hawthorn.js" popupUrl="popup.html"
	reAcquireUrl="reacquire.jsp">
	<server>http://192.168.0.100:13370/</server>
</hawthorn:init>

<hawthorn:reAcquireAllow channel="${param.channel}" id="${param.id}"/>
