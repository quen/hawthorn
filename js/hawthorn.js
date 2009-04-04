/*
Copyright 2009 Samuel Marshall
http://www.leafdigital.com/software/hawthorn/

This file is part of Hawthorn.

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
 */

// System
var hawthorn =
{
	id : 1,
	handlers : [],
	currentServer : null,

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
		if(!hawthorn.currentServer)
		{
			hawthorn.getHandler(currentId).failure('Not inited');
			this.id++;
			return;
		}
		this.addTagAnyServer(this.currentServer + path);
	},

	addTagAnyServer : function(url)
	{
		var head = document.getElementsByTagName("head")[0];
		var newScript = document.createElement('script');
		newScript.id = 'hawthorn_script' + this.id;
		newScript.type = 'text/javascript';
		var src = url;
		if(src.indexOf('?') == -1)
		{
			src += '?id=' + this.id;
		}
		else
		{
			src += '&id=' + this.id;
		}
		newScript.src = src;
		var currentId = this.id;
		newScript.onerror = function()
		{
			hawthorn.getHandler(currentId).failure('Error accessing chat server');
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
		maxNames,continuation,failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/getRecent?channel=' + channel + '&user=' + user
				+ '&displayname=' + encodeURIComponent(displayName) + '&keytime=' + keyTime
				+ "&key=" + key + "&maxage=" + maxAge + "&maxnumber=" + maxNumber
				+ (maxNames==null ? '' : "&maxnames=" + maxNames));
	},

	getRecentComplete : function(id, messages, names, lastTime)
	{
		this.removeTag(id);
		this.getHandler(id).continuation(messages, names, lastTime);
	},

	getRecentError : function(id,error)
	{
		this.removeTag(id);
		this.getHandler(id).failure(error);
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
				+ '&displayname=' + encodeURIComponent(displayName) + '&keytime=' + keyTime
				+ "&key=" + key + "&message=" + encodeURIComponent(message));
	},

	sayComplete : function(id)
	{
		this.removeTag(id);
		this.getHandler(id).continuation();
	},

	sayError : function(id,error)
	{
		this.removeTag(id);
		this.getHandler(id).failure(error);
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
				+ '&displayname=' + encodeURIComponent(displayName) + '&keytime=' + keyTime
				+ "&key=" + key);
	},

	leaveComplete : function(id)
	{
		this.removeTag(id);
		this.getHandler(id).continuation();
	},

	leaveError : function(id,error)
	{
		this.removeTag(id);
		this.getHandler(id).failure(error);
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
				+ '&displayname=' + encodeURIComponent(displayName) + '&keytime=' + keyTime
				+ "&key=" + key + "&lasttime=" + lastTime);
	},

	waitForMessageComplete : function(id, lastTime, messages, names)
	{
		this.removeTag(id);
		this.getHandler(id).continuation(lastTime, messages, names);
	},

	waitForMessageError : function(id, error)
	{
		this.removeTag(id);
		this.getHandler(id).failure(error);
	},

	reAcquire : function(url, channel, user, displayName, continuation, failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTagAnyServer(url + '?channel=' + channel + '&user=' + user
		+ '&displayname=' + encodeURIComponent(displayName));
	},

	reAcquireComplete : function(id, key, keyTime)
	{
		this.removeTag(id);
		this.getHandler(id).continuation(key, keyTime);
	},

	reAcquireError : function(id, error)
	{
		this.removeTag(id);
		this.getHandler(id).failure(error);
	},

	getLog : function(channel, keyTime, key, date, continuation, failure)
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
		this.getHandler(id).continuation(lines);
	},

	getLogError : function(id,error)
	{
		this.removeTag(id);
		this.getHandler(id).failure(error);
	},

	openPopup : function(url,reAcquireUrl,channel,user,displayName,keyTime,key,title)
	{
		this.chatWindow = window.open(url + '?reacquire=' +
			encodeURIComponent(reAcquireUrl) + '&channel=' + channel + '&user=' +
			user + '&displayName=' + encodeURIComponent(displayName) + '&keyTime=' +
			keyTime + '&key=' + key + '&title=' + encodeURIComponent(title) +
			'&server=' + encodeURIComponent(this.currentServer), '_' + channel,
			'width=500,height=400,menubar=no,'+
			'toolbar=no,location=no,directories=no,status=no,resizable=yes,'+
			'scrollbars=no');
	},
	
	handleGetRecent : function(details)
	{
		var el=document.getElementById(details.id);
		this.getRecent(details.channel, details.user, details.displayName, 
			details.keyTime, details.key, details.maxAge, details.maxMessages, 
			details.maxNames, 
			function(messages, names, lastTime) 
			{
				while(el.firstChild) el.removeChild(el.firstChild);
				var ul=document.createElement('ul');
				el.appendChild(ul);
				for(var i=0;i<messages.length;i++)
				{
					var li=document.createElement('li');

					var text;
					switch(messages[i].type)
					{
					case 'SAY': text=messages[i].text; break;
					case 'JOIN': text="JOIN"; break;
					case 'LEAVE': text="LEAVE" + (messages[i].timeout ? " (timeout)" : " (requested)"); break;
					}

					ul.appendChild(li);
					li.appendChild(document.createTextNode(
							new Date(messages[i].time)+' - '+
							messages[i].user+' ('+messages[i].displayName+'): '+
							text));
				}
				intro=document.createElement('p');
				intro.appendChild(document.createTextNode(names.length+' names:'));
				var ul=document.createElement('ul');
				el.appendChild(ul);
				for(var i=0;i<names.length;i++)
				{
					var li=document.createElement('li');
					ul.appendChild(li);
					li.appendChild(document.createTextNode(
							names[i].user+' ('+names[i].displayName+')'));
				}
			}, 
			function(error)
			{
				while(el.firstChild) el.removeChild(el.firstChild);
				el.appendChild(document.createTextNode(error));
			});
	}
	
}

