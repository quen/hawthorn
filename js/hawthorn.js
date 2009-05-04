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
	servers : null,

	init : function(servers)
	{
		this.servers = servers;
		this.currentServer = Math.floor(Math.random() * servers.length);
	},

	addTag : function(path)
	{
		if (!this.servers)
		{
			this.getHandler(currentId).failure('Not inited');
			this.id++;
			return;
		}

		// Make an array that does this request on each available server,
		// with the current server being first.
		var urlArray = [];
		var index = this.currentServer;
		for (var i=0; i<this.servers.length; i++)
		{
			if (index == this.servers.length)
			{
				index = 0;
			}
			urlArray[i] = this.servers[index] + path;
			index++;
		}

		this.addTagAnyServer(urlArray, true);
	},

	addTagAnyServer : function(url, updateCurrent)
	{
		var head = document.getElementsByTagName("head")[0];
		var newScript = document.createElement('script');
		newScript.id = 'hawthorn_script' + this.id;
		newScript.type = 'text/javascript';

		// Make sure input variable is an array
		if (url.constructor != Array)
		{
			url = [url];
		}

		// Add ID to each input URL
		for (var i=0; i<url.length; i++)
		{
			if (url[i].indexOf('?') == -1)
			{
				url[i] += '?id=' + this.id;
			}
			else
			{
				url[i] += '&id=' + this.id;
			}
		}

		newScript.src = url.shift();

		var data = new Object();
		data.tag = newScript;
		data.urls = url;
		data.eventId = this.id;

		// onerror function: retries or calls the error handler
		var errorFunction = function()
		{
			// Clear timeout
			window.clearTimeout(data.timeoutId);

			// Remove tag
			data.tag.parentNode.removeChild(data.tag);

			// Retry or give error
			if (data.urls.length == 0)
			{
				hawthorn.getHandler(data.eventId).failure('Error accessing chat server');
			}
			else
			{
				// Retry with next URL
				var anotherScript = document.createElement('script');
				anotherScript.id = data.tag.id;
				anotherScript.type = 'text/javascript';
				anotherScript.src = data.urls.shift();
				anotherScript.onerror = errorFunction;
				anotherScript.onload = loadFunction;
				data.timeoutId = window.setTimeout(errorFunction, 20000);
				head.appendChild(anotherScript);
				data.tag = anotherScript;

				// Update current server
				if (updateCurrent)
				{
					hawthorn.currentServer++;
					if (hawthorn.currentServer >= hawthorn.servers.length)
					{
						hawthorn.currentServer = 0;
					}
				}
			}
		};

		// onload function: clears the timeout
		var loadFunction = function()
		{
			var currentTag = data.tag;
			window.clearTimeout(data.timeoutId);
		}

		newScript.onerror = errorFunction;
		newScript.onload = loadFunction;
		head.appendChild(newScript);

		// Timeout 20s
		data.timeoutId = window.setTimeout(errorFunction, 20000);

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

	recent : function(channel, user, displayName, permissions, keyTime, key,
		maxAge, maxNumber, maxNames, continuation, failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/recent?channel=' + channel + '&user=' + user
				+ '&displayname=' + encodeURIComponent(displayName) + '&permissions='
				+ permissions + '&keytime=' + keyTime
				+ "&key=" + key + "&maxage=" + maxAge + "&maxnumber=" + maxNumber
				+ (maxNames==null ? '' : "&maxnames=" + maxNames));
	},

	recentComplete : function(id, messages, names, lastTime)
	{
		this.removeTag(id);
		this.getHandler(id).continuation(messages, names, lastTime);
	},

	recentError : function(id,error)
	{
		this.removeTag(id);
		this.getHandler(id).failure(error);
	},

	poll : function(channel, user, displayName, permissions, keyTime, key,
		lastTime, continuation, failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/poll?channel=' + channel + '&user=' + user
			+ '&displayname=' + encodeURIComponent(displayName)
			+ '&permissions=' + permissions + '&keytime=' + keyTime + "&key=" + key
			+ '&lasttime=' + lastTime);
	},

	pollComplete : function(id, messages, lastTime, delay)
	{
		this.removeTag(id);
		this.getHandler(id).continuation(messages, lastTime, delay);
	},

	pollError : function(id, error)
	{
		this.removeTag(id);
		this.getHandler(id).failure(error);
	},

	say : function(channel, user, displayName, permissions, keyTime, key,
		message, continuation, failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/say?channel=' + channel + '&user=' + user
				+ '&displayname=' + encodeURIComponent(displayName)
				+ '&permissions=' + permissions + '&keytime=' + keyTime
				+ '&key=' + key + "&message=" + encodeURIComponent(message)
				+ '&unique=' + (new Date()).getTime());
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

	ban : function(channel, user, displayName, permissions, keyTime, key,
		ban, until, continuation, failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/ban?channel=' + channel + '&user=' + user
				+ '&displayname=' + encodeURIComponent(displayName)
				+ '&permissions=' + permissions + '&keytime=' + keyTime
				+ '&key=' + key + "&ban=" + ban + "&until=" + until
				+ '&unique=' + (new Date()).getTime());
	},

	banComplete : function(id)
	{
		this.removeTag(id);
		this.getHandler(id).continuation();
	},

	banError : function(id,error)
	{
		this.removeTag(id);
		this.getHandler(id).failure(error);
	},

	leave : function(channel, user, displayName, permissions, keyTime, key,
		continuation, failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/leave?channel=' + channel + '&user=' + user
				+ '&displayname=' + encodeURIComponent(displayName)
				+ '&permissions=' + permissions
				+ '&keytime=' + keyTime + '&key=' + key);
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

	wait : function(channel, user, displayName, permissions, keyTime, key,
		lastTime, continuation, failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/wait?channel=' + channel + '&user=' + user
				+ '&displayname=' + encodeURIComponent(displayName)
				+ '&permissions=' + permissions + '&keytime=' + keyTime
				+ "&key=" + key + "&lasttime=" + lastTime);
	},

	waitComplete : function(id, lastTime, messages, names)
	{
		this.removeTag(id);
		this.getHandler(id).continuation(lastTime, messages, names);
	},

	waitError : function(id, error)
	{
		this.removeTag(id);
		this.getHandler(id).failure(error);
	},

	reAcquire : function(url, channel, user, permissions, displayName,
		continuation, failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTagAnyServer(url + '?channel=' + channel + '&user=' + user
			+ '&displayname=' + encodeURIComponent(displayName)
			+ '&permissions=' + permissions, false);
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

	log : function(channel, user, displayName, permissions, keyTime, key, date,
		continuation, failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/log?channel=' + channel
				+ '&user=' + user + '&displayname=' + encodeURIComponent(displayName)
				+ '&permissions=' + permissions + '&keytime=' + keyTime + "&key=" + key
				+ "&date=" + date);
	},

	logComplete : function(id,lines)
	{
		this.removeTag(id);
		this.getHandler(id).continuation(lines);
	},

	logError : function(id,error)
	{
		this.removeTag(id);
		this.getHandler(id).failure(error);
	},

	openPopup : function(url, reAcquireUrl, channel, user, displayName,
		permissions, keyTime, key, title)
	{
		var servers = '';
		for (var i=0; i<this.servers.length; i++)
		{
			if (i!=0)
			{
				servers += ',';
			}
			servers += this.servers[i];
		}
		this.chatWindow = window.open(url + '?reacquire='
			+ encodeURIComponent(reAcquireUrl) + '&channel=' + channel + '&user='
			+ user + '&displayname=' + encodeURIComponent(displayName)
			+ '&permissions=' + permissions + '&keyTime='
			+ keyTime + '&key=' + key + '&title=' + encodeURIComponent(title)
			+ '&servers=' + encodeURIComponent(servers), '_' + channel,
			'width=500,height=400,menubar=no,'
			+ 'toolbar=no,location=no,directories=no,status=no,resizable=yes,'
			+ 'scrollbars=no');
	  this.chatWindow.focus();
		return false;
	},
	
	getStatisticsURLs : function(user, displayName, permissions, keyTime, key)
	{
		var result = new Array();
		for (var i = 0; i<this.servers.length; i++)
		{
			result[i] = this.servers[i] + 'hawthorn/html/statistics?channel=!system'
				+ '&user=' + user + '&displayname=' + encodeURIComponent(displayName)
				+ '&permissions=' + permissions + '&keytime=' + keyTime + "&key=" + key;
		}
		return result;
	},

	handleRecent : function(details)
	{
		var el=document.getElementById(details.id);
		this.recent(details.channel, details.user, details.displayName,
			details.permissions, details.keyTime, details.key, details.maxAge,
			details.maxMessages, details.maxNames,
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
					case 'SAY': text = messages[i].text; break;
					case 'JOIN': text = "JOIN"; break;
					case 'BAN': text = "BAN " + messages[i].ban + " " + messages[i].until; break;
					case 'LEAVE': text = "LEAVE" + (messages[i].timeout ? " (timeout)" : " (requested)"); break;
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
				var div = document.createElement('div');
				div.className='hawthorn-error';
				div.appendChild(document.createTextNode(error));
				el.appendChild(div);
			});
	}
	
}

