import sys

def read_file(file_name):
	f = open(file_name, "r")
	data = f.read()
	f.close()
	return data



def get_extent_of_compound(joern_db, compound_id):
	# For some reason, the compound_statement has boggled location-info
	# workaround...

	query = """g.v(%s).children()""" % compound_id
	children = joern_db.runGremlinQuery(query)
	start = sys.maxint
	end = -1
	for c in children:
		location_splitters = c['location'].split(':')
		start = min(start, int(location_splitters[2]))
		end = max(end, int(location_splitters[3]))
	return start, end


def get_body_extent(joern_db, body, data):
	body_start = None
	body_end = None
	if(body[0]["type"] == "CompoundStatement"):
		compound_id = body[0]._id
	
		# point to first/last char of content
		body_start, body_end = get_extent_of_compound(joern_db, compound_id)
	
		
		# rectify body to start at braces
		body_start -= 1
		while(data[body_start] != "{"):
			body_start -= 1
		body_end += 1
		while(data[body_end] != "}"):
			body_end += 1
	else:
		# simple instruction
		location_splitters = body[0]["location"].split(":")
		# point to first/last char of content
		body_start = int(location_splitters[2])
		body_end = int(location_splitters[3])

	return body_start, body_end


def get_if_extent(joern_db, if_condition_id):
	query = """g.v(%s)""" % if_condition_id
	if_condition = joern_db.runGremlinQuery(query)

	query = """g.v(%s).parents()[0].children()""" % if_condition_id
	ifs_childs = joern_db.runGremlinQuery(query)
	assert(len(ifs_childs) in [2,3])

	file_path = get_file_path(joern_db, if_condition)
	location_splitters = if_condition['location'].split(':')
	cond_start = int(location_splitters[2]) # points to first char of condition
	cond_end = int(location_splitters[3]) # points to last char of condition

	# rectify condition to start at braces
	data = read_file(file_path)
	cond_start -= 1
	while(data[cond_start] != "("):
		cond_start -= 1
	cond_end += 1
	while(data[cond_end] != ")"):
		cond_end += 1

	ret_dict = {"cond_start": cond_start, "cond_end": cond_end}

	if(len(ifs_childs) == 3):
		# it's an if-else

		# parent of condition => if; third child: ElseStatement,
		# it's child: compound_statement or instruction
		# For some reason, the compound_statement has boggled location-info
		# workaround...
		query = """g.v(%s).parents().children()[2].children()[0]""" % if_condition_id
		body = joern_db.runGremlinQuery(query)

		ret_dict["else_start"], ret_dict["else_end"] = get_body_extent(joern_db, body, data)


	# parent of condition => if; second child: compound_statement or instruction
	query = """g.v(%s).parents().children()[1]""" % if_condition_id
	body = joern_db.runGremlinQuery(query)
	ret_dict["body_start"], ret_dict["body_end"] = get_body_extent(joern_db, body, data)

	return file_path, ret_dict


def get_source_range(data, start, end):
	return data[start:end+1]


def get_if_else_source(data, data_dict):#body_start, body_end, else_start, else_end):
	if_src = get_source_range(data, data_dict["body_start"], data_dict["body_end"])
	else_src = ""
	if("else_start" in data_dict):
		else_src = get_source_range(data, data_dict["else_start"], data_dict["else_end"])
	return if_src, else_src


def instrument_if(joern_db, if_condition_id):
	file_path, data_dict = get_if_extent(joern_db, if_condition_id)
	data = read_file(file_path)
	if_src, else_src = get_if_else_source(data, data_dict)

	guess = if_inspection.always_execute_guess(if_src, else_src)
	always_execute = None
	if(guess == 1):
		always_execute = True
	elif(guess == -1):
		always_execute = False
	else:
		print "Dunno"
		sys.exit(1)
	print "Should always execute:", always_execute

	all_instr = instrumentation.get_all_instrumentations()
	possible = []
	for it in all_instr:
		dummy = it(file_path, data_dict)
		if(dummy.is_possible(always_execute)):
			possible.append(it)
	print "Choosing from", len(possible), "possible instrumentations"
	random_index = randrange(0, len(possible))
	instr = possible[random_index](file_path, data_dict)
	print "Instrumenting using:", instr.description
	instr.instrument()