/**
 * Standard implementation of popup window used to display chat in a channel.
 * This constructor should be called from the page that defines the popup.
 * It inits the Hawthorn system and starts listening for events.
 * @constructor
 */
function HawthornPopup()
{
	this.keyAcquireURL = this.getPageParam('reacquire');
	this.server = this.getPageParam('server');
	hawthorn.init([this.server], 0);

	this.channel = this.getPageParam('channel');
	this.user = this.getPageParam('user');
	this.displayName = this.getPageParam('displayName');
	this.keyTime = this.getPageParam('keyTime');
	this.key = this.getPageParam('key');
	document.title = this.getPageParam('title');
	this.maxAge = 10*60*1000; // 10 minutes old (default)
	this.maxNumber = 10; // 10 messages (default)
	this.lastTime = 0;
	this.lastDisplayTime = '';

	this.strJoined = ' joined the chat';
	this.strLeft = ' left the chat';
	this.strError = 'A system error occurred';

	this.initLayout();
	var p = this;
	this.textBox.onkeypress=function(e)
	{
		var key;
		if(window.event)
		{
			key = e.keyCode;
		}
		else
		{
			key = e.which;
		}
		if(key==13)
		{
		  p.say();
		}
	};
	this.namesArea.present = new Object();
	this.closeButton.onclick=function()
	{
		p.left=true;
		hawthorn.leave(p.channel, p.user, p.displayName, p.keyTime,
			p.key, function() { window.close(); }, function(error) { window.close(); });
	};
	this.startWait();
}

HawthornPopup.prototype.getPageParam = function(name)
{
	var re = new RegExp("[?&]"+name+"=([^&#]*)");
	var matches = re.exec(window.location.href);
	if(matches)
	{
		return decodeURIComponent(matches[1]);
	}
	else
	{
		throw "Window requires parameter '"+name+"'";
	}
}

HawthornPopup.prototype.reAcquire = function(continuation)
{
	var p = this;
	hawthorn.reAcquire(this.keyAcquireURL, this.channel, this.user, this.displayName,
		function(key, keyTime)
		{
			p.key = key;
			p.keyTime = keyTime;
			continuation();
		},
		function(error)
		{
			p.addError(error);
		});
}

HawthornPopup.prototype.startWait = function()
{
	if(this.left)
	{
		return;
	}
	var p = this;
	var ok = function(lastTime,messages)
	{
		p.lastTime = lastTime;
		for(var i = 0; i < messages.length; i++)
		{
			var message = messages[i];
			if(message.type == 'JOIN')
			{
				p.addJoin(message.time, message.user, message.displayName,
					message.user == p.user);
			}
			else if(message.type == 'LEAVE')
			{
				p.addLeave(message.time, message.user, message.displayName,
					message.user == p.user);
			}
			else if(message.type == 'SAY')
			{
				p.addMessage(message.time, message.user, message.displayName,
					message.text, message.user == p.user)
			}
		}
		if(lastTime - p.keyTime > 55*60*1000)
		{
			p.reAcquire(function() { p.startWait(); });
		}
		else
		{
			p.startWait();
		}
	};
	var first = function(messages,names,lastTime)
	{
		for(var i = 0; i < names.length; i++)
		{
			var name=names[i];
			p.addName(name.user, name.displayName);
		}	
		ok(lastTime,messages);
	}
	var fail = function(error)
	{
		p.addError(error);
	};

	if(this.lastTime == 0)
	{
		hawthorn.getRecent(this.channel, this.user, this.displayName,
			this.keyTime, this.key, this.maxAge, this.maxNumber, null, first, fail);
	}
	else
	{
		hawthorn.waitForMessage(this.channel, this.user, this.displayName,
			this.keyTime, this.key, this.lastTime, ok, fail);
	}
}

/**
 * Sets data variables that hold the three main components (used later by
 * default implementations of addMessage etc).
 */
HawthornPopup.prototype.initLayout = function()
{
	this.mainArea = document.getElementById("main");
	this.namesArea = document.getElementById("names");
	this.textBox = document.getElementById("textbox");
	this.closeButton = document.getElementById('closebutton');
}

/**
 * Adds the given user to the name list (alphabetically sorted).
 * @param user User ID
 * @param displayName Display name for user
 */