/**
 * Standard implementation of popup window used to display chat in a channel.
 * This constructor should be called from the page that defines the popup.
 * It inits the Hawthorn system and starts listening for events.
 * @constructor
 */
function HawthornPopup(useWait)
{
	this.keyAcquireURL = this.getPageParam('reacquire');
	this.servers = this.getPageParam('servers').split(',');
	hawthorn.init(this.servers);

	this.channel = this.getPageParam('channel');
	this.user = this.getPageParam('user');
	this.displayName = this.getPageParam('displayname');
	this.permissions = this.getPageParam('permissions');
	this.keyTime = this.getPageParam('keyTime');
	this.key = this.getPageParam('key');
	document.title = this.getPageParam('title');
	this.maxAge = 10*60*1000; // 10 minutes old (default)
	this.maxNumber = 10; // 10 messages (default)
	this.lastTime = 0;
	this.pollTime = 0;
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
		hawthorn.leave(p.channel, p.user, p.displayName, p.permissions, p.keyTime,
			p.key, function() { window.close(); }, function(error) { window.close(); });
	};
	if (useWait)
	{
		this.startWait();
	}
	else
	{
		this.startPoll();
	}
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
	hawthorn.reAcquire(this.keyAcquireURL, this.channel, this.user,
		this.displayName, this.permissions,
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

HawthornPopup.prototype.handleMessages = function(messages)
{
	for(var i = 0; i < messages.length; i++)
	{
		var message = messages[i];
		if(message.type == 'JOIN')
		{
			this.addJoin(message.time, message.user, message.displayName,
				message.user == this.user);
		}
		else if(message.type == 'LEAVE')
		{
			this.addLeave(message.time, message.user, message.displayName,
				message.user == this.user);
		}
		else if(message.type == 'SAY')
		{
			this.addMessage(message.time, message.user, message.displayName,
				message.text, message.user == this.user);
		}
		else if(message.type == 'NOTICE')
		{
			this.addNotice(message.time, message.text);
		}
	}
}

HawthornPopup.prototype.startPoll = function()
{
	var p = this;
	setTimeout(function() { p.poll(); }, 50);
}

HawthornPopup.prototype.poll = function()
{
	// Is it time to poll?
	var now = (new Date()).getTime();
	if (now < this.pollTime)
	{
		// No, try again later
		this.startPoll();
		return;
	}

	// Result handler functions
	var p = this;
	var ok = function(messages, lastTime, delay)
	{
		p.handleMessages(messages);
		var now = (new Date()).getTime();
		p.pollTime = now + delay;
		p.lastTime = lastTime;
		p.startPoll();
	};
	var first = function(messages,names,lastTime)
	{
		for(var i = 0; i < names.length; i++)
		{
			var name=names[i];
			p.addName(name.user, name.displayName);
		}
		ok(messages, lastTime, 2000);
	};
	var fail = function(error)
	{
		p.addError(error);
		// And don't continue polling
	};

	// Poll
	if (this.lastTime == 0)
	{
		hawthorn.recent(this.channel, this.user, this.displayName, this.permissions,
				this.keyTime, this.key, this.maxAge, this.maxNumber, null, first, fail);
	}
	else
	{
		hawthorn.poll(this.channel, this.user, this.displayName, this.permissions,
			this.keyTime, this.key, this.lastTime, ok, fail);
	}
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
		p.handleMessages(messages);
		// If there's only 5 minutes until the key expires, request another
		if(p.keyTime - lastTime < 5*60*1000)
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
		hawthorn.recent(this.channel, this.user, this.displayName, this.permissions,
			this.keyTime, this.key, this.maxAge, this.maxNumber, null, first, fail);
	}
	else
	{
		hawthorn.wait(this.channel, this.user, this.displayName, this.permissions,
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
  * Adds a newly-received system notice to the message area.
  * @param time Message time (ms since 1970)
  * @param message Message from system
  */
 HawthornPopup.prototype.addNotice = function(time, message)
 {
 	var entry = document.createElement('div');
 	entry.className='entry notice';
 	var inner = document.createElement('div');
 	inner.className = 'message';
 	inner.appendChild(document.createTextNode(message));
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
	hawthorn.say(this.channel, this.user, this.displayName, this.permissions,
		this.keyTime, this.key, text, function() {}, this.addError);
	// Make it poll again real soon to get this (if polling)
	this.pollTime = 0;
}