def find_all_checks(joern_db, path):
	checks = []
	for p in path:
		ergo = joern_db.runGremlinQuery("g.v(%s).filter{it.type == 'Condition'}" % (p))
		if(len(ergo) != 0):
			checks.append(p)
	return checks





def is_relevant_check(joern_db, check_id, overarched_by):
	uses = joern_db.runGremlinQuery("""g.v(%s).uses().code""" % check_id)
	uses = set(uses)
#	print uses

	if(not overarched_by in uses):
		return False

	check_code = joern_db.runGremlinQuery("""g.v(%s).code""" % check_id)
	and_checks = check_code.split("&&")
	all_checks = []
	for it in and_checks:
		all_checks += it.split("||")

	all_checks = map(lambda c: " " + c + " ", all_checks)

	comp_ops = "< > <= >=".split(" ")
	for c in all_checks:
		for op in comp_ops:
			if((" " + op + " " in c) and " " + overarched_by + " " in c):
				return True
	return False




def insert_at_position(original, insert, position):
	return original[:position] + insert + original[position:]




class Instrumentation(object):
	def is_possible(self, always_execute):
		if(self.always_execute != always_execute):
			return False
		required = "cond_start cond_end".split(" ")
		for r in required:
			if(not r in self.data_dict):
				return False
		return True

	def __init__(self, file_path, data_dict):
		self.always_execute = None
		self.description = ""

		self.file_path = file_path
		self.data_dict = data_dict

	def instrument(self):
		pass




class Instrumentation_0_and(Instrumentation):
	# Just do as the base-class does
	def __init__(self, file_path, data_dict):
		super(Instrumentation_0_and, self).__init__(file_path, data_dict)
		self.always_execute = False
		self.description = "if(len < 256)   => if(0 && (len < 256))"

	def is_possible(self, always_execute):
		return super(Instrumentation_0_and, self).is_possible(always_execute)

	def instrument(self):
		self.cond_start = self.data_dict["cond_start"]
		self.cond_end = self.data_dict["cond_end"]

		data = read_file(self.file_path)
		data = insert_at_position(data, ")", self.cond_end)
		data = insert_at_position(data, "0 && (", self.cond_start+1)
		print data
		# write it to some file...


class Instrumentation_1_or(Instrumentation):
	# Just do as the base-class does
	def __init__(self, file_path, data_dict):
		super(Instrumentation_1_or, self).__init__(file_path, data_dict)
		self.always_execute = True
		self.description = "if(len < 256)   => if(1 || (len < 256))"


	def is_possible(self, always_execute):
		return super(Instrumentation_1_or, self).is_possible(always_execute)


	def instrument(self):
		self.cond_start = self.data_dict["cond_start"]
		self.cond_end = self.data_dict["cond_end"]

		data = read_file(self.file_path)
		data = insert_at_position(data, ")", self.cond_end)
		data = insert_at_position(data, "1 || (", self.cond_start+1)
		print data
		# write it to some file...


# More instrumentations...





def get_all_instrumentations():
	ret = [
  Instrumentation_0_and
 , Instrumentation_1_or
]
	return ret



def main(argv):
	if(len(argv) == 1):
		print "Usage:", argv[0], "[<path_node> <var_name>]^+"
		sys.exit(1)

	joern_db = init_joern()

	path = map(lambda it: int(it), argv[1::2])
    overarch = argv[2::2]

	all_checks = find_all_checks(joern_db, path)
	for c in all_checks:
		overarched_by = overarch[cur_path.index(c)-1]
#		print c, "is overarched by", overarched_by
		is_rel = is_relevant_check(joern_db, c, overarched_by)
		
		if(is_rel):
			print c, "seems to be relevant check:", joern_db.runGremlinQuery("g.v(%s).code" % (c))
		instrument_if(joern_db, c)


if(__name__ == "__main__"):
	main(sys.argv)





