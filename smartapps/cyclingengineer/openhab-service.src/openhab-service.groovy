/**
 *  OpenHAB Service
 *
 *  Copyright 2015 Paul Hampson
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "OpenHAB Service",
    namespace: "cyclingengineer",
    author: "Paul Hampson",
    description: "Allow SmartThings to interact with an OpenHAB server",
    category: "My Apps",
    iconUrl: "https://avatars3.githubusercontent.com/u/1007353?v=3&s=50",
    iconX2Url: "https://avatars3.githubusercontent.com/u/1007353?v=3&s=100",
    iconX3Url: "https://avatars3.githubusercontent.com/u/1007353?v=3&s=200")


preferences {
	page(name: "pageOne", title: "Server Configuration", nextPage: "sitemapSelection", uninstall: true, install: false) {
        section("Server details"){
        	input "name", "text", title: "Display Name", description: "Name of server", required: true
        	input "hostname", "text", title: "IP Address", description: "Address of OpenHAB server", required: true
            input "port", "number", title: "Port", description: "Port of OpenHAB server", required: true
        }
    }    
    page(name: "sitemapSelection", content: "sitemapSelection", nextPage: "itemSelection")
    page(name: "itemSelection", content: "itemSelection", nextPage: "installPage")
    page(name: "installPage", title: "Ready to install!", install: true, uninstall: true) {
    	section("Install Ready!"){
    		paragraph "OpenHAB items are now ready for install! Press install to continue"
        }
    }
    
}

def sitemapSelection() {
 	int refreshCount = !state.refreshCount ? 0 : state.refreshCount as int
	state.refreshCount = refreshCount + 1
	def refreshInterval = 5
    
    if(!state.subscribe) {
		subscribe(location, null, locationHandler, [filterEvents:false])
		state.subscribe = true
	}
    
    // every 15s query server
    if ((refreshCount % 3) == 0)
    {
    	log.debug "calling queryOpenhabSitemaps()"
    	queryOpenhabSitemaps()
    }
    
    def sitemapList = ["__st_reserved_allItems":"All Items"] + getDiscoveredSitemaps()
    def num_found = sitemapList.size() ?: 0
    
    dynamicPage(name: "sitemapSelection", title: "OpenHAB Sitemap Selection", uninstall: true, install: true, refreshInterval: refreshInterval) {
    	section ("Choose Sitemap") {
        	paragraph "Please be patient. It may take some time to query the OpenHAB server you selected"
        	input( name:"sitemaps", type: "enum", title: "Sitemaps (${num_found} found)", required: true, multiple: true, options: sitemapList)
        }
    }
}

def itemSelection() {   
    int refreshCount = !state.refreshCount ? 0 : state.refreshCount as int
	state.refreshCount = refreshCount + 1
	def refreshInterval = 5
    def sitemapslist
    
    if(!state.subscribe) {
		subscribe(location, null, locationHandler, [filterEvents:false])
		state.subscribe = true
	}
    
    // every 15s query server
    if ((refreshCount % 3) == 0)
    {
    	//log.debug "settings.sitemaps = "+settings.sitemaps
        //if (!(settings.sitemaps instanceof List)) {
       	//	sitemapslist = [settings.sitemaps]
    	//} else {
        //	sitemapslist = settings.sitemaps
    	//}
    	   	
        //sitemapslist.each {
        //    if (it == "__st_reserved_allItems") queryOpenhabAllItems()
        //    else querySitemapItems(it)
        //}
    }
    
    def itemList = getDiscoveredItems()
    def num_found = itemList.size() ?: 0
    
    dynamicPage(name: "itemSelection", title: "OpenHAB Item Selection", uninstall: true, install: true, refreshInterval: refreshInterval) {
    	section ("Choose Items") {
        	paragraph "Please be patient. It may take some time to query the OpenHAB server you selected"
        	input( name:"items", type: "enum", title: "Items (${num_found} found)", required: true, multiple: true, options: itemList)
        }
    }
    	
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	if (settings.items) {
        addOpenhabChildDevices()
	}
}

private addOpenhabChildDevices()
{
	def itemsList = getOpenhabItems()
    
    def selectedItems
    if (!(settings.items instanceof List)) {
    	selectedItems = [settings.items]
    } else {
    	selectedItems = settings.items
    }
        
	settings.items.each { itemName ->
    	def d = getChildDevice(itemName)
        if (!d) {
        	def newDevice = items.find{ it.name == itemName }
    		def dni = "${settings.hostname}:${settings.port}/rest/item/${d.name}"
    		log.debug "creating child for ${itemName} of type ${newDevice?.value.type}"
            def deviceType = "OpenHAB ${newDevice?.value.type}"            
    		addChildDevice("cyclingengineer", deviceType, dni, null, ["label":newDevice?.value.label, "name":itemName])    		
        }
    }
    	subscribeAll()
    
}

private queryOpenhabAllItems()
{	
	log.debug "calling queryOpenhabAllItems()"
	def deviceNetworkId = settings.hostname + ":" + settings.port
	
    def action = new physicalgraph.device.HubAction(
    method: "GET",    
    path: "/rest/items?type=json",
    headers: [
        HOST: "${deviceNetworkId}"
    ])
    
    sendHubCommand(action)
}

private queryOpenhabSitemaps()
{	
	log.debug "calling queryOpenhabSitemaps()"
	def deviceNetworkId = settings.hostname + ":" + settings.port
	
    def action = new physicalgraph.device.HubAction(
    method: "GET",
    path: "/rest/sitemaps?type=json",
    headers: [
        HOST: "${deviceNetworkId}"
    ])
    
    sendHubCommand(action)
}

private querySitemapItems(sitemapName)
{
	log.debug "calling querySitemapItems(sitemapName=\"${sitemapName}\")"    
    def deviceNetworkId = settings.hostname + ":" + settings.port
	
    def action = new physicalgraph.device.HubAction(
    method: "GET",
    path: "/rest/sitemaps/${sitemapName}",
    headers: [
        HOST: "${deviceNetworkId}",
        "Accept":"application/json"
    ])
    log.debug "action = "+action
    sendHubCommand(action)
}

private getOpenhabItems()
{
	if (!state.itemList) state.itemList = [:]    
    state.itemList
}

private getOpenhabSitemaps()
{
	if (!state.sitemapList) state.sitemapList = [:]    
    state.sitemapList
}

private itemTypeSupported(type)
{
	(type == "SwitchItem" ) //|| type == "DimmerItem" || type == "ContactItem")
}

private processOpenhabItems(itemMap)
{
	def itemList = getOpenhabItems()
    
    itemMap.each{
    	if (itemTypeSupported(it.type)) {
        	
            def itemName = it.name
            def itemType = it.type
            if (!itemList.find { it.key == itemName }) {
            	// switch isn't already in the list so add it
            	itemList << ["${itemName}" : [ name: itemName, type:itemType ]]
                log.debug "added item ${itemName} to list"
            }
        }        
    }
}

private processOpenhabSitemaps(sitemapMap)
{
	def sitemapList = getOpenhabSitemaps()
    
    sitemapMap.each{
        def sitemapName = it.name
        def sitemapLabel = it.label
        
        if (!sitemapList.find { it.key == sitemapName }) {
         	// sitemap isn't already in the list so add it
           	sitemapList << ["${sitemapName}" : [ label: sitemapLabel ]]
            log.debug "added sitemap ${sitemapName} (${sitemapLabel}) to list"
        }        
        
    }
}

private processOpenhabSitemapItems(homepage)
{
	def itemList = getOpenhabItems()
    
    homepage.widget.each {  
    	// navigate the tree to find all the items in the sitemap
        processSitemapWidget(it)        
    }
}

private processSitemapWidget(widget)
{  
	//log.debug "calling processSitemapWidget"
    //log.debug widget
    
    if (widget instanceof Map){ // not an array of widgets, so process this one
    	//log.debug "not array"
    	if (widget.containsKey("item")){ // we got an item
            def itemList = getOpenhabItems()
    		def item = widget["item"]
        	def label = widget["label"] - ~/\\s?[.*]\s?$/
        	def name = item["name"]
        	def type = item["type"]
        
        	//log.debug "Found \""+name+"\""
        
        	if (itemTypeSupported(type)) {
            	if (!itemList.find { it.key == name }) {
            		// switch isn't already in the list so add it
            		itemList << ["${name}" : [ name: name, type:type, label:label ]]
                	log.debug "added item ${name} to list"
            	}
        	}
    	}
    	if (widget.containsKey("widget")){ // we have more widgets below us so keep traversing
        	//log.debug "Frame"
    		processSitemapWidget(widget["widget"])        
    	}
    	if (widget.containsKey("linkedPage")){ // found another page so keep traversing
        	//log.debug "linkedPage"
    		processSitemapWidget(widget["linkedPage"])
    	}
    }
    else { // array of widgets
    	widget.each{
        	processSitemapWidget(it)
        }
    }
}

private getDiscoveredItems() {
	def itemList = getOpenhabItems()
    
    def map = [:]
    itemList.each {
    	def typeStringList = ["SwitchItem":"Switch", "DimmerItem":"Dimmer", "ContactItem":"Contact"]
        def value = ""
        if (it.value.label) {
        	value = it.value.label + " ("+it.value.name+" - "+typeStringList[it.value.type] +")"
        }
        else {
    		value = it.value.name + " - "+ typeStringList[it.value.type]
        }
        def key = it.value.name
        map["${key}"] = value        
    }
    map
}

private getDiscoveredSitemaps() {
	def sitemapList = getOpenhabSitemaps()
    
    def map = [:]
    sitemapList.each {    	
    	def value = it.value.label
        def key = it.key
        map["${key}"] = value        
    }
    map
}

def locationHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId
	def parsedEvent = parseLanMessage(description)
	parsedEvent << ["hub":hub]
    log.debug "Got event..."
    
    if (parsedEvent.json) {
    	log.debug "got json..."        
    	def parsedJson = parsedEvent.json     
        
        if (parsedJson.item) {
        	log.debug "Got openhab item list"
        	processOpenhabItems(parsedJson.item)
        }
        else if (parsedJson.homepage) {
        	log.debug "Got openhab sitemap items"
            processOpenhabSitemapItems(parsedJson.homepage)
        }
        else if (parsedJson.sitemap) {
        	log.debug "Got openhab sitemap list"
        	processOpenhabSitemaps(parsedJson.sitemap)
        }
        
        
    } else {
    	log.debug "******** no json *********"
    	log.debug "description = "+description
        log.debug "parsedEvent = "+parsedEvent
    }
    
}