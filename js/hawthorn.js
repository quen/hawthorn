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

	recent : function(channel, user, displayName, extra, permissions, keyTime, key,
		maxAge, maxNumber, maxNames, sayOnly, continuation, failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/recent?channel=' + channel + '&user=' + user
				+ '&displayname=' + encodeURIComponent(displayName) + '&extra='
				+ encodeURIComponent(extra) + '&permissions=' + permissions
				+ '&keytime=' + keyTime + "&key=" + key + "&maxage=" + maxAge
				+ "&maxnumber=" + maxNumber
				+ (maxNames==null ? '' : "&maxnames=" + maxNames)
				+ (sayOnly ? '&filter=say' : ''));
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

	poll : function(channel, user, displayName, extra, permissions, keyTime, key,
		lastTime, continuation, failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/poll?channel=' + channel + '&user=' + user
			+ '&displayname=' + encodeURIComponent(displayName) + '&extra='
			+ encodeURIComponent(extra) + '&permissions=' + permissions
			+ '&keytime=' + keyTime + "&key=" + key + '&lasttime=' + lastTime);
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

	say : function(channel, user, displayName, extra, permissions, keyTime, key,
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
				+ '&extra=' + encodeURIComponent(extra)
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

	ban : function(channel, user, displayName, extra, permissions, keyTime, key,
		ban, banDisplayName, banExtra, until, continuation, failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/ban?channel=' + channel + '&user=' + user
				+ '&displayname=' + encodeURIComponent(displayName)
				+ '&extra=' + encodeURIComponent(extra)
				+ '&permissions=' + permissions + '&keytime=' + keyTime
				+ '&key=' + key + "&ban=" + ban + "&bandisplayname="
				+ encodeURIComponent(banDisplayName) + "&banextra="
				+ encodeURIComponent(banExtra) + "&until=" + until
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

	leave : function(channel, user, displayName, extra, permissions, keyTime, key,
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
				+ '&extra=' + encodeURIComponent(extra)
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

	wait : function(channel, user, displayName, extra, permissions, keyTime, key,
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
				+ '&extra=' + encodeURIComponent(extra)
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

	reAcquire : function(url, channel, user, displayName, extra, permissions,
		continuation, failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTagAnyServer(url + '?channel=' + channel + '&user=' + user
			+ '&extra=' + encodeURIComponent(extra)
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

	log : function(channel, user, displayName, extra, permissions, keyTime, key,
		date, continuation, failure)
	{
		this.handlers.push(
		{
			id : this.id,
			continuation : continuation,
			failure : failure
		});
		this.addTag('hawthorn/log?channel=' + channel
				+ '&user=' + user + '&displayname=' + encodeURIComponent(displayName)
				+ '&extra=' + encodeURIComponent(extra)
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

	openPopup : function(url, reAcquireUrl, channel, user, displayName, extra,
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
			+ '&extra=' + encodeURIComponent(extra)
			+ '&permissions=' + permissions + '&keyTime='
			+ keyTime + '&key=' + key + '&title=' + encodeURIComponent(title)
			+ '&servers=' + encodeURIComponent(servers), '_' + channel,
			'width=500,height=400,menubar=no,'
			+ 'toolbar=no,location=no,directories=no,status=no,resizable=yes,'
			+ 'scrollbars=no');
	  this.chatWindow.focus();
		return false;
	},

	getStatisticsURLs : function(user, displayName, extra, permissions,
		keyTime, key)
	{
		var result = new Array();
		for (var i = 0; i<this.servers.length; i++)
		{
			result[i] = this.servers[i] + 'hawthorn/html/statistics?channel=!system'
				+ '&user=' + user + '&displayname=' + encodeURIComponent(displayName)
				+ '&extra=' + encodeURIComponent(extra)
				+ '&permissions=' + permissions + '&keytime=' + keyTime + "&key=" + key;
		}
		return result;
	},

	handleRecent : function(details)
	{
		var el=document.getElementById(details.id);
		this.recent(details.channel, details.user, details.displayName,
			details.extra, details.permissions, details.keyTime, details.key,
			details.maxAge, details.maxMessages, details.maxNames, details.sayOnly,
			function(messages, names, lastTime)
			{
				while(el.firstChild) el.removeChild(el.firstChild);
				if(messages.length > 0)
				{
					var div = document.createElement('div');
					el.appendChild(div);
					div.className = 'hawthorn_recent_messages';
					if(details.recentText != '')
					{
						var heading = document.createElement('h' + details.headingLevel);
						div.appendChild(heading);
						heading.appendChild(document.createTextNode(details.recentText));
					}
					var ul=document.createElement('ul');
					div.appendChild(ul);
					for(var i=0;i<messages.length;i++)
					{
						var li=document.createElement('li');
						if(i==0)
						{
							li.className = 'hawthorn_first';
						}

						var text;
						switch(messages[i].type)
						{
						case 'SAY': text = messages[i].text; break;
						// Other types are only for debug/test purposes, display isn't nice
						case 'JOIN': text = "JOIN"; break;
						case 'BAN': text = "BAN " + messages[i].ban + " "
							+ messages[i].until; break;
						case 'LEAVE': text = "LEAVE" + (messages[i].timeout
							? " (timeout)" : " (requested)"); break;
						}

						ul.appendChild(li);

						// Get time in user's locale, but chop off seconds
						var time = (new Date(messages[i].time)).toLocaleTimeString();
						time = time.replace(/:[0-9]{2}$/, '');
						var span = document.createElement('span');
						li.appendChild(span);
						span.className = 'hawthorn_recent_time';
						span.appendChild(document.createTextNode(time));
						li.appendChild(document.createTextNode(' <'));
						span = document.createElement('span');
						li.appendChild(span);
						span.className = 'hawthorn_recent_name';
						span.appendChild(document.createTextNode(messages[i].displayName));
						li.appendChild(document.createTextNode('> '));
						span = document.createElement('span');
						li.appendChild(span);
						span.className = 'hawthorn_recent_text';
						span.appendChild(document.createTextNode(text));
					}
				}
				if(names.length > 0)
				{
					var div = document.createElement('div');
					el.appendChild(div);
					div.className = 'hawthorn_recent_names';
					if(details.namesText != '')
					{
						var heading = document.createElement('h' + details.headingLevel);
						div.appendChild(heading);
						heading.appendChild(document.createTextNode(details.namesText));
					}
					var ul=document.createElement('ul');
					div.appendChild(ul);
					for(var i=0;i<names.length;i++)
					{
						ul.appendChild(document.createTextNode(' '));
						var li=document.createElement('li');
						if(i==0)
						{
							li.className = 'hawthorn_first';
						}
						ul.appendChild(li);
						li.appendChild(document.createTextNode(names[i].displayName));
					}
					if(names.length == details.maxNames)
					{
						ul.appendChild(document.createTextNode(' '));
						var li=document.createElement('li');
						ul.appendChild(li);
						li.appendChild(document.createTextNode(String.fromCharCode(0x2026)));
					}
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
 * It sets up parameters from the URL and from defaults. The page can then
 * alter defaults if required before calling init().
 * @constructor
 */
function HawthornPopup()
{
	this.keyAcquireURL = this.getPageParam('reacquire');
	this.servers = this.getPageParam('servers').split(',');
	hawthorn.init(this.servers);

	this.channel = this.getPageParam('channel');
	this.user = this.getPageParam('user');
	this.displayName = this.getPageParam('displayname');
	this.extra = this.getPageParam('extra');
	this.permissions = this.getPageParam('permissions');
	this.keyTime = this.getPageParam('keyTime');
	this.key = this.getPageParam('key');
	document.title = this.getPageParam('title');
	this.maxAge = 10*60*1000; // 10 minutes old (default)
	this.maxNumber = 10; // 10 messages (default)
	this.lastTime = 0;
	this.pollTime = 0;
	this.banTime = 4*60*60*1000; // 4 hours (default)
	this.lastDisplayTime = '';
	this.present = new Object();
	this.useWait = false; // Default is to poll

	this.strJoined = ' joined the chat';
	this.strLeft = ' left the chat';
	this.strBanned = ' banned user: ';
	this.strError = 'A system error occurred';
	this.strConfirmBan = 'Are you sure you want to ban $1?\n\nBanning means they will '
		+ 'not be able to chat here, or watch this chat, for the next 4 hours.';
	this.strCloseChat = 'Close chat';
	this.strIntro = 'To chat, type messages in the textbox and press Return to send.';
	this.strBanUser = 'Ban user';
}

/**
 * Initialises the Hawthorn popup including all user-interface elements,
 * JavaScript listeners etc.
 */
HawthornPopup.prototype.init = function()
{
	// Create the layout elements
	this.initLayout();

	// Ban button is hidden except for moderators
	if(this.permissions.indexOf('m') == -1)
	{
		this.banButtonDiv.style.display = 'none';
	}

	// Begin with no selected user
	this.setSelectedUser(null);

	// Set up textbox actions
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
	this.closeButton.onclick = function()
	{
		p.left=true;
		hawthorn.leave(p.channel, p.user, p.displayName, p.extra, p.permissions,
			p.keyTime, p.key, function() { window.close(); },
			function(error) { window.close(); });
	};
	if(this.useWait)
	{
		this.startWait();
	}
	else
	{
		this.startPoll();
	}
	this.banButton.onclick = function()
	{
		var ban = p.selectedUser;
		var banDisplayName = p.present[ban].displayName;
		var banExtra = p.present[ban].extra;
		var until = (new Date()).getTime() + p.banTime;
		if(confirm(p.strConfirmBan.replace('$1', banDisplayName)))
		{
			hawthorn.ban(p.channel, p.user, p.displayName, p.extra, p.permissions,
				p.keyTime, p.key, ban, banDisplayName, banExtra, until, function() {},
				function(error)
				{
					p.addError(error);
				});
		}
	};

	// Focus textbox
	setTimeout(function() { p.textBox.focus(); },0);
};

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
};

HawthornPopup.prototype.reAcquire = function(continuation)
{
	var p = this;
	hawthorn.reAcquire(this.keyAcquireURL, this.channel, this.user,
		this.displayName, this.extra, this.permissions,
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
};

HawthornPopup.prototype.handleMessages = function(messages)
{
	for(var i = 0; i < messages.length; i++)
	{
		var message = messages[i];
		if(message.type == 'JOIN')
		{
			this.addJoin(message.time, message.user, message.displayName,
				message.extra, message.user == this.user);
		}
		else if(message.type == 'LEAVE')
		{
			this.addLeave(message.time, message.user, message.displayName,
				message.extra, message.user == this.user);
		}
		else if(message.type == 'SAY')
		{
			this.addMessage(message.time, message.user, message.displayName,
				message.extra, message.text, message.user == this.user);
		}
		else if(message.type == 'BAN')
		{
			this.addBan(message.time, message.user, message.displayName,
				message.extra, message.ban, message.banDisplayName, message.banExtra,
				message.until, message.user == this.user);
		}
	}
};

HawthornPopup.prototype.startPoll = function()
{
	var p = this;
	setTimeout(function() { p.poll(); }, 50);
};

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
			p.addName(name.user, name.displayName, name.extra);
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
		hawthorn.recent(this.channel, this.user, this.displayName, this.extra,
			this.permissions, this.keyTime, this.key, this.maxAge, this.maxNumber,
			null, false, first, fail);
	}
	else
	{
		hawthorn.poll(this.channel, this.user, this.displayName, this.extra,
			this.permissions, this.keyTime, this.key, this.lastTime, ok, fail);
	}
};

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
			p.addName(name.user, name.displayName, name.extra);
		}
		ok(lastTime,messages);
	}
	var fail = function(error)
	{
		p.addError(error);
	};

	if(this.lastTime == 0)
	{
		hawthorn.recent(this.channel, this.user, this.displayName, this.extra,
			this.permissions, this.keyTime, this.key, this.maxAge, this.maxNumber,
			null, false, first, fail);
	}
	else
	{
		hawthorn.wait(this.channel, this.user, this.displayName, this.extra,
			this.permissions, this.keyTime, this.key, this.lastTime, ok, fail);
	}
};

/**
 * Sets data variables that hold the three main components (used later by
 * default implementations of addMessage etc).
 */
HawthornPopup.prototype.initLayout = function()
{
	var outer = document.createElement('div');

	// Intro section and close button
	var intro = document.createElement('div');
	outer.appendChild(intro);
	intro.id = 'intro';
	var closeButtonDiv = document.createElement('div');
	intro.appendChild(closeButtonDiv);
	closeButtonDiv.id = 'closebuttondiv';
	this.closeButton = document.createElement('input');
	closeButtonDiv.appendChild(this.closeButton);
	this.closeButton.type = 'button';
	this.closeButton.value = this.strCloseChat;
	var p = document.createElement('p');
	intro.appendChild(p);
	p.appendChild(document.createTextNode(this.strIntro));

	// Main and names
	this.mainArea = document.createElement("div");
	outer.appendChild(this.mainArea);
	this.mainArea.id = 'main';
	this.namesArea = document.createElement('div');
	outer.appendChild(this.namesArea);
	this.namesArea.id = 'names';
	// This div gets added to the top of the names area, because without it,
	// Firefox won't tab to the first name in the list.
	var firefoxBug = document.createElement('div');
	this.namesArea.appendChild(firefoxBug);

	// Ban button
	this.banButtonDiv = document.createElement('div');
	outer.appendChild(this.banButtonDiv);
	this.banButtonDiv.id = 'banbuttondiv';
	var inner = document.createElement('div');
	this.banButtonDiv.appendChild(inner);
	inner.className = 'inner';
	this.banButton = document.createElement('input');
	inner.appendChild(this.banButton);
	this.banButton.type = 'button';
	this.banButton.value = this.strBanUser;

	// Text box
	this.textBox = document.createElement('input');
	outer.appendChild(this.textBox);
	this.textBox.type = 'text';
	this.textBox.id = 'textbox';

	// Add the whole lot
	document.body.appendChild(outer);

	// Layout listener. It's too hard to make it work in CSS, so here's a JS
	// implementation.
	window.onresize = function()
	{
		var h = window.innerHeight;
		var w = window.innerWidth;

		var namesWidth = Math.floor(w / 4);

		var intro = document.getElementById('intro');
		intro.style.width = (w - 10) + 'px';
		var introHeight = intro.offsetHeight;

		var textbox = document.getElementById('textbox');
		var lowerHeight = textbox.offsetHeight;

		var banbuttondiv = document.getElementById('banbuttondiv');
		var banbuttondivHeight = banbuttondiv.style.display != 'none'
			? banbuttondiv.offsetHeight : 0;

		popup.mainArea.style.top = introHeight + 'px';
		popup.mainArea.style.width = (w - namesWidth - 10) + 'px';
		popup.mainArea.style.height = (h - lowerHeight - introHeight - 5) + 'px';

		popup.namesArea.style.top = introHeight + 'px';
		popup.namesArea.style.left = (w - namesWidth) + 'px';
		popup.namesArea.style.width = namesWidth + 'px';
		popup.namesArea.style.height = (h - lowerHeight - introHeight
			- banbuttondivHeight) + 'px';

		banbuttondiv.style.top = (h-lowerHeight-banbuttondivHeight) + 'px';
		banbuttondiv.style.left = (w-namesWidth) + 'px';
		banbuttondiv.style.width = (namesWidth) + 'px';

		textbox.style.top = (h - lowerHeight) + 'px';
		textbox.style.width = w + 'px';
	};

	// Put a classname on the BODY so that people can do per-channel CSS if they
	// like.
	if(document.body.className)
	{
		document.body.className += ' chan-' + this.channel;
	}
	else
	{
		document.body.className = ' chan-' + this.channel;
	}

	// Do layout
	window.onresize();
};

/**
 * Adds the given user to the name list (alphabetically sorted).
 * @param user User ID
 * @param displayName Display name for user
 * @param extra Extra user data
 */
HawthornPopup.prototype.addName = function(user, displayName, extra)
{
	var p = this;
	var el = this.present[user];
	if (el)
	{
		return;
	}

	var newEl = document.createElement('div');
	newEl.className = 'name';
	newEl.displayName = displayName;
	newEl.extra = extra;
	var newInner = document.createElement('div');
	newEl.appendChild(newInner);
	newInner.className = 'inner';
	newInner.appendChild(document.createTextNode(displayName));
	if(this.permissions.indexOf('m') != -1)
	{
		newEl.user = user;
		newEl.setAttribute('tabindex','0');
		p.selectedUser = null;
		newEl.onselectstart = function()
		{
			return false;
		};
		newEl.onkeypress = function(e)
		{
			var key = e.keyCode ? e.keyCode : e.which;
			if(key==13 || key==32)
			{
			  newEl.onmousedown();
			}
		};
		newEl.onmousedown = function()
		{
			for(var otherUser in p.present)
			{
				if(otherUser == user && p.selectedUser != user)
				{
					p.present[otherUser].className = 'name selected';
				}
				else
				{
					p.present[otherUser].className = 'name';
				}
			}
			if(p.selectedUser == user)
			{
				p.setSelectedUser(null);
			}
			else
			{
				p.setSelectedUser(user);
			}
			p.textBox.focus();
			return false;
		};
	}
	this.present[user] = newEl;

	for(var current = this.namesArea.firstChild; current != null;
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
};

HawthornPopup.prototype.setSelectedUser = function(selectedUser)
{
	this.selectedUser = selectedUser;
	this.banButton.disabled = selectedUser == null || selectedUser == this.user;
};

/**
 * Removes the given user from the name list.
 * @param user User ID
 * @param displayName Display name for user
 */
HawthornPopup.prototype.removeName = function(user,displayName)
{
	var el = this.present[user];
	if(!el)
	{
		return;
	}
	this.namesArea.removeChild(el);
	delete this.present[user];
	if(this.selectedUser == user)
	{
		this.setSelectedUser(null);
	}
};

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
};

/**
 * Adds a newly-received message to the message area.
 * @param time Message time (ms since 1970)
 * @param user User ID
 * @param displayName Display name for user
 * @param extra Extra user data
 * @param message Message from user
 * @param self True if this is a message from the current user
 */
HawthornPopup.prototype.addMessage = function(time, user, displayName, extra,
	message, self)
{
	var entry = document.createElement('div');
	entry.className='entry say' + (self ? ' self' : '');
	var inner = document.createElement('div');
	entry.appendChild(inner);
	inner.className = 'message';
	inner.appendChild(document.createTextNode('<'));
	var name = document.createElement('strong');
	name.className = 'name';
	name.appendChild(document.createTextNode(displayName));
	inner.appendChild(name);
	inner.appendChild(document.createTextNode('> ' + message));
	this.addMessageExtra('SAY', extra, entry);
	this.addEntry(time, entry);
};
 
/**
 * Adds a newly-received join message to the message area.
 * @param time Message time (ms since 1970)
 * @param user User ID
 * @param displayName Display name for user
 * @param extra Extra user data
 * @param self True if this is a message from the current user
 */
HawthornPopup.prototype.addJoin = function(time, user, displayName, extra, self)
{
	var entry = document.createElement('div');
	entry.className='entry join' + (self ? ' self' : '');
	var inner = document.createElement('div');
	entry.appendChild(inner);
	inner.className = 'message';
	inner.appendChild(document.createTextNode('\u2022 '));
	var name = document.createElement('strong');
	name.className='name';
	name.appendChild(document.createTextNode(displayName));
	inner.appendChild(name);
	inner.appendChild(document.createTextNode(this.strJoined));
	this.addMessageExtra('JOIN', extra, entry);
	this.addEntry(time, entry);
	this.addName(user, displayName, extra);
};

/**
 * Adds a newly-received leave message to the message area.
 * @param time Message time (ms since 1970)
 * @param user User ID
 * @param displayName Display name for user
 * @param extra Extra user data
 * @param self True if this is a message from the current user
 */
HawthornPopup.prototype.addLeave = function(time, user, displayName, extra,
	self)
{
	this.removeName(user, displayName);
	var entry = document.createElement('div');
	entry.className = 'entry leave' + (self ? ' self' : '');
	var inner = document.createElement('div');
	entry.appendChild(inner);
	inner.className = 'message';
	inner.appendChild(document.createTextNode('\u2022 '));
	var name = document.createElement('strong');
	name.className = 'name';
	name.appendChild(document.createTextNode(displayName));
	inner.appendChild(name);
	inner.appendChild(document.createTextNode(this.strLeft));
	this.addMessageExtra('LEAVE', extra, entry);
	this.addEntry(time, entry);
};

 /**
 * Adds a newly-received ban notice to the message area.
 * @param time Message time (ms since 1970)
 * @param user User ID sending message
 * @param displayName Display name for user
 * @param ban User being banned
 * @param banDisplayName Display name of user being banned
 * @param until Time user is banned until
 * @param self True if it is the current user who set the ban
 */
HawthornPopup.prototype.addBan = function(time, user, displayName, extra,
	ban, banDisplayName, banExtra, until, self)
{
	var entry = document.createElement('div');
	entry.className='entry ban' + (self ? ' self' : '');
	var inner = document.createElement('div');
	entry.appendChild(inner);
	inner.className = 'message';
	inner.appendChild(document.createTextNode('\u2022 '));
	var name = document.createElement('strong');
	name.className = 'name';
	name.appendChild(document.createTextNode(displayName));
	inner.appendChild(name);
	inner.appendChild(document.createTextNode(this.strBanned));
	name = document.createElement('strong');
	name.className = 'name';
	name.appendChild(document.createTextNode(banDisplayName));
	inner.appendChild(name);
	this.addMessageExtraBan(extra, banExtra, entry);
	this.addEntry(time, entry);
};

/**
 * Can be overridden to put extra information into display based on the
 * 'extra' data. Default does nothing.
 * @param type Type of call (JOIN, LEAVE, SAY, BAN)
 * @param extra Extra data
 * @param entry Entry element
 */
HawthornPopup.prototype.addMessageExtra = function(type, extra, entry)
{
};

/**
 * Can be overridden to put extra information into display based on the
 * 'extra' and 'ban extra' data. Default calls standard addMessageExtra().
 * @param extra Extra data about user sending ban
 * @param banExtra Extra data about banned user
 * @param entry Entry element
 */
HawthornPopup.prototype.addMessageExtraBan = function(extra, banExtra, entry)
{
	this.addMessageExtra('BAN', extra, entry);
};

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
};

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
	hawthorn.say(this.channel, this.user, this.displayName, this.extra,
		this.permissions, this.keyTime, this.key, text, function() {},
		this.addError);
	// Make it poll again real soon to get this (if polling)
	this.pollTime = 0;
};
