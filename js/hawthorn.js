/*
Copyright 2009 Samuel Marshall
http://www.leafdigital.com/software/hawthorn/

This file is part of hawthorn.

hawthorn is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

hawthorn is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with hawthorn.  If not, see <http://www.gnu.org/licenses/>.
 */

// System
var hawthorn =
{
	id : 1,
	handlers :
	[],

	init : function(servers,preferred)
	{
		this.servers = servers;
		if (preferred == -1)
		{
			preferred = Math.floor(Math.random() * servers.length);
		}
		this.currentServer = servers[preferred];
	},

	addTag : function(path)
	{
		var head = document.getElementsByTagName("head")[0];
		var newScript = document.createElement('script');
		var currentId = this.id;
		newScript.id = 'hawthorn_script' + this.id;
		newScript.type = 'text/javascript';
		newScript.src = this.currentServer + path + "&id=" + this.id;
		newScript.onerror = function()
		{
			hawthorn.getHandler(currentId).failure('Load error');
		};
		head.appendChild(newScript);
		this.id++;
	},

	removeTag : function(id)
	{
		var oldScript = document.getElementById('hawthorn_script' + id);
		oldScript.parentNode.removeChild(oldScript);
	},

	getHandler : function(id)
	{
		for ( var i = 0; i < this.handlers.length; i++)
		{
			if (this.handlers[i].id == id)
			{
				var handler = this.handlers[i];
				this.handlers.splice(i, 1);
				return handler;
			}
		}
	},

	getRecent : function(channel,user,displayName,keyTime,key,maxAge,maxNumber,
			continuation,failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/getRecent?channel=' + channel + '&user=' + user
				+ '&displayname=' + escape(displayName) + '&keytime=' + keyTime
				+ "&key=" + key + "&maxage=" + maxAge + "&maxnumber=" + maxNumber);
	},

	getRecentComplete : function(id,messages)
	{
		this.removeTag(id);
		this.getHandler(id, 'getRecent').continuation(messages);
	},

	getRecentError : function(id,error)
	{
		this.removeTag(id);
		this.getHandler(id, 'getRecent').failure(error);
	},

	say : function(channel,user,displayName,keyTime,key,message,continuation,
			failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/say?channel=' + channel + '&user=' + user
				+ '&displayname=' + escape(displayName) + '&keytime=' + keyTime
				+ "&key=" + key + "&message=" + escape(message));
	},

	sayComplete : function(id)
	{
		this.removeTag(id);
		this.getHandler(id, 'say').continuation();
	},

	sayError : function(id,error)
	{
		this.removeTag(id);
		this.getHandler(id, 'say').failure(error);
	},

	leave : function(channel,user,displayName,keyTime,key,continuation,failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/leave?channel=' + channel + '&user=' + user
				+ '&displayname=' + escape(displayName) + '&keytime=' + keyTime
				+ "&key=" + key);
	},

	leaveComplete : function(id)
	{
		this.removeTag(id);
		this.getHandler(id, 'leave').continuation();
	},

	leaveError : function(id,error)
	{
		this.removeTag(id);
		this.getHandler(id, 'leave').failure(error);
	},

	waitForMessageFirst : function(channel,user,displayName,keyTime,key,maxAge,
			maxNumber,continuation,failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/waitForMessage?channel=' + channel + '&user=' + user
				+ '&displayname=' + escape(displayName) + '&keytime=' + keyTime
				+ "&key=" + key + "&maxage=" + maxAge + "&maxnumber=" + maxNumber);
	},

	waitForMessage : function(channel,user,displayName,keyTime,key,lastTime,
			continuation,failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/waitForMessage?channel=' + channel + '&user=' + user
				+ '&displayname=' + escape(displayName) + '&keytime=' + keyTime
				+ "&key=" + key + "&lasttime=" + lastTime);
	},

	waitForMessageComplete : function(id,lastTime,messages)
	{
		this.removeTag(id);
		this.getHandler(id, 'waitForMessage').continuation(lastTime, messages);
	},

	waitForMessageError : function(id,error)
	{
		this.removeTag(id);
		this.getHandler(id, 'waitForMessage').failure(error);
	},

	getLog : function(channel,keyTime,key,date,continuation,failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/getLog?channel=' + channel
				+ '&user=_admin&displayname=_&keytime=' + keyTime + "&key=" + key
				+ "&date=" + date);
	},

	getLogComplete : function(id,lines)
	{
		this.removeTag(id);
		this.getHandler(id, 'getLog').continuation(lines);
	},

	getLogError : function(id,error)
	{
		this.removeTag(id);
		this.getHandler(id, 'getLog').failure(error);
	}
}