HawthornPopup.prototype.addName = function(user,displayName)
{
	var el = this.namesArea.present[user];
	if (el)
	{
		return;
	}

	var newEl = document.createElement('div');
	newEl.className = 'name';
	newEl.appendChild(document.createTextNode(displayName));
	this.namesArea.present[user] = newEl;

	for (var current = this.namesArea.firstChild; current != null; 
		current = current.nextSibling)
	{
		if(current.className != 'name')
		{
			continue;
		}
		var currentName = current.firstChild.nodeValue;
		if(currentName > displayName)
		{
			this.namesArea.insertBefore(newEl, current);
			return;
		}
	}

	this.namesArea.appendChild(newEl);
}

/**
 * Removes the given user from the name list.
 * @param user User ID
 * @param displayName Display name for user
 */
HawthornPopup.prototype.removeName = function(user,displayName)
{
	var el = this.namesArea.present[user];
	if(!el)
	{
		return;
	}
	this.namesArea.removeChild(el);
	this.namesArea.present[user] = undefined;
}

/**
 * Adds an entry to the message area.
 * @param el Element
 */
HawthornPopup.prototype.addEntry = function(time,el)
{
	var date = new Date(time);
	var hours = date.getHours(), mins = date.getMinutes();
	if (hours<10)
	{
		hours = '0' + hours;
	}
	if (mins<10)
	{
		mins = '0' + mins;
	}
	var displayTime = hours+":"+mins;
	if(displayTime != this.lastDisplayTime)
	{
		this.lastDisplayTime = displayTime;
		var timeEl = document.createElement('div');
		timeEl.className = 'timestamp';
		var inner = document.createElement('span');
		timeEl.appendChild(inner);
		inner.appendChild(document.createTextNode(displayTime));
		this.mainArea.appendChild(timeEl);
	}

	this.mainArea.appendChild(el);
	el.scrollIntoView();
}

/**
 * Adds a newly-received message to the message area.
 * @param time Message time (ms since 1970)
 * @param user User ID
 * @param displayName Display name for user
 * @param message Message from user
 * @param self True if this is a message from the current user
 */
HawthornPopup.prototype.addMessage = function(time,user,displayName,message,self)
{
	var entry = document.createElement('div');
	entry.className='entry say' + (self ? ' self' : '');
	var inner = document.createElement('div');
	inner.className = 'message';
	inner.appendChild(document.createTextNode('<'));
	var name = document.createElement('strong');
	name.className = 'name';
	name.appendChild(document.createTextNode(displayName));
	inner.appendChild(name);
	inner.appendChild(document.createTextNode('> ' + message));
	entry.appendChild(inner);
	this.addEntry(time, entry);
}

/**
 * Adds a newly-received join message to the message area.
 * @param time Message time (ms since 1970)
 * @param user User ID
 * @param displayName Display name for user
 * @param self True if this is a message from the current user
 */
HawthornPopup.prototype.addJoin = function(time,user,displayName,self)
{
	var entry = document.createElement('div');
	entry.className='entry join' + (self ? ' self' : '');
	entry.appendChild(document.createTextNode('\u2022 '));
	var name = document.createElement('strong');
	name.className='name';
	name.appendChild(document.createTextNode(displayName));
	entry.appendChild(name);
	entry.appendChild(document.createTextNode(this.strJoined));
	this.addEntry(time, entry);
	this.addName(user, displayName);
}

/**
 * Adds a newly-received leave message to the message area.
 * @param time Message time (ms since 1970)
 * @param user User ID
 * @param displayName Display name for user
 * @param self True if this is a message from the current user
 */
HawthornPopup.prototype.addLeave = function(time,user,displayName,self)
{
	this.removeName(user, displayName);
	var entry = document.createElement('div');
	entry.className = 'entry leave' + (self ? ' self' : '');
	entry.appendChild(document.createTextNode('\u2022 '));
	var name = document.createElement('strong');
	name.className = 'name';
	name.appendChild(document.createTextNode(displayName));
	entry.appendChild(name);
	entry.appendChild(document.createTextNode(this.strLeft));
	this.addEntry(time, entry);
}

/**
 * Adds an error message to the message area.
 * @param message Error message
 */
HawthornPopup.prototype.addError = function(message)
{
	var entry = document.createElement('div');
	entry.className = 'entry error';
	var h3 = document.createElement('h3');
	h3.appendChild(document.createTextNode(this.strError));
	entry.appendChild(h3);
	var div = document.createElement('div');
	div.appendChild(document.createTextNode(message));
	entry.appendChild(div);
	this.addEntry((new Date()).getTime(), entry);
	this.mainArea.appendChild(entry);
}

/**
 * Called when user types Return. Obtains text from the textbox and
 * sends a message to the channel with that text.
 */
HawthornPopup.prototype.say = function()
{
	var text = this.textBox.value;
	if(text == '')
	{
		return;
	}
	this.textBox.value = '';
	hawthorn.say(this.channel, this.user, this.displayName, this.keyTime,
		this.key, text, function() {}, this.addError);
}
