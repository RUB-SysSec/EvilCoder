# Function to count error keywords and secondary error keywords in a piece of code.
def count_error_keywords(src):
	error_keywords = "return exit abort throw warning error".split(" ")
	secondary_error_keywords = "warn err signal raise longjmp".split(" ")

	nof_error_keywords = 0
	for it in error_keywords:
		if(src.find(it) != -1):
			nof_error_keywords += 1
#			print "found error-keyword \"" + it + "\""

	nof_secondary_error_keywords = 0
	for it in secondary_error_keywords:
		if(src.find(it) != -1):
			nof_secondary_error_keywords += 1
#			print "found secondary-error-keyword \"" + it + "\""

	return nof_error_keywords, nof_secondary_error_keywords



# Count error-keywords in if- and else-body, and uses this to guess,
# whether the if should be instrumented to be always executed.
# Return values:
#	 1: Always execute
#	 0: Don't know
#	-1: Never execute
def always_execute_guess(if_src, else_src):
	if_only_threshold = 2
	if_else_threshold = 2


	if_ek, if_sek = count_error_keywords(if_src)
	else_ek, else_sek = count_error_keywords(else_src)
	print "error-keywords in if:            ", if_ek
	print "secondary error-keywords in if:  ", if_sek
	print "error-keywords in else:          ", else_ek
	print "secondary error-keywords in else:", else_sek

	if(else_src == ""):
		val = 2*if_ek + if_sek
		if(val >= 2):
			return -1
		else:
			return 1
	else:
		val = 2*if_ek + if_sek - (2*else_ek + else_sek)
		if(abs(val) >= if_else_threshold):
			if(val > 0):
				return -1
			else:
				return 1
		else:
			return 0

