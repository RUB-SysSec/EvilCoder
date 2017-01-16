from joern.all import JoernSteps

#joern_db = None

#def clear_joern():
#	global joern_db
#	joern_db = None

def init_joern():
	joern_db = JoernSteps()
	joern_db.setGraphDbURL("http://localhost:7474/db/data/")
	joern_db.connectToDatabase()
	return joern_db

def queryNodeIndex(joern_db, query):
	return joern_db.runGremlinQuery("g.queryNodeIndex('%s')" % (query))


# Find the first parent-id, which has an incoming reaches-edge
# Input: id of node in question
# Output:	node_id		<=> node has incoming REACHES-edge
#		-1		<=> node has no parent
#		recursive	<=> node has parents
def find_reached_parent(joern_db, node_id, edge_type="DATA_FLOW_EDGE", in_edge = True): # REACHES
	e = None
	if(in_edge):
		e = joern_db.runGremlinQuery("g.v(%s).in(%s)" % (node_id, edge_type))
	else:
		e = joern_db.runGremlinQuery("g.v(%s).out(%s)" % (node_id, edge_type))
	if(len(e) > 0):
		return node_id

	p = joern_db.runGremlinQuery("g.v(%s).parents()" % (node_id))
	if(len(p) == 0):
		return -1
	elif(len(p) != 1):
		raise Exception("expected only one parent")

	return find_reached_parent(joern_db, p[0]._id)


def get_function_id(joern_db, node_id):
	node = joern_db.runGremlinQuery("g.v(%s)" % (node_id))
	if("functionId" in node):
		return node["functionId"]
	else:
		p = joern_db.runGremlinQuery("g.v(%s).parents()" % (node_id))[0]
		return get_function_id(joern_db, p._id)

# Find the ID of a function-definition, the function's name
# Assumes that the function's name is unique
def get_function_id_by_name(joern_db, func_name):
	call = joern_db.runGremlinQuery("getFunctionsByName('%s')" % func_name)
	if(len(call) != 1):
		return -1
#		raise Exception("expected length 1")
	return call[0]._id


def get_function_ids_by_name(joern_db, func_name):
	calls = joern_db.runGremlinQuery("getFunctionsByName('%s').id" % func_name)
	return calls





# Returns all calls to a function, given the function's name.
# Simple frontend to the getCallsTo-Query.
def get_calls_to(joern_db, func_name):
	calls = joern_db.runGremlinQuery("getCallsTo('%s')" % func_name) 
	return calls



def remove_edge_from_db(joern_db, edge_id):
	joern_db.runGremlinQuery("g.e(%s).remove()" % (edge_id))


def properties_to_gremlin_list(properties):
	str_properties = "[";
	for k in properties.keys():
		str_properties += k + ": \"" + str(properties[k]).replace("\"", "\\\"") + "\", "
	if(len(str_properties) > 1):
		str_properties = str_properties[0:-2] + "]"
	else:
		str_properties += "]"

	return str_properties




def addNode(joern_db, properties):
	str_properties = properties_to_gremlin_list(properties)
	q = "g.addVertex(null, %s)" % str_properties
#	print q
	newNode = joern_db.runGremlinQuery(q)
	return newNode


def addRelationship(joern_db, src, dst, relType, properties):
	if(len(properties) == 0):
		q = "g.addEdge(null, g.v(%s), g.v(%s), '%s')" % (src, dst, relType)
	else:
		str_properties = properties_to_gremlin_list(properties)
		q = "g.addEdge(null, g.v(%s), g.v(%s), '%s', %s)" % (src, dst, relType, str_properties)
#	print q
	joern_db.runGremlinQuery(q)



def getNodeById(joern_db, node_id):
	node = joern_db.runGremlinQuery("g.v(%s)" % (node_id))
	return node


def getCalleeFromCall(joern_db, node_id):
	node = joern_db.runGremlinQuery("g.v(%s).callToCallee()" % (node_id))
	return node

def getNodeType(joern_db, node_id):
	node = joern_db.runGremlinQuery("g.v(%s)" % (node_id))
	return node["type"]

def getNodeCode(joern_db, node_id):
	node = joern_db.runGremlinQuery("g.v(%s)" % (node_id))
	return node["code"]

def getOperatorCode(joern_db, node_id):
	node = joern_db.runGremlinQuery("g.v(%s)" % (node_id))
	return node["operator"]

def getNodeChildNum(joern_db, node_id):
	node = joern_db.runGremlinQuery("g.v(%s)" % (node_id))
	if("childNum" in node):
		return int(node["childNum"])
	else:
		return 0

