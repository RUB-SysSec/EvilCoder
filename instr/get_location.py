from instrumentation import *

def get_file_path(joern_db, node):
	query = """g.v(%d).in().filter{it.type=='File'}.filepath""" % node['functionId']
	file_path = joern_db.runGremlinQuery(query)[0]
	if(len(file_path) == 0):
		raise Exception("No file path found")
	return file_path


def get_file_path_from_id(joern_db, node_id):
	query = """g.v(%s)""" % node_id
	node = joern_db.runGremlinQuery(query)
	return get_file_path(joern_db, node)



def get_general_parent(joern_db, node_id):
#AST_EDGE = 'IS_AST_PARENT'
#CFG_EDGE = 'FLOWS_TO'
#USES_EDGE = 'USE'
#DEFINES_EDGE = 'DEF'
#DATA_FLOW_EDGE = 'REACHES'
#FUNCTION_TO_AST_EDGE = 'IS_FUNCTION_OF_AST'
#CFG_TO_FUNCION_EDGE = 'IS_FUNCTION_OF_CFG' // JANNIK

	parent = joern_db.runGremlinQuery("g.v(%s).in(AST_EDGE)" % node_id)
	if(parent != []):
#		print "ast_parent:", parent
		return parent[0]._id

	parent = joern_db.runGremlinQuery("g.v(%s).in(FUNCTION_TO_AST_EDGE)" % node_id)
	if(parent != []):
#		print "func_ast_parents:", parent
		return parent[0]._id

	parent = joern_db.runGremlinQuery("g.v(%s).in(CFG_TO_FUNCTION_EDGE)" % node_id)
	if(parent != []):
#		print "func_cfg_parents:", parent
		return parent[0]._id
	raise Exception("Cannot find general parent for " + str(node_id))


def get_function_name_for_node_id(joern_db, node_id):
	cur_id = node_id
	func_id = None
	while(1):
		func_id = joern_db.runGremlinQuery("g.v(%s).functionId" % cur_id)
		if(func_id != None):
			break
		cur_id = get_general_parent(joern_db, cur_id)
#		print "cur_id:", cur_id
	func_name = joern_db.runGremlinQuery("g.v(%s).name" % func_id)
	return func_name


def get_location_for_node_id(joern_db, node_id):
	cur_id = node_id
	location = None
	while(1):
		location = joern_db.runGremlinQuery("g.v(%s).location" % cur_id)
		if(location != None):
			break
		cur_id = get_general_parent(joern_db, cur_id)
#		print "cur_id:", cur_id
	return location


read_files = dict()
def get_location_tuple(joern_db, node_id):
	global read_files
	node = joern_db.runGremlinQuery("g.v(%s)" % node_id)
	if(node["type"] in ["CFGExitNode", "Symbol"]):
		raise Exception("Cannot find location for CFGExitNode or Symbol")

	file_name = get_file_path_from_id(joern_db, node_id)
	func_name = get_function_name_for_node_id(joern_db, node_id)
	location = get_location_for_node_id(joern_db, node_id)
	line_no = int(location.split(":")[0])

	if(not file_name in read_files):
		f = open(file_name, "r")
		data = f.read()
		f.close()
		read_files[file_name] = data
#	extent = read_files[file_name][line_no-1]
	loc_splitters = location.split(":")
	extent = get_source_range(read_files[file_name], int(loc_splitters[2]), int(loc_splitters[3]))
	return (file_name, func_name, line_no, extent)





def test_get_location_tuple(joern_db):
	# Testing get_location_tuple for libpng, with function "png_do_write_transformations"
	all_nodes_of_func = joern_db.runGremlinQuery("""g.V.filter{it.functionId == %s}""" % "130007")
	for a in all_nodes_of_func:
		if(a["type"] in ["CFGExitNode", "Symbol"]):
			continue
		print a._id
		print get_location_tuple(joern_db, a._id)
	sys.exit(1)

